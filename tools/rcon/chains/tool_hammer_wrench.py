#!/usr/bin/env python3
"""tool-hammer-wrench — the hammer + wrench /give live acceptance chain (the
act give-arm shape: RCON sessions have no player, so the headless give is
`item replace block <chest> container.N with <stack>` + a `data get block` probe).

Chain semantics (task tool-hammer-wrench ACCEPTANCE ③):

  A the give arm: the hammer + wrench registration rows land as real stacks in a
    chest — `item replace block` with a plain stack, then the Items[N].id probe
    (an unregistered id would fail the replace loudly).

  B the durability arm: the hammer's 512-point steel tier Damage axis round-trips
    through the stack carrier (1.20.1 `Damage:22` NBT vs the 21.1
    `minecraft:damage=22` component envelope — the GT6Circuits carrier ruling);
    the worn token is probed back out of the chest NBT.

  C the teardown: fill-air over the chest site. No other world state.

Recipe-load evidence rides the runServer gate (zero ERROR lines — an unparseable
gt6 recipe JSON logs loudly) plus the committed datagen JSON pins
(HammerWrenchTest/GT6TagsDatagenTest); RCON has no player, so /recipe give is not
addressable headless (the act ruling).

passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/tool_hammer_wrench.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the chest site — x 416 z 124 sits clear of the P24 bands (grass 392-394, dye 386,
# ACT 410-412); the box is the framework's only cleanup surface (no blocks persist).
SITE = gt6world.Site(416, 64, 124, dx=0, dy=0, dz=0)
POS = "416 64 124"

# the worn-hammer payload per node (the GT6Circuits carrier ruling): the 1.20.1
# Damage NBT key vs the 21.1 minecraft:damage component envelope — same 22-of-512 axis.
HAMMER_WORN_REPLACE = {
    "1.20.1": f"item replace block {POS} container.2 with gt6:hammer{{Damage:22}} 1",
    "1.21.1": f"item replace block {POS} container.2 with gt6:hammer[minecraft:damage=22] 1",
}

steps = []

# ------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — both registration rows land as real stacks (gt6:hammer / gt6:wrench)"),
    Step(f"setblock {POS} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {POS} container.0 with gt6:hammer 1", expect="Replaced"),
    Step(f"data get block {POS} Items[0]", expect="gt6:hammer"),
    Step(f"item replace block {POS} container.1 with gt6:wrench 1", expect="Replaced"),
    Step(f"data get block {POS} Items[1]", expect="gt6:wrench"),
]

# ------------------------------------------------- B: the durability arm
steps += [
    phase("B: the durability arm — the 512-point steel tier, the Damage axis round-trips"),
    Step(HAMMER_WORN_REPLACE["1.20.1"], expect="Replaced",
         node_cmds={"1.21.1": HAMMER_WORN_REPLACE["1.21.1"]}),
    # the data-get rendering drifts per node (the pchk s17 node_expects rule):
    # 1.20.1 NBT `Damage: 22` vs the 21.1 components `"minecraft:damage": 22`
    Step(f"data get block {POS} Items[2]", expect="Damage: 22",
         node_cmds={"1.21.1": f"data get block {POS} Items[2]"},
         node_expects={"1.21.1": '"minecraft:damage": 22'}),
    # a fresh hammer carries the clean axis: 1.20.1 Count: 1b (+Damage: 0 default tag),
    # 21.1 count: 1 (the empty component map is omitted)
    Step(f"item replace block {POS} container.3 with gt6:hammer 1", expect="Replaced"),
    Step(f"data get block {POS} Items[3]", expect="Count: 1b",
         node_cmds={"1.21.1": f"data get block {POS} Items[3]"},
         node_expects={"1.21.1": "count: 1"}),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the chest site restored to air (the pass-open bbox is the backstop)"),
    Step(f"setblock {POS} minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="tool-hammer-wrench",
    slug="toolhammerwrench",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26108, 26118),      # this card's pinned rcon/query pair (the 2610x segment, after grassblock 26107/26117)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
