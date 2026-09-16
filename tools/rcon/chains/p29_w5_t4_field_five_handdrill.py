#!/usr/bin/env python3
"""p29-w5-t4-field-five HAND_DRILL chain — the declared-empty negative arm.

Chain semantics (task p29-w5-t4-field-five RCON arm 4, the /gt6field speed channel —
the mining-face read, upstream GT_Tool_HandDrill.java:33/:55):

  A the negative surface: on stone the drill reads speed=0.0 + correctForDrops=false —
    the isMiningTool-F face verbatim (the tool never accelerates mining and never
    authorises drops);
  B the structural emptiness: on diamond_ore the SAME pair — the empty TOOL_drill arm
    is material-independent (the declared empty set, not a soft gate).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t4_field_five_handdrill.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

READS = gt6world.Site(413, 64, 225, dz=1)  # the two speed-read targets (z-1 stone, z+1 ore)

steps = []

# --------------------------------- A+B: the zero-speed / no-drop double negative
steps += [
    phase("A: the hand drill reads zero speed and no drop authorization on stone"),
    Step("setblock 413 64 224 minecraft:stone", expect="Changed the block"),
    Step("gt6field speed 413 64 224 hand_drill",
         expect="speed=0.0, correctForDrops=false"),
    phase("B: the same pair on diamond_ore — the empty TOOL_drill arm is structural"),
    Step("setblock 413 64 226 minecraft:diamond_ore", expect="Changed the block"),
    Step("gt6field speed 413 64 226 hand_drill",
         expect="speed=0.0, correctForDrops=false"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 413 64 224 413 64 226 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t4-field-five-handdrill",
    slug="p29w5t4fieldfivehanddrill",
    sites=gt6world.declare_sites(READS),
    preferred_ports=(26215, 26216),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
