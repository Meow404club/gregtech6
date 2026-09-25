#!/usr/bin/env python3
"""boxinator — the Boxinator chain (task eu-hu-families, the EU
RM.Boxinator consumer :1635-1639, NO NBT_EFFICIENCY key = the 10000 identity, no
parallel, item in left|top → out right, energy bottom):

  the boxinator.json smoke row (the GT6_Main.java:350 verbatim shape: 8x paper +
  1x compass → 1x map, eUt 16, duration 16):
    T1 budget units(16×16×1, 10000, 10000, T) = 256 → 4 ticks @ 64 EU/tick —
    the deterministic completion out[0]=1x map.
  the window pin: minIn=16 recIn=32 maxIn=64 + parallel=1 parallelDuration=false.

The inventory merge carries BOTH row inputs (the press two-slot shape,
the loader-versioned Count/count fork). passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/boxinator.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

SITE_B1 = gt6world.Site(418, 64, 200, dy=1, dz=1)  # boxinator T1
B1 = F(SITE_B1)

BOX_MERGE = {
    "1.20.1": ('data merge block ' + B1 + ' {inventory:{Size:3,Items:['
               '{Slot:0b,id:"minecraft:paper",Count:8b},'
               '{Slot:1b,id:"minecraft:compass",Count:1b}]}}'),
    "1.21.1": ('data merge block ' + B1 + ' {inventory:{Size:3,Items:['
               '{Slot:0b,id:"minecraft:paper",count:8},'
               '{Slot:1b,id:"minecraft:compass",count:1}]}}'),
}

steps = [
    phase("A: the T1 boxinator — the smoke row at the 10000 identity, budget 256"),
    Step(f"gt6machine boxinator place {B1}", expect="GT6 boxinator placed"),
    Step(f"gt6machine boxinator check {B1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine boxinator check {B1}", expect="parallel=1 parallelDuration=false"),
    Step(BOX_MERGE["1.20.1"], expect="Modified block data", node_cmds={"1.21.1": BOX_MERGE["1.21.1"]}),
    Step(f"gt6machine boxinator inject 3 64 {B1}", expect="progress=192/256"),
    Step(f"gt6machine boxinator inject 1 64 {B1}", expect="progress=0/0"),
    Step(f"gt6machine boxinator check {B1}",
         expect="out[0]=1x map",
         node_expects={"1.21.1": "out[0]=1x minecraft:map"}),

    phase("B: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 417 62 199 419 67 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="boxinator",
    slug="boxinator",
    sites=gt6world.declare_sites(SITE_B1),
    preferred_ports=(26223, 26233),      # the card-D pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
