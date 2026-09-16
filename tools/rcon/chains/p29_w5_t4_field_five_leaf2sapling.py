#!/usr/bin/env python3
"""p29-w5-t4-field-five BRANCH_CUTTER chain — the oak-leaf-to-sapling live arm.

Chain semantics (task p29-w5-t4-field-five RCON arm 3, the /gt6field break channel —
the BRANCHCUTTER_LEAVES GLM over the loot seam, upstream
GT_Tool_BranchCutter.java:84-86):

  A the conversion: one oak leaf breaks into EXACTLY [minecraft:oak_sapling x1]
    (count 1) — the vanilla sapling/apple/stick roll is REPLACED;
  B the declared fortune-0 deviation (:86): upstream rolls the apple 1-in-9 at
    fortune 0; the port FLOORS the arm to no apple (aFortune > 0 gate) — the count-1
    sapling-only drops list is the deviation verdict (no minecraft:apple, no stick).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t4_field_five_leaf2sapling.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

LEAF = gt6world.Site(410, 64, 225, dx=1, dy=2, dz=1)  # the single leaf + clear headroom

steps = []

# --------------------------------- A+B: the deterministic sapling replacement
steps += [
    phase("A: the branch cutter turns one oak leaf into exactly its sapling (fortune 0, no apple)"),
    Step("fill 409 62 224 411 67 226 minecraft:air", expect="filled"),
    Step("setblock 410 64 225 minecraft:oak_leaves", expect="Changed the block"),
    Step("gt6field break 410 64 225 branch_cutter",
         expect="neighbours 1 -> 0 (broke 1), drops=[minecraft:oak_sapling x1] (count 1), "
                "toolDamage=1/128"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 409 62 224 411 67 226 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t4-field-five-leaf2sapling",
    slug="p29w5t4fieldfiveleaf2sapling",
    sites=gt6world.declare_sites(LEAF),
    preferred_ports=(26204, 26214),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
