#!/usr/bin/env python3
"""p19-chisel-recipes — the chisel universal-gate live acceptance chain.

Chain semantics (task p19-chisel-recipes ACCEPTANCE ③, the ToolCompat.java:224-229
transcription driven live — the P16 same-source convention: /gt6chisel click runs the
exact GTChiselItem.stoneToolClick the item's useOn gate arm runs, with a fake player
holding a REAL gt6:chisel so the durability payment is observable):

  A the GT stone conversion arm (the RM.java:470 row, STONE -> CHISL): place
    gt6:granite_black (the per-pair variant-0 id, p21 re-key) -> click -> the report pins
    toolDamage=10000, the block pair gt6:granite_black -> gt6:granite_black_bricks_chiseled
    and chiselDamage=25/512 (the Behavior_Tool :63 conversion of the 10000 return at
    mDamage=25), and the execute-if-block predicate pins the blockstate on the world side.

  B the vanilla arms (Loader_Recipes_Vanilla.java:772-773): minecraft:stone ->
    minecraft:chiseled_stone_bricks and minecraft:stone_bricks ->
    minecraft:cracked_stone_bricks, both toolDamage=10000.

  C the sneak negative (the :224 !aSneaking gate): the same stone variant, the click
    carrying `sneak` -> toolDamage=0, chiselDamage=0, the blockstate UNCHANGED.

  D the no-recipe negative: minecraft:diorite -> toolDamage=0, no conversion (the book
    has no diorite row), and the chiseled output has no reverse row (stone variant of
    the chiseled bricks block target) — the book is one-way.

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox cleanup
restores the sites between passes. Run with GT6_SESSION=off (the pinned per-chain ports;
the session model ignores chain.preferred_ports — the p16-aqua-fluids lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p19_chisel.py
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

# The sites — x 372..378, z 124..130: clear of the p13/p14 boiler chains (x 104..172),
# the p16 chains (x 184..200 / x 300..360 bands) and the p19 drying rig (x 360, z 20).
#   G — the GT stone conversion site (372, 64, 124)
#   V — the vanilla pair site (374, 64, 124)
#   N — the negative-arm site (376, 64, 124)
G = gt6world.Site(372, 64, 124, dx=1)
V = gt6world.Site(374, 64, 124, dx=1)
N = gt6world.Site(376, 64, 124, dx=1)

G_POS = "372 64 124"
V_POS = "374 64 124"
N_POS = "376 64 124"

steps = []

# ------------------------------------------- A: the GT stone conversion arm (:470)
# (ids re-keyed by p21-stoneblocks-16item-registry-split: the blockstate variant
# property retired — one block+item id per (stone, variant), variant 0 keeps the
# bare snake, CHISL is its own gt6:granite_black_bricks_chiseled)
steps += [
    phase("A: the GT stone arm — gt6:granite_black chiseled to gt6:granite_black_bricks_chiseled, 25 points paid"),
    Step(f"setblock {G_POS} gt6:granite_black", expect="Changed the block"),
    Step(f"execute if block {G_POS} gt6:granite_black", expect="Test passed"),
    Step(f"gt6chisel click {G_POS}",
         expect="toolDamage=10000, state Block{gt6:granite_black} -> "
                "Block{gt6:granite_black_bricks_chiseled}, chiselDamage=25/512"),
    Step(f"execute if block {G_POS} gt6:granite_black_bricks_chiseled", expect="Test passed"),
    # the CHISL target has no reverse row (the book is one-way; the TE-face CHISEL_MAPPINGS
    # self-map is the pooled BlockStones.java:573-576 face, not this gate)
    Step(f"gt6chisel click {G_POS}", expect="toolDamage=0"),
]

# ------------------------------------------------ B: the vanilla pair (:772-:773)
steps += [
    phase("B: the vanilla arms — stone -> chiseled stone bricks, stone bricks -> cracked"),
    Step(f"setblock {V_POS} minecraft:stone", expect="Changed the block"),
    Step(f"gt6chisel click {V_POS}", expect="toolDamage=10000"),
    Step(f"execute if block {V_POS} minecraft:chiseled_stone_bricks", expect="Test passed"),
    # the chiseled output has no reverse row either
    Step(f"gt6chisel click {V_POS}", expect="toolDamage=0"),
    Step(f"setblock {V_POS} minecraft:stone_bricks", expect="Changed the block"),
    Step(f"gt6chisel click {V_POS}", expect="toolDamage=10000"),
    Step(f"execute if block {V_POS} minecraft:cracked_stone_bricks", expect="Test passed"),
]

# ------------------------------------------- C: the sneak negative (:224 !aSneaking)
steps += [
    phase("C: the sneak arm — a sneaking click declines, the blockstate stays, nothing is paid"),
    Step(f"setblock {N_POS} gt6:granite_black", expect="Changed the block"),
    Step(f"gt6chisel click {N_POS} sneak",
         expect="toolDamage=0, state Block{gt6:granite_black} -> "
                "Block{gt6:granite_black}, chiselDamage=0/512"),
    Step(f"execute if block {N_POS} gt6:granite_black", expect="Test passed"),
]

# -------------------------------------------------- D: the no-recipe negatives
steps += [
    phase("D: the negative arms — diorite has no row; the book answers nothing, pays nothing"),
    Step(f"setblock {N_POS} minecraft:diorite", expect="Changed the block"),
    Step(f"gt6chisel click {N_POS}", expect="toolDamage=0"),
    Step(f"execute if block {N_POS} minecraft:diorite", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("T: teardown — the explicit restore over every site (the pass-open bbox is the backstop)"),
    Step("fill 371 62 123 373 66 125 air", expect="filled"),
    Step("fill 373 62 123 375 66 125 air", expect="filled"),
    Step("fill 375 62 123 377 66 125 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p19-chisel-recipes",
    slug="p19chisel",
    sites=gt6world.declare_sites(G, V, N),
    preferred_ports=(26003, 26013),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
