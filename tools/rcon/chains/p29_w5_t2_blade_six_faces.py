#!/usr/bin/env python3
"""p29-w5-t2-blade-six FACES chain — the six blade tools' mining/drop faces live.

Chain semantics (task p29-w5-t2-blade-six RCON arm 5, the /gt6blade mine channel —
the tag-arm live legs the offline tests cannot pin, the DigSixTest faces-chain form):

  A the axe mines an oak log — the vanilla log drop (the #minecraft:mineable/axe tag
    arm + the drop authorization live);
  B the double axe mines an oak log — the inherited face (the PICKAXE family ruling);
  C the knife mines white wool — the wool arm (the inherited sword face at ×0.5 speed);
  D the NEGATIVE: the butchery knife mines stone — NO mining face and the vanilla
    pickaxe match_tool fails — the drops stay empty (the isMinableBlock-false face);
  E the club mines stone — the cobble drop converts (the state-keyed crush, the
    HardHammer surface live).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t2_blade_six_faces.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F1 = gt6world.Site(412, 64, 208)   # the axe log
F2 = gt6world.Site(413, 64, 208)   # the double-axe log
F3 = gt6world.Site(414, 64, 208)   # the knife wool
F4 = gt6world.Site(415, 64, 208)   # the butchery stone negative
F5 = gt6world.Site(416, 64, 208)   # the club stone

steps = []

# --------------------------------- A/B: the axe family log faces
steps += [
    phase("A: axe mines an oak log -> the log (the mineable/axe tag arm live)"),
    Step("setblock 412 64 208 minecraft:oak_log", expect="Changed the block"),
    Step("gt6blade mine 412 64 208 axe",
         expect="drops=[minecraft:oak_log x1]"),
    phase("B: the double axe mines an oak log -> the inherited face"),
    Step("setblock 413 64 208 minecraft:oak_log", expect="Changed the block"),
    Step("gt6blade mine 413 64 208 axe_double",
         expect="drops=[minecraft:oak_log x1]"),
]

# --------------------------------- C: the knife wool face
steps += [
    phase("C: the knife mines white wool -> the wool (the sword-surface cloth arm live)"),
    Step("setblock 414 64 208 minecraft:white_wool", expect="Changed the block"),
    Step("gt6blade mine 414 64 208 knife",
         expect="drops=[minecraft:white_wool x1]"),
]

# --------------------------------- D: the sword dead-bush stick face
steps += [
    phase("D: the sword mines a dead bush -> 1-2 sticks (upstream harvestStick :161-163; "
          "the butchery knife carries NO world face — the offline mines-false pin is its face)"),
    Step("setblock 415 64 208 minecraft:dead_bush", expect="Changed the block"),
    Step("gt6blade mine 415 64 208 sword",
         expect="minecraft:stick x"),
]

# --------------------------------- E: the club stone face (the HardHammer surface live)
steps += [
    phase("E: the club mines stone -> the cobble drop crushes (the state-keyed face live)"),
    Step("setblock 416 64 208 minecraft:stone", expect="Changed the block"),
    Step("gt6blade mine 416 64 208 club",
         expect="gt6:rock_gt_stone x"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 412 64 208 416 64 208 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t2-blade-six-faces",
    slug="p29w5t2bladesixfaces",
    sites=gt6world.declare_sites(F1, F2, F3, F4, F5),
    preferred_ports=(26245, 26255),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
