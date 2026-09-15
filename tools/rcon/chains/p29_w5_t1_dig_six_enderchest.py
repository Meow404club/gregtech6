#!/usr/bin/env python3
"""p29-w5-t1-dig-six ENDER CHEST chain — the construction-pick drop conversion live arm.

Chain semantics (task p29-w5-t1-dig-six RCON arm 3, the /gt6dig mine channel — the
GLOBAL loot modifier chain fires inside the dropResources TOOL context):

  A the POSITIVE: the construction pickaxe mines an ender chest and the GLM
    (gt6:construction_ender_chest, mode ENDER_CHEST_SELF) replaces the vanilla 8-obsidian
    drop with the chest itself (upstream GT_Tool_PickaxeConstruction.java:53-59 verbatim);
  B the NEGATIVE: the plain pickaxe mines the same block — vanilla drops 8 obsidian
    (the conversion is per-tool identity, the gt6:holds_tool gate);
  C the BARE negative: the empty hand also falls to the 8-obsidian baseline.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t1_dig_six_enderchest.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

E1 = gt6world.Site(394, 64, 192)   # the construction-pick chest
E2 = gt6world.Site(395, 64, 192)   # the plain-pick negative
E3 = gt6world.Site(396, 64, 192)   # the bare-hand negative

steps = []

# --------------------------------- A: the conversion (the GLM replaces the obsidian)
steps += [
    phase("A: pickaxe_construction mines the ender chest -> the chest itself (GLM ENDER_CHEST_SELF)"),
    Step(f"setblock 394 64 192 minecraft:ender_chest", expect="Changed the block"),
    Step("gt6dig mine 394 64 192 pickaxe_construction",
         expect="drops=[minecraft:ender_chest x1]"),
]

# --------------------------------- B: the plain pickaxe negative (vanilla 8 obsidian)
steps += [
    phase("B: the plain pickaxe — the vanilla 8-obsidian baseline (the holds_tool gate)"),
    Step(f"setblock 395 64 192 minecraft:ender_chest", expect="Changed the block"),
    Step("gt6dig mine 395 64 192 pickaxe",
         expect="drops=[minecraft:obsidian x8]"),
]

# --------------------------------- C: the bare-hand negative
steps += [
    phase("C: the bare hand — the same 8-obsidian baseline"),
    Step(f"setblock 396 64 192 minecraft:ender_chest", expect="Changed the block"),
    Step("gt6dig mine 396 64 192 bare hand",
         expect="drops=[minecraft:obsidian x8]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 394 64 192 396 64 192 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t1-dig-six-enderchest",
    slug="p29w5t1digsixenderchest",
    sites=gt6world.declare_sites(E1, E2, E3),
    preferred_ports=(26197, 26207),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
