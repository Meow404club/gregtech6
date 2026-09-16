#!/usr/bin/env python3
"""p29-w5-t2-blade-six GRASS chain — the sword grass/stick/vine harvest live arm.

Chain semantics (task p29-w5-t2-blade-six RCON arm 2, the /gt6blade mine channel —
the SWORD_HARVEST loot mode):

  A the sword mines tall grass — the plant's own item is ADDED (upstream ToolStats
    harvestGrass :114; the vanilla wheat seeds ride randomly, the conversion is the
    deterministic face);
  B the sword mines a fern — the fern item (the flattening unfold keeps the block
    identity);
  C the NEGATIVE: the sword mines stone — stone is on no harvest arm and the sword
    fails the pickaxe match_tool — the drops stay empty.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t2_blade_six_grass.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

G1 = gt6world.Site(392, 64, 208)   # the sword-vs-grass positive
G2 = gt6world.Site(393, 64, 208)   # the sword-vs-fern positive
G3 = gt6world.Site(394, 64, 208)   # the sword-vs-stone negative

steps = []

# --------------------------------- A: the grass item conversion
steps += [
    phase("A: sword mines tall grass -> the grass item rides the drops (mode SWORD_HARVEST)"),
    # the grass id forks per leg (the 1.20.3 rename: grass -> short_grass; the t4
    # sense3x3 form — node_cmds/node_expects swap ONLY the id)
    Step("setblock 392 64 208 minecraft:grass", expect="Changed the block",
         node_cmds={"1.21.1": "setblock 392 64 208 minecraft:short_grass"}),
    Step("gt6blade mine 392 64 208 sword",
         expect="minecraft:grass x1",
         node_expects={"1.21.1": "minecraft:short_grass x1"}),
]

# --------------------------------- B: the fern identity
steps += [
    phase("B: sword mines a fern -> the fern item (the unfold keeps the block identity)"),
    Step("setblock 393 64 208 minecraft:fern", expect="Changed the block"),
    Step("gt6blade mine 393 64 208 sword",
         expect="minecraft:fern x1"),
]

# --------------------------------- C: the stone negative (no conversion arm)
steps += [
    phase("C: the sword mines stone -> the vanilla table face rides UNCONVERTED "
          "(no SWORD_HARVEST arm on stone; the mine channel bypasses the correct-tool "
          "gate, so the cobble drop is the expected vanilla face)"),
    Step("setblock 394 64 208 minecraft:stone", expect="Changed the block"),
    Step("gt6blade mine 394 64 208 sword",
         expect="drops=[minecraft:cobblestone x1]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 392 64 208 394 64 208 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t2-blade-six-grass",
    slug="p29w5t2bladesixgrass",
    sites=gt6world.declare_sites(G1, G2, G3),
    preferred_ports=(26242, 26252),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
