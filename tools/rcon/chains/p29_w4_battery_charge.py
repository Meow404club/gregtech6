#!/usr/bin/env python3
"""p29-w4-battery-storage (charge leg) — the BatteryBox CHARGE chain (task
p29-w4-battery-storage, RCON group p29_w4_battery; the fresh z=336 band, the
chain-1 column x480..483, the chain-2 discharge column sits at x490..494):

  A CHARGE ARM (x480): battery_box_ulv with an EMPTY Lead-Acid ULV battery in
    slot 0 + a /gt6energy source (volt 16, amp 40 — inside the V[0]*2 input
    band) pumping 640 EU/t into the box's all-but-front faces. The poll pins
    the doInject cap: the buffer rides to EXACTLY 10240 EU = mInput*320*slots
    (the Base10 :185/:216 headroom term). Then the source stops and a
    /data merge resets the buffer into the TOP exchange band (gt.energy 8960L
    -> bind3(8960/1280) = 7): the box PUSHES the charge into the battery. The
    push runs deterministically to the band-6 floor (7680): 20 phases x 8
    packets x 8 EU = the battery lands at EXACTLY 1280 EU and the buffer at
    EXACTLY 7680 — the battery charge tag read back off inventory.Items[0]
    proves the charge arm end to end (acceptance ③ charge).

  B OVERVOLTAGE REJECT (x482): a volt-32 source (2x the V[0] input band,
    maxIn 16) onto a second battery_box_ulv: every oversized packet is
    consumed-but-NOT-stored (the Base10 :181-:183 + the overload ladder) —
    the buffer reads gt.energy: 0 after the window while the BE stays alive
    and answering (acceptance ③ 超压拒充, the live arm; the ladder count is
    pinned offline in GT6BatteryBoxBlockEntityTest).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_battery_charge.py
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

BOX1 = gt6world.Site(480, 65, 336)
SRC1 = gt6world.Site(481, 65, 336)
BOX2 = gt6world.Site(482, 65, 336)
SRC2 = gt6world.Site(483, 65, 336)

BOX1_P, SRC1_P, BOX2_P, SRC2_P = F(BOX1), F(SRC1), F(BOX2), F(SRC2)

BOX = "gt6:battery_box_ulv"

# the charged-battery NBT fork: 1.20.1 rides the stack tag, 21.1 the opaque
# minecraft:custom_data envelope with the SAME gt.energy key (the
# GT6DataComponents payload convention — the p28 feed-merge fork, one lane deeper)
EMPTY_BATTERY = {
    "1.20.1": f'data merge block {BOX1_P} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:battery_lead_acid_ulv",Count:1b}}]}}}}',
    "1.21.1": f'data merge block {BOX1_P} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:battery_lead_acid_ulv",count:1}}]}}}}',
}

steps = [
    # ---------------------------------------------------------------- arm A
    phase("A: the charge arm — source fills the buffer to the cap, the top-band exchange pushes 1280 EU into the battery"),
    Step(f"setblock {BOX1_P} {BOX}", expect="Changed the block", sleep=1.0),
    Step(EMPTY_BATTERY["1.20.1"], expect="Modified block data", node_cmds=EMPTY_BATTERY),
    Step(f"gt6energy place {SRC1_P}", expect="GT6 energy source placed"),
    Step(f"gt6energy volt {SRC1_P} 16", expect="voltage 16"),
    Step(f"gt6energy amp {SRC1_P} 40", expect="amperage 40"),
    Step(f"gt6energy mode {SRC1_P} on", expect="emitting true", sleep=1.0),
    # the buffer caps at mInput*320*slots = 16*320*4 = 10240 (the doInject :185 throttle);
    # volt 16 stays inside the maxIn 16 band, amp 40 = 640 EU/t fills it in ~16 s
    Step(f"data get block {BOX1_P} gt.energy", expect="10240", poll=60.0),
    Step(f"gt6energy mode {SRC1_P} off", expect="emitting false", sleep=1.0),
    # the deterministic push: reset the buffer into the top band, source OFF —
    # 20 phases x 8 packets x 8 EU (the :159 per-call cap) land in the battery
    Step(f"data merge block {BOX1_P} {{gt.energy: 8960L}}", expect="Modified block data", sleep=24.0),
    Step(f"data get block {BOX1_P} gt.energy", expect="7680"),
    Step(f'data get block {BOX1_P} inventory.Items[0].tag.gt.energy 1', expect="1280"),

    # ---------------------------------------------------------------- arm B
    phase("B: the overvoltage reject — 32 EU packets against the ULV maxIn 16 are consumed but never stored"),
    Step(f"setblock {BOX2_P} {BOX}", expect="Changed the block", sleep=1.0),
    Step(f"gt6energy place {SRC2_P}", expect="GT6 energy source placed"),
    Step(f"gt6energy volt {SRC2_P} 32", expect="voltage 32"),
    Step(f"gt6energy amp {SRC2_P} 1", expect="amperage 1"),
    Step(f"gt6energy mode {SRC2_P} on", expect="emitting true", sleep=2.5),
    # the buffer reads ZERO after the window: consumed-but-not-stored (:183), and
    # the ~50 ticks stay inside the 100-strike soft ladder so the box survives
    Step(f"data get block {BOX2_P} gt.energy", expect="gt.energy: 0"),
    Step(f"gt6energy mode {SRC2_P} off", expect="emitting false"),
]

CHAIN = Chain(__file__, steps)

if __name__ == "__main__":
    main(CHAIN)
