#!/usr/bin/env python3
"""bees_a — the bumblebee item-domain chain (task bees-lv3-a-items RCON:
the 8-face princess/drone/queen/dead family live-registered + the gt.bumble gene
carrier face).

  A THE REGISTRATION CENSUS (the qu_usb chest form): one chest takes the 8
    bee faces (bumble_drone..bumble_dead_scanned), slot probes read the first
    and last rows back — a real registered item stack answers `item replace` +
    `data get`, the FML-registration live proof.

  B THE GENE CARRIER (the acceptance-NBT face): the card-C hive-loot write shape
    merged onto a live princess — the gt.bumble 13-key compound (the plains-rolled
    shape, IItemBumbleBee.java:134-151) + the port's species-code int
    gt.bumble.meta = 310 (Hellish, family 3 tier 1) — then read back through the
    vanilla data face, and the code-overwrite arm proves the write-repeat face.
    The 1.20.1 leg merges into Items[0].tag (the freeform stack tag); the 21.1
    leg replaces the whole entry through the CUSTOM_DATA component envelope (the
    qu_usb fork).

passes=1 (stateless placement). teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/bees_a.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=360 band, single chest column at x384 — x/z-disjoint from the
# qu_usb band z=348 and the gui_distill band z=356 (x-disjoint columns).
CHEST = "384 65 360"
C = CHEST

FACES = [
    "gt6:bumble_drone",
    "gt6:bumble_princess",
    "gt6:bumble_queen",
    "gt6:bumble_dead",
    "gt6:bumble_drone_scanned",
    "gt6:bumble_princess_scanned",
    "gt6:bumble_queen_scanned",
    "gt6:bumble_dead_scanned",
]

# the gene payload: the :134-151 plains-rolled shape over the Hellish family
# (code 310 = family 3 tier 1 — the species-code int the port carries beside the
# 13 upstream keys)
GENES = ("{minhum:0.3f,maxhum:0.7f,mintemp:266L,maxtemp:316L,offspring:2L,"
         "aggro:500L,work:6000L,life:24000L,rain:0b,storm:0b,day:1b,night:0b,"
         "outside:1b,inside:0b}")


def write_genes(code):
    """The carrier write, forked per leg — `data merge` takes NO path argument, so
    the nested face rides `data modify ... merge value` (1.20.1: into the freeform
    Items[0].tag; 21.1: the whole-entry set value through the CUSTOM_DATA envelope,
    same keys — the GT6BatteryItem/qu_usb fork)."""
    return {
        "1.20.1": "data modify block %s Items[0].tag merge value {gt.bumble:%s,gt.bumble.meta:%d}" % (C, GENES, code),
        "1.21.1": 'data modify block %s Items[0] set value {count:1,id:"gt6:bumble_princess",components:{"minecraft:custom_data":{gt.bumble:%s,gt.bumble.meta:%d}}}' % (C, GENES, code),
    }


steps = [
    phase("A: the registration census — the 8 bee faces land as real stacks"),
    Step("fill 382 63 358 398 67 362 air", expect="filled"),  # the band clear (idempotent)
    Step("setblock %s minecraft:chest" % C, expect="Changed the block"),
]
for slot, item_id in enumerate(FACES):
    steps.append(Step("item replace block %s container.%d with %s 1" % (C, slot, item_id), expect="Replaced"))
steps += [
    Step("data get block %s Items[0]" % C, expect="gt6:bumble_drone"),
    Step("data get block %s Items[2]" % C, expect="gt6:bumble_queen"),
    Step("data get block %s Items[7]" % C, expect="gt6:bumble_dead_scanned"),

    phase("B: the gene carrier — the gt.bumble compound + the species code over a live princess"),
    Step("item replace block %s container.0 with gt6:bumble_princess 1" % C, expect="Replaced"),
    Step(write_genes(310)["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": write_genes(310)["1.21.1"]}),
    # the read-back: 1.20.1 the freeform tag face; 21.1 the entry prints the component
    Step("data get block %s Items[0].tag" % C, expect="gt.bumble.meta: 310",
         poll=30, node_cmds={"1.21.1": "data get block %s Items[0]" % C},
         node_expects={"1.21.1": "gt.bumble.meta: 310"}),
    Step("data get block %s Items[0].tag" % C, expect="work: 6000",
         poll=30, node_cmds={"1.21.1": "data get block %s Items[0]" % C},
         node_expects={"1.21.1": "work: 6000L"}),
    # the code-overwrite arm: the HiveKind.species 700 (Frosty) lands over the 310
    Step(write_genes(700)["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": write_genes(700)["1.21.1"]}),
    Step("data get block %s Items[0].tag" % C, expect="gt.bumble.meta: 700",
         poll=30, node_cmds={"1.21.1": "data get block %s Items[0]" % C},
         node_expects={"1.21.1": "gt.bumble.meta: 700"}),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 382 63 358 398 67 362 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="bees-lv3-a-items bees_a",
    slug="beesa",
    sites=gt6world.declare_sites(gt6world.Site(384, 65, 360)),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
