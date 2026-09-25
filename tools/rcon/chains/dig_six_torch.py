#!/usr/bin/env python3
"""dig-six TORCH chain — the Behavior_Place_Torch live arm.

Chain semantics (task dig-six RCON arm 1, the /gt6dig place channel — the
command drives the item's own useOn so the inventory scan is the LIVE one):

  A the pickaxe/spade/shovel/universal_spade place a torch from the fake player's
    inventory onto a STONE floor block (stone = the no-path-arm floor: the
    spade/shovel/universal run the path arm BEFORE the torch arm — the upstream
    behavior order GT_Tool_Spade.java:110-113 — so a grass floor is consumed by
    Place_Path first; the grass-face conversion lives in the path chain) (upstream Behavior_Place_Torch.java:37-59 —
    tryPlaceItemIntoWorld counterpart = BlockItem.place, the torch lands on TOP,
    the stack shrinks 4 -> 3);
  B the second arm on the same tool: 3 -> 2 (the scan keeps finding the stack);
  C the bare negative: empty hand + torches in inventory = PASS (no tool arm);
  D the no-torch negative handled by the place report's torchesLeft verdict.

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its floor).

Run:  GT6_SESSION=off python3 tools/rcon/chains/dig_six_torch.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F1 = gt6world.Site(384, 64, 192)   # the pickaxe floor
F2 = gt6world.Site(385, 64, 192)   # the spade floor
F3 = gt6world.Site(386, 64, 192)   # the shovel floor
F4 = gt6world.Site(387, 64, 192)   # the universal spade floor
F5 = gt6world.Site(388, 64, 192)   # the bare-hand negative floor

steps = []

# --------------------------------- A: the four torch-carrying tools place from inventory
steps += [
    phase("A: pickaxe/spade/shovel/universal_spade place a torch from the inventory (Behavior_Place_Torch :37-59)"),
    Step(f"setblock 384 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig place 384 64 192 pickaxe",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=Block{minecraft:torch}"),
    Step(f"setblock 385 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig place 385 64 192 spade",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=Block{minecraft:torch}"),
    Step(f"setblock 386 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig place 386 64 192 shovel",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=Block{minecraft:torch}"),
    Step(f"setblock 387 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig place 387 64 192 universal_spade",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=Block{minecraft:torch}"),
]

# --------------------------------- B: the second placement (the scan keeps finding the stack)
# the arm-A torch must be cleared first: a second placement at an OCCUPIED spot is a
# vanilla canPlace refusal (PASS, no consumption — the upstream tryPlaceItemIntoWorld
# false arm, live in the first forge sweep). The /gt6dig place channel re-seeds the
# 4-torch stack per invocation, so the count resets to 4 -> 3; the within-one-call
# consumption face (4 -> 3 on CONSUME) is what arm A already pinned.
steps += [
    phase("B: the second arm — clear the landed torch, re-place succeeds again"),
    Step("setblock 384 65 192 minecraft:air", expect="Changed the block"),
    Step("gt6dig place 384 64 192 pickaxe",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=Block{minecraft:torch}"),
]

# --------------------------------- C: the bare negative (no tool, no arm)
steps += [
    phase("C: the bare negative — empty hand never runs the scan"),
    Step(f"setblock 388 64 192 minecraft:stone", expect="Changed the block"),
    Step("gt6dig place 388 64 192 bare", expect="torchPlaced=false, torchesLeft=4 (of 4)"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 384 64 192 388 66 192 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="dig-six-torch",
    slug="digsixtorch",
    sites=gt6world.declare_sites(F1, F2, F3, F4, F5),
    preferred_ports=(26195, 26205),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
