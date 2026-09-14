#!/usr/bin/env python3
"""p29-w3-large-coagulator — the Large Coagulator Array chain (task p29-w3-large-12,
the p29_w3_large12 sweep group; Loader_MultiTileEntities.java:1231).

  TU self-generating, window 1..16, parallel 64, NO_CONSTANT_POWER; the coagulator.json
  water row (1000 L -> snowball, eUt 1, dur 64) — the 64-progress bar at the exact
  1/tick self-generation pace = 64 ticks, the pollable active window.

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  3 the NO_CONSTANT_POWER cycle: the stopped toggle kills the self-generation (active
    drops), the parked bar (maxprogress 64) SURVIVES the gap, the restart completes the
    row (the exact progress-vs-reset semantics ride the offline
    GT6LargeMachineSemanticsTest pin — the multiblock machines carry no /gt6machine arm)

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_coagulator.py
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

CTRL = gt6world.Site(408, 65, 340)
RIG = gt6world.Site(408, 64, 340)   # the under-controller cell: FREE on this machine
C, R = F(CTRL), F(RIG)              # (the reserved site — no rig is placed, the TU row
WRONG = gt6world.Site(408, 65, 342) # self-generates)
W = F(WRONG)

MACHINE = "large_coagulator"

def merge(items, fluid=None):
    out = {}
    for k in ("1.20.1", "1.21.1"):
        parts = ",".join(
            '{Slot:%db,id:"%s",%s:%d%s}' % (i, iid, "Count" if k == "1.20.1" else "count", n, "b" if k == "1.20.1" else "")
            for i, (iid, n) in enumerate(items))
        payload = "inventory:{Size:11,Items:[" + parts + "]}"
        if fluid:
            payload += ',input_tank:{FluidName:"%s",Amount:%d}' % fluid
        out[k] = "data merge block " + C + " {" + payload + "}"
    return out

FEED = merge([], fluid=("minecraft:water", 1000))
RESTART = {
    "1.20.1": "data merge block " + C + " {stopped:0b}",
    "1.21.1": "data merge block " + C + " {stopped:0b}",
}
STOP = {
    "1.20.1": "data merge block " + C + " {stopped:1b}",
    "1.21.1": "data merge block " + C + " {stopped:1b}",
}

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 405 60 337 411 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the TU self-generation, the 64-tick water row"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 1b", poll=20),        # the self-generated run (the placed machine starts un-stopped)
    Step("data get block " + C, expect="maxprogress: 64L", poll=20),  # the 1x64 TU-constant bar

    phase("C: the NO_CONSTANT_POWER cycle — the stopped-toggle gap parks the bar"),
    Step(STOP["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": STOP["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),      # the gap: the generation is off
    Step("data get block " + C, expect="maxprogress: 64L", poll=10),  # the PARKED bar survives (the retain face)
    Step(RESTART["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": RESTART["1.21.1"]}),
    Step("data get block " + C, expect="active: 1b", poll=10),      # the restart
    Step("data get block " + C, expect="minecraft:snowball", poll=30),  # the row completes after the gap

    phase("D: teardown — the explicit band restore"),
    Step("fill 405 60 337 411 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-large-coagulator p29_w3_large12",
    slug="p29w3largecoagulator",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
