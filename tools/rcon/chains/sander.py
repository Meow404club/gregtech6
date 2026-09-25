#!/usr/bin/env python3
"""sander — the Sanding Machine family (RU, RM.SHARPENING) live acceptance
chain, the FULL ladder T1-T4 (task kinetic-process-ladder).

The sander is the item-only family of the six (the SHARPENING map is 0/0/0 fluids —
zero fluid MASKS, data-only; the fluid face stays, the seam-② hard constraint). No
parallel key -> 1; energy rides the TOP face (the :1589 SBIT_U row).

Rig split (the card RCON ruling):
  T1 — the LIVE kinetic fixture: the /gt6energy source sits DIRECTLY ABOVE the
    machine (the machine's SBIT_U top face meets it — no facing merge needed, top
    stays top under a horizontal facing). The diesel->axle form is impossible for a
    top-face machine: the engine is HORIZONTAL_FACING-only (GTDieselEngineBlock
    :42) so it can never emit downward, and the straight axle refuses perpendicular
    faces (GTAxleBlockEntity :72-74) — the p8-d4 source emits on all six sides.
  T2-T4 — the gt6machine inject rig.

The sharpening row (data/gt6/recipe_maps/sharpening.json): 1 sandstone -> 2x sand,
eUt 16 duration 16 — one row one process per tier, parallel=1 pins throughout.

check pins: data=-2, parallel=1 parallelDuration=false, the T2 TIER_INPUTS ramp, and
the zero-tank masks face (fluid stat: masks fluidIn=127 fluidOut=127 — the 127
all-sides upstream field defaults with no tank keys on the :1589 rows).

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/sander.py
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

# the fresh z=252 band — the sander column x432..440 (family x-disjoint; T1 carries
# the /gt6energy source rig ABOVE the machine: machine y64, source y65)
ENGINE = gt6world.Site(432, 65, 252)
MILL_T1 = gt6world.Site(432, 64, 252)
MILL_T2 = gt6world.Site(435, 64, 252)
MILL_T3 = gt6world.Site(437, 64, 252)
MILL_T4 = gt6world.Site(439, 64, 252)
T1, T2, T3, T4 = F(MILL_T1), F(MILL_T2), F(MILL_T3), F(MILL_T4)

# the T1 rig: the /gt6energy source (the p8-d4 command-driven test generator) sits at
# ENGINE, directly ABOVE the machine, feeding the SBIT_U top energy face. The engine
# block is HORIZONTAL_FACING-only (GTDieselEngineBlock.java:42) — it can never emit
# downward, and the straight axle refuses perpendicular faces (GTAxleBlockEntity
# :72-74), so a top-face machine cannot be engine-rigged. The source emits on all six
# sides (GTEnergySourceBlockEntity :60) — the gearbox-transformer chain form.
RIG = ENGINE


def feed_merge(pos, count=1):
    """Count sandstone (the Count key fork)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:sandstone",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:sandstone",count:{count}}}]}}}}',
    }


SAND_INJ = {"1.20.1": "outputs=[2x sand; ]", "1.21.1": "outputs=[2x minecraft:sand; ]"}

steps = [
    phase("A: T1 — the /gt6energy source ABOVE the machine (the SBIT_U top energy face); one row one process"),
    Step(f"setblock {T1} gt6:sanding_machine", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1)),
    Step(f"gt6energy place {F(RIG)}", expect=f"GT6 energy source placed at {RIG.x}, {RIG.y}, {RIG.z}"),
    Step(f"gt6energy type {F(RIG)} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6energy volt {F(RIG)} 32", expect="voltage 32"),
    Step(f"gt6energy mode {F(RIG)} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="out[0]=2x sand",
         node_expects={"1.21.1": "out[0]=2x minecraft:sand"}, poll=45.0),
    Step(f"gt6machine wiremill check {T1}", expect="parallel=1 parallelDuration=false"),
    Step(f"gt6machine wiremill check {T1}", expect="data=-2"),
    # the zero-tank masks face: no NBT_TANK_* keys on the :1589 rows -> the 127 defaults
    Step(f"gt6machine wiremill fluid stat {T1}", expect="masks fluidIn=127 fluidOut=127"),

    phase("B: T2-T4 — the inject rig over the window ramp"),
    Step(f"setblock {T2} gt6:sanding_machine_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(feed_merge(T2)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2)),
    Step(f"gt6machine wiremill inject 6 256 {T2}", expect=SAND_INJ["1.20.1"], node_expects=SAND_INJ),
    Step(f"setblock {T3} gt6:sanding_machine_t3", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T3)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T3)),
    Step(f"gt6machine wiremill inject 6 1024 {T3}", expect=SAND_INJ["1.20.1"], node_expects=SAND_INJ),
    Step(f"setblock {T4} gt6:sanding_machine_t4", expect="Changed the block", sleep=1.0),
    Step(feed_merge(T4)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T4)),
    Step(f"gt6machine wiremill inject 6 4096 {T4}", expect=SAND_INJ["1.20.1"], node_expects=SAND_INJ),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 431 62 251 441 69 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="process sander",
    slug="sander",
    sites=gt6world.declare_sites(ENGINE, MILL_T1, MILL_T2, MILL_T3, MILL_T4),
    preferred_ports=(26190, 26200),      # the cluster's pinned pair (shared session boot)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
