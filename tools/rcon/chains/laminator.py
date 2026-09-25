#!/usr/bin/env python3
"""eu-special/laminator — the Laminator chain (task eu-special
ACCEPTANCE ③+④: the HU window TIER_INPUTS row-by-row + the burning-box supply, the
fermenter chain form).

The machine is the HU 4-tier Heat_T ladder (Loader:1532-1535, NBT_ENERGY_ACCEPTED HU,
energy SBIT_D — the burning-box bottom-feed form), RM.LAMINATOR (the card-①
DECLARED-empty base map), on the committed laminator.json smoke row (piston +
slime_ball -> sticky_piston, eUt 16, duration 16 — the OD.itemSlime listener row
:190 verbatim in port ids). The upstream NBT_GUI machines/Laminator.png path is the
DECLARED fidelity record on the section doc (the null-menu convention; no GUI arm
live here). No /gt6machine command arm (the FILES_SCOPE form, see the autocrafter
chain): setblock + data merge + the shared-literal pass-through drive.

  A-D the WINDOW LADDER row-by-row (one site per tier): the check line pins each
    tier's :126 window (T1 {16,32,64} / T2 {64,128,256} / T3 {256,512,1024} /
    T4 {1024,2048,4096}); the T1 arm adds the <min rejection (8-size packets dead,
    progress=0/0) and the mid-window train (4x32 = 128/256); T2 the mid-window
    single (64/256); T3/T4 the completion arms (the rec-size packet binds and the
    256-budget closes inside the inject loop, outputs=[1x sticky_piston]).
  E the >max overcharge arm on the throwaway T1 (an 4096 packet vs the 64 ceiling:
    used=1, the machine explodes — the STAT FAILED absence proof, the fermenter
    arm-D form).
  F the burning-box supply (the P13 boiler firebox adjacency form, the HU source
    the fermenter chain rode): the brick box UNDER a fresh T1 (its top face emits
    into the machine's SBIT_D bottom acceptor), fueled + ignited, is the ONLY
    energy — the 16 HU/t train covers the whole 256 budget (~16 s real; poll 120).
  G teardown (extinguish + the band restore).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/laminator.py
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

# the fresh z=276 band: five x-disjoint columns (the T1 fresh machine of the box arm
# shares column x428: box at y64, machine at y65 — the firebox adjacency)
SITE_T1 = gt6world.Site(408, 65, 276, dy=1)
SITE_T2 = gt6world.Site(412, 65, 276, dy=1)
SITE_T3 = gt6world.Site(416, 65, 276, dy=1)
SITE_T4 = gt6world.Site(420, 65, 276, dy=1)
SITE_THROW = gt6world.Site(424, 65, 276, dy=1)
SITE_BOX = gt6world.Site(428, 64, 276)
SITE_FRESH = gt6world.Site(428, 65, 276, dy=1)
T1, T2, T3, T4, THROW, FRESH = F(SITE_T1), F(SITE_T2), F(SITE_T3), F(SITE_T4), F(SITE_THROW), F(SITE_FRESH)
BOX = F(SITE_BOX)

STOCK = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:piston",Count:1b}},{{Slot:1b,id:"minecraft:slime_ball",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:piston",count:1}},{{Slot:1b,id:"minecraft:slime_ball",count:1}}]}}}}',
}

OUT = {"1.21.1": "out[0]=1x minecraft:sticky_piston"}

steps = [
    phase("A: the T1 window — check pin, <min 拒, mid-window train"),
    Step(f"setblock {T1} gt6:laminator", expect="Changed the block"),
    Step(f"gt6machine shredder fluid stat {T1}", expect="masks fluidIn=127 fluidOut=127 energyIn=65"),
    Step(f"gt6machine shredder check {T1}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(STOCK["1.20.1"].format(p=T1), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=T1)}),
    Step(f"gt6machine shredder inject 40 8 {T1}", expect="progress=0/0"),
    Step(f"gt6machine shredder check {T1}", expect="progress=0/0"),
    Step(f"gt6machine shredder inject 4 32 {T1}", expect="progress=128/256"),

    phase("B: the T2 window"),
    Step(f"setblock {T2} gt6:laminator_t2", expect="Changed the block"),
    Step(f"gt6machine shredder check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(STOCK["1.20.1"].format(p=T2), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=T2)}),
    Step(f"gt6machine shredder inject 1 64 {T2}", expect="progress=64/"),

    phase("C: the T3 window — the rec-size packet binds and closes the budget"),
    Step(f"setblock {T3} gt6:laminator_t3", expect="Changed the block"),
    Step(f"gt6machine shredder check {T3}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(STOCK["1.20.1"].format(p=T3), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=T3)}),
    Step(f"gt6machine shredder inject 2 256 {T3}", expect="progress=512/"),
    # the second train closes the 1024 budget → the completion lands in the same loop
    Step(f"gt6machine shredder inject 2 256 {T3}", expect="outputs=[1x sticky_piston",
         node_expects={"1.21.1": "outputs=[1x minecraft:sticky_piston"}),

    phase("D: the T4 window"),
    Step(f"setblock {T4} gt6:laminator_t4", expect="Changed the block"),
    Step(f"gt6machine shredder check {T4}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(STOCK["1.20.1"].format(p=T4), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=T4)}),
    Step(f"gt6machine shredder inject 1 2048 {T4}", expect="outputs=[1x sticky_piston",
         node_expects={"1.21.1": "outputs=[1x minecraft:sticky_piston"}),

    phase("E: the >max overcharge arm on the throwaway T1 (the :493 ceiling 64)"),
    Step(f"setblock {THROW} gt6:laminator", expect="Changed the block"),
    # used=1: the :495 overcharge return convention — the packet counts consumed, the
    # machine explodes; the honest face is the STAT FAILED absence below
    Step(f"gt6machine shredder inject 1 4096 {THROW}", expect="used=1 progress=0/0"),
    Step(f"gt6machine shredder check {THROW}", expect="STAT FAILED", allow_failed=True),

    phase("F: the burning-box supply — the box is the ONLY energy for the whole budget"),
    Step(f"setblock {FRESH} gt6:laminator", expect="Changed the block"),
    Step(STOCK["1.20.1"].format(p=FRESH), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=FRESH)}),
    Step(f"gt6burner place {BOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner fuel {BOX} minecraft:coal 4", expect="minecraft:coal x4"),
    Step(f"gt6burner ignite {BOX}", expect="burning=true"),
    Step(f"gt6machine shredder check {FRESH}", expect="out[0]=1x sticky_piston",
         node_expects={"1.21.1": "out[0]=1x minecraft:sticky_piston"}, poll=120.0),
    Step(f"gt6machine shredder check {FRESH}", expect="progress=0/0"),
    Step(f"gt6burner stat {BOX}", expect="burning=true"),

    phase("G: teardown — extinguish, restore the band"),
    Step(f"gt6burner extinguish {BOX}", expect="burning=false"),
    Step("fill 407 63 275 429 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="eu_special laminator",
    slug="laminator",
    sites=gt6world.declare_sites(SITE_T1, SITE_T2, SITE_T3, SITE_T4, SITE_THROW, SITE_BOX, SITE_FRESH),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
