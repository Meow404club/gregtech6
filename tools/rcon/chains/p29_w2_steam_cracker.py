#!/usr/bin/env python3
"""p29-w2-steam-cracker — the Steam Cracker chain (task p29-w2-hu-tu-piggyback, group
p29_w2_hu_tu; the HU 4-ladder :1576-1579 over the JSON-poured steamcracking.json smoke
row — 1 coal + water 1000 -> 1 charcoal, eUt 32, duration 128):

  T1 budget units(32x128x1, 10000, 10000, T) = 4096 -> 128 ticks @ 32 EU/tick.
  the HU window ladder (TIER_INPUTS per row, the ACCEPTANCE-① per-row pin):
    T1 16/32/64, T2 64/128/256, T3 256/512/1024, T4 1024/2048/4096.
  the dead-packet wall: 40x 8 HU packets never cross mInputMin 16 -> 0/0;
  the run: 128x 32 HU ticks complete IN ONE COMMAND -> out charcoal.

The catalytic_cracker chain runs the MIRROR row on the twin map (the same-parameters
对拍 the card names). passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_steam_cracker.py
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

SC1 = gt6world.Site(384, 65, 280, dz=1)  # steamcracker T1
SC2 = gt6world.Site(386, 65, 280)        # T2
SC3 = gt6world.Site(388, 65, 280)        # T3
SC4 = gt6world.Site(390, 65, 280)        # T4

steps = [
    phase("A: the T1 cracker — the smoke row, the 8-HU wall, then the full 4096 bar in one command"),
    Step(f"gt6machine steamcracker place {F(SC1)}", expect="GT6 steamcracker placed at 384, 65, 280"),
    Step(f"gt6machine steamcracker input 1 {F(SC1)}",
         expect="GT6 steamcracking input: 1x coal into slot 0",  # the report name is the MAP local (getTileEntityName), not the block literal
         node_expects={"1.21.1": "GT6 steamcracking input: 1x minecraft:coal into slot 0"}),
    Step(f"gt6machine steamcracker fluid fill up minecraft:water 1000 {F(SC1)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine steamcracker check {F(SC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine steamcracker inject 40 8 {F(SC1)}", expect="progress=0/0"),
    Step(f"gt6machine steamcracker inject 128 32 {F(SC1)}",
         expect="outputs=[1x charcoal; ]",
         node_expects={"1.21.1": "outputs=[1x minecraft:charcoal; ]"}),

    phase("B: the T2-T4 window ladder (the TIER_INPUTS per-row pin)"),
    Step(f"gt6machine steamcracker_t2 place {F(SC2)}", expect="GT6 steamcracker_t2 placed at 386, 65, 280"),
    Step(f"gt6machine steamcracker_t2 check {F(SC2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine steamcracker_t3 place {F(SC3)}", expect="GT6 steamcracker_t3 placed at 388, 65, 280"),
    Step(f"gt6machine steamcracker_t3 check {F(SC3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine steamcracker_t4 place {F(SC4)}", expect="GT6 steamcracker_t4 placed at 390, 65, 280"),
    Step(f"gt6machine steamcracker_t4 check {F(SC4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 278 391 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p29_w2_hu_tu matches this member
    # substring and joins the whole cluster (the p29_w1_mixer_pair form)
    name="p29-w2-steam-cracker p29_w2_hu_tu",
    slug="p29w2steamcracker",
    sites=gt6world.declare_sites(SC1, SC2, SC3, SC4),
    preferred_ports=(26341, 26351),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
