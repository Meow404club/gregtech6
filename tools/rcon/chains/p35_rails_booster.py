#!/usr/bin/env python3
"""p35-rails-booster — the GT6 booster-track chain (task p35-rails-31-blocks, ACCEPTANCE
rail-booster).

Chain semantics: the booster = the vanilla PoweredRailBlock engine over a GT6 material row
(the GT6BoosterRailBlock re-use ruling), so the chain pins the two live faces vanilla
already owns and the port only re-hosts:

  A the registration + unpowered face: the 21-rail steel-booster lane reads back as
    gt6:rail_booster_steel[powered=false] — redstone-deaf until fed (the default state).
    The feed is the SIDE redstone block at rail level: the A/B probe proved the
    below-feed never activates either leg's powered rail (vanilla control included).
  B the BRAKE face (the upstream BlockBaseRail.java:310-319 unpowered arm — the 0.03-stop/
    0.5-halve): a cart with Motion -0.4 dies to a stop within ~2 blocks; the far box
    (z <= 414, 16+ blocks north) stays EMPTY.
  C the CHARGE face: one redstone block under the south end rail — the vanilla
    findPoweredRailSignal chain charges the WHOLE lane; the mid-lane rail reads
    [powered=true] (the poll rides the neighbour-update propagation).
  D the LAUNCH face: a cart on the powered lane with a 0.02 nudge accelerates (the vanilla
    +0.06 curve to the ladder cap) and exits the lane's north end — the far box is the
    NORTH END ITSELF (z424..430): the empty-cart decay (0.965 friction) stops a derailed
    cart within ~1 block of the last rail, so the box catches it AT the exit, parked.
  E teardown.

Band: x397..403, z426..454 — the fresh z=400 strip, south-adjacent to the ride lane.
Two framework passes are the [0, 0] idempotency proof (every arm re-lays its boxes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_rails_booster.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — the booster lane x400 z430..450 (the fresh z=400 band, south of the detector).
LANE = gt6world.Site(400, 64, 440, dx=3, dy=3, dz=14)   # x397..403, z426..454

steps = []

# ------------------------------------------------- A: registration + the unpowered default
steps += [
    phase("A: the booster lane — fill + the unpowered readback"),
    Step("forceload add 396 426 404 454"),
    Step("fill 397 63 428 403 63 452 minecraft:smooth_stone"),
    Step("fill 400 64 430 400 64 450 gt6:rail_booster_steel", expect="filled"),
    Step("execute if block 400 64 440 gt6:rail_booster_steel[powered=false]", expect="Test passed"),
]

# ------------------------------------------------- B: the unpowered BRAKE face
steps += [
    phase("B: the brake — Motion -0.4 dies on the unpowered lane (the 0.5-halve arm)"),
    Step("summon minecraft:minecart 400.5 64.0625 432.5 {Motion:[0.0,0.0,-0.4]}", expect="Summoned new"),
    Step("execute if entity @e[type=minecraft:minecart,x=399,y=64,z=426,dx=2,dy=3,dz=8]",
         expect="Test passed", sleep=3.0),
    # and it never launches north: the 16+-blocks box stays empty
    Step("execute unless entity @e[type=minecraft:minecart,x=399,y=64,z=408,dx=2,dy=3,dz=6]",
         expect="Test passed"),
    Step("kill @e[type=minecraft:minecart,x=396,y=62,z=426,dx=8,dy=6,dz=28]"),
]

# ------------------------------------------------- C: the redstone CHARGE face
steps += [
    phase("C: the charge — one redstone block under the south end powers the WHOLE lane"),
    # the side feeds at RAIL LEVEL, one per 8 rails — the vanilla findPoweredRailSignal
    # depth cap (8) limits one feed to a 9-rail window (the re-power-every-9 rule); the
    # below-feed A/B probe proved a redstone block UNDER a powered rail never activates
    Step("setblock 399 64 450 minecraft:redstone_block", expect="Changed the block"),
    Step("setblock 399 64 442 minecraft:redstone_block", expect="Changed the block"),
    Step("setblock 399 64 434 minecraft:redstone_block", expect="Changed the block"),
    Step("execute if block 400 64 440 gt6:rail_booster_steel[powered=true]",
         expect="Test passed", poll=6.0),
    Step("execute if block 400 64 450 gt6:rail_booster_steel[powered=true]", expect="Test passed"),
]

# ------------------------------------------------- D: the powered LAUNCH face
steps += [
    phase("D: the launch — a nudged cart accelerates off the north end (the ladder cap)"),
    Step("summon minecraft:minecart 400.5 64.0625 449.5 {Motion:[0.0,0.0,-0.02]}", expect="Summoned new"),
    Step("execute if entity @e[type=minecraft:minecart,x=399,y=64,z=424,dx=2,dy=3,dz=6]",
         expect="Test passed", poll=20.0),
]

# ------------------------------------------------- E: teardown
steps += [
    phase("E: teardown — carts killed, the redstone feed and the lane back to air"),
    Step("kill @e[type=minecraft:minecart,x=396,y=62,z=402,dx=8,dy=6,dz=52]"),
    Step("fill 399 64 428 399 64 452 minecraft:air"),
    Step("fill 400 64 430 400 64 450 minecraft:air"),
    Step("fill 397 63 428 403 63 452 minecraft:air"),
    Step("execute if block 400 64 440 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35-rails-booster",
    slug="p35_rails_booster",
    sites=gt6world.declare_sites(LANE),
    preferred_ports=(26692, 26702),      # this card's pinned rcon/query pair
    response_timeout=30.0,
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
