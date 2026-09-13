#!/usr/bin/env python3
"""p29_w1_pressurewasher — the Pressure Washer family (RU, RM.PRESSURE_WASHER) live
acceptance chain, the FULL ladder T1-T4 (task p29-w1-kinetic-process-ladder
ACCEPTANCE ⑤: 水耗扣减断言). The NBT_TEXTURE "debarker" stays verbatim upstream
(the art-token fidelity) but the machine name/registry path is the pressure washer.

The washer row (data/gt6/recipe_maps/pressurewasher.json): 1 coarse_dirt + 100 L
water -> 1 dirt, eUt 16 duration 16 — the water-consumption face is the :1615 tank
IN mask SBIT_U|SBIT_D auto TOP (no tank OUT key — the map is IN 1 / OUT 0 fluids).
Parallel 1: every process drains exactly one recipe-worth.

Rig split (the card RCON ruling):
  T1 — the LIVE kinetic fixture (diesel -> axle -> machine, the SBIT_B back face).
  T2 — the NEGATIVE: dry inject (no water) -> progress=0/0, then fill and complete.
  T3-T4 — the inject rig wet.

check pins: data=-2, parallel=1 parallelDuration=false, the T2 TIER_INPUTS ramp.
The consumption verdict rides `fluid stat`: 1000 fill -> 800 after two washes on T1.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_pressurewasher.py
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

# the fresh z=252 band — the washer column x444..451 (family x-disjoint)
ENGINE = gt6world.Site(444, 64, 252)
AXLE1 = gt6world.Site(445, 64, 252)
MILL_T1 = gt6world.Site(446, 64, 252)
MILL_T2 = gt6world.Site(448, 64, 252)
MILL_T3 = gt6world.Site(450, 64, 252)
MILL_T4 = gt6world.Site(452, 64, 252)
T1, T2, T3, T4 = F(MILL_T1), F(MILL_T2), F(MILL_T3), F(MILL_T4)

DIESEL = "gt6:diesel_engine_bronze"
AXLE = "gt6:axle_wood_treated_small"


def feed_merge(pos, count):
    """Count coarse dirt (the Count key fork)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:coarse_dirt",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:coarse_dirt",count:{count}}}]}}}}',
    }


DIRT_INJ = {"1.20.1": "outputs=[1x dirt; ]", "1.21.1": "outputs=[1x minecraft:dirt; ]"}

steps = [
    phase("A: T1 — the diesel -> axle live rig; TWO washes drain 2x100 L of the 1000 fill"),
    Step(f"setblock {T1} gt6:pressure_washer", expect="Changed the block", sleep=1.0),
    Step(f"data merge block {T1} {{facing:5}}", expect="Modified block data"),  # back = west toward the axle
    Step(feed_merge(T1, 2)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 2)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T1}", expect="filled 1000/1000"),
    Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {F(AXLE1)} {AXLE}[axis=x]", expect="Changed the block"),
    Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled"),
    Step(f"gt6machine wiremill check {T1}", expect="out[0]=2x dirt",
         node_expects={"1.21.1": "out[0]=2x minecraft:dirt"}, poll=45.0),
    Step(f"gt6machine wiremill check {T1}", expect="parallel=1 parallelDuration=false"),
    Step(f"gt6machine wiremill check {T1}", expect="data=-2"),
    # THE consumption verdict: 1000 - 2x100
    Step(f"gt6machine wiremill fluid stat {T1}", expect="in[0]=800 L of minecraft:water"),

    phase("B: T2 — the dry NEGATIVE then the wet control; T3-T4 wet inject"),
    Step(f"setblock {T2} gt6:pressure_washer_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(feed_merge(T2, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2, 1)),
    Step(f"gt6machine wiremill inject 6 256 {T2}", expect="progress=0/0"),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T2}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 256 {T2}", expect=DIRT_INJ["1.20.1"], node_expects=DIRT_INJ),
    Step(f"setblock {T3} gt6:pressure_washer_t3", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T3, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T3, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T3}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 1024 {T3}", expect=DIRT_INJ["1.20.1"], node_expects=DIRT_INJ),
    Step(f"setblock {T4} gt6:pressure_washer_t4", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T4, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T4, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T4}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 4096 {T4}", expect=DIRT_INJ["1.20.1"], node_expects=DIRT_INJ),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 443 62 251 454 68 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w1_process pressurewasher",
    slug="p29w1pressurewasher",
    sites=gt6world.declare_sites(ENGINE, AXLE1, MILL_T1, MILL_T2, MILL_T3, MILL_T4),
    preferred_ports=(26190, 26200),      # the cluster's pinned pair (shared session boot)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
