#!/usr/bin/env python3
"""p29-w5-t8-armor-give — the Hazmat armor give/census arm (task p29-w5-t8-armor-24;
the p29-w4-hot-lube-lube chest form, RCON sessions have no player so the headless
give rides `item replace block <chest> container.N with <stack>` + data probes):

  A THE 24-ITEM CENSUS: one chest takes all 24 flat pieces (6 suits x 4 slots, the
    SUITS-walk order), slot probes read the first and last rows back out.

passes=1 (stateless placement). teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t8_armor_give.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=332 band, x 384..398 — x-disjoint from the wear band (410..420) and the
# recipe band (430..440) of the same card (the one-fresh-band-per-cluster discipline)
CHEST = "384 65 332"

SUITS = ["insect", "frost", "heat", "radiation", "biochemgas", "universal"]
PIECES = ["helmet", "chestplate", "leggings", "boots"]
IDS = [f"gt6:hazmat_{s}_{p}" for s in SUITS for p in PIECES]

steps = [phase("A: the 24-item census — every flat piece lands as a real stack")]
steps.append(Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"))
for slot, item_id in enumerate(IDS):
    steps.append(Step(f"item replace block {CHEST} container.{slot} with {item_id} 1", expect="Replaced slot"))
# the roster probes: the first suit's helmet, the biochemgas/leggings middle, the
# universal/boots tail — the id walk itself is pinned offline (ArmorSetTest census)
steps += [
    Step(f"data get block {CHEST} Items[0]", expect="gt6:hazmat_insect_helmet"),
    Step(f"data get block {CHEST} Items[5]", expect="gt6:hazmat_frost_boots"),
    Step(f"data get block {CHEST} Items[10]", expect="gt6:hazmat_biochemgas_leggings"),
    Step(f"data get block {CHEST} Items[15]", expect="gt6:hazmat_universal_helmet"),
    Step(f"data get block {CHEST} Items[23]", expect="gt6:hazmat_universal_boots"),

    phase("B: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 382 62 329 398 68 336 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t8-armor-give p29_w5_t8_armor",
    slug="p29w5t8armorgive",
    sites=gt6world.declare_sites(gt6world.Site(384, 65, 332)),
    preferred_ports=(26423, 26433),
    game_port=26413,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
