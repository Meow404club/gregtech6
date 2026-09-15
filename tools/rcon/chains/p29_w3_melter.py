#!/usr/bin/env python3
"""p29-w3-melter — the Melter HU single live acceptance chain (task
p29-w3-heat-smelter, the p29_w3_smelter shape over the ONE :1657 row —
ANY.Iron, RM.Melter, ice 1 -> water 1000 L, eut 16 / duration 2000):

  the window pin: TIER_INPUTS[0] = 16/32/64 (the NBT_INPUT 32 column), the
  check report pins parallel=1000 parallelDuration=true (the :1657 columns).
  the dead-packet wall: 40x 8 HU packets never cross mInputMin 16 -> 0/0.
  the run: units(16 x 2000, 10000, 10000, T) = 32000 bar -> 500 ticks @ 64 ->
  the fluid stat pins out[0]=1000 L of minecraft:water.

passes=2 is the idempotency proof. teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_melter.py
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

ME1 = gt6world.Site(416, 65, 304, dz=1)  # the Melter single

steps = [
    phase("A: the Melter — the ice row, the 8-HU wall, then the 32000 bar in one command"),
    Step(f"gt6machine melter place {F(ME1)}", expect="GT6 melter placed at 416, 65, 304"),
    Step(f"gt6machine melter input 1 {F(ME1)}",
         expect="GT6 melter input: 1x ice into slot 0",
         node_expects={"1.21.1": "GT6 melter input: 1x minecraft:ice into slot 0"}),
    Step(f"gt6machine melter check {F(ME1)}",
         expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine melter check {F(ME1)}",
         expect="parallel=1000 parallelDuration=true"),
    Step(f"gt6machine melter inject 40 8 {F(ME1)}", expect="progress=0/0"),
    Step(f"gt6machine melter inject 500 64 {F(ME1)}", expect="used=500 progress=0/0 energy=0"),
    Step(f"gt6machine melter fluid stat {F(ME1)}", expect="out[0]=1000 L of minecraft:water"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 414 62 302 418 68 306 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-melter p29_w3_heat_smelter",
    slug="p29w3melter",
    sites=gt6world.declare_sites(ME1),
    preferred_ports=(26402, 26412),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
