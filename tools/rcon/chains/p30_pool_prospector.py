#!/usr/bin/env python3
"""p30-pool-prospector — the /gt6tool prospect live acceptance chain (the p29-w5 band
form: band-local 9^3 envelope fills, one /gt6tool prospect arm per scenario; the
upstream ToolCompat.java:391-437 answer faces, byte-for-byte the :400-401 unlocalized
literals). The command drives the SAME GT6Prospector seam the hammer item's useOn runs
(the crowbar single-source ruling) with a fake player holding gt6:hammer — the give +
player-face useOn smoke; every successful arm reports hammerDamage=1/512 (the
512->511 durability face).

Chain semantics (task p30-pool-prospector ACCEPTANCE ③):

  A the uniform-stone arm: the 9^3 stone envelope answers "No traces of Ore found"
    (the ray sees four stone steps, the 16-point sampling hits stone only);
  B the lava arm: lava 2 below the click (the ray steps AWAY from the clicked UP face)
    -> "There is Lava behind this Rock";
  C the air-pocket arm: air 1 below -> "There is an Air Pocket behind this Rock";
  D the GT-stone boundary arm: clicked gt6:granite with gt6:marble 1 below (the
    block-identity compare — the per-pair universe has no meta axis, the declared
    equivalence face) -> "Material is changing behind this Rock";
  E the ore-trace arm: the envelope filled gt6:ore_small_stone_copper with a stone
    click column — the seeded 16-point sampling over the ore cube answers "Found
    traces of Copper"; plus the DIRECT ore click -> the :385 ore arm through
    getLocalName(OP.oreSmall, Copper) = "Small Copper Ore!" (toolDamage=100);
  F the negative arm: dirt answers nothing (toolDamage=0, hammerDamage=0/512,
    lines=[] — the upstream :372 return 0, no chat no payment).

Each scenario rigs its own x-disjoint 9^3 envelope (x596..656 at z364..372 — the ray
and the sampling stay inside the filled cube, so natural terrain cannot leak an answer).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p30_pool_prospector.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# five scenario rigs, x-disjoint envelopes (centers 600/612/624/636/648, the 9^3 fills),
# z=368 the fresh band (clear of the pocket z352 / rocks z300 strips)
CENTERS = [600, 612, 624, 636, 648]
Z = 368

steps = []

# ------------------------------------------------- A: the five prospect scenarios
steps += [
    phase("A1: uniform stone — No traces of Ore found (the :435 empty-answer row)"),
    Step("fill 596 60 364 604 68 372 minecraft:stone", expect="filled"),
    Step("gt6tool prospect 600 64 368 up", expect="lines=[No traces of Ore found]"),
    phase("A2: lava cavity 2 behind — There is Lava behind this Rock (:403)"),
    Step("fill 608 60 364 616 68 372 minecraft:stone", expect="filled"),
    Step("setblock 612 62 368 minecraft:lava", expect="Changed the block"),
    Step("gt6tool prospect 612 64 368 up", expect="lines=[There is Lava behind this Rock]"),
    phase("A3: air pocket 1 behind — There is an Air Pocket behind this Rock (:411)"),
    Step("fill 620 60 364 628 68 372 minecraft:stone", expect="filled"),
    Step("setblock 624 63 368 minecraft:air", expect="Changed the block"),
    Step("gt6tool prospect 624 64 368 up", expect="lines=[There is an Air Pocket behind this Rock]"),
    phase("A4: GT-stone boundary — clicked gt6:granite, gt6:marble 1 behind "
          "(the BlockStones.java:572 consumer face) -> Material is changing (:415)"),
    Step("fill 632 60 364 640 68 372 minecraft:stone", expect="filled"),
    Step("setblock 636 64 368 gt6:granite", expect="Changed the block"),
    Step("setblock 636 63 368 gt6:marble", expect="Changed the block"),
    Step("gt6tool prospect 636 64 368 up", expect="lines=[Material is changing behind this Rock]"),
    phase("A5: ore trace — the ore_small_stone_copper cube, the seeded 16-point sampling "
          "-> Found traces of Copper (:430); then the direct ore click -> the :385 arm"),
    Step("fill 644 60 364 652 68 372 gt6:ore_small_stone_copper", expect="filled"),
    # the stone click column: the clicked block + the four ray steps (the UP face walks down)
    Step("fill 648 60 368 648 64 368 minecraft:stone", expect="filled"),
    Step("gt6tool prospect 648 64 368 up", expect="lines=[Found traces of Copper]"),
    Step("gt6tool prospect 652 68 364 up", expect="lines=[Small Copper Ore!]"),
    Step("gt6tool prospect 652 68 364 up", expect="toolDamage=100"),
    phase("A6: the durability arm — every successful arm pays ONE point (512 -> 511)"),
    Step("gt6tool prospect 600 64 368 up", expect="hammerDamage=1/512"),
]

# ------------------------------------------------- B: the negative arm
steps += [
    phase("B: the negative — dirt answers nothing (:372 return 0: no chat, no payment)"),
    Step("setblock 656 64 368 minecraft:dirt", expect="Changed the block"),
    Step("gt6tool prospect 656 64 368 up", expect="lines=[]"),
    Step("gt6tool prospect 656 64 368 up", expect="toolDamage=0"),
]

# ------------------------------------------------- T: teardown
teardown = [phase("T: teardown — the envelopes back to air (the pass-open bbox is the backstop)")]
for x in CENTERS:
    teardown.append(Step(f"fill {x - 4} 60 {Z - 4} {x + 4} 68 {Z + 4} minecraft:air", expect="filled"))
teardown += [
    Step("setblock 656 64 368 minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]
steps += teardown

CHAIN = Chain(
    name="p30-pool-prospector",
    slug="p30poolprospector",
    sites=gt6world.declare_sites(
        *[gt6world.Site(x, 64, Z, dx=4, dy=4, dz=4) for x in CENTERS],
        gt6world.Site(656, 64, Z)),
    preferred_ports=(26540, 26550),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
