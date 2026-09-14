#!/usr/bin/env python3
"""p29-w3-large-sluice — the Large Sluice chain (task p29-w3-large-12, the
p29_w3_large12 sweep group; Loader_MultiTileEntities.java:1237).

  RU eff 5000, window 512..4096, parallel 64 + DURATION; the 7x3x3 titanium trough
  (the centre line OUT / the far-row energy walls / the sluice-part input top); the
  heavy gravel row (gravel + water 100 -> clay_ball, eUt 16, dur 512) — 8 gravel =
  8 processes, the 131072-progress bar at the 512/t rig pace = 256 ticks.

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection
  4 eff 5000 numeric bar: maxprogress = units(16x512x8, 5000, 10000) = 131072
  5 the type gate: dialed EU the RU door refuses the waiting batch; the RU resume runs it
  6 the window-511 refusal (volt 511 < min 512 stalls) and the 512 resume

NOTE the data-merge feeds REWRITE the whole Items list — the later merge re-includes the
accumulated output stack (the water rides the input_tank key, unaffected).

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_large_sluice.py
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

CTRL = gt6world.Site(480, 65, 340)
RIG = gt6world.Site(480, 64, 340)   # directly UNDER the controller
C, R = F(CTRL), F(RIG)
WRONG = gt6world.Site(480, 65, 342)
W = F(WRONG)

MACHINE = "large_sluice"

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

FEED = merge([("minecraft:gravel", 8)], fluid=("minecraft:water", 1000))
REFILL = merge([("minecraft:gravel", 8), ("minecraft:clay_ball", 8)],
               fluid=("minecraft:water", 1000))

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 477 60 337 483 70 352 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the run — the numeric 131072 bar over the trough"),
    Step(FEED["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED["1.21.1"]}),
    Step("gt6energy place " + R, expect="GT6 energy source placed"),
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("gt6energy mode " + R + " on", expect="emitting true"),
    Step("data get block " + C, expect="maxprogress: 131072L", poll=20),  # the eff-5000 numeric bar, live

    phase("C: the window-511 refusal + the batch completion"),
    Step("gt6energy volt " + R + " 511", expect="voltage 511"),
    Step(REFILL["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": REFILL["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),  # 511 < mInputMin 512: the :780 gate stalls
    Step("gt6energy volt " + R + " 512", expect="voltage 512"),
    Step("data get block " + C, expect="Count: 16b", poll=60,   # the resume: 8 + 8 clay stacked
         node_expects={"1.21.1": "Count: 16"}),

    phase("D: the type gate — dialed EU the RU door refuses the waiting batch"),
    Step("gt6energy type " + R + " EU", expect="type ENERGY."),
    Step(REFILL["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": REFILL["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=10),   # the cross-type refusal: EU on an RU row
    Step("gt6energy type " + R + " RU", expect="type ENERGY."),
    Step("data get block " + C, expect="Count: 24b", poll=60,    # the resume: 16 + 8 stacked
         node_expects={"1.21.1": "Count: 24"}),

    phase("E: teardown — the explicit band restore"),
    Step("fill 477 60 337 483 70 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-large-sluice p29_w3_large12",
    slug="p29w3largesluice",
    sites=gt6world.declare_sites(CTRL, RIG),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
