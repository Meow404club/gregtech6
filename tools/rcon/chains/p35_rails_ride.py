#!/usr/bin/env python3
"""p35-rails-ride — the GT6 rail RIDE chain (task p35-rails-31-blocks, ACCEPTANCE rail-ride).

Chain semantics: the per-rail speed ladder (the census-erratum direct translation through
the getRailMaxSpeed hook on both legs) is proven LIVE by a two-lane race on POWERED lanes
— the vanilla empty-cart decay (isVehicle ? 0.997 : 0.965 friction, AbstractMinecart
moveAlongTrack) kills an unpowered glide within ~12 blocks, so the race rides the
self-sustaining boost form: a redstone-fed steel booster lane (ladder cap 0.60/tick)
against a redstone-fed aluminium booster lane (ladder cap 0.20/tick). The boost adds
+0.06/tick until the RAIL CAP clamps — the cap IS the ladder value being proven, so the
terminal speeds are the loader columns:

  A the registration face: both lanes fill + blockstate readback, the redstone feeds
    placed, the chain charge read back [powered=true].
  B the race: two carts summoned at the south ends with Motion [0, 0, -0.02] (the nudge;
    the boost does the rest). B1 polls until the steel cart enters the far box
    (z <= 388, traveled >= 32) — ~65-100 ticks at the 0.60 cap. B2 then asserts the
    aluminium cart has NOT entered its far box (z <= 394 would be 26+ blocks, needing
    133+ ticks at the 0.20 cap — unreachable while B1's box is reachable in 65; the
    ladder ordering is tick-count-locked, wall-clock independent).
  C teardown: carts killed, lanes/floors/feeds back to air.

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
STEEL = gt6world.Site(400, 64, 400, dx=1, dy=2, dz=24)   # the steel booster lane x399..401 z376..424
ALU = gt6world.Site(410, 64, 400, dx=1, dy=2, dz=24)     # the aluminium booster lane x409..411 z376..424
FLOORS = gt6world.Site(405, 63, 396, dx=9, dy=0, dz=24)  # both lane floors x396..414 z372..420

steps = []

# ------------------------------------------------- A: registration + the charge face
steps += [
    phase("A: the two booster lanes — fill, readback, redstone feed, chain charge"),
    Step("forceload add 396 372 414 424"),
    Step("fill 396 63 372 414 63 420 minecraft:smooth_stone"),
    Step("fill 400 64 380 400 64 420 gt6:rail_booster_steel", expect="filled"),
    Step("fill 410 64 380 410 64 420 gt6:rail_booster_aluminium", expect="filled"),
    Step("execute if block 400 64 400 gt6:rail_booster_steel", expect="Test passed"),
    Step("execute if block 410 64 400 gt6:rail_booster_aluminium", expect="Test passed"),
    # the side feeds at RAIL LEVEL, one per 8 rails BOTH lanes — the vanilla
    # findPoweredRailSignal depth cap (8) limits one feed to a 9-rail window (the classic
    # vanilla re-power-every-9 rule; the below-feed A/B probe also proved a redstone block
    # UNDER a powered rail — gt6 OR vanilla — never activates it)

]

# the feeds: one per 8 rails both lanes (the re-power-every-9 vanilla rule)
for tZ in (420, 412, 404, 396, 388, 380):
    steps.append(Step(f"setblock 399 64 {tZ} minecraft:redstone_block", expect="Changed the block"))
    steps.append(Step(f"setblock 409 64 {tZ} minecraft:redstone_block", expect="Changed the block"))

steps += [
    Step("execute if block 400 64 400 gt6:rail_booster_steel[powered=true]",
         expect="Test passed", poll=6.0),
    Step("execute if block 410 64 400 gt6:rail_booster_aluminium[powered=true]",
         expect="Test passed", poll=6.0),
]

# ------------------------------------------------- B: the two-lane ladder race
steps += [
    phase("B: the race — the boost sustains both, the ladder caps split them"),
    Step("summon minecraft:minecart 400.5 64.0625 420.5 {Motion:[0.0,0.0,-0.02]}", expect="Summoned new"),
    Step("summon minecraft:minecart 410.5 64.0625 420.5 {Motion:[0.0,0.0,-0.02]}", expect="Summoned new"),
    # B1 — the steel cart at the 0.60 cap covers 32+ blocks in ~65 ticks
    Step("execute if entity @e[type=minecraft:minecart,x=399,y=64,z=376,dx=2,dy=3,dz=12]",
         expect="Test passed", poll=25.0),
    # B2 — the ladder ordering: by the same tick count the aluminium cart (0.20 cap) has
    # covered < 26 blocks and cannot sit in the z <= 394 box (needs 133 ticks)
    Step("execute unless entity @e[type=minecraft:minecart,x=409,y=64,z=388,dx=2,dy=3,dz=6]",
         expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — carts killed, lanes/floors/feeds back to air"),
    Step("kill @e[type=minecraft:minecart,x=396,y=62,z=368,dx=18,dy=6,dz=56]"),
    Step("fill 400 64 380 400 64 420 minecraft:air"),
    Step("fill 410 64 380 410 64 420 minecraft:air"),
    Step("fill 399 64 372 399 64 424 minecraft:air"),
    Step("fill 409 64 372 409 64 424 minecraft:air"),
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
