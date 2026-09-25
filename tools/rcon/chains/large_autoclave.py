#!/usr/bin/env python3
"""large-autoclave — the Large Autoclave chain (task large-12, the
large12 sweep group; Loader_MultiTileEntities.java:1232).

  TU self-generating, window 1..16, parallel 16, NO_CONSTANT_POWER; the 3x3x3 hollow
  of dense SS walls ONE ABOVE the controller; the autoclave.json kelp row (kelp +
  bone meal + water 500 -> 2 kelp, eUt 16, dur 128) — the 2048-progress bar at the
  1/tick self-generation pace = 102 s, the long pollable active window.

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  3 the NO_CONSTANT_POWER cycle: the stopped toggle parks the bar mid-run (active drops,
    maxprogress 2048 survives the gap), the restart resumes the run

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/large_autoclave.py
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

CTRL = gt6world.Site(420, 65, 340)
RIG = gt6world.Site(420, 64, 340)   # the under-controller cell: FREE (the shell sits above)
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(420, 65, 342) # a shell cell of the hollow
W = F(WRONG)

MACHINE = "large_autoclave"

def merge(items, fluid=None):
    # the tank shapes are loader-versioned too (the 21.1 FluidStack codec keys vs the
    # forge FluidName/Amount pair — the FluidTankGT read forks per leg)
    tank = {"1.20.1": 'input_tank:{FluidName:"%s",Amount:%d}' % fluid if fluid else "",
            "1.21.1": 'input_tank:{id:"%s",amount:%d}' % fluid if fluid else ""}
    out = {}
    for k in ("1.20.1", "1.21.1"):
        parts = ",".join(
            '{Slot:%db,id:"%s",%s:%d%s}' % (i, iid, "Count" if k == "1.20.1" else "count", n, "b" if k == "1.20.1" else "")
            for i, (iid, n) in enumerate(items))
        payload = "inventory:{Size:11,Items:[" + parts + "]}"
        if fluid:
            payload += "," + tank[k]
        out[k] = "data merge block " + C + " {" + payload + "}"
    return out

FEED = merge([("minecraft:kelp", 1), ("minecraft:bone_meal", 1)], fluid=("minecraft:water", 500))
RESTART = {k: "data merge block " + C + " {stopped:0b}" for k in ("1.20.1", "1.21.1")}
STOP = {k: "data merge block " + C + " {stopped:1b}" for k in ("1.20.1", "1.21.1")}

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 417 60 337 423 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the TU self-generation, the 2048-tick kelp row"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 1b", poll=60),        # the self-generated run
    Step("data get block " + C, expect="maxprogress: 2048L", poll=20),  # the 16x128 TU-constant bar

    phase("C: the NO_CONSTANT_POWER cycle — the stopped-toggle gap parks the bar"),
    Step(STOP["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": STOP["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),        # the gap
    Step("data get block " + C, expect="maxprogress: 2048L", poll=10),  # the PARKED bar survives
    Step(RESTART["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": RESTART["1.21.1"]}),
    Step("data get block " + C, expect="minecraft:kelp", poll=180),   # the restart runs the row out: 2 kelp land (the 2048-tick batch at the 1/t self-gen pace)

    phase("D: teardown — the explicit band restore"),
    Step("fill 417 60 337 423 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="large-autoclave large12",
    slug="largeautoclave",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
