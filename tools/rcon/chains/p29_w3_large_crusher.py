#!/usr/bin/env python3
"""p29-w3-large-crusher — the Large Crusher chain (task p29-w3-large-12, the
p29_w3_large12 sweep group; Loader_MultiTileEntities.java:1238).

  RU eff 5000, window 512..4096, parallel 64 + DURATION + NO_CONSTANT_POWER; the 5x5x3
  tungstensteel basin (the wheel filling 3x3x2, the two perpendicular mid-edge ENERGY_IN
  walls per facing); the heavy granite row (granite -> cobblestone, eUt 16, dur 512) —
  64 granite (the slot cap) = 64 processes, the 1048576-progress bar at the 512/t rig
  pace = 2048 ticks, the long pollable window.

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  3 the NO_CONSTANT_POWER cycle: the rig-off gap parks the bar (active drops, the buffer
    drains, maxprogress SURVIVES the gap), the rig restart runs the batch out (the exact
    progress-vs-reset semantics ride the offline GT6LargeMachineSemanticsTest pin)

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_crusher.py
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

CTRL = gt6world.Site(492, 65, 340)
RIG = gt6world.Site(492, 64, 340)   # directly UNDER the controller
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(492, 65, 342)
W = F(WRONG)

MACHINE = "large_crusher"

def merge(items):
    return {
        "1.20.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",Count:%db}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
        "1.21.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",count:%d}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
    }

FEED = merge([("minecraft:granite", 64)])

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 489 60 337 495 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the numeric 524288 bar over the wheel basin"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="maxprogress: 1048576L", poll=30),  # the eff-5000 numeric bar, live (units(16x512x64, 5000, 10000))

    phase("C: the NO_CONSTANT_POWER cycle — the rig-off gap parks the bar"),
    Step("gt6energy mode " + R + " off", expect="emitting false"),
    Step("data get block " + C, expect="active: 0b", poll=10),            # the gap: the buffer drains, the run stops
    Step("data get block " + C, expect="maxprogress: 1048576L", poll=10),  # the PARKED bar survives
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="active: 1b", poll=10),            # the restart resumes the run
    Step("gt6energy volt " + R + " 4096", expect="voltage 4096"),         # the fast finish (the window max): 64 procs x 8192 bar / 4096 = 128 ticks
    Step("data get block " + C, expect="Count: 64b", poll=90,             # 64 cobblestone out (one stack)
         node_expects={"1.21.1": "Count: 64"}),

    phase("D: the type gate — dialed EU the RU door refuses the waiting batch"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: EU on an RU row
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("data get block " + C, expect="active: 1b", poll=10),   # the RU resume

    phase("E: teardown — the explicit band restore"),
    Step("fill 489 60 337 495 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-large-crusher p29_w3_large12",
    slug="p29w3largecrusher",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
