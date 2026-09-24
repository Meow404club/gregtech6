#!/usr/bin/env python3
"""p37-chains-anvil — the stone/blackstone anvil live-smoke chain (task
p37-chains-anvil, the P37-d s4-2 gap closure).

The gap (research.p37-gap-scan.s4-2): both anvil rows are registered
(registry/GT6Anvils.java:73-75 — stone_anvil 10000 / blackstone_anvil 100000)
and offline-pinned (GT6AnvilBlockEntityTest.java + GT6RecipesAnvilTest.java)
but had zero live smoke. Two arms, the shortest Java-free live face:

  A REGISTRATION + PLACEMENT + THE ROW CARRIER: both rows place, read back as
    gt6:stone_anvil[facing=north] / gt6:blackstone_anvil[facing=north], and the
    BE /data dump pins the carrier row values — gt.durability 10000L vs
    100000L. That is the ONLY data distinguishing the two rows live (the BE
    reads it off the block carrier at construction, GT6AnvilBlockEntity.java
    :139-143, and persists it, :463-465) — the row axis of
    rowAxisReproducesTheLoaderAnchors, live.
  B THE DROPSELF LOOT 到账: each row's loot table (GT6LootTables
    .GT6AnvilBlockLoot — dropSelf per row) rolled into a chest, the block item
    read back from the container NBT (the p34_loot_inject proven face).

  C teardown.

THE DECLARED DEVIATION (coordinator ruling on the card's deviation note): the
strike/craft arm stays OFFLINE-pinned. The BE's only live driver is the player
right-click (GTAnvilBlock.use → activateChain); no /gt6* command reaches the
anvil BE (/gt6kitchen resolves only GT6ManualKitchenBlockEntity) and the anvil
exposes no item capability (the root mInventory carrier is never wired —
setInventory is never called — so hoppers have no insert face either; the
upstream MultiTileEntityAnvil carries no tool-repair face either, the port
faithfully does not invent one). The strike math is GT6AnvilBlockEntityTest's
fixture rows; if the live strike ever needs an RCON face it is a /gt6anvil
interact command card (the GT6KitchenCommand.java:264 shape), not this chain.

Band: x756..768 z96..104 (fresh, x/z-disjoint from every registered band: the
loot shelf x705..769 sits z308..317, cover-hosts x696..724 z554..574, the
sensors x680 z125..143, the x=700 loot band z200..218).
Two framework passes are the [0, 0] idempotency proof (every arm re-lays).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p37_anvil_run.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — one band, two anvil columns: stone on x760, blackstone on x764.
BAND = gt6world.Site(762, 64, 100, dx=6, dy=3, dz=4)   # x756..768, y61..67, z96..104
STONE = "760 64 100"
BLACKSTONE = "764 64 100"
CHEST_S = "760 66 102"
CHEST_B = "764 66 102"

steps = []

# ------------------------------------------------- A: registration + placement + row carrier
steps += [
    phase("A: the two rows place and carry their registration durability"),
    Step("forceload add 754 94 770 106"),
    Step("fill 756 63 96 768 63 104 minecraft:smooth_stone", expect="filled"),
    Step(f"setblock {STONE} gt6:stone_anvil", expect="Changed the block"),
    Step(f"setblock {BLACKSTONE} gt6:blackstone_anvil", expect="Changed the block"),
    Step(f"execute if block {STONE} gt6:stone_anvil[facing=north]", expect="Test passed"),
    Step(f"execute if block {BLACKSTONE} gt6:blackstone_anvil[facing=north]",
         expect="Test passed"),
    Step(f"data get block {STONE}", expect='te_name: "anvil"'),
    Step(f"data get block {STONE}", expect="gt.durability: 10000L"),
    Step(f"data get block {BLACKSTONE}", expect="gt.durability: 100000L"),
]

# ------------------------------------------------- B: the dropSelf loot 到账
steps += [
    phase("B: the dropSelf loot tables roll the block items into chests"),
    Step(f"setblock {CHEST_S} minecraft:chest", expect="Changed the block"),
    Step(f"loot insert {CHEST_S} loot gt6:blocks/stone_anvil", expect="Dropped"),
    Step(f"data get block {CHEST_S} Items", expect="gt6:stone_anvil"),
    Step(f"setblock {CHEST_B} minecraft:chest", expect="Changed the block"),
    Step(f"loot insert {CHEST_B} loot gt6:blocks/blackstone_anvil", expect="Dropped"),
    Step(f"data get block {CHEST_B} Items", expect="gt6:blackstone_anvil"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — chests, both anvils and the floor back to air"),
    Step(f"setblock {CHEST_S} minecraft:air", expect="Changed the block"),
    Step(f"setblock {CHEST_B} minecraft:air", expect="Changed the block"),
    Step(f"setblock {STONE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {BLACKSTONE} minecraft:air", expect="Changed the block"),
    Step(f"execute if block {STONE} minecraft:air", expect="Test passed"),
    Step(f"execute if block {BLACKSTONE} minecraft:air", expect="Test passed"),
    Step("fill 756 63 96 768 63 104 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p37-anvil-run",
    slug="p37anvilrun",
    sites=gt6world.declare_sites(BAND),
    # NO preferred_ports pin: the single-chain session falls back to the node
    # session-ports table (the p34_cover_hosts form).
    mutates=(),                          # no global regime flips — plain setblock/loot/fill
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
