#!/usr/bin/env python3
"""f1-chemicals (gas leg) — the Gas Turbine chain over the TRUE FM.Gas rows
(task f1-chemicals, RCON group chemicals; the P29 W3 natural-gas shape
with the methane row of the shipped gas_fuels.json, Loader_Fuels.java:161):

  A the FUEL arm: the gas_turbine_magnalium forms over its 35 dense walls; the input
    tank is live-merged with gt6:methane (the forge FluidName/Amount shape, the 21.1
    codec node override — the natural_gas fixture ruling); the methane row (5 L, -64
    EUt x 30) burns in the deficit-ceiling parallel and the RU packet leaves through
    the far plate into a flux_dynamo_t5 sink (gt.last_out: 8192, the same
    LIMIT_CONSUMPTION cap the W3 chain pinned). BOTH exhaust legs assert: the water
    6 (tank 0) AND the restored gt6:carbondioxide 3 (tank 1) — the P29 W4 closure
    makes the upstream byproduct live.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/chemicals_gas_turbine.py
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

Z = 320
XF = 464  # the fuel column (cracker arm at x448..458 — x-disjoint within the fresh z=320 band)

def ctrl(x):   return F(gt6world.Site(x, 65, Z))
def sink(x):   return F(gt6world.Site(x, 65, Z + 4))

SITES = [
    gt6world.Site(XF, 65, Z + 1, dx=2, dy=1, dz=4),
]

FUEL_MERGE = 'data merge block %s {gt.tank:{FluidName:"gt6:methane",Amount:6144}}'
FUEL_MERGE_1211 = 'data merge block %s {gt.tank:{id:"gt6:methane",amount:6144}}'

steps = [
    phase("A: the fuel arm — the methane FM.Gas row burns into the LIMIT_CONSUMPTION-capped RU"),
    Step(f"setblock {ctrl(XF)} gt6:gas_turbine_magnalium[facing=north]", expect="Changed the block"),
    Step(f"gt6multiblock form {ctrl(XF)}", expect="formed=true okay=true"),
    Step(f"gt6multiblock form {ctrl(XF)}", expect="first_failed_cell=none"),
    Step(f"setblock {sink(XF)} gt6:flux_dynamo_t5[facing=south]", expect="Changed the block"),
    Step(FUEL_MERGE % ctrl(XF), expect="Modified block data",
         node_cmds={"1.21.1": FUEL_MERGE_1211 % ctrl(XF)}),
    Step(f"data get block {ctrl(XF)}", expect="gt.last_out: 8192L", sleep=1.5, poll=15.0),
    Step(f"data get block {ctrl(XF)}", expect="gt.limit_consumption: 1b"),
    Step(f"data get block {ctrl(XF)}", expect="minecraft:water", sleep=1.0),
    Step(f"data get block {ctrl(XF)}", expect="gt6:carbondioxide", sleep=1.0),

    phase("B: teardown — restore the band"),
    Step(f"fill {XF - 2} 63 {Z - 2} {XF + 2} 68 {Z + 5} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="chemicals-gas-turbine chemicals",
    slug="chemicalsgasturbine",
    sites=gt6world.declare_sites(*SITES),
    preferred_ports=(26343, 26353),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
