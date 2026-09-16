#!/usr/bin/env python3
"""p29-w5-t4-field-five PLOW chain — the 3x3x3 snow-sweep live arm.

Chain semantics (task p29-w5-t4-field-five RCON arm 1, the /gt6field break channel —
the REAL ServerPlayerGameMode.destroyBlock face whose mineBlock hook rides
GT6ToolSweep.sweep, upstream GT_Tool_Plow.java:67-75):

  A the sweep: a 3x3x3 snow_block cube breaks 27 -> 1 in ONE /gt6field break — the
    centre pays its point through mineBlock and the 26 neighbours pass the dig-speed
    gate (plow getDestroySpeed = 6.0 on the snow family) and break inside the same
    call (destroyBlock is one-shot removal, ServerPlayerGameMode.java:236);
  B the gate: the one off-surface corner (stone — plow speed 0.0 off the snow/fire
    family) SURVIVES the sweep — the 27 -> 1 census is the sweep+gate verdict in one
    number (the upstream getDigSpeed > 0 arm :71 verbatim);
  C the drops: every snow neighbour drops its vanilla snowballs (the plow has NO GLM —
    the loot seam is not the plow's face, the card RED LINE).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t4_field_five_plow3x3.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

CUBE = gt6world.Site(401, 65, 225, dx=1, dy=1, dz=1)  # the 3x3x3 snow cube, centre

steps = []

# --------------------------------- A+B: the one-call sweep with the off-surface corner
steps += [
    phase("A: the plow clears a 3x3x3 snow cube in ONE break — minus the off-surface corner"),
    Step("fill 400 64 224 402 66 226 minecraft:snow_block", expect="filled"),
    # the dig-speed gate target: stone is off the snow/fire family -> speed 0.0 -> skipped
    Step("setblock 400 64 224 minecraft:stone", expect="Changed the block"),
    Step("gt6field break 401 65 225 plow",
         expect="destroyed=true, neighbours 27 -> 1 (broke 26), drops=[minecraft:snowball"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 400 64 224 402 66 226 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t4-field-five-plow3x3",
    slug="p29w5t4fieldfiveplow3x3",
    sites=gt6world.declare_sites(CUBE),
    preferred_ports=(26201, 26211),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
