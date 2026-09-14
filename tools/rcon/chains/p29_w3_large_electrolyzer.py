#!/usr/bin/env python3
"""p29-w3-large-electrolyzer — the Large Electrolyzer chain (task p29-w3-large-12, the
p29_w3_large12 sweep group; Loader_MultiTileEntities.java:1230).

  EU eff 5000, window 512..4096, parallel 16 + DURATION; the heavy snow_block row
  (1 -> 4 snowball, eUt 16, dur 512; the pollable bar) over the electrolyzer_part shell.

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  4 eff 5000 numeric bar: maxprogress = units(16x512x4, 5000, 10000) = 65536 at the
    4-process snow_block batch (the W1 units() half-speed ruling, live)
  5 the type gate: dialed RU the EU door refuses the waiting batch; the EU resume runs it
  6 the window-511 refusal (volt 511 < min 512 stalls) and the 512 resume

NOTE the data-merge feeds REWRITE the whole Items list — the later merge re-includes the
accumulated output stack.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_electrolyzer.py
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

CTRL = gt6world.Site(396, 65, 340)
RIG = gt6world.Site(396, 64, 340)   # directly UNDER the controller
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(396, 65, 342)
W = F(WRONG)

MACHINE = "large_electrolyzer"

def merge(items):
    return {
        "1.20.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",Count:%db}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
        "1.21.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",count:%d}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
    }

SNOWB = merge([("minecraft:snow_block", 8)])  # 4 processes at dur 512 (the pollable bar)
BATCH2 = merge([("minecraft:snow_block", 8), ("minecraft:snowball", 32)])  # re-include the outputs

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 393 60 337 399 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the numeric 65536 bar"),
    Step(SNOWB["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": SNOWB["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="maxprogress: 65536L", poll=20),  # the eff-5000 numeric bar, live

    phase("C: the window-511 refusal + the batch completion"),
    Step("gt6energy volt " + R + " 511", expect="voltage 511"),
    Step(BATCH2["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": BATCH2["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),  # 511 < mInputMin 512: the :780 gate stalls
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("data get block " + C, expect="Count: 36b", poll=30,   # the resume: 4 + 32 snowball stacked
         node_expects={"1.21.1": "Count: 36"}),

    phase("D: the type gate — dialed RU the EU door refuses the waiting batch"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step(BATCH2["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": BATCH2["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: RU on an EU row
    Step("data get block " + C, expect="energy: 0L"),            # the starved buffer (no packet paid)
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step("data get block " + C, expect="Count: 68b", poll=30,    # the resume: 36 + 32 stacked
         node_expects={"1.21.1": "Count: 68"}),

    phase("E: teardown — the explicit band restore"),
    Step("fill 393 60 337 399 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-large-electrolyzer p29_w3_large12",
    slug="p29w3largeelectrolyzer",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
