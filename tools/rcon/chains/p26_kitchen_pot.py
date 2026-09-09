#!/usr/bin/env python3
"""p26-kitchen-pot-bowl — the manual kitchen family e2e (declarative framework, the
p26_cfoam_refill shape).

Chain semantics (task p26-kitchen-pot-bowl ACCEPTANCE rcon — "place pot→inject 流体+
物品→右键交互臂出料断言；tap 灌装臂（Faucet/tap 链）"), over /gt6kitchen:

  A the steel pot (RM.Bath carrier, 8000 L): place → fluid fill minecraft:water 1000
    through the capability face = the LIVE proof of the :302 fill admission doors (the
    hot/density predicates the offline JVM cannot carry — GT6KitchenBlockEntityTest's
    declared boundary) → check pins tank0=water:1000/8000 → interact reports "no action"
    (RM.Bath has ZERO resolvable rows in this universe — GT6RecipesBath's reconciled
    dormancy — so the top-face round is the idle verdict, LIVE).

  B the tap-pour arm (P12 TapFillable): barrel east of the tap, the tap mounted on the
    barrel's east face (facing=west) ABOVE the pot → /gt6tank tap runs the tap-to-tap
    chain: the pot's tapFill (the :302 admission, executed) accepts 250 L
    (TAP_TO_TAP_DEFAULT), the barrel pays exactly that — "tap-to-tap moved 250 L" and
    the pot census climbs to 1250.

  C the wooden pot (4000 L): place → water fill → check pins tank0=.../4000L — the two
    pot capacities LIVE (Loader:2173 4000 / :2175 8000).

  D the ceramic bowl RUNS A REAL ROW (RM.Mixer, the C-Foam base rock group :252-253):
    6x dust_stone + 2x dust_sand + 1x dust_small_clay + 1000 water → interact
    "processed: fluids (+tanks)" → check pins tank6=gt6:cfoam:1000/8000 (the output
    tank) with the input slots consumed → fluid draw pulls 500 L of gt6:cfoam back out.
    The Mixer row exercises the SAME activateChain code path the Bath rows would ride —
    the row-processing contract proven with a live row.

  E teardown: the explicit fill-air over the band. No global state is touched.

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_kitchen_pot.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the 420 band of the 64 layer — clear of p26cfoamrefill's 412-418 band
POT = "420 64 20"          # the steel pot
TAP = "420 65 20"          # the tap, directly ABOVE the pot (the tap-to-tap target side)
BARREL = "419 65 20"       # the tap's host barrel, WEST of the tap (facing=west)
WOOD_POT = "424 64 20"     # the wooden pot
BOWL = "422 64 20"         # the ceramic bowl

steps = []

# ------------------------------------------------- A: the steel pot — the fill admission, LIVE
steps += [
    phase("A: the steel pot — water through the :302 admission (the live door proof), the idle interact"),
    Step(f"gt6kitchen pot place {POT}", expect="GT6 bathing_pot_steel placed at"),
    Step(f"gt6kitchen pot fluid fill up minecraft:water 1000 {POT}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6kitchen pot check {POT}", expect="tank0=minecraft:water:1000/8000L"),
    # RM.Bath carries ZERO resolvable rows (the reconciled dormancy) — the top-face
    # manual round with no recipe is the idle verdict, LIVE
    Step(f"gt6kitchen pot interact {POT}", expect="GT6 bathing_pot interact: no action"),  # the shared BET name (ADR-P3-1)
]

# ------------------------------------------------- B: the tap-pour arm (P12 TapFillable)
steps += [
    phase("B: the tap above the pot — the tap-to-tap chain pours through the pot's tapFill"),
    Step(f"setblock {BARREL} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6tank fill {BARREL} minecraft:water 2000", expect="filled 2000/2000"),
    Step(f"setblock {TAP} gt6:tap_stainless_steel[facing=west]", expect="Changed the block"),
    Step(f"gt6tank tap {TAP}", expect="tap-to-tap moved 250 L"),
    Step(f"gt6kitchen pot check {POT}", expect="tank0=minecraft:water:1250/8000L"),
]

# ------------------------------------------------- C: the wooden pot — the 4000 L row, LIVE
steps += [
    phase("C: the wooden pot — the 4000 L carrier capacity (:2173), live"),
    Step(f"gt6kitchen pot_wood place {WOOD_POT}", expect="GT6 bathing_pot_wood placed at"),
    Step(f"gt6kitchen pot_wood fluid fill up minecraft:water 1000 {WOOD_POT}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6kitchen pot_wood check {WOOD_POT}", expect="tank0=minecraft:water:1000/4000L"),
]

# ------------------------------------------------- D: the bowl RUNS a real RM.Mixer row
steps += [
    phase("D: the bowl — the C-Foam base rock row (:252-253) processed through activateChain"),
    Step(f"gt6kitchen bowl place {BOWL}", expect="GT6 mixing_bowl placed at"),
    Step(f"gt6kitchen bowl input gt6:dust_stone 6 {BOWL}",
         expect="GT6 mixing_bowl input: 6x gt6:dust_stone into slot 0"),
    Step(f"gt6kitchen bowl input gt6:dust_sand 2 {BOWL}",
         expect="GT6 mixing_bowl input: 2x gt6:dust_sand into slot 1"),
    Step(f"gt6kitchen bowl input gt6:dust_small_clay 1 {BOWL}",
         expect="GT6 mixing_bowl input: 1x gt6:dust_small_clay into slot 2"),
    Step(f"gt6kitchen bowl fluid fill up minecraft:water 1000 {BOWL}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6kitchen bowl interact {BOWL}", expect="GT6 mixing_bowl interact: processed:"),
    Step(f"gt6kitchen bowl check {BOWL}", expect="tank6=gt6:cfoam:1000/8000L"),
    Step(f"gt6kitchen bowl fluid draw up 500 {BOWL}",
         expect="drawn 500/500 L of gt6:cfoam (ACCEPTED)"),
    Step(f"gt6kitchen bowl check {BOWL}", expect="tank6=gt6:cfoam:500/8000L"),
]

# ------------------------------------------------- E: teardown
steps += [
    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 418 62 19 425 67 21 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-kitchen-pot",
    slug="p26kitchenpot",
    sites=gt6world.declare_sites(gt6world.Site(420, 64, 20), gt6world.Site(422, 64, 20), gt6world.Site(424, 64, 20)),
    preferred_ports=(26131, 26141),      # this card's pinned rcon/query pair (the 2613x segment, after p26cfoamrefill 26111/26121)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
