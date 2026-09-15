#!/usr/bin/env python3
"""p29-w5-t1-dig-six OPENABLE chain — the universal-spade openableCrowbar Unboxinator
walk live arm.

Chain semantics (task p29-w5-t1-dig-six RCON arm 6, the /gt6dig mine channel — the GLM
gt6:universal_spade_openable, mode UNBOXINATOR_OPEN, routes every drop through
GT6RecipeMaps.UNBOXINATOR.findRecipe; upstream GT_Tool_UniversalSpade.java:98-115):

  A the POSITIVE arms: the universal spade mines iron/gold/diamond blocks — the walk
    runs and, with the CURRENT row set (the map smoke row only — no storage-block
    unpack rows exist yet), every drop rides through UNCHANGED: the identity. The arm
    is structural: when unpack rows land, these drops swap for the recipe outputs
    with ZERO code change (the consumer contract on GT6ToolLootModifiers);
  B the never-lost face: no arm may lose a drop (the walk's invariant);
  C the cross-tool negative: the construction pickaxe does NOT run the openable walk
    (the gt6:holds_tool identity) — the same block drops itself by the vanilla table.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t1_dig_six_openable.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

O1 = gt6world.Site(417, 64, 192)   # the iron block
O2 = gt6world.Site(418, 64, 192)   # the gold block
O3 = gt6world.Site(419, 64, 192)   # the diamond block

steps = []

# --------------------------------- A: the universal walk over the openable set
steps += [
    phase("A: universal_spade mines the openable blocks — the identity (no unpack rows yet)"),
    Step(f"setblock 417 64 192 minecraft:iron_block", expect="Changed the block"),
    Step("gt6dig mine 417 64 192 universal_spade", expect="drops=[minecraft:iron_block x1]"),
    Step(f"setblock 418 64 192 minecraft:gold_block", expect="Changed the block"),
    Step("gt6dig mine 418 64 192 universal_spade", expect="drops=[minecraft:gold_block x1]"),
    Step(f"setblock 419 64 192 minecraft:diamond_block", expect="Changed the block"),
    Step("gt6dig mine 419 64 192 universal_spade", expect="drops=[minecraft:diamond_block x1]"),
]

# --------------------------------- C: the cross-tool negative
steps += [
    phase("C: the construction pickaxe on an openable block — vanilla table, no walk"),
    Step(f"setblock 417 64 192 minecraft:iron_block", expect="Changed the block"),
    Step("gt6dig mine 417 64 192 pickaxe_construction", expect="drops=[minecraft:iron_block x1]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 417 64 192 419 64 192 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t1-dig-six-openable",
    slug="p29w5t1digsixopenable",
    sites=gt6world.declare_sites(O1, O2, O3),
    preferred_ports=(26200, 26210),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
