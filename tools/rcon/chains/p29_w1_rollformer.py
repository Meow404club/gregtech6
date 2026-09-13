#!/usr/bin/env python3
"""p29_w1_p29_w1_rollformer — the Roll Former RU ladder live acceptance chain (task
p29-w1-kinetic-roll-ladder, the p29_w1_rollingmill shape minus the diesel rig —
the rollingmill chain carries the cluster's real RU source; the sibling ladders
prove the same acceptance set through the /gt6machine inject driver, the same
driver the p26_w1 trio and the p28 arms C used).

  A THE 8 RU PACKET IS DEAD BELOW T1 (x410): one stick_iron in, an inject of 60 x 8
    leaves progress=0/0 (the 8 RU packet is below the ladder min 16 — the ULV-wall
    semantics on the RU side), and the CONTROL leg on the SAME machine (inject 60 x 64)
    completes the row: the red is the packet size, not the rig.
  B THE T2-T4 INJECT LADDER: one item in, packet = the tier max (256/1024/4096), the
    poured smoke row (stick_iron -> rail_gt_iron, eUt 16 / duration 32) completes on every
    tier — the outputs pin 1x rail_gt_iron.
  C THE TIER_INPUTS WINDOW PINS: minIn=64 recIn=128 maxIn=256 / minIn=256 recIn=512 maxIn=1024 / minIn=1024 recIn=2048 maxIn=4096 across the four tiers
    (the :126 conversion of NBT_INPUT 128/512/2048, read off the BE reports).

The item-name expects are PER-LEG via node_expects (the p26_w1 fork): bare registry
path on 1.20.1, NAMESPACED on 21.1.

passes=2 is the idempotency proof. D teardown: the explicit fill-air over the band. No
global state is touched (no mutates — one session boot serves the whole roll cluster).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_rollformer.py
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

# --- arm A: the 8 RU wall on a T1 (x410)
MILL_T1 = gt6world.Site(410, 64, 180)
# --- arm B: the T2-T4 inject ladder (x412..x416)
MILL_T2 = gt6world.Site(412, 64, 180)
MILL_T3 = gt6world.Site(414, 64, 180)
MILL_T4 = gt6world.Site(416, 64, 180)

MILL_T1_P = F(MILL_T1)
MILL_T2_P, MILL_T3_P, MILL_T4_P = F(MILL_T2), F(MILL_T3), F(MILL_T4)

# the output-slot pin: the inject report lists outputs=[<count>x <item>; ] — the 21.1
# leg namespaces the item path (the p26_w1 fork)
OUT = {"1.20.1": "outputs=[1x rail_gt_iron; ]", "1.21.1": "outputs=[1x gt6:rail_gt_iron; ]"}

steps = [
    # ---------------------------------------------------------------- arm A
    phase("A: the 8 RU packet is dead below T1 (min 16) — inject 8 red, inject 64 on the SAME machine completes"),
    Step(f"setblock {MILL_T1_P} gt6:rollformer", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine rollformer input 1 {MILL_T1_P}",
         expect="GT6 rollformer input: 1x stick_iron into slot 0",
         node_expects={"1.21.1": "GT6 rollformer input: 1x gt6:stick_iron into slot 0"}),
    # the wall: 8 < mInputMin 16 — no recipe ever binds
    Step(f"gt6machine rollformer inject 60 8 {MILL_T1_P}", expect="progress=0/0"),
    Step(f"gt6machine rollformer check {MILL_T1_P}", expect="progress=0/0"),
    # the control leg: the packet size was the red, not the rig
    Step(f"gt6machine rollformer inject 60 64 {MILL_T1_P}", expect=OUT, node_expects=OUT),

    # ---------------------------------------------------------------- arm B
    phase("B: the T2-T4 inject ladder — one item, packet = the tier max, the smoke row completes on every tier"),
]

# arm B continued: the T2/T3/T4 ladder
for tPos, tPacket, tPath in [
        (MILL_T2_P, 256, "rollformer_t2"),
        (MILL_T3_P, 1024, "rollformer_t3"),
        (MILL_T4_P, 4096, "rollformer_t4")]:
    steps += [
        Step(f"setblock {tPos} gt6:{tPath}", expect="Changed the block", sleep=1.0),
        Step(f"gt6machine {tPath} input 1 {tPos}",
             expect="GT6 rollformer input: 1x stick_iron into slot 0",
             node_expects={"1.21.1": "GT6 rollformer input: 1x gt6:stick_iron into slot 0"}),
        Step(f"gt6machine {tPath} inject 60 {tPacket} {tPos}", expect=OUT, node_expects=OUT),
    ]

steps += [
    # ---------------------------------------------------------------- arm C
    phase("C: the TIER_INPUTS window pins across the four tiers"),
    Step(f"gt6machine rollformer check {MILL_T1_P}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine rollformer_t2 check {MILL_T2_P}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine rollformer_t3 check {MILL_T3_P}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine rollformer_t4 check {MILL_T4_P}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 408 62 179 418 68 182 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w1-p29_w1_rollformer",
    slug="p29w1rollformer",
    sites=gt6world.declare_sites(MILL_T1, MILL_T2, MILL_T3, MILL_T4),
    preferred_ports=(26173, 26183),      # this card's pinned rcon/query pair (the fresh 2617x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
