#!/usr/bin/env python3
"""t3 machine face — the soft-hammer rotation arm (task machine-face-four
ACCEPTANCE ③, the tool_hammer_wrench give-arm shape + the /gt6machineface stand-in
command; RCON sessions have no player, so the arm drives the item's own useOn through a
fake player, the GT6ChiselCommand same-source convention).

Chain semantics:

  A the give arm: gt6:soft_hammer lands as a real stack in a chest (`item replace block`
     + the Items[N].id probe — an unregistered id would fail the replace loudly).

  B the rotation arm (the :125 tagline "Can rotate vanilla-ish things and toggle
     Lamps/Rails", the 1.20.1 cut list):
     - oak_stairs (default facing=north) -> ROTATED, facing=east (the RCON facing face);
     - powered_rail (flat N-S) -> shape=east_west (the upstream (aMeta+8)%16 two-flip,
       ToolCompat.java:273-284);
     - redstone_lamp (unlit) -> lit=true (the upstream :261-272 lit/unlit toggle);
     - the furnace negative -> "same ... softHammerDamage=0" (the RED LINE external-pool
       verdict: the upstream :285-297 piston/furnace/chest/hopper arms are CUT);
     every rotated form pays exactly ONE durability point (the 100-unit click fold).

  C the teardown: fill-air over the column. passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/machine_face_softhammer.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=292 band — x 384..388 (this chain), clear of the W2 injector strip
# (z=284, x396..404) and the W2 eu_special band (z=276); pairwise x-disjoint from
# this card's magnifying-glass (x392..396) and pincers (x400..404) columns.
CHEST = "386 64 292"
STAIRS = "386 64 290"
C_STAIRS = "386, 64, 290"
RAIL = "386 64 288"
C_RAIL = "386, 64, 288"
RAIL_BASE = "386 63 288"   # X Y Z — the support sits DIRECTLY BELOW the rail (y=63)
LAMP = "386 64 286"
FURNACE = "386 64 284"
SITE = gt6world.Site(386, 64, 288, dy=4, dz=4)

steps = []

# ------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — gt6:soft_hammer lands as a real stack"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST} container.0 with gt6:soft_hammer 1", expect="Replaced"),
    Step(f"data get block {CHEST} Items[0]", expect="gt6:soft_hammer"),
]

# ------------------------------------------------- B: the rotation arm
steps += [
    phase("B: the rotation arm — stairs facing, rail flat-flip, lamp toggle, the furnace negative"),
    Step(f"setblock {STAIRS} minecraft:oak_stairs", expect="Changed the block"),
    # the report rides BlockPos.toShortString() — the "x, y, z" comma form (the wandclick lesson)
    Step(f"gt6machineface rotate {STAIRS}", expect=f"rotate ROTATED at {C_STAIRS}: state Block{{minecraft:oak_stairs}}"),
    Step(f"execute if block {STAIRS} minecraft:oak_stairs[facing=east] run time query daytime", expect="The time is"),
    # the straight rail needs a solid base — floating rails pop on the placement update
    Step(f"setblock {RAIL_BASE} minecraft:stone", expect="Changed the block"),
    Step(f"setblock {RAIL} minecraft:powered_rail", expect="Changed the block"),
    Step(f"gt6machineface rotate {RAIL}", expect=f"rotate ROTATED at {C_RAIL}"),
    Step(f"execute if block {RAIL} minecraft:powered_rail[shape=east_west] run time query daytime", expect="The time is"),
    Step(f"setblock {LAMP} minecraft:redstone_lamp", expect="Changed the block"),
    Step(f"gt6machineface rotate {LAMP}", expect="rotate ROTATED"),
    Step(f"execute if block {LAMP} minecraft:redstone_lamp[lit=true] run time query daytime", expect="The time is"),
    Step(f"setblock {FURNACE} minecraft:furnace", expect="Changed the block"),
    Step(f"gt6machineface rotate {FURNACE}", expect="rotate same"),
    Step(f"gt6machineface rotate {STAIRS}", expect="softHammerDamage=1/512"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the column restored to air"),
    Step(f"fill 384 62 284 388 67 293 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="machine-face-softhammer",
    slug="softhammer",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26520, 26530),      # this card's pinned rcon/query pair (the fresh 2652x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
