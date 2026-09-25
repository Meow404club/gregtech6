#!/usr/bin/env python3
"""small_ore_datagen — the small-ore worldgen datagen live arm (task small-ore-datagen).

The /place feature live-fire triptych over the 91 (row, dim) configured/placed
features this card generates (zero new blocks — every placed block is the ore-1
ore_small universe). `/place feature` places the CONFIGURED feature directly at
the position (PlaceCommand.java:238-253 — no placement modifiers, no BiomeFilter),
so each case targets one host face deterministically; OreFeature size=1 scatters
its single block within x±2 / y±3 / z±2 of the origin (OreFeature.java:28-33
extent arithmetic), so each positive case scans that box with a
`/fill <box> air replace <block>` probe — the response "Successfully filled N
blocks" fires only when the ore landed (N=0 fails "No blocks were filled").

  A. stone pad on the flat-world surface bench (y=-60): place
     gt6:ore_small_overworld/copper onto forced stone -> gt6:ore_small_stone_copper
     lands (the stone_ore_replaceables target)
  B. deepslate pad at y=-10 (below the flat floor, outside every natural band):
     the same feature -> gt6:ore_small_deepslate_copper (the
     deepslate_ore_replaceables target; strict attribution is absolute here)
  C. non-host: place copper onto oak_planks -> "Failed to place feature"
     (the WD.setSmallOre host rule rejects, the vanilla error line)
  D. nether sample of 3 (niter/efrine/cinnabar — the nether-exclusive rows) at
     the y=10 floor bench: base_stone_nether host -> ore_small_netherrack_<m>
  E. end sample of 2 (endium/sugilite) at the y=30 island-interior bench:
     end_stone host -> ore_small_endstone_<m>

Attribution: the flat-world floor carries no natural ore below y=-60 and the
overworld natural bands start at y>=1, so A/B/C are strictly attributed; in
D/E the probe box can also hold NATURALLY generated ore (the card's own biome
modifiers decorate fresh chunks) — every positive expect is "Successfully
filled" (>=1), never an exact count. Pass 2 is the idempotency proof (the
framework reruns on the same world; the pads are re-laid, the probe boxes are
re-filled).

Run:  python3 tools/rcon/chains/small_ore_datagen.py [--node 1.21.1-neoforge]
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The bench rides the flat world's own height envelope: gt6server provisions
# level-type flat (surface grass at y=-61), and OreFeature.place gates on
# `y - 3 <= getHeight(OCEAN_FLOOR_WG)` (OreFeature.java:56-58) — a bench at
# y >= -57 can never pass that gate on flat ground (the live-run RED finding).
# y=-60 sits one block under the surface: the gate reads -63 <= -60, the walk
# envelope y-2..y+2 stays inside the forced pad, and the natural ore bands
# ([1,250]...) never reach the box (strict attribution, now in BOTH probe arms).
# The bench rides the flat world's own height envelope: gt6server provisions
# level-type flat (native layers bedrock/dirt/grass, surface -61, NO stone), and
# OreFeature.place gates on `y - 3 <= getHeight(OCEAN_FLOOR_WG)` (OreFeature.java:56-58)
# — a bench at y >= -57 can never pass that gate on flat ground (live-run finding).
# y=-60 sits one block under the surface: the gate reads -63 <= -60, and the natural
# ore bands ([1,250]...) never reach y<0, so the overworld arms are strictly attributed.
O = gt6world.Site(384, -60, 400)
OX, OY, OZ = O.x, O.y, O.z

COPPER = "gt6:ore_small_overworld/copper"
STONE_ORE = "gt6:ore_small_stone_copper"
DEEPSLATE_ORE = "gt6:ore_small_deepslate_copper"


def host_pad(dim: str, block: str, x: int, y: int, z: int) -> list[Step]:
    """Force a 3x5x3 host pad (x±1, y±2, z±1): the OreFeature size=4 walk lands within
    that envelope. The AIR-CLEAR covers the FULL PROBE BOX first (it wipes any ore a
    previous pass/run left inside it — the live-run pollution finding), with
    allow_failed (a no-op clear on an already-air box reports "No blocks were filled");
    the host fill is then judged STRICTLY — after the clear it always changes, so the
    strict expect holds on every pass (the pass-2 idempotency finding: a pad refill
    onto an unchanged pad reports "No blocks were filled")."""
    return [
        Step(f"execute in {dim} run fill {x - 2} {y - 3} {z - 2} {x + 2} {y + 3} {z + 2} minecraft:air",
             allow_failed=True),
        Step(f"execute in {dim} run fill {x - 1} {y - 2} {z - 1} {x + 1} {y + 2} {z + 1} {block}",
             expect="Successfully filled"),
    ]


def place_probe(dim: str, feature: str, block: str, x: int, y: int, z: int) -> list[Step]:
    """place at (x,y,z) then scan the OreFeature size=4 scatter box for the block.

    Both commands ride `execute in <dim>`. The place step polls: a size=4 walk places
    on ~75% of attempts (the walk sphere reaches a block center only for the
    half-integer-y points), so the SAME /place is resent until it reports Placed or
    the deadline passes; the final response is judged exactly once. The probe box is
    the OreFeature.java:28-33 extent (x±2, y±3, z±2) padded one block; `fill ...
    replace <block>` reports "Successfully filled N blocks" only when at least one
    matching block was in the box.
    """
    return [
        Step(f"execute in {dim} run place feature {feature} {x} {y} {z}",
             expect=f'Placed "{feature}"', poll=12.0),
        Step(f"execute in {dim} run fill {x - 2} {y - 3} {z - 2} "
             f"{x + 2} {y + 3} {z + 2} minecraft:air replace {block}",
             expect="Successfully filled"),
    ]


CHAIN = Chain(
    name="small-ore-datagen",
    slug="small_ore_datagen",
    sites=gt6world.declare_sites(O),
    preferred_ports=(26542, 26552),      # this card's pinned rcon/query pair (fresh 2654x segment)
    fresh_boot=True,                     # fresh world: the probe boxes start GT6-ore-free in B/C
    steps=[
        phase("A: stone pad on the flat bench — the stone_ore_replaceables target"),
        *host_pad("minecraft:overworld", "minecraft:stone", OX, OY, OZ),
        *place_probe("minecraft:overworld", COPPER, STONE_ORE, OX, OY, OZ),

        phase("B: deepslate pad, same bench — the deepslate_ore_replaceables target "
              "(strict attribution: the natural bands never reach y<0 and the flat "
              "floor carries no deepslate or GT ore to confuse the box with)"),
        *host_pad("minecraft:overworld", "minecraft:deepslate", OX, OY, OZ),
        *place_probe("minecraft:overworld", COPPER, DEEPSLATE_ORE, OX, OY, OZ),

        phase("C: non-host oak_planks pad — the WD.setSmallOre host rule rejects "
              "(the walk lands on planks, not on a missed slot)"),
        *host_pad("minecraft:overworld", "minecraft:oak_planks", OX, OY, OZ),
        Step(f"place feature {COPPER} {OX} {OY} {OZ}", expect="Failed to place feature", poll=12.0),

        phase("D: nether sample of 3 — niter/efrine/cinnabar, the base_stone_nether host "
              "(bench y=10: the heightmap gate reads 7 <= terrain height, always true "
              "over the nether floor; forceload is async — one range mark + settle sleep)"),
        Step("execute in minecraft:the_nether run forceload add 28 28 52 52", sleep=4.0),
        *host_pad("minecraft:the_nether", "minecraft:netherrack", 30, 10, 30),
        *place_probe("minecraft:the_nether", "gt6:ore_small_nether/niter",
                     "gt6:ore_small_netherrack_niter", 30, 10, 30),
        *host_pad("minecraft:the_nether", "minecraft:netherrack", 40, 10, 40),
        *place_probe("minecraft:the_nether", "gt6:ore_small_nether/efrine",
                     "gt6:ore_small_netherrack_efrine", 40, 10, 40),
        *host_pad("minecraft:the_nether", "minecraft:netherrack", 50, 10, 50),
        *place_probe("minecraft:the_nether", "gt6:ore_small_nether/cinnabar",
                     "gt6:ore_small_netherrack_cinnabar", 50, 10, 50),

        phase("E: end sample of 2 — endium/sugilite, the end_stone host "
              "(bench y=30 inside the main island: gate 27 <= island height)"),
        Step("execute in minecraft:the_end run forceload add 0 0 12 12", sleep=4.0),
        *host_pad("minecraft:the_end", "minecraft:end_stone", 0, 30, 0),
        *place_probe("minecraft:the_end", "gt6:ore_small_end/endium",
                     "gt6:ore_small_endstone_endium", 0, 30, 0),
        *host_pad("minecraft:the_end", "minecraft:end_stone", 10, 30, 10),
        *place_probe("minecraft:the_end", "gt6:ore_small_end/sugilite",
                     "gt6:ore_small_endstone_sugilite", 10, 30, 10),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
