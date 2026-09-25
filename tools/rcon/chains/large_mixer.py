#!/usr/bin/env python3
"""large-mixer — the Large Batch Mixer chain (task large-12, the
large12 sweep group; Loader_MultiTileEntities.java:1234).

  RU eff 10000, window 512..4096, parallel 256 + DURATION; the 3x3x2 SS shell (the
  bottom OUT / top IN / the energy centre column); the C-Foam static row (6 dust_stone +
  2 dust_sand + dust_small_clay + water 1000 -> cfoam, eUt 16, dur 128 — the P29 W1
  mixer_pair live-proven item set).

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  5 the type gate: dialed EU the RU door refuses the waiting batch; the RU resume runs it
  6 the window-511 refusal (volt 511 < min 512 stalls) and the 512 resume

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/large_mixer.py
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

CTRL = gt6world.Site(444, 65, 340)
RIG = gt6world.Site(444, 64, 340)   # directly UNDER the controller
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(444, 65, 342)
W = F(WRONG)

MACHINE = "large_batch_mixer"

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

FEED = merge([("gt6:dust_stone", 6), ("gt6:dust_sand", 2), ("gt6:dust_small_clay", 1)],
             fluid=("minecraft:water", 1000))

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 441 60 337 447 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the C-Foam row through the rig under the controller"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="Amount: 1000", poll=30),  # batch 1: the cfoam lands in the output tank

    phase("C: the window-511 refusal — the next batch stalls under the window minimum"),
    Step("gt6energy volt " + R + " 511", expect="voltage 511"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),  # 511 < mInputMin 512: the :780 gate stalls
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("data get block " + C, expect="Amount: 2000", poll=30),  # the resume: batch 2 completes

    phase("D: the type gate — dialed EU the RU door refuses the waiting batch"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: EU on an RU row
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("data get block " + C, expect="Amount: 3000", poll=30),   # the RU resume: batch 3 completes

    phase("E: teardown — the explicit band restore"),
    Step("fill 441 60 337 447 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="large-mixer large12",
    slug="largemixer",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
