#!/usr/bin/env python3
"""p29_w1_rollingmill — the RollingMill RU ladder live acceptance chain (task
p29-w1-kinetic-roll-ladder, the p26_w1_wiremill shape + the p28 arm-A real-source rig).

The RU RollingMill ladder (20111-20114) shares the ROLLING_MILL map with the p28 ULV
electric rung (20115) — arm D proves the same-map co-existence live: the RU row and the
ULV row each run on their own window (TIER_INPUTS vs the {4,8,16} ULV one).

  A REAL RU SOURCE (x381..383): diesel_engine_bronze[facing=east] burns 16 RU/t DC
    through one wood-small axle DIRECTLY onto rollingmill_t1 (data-merged mFacing=5
    east, so the SBIT_B energy face = back = west meets the axle). The machine report
    pins the T1 TIER_INPUTS window minIn=16 recIn=32 maxIn=64 verbatim; the poured
    smoke row (data/gt6/recipe_maps/rollingmill.json, ingot->plate, eUt 16 / duration
    32) completes on real grid packets — the diesel is the RU source the card names
    (MGT6GA_DieselEngine EMITS TD.Energy.RU).
  B THE 8 RU PACKET IS DEAD BELOW T1 (x385): the same machine shape takes one ingot,
    an inject of 60 x 8 leaves progress=0/0 (the 8 RU packet is below the ladder min
    16 — the ULV-wall semantics on the RU side), and the CONTROL leg on the SAME
    machine (inject 60 x 64) completes the row: the red is the packet size, not the rig.
  C THE T1-T4 INJECT LADDER (x387..391): one ingot in, packet = the tier max
    (64/256/1024/4096), the smoke row completes on every tier — the outputs pin 1x
    plate_iron.
  D TIER_INPUTS WINDOW PINS + the same-map co-existence: the check report pins
    {16,32,64}/{64,128,256}/{256,512,1024}/{1024,2048,4096} across the four tiers, and
    the p28 ULV rung (rollingmill, 20115) runs its own copper wireFine row on the SAME
    map — the two domains share the map, not the window.

The item-name expects are PER-LEG via node_expects (the p26_w1 fork): bare registry
path on 1.20.1, NAMESPACED on 21.1.

passes=2 is the idempotency proof. D teardown: the explicit fill-air over the band. No
global state is touched (no mutates — one session boot serves the whole roll cluster).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_rollingmill.py
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

# --- arm A: diesel -> axle -> rollingmill_t1, the real RU source rig (x381..383)
ENGINE = gt6world.Site(381, 64, 180)
AXLE = gt6world.Site(382, 64, 180)
MILL_A = gt6world.Site(383, 64, 180)
# --- arm B: the 8 RU wall on a second T1 (x385)
MILL_T1 = gt6world.Site(385, 64, 180)
# --- arm C: the T2-T4 inject ladder (x387..391)
MILL_T2 = gt6world.Site(387, 64, 180)
MILL_T3 = gt6world.Site(389, 64, 180)
MILL_T4 = gt6world.Site(391, 64, 180)
# --- arm D: the p28 ULV rung on the SAME map (x393)
MILL_ULV = gt6world.Site(393, 64, 180)

ENGINE_P, AXLE_P = F(ENGINE), F(AXLE)
MILL_A_P, MILL_T1_P = F(MILL_A), F(MILL_T1)
MILL_T2_P, MILL_T3_P, MILL_T4_P = F(MILL_T2), F(MILL_T3), F(MILL_T4)
MILL_ULV_P = F(MILL_ULV)

DIESEL = "gt6:diesel_engine_bronze"
AXLE_B = "gt6:axle_wood_treated_small"

# the output-slot pins: the inject report lists outputs=[<count>x <item>; ], the check
# report out[0]=<count>x <item> — the 21.1 leg namespaces the item path (the p26_w1 fork)
PLATE_OUT = {"1.20.1": "outputs=[1x plate_iron; ]", "1.21.1": "outputs=[1x gt6:plate_iron; ]"}
PLATE_CHECK = {"1.20.1": "out[0]=1x plate_iron", "1.21.1": "out[0]=1x gt6:plate_iron"}
ULV_OUT = {"1.20.1": "outputs=[4x wire_fine_copper; ]", "1.21.1": "outputs=[4x gt6:wire_fine_copper; ]"}

# the p28 feed_merge fork: 1.20.1 NBT Count:1b vs the 21.1 component-era count:1
def feed_merge(pos, item_id):
    return {
        "1.20.1": f"data merge block {pos} {{inventory:{{Size:2,Items:[{{Slot:0b,id:\"{item_id}\",Count:1b}}]}}}}",
        "1.21.1": f"data merge block {pos} {{inventory:{{Size:2,Items:[{{Slot:0b,id:\"{item_id}\",count:1}}]}}}}",
    }

ULV_MERGE = feed_merge(MILL_ULV_P, "gt6:stick_copper")

steps = [
    # ---------------------------------------------------------------- arm A
    phase("A: the real RU source — diesel 16 RU/t -> axle -> rollingmill_t1, the T1 window + smoke row on grid packets"),
    Step(f"setblock {ENGINE_P} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {AXLE_P} {AXLE_B}[axis=x]", expect="Changed the block"),
    Step(f"setblock {MILL_A_P} gt6:rollingmill_t1", expect="Changed the block", sleep=1.0),
    # the setblock facing is ignored by the BE (mFacing only moves via placement/NBT —
    # the p28 calibration finding): merge mFacing=5 (east) so the SBIT_B back face
    # (west) meets the axle
    Step(f"data merge block {MILL_A_P} {{facing:5}}", expect="Modified block data"),
    Step(f"gt6machine rollingmill_t1 check {MILL_A_P}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine rollingmill_t1 input 1 {MILL_A_P}",
         expect="GT6 rollingmill_t1 input: 1x ingot_iron into slot 0",
         node_expects={"1.21.1": "GT6 rollingmill_t1 input: 1x gt6:ingot_iron into slot 0"}),
    Step(f"gt6engine fuel {ENGINE_P} gt6:diesel 2000", expect="filled 160 L"),
    # the grid-fed completion: the 16 RU/t train meets the row's 16/t drain 1:1, the
    # poured smoke row (ingot->plate, eUt 16 / duration 32) completes un-overclocked
    Step(f"gt6machine rollingmill_t1 check {MILL_A_P}", expect=PLATE_CHECK, node_expects=PLATE_CHECK, poll=45.0),
    Step(f"gt6engine stat {ENGINE_P}", expect="rate=16 RU/t (DC constant-sign)"),

    # ---------------------------------------------------------------- arm B
    phase("B: the 8 RU packet is dead below T1 (min 16) — inject 8 red, inject 64 on the SAME machine completes"),
    Step(f"setblock {MILL_T1_P} gt6:rollingmill_t1", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine rollingmill_t1 input 1 {MILL_T1_P}",
         expect="GT6 rollingmill_t1 input: 1x ingot_iron into slot 0",
         node_expects={"1.21.1": "GT6 rollingmill_t1 input: 1x gt6:ingot_iron into slot 0"}),
    # the wall: 8 < mInputMin 16 — no recipe ever binds
    Step(f"gt6machine rollingmill_t1 inject 60 8 {MILL_T1_P}", expect="progress=0/0"),
    Step(f"gt6machine rollingmill_t1 check {MILL_T1_P}", expect="progress=0/0"),
    # the control leg: the packet size was the red, not the rig
    Step(f"gt6machine rollingmill_t1 inject 60 64 {MILL_T1_P}", expect=PLATE_OUT, node_expects=PLATE_OUT),

    # ---------------------------------------------------------------- arm C
    phase("C: the T2-T4 inject ladder — one ingot, packet = the tier max, the smoke row completes on every tier"),
]

# arm C continued: the T2/T3/T4 ladder, one ingot + the tier-max packet each
for tPos, tPacket, tPath in [
        (MILL_T2_P, 256, "rollingmill_t2"),
        (MILL_T3_P, 1024, "rollingmill_t3"),
        (MILL_T4_P, 4096, "rollingmill_t4")]:
    steps += [
        Step(f"setblock {tPos} gt6:{tPath}", expect="Changed the block", sleep=1.0),
        Step(f"gt6machine {tPath} input 1 {tPos}",
             expect=f"GT6 {tPath} input: 1x ingot_iron into slot 0",
             node_expects={"1.21.1": f"GT6 {tPath} input: 1x gt6:ingot_iron into slot 0"}),
        Step(f"gt6machine {tPath} inject 60 {tPacket} {tPos}", expect=PLATE_OUT, node_expects=PLATE_OUT),
    ]

steps += [
    # ---------------------------------------------------------------- arm D
    phase("D: the TIER_INPUTS window pins + the same-map co-existence — the p28 ULV rung runs its own row on the SAME map"),
    Step(f"gt6machine rollingmill_t2 check {MILL_T2_P}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine rollingmill_t3 check {MILL_T3_P}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine rollingmill_t4 check {MILL_T4_P}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(f"setblock {MILL_ULV_P} gt6:rollingmill", expect="Changed the block", sleep=1.0),
    Step(ULV_MERGE["1.20.1"], expect="Modified block data", node_cmds=ULV_MERGE),
    # the ULV window pin — the SAME family BET, the {4,8,16} window (the p28 ruling)
    Step(f"gt6machine rollingmill check {MILL_ULV_P}", expect="minIn=4 recIn=8 maxIn=16"),
    Step(f"gt6machine rollingmill inject 300 16 {MILL_ULV_P}", expect=ULV_OUT, node_expects=ULV_OUT),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 380 62 179 394 68 182 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w1-rollingmill",
    slug="p29w1rollingmill",
    sites=gt6world.declare_sites(ENGINE, AXLE, MILL_A, MILL_T1, MILL_T2, MILL_T3, MILL_T4, MILL_ULV),
    preferred_ports=(26171, 26181),      # this card's pinned rcon/query pair (the fresh 2617x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
