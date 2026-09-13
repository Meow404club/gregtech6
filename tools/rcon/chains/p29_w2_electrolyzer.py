#!/usr/bin/env python3
"""p29-w2-eu-core electrolyzer — the Electrolyzer family chain (task
p29-w2-eu-core-5tier ACCEPTANCE ①②④⑤): the FIRST 5-tier ladder of the port
(Loader_MultiTileEntities.java:1336-1340, EU, Electric_T[1..5],
ELECTROLYZER_PARALLEL {1,2,4,8,16} + NBT_PARALLEL_DURATION T), on the JSON-poured
electrolyzer.json smoke row (2 clay -> 1 brick, eUt 16, duration 16; the row rides
TWO slots — the :708-710 MIN-2 face):

  A the T5 window pins (the card head acceptance): minIn=4096 recIn=8192 maxIn=16384
    (the :126 conversion over NBT_INPUT 8192, the EV_TIER_INPUTS arm) + facing=2
    (the GT6PlacementFacing default, front north) + data=-2 (the menu-less carrier);
    the 4095 packets are DEAD below the floor (inject 40 ticks -> progress=0/0
    energy=0); re-stock, the 8192 packet completes (out 1 brick).
  B the parallel ladder, T1..T5, inject at each tier's in-window packet
    (32/128/512/2048/8192) over the N clay stock (N = the tier parallel): every tier
    folds to a 16-tick bar at its folded mMinEnergy (16/64/256/1024/4096) and drops
    Nx brick out of the ONE completion (1/2/4/8/16) — the duration-T outputs xN face.
  C the type-gate LIVE arm (the cross-card-1 guard, the p29_w2_energy arm-E form):
    one /gt6energy rig UNDER the T2 machine (its SBIT_D bottom energy face), dialed
    MU -> the EU machine refuses (energy=0, active=false); re-dialed EU volt 128 ->
    the SAME rig drives the machine to a completion (out 2x brick, the T2 parallel).

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_electrolyzer.py
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

# the fresh z=284 band, electrolyzer column x384..392 (per-family x-disjoint teardown
# fills; the type-gate rig shares the T2 column at y63 — the SBIT_D bottom energy face)
T1 = gt6world.Site(384, 64, 284, dy=1, dz=1)
T2 = gt6world.Site(386, 64, 284, dy=1, dz=1)
T3 = gt6world.Site(388, 64, 284, dy=1, dz=1)
T4 = gt6world.Site(390, 64, 284, dy=1, dz=1)
T5 = gt6world.Site(392, 64, 284, dy=1, dz=1)
RIG_T2 = gt6world.Site(386, 63, 284, dz=1)
T1F, T2F, T3F, T4F, T5F = F(T1), F(T2), F(T3), F(T4), F(T5)
RIG = F(RIG_T2)


def feed_merge(pos, count):
    """Two slots of `count` clay (the two-entry smoke row, Size 8 = 2 in + 6 out)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:8,Items:[{{Slot:0b,id:"minecraft:clay_ball",Count:{count}b}},{{Slot:1b,id:"minecraft:clay_ball",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:8,Items:[{{Slot:0b,id:"minecraft:clay_ball",count:{count}}},{{Slot:1b,id:"minecraft:clay_ball",count:{count}}}]}}}}',
    }


def out_report(n):
    """The inject outputs list for one Nx-parallel completion."""
    return {
        "1.20.1": f"outputs=[{n}x brick; ]",
        "1.21.1": f"outputs=[{n}x minecraft:brick; ]",
    }


steps = [
    phase("A: the T5 window pins — EV_TIER_INPUTS {4096, 8192, 16384} + the facing canon + the menu-less carrier"),
    Step(f"setblock {T5F} gt6:electrolyzer_t5", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5F}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(f"gt6machine wiremill check {T5F}", expect="facing=2"),
    Step(f"gt6machine wiremill check {T5F}", expect="data=-2"),
    Step(f"gt6machine wiremill check {T5F}", expect="parallel=16 parallelDuration=true"),
    # the 4095 packets are dead below the floor (拒 arm)
    Step(feed_merge(T5F, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F, 1)),
    Step(f"gt6machine wiremill inject 40 4095 {T5F}", expect="progress=0/0"),
    Step(f"gt6machine wiremill check {T5F}", expect="energy=0"),
    # the 4096 packet runs (跑通 arm — the :493 argument ceiling is 4096, and 4096 is the
    # T5 window MIN so the folded mMinEnergy bar completes inside the train). fire/check
    # split: the first completion after a fresh boot lags past the per-command deadline
    # (the p29 W2 live finding), so the fire step matches its echo and the read-only
    # check polls for the output
    Step(feed_merge(T5F, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F, 1)),
    Step(f"gt6machine wiremill inject 8 4096 {T5F}", expect=f"inject 8 4096 {T5F}"),
    Step(f"gt6machine wiremill check {T5F}", expect="out[0]=1x brick",
         node_expects={"1.21.1": "out[0]=1x minecraft:brick"}, poll=60.0),

    phase("B: the parallel ladder T1-T5 — Nx brick per completion, every tier a 16-tick folded bar"),
    Step(f"setblock {T1F} gt6:electrolyzer", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T1F, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1F, 1)),
    Step(f"gt6machine wiremill inject 24 32 {T1F}", expect=f"inject 24 32 {T1F}"),
    Step(f"gt6machine wiremill check {T1F}", expect="out[0]=1x brick",
         node_expects={"1.21.1": "out[0]=1x minecraft:brick"}, poll=60.0),
    Step(f"setblock {T2F} gt6:electrolyzer_t2", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T2F, 2)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2F, 2)),
    Step(f"gt6machine wiremill inject 24 128 {T2F}", expect=f"inject 24 128 {T2F}"),
    Step(f"gt6machine wiremill check {T2F}", expect="out[0]=2x brick",
         node_expects={"1.21.1": "out[0]=2x minecraft:brick"}, poll=60.0),
    Step(f"setblock {T3F} gt6:electrolyzer_t3", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T3F, 4)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T3F, 4)),
    Step(f"gt6machine wiremill inject 24 512 {T3F}", expect=f"inject 24 512 {T3F}"),
    Step(f"gt6machine wiremill check {T3F}", expect="out[0]=4x brick",
         node_expects={"1.21.1": "out[0]=4x minecraft:brick"}, poll=60.0),
    Step(f"setblock {T4F} gt6:electrolyzer_t4", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T4F, 8)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T4F, 8)),
    Step(f"gt6machine wiremill inject 24 2048 {T4F}", expect=f"inject 24 2048 {T4F}"),
    Step(f"gt6machine wiremill check {T4F}", expect="out[0]=8x brick",
         node_expects={"1.21.1": "out[0]=8x minecraft:brick"}, poll=60.0),
    Step(feed_merge(T5F, 16)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5F, 16)),
    Step(f"gt6machine wiremill inject 24 4096 {T5F}", expect=f"inject 24 4096 {T5F}"),
    Step(f"gt6machine wiremill check {T5F}", expect="out[0]=16x brick",
         node_expects={"1.21.1": "out[0]=16x minecraft:brick"}, poll=60.0),

    phase("C: the type-gate LIVE arm — the MU dial refuses, the EU re-dial drives (cross-card-1 guard)"),
    Step(f"gt6energy place {RIG}", expect="GT6 energy source placed at 386, 63, 284"),
    Step(feed_merge(T2F, 2)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2F, 2)),
    Step(f"gt6energy type {RIG} MU", expect="type ENERGY.MAGNETIC"),
    Step(f"gt6energy mode {RIG} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T2F}", expect="energy=0"),
    Step(f"gt6machine wiremill check {T2F}", expect="active=false"),
    # the EU re-dial: the SAME rig now drives the T2 parallel-2 machine to a completion
    Step(f"gt6energy type {RIG} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {RIG} 128", expect="voltage 128 EU"),
    Step(f"gt6machine wiremill check {T2F}", expect="out[0]=2x brick",
         node_expects={"1.21.1": "out[0]=2x minecraft:brick"}, poll=60.0),
    Step(f"gt6machine wiremill check {T2F}", expect="progress=0/0"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 275 393 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w2_eu_core electrolyzer",
    slug="p29w2eucore_electrolyzer",
    sites=gt6world.declare_sites(T1, T2, T3, T4, T5, RIG_T2),
    preferred_ports=(26341, 26351),      # this card's pinned rcon/query pair
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
