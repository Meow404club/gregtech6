#!/usr/bin/env python3
"""p16-drying-rows-backfill — the DRYING backfill round-trip chain (declarative framework).

Chain semantics (task p16-drying-rows-backfill ACCEPTANCE — "Dryer 新行 round-trip 抽样
≥3"): one T1 dryer driven by the existing dryer RCON shape (no new command arms), four
new-row round-trips through the p16 backfill:

  A the rig: dryer placed (FACING north, no fuel), baseline empty tanks.

  B three water-family arms (the p16-aqua-fluids six, Loader_Recipes_Chem.java:526-532):
    fill east gt6:<fluid> <in> → inject (the doInject driver, the p14-dryer-family arm)
    → fluid stat pins the EXACT arithmetic in[0]=0 + out[0]=<out> L of gt6:distilled_water
    → draw up <out> empties the bank (the next arm starts clean).
      spdew 10 → 8, mnwtr 10 → 8 (:526/:527), water_geothermal 25 → 20 (:528).

  C the ice-family arm (:521 snowball — the one ice row whose input is a vanilla item, so
    the existing vanilla container surface feeds it): a hopper above the dryer pushes ONE
    snowball into input slot 0 (the default item masks are all-open, mItemInputs=127; the
    gated IItemHandler inserts input-range slots only), the :521 row runs
    (250 L, duration 1000, EUt 16 — 16000 progress budget, the driven tick lands ~12.8/t
    per the p14-dryer-family census, inject 4000 overshoots with the idle-stall backstop)
    → stat pins out[0]=250 L of gt6:distilled_water → draw up 250.

The remaining ice rows are per-row pinned offline (GT6RecipesDryingTest: transcription
walk + live-universe census + findRecipe + consume); the live pour census for ALL rows is
the server log line "GT6 Drying poured: 18 loaded, 3 skipped" (:530 water_hot + the two
upstream-dead gemChipped/gemFlawed Ice rows).

passes=2 is the idempotency proof (the [0,0] of this chain); the sites bbox cleanup
between passes re-airs the rig.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p16_drying_rows.py
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

# The rig site: the dryer (340,64,20) with the hopper stacked one above (dy=2 covers it).
# Band x=340, clear of every earlier chain's sites (the aqua card used the x=300 band).
DRYER_SITE = gt6world.Site(340, 64, 20, dx=1, dy=2, dz=1)
DRYER = F(DRYER_SITE)          # 340 64 20
HOPPER = "340 65 20"           # stacked on the dryer, facing down into it

steps = []

# ------------------------------------------------- A: the rig
steps += [
    phase("A: the rig — dryer placed, no fuel, empty banks"),
    Step(f"gt6machine dryer place {DRYER}", expect="GT6 dryer placed"),
    # data=-1 not -2: the gt6:dryer menu supplier is bound since p16-machine-fluid-gui,
    # so the check probe CONSTRUCTS the menu (the old data=-2 menu-less marker is gone)
    # and the fresh-placed idle ContainerData (progress=0/0, never started) reads -1 —
    # the same flip p14_loop_closure took at its :75 arm
    Step(f"gt6machine dryer check {DRYER}", expect="data=-1"),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
    Step(f"setblock {HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="in[0]=0 L of nothing; out[0]=0 L of nothing"),
]

# ------------------------------------------------- B: the water-family arms
WATER_ARMS = [
    ("spdew"           , 10,   8),  # :526 — Loader_Recipes_Chem.java:526 verbatim 10/8
    ("mnwtr"           , 10,   8),  # :527 — 10/8
    ("water_geothermal", 25,  20),  # :528 — 25/20
]

for _fluid, _inp, _out in WATER_ARMS:
    steps += [
        phase(f"B: {_fluid} {_inp} -> {_out} — fill, drive, exact arithmetic, draw"),
        Step(f"gt6machine dryer fluid fill east gt6:{_fluid} {_inp} {DRYER}",
             expect=f"filled {_inp}/{_inp} L of gt6:{_fluid} (ACCEPTED)"),
        Step(f"gt6machine dryer inject 80 64 {DRYER}", expect="used=80"),
        Step(f"gt6machine dryer fluid stat {DRYER}",
             expect=f"in[0]=0 L of nothing; out[0]={_out} L of gt6:distilled_water"),
        Step(f"gt6machine dryer fluid draw up {_out} {DRYER}",
             expect=f"drawn {_out}/{_out} L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
    ]

# ------------------------------------------------- C: the ice-family arm (:521 snowball)
steps += [
    phase("C: snowball 1 -> 250 — the :521 row, the hopper-fed item input, 250 L distilled"),
    Step(f"item replace block {HOPPER} container.0 with minecraft:snowball 1",
         expect="Replaced", sleep=4.0),
    # the hopper push cadence (8 game ticks) landed the snowball in slot 0
    Step(f"gt6machine dryer check {DRYER}", expect="snowball"),
    # 16000 progress budget (duration 1000 x EUt 16); ~12.8/t driven; 4000 overshoots,
    # the row stalls honestly once the single item is consumed
    Step(f"gt6machine dryer inject 4000 64 {DRYER}", expect="used=4000"),
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="in[0]=0 L of nothing; out[0]=250 L of gt6:distilled_water"),
    Step(f"gt6machine dryer fluid draw up 250 {DRYER}",
         expect="drawn 250/250 L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
]

# ------------------------------------------------- teardown
steps += [
    phase("D: teardown — the explicit restore over the rig (the pass-open bbox is the backstop)"),
    Step("fill 337 61 17 343 67 23 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p16-drying-rows-backfill",
    slug="p16dryrows",
    sites=gt6world.declare_sites(DRYER_SITE),
    preferred_ports=(25775, 25785),      # this card's pinned rcon/query pair (P16 segment)
    response_timeout=60.0,               # the 4000-tick inject loop runs server-side
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
