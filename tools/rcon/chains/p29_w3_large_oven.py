#!/usr/bin/env python3
"""p29-w3-large-oven — the Large Electric Oven chain (task p29-w3-large-12, the
p29_w3_large12 sweep group; Loader_MultiTileEntities.java:1236).

  EU eff 2500, window 512..4096, parallel 64 + DURATION; the 3x3x3 (Invar walls / the
  8-coil Nichrome ring around an air centre / Invar walls — the coil-type pattern is the
  standing-block probe, the :62-63 try-Nichrome-then-Carborundum two-step); the FURNACE
  vanilla bridge (sand -> glass).

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  4 the eff-2500 row of the efficiency ladder (the exact bars ride the offline
    GT6LargeMachineSemanticsTest pin; the live leg proves the vanilla-bridge consumption)
  6 the window-511 refusal (volt 511 < min 512 stalls) and the 512 resume

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_oven.py
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

CTRL = gt6world.Site(468, 65, 340)
RIG = gt6world.Site(468, 64, 340)   # directly UNDER the controller
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(468, 65, 342) # the middle-ring coil cell (the two-coil try block)
W = F(WRONG)

MACHINE = "large_electric_oven"

def merge(items):
    return {
        "1.20.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",Count:%db}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
        "1.21.1": ("data merge block " + C + " {inventory:{Size:11,Items:[" +
                   ",".join('{Slot:%db,id:"%s",count:%d}' % (i, iid, n) for i, (iid, n) in enumerate(items)) + "]}}"),
    }

FEED = merge([("minecraft:sand", 64)])

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 465 60 337 471 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the vanilla-bridge smelting through the rig under the controller"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="minecraft:glass", poll=60),  # the bridge smelts

    phase("C: the window-511 refusal — the next batch stalls under the window minimum"),
    Step("gt6energy volt " + R + " 511", expect="voltage 511"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),  # 511 < mInputMin 512: the :780 gate stalls
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("data get block " + C, expect="active: 1b", poll=10),  # the resume on the fresh 512 packet

    phase("D: the type gate — dialed HU the EU door refuses the waiting batch"),
    Step("gt6energy type " + R + " HU", expect="type ENERGY."),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: HU on an EU row
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step("data get block " + C, expect="active: 1b", poll=10),   # the EU resume

    phase("E: teardown — the explicit band restore"),
    Step("fill 465 60 337 471 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-large-oven p29_w3_large12",
    slug="p29w3largeoven",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
