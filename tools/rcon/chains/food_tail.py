#!/usr/bin/env python3
"""food-tail — the card-block-outside drink-fluid live chain (the fooddrink shape).

  A the DRINK arms: one real right-click dispatch per arm (/gt6drink use drives
    ServerPlayerGameMode.useItemOn with an EMPTY main hand at the metal barrel —
    the GTBarrelBlock.use seam via the food-tail tryTankDrink helper):
    - gt6:mnwtr at food 18: CONSUME, 250 mB drained, food 18 -> 19 + regeneration
      (the upstream :371 potion.mineralwater row over the P16 aqua id — the KEPT
      fill this card landed),
    - gt6:potion.coffee at food 15: CONSUME, food 15 -> 19 (the tail coffee family,
      the C+39 row — no effects, the caffeine channel has no modern face),
    - gt6:potion.goldenapplejuice at food 15: CONSUME, food 15 -> 19 + the
      absorption 2400/0 + regeneration 100/1 effect pair (:607 verbatim, the
      .setLuminosity(15) family face),
    - gt6:lemonade: stat pins temperature 275 K (the :603 carrier correction, the
      b1 300 K honest-default fixed) then CONSUME at food 15 -> 19 + haste 900/1
      (the KEPT row; the :603 digSpeed effect, registry name haste),
    - gt6:sake at food 16: CONSUME, food 16 -> 20 + strength 300/1 (the
      KEPT-fill face, upstream :486 over the b1 "sake" id),
    - gt6:chlorine: PASS, tank untouched (the unregistered-fluid negative — the
      seam still gates, the helper fold is behaviour-preserving).

  C the census arms: one 144 mB fill/draw round-trip per row, FOOD_TAIL_SPECS
    order — all 10 tail registrations resolve through the LIVE tank handler, with
    the darkcoffee stat arm pinned at its 300 K carrier.

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/food_tail.py
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

# the single-block barrel site x=420 z=20 — the z=20 row keeps the barrel-band
# convention (this bbox keeps 418..422 with margin, clear of fooddrink's
# 402..406 and the act/cfoam 408..416 run); nothing sits east of 416 on
# the row
BARREL_SITE = gt6world.Site(420, 64, 20)
BARREL = F(BARREL_SITE)  # 420 64 20

TAIL_IDS = [
    "potion.goldenapplejuice",
    "potion.goldencider",
    "potion.idunsapplejuice",
    "potion.notchesbrew",
    "potion.darkcoffee",
    "potion.darkcafeaulait",
    "potion.coffee",
    "potion.cafeaulait",
    "potion.laitaucafe",
    "potion.darkchocolatemilk",
]

steps = [
    Step(f"setblock {BARREL} gt6:barrel_metal", expect="Changed the block"),
]

phase("A: the drink arms — one real useItemOn dispatch per arm (the tryTankDrink seam)")
steps += [
    Step("gt6drink clear", expect="food=20 effects=0"),
    Step("gt6drink hunger 18", expect="food level set: food=18"),
    Step(f"gt6tank fill {BARREL} gt6:mnwtr 250",
         expect="filled 250/250 L of gt6:mnwtr (ACCEPTED)"),
    Step(f"gt6tank stat {BARREL}",
         expect="250/64000 L of gt6:mnwtr, temperature 300 K"),
    Step(f"gt6drink use {BARREL}",
         # the food=1 row arithmetic 18 -> 19 (the :371 row reachable; the
         # regeneration add shows in the dispatch line — the effects face rides
         # the golden arm's pin, saturation is pass-nondeterministic so it stays
         # unpinned, the b2 food-face form)
         expect="result=CONSUME tank=empty food=19"),
    Step("gt6drink clear", expect="food=20 effects=0"),
    Step("gt6drink hunger 15", expect="food level set: food=15"),
    Step(f"gt6tank fill {BARREL} gt6:potion.coffee 250",
         expect="filled 250/250 L of gt6:potion.coffee (ACCEPTED)"),
    Step(f"gt6drink use {BARREL}",
         expect="result=CONSUME tank=empty food=19"),
    Step("gt6drink clear", expect="food=20 effects=0"),
    Step("gt6drink hunger 15", expect="food level set: food=15"),
    Step(f"gt6tank fill {BARREL} gt6:potion.goldenapplejuice 250",
         expect="filled 250/250 L of gt6:potion.goldenapplejuice (ACCEPTED)"),
    Step(f"gt6drink use {BARREL}",
         # the :607 absorption add, live — the shared suffix (forge bare path /
         # neo registeredName carry the same duration:amplifier tail)
         expect="absorption:2400:0"),
    Step("gt6drink clear", expect="food=20 effects=0"),
    Step("gt6drink hunger 15", expect="food level set: food=15"),
    Step(f"gt6tank fill {BARREL} gt6:lemonade 250",
         expect="filled 250/250 L of gt6:lemonade (ACCEPTED)"),
    Step(f"gt6tank stat {BARREL}",
         expect="250/64000 L of gt6:lemonade, temperature 275 K"),
    Step(f"gt6drink use {BARREL}",
         # the :603 digSpeed row — the modern registry name is haste (MobEffects.DIG_SPEED)
         expect="haste:900:1"),
    Step("gt6drink clear", expect="food=20 effects=0"),
    Step("gt6drink hunger 16", expect="food level set: food=16"),
    Step(f"gt6tank fill {BARREL} gt6:sake 250",
         expect="filled 250/250 L of gt6:sake (ACCEPTED)"),
    Step(f"gt6drink use {BARREL}",
         # the KEPT-fill face (:486 over the b1 "sake" id, the reverse-census
         # closure arm) — Potion.damageBoost is minecraft:strength in the modern
         # registry
         expect="strength:300:1"),
    Step("gt6drink clear", expect="food=20 effects=0"),
    Step(f"gt6tank fill {BARREL} gt6:chlorine 250",
         expect="filled 250/250 L of gt6:chlorine (ACCEPTED)"),
    Step(f"gt6drink use {BARREL}",
         expect="result=PASS tank=gt6:chlorine 250L food=20"),
    # the negative arm leaves its 250 L in the barrel — the single-fluid rule
    # (FluidTankGT fill contains-gate) would reject the census's first fill, so
    # the band empties before phase C
    Step(f"gt6tank draw {BARREL} 250",
         expect="drawn 250/250 L of gt6:chlorine (ACCEPTED)"),
]

phase("C: the census arms — 144 mB fill/draw round-trip per row (the full 10-registration live walk)")
for _id in TAIL_IDS:
    steps += [
        Step(f"gt6tank fill {BARREL} gt6:{_id} 144",
             expect=f"filled 144/144 L of gt6:{_id} (ACCEPTED)"),
        Step(f"gt6tank stat {BARREL}",
             expect=f"144/64000 L of gt6:{_id}, temperature 300 K"),
        Step(f"gt6tank draw {BARREL} 144",
             expect=f"drawn 144/144 L of gt6:{_id} (ACCEPTED)"),
    ]

phase("D: teardown — the explicit restore over the band (the pass-open bbox is the backstop)")
steps += [
    Step("fill 418 62 18 422 66 22 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="food-tail",
    slug="foodtail",
    sites=gt6world.declare_sites(BARREL_SITE),
    preferred_ports=(26135, 26145),      # this card's pinned rcon/query pair (after fooddrink 26113/26123, 26129/26139..26134/26144 taken)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
