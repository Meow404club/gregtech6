#!/usr/bin/env python3
"""cryo_mixer — the Cryo Mixer family (CU, RM.CRYO_MIXER) live acceptance chain,
the FULL 5-tier ladder T1-T5 (task exotic-energy ACCEPTANCE ②/④: the
CRYO_PARALLEL {4,8,16,32,64} duration-T five-element ladder asserted per tier, and the
CU supply riding the card-① gt6energy source-block dial).

Rig split (the energy_types arm-E source-dial precedent):
  T1 — the LIVE CU rig: the :1628 energy mask is SBIT_D — the rig sits UNDER the
    machine (the bottom face). EU -> refused (energy=0, active=false); CU -> the row
    completes. The water leg fills through the TOP face (the tank-in mask SBIT_L|SBIT_U,
    auto TOP).
  T1-T5 — the parallel ladder: five setblock columns, each `check` pins
    parallel=4/8/16/32/64 AND the window ramp
    16/32/64 | 64/128/256 | 256/512/1024 | 1024/2048/4096 | 4096/8192/16384
    (TIER_INPUTS[0..3] + EV_TIER_INPUTS — the 5-tier 立行制 window column; the
    parallelDuration=true flag rides the same report).
  T5 — the inject rig closes the run face (inject 4096 over the T5 row (the brigadier inject-size cap)).

The smoke row (data/gt6/recipe_maps/cryomixer.json): 1 snowball + 1 clay_ball + 100 L
water -> 1 snow_block, eUt 16 duration 16 — a DECLARED smoke row (the cryogenic-chemical
rows ride the batch-F wave, the card ruling "smoke 行用在册流体"; the CRYO_MIXER map
MIN 2 gate is why the row carries two inputs).

passes=2 is the idempotency proof. No global state is touched — one session boot serves
the exotic cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/cryo_mixer.py
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

# the z=268 band: the cryo-mixer block x434..438 — five tier columns (T1..T5 eastward),
# the live rig under the T1 machine.
MILL_T1 = gt6world.Site(434, 64, 268, dy=1, dz=1)
MILL_T2 = gt6world.Site(435, 64, 268, dy=1, dz=1)
MILL_T3 = gt6world.Site(436, 64, 268, dy=1, dz=1)
MILL_T4 = gt6world.Site(437, 64, 268, dy=1, dz=1)
MILL_T5 = gt6world.Site(438, 64, 268, dy=1, dz=1)
T1, T2, T3, T4, T5 = F(MILL_T1), F(MILL_T2), F(MILL_T3), F(MILL_T4), F(MILL_T5)

SNOW_OUT = {"1.20.1": "out[0]=1x snow_block", "1.21.1": "out[0]=1x minecraft:snow_block"}
SNOW_OUT_INJ = {"1.20.1": "outputs=[1x snow_block; ]", "1.21.1": "outputs=[1x minecraft:snow_block; ]"}

# the parallel ladder (the CRYO_PARALLEL card-① constant, the :1628-1632 NBT_PARALLEL
# column) and the window ramp (TIER_INPUTS[0..3] + EV_TIER_INPUTS). The check report
# carries parallel=N AFTER parallelDuration= — the parallel pin rides the trailing form.
LADDER = [
    (T1, "gt6:cryo_mixer"   , "parallel=4 parallelDuration=true", "minIn=16 recIn=32 maxIn=64"),
    (T2, "gt6:cryo_mixer_t2", "parallel=8 parallelDuration=true", "minIn=64 recIn=128 maxIn=256"),
    (T3, "gt6:cryo_mixer_t3", "parallel=16 parallelDuration=true", "minIn=256 recIn=512 maxIn=1024"),
    (T4, "gt6:cryo_mixer_t4", "parallel=32 parallelDuration=true", "minIn=1024 recIn=2048 maxIn=4096"),
    (T5, "gt6:cryo_mixer_t5", "parallel=64 parallelDuration=true", "minIn=4096 recIn=8192 maxIn=16384"),
]


def feed_merge(pos, size, items):
    slots = "".join(
        f'{{Slot:{i}b,id:"minecraft:{item}",Count:1b}},' for i, item in enumerate(items)
    )
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{slots[:-1]}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{slots[:-1].replace("Count:1b", "count:1")}]}}}}',
    }


steps = [phase("A: the five tier columns — the CRYO_PARALLEL ladder and the window ramp pinned per tier")]
for t, block, parallel, window in LADDER:
    steps.append(Step(f'setblock {t} {block}', expect="Changed the block", sleep=0.5))
for t, block, parallel, window in LADDER:
    steps.append(Step(f"gt6machine wiremill check {t}", expect=window))
for t, block, parallel, window in LADDER:
    steps.append(Step(f"gt6machine wiremill check {t}", expect=parallel))

steps += [
    phase("B: T1 — the LIVE CU rig under; EU refused, CU drives the two-input row"),
    Step(feed_merge(T1, 7, ["snowball", "clay_ball"])["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 7, ["snowball", "clay_ball"])),
    Step(f"gt6machine wiremill fluid fill up minecraft:water 1000 {T1}", expect="filled 1000/1000"),
    Step(f"gt6energy place {F(gt6world.Site(434, 63, 268))}", expect="GT6 energy source placed at 434, 63, 268"),
    Step(f"gt6energy type {F(gt6world.Site(434, 63, 268))} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(gt6world.Site(434, 63, 268))} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="energy=0"),
    Step(f"gt6energy type {F(gt6world.Site(434, 63, 268))} CU", expect="type ENERGY.CRYO"),
    Step(f"gt6energy stat {F(gt6world.Site(434, 63, 268))}", expect="type ENERGY.CRYO", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect=SNOW_OUT["1.20.1"], node_expects=SNOW_OUT, poll=45.0),

    phase("C: T5 — the run face over the 5-tier window (the self-typed carrier inject)"),
    Step(feed_merge(T5, 7, ["snowball", "clay_ball"])["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5, 7, ["snowball", "clay_ball"])),
    Step(f"gt6machine wiremill fluid fill up minecraft:water 1000 {T5}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 8 4096 {T5}", expect=SNOW_OUT_INJ["1.20.1"], node_expects=SNOW_OUT_INJ),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 433 62 267 439 67 269 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="exotic cryo-mixer",
    slug="cryomixer",
    sites=gt6world.declare_sites(MILL_T1, MILL_T2, MILL_T3, MILL_T4, MILL_T5, gt6world.Site(434, 63, 268)),
    preferred_ports=(26365, 26375),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
