#!/usr/bin/env python3
"""p29-w2-autoclave — the TU Autoclave chain (task p29-w2-hu-tu-piggyback, group
p29_w2_hu_tu; the TU single :1655 over the JSON-poured autoclave.json smoke row —
1 kelp + 1 bone meal + water 500 -> 2 kelp, eUt 16, duration 128 — the TWO-ITEM
  row form the map MIN-ITEM 2 demands):

  budget units(16x128x1, 10000, 10000, T) = 2048 -> 128 ticks @ 16 EU/tick.
  the DISTINCTIVE :1655 masks pinned live through the fill faces: the tank-in face is
  BOTTOM|LEFT (the fill arm drives 'down' — SBIT_D — the only TU four row whose tank-in
  face excludes the top), item+tank out BACK|RIGHT.
  the TU window {1, 1, 16} + the full 2048 bar in one command -> out 3x dried kelp.
  (the 17-包 overcharge wall rides the p29_w2_coagulator chain's throwaway arm.)

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_autoclave.py
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

AUTO = gt6world.Site(426, 65, 280, dz=1)

# the two-stack row feed (kelp + bone_meal), loader-versioned key shapes (the p19
# ruling: 1.20.1 Count:1b vs 1.21.1 count:1)
def auto_merge(pos):
    return {
        "1.20.1": ('data merge block ' + pos + ' {inventory:{Size:5,Items:['
                   '{Slot:0b,id:"minecraft:kelp",Count:1b},'
                   '{Slot:1b,id:"minecraft:bone_meal",Count:1b}]}}'),
        "1.21.1": ('data merge block ' + pos + ' {inventory:{Size:5,Items:['
                   '{Slot:0b,id:"minecraft:kelp",count:1},'
                   '{Slot:1b,id:"minecraft:bone_meal",count:1}]}}'),
    }

steps = [
    phase("A: the TU autoclave — the bottom-face tank fill (the :1655 D|L mask), the full 2048 bar"),
    Step(f"gt6machine autoclave place {F(AUTO)}", expect="GT6 autoclave placed at 426, 65, 280"),
    # the row is the TWO-ITEM form (RM.java:91 items 2/3/2 — MIN-ITEM 2, the map's own
    # minimum the single-item smoke row violated, the first live run's red); the two
    # input stacks ride the data-merge inventory form (the p29_w1_mixer_pair shape —
    # the input command feeds slot 0 only; Size = input 2 + output 3 slots)
    Step(auto_merge(F(AUTO))["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": auto_merge(F(AUTO))["1.21.1"]}),
    Step(f"gt6machine autoclave fluid fill down minecraft:water 500 {F(AUTO)}",
         expect="filled 500/500 L of minecraft:water (ACCEPTED), input tanks hold 500 L"),
    Step(f"gt6machine autoclave check {F(AUTO)}", expect="minIn=1 recIn=1 maxIn=16"),
    Step(f"gt6machine autoclave inject 128 16 {F(AUTO)}",
         expect="outputs=[2x kelp; ]",
         node_expects={"1.21.1": "outputs=[2x minecraft:kelp; ]"}),

    phase("B: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 425 62 278 427 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w2-autoclave p29_w2_hu_tu",
    slug="p29w2autoclave",
    sites=gt6world.declare_sites(AUTO),
    preferred_ports=(26346, 26356),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
