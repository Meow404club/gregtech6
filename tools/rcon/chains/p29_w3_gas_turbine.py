#!/usr/bin/env python3
"""p29-w3-turbine-dynamo (gas leg) — the Gas Turbine chain (task
p29-w3-turbine-dynamo RCON group p29_w3_turbine_dynamo, the FM.Gas 行跑通 +
LIMIT_CONSUMPTION leg).

Two columns of the fresh z=300 band:

  A the FUEL arm: the gas_turbine_magnalium forms over its 35 dense walls; the
    input tank is live-merged with gt6:natural_gas (data merge — the forge
    FluidName/Amount shape with the 21.1 codec node override, the
    p12-fluid-item-carrier key-shape ruling); the FM.Gas row (the datapack
    gas_fuels.json, Loader_Fuels.java:169 transcription) burns in the deficit
    ceiling parallel and the RU packet leaves through the far plate into a
    flux_dynamo_t5 sink. The steady emit is 8192 = outMax — the LIVE
    LIMIT_CONSUMPTION cap (the ceil ceiling overshoots the window, the :70 cap
    holds the packet; the offline suite pins the same math). The row flags ride
    the persisted config mirror (gt.limit_consumption: 1b / gt.waste_energy: 0b)
    and the water byproduct lands in the exhaust tank.

  B the HU arm: the second column burns nothing — the /gt6energy HU dial at the
    NBT_INPUT window centre charges the capacitor; the idle drain floors it each
    tick until the stockpile reaches the full window, whose clamp leg then
    converts exactly 8192 per tick ("hacking my own code",
    LargeTurbineGas.java:108-115).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_gas_turbine.py
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

Z = 300
XF, XH = 448, 464  # the fuel column, the HU column (between the steam and dynamo columns)

def ctrl(x):   return F(gt6world.Site(x, 65, Z))
def src(x):    return F(gt6world.Site(x, 65, Z - 1))
def sink(x):   return F(gt6world.Site(x, 65, Z + 4))

SITES = [
    gt6world.Site(XF, 65, Z + 1, dx=2, dy=1, dz=4),
    gt6world.Site(XH, 65, Z + 1, dx=2, dy=1, dz=4),
]

FUEL_MERGE = 'data merge block %s {gt.tank:{FluidName:"gt6:natural_gas",Amount:6144}}'
FUEL_MERGE_1211 = 'data merge block %s {gt.tank:{id:"gt6:natural_gas",amount:6144}}'

steps = [
    phase("A: the fuel arm — the FM.Gas row burns natural gas into the LIMIT_CONSUMPTION-capped RU"),
    Step(f"setblock {ctrl(XF)} gt6:gas_turbine_magnalium[facing=north]", expect="Changed the block"),
    Step(f"gt6multiblock form {ctrl(XF)}", expect="formed=true okay=true"),
    Step(f"gt6multiblock form {ctrl(XF)}", expect="first_failed_cell=none"),
    Step(f"setblock {sink(XF)} gt6:flux_dynamo_t5[facing=south]", expect="Changed the block"),
    Step(FUEL_MERGE % ctrl(XF), expect="Modified block data",
         node_cmds={"1.21.1": FUEL_MERGE_1211 % ctrl(XF)}),
    Step(f"data get block {ctrl(XF)}", expect="gt.last_out: 8192L", sleep=1.5, poll=15.0),
    Step(f"data get block {ctrl(XF)}", expect="gt.limit_consumption:1b"),
    Step(f"data get block {ctrl(XF)}", expect="gt.waste_energy:0b"),
    Step(f"data get block {ctrl(XF)}", expect="minecraft:water", sleep=1.0),

    phase("B: the HU arm — the window-charge clamp leg over the idle drain"),
    Step(f"setblock {ctrl(XH)} gt6:gas_turbine_magnalium[facing=north]", expect="Changed the block"),
    Step(f"gt6multiblock form {ctrl(XH)}", expect="formed=true okay=true"),
    Step(f"gt6multiblock form {ctrl(XH)}", expect="first_failed_cell=none"),
    Step(f"setblock {sink(XH)} gt6:flux_dynamo_t5[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {src(XH)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {src(XH)} HU", expect="type ENERGY.HEAT"),
    Step(f"gt6energy volt {src(XH)} 12288", expect="voltage 12288 EU"),
    Step(f"gt6energy mode {src(XH)} on", expect="emitting true"),
    Step(f"data get block {ctrl(XH)}", expect="gt.last_out: 8192L", sleep=1.5, poll=15.0),

    phase("C: teardown — restore the band"),
    Step(f"fill {XF - 2} 63 {Z - 2} {XH + 2} 68 {Z + 5} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w3_turbine_dynamo gas turbine",
    slug="p29w3gasturbine",
    sites=gt6world.declare_sites(*SITES),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
