#!/usr/bin/env python3
"""t3 machine face — the magnifying-glass zero-change arm (task
machine-face-four ACCEPTANCE ③, the softhammer-chain sibling; the
/gt6machineface inspect stand-in drives the item's own useOn through a fake player).

Chain semantics:

  A the give arm: gt6:magnifying_glass lands as a real stack in a chest.

  B the zero-change arm (the acceptance's 方块/实体零变更断言): the inspect arm FAILS
     unless the blockstate, the item-entity set AND the held stack (count 1, damage 0)
     are all untouched — over a plain stone block AND over a chest (a vanilla-use block:
     the PASS must not open or alter it). result=PASS is the non-consumption verdict;
     the AHA/HMM villager sound is the arm's only observable (server broadcast).

  C the teardown. passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/machine_face_magnglass.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=292 band — x 392..396 (this chain), x-disjoint from the softhammer
# column (x384..388) and the pincers column (x400..404).
CHEST = "394 64 292"
STONE = "394 64 290"
CHESTY = "394 64 288"
SITE = gt6world.Site(394, 64, 290, dy=2, dz=4)

steps = []

# ------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — gt6:magnifying_glass lands as a real stack"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST} container.0 with gt6:magnifying_glass 1", expect="Replaced"),
    Step(f"data get block {CHEST} Items[0]", expect="gt6:magnifying_glass"),
]

# ------------------------------------------------- B: the zero-change arm
steps += [
    phase("B: the zero-change arm — stone and a vanilla-use block, nothing moves"),
    Step(f"setblock {STONE} minecraft:stone", expect="Changed the block"),
    Step(f"gt6machineface inspect {STONE}", expect="inspect check OK", node_expects={"1.21.1": "inspect check OK"}),
    Step(f"execute if block {STONE} minecraft:stone run time query daytime", expect="The time is"),
    Step(f"setblock {CHESTY} minecraft:chest", expect="Changed the block"),
    Step(f"gt6machineface inspect {CHESTY}", expect="result=PASS"),
    Step(f"execute if block {CHESTY} minecraft:chest run time query daytime", expect="The time is"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the column restored to air"),
    Step("fill 392 62 288 396 67 293 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="machine-face-magnglass",
    slug="magnglass",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26521, 26531),      # this card's pinned rcon/query pair (the fresh 2652x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
