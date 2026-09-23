#!/usr/bin/env python3
"""p35-long-distance-pipes — the fluid pipeline live acceptance (sweep group
p35_pipes_ld; the fresh z=502..506 band, x559..642 — the item chain's band
z=500 sits z-adjacent with a 2-block clearance, x/z-disjoint from every
other registered band).

  THE FILL WINDOW (the upstream IFluidHandler delegation :199-233): the
  sender endpoint forwards fill + tank query straight into the TARGET's
  BACK-adjacent barrel — /gt6tank fill on the sender fills the remote drum,
  ZERO transport ticks. Fill-ONLY: the drain arms return EMPTY (:207-225).

  THE LINE (two columns): sender(x561)[facing=west] -> wire line
  (x562..637, crossing FOUR chunk borders) -> target(x638)[facing=west] ->
  barrel_metal(x639). Column A rides long_dist_pipe_1 (SS, 1943 K); column
  B rides long_dist_pipe_0 (the ITEM pipe, -1 K) between two FLUID
  endpoints — the structural no-link arm.

  A THE FLOW LEG: 1000 L of water (300 K) into the sender — the remote
    barrel holds it; the tank query forwards too (the stat readback).
  B THE TEMPERATURE GATE LEG: 500 L of gt6:tungsten_molten (3695 K > the
    1943 K rating) — refused at the sender, the barrel untouched (the
    :200 gate live).
  C THE STRUCTURAL LEG: the fluid endpoints over the ITEM pipe line link
    nothing (the :149 <= 0 arm) — the water stays with the sender.

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
SENDER_A = gt6world.Site(561, 65, Z_A)
BARREL_A = gt6world.Site(639, 65, Z_A)
SENDER_B = gt6world.Site(561, 65, Z_B)
BARREL_B = gt6world.Site(639, 65, Z_B)

steps = [
    Step(f"fill 559 62 500 642 68 508 air", expect="filled"),
    Step("forceload add 559 500 642 508"),

    phase("A: the flow leg — water through the SS line into the remote barrel"),
    Step(f"setblock {F(BARREL_A)} gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 638 65 {Z_A} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"fill 562 65 {Z_A} 637 65 {Z_A} gt6:long_dist_pipe_1", expect="filled 76 blocks"),
    Step(f"setblock {F(SENDER_A)} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"gt6tank fill {F(SENDER_A)} minecraft:water 1000",
         expect="filled 1000/1000 L of minecraft:water", sleep=1.0),
    Step(f"gt6tank stat {F(BARREL_A)}", expect="1000/64000 L of minecraft:water",
         label="the remote barrel holds it — the fill window crossed the blob"),
    Step(f"data get block {F(SENDER_A)} gt.target", expect="gt.target: 1b"),

    phase("B: the temperature gate leg — molten tungsten over the SS rating refuses"),
    Step(f"gt6tank fill {F(SENDER_A)} gt6:tungsten_molten 500",
         expect="filled 0/500 L of gt6:tungsten_molten",
         label="3695 K > the 1943 K rating — the :200 gate"),
    Step(f"gt6tank stat {F(BARREL_A)}", expect="1000/64000 L of minecraft:water",
         label="the barrel untouched"),

    phase("C: the structural leg — the fluid endpoints over the ITEM pipe link nothing"),
    Step(f"setblock {F(BARREL_B)} gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 638 65 {Z_B} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"fill 562 65 {Z_B} 637 65 {Z_B} gt6:long_dist_pipe_0", expect="filled 76 blocks"),
    Step(f"setblock {F(SENDER_B)} gt6:longdist_fluid_pipe[facing=west]", expect="Changed the block"),
    Step(f"gt6tank fill {F(SENDER_B)} minecraft:water 1000",
         expect="filled 0/1000 L of minecraft:water",
         label="the -1 K item meta refuses the fluid pairing (the :149 arm)"),
    Step(f"gt6tank stat {F(BARREL_B)}", expect="0/64000 L of nothing"),

    phase("D: teardown — restore the band"),
    Step("fill 559 62 500 642 68 508 air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35_pipes_ld ld-pipe-fluid cross-chunk fluid delegation",
    slug="p35pipesldfluid",
    sites=gt6world.declare_sites(SENDER_A, BARREL_A, SENDER_B, BARREL_B),
    preferred_ports=(26752, 26762),
    game_port=26502,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
