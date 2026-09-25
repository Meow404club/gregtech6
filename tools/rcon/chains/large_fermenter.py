#!/usr/bin/env python3
"""large-fermenter — the Large Fermenter chain (task large-12, the
large12 sweep group; Loader_MultiTileEntities.java:1235).

  HU, window 512/1/4096, parallel 256 + DURATION, the SIDE_BACK auto-out; the 5x5
  transmitter slab BELOW the 5x5x2 SS shell (the back mid-edge cells design 7); the
  fermenter.json wheat row (wheat + water 1000 -> sugar, eUt 16, dur 128) — the
  2048-progress bar dialed down to the 2/t pace = 1024 ticks, the long pollable window.

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  2 parallel 256 + DURATION numeric bar: maxprogress = units(16x128, 10000, 10000) = 2048
  5 the HU domain through the RELAYING transmitter part cell (the rig rides under the
    bottom-layer transmitter, the HeatTransmitterBlockEntity relay feeds the controller);
    the type gate: dialed RU the HU door refuses; the HU resume runs it

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/large_fermenter.py
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

CTRL = gt6world.Site(456, 65, 340)
RIG = gt6world.Site(456, 63, 340)   # UNDER the transmitter part cell at y=64 (the relay layer)
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(456, 65, 342)
W = F(WRONG)

MACHINE = "large_fermenter"

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

FEED = merge([("minecraft:wheat", 1)], fluid=("minecraft:water", 1000))

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 453 60 337 459 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the HU relay rig under the transmitter, the numeric 2048 bar"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " HU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 32", expect="voltage 32"),  # >= the bar pace floor (mMinEnergy 16); 2048/32 = 64 ticks
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="maxprogress: 2048L", poll=20),  # the eff-10000 numeric bar, live

    phase("C: the type gate — dialed RU the HU door refuses the waiting batch"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: RU on an HU row
    Step("gt6energy type " + R + " HU", expect="type ENERGY."),
    Step("data get block " + C, expect="active: 1b", poll=10),   # the HU resume
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),  # the fast finish
    Step("data get block " + C, expect="minecraft:sugar", poll=30),  # the row completes

    phase("D: teardown — the explicit band restore"),
    Step("fill 453 60 337 459 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="large-fermenter large12",
    slug="largefermenter",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
