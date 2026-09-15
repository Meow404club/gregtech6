#!/usr/bin/env python3
"""p29-w5-t1-dig-six TORCH chain — the Behavior_Place_Torch live arm.

Chain semantics (task p29-w5-t1-dig-six RCON arm 1, the /gt6dig place channel — the
command drives the item's own useOn so the inventory scan is the LIVE one):

  A the pickaxe/spade/shovel/universal_spade place a torch from the fake player's
    inventory onto a floor block (upstream Behavior_Place_Torch.java:37-59 —
    tryPlaceItemIntoWorld counterpart = BlockItem.place, the torch lands on TOP,
    the stack shrinks 4 -> 3);
  B the second arm on the same tool: 3 -> 2 (the scan keeps finding the stack);
  C the bare negative: empty hand + torches in inventory = PASS (no tool arm);
  D the no-torch negative handled by the place report's torchesLeft verdict.

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its floor).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t1_dig_six_torch.py
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
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=minecraft:torch"),
    Step(f"setblock 385 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig place 385 64 192 spade",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=minecraft:torch"),
    Step(f"setblock 386 64 192 minecraft:dirt", expect="Changed the block"),
    Step("gt6dig place 386 64 192 shovel",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=minecraft:torch"),
    Step(f"setblock 387 64 192 minecraft:grass_block", expect="Changed the block"),
    Step("gt6dig place 387 64 192 universal_spade",
         expect="torchPlaced=true, torchesLeft=3 (of 4), landed=minecraft:torch"),
]

# --------------------------------- B: the second placement (the scan keeps finding the stack)
steps += [
    phase("B: the second arm — torchesLeft 3 -> 2"),
    Step("gt6dig place 384 64 192 pickaxe",
         expect="torchPlaced=true, torchesLeft=2 (of 4), landed=minecraft:torch"),
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
    name="p29-w5-t1-dig-six-torch",
    slug="p29w5t1digsixtorch",
    sites=gt6world.declare_sites(F1, F2, F3, F4, F5),
    preferred_ports=(26195, 26205),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
