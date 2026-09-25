#!/usr/bin/env python3
"""axle-family — the Axle RU-carrier acceptance chain (declarative framework).

Chain semantics (task axle-family acceptance b):

  A axle line placement: crank (facing=east, emit x+) -> 3x wood-small axles
    [axis=x] -> shredder, all on one straight X line. The axle /gt6engine stat
    reads the ratings off the placed row (speed rating=16 = VMAX[0], bandwidth=1
    = the wood-small NBT_PIPEBANDWIDTH, Loader_MultiTileEntities.java:1663).

  B the e2e drive + zero loss: /gt6engine crank 200 arms the server-driven
    window; the -16x1 RU packets walk the three axles (the first ticks die on
    the oRotationDir == 0 idle gate, upstream :112 — the window absorbs that)
    into the shredder, which produces dust. The ZERO-LOSS proof is the
    magnitude equality across the line: the crank packet |size| = 16 (stat) ==
    the exit axle transferred = 16 RU/t (stat) — the upstream :159
    getEnergyLossPerMeter = 0 semantic, one /gt6engine stat per end.

  C the overspeed arm: the energy_source rig retyped to RU at voltage 64 ( >
    VMAX[0] = 16) drives one wood-small axle directly. The packet walks
    transferRotations, addToEnergyTransferred trips |aSpeed| > mSpeed, the
    break flag pops the axle off on its own next tick (level.destroyBlock —
    the loot drop path). Assertions: the axle BLOCK is gone (execute if block
    fails), and the rig is UNCHANGED (stat still voltage 64 — the rig has no
    consumption ledger, the packet's original amount was returned to the
    caller, upstream :128, so nothing anywhere booked a spend).

  D the wrong-axis arm: a second crank (facing=east) fires into an axle placed
    [axis=z] — PERPENDICULAR to the emit direction. The packet lands on the
    axle's west face, whose axis (X) is not the axle's axis (Z), so
    isEnergyAcceptingFrom refuses (doEnergyInjection returns 0, the packet
    unconsumed) and nothing downstream moves: the shredder stays energy=0,
    active=false with a full input stack, and the axle block SURVIVES (the
    refusal never arms any break).

Run:  python3 tools/rcon/chains/axle-family.py
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

# The eight declared sites — the e2e line, the overspeed arm, the wrong-axis arm;
# the bbox cleanup union covers all of them per pass.
RIG = gt6world.Site(0, 64, 0)
CRANK, AXLE1, AXLE2, AXLE3, SHREDDER = (gt6world.Site(4, 64, 8), gt6world.Site(5, 64, 8),
                                        gt6world.Site(6, 64, 8), gt6world.Site(7, 64, 8),
                                        gt6world.Site(8, 64, 8))
RIG_OS, AXLE_OS = gt6world.Site(4, 64, 12), gt6world.Site(5, 64, 12)
CRANK2, AXLE_BAD, SHREDDER2 = (gt6world.Site(4, 64, 16), gt6world.Site(5, 64, 16),
                               gt6world.Site(5, 64, 17))

AXLE = "gt6:axle_wood_treated_small"


CHAIN = Chain(
    name="axle-family",
    slug="axle",
    sites=gt6world.declare_sites(RIG, CRANK, AXLE1, AXLE2, AXLE3, SHREDDER, RIG_OS, AXLE_OS,
                                 CRANK2, AXLE_BAD, SHREDDER2),
    preferred_ports=(25720, 25730),      # this card's pinned rcon/query pair
    game_port=25710,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: the straight line — crank(facing=east) -> 3x axle[axis=x] -> shredder"),
        Step(f"setblock {F(CRANK)} gt6:crank[facing=east]", expect="Changed the block"),
        Step(f"setblock {F(AXLE1)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"setblock {F(AXLE2)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"setblock {F(AXLE3)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"gt6machine shredder place {F(SHREDDER)}", expect="GT6 shredder placed at 8, 64, 8"),
        Step(f"gt6machine shredder input 8 {F(SHREDDER)}", expect="cobblestone into slot 0"),
        Step(f"gt6engine stat {F(CRANK)}", expect="facing=east(5) emit-side"),
        Step(f"gt6engine stat {F(AXLE1)}", expect="axis=x"),
        Step(f"gt6engine stat {F(AXLE1)}", expect="speed rating=16 RU"),
        Step(f"gt6engine stat {F(AXLE1)}", expect="bandwidth=1 packets/t"),

        phase("B: the e2e drive — 200 ticks through three axles into the shredder, zero loss"),
        # 20 tps: 200 ticks ~= 10 s; the drive is still running at the stats below,
        # which is what makes the transferred readout live
        Step(f"gt6engine crank {F(CRANK)} 200", expect="armed 200 ticks", sleep=6.0),
        Step(f"gt6machine shredder check {F(SHREDDER)}", expect="out[0]="),
        Step(f"gt6machine shredder check {F(SHREDDER)}", expect="dust_stone"),
        # the ZERO-LOSS equality: crank |packet| = 16 == exit-axle transferred = 16 RU/t
        Step(f"gt6engine stat {F(CRANK)}", expect="RU packet size=-16"),
        Step(f"gt6engine stat {F(AXLE3)}", expect="transferred=16 RU/t"),
        Step(f"gt6engine stat {F(AXLE3)}", expect="break pending=false"),

        phase("C: the overspeed arm — rig(RU, 64/t) overdrives one axle -> popOff + rig unchanged"),
        Step(f"gt6energy place {F(RIG_OS)}", expect="GT6 energy source placed at 4, 64, 12"),
        Step(f"setblock {F(AXLE_OS)} {AXLE}[axis=x]", expect="Changed the block"),
        Step(f"gt6energy type {F(RIG_OS)} RU", expect="type ENERGY.KINETIC_ROTATION"),
        Step(f"gt6energy volt {F(RIG_OS)} 64", expect="voltage 64"),
        # mode is still OFF here — the axle stands untouched
        Step(f"execute if block {F(AXLE_OS)} {AXLE}[axis=x]", expect="Test passed"),
        Step(f"gt6energy mode {F(RIG_OS)} on", expect="emitting true", sleep=1.5),
        # 64 > VMAX[0]=16: the first packet arms the break, the axle's next tick destroyBlocks it
        Step(f"execute if block {F(AXLE_OS)} {AXLE}[axis=x]", expect="Test failed"),
        # the source packet came back to the caller (upstream :128) and the rig keeps no
        # ledger — its stat is byte-identical to the pre-drive state
        Step(f"gt6energy stat {F(RIG_OS)}", expect="voltage 64"),
        Step(f"gt6energy stat {F(RIG_OS)}", expect="amperage 1"),

        phase("D: the wrong-axis arm — a perpendicular axle refuses the packet, everything inert"),
        Step(f"setblock {F(CRANK2)} gt6:crank[facing=east]", expect="Changed the block"),
        Step(f"setblock {F(AXLE_BAD)} {AXLE}[axis=z]", expect="Changed the block"),
        Step(f"gt6machine shredder place {F(SHREDDER2)}", expect="GT6 shredder placed at 5, 64, 17"),
        Step(f"gt6machine shredder input 8 {F(SHREDDER2)}", expect="cobblestone into slot 0"),
        Step(f"execute if block {F(AXLE_BAD)} {AXLE}[axis=z]", expect="Test passed"),
        Step(f"gt6engine crank {F(CRANK2)} 200", expect="armed 200 ticks", sleep=6.0),
        # the packet hit the axle's WEST face (axis X != axle axis Z): refused, unconsumed
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="energy=0"),
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="active=false"),
        Step(f"execute if block {F(AXLE_BAD)} {AXLE}[axis=z]", expect="Test passed"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
