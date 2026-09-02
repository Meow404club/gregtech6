#!/usr/bin/env python3
"""p12-engine-diesel — the Diesel Engine acceptance chain (declarative framework).

Chain semantics (task p12-engine-diesel acceptance b):

  A the e2e line: diesel_engine_bronze (facing=east, emit x+, rate 16 RU/t DC)
    -> 3x wood-small axles [axis=x] -> shredder. /gt6engine fuel <pos>
    gt6:diesel 2000 writes the input tank through the funnelFill surface (the
    containsInput gate live — water is REFUSED; the bronze tank caps the
    accepted amount at mRate * 10 = 160 L, and by the first stat the machine
    has already burned its opening litre); the engine burns the FM.Engine
    row (448 power/L) into +16 RU packets and the shredder produces dust — the
    W3 closure of the fluid-fuel -> RU DC -> axle -> consumer chain.

  B the DC signature: rate=16 RU/t (DC constant-sign) on the engine stat; the
    exit axle transfers=16 RU/t (zero-loss magnitude equality) — the POSITIVE
    counterpart of the crank chain's -16 packets.

  C the exhaust arm (the CO2 mark consumption): a wood barrel at the BACK face
    plus a solid block PAST it (the upstream vent probe
    getOffset(OPOS[mFacing], 1)) holds the exhaust RETAINED — stat asserts
    "retained". Removing both opens the back: the :142-145 direct-vent branch
    keeps the second engine's exhaust at x0 and drains the first to x0.

  D the out-of-fuel arm: fuel 4 -> burns dry -> stat asserts input=empty and
    active=false (the banked 1792 energy emits away in 112 ticks).

Run:  python3 tools/rcon/chains/p12-engine-diesel.py
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

# The declared sites: the e2e line (A/B), the exhaust arm (C), the burnout arm (D);
# the bbox cleanup union covers all of them per pass.
ENGINE, AXLE1, AXLE2, AXLE3, SHREDDER = (gt6world.Site(4, 64, 8), gt6world.Site(5, 64, 8),
                                         gt6world.Site(6, 64, 8), gt6world.Site(7, 64, 8),
                                         gt6world.Site(8, 64, 8))
BARREL, WALL = gt6world.Site(3, 64, 8), gt6world.Site(2, 64, 8)   # the back face + the vent-probe block
ENGINE2 = gt6world.Site(4, 64, 12)                                # the open-back vent arm
ENGINE3 = gt6world.Site(4, 64, 16)                                # the burnout arm

DIESEL = "gt6:diesel_engine_bronze"
AXLE = "gt6:axle_wood_treated_small"


CHAIN = Chain(
    name="p12-engine-diesel",
    slug="p12diesel",
    sites=gt6world.declare_sites(ENGINE, AXLE1, AXLE2, AXLE3, SHREDDER, BARREL, WALL, ENGINE2, ENGINE3),
    preferred_ports=(25713, 25723),      # this card's pinned rcon/query pair
    game_port=25703,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: the line — diesel engine(facing=east) fuelled gt6:diesel -> 3x axle -> shredder e2e"),
        Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
        Step(f"setblock {F(AXLE1)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"setblock {F(AXLE2)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"setblock {F(AXLE3)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"gt6machine shredder place {F(SHREDDER)}", expect="GT6 shredder placed at 8, 64, 8"),
        Step(f"gt6machine shredder input 8 {F(SHREDDER)}", expect="8x cobblestone into slot 0"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="input=empty"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="rate=16 RU/t (DC constant-sign)"),
        Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled 160 L of gt6:diesel"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="input=gt6:diesel x1"),
        # the negative arm: the refusal itself carries the FAILED marker (allow_failed) —
        # the expect pins the containsInput refusal text
        Step(f"gt6engine fuel {F(ENGINE)} gt6:water 100", expect="is not an ENGINE_FUELS input",
             allow_failed=True),

        phase("B: the DC drive — 448 power/L into +16 RU/t packets through three axles"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="active=true", sleep=4.0),
        Step(f"gt6machine shredder check {F(SHREDDER)}", expect="out[0]="),
        Step(f"gt6engine stat {F(AXLE3)}", expect="transferred=16 RU/t"),
        Step(f"gt6engine stat {F(AXLE3)}", expect="break pending=false"),

        phase("C: the exhaust arm — barrel + wall retain CO2, open back vents to x0"),
        Step(f"setblock {F(BARREL)} gt6:barrel_wood", expect="Changed the block"),
        Step(f"setblock {F(WALL)} stone", expect="Changed the block", sleep=2.0),
        Step(f"gt6engine stat {F(ENGINE)}", expect="back tank@3, 64, 8"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="retained"),
        # the second engine with an OPEN back: the vent branch zeroes the exhaust every tick
        Step(f"setblock {F(ENGINE2)} {DIESEL}[facing=east]", expect="Changed the block"),
        Step(f"gt6engine fuel {F(ENGINE2)} gt6:diesel 500", expect="filled 160 L", sleep=2.0),
        Step(f"gt6engine stat {F(ENGINE2)}", expect="exhaust=CO2 x0 (back open)"),
        # open the first engine's back (barrel + wall away): the retained units vent to x0
        Step(f"setblock {F(WALL)} air", expect="Changed the block"),
        Step(f"setblock {F(BARREL)} air", expect="Changed the block", sleep=2.0),
        Step(f"gt6engine stat {F(ENGINE)}", expect="exhaust=CO2 x0 (back open)"),

        phase("D: the burnout arm — fuel 4 burns dry, input=empty and active=false"),
        Step(f"setblock {F(ENGINE3)} {DIESEL}[facing=east]", expect="Changed the block"),
        Step(f"gt6engine fuel {F(ENGINE3)} gt6:diesel 4", expect="filled 4 L", sleep=14.0),
        Step(f"gt6engine stat {F(ENGINE3)}", expect="input=empty"),
        Step(f"gt6engine stat {F(ENGINE3)}", expect="active=false"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
