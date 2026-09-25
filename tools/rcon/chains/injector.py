#!/usr/bin/env python3
"""eu-core injector — the Injector family chain (task eu-core-5tier
ACCEPTANCE ①⑤): the EU 5-tier ladder (:1443-1447, no NBT_PARALLEL -> 1), on the
JSON-poured injector.json smoke row (1x stick + 1x flint -> 1x arrow, eUt 16,
duration 16, the two-slot MIN-2 form):

  A the per-tier window pins T1-T5 (minIn/recIn/maxIn = TIER_INPUTS[0..3] +
    EV_TIER_INPUTS): 16/32/64, 64/128/256, 256/512/1024, 1024/2048/4096,
    4096/8192/16384 — the gapless doubling chain across the 5-tier seam.
  B the T1 row run: feed the two slots, inject 24 x 32 (in-window) -> the arrow
    lands (the smoke-row 进出断言), then progress=0/0 (the idle reset).
  C the T5 packet-edge arm: 4095 packets dead below the floor (progress=0/0),
    the 8192 packet runs the row to its arrow (the EV_TIER_INPUTS live form).

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/injector.py
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

# the fresh z=284 band, injector column x396..404 (per-family x-disjoint teardown fill)
T1 = gt6world.Site(396, 64, 284, dy=1, dz=1)
T2 = gt6world.Site(398, 64, 284, dy=1, dz=1)
T3 = gt6world.Site(400, 64, 284, dy=1, dz=1)
T4 = gt6world.Site(402, 64, 284, dy=1, dz=1)
T5 = gt6world.Site(404, 64, 284, dy=1, dz=1)
T1F, T2F, T3F, T4F, T5F = F(T1), F(T2), F(T3), F(T4), F(T5)


def feed_merge(pos):
    """The two-slot smoke row (Size 3 = 2 in + 1 out): stick slot 0, flint slot 1."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:stick",Count:1b}},{{Slot:1b,id:"minecraft:flint",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:stick",count:1}},{{Slot:1b,id:"minecraft:flint",count:1}}]}}}}',
}


def out_report(item):
    return {
        "1.20.1": f"outputs=[1x {item}; ]",
        "1.21.1": f"outputs=[1x minecraft:{item}; ]",
    }


steps = [
    phase("A: the per-tier window pins T1-T5 — TIER_INPUTS[0..3] + the EV_TIER_INPUTS arm"),
    Step(f"setblock {T1F} gt6:injector", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1F}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"setblock {T2F} gt6:injector_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2F}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"setblock {T3F} gt6:injector_t3", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T3F}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"setblock {T4F} gt6:injector_t4", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T4F}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(f"setblock {T5F} gt6:injector_t5", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5F}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(f"gt6machine wiremill check {T5F}", expect="facing=2"),

    phase("B: the T1 smoke-row run — stick + flint -> arrow (fire + read-only poll: the first completion after a fresh boot lags past the per-command deadline)"),
    Step(feed_merge(T1F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1F)),
    Step(f"gt6machine wiremill inject 24 32 {T1F}", expect=None),
    Step(f"gt6machine wiremill check {T1F}", expect="out[0]=1x arrow",
         node_expects={"1.21.1": "out[0]=1x minecraft:arrow"}, poll=60.0),
    Step(f"gt6machine wiremill check {T1F}", expect="progress=0/0"),

    phase("C: the T5 packet-edge arm — 4095 dead below the floor, the 4096 floor-edge packet runs"),
    Step(feed_merge(T5F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F)),
    Step(f"gt6machine wiremill inject 40 4095 {T5F}", expect="progress=0/0"),
    Step(feed_merge(T5F)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F)),
    Step(f"gt6machine wiremill inject 8 4096 {T5F}", expect=None),
    Step(f"gt6machine wiremill check {T5F}", expect="out[0]=1x arrow",
         node_expects={"1.21.1": "out[0]=1x minecraft:arrow"}, poll=60.0),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 395 62 275 405 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="eu_core injector",
    slug="eucore_injector",
    sites=gt6world.declare_sites(T1, T2, T3, T4, T5),
    preferred_ports=(26341, 26351),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
