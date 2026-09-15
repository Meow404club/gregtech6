#!/usr/bin/env python3
"""p29_w5_t7_pocket_smoke — the pocket multitool smoke chain (task p29-w5-t7-pocket-eight,
the ACCEPTANCE RCON 替身面 arm 4: the chisel-form zero-behavior smoke + the mCheckTarget
negative — the switch must NOT fire when the target carries a tile entity, the upstream
Behavior_Switch_Metadata onItemUseFirst :43-50 gate).

Chain semantics (/gt6pocket use drives ONE real useOn dispatch with a fake player):

  A the zero-behavior arm: pocket_multitool_chisel NON-sneak at a bare stone — the stone
    gate finds no chisel recipe for vanilla stone (GTChiselItem.stoneToolClick returns 0),
    the block stays and the form stays: "result=PASS held=pocket_multitool_chisel block
    minecraft:stone->minecraft:stone" (the chisel decalcify/stone faces RIDE the delegated
    twin statics; their live behavior is the p16/p19/p21 chains' domain).

  B the mCheckTarget negative: pocket_multitool_saw SNEAK at a chest (a BE target) — the
    switch arm is blocked (no switch on a tile-entity target), the saw form has no BE
    face, so PASS and the held form survives.

  C the switch positive (bare sneak): pocket_multitool_chisel SNEAK at bare stone —
    CONSUME and the held stack wraps to the closed multitool (the :183 wrap hop).

  D the teardown. No other world state.

passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t7_pocket_smoke.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=352 W5 band — smoke column x404..405 (disjoint from ring x384 / faces x394).
SITE = gt6world.Site(404, 64, 352, dx=2, dy=0, dz=0)
STONE = "404 64 352"
CHEST = "405 64 352"

steps = []

# ------------------------------------------------- A: the zero-behavior arm
steps += [
    phase("A: the zero-behavior arm — chisel form, bare stone, non-sneak: PASS, nothing changes"),
    Step(f"setblock {STONE} minecraft:stone", expect="Changed the block"),
    Step(f"gt6pocket use chisel {STONE}",
         expect="result=PASS held=pocket_multitool_chisel@0 block minecraft:stone->minecraft:stone"),
]

# ------------------------------------------------- B: the mCheckTarget negative
steps += [
    phase("B: the mCheckTarget negative — sneak at a BE target never switches"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"gt6pocket use saw {CHEST} sneak",
         expect="result=PASS held=pocket_multitool_saw@0 block minecraft:chest->minecraft:chest"),
]

# ------------------------------------------------- C: the switch positive
steps += [
    phase("C: the switch positive — bare sneak target wraps chisel -> multitool"),
    Step(f"gt6pocket use chisel {STONE} sneak",
         expect="result=CONSUME held=pocket_multitool@0 block minecraft:stone->minecraft:stone"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the site restored to air (the pass-open bbox is the backstop)"),
    Step(f"setblock {STONE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {CHEST} minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w5_t7_pocket_smoke",
    slug="p29w5t7pocketsmoke",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26471, 26481),      # this card's pinned rcon/query pair (shared with the sibling chains)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
