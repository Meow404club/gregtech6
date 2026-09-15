#!/usr/bin/env python3
"""p29_w6_rocks_sticks — the surface rock/stick live acceptance chain (task
p30-w6-rocks-sticks; the p21 stoneblocks destroy-drop form).

Chain semantics (the card POC face: setblock placement + break-drop pair-check +
the support pop):

  A stone rock: setblock -> the DOWN default facing pinned (execute if block
    [facing=down]) -> `setblock air destroy` drops gt6:rock_gt_stone (the
    NBT-less default-rock face, MultiTileEntityRock.java:174) — the item-entity
    NBT id pinned via `execute if entity`.

  B flint rock: the same destroy face drops minecraft:flint (11/12 of the
    upstream NBT half, WorldgenRocks.java:63).

  C meteorite rock: 40 destroy cycles WITHOUT kills — the 3:1 rockGt:oreRaw
    lottery (WorldgenRocks.java:63 nextInt(4)==0 ? oreRaw : rockGt) must land
    BOTH gt6:rock_gt_meteoric_iron and gt6:ore_raw_meteoric_iron at least once
    (P(miss ore_raw in 40) ~ 1e-5). The loot-table weights are the committed
    transcription evidence; the live arm pins the variance.

  D stick: the destroy face drops minecraft:stick (MultiTileEntityStick
    .java:103; the biome wood band is the declared-converged deviation).

  E support pop: a rock on a stone base pops WITH its loot when the base goes
    (canSurvive + updateOrDestroy, the vanilla flower row) — block air + item
    id both pinned.

  F the loot-table display face: `loot spawn` consults the committed tables —
    the flint table prints the FLINT item name, the stick table "Stick" (the
    composed rock template rides the item names only via the material items,
    not the table).

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox
cleanup restores the sites between passes. Run with GT6_SESSION=off (the
pinned per-chain ports).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w6_rocks_sticks.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — one fresh z=300 band, x384..390 (the support-pop / loot-spawn
# faces at z=304): x/z-disjoint from every roster band (the p26 W1 z=172 band,
# the p29 W1 z=180/200/252 bands, the p29 W2 z=264..284 bands).
S = gt6world.Site(384, 64, 300, dx=1)   # the stone rock arm
F = gt6world.Site(386, 64, 300, dx=1)   # the flint rock arm
M = gt6world.Site(388, 64, 300, dx=1)   # the meteorite lottery arm
K = gt6world.Site(390, 64, 300, dx=1)   # the stick arm
P = gt6world.Site(384, 64, 304, dy=1)   # the support-pop arm (base 64 + rock 65)
L = gt6world.Site(386, 64, 304)         # the loot-spawn display face

S_POS = "384 64 300"
F_POS = "386 64 300"
M_POS = "388 64 300"
K_POS = "390 64 300"
P_BASE = "384 64 304"
P_ROCK = "384 65 304"
L_POS = "386 64 304"

ITEMS = "@e[type=minecraft:item,distance=..8,x=%d,y=64,z=%d]"
ITEM_ID = '@e[type=minecraft:item,nbt={Item:{id:"%s"}},distance=..8,x=%d,y=64,z=%d]'

steps = []

# ------------------------------------------- A: the stone rock destroy face
steps += [
    phase("A: stone rock — DOWN default facing + destroy drops gt6:rock_gt_stone"),
    Step(f"setblock {S_POS} gt6:surface_rock_stone", expect="Changed the block"),
    Step(f"execute if block {S_POS} gt6:surface_rock_stone[facing=down]", expect="Test passed"),
    Step(f"setblock {S_POS} air destroy", expect="Changed the block"),
    Step(f"execute if entity {ITEM_ID % ('gt6:rock_gt_stone', 384, 300)}", expect="Test passed"),
    Step(f"kill {ITEMS % (384, 300)}", expect="Killed"),
]

# ------------------------------------------- B: the flint rock destroy face
steps += [
    phase("B: flint rock — destroy drops minecraft:flint (the 11/12 NBT face)"),
    Step(f"setblock {F_POS} gt6:surface_rock_flint", expect="Changed the block"),
    Step(f"execute if block {F_POS} gt6:surface_rock_flint[facing=down]", expect="Test passed"),
    Step(f"setblock {F_POS} air destroy", expect="Changed the block"),
    Step(f"execute if entity {ITEM_ID % ('minecraft:flint', 386, 300)}", expect="Test passed"),
    Step(f"kill {ITEMS % (386, 300)}", expect="Killed"),
]

# --------------------------------- C: the meteorite 3:1 lottery (40 cycles)
steps += [
    phase("C: meteorite rock — 40 destroy cycles must land BOTH rockGt and oreRaw (3:1 weights)"),
]
for _cycle in range(40):
    steps += [
        Step(f"setblock {M_POS} gt6:surface_rock_meteorite", expect="Changed the block"),
        Step(f"setblock {M_POS} air destroy", expect="Changed the block"),
    ]
steps += [
    Step(f"execute if entity {ITEM_ID % ('gt6:rock_gt_meteoric_iron', 388, 300)}", expect="Test passed"),
    Step(f"execute if entity {ITEM_ID % ('gt6:ore_raw_meteoric_iron', 388, 300)}", expect="Test passed"),
    Step(f"kill {ITEMS % (388, 300)}", expect="Killed"),
]

# ------------------------------------------- D: the stick destroy face
steps += [
    phase("D: stick — destroy drops minecraft:stick (the vanilla-converged first batch)"),
    Step(f"setblock {K_POS} gt6:surface_stick", expect="Changed the block"),
    Step(f"execute if block {K_POS} gt6:surface_stick[facing=down]", expect="Test passed"),
    Step(f"setblock {K_POS} air destroy", expect="Changed the block"),
    Step(f"execute if entity {ITEM_ID % ('minecraft:stick', 390, 300)}", expect="Test passed"),
    Step(f"kill {ITEMS % (390, 300)}", expect="Killed"),
]

# ------------------------------------------- E: the support pop
steps += [
    phase("E: support pop — the base goes, the rock pops WITH its loot"),
    Step(f"setblock {P_BASE} minecraft:stone", expect="Changed the block"),
    Step(f"setblock {P_ROCK} gt6:surface_rock_stone", expect="Changed the block"),
    Step(f"setblock {P_BASE} air", expect="Changed the block"),
    Step(f"execute if block {P_ROCK} air", expect="Test passed"),
    Step(f"execute if entity {ITEM_ID % ('gt6:rock_gt_stone', 384, 304)}", expect="Test passed"),
    Step(f"kill {ITEMS % (384, 304)}", expect="Killed"),
]

# ------------------------------------------- F: the loot-table display face
steps += [
    phase("F: the committed tables — loot spawn prints the item display faces"),
    Step(f"kill {ITEMS % (386, 304)}", expect="No entity was found"),
    Step(f"loot spawn {L_POS} loot gt6:blocks/surface_rock_flint", expect="Flint"),
    Step(f"loot spawn {L_POS} loot gt6:blocks/surface_stick", expect="Stick"),
    Step(f"kill {ITEMS % (386, 304)}", expect="Killed"),
]

# ---------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore over every site (the pass-open bbox is the backstop)"),
    Step("fill 383 62 299 391 66 305 air", expect="filled"),
    Step(f"kill {ITEMS % (384, 300)}", expect="No entity was found"),
    Step(f"kill {ITEMS % (386, 300)}", expect="No entity was found"),
    Step(f"kill {ITEMS % (388, 300)}", expect="No entity was found"),
    Step(f"kill {ITEMS % (390, 300)}", expect="No entity was found"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w6-rocks-sticks",
    slug="p29w6rockssticks",
    sites=gt6world.declare_sites(S, F, M, K, P, L),
    preferred_ports=(26303, 26313),      # this card's pinned rcon/query pair (the fresh 2630x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
