#!/usr/bin/env python3
"""freezer — the Freezer family (CU, RM.FREEZER) live acceptance chain, the FULL
5-tier ladder T1-T5 (task exotic-energy ACCEPTANCE ④: the CU supply rides the
card-① gt6energy source-block dial — the CU arm shared with the CryoMixer chain).

Rig split (the energy_types arm-E source-dial precedent):
  T1 — the LIVE CU rig: the :1621 energy mask is SBIT_B — the machine's BACK face. At
    the default north facing the back maps to world SOUTH (the GTSideTables
    FACING_ROTATIONS row 2: world 3 -> relative 5), so the rig sits SOUTH of the machine
    (the first side-face rig of the card). EU -> refused (energy=0, active=false);
    CU -> the row completes. The water leg fills through the TOP face (the tank-in mask
    SBIT_U|SBIT_L).
  T5 — the inject rig over the 5-tier window (EV_TIER_INPUTS 4096/8192/16384).

The smoke row (data/gt6/recipe_maps/freezer.json): 1 snowball + 1000 L water -> 1 ice,
eUt 16 duration 16 — the row SHAPE mirrors the upstream freezer's water->ice freezing
semantics over vanilla entries (the real low-temperature fluid rows ride the batch-F
wave, the card ruling "smoke 行用在册流体").

passes=2 is the idempotency proof. No global state is touched — one session boot serves
the exotic cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/freezer.py
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

# the z=268 band: the freezer column x424..430 — the machine at z=268, the back-face rig
# at z=269 (world SOUTH of the north-facing machine), the T5 rung two steps east.
MILL_T1 = gt6world.Site(424, 64, 268, dy=1, dz=1)
RIG_BACK = gt6world.Site(424, 64, 269, dz=1)
MILL_T5 = gt6world.Site(428, 64, 268, dy=1, dz=1)
T1, T5 = F(MILL_T1), F(MILL_T5)

ICE_OUT = {"1.20.1": "out[0]=1x ice", "1.21.1": "out[0]=1x minecraft:ice"}
ICE_OUT_INJ = {"1.20.1": "outputs=[1x ice; ]", "1.21.1": "outputs=[1x minecraft:ice; ]"}


def feed_merge(pos, size, item):
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:{item}",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:{item}",count:1}}]}}}}',
    }


steps = [
    phase("A: T1 — the LIVE CU rig behind (the SBIT_B back face); EU refused, CU completes"),
    Step(f'setblock {T1} gt6:freezer', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(feed_merge(T1, 2, "snowball")["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 2, "snowball")),
    # the water leg fills through the TOP face (the :1621 tank-in mask SBIT_U|SBIT_L)
    Step(f"gt6machine wiremill fluid fill up minecraft:water 1000 {T1}", expect="filled 1000/1000"),
    # the source rig BEHIND the machine (world south — the :1621 SBIT_B energy face)
    Step(f"gt6energy place {F(RIG_BACK)}", expect="GT6 energy source placed at 424, 64, 269"),
    # EU dial: the freezer carries CU (:1621) — the packets are refused, the gate holds
    Step(f"gt6energy type {F(RIG_BACK)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(RIG_BACK)} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="energy=0"),
    Step(f"gt6machine wiremill check {T1}", expect="active=false"),
    # CU re-dial: the SAME rig now drives the machine
    Step(f"gt6energy type {F(RIG_BACK)} CU", expect="type ENERGY.CRYO"),
    Step(f"gt6energy stat {F(RIG_BACK)}", expect="type ENERGY.CRYO", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect=ICE_OUT["1.20.1"], node_expects=ICE_OUT, poll=45.0),
    # the water leg was consumed 1000 L (the row carries the full bucket; the empty tank prints "nothing")
    Step(f"gt6machine wiremill fluid stat {T1}", expect="in[0]=0 L of nothing"),

    phase("B: T5 — the 5-tier rung, the EV_TIER_INPUTS window (4096/8192/16384)"),
    Step(f'setblock {T5} gt6:freezer_t5', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(feed_merge(T5, 2, "snowball")["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5, 2, "snowball")),
    Step(f"gt6machine wiremill fluid fill up minecraft:water 1000 {T5}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 8 4096 {T5}", expect=ICE_OUT_INJ["1.20.1"], node_expects=ICE_OUT_INJ),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 423 62 267 430 67 269 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="exotic freezer",
    slug="freezer",
    sites=gt6world.declare_sites(MILL_T1, RIG_BACK, MILL_T5),
    preferred_ports=(26364, 26374),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
