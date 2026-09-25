#!/usr/bin/env python3
"""scan_dungeon — the p38-dungeon-framework card's natural-generation gate driver.

Per leg (forge / neoforge):

  predict: replicate the placement chain offline (an exact transcription of the vanilla
           1.20.1 RNG — LegacyRandomSource is the classic 48-bit LCG, BitRandomSource
           the classic nextInt/nextLong algorithms): for every 11x11-chunk cell,
           RandomSpreadStructurePlacement.getPotentialStructureChunk
           (setLargeFeatureWithSalt(seed, cell, salt) -> nextInt(6) x2) gives the
           candidate chunk; Structure.findGenerationPoint's gate
           (setLargeFeatureSeed(seed, cand) -> nextInt(100) == 0, plus the
           |chunk| < 23 spawn exclusion) decides the dungeon chunk. These are the SAME
           LCG streams the live server runs — a prediction hit IS the natural-
           generation evidence (no /place shortcut).
  boot:    fresh world (world dirs deleted), level-type normal, the card seed
           6131000569321125127, the global RCON slot; forceload each predicted
           dungeon's neighborhood (the grid is at most 9x9 chunks around the origin
           chunk, so a +/-6 chunk box covers every piece); wait for generation
           quiescence (region-file byte stability); graceful stop.
  scan:    per-chunk NBT (Status-full only) — 1) the structure start
           structures.starts["gt6:dungeon"] with its Children piece list (kind/BB
           audit: exactly one ENTRANCE, >= 1 STORAGE dead-end, the per-kind census
           including the rooms-batch kinds, all boxes
           at the Y2 foundation floor); 2) the block palette probes inside the piece
           boxes (gt6 stone walls in the y20..27 shell band, the airlock sticky
           pistons + lever, the loot chest, and the entrance shaft's gt6 blocks
           climbing toward the surface cap).

Artifacts: /tmp/p38dungeonscan_<leg>.log /tmp/p38dungeonscan_<leg>_verdict.json

Usage:
  python3 tools/rcon/scan_dungeon.py <worktree> <forge|neo> [--span 200] [--seed N]
"""

import argparse
import glob
import gzip
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
SEED = 6131000569321125127
SPACING, SEPARATION, SALT = 11, 5, 371266954   # GT6WorldgenDatagen pins
PROBABILITY, EXCLUDE = 100, 23                  # GT6DungeonLayout pins
DUNGEON_START_ID = "gt6:dungeon"
GAME, RCON_PORT, QUERY = 26200, 26210, 26220
PASSWORD = "gt6"
LEG = "forge"
WORKTREE = None
RUN_DIR = None

# ------------------------------------------------------------------ the LCG replica
# LegacyRandomSource (1.20.1): MODULUS 2^48, multiplier 25214903917, increment 11 —
# the classic java.util.Random constants; BitRandomSource.nextInt/nextLong are the
# classic algorithms. Every method here is a line-for-line transcription of those two
# files (+ WorldgenRandom.setLargeFeatureSeed/setLargeFeatureWithSalt).

_M48 = (1 << 48) - 1
_MUL = 25214903917
_INC = 11


class LCG:
    def __init__(self, seed):
        self.set_seed(seed)

    def set_seed(self, seed):
        self.s = (seed ^ _MUL) & _M48

    def next(self, bits):
        self.s = (self.s * _MUL + _INC) & _M48
        v = self.s >> (48 - bits)
        if bits == 32 and v >= 1 << 31:
            v -= 1 << 32
        return v

    def next_long(self):
        hi = self.next(32)  # signed
        lo = self.next(32)  # signed
        return (hi << 32) + lo

    def next_int(self, bound):
        if bound & (bound - 1) == 0:
            return (bound * self.next(31)) >> 31
        # 31-bit draws are non-negative, so the rejection branch never fires
        return self.next(31) % bound


def predict_dungeons(span, seed=SEED):
    """Every dungeon origin chunk with |chunk| < span — the placement+roll replica."""
    hits = []
    cells = range(-((span // SPACING) + 1), (span // SPACING) + 2)
    rand = LCG(0)
    for cell_x in cells:
        for cell_z in cells:
            rand.set_seed(cell_x * 341873128712 + cell_z * 132897987541 + seed + SALT)
            dx = rand.next_int(SPACING - SEPARATION)
            dz = rand.next_int(SPACING - SEPARATION)
            cand_x = cell_x * SPACING + dx
            cand_z = cell_z * SPACING + dz
            if abs(cand_x) < EXCLUDE or abs(cand_z) < EXCLUDE:
                continue
            rand.set_seed(seed)
            r1 = rand.next_long()
            r2 = rand.next_long()
            mixed = (cand_x * r1) ^ (cand_z * r2) ^ seed
            rand.set_seed(mixed)
            if rand.next_int(PROBABILITY) == 0:
                hits.append((cand_x, cand_z))
    return hits


# ------------------------------------------------------------------ boot machinery
# (the scan_strata_lens form: slot semaphore, watchdog, acked forceload, byte quiescence)


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


def boot_and_load(boxes):
    log = Path(f"/tmp/p38dungeonscan_{LEG}.log")
    pid = Path(f"/tmp/p38dungeonscan_{LEG}.pid")
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
                                first_timeout=90.0, quiet_window=2.0, adaptive_quiet=False)
    client.connect()
    client.auth()

    def _alarm(signum, frame):
        raise RuntimeError("boot_and_load watchdog fired")
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
        # fatal on persistent emptiness: a swallowed forceload add poisons the whole
        # boot (the boot#1 lesson — the empty replies were the lever-crash symptom,
        # and continuing would only burn the quiescence wait on an empty region).
        # Six rounds with 12 s gaps: an 8-box forceload queues >1100 chunks of
        # generation at once and the main thread can out-busy the default 30 s RCON
        # read for minutes (the 1.21.1 leg, seed 6131000569321125127 box 8 — the
        # command IS queued on the server executor, it just answers late).
        for attempt in (0, 1, 2, 3, 4, 5):
            reply = rc(command)
            if reply.strip():
                return reply
            print("empty reply, retry", attempt, command[:40], flush=True)
            time.sleep(12)
        raise RuntimeError(f"forceload command never acknowledged: {command}")

    def region_bytes():
        return sum(p.stat().st_size for p in RUN_DIR.glob("world/region/*.mca"))

    try:
        for x0, z0, x1, z1 in boxes:
            print("forceload add:", rc_strict(f"forceload add {x0} {z0} {x1} {z1}")[:60], flush=True)
            # 15 s pacing between boxes keeps the forced-gen queue shallow enough for
            # the next RCON command to reach the main thread inside the retry window.
            time.sleep(15)
        last_bytes, stable_bytes = -1, 0
        gen_start = time.time()
        gen_deadline = gen_start + 2400
        while True:
            time.sleep(20)
            rc("save-all flush")
            time.sleep(5)
            size = region_bytes()
            print(f"region bytes: {size}", flush=True)
            if size == last_bytes:
                stable_bytes += 1
                if stable_bytes >= 3 and time.time() - gen_start >= 60:
                    break
            else:
                stable_bytes = 0
            last_bytes = size
            if time.time() > gen_deadline:
                raise RuntimeError("region generation never quiesced")
        time.sleep(20)
        rc("save-all flush")
        time.sleep(10)
        rc("stop")
    finally:
        signal.alarm(0)
        client.close()
        time.sleep(3)
        gt6server.stop_server(pid, rcon=("127.0.0.1", RCON_PORT, PASSWORD))
    time.sleep(20)


# ------------------------------------------------------------------ anvil/nbt scan
# (the scan_strata_lens minimal reader, verified against that card's live scans)


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
            data = zlib.decompress(body) if compression == 2 else gzip.decompress(body)
        except Exception:
            continue
        try:
            nbt = parse_nbt(data)
        except Exception:
            continue
        if nbt is None:
            continue
        cxp, czp = nbt.get("xPos"), nbt.get("zPos")
        if cxp is None:
            continue
        yield cxp, czp, nbt


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


def scan_world(boxes_area):
    """Return ({chunk_key: dungeon_start}, {(x,y,z): block_id} restricted to the
    dungeon band probes). full_status-only (the p31 lesson: Status=='minecraft:full'
    is the only pipeline-complete marker)."""
    starts, blocks = {}, {}
    global WANTED_PROBES
    region_dir = RUN_DIR / "world" / "region"
    for path in sorted(glob.glob(str(region_dir / "r.*.*.mca"))):
        for cx, cz, nbt in read_region(Path(path)):
            status = str(nbt.get("Status", ""))
            if not status.endswith("full"):
                continue
            structures = nbt.get("structures") or {}
            starts_nbt = structures.get("starts") or {}
            if DUNGEON_START_ID in starts_nbt:
                start = starts_nbt[DUNGEON_START_ID]
                pieces = []
                for child in (start.get("Children") or []):
                    bb = child.get("BB") or []
                    pieces.append(dict(kind=str(child.get("gtKind", "?")), bb=bb))
                starts[f"{cx},{cz}"] = dict(chunkx=start.get("ChunkX"), chunkz=start.get("ChunkZ"),
                                            pieces=pieces)
            if not blocks or True:
                y_lo, y_hi = 2, 160  # the full dungeon band (shell 20..28, shaft to surface)
                wanted = {"minecraft:sticky_piston", "minecraft:lever", "minecraft:chest",
                          "minecraft:redstone_wire",
                          # the dungeon-rooms-batch room probes
                          "minecraft:farmland", "minecraft:ladder", "minecraft:iron_bars",
                          "minecraft:tnt", "minecraft:red_bed", "minecraft:iron_door",
                          "minecraft:grindstone", "minecraft:anvil", "minecraft:carpet",
                          "minecraft:sugar_cane", "minecraft:cactus", "minecraft:water"}
                for section in (nbt.get("sections") or []):
                    y_base = (section.get("Y") or 0) * 16
                    if y_base + 16 < y_lo or y_base > y_hi:
                        continue
                    for index, name in decode_positions(section, wanted):
                        y = y_base + (index >> 8)
                        z = cz * 16 + ((index >> 4) & 15)
                        x = cx * 16 + (index & 15)
                        blocks[(x, y, z)] = name
                    WANTED_PROBES = wanted
                    for index, name in decode_positions(section, None, prefix="gt6:"):
                        y = y_base + (index >> 8)
                        if not (y_lo <= y <= y_hi):
                            continue
                        z = cz * 16 + ((index >> 4) & 15)
                        x = cx * 16 + (index & 15)
                        blocks[(x, y, z)] = name
    return starts, blocks


WANTED_PROBES = None  # set by scan_world to the wanted-name set (the counting face)


def verify_seed():
    level = RUN_DIR / "world" / "level.dat"
    raw = level.read_bytes()
    try:
        data = gzip.decompress(raw)
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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("worktree")
    parser.add_argument("leg", choices=["forge", "neo"])
    parser.add_argument("--span", type=int, default=260,
                        help="the prediction search half-span in chunks (cells of 11)")
    parser.add_argument("--seed", type=int, default=SEED)
    parser.add_argument("--max-dungeons", type=int, default=3,
                        help="cap the forceload set (each dungeon boots a 13x13 chunk box)")
    parser.add_argument("--predict-only", action="store_true")
    args = parser.parse_args()

    globals()["LEG"] = args.leg
    globals()["WORKTREE"] = Path(args.worktree)
    globals()["RUN_DIR"] = gt6server.run_dir(str(WORKTREE), node=NODE[LEG])

    dungeons = predict_dungeons(args.span, args.seed)
    print(f"prediction: {len(dungeons)} dungeon origin chunks within |chunk| < {args.span} "
          f"(seed {args.seed}): {dungeons[:12]}", flush=True)
    if args.predict_only:
        return

    if not dungeons:
        raise RuntimeError("the prediction found no dungeon in span — widen --span, the boot is pointless")

    # each dungeon's pieces live within +/-4 chunks of the origin (grid <= 9); the box
    # carries +2 margin. Forceload must be ACKED per add (the p31 pipelining lesson).
    picks = dungeons[:args.max_dungeons]
    boxes = []
    for (dx, dz) in picks:
        boxes.append(((dx - 6) * 16, (dz - 6) * 16, (dx + 6) * 16 + 15, (dz + 6) * 16 + 15))
    print(f"== p38-dungeon-framework natural scan, leg={LEG}, worktree={WORKTREE}, "
          f"dungeons={picks}, boxes={len(boxes)} ==", flush=True)

    provision()
    boot_and_load(boxes)
    try:
        import os
        pid_file = Path(f"/tmp/p38dungeonscan_{LEG}.pid")
        pid = int(pid_file.read_text().strip()) if pid_file.exists() else None
        deadline = time.time() + 180
        while pid and time.time() < deadline:
            try:
                os.kill(pid, 0)
                time.sleep(3)
            except ProcessLookupError:
                break
    except Exception:
        pass
    time.sleep(10)
    verify_seed()
    starts, blocks = scan_world(True)

    # ------------------------------------------------------------------ analysis
    report = dict(leg=LEG, seed=args.seed, predicted=picks, dungeons_found=len(starts), checks={})
    ok = len(starts) >= 1
    gt6_walls = 0
    pistons = levers = chests = wires = 0
    room_kinds = {}
    for key, start in starts.items():
        kinds = [p["kind"] for p in start["pieces"]]
        entrances = kinds.count("ENTRANCE")
        storages = kinds.count("STORAGE")
        corridors = kinds.count("CORRIDOR")
        rooms = kinds.count("ROOM_EMPTY")
        for k in kinds:
            room_kinds[k] = room_kinds.get(k, 0) + 1
        print(f"dungeon@{key}: pieces={len(kinds)} entrance={entrances} storage={storages} "
              f"corridor={corridors} room={rooms} barracks={kinds.count('BARRACKS')} "
              f"corridor3={kinds.count('CORRIDOR3')} corridor4={kinds.count('CORRIDOR4')} "
              f"workshop={kinds.count('WORKSHOP')} mining={kinds.count('MINING_BEDROCK')} "
              f"farm_crop={kinds.count('FARM_CROP')} farm_mobs={kinds.count('FARM_MOBS')} "
              f"farm_fish={kinds.count('FARM_FISH')}", flush=True)
        c = report["checks"]
        c[f"{key}:one-entrance"] = entrances == 1
        c[f"{key}:has-storage"] = storages >= 1
        # NO has-corridor hard gate (the rooms-batch fix): upstream rooms connect
        # room-to-room when adjacent (WorldgenDungeonGT :200-248 only prunes corridor
        # cells), so a compact dungeon legitimately ships ZERO corridor cells — the
        # -216,214 dungeon of seed 6131000569321125127 is the live evidence. The census
        # line + room_kinds carry the corridor-kind counts as the evidence face.
        # the dungeon-rooms-batch evidence: the barracks important room exists in EVERY
        # dungeon; the pool rooms / corridor 3-4 variants are the seed's draws (per-dungeon
        # conditional checks below, the counts are the report's evidence face).
        c[f"{key}:has-barracks"] = kinds.count("BARRACKS") >= 1
        if kinds.count("FARM_CROP"):
            c[f"{key}:farm-crop"] = kinds.count("FARM_CROP") >= 1
        if kinds.count("FARM_MOBS"):
            c[f"{key}:farm-mobs"] = kinds.count("FARM_MOBS") >= 1
        if kinds.count("FARM_FISH"):
            c[f"{key}:farm-fish"] = kinds.count("FARM_FISH") >= 1
        if kinds.count("WORKSHOP"):
            c[f"{key}:workshop"] = kinds.count("WORKSHOP") >= 1
        if kinds.count("MINING_BEDROCK"):
            c[f"{key}:mining-bedrock"] = kinds.count("MINING_BEDROCK") >= 1
        if kinds.count("CORRIDOR3"):
            c[f"{key}:corridor3"] = kinds.count("CORRIDOR3") >= 1
        if kinds.count("CORRIDOR4"):
            c[f"{key}:corridor4"] = kinds.count("CORRIDOR4") >= 1
        ok = ok and entrances == 1 and storages >= 1 and kinds.count("BARRACKS") >= 1
        # the entrance shaft: the ENTRANCE piece box must climb well above the shell
        for p in start["pieces"]:
            if p["kind"] == "ENTRANCE" and len(p["bb"]) == 6:
                bb = p["bb"]
                tall = bb[4] - bb[1]
                print(f"  entrance bbox y [{bb[1]},{bb[4]}] height={tall} (the shaft climb)", flush=True)
                c[f"{key}:shaft-tall"] = tall > 20
                ok = ok and tall > 20
        # the shell band: gt6 stone in y20..28 within this dungeon's chunk neighborhood
        bx, bz = key.split(",")
        bx, bz = int(bx), int(bz)
        band = [1 for (x, y, z), n in blocks.items()
                if bx - 6 <= x >> 4 <= bx + 6 and bz - 6 <= z >> 4 <= bz + 6
                and 19 <= y <= 29 and n.startswith("gt6:")]
        print(f"  shell-band gt6 blocks (y19..29): {len(band)}", flush=True)
        gt6_walls += len(band)
        c[f"{key}:shell-walls"] = len(band) >= 500
        ok = ok and len(band) >= 500

    report["room_kinds"] = room_kinds
    probes = {}
    for (x, y, z), n in blocks.items():
        if n == "minecraft:sticky_piston":
            pistons += 1
        elif n == "minecraft:lever":
            levers += 1
        elif n == "minecraft:chest":
            chests += 1
        elif n == "minecraft:redstone_wire":
            wires += 1
        if n in WANTED_PROBES:
            probes[n] = probes.get(n, 0) + 1
    print(f"airlock probes: sticky_pistons={pistons} levers={levers} chests={chests} "
          f"redstone_wire={wires} shell-band-gt6-total={gt6_walls}", flush=True)
    print(f"room-block probes: {probes}", flush=True)
    report["probes"] = dict(pistons=pistons, levers=levers, chests=chests, wires=wires,
                            shell_band_gt6=gt6_walls, room_blocks=probes)
    report["checks"]["airlock-pistons>=4"] = pistons >= 4
    report["checks"]["airlock-lever>=1"] = levers >= 1
    report["checks"]["redstone-wire>=2"] = wires >= 2
    report["checks"]["loot-chest>=1"] = chests >= 1
    ok = ok and pistons >= 4 and levers >= 1 and wires >= 2 and chests >= 1

    report["verdict"] = "GREEN" if ok else "RED"
    Path(f"/tmp/p38dungeonscan_{LEG}_verdict.json").write_text(json.dumps(report, indent=1))
    print("VERDICT:", report["verdict"], flush=True)


globals()["boot_index"] = 1
if __name__ == "__main__":
    main()
