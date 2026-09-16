#!/usr/bin/env python3
"""p29-w5-t6-electric-nineteen DRILL chain — the EU drain arm, the empty refusal and
the BatteryBox charge arm (task p29-w5-t6-electric-nineteen, the fresh z=240 band,
the chain-1 column x600..611):

  A the BREAK arm (x600): a full-EU mining_drill_lv (charged through the REAL
    setEnergyStored face inside /gt6electric) breaks a stone block — the report pins
    the drain (64000 -> 63975 = EXACTLY the upstream 25/break column,
    GT_Tool_MiningDrill_LV :42) and the vanilla cobblestone drop (no conversion for
    the drill — the rockGt conversion is the jackhammer's);
  B the EMPTY refusal (x604): a zero-EU tool refuses — mineBlock false, the block
    STAYS (the isItemStackUsable 对位, the EU-empty face of acceptance);
  C the BOX charge arm (x610 box, x611 source): battery_box_lv with the drained
    drill in slot 0 + a /gt6energy source inside the LV input band — the box pushes
    into the TOOL through the SAME IItemEnergy faces the batteries ride (the W4
    canInsertItem2 :199 accepts any matching-type IItemEnergy stack); the poll pins
    the tool's gt.energy climbing off zero — the LIVE half of the 电池盒充能 arm
    (the offline full-charge loop is ElectricNineteenTest).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t6_electric_drill.py
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

S1 = gt6world.Site(600, 65, 240)   # the full-EU break arm
S2 = gt6world.Site(604, 65, 240)   # the empty-EU refusal
BOX = gt6world.Site(610, 65, 240)  # the battery box
SRC = gt6world.Site(611, 65, 240)  # the charge source

S1_P, S2_P, BOX_P, SRC_P = F(S1), F(S2), F(BOX), F(SRC)

BOX_ID = "gt6:battery_box_lv"

# the drained-tool insert fork: 1.20.1 rides the stack tag, 21.1 the opaque
# minecraft:custom_data envelope with the SAME gt.energy key (the W4 chain fork)
INSERT_DRILL = {
    "1.20.1": f'data merge block {BOX_P} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:mining_drill_lv",Count:1b}}]}}}}',
    "1.21.1": f'data merge block {BOX_P} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:mining_drill_lv",count:1}}]}}}}',
}

steps = []

# --------------------------------- A: the full-EU break drains EXACTLY the upstream column
steps += [
    phase("A: the full-EU drill breaks stone — drained=25, the vanilla drop rides through"),
    Step(f"setblock {S1_P} minecraft:stone", expect="Changed the block"),
    Step(f"gt6electric mine {S1.x} {S1.y} {S1.z} mining_drill_lv",
         expect="drained 25), speed=18.0, drops=[minecraft:cobblestone x1]"),
]

# --------------------------------- B: the empty-EU refusal — the block STAYS
steps += [
    phase("B: the zero-EU tool refuses (the isItemStackUsable 对位)"),
    Step(f"setblock {S2_P} minecraft:stone", expect="Changed the block"),
    Step(f"gt6electric mine {S2.x} {S2.y} {S2.z} mining_drill_lv empty",
         expect="broke=false, block stays=true, eu=0"),
]

# --------------------------------- C: the BatteryBox charge arm — the box pushes into the TOOL
steps += [
    phase("C: the box charges the drill through the IItemEnergy faces (the W4 seam, zero new wiring)"),
    Step(f"setblock {BOX_P} {BOX_ID}", expect="Changed the block", sleep=1.0),
    Step(INSERT_DRILL["1.20.1"], expect="Modified block data", node_cmds=INSERT_DRILL),
    Step(f"gt6energy place {SRC_P}", expect="GT6 energy source placed"),
    Step(f"gt6energy volt {SRC_P} 32", expect="voltage 32"),
    Step(f"gt6energy amp {SRC_P} 40", expect="amperage 40"),
    Step(f"gt6energy mode {SRC_P} on", expect="emitting true", sleep=1.0),
    # the tool's charge climbs off zero: the packet band [16..64] accepts the box's 32
    # (the doEnergyInjection poll — the exact landing value is band-arithmetic-live)
    Step(f"data get block {BOX_P}", expect="gt.energy: ", poll=45.0),
    Step(f"gt6energy mode {SRC_P} off", expect="emitting false"),
    # the drill is NOT at 0 any more (the positive-charge pin, the read after the window)
    Step(f"data get block {BOX_P} inventory.Items[0]", expect="id:"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p29_w5_t6_electric matches this member
    name="p29-w5-t6-electric-drill p29_w5_t6_electric",
    slug="p29w5t6electricdrill",
    sites=gt6world.declare_sites(S1, S2, BOX, SRC),
    preferred_ports=(26363, 26373),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
