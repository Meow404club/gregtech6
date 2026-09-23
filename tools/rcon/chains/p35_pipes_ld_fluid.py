#!/usr/bin/env python3
"""p35-long-distance-pipes — the fluid pipeline live acceptance (sweep group
p35_pipes_ld; the fresh z=502..506 band, x559..642 — the item chain's band
z=500 sits z-adjacent with a 2-block clearance, x/z-disjoint from every
other registered band).

  THE FILL WINDOW (the upstream IFluidHandler delegation :199-233): the
  sender endpoint forwards fill + tank query straight into the TARGET's
  BACK-adjacent barrel — ZERO transport ticks. Fill-ONLY: the drain arms
  return EMPTY (:207-225).

  THE PUSHER (the real machine lever, the p26/p24 pipe-command precedent):
  a wood fluid pipe west of the sender, connected with /gt6pipe place
  (against face 4 = WEST — the neighbour sits at OPOS[4] = EAST = the
  sender), filled with /gt6pipe inject (side 0 = DOWN — the received-mask
  marker must NOT cover the pushing face). The pipe's distribute tick
  probes the sender's fluid capability — the window forwards to the
  remote barrel. NO pipe-to-endpoint auto-connect exists; the pipe
  connects only through the place driver.

  THE LINES: sender(x561)[facing=west] -> wire line (x562..637, crossing
  FOUR chunk borders) -> target(x638)[facing=west] -> barrel_metal(x639).
  Column A (z=502) rides long_dist_pipe_1 (SS, 1943 K); column B (z=506)
  rides long_dist_pipe_0 (the ITEM pipe, -1 K) between two FLUID
  endpoints — the structural no-link arm.

  A THE FLOW LEG: 50 L of water (300 K) injected into the pusher pipe —
    the distribute tick lands it in the remote barrel; the tank query
    forwards too (the stat readback).
  B THE TEMPERATURE GATE LEG: the pusher's tank data-merged with
    gt6:tungsten_molten (3695 K > the 1943 K rating) — the window refuses,
    the pipe RETAINS its charge, the barrel untouched (the :200 gate live;
    the merge is the node_cmds fork — the 1.21.1 FluidStack codec shape).
  C THE STRUCTURAL LEG: the fluid endpoints over the ITEM pipe line link
    nothing (the :149 <= 0 arm) — the water stays in the pusher.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_pipes_ld_fluid.py
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

Z_A = 502  # column A — the SS-rated fluid line
Z_B = 506  # column B — the item-pipe structural negative
PUSHER_A = gt6world.Site(560, 65, Z_A)
SENDER_A = gt6world.Site(561, 65, Z_A)
BARREL_A = gt6world.Site(639, 65, Z_A)
PUSHER_B = gt6world.Site(560, 65, Z_B)
SENDER_B = gt6world.Site(561, 65, Z_B)
BARREL_B = gt6world.Site(639, 65, Z_B)

# the pusher tank payload — the forge FluidStack NBT vs the 1.21.1 codec shape
# (the p24_canner_refill key-shape fork)
HOT_MERGE = {
    "1.20.1": f'data merge block {F(PUSHER_A)} {{"tank.0":{{FluidName:"gt6:tungsten_molten",Amount:250}}}}',
    "1.21.1": f'data merge block {F(PUSHER_A)} {{"tank.0":{{id:"gt6:tungsten_molten",amount:250}}}}',
}
HOT_READ = {
    "1.20.1": f'data get block {F(PUSHER_A)} "tank.0"',
    "1.21.1": f'data get block {F(PUSHER_A)} "tank.0"',
}

steps = [
    Step(f"fill 559 62 500 642 68 508 air", expect="filled"),
    Step("forceload add 559 500 642 508"),

    phase("A: the flow leg — water pushed through the SS line into the remote barrel"),
    Step(f"setblock {F(BARREL_A)} gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 638 65 {Z_A} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"fill 562 65 {Z_A} 637 65 {Z_A} gt6:long_dist_pipe_1", expect="filled 76 block"),
    Step(f"setblock {F(SENDER_A)} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    # the headless placement driver: setBlock + the onPlaced connect — face 4 (WEST)
    # means the support neighbour sits at OPOS[4] = EAST = the sender; the connect's
    # canConnect probe rides the endpoint window (getTanks forwards through the link)
    Step(f"gt6pipe place {F(PUSHER_A)} 4", expect="connections 32", label="SBIT[5]=32 — the EAST connect to the sender"),
    Step(f"gt6pipe toggle {F(PUSHER_A)} 4", expect="connections 48",
         label="+ SBIT[4]=16 — the WEST fill mouth (pipes only accept on connected faces)"),
    Step(f"gt6pipe inject {F(PUSHER_A)} 4 250", expect="filled 50 of 250 L",
         label="the fill marks the WEST received mask — distribute skips it, the EAST push survives"),
    Step(f"gt6tank stat {F(BARREL_A)}", expect="50/64000 L of minecraft:water", sleep=2.0, poll=30.0,
         label="the remote barrel holds it — the distribute tick crossed the blob"),
    Step(f'data get block {F(SENDER_A)} "gt.target"', expect="block data: 1b"),

    phase("B: the temperature gate leg — molten tungsten over the SS rating refuses"),
    Step(HOT_MERGE["1.20.1"], expect="Modified block data", node_cmds=HOT_MERGE),
    Step(f"gt6tank stat {F(BARREL_A)}", expect="50/64000 L of minecraft:water", sleep=3.0,
         label="the barrel untouched — the :200 gate refused the 3695 K charge"),
    Step(HOT_READ["1.20.1"], expect="tungsten_molten", node_cmds=HOT_READ,
         label="the pipe RETAINS its hot charge — nothing accepted it"),

    phase("C: the structural leg — the fluid endpoints over the ITEM pipe link nothing"),
    Step(f"setblock {F(BARREL_B)} gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 638 65 {Z_B} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"fill 562 65 {Z_B} 637 65 {Z_B} gt6:long_dist_pipe_0", expect="filled 76 block"),
    Step(f"setblock {F(SENDER_B)} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"gt6pipe place {F(PUSHER_B)} 4", expect="connections 0",
         label="the connect probe rides the link-less window (getTanks 0) — no pairing, no connection"),
    Step(f"gt6pipe toggle {F(PUSHER_B)} 4", expect="connections 16", label="+ SBIT[4]=16 — the WEST fill mouth"),
    Step(f"gt6pipe inject {F(PUSHER_B)} 4 250", expect="filled 50 of 250 L"),
    Step(f"gt6tank stat {F(BARREL_B)}", expect="0/64000 L of nothing", sleep=3.0,
         label="the -1 K item meta refuses the fluid pairing (the :149 arm)"),
    Step(f'data get block {F(PUSHER_B)} "tank.0"', expect="minecraft:water",
         label="the water stays with the pusher"),

    phase("D: teardown — restore the band"),
    Step("fill 559 62 500 642 68 508 air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35_pipes_ld ld-pipe-fluid cross-chunk fluid delegation",
    slug="p35pipesldfluid",
    sites=gt6world.declare_sites(PUSHER_A, SENDER_A, BARREL_A, PUSHER_B, SENDER_B, BARREL_B),
    preferred_ports=(26752, 26762),
    game_port=26502,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
