#!/usr/bin/env python3
"""p31_tool_ladder_dig_materials — the dig-family material ladder chain (task p31-dig-ladder,
the p29_w5_t1_dig_six speed-arm shape extended with the <material> identity arms).

Chain semantics (the card ACCEPTANCE RCON 面 — 逐材质 give + 挖掘断言):

  A the material speed arms: /gt6dig speed <pos> <tool> <material> stamps the
    GT.ToolStats identity through GT6ToolLadder.stampIdentity (the same face the
    gt6:material_tool recipes assemble through) and reads the ladder live:
    - bronze pickaxe: speed 5.5 (the bronze mToolSpeed), maxDamage 448, iron/diamond
      tier authorized (quality 2), OBSIDIAN GATED (speed 0.0, correctForDrops=false —
      the :482 quality gate, the ladder's LEVEL face),
    - tungstensteel: speed 10.0, maxDamage 5120, obsidian UN-gated (quality 4 ≥ 3 —
      the only dig-family material above the diamond tier),
    - copper: speed 4.0, maxDamage 64, IRON ORE GATED (quality 0 < 1),
    - pickaxe_gem bronze: maxDamage 112 = 448 × 0.25 (the form multiplier through the
      seam, GTPickaxeGemItem), speed unchanged ×1.0,
    - pickaxe_construction bronze: speed 11.0 = 5.5 × 2 (the construction re-point),
      tungstensteel on diamond_ore: 20/4 = 5.0 (the ore-stone penalty).

  B the give arm: a REAL /give-shaped stack (item replace into a chest) carrying the
    leg-dialect identity payload — 1.20.1 root NBT {GT.ToolStats:{a:8610s,j:44800L}}
    vs 1.21.1 the gt6:tool_stats component — then the data-get probe names the payload
    back. Both legs prove the identity-carrying stacks are give-able and readable.

  C the mine arm at the bronze budget: one point per break against maxDamage=448
    (the toolDamage=1/448 report line IS the live durability readout).

  D teardown. passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_dig_ladder_materials.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the z=192 dig band, x424..436 (the dig-six sibling chains end at x419 — bbox-disjoint).
SITE = gt6world.Site(424, 64, 192, dx=12, dy=2, dz=0)
CHEST = "424 64 192"
STONE = "425 64 192"
IRON_ORE = "426 64 192"
DIAMOND_ORE = "427 64 192"
OBSIDIAN = "428 64 192"

steps = []

# ------------------------------------------------- A: the material speed arms
steps += [
    phase("A: the material speed arms — bronze / tungstensteel / copper, the quality gate"),
    Step(f"setblock {STONE} minecraft:stone", expect="Changed the block"),
    Step(f"setblock {IRON_ORE} minecraft:iron_ore", expect="Changed the block"),
    Step(f"setblock {DIAMOND_ORE} minecraft:diamond_ore", expect="Changed the block"),
    Step(f"setblock {OBSIDIAN} minecraft:obsidian", expect="Changed the block"),
    # bronze: the ladder face — bronze speed, bronze budget, iron+diamond tier OK
    Step(f"gt6dig speed {STONE} pickaxe bronze", expect="speed=5.5, correctForDrops=true, maxDamage=448"),
    Step(f"gt6dig speed {IRON_ORE} pickaxe bronze", expect="speed=5.5, correctForDrops=true, maxDamage=448"),
    Step(f"gt6dig speed {DIAMOND_ORE} pickaxe bronze", expect="speed=5.5, correctForDrops=true, maxDamage=448"),
    # the :482 quality gate — bronze (2 < 3) answers ZERO on the diamond tier
    Step(f"gt6dig speed {OBSIDIAN} pickaxe bronze", expect="speed=0.0, correctForDrops=false, maxDamage=448"),
    # tungstensteel: the ladder payoff — the diamond tier opens, the budget ×10
    Step(f"gt6dig speed {OBSIDIAN} pickaxe tungstensteel", expect="speed=10.0, correctForDrops=true, maxDamage=5120"),
    # copper: quality 0 — iron ore is gated, the budget folds to 64
    Step(f"gt6dig speed {IRON_ORE} pickaxe copper", expect="speed=0.0, correctForDrops=false, maxDamage=64"),
    Step(f"gt6dig speed {STONE} pickaxe copper", expect="speed=4.0, correctForDrops=true, maxDamage=64"),
    # the gem form multiplier through the seam: 448 × 0.25 = 112, speed ×1.0 unchanged
    Step(f"gt6dig speed {STONE} pickaxe_gem bronze", expect="speed=5.5, correctForDrops=true, maxDamage=112"),
    # the construction re-point: 5.5 × 2 = 11.0; tungstensteel on diamond ore: 20/4 = 5.0 (the ore-stone penalty)
    Step(f"gt6dig speed {STONE} pickaxe_construction bronze", expect="speed=11.0, correctForDrops=true, maxDamage=448"),
    Step(f"gt6dig speed {DIAMOND_ORE} pickaxe_construction tungstensteel", expect="speed=5.0, correctForDrops=true, maxDamage=5120"),
]

# ------------------------------------------------- B: the give arm (leg-dialect payloads)
BRONZE_GIVE = {
    "1.20.1": f"item replace block {CHEST} container.0 with gt6:pickaxe{{GT.ToolStats:{{a:8610s,j:44800L}}}} 1",
    "1.21.1": f"item replace block {CHEST} container.0 with gt6:pickaxe[gt6:tool_stats={{a:8610s,j:44800L}}] 1",
}
BRONZE_PROBE = {
    "1.20.1": f"data get block {CHEST} Items[0].tag.GT.ToolStats.j",
    "1.21.1": f"data get block {CHEST} Items[0].components.\"gt6:tool_stats\".j",
}
TUNGSTEN_GIVE = {
    "1.20.1": f"item replace block {CHEST} container.1 with gt6:pickaxe{{GT.ToolStats:{{a:8635s,j:512000L}}}} 1",
    "1.21.1": f"item replace block {CHEST} container.1 with gt6:pickaxe[gt6:tool_stats={{a:8635s,j:512000L}}] 1",
}

steps += [
    phase("B: the give arm — the identity payload rides the leg-dialect carrier into a real stack"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(BRONZE_GIVE["1.20.1"], expect="Replaced", node_cmds=BRONZE_GIVE),
    Step(BRONZE_PROBE["1.20.1"], expect="44800", node_cmds=BRONZE_PROBE),
    Step(TUNGSTEN_GIVE["1.20.1"], expect="Replaced", node_cmds=TUNGSTEN_GIVE),
    Step(f"data get block {CHEST} Items[1]", expect="512000"),
]

# ------------------------------------------------- C: the mine arm at the bronze budget
steps += [
    phase("C: the mine arm — one point per break against the bronze budget (the live durability readout)"),
    Step(f"gt6dig mine {STONE} pickaxe bronze", expect="drops=[minecraft:stone x1], toolDamage=1/448"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the site restored to air (the pass-open bbox is the backstop)"),
    Step(f"setblock {CHEST} minecraft:air", expect="Changed the block"),
    Step(f"setblock {STONE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {IRON_ORE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {DIAMOND_ORE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {OBSIDIAN} minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p31_tool_ladder_dig_materials",
    slug="p31toolladderdigmaterials",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26491, 26501),      # per-chain pair — DISAGREEING pins on purpose (session_ports: the session falls back to the node segments, keeping the --dual legs apart)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
