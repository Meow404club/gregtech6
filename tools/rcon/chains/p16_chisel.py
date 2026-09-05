#!/usr/bin/env python3
"""p16-chisel-decalcify — the chisel item + the decalcify use-on arm acceptance chain.

Chain semantics (task p16-chisel-decalcify ACCEPTANCE ②/④, against p13_boiler_tank B3):

  A the give arm: the chisel lands as a REAL registered stack — chest + item replace
     gt6:chisel + data get block asserts Items[0].id="gt6:chisel". Vanilla /give needs an
     online player selector, which the headless RCON channel has none of; the container
     landing is the headless give (the p16_drying_rows `item replace block` precedent).
     The live useOn CLICK itself rides the /gt6boiler decalcify command arm, which calls
     the SAME GTBoilerTankBlockEntity.chisel the item dispatches (the single-source
     semantics; the fake-player context drive is the GTToolCommand shape, outside this
     card's file scope).

  B1 the low-scale repair arm (the ≤15/31 branch, GTBoilerTankBlockEntityTest
     theChiselDetonates* repair half on the live server): place → fill water →
     40× inject-hu 160 (the rng(10) calcification rolls, ~2 conversions each) →
     efficiency SCALED → decalcify → "descaled (repair …)" + efficiency=10000/10000
     (PRISTINE) → the block SURVIVES.

  B2 the high-pressure explosion arm (p13 B3 verbatim): place → fill → 40× inject-hu 160
     → the 150000 seed (barometer lands 24..30 > 15) → decalcify → OVERPRESSURE echo →
     the block is GONE (the deferred explode(F) on the next boiler tick).

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox cleanup
restores the sites between passes. Run with GT6_SESSION=off (the pinned per-chain ports;
the session model ignores chain.preferred_ports and parallel coders on the same node
segment would cross-fire — the p16-aqua-fluids lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p16_chisel.py
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

# The sites — x 184..200, z 124..140: clear of the p13/p14 boiler chain (x 104..172),
# the p16_side_io probe (x 100..120) and the fluid-gui tier row (z 200).
#   CHEST — the give site at (184,64,124): one chest, the chisel in container.0.
#   R     — the repair site at (184,64,132): no explosion at all.
#   X     — the detonation site at (200,64,140), strength ~5.6, the 8-block bbox.
CHEST = gt6world.Site(184, 64, 124, dx=1)
R = gt6world.Site(184, 64, 132, dy=1)
X = gt6world.Site(200, 64, 140, dx=8, dy=8, dz=8)

CHEST_POS = "184 64 124"
REPAIR = "184 64 132"
BLAST = "200 64 140"

steps = []

# ---------------------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — the chisel as a real registered stack (the headless give)"),
    Step(f"setblock {CHEST_POS} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST_POS} container.0 with gt6:chisel 1", expect="Replaced"),
    Step(f"data get block {CHEST_POS} Items[0]", expect="gt6:chisel"),
]

# ------------------------------------------------- B1: the low-scale repair arm
steps += [
    phase("B1: the repair arm — calcify at low pressure, chisel → descaled, block survives"),
    Step(f"gt6boiler place {REPAIR} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler fill {REPAIR} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
]
# 40 conversion beats (2 conversions each per 160 HU) — the rng(10) scale rolls
for _ in range(40):
    steps += [Step(f"gt6boiler inject-hu {REPAIR} 160", expect="booked 160/160 HU (ACCEPTED)", sleep=0.2)]
steps += [
    Step(f"gt6boiler efficiency {REPAIR}", expect="SCALED"),
    # the repair branch: the steam accumulated at this point sits far below the 15/31
    # gate (12800 L max against the 320000 L tank = barometer ~2), so the descaled echo
    # is itself the ≤15 branch verdict — an armed detonation would print OVERPRESSURE
    Step(f"gt6boiler decalcify {REPAIR}", expect="descaled (repair "),
    Step(f"gt6boiler efficiency {REPAIR}", expect="PRISTINE"),
    Step(f"execute if block {REPAIR} gt6:steam_boiler_tank_lead", expect="Test passed"),
]

# --------------------------------------- B2: the high-pressure explosion arm
steps += [
    phase("B2: the detonation arm — calcify, seed past 15/31, chisel → OVERPRESSURE (p13 B3)"),
    Step(f"gt6boiler place {BLAST} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler fill {BLAST} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
]
for _ in range(40):
    steps += [Step(f"gt6boiler inject-hu {BLAST} 160", expect="booked 160/160 HU (ACCEPTED)", sleep=0.2)]
steps += [
    # the seed: ~1875 conversions → the tank lands 24..31/31 (always > 15), never full
    Step(f"gt6boiler inject-hu {BLAST} 150000", expect="booked 150000/150000 HU (ACCEPTED)", sleep=2.0),
    Step(f"gt6boiler decalcify {BLAST}", expect="OVERPRESSURE", sleep=3.0),
    Step(f"execute unless block {BLAST} gt6:steam_boiler_tank_lead", expect="Test passed"),
    Step("time query daytime", expect="The time is"),  # the server survived the blast
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit restore over every site (the pass-open bbox is the backstop)"),
    Step("fill 182 62 122 186 66 126 air", expect="filled"),
    Step("fill 183 62 131 185 66 133 air", expect="filled"),
    Step("fill 192 56 132 208 72 148 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p16-chisel-decalcify",
    slug="p16chisel",
    sites=gt6world.declare_sites(CHEST, R, X),
    preferred_ports=(25778, 25788),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
