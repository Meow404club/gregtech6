#!/usr/bin/env python3
"""p29-w5-t5-scene-six TRIPWIRE chain — the silent-disarm arm (Behavior_TripwireCutting :123).

Chain semantics (task p29-w5-t5-scene-six RCON arm 3, the /gt6scene6 mine channel — the
command drives the ServerPlayerGameMode.destroyBlock face, the ONLY walk that runs
Block.playerWillDestroy, where the forge patch checks canPerformAction(SHEARS_DISARM)
(TripWireBlock.java.patch:8); the scissors inherit the action from the ShearsItem base):

  A hook + wire laid, the SCISSORS mine the wire: the wire drops its string (the loot
    table with the TOOL context) and the HOOK STAYS (the silent disarm — a non-shears
    break would pop the hook's connection and pulse redstone);
  B the wire is re-laid per pass (the two framework passes are the [0, 0] idempotency
    proof).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t5_scene_six_tripwire.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

T1 = gt6world.Site(403, 64, 240)   # the hook block
T2 = gt6world.Site(404, 64, 240)   # the wire block (east of the hook)

steps = []

# --------------------------------- A: the scissors disarm the wire, the hook stays
steps += [
    phase("A: the scissors cut the wire — string drops, the hook is NOT tripped (SHEARS_DISARM, TripWireBlock.java.patch:8)"),
    Step("setblock 403 64 240 minecraft:tripwire_hook[facing=east,attached=true]", expect="Changed the block"),
    Step("setblock 404 64 240 minecraft:tripwire[attached=true,east=true,north=false,south=false,west=false]", expect="Changed the block"),
    Step("gt6scene6 mine 404 64 240 scissors", expect="drops=[minecraft:string"),
    Step("execute if block 403 64 240 minecraft:tripwire_hook", expect="Test passed"),
]

# --------------------------------- B: the re-lay (the pass-2 rerun face)
steps += [
    phase("B: the wire re-laid — the pass-2 rerun (the hook persists across passes)"),
    Step("setblock 404 64 240 minecraft:tripwire[attached=true,east=true,north=false,south=false,west=false]", expect="Changed the block"),
    Step("gt6scene6 mine 404 64 240 scissors", expect="drops=[minecraft:string"),
    Step("execute if block 403 64 240 minecraft:tripwire_hook", expect="Test passed"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 403 64 240 404 66 240 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t5-scene-six-tripwire",
    slug="p29w5t5scenesixtripwire",
    sites=gt6world.declare_sites(T1, T2),
    preferred_ports=(26215, 26225),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
