#!/usr/bin/env python3
"""scene-six IGNITE chain — the flint-and-tinder chance-strike arm.

Chain semantics (task scene-six RCON arm 1, the /gt6scene6 ignite channel —
the command retries the item's own useOn until fire lands, so the upstream 30% roll
(GT6_Main.java:111 FlintAndSteelChance=30) becomes assertable; 60 strikes leave a
~5e-10 failure rate):

  A the flint strikes on a NETHERRACK floor (the eternal-fire-legal support) and the
    fire block lands on TOP;
  B the bare floor is re-laid per pass (the two framework passes are the [0, 0]
    idempotency proof — the previous pass's fire is wiped by the setblock).

Run:  GT6_SESSION=off python3 tools/rcon/chains/scene_six_ignite.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F1 = gt6world.Site(401, 64, 240)   # the netherrack strike floor

steps = []

# --------------------------------- A: the chance strike lands a fire
steps += [
    phase("A: the flint strikes the netherrack floor until fire lands (Behavior_FlintAndTinder :45-61)"),
    Step("setblock 401 64 240 minecraft:netherrack", expect="Changed the block"),
    Step("gt6scene6 ignite 401 64 240", expect="lit=true, attempts="),
    Step("execute if block 401 65 240 minecraft:fire", expect="Test passed"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 401 64 240 401 66 240 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="scene-six-ignite",
    slug="scenesixignite",
    sites=gt6world.declare_sites(F1),
    preferred_ports=(26215, 26225),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
