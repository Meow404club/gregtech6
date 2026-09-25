#!/usr/bin/env python3
"""eu-bridge (roasting leg) — the Roasting Oven 4-ladder live acceptance chain
(task eu-bridge; the smelter chain shape — the /gt6machine arms over
the Boudouard row of roasting.json):

  A the T1 run: coal dust 1 + CO2 864 -> CO 1152 (the Loader_Recipes_Chem.java:400
    pair, U = 144), 16 EUt x 16 — the HU bottom-door inject, the fluid fill through
    the BACK door (the :1386 NBT_TANK_SIDE_IN SBIT_B|SBIT_L), the fluid stat pins
    out[0]=1152 L of gt6:carbonmonoxide. The dead-packet wall first: 40x 8 HU never
    cross mInputMin 16 -> 0/0.
  B the window ladder (the ACCEPTANCE-③ per-row pin): T1 16/32/64 parallel 1,
    T2 64/128/256 parallel 2, T3 256/512/1024 parallel 4, T4 1024/2048/4096
    parallel 8 — parallelDuration=false on every tier (the :1386-1389 rows carry
    NO NBT_PARALLEL_DURATION key).

passes=2 is the idempotency proof. teardown: the explicit band restore.
Run:  GT6_SESSION=off python3 tools/rcon/chains/roasting_oven.py
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

R1 = gt6world.Site(476, 65, 324, dz=1)   # roasting T1
R2 = gt6world.Site(480, 65, 324)         # T2
R3 = gt6world.Site(484, 65, 324)         # T3
R4 = gt6world.Site(488, 65, 324)         # T4

steps = [
    phase("A: the T1 Boudouard run — the 8-HU wall, then the CO 1152 L out"),
    Step(f"gt6machine roasting_oven place {F(R1)}", expect="GT6 roasting_oven placed at 476, 65, 324"),
    # the report name is the BE's tile-entity name ("roasting_oven"), the item the bare
    # path on 1.20.1 (the cracker form); the fill side is the WORLD south = the machine
    # BACK for the north-facing placer canon (the :1386 NBT_TANK_SIDE_IN SBIT_B)
    Step(f"gt6machine roasting_oven input 1 {F(R1)}",
         expect="GT6 roaster input: 1x dust_coal into slot 0",  # the report name is the BE tile-entity name ("roaster"), not the block path
         node_expects={"1.21.1": "GT6 roaster input: 1x gt6:dust_coal into slot 0"}),
    Step(f"gt6machine roasting_oven fluid fill south gt6:carbondioxide 864 {F(R1)}",
         expect="filled 864/864 L of gt6:carbondioxide (ACCEPTED), input tanks hold 864 L"),  # the tank takes exactly the row amount
    Step(f"gt6machine roasting_oven check {F(R1)}",
         expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine roasting_oven check {F(R1)}",
         expect="parallel=1 parallelDuration=false"),
    Step(f"gt6machine roasting_oven inject 40 8 {F(R1)}", expect="progress=0/0"),
    Step(f"gt6machine roasting_oven inject 500 64 {F(R1)}", expect="used=500 progress=0/0 energy=0"),
    Step(f"gt6machine roasting_oven fluid stat {F(R1)}", expect="out[0]=1152 L of gt6:carbonmonoxide"),

    phase("B: the window ladder — the per-row pin + the parallel 1/2/4/8 对拍"),
    Step(f"gt6machine roasting_oven_t2 place {F(R2)}", expect="GT6 roasting_oven_t2 placed at 480, 65, 324"),
    Step(f"gt6machine roasting_oven_t2 check {F(R2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine roasting_oven_t2 check {F(R2)}", expect="parallel=2 parallelDuration=false"),
    Step(f"gt6machine roasting_oven_t3 place {F(R3)}", expect="GT6 roasting_oven_t3 placed at 484, 65, 324"),
    Step(f"gt6machine roasting_oven_t3 check {F(R3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine roasting_oven_t3 check {F(R3)}", expect="parallel=4 parallelDuration=false"),
    Step(f"gt6machine roasting_oven_t4 place {F(R4)}", expect="GT6 roasting_oven_t4 placed at 488, 65, 324"),
    Step(f"gt6machine roasting_oven_t4 check {F(R4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(f"gt6machine roasting_oven_t4 check {F(R4)}", expect="parallel=8 parallelDuration=false"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 474 62 322 490 68 326 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="eu_bridge roasting oven",
    slug="roasting",
    sites=gt6world.declare_sites(R1, R2, R3, R4),
    preferred_ports=(26401, 26411),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
