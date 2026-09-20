#!/usr/bin/env python3
"""p32-qu-scanner-replicator — the QU machine pair chain (task p32-qu-scanner-replicator,
the p32_qu_machines sweep group; Loader_MultiTileEntities.java:1551/:1556-1558).

  A THE SCANNER ARM (acceptance ② first half): a T3 USB stick + a dust_hydrogen land in
    the gt6:molecular_scanner_t3 inventory (slots 0/1) on the fakesource drive; the
    synthesis (GT6RecipeMapScannerMolecular, RecipeMapScannerMolecular.java:57-61) eats
    both and the stick comes back in output slot 2 carrying the scan data —
    gt.replicator.data: 10s (10 = MT.H's registry id, MT.java:943, 1p/0n) +
    gt.usb.tier: 3b. maxprogress 262144 = eUt (1+0)x512 x duration 512 (the :57 face).

  B THE REPLICATOR ARM (acceptance ② second half): the SAME payload the scanner writes
    (tier-3 stick, gt.replicator.data = 10s) is merged into the gt6:replicator_t2 slot 0
    + chargedmatter 1 mB (1 mB = 1 proton, the :90 leg; the neutral leg is the NF arm —
    hydrogen has 0 neutrons) parked in input tank 0; the synthesis
    (GT6RecipeMapReplicator.getReplicatorRecipe, RecipeMapReplicator.java:88-114) runs
    and output slot 3 carries the replicated gt6:dust_hydrogen — while the stick REMAINS
    in slot 0 with its data intact (the ST.amount(0, aUSB) never-consumed face) and the
    matter tank drains to 0.

  the replicator rung rides TIER_INPUTS[1] (window 64/128/256, the :1557 NBT_INPUT 128);
  hydrogen's eUt 256 lands exactly at the window max — the :554 absGreaterEqual face
  live. The redstone six (:941-946) are display rows (the map's MIN-1-item gate keeps
  the fluid-only rows machine-unfindable, the :929 ruling) — their constants are pinned
  by GT6QuMachinesTest (acceptance ③), not by this chain.

passes=1 (stateless placement). teardown: fakesource off + the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p32_qu_machines.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=348 band EAST of the p32_qu_usb chest column (x382..398): the two rigs
# sit x456..470 — x-disjoint from every roster band on the column
SC = gt6world.Site(458, 65, 348)                 # the Molecular Scanner T3
RP = gt6world.Site(466, 65, 348)                 # the Matter Replicator T2
BAND = (454, 63, 346, 470, 68, 352)              # the fresh band
S = gt6world.fmt(SC)
R = gt6world.fmt(RP)

H_ID = 10  # MT.H's registry id (MT.java:943 — diatomicgas(10, "Hydrogen", ..., UUM))

# the scanner-written carrier payload — the SAME compound both arms speak
TAG_1201 = 'tag:{gt.usb.tier:3b,gt.usb.data:{gt.replicator.data:%ds}}' % H_ID
CD_1121 = '"minecraft:custom_data":{gt.usb.tier:3b,gt.usb.data:{gt.replicator.data:%ds}}' % H_ID

SCANNER_MERGE = {
    "1.20.1": 'data merge block %s {inventory:{Size:3,Items:[{Slot:0b,id:"gt6:usb_stick_3",Count:1b},{Slot:1b,id:"gt6:dust_hydrogen",Count:1b}]}}' % S,
    "1.21.1": 'data merge block %s {inventory:{Size:3,Items:[{Slot:0b,id:"gt6:usb_stick_3",count:1},{Slot:1b,id:"gt6:dust_hydrogen",count:1}]}}' % S,
}

# the replicator input face — the tagged stick is a FRESH list entry inside one merge on
# 1.20.1 (the freeform tag rides the item compound); 21.1 needs the proven two-step form
# (the p32_qu_usb 4c33979c4 lesson: the envelope goes through a whole-entry set value)
REPLICATOR_MERGE = {
    "1.20.1": 'data merge block %s {inventory:{Size:6,Items:[{Slot:0b,id:"gt6:usb_stick_3",Count:1b,%s}]}}' % (R, TAG_1201),
    "1.21.1": 'data merge block %s {inventory:{Size:6,Items:[{Slot:0b,id:"gt6:usb_stick_3",count:1}]}}' % R,
}
REPLICATOR_STICK_1121 = 'data modify block %s Items[0] set value {count:1,id:"gt6:usb_stick_3",components:{%s}}' % (R, CD_1121)

# the matter leg (the :90 charged arm) — the FluidName/Amount contract pair (ADR-P15-1)
TANK_MERGE = 'data merge block %s {tanks:{in:{"0":{FluidName:"gt6:chargedmatter",Amount:1}}}}' % R

steps = [
    phase("A: the scanner arm — dust_hydrogen + a blank stick go in, the data stick comes out"),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),  # the band clear (idempotent)
    Step("forceload add 454 346 470 352"),                        # the tick driver
    Step("setblock %s gt6:molecular_scanner_t3" % S, expect="Changed the block"),
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(SCANNER_MERGE["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": SCANNER_MERGE["1.21.1"]}),
    Step("data get block " + S, expect="active: 1b", poll=120),
    Step("data get block " + S, expect="maxprogress: 262144L", poll=60),  # 512 eUt x 512 t (the :57 face)
    # the scan data lands in output slot 2 — the acceptance "data write" assert
    Step("data get block %s inventory" % S, expect="gt.replicator.data: 10s", poll=60),
    Step("data get block %s inventory" % S, expect="gt.usb.tier: 3b", poll=30),

    phase("B: the replicator arm — the data stick + 1 mB charged matter replicate hydrogen"),
    Step("setblock %s gt6:replicator_t2" % R, expect="Changed the block"),
    Step(REPLICATOR_MERGE["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": REPLICATOR_MERGE["1.21.1"]}),
    Step(REPLICATOR_STICK_1121, expect="Modified block data",
         node_cmds={"1.20.1": "time query daytime"},
         node_expects={"1.20.1": "The time is"}),  # the 21.1-only envelope step (the 4c33979c4 dialect)
    Step(TANK_MERGE, expect="Modified block data"),
    Step("data get block " + R, expect="active: 1b", poll=120),
    # the replicated product in output slot 3 (the acceptance product/eUt face — the
    # eUt constant itself is the unit-pinned (1+0)x256=256, the window max live)
    Step("data get block %s inventory" % R, expect='id: "gt6:dust_hydrogen"', poll=300),
    # THE NEVER-CONSUMED FACE: the stick is still in slot 0 with its data intact
    Step("data get block %s inventory" % R, expect="gt.replicator.data: 10s", poll=30),
    Step("data get block %s \"tanks.in.0\"" % R, expect="Amount: 0", poll=120,
         node_expects={"1.21.1": "amount: 0"}),

    phase("C: teardown — the global-state restore + the explicit band restore"),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p32-qu-scanner-replicator p32_qu_machines",
    slug="p32qumachines",
    sites=gt6world.declare_sites(SC, RP),
    preferred_ports=(26612, 26622),
    game_port=26602,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
