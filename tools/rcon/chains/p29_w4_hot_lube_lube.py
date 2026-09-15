#!/usr/bin/env python3
"""p29-w4-hot-lube-lube — the Diesel Engine lubricant live acceptance chain (task
p29-w4-hot-lube; the p12-engine-diesel e2e form):

  A THE ITEM FACE (acceptance ④): one chest + `item replace` puts the new
    gt6:lubricant_bucket live (the DieselEngine crafting 'L' slot carrier, the
    OD.itemLubricant port counterpart) — `data get` reads it back out of the
    chest slot.
  B THE ENGINE FACE: the steel diesel engine (the row the 8 crafting jsons
    build — the crafting-grid proof is the offline GT6DieselCraftingJsonTest +
    the generated files; this chain proves the TARGET lives and burns the
    FM.Engine diesel) — place, stat rate=32 RU/t (the NBT_OUTPUT row), fuel,
    the diesel fuel line registers.

passes=2 is the idempotency proof. teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_hot_lube_lube.py
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

# the fresh z=328 band, x-disjoint column from the hex chain (426..436): the lube
# face rides x 446..448
CHEST = gt6world.Site(446, 65, 328)
ENGINE = gt6world.Site(448, 65, 328)

CHEST_P = "446 65 328"
ENGINE_P = "448 65 328"
DIESEL = "gt6:diesel_engine_steel"

steps = [
    phase("A: the item face — the lubricant bucket lives (the crafting 'L' slot carrier)"),
    Step(f"setblock {CHEST_P} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST_P} container.0 with gt6:lubricant_bucket 1", expect=""),
    Step(f"data get block {CHEST_P} Items", expect="gt6:lubricant_bucket"),

    phase("B: the engine face — the steel diesel engine the rows build burns the FM.Engine line"),
    Step(f"setblock {ENGINE_P} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"gt6engine stat {ENGINE_P}", expect="rate=32 RU/t"),
    Step(f"gt6engine fuel {ENGINE_P} gt6:diesel 1000", expect="of gt6:diesel"),
    Step(f"gt6engine stat {ENGINE_P}", expect="gt6:diesel"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 444 62 324 452 68 333 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w4-hot-lube-lube p29_w4_hot_lube",
    slug="p29w4hotlubelube",
    sites=gt6world.declare_sites(CHEST, ENGINE),
    preferred_ports=(26422, 26432),      # this card's pinned rcon/query pair (disjoint from the hex chain's 26421)
    game_port=26412,                     # the pinned game port (rcon - 10)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
