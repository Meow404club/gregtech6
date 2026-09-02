#!/usr/bin/env python3
"""p12-gearbox-transformer — the GearBox split / Transformer torque acceptance chain.

Chain semantics (task p12-gearbox-transformer acceptance b, ports 25712/25722):

  A the dual-output split arm: crank(facing=east) -> gearbox (/gt6engine gearbox
    writes gears=28 = faces W+N+S, no through-axle) -> axle[z] north + axle[z]
    south -> shredder x2. The round-robin (mOrder, upstream :214) feeds BOTH
    branches: each shredder ends the window with dust out. The gearbox tachometer
    stat reads transferred=16 RU/t (one -16x1 crank packet moved per tick).

  B the single-output arm + the max(1, power/3) cap: rig(RU 8 x 12) -> gearbox
    (gears=20 = W+N) -> shredder (with input). Per pass the output walk inserts at
    most max(1, power/3) packets (upstream :193): pass caps 4/2/2/1... against the
    shredder's 64-unit headroom the tick ends with power=4 RETAINED (8 packets =
    64 units moved) and the next input is mIgnorePower-rejected (:401) until the
    queue drains — the live readout is the steady stat "current speed=8 x
    power=4" + "transferred=64 RU/t". (The exact per-pass package sequence is
    pinned offline by GearBoxTest.outputRespectsMaxOnePowerThirdPerFacePerPass.)

  C the gear-explosion arm: rig(RU 64 > VMAX[0]=16) -> gearbox (gears=28), placed
    and aged past the mTimer<10 load grace (:358). The first packet EXPLODES the
    gears: scrap + (tCount-1) gear legs drop (the gear item is the p12-gear-items
    pool — the block item itself drops instead, declared) and the mask zeroes
    while the BLOCK SURVIVES (:366-368). Assertions: stat gears=0/axle=0/
    gearsWork=true, the block still present, item entities on the ground, and the
    rig stat unchanged (no consumption ledger).

  D the transformer arm: crank(facing=east) -> transformer_rotation[facing=west]
    (FRONT=input) -> axle_wood_huge[x] -> shredder. The ÷4 x 4 pair read off the
    stat: "last in=-16x1" and "last out=-4x4" (speed ÷4, power x4, NEGATIVE sign
    preserved — the counterclockwise semantic survives the torque conversion), the
    axle moves |4x4| = 16 RU/t (the conservation equality with the direct line)
    with break pending=false, and the shredder produces.

Run:  python3 tools/rcon/chains/p12-gearbox-transformer.py
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

# The declared sites — four arms on their own z rows.
CRANK, GEARBOX, AXLE_N, SHRED_N, AXLE_S, SHRED_S = (
    gt6world.Site(4, 64, 8), gt6world.Site(5, 64, 8),
    gt6world.Site(5, 64, 7), gt6world.Site(5, 64, 6),
    gt6world.Site(5, 64, 9), gt6world.Site(5, 64, 10))
RIG_B, GEARBOX_B, SHRED_B = gt6world.Site(4, 64, 13), gt6world.Site(5, 64, 13), gt6world.Site(5, 64, 12)
RIG_C, GEARBOX_C = gt6world.Site(4, 64, 16), gt6world.Site(5, 64, 16)
CRANK_D, TRANS, AXLE_D, SHRED_D = (
    gt6world.Site(4, 64, 20), gt6world.Site(5, 64, 20),
    gt6world.Site(6, 64, 20), gt6world.Site(7, 64, 20))

AXLE_Small = "gt6:axle_wood_treated_small"
AXLE_Huge = "gt6:axle_wood_treated_huge"


CHAIN = Chain(
    name="p12-gearbox-transformer",
    slug="p12gearbox",
    sites=gt6world.declare_sites(CRANK, GEARBOX, AXLE_N, SHRED_N, AXLE_S, SHRED_S,
                                 RIG_B, GEARBOX_B, SHRED_B, RIG_C, GEARBOX_C,
                                 CRANK_D, TRANS, AXLE_D, SHRED_D),
    preferred_ports=(25712, 25722),      # this card's pinned rcon/query pair
    game_port=25702,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: the split arm — crank -> gearbox(gears W+N+S) -> axle[z] x2 -> shredder x2"),
        Step(f"setblock {F(CRANK)} gt6:crank[facing=east]", expect="Changed the block"),
        Step(f"setblock {F(GEARBOX)} gt6:gearbox", expect="Changed the block"),
        Step(f"gt6engine gearbox {F(GEARBOX)} 28 0", expect="masks set gears=28, axle=0, gearsWork=true"),
        Step(f"setblock {F(AXLE_N)} {AXLE_Small}[axis=z]", expect="Changed the block"),
        Step(f"setblock {F(AXLE_S)} {AXLE_Small}[axis=z]", expect="Changed the block"),
        Step(f"gt6machine shredder place {F(SHRED_N)}", expect="GT6 shredder placed at 5, 64, 6"),
        Step(f"gt6machine shredder place {F(SHRED_S)}", expect="GT6 shredder placed at 5, 64, 10"),
        Step(f"gt6machine shredder input 32 {F(SHRED_N)}", expect="32x cobblestone into slot 0"),
        Step(f"gt6machine shredder input 32 {F(SHRED_S)}", expect="32x cobblestone into slot 0"),
        Step(f"gt6engine stat {F(CRANK)}", expect="facing=east(5) emit-side"),
        Step(f"gt6engine stat {F(GEARBOX)}", expect="gears=28 (bits0-5)"),
        Step(f"gt6engine stat {F(GEARBOX)}", expect="gearsWork=true"),
        Step(f"gt6engine crank {F(CRANK)} 600", expect="armed 600 ticks", sleep=15.0),
        # both branches of the round-robin produced (the mOrder wheel feeds W+N+S)
        Step(f"gt6machine shredder check {F(SHRED_N)}", expect="out[0]="),
        Step(f"gt6machine shredder check {F(SHRED_S)}", expect="out[0]="),
        Step(f"gt6machine shredder check {F(SHRED_N)}", expect="dust_stone"),
        Step(f"gt6machine shredder check {F(SHRED_S)}", expect="dust_stone"),
        # the tachometer readout: one 16x1 packet moved per tick
        Step(f"gt6engine stat {F(GEARBOX)}", expect="transferred=16 RU/t"),
        Step(f"gt6engine stat {F(GEARBOX)}", expect="jammed=false"),

        phase("B: the single-output arm — rig 8x12 -> gearbox(gears W+N) -> shredder, the per-pass cap"),
        Step(f"gt6machine shredder place {F(SHRED_B)}", expect="GT6 shredder placed at 5, 64, 12"),
        Step(f"gt6machine shredder input 32 {F(SHRED_B)}", expect="32x cobblestone into slot 0"),
        Step(f"setblock {F(GEARBOX_B)} gt6:gearbox", expect="Changed the block"),
        Step(f"gt6engine gearbox {F(GEARBOX_B)} 20 0", expect="masks set gears=20, axle=0, gearsWork=true"),
        Step(f"gt6energy place {F(RIG_B)}", expect="GT6 energy source placed at 4, 64, 13"),
        Step(f"gt6energy type {F(RIG_B)} RU", expect="type ENERGY.KINETIC_ROTATION"),
        Step(f"gt6energy volt {F(RIG_B)} 8", expect="voltage 8"),
        Step(f"gt6energy amp {F(RIG_B)} 12", expect="amperage 12"),
        Step(f"gt6energy mode {F(RIG_B)} on", expect="emitting true", sleep=2.0),
        # the steady state: each tick moves 8 packets (64 units = the shredder cap)
        # and RETAINS power=4 — the per-pass max(1, power/3) cap + the :401 queue
        Step(f"gt6engine stat {F(GEARBOX_B)}", expect="current speed=8 x power=4"),
        Step(f"gt6engine stat {F(GEARBOX_B)}", expect="transferred=64 RU/t"),
        Step(f"gt6machine shredder check {F(SHRED_B)}", expect="out[0]="),

        phase("C: the gear-explosion arm — rig 64 > VMAX[0]=16 -> gears explode, the block survives"),
        Step(f"setblock {F(GEARBOX_C)} gt6:gearbox", expect="Changed the block"),
        Step(f"gt6engine gearbox {F(GEARBOX_C)} 28 0", expect="masks set gears=28, axle=0, gearsWork=true"),
        Step(f"execute if block {F(GEARBOX_C)} gt6:gearbox", expect="Test passed", sleep=2.0),
        # ... the 2 s sleep above ages the box past the mTimer < 10 load grace (:358)
        Step(f"gt6energy place {F(RIG_C)}", expect="GT6 energy source placed at 4, 64, 16"),
        Step(f"gt6energy type {F(RIG_C)} RU", expect="type ENERGY.KINETIC_ROTATION"),
        Step(f"gt6energy volt {F(RIG_C)} 64", expect="voltage 64"),
        Step(f"gt6energy mode {F(RIG_C)} on", expect="emitting true", sleep=1.5),
        # the explosion: the mask zeroes, the block SURVIVES, debris on the ground
        Step(f"gt6engine stat {F(GEARBOX_C)}", expect="gears=0 (bits0-5)"),
        Step(f"gt6engine stat {F(GEARBOX_C)}", expect="axle=0 (0=none 1=X 2=Y 3=Z)"),
        Step(f"gt6engine stat {F(GEARBOX_C)}", expect="gearsWork=true"),
        Step(f"execute if block {F(GEARBOX_C)} gt6:gearbox", expect="Test passed"),
        Step(f"execute if entity @e[type=minecraft:item,x=5,y=64,z=16,distance=..2]", expect="Test passed"),
        # the rig has no ledger — the exploded packet was consumed by the box (:369)
        Step(f"gt6energy stat {F(RIG_C)}", expect="voltage 64"),

        phase("D: the transformer arm — crank -> transformer_rotation[facing=west] -> axle[x] -> shredder"),
        Step(f"setblock {F(CRANK_D)} gt6:crank[facing=east]", expect="Changed the block"),
        Step(f"setblock {F(TRANS)} gt6:transformer_rotation[facing=west]", expect="Changed the block"),
        Step(f"setblock {F(AXLE_D)} {AXLE_Huge}[axis=x]", expect="Changed the block"),
        Step(f"gt6machine shredder place {F(SHRED_D)}", expect="GT6 shredder placed at 7, 64, 20"),
        Step(f"gt6machine shredder input 32 {F(SHRED_D)}", expect="32x cobblestone into slot 0"),
        Step(f"gt6engine stat {F(TRANS)}", expect="facing=west(4) input-side"),
        Step(f"gt6engine crank {F(CRANK_D)} 600", expect="armed 600 ticks", sleep=6.0),
        # the ÷4 x 4 pair with the NEGATIVE sign preserved (direction kept)
        Step(f"gt6engine stat {F(TRANS)}", expect="last in=-16x1"),
        Step(f"gt6engine stat {F(TRANS)}", expect="last out=-4x4"),
        Step(f"gt6engine stat {F(TRANS)}", expect="multiplier=4 (speed ÷4 power ×4, 8→2 wood row)"),
        # the axle moves |4x4| = 16 RU/t — the conservation equality, zero loss
        Step(f"gt6engine stat {F(AXLE_D)}", expect="transferred=16 RU/t"),
        Step(f"gt6engine stat {F(AXLE_D)}", expect="break pending=false"),
        Step(f"gt6machine shredder check {F(SHRED_D)}", expect="out[0]="),
        Step(f"gt6machine shredder check {F(SHRED_D)}", expect="dust_stone"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
