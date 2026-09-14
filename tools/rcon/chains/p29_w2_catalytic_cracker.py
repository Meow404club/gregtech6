#!/usr/bin/env python3
"""p29-w2-catalytic-cracker — the Catalytic Cracker chain (task p29-w2-hu-tu-piggyback,
group p29_w2_hu_tu; the HU 4-ladder :1570-1573 over the JSON-poured catalyticcracking.json
smoke row — 1 charcoal + water 1000 -> 1 coal, eUt 32, duration 128 — the MIRROR of the
steamcracking row: the DOUBLE-VARIANT same-parameters 对拍, both maps carry the IDENTICAL
RM.java:67/:68 constants row and the IDENTICAL :1570 masks, split only by map+texture):

  T1 budget units(32x128x1, 10000, 10000, T) = 4096 -> 128 ticks @ 32 EU/tick.
  the T1 window + the full run in one command; the T2-T4 window ladder.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_catalytic_cracker.py
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

CC1 = gt6world.Site(394, 65, 280, dz=1)  # catalyticcracker T1
CC2 = gt6world.Site(396, 65, 280)        # T2
CC3 = gt6world.Site(398, 65, 280)        # T3
CC4 = gt6world.Site(400, 65, 280)        # T4

steps = [
    phase("A: the T1 catalytic cracker — the MIRROR smoke row, the 8-HU wall, the full 4096 bar"),
    Step(f"gt6machine catalyticcracker place {F(CC1)}", expect="GT6 catalyticcracker placed at 394, 65, 280"),
    Step(f"gt6machine catalyticcracker input 1 {F(CC1)}",
         expect="GT6 catalyticcracking input: 1x charcoal into slot 0",  # the report name is the MAP local (getTileEntityName), not the block literal
         node_expects={"1.21.1": "GT6 catalyticcracking input: 1x minecraft:charcoal into slot 0"}),
    Step(f"gt6machine catalyticcracker fluid fill up minecraft:water 1000 {F(CC1)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine catalyticcracker check {F(CC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine catalyticcracker inject 40 8 {F(CC1)}", expect="progress=0/0"),
    Step(f"gt6machine catalyticcracker inject 128 32 {F(CC1)}",
         expect="outputs=[1x coal; ]",
         node_expects={"1.21.1": "outputs=[1x minecraft:coal; ]"}),

    phase("B: the T2-T4 window ladder (the TIER_INPUTS per-row pin)"),
    Step(f"gt6machine catalyticcracker_t2 place {F(CC2)}", expect="GT6 catalyticcracker_t2 placed at 396, 65, 280"),
    Step(f"gt6machine catalyticcracker_t2 check {F(CC2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine catalyticcracker_t3 place {F(CC3)}", expect="GT6 catalyticcracker_t3 placed at 398, 65, 280"),
    Step(f"gt6machine catalyticcracker_t3 check {F(CC3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine catalyticcracker_t4 place {F(CC4)}", expect="GT6 catalyticcracker_t4 placed at 400, 65, 280"),
    Step(f"gt6machine catalyticcracker_t4 check {F(CC4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 393 62 278 401 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w2-catalytic-cracker p29_w2_hu_tu",
    slug="p29w2catalyticcracker",
    sites=gt6world.declare_sites(CC1, CC2, CC3, CC4),
    preferred_ports=(26342, 26352),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
