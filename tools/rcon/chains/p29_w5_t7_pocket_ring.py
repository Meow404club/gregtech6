#!/usr/bin/env python3
"""p29_w5_t7_pocket_ring — the pocket multitool ring chain (task p29-w5-t7-pocket-eight,
the p25_tool_hammer_wrench give-arm shape: RCON sessions have no player, so the headless
give is `item replace block <chest> container.N with <stack>` + a `data get block` probe).

Chain semantics (the card ACCEPTANCE RCON 替身面, arm 1 — the sneak-right-click ring):

  A the give arm: all EIGHT registration rows land as real stacks in a chest — the
    pocket_multitool closed form + the seven switch forms (an unregistered id fails the
    replace loudly; the data-get probe names each id).

  B the ring arm: /gt6pocket walk drives the item's own useOn dispatch eight times
    against a bare stone rig — a fake player holding the closed multitool worn to
    damage 5 sneak-right-clicks; the command report names every hop form@damage and
    self-checks the :176-183 chain. GREEN = "result=RING_OK damage=KEPT" (all eight
    forms visited, the eighth hop home, the durability axis untouched).

  C the teardown: fill-air over the site. No other world state.

passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t7_pocket_ring.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=352 W5 band — ring column x384..386 (faces x394 / smoke x404 are the
# sibling chains' disjoint columns; the chest at 384, the bare-stone walk rig at 386).
SITE = gt6world.Site(385, 64, 352, dx=3, dy=0, dz=1)
CHEST = "384 64 352"
RIG = "386 64 352"

FORMS = ["pocket_multitool", "pocket_multitool_knife", "pocket_multitool_saw", "pocket_multitool_file",
         "pocket_multitool_screwdriver", "pocket_multitool_wire_cutter", "pocket_multitool_scissors",
         "pocket_multitool_chisel"]

steps = []

# ------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — the eight pocket forms land as real stacks"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
]
for i, tForm in enumerate(FORMS):
    steps += [
        Step(f"item replace block {CHEST} container.{i} with gt6:{tForm} 1", expect="Replaced"),
        Step(f"data get block {CHEST} Items[{i}]", expect=f"gt6:{tForm}"),
    ]

# ------------------------------------------------- B: the ring walk
steps += [
    phase("B: the ring arm — eight real useOn dispatches walk the :176-183 chain, durability kept"),
    Step(f"setblock {RIG} minecraft:stone", expect="Changed the block"),
    Step(f"gt6pocket walk {RIG} 5",
         expect="hop8=pocket_multitool@5 result=RING_OK damage=KEPT"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the site restored to air (the pass-open bbox is the backstop)"),
    Step(f"setblock {CHEST} minecraft:air", expect="Changed the block"),
    Step(f"setblock {RIG} minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w5_t7_pocket_ring",
    slug="p29w5t7pocketring",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26471, 26481),      # per-chain pair — DISAGREEING pins on purpose (session_ports: the session falls back to the node segments, keeping the --dual legs apart)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
