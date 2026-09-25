#!/usr/bin/env python3
"""rails-detector — the GT6 detector-track chain (task rails-31-blocks, ACCEPTANCE
rail-detector).

Chain semantics: the detector = the vanilla DetectorRailBlock engine over a GT6 material
row (the GT6DetectorRailBlock re-use ruling), so the chain pins the detection signal both
ways through the blockstate the engine owns:

  A the idle face: the rail reads gt6:rail_detector_steel[powered=false] and the lamp
    stacked above it (the strong-power-to-up face) reads [lit=false].
  B the DETECT face: a minecart summoned onto the rail flips the POWERED bit and lights
    the lamp (both polled — the cart AABB scan and the lamp update each need a tick).
  C the CLEAR face: the cart killed, the 20-tick scheduled reschedule drains, the bit and
    the lamp revert (both polled).
  D teardown.

Band: x397..403, z456..464 — the fresh z=400 strip, north of the booster lane.
Two framework passes are the [0, 0] idempotency proof (every arm re-lays its boxes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/rails_detector.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — the single detector rig at x400 z460.
RIG = gt6world.Site(400, 64, 460, dx=3, dy=4, dz=4)   # x397..403, z456..464

steps = []

# ------------------------------------------------- A: the idle face
steps += [
    phase("A: the detector rig — the rail and the lamp both idle"),
    Step("forceload add 396 456 404 464"),
    Step("setblock 400 63 460 minecraft:smooth_stone", expect="Changed the block"),
    Step("setblock 400 64 460 gt6:rail_detector_steel", expect="Changed the block"),
    Step("setblock 400 65 460 minecraft:redstone_lamp", expect="Changed the block"),
    Step("execute if block 400 64 460 gt6:rail_detector_steel[powered=false]", expect="Test passed"),
    Step("execute if block 400 65 460 minecraft:redstone_lamp[lit=false]", expect="Test passed"),
]

# ------------------------------------------------- B: the DETECT face
steps += [
    phase("B: the detect — a cart on the rail flips POWERED and lights the lamp"),
    Step("summon minecraft:minecart 400.5 64.0625 460.5", expect="Summoned new"),
    Step("execute if block 400 64 460 gt6:rail_detector_steel[powered=true]",
         expect="Test passed", poll=6.0),
    Step("execute if block 400 65 460 minecraft:redstone_lamp[lit=true]",
         expect="Test passed", poll=6.0),
]

# ------------------------------------------------- C: the CLEAR face
steps += [
    phase("C: the clear — the cart gone, the scheduled drain reverts both faces"),
    Step("kill @e[type=minecraft:minecart,x=397,y=62,z=456,dx=6,dy=6,dz=8]"),
    Step("execute if block 400 64 460 gt6:rail_detector_steel[powered=false]",
         expect="Test passed", poll=8.0),
    Step("execute if block 400 65 460 minecraft:redstone_lamp[lit=false]",
         expect="Test passed", poll=8.0),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the rig back to air"),
    Step("fill 400 63 460 400 65 460 minecraft:air"),
    Step("execute if block 400 64 460 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="rails-detector",
    slug="rails_detector",
    sites=gt6world.declare_sites(RIG),
    preferred_ports=(26692, 26702),      # this card's pinned rcon/query pair
    response_timeout=30.0,
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
