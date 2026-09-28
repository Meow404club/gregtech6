#!/usr/bin/env python3
"""scan_negcoord_census — issue #31's four-quadrant natural-generation census.

The r6-31b verdict driver. A scan-class driver committed per the tools/rcon
README (same shelf as scan_strata_lens.py, whose boot/scan machinery this
reuses verbatim via import — ONE boot, no determinism face: the static audit
(r6-31) found no sign-asymmetric mechanism in the ported worldgen, so a
GT-wide quadrant collapse would be a live-world fact about current main,
which is exactly what this card must rule on).

Per leg (default forge; a single leg suffices to rule):

  boot:   fresh world (world dirs deleted), the p31 seed 6131000569321125127,
          the symmetric EVEN chunk domain chunks -N..N-1 (chunks never straddle
          block axis 0, so the four quadrants are exactly equal areas —
          region 48 = blocks -384..383 = 384x384 blocks per quadrant);
          forceload + generation quiescence + graceful stop, all skeleton code.
  scan:   Status=='minecraft:full' chunks ONLY (the p31 W1 iron rule); every
          gt6:* block counted per chunk (the gcounts channel) + the vanilla
          iron/coal control, exactly the p31 census channels.
  census: per quadrant (+x+z / -x+z / +x-z / -x-z): gt6 total, chunks-with-gt6,
          distinct ids, and the non-ore (stone/lens family) slice — the
          large-vein host face.
  verdict: GREEN when all four quadrants are populated at the same order of
          magnitude with a live vanilla control — H1 (issue #31 NOT
          reproducible on current main; environment explanation: reporter's
          map/build predates the p26-p32 worldgen, plus the design note that
          unmapped hosts — granite/diorite/andesite/tuff — are skipped by the
          large-vein feature BY DESIGN, GT6LargeVeinFeature.java:57-58, so a
          pure-tuff region shows no large veins regardless of sign).
          RED when a quadrant is empty, or collapsed >= 10x vs the smallest
          populated peer — H2 real regression; the per-quadrant missing-id
          list (ids present in some quadrant but absent here) is the
          localization lead.

Artifacts: /tmp/scan_<leg>_boot<N>.log/.pid (p31-era logs preserved as *.p31),
           /tmp/scan_<leg>_negcoord_census.json

Usage:
  python3 tools/rcon/scan_negcoord_census.py <worktree> <forge|neo> [--region 48]
  python3 tools/rcon/scan_negcoord_census.py --selftest
"""

import argparse
import json
import shutil
import sys
import time
from pathlib import Path

_REPO = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_REPO / "tools" / "rcon"))

import scan_strata_lens as S  # noqa: E402  (the boot/scan machinery, reused verbatim)

QUADRANTS = ("+x+z", "-x+z", "+x-z", "-x-z")


def quadrant_of(cx, cz):
    return ("-x" if cx < 0 else "+x") + ("-z" if cz < 0 else "+z")


def census(gt6_all, vanilla):
    quads = {q: dict(gt6_total=0, gt6_chunks=0, gt6_ids={}, stone_ids={}, vanilla={})
             for q in QUADRANTS}
    for key, counts in gt6_all.items():
        cx, cz = (int(v) for v in key.split(","))
        cell = quads[quadrant_of(cx, cz)]
        cell["gt6_chunks"] += 1
        for name, n in counts.items():
            cell["gt6_total"] += n
            cell["gt6_ids"][name] = cell["gt6_ids"].get(name, 0) + n
            if not name.startswith("gt6:ore"):  # stone/lens face = the large-vein host material
                cell["stone_ids"][name] = cell["stone_ids"].get(name, 0) + n
    for key, counts in vanilla.items():
        cx, cz = (int(v) for v in key.split(","))
        cell = quads[quadrant_of(cx, cz)]
        for name, n in counts.items():
            cell["vanilla"][name] = cell["vanilla"].get(name, 0) + n
    return quads


def verdict(quads):
    totals = {q: quads[q]["gt6_total"] for q in QUADRANTS}
    stotals = {q: sum(quads[q]["stone_ids"].values()) for q in QUADRANTS}
    vtotals = {q: sum(quads[q]["vanilla"].values()) for q in QUADRANTS}
    all_ids = set()
    for cell in quads.values():
        all_ids |= set(cell["gt6_ids"])
    missing = {}
    for q in QUADRANTS:
        absent = sorted(all_ids - set(quads[q]["gt6_ids"]),
                        key=lambda i: -max(quads[o]["gt6_ids"].get(i, 0) for o in QUADRANTS))
        if absent:
            missing[q] = absent[:24]
    populated_min = min([t for t in totals.values() if t > 0] or [0])
    empty = [q for q, t in totals.items() if t == 0]
    collapsed = [q for q, t in totals.items() if 0 < t < populated_min / 10]
    control_alive = all(v > 0 for v in vtotals.values())
    green = not empty and not collapsed and control_alive
    return dict(green=green, gt6_totals=totals, stone_totals=stotals, vanilla_totals=vtotals,
                empty_quadrants=empty, collapsed_quadrants=collapsed,
                control_alive=control_alive, missing_ids=missing)


def _selftest():
    # the axis check: chunk keys are "cx,cz"; a sign flip here poisons the whole verdict
    gt6 = {"3,5": {"gt6:ore_small_copper": 7}, "-3,5": {"gt6:marble": 2},
           "3,-5": {"gt6:basalt": 1}, "-3,-5": {"gt6:ore_vein_copper": 4}}
    van = {"3,5": {"minecraft:iron_ore": 9}, "-3,5": {"minecraft:iron_ore": 1},
           "3,-5": {"minecraft:coal_ore": 2}, "-3,-5": {"minecraft:coal_ore": 8}}
    quads = census(gt6, van)
    assert quads["+x+z"]["gt6_total"] == 7 and quads["-x+z"]["gt6_total"] == 2
    assert quads["+x-z"]["gt6_total"] == 1 and quads["-x-z"]["gt6_total"] == 4
    assert quads["+x+z"]["gt6_chunks"] == 1
    assert quads["+x+z"]["vanilla"] == {"minecraft:iron_ore": 9}
    assert sum(quads["+x+z"]["stone_ids"].values()) == 0
    assert sum(quads["-x+z"]["stone_ids"].values()) == 2
    v = verdict(quads)
    assert v["green"] and v["control_alive"]
    assert not v["empty_quadrants"] and not v["collapsed_quadrants"]
    # +x+z lacks three ids; ranked by the best count elsewhere (vein 4 > marble 2 > basalt 1)
    assert v["missing_ids"]["+x+z"] == ["gt6:ore_vein_copper", "gt6:marble", "gt6:basalt"]
    print("selftest OK")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("worktree", nargs="?")
    parser.add_argument("leg", choices=["forge", "neo"], nargs="?")
    parser.add_argument("--region", type=int, default=48,
                        help="scan region side in chunks (EVEN — quadrant block symmetry)")
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    if args.selftest:
        _selftest()
        return

    S.LEG = args.leg
    S.WORKTREE = Path(args.worktree)
    S.RUN_DIR = S.gt6server.run_dir(str(S.WORKTREE), node=S.NODE[S.LEG])
    half = args.region // 2
    S.CHUNK_LO, S.CHUNK_HI = -half, args.region - half - 1

    # the skeleton writes the same /tmp log/pid names the p31 evidence used — preserve those
    for suffix in (".log", ".pid"):
        src = Path(f"/tmp/scan_{S.LEG}_boot1{suffix}")
        if src.exists():
            shutil.move(str(src), str(src) + ".p31")

    print(f"== negcoord census, leg={S.LEG}, seed={S.SEED}, "
          f"region={args.region}x{args.region} chunks ({S.CHUNK_LO}..{S.CHUNK_HI}) =="
          f" blocks {S.CHUNK_LO * 16}..{S.CHUNK_HI * 16 + 15}", flush=True)
    t0 = time.time()
    _per_chunk, _positions, vanilla, gt6_all = S.run_boot(1, S.CHUNK_LO, S.CHUNK_HI)

    quads = census(gt6_all, vanilla)
    v = verdict(quads)
    for q in QUADRANTS:
        cell = quads[q]
        top = sorted(cell["gt6_ids"].items(), key=lambda kv: -kv[1])[:10]
        print(f"{q}: gt6_total={cell['gt6_total']} chunks_with_gt6={cell['gt6_chunks']} "
              f"stone_total={sum(cell['stone_ids'].values())} vanilla={cell['vanilla']}", flush=True)
        print(f"   top: {top}", flush=True)
    result = dict(leg=S.LEG, seed=S.SEED, region_chunks=args.region,
                  block_domain=[S.CHUNK_LO * 16, S.CHUNK_HI * 16 + 15],
                  elapsed_s=round(time.time() - t0), verdict=v, quadrants=quads)
    out = Path(f"/tmp/scan_{S.LEG}_negcoord_census.json")
    out.write_text(json.dumps(result, indent=1))
    print(f"verdict JSON: {out}", flush=True)
    print("VERDICT:", "GREEN (H1: not reproducible on this build — environment explanation stands)"
          if v["green"] else
          f"RED (H2: quadrant regression — empty={v['empty_quadrants']} "
          f"collapsed={v['collapsed_quadrants']} missing={v['missing_ids']})", flush=True)


if __name__ == "__main__":
    main()
