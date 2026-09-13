#!/usr/bin/env python3
"""p29-w2-bath — the TU Bath chain (task p29-w2-hu-tu-piggyback, group p29_w2_hu_tu;
the TU single :1653 riding the P26 IN-CATALOG RM.BATH map — reused, never rebuilt; this
card pours ONE bath.json smoke row: 1 white_wool + water 1000 -> 4 string, eUt 16,
duration 128):

  budget units(16x128x1, 10000, 10000, T) = 2048 -> 128 ticks @ 16 EU/tick (the :770
  TU arm keeps mMinEnergy = mEUt). The BATH map is the card-A base-RecipeMap constant
  the p26_kitchen_pot card registered (its static wood-oil ladder pours ZERO rows live
  — the planks are port-absent — so this smoke row IS the map's live row stock).
  the TU window {1, 1, 16} + the full 2048 bar in one command -> out 4x string.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_bath.py
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

BATH = gt6world.Site(420, 65, 280, dz=1)

steps = [
    phase("A: the TU bath machine on the P26 in-catalog map — the poured smoke row end-to-end"),
    Step(f"gt6machine bath place {F(BATH)}", expect="GT6 bath placed at 420, 65, 280"),
    Step(f"gt6machine bath input 1 {F(BATH)}",
         expect="GT6 bath input: 1x white_wool into slot 0",
         node_expects={"1.21.1": "GT6 bath input: 1x minecraft:white_wool into slot 0"}),
    Step(f"gt6machine bath fluid fill up minecraft:water 1000 {F(BATH)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine bath check {F(BATH)}", expect="minIn=1 recIn=1 maxIn=16"),
    Step(f"gt6machine bath inject 128 16 {F(BATH)}",
         expect="outputs=[4x string; ]",
         node_expects={"1.21.1": "outputs=[4x minecraft:string; ]"}),

    phase("B: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 419 62 278 421 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w2-bath p29_w2_hu_tu",
    slug="p29w2bath",
    sites=gt6world.declare_sites(BATH),
    preferred_ports=(26345, 26355),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
