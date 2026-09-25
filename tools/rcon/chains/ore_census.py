#!/usr/bin/env python3
"""ore_census — the P30 ore wave close-out card's live census chain (task
ore-5-census; the ore registration card 1 fresh_boot form + the rocks_sticks
destroy-drop form).

Three faces, all in ONE reused cell (steps run strictly sequentially, so a single spot
serves every arm; the item-entity selectors are scoped to that spot and each arm ends
with its own kill, so family-invariant ids like ore_raw_copper can never cross-contaminate):

  A PLACEMENT MATRIX — all 26 families x 3 representative axis materials (Copper /
    Cinnabar / Nikolite, axis first / mid / last) placed as the normal form and pinned
    with `execute if block <pos> gt6:ore_<family>_<material>` (GTOreBlock is a plain
    Block — no blockstate properties, so the id check IS the state check). 78 cells.
  B BREAK-DROP PAIRING — every family x 1 sample (copper normal block), three mining
    conditions against the committed loot dispatch (GT6OreLootTables.java:117-141,
    the Drops.java:74 arm-ification):
      - fortune 0 bare: `setblock air destroy` drops the BROKEN-PAIR item for the 22
        three-form families, SELF for the 4 broken==normal loose dust families;
      - silk touch: drops SELF (every family);
      - fortune III: drops the OP.oreRaw item (gt6:ore_raw_copper) — never self/broken.
    The silk/fortune tools ride `loot spawn mine <pos> <tool>` (an RCON session holds
    no item); the tool NBT is the one per-node command fork (1.20.1 tag NBT vs the
    21.1 component syntax).
  C FORTUNE COUNT DISTRIBUTION — 12 fortune-III rolls on one block (ore_stone_copper),
    each Count captured via `data get ... Item.Count` into a scoreboard and summed:
    uniform_bonus_count mult 1 at fortune 3 = 1+nextInt(4) per roll (Drops.java:74),
    mean 30; the gate `matches 17..48` fails the no-bonus flat line (12) and any
    per-roll overflow past the 1..4 cap. The exact 1..4 distribution stays the offline
    parity pin (GT6OreLootParityTest); the live arm pins that the bonus FIRES.

The fresh boot's slice carries the three registration lines (ledger 1 live:
"GT6 registered 3922 ore blocks / 3922 ore block items / 1 ore creative tab") — the
runner greps them out of this chain's boot log as the census summary.

Known non-defect (declared): the live break arms show XP=0 — the family XP column is a
BLOCK-side attachment (getExpDrop/DropExperienceBlock), ore-4's declared carry for the
block files card, not a loot face.

The card's acceptance legs run this chain through tools/rcon/census_ore.py (NORMAL
world + the fixed seed 6131000569321125127 + world delete — the P30 worldgen card
form); the roster/sweep path boots flat and is equally fine — the chain only needs
its own support stone, placed as step 1 of every pass (the S14 gravity lesson).

Run:  python3 tools/rcon/chains/ore_census.py [--node 1.21.1-neoforge]
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# ---------------------------------------------------------------------------
# the census matrix (GT6OreBlocksRegistrationTest's SNAKES/kinds, mirrored):
# 26 families, walk order; the 4 dust families are broken==normal (their bare
# arm drops SELF, not the broken pair); 3 representative materials.
# ---------------------------------------------------------------------------
FAMILIES = (
    "stone", "deepslate", "netherrack", "endstone", "sandstone",
    "gravel", "sand", "redsand", "mud",
    "blackgranite", "redgranite", "basalt", "marble", "limestone", "granite",
    "diorite", "andesite", "komatiite", "greenschist", "blueschist",
    "kimberlite", "quartzite", "lightprismarine", "darkprismarine", "slate",
    "shale")
LOOSE = {"gravel", "sand", "redsand", "mud"}          # broken==normal families
MATERIALS = ("copper", "cinnabar", "nikolite")        # axis first / mid / last
SAMPLE = "copper"                                     # the drop-pairing sample

POS = "384 64 400"                                     # the single reused census cell
SPOT = dict(x=384, y=64, z=400)
ITEMS_NEAR = f"@e[type=minecraft:item,distance=..4,x={SPOT['x']},y={SPOT['y']},z={SPOT['z']}]"
ITEM_ID = (f"@e[type=minecraft:item,nbt={{Item:{{id:\"%s\"}}}},"
           f"distance=..4,x={SPOT['x']},y={SPOT['y']},z={SPOT['z']}]")

# the per-node tool forks: 1.20.1 tag NBT vs the 21.1 component syntax
# (1.21.1 ItemEnchantments FULL_CODEC keeps the "levels" field)
SILK_PICK = "minecraft:diamond_pickaxe{Enchantments:[{id:\"minecraft:silk_touch\",lvl:1s}]}"
FORTUNE_PICK = "minecraft:diamond_pickaxe{Enchantments:[{id:\"minecraft:fortune\",lvl:3s}]}"
NEO_SILK_PICK = 'minecraft:diamond_pickaxe[enchantments={levels:{"minecraft:silk_touch":1}}]'
NEO_FORTUNE_PICK = 'minecraft:diamond_pickaxe[enchantments={levels:{"minecraft:fortune":3}}]'


def place_normal(family, material):
    """setblock + execute if block — the placement-matrix cell (normal form)."""
    block = f"gt6:ore_{family}_{material}"
    return [
        Step(f"setblock {POS} {block}", expect="Changed the block"),
        Step(f"execute if block {POS} {block}", expect="Test passed"),
    ]


steps = []

# ------------------------------------------------------------- S: support stone
# the S14 gravity lesson: every falling family row rests on a support stone placed
# AFTER the pass-open bbox cleanup stripped the cell.
steps += [
    phase("S: support stone under the census cell (the S14 falling-block lesson)"),
    Step(f"setblock {SPOT['x']} 63 {SPOT['z']} minecraft:stone", expect="Changed the block"),
]

# ------------------------------------------------- A: the placement matrix (78 cells)
steps += [
    phase("A: placement matrix — 26 families x 3 representative materials, normal form, "
          "execute if block pins every cell (GTOreBlock: no blockstate properties)"),
]
for family in FAMILIES:
    for material in MATERIALS:
        steps += place_normal(family, material)
steps += [
    Step(f"setblock {POS} air", expect="Changed the block"),  # leave the matrix cell
]

# ------------------------------------- B: the break-drop pairing (26 x 3 arms)
steps += [
    phase("B: break-drop pairing — per family x copper sample; fortune 0 bare -> "
          "broken pair (loose: self), silk -> self, fortune III -> oreRaw"),
]
for family in FAMILIES:
    bare_drop = f"gt6:ore_{family}_{SAMPLE}" if family in LOOSE \
        else f"gt6:ore_broken_{family}_{SAMPLE}"
    block = f"gt6:ore_{family}_{SAMPLE}"
    silk = Step(f"loot spawn 384 65 400 mine {POS} {SILK_PICK}", expect="Dropped",
                node_cmds={"1.21.1": f"loot spawn 384 65 400 mine {POS} {NEO_SILK_PICK}"})
    fortune = Step(f"loot spawn 384 65 400 mine {POS} {FORTUNE_PICK}", expect="Dropped",
                   node_cmds={"1.21.1": f"loot spawn 384 65 400 mine {POS} {NEO_FORTUNE_PICK}"})
    steps += [
        phase(f"B/{family}: bare -> {bare_drop} | silk -> gt6:ore_{family}_{SAMPLE} | "
              "fortune III -> gt6:ore_raw_copper"),
        # bare arm (fortune 0): the real destroy path
        Step(f"kill {ITEMS_NEAR}", allow_failed=True),
        *place_normal(family, SAMPLE),
        Step(f"setblock {POS} air destroy", expect="Changed the block"),
        Step(f"execute if entity {ITEM_ID % bare_drop}", expect="Test passed"),
        Step(f"kill {ITEMS_NEAR}", expect="Killed"),
        # silk arm
        Step(f"setblock {POS} {block}", expect="Changed the block"),
        silk,
        Step(f"execute if entity {ITEM_ID % f'gt6:ore_{family}_{SAMPLE}'}", expect="Test passed"),
        Step(f"kill {ITEMS_NEAR}", expect="Killed"),
        # fortune III arm (the id face) — the silk arm left the block placed, and
        # setblock refuses the same-state no-op ("Could not set the block"), so no
        # re-placement here: the loot mine consults the standing block
        fortune,
        Step(f"execute if entity {ITEM_ID % 'gt6:ore_raw_copper'}", expect="Test passed"),
        Step(f"kill {ITEMS_NEAR}", expect="Killed"),
    ]

# --------------------------------------- C: the fortune count distribution face
steps += [
    phase("C: fortune III count distribution — 12 rolls on ore_stone_copper, the "
          "per-roll Count summed; 1+nextInt(4) per roll means 30 mean, the gate "
          "17..48 rejects the no-bonus flat line (12) and any cap overflow"),
    # allow_failed: pass 2 re-adds — "already exists" is the idempotency face, the
    # following players set proves the objective
    Step("scoreboard objectives add orecensus dummy", allow_failed=True),
    Step("scoreboard players set oreTotal orecensus 0", expect="oreTotal"),
]
for roll in range(12):
    steps += [
        Step(f"kill {ITEMS_NEAR}", allow_failed=True),
        # setblock refuses the same-state no-op: rolls 2..12 clear the standing
        # block first (roll 1 lands on the previous B family's block — a change)
        *([Step(f"setblock {POS} air", expect="Changed the block")] if roll else []),
        Step(f"setblock {POS} gt6:ore_stone_copper", expect="Changed the block"),
        Step(f"loot spawn 384 65 400 mine {POS} {FORTUNE_PICK}", expect="Dropped",
             node_cmds={"1.21.1": f"loot spawn 384 65 400 mine {POS} {NEO_FORTUNE_PICK}"}),
        # roll 1 also pins the dropped id face via the subset NBT match
        *([Step(f"execute if entity {ITEM_ID % 'gt6:ore_raw_copper'}", expect="Test passed")]
          if roll == 0 else []),
        Step(f"execute store result score oreC orecensus run data get entity "
             f"@e[type=minecraft:item,distance=..4,x={SPOT['x']},y={SPOT['y']},z={SPOT['z']},limit=1] "
             f"Item.Count 1",
             expect="Item.Count",
             node_cmds={"1.21.1": f"execute store result score oreC orecensus run data get entity "
                                  f"@e[type=minecraft:item,distance=..4,x={SPOT['x']},y={SPOT['y']},z={SPOT['z']},limit=1] "
                                  f"Item.count 1"},
             node_expects={"1.21.1": "Item.count"}),
        Step("scoreboard players operation oreTotal orecensus += oreC orecensus",
             expect="oreTotal"),
    ]
steps += [
    Step(f"kill {ITEMS_NEAR}", expect="Killed"),
    Step("execute if score oreTotal orecensus matches 17..48", expect="Test passed"),
]

# ---------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step(f"setblock {POS} air", expect="Changed the block"),
    Step(f"setblock {SPOT['x']} 63 {SPOT['z']} air", expect="Changed the block"),
    Step(f"kill {ITEMS_NEAR}", allow_failed=True),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="ore-census",
    slug="ore_census",
    # one small site: the single reused census cell + the support stone (the
    # sequential reuse makes 78 cells one 5x5x5 bbox — the tightest cleanup
    # footprint on the roster)
    sites=gt6world.declare_sites(gt6world.Site(384, 64, 400, dy=1)),
    preferred_ports=(26308, 26318),      # this card's pinned rcon/query pair (the 2630x segment)
    fresh_boot=True,                     # the registration-log trio belongs to the fresh boot's slice
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
