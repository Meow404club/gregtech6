#!/usr/bin/env python3
"""p35-rails-ride — the GT6 rail RIDE chain (task p35-rails-31-blocks, ACCEPTANCE rail-ride).

Chain semantics: the per-rail speed ladder (the census-erratum direct translation through
the getRailMaxSpeed hook on both legs) is proven LIVE by a two-lane race — the same initial
motion on a steel lane (ladder 0.60/tick) and an aluminium lane (ladder 0.20/tick):

  A the registration face: both lanes setblock-filled and read back (an unregistered rail
    id would fail the fill), blockstates settled as the default NORTH_SOUTH straight.
  B the race: two minecarts summoned at the south ends with Motion [0, 0, -0.55] — the
    steel clamp keeps 0.55, the aluminium clamp pulls it to 0.20 (the moveMinecartOnRail
    setDeltaMovement clamp, both legs). Step B1 polls until the steel cart enters the far
    box (z <= 388, traveled >= 32) — tick-count-locked at ~62 ticks; B2 then asserts the
    aluminium cart has NOT entered its far box (z <= 394 would be 26+ blocks, unreachable
    at 0.20/tick in 62 ticks — the ladder ordering is wall-clock independent).
  C teardown: carts killed, lanes and floors back to air.

Band: x396..414, z368..424 — a fresh z=400 strip, disjoint from every registered band.
Two framework passes are the [0, 0] idempotency proof (every arm re-lays its boxes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_rails_ride.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — the fresh z=400 band, clear of every registered band.
STEEL = gt6world.Site(400, 64, 400, dx=0, dy=2, dz=24)   # the steel lane x400 z376..424
ALU = gt6world.Site(410, 64, 400, dx=0, dy=2, dz=24)     # the aluminium lane x410 z376..424
FLOORS = gt6world.Site(405, 63, 396, dx=9, dy=0, dz=24)  # both lane floors x396..414 z372..420

steps = []

# ------------------------------------------------- A: the registration face
steps += [
    phase("A: the two rail lanes — fill + blockstate readback (the registration face)"),
    Step("forceload add 396 372 414 424"),
    Step("fill 396 63 372 414 63 420 minecraft:smooth_stone"),
    Step("fill 400 64 380 400 64 420 gt6:rail_steel", expect="filled"),
    Step("fill 410 64 380 410 64 420 gt6:rail_aluminium", expect="filled"),
    Step("execute if block 400 64 400 gt6:rail_steel", expect="Test passed"),
    Step("execute if block 410 64 410 gt6:rail_aluminium", expect="Test passed"),
]

# ------------------------------------------------- B: the two-lane ladder race
steps += [
    phase("B: the race — same initial motion, steel clamps at 0.55, aluminium at 0.20"),
    Step("summon minecraft:minecart 400.5 64.0625 420.5 {Motion:[0.0,0.0,-0.55]}", expect="Summoned new minecart"),
    Step("summon minecraft:minecart 410.5 64.0625 420.5 {Motion:[0.0,0.0,-0.55]}", expect="Summoned new minecart"),
    # B1 — tick-count-locked: the steel cart covers 32+ blocks in ~62 ticks at 0.55/tick
    Step("execute if entity @e[type=minecraft:minecart,x=399,y=64,z=376,dx=2,dy=3,dz=12]",
         expect="Test passed", poll=25.0),
    # B2 — the ladder ordering: by the same tick count the aluminium cart (0.20/tick) has
    # covered < 26 blocks and cannot sit in the z <= 394 box (needs 130 ticks)
    Step("execute unless entity @e[type=minecraft:minecart,x=409,y=64,z=388,dx=2,dy=3,dz=6]",
         expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — carts killed, lanes and floors back to air"),
    Step("kill @e[type=minecraft:minecart,x=396,y=62,z=368,dx=18,dy=6,dz=56]"),
    Step("fill 400 64 380 400 64 420 minecraft:air"),
    Step("fill 410 64 380 410 64 420 minecraft:air"),
    Step("fill 396 63 372 414 63 420 minecraft:air"),
    Step("execute if block 400 64 400 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35-rails-ride",
    slug="p35_rails_ride",
    sites=gt6world.declare_sites(STEEL, ALU, FLOORS),
    preferred_ports=(26692, 26702),      # this card's pinned rcon/query pair
    response_timeout=30.0,
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
