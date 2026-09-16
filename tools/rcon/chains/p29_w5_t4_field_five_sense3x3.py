#!/usr/bin/env python3
"""p29-w5-t4-field-five SENSE chain — the 3x3 grass-carpet live arm.

Chain semantics (task p29-w5-t4-field-five RCON arm 2, the /gt6field break channel —
the sweep + the SENSE_VEGETAL GLM, upstream GT_Tool_Sense.java:76-91/:87-88):

  A the sweep: a 3x3x1 grass carpet breaks 18 -> 9 in ONE /gt6field break — the 8
    neighbours pass the dig-speed gate (the plant family) and break inside the call;
    the grass_block FLOOR is off-surface (speed 0.0) and survives — the census pair is
    the sweep+gate verdict (the floor row IS the counted remainder);
  B no duplicate drops: every broken plant pays the plant item ITSELF x1 (the
    harvestGrass conversion REPLACES the vanilla seed roll — no wheat_seeds, no
    double-pay), 9 entities of exactly one item;
  C the hardness payment gate: plants are hardness 0 -> the upstream
    getDestroySpeed(level,pos) != 0 payment arm fires nowhere -> toolDamage 0
    (unit-pinned in FieldFiveTest; the census is the live face here).

Grass id forks per leg (the 1.20.3 rename): minecraft:grass / minecraft:short_grass —
node_cmds/node_expects swap ONLY the id, the judged geometry stays byte-identical.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t4_field_five_sense3x3.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

CARPET = gt6world.Site(406, 64, 225, dx=1, dy=2, dz=1)  # the 3x3 plant row + floor + headroom

steps = []

# --------------------------------- A+B: the one-call carpet sweep, self-drop singles
steps += [
    phase("A: the sense sweeps a 3x3 grass carpet in ONE break — the floor survives"),
    Step("fill 405 62 224 407 67 226 minecraft:air", expect="filled"),
    Step("fill 405 63 224 407 63 226 minecraft:grass_block", expect="filled"),
    Step("fill 405 64 224 407 64 226 minecraft:grass",
         expect="filled",
         node_cmds={"1.21.1": "fill 405 64 224 407 64 226 minecraft:short_grass"}),
    Step("gt6field break 406 64 225 sense",
         expect="destroyed=true, neighbours 18 -> 9 (broke 9), "
                "drops=[minecraft:grass x1] (count 9), toolDamage=0/2048",
         node_expects={"1.21.1": "destroyed=true, neighbours 18 -> 9 (broke 9), "
                                 "drops=[minecraft:short_grass x1] (count 9), toolDamage=0/2048"}),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 405 62 224 407 67 226 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t4-field-five-sense3x3",
    slug="p29w5t4fieldfivesense3x3",
    sites=gt6world.declare_sites(CARPET),
    preferred_ports=(26202, 26212),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
