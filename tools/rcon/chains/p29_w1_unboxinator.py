#!/usr/bin/env python3
"""p29-w1-unboxinator — the Unboxinator chain (task p29-w1-eu-hu-families, the EU
RM.Unboxinator consumer :1642-1646, NO efficiency key, no parallel, the SAME boxinator
masks; the map is the BASE-RecipeMap form — the upstream RecipeMapUnboxinator
.java:43-89 loot runtime-synthesis arm is POOLED, the GT6RecipeMaps.UNBOXINATOR field
doc + the offline GT6EuHuFamiliesRowTest pool assertion):

  the unboxinator.json smoke row (the declared reverse of the boxinator row:
  1x map → 8x paper + 1x compass, eUt 16, duration 16):
    T1 budget units(16×16×1, 10000, 10000, T) = 256 → 4 ticks @ 64 EU/tick —
    the deterministic two-slot completion out[0]=8x paper + out[1]=1x compass.

The `input 1` feed is the map (the smoke row's single item input — the family
literal's feed). passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_unboxinator.py
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

SITE_U1 = gt6world.Site(426, 64, 200, dy=1, dz=1)  # unboxinator T1
U1 = F(SITE_U1)

steps = [
    phase("A: the T1 unboxinator — the reverse smoke row, budget 256, the loot arm absent"),
    Step(f"gt6machine unboxinator place {U1}", expect="GT6 unboxinator placed"),
    Step(f"gt6machine unboxinator check {U1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine unboxinator input 1 {U1}",
         expect="GT6 unboxinator input: 1x map into slot 0",
         node_expects={"1.21.1": "GT6 unboxinator input: 1x minecraft:map into slot 0"}),
    Step(f"gt6machine unboxinator inject 3 64 {U1}", expect="progress=192/256"),
    Step(f"gt6machine unboxinator inject 1 64 {U1}", expect="progress=0/0"),
    Step(f"gt6machine unboxinator check {U1}",
         expect="out[0]=8x paper out[1]=1x compass",
         node_expects={"1.21.1": "out[0]=8x minecraft:paper out[1]=1x minecraft:compass"}),

    phase("B: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 425 62 199 427 67 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w1-unboxinator",
    slug="p29w1unboxinator",
    sites=gt6world.declare_sites(SITE_U1),
    preferred_ports=(26224, 26234),      # the card-D pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
