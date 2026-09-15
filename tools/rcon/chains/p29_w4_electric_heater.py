#!/usr/bin/env python3
"""p29-w4-eu-bridge (heater leg) — the Electric Heater EU->HU live acceptance chain
(task p29-w4-eu-bridge RCON group p29_w4_eu_bridge; the p29_w3_large_dynamo rig
geometry: the /gt6energy dial at z-1, the machine facing=south (the BACK face north toward the dial seat), the consumer east of
the emission face).

  A the T1 delivery leg: the EU dial (volt 32 = the NBT_INPUT rec column) behind the
    heater, the STEAM BOILER TANK on the HU emission face (the p13 W2 firebox arm's
    consumer: the tank's Root-attachable HU door takes a horizontal packet) with
    4000 L water aboard — the /gt6bridge reset + the dial on -> the stat pins
    "half true" (the persisted accounting pair: in == 2*out EXACTLY, the units()
    half-rate; the WASTE vent pins the per-tick pairing) and the boiler stat pins
    the steam produced (the heat actually landed).
  B the cross-domain arms (ACCEPTANCE ②): the dial re-typed KU then RU behind the
    heater -> the stat pins "in 0" (the EU-only input arm refuses outright).
  C the waste arm: the emission face walled (boiler swapped for stone) -> "out 0"
    with a live intake (the WASTE_ENERGY=T vent, nothing banked).
  D the T5 rung: a second column, dial volt 8192 -> "half true" (the ladder's top
    rung live; the 32/16 pair is rung 1).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_electric_heater.py
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

Z = 324  # the fresh band east of the card-1 chemicals band (z=320)
H1 = gt6world.Site(446, 65, Z)        # the T1 heater
H1_SRC = gt6world.Site(446, 65, Z - 1)   # the EU dial (the dynamo-rig source seat)
H1_SINK = gt6world.Site(446, 65, Z + 1)  # the boiler tank on the HU face
H5 = gt6world.Site(452, 65, Z)        # the T5 heater
H5_SRC = gt6world.Site(452, 65, Z - 1)
H5_SINK = gt6world.Site(452, 65, Z + 1)

steps = [
    phase("A: the T1 delivery leg — EU 32 in, HU 16 out, the boiler eats the heat"),
    Step(f"setblock {F(H1)} gt6:electric_heater[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(H1_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(H1_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(H1_SRC)} 32", expect="voltage 32 EU"),
    Step(f"setblock {F(H1_SINK)} gt6:steam_boiler_tank_lead", expect="Changed the block"),
    Step(f"gt6boiler fill {F(H1_SINK)} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6bridge reset {F(H1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(H1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(H1)}", expect="half true", sleep=3.0, poll=15.0),
    Step(f"gt6boiler stat {F(H1_SINK)}", expect="barometer=", sleep=1.0),

    phase("B: the cross-domain arms — KU and RU offers are refused outright"),
    Step(f"gt6energy mode {F(H1_SRC)} off", expect="emitting false"),
    Step(f"gt6energy type {F(H1_SRC)} KU", expect="type ENERGY.KINETIC_PUSH"),
    Step(f"gt6bridge reset {F(H1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(H1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(H1)}", expect="in 0,", sleep=2.0),
    Step(f"gt6energy type {F(H1_SRC)} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6bridge reset {F(H1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6bridge stat {F(H1)}", expect="in 0,", sleep=2.0),

    phase("C: the waste arm — the emission face walled, the intake still pays"),
    Step(f"gt6energy mode {F(H1_SRC)} off", expect="emitting false"),
    Step(f"gt6energy type {F(H1_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"setblock {F(H1_SINK)} minecraft:stone", expect="Changed the block"),
    Step(f"gt6bridge reset {F(H1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(H1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(H1)}", expect="out 0,", sleep=2.0),
    Step(f"gt6bridge stat {F(H1)}", expect="half false", sleep=0.5),
    Step(f"gt6energy mode {F(H1_SRC)} off", expect="emitting false"),

    phase("D: the T5 rung — 8192 EU in, 4096 HU out"),
    Step(f"setblock {F(H5)} gt6:electric_heater_t5[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(H5_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(H5_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(H5_SRC)} 8192", expect="voltage 8192 EU"),
    Step(f"setblock {F(H5_SINK)} gt6:steam_boiler_tank_lead", expect="Changed the block"),
    Step(f"gt6boiler fill {F(H5_SINK)} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6bridge reset {F(H5)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(H5_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(H5)}", expect="half true", sleep=3.0, poll=15.0),
    Step(f"gt6energy mode {F(H5_SRC)} off", expect="emitting false"),

    phase("E: teardown — restore the band"),
    Step(f"fill 444 62 {Z - 2} 454 68 {Z + 2} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w4_eu_bridge electric heater",
    slug="p29w4heater",
    sites=gt6world.declare_sites(H1, H1_SRC, H1_SINK, H5, H5_SRC, H5_SINK),
    preferred_ports=(26401, 26411),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
