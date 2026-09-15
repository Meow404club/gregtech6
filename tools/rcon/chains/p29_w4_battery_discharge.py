#!/usr/bin/env python3
"""p29-w4-battery-storage (discharge leg) — the BatteryBox DISCHARGE / SUPPLY
chain (task p29-w4-battery-storage, RCON group p29_w4_battery; the same fresh
z=336 band as the charge leg, the chain-2 column x490..494):

  A SUPPLY ARM (x490): battery_box_ulv[facing=east] carrying FOUR charged
    Lead-Acid ULV batteries (the front = the OUTPUT face, Base10 :232-:233)
    sits FRONT-TO-BACK on a wiremill_ulv (the p28 arm-A consumer). The emit
    pass (the :141-:151) drives min(mMode?0:mBatteryCount, 4) = 4 packets of
    V[0]=8 EU per tick = 32 EU/t >= the RM.Wiremill row's eUt 16 — the mill
    completes the copper-stick row (outputs=[4x wire_fine_copper]) off pure
    battery power: the DISCHARGE arm powers a downstream EU machine live.
    The battery stock (4 x 6400 EU) covers the row many times over while the
    20-tick pull returns up to min(mSizeRec,40)=8 packets per battery.

  B THE V[i] GATE ON THE OUTPUT (x493): the same rig over an OVEN (the p8
    idiom's consumer, minIn 16): every V[0]=8 packet dies below the oven's
    input minimum — the EnergyGate small-packet arm SWALLOWS the offered
    energy (the Root :717 arm), so the oven reports energy=0 through the
    emission window: an ULV box cannot feed an LV machine, the tier wall
    holds on the discharge side too.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_battery_discharge.py
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

BOXA = gt6world.Site(490, 65, 336)
MILL = gt6world.Site(491, 65, 336)
BOXB = gt6world.Site(493, 65, 336)
OVEN = gt6world.Site(494, 65, 336)

BOXA_P, MILL_P, BOXB_P, OVEN_P = F(BOXA), F(MILL), F(BOXB), F(OVEN)

BOX_E = "gt6:battery_box_ulv[facing=east]"  # the FRONT (output) points at the x+1 consumer


def charged_batteries(pos):
    """Four charged ULV batteries (gt.energy 6400 = 800 packets of 8).

    The carrier fork: 1.20.1 rides the stack tag, 21.1 the opaque
    minecraft:custom_data envelope with the SAME gt.energy key (the
    GT6DataComponents payload convention — the p28 feed-merge fork, one lane deeper).
    """
    return {
        "1.20.1": (
            f"data merge block {pos} {{inventory:{{Size:4,Items:["
            + ",".join(f'{{Slot:{i}b,id:"gt6:battery_lead_acid_ulv",Count:1b,tag:{{gt.energy:6400L}}}}' for i in range(4))
            + "]}}"  # ] + }} (inventory) + }} (root) — the rendered form
        ),
        "1.21.1": (
            f"data merge block {pos} {{inventory:{{Size:4,Items:["
            + ",".join(
                f'{{Slot:{i}b,id:"gt6:battery_lead_acid_ulv",count:1,components:{{"minecraft:custom_data":{{gt.energy:6400L}}}}}}'
                for i in range(4)
            )
            + "]}}"  # ] + }} (inventory) + }} (root) — the rendered form
        ),
    }


CHARGE_A = charged_batteries(BOXA_P)
CHARGE_B = charged_batteries(BOXB_P)

# the mill feed renders fork on the item-path namespace (the p26_w1 fork pattern)
MILL_FEED = {
    "1.20.1": f'data merge block {MILL_P} {{inventory:{{Size:2,Items:[{{Slot:0b,id:"gt6:stick_copper",Count:1b}}]}}}}',
    "1.21.1": f'data merge block {MILL_P} {{inventory:{{Size:2,Items:[{{Slot:0b,id:"gt6:stick_copper",count:1}}]}}}}',
}
MILL_OUT = {"1.20.1": "outputs=[4x wire_fine_copper; ]", "1.21.1": "outputs=[4x gt6:wire_fine_copper; ]"}

steps = [
    # ---------------------------------------------------------------- arm A
    phase("A: the discharge supply — four charged batteries drive a wiremill_ulv row off pure battery power"),
    Step(f"setblock {BOXA_P} {BOX_E}", expect="Changed the block", sleep=1.0),
    Step(CHARGE_A["1.20.1"], expect="Modified block data", node_cmds=CHARGE_A),
    Step(f"setblock {MILL_P} gt6:wiremill_ulv", expect="Changed the block", sleep=1.0),
    # the mill's SBIT_B energy face = back (west) after the facing merge — the box
    # front (east) meets it directly (the p28 arm-A calibration finding)
    Step(f"data merge block {MILL_P} {{facing:5}}", expect="Modified block data"),
    Step(MILL_FEED["1.20.1"], expect="Modified block data", node_cmds=MILL_FEED),
    # the battery-driven completion: 4 packets/tick x 8 EU = 32 EU/t over the row's eUt 16
    Step(f"gt6machine wiremill check {MILL_P}", expect=MILL_OUT["1.20.1"], node_expects=MILL_OUT, poll=60.0),

    # ---------------------------------------------------------------- arm B
    phase("B: the V[i] output gate — 8 EU packets into the LV oven (minIn 16) are swallowed white"),
    Step(f"setblock {BOXB_P} {BOX_E}", expect="Changed the block", sleep=1.0),
    Step(CHARGE_B["1.20.1"], expect="Modified block data", node_cmds=CHARGE_B),
    Step(f"gt6oven place {OVEN_P}", expect="GT6 oven placed", sleep=1.0),
    Step(f"gt6oven input 8 {OVEN_P}", expect="8 cobblestone"),
    # through the emission window the oven still reports energy=0 — every 8 EU
    # packet dies below minIn 16 (the white burn; the box keeps paying, the oven
    # never sees charge, exactly the p28 arm-B swallow shape on the battery leg)
    Step(f"gt6oven check {OVEN_P}", expect="energy=0", poll=8.0),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p29_w4_battery matches this member
    name="p29-w4-battery-discharge p29_w4_battery",
    slug="p29w4batterydischarge",
    sites=gt6world.declare_sites(BOXA, MILL, BOXB, OVEN),
    preferred_ports=(26363, 26373),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
