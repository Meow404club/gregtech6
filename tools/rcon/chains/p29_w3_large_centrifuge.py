#!/usr/bin/env python3
"""p29-w3-large-centrifuge — the Large Centrifuge chain (task p29-w3-large-12, the
p29_w3_large12 sweep group; Loader_MultiTileEntities.java:1229).

  RU eff 5000, window 512..4096, parallel 16 + DURATION; the centrifuge.json rows —
  the heavy SNOW row (2 snow -> clay, eUt 16, dur 512; the pollable 65536-progress bar)
  and the fast snowball row (2 snowball -> clay_ball + flint, eUt 16, dur 16) for the
  batch arms (2 snowball/process x the 16 parallel = one exact 32-item batch).

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection (the form scaffold's hard-failure class)
  4 eff 5000 numeric bar: maxprogress = units(16x512x4, 5000, 10000) = 65536 at the
    4-process snow batch (the W1 units() half-speed ruling, live)
  6 the window-511 refusal (volt 511 < min 512 stalls the next batch) and the 512 resume
  5 the type gate: dialed EU the RU door refuses the waiting batch (active stays low,
    the buffer starves); the RU resume runs it out

NOTE the data-merge feeds REWRITE the whole Items list — every later merge re-includes
the accumulated output stacks (slots 1-2) or they are wiped.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_centrifuge.py
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

# the fresh z=340 band (the p29 W3 large-12 cluster): one x-disjoint column per machine
CTRL = gt6world.Site(384, 65, 340)
RIG = gt6world.Site(384, 64, 340)   # directly UNDER the controller (the free face; the
C, R = F(CTRL), F(RIG)              # upstream updateAdjacentToggleableEnergySources probe cell)
WRONG = gt6world.Site(384, 65, 342) # a ring cell the wrong-part arm poisons
W = F(WRONG)

MACHINE = "large_centrifuge"

def merge(items):
    # the loader-versioned NBT shapes (the p19 ruling: 1.20.1 Count:1b vs 1.21.1 count:1);
    # Size = the 11-slot inventory (1 input + 9 outputs + 1 special)
    return {
        "1.20.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",Count:%db}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
        "1.21.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",count:%d}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
    }

SNOW = merge([("minecraft:snow", 8)])  # the heavy row: 4 processes at dur 512 (the pollable bar)
BATCH2 = merge([("minecraft:snowball", 32), ("minecraft:clay_ball", 4), ("minecraft:flint", 4)])
BATCH3 = merge([("minecraft:snowball", 32), ("minecraft:clay_ball", 20), ("minecraft:flint", 20)])

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 381 60 337 387 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the rig under the controller, the heavy-row numeric bar"),
    # the snow row (2 snow -> clay, dur 512): 8 snow = 4 processes, the eff-5000 bar
    # = units(16x512x4, 5000, 10000) = 65536 -> 128 ticks at the 512/t rig pace
    Step(SNOW["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": SNOW["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="maxprogress: 65536L", poll=20),  # the eff-5000 numeric bar, live

    phase("C: the window-511 refusal — the next batch stalls under the window minimum"),
    Step("gt6energy volt " + R + " 511", expect="voltage 511"),
    Step(BATCH2["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": BATCH2["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),  # 511 < mInputMin 512: the :780 gate stalls
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("data get block " + C, expect="active: 1b", poll=10),  # the resume on the fresh 512 packet
    Step("data get block " + C, expect="Count: 20b", poll=30,   # batch 2 done: 4 + 16 clay stacked
         node_expects={"1.21.1": "Count: 20"}),

    phase("D: the type gate — dialed EU the RU door refuses the waiting batch"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step(BATCH3["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": BATCH3["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: EU on an RU row never runs
    Step("data get block " + C, expect="energy: 0L"),            # the starved buffer (no packet paid)
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("data get block " + C, expect="Count: 36b", poll=30,    # the resume: batch 3 stacks (20 + 16)
         node_expects={"1.21.1": "Count: 36"}),

    phase("E: teardown — the explicit band restore"),
    Step("fill 381 60 337 387 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-large-centrifuge p29_w3_large12",
    slug="p29w3largecentrifuge",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
