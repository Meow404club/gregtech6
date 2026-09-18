#!/usr/bin/env python3
"""scan_bedrock_ore — the p31-bedrock-ore-worldgen card's natural-generation gate driver.

The scan_strata_lens.py form (the p31-strata-lens card's separate normal+seed world scan),
committed per the tools/rcon README (no task-local tmp drivers). Per leg (forge / neoforge):

  boot #1: fresh world (world dirs deleted), level-type normal, the card seed
           6131000569321125127, the global RCON slot semaphore; forceload the 64x64
           chunk scan region (chunks x 0..63, z 64..127 — the PARITY TEST's calibrated
           window: the offline projection pins 27 chunk decisions, coal=1 graphite=1;
           16 boxes of 16x16 chunks = 256x256 blocks <= the 256/command cap); wait for
           the generation to settle; graceful stop; offline region scan.
  scan:    per-position hits of the bedrock-ore blocks (gt6:ore_bedrock_* and
           gt6:ore_small_bedrock_* prefixes) within y -64..-54, per-chunk counts.
  analysis: 1) the acceptance gate: coal >= 1 AND graphite >= 1 across the two boots
           (the offline decision set carries exactly one of each; block-survival drift
           may eat one in a single boot, so the gate is cross-boot — the pipeline-drift
           semantics of decisions.2026-09-18-p31-strata-lens-determinism-acceptance);
            2) the vein-count floor: >= 15 hit chunks per boot (of the 27 decisions);
            3) boot #2: DELETE the world again, same seed, same scan — the DECISION-level
           determinism face: the material-set union must be identical boot-to-boot, the
           total hit counts within +/-30%, and the per-chunk hit-material-set agreement
           >= 80% (block positions drift with the feature-list interplay, decisions do
           not — the offline replay pins the decisions exactly).

Artifacts: /tmp/p31bedrockscan_<leg>_boot<N>.log/.pid, /tmp/p31bedrockscan_<leg>_verdict.json

Usage:
  python3 tools/rcon/scan_bedrock_ore.py <worktree> <forge|neo>
"""

import argparse
import glob
import io
import json
import re
import shutil
import signal
import struct
import subprocess
import sys
import time
import zlib
from pathlib import Path

_REPO = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_REPO / "tools" / "rcon"))

import gt6server  # noqa: E402
import gt6rcon    # noqa: E402

NODE = {"forge": "1.20.1-forge", "neo": "1.21.1-neoforge"}
SEED = "6131000569321125127"
PREFIXES = ("gt6:ore_bedrock_", "gt6:ore_small_bedrock_")
CHUNK_X0, CHUNK_X1 = 0, 63      # the calibrated parity window (GT6BedrockOreWorldgenTest)
CHUNK_Z0, CHUNK_Z1 = 64, 127
Y_MIN, Y_MAX = -64, -54
GAME, RCON_PORT, QUERY = 26200, 26210, 26220
PASSWORD = "gt6"
LEG = "forge"
WORKTREE = None
RUN_DIR = None
boot_index = 0


def sh(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True).stdout


def provision():
    gt6server.provision_run_dir(str(WORKTREE), GAME, RCON_PORT, QUERY, PASSWORD, node=NODE[LEG])
    props = RUN_DIR / "server.properties"
    lines = props.read_text().splitlines()
    out = []
    for line in lines:
        key = line.split("=", 1)[0]
        if key == "level-type":
            out.append("level-type=minecraft\\:normal")
        elif key == "level-seed":
            out.append(f"level-seed={SEED}")
        else:
            out.append(line)
    if not any(l.startswith("level-seed=") for l in out):
        out.append(f"level-seed={SEED}")
    props.write_text("\n".join(out) + "\n")
    for d in ("world", "world_nether", "world_the_end"):
        p = RUN_DIR / d
        if p.exists():
            shutil.rmtree(p)


def boot_and_load(box_count):
    global boot_index
    boot_index += 1
    log = Path(f"/tmp/p31bedrockscan_{LEG}_boot{boot_index}.log")
    pid = Path(f"/tmp/p31bedrockscan_{LEG}_boot{boot_index}.pid")
    gt6server.start_server(str(WORKTREE), log, pid, gt6server.gradle_task(NODE[LEG]))
    try:
        deadline = time.time() + 1200
        reached = False
        while time.time() < deadline:
            text = log.read_text(errors="replace") if log.exists() else ""
            if "Done (" in text:
                reached = True
                break
            time.sleep(3)
        if not reached:
            raise RuntimeError("boot never reached Done")
    finally:
        if not reached:
            gt6server.stop_server(pid, owner=WORKTREE.name, force=True)
    time.sleep(5)
    client = gt6rcon.RconClient("127.0.0.1", RCON_PORT, PASSWORD, timeout=30.0,
                            first_timeout=90.0, quiet_window=2.0,
                            adaptive_quiet=False)  # the t1 lesson: scan-class wide replies get eaten by adaptive quiet
    client.connect()
    client.auth()

    def _alarm(signum, frame):
        raise RuntimeError("boot_and_load watchdog fired (80 min without completing)")
    signal.signal(signal.SIGALRM, _alarm)
    signal.alarm(4800)

    def rc(command):
        for attempt in (0, 1):
            try:
                frames = client.run_command(command)
                return " ".join(str(f) for f in frames) if isinstance(frames, list) else str(frames)
            except Exception as exc:
                print("rcon retry", attempt, command[:40], "->", exc, flush=True)
                try:
                    client.close()
                except Exception:
                    pass
                time.sleep(5)
                client.connect()
                client.auth()
        return ""

    def rc_strict(command):
        # every add must come back acknowledged; 0.5s spacing keeps the RCON link ordered
        # (the 16-box add storm lesson, the strata driver's rc_strict)
        for attempt in (0, 1, 2):
            reply = rc(command)
            if reply.strip():
                return reply
            print("empty reply, retry", attempt, command[:40])
            time.sleep(2)
        return ""

    def forceload_adds():
        boxes = [(x0, z0, min(x0 + 255, (CHUNK_X1 + 1) * 16 - 1), min(z0 + 255, (CHUNK_Z1 + 1) * 16 - 1))
                 for x0 in range(CHUNK_X0 * 16, (CHUNK_X1 + 1) * 16, 256)
                 for z0 in range(CHUNK_Z0 * 16, (CHUNK_Z1 + 1) * 16, 256)]
        for x0, z0, x1, z1 in boxes:
            print("forceload add:", rc_strict(f"forceload add {x0} {z0} {x1} {z1}")[:60])
            time.sleep(0.5)

    def forceload_count():
        reply = rc("forceload query")
        match = re.match(r"\s*(\d+)", reply)
        return (int(match.group(1)) if match else 0, reply[:80])

    def region_bytes():
        return sum(p.stat().st_size for p in RUN_DIR.glob("world/region/*.mca"))

    try:
        for attempt in range(3):
            forceload_adds()
            last, stable = -1, 0
            deadline = time.time() + 2400
            while time.time() < deadline:
                count, preview = forceload_count()
                print("forceload query:", preview, flush=True)
                if count == last and count >= box_count:
                    stable += 1
                    if stable >= 3:
                        break
                else:
                    stable = 0
                last = count
                time.sleep(10)
            if last >= box_count:
                break
            print(f"forceload region incomplete ({last}/{box_count}), re-adding (attempt {attempt + 1})", flush=True)
        last_bytes, stable_bytes = -1, 0
        gen_start = time.time()
        gen_deadline = gen_start + 5400
        while True:
            time.sleep(30)
            rc("save-all flush")
            time.sleep(5)
            size = region_bytes()
            print(f"region bytes: {size}", flush=True)
            if size == last_bytes:
                stable_bytes += 1
                if stable_bytes >= 4 and time.time() - gen_start >= 120:
                    break
            else:
                stable_bytes = 0
            last_bytes = size
            if time.time() > gen_deadline:
                raise RuntimeError("region generation never quiesced")
        time.sleep(30)  # decoration tail margin
        rc("save-all flush")
        time.sleep(10)
        rc("stop")
    finally:
        signal.alarm(0)
        client.close()
        time.sleep(3)
        gt6server.stop_server(pid, rcon=("127.0.0.1", RCON_PORT, PASSWORD))
    time.sleep(20)


# ------------------------------------------------------------------ anvil scan
# (the scan_strata_lens minimal reader — verified against that card's live scans)

def read_region(path):
    """Yield (cx, cz, nbt) for every chunk in an .mca file."""
    raw = path.read_bytes()
    if len(raw) < 8192:
        return
    for index in range(1024):
        entry = struct.unpack(">I", b"\x00" + raw[index * 4:index * 4 + 3])[0]
        sectors = raw[index * 4 + 3]
        if entry == 0 or sectors == 0:
            continue
        offset = entry * 4096
        length = struct.unpack(">I", raw[offset:offset + 4])[0]
        compression = raw[offset + 4]
        body = raw[offset + 5:offset + 4 + length]
        try:
            data = zlib.decompress(body) if compression == 2 else io.BytesIO(gzip_decompress(body))
        except Exception:
            continue
        try:
            nbt = parse_nbt(data if isinstance(data, bytes) else data.getvalue())
        except Exception:
            continue
        if nbt is None:
            continue
        cxp, czp = nbt.get("xPos"), nbt.get("zPos")
        if cxp is None:
            continue
        yield cxp, czp, nbt


def gzip_decompress(body):
    import gzip
    return gzip.decompress(body)


# --- minimal NBT (big-endian) reader: returns dict at top level Compound ---

def _r_int(b, o):
    return struct.unpack(">i", b[o:o + 4])[0]


def _r_short(b, o):
    return struct.unpack(">h", b[o:o + 2])[0]


def parse_nbt(b):
    nlen = struct.unpack(">H", b[1:3])[0]
    value, _ = read_compound(b, 3 + nlen)
    return value


def read_payload(b, o, tag):
    if tag == 1:
        return b[o], o + 1
    if tag == 2:
        return _r_short(b, o), o + 2
    if tag == 3:
        return _r_int(b, o), o + 4
    if tag == 4:
        return struct.unpack(">q", b[o:o + 8])[0], o + 8
    if tag == 5:
        return struct.unpack(">f", b[o:o + 4])[0], o + 4
    if tag == 6:
        return struct.unpack(">d", b[o:o + 8])[0], o + 8
    if tag == 7:
        n = _r_int(b, o); o += 4
        return b[o:o + n], o + n
    if tag == 8:
        n = _r_short(b, o); o += 2
        return b[o:o + n].decode("utf-8", "replace"), o + n
    if tag == 9:
        et = b[o]; o += 1
        n = _r_int(b, o); o += 4
        out = []
        for _ in range(n):
            v, o = read_payload(b, o, et)
            out.append(v)
        return out, o
    if tag == 10:
        return read_compound(b, o)
    if tag == 11:
        n = _r_int(b, o); o += 4
        return [struct.unpack(">i", b[o + 4 * i:o + 4 * i + 4])[0] for i in range(n)], o + 4 * n
    if tag == 12:
        n = _r_int(b, o); o += 4
        return [struct.unpack(">q", b[o + 8 * i:o + 8 * i + 8])[0] for i in range(n)], o + 8 * n
    raise ValueError(f"tag {tag}")


def read_compound(b, o):
    out = {}
    while True:
        tag = b[o]
        if tag == 0:
            return out, o + 1
        nlen = struct.unpack(">H", b[o + 1:o + 3])[0]
        name = b[o + 3:o + 3 + nlen].decode("utf-8", "replace")
        o = o + 3 + nlen
        v, o = read_payload(b, o, tag)
        out[name] = v


def decode_positions(section, prefixes):
    """Return (index, name) pairs of the palette entries starting with any prefix."""
    bs = section.get("block_states") or {}
    palette = [p.get("Name", "") for p in (bs.get("palette") or [])]
    hits = {i: n for i, n in enumerate(palette) if any(n.startswith(px) for px in prefixes)}
    if not hits:
        return []
    data = bs.get("data")
    if data is None:
        return [(i, hits[0]) for i in range(4096) if 0 in hits]
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for li, long_v in enumerate(data):
        if long_v < 0:
            long_v += 1 << 64
        for k in range(per_long):
            index = li * per_long + k
            if index >= 4096:
                break
            value = (long_v >> (k * bits)) & mask
            if value in hits:
                out.append((index, hits[value]))
    return out


def scan_world():
    """Return ({chunk_key: {materialish_id: count}}, total_blocks) over Status-full chunks.

    Only y -64..-54 counts (the acceptance's scan band: the bedrock face + the muffin +
    the tail band's lower reach); the per-chunk key set IS the chunk's hit-material set.
    """
    per_chunk, totals = {}, {}
    region_dir = RUN_DIR / "world" / "region"
    scanned = 0
    for path in sorted(glob.glob(str(region_dir / "r.*.*.mca"))):
        for cx, cz, nbt in read_region(Path(path)):
            if not (CHUNK_X0 <= cx <= CHUNK_X1 and CHUNK_Z0 <= cz <= CHUNK_Z1):
                continue
            scanned += 1
            status = str(nbt.get("Status", ""))
            if not status.endswith("full"):
                continue
            counts = {}
            for section in (nbt.get("sections") or []):
                y_base = (section.get("Y") or 0) * 16
                if y_base + 15 < Y_MIN or y_base > Y_MAX:
                    continue
                for index, name in decode_positions(section, PREFIXES):
                    y = y_base + (index >> 8)
                    if not (Y_MIN <= y <= Y_MAX):
                        continue
                    counts[name] = counts.get(name, 0) + 1
            if counts:
                key = f"{cx},{cz}"
                per_chunk[key] = counts
                for name, count in counts.items():
                    totals[name] = totals.get(name, 0) + count
    return per_chunk, totals, scanned


# ------------------------------------------------------------------ analysis

def analyze(per_chunk, totals, scanned, boot):
    hit_chunks = len(per_chunk)
    coal = sum(v for k, v in totals.items() if k.startswith("gt6:ore_bedrock_coal"))
    graphite = sum(v for k, v in totals.items() if k.startswith("gt6:ore_bedrock_graphite"))
    small = sum(v for k, v in totals.items() if k.startswith("gt6:ore_small_bedrock_"))
    large = sum(v for k, v in totals.items() if k.startswith("gt6:ore_bedrock_") and not k.startswith("gt6:ore_bedrock_coal")
                and not k.startswith("gt6:ore_bedrock_graphite"))
    r = {
        "boot": boot,
        "chunks_scanned": scanned,
        "hit_chunks": hit_chunks,
        "coal_blocks": coal,
        "graphite_blocks": graphite,
        "small_blocks": small,
        "other_large_blocks": large,
        "totals_by_id": totals,
        "per_chunk": per_chunk,
    }
    print(f"[boot {boot}] scanned={scanned} hit_chunks={hit_chunks} coal={coal} graphite={graphite} "
          f"small={small} other_large={large}", flush=True)
    return r


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("worktree")
    parser.add_argument("leg", choices=["forge", "neo"])
    args = parser.parse_args()

    global LEG, WORKTREE
    LEG = args.leg
    WORKTREE = Path(args.worktree).resolve()

    results = []
    for boot in (1, 2):
        provision()
        # 64x64 = 4096 chunks, 16 boxes of 16x16 chunks
        boot_and_load(4096)
        per_chunk, totals, scanned = scan_world()
        results.append(analyze(per_chunk, totals, scanned, boot))

    # ---- the gates (the cross-boot decision-level semantics, see the module docstring)
    gate = {}
    both = results[0], results[1]
    coal = max(both[0]["coal_blocks"], both[1]["coal_blocks"])
    graphite = max(both[0]["graphite_blocks"], both[1]["graphite_blocks"])
    gate["coal_ge_1"] = coal >= 1
    gate["graphite_ge_1"] = graphite >= 1
    gate["hit_floor_15"] = both[0]["hit_chunks"] >= 15 and both[1]["hit_chunks"] >= 15
    union0 = set()
    union1 = set()
    for chunk in both[0]["per_chunk"].values():
        union0.update(chunk.keys())
    for chunk in both[1]["per_chunk"].values():
        union1.update(chunk.keys())
    gate["material_union_identical"] = union0 == union1
    t0, t1 = sum(both[0]["totals_by_id"].values()), sum(both[1]["totals_by_id"].values())
    gate["totals_within_30pct"] = abs(t0 - t1) <= 0.30 * max(t0, t1, 1)
    keys0 = {k: frozenset(v.keys()) for k, v in both[0]["per_chunk"].items()}
    keys1 = {k: frozenset(v.keys()) for k, v in both[1]["per_chunk"].items()}
    common = set(keys0) | set(keys1)
    agree = sum(1 for k in common if keys0.get(k, frozenset()) == keys1.get(k, frozenset()))
    rate = agree / max(len(common), 1)
    gate["chunk_decision_agreement_ge_80"] = rate >= 0.80
    gate["agreement_rate"] = round(rate, 4)
    gate["PASS"] = all(v for v in gate.values() if isinstance(v, bool))

    verdict = {"leg": LEG, "seed": SEED, "region": f"x {CHUNK_X0}..{CHUNK_X1}, z {CHUNK_Z0}..{CHUNK_Z1}",
               "boots": results, "gates": gate}
    out = Path(f"/tmp/p31bedrockscan_{LEG}_verdict.json")
    out.write_text(json.dumps(verdict, indent=2, sort_keys=True))
    print("verdict:", json.dumps(gate, sort_keys=True), flush=True)
    print("verdict file:", out, flush=True)
    if not gate["PASS"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
