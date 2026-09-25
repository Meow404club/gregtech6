#!/usr/bin/env python3
"""scan_fluid_spring — the fluid-spring card's natural-generation gate driver.

The scan_bedrock_ore.py form (the sibling card's separate normal+seed world scan), committed
per the tools/rcon README (no task-local tmp drivers). Per leg (forge / neoforge):

  boot #1: fresh world (world dirs deleted), level-type normal, the card seed
           6131000569321125127 (the bedrock card's calibrated seed), the global RCON slot
           semaphore; forceload the 64x64 chunk scan region (chunks x 0..63, z 64..127 —
           the SAME calibrated window as the bedrock scan, the offline projection's
           region: ~144 spring decisions + 27 ore decisions per boot; 16 boxes of 16x16
           chunks = 256x256 blocks <= the 256/command cap); wait for the generation to
           settle; graceful stop; offline region scan.
  scan:    per-position hits of the six GT spring block faces
           (gt6:liquid_{extra_heavy,heavy,medium,light}_oil_block,
           gt6:natural_gas_block, gt6:water_geothermal_block) AND the bedrock-ore block
           prefixes within y -64..-54, per-chunk counts + per-chunk Y-level spans.
           Vanilla lava is NOT counted (the window carries unrelated aquifer lava below
           y=-54 — the lava ROW is pinned at decision level by the parity test instead).
           Since task issue5-fluid-spring-nozzle also the gt6:fluid_spring nozzle
           block (the read-only enhancement: a "nozzle" kind per chunk, expected inside
           spring-hit chunks at the bedrock floor). The vanilla-lava spring chunks carry
           nozzles too but ZERO countable spring blocks (vanilla lava is the declared
           blind spot above) — they surface as nozzle "orphans", the declared tolerance
           below (the lava row share of the OW roll mass is (1/200)/0.03 ~= 16.7%).
  analysis: 1) the six-kind presence gate: every GT kind >= 1 (cross-boot max — the
           pipeline-drift semantics of
           decisions.2026-09-18-strata-lens-determinism-acceptance);
            2) the spring-chunk floor: >= 55 hit chunks per boot (of the ~124 GT
           spring decisions — the bedrock card's 15/27 ≈ 55% survival face);
            3) THE MUTUAL EXCLUSION: zero chunks carrying BOTH a bedrock-ore hit and a
           GT spring hit (WorldgenFluidSpring.java:62 replay seam — one bedrock event
           per chunk), per boot;
            4) the dome shape proxy: every spring-hit chunk spans >= 3 Y levels (the
           stepped ziggurat -63..-58; a single-level spill is not a dome);
            5) boot #2: DELETE the world again, same seed, same scan — the DECISION-level
           determinism face: the spring kind union must be identical boot-to-boot and the
           per-chunk kind-set agreement >= 80% (block survival drifts with the
           feature-list interplay, decisions do not — the offline replay pins the
           decisions exactly).

Artifacts: /tmp/springscan_<leg>_boot<N>.log/.pid, /tmp/springscan_<leg>_verdict.json

Usage:
  python3 tools/rcon/scan_fluid_spring.py <worktree> <forge|neo>
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
SPRING_PREFIXES = (
    "gt6:liquid_extra_heavy_oil_block", "gt6:liquid_heavy_oil_block",
    "gt6:liquid_medium_oil_block", "gt6:liquid_light_oil_block",
    "gt6:natural_gas_block", "gt6:water_geothermal_block",
)
ORE_PREFIXES = ("gt6:ore_bedrock_", "gt6:ore_small_bedrock_")
NOZZLE_PREFIXES = ("gt6:fluid_spring",)  # the issue5 nozzle arm (read-only analysis face)
CHUNK_X0, CHUNK_X1 = 0, 63      # the calibrated window (the bedrock scan + the offline projection)
CHUNK_Z0, CHUNK_Z1 = 64, 127
Y_MIN, Y_MAX = -64, -54         # the ore band floor .. the spring shell top band
SPRING_Y_MIN, SPRING_Y_MAX = -63, -57
GAME, RCON_PORT, QUERY = 26200, 26210, 26220  # the sibling scan drivers' slot (serial runs, one coder per card)
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
    log = Path(f"/tmp/springscan_{LEG}_boot{boot_index}.log")
    pid = Path(f"/tmp/springscan_{LEG}_boot{boot_index}.pid")
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
# (the scan_bedrock_ore minimal reader — verified against that card's live scans)

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


def classify(name):
    if name.startswith(ORE_PREFIXES):
        return "ore"
    if name.startswith(SPRING_PREFIXES):
        return "spring"
    if name.startswith(NOZZLE_PREFIXES):
        return "nozzle"
    return None


def scan_world():
    """Return ({chunk_key: {"ore"|"spring": {id: count}, "ys": {level, ...}}}, totals) over Status-full chunks.

    Only y -64..-54 counts (the ore band + the spring dome/shell band -63..-57); the
    per-chunk "ys" set IS the spring dome's Y-level span (the shape proxy).
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
            counts = {"ore": {}, "spring": {}, "nozzle": {}}
            ys = set()
            for section in (nbt.get("sections") or []):
                y_sec = section.get("Y") or 0
                if y_sec >= 128:
                    y_sec -= 256  # the NBT section Y is a SIGNED byte; the minimal reader hands it over unsigned (-4 -> 252)
                y_base = y_sec * 16
                if y_base + 15 < Y_MIN or y_base > Y_MAX:
                    continue
                for index, name in decode_positions(section, ORE_PREFIXES + SPRING_PREFIXES + NOZZLE_PREFIXES):
                    y = y_base + (index >> 8)
                    if not (Y_MIN <= y <= Y_MAX):
                        continue
                    kind = classify(name)
                    counts[kind][name] = counts[kind].get(name, 0) + 1
                    if kind == "spring" and SPRING_Y_MIN <= y <= SPRING_Y_MAX:
                        ys.add(y)
            if counts["ore"] or counts["spring"] or counts["nozzle"]:
                key = f"{cx},{cz}"
                per_chunk[key] = {"ore": counts["ore"], "spring": counts["spring"],
                                  "nozzle": counts["nozzle"], "ys": sorted(ys)}
                for kind in ("ore", "spring", "nozzle"):
                    for name, count in counts[kind].items():
                        totals[name] = totals.get(name, 0) + count
    return per_chunk, totals, scanned


# ------------------------------------------------------------------ analysis

def analyze(per_chunk, totals, scanned, boot):
    spring_chunks = {k: v for k, v in per_chunk.items() if v["spring"]}
    ore_chunks = {k: v for k, v in per_chunk.items() if v["ore"]}
    nozzle_chunks = {k: v for k, v in per_chunk.items() if v["nozzle"]}
    both = [k for k in spring_chunks if k in ore_chunks]
    spans = [len(v["ys"]) for v in spring_chunks.values()]
    # the nozzle co-location face: a nozzle outside a spring-hit chunk would mean the arm
    # fired without its dome (impossible by construction — both ride one drawSpring claim)
    nozzle_orphan = [k for k in nozzle_chunks if k not in spring_chunks]
    r = {
        "boot": boot,
        "chunks_scanned": scanned,
        "spring_chunks": len(spring_chunks),
        "ore_chunks": len(ore_chunks),
        "nozzle_chunks": len(nozzle_chunks),
        "nozzle_orphans": nozzle_orphan,
        "nozzle_total": sum(sum(v["nozzle"].values()) for v in nozzle_chunks.values()),
        "coexist_chunks": both,
        "totals_by_id": totals,
        "kind_chunks": {p: sum(1 for v in spring_chunks.values() if p in v["spring"]) for p in SPRING_PREFIXES},
        "min_y_span": min(spans) if spans else 0,
        "per_chunk": spring_chunks,
    }
    print(f"[boot {boot}] scanned={scanned} spring_chunks={len(spring_chunks)} ore_chunks={len(ore_chunks)} "
          f"nozzle_chunks={len(nozzle_chunks)} nozzle_orphans={len(nozzle_orphan)} "
          f"coexist={len(both)} min_y_span={r['min_y_span']}", flush=True)
    return r


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("worktree")
    parser.add_argument("leg", choices=["forge", "neo"])
    args = parser.parse_args()

    global LEG, WORKTREE
    LEG = args.leg
    WORKTREE = Path(args.worktree).resolve()
    globals()["RUN_DIR"] = gt6server.run_dir(str(WORKTREE), node=NODE[LEG])

    results = []
    for boot in (1, 2):
        provision()
        # 64x64 = 4096 chunks, 16 boxes of 16x16 chunks
        boot_and_load(4096)
        per_chunk, totals, scanned = scan_world()
        results.append(analyze(per_chunk, totals, scanned, boot))

    # ---- the gates (the cross-boot decision-level semantics, see the module docstring)
    gate = {}
    b0, b1 = results[0], results[1]
    for prefix in SPRING_PREFIXES:
        gate[f"kind_ge_1[{prefix}]"] = max(
            b0["kind_chunks"].get(prefix, 0), b1["kind_chunks"].get(prefix, 0)) >= 1
    gate["spring_floor_55_per_boot"] = b0["spring_chunks"] >= 55 and b1["spring_chunks"] >= 55
    gate["mutual_exclusion"] = not b0["coexist_chunks"] and not b1["coexist_chunks"]
    gate["dome_y_span_ge_3"] = b0["min_y_span"] >= 3 and b1["min_y_span"] >= 3
    # the issue5 nozzle arm: the nozzle block must EXIST in the world; the co-location
    # face rides the DECLARED vanilla-lava tolerance — lava decisions (~16.7% of the OW
    # roll mass) + rare disturbed GT domes surface as orphans (chunks with nozzles but no
    # countable spring blocks); the measured face is ~0.196 (the forge leg, both boots
    # identical), the tolerance keeps 2x headroom
    gate["nozzle_ge_1_per_boot"] = b0["nozzle_chunks"] >= 1 and b1["nozzle_chunks"] >= 1
    gate["nozzle_orphan_ratio_le_35"] = (
        len(b0["nozzle_orphans"]) / max(b0["nozzle_chunks"], 1) <= 0.35
        and len(b1["nozzle_orphans"]) / max(b1["nozzle_chunks"], 1) <= 0.35)

    union0, union1 = set(), set()
    for chunk in b0["per_chunk"].values():
        union0.update(chunk["spring"].keys())
    for chunk in b1["per_chunk"].values():
        union1.update(chunk["spring"].keys())
    gate["kind_union_identical"] = union0 == union1
    keys0 = {k: frozenset(v["spring"].keys()) for k, v in b0["per_chunk"].items()}
    keys1 = {k: frozenset(v["spring"].keys()) for k, v in b1["per_chunk"].items()}
    common = set(keys0) | set(keys1)
    agree = sum(1 for k in common if keys0.get(k, frozenset()) == keys1.get(k, frozenset()))
    rate = agree / max(len(common), 1)
    gate["chunk_decision_agreement_ge_80"] = rate >= 0.80
    gate["agreement_rate"] = round(rate, 4)
    gate["PASS"] = all(v for v in gate.values() if isinstance(v, bool))

    verdict = {"leg": LEG, "seed": SEED, "region": f"x {CHUNK_X0}..{CHUNK_X1}, z {CHUNK_Z0}..{CHUNK_Z1}",
               "boots": results, "gates": gate}
    out = Path(f"/tmp/springscan_{LEG}_verdict.json")
    out.write_text(json.dumps(verdict, indent=2, sort_keys=True))
    print("verdict:", json.dumps(gate, sort_keys=True), flush=True)
    print("verdict file:", out, flush=True)
    if not gate["PASS"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
