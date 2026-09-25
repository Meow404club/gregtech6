#!/usr/bin/env python3
"""dig-six FACES chain — the mining-surface live legs (the offline-pure/tag
split: this chain carries the TAG arms the offline JVM cannot bind).

Chain semantics (task dig-six RCON arm 5 — the /gt6dig speed + mine channels,
the upstream isMinableBlock mappings LIVE):

  A the universal spade five-face: the speed read on vine / oak_leaves / rail / snow
    (layer) / snow_block — the ×0.75 surface speed 4.5 + correctForDrops=true on every
    leg (the mineable/leaves + sword_efficient + wool TAG arms and the pure plant/snow
    families together);
  B the universal negative: stone answers speed 1.0 / correct=false (NO pickaxe face
    upstream);
  C the pickaxe/construction speed legs: stone 6.0 / glass 6.0 / iron_ore 12.0->3.0
    (the construction pick's ore-stone penalty) / iron_ore 3.0 for the construction;
  D the break legs: the universal spade MINES the vine (breaks, no item drops — the
    shears-only loot) and the rail (the rail item drops — the drop authorization).

Run:  GT6_SESSION=off python3 tools/rcon/chains/dig_six_faces.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

X1 = gt6world.Site(408, 64, 192)   # the vine leg
X2 = gt6world.Site(409, 64, 192)   # the leaves leg
X3 = gt6world.Site(410, 64, 192)   # the rail leg
X4 = gt6world.Site(411, 64, 192)   # the snow legs
X5 = gt6world.Site(412, 64, 192)   # the stone negative
X6 = gt6world.Site(413, 64, 192)   # the pickaxe speed legs
X7 = gt6world.Site(414, 64, 192)   # the construction ore leg

steps = []

# --------------------------------- A: the universal five-face speed legs
steps += [
    phase("A: universal_spade speed 4.5 + correctForDrops on vine/leaves/rail/snow/snow_block"),
    Step(f"setblock 408 64 192 minecraft:vine", expect="Changed the block"),
    Step("gt6dig speed 408 64 192 universal_spade", expect="speed=4.5, correctForDrops=true"),
    Step(f"setblock 409 64 192 minecraft:oak_leaves", expect="Changed the block"),
    Step("gt6dig speed 409 64 192 universal_spade", expect="speed=4.5, correctForDrops=true"),
    Step(f"setblock 410 64 192 minecraft:rail", expect="Changed the block"),
    Step("gt6dig speed 410 64 192 universal_spade", expect="speed=4.5, correctForDrops=true"),
    # the support floor (the bbox cleanup strips y62..66 — a bare snow layer pops)
    Step(f"setblock 411 63 192 minecraft:stone", expect="Changed the block"),
    Step(f"setblock 411 64 192 minecraft:snow", expect="Changed the block"),
    Step("gt6dig speed 411 64 192 universal_spade", expect="speed=4.5, correctForDrops=true"),
    Step(f"setblock 411 64 192 minecraft:snow_block", expect="Changed the block"),
    Step("gt6dig speed 411 64 192 universal_spade", expect="speed=4.5, correctForDrops=true"),
]

# --------------------------------- B: the universal negative (no pickaxe face)
steps += [
    phase("B: universal_spade on stone — speed 1.0, not correct (the crowbar-adjacent cut)"),
    Step(f"setblock 412 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig speed 412 64 192 universal_spade", expect="speed=1.0, correctForDrops=false"),
]

# --------------------------------- C: the pickaxe family speed legs
steps += [
    phase("C: pickaxe 6.0 on stone/glass; construction 12.0 on stone and 3.0 on iron_ore"),
    Step(f"setblock 413 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig speed 413 64 192 pickaxe", expect="speed=6.0, correctForDrops=true"),
    Step(f"setblock 413 64 192 minecraft:glass", expect="Changed the block"),
    Step("gt6dig speed 413 64 192 pickaxe", expect="speed=6.0, correctForDrops=true"),
    Step(f"setblock 414 64 192 minecraft:iron_ore", expect="Changed the block"),
    Step("gt6dig speed 414 64 192 pickaxe_construction", expect="speed=3.0, correctForDrops=true"),
    Step(f"setblock 414 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig speed 414 64 192 pickaxe_construction", expect="speed=12.0, correctForDrops=true"),
]

# --------------------------------- D: the break legs (the mine channel)
steps += [
    phase("D: universal_spade breaks the vine (no drops) and the rail (the rail drops)"),
    Step(f"setblock 408 64 192 minecraft:vine", expect="Changed the block"),
    Step("gt6dig mine 408 64 192 universal_spade", expect="drops=[]"),
    Step(f"setblock 410 64 192 minecraft:rail", expect="Changed the block"),
    Step("gt6dig mine 410 64 192 universal_spade", expect="drops=[minecraft:rail x1]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 408 64 192 414 65 192 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="dig-six-faces",
    slug="digsixfaces",
    sites=gt6world.declare_sites(X1, X2, X3, X4, X5, X6, X7),
    preferred_ports=(26199, 26209),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
