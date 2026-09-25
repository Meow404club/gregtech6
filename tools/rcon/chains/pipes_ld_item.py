#!/usr/bin/env python3
"""long-distance-pipes — the item pipeline live acceptance (sweep group
pipes_ld; the fresh z=500 band, x559..642 — z/x-disjoint from every
registered band: the energy tail z=420..432, the ignition band z452..478,
the covers-hosts band z554..574).

  THE WINDOW (the upstream IInventory delegation :190-285): the sender
  endpoint IS a window into the TARGET's BACK-adjacent chest — a vanilla
  hopper pushing the sender writes the remote chest directly, ZERO
  transport ticks, the BFS pairing ran lazily on the first capability
  access.

  THE LINE: hopper(x560)[facing=east] -> sender(x561)[facing=west] ->
  76x long_dist_pipe_0 (x562..637, crossing FOUR chunk borders x=576/592/
  608/624) -> target(x638)[facing=west] -> chest(x639). BOTH endpoints face
  WEST (the upstream same-direction convention: the sender's scan seed is
  its BACK-adjacent wire, the peer match is the endpoint whose FRONT sits
  in the wire blob).

  A THE FLOW LEG: seed the hopper with 8 iron ingots — they land in the
    remote chest through the window; the sender's gt.target_pos NBT pins
    the delegation DIRECTNESS (the packed position of the FAR endpoint,
    not a neighbor — the one-hop proof).
  B THE SEVER LEG: remove a middle wire (x600) — the machine-block-update
    flood resets both endpoints, the lazy rescan finds no peer, the
    re-seeded hopper item STAYS in the hopper (gt.target absent from the
    sender NBT).
  C THE RELINK LEG: re-place the wire — the flood resets again, the next
    access re-scans, the hopper empties, the chest holds 16 (8+8 stacked).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/pipes_ld_item.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

Z = 500  # the fresh band (x559..642, z-disjoint from every roster band)
HOPPER = gt6world.Site(560, 65, Z)   # minecraft:hopper[facing=east] — the pusher
SENDER = gt6world.Site(561, 65, Z)   # gt6:longdist_item_pipe[facing=west]
WIRE_A = gt6world.Site(562, 65, Z)   # the wire line x562..637
WIRE_MID = gt6world.Site(600, 65, Z) # the severed block (chunk border x=608 sits east of it)
WIRE_Z = gt6world.Site(637, 65, Z)   # the last wire block
TARGET = gt6world.Site(638, 65, Z)   # gt6:longdist_item_pipe[facing=west]
CHEST = gt6world.Site(639, 65, Z)    # the remote inventory — the delegation target

SEED_A = "item replace block %s container.0 with minecraft:iron_ingot 8"


def pl(x, y, z):
    """The vanilla BlockPos.asLong packing (the sender's gt.target_pos NBT face)."""
    return (x & 0x3FFFFFF) << 38 | (z & 0x3FFFFFF) << 12 | y & 0xFFF


steps = [
    Step(f"fill 559 62 {Z - 2} 642 68 {Z + 5} air", expect="filled"),
    Step(f"forceload add 559 {Z - 2} 642 {Z + 5}"),

    phase("A: the flow leg — the hopper pushes the sender, the remote chest receives"),
    Step(f"setblock {F(CHEST)} minecraft:chest", expect="Changed the block"),
    Step(f"setblock {F(TARGET)} gt6:longdist_item_pipe[facing=west]", expect="Changed the block"),
    Step(f"fill {F(WIRE_A)} 637 65 {Z} gt6:long_dist_pipe_0", expect="filled 76 block"),
    Step(f"setblock {F(SENDER)} gt6:longdist_item_pipe[facing=west]", expect="Changed the block"),
    Step(f"setblock {F(HOPPER)} minecraft:hopper[facing=east]", expect="Changed the block"),
    Step(SEED_A % F(HOPPER), expect="Replaced"),
    # the lazy pairing runs on the first hopper push; the window writes the remote chest
    Step(f"data get block {F(CHEST)} Items[0]", expect="iron_ingot", sleep=3.0, poll=30.0),
    Step(f'data get block {F(SENDER)} "gt.target"', expect="block data: 1b"),
    Step(f'data get block {F(SENDER)} "gt.target_pos"', expect=f"block data: {pl(638, 65, Z)}L",
         label="the DIRECT-delegation pin — the packed FAR endpoint, not a neighbor"),

    phase("B: the sever leg — the flood resets the endpoints, the rescan finds no peer"),
    Step(f"setblock {F(WIRE_MID)} air", expect="Changed the block"),
    Step(f'data get block {F(SENDER)} "gt.target"', expect="Found no elements",
         label="the flood reset the link — the target key is gone"),
    Step(SEED_A % F(HOPPER), expect="Replaced"),
    Step(f"data get block {F(HOPPER)} Items[0]", expect="iron_ingot", sleep=3.0, poll=10.0,
         label="the window is dead — the stack stays with the hopper"),

    phase("C: the relink leg — the re-placed wire floods, the next access re-scans"),
    Step(f"setblock {F(WIRE_MID)} gt6:long_dist_pipe_0", expect="Changed the block"),
    Step(f"data get block {F(CHEST)} Items[0]", expect="iron_ingot", sleep=3.0, poll=30.0),
    Step(f"data get block {F(CHEST)} Items[0]", sleep=1.0, poll=30.0,
         node_expects={"1.20.1": "Count: 16", "1.21.1": "count: 16"},
         label="8 + 8 stacked in the remote slot — the relink flowed"),

    phase("D: teardown — restore the band"),
    Step(f"fill 559 62 {Z - 2} 642 68 {Z + 5} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="pipes_ld ld-pipe-item cross-chunk item delegation",
    slug="pipeslditem",
    sites=gt6world.declare_sites(HOPPER, SENDER, WIRE_A, WIRE_MID, WIRE_Z, TARGET, CHEST),
    preferred_ports=(26732, 26742),
    game_port=26492,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
