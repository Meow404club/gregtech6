#!/usr/bin/env python3
"""p29_w6_t2_surface — the surface-plants + fallen-woods live acceptance chain (task
p30-w6-t2-surface-blocks; the p29_w6_t1_trees /place-feature form).

Chain semantics (the /place lesson: /place feature takes a CONFIGURED feature id —
no placement modifiers, no rarity roll, deterministic at a known origin):

  A the 9-cell dirt shelf at y=100 (x641..769 z299..317, disjoint from every
    registered band), one 14-wide cell per feature:
    glowtus (a 3x3 water pool, the plate lands at the surface slot), bush,
    the four fallen-log woods (dry/rotten land arms + the frozen snow-layer
    arm — the row/pile shapes all cover the cell origin or the snow slot),
    then the three soil disks (black sand / turf / the vanilla clay pit) —
    each /place lands "Placed" and the cell origin (or the platform top for
    the disks) carries the block (execute if block, "Test passed").

  B the destroy-drop faces: setblock + `air destroy` drops the block item —
    glowtus / berry_bush / black_sand / turf / dead_log (the self-drop loot,
    the p29_w6_rocks_sticks item-entity idiom).

  C the tag faces: #minecraft:logs (the coke-oven rebuild source) answers with
    the 4 fallen-log woods (13 gt6 members = 9 trees + 4 woods).

  T teardown: the sky band back to air.

Two framework passes are the [0, 0] idempotency proof (every arm re-clears its
cell and re-lays the shelf first). Run with GT6_SESSION=off (the pinned ports).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w6_t2_surface.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x 641..769, z 299..317, y 98..121: a fresh sky band disjoint from
# the t1 tree band (x493..619 z141..155), the rocks/sticks band (x384..390
# z299..305) and every machine strip.
PLATFORM = gt6world.Site(705, 100, 308, dx=64, dy=0, dz=9)   # the dirt shelf bbox
HEADROOM = gt6world.Site(705, 111, 308, dx=64, dy=10, dz=9)  # the placement headroom bbox

# The 9 feature cells (14-wide), z=300 row.
CELLS = {
    "glowtus": 648, "bush": 662, "dead": 676, "rotten": 690, "mossy": 704,
    "frozen": 718, "blacksand": 732, "turf": 746, "clay": 760,
}
Z = 300
# The loot arms, z=312 row.
LOOT = [("glowtus", 648), ("berry_bush", 656), ("black_sand", 664),
        ("turf", 672), ("dead_log", 680)]

ITEM_ID = '@e[type=minecraft:item,nbt={Item:{id:"%s"}},distance=..6,x=%d,y=101,z=%d]'
ITEMS = "@e[type=minecraft:item,distance=..6,x=%d,y=101,z=%d]"

steps = []

# ------------------------------------------------- A: the shelf + the 9 /place probes
# NOTE (the t1 chain fill finding): /fill returns an EMPTY RCON body on this server
# build — the fills carry expect=None and the shelf's existence is proven by the
# per-cell dirt probe instead (execute if block).
steps += [
    phase("A: the shelf — /fill the y=100 dirt shelf, then /place feature per cell"),
    Step("fill 641 100 299 769 100 317 minecraft:dirt"),
    Step("execute if block 648 100 300 minecraft:dirt", expect="Test passed"),
]
# glowtus: the 3x3 water pool, the plate lands at the surface slot
steps += [
    Step("fill 647 100 299 649 100 301 minecraft:water"),
    Step("place feature gt6:plant_glowtus 648 101 300", expect="Placed"),
    Step("execute if block 648 101 300 gt6:glowtus", expect="Test passed"),
]
# bush (the ground contact)
steps += [
    Step("place feature gt6:plant_bush 662 101 300", expect="Placed"),
    Step("execute if block 662 101 300 gt6:berry_bush", expect="Test passed"),
]
# the fallen logs: every shape covers the origin slot (piles 100..102, rows at 101)
for tName, tCx, tFeature, tBlock in [
        ("dead", CELLS["dead"], "gt6:log_dry", "gt6:dead_log"),
        ("rotten", CELLS["rotten"], "gt6:log_rotten", "gt6:rotten_log"),
        ("mossy", CELLS["mossy"], "gt6:log_mossy", "gt6:mossy_log")]:
    steps += [
        Step(f"place feature {tFeature} {tCx} 101 {Z}", expect="Placed"),
        Step(f"execute if block {tCx} 101 {Z} {tBlock}", expect="Test passed"),
    ]
# frozen: the snow-layer contact arm — the logs replace/start at the snow slot
steps += [
    Step(f"setblock {CELLS['frozen']} 101 {Z} minecraft:snow", expect="Changed the block"),
    Step(f"place feature gt6:log_frozen {CELLS['frozen']} 102 {Z}", expect="Placed"),
    Step(f"execute if block {CELLS['frozen']} 101 {Z} gt6:frozen_log", expect="Test passed"),
]
# the soil disks: the platform top row (half_height 0/1/4 all cover y=100)
steps += [
    Step(f"place feature gt6:river_magnetite {CELLS['blacksand']} 101 {Z}", expect="Placed"),
    Step(f"execute if block {CELLS['blacksand']} 100 {Z} gt6:black_sand", expect="Test passed"),
    Step(f"place feature gt6:swamp_turf {CELLS['turf']} 101 {Z}", expect="Placed"),
    Step(f"execute if block {CELLS['turf']} 100 {Z} gt6:turf", expect="Test passed"),
    Step(f"place feature gt6:pit_clay_vanilla {CELLS['clay']} 101 {Z}", expect="Placed"),
    Step(f"execute if block {CELLS['clay']} 100 {Z} minecraft:clay", expect="Test passed"),
]

# ------------------------------------------------- B: the destroy-drop faces
steps += [
    phase("B: the destroy-drop faces — setblock + air destroy drops the block item"),
]
for tName, tX in LOOT:
    steps += [
        Step(f"setblock {tX} 101 312 gt6:{tName}", expect="Changed the block"),
        Step(f"setblock {tX} 101 312 air destroy", expect="Changed the block"),
        Step(f"execute if entity {ITEM_ID % ('gt6:' + tName, tX, 312)}", expect="Test passed"),
        Step(f"kill {ITEMS % (tX, 312)}", expect="Killed"),
    ]

# ------------------------------------------------- C: the tag faces (the coke-oven source)
steps += [
    phase("C: the tag faces — #minecraft:logs carries the 4 fallen-log woods"),
    Step("gt6tags dump minecraft:logs", expect="gt6:dead_log"),
    Step("gt6tags dump minecraft:logs", expect="gt6:frozen_log"),
    Step("gt6tags dump minecraft:logs", expect="gt6:mossy_log"),
    Step("gt6tags dump minecraft:logs", expect="gt6:rotten_log"),
]

# ---------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the sky band back to air (the pass-open bbox is the backstop)"),
    Step("fill 641 98 299 769 121 317 minecraft:air"),
    Step("execute if block 648 100 300 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w6-t2-surface",
    slug="p29_w6_t2_surface",
    sites=gt6world.declare_sites(PLATFORM, HEADROOM),
    preferred_ports=(26403, 26413),      # this card's pinned rcon/query pair (the fresh 2640x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
