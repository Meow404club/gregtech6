#!/usr/bin/env python3
"""p29-w5-t5-scene-six DRAIN chain — the plunger 1000 L void-drain arm (Behavior_Plunger_Fluid :48-62).

Chain semantics (task p29-w5-t5-scene-six RCON arm 5, the /gt6scene6 use channel over
the platform fluid-capability seam — the pipe domain exposes the handler through
SideFluidHandler, so the plunger needs NO new seam; the command's fill face is the
test fixture that feeds the pipe):

  A a WOOD FLUID BARREL (16000 L, gt6:barrel_wood) is laid, the fixture fills it with
    1000 mB of water, the PLUNGER voids it — the report names the one-point durability
    payment (the upstream getToolDamagePerDropConversion() = 100 units through the
    10000=1 mapping); the SECOND use is the negative face (the empty handler PASSes,
    the command reports NO-OP);
  B the re-fill per pass is the [0, 0] idempotency proof (the live drain arm — the
    barrel BE persists across the two passes, the teardown fill clears the band).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t5_scene_six_drain.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

P1 = gt6world.Site(407, 64, 240)   # the wood fluid pipe

steps = []

# --------------------------------- A: fill the pipe, the plunger voids 1000 mB
steps += [
    phase("A: the plunger voids the filled barrel (Behavior_Plunger_Fluid.java:48-62, one point per drain)"),
    Step("setblock 407 64 240 gt6:barrel_wood", expect="Changed the block"),
    Step("gt6scene6 fill 407 64 240 1000", expect="filled=1000"),
    Step("gt6scene6 use 407 64 240 plunger", expect="use check OK"),
    Step("gt6scene6 use 407 64 240 plunger", expect="use NO-OP"),
]

# --------------------------------- B: the re-lay + re-fill (the pass-2 rerun face)
steps += [
    phase("B: the barrel re-filled — the pass-2 rerun (the drained BE would PASS)"),
    Step("gt6scene6 fill 407 64 240 1000", expect="filled=1000"),
    Step("gt6scene6 use 407 64 240 plunger", expect="use check OK"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 407 64 240 407 66 240 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t5-scene-six-drain",
    slug="p29w5t5scenesixdrain",
    sites=gt6world.declare_sites(P1),
    preferred_ports=(26215, 26225),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
