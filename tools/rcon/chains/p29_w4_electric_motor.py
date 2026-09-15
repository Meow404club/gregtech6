#!/usr/bin/env python3
"""p29-w4-eu-bridge (motor leg) — the Electric Motor EU->RU live acceptance chain
(task p29-w4-eu-bridge RCON group p29_w4_eu_bridge; the gt6rcon diesel->axle 判例
geometry — the RU consumer the port's rotation system already runs on).

  A the T1 delivery leg: the EU dial (volt 32) behind the motor, the wood-small axle
    (axis=z, the north-south run) on the RU emission face — /gt6bridge stat pins
    "half true" (the axle ACCEPTED the RU packets: the live half-rate, in == 2*out
    EXACTLY over the run window; the WASTE vent pins the per-tick pairing).
  B the T3 rung: a second column at volt 512 -> "half true" (the mid-ladder live).
  C the waste arm: the axle swapped for stone -> "out 0" + "half false" with a live
    intake (nothing banked, nothing emitted).
  D the cross-domain input arms: KU then HU dials behind the motor -> "in 0".

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_electric_motor.py
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

Z = 324
M1 = gt6world.Site(464, 65, Z)        # the T1 motor
M1_SRC = gt6world.Site(464, 65, Z - 1)   # the EU dial
M1_SINK = gt6world.Site(464, 65, Z + 1)  # the RU axle
M3 = gt6world.Site(470, 65, Z)        # the T3 motor
M3_SRC = gt6world.Site(470, 65, Z - 1)
M3_SINK = gt6world.Site(470, 65, Z + 1)

steps = [
    phase("A: the T1 delivery leg — EU 32 in, RU 16 out, the axle spins"),
    Step(f"setblock {F(M1)} gt6:electric_motor[facing=north]", expect="Changed the block"),
    Step(f"gt6energy place {F(M1_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(M1_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(M1_SRC)} 32", expect="voltage 32 EU"),
    Step(f"setblock {F(M1_SINK)} gt6:axle_wood_treated_small[axis=z]", expect="Changed the block"),
    Step(f"gt6bridge reset {F(M1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(M1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(M1)}", expect="half true", sleep=3.0, poll=15.0),

    phase("B: the T3 rung — 512 EU in, 256 RU out"),
    Step(f"setblock {F(M3)} gt6:electric_motor_t3[facing=north]", expect="Changed the block"),
    Step(f"gt6energy place {F(M3_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(M3_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(M3_SRC)} 512", expect="voltage 512 EU"),
    Step(f"setblock {F(M3_SINK)} gt6:axle_wood_treated_small[axis=z]", expect="Changed the block"),
    Step(f"gt6bridge reset {F(M3)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(M3_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(M3)}", expect="half true", sleep=3.0, poll=15.0),
    Step(f"gt6energy mode {F(M3_SRC)} off", expect="emitting false"),

    phase("C: the waste arm — the axle walled, the intake still pays"),
    Step(f"setblock {F(M1_SINK)} minecraft:stone", expect="Changed the block"),
    Step(f"gt6bridge reset {F(M1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(M1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(M1)}", expect="out 0,", sleep=2.0),
    Step(f"gt6bridge stat {F(M1)}", expect="half false", sleep=0.5),
    Step(f"gt6energy mode {F(M1_SRC)} off", expect="emitting false"),

    phase("D: the cross-domain input arms — KU then HU offers refused"),
    Step(f"gt6bridge reset {F(M1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy type {F(M1_SRC)} KU", expect="type ENERGY.KINETIC_PUSH"),
    Step(f"gt6energy mode {F(M1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(M1)}", expect="in 0,", sleep=2.0),
    Step(f"gt6energy type {F(M1_SRC)} HU", expect="type ENERGY.HEAT"),
    Step(f"gt6bridge reset {F(M1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6bridge stat {F(M1)}", expect="in 0,", sleep=2.0),
    Step(f"gt6energy mode {F(M1_SRC)} off", expect="emitting false"),

    phase("E: teardown — restore the band"),
    Step(f"fill 462 62 {Z - 2} 472 68 {Z + 2} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w4_eu_bridge electric motor",
    slug="p29w4motor",
    sites=gt6world.declare_sites(M1, M1_SRC, M1_SINK, M3, M3_SRC, M3_SINK),
    preferred_ports=(26401, 26411),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
