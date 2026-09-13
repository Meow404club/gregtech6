#!/usr/bin/env python3
"""p29_w1_centrifuge — the Centrifuge family (RU, RM.CENTRIFUGE) live acceptance chain,
the FULL ladder T1-T4 (task p29-w1-kinetic-process-ladder ACCEPTANCE ③: 并行 1/2/4/8
非标断言 — the NON-standard CENTRIFUGE_PARALLEL table, the T1 = 1 arm is what makes
the ladder non-standard; the offline contrast pins the Crusher 4/8/16/32 shape).

Rig split (the card RCON ruling):
  T1 — the LIVE kinetic fixture, diesel engine -> wood-small axle -> machine (the
    machine's SBIT_D energy face = BOTTOM, so the machine sits ON the axle: the
    engine emits 16 RU/t DC, the axle forwards into the machine's bottom face).
  T2-T4 — the gt6machine inject rig (the p26_w1_wiremill form; RU delivers on every
    completed tick of a plain positive train).

The centrifuge row (data/gt6/recipe_maps/centrifuge.json): 2 snowballs -> 1 clay_ball
+ 1 flint, eUt 16 duration 16, NBT_PARALLEL_DURATION T:

  T1 parallel 1: 2 snowballs -> ONE row per completion -> 1x clay + 1x flint.
  T2 parallel 2 (packet 256): 4 snowballs -> 2x + 2x.
  T3 parallel 4 (packet 1024): 8 snowballs -> 4x + 4x.
  T4 parallel 8 (packet 4096): 16 snowballs -> 8x + 8x.

The item verdict rides the inject/check REPORTS (the outputs=[...] list — the
deterministic getOutputs(count) overload resolves every chance>0 slot at full
certainty, Recipe.java:186).

check pins: data=-2 (the menu-less carrier), parallel=P parallelDuration=true —
the T1 parallel=1 IS the non-standard face (the sifter/compressor/crusher T1 arms
all carry 4), and the T2 TIER_INPUTS ramp {64,128,256}.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_centrifuge.py
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

# the fresh z=252 band — the centrifuge column x408..418 (family x-disjoint; T1
# carries the diesel rig: engine 408, axle 409 under the machine 409 y65)
ENGINE = gt6world.Site(408, 64, 252)
AXLE1 = gt6world.Site(409, 64, 252)
MILL_T1 = gt6world.Site(409, 65, 252)
MILL_T2 = gt6world.Site(412, 64, 252)
MILL_T3 = gt6world.Site(414, 64, 252)
MILL_T4 = gt6world.Site(416, 64, 252)
T1, T2, T3, T4 = F(MILL_T1), F(MILL_T2), F(MILL_T3), F(MILL_T4)

DIESEL = "gt6:diesel_engine_bronze"
AXLE = "gt6:axle_wood_treated_small"


def feed_merge(pos, count):
    """Count snowballs (the Count key fork)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:7,Items:[{{Slot:0b,id:"minecraft:snowball",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:7,Items:[{{Slot:0b,id:"minecraft:snowball",count:{count}}}]}}}}',
    }


def pair_report(clay, flint):
    """The inject outputs list for one completion: clay first, flint second."""
    return {
        "1.20.1": f"outputs=[{clay}x clay_ball; {flint}x flint; ]",
        "1.21.1": f"outputs=[{clay}x minecraft:clay_ball; {flint}x minecraft:flint; ]",
    }


steps = [
    phase("A: T1 — the diesel -> axle rig under the machine (the SBIT_D bottom energy face); parallel 1 = ONE row per completion"),
    Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {F(AXLE1)} {AXLE}[axis=y]", expect="Changed the block"),
    Step(f"setblock {T1} gt6:centrifuge", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T1, 2)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 2)),
    Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled"),
    Step(f"gt6machine wiremill check {T1}", expect="out[0]=1x clay_ball",
         node_expects={"1.21.1": "out[0]=1x minecraft:clay_ball"}, poll=45.0),
    Step(f"gt6machine wiremill check {T1}", expect="out[1]=1x flint",
         node_expects={"1.21.1": "out[1]=1x minecraft:flint"}),
    Step(f"gt6machine wiremill check {T1}", expect="parallel=1 parallelDuration=true"),
    Step(f"gt6machine wiremill check {T1}", expect="data=-2"),

    phase("B: T2-T4 — the inject rig; the 2/4/8 arms multiply the ONE row (2 snowballs) by the parallel table"),
    Step(f"setblock {T2} gt6:centrifuge_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(feed_merge(T2, 4)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2, 4)),
    Step(f"gt6machine wiremill inject 8 256 {T2}", expect=pair_report(2, 2)["1.20.1"], node_expects=pair_report(2, 2)),
    Step(f"setblock {T3} gt6:centrifuge_t3", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T3, 8)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T3, 8)),
    Step(f"gt6machine wiremill inject 8 1024 {T3}", expect=pair_report(4, 4)["1.20.1"], node_expects=pair_report(4, 4)),
    Step(f"setblock {T4} gt6:centrifuge_t4", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T4, 16)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T4, 16)),
    Step(f"gt6machine wiremill inject 8 4096 {T4}", expect=pair_report(8, 8)["1.20.1"], node_expects=pair_report(8, 8)),

    phase("C: the non-standard ladder pins — parallel 2/4/8 read back (the T1 = 1 arm is the face no 4/8/16/32 family carries)"),
    Step(f"gt6machine wiremill check {T2}", expect="parallel=2 parallelDuration=true"),
    Step(f"gt6machine wiremill check {T3}", expect="parallel=4 parallelDuration=true"),
    Step(f"gt6machine wiremill check {T4}", expect="parallel=8 parallelDuration=true"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 407 62 251 418 68 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w1_process centrifuge",
    slug="p29w1centrifuge",
    sites=gt6world.declare_sites(ENGINE, AXLE1, MILL_T1, MILL_T2, MILL_T3, MILL_T4),
    preferred_ports=(26190, 26200),      # the cluster's pinned pair (shared session boot)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
