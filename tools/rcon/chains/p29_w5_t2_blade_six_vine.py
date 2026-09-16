#!/usr/bin/env python3
"""p29-w5-t2-blade-six VINE chain — the sword vine harvest live arm.

Chain semantics (task p29-w5-t2-blade-six RCON arm 4, the /gt6blade mine channel):

  A the sword mines a VINE — the vanilla no-shears empty table is REPLACED by the vine
    itself (upstream GT_Tool_Sword.java:98-101 verbatim, the deterministic single drop);
  B the NEGATIVE: the axe mines a vine — the felling gate refuses (isFellable F on the
    vine, upstream :107 isWood F) and the vanilla table drops nothing — the drops stay
    empty.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t2_blade_six_vine.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

V1 = gt6world.Site(406, 64, 208)   # the sword-vs-vine positive
V2 = gt6world.Site(407, 64, 208)   # the axe-vs-vine negative

steps = []

# --------------------------------- A: the vine self-drop
steps += [
    phase("A: sword mines a vine -> the vine itself (mode SWORD_HARVEST, upstream :98-101)"),
    Step("setblock 406 64 208 minecraft:vine", expect="Changed the block"),
    Step("gt6blade mine 406 64 208 sword",
         expect="drops=[minecraft:vine x1]"),
]

# --------------------------------- B: the axe negative (the felling gate refuses)
steps += [
    phase("B: the axe mines a vine -> no felling, no drops (isFellable F, the empty table)"),
    Step("setblock 407 64 208 minecraft:vine", expect="Changed the block"),
    Step("gt6blade mine 407 64 208 axe",
         expect="drops=[]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 406 64 208 407 64 208 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t2-blade-six-vine",
    slug="p29w5t2bladesixvine",
    sites=gt6world.declare_sites(V1, V2),
    preferred_ports=(26244, 26254),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
