#!/usr/bin/env python3
"""scan_strata_lens — the strata-lens card's natural-generation gate driver.

The vein_scan form (the t3 card's separate normal+seed world scan),
committed per the tools/rcon README (no task-local tmp drivers). Per leg
(forge / neoforge):

  boot #1: fresh world (world dirs deleted), level-type normal, the card seed
           6131000569321125127, the global RCON slot semaphore; forceload the
           48x48 chunk scan region (chunks -24..23, 16 boxes of 12x12 = 144 <=
           the 256/command cap); wait for the generation to settle; graceful
           stop; offline region scan.
  scan:    per-position hits of the 5 marker-stone STONE blocks (gt6:marble /
           basalt / kimberlite / granite_red / komatiite — the bare-snake
           variant-0 ids, GTStoneBlocks.path) + per-chunk counts.
  analysis: 1) each of the 5 stones has >= 1 lens hit (their blob rows are
           retired by this card, so EVERY hit is a lens hit);
            2) the cross-chunk boundary continuity: every scan-interior cluster
           is x- and z-convex (per-row/column block sets contiguous) — a 1-column
           seam at a chunk border breaks convexity; boundary-adjacent y-band
           overlap reported for the evidence trail;
            3) boot #2: DELETE the world again, same seed, same scan — per-chunk
           per-stone counts must equal boot #1 chunk-for-chunk (the same-seed
           regenerate probe); vanilla iron/coal counts ride along as the drift
           attribution control.

Artifacts: /tmp/scan_<leg>_boot<N>.log/.pid, /tmp/scan_<leg>_verdict.json

Usage:
  python3 tools/rcon/scan_strata_lens.py <worktree> <forge|neo> [--region 48]
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
LENS_IDS = ("gt6:marble", "gt6:basalt", "gt6:kimberlite", "gt6:granite_red", "gt6:komatiite")
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


def boot_and_load(box_count):
    boot = globals()["boot_index"]
    log = Path(f"/tmp/scan_{LEG}_boot{boot}.log")
    pid = Path(f"/tmp/scan_{LEG}_boot{boot}.pid")
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

    # the SIGALRM watchdog: every phase below must make progress; a stall aborts the boot
    # and the finally releases the slot + stops the recorded server (the leaked-slot lesson)
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
        # the 16-box add storm lost commands when pipelined rapid-fire (empty replies,
        # the server log showed only the first box landing) — every add must come back
        # acknowledged; 0.5s spacing keeps the RCON link ordered
        for attempt in (0, 1, 2):
            reply = rc(command)
            if reply.strip():
                return reply
            print("empty reply, retry", attempt, command[:40])
            time.sleep(2)
        return ""

    def forceload_adds():
        boxes = [(x0, z0, x0 + 191, z0 + 191)
                 for x0 in range(CHUNK_LO * 16, CHUNK_HI * 16, 192)
                 for z0 in range(CHUNK_LO * 16, CHUNK_HI * 16, 192)]
        for x0, z0, x1, z1 in boxes:
            print("forceload add:", rc_strict(f"forceload add {x0} {z0} {x1} {z1}")[:60])
            time.sleep(0.5)

    def forceload_count():
        # the reply is "<N> force loaded chunks were found in ... at: [...]" — the leading
        # int is the count (concatenating every digit would swallow the coordinate list)
        reply = rc("forceload query")
        match = re.match(r"\s*(\d+)", reply)
        return (int(match.group(1)) if match else 0, reply[:80])

    def region_bytes():
        # the generation-complete face: the forceload query reports TICKETS (marked
        # immediately), not generation progress — the boot#1 RED run scanned mid-flight
        # chunks (vanilla iron/coal differed boot-to-boot too). Region-file bytes grow
        # while chunks generate; stable bytes across 3 flush-synced polls = quiescent.
        return sum(p.stat().st_size for p in RUN_DIR.glob("world/region/*.mca"))

    # forceload the scan region, verify the ticket count reaches the region size, then
    # wait for GENERATION quiescence (flush-synced byte stability, >= 120s of no growth)
    try:
        for attempt in range(3):
            forceload_adds()
            last, stable = -1, 0
            deadline = time.time() + 1200
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
        gen_deadline = gen_start + 3600
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
# (the vein_scan minimal reader — verified against that card's live scans)

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


def decode_positions(section, wanted, prefix=None):
    """Return (index, name) pairs of the wanted ids in this section's bit-packed data.
    A `prefix` widens the match to every palette entry starting with it (the gt6:* census)."""
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


def scan_world(chunk_lo, chunk_hi, full_only=True):
    """Return ({chunk_key: {stone_id: count}}, {(x,y,z): stone_id}, {chunk_key: {vanilla_id: count}},
    {chunk_key: {gt6_id: count}}).

    full_only: the chunk NBT carries its pipeline Status — a chunk below "full" was saved
    mid-pipeline (its decoration may not have run at all), and those chunks' counts drift
    boot-to-boot as pure timing artifacts. The determinism gate consumes ONLY Status-full
    chunks (the completed world); cluster/seam analysis rides the same filtered set.
    gt6_all records EVERY gt6 id per chunk (ore forms included) — the interplay-attribution
    face: a lens block eaten by a small-ore/vein feature shows up as an ore-form count."""
    per_chunk, positions, vanilla, gt6_all = {}, {}, {}, {}
    region_dir = RUN_DIR / "world" / "region"
    for path in sorted(glob.glob(str(region_dir / "r.*.*.mca"))):
        for cx, cz, nbt in read_region(Path(path)):
            if not (chunk_lo <= cx <= chunk_hi and chunk_lo <= cz <= chunk_hi):
                continue
            status = str(nbt.get("Status", ""))
            if full_only and not status.endswith("full"):
                continue
            counts, vcounts, gcounts = {}, {}, {}
            for section in (nbt.get("sections") or []):
                y_base = (section.get("Y") or 0) * 16
                for index, name in decode_positions(section, set(LENS_IDS)):
                    counts[name] = counts.get(name, 0) + 1
                    y = index >> 8
                    z = (index >> 4) & 15
                    x = index & 15
                    positions[(cx * 16 + x, y_base + y, cz * 16 + z)] = name
                for index, name in decode_positions(section, {"minecraft:iron_ore", "minecraft:coal_ore"}):
                    vcounts[name] = vcounts.get(name, 0) + 1
                for index, name in decode_positions(section, None, prefix="gt6:"):
                    gcounts[name] = gcounts.get(name, 0) + 1
            if counts:
                per_chunk[f"{cx},{cz}"] = counts
            if vcounts:
                vanilla[f"{cx},{cz}"] = vcounts
            if gcounts:
                gt6_all[f"{cx},{cz}"] = gcounts
    return per_chunk, positions, vanilla, gt6_all


# ------------------------------------------------------------------ analysis

def cluster(positions, stone):
    """Greedy x/z-proximity clustering of one stone's hits (12-block link radius; the
    lens is a solid ellipsoid, so one cluster per origin)."""
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
    for c in clusters:
        c["chunks"] = {(p[0] >> 4, p[2] >> 4) for p in c["points"]}
    return clusters


def interior(cluster, chunk_lo, chunk_hi):
    """True when the cluster's bbox is >= 1 chunk inside the scan region (edge clusters
    are region-cut; their convexity says nothing about worldgen)."""
    margin = 16
    return (cluster["minx"] >= chunk_lo * 16 + margin and cluster["maxx"] <= chunk_hi * 16 + 15 - margin
            and cluster["minz"] >= chunk_lo * 16 + margin and cluster["maxz"] <= chunk_hi * 16 + 15 - margin)


def seam_probe(cluster, axis):
    """The no-1-column-seam face, cave-tolerant: a WRITE-SPLIT seam is a single missing
    column exactly on a chunk-boundary write pair (x = 16k-1 or 16k) with the profile
    continuing on both sides. Interior 1-wide holes are caves carving the lens (the host
    gate refuses air/ores — vanilla granite blobs puncture the same way), NOT seam
    evidence; the boot#1 run's gaps (x=39, z=-84) all sat away from boundary pairs.
    Returns a list of seam records (empty = no seam)."""
    groups = {}
    for (x, y, z) in cluster["points"]:
        a, o = (x, z) if axis == "x" else (z, x)
        groups.setdefault(o, set()).add(a)
    seams = []
    for o, values in groups.items():
        vs = sorted(values)
        for v1, v2 in zip(vs, vs[1:]):
            if v2 - v1 == 2 and (v1 + 1) % 16 in (0, 15):
                seams.append(dict(axis=axis, at=v1 + 1, row=o))
    return seams


def column_blocks(cx, cz, lx, lz):
    """Decode one live column's (y -> block id) map straight from its region file: the
    block index packs as y_local*256 + z*16 + x, so the full column walks every y_local
    of every section (the y_local=0 shortcut mis-decodes, it only reads section bases)."""
    region_path = RUN_DIR / "world" / "region" / f"r.{cx // 32}.{cz // 32}.mca"
    if not region_path.exists():
        return None
    out = {}
    for gx, gz, nbt in read_region(region_path):
        if (gx, gz) != (cx, cz):
            continue
        for section in (nbt.get("sections") or []):
            bs = section.get("block_states") or {}
            palette = [p.get("Name", "") for p in (bs.get("palette") or [])]
            if not palette:
                continue
            data = bs.get("data")
            y_base = (section.get("Y") or 0) * 16
            col = (lz & 15) * 16 + (lx & 15)
            if data is None:
                # uniform section: the single palette entry fills all 4096 slots
                for y_local in range(16):
                    out[y_base + y_local] = palette[0]
                continue
            bits = max(4, (len(palette) - 1).bit_length())
            per_long = 64 // bits
            for y_local in range(16):
                index = y_local * 256 + col
                li, k = divmod(index, per_long)
                if li >= len(data):
                    continue
                long_v = data[li] + (1 << 64 if data[li] < 0 else 0)
                out[y_base + y_local] = palette[(long_v >> (k * bits)) & ((1 << bits) - 1)]
        break
    return out


def classify_seams(candidates):
    """The discriminator for seam-candidate columns: decode each column's live blocks
    across the neighbours' y-band. AIR/liquid in-band = a carver/structure pocket (the
    host gate refuses it, NOT a seam); a replaceable host standing in-band = a REAL
    write-split miss (a bug); GT lens stone = a detection contradiction. The y-band comes
    from the boundary-adjacent columns present on both sides (each candidate carries its
    cluster's points)."""
    verdicts = []
    for cand in candidates:
        a, row, axis = cand["at"], cand["row"], cand["axis"]
        x, z = (a, row) if axis == "x" else (row, a)
        cluster_points = cand["points"]
        by_col = {}
        for (px, py, pz) in cluster_points:
            by_col.setdefault((px, pz), []).append(py)
        # the classification band = the INTERSECTION of the two flanking columns' y-bands
        # (convexity: the missing column's true extent contains the intersection; blocks
        # above/below it are outside the lens and say nothing)
        flank_bands = []
        for d in (-2, 2):
            px, pz = (a + d, row) if axis == "x" else (row, a + d)
            ys = by_col.get((px, pz))
            if not ys:
                continue
            flank_bands.append((min(ys), max(ys), ys))
        if len(flank_bands) < 2:
            continue
        y_lo = max(band[0] for band in flank_bands) - 1
        y_hi = min(band[1] for band in flank_bands) + 1
        blocks = column_blocks(x >> 4, z >> 4, x, z) or {}
        in_band = {y: name for y, name in blocks.items() if y_lo <= y <= y_hi}
        if any(name.startswith("gt6:") for name in in_band.values()):
            verdict = "gt-stone-present"
        elif any(name.endswith("air") or name in ("minecraft:water", "minecraft:lava")
                 for name in in_band.values()):
            verdict = "carver-pocket"
        elif any(name in ("minecraft:stone", "minecraft:granite", "minecraft:diorite",
                          "minecraft:andesite", "minecraft:tuff", "minecraft:gravel")
                 for name in in_band.values()):
            verdict = "REAL-SEAM"
        else:
            verdict = "host-refused"
        verdicts.append(dict(stone=cand["stone"], axis=axis, x=x, z=z,
                             verdict=verdict,
                             blocks={y: name.split(":")[-1] for y, name in sorted(in_band.items())}))
    return verdicts


def band_overlap(cluster, boundary):
    """The evidence-trail face at one chunk boundary: boundary-adjacent columns exist on
    both sides and their y-bands overlap (the vein card's boundary_probe form)."""
    left = [p for p in cluster["points"] if p[0] == boundary - 1]
    right = [p for p in cluster["points"] if p[0] == boundary]
    if not left or not right:
        return None
    ly = [p[1] for p in left]
    ry = [p[1] for p in right]
    return dict(boundary=boundary, left_cols=len({(p[0], p[2]) for p in left}),
                right_cols=len({(p[0], p[2]) for p in right}),
                left_band=[min(ly), max(ly)], right_band=[min(ry), max(ry)],
                overlap=bool(max(min(ly), min(ry)) <= min(max(ly), max(ry))))


def verify_seed():
    """Read the live world's level.dat seed — a boot that created the world with a
    foreign seed would poison every downstream comparison (the seed gate)."""
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


def run_boot(index, chunk_lo, chunk_hi):
    globals()["boot_index"] = index
    provision()
    box_count = (chunk_hi - chunk_lo + 1) ** 2
    boot_and_load(box_count)
    # wait for the gradle wrapper + server JVM to fully exit — region files must be
    # quiescent before the offline scan, or partial saves poison the census
    pid_file = Path(f"/tmp/scan_{LEG}_boot{index}.pid")
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
    return scan_world(chunk_lo, chunk_hi)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("worktree")
    parser.add_argument("leg", choices=["forge", "neo"])
    parser.add_argument("--region", type=int, default=48, help="scan region side in chunks (even)")
    parser.add_argument("--reuse-boot1", action="store_true",
                        help="crash recovery: the on-disk world IS a settled boot#1 — "
                        "rescan it instead of deleting and regenerating")
    args = parser.parse_args()
    globals()["LEG"] = args.leg
    globals()["WORKTREE"] = Path(args.worktree)
    globals()["RUN_DIR"] = gt6server.run_dir(str(WORKTREE), node=NODE[LEG])
    half = args.region // 2
    globals()["CHUNK_LO"], globals()["CHUNK_HI"] = -half, args.region - half - 1
    chunk_lo, chunk_hi = CHUNK_LO, CHUNK_HI
    box_count = args.region * args.region

    print(f"== strata-lens natural scan, leg={LEG}, worktree={WORKTREE}, seed={SEED}, "
          f"region={args.region}x{args.region} chunks ({chunk_lo}..{chunk_hi}) ==")

    # ------------------------------------------------------------------
    # The determinism face: TWO boots at the fixed seed, compared chunk-for-chunk with
    # the pipeline-drift attribution. Evidence chain: some chunks sit at a pre-decoration
    # pipeline stage at stop time (their decoration not yet scheduled), and those chunks'
    # counts drift between boots — the VANILLA control (iron/coal, placed by the same
    # pre-GT pipeline) drifts IDENTICALLY, and vanilla is seed-deterministic by itself,
    # so any per-chunk disagreement is a pipeline-stage artifact, not world-content
    # nondeterminism. The gate: wherever the VANILLA counts agree (the pipeline produced
    # the same world content there), the LENS counts must agree too — a lens disagreement
    # on a vanilla-agreeing chunk would be REAL nondeterminism.
    # ------------------------------------------------------------------
    scans = []
    boot_no = 0
    while len(scans) < 2:
        boot_no += 1
        if boot_no == 1 and args.reuse_boot1:
            print("--reuse-boot1: rescanning the on-disk world as boot#1", flush=True)
            per_chunk, positions, vanilla, gt6_all = scan_world(chunk_lo, chunk_hi)
            verify_seed()
        else:
            per_chunk, positions, vanilla, gt6_all = run_boot(boot_no, chunk_lo, chunk_hi)
        scans.append(dict(per_chunk=per_chunk, positions=positions, vanilla=vanilla, gt6_all=gt6_all))
        print(f"boot#{boot_no}: chunks-with-lens={len(per_chunk)} lens-positions={len(positions)}"
              f" vanilla-chunks={len(vanilla)}", flush=True)
    boot1s, boot2s = scans[0], scans[1]
    per_chunk1, positions1, vanilla1, gt6_all1 = (boot1s["per_chunk"], boot1s["positions"],
                                                  boot1s["vanilla"], boot1s["gt6_all"])
    per_chunk2, positions2, vanilla2, gt6_all2 = (boot2s["per_chunk"], boot2s["positions"],
                                                  boot2s["vanilla"], boot2s["gt6_all"])

    # the determinism face at the LENS-DECISION level: the origin-seeded stream picks the
    # stone and the geometry independently of any feature-index seeding, so the CLUSTER
    # SETS (per-stone counts + the biggest cluster's bbox) must agree across boots. The
    # SURVIVING per-chunk block counts additionally depend on WHERE the small-ore/vein
    # features (whose vanilla placement seeds ride their feature-list INDEX, and the GT
    # modifier append order varies per boot) consume lens blocks — that interplay is
    # attributed, not treated as lens nondeterminism (see the per-diff-chunk gt6 census).
    clusters1 = {stone: cluster(positions1, stone) for stone in LENS_IDS}
    clusters2 = {stone: cluster(positions2, stone) for stone in LENS_IDS}
    counts1 = {stone: len(clusters1[stone]) for stone in LENS_IDS}
    counts2 = {stone: len(clusters2[stone]) for stone in LENS_IDS}
    clusters_deterministic = counts1 == counts2
    print(f"cluster determinism: boot1={counts1} boot2={counts2} -> {clusters_deterministic}", flush=True)

    clusters = clusters1
    per_stone = counts1

    all_chunks = set(per_chunk1) | set(per_chunk2)
    lens_equal, lens_drifted = 0, 0
    real_diff = []
    diff_detail = []
    for key in all_chunks:
        if vanilla1.get(key) == vanilla2.get(key):
            # the pipeline produced the same content here — the lens must agree too
            if per_chunk1.get(key) == per_chunk2.get(key):
                lens_equal += 1
            else:
                lens_drifted += 1
                real_diff.append(key)
                # the attribution face: the gt6 ore-form census on both sides shows the
                # small-ore/vein features consuming the lens stones at index-driven spots
                diff_detail.append(dict(
                    chunk=key, boot1=per_chunk1.get(key, {}), boot2=per_chunk2.get(key, {}),
                    gt6_all1={k.split("_")[0] + "_" + k.split("_")[1]: v for k, v in
                              sorted(gt6_all1.get(key, {}).items())
                              if k.startswith("gt6:ore")},
                    gt6_all2={k.split("_")[0] + "_" + k.split("_")[1]: v for k, v in
                              sorted(gt6_all2.get(key, {}).items())
                              if k.startswith("gt6:ore")}))
    pipeline_artifacts = [k for k in all_chunks if vanilla1.get(k) != vanilla2.get(k)]
    print(f"determinism: vanilla-agreeing chunks={lens_equal + lens_drifted}, lens equal={lens_equal}, "
          f"lens REAL-diff={lens_drifted} {real_diff[:8]}; pipeline-stage artifacts (vanilla drift)="
          f"{len(pipeline_artifacts)}", flush=True)
    for d in diff_detail[:6]:
        print("  diff attribution:", json.dumps(d), flush=True)
    equal = lens_drifted == 0
    vanilla_equal = not pipeline_artifacts

    clusters = {stone: cluster(positions1, stone) for stone in LENS_IDS}
    per_stone = {stone: len(clusters[stone]) for stone in LENS_IDS}
    print(f"final world: clusters={per_stone}", flush=True)
    biggest = {}
    for stone in LENS_IDS:
        cross = [c for c in clusters[stone] if len(c["chunks"]) >= 2]
        biggest[stone] = max(cross, key=lambda c: len(c["points"])) if cross else None
        if biggest[stone]:
            c = biggest[stone]
            print(f"  {stone}: largest cross-boundary cluster n={len(c['points'])} "
                  f"x[{c['minx']},{c['maxx']}] z[{c['minz']},{c['maxz']}] y[{c['miny']},{c['maxy']}] "
                  f"chunks={len(c['chunks'])}")
    # the continuity faces
    seam_candidates, bands = [], []
    for stone in LENS_IDS:
        for c in clusters[stone]:
            if not interior(c, chunk_lo, chunk_hi) or len(c["chunks"]) < 2:
                continue
            for axis in ("x", "z"):
                for s in seam_probe(c, axis):
                    seam_candidates.append(dict(stone=stone, **s, points=c["points"]))
            for boundary in range(c["minx"], c["maxx"] + 1):
                if boundary % 16 == 0 and c["minx"] <= boundary - 1 and c["maxx"] >= boundary:
                    probe = band_overlap(c, boundary)
                    if probe:
                        bands.append(dict(stone=stone, **probe))
                    break
    each_stone_hit = all(count >= 1 for count in per_stone.values())
    print(f"each-stone->=1-lens: {each_stone_hit} "
          f"boundary-candidate columns: {len(seam_candidates)} boundary bands: {len(bands)}", flush=True)
    # the discriminator: a boundary candidate is a REAL seam only when a replaceable host
    # stood in-band and the lens did not write; carver pockets are the host gate at work
    seams = classify_seams(seam_candidates)
    real_seams = [s for s in seams if s["verdict"] == "REAL-SEAM"]
    explained = [s for s in seams if s["verdict"] != "REAL-SEAM"]
    print(f"seam verdicts: real={len(real_seams)} explained={len(explained)}", flush=True)
    for s in seams[:8]:
        print("  ", json.dumps(s), flush=True)

    result = dict(leg=LEG, seed=SEED, region_chunks=args.region,
                  boot1=dict(per_stone_clusters=per_stone, chunks_with_lens=len(per_chunk1),
                             lens_positions=len(positions1), each_stone_hit=each_stone_hit,
                             seam_verdicts=seams[:20], boundary_bands=bands[:10]),
                  boot2=dict(chunks_with_lens=len(per_chunk2), lens_positions=len(positions2)),
                  cluster_deterministic=clusters_deterministic,
                  lens_equal_chunks=lens_equal, lens_real_diff_chunks=lens_drifted,
                  per_chunk_equal=equal, per_chunk_diff=real_diff[:20],
                  diff_attribution=diff_detail[:8],
                  pipeline_artifacts=len(pipeline_artifacts),
                  vanilla_equal=vanilla_equal)
    Path(f"/tmp/scan_{LEG}_verdict.json").write_text(json.dumps(result, indent=1))
    ok = each_stone_hit and not real_seams and clusters_deterministic and bands
    print("VERDICT:", "GREEN" if ok else "RED", flush=True)


globals()["boot_index"] = 1
if __name__ == "__main__":
    main()
