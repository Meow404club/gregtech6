#!/usr/bin/env python3
"""p30_w6_small_ore_datagen — the small-ore worldgen datagen live arm (task p30-w6-small-ore-datagen).

The /place feature live-fire triptych over the 91 (row, dim) configured/placed
features this card generates (zero new blocks — every placed block is the ore-1
ore_small universe). `/place feature` places the CONFIGURED feature directly at
the position (PlaceCommand.java:238-253 — no placement modifiers, no BiomeFilter),
so each case targets one host face deterministically; OreFeature size=1 scatters
its single block within x±2 / y±3 / z±2 of the origin (OreFeature.java:28-33
extent arithmetic), so each positive case scans that box with a
`/fill <box> air replace <block>` probe — the response "Successfully filled N
blocks" fires only when the ore landed (N=0 fails "No blocks were filled").

  A. stone face y>=0: place gt6:ore_small_overworld/copper onto forced stone ->
     gt6:ore_small_stone_copper lands (the stone_ore_replaceables target)
  B. deepslate face y=-10: place the same feature onto forced deepslate ->
     gt6:ore_small_deepslate_copper (the deepslate_ore_replaceables target; the
     y<0 host split rides the target tags — attribution is absolute here, the
     natural [60,120] band never reaches the box)
  C. non-host: place copper onto oak_planks -> "Failed to place feature"
     (the WD.setSmallOre host rule rejects, the vanilla error line)
  D. nether sample of 3 (niter/efrine/cinnabar — the nether-exclusive rows):
     base_stone_nether host -> ore_small_netherrack_<m>
  E. end sample of 2 (endium/sugilite): end_stone host -> ore_small_endstone_<m>

Declared fuzz: in A/D/E the probe box can also hold NATURALLY generated ore
(the card's own biome modifiers decorate fresh chunks); every positive expect
is "Successfully filled" (>=1), never an exact count — attribution is strict
only in B and C. Pass 2 is the idempotency proof (the framework reruns on the
same world; the pads are re-laid, the probe boxes are re-filled).

Run:  python3 tools/rcon/chains/p30_w6_small_ore_datagen.py [--node 1.21.1-neoforge]
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

O = gt6world.Site(384, 64, 400)   # the overworld triptych bench (fresh band z>=400)
OX, OY, OZ = O.x, O.y, O.z

COPPER = "gt6:ore_small_overworld/copper"
STONE_ORE = "gt6:ore_small_stone_copper"
DEEPSLATE_ORE = "gt6:ore_small_deepslate_copper"


def place_probe(dim_prefix: str, feature: str, block: str, x: int, y: int, z: int) -> list[Step]:
    """place at (x,y,z) then scan the OreFeature size=1 scatter box for the block.

    The box is the OreFeature.java:28-33 extent (x±2, y±3, z±2) padded one block;
    `fill ... replace <block>` reports "Successfully filled N blocks" only when at
    least one matching block was in the box.
    """
    return [
        Step(f"place feature {feature} {x} {y} {z}",
             expect=f'Placed "{feature}"'),
        Step(f"execute in {dim_prefix} run fill {x - 2} {y - 3} {z - 2} "
             f"{x + 2} {y + 3} {z + 2} minecraft:air replace {block}",
             expect="Successfully filled"),
    ]


CHAIN = Chain(
    name="p30-w6-small-ore-datagen",
    slug="p30_w6_small_ore_datagen",
    sites=gt6world.declare_sites(O),
    preferred_ports=(26542, 26552),      # this card's pinned rcon/query pair (fresh 2654x segment)
    fresh_boot=True,                     # fresh world: the probe boxes start GT6-ore-free in B/C
    steps=[
        phase("A: stone face y>=0 — the stone_ore_replaceables target"),
        Step(f"setblock {OX} {OY} {OZ} minecraft:stone", expect="Changed the block"),
        *place_probe("minecraft:overworld", COPPER, STONE_ORE, OX, OY, OZ),

        phase("B: deepslate face y=-10 — the deepslate_ore_replaceables target "
              "(strict attribution: no natural band reaches y<=7)"),
        Step(f"setblock {OX} -10 {OZ} minecraft:deepslate", expect="Changed the block"),
        *place_probe("minecraft:overworld", COPPER, DEEPSLATE_ORE, OX, -10, OZ),

        phase("C: non-host oak_planks — the WD.setSmallOre host rule rejects"),
        Step(f"setblock {OX} {OY} {OZ} minecraft:oak_planks", expect="Changed the block"),
        Step(f"place feature {COPPER} {OX} {OY} {OZ}", expect="Failed to place feature"),

        phase("D: nether sample of 3 — niter/efrine/cinnabar, the base_stone_nether host"),
        Step("execute in minecraft:the_nether run forceload add 30 30"),
        Step("execute in minecraft:the_nether run setblock 30 70 30 minecraft:netherrack",
             expect="Changed the block"),
        *place_probe("minecraft:the_nether", "gt6:ore_small_nether/niter",
                     "gt6:ore_small_netherrack_niter", 30, 70, 30),
        Step("execute in minecraft:the_nether run setblock 40 70 40 minecraft:netherrack",
             expect="Changed the block"),
        *place_probe("minecraft:the_nether", "gt6:ore_small_nether/efrine",
                     "gt6:ore_small_netherrack_efrine", 40, 70, 40),
        Step("execute in minecraft:the_nether run setblock 50 70 50 minecraft:netherrack",
             expect="Changed the block"),
        *place_probe("minecraft:the_nether", "gt6:ore_small_nether/cinnabar",
                     "gt6:ore_small_netherrack_cinnabar", 50, 70, 50),

        phase("E: end sample of 2 — endium/sugilite, the end_stone host"),
        Step("execute in minecraft:the_end run forceload add 0 0"),
        Step("execute in minecraft:the_end run setblock 0 64 0 minecraft:end_stone",
             expect="Changed the block"),
        *place_probe("minecraft:the_end", "gt6:ore_small_end/endium",
                     "gt6:ore_small_endstone_endium", 0, 64, 0),
        Step("execute in minecraft:the_end run setblock 10 64 10 minecraft:end_stone",
             expect="Changed the block"),
        *place_probe("minecraft:the_end", "gt6:ore_small_end/sugilite",
                     "gt6:ore_small_endstone_sugilite", 10, 64, 10),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
