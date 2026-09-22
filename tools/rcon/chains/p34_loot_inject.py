#!/usr/bin/env python3
"""p34-loot-injection — the dungeon-loot injection live acceptance chain.

The card's ACCEPTANCE ② face: "/loot give 滚 minecraft:chests/simple_dungeon 断言
GT 条目可出现（RNG 面=多轮滚出存在性+权重表静态钉双保险）". The headless dedicated
server has no online player, so the player-bound /loot give arm rides its server-side
twin /loot insert (the SAME roll path: LootTable.getRandomItems → the Forge patch
ForgeHooks.modifyLoot → the GT6DungeonLootModifier chain — LootTable.java.patch
m_230922_; only the consumer of the rolled stacks differs). Items land in a chest and
the container NBT is read back.

  A THE INJECTION EXISTENCE (the multi-roll face): minecraft:chests/simple_dungeon
    rolled SIX times into one chest — the modifier rolls [1,3] GT entries per roll
    (the deterministic lower bound 1), so "gt6:" in the container NBT is the
    existence assertion.
  B THE TARGETED TABLES: one roll each of the other six injected tables
    (abandoned_mineshaft, village/village_weaponsmith, stronghold_corridor,
    jungle_temple, desert_pyramid, jungle_temple_dispenser) — every roll carries
    >= 1 GT entry by construction, each arm asserts "gt6:".
  C THE WEIGHT TABLES (the gt.flawless/gems/misc 对位): one roll each of
    gt6:chests/gt_flawless and gt6:chests/gt_gems — every entry is a gem, so
    "gt6:gem" is guaranteed; gt_misc rolled five times (mixed vanilla+GT entries,
    nothing individually guaranteed — the load+roll face is the live assertion, the
    row fidelity is the offline pin).

passes=2 (the idempotent re-roll proof — the [0,0] of this chain). teardown: the
explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p34_loot_inject.py
      python3 tools/rcon/chains/p34_loot_inject.py --node 1.21.1-neoforge
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

# the fresh x=700 band at z 200-218, y = 64 — clear of every registered band
# (the x=678..684 sensor band sits at z 125..143; the x=6xx electric chains at
# z 240/364; the bee/gui bands at z 348..484).
CHESTS = {
    "dungeon": gt6world.Site(700, 64, 200),
    "mineshaft": gt6world.Site(700, 64, 202),
    "village": gt6world.Site(700, 64, 204),
    "corridor": gt6world.Site(700, 64, 206),
    "jungle": gt6world.Site(700, 64, 208),
    "pyramid": gt6world.Site(700, 64, 210),
    "dispenser": gt6world.Site(700, 64, 212),
    "flawless": gt6world.Site(700, 64, 214),
    "gems": gt6world.Site(700, 64, 216),
    "misc": gt6world.Site(700, 64, 218),
}
P = {k: F(v) for k, v in CHESTS.items()}

steps = []

# ------------------------------------------------- A: the multi-roll injection existence
steps += [
    phase("A: the simple_dungeon multi-roll — six inserts, GT entries must appear"),
    Step(f"setblock {P['dungeon']} minecraft:chest", expect="Changed the block"),
]
for _ in range(6):
    steps.append(Step(f"loot insert {P['dungeon']} loot minecraft:chests/simple_dungeon",
                      expect="Dropped"))  # the roll ack (the /loot feedback form)
steps += [
    Step(f"data get block {P['dungeon']} Items", expect="gt6:"),
]

# ------------------------------------------------- B: the targeted tables
# rolls-per-arm: most tables carry ONLY gt6-id rows (any pick lands a "gt6:" id),
# but the mineshaft rows include seven minecraft:*_ore vanilla-id rows (upstream
# ST.make(Blocks.coal_ore ...) verbatim) and the dispenser's fire-charge row is a
# VANILLA id that merges into the vanilla fire-charge stack — a single "gt6:"
# assert would flake whenever the weighted pick lands there, so those two arms
# get six inserts (the A-arm multi-roll shape).
MULTI = ("mineshaft", "dispenser")
for name, table in [
    ("mineshaft", "minecraft:chests/abandoned_mineshaft"),
    ("village", "minecraft:chests/village/village_weaponsmith"),
    ("corridor", "minecraft:chests/stronghold_corridor"),
    ("jungle", "minecraft:chests/jungle_temple"),
    ("pyramid", "minecraft:chests/desert_pyramid"),
    ("dispenser", "minecraft:chests/jungle_temple_dispenser"),
]:
    steps += [
        Step(f"setblock {P[name]} minecraft:chest", expect="Changed the block"),
    ]
    for _ in range(6 if name in MULTI else 1):
        steps.append(Step(f"loot insert {P[name]} loot {table}", expect="Dropped"))
    steps += [
        Step(f"data get block {P[name]} Items", expect="gt6:"),
    ]

# ------------------------------------------------- C: the weight tables (the bag 对位)
steps += [
    phase("C: the gt.flawless/gems/misc bag tables — the gem tables and the misc load+roll"),
    Step(f"setblock {P['flawless']} minecraft:chest", expect="Changed the block"),
    Step(f"loot insert {P['flawless']} loot gt6:chests/gt_flawless", expect="Dropped"),
    Step(f"data get block {P['flawless']} Items", expect="gt6:gem"),
    Step(f"setblock {P['gems']} minecraft:chest", expect="Changed the block"),
    Step(f"loot insert {P['gems']} loot gt6:chests/gt_gems", expect="Dropped"),
    Step(f"data get block {P['gems']} Items", expect="gt6:gem"),
    Step(f"setblock {P['misc']} minecraft:chest", expect="Changed the block"),
]
for _ in range(5):
    steps.append(Step(f"loot insert {P['misc']} loot gt6:chests/gt_misc", expect="Dropped"))
steps += [
    Step(f"data get block {P['misc']} Items", expect="minecraft:"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the explicit band restore"),
    Step("fill 698 62 198 702 66 220 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p34-loot-inject",
    slug="p34lootinject",
    sites=gt6world.declare_sites(*CHESTS.values()),
    preferred_ports=(26406, 26416),      # this card's pinned rcon/query pair (the free 2640x slot)
    mutates=(),                          # no global regime flips — plain setblock/loot/fill
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
