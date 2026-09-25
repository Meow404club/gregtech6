#!/usr/bin/env python3
"""magnetic_separator — the Magnetic Separator family (MU, RM.MAGNETIC_SEPARATOR)
live acceptance chain, the FULL 5-tier ladder T1-T5 (task exotic-energy).

Rig split (the energy_types arm-E source-dial precedent):
  T1 — the LIVE MU rig: the gt6energy source sits ABOVE the machine (the :1470 energy
    mask is SBIT_U — the machine's TOP face receives the rig's emission; the mirror of
    the polarizer's under-rig). The row carries a WATER leg (the tank-in mask SBIT_L =
    the machine's LEFT face = world EAST at the default north facing), filled through the
    `fluid fill east` arm — the first fluid-face exercise over the MU domain.
  T5 — the inject rig over the 5-tier window (EV_TIER_INPUTS 4096/8192/16384): check
    pins minIn/recIn/maxIn, inject 4096 runs the row (the brigadier inject-size cap; minIn IS the window floor).

The smoke row (data/gt6/recipe_maps/magneticseparator.json): 1 gravel + 100 L water ->
1 sand + 1 flint, eUt 16 duration 16 — a DECLARED smoke row (the ore-magnetic recipe
domain rides the batch-F wave). The two outputs exercise the SBIT_R|SBIT_D out face
shape (data-only at the check surface).

passes=2 is the idempotency proof. No global state is touched — one session boot serves
the exotic cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/magnetic_separator.py
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

# the z=268 band: the magnetic-separator column x394..400, the live rig y=65 (above —
# the :1470 SBIT_U energy face).
MILL_T1 = gt6world.Site(394, 64, 268, dy=1, dz=1)
MILL_T5 = gt6world.Site(398, 64, 268, dy=1, dz=1)
T1, T5 = F(MILL_T1), F(MILL_T5)

SAND_OUT = {"1.20.1": "out[0]=1x sand", "1.21.1": "out[0]=1x minecraft:sand"}
SAND_OUT_INJ = {"1.20.1": "outputs=[1x sand; 1x flint; ]", "1.21.1": "outputs=[1x minecraft:sand; 1x minecraft:flint; ]"}


def feed_merge(pos, size, item):
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:{item}",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:{item}",count:1}}]}}}}',
    }


steps = [
    phase("A: T1 — the LIVE MU rig above; the water leg filled through the LEFT (east) face"),
    Step(f'setblock {T1} gt6:magnetic_separator', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(feed_merge(T1, 7, "gravel")["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 7, "gravel")),
    # the tank-in mask SBIT_L = machine-relative LEFT = world EAST at the north facing
    Step(f"gt6machine wiremill fluid fill east minecraft:water 1000 {T1}", expect="filled 1000/1000"),
    # the source rig ABOVE the machine (the :1470 SBIT_U energy face)
    Step(f"gt6energy place {F(gt6world.Site(394, 65, 268))}", expect="GT6 energy source placed at 394, 65, 268"),
    # EU dial: refused (the MU carrier gate holds) — then the MU re-dial drives the row
    Step(f"gt6energy type {F(gt6world.Site(394, 65, 268))} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(gt6world.Site(394, 65, 268))} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="energy=0"),
    Step(f"gt6energy type {F(gt6world.Site(394, 65, 268))} MU", expect="type ENERGY.MAGNETIC"),
    Step(f"gt6energy stat {F(gt6world.Site(394, 65, 268))}", expect="type ENERGY.MAGNETIC", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect=SAND_OUT["1.20.1"], node_expects=SAND_OUT, poll=45.0),
    # the water leg was consumed 100 L of the 1000 filled (the :143 mask face accepted it)
    Step(f"gt6machine wiremill fluid stat {T1}", expect="in[0]=900 L of minecraft:water"),

    phase("B: T5 — the 5-tier rung, the EV_TIER_INPUTS window (4096/8192/16384)"),
    Step(f'setblock {T5} gt6:magnetic_separator_t5', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(feed_merge(T5, 7, "gravel")["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5, 7, "gravel")),
    Step(f"gt6machine wiremill fluid fill east minecraft:water 1000 {T5}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 8 4096 {T5}", expect=SAND_OUT_INJ["1.20.1"], node_expects=SAND_OUT_INJ),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 393 62 267 400 67 269 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="exotic magnetic-separator",
    slug="magneticseparator",
    sites=gt6world.declare_sites(MILL_T1, MILL_T5, gt6world.Site(394, 65, 268)),
    preferred_ports=(26361, 26371),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
