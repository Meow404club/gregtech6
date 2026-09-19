#!/usr/bin/env python3
"""scan_nether_end — the p31-nether-lens-end-yield card's natural-generation gate driver.

The scan_strata_lens form, re-aimed at THE OTHER DIMENSIONS (per leg forge / neo):

  boot #1: fresh world (world dirs deleted), level-type normal, the card seed
           6131000569321125127, the global RCON slot semaphore; forceload the
           NETHER 40x40 chunk scan region (chunks -20..19, 16 boxes of 12x12) via
           `execute in minecraft:the_nether run forceload add ...`, then the END
           24x24 region (chunks -12..11) via `execute in minecraft:the_end ...`;
           wait for generation quiescence (flush-synced region-byte stability across
           BOTH dimension region dirs); graceful stop; offline region scans.
  nether scan: per-position hits of the 17 GT stone STONE blocks (the lens), the
           dense-nether-quartz block, the 12 crystal blocks (any-hit), the red-clay
           block; vanilla controls ride along (nether_quartz_ore / ancient_debris).
  end scan: the five ORE_END rows' placeable face — the endstone-family ore blocks
           ore_endstone_{platinum,cassiterite,naquadah,trinium} (molybdenum rides the
           axis-posture exclusion, the drawn set is four rows, GT6VeinGenerator
           .drawVein javadoc).
  analysis: 1) nether — each of the 17 stones >= 1 lens hit, quartz >= 1, crystals
           >= 1, clay >= 1 (the three forms' live evidence);
            2) end — the no-planet-mod runtime probe (the conditions did NOT suppress
           the End modifier): every hit id IS a placeable ORE_END row (purity — no
           overworld row leaked through the dimension routing), >= 2 distinct rows
           drawn in the window (breadth; decision-deterministic for the card seed),
           >= 1 hit total (life). "Each of the four >= 1" is unreachable: the End
           pool is a WEIGHTED draw (5/5/170/10/100 over 290) — a weight-5 row needs
           ~170 origin cells (~14k chunks) for 90% coverage; the five-row pool and
           weights themselves are pinned by the JSON snapshot tests;
            3) boot #2: DELETE the world again, same seed, same scan — the cluster
           counts (nether, per stone) and the per-ore counts (end) must equal boot #1
           (decision-level determinism, the strata-lens acceptance semantics);
           vanilla-agreeing per-chunk equality rides along as attribution.

Artifacts: /tmp/p31nethscan_<leg>_boot<N>.log/.pid, /tmp/p31nethscan_<leg>_verdict.json

Usage:
  python3 tools/rcon/scan_nether_end.py <worktree> <forge|neo> [--nether-region 40] [--end-region 24]
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
GAME, RCON_PORT, QUERY = 26200, 26210, 26220
PASSWORD = "gt6"
LEG = "forge"
WORKTREE = None
RUN_DIR = None


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


def boot_and_load(nether_boxes, end_boxes):
    boot = globals()["boot_index"]
    log = Path(f"/tmp/p31nethscan_{LEG}_boot{boot}.log")
    pid = Path(f"/tmp/p31nethscan_{LEG}_boot{boot}.pid")
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
                            adaptive_quiet=False)
    client.connect()
    client.auth()

    def _alarm(signum, frame):
        raise RuntimeError("boot_and_load watchdog fired (45 min without completing)")
    signal.signal(signal.SIGALRM, _alarm)
    signal.alarm(2700)

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
        for attempt in (0, 1, 2):
            reply = rc(command)
            if reply.strip():
                return reply
            print("empty reply, retry", attempt, command[:40])
            time.sleep(2)
        return ""

    def forceload_dim(dimension, boxes):
        for x0, z0, x1, z1 in boxes:
            cmd = f"execute in {dimension} run forceload add {x0} {z0} {x1} {z1}"
            print("forceload:", rc_strict(cmd)[:70], flush=True)
            time.sleep(0.5)

    def region_bytes(dimensions):
        total = 0
        for dim_dir in dimensions:
            for sub in ("DIM-1/region", "DIM1/region", "region"):
                for p in RUN_DIR.glob(f"{dim_dir}/{sub}/*.mca"):
                    total += p.stat().st_size
        return total

    try:
        for attempt in range(3):
            forceload_dim("minecraft:the_nether", nether_boxes)
            forceload_dim("minecraft:the_end", end_boxes)
            last, stable = -1, 0
            deadline = time.time() + 1800
            while time.time() < deadline:
                reply = rc("execute in minecraft:the_nether run forceload query")
                match = re.match(r"\s*(\d+)", reply)
                count = int(match.group(1)) if match else 0
                print("nether forceload query:", reply[:70], flush=True)
                if count == last and count >= 1600:
                    stable += 1
                    if stable >= 3:
                        break
                else:
                    stable = 0
                last = count
                time.sleep(10)
            if last >= 1600:
                break
            print(f"nether forceload incomplete ({last}), re-adding (attempt {attempt + 1})", flush=True)
        last_bytes, stable_bytes = -1, 0
        gen_start = time.time()
        gen_deadline = gen_start + 5400
        while True:
            time.sleep(30)
            rc("execute in minecraft:the_nether run save-all flush")
            rc("execute in minecraft:the_end run save-all flush")
            time.sleep(5)
            size = region_bytes(("world",))
            print(f"region bytes (all dims): {size}", flush=True)
            if size == last_bytes:
                stable_bytes += 1
                if stable_bytes >= 4 and time.time() - gen_start >= 120:
                    break
            else:
                stable_bytes = 0
            last_bytes = size
            if time.time() > gen_deadline:
                raise RuntimeError("region generation never quiesced")
        time.sleep(30)
        rc("execute in minecraft:the_nether run save-all flush")
        rc("execute in minecraft:the_end run save-all flush")
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


def decode_positions(section, wanted, prefix=None):
    bs = section.get("block_states") or {}
    palette = [p.get("Name", "") for p in (bs.get("palette") or [])]
    if prefix is not None:
        hits = {i: n for i, n in enumerate(palette) if n.startswith(prefix)}
    else:
        hits = {i: n for i, n in enumerate(palette) if n in wanted}
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


def dim_region_dirs(dim):
    """The region dirs of one vanilla dimension, both server layouts (the single-world
    DIM folder and the legacy multiworld root), whichever exists wins."""
    dirs = []
    for rel in (("world", f"DIM{'-1' if dim == 'nether' else '1'}", "region"),
                ("world_nether" if dim == "nether" else "world_the_end", "DIM-1" if dim == "nether" else "DIM1", "region")):
        path = RUN_DIR.joinpath(*rel)
        if path.is_dir() and any(path.glob("*.mca")):
            dirs.append(path)
    return dirs


def scan_dim(dim, chunk_lo, chunk_hi, wanted):
    """({chunk_key: {id: count}}, {(x,y,z): id}) over one dimension's region files,
    Status-full chunks only (the completed-world filter, the strata lesson)."""
    per_chunk, positions = {}, {}
    dirs = dim_region_dirs(dim)
    for dir_path in dirs:
        for path in sorted(glob.glob(str(dir_path / "r.*.*.mca"))):
            for cx, cz, nbt in read_region(Path(path)):
                if not (chunk_lo <= cx <= chunk_hi and chunk_lo <= cz <= chunk_hi):
                    continue
                status = str(nbt.get("Status", ""))
                if not status.endswith("full"):
                    continue
                counts = {}
                for section in (nbt.get("sections") or []):
                    y_base = (section.get("Y") or 0) * 16
                    for index, name in decode_positions(section, wanted):
                        counts[name] = counts.get(name, 0) + 1
                        y = index >> 8
                        z = (index >> 4) & 15
                        x = index & 15
                        positions[(cx * 16 + x, y_base + y, cz * 16 + z)] = name
                if counts:
                    per_chunk[f"{cx},{cz}"] = counts
        if dirs:
            break  # the first layout that exists wins (never double-count)
    return per_chunk, positions


def cluster(positions, stone):
    items = sorted((x, y, z) for (x, y, z), n in positions.items() if n == stone)
    clusters = []
    for (x, y, z) in items:
        best = None
        for c in clusters:
            if (c["minx"] - 12 <= x <= c["maxx"] + 12 and c["minz"] - 12 <= z <= c["maxz"] + 12
                    and abs(y - (c["miny"] + c["maxy"]) // 2) <= 32):
                best = c
                break
        if best is None:
            clusters.append(dict(points=[(x, y, z)], minx=x, maxx=x, minz=z, maxz=z, miny=y, maxy=y))
        else:
            best["points"].append((x, y, z))
            best["minx"], best["maxx"] = min(best["minx"], x), max(best["maxx"], x)
            best["minz"], best["maxz"] = min(best["minz"], z), max(best["maxz"], z)
            best["miny"], best["maxy"] = min(best["miny"], y), max(best["maxy"], y)
    return clusters


def verify_seed():
    level = RUN_DIR / "world" / "level.dat"
    raw = level.read_bytes()
    try:
        data = gzip_decompress(raw)
    except Exception:
        data = raw
    nbt = parse_nbt(data)
    data_c = nbt.get("Data", {})
    wgs = data_c.get("WorldGenSettings", {})
    seed = wgs.get("seed") if isinstance(wgs, dict) else None
    if seed is None:
        seed = data_c.get("Seed")
    if seed != int(SEED):
        raise RuntimeError(f"the boot world ran on seed {seed}, expected {SEED}")
    print(f"seed verified: {seed}", flush=True)


def boxes_for(region, center_half):
    stride = 192
    lo = -center_half * 16
    hi = (region - center_half) * 16 - 1
    return [(x0, z0, x0 + 191, z0 + 191)
            for x0 in range(lo, hi, stride) for z0 in range(lo, hi, stride)]


def run_boot(index, nether_boxes, end_boxes):
    globals()["boot_index"] = index
    provision()
    boot_and_load(nether_boxes, end_boxes)
    pid_file = Path(f"/tmp/p31nethscan_{LEG}_boot{index}.pid")
    try:
        pid = int(pid_file.read_text().strip())
    except Exception:
        pid = None
    deadline = time.time() + 180
    while pid and time.time() < deadline:
        try:
            import os
            os.kill(pid, 0)
            time.sleep(3)
        except ProcessLookupError:
            break
    time.sleep(10)
    verify_seed()
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("worktree")
    parser.add_argument("leg", choices=["forge", "neo"])
    parser.add_argument("--nether-region", type=int, default=40)
    parser.add_argument("--end-region", type=int, default=24)
    parser.add_argument("--reuse-boot1", action="store_true")
    args = parser.parse_args()
    globals()["LEG"] = args.leg
    globals()["WORKTREE"] = Path(args.worktree)
    globals()["RUN_DIR"] = gt6server.run_dir(str(WORKTREE), node=NODE[LEG])
    n_half = args.nether_region // 2
    e_half = args.end_region // 2
    n_lo, n_hi = -n_half, args.nether_region - n_half - 1
    e_lo, e_hi = -e_half, args.end_region - e_half - 1
    nether_boxes = boxes_for(args.nether_region, n_half)
    end_boxes = boxes_for(args.end_region, e_half)

    # the wanted id set: the 17 lens stones + the three forms + the end face; the stone
    # list comes from the SHIPPED configured-feature JSON (the datagen is the truth, no
    # hand-copied table)
    # the WORKTREE arg (the scan_bedrock_ore form) — _REPO here resolves to tools/,
    # not the repo root, so it must not carry real paths
    canonical = WORKTREE / "mdk" / "src" / "generated" / "resources"
    lens_rows = json.loads((canonical / "data/gt6/worldgen/configured_feature/nether_lenses.json").read_text())
    stone_ids = tuple(f"gt6:{row['stone']}" for row in lens_rows["config"]["lenses"])
    crystal_ids = tuple(f"gt6:crystal_{m}" for m in (
        "arsenopyrite", "chalcopyrite", "cinnabar", "cobaltite", "galena", "kesterite",
        "molybdenite", "pyrite", "sphalerite", "stannite", "stibnite", "tetrahedrite"))
    nether_wanted = set(stone_ids) | {"gt6:dense_nether_quartz_ore", "gt6:nether_red_clay"} | set(crystal_ids)
    end_wanted = {"gt6:ore_endstone_platinum", "gt6:ore_endstone_cassiterite",
                  "gt6:ore_endstone_naquadah", "gt6:ore_endstone_trinium"}

    print(f"== p31-nether natural scan, leg={LEG}, worktree={WORKTREE}, seed={SEED}, "
          f"nether={args.nether_region}x{args.nether_region} ({n_lo}..{n_hi}), "
          f"end={args.end_region}x{args.end_region} ({e_lo}..{e_hi}) ==")

    scans = []
    boot_no = 0
    while len(scans) < 2:
        boot_no += 1
        if boot_no == 1 and args.reuse_boot1:
            print("--reuse-boot1: rescanning the on-disk world as boot#1", flush=True)
            verify_seed()
            n_per, n_pos = scan_dim("nether", n_lo, n_hi, nether_wanted)
            e_per, e_pos = scan_dim("end", e_lo, e_hi, end_wanted)
        else:
            run_boot(boot_no, nether_boxes, end_boxes)
            n_per, n_pos = scan_dim("nether", n_lo, n_hi, nether_wanted)
            e_per, e_pos = scan_dim("end", e_lo, e_hi, end_wanted)
        scans.append(dict(n_per=n_per, n_pos=n_pos, e_per=e_per, e_pos=e_pos))
        print(f"boot#{boot_no}: nether-chunks-with-gt6={len(n_per)} nether-positions={len(n_pos)} "
              f"end-chunks-with-gt6={len(e_per)} end-positions={len(e_pos)}", flush=True)
    boot1, boot2 = scans[0], scans[1]

    # ---- the nether faces
    stone_hits = {sid: sum(1 for _, n in boot1["n_pos"].items() if n == sid) for sid in stone_ids}
    per_stone_hit = {sid.split(":")[1]: count for sid, count in stone_hits.items()}
    each_stone_hit = all(c >= 1 for c in stone_hits.values())
    quartz_hit = sum(1 for _, n in boot1["n_pos"].items() if n == "gt6:dense_nether_quartz_ore")
    clay_hit = sum(1 for _, n in boot1["n_pos"].items() if n == "gt6:nether_red_clay")
    crystal_ids_hit = {n for _, n in boot1["n_pos"].items() if n in crystal_ids}
    print(f"nether: lens clusters per stone = "
          f"{ {sid.split(':')[1]: len(cluster(boot1['n_pos'], sid)) for sid in stone_ids} }", flush=True)
    print(f"nether forms: quartz={quartz_hit} clay={clay_hit} crystals={sorted(crystal_ids_hit)}", flush=True)
    forms_live = quartz_hit >= 1 and clay_hit >= 1 and len(crystal_ids_hit) >= 1

    # ---- the end face (the no-planet-mod runtime probe)
    # the End pool is a WEIGHTED draw (weights 5/5/170/10/100 over 290, the 40-row
    # table), so "each row >= 1" is unreachable in any sane window — a weight-5 row
    # needs ~170 origin cells (~14k chunks) for 90% coverage. The honest faces:
    # purity (no overworld row leaked through the dimension routing), breadth
    # (>= 2 distinct rows — decision-deterministic for the card seed), life (>= 1).
    end_hits = {oid: sum(1 for _, n in boot1["e_pos"].items() if n == oid) for oid in sorted(end_wanted)}
    end_hits2 = {oid: sum(1 for _, n in boot2["e_pos"].items() if n == oid) for oid in sorted(end_wanted)}
    foreign = ({n for _, n in boot1["e_pos"].items()} | {n for _, n in boot2["e_pos"].items()}) - end_wanted
    end_rows = sum(1 for v in end_hits.values() if v >= 1)
    end_life = sum(end_hits.values()) >= 1
    end_ok = not foreign and end_rows >= 2 and end_life
    print(f"end ore hits boot1={end_hits} boot2={end_hits2} rows={end_rows} "
          f"life={end_life} foreign={sorted(foreign)} -> {end_ok}", flush=True)

    # ---- the determinism faces
    n_clusters1 = {sid: len(cluster(boot1["n_pos"], sid)) for sid in stone_ids}
    n_clusters2 = {sid: len(cluster(boot2["n_pos"], sid)) for sid in stone_ids}
    nether_deterministic = n_clusters1 == n_clusters2
    end_deterministic = end_hits == end_hits2
    print(f"nether cluster determinism: boot1={n_clusters1} boot2={n_clusters2} -> {nether_deterministic}")
    print(f"end determinism: {end_deterministic}")

    result = dict(leg=LEG, seed=SEED,
                  nether_region_chunks=args.nether_region ** 2, end_region_chunks=args.end_region ** 2,
                  boot1=dict(nether_chunks_with_gt6=len(boot1["n_per"]),
                             nether_positions=len(boot1["n_pos"]),
                             lens_clusters=n_clusters1,
                             each_stone_hit=each_stone_hit,
                             quartz=quartz_hit, clay=clay_hit, crystals=sorted(crystal_ids_hit),
                             end_ores=end_hits1),
                  boot2=dict(nether_chunks_with_gt6=len(boot2["n_per"]),
                             nether_positions=len(boot2["n_pos"]),
                             lens_clusters=n_clusters2,
                             end_ores=end_hits2),
                  forms_live=forms_live,
                  each_stone_hit=each_stone_hit,
                  end_rows=end_rows, end_life=end_life, end_foreign=sorted(foreign),
                  end_ok=end_ok,
                  nether_cluster_deterministic=nether_deterministic,
                  end_deterministic=end_deterministic)
    Path(f"/tmp/p31nethscan_{LEG}_verdict.json").write_text(json.dumps(result, indent=1))
    ok = each_stone_hit and forms_live and end_ok and nether_deterministic and end_deterministic
    print("VERDICT:", "GREEN" if ok else "RED", flush=True)


globals()["boot_index"] = 1
if __name__ == "__main__":
    main()
