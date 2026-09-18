#!/usr/bin/env python3
"""p31-graagg — the Von da Graagg spawn-suppression tower chain (task p31-graagg,
the p31_graagg sweep group; Loader_MultiTileEntities.java:1280).

  the controller-anchored tower: cornerless 5x5x2 base of Dense Galvanized Steel
  Walls (18028) + the 5m Large Copper Coil pole (18040) + the 17 Dense Steel Wall
  (18029) top box; EU consumer, range = bind8(min(mEnergy, 4096)/16) <= 255,
  4096 EU/t drain; suppression rides the EntityJoinLevelEvent join gate
  (CheckSpawn has no modern counterpart — the coordinator-approved deviation).

acceptance arms this chain carries:
  1 form: the wrong-part rejection, then the all-green form (64 forming cells,
    the controller self-cell pass) + the FORMED blockstate assert
  2 powered suppression: gt.energy merged -> gt.range 255 (the bind8 ceiling,
    UT.java:1560 — "256" is display-only); a fresh summon at an in-range spot is
    ABSENT (EntityType.spawn succeeds even when the join is vetoed, so the
    entity-selector absence is the load-bearing assertion, not the command text)
  3 the mossy exemption: vanilla mossy_cobblestone under the spot AND the GT
    stone MCOBL variant (gt6:granite_black_cobble_mossy, the BlockStones.MCOBL
    meta-2 port) both let spawns through
  4 power-off restore: gt.energy 0 -> gt.range 0 -> the spot spawns again
  5 the chunk-reload pass: the disk-loaded survivor (summed unpowered BEFORE the
    power-on, forceload round-trip under full power) stays alive —
    loadedFromDisk joins are never suppressed (no reload apocalypse)

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_graagg.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

CTRL = gt6world.Site(392, 65, 412)            # the controller: bottom-centre of the base
TOWER = gt6world.Site(392, 65, 412, dx=2, dy=8, dz=2)  # the full tower footprint
SUP = gt6world.Site(396, 66, 412, dy=1)       # the in-range summon spot (tower-suppressed when powered)
MOSS = gt6world.Site(396, 65, 416, dy=1)      # the vanilla mossy exemption spot
GMOSS = gt6world.Site(396, 65, 420, dy=1)     # the GT stone MCOBL exemption spot
REL = gt6world.Site(402, 66, 412)            # the unpowered survivor (the reload arm; 6+ blocks clear of every kill radius)

GALV = "gt6:dense_wall_galvanized_steel"
COIL = "gt6:large_copper_coil"
STEEL = "gt6:dense_wall_steel"
C = gt6world.fmt(CTRL)


def zombie_at(x, y, z, radius=2):
    return "@e[type=minecraft:zombie,x=%d,y=%d,z=%d,distance=..%d]" % (x, y, z, radius)


steps = [
    phase("A: the site — the cornerless base, the coil pole, the top box, the controller"),
    Step("difficulty easy"),                                      # the arms need despawn-proof zombies; no judge — the response forks on already-set
    Step("fill 389 60 409 399 76 424 air", expect="filled"),      # the fresh band pre-clean
    Step("forceload add 389 409 399 424"),                        # the tick driver — no ticket, no BE tick,
                                                                  # no gt.range derivation (run-1 lesson)
    Step("fill 390 65 410 394 65 414 " + GALV, expect="filled"),  # base layer y+0 (the centre is re-set below)
    Step("setblock 390 65 410 air", expect="Changed the block"),  # the |i*j|==4 corners, layer y+0
    Step("setblock 394 65 410 air", expect="Changed the block"),
    Step("setblock 390 65 414 air", expect="Changed the block"),
    Step("setblock 394 65 414 air", expect="Changed the block"),
    Step("fill 390 66 410 394 66 414 " + GALV, expect="filled"),  # base layer y+1
    Step("setblock 390 66 410 air", expect="Changed the block"),  # the corners, layer y+1
    Step("setblock 394 66 410 air", expect="Changed the block"),
    Step("setblock 390 66 414 air", expect="Changed the block"),
    Step("setblock 394 66 414 air", expect="Changed the block"),
    Step("setblock " + C + " gt6:von_da_graagg", expect="Changed the block"),
    Step("fill 391 71 411 393 71 413 " + STEEL, expect="filled"), # the y+6 ring (the centre is re-coiled below)
    Step("setblock 391 70 412 " + STEEL, expect="Changed the block"),  # the y+5 cross
    Step("setblock 393 70 412 " + STEEL, expect="Changed the block"),
    Step("setblock 392 70 411 " + STEEL, expect="Changed the block"),
    Step("setblock 392 70 413 " + STEEL, expect="Changed the block"),
    Step("fill 391 72 412 393 72 412 " + STEEL, expect="filled"), # the y+7 cross (x arm)
    Step("setblock 392 72 411 " + STEEL, expect="Changed the block"),
    Step("setblock 392 72 413 " + STEEL, expect="Changed the block"),
    Step("fill 392 67 412 392 71 412 " + COIL, expect="filled"),  # the 5m coil pole (reclaims the y+6 centre)

    phase("B: the wrong-part rejection, then the all-green form"),
    Step("setblock 393 66 412 minecraft:stone", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock 393 66 412 " + GALV, expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:von_da_graagg[formed=true]", expect="Test passed"),

    phase("C: the unpowered survivor — summed BEFORE any power (the reload arm's subject)"),
    # NoAI keeps it on its block (the phases take wall-clock minutes of server time —
    # a free-roaming zombie wanders out of the selector box), PersistenceRequired belts
    # the despawn face; both are plain entity NBT (no item-component leg fork).
    Step("summon minecraft:zombie 402 66 412 {NoAI:1b,PersistenceRequired:1b}", expect="Summoned"),
    Step("execute if entity " + zombie_at(402, 66, 412), expect="Test passed"),

    phase("D: powered suppression — range 255 (the bind8 ceiling), the fresh summon is ABSENT"),
    Step("data merge block " + C + " {gt.energy:1000000000}", expect="Modified block data"),
    Step("data get block " + C, expect="gt.range: 255", poll=60),  # the tick derivation, the UT.java:1560 cap
    Step("summon minecraft:zombie 396 66 412", expect="Summoned"),  # EntityType.spawn succeeds even on a vetoed join
    Step("execute if entity " + zombie_at(396, 66, 412), expect="Test failed"),  # the suppression proof
    Step("data get block " + C, expect="gt.range: 255"),           # the cap holds while the store is deep

    phase("E: the vanilla mossy exemption — mossy_cobblestone under the spot"),
    Step("setblock 396 65 416 minecraft:mossy_cobblestone", expect="Changed the block"),
    Step("summon minecraft:zombie 396 66 416", expect="Summoned"),
    Step("execute if entity " + zombie_at(396, 66, 416), expect="Test passed"),
    Step("kill " + zombie_at(396, 66, 416), expect="Killed"),

    phase("F: the GT stone MCOBL exemption — gt6:granite_black_cobble_mossy (BlockStones.MCOBL meta 2)"),
    Step("setblock 396 65 420 gt6:granite_black_cobble_mossy", expect="Changed the block"),
    Step("summon minecraft:zombie 396 66 420", expect="Summoned"),
    Step("execute if entity " + zombie_at(396, 66, 420), expect="Test passed"),
    Step("kill " + zombie_at(396, 66, 420), expect="Killed"),

    phase("G: power-off restore — gt.range 0, the spot spawns again"),
    Step("data merge block " + C + " {gt.energy:0}", expect="Modified block data"),
    Step("data get block " + C, expect="gt.range: 0", poll=80),
    Step("summon minecraft:zombie 396 66 412", expect="Summoned"),
    Step("execute if entity " + zombie_at(396, 66, 412), expect="Test passed"),
    Step("kill " + zombie_at(396, 66, 412), expect="Killed"),

    phase("H: the chunk-reload pass — the disk-loaded survivor stays under full power"),
    Step("data merge block " + C + " {gt.energy:1000000000}", expect="Modified block data"),
    Step("data get block " + C, expect="gt.range: 255", poll=60),
    Step("forceload add 402 66 412"),                              # mark, then unmark: one deterministic round trip
    Step("forceload remove 402 66 412"),
    Step("time query daytime", expect="The time is"),              # the unload burn (8 round trips)
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("forceload add 402 66 412"),                              # the disk reload under the powered tower
    Step("execute if entity " + zombie_at(402, 66, 412), expect="Test passed", poll=30),  # loadedFromDisk passes

    phase("I: teardown — the explicit band restore"),
    Step("kill @e[type=minecraft:zombie,x=389,y=60,z=409,dx=16,dy=20,dz=20]", expect="Killed"),
    Step("fill 389 60 409 399 76 424 air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p31-graagg p31_graagg",
    slug="p31graagg",
    sites=gt6world.declare_sites(TOWER, SUP, MOSS, GMOSS, REL),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
