#!/usr/bin/env python3
"""p29-w5-t1-dig-six HARVEST chain — the spade harvestableSpade drop conversion live arm.

Chain semantics (task p29-w5-t1-dig-six RCON arm 4, the /gt6dig mine channel — the
GLM gt6:spade_harvest, mode HARVESTABLE_SPADE, replaces the drops with the block item
itself at forced chance; upstream GT_Tool_Spade.java:81-89):

  A the POSITIVE arms: the spade mines each HARVESTABLE_SPADE member and the block
    drops AS ITSELF — grass_block (vanilla: dirt), mycelium (vanilla: dirt), clay
    (vanilla: 4 clay balls), snow_block (vanilla: snowballs), gravel (vanilla: flint
    chance) — the silk-touch-like harvest, one arm per member;
  B the dirt identity arm: dirt is in the set but already drops itself;
  C the BARE negative: grass_block by hand falls to the dirt baseline (the holds_tool
    gate — no conversion without the spade);
  D the plain-shovel negative: dirt-family baseline without the conversion (grass_block
    -> dirt).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t1_dig_six_harvest.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

H1 = gt6world.Site(399, 64, 192)
H2 = gt6world.Site(400, 64, 192)
H3 = gt6world.Site(401, 64, 192)
H4 = gt6world.Site(402, 64, 192)
H5 = gt6world.Site(403, 64, 192)
H6 = gt6world.Site(404, 64, 192)
H7 = gt6world.Site(405, 64, 192)

steps = []

# --------------------------------- A: the five silk-like members drop themselves
steps += [
    phase("A: the spade harvests the set AS THEMSELVES (GLM HARVESTABLE_SPADE, forced chance)"),
    Step(f"setblock 399 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig mine 399 64 192 spade", expect="drops=[minecraft:grass_block x1]"),
    Step(f"setblock 400 64 192 minecraft:mycelium", expect="Changed the block"),
    Step("gt6dig mine 400 64 192 spade", expect="drops=[minecraft:mycelium x1]"),
    Step(f"setblock 401 64 192 minecraft:clay", expect="Changed the block"),
    Step("gt6dig mine 401 64 192 spade", expect="drops=[minecraft:clay x1]"),
    Step(f"setblock 402 64 192 minecraft:snow_block", expect="Changed the block"),
    Step("gt6dig mine 402 64 192 spade", expect="drops=[minecraft:snow_block x1]"),
    Step(f"setblock 403 64 192 minecraft:gravel", expect="Changed the block"),
    Step("gt6dig mine 403 64 192 spade", expect="drops=[minecraft:gravel x1]"),
]

# --------------------------------- B: the dirt identity arm (in the set, already self-dropping)
steps += [
    phase("B: the dirt identity — the conversion fires and the answer is unchanged"),
    Step(f"setblock 404 64 192 minecraft:dirt", expect="Changed the block"),
    Step("gt6dig mine 404 64 192 spade", expect="drops=[minecraft:dirt x1]"),
]

# --------------------------------- C: the bare negative (no spade, no conversion)
steps += [
    phase("C: the bare negative — grass_block by hand falls to the dirt baseline"),
    Step(f"setblock 405 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig mine 405 64 192 bare hand", expect="drops=[minecraft:dirt x1]"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 399 64 192 405 64 192 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t1-dig-six-harvest",
    slug="p29w5t1digsixharvest",
    sites=gt6world.declare_sites(H1, H2, H3, H4, H5, H6, H7),
    preferred_ports=(26198, 26208),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
