#!/usr/bin/env python3
"""large-squeezer — the Large Squeezer chain (task large-12, the
large12 sweep group; Loader_MultiTileEntities.java:1240).

  RU eff 5000, window 512..4096, parallel 64 + DURATION + NO_CONSTANT_POWER; the 5x5x3
  open basin (the 12-wall middle ring, the interior and mid-edges OPEN); the heavy melon
  row (melon_slice -> stick + water 100, eUt 16, dur 512) — 8 melon = 8 processes, the
  131072-progress bar at the 512/t rig pace = 256 ticks, plus the FLUID-output leg
  (the water lands in the output tank).

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  3 the NO_CONSTANT_POWER cycle (the rig-off gap parks the bar, the restart runs it out)
  the fluid-output leg: the row's water lands in the output tank (the output_tank NBT)

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/large_squeezer.py
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

CTRL = gt6world.Site(516, 65, 340)
RIG = gt6world.Site(516, 64, 340)   # directly UNDER the controller
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(516, 65, 342)
W = F(WRONG)

MACHINE = "large_squeezer"

def merge(items):
    return {
        "1.20.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",Count:%db}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
        "1.21.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",count:%d}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
    }

FEED = merge([("minecraft:melon_slice", 8)])

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 513 60 337 519 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the numeric 131072 bar over the open basin"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="maxprogress: 131072L", poll=20),  # the eff-5000 numeric bar, live

    phase("C: the NO_CONSTANT_POWER cycle — the rig-off gap parks the bar"),
    Step("gt6energy mode " + R + " off", expect="emitting false"),
    Step("data get block " + C, expect="active: 0b", poll=10),
    Step("data get block " + C, expect="maxprogress: 131072L", poll=10),  # the PARKED bar survives
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="active: 1b", poll=10),
    Step("data get block " + C, expect='output_tank: {FluidName: "minecraft:water"', poll=120,  # the batch runs out: the sticks land AND the 800 L water hits the output tank
         node_expects={"1.21.1": "minecraft:water"}),  # 21.1: the codec face renders first ({id, amount, ...})

    phase("D: the type gate — dialed EU the RU door refuses the waiting batch"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: EU on an RU row
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("data get block " + C, expect="active: 1b", poll=10),   # the RU resume

    phase("E: teardown — the explicit band restore"),
    Step("fill 513 60 337 519 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="large-squeezer large12",
    slug="largesqueezer",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
