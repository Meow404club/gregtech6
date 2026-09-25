#!/usr/bin/env python3
"""laser_welder — the Laser Welder family (LU, RM.WELDER) live acceptance chain,
the FULL 5-tier ladder T1-T5 (task exotic-energy).

Rig split (the energy_types arm-E source-dial precedent):
  T1 — the LIVE LU rig: the gt6energy source sits ABOVE the machine (the :1490 energy
    mask is SBIT_U). LU drives the row to completion. The chain also exercises the
    WELDER's tank-in shape (:1490 SBIT_D|SBIT_L auto BOTTOM — the ONLY tank-in face
    among the six exotic families with NO tank-out key, the map is 1/0/0 fluids): the
    `fluid fill down` arm fills 500 L through the machine's BOTTOM face.
  T5 — the inject rig over the 5-tier window (EV_TIER_INPUTS 4096/8192/16384).

The smoke row (data/gt6/recipe_maps/welder.json): 1 iron_ingot + 1 gold_ingot -> 1
chain, eUt 16 duration 16 — a DECLARED smoke row (the upstream welding rows ride the
batch-F fluid domain; the DYE_OREDICTS_LENS[Yellow] housing-lens dependency is the
unported crafting domain, the LENS item family pooled by the card ruling — the row
carries NO lens dependency). Two inputs exercise the WELDER map's MIN-2 gate.

passes=2 is the idempotency proof. No global state is touched — one session boot serves
the exotic cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/laser_welder.py
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

# the z=268 band: the laser-welder column x414..420, the live rig y=65 (above — the
# :1490 SBIT_U energy face).
MILL_T1 = gt6world.Site(414, 64, 268, dy=1, dz=1)
MILL_T5 = gt6world.Site(418, 64, 268, dy=1, dz=1)
T1, T5 = F(MILL_T1), F(MILL_T5)

CHAIN_OUT = {"1.20.1": "out[0]=1x chain", "1.21.1": "out[0]=1x minecraft:chain"}
CHAIN_OUT_INJ = {"1.20.1": "outputs=[1x chain; ]", "1.21.1": "outputs=[1x minecraft:chain; ]"}


def feed_merge(pos, size, items):
    slots = "".join(
        f'{{Slot:{i}b,id:"minecraft:{item}",Count:1b}},' for i, item in enumerate(items)
    )
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{slots[:-1]}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{slots[:-1].replace("Count:1b", "count:1")}]}}}}',
    }


steps = [
    phase("A: T1 — the LIVE LU rig above; the tank-in face filled through the BOTTOM"),
    Step(f'setblock {T1} gt6:laser_welder', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(feed_merge(T1, 10, ["iron_ingot", "gold_ingot"])["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 10, ["iron_ingot", "gold_ingot"])),
    # the :1490 tank-in mask SBIT_D|SBIT_L — the bottom face fills (the machine exposes
    # a sided fluid handler there; the map's fluid leg is OPTIONAL 1/0/0, the row runs dry
    # too, the fill only pins the face geometry)
    Step(f"gt6machine wiremill fluid fill down minecraft:water 500 {T1}", expect="filled 500/500"),
    # the source rig ABOVE the machine (the :1490 SBIT_U energy face)
    Step(f"gt6energy place {F(gt6world.Site(414, 65, 268))}", expect="GT6 energy source placed at 414, 65, 268"),
    Step(f"gt6energy type {F(gt6world.Site(414, 65, 268))} LU", expect="type ENERGY.LIGHT"),
    Step(f"gt6energy mode {F(gt6world.Site(414, 65, 268))} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect=CHAIN_OUT["1.20.1"], node_expects=CHAIN_OUT, poll=45.0),

    phase("B: T5 — the 5-tier rung, the EV_TIER_INPUTS window (4096/8192/16384)"),
    Step(f'setblock {T5} gt6:laser_welder_t5', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(feed_merge(T5, 10, ["iron_ingot", "gold_ingot"])["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5, 10, ["iron_ingot", "gold_ingot"])),
    Step(f"gt6machine wiremill inject 8 4096 {T5}", expect=CHAIN_OUT_INJ["1.20.1"], node_expects=CHAIN_OUT_INJ),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 413 62 267 420 67 269 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="exotic laser-welder",
    slug="laserwelder",
    sites=gt6world.declare_sites(MILL_T1, MILL_T5, gt6world.Site(414, 65, 268)),
    preferred_ports=(26363, 26373),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
