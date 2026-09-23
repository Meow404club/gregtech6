#!/usr/bin/env python3
"""p29-w2-eu-core slicer — the Slicer family chain (task p29-w2-eu-core-5tier
ACCEPTANCE ①⑤): the EU 5-tier ladder (:1525-1529, no NBT_PARALLEL -> 1, NO tank
keys — the zero-fluid face, data-only), on the REAL poured leather row
(GT6RecipesSlicer.java:134 = upstream Loader_Recipes_Vanilla:638 verbatim:
1x leather_helmet + the split blade (gt6:shape_slicer_split, never consumed)
-> 1x leather, eUt 16, duration 16).

RE-RECORD (task p35-fix-p29-slicer-chain): B/C originally pinned the
slicer.json smoke row (stone+stone -> 2x stone_slab); that fake row was deleted
by p35-slicer-row-domain (the smoke pins went 5 -> 4), leaving outputs=[]
forever (the p35 solo batch2 live log: expect 15/19 FAIL x 2 legs x 2 passes).
The helmet row is the closest live row to the original chain intent — the same
two-slot feed + single-output shape, and the identical EUt 16 / duration 16
energy profile keeps the T1/T5 inject seats untouched. Phase A is byte-identical.

  A the per-tier window pins T1-T5 (the gapless doubling chain, the T5 arm
    minIn=4096 recIn=8192 maxIn=16384) + facing=2 + the menu-less carrier.
  B the T1 row run: feed the two slots (helmet slot 0, blade slot 1), inject
    24 x 32 -> the 1x leather land (the row 进出断言), then progress=0/0.
  C the T5 mid-window run: the 4096 floor-edge packet drives the same row to
    its leather.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_slicer.py
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

# the fresh z=284 band, slicer column x432..440 (per-family x-disjoint teardown fill)
T1 = gt6world.Site(432, 64, 284, dy=1, dz=1)
T2 = gt6world.Site(434, 64, 284, dy=1, dz=1)
T3 = gt6world.Site(436, 64, 284, dy=1, dz=1)
T4 = gt6world.Site(438, 64, 284, dy=1, dz=1)
T5 = gt6world.Site(440, 64, 284, dy=1, dz=1)
T1F, T2F, T3F, T4F, T5F = F(T1), F(T2), F(T3), F(T4), F(T5)


def feed_merge(pos):
    """The two-slot leather row (Size 4 = 2 in + 2 out): helmet slot 0, split blade slot 1
    (the never-consumed second leg — Recipe.sNotConsumable's isBlade arm skips the shrink)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:leather_helmet",Count:1b}},{{Slot:1b,id:"gt6:shape_slicer_split",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:leather_helmet",count:1}},{{Slot:1b,id:"gt6:shape_slicer_split",count:1}}]}}}}',
    }


def out_report():
    return {
        "1.20.1": "outputs=[1x leather; ]",
        "1.21.1": "outputs=[1x minecraft:leather; ]",
    }


steps = [
    phase("A: the per-tier window pins T1-T5 + the placement canon + the menu-less carrier"),
    Step(f"setblock {T1F} gt6:slicer", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1F}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine wiremill check {T1F}", expect="facing=2"),
    Step(f"gt6machine wiremill check {T1F}", expect="data=-2"),
    Step(f"setblock {T2F} gt6:slicer_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2F}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"setblock {T3F} gt6:slicer_t3", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T3F}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"setblock {T4F} gt6:slicer_t4", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T4F}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(f"setblock {T5F} gt6:slicer_t5", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5F}", expect="minIn=4096 recIn=8192 maxIn=16384"),

    phase("B: the T1 leather-row run — helmet + blade -> 1x leather (fire + read-only poll: the first completion after a fresh boot lags past the per-command deadline)"),
    Step(feed_merge(T1F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1F)),
    Step(f"gt6machine wiremill inject 24 32 {T1F}", expect=None),
    Step(f"gt6machine wiremill check {T1F}", expect="out[0]=1x leather",
         node_expects={"1.21.1": "out[0]=1x minecraft:leather"}, poll=60.0),
    Step(f"gt6machine wiremill check {T1F}", expect="progress=0/0"),

    phase("C: the T5 mid-window run — the 4096 floor-edge packet drives the same row"),
    Step(feed_merge(T5F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F)),
    Step(f"gt6machine wiremill inject 8 4096 {T5F}", expect=None),
    Step(f"gt6machine wiremill check {T5F}", expect="out[0]=1x leather",
         node_expects={"1.21.1": "out[0]=1x minecraft:leather"}, poll=60.0),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 431 62 275 441 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w2_eu_core slicer",
    slug="p29w2eucore_slicer",
    sites=gt6world.declare_sites(T1, T2, T3, T4, T5),
    preferred_ports=(26341, 26351),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
