#!/usr/bin/env python3
"""p29_w1_sluice — the Sluice family (RU, RM.SLUICE) live acceptance chain, the FULL
ladder T1-T4 (task p29-w1-kinetic-process-ladder ACCEPTANCE ④: 双最低输入行跑通).

The sluice is the PLAIN BasicMachine — NO world interaction, NO flowing-water
dependency: the 流水 lives in the RECIPE domain (RM.SLUICE fluid 1/1/1 + MIN 2 =
every row MUST carry BOTH an item leg AND a fluid leg, the :708/:709/:710 gate trio
at checkRecipe). The row (data/gt6/recipe_maps/sluice.json): 1 sand + 100 L water ->
1 clay_ball + 1 gold_nugget, eUt 16 duration 16.

Rig split (the card RCON ruling):
  T1 — the LIVE kinetic fixture (diesel -> axle -> machine, the SBIT_B back energy
    face rotated east by the NBT facing merge) + the DUAL-LEG NEGATIVE: water alone
    (no item) never starts a process — progress=0/0 and the tank intact — then the
    sand feed completes the row.
  T2-T4 — the gt6machine inject rig (the p26_w1_wiremill form), each tier fed both
    minimum legs.

check pins: data=-2, parallel=1 parallelDuration=false, the T2 TIER_INPUTS ramp.
The tank verdicts ride `fluid stat` (in[0]=... of minecraft:water).

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_sluice.py
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

# the fresh z=252 band — the sluice column x420..427 (family x-disjoint)
ENGINE = gt6world.Site(420, 64, 252)
AXLE1 = gt6world.Site(421, 64, 252)
MILL_T1 = gt6world.Site(422, 64, 252)
MILL_T2 = gt6world.Site(424, 64, 252)
MILL_T3 = gt6world.Site(426, 64, 252)
MILL_T4 = gt6world.Site(428, 64, 252)
T1, T2, T3, T4 = F(MILL_T1), F(MILL_T2), F(MILL_T3), F(MILL_T4)

DIESEL = "gt6:diesel_engine_bronze"
AXLE = "gt6:axle_wood_treated_small"


def feed_merge(pos, count):
    """Count sand (the Count key fork)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:10,Items:[{{Slot:0b,id:"minecraft:sand",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:10,Items:[{{Slot:0b,id:"minecraft:sand",count:{count}}}]}}}}',
    }


def pair_report(clay, nugget):
    """The inject outputs list (the clay + gold-nugget pair per row)."""
    return {
        "1.20.1": f"outputs=[{clay}x clay_ball; {nugget}x gold_nugget; ]",
        "1.21.1": f"outputs=[{clay}x minecraft:clay_ball; {nugget}x minecraft:gold_nugget; ]",
    }


steps = [
    phase("A: T1 — the diesel -> axle live rig; the DUAL-LEG gate: water alone refuses, item+water completes"),
    Step(f"setblock {T1} gt6:sluice", expect="Changed the block", sleep=1.0),
    Step(f"data merge block {T1} {{facing:5}}", expect="Modified block data"),  # back = west toward the axle
    # the fluid leg alone — the :708 MIN-ITEM gate refuses (no recipe ever binds)
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T1}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 20 64 {T1}", expect="progress=0/0"),
    Step(f"gt6machine wiremill fluid stat {T1}", expect="in[0]=1000 L of minecraft:water"),
    # both minimum legs -> the row completes over the diesel
    Step(feed_merge(T1, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 1)),
    Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {F(AXLE1)} {AXLE}[axis=x]", expect="Changed the block"),
    Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled"),
    Step(f"gt6machine wiremill check {T1}", expect="out[0]=1x clay_ball",
         node_expects={"1.21.1": "out[0]=1x minecraft:clay_ball"}, poll=45.0),
    Step(f"gt6machine wiremill check {T1}", expect="out[1]=1x gold_nugget",
         node_expects={"1.21.1": "out[1]=1x minecraft:gold_nugget"}),
    Step(f"gt6machine wiremill fluid stat {T1}", expect="in[0]=900 L of minecraft:water"),
    Step(f"gt6machine wiremill check {T1}", expect="parallel=1 parallelDuration=false"),

    phase("B: T2-T4 — the inject rig, both minimum legs every tier"),
    Step(f"setblock {T2} gt6:sluice_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(feed_merge(T2, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T2}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 256 {T2}", expect=pair_report(1, 1)["1.20.1"], node_expects=pair_report(1, 1)),
    Step(f"setblock {T3} gt6:sluice_t3", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T3, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T3, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T3}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 1024 {T3}", expect=pair_report(1, 1)["1.20.1"], node_expects=pair_report(1, 1)),
    Step(f"setblock {T4} gt6:sluice_t4", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T4, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T4, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T4}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 4096 {T4}", expect=pair_report(1, 1)["1.20.1"], node_expects=pair_report(1, 1)),

    phase("C: the row-carrier pins — menu-less carriers everywhere"),
    Step(f"gt6machine wiremill check {T3}", expect="data=-2"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 419 62 251 430 68 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w1_process sluice",
    slug="p29w1sluice",
    sites=gt6world.declare_sites(ENGINE, AXLE1, MILL_T1, MILL_T2, MILL_T3, MILL_T4),
    preferred_ports=(26190, 26200),      # the cluster's pinned pair (shared session boot)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
