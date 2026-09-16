#!/usr/bin/env python3
"""p29-w5-t5-scene-six SCOOP chain — the cobweb/vine full-drop arm (the PLANT_SELF_DROP loot seam).

Chain semantics (task p29-w5-t5-scene-six RCON arm 4, the /gt6scene6 mine channel over
the GLOBAL loot modifier chain — the vanilla match_tool predicate is the bare-shears
ITEM identity and cannot see gt6:scoop, so the drop path IS the gt6:gt6_tool_convert
modifier gated by gt6:holds_tool):

  A the SCOOP mines a COBWEB: the web drops ITSELF (the vanilla table's shears gate
    would have dropped nothing for an unknown tool — the GLM replaces the empty drop
    list with the block item);
  B the SCOOP mines a VINE: the vine drops ITSELF (the upstream :88 vine arm mapped to
    the scoop's shears-class face);
  C the bare re-lay per pass is the [0, 0] idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t5_scene_six_scoop.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

C1 = gt6world.Site(405, 64, 240)   # the cobweb block
V1 = gt6world.Site(406, 64, 240)   # the vine block

steps = []

# --------------------------------- A: the scoop full-drops the cobweb
steps += [
    phase("A: the scoop mines the cobweb — the web drops ITSELF (PLANT_SELF_DROP, holds_tool gt6:scoop)"),
    Step("setblock 405 64 240 minecraft:cobweb", expect="Changed the block"),
    Step("gt6scene6 mine 405 64 240 scoop", expect="drops=[minecraft:cobweb"),
]

# --------------------------------- B: the scoop full-drops the vine
steps += [
    phase("B: the scoop mines the vine — the vine drops ITSELF (the upstream :88 arm)"),
    Step("setblock 406 64 240 minecraft:vine", expect="Changed the block"),
    Step("gt6scene6 mine 406 64 240 scoop", expect="drops=[minecraft:vine"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 405 64 240 406 66 240 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t5-scene-six-scoop",
    slug="p29w5t5scenesixscoop",
    sites=gt6world.declare_sites(C1, V1),
    preferred_ports=(26215, 26225),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
