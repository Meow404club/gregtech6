#!/usr/bin/env python3
"""p32-usb-data — the USB Stick data-face chain (task p32-usb-data, the p32_qu_usb
sweep group; MultiItemTechnological.java:791-794). Machines are ABSENT (the W2
scanner/replicator card consumes this face), so the chain proves the data plane:

  A THE REGISTRATION CENSUS (the acceptance-① live half; the p29-w5-t8-armor-give
    chest form): one chest takes the four sticks (usb_stick_1..4), slot probes read
    the first and last rows back — a real registered item stack answers `item
    replace` + `data get`, which is the FML-registration live proof (the P31 id686
    lesson: cleanTest green does not prove a registration is alive).

  B THE DATA CARRIER (the acceptance-③ face): the scanner write shape
    (RecipeMapScannerMolecular.java:58-60) merged onto a live stack —
    gt.usb.tier = 3b + gt.usb.data = {gt.replicator.data: 260s} (260 = MT.Fe's
    registry id, MT.java:1011; the p31 massfab walk's 26p/30n material) — then read
    back through the vanilla data face, and the tier-overwrite arm proves the
    write-repeat face (the USBSwitch swap shape, MultiTileEntityUSBSwitch.java:72).
    The 1.20.1 leg merges into Items[0].tag directly (the freeform stack tag); the
    21.1 leg replaces the whole entry through the opaque CUSTOM_DATA component
    envelope (the same keys, the GT6BatteryItem carrier fork).

passes=1 (stateless placement). teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p32_qu_usb.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=348 band, single chest column at x384 — x/z-disjoint from every
# roster band (the armor-give give-chest form at the band edge)
CHEST = "384 65 348"
C = CHEST

STICKS = ["gt6:usb_stick_1", "gt6:usb_stick_2", "gt6:usb_stick_3", "gt6:usb_stick_4"]

# the scanner write payload (B): the :58-60 shape over MT.Fe (id 260, MT.java:1011)
FE_ID = 260
TAG_1201 = "gt.usb.tier:3b,gt.usb.data:{gt.replicator.data:%ds}" % FE_ID
TAG_1121 = "gt.usb.tier:3b,gt.usb.data:{gt.replicator.data:%ds}" % FE_ID


def write_merge(tier_byte):
    """The carrier write, forked per leg — 1.20.1 merges into the freeform stack tag;
    21.1 replaces the whole entry through the opaque CUSTOM_DATA envelope (same keys)."""
    return {
        "1.20.1": "data merge block %s Items[0].tag {gt.usb.tier:%db,gt.usb.data:{gt.replicator.data:%ds}}" % (C, tier_byte, FE_ID),
        "1.21.1": 'data merge block %s Items[0] {count:1,id:"gt6:usb_stick_3",components:{"minecraft:custom_data":{gt.usb.tier:%db,gt.usb.data:{gt.replicator.data:%ds}}}}' % (C, tier_byte, FE_ID),
    }


steps = [
    phase("A: the registration census — the four sticks land as real stacks"),
    Step("fill 382 63 346 398 67 352 air", expect="filled"),  # the band clear (idempotent)
    Step("setblock %s minecraft:chest" % C, expect="Changed the block"),
]
for slot, item_id in enumerate(STICKS):
    steps.append(Step("item replace block %s container.%d with %s 1" % (C, slot, item_id), expect="Replaced"))
steps += [
    Step("data get block %s Items[0]" % C, expect="gt6:usb_stick_1"),
    Step("data get block %s Items[3]" % C, expect="gt6:usb_stick_4"),

    phase("B: the data carrier — the scanner write shape over MT.Fe (id 260)"),
    Step("item replace block %s container.0 with gt6:usb_stick_3 1" % C, expect="Replaced"),
    Step(write_merge(3)["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": write_merge(3)["1.21.1"]}),
    # the read-back: 1.20.1 the freeform tag face; 21.1 the entry prints the component
    Step("data get block %s Items[0].tag" % C, expect="gt.replicator.data: 260s",
         poll=30, node_cmds={"1.21.1": "data get block %s Items[0]" % C},
         node_expects={"1.21.1": "gt.replicator.data: 260s"}),
    Step("data get block %s Items[0].tag" % C, expect="gt.usb.tier: 3b",
         poll=30, node_cmds={"1.21.1": "data get block %s Items[0]" % C},
         node_expects={"1.21.1": "gt.usb.tier: 3b"}),
    # the tier-overwrite arm (the USBSwitch swap shape): tier 1 lands over tier 3
    Step(write_merge(1)["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": write_merge(1)["1.21.1"]}),
    Step("data get block %s Items[0].tag" % C, expect="gt.usb.tier: 1b",
         poll=30, node_cmds={"1.21.1": "data get block %s Items[0]" % C},
         node_expects={"1.21.1": "gt.usb.tier: 1b"}),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 382 63 346 398 67 352 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p32-usb-data p32_qu_usb",
    slug="p32quusb",
    sites=gt6world.declare_sites(gt6world.Site(384, 65, 348)),
    preferred_ports=(26562, 26572),
    game_port=26416,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
