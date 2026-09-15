#!/usr/bin/env python3
"""p29-w5-t8-armor-recipe — the universal-assembly craft surrogate arm (task
p29-w5-t8-armor-24). RCON has no player and vanilla exposes no crafting command (the
p25-tool-hammer-wrench precedent), so the "craft 一套 universal" face is the committed
datagen JSON + an offline ArmorSetTest grid pin, and THIS chain stages the LIVE
INGREDIENT FACE: a chest laid out in the exact 3x3 grid of the universal LEGGINGS row
(Loader_Tools.java:95 — 'A' biochemgas legs, 'B' insect legs, 'C' frost legs,
'D' heat legs, 'E' radiation legs x2, 'F' the vanilla chainmail legs; the dropped
tool-letter 'l' folds to an empty cell), plus the result pieces probed live.

grid slots (container.N = row-major 3x3):
  0=A 1=. 2=B
  3=C 4=. 5=D
  6=E 7=F 8=E

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t8_armor_recipe.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

CHEST = "430 65 332"          # the fresh z=332 band, x 430..440

GRID = {
    0: "gt6:hazmat_biochemgas_leggings",
    2: "gt6:hazmat_insect_leggings",
    3: "gt6:hazmat_frost_leggings",
    5: "gt6:hazmat_heat_leggings",
    6: "gt6:hazmat_radiation_leggings",
    7: "minecraft:chainmail_leggings",
    8: "gt6:hazmat_radiation_leggings",
}

steps = [phase("A: the universal-leggings 3x3 ingredient face — the :95 grid, laid out live")]
steps.append(Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"))
for slot, item_id in sorted(GRID.items()):
    steps.append(Step(f"item replace block {CHEST} container.{slot} with {item_id} 1", expect="Replaced slot"))
steps += [
    Step(f"data get block {CHEST} Items[0].id", expect="gt6:hazmat_biochemgas_leggings"),
    Step(f"data get block {CHEST} Items[6].id", expect="gt6:hazmat_radiation_leggings"),
    Step(f"data get block {CHEST} Items[7].id", expect="minecraft:chainmail_leggings"),

    phase("B: the result face — the five assembled pieces exist as live items"),
    Step(f"item replace block {CHEST} container.13 with gt6:hazmat_universal_leggings 1", expect="Replaced slot"),
    Step(f"data get block {CHEST} Items[13].id", expect="gt6:hazmat_universal_leggings"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 428 62 329 440 68 336 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t8-armor-recipe p29_w5_t8_armor",
    slug="p29w5t8armorrecipe",
    sites=gt6world.declare_sites(gt6world.Site(430, 65, 332)),
    preferred_ports=(26425, 26435),
    game_port=26415,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
