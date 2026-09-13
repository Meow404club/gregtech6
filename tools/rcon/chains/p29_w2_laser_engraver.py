#!/usr/bin/env python3
"""p29_w2_laser_engraver — the Laser Engraver family (LU, RM.LASER_ENGRAVER) live
acceptance chain, the FULL 5-tier ladder T1-T5 (task p29-w2-exotic-energy ACCEPTANCE ①:
the LU arm of the three-type type-gate trio — the machine eats LU packets and REFUSES
EU packets).

Rig split (the p29_w2_energy_types arm-E source-dial precedent):
  T1 — the LIVE LU rig: the gt6energy source sits ABOVE the machine (the :1483 energy
    mask is SBIT_U). EU -> refused (energy=0, active=false); LU -> the row completes.
  T5 — the inject rig over the 5-tier window (EV_TIER_INPUTS 4096/8192/16384).

The NAME column is the card's declaration-fidelity face: the upstream rows name
"(T1)".."(T5)" — LITERAL tier words, NOT voltage words (:1483-1487). The menu-less
carrier exposes no RCON name surface, so the composed-name pin rides the OFFLINE half
(GT6LangParityTest "Laser Engraver (T1)"/"(T5)" + 激光刻蚀机 (T5), both faces, both ends
of the ladder) — this chain pins the window and the run instead.

The smoke row (data/gt6/recipe_maps/laserengraver.json): 1 paper + 1 ink_sac -> 1
name_tag, eUt 16 duration 16 — a DECLARED smoke row (the upstream engraving rows ride
the circuit/hardened-clay crafting domain, unported; the LENS item family is pooled by
the card ruling — this row carries NO lens dependency).

passes=2 is the idempotency proof. No global state is touched — one session boot serves
the p29_w2_exotic cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_laser_engraver.py
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

# the z=268 band: the laser-engraver column x404..410, the live rig y=65 (above — the
# :1483 SBIT_U energy face).
MILL_T1 = gt6world.Site(404, 64, 268, dy=1, dz=1)
MILL_T5 = gt6world.Site(408, 64, 268, dy=1, dz=1)
T1, T5 = F(MILL_T1), F(MILL_T5)

TAG_OUT = {"1.20.1": "out[0]=1x name_tag", "1.21.1": "out[0]=1x minecraft:name_tag"}
TAG_OUT_INJ = {"1.20.1": "outputs=[1x name_tag; ]", "1.21.1": "outputs=[1x minecraft:name_tag; ]"}


def feed_merge(pos, size, items):
    """The two-slot feed (the LASER_ENGRAVER map is 2 in / 1 out, MIN 2 — two inputs)."""
    slots = "".join(
        f'{{Slot:{i}b,id:"minecraft:{item}",Count:1b}},' for i, item in enumerate(items)
    )
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{slots[:-1]}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{slots[:-1].replace("Count:1b", "count:1")}]}}}}',
    }


steps = [
    phase("A: T1 — the LIVE LU rig above; EU refused, LU drives the two-input row"),
    Step(f'setblock {T1} gt6:laser_engraver', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(feed_merge(T1, 3, ["paper", "ink_sac"])["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 3, ["paper", "ink_sac"])),
    # the source rig ABOVE the machine (the :1483 SBIT_U energy face)
    Step(f"gt6energy place {F(gt6world.Site(404, 65, 268))}", expect="GT6 energy source placed at 404, 65, 268"),
    # EU dial: the engraver carries LU (:1483) — the packets are refused, the gate holds
    Step(f"gt6energy type {F(gt6world.Site(404, 65, 268))} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(gt6world.Site(404, 65, 268))} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect="energy=0"),
    Step(f"gt6machine wiremill check {T1}", expect="active=false"),
    # LU re-dial: the SAME rig now drives the machine
    Step(f"gt6energy type {F(gt6world.Site(404, 65, 268))} LU", expect="type ENERGY.LIGHT"),
    Step(f"gt6energy stat {F(gt6world.Site(404, 65, 268))}", expect="type ENERGY.LIGHT", sleep=2.0),
    Step(f"gt6machine wiremill check {T1}", expect=TAG_OUT["1.20.1"], node_expects=TAG_OUT, poll=45.0),

    phase("B: T5 — the 5-tier rung, the EV_TIER_INPUTS window (4096/8192/16384)"),
    Step(f'setblock {T5} gt6:laser_engraver_t5', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T5}", expect="minIn=4096 recIn=8192 maxIn=16384"),
    Step(feed_merge(T5, 3, ["paper", "ink_sac"])["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T5, 3, ["paper", "ink_sac"])),
    Step(f"gt6machine wiremill inject 8 4096 {T5}", expect=TAG_OUT_INJ["1.20.1"], node_expects=TAG_OUT_INJ),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 403 62 267 410 67 269 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w2_exotic laser-engraver",
    slug="p29w2laserengraver",
    sites=gt6world.declare_sites(MILL_T1, MILL_T5, gt6world.Site(404, 65, 268)),
    preferred_ports=(26362, 26372),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
