#!/usr/bin/env python3
"""p29-w5-t2-blade-six ROCK chain — the club rockGt crush live arm.

Chain semantics (task p29-w5-t2-blade-six RCON arm 3, the /gt6blade mine channel —
the CLUB_ROCK_CRUSH loot mode):

  A the club mines COBBLESTONE — the drop is REPLACED by 1-4 rockGt (Stone) items
    (upstream GT_Tool_Club.java:64-68, the :66 1+nextInt(4) row; the count varies so
    the assertion pins the item id only);
  B the club mines REDSTONE ORE — the Cinnabar gemChipped chips (the :104-108 row, the
    state-keyed face: the vanilla table drops nothing without a pickaxe, the conversion
    keys the state block);
  C the NEGATIVE: the sword mines cobblestone — the vanilla cobble drop rides untouched
    (the gt6:holds_tool gate keeps the conversion per-tool).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t2_blade_six_rock.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

R1 = gt6world.Site(398, 64, 208)   # the club-vs-cobblestone crush
R2 = gt6world.Site(399, 64, 208)   # the club-vs-redstone-ore chip
R3 = gt6world.Site(400, 64, 208)   # the sword-vs-cobblestone negative

steps = []

# --------------------------------- A: the cobblestone crush (rockGt Stone)
steps += [
    phase("A: club mines cobblestone -> 1-4 rockGt (Stone) replace the drop (mode CLUB_ROCK_CRUSH)"),
    Step("setblock 398 64 208 minecraft:cobblestone", expect="Changed the block"),
    Step("gt6blade mine 398 64 208 club",
         expect="gt6:rock_gt_stone x"),
]

# --------------------------------- B: the redstone ore crush (gemChipped Cinnabar)
steps += [
    phase("B: club mines redstone ore -> 1-4 Cinnabar chips (the state-keyed face)"),
    Step("setblock 399 64 208 minecraft:redstone_ore", expect="Changed the block"),
    Step("gt6blade mine 399 64 208 club",
         expect="gt6:gem_chipped_cinnabar x"),
]

# --------------------------------- C: the sword negative (the holds_tool gate)
steps += [
    phase("C: the sword mines cobblestone -> the vanilla cobble drop rides untouched"),
    Step("setblock 400 64 208 minecraft:cobblestone", expect="Changed the block"),
    Step("gt6blade mine 400 64 208 sword",
         expect="drops=[minecraft:cobblestone x1]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 398 64 208 400 64 208 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t2-blade-six-rock",
    slug="p29w5t2bladesixrock",
    sites=gt6world.declare_sites(R1, R2, R3),
    preferred_ports=(26243, 26253),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
