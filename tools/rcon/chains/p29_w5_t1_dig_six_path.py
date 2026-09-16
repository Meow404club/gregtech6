#!/usr/bin/env python3
"""p29-w5-t1-dig-six PATH chain — the Behavior_Place_Path live arm.

Chain semantics (task p29-w5-t1-dig-six RCON arm 2, the /gt6dig use channel — the
item's own useOn with the inventory cleared so the torch arm cannot fire first):

  A the spade/shovel/universal_spade convert a vanilla grass_block into the
    vanilla-native dirt_path (upstream Behavior_Place_Path.java:50-74 over the
    BlocksGT.Paths substitute — the vanilla ShovelItem FLATTENABLES target), each
    paying one durability point (the upstream 50-unit cost folded);
  B the repeat arm: dirt_path is no longer in FLATTENABLES — the use PASSes (the
    upstream same-path return-T folds to a PASS, the report stays NO-OP, unpaid);
  C the pickaxe negative: no path arm upstream — NO-OP unpaid.

The PADDY arm is CUT (declared): upstream Behavior_Place_Paddy.java:48 gates on
IL.GrC_Paddy.exists() — the GrowthCraft block is absent in this universe, so the
upstream arm is INERT; the port keeps it inert instead of inventing a target.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t1_dig_six_path.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

P1 = gt6world.Site(389, 64, 192)   # the spade path arm
P2 = gt6world.Site(390, 64, 192)   # the shovel path arm
P3 = gt6world.Site(391, 64, 192)   # the universal spade path arm
P4 = gt6world.Site(392, 64, 192)   # the pickaxe negative

steps = []

# --------------------------------- A: the three path-carrying tools convert grass
steps += [
    phase("A: spade/shovel/universal_spade convert grass_block -> dirt_path (Behavior_Place_Path, 1 durability point)"),
    Step(f"setblock 389 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig use 389 64 192 spade",
         expect="state Block{minecraft:grass_block} -> Block{minecraft:dirt_path}, toolDamage=1/512"),
    Step(f"setblock 390 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig use 390 64 192 shovel",
         expect="state Block{minecraft:grass_block} -> Block{minecraft:dirt_path}, toolDamage=1/512"),
    Step(f"setblock 391 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig use 391 64 192 universal_spade",
         expect="state Block{minecraft:grass_block} -> Block{minecraft:dirt_path}, toolDamage=1/512"),
]

# --------------------------------- B: the repeat arm — a path is a no-op
steps += [
    phase("B: the repeat arm — dirt_path never re-converts (unpaid NO-OP)"),
    Step("gt6dig use 389 64 192 spade", expect="NO-OP"),
]

# --------------------------------- C: the pickaxe negative (no path arm upstream)
steps += [
    phase("C: the pickaxe negative — the pickaxe carries the torch arm only"),
    Step(f"setblock 392 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig use 392 64 192 pickaxe", expect="NO-OP"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 389 64 192 392 65 192 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t1-dig-six-path",
    slug="p29w5t1digsixpath",
    sites=gt6world.declare_sites(P1, P2, P3, P4),
    preferred_ports=(26196, 26206),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
