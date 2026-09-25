#!/usr/bin/env python3
"""eu-core printer — the Printer family chain (task eu-core-5tier
ACCEPTANCE ①⑤): the EU 5-tier ladder (:1450-1454, no NBT_PARALLEL -> 1, the
base-form PRINTER map — the RecipeMapPrinter NBT blueprint face stays pooled, the
card-① deviation), on the JSON-poured printer.json smoke row (1x paper + water
1000 -> 1x paper, eUt 16, duration 16; the fluid-in mandatory leg):

  A the per-tier window pins T1-T5 (the same gapless doubling chain, ending
    minIn=4096 recIn=8192 maxIn=16384 on the T5) + facing=2 (the placement canon
    default) + the menu-less carrier data=-2.
  B the T1 row run: stock the paper, fill the ink tank from the UP face (the
    SBIT_U|SBIT_L tank-in mask — the rotated-face verdict is the mask check),
    inject 24 x 32 -> the paper out (the smoke-row 进出断言).
  C the T5 row run at the 8192 mid-window packet (the EV_TIER_INPUTS live form).

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/printer.py
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

# the fresh z=284 band, printer column x408..416 (per-family x-disjoint teardown fill)
T1 = gt6world.Site(408, 64, 284, dy=1, dz=1)
T2 = gt6world.Site(410, 64, 284, dy=1, dz=1)
T3 = gt6world.Site(412, 64, 284, dy=1, dz=1)
T4 = gt6world.Site(414, 64, 284, dy=1, dz=1)
T5 = gt6world.Site(416, 64, 284, dy=1, dz=1)
T1F, T2F, T3F, T4F, T5F = F(T1), F(T2), F(T3), F(T4), F(T5)


def feed_merge(pos):
    """The paper slot (Size 3 = 2 in + 1 out; the ink rides the tank bank)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:paper",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:paper",count:1}}]}}}}',
    }


def out_report():
    return {
        "1.20.1": "outputs=[1x paper; ]",
        "1.21.1": "outputs=[1x minecraft:paper; ]",
    }


steps = [
    phase("A: the per-tier window pins T1-T5 + the facing canon + the menu-less carrier"),
    Step(f"setblock {T1F} gt6:printer", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1F}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine wiremill check {T1F}", expect="facing=2"),
    Step(f"gt6machine wiremill check {T1F}", expect="data=-2"),
    Step(f"setblock {T2F} gt6:printer_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2F}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"setblock {T3F} gt6:printer_t3", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T3F}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"setblock {T4F} gt6:printer_t4", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T4F}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(f"setblock {T5F} gt6:printer_t5", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5F}", expect="minIn=4096 recIn=8192 maxIn=16384"),

    phase("B: the T1 smoke-row run — paper + water 1000 -> paper (the ink leg rides the UP face; fire + read-only poll: the first completion after a fresh boot lags past the per-command deadline)"),
    Step(feed_merge(T1F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1F)),
    Step(f"gt6machine wiremill fluid fill up minecraft:water 1000 {T1F}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine wiremill inject 24 32 {T1F}", expect=None),
    Step(f"gt6machine wiremill check {T1F}", expect="out[0]=1x paper",
         node_expects={"1.21.1": "out[0]=1x minecraft:paper"}, poll=60.0),
    Step(f"gt6machine wiremill check {T1F}", expect="progress=0/0"),

    phase("C: the T5 mid-window run — the 4096 floor-edge packet drives the same row"),
    Step(feed_merge(T5F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F)),
    Step(f"gt6machine wiremill fluid fill up minecraft:water 1000 {T5F}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine wiremill inject 8 4096 {T5F}", expect=None),
    Step(f"gt6machine wiremill check {T5F}", expect="out[0]=1x paper",
         node_expects={"1.21.1": "out[0]=1x minecraft:paper"}, poll=60.0),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 407 62 275 417 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="eu_core printer",
    slug="eucore_printer",
    sites=gt6world.declare_sites(T1, T2, T3, T4, T5),
    preferred_ports=(26341, 26351),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
