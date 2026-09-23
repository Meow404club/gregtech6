#!/usr/bin/env python3
"""p35-portal-nether — the Miniature Nether Portal live chain (task
p35-portals-mini-nether-end, RCON group p35_portals). The CROSS-DIMENSION triple
live proof: item + fluid + redstone relayed between the OVERWORLD and the NETHER
through the paired portals (the forceload placement = the user chunkload
responsibility, the upstream tooltip-only semantics — no forced loading API).

  Rig (the mirror geometry the OPOS delegation demands):
    OVERWORLD  A=(448,65,358) the portal
               hopper (448,66,358) facing=down, pushes into A's UP face
                 -> delegate = B.relative(OPOS[UP]=DOWN) -> the chest UNDER B
               redstone_block (448,65,357) north of A
                 -> A's NORTH face reads 15 -> B.mRedstone[OPOS[NORTH]=SOUTH]
                    -> mRedstone[s] answers queries with d=s, so the receiver
                    sits on the OPOS mirror: the lamp SOUTH of B lights
               fluid pipe (449,65,358) east of A
                 -> A's EAST face -> delegate = B.relative(OPOS[EAST]=WEST)
                    -> the wood barrel WEST of B
    NETHER     B=(56,65,44) the portal
               chest (56,64,44) | lamp (56,65,43) | barrel (55,65,44)

  A place -> hopper armed -> nether forceload+rig -> ignite both -> pairing
  checks both sides (x8/128m) -> item poll (chest gains cobblestone) -> pipe
  place+inject -> barrel poll (water) -> lamp poll (lit=true) -> extinguish +
  source removal -> lamp dark poll (the 20t watchdog decay, live).

  The nether band teardown is explicit (the framework bbox cleanup is
  overworld-only) — `execute in` fill + forceload remove all.

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_portal_nether.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)
if str(_HERE.parent) not in sys.path:
    sys.path.insert(0, str(_HERE.parent))

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# the fresh overworld band (east of the p34 z=350 strip; x446..451, z356..360)
A = gt6world.Site(448, 65, 358)       # the overworld portal
HOPPER = gt6world.Site(448, 66, 358)  # the item pusher above A
RSRC = gt6world.Site(448, 65, 357)    # the redstone source north of A
PIPE = gt6world.Site(449, 65, 358)    # the fluid pusher east of A

NETH = "execute in minecraft:the_nether run "
B = "56 65 44"                        # the nether portal
CHEST = "56 64 44"                    # the item sink under B
LAMP = "56 65 45"                     # the relay lamp south of B (the OPOS mirror of the source face)
BARREL = "55 65 44"                   # the fluid sink west of B

steps = [
    phase("A: the overworld rig — portal + hopper (UP face) + redstone source (NORTH face)"),
    Step(f"setblock {F(A)} gt6:mini_portal_nether", expect="Changed the block"),
    Step(f"setblock {F(HOPPER)} minecraft:hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {F(HOPPER)} container.0 with minecraft:cobblestone 16", expect="Replaced"),
    Step(f"setblock {F(RSRC)} minecraft:redstone_block", expect="Changed the block"),

    phase("B: the nether rig — forceload (the user chunkload duty) + portal + sinks"),
    Step(NETH + "forceload add 48 32 63 47", expect="to be force loaded", sleep=1.0),
    Step(NETH + f"setblock {B} gt6:mini_portal_nether", expect="Changed the block"),
    Step(NETH + f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(NETH + f"setblock {LAMP} minecraft:redstone_lamp", expect="Changed the block"),
    Step(NETH + f"setblock {BARREL} gt6:barrel_wood", expect="Changed the block"),

    phase("C: ignite both — the ×8/128m pairing establishes across the dimensions"),
    Step(f"gt6portal ignite {F(A)}", expect="active=true"),
    Step(NETH + f"gt6portal ignite {B}", expect="active=true"),
    Step(f"gt6portal check {F(A)}", expect="target=X: 56   Y: 65   Z: 44",
         poll=60.0),  # the OW portal pairs the nether one (dx=0, dz=2 under the ×8 factor)
    Step(NETH + f"gt6portal check {B}", expect="target=X: 448   Y: 65   Z: 358",
         poll=60.0),  # the nether portal pairs back (the mirrored factor arm)

    phase("D: the ITEM leg — hopper -> A's UP face -> the chest under B"),
    Step(NETH + f"data get block {CHEST} Items",
         expect="cobblestone",
         poll=120.0),  # the hopper pushes through the portal capability within ~8t batches

    phase("E: the FLUID leg — pipe -> A's EAST face -> the barrel west of B"),
    Step(f"gt6pipe place {F(PIPE)} 5", expect="connections 16"),  # WEST bit — the connect answered through the relay (the nether barrel's getTanks)
    Step(f"gt6pipe toggle {F(PIPE)} 2", expect="connections 20"),  # the north air mouth (SBIT 2|16) — the inject inlet
    Step(f"gt6pipe inject {F(PIPE)} 2 1000", expect="GT6 pipe inject"),
    Step(NETH + f"gt6tank show {BARREL}",
         expect="minecraft:water",
         poll=120.0),  # the pipe distribute round pushes through the portal capability

    phase("F: the REDSTONE leg — the source on A's NORTH face lights the lamp on B's OPOS mirror (south)"),
    Step(NETH + f"execute if block {LAMP} minecraft:redstone_lamp[lit=true]",
         expect="Test passed",
         poll=60.0),

    phase("G: extinguish + source removal — the 20t watchdog decays the relay live"),
    Step(f"gt6portal extinguish {F(A)}", expect="active=false"),
    Step(f"setblock {F(RSRC)} air", expect="Changed the block"),
    Step(NETH + f"execute if block {LAMP} minecraft:redstone_lamp[lit=false]",
         expect="Test passed",
         poll=60.0),  # the watchdog zeroes B's mRedstone after <=20t (upstream :126-133)

    phase("H: teardown — both dimensions restored"),
    Step(NETH + f"setblock {B} air", expect="Changed the block"),
    Step(NETH + f"setblock {CHEST} air", expect="Changed the block"),
    Step(NETH + f"setblock {LAMP} air", expect="Changed the block"),
    Step(NETH + f"setblock {BARREL} air", expect="Changed the block"),
    Step(NETH + "forceload remove all", expect="removed", allow_failed=True),
    Step(f"fill 446 62 355 451 68 361 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the group key: sweep --group p35_portals matches
    name="p35-portal-nether p35_portals",
    slug="p35portalnether",
    sites=gt6world.declare_sites(A, HOPPER, RSRC, PIPE),
    preferred_ports=(26692, 26702),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
