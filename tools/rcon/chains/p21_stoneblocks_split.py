#!/usr/bin/env python3
"""p21-stoneblocks-16item-registry-split — the per-pair registry behavior face.

Chain semantics (task p21 ACCEPTANCE, the live halves the offline census cannot reach):

  A the per-pair destroy face: a NON-zero variant block (marble bricks_chiseled) setblocked
    then `setblock air destroy` drops ITS OWN variant item — the item-entity NBT id
    gt6:marble_bricks_chiseled pinned via `execute if entity`.

  B the :731 swap face (the P19 pool item this card closes): the variant-0 block
    (gt6:marble) destroyed drops the SAME STONE's COBBL variant item gt6:marble_cobble —
    the loot table's single pool entry, asserted on the dropped entity id.

  C the loot-table face (the p19 render evidence form): `loot spawn` directly consults the
    per-pair tables — gt6:blocks/marble prints "Dropped 1 [Marble Cobblestone]" (the cobble
    display name), gt6:blocks/marble_bricks_chiseled prints "Dropped 1 [Chiseled Marble]"
    (the composed per-variant name — the p20 template face live).

  D the chisel gate on the new face (the adapted p19 A/B arms, live): gt6:granite_black ->
    gt6:granite_black_bricks_chiseled (toolDamage=10000, 25 points paid) and the RM.java:508
    bricks row gt6:granite_black_bricks -> gt6:granite_black_bricks_cracked — the recipe
    legs now resolve REAL per-variant items.

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox cleanup
restores the sites between passes. Run with GT6_SESSION=off (the pinned per-chain ports).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p21_stoneblocks_split.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x 380..385, z 124..130: clear of the p13/p14 boiler chains (x 104..172),
# the p16 chains (x 184..200 / x 300..360 bands), the p19 drying rig (x 360, z 20) and
# the p19 chisel band (x 372..377, z 123..125).
M = gt6world.Site(380, 64, 124, dx=1)   # the variant destroy site
C = gt6world.Site(382, 64, 124, dx=1)   # the variant-0 (cobble swap) destroy site
L = gt6world.Site(384, 64, 124, dx=1)   # the loot spawn site
G = gt6world.Site(380, 64, 128, dx=1)   # the chisel STONE->CHISL site
K = gt6world.Site(382, 64, 128, dx=1)   # the chisel BRICK->CRACK site

M_POS = "380 64 124"
C_POS = "382 64 124"
L_POS = "384 64 124"
G_POS = "380 64 128"
K_POS = "382 64 128"

ITEMS_M = "@e[type=minecraft:item,distance=..8,x=380,y=64,z=124]"
ITEMS_C = "@e[type=minecraft:item,distance=..8,x=382,y=64,z=124]"
ITEMS_L = "@e[type=minecraft:item,distance=..8,x=384,y=64,z=124]"
# the nbt argument rides INSIDE the selector (a second [] pair after the first is a parse error)
ITEM_M_VAR = '@e[type=minecraft:item,nbt={Item:{id:"gt6:marble_bricks_chiseled"}},distance=..8,x=380,y=64,z=124]'
ITEM_C_COBBLE = '@e[type=minecraft:item,nbt={Item:{id:"gt6:marble_cobble"}},distance=..8,x=382,y=64,z=124]'

steps = []

# ------------------------------------------- A: the per-pair destroy face
steps += [
    phase("A: the variant block destroy — marble bricks_chiseled drops its OWN variant item"),
    Step(f"setblock {M_POS} gt6:marble_bricks_chiseled", expect="Changed the block"),
    Step(f"execute if block {M_POS} gt6:marble_bricks_chiseled", expect="Test passed"),
    Step(f"setblock {M_POS} air destroy", expect="Changed the block"),
    Step(f"execute if entity {ITEM_M_VAR}", expect="Test passed"),
    Step(f"kill {ITEMS_M}", expect="Killed"),
]

# ------------------------------------------- B: the :731 cobble swap face
steps += [
    phase("B: the variant-0 block destroy — gt6:marble drops the same stone's COBBL item gt6:marble_cobble"),
    Step(f"setblock {C_POS} gt6:marble", expect="Changed the block"),
    Step(f"execute if block {C_POS} gt6:marble", expect="Test passed"),
    Step(f"setblock {C_POS} air destroy", expect="Changed the block"),
    Step(f"execute if entity {ITEM_C_COBBLE}", expect="Test passed"),
    Step(f"kill {ITEMS_C}", expect="Killed"),
]

# ------------------------------------------- C: the loot-table face
steps += [
    phase("C: the per-pair tables — gt6:blocks/marble yields the cobble item, the chiseled table yields itself"),
    Step(f"loot spawn {L_POS} loot gt6:blocks/marble", expect="Dropped 1 [Marble Cobblestone]"),
    Step(f"loot spawn {L_POS} loot gt6:blocks/marble_bricks_chiseled", expect="Dropped 1 [Chiseled Marble]"),
    Step(f"kill {ITEMS_L}", expect="Killed"),
]

# ------------------------------------------- D: the chisel gate on the new face
steps += [
    phase("D: the chisel gate — granite_black stone -> its chiseled pair item block, 25 points paid"),
    Step(f"setblock {G_POS} gt6:granite_black", expect="Changed the block"),
    Step(f"gt6chisel click {G_POS}",
         expect="toolDamage=10000, state Block{gt6:granite_black} -> "
                "Block{gt6:granite_black_bricks_chiseled}, chiselDamage=25/512"),
    Step(f"execute if block {G_POS} gt6:granite_black_bricks_chiseled", expect="Test passed"),
    Step(f"setblock {K_POS} gt6:granite_black_bricks", expect="Changed the block"),
    Step(f"gt6chisel click {K_POS}",
         expect="toolDamage=10000, state Block{gt6:granite_black_bricks} -> "
                "Block{gt6:granite_black_bricks_cracked}, chiselDamage=25/512"),
    Step(f"execute if block {K_POS} gt6:granite_black_bricks_cracked", expect="Test passed"),
]

# ---------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore over every site (the pass-open bbox is the backstop)"),
    Step("fill 379 62 123 386 66 129 air", expect="filled"),
    Step(f"kill {ITEMS_M}", expect="No entity was found"),
    Step(f"kill {ITEMS_C}", expect="No entity was found"),
    Step(f"kill {ITEMS_L}", expect="No entity was found"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p21-stoneblocks-16item-registry-split",
    slug="p21stonesplit",
    sites=gt6world.declare_sites(M, C, L, G, K),
    preferred_ports=(26203, 26213),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
