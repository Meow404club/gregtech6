#!/usr/bin/env python3
"""qu-laser-domain — the EU->LU->EU laser chain live acceptance (sweep group
qu_laser; the electric_heater rig geometry, one column per arm):

  A THE CHAIN LEG (acceptance ②): the EU dial (volt 32 = the NBT_INPUT rec column)
    behind the T1 CO2 Laser (facing=south: intake all-but-front, LU out the FRONT),
    TWO laser fiber wire segments carrying the beam losslessly, and the T1 Laser
    Absorber on the far end (facing=south: the beam lands on its BACK face, EU out
    the FRONT). /gt6laser reset + the dial on -> the laser stat pins "half true"
    (in == 2*out EXACTLY — the units() half-rate) and the absorber stat pins the EU
    landing (in = the LU hop = the laser's out; the frequency tuning: the 16-sized
    LU packets ride the absorber's 16 inMin door exactly). The absorber's EU tail
    (8-sized packets) lands in a second T1 laser: the white-burn door — no port EU
    machine accepts 8 without burning it (the P28 ULV-wall census), and the Root
    gate counts the burned offer as USED, so the emitter side books the out.

  B THE TUNING ARMS (acceptance ③ live half): the dial re-volted BELOW the laser's
    inMin (8 < 16) -> the white burn (Root:717): "in 0" on a live dial — the
    detuned source tunes to nothing; back to 32 -> flows again.

  C THE T5 RUNG: a second column at volt 8192 with the emission face walled (no
    consumer downstream) -> "out 0" with a live intake (the WASTE_ENERGY=T vent,
    nothing banked — the lossy-by-design form).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/qu_laser.py
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

Z = 352  # the fresh band east of the usb-data chest column (z=348)
# the T1 chain column: dial -> laser -> wire -> wire -> absorber (the beam flies -Z+3)
L1 = gt6world.Site(446, 65, Z)          # the T1 CO2 Laser
L1_SRC = gt6world.Site(446, 65, Z - 1)  # the EU dial (the dynamo-rig source seat)
L1_W1 = gt6world.Site(446, 65, Z + 1)   # fiber wire segment 1 (the FRONT face)
L1_W2 = gt6world.Site(446, 65, Z + 2)   # fiber wire segment 2
L1_ABS = gt6world.Site(446, 65, Z + 3)  # the T1 Laser Absorber (the beam on its BACK)
L1_SINK = gt6world.Site(446, 65, Z + 4) # the T1 laser EATING the absorber's 8 EU packets — the
                                        # white-burn door (no port machine accepts 8 without
                                        # burning it; the emitter side still books the out)
# the T5 wall arm: laser + dial only (the emission face walled by air-abstention is
# NOT enough — the flood needs no consumer, so the T5 laser emits into bare air: the
# WASTE vent clears the bucket regardless; the absorber leg proves the landing on T1)
L5 = gt6world.Site(452, 65, Z)
L5_SRC = gt6world.Site(452, 65, Z - 1)

steps = [
    phase("A: the T1 chain — EU 32 in, LU 16 over two fiber wires, EU 8 out the absorber"),
    Step(f"setblock {F(L1)} gt6:co2_laser[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(L1_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(L1_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(L1_SRC)} 32", expect="voltage 32 EU"),
    Step(f"gt6wire place wire_laser {F(L1_W1)}", expect="GT6 wire placed"),
    Step(f"setblock {F(L1_ABS)} gt6:laser_absorber[facing=south]", expect="Changed the block"),
    Step(f"gt6wire place wire_laser {F(L1_W2)}", expect="GT6 wire placed"),
    Step(f"setblock {F(L1_SINK)} gt6:co2_laser[facing=south]", expect="Changed the block"),
    Step(f"gt6laser reset {F(L1)}", expect="GT6 laser accounting reset"),
    Step(f"gt6laser reset {F(L1_ABS)}", expect="GT6 laser accounting reset"),
    Step(f"gt6energy mode {F(L1_SRC)} on", expect="emitting true"),
    Step(f"gt6laser stat {F(L1)}", expect="EU->LU", sleep=3.0, poll=15.0),
    Step(f"gt6laser stat {F(L1)}", expect="half true", sleep=0.5),
    Step(f"gt6laser stat {F(L1_ABS)}", expect="LU->EU", sleep=0.5),
    Step(f"gt6laser stat {F(L1_ABS)}", expect="half true", sleep=0.5),
    Step(f"gt6laser stat {F(L1_ABS)}", expect="in ", sleep=0.5),

    phase("B: the tuning arms — a detuned dial white-burns, the retune flows"),
    Step(f"gt6energy mode {F(L1_SRC)} off", expect="emitting false"),
    Step(f"gt6energy volt {F(L1_SRC)} 8", expect="voltage 8 EU"),
    Step(f"gt6laser reset {F(L1)}", expect="GT6 laser accounting reset"),
    Step(f"gt6laser reset {F(L1_ABS)}", expect="GT6 laser accounting reset"),
    Step(f"gt6energy mode {F(L1_SRC)} on", expect="emitting true"),
    Step(f"gt6laser stat {F(L1)}", expect="in 0,", sleep=2.0, poll=15.0),
    Step(f"gt6laser stat {F(L1_ABS)}", expect="in 0,", sleep=0.5),
    Step(f"gt6energy mode {F(L1_SRC)} off", expect="emitting false"),
    Step(f"gt6energy volt {F(L1_SRC)} 32", expect="voltage 32 EU"),
    Step(f"gt6laser reset {F(L1)}", expect="GT6 laser accounting reset"),
    Step(f"gt6laser reset {F(L1_ABS)}", expect="GT6 laser accounting reset"),
    Step(f"gt6energy mode {F(L1_SRC)} on", expect="emitting true"),
    Step(f"gt6laser stat {F(L1_ABS)}", expect="half true", sleep=2.0, poll=15.0),

    phase("C: the T5 rung — 8192 EU in, the emission face walled, the waste form"),
    Step(f"gt6energy mode {F(L1_SRC)} off", expect="emitting false"),
    Step(f"setblock {F(L5)} gt6:co2_laser_t5[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(L5_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(L5_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(L5_SRC)} 8192", expect="voltage 8192 EU"),
    Step(f"gt6laser reset {F(L5)}", expect="GT6 laser accounting reset"),
    Step(f"gt6energy mode {F(L5_SRC)} on", expect="emitting true"),
    Step(f"gt6laser stat {F(L5)}", expect="out 0,", sleep=2.0, poll=15.0),
    Step(f"gt6laser stat {F(L5)}", expect="half false", sleep=0.5),
    Step(f"gt6laser stat {F(L5)}", expect="in ", sleep=0.5),
    Step(f"gt6energy mode {F(L5_SRC)} off", expect="emitting false"),

    phase("D: teardown — restore the band"),
    Step(f"fill 444 62 {Z - 2} 458 68 {Z + 5} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="qu_laser laser domain",
    slug="qulaser",
    sites=gt6world.declare_sites(L1, L1_SRC, L1_W1, L1_W2, L1_ABS, L1_SINK, L5, L5_SRC),
    preferred_ports=(26582, 26592),
    game_port=26436,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
