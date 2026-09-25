#!/usr/bin/env python3
"""dryer-family — the Dryer ladder acceptance chain (declarative framework).

Chain semantics (task dryer-family ACCEPTANCE, arm ⑥ re-landed 2026-09-04 per
the card's own pool debt "pour DRYING 行时…改 RCON 链空图臂为产出行"): four dryers
placed (the placed default FACING = north, no placer through the command), then per
tier the row geometry and the PRODUCTION verdict — the DRYING map is no longer empty
(the P14 loop-closure card poured the water→distilled_water row), so the old
declared-empty arms (progress=0/0) are stale on main and were rewritten:

  place → fluid fill east/south minecraft:water (the rotated SBIT_B|SBIT_L tank-in mask:
          relative back = world south, relative left = world east — ACCEPTED) →
  fluid fill down / up (the negative arms: bottom is the SBIT_D ENERGY face, top is the
          SBIT_U OUT-only face — REJECTED, 0 filled is a legitimate verdict) →
  fluid draw up (the OUT mask is open but the output bank is empty and the input tank is
          fill-only — REJECTED) →
  fluid stat (the live census: in[0]=1000 L of water — the constructed 1000 mB default tank exactly filled + the six-side face lists, the :511
          isEnergyAcceptingFrom probe pinned to energyIn=down) →
  input 1 (the minimal stub feed arm: bricks, the upstream frame 'B' column) →
  inject 40 (HU packets through doInject at the row's mInputMax — the HU carrier live;
          the poured DRYING map starts: progress moves by the exact driven arithmetic
          40 ticks at mInputMax — 512/2048, 2048/8192, 8192/32768, 32768/73728) →
  check (the tier columns: parallel 8/16/32/64 + parallelDuration + recIn 32/128/512/2048,
          data=0 = the gt6:dryer menu opens since machine-fluid-gui bound the row
          supplier (the old data=-2 menu-less marker is gone) — t4 pins data=-1 instead:
          its parallel-64 batch drains the whole 1000 L input tank inside the two driven
          injects, so the post-reset re-find is DID_NOT_FIND and the dial rests on the
          GTOvenMenu -1 arm (mMaxProgress==0), which still proves the menu constructs;
          after the driven loop the
          batch resets on the first starved tick: progress=0/<maxProgress> (t4: 0/0, its
          batches complete inside the driven injects and drain the tank), active=false;
          outputs=[] is the ITEM slot list — the DistW lands in the output TANK).

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox cleanup
restores the sites between passes.

Run:  python3 tools/rcon/chains/dryer_family.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# The four dryer sites, >=16 blocks apart, clear of the P13/P14 chains' sites. The placed
# default facing is north (place() sets defaultBlockState, no placer), so the rotated
# tank-in faces are world east (relative left) + world south (relative back) on all four.
T1 = gt6world.Site(100, 64, 100, dx=1, dy=2, dz=1)
T2 = gt6world.Site(116, 64, 100, dx=1, dy=2, dz=1)
T3 = gt6world.Site(132, 64, 100, dx=1, dy=2, dz=1)
T4 = gt6world.Site(148, 64, 100, dx=1, dy=2, dz=1)

# (literal, pos, parallel, recIn, maxIn, injectProgress, checkReset, dataDial) — the
# tier columns the check report must pin; maxIn doubles as the inject packet size (the
# size-then-pos branch of the inject tree needs the explicit size before the
# coordinate). The progress columns are the poured-DRYING-map production arithmetic
# observed on main (2026-09-04, deterministic driven ticks × mInputMax; identical on
# both nodes — the recipe row and the HU arithmetic are shared code). The check arm
# pins the reset prefix "progress=0/" (race-free: the starved reset lands 0/<max> or
# 0/0 depending on whether the input tank emptied first — both mean the batch never
# completed). The last column is the post-reset menu-dial arm (GTMachineCommand check
# data=): t1-t3 keep water (80/160/320 L per batch off a 1000 L tank), so the starved
# reset leaves the recipe-aware maxProgress on the dial → data=0. t4 (parallel 64)
# burns through the whole 1000 L inside its two driven injects (batch 1 = 64 ops ×
# 10 L completes in inject 1 at progress 32768/73728, batch 2 finishes the tank and
# completes in inject 2 — 800 L DistW out, in[0]=0; live probe /tmp/co_probe_t4.log,
# 2026-09-05), so the post-reset re-find is DID_NOT_FIND and the dial rests on the
# GTOvenMenu -1 arm (:285, mMaxProgress==0) — data=-1, stable across real ticks.
# data=-1 still proves the gt6:dryer menu CONSTRUCTS (the menu-less carrier reads -2);
# found by the closeout dual-node gate 2026-09-05.
TIERS = [
    ("dryer",    T1, "parallel=8",  "recIn=32",   64,   "used=40 progress=512/2048",    "progress=0/", "data=0"),
    ("dryer_t2", T2, "parallel=16", "recIn=128",  256,  "used=40 progress=2048/8192",   "progress=0/", "data=0"),
    ("dryer_t3", T3, "parallel=32", "recIn=512",  1024, "used=40 progress=8192/32768",  "progress=0/", "data=0"),
    ("dryer_t4", T4, "parallel=64", "recIn=2048", 4096, "used=40 progress=32768/73728", "progress=0/", "data=-1"),
]

steps = []

# ------------------------------------------- the four tiers, one protocol each
for literal, pos, parallel, recin, maxin, inject_progress, max_progress, data_dial in TIERS:
    steps += [
        phase(f"{literal}: place, the rotated tank masks, the stub feed, HU, the production verdict"),
        Step(f"gt6machine {literal} place {F(pos)}", expect=f"GT6 {literal} placed"),
        # the positive arms — relative back (world south) + relative left (world east)
        Step(f"gt6machine {literal} fluid fill east minecraft:water 700 {F(pos)}",
             expect="filled 700/700 L of minecraft:water (ACCEPTED)"),
        Step(f"gt6machine {literal} fluid fill south minecraft:water 300 {F(pos)}",
             expect="filled 300/300 L of minecraft:water (ACCEPTED)"),
        # the negative arms — the bottom (the SBIT_D energy face) and the top (OUT-only)
        Step(f"gt6machine {literal} fluid fill down minecraft:water 100 {F(pos)}",
             expect="filled 0/100 L of minecraft:water (REJECTED)"),
        Step(f"gt6machine {literal} fluid fill up minecraft:water 100 {F(pos)}",
             expect="filled 0/100 L of minecraft:water (REJECTED)"),
        # the OUT arm: the top mask is open, the output bank is empty, the input tank is
        # fill-only — nothing is drainable
        Step(f"gt6machine {literal} fluid draw up 100 {F(pos)}",
             expect="drawn 0/100 L of nothing (REJECTED)"),
        # the live census + the six-side face lists (the :511 probe pinned to bottom-only)
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect="in[0]=1000 L of minecraft:water"),
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect="masks fluidIn=100 fluidOut=66 energyIn=65"),
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect="faces fluidIn=south,east fluidOut=up energyIn=down"),
        # the minimal stub feed arm (the DRYING item slot; never starts a process)
        Step(f"gt6machine {literal} input 1 {F(pos)}",
             expect="bricks into slot 0"),
        # the HU carrier live at the row's tier band — one mInputMax packet per iteration
        # (the :503 band saturates at mInputMax - mEnergy; doWork drains it back each tick);
        # the poured DRYING map starts on the water row — progress moves the exact driven
        # arithmetic, the batch cannot complete inside 40 ticks so outputs stay empty
        Step(f"gt6machine {literal} inject 40 {maxin} {F(pos)}",
             expect=inject_progress),
        Step(f"gt6machine {literal} inject 40 {maxin} {F(pos)}",
             expect="outputs=[]"),
        # the production verdict + the tier columns + the menu marker: the driven
        # loop's batch resets on the first starved real tick (active=false), leaving the
        # recipe-aware maxProgress on the dial
        Step(f"gt6machine {literal} check {F(pos)}", expect=max_progress),
        Step(f"gt6machine {literal} check {F(pos)}", expect="active=false"),
        Step(f"gt6machine {literal} check {F(pos)}", expect=parallel),
        Step(f"gt6machine {literal} check {F(pos)}", expect="parallelDuration=true"),
        Step(f"gt6machine {literal} check {F(pos)}", expect=recin),
        # flipped by machine-fluid-gui: the gt6:dryer menu supplier is bound, so the
        # check probe CONSTRUCTS the menu (the old data=-2 menu-less marker is gone) and
        # the ContainerData dial reads the post-reset state — t1-t3 still hold recipe
        # water so the re-find leaves units(0, <recipe max>) = 0; t4 drained the tank
        # inside its own driven injects so the re-find is DID_NOT_FIND and the dial
        # rests on the -1 arm — per-tier column, see the TIERS comment
        Step(f"gt6machine {literal} check {F(pos)}", expect=data_dial),
        Step(f"gt6machine {literal} check {F(pos)}", expect="bricks"),
    ]

# ------------------------------------------- teardown
steps += [
    phase("C: teardown — the explicit restore over the dryer row (the pass-open bbox is the backstop)"),
    Step("fill 93 61 93 156 68 107 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="dryer-family",
    slug="dryer",
    sites=gt6world.declare_sites(T1, T2, T3, T4),
    preferred_ports=(25753, 25763),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
