#!/usr/bin/env python3
"""t3 machine face — the pincers dragon-egg arm (task
machine-face-four ACCEPTANCE ③, the softhammer-chain sibling; the
/gt6machineface collect stand-in drives the item's own useOn through a SNEAKING fake
player — the vanilla teleport owns the non-sneak click, DragonEggBlock.use/:32).

Chain semantics:

  A the give arm: gt6:pincers lands as a real stack in a chest.

  B the collect arm (the canCollect face, GT_Tool_Pincers.java:105 + the Material.dragonEgg
     isMinableBlock arm :108-110): the dragon egg pops as EXACTLY ONE item entity (counted,
     discarded for rerun idempotency — the egg's loot table minecraft:blocks/dragon_egg
     drops itself unconditionally), the block clears to air, and the pincers pay ONE
     durability point (pincersDamage=1 in the report).

  C the teardown. passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/machine_face_pincers.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=292 band — x 400..404 (this chain), x-disjoint from the softhammer
# column (x384..388) and the magnifying-glass column (x392..396).
CHEST = "402 64 292"
EGG = "402 64 290"
SITE = gt6world.Site(402, 64, 290, dy=2, dz=4)

steps = []

# ------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — gt6:pincers lands as a real stack"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST} container.0 with gt6:pincers 1", expect="Replaced"),
    Step(f"data get block {CHEST} Items[0]", expect="gt6:pincers"),
]

# ------------------------------------------------- B: the collect arm
steps += [
    phase("B: the collect arm — the sneaking pincers pop the dragon egg"),
    Step(f"setblock {EGG} minecraft:dragon_egg", expect="Changed the block"),
    Step(f"gt6machineface collect {EGG}", expect="drops=[minecraft:dragon_egg x1], egg_cleared=true"),
    Step(f"execute if block {EGG} minecraft:air run time query daytime", expect="The time is"),
    Step(f"data get block {CHEST} Items[0]", expect="gt6:pincers"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the column restored to air"),
    Step("fill 400 62 288 404 67 293 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="machine-face-pincers",
    slug="pincers",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26522, 26532),      # this card's pinned rcon/query pair (the fresh 2652x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
