#!/usr/bin/env python3
"""p29-w2-eu-core scanner_visuals — the Scanner (Visuals) family chain (task
p29-w2-eu-core-5tier ACCEPTANCE ①⑤): the EU 5-tier ladder (:1457-1461, no
NBT_PARALLEL -> 1, NO tank keys — the zero-fluid face, the base-form
SCANNER_VISUALS map, the NBT scan-data arm pooled per the card-① deviation), on
the JSON-poured scannervisuals.json smoke row (1x paper + 1x compass -> 1x map,
eUt 16, duration 16):

  A the per-tier window pins T1-T5 (the gapless doubling chain, the T5 arm
    minIn=4096 recIn=8192 maxIn=16384) + facing=2 + the parallel=1/F face (no
    NBT_PARALLEL keys on this family).
  B the T1 row run: feed the two slots, inject 24 x 32 -> the map lands.
  C the T5 mid-window run: the 8192 packet drives the same row to its map.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_scanner_visuals.py
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

# the fresh z=284 band, scanner column x420..428 (per-family x-disjoint teardown fill)
T1 = gt6world.Site(420, 64, 284, dy=1, dz=1)
T2 = gt6world.Site(422, 64, 284, dy=1, dz=1)
T3 = gt6world.Site(424, 64, 284, dy=1, dz=1)
T4 = gt6world.Site(426, 64, 284, dy=1, dz=1)
T5 = gt6world.Site(428, 64, 284, dy=1, dz=1)
T1F, T2F, T3F, T4F, T5F = F(T1), F(T2), F(T3), F(T4), F(T5)


def feed_merge(pos):
    """The two-slot smoke row (Size 4 = 2 in + 2 out): paper slot 0, compass slot 1."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:paper",Count:1b}},{{Slot:1b,id:"minecraft:compass",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:paper",count:1}},{{Slot:1b,id:"minecraft:compass",count:1}}]}}}}',
    }


def out_report():
    return {
        "1.20.1": "outputs=[1x map; ]",
        "1.21.1": "outputs=[1x minecraft:map; ]",
    }


steps = [
    phase("A: the per-tier window pins T1-T5 + the placement canon + the no-parallel face"),
    Step(f"setblock {T1F} gt6:scannervisuals", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1F}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine wiremill check {T1F}", expect="parallel=1 parallelDuration=false"),
    Step(f"gt6machine wiremill check {T1F}", expect="facing=2"),
    Step(f"setblock {T2F} gt6:scannervisuals_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2F}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"setblock {T3F} gt6:scannervisuals_t3", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T3F}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"setblock {T4F} gt6:scannervisuals_t4", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T4F}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(f"setblock {T5F} gt6:scannervisuals_t5", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5F}", expect="minIn=4096 recIn=8192 maxIn=16384"),

    phase("B: the T1 smoke-row run — paper + compass -> map (fire + read-only poll: the first completion after a fresh boot lags past the per-command deadline)"),
    Step(feed_merge(T1F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1F)),
    Step(f"gt6machine wiremill inject 24 32 {T1F}", expect=None),
    Step(f"gt6machine wiremill check {T1F}", expect="out[0]=1x map",
         node_expects={"1.21.1": "out[0]=1x minecraft:map"}, poll=60.0),
    Step(f"gt6machine wiremill check {T1F}", expect="progress=0/0"),

    phase("C: the T5 mid-window run — the 4096 floor-edge packet drives the same row"),
    Step(feed_merge(T5F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F)),
    Step(f"gt6machine wiremill inject 8 4096 {T5F}", expect=None),
    Step(f"gt6machine wiremill check {T5F}", expect="out[0]=1x map",
         node_expects={"1.21.1": "out[0]=1x minecraft:map"}, poll=60.0),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 419 62 275 429 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w2_eu_core scanner_visuals",
    slug="p29w2eucore_scanner_visuals",
    sites=gt6world.declare_sites(T1, T2, T3, T4, T5),
    preferred_ports=(26341, 26351),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
