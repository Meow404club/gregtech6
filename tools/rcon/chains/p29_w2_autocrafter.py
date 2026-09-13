#!/usr/bin/env python3
"""p29-w2-eu-special/autocrafter — the Autocrafter chain (task p29-w2-eu-special
ACCEPTANCE ①+④: the WAVE'S FIRST DUAL ENERGY FACE U|D live proof + the smoke row run).

The machine is the EU 5-tier ladder's T1 row (Loader_MultiTileEntities.java:1497,
NBT_ENERGY_ACCEPTED_SIDES SBIT_U|SBIT_D — every other machine of the wave carries a
SINGLE energy face), RM.AUTOCRAFTER (the card-① DECLARED-empty base map — the
crafting-grid auto-fill arm stays POOLED), on the committed autocrafter.json smoke row
(4x oak_planks -> 1x crafting_table, eUt 16, duration 32; the vanilla-shape smoke row,
the static-row face upstream is the pooled runtime arm). No /gt6machine command arm
exists for this family (the card FILES_SCOPE excludes GTMachineCommand) — the chain
drives through SETBLOCK placement + the inventory data merge + the SHARED BET faces
of the pre-existing literals (the p28_fe_inbound wiremill_ulv form; inject/check/fluid
operate on whatever TileEntityBasicMachine sits at the pos).

  A the smoke row run (T1, inject arm): place -> stock 4 planks (slot 0) ->
    8-size packets are DEAD below the window floor mInputMin 16 (progress=0/0,
    the fermenter arm form) -> 64-size packets run the row to completion
    (outputs=[1x crafting_table]; budget 16x32=512 = 8 ticks of mInputMax).
    The mask line rides the fluid stat: energyIn=67 = SBIT_U|SBIT_D|SBIT_A.
  B the TOP-face supply (the LIVE dual-face half): a fresh machine, facing north
    (data merge facing:2), a gt6energy rig directly ABOVE it (its down face feeds
    the machine's SBIT_U top face — the card-① rig adjacency form, EU dialed) is
    the ONLY energy source; the poll check delivers the full completion
    out[0]=1x crafting_table — supply through U 贯通.
  C the FRONT-face rejection: a fresh machine facing north, the rig to its NORTH
    (the relative FRONT face, SBIT_F — NOT in the mask): check stays energy=0 /
    active=false forever — the other-faces-reject arm (the offline truth table's
    live half; SBIT_F/SBIT_L/SBIT_R/SBIT_B all refuse, one face proves the gate).
  D teardown (the explicit fill + the extinguish-free band restore).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_autocrafter.py
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

# the fresh z=276 band: three x-disjoint columns (the W2 card-① band form)
SITE_A = gt6world.Site(384, 65, 276, dy=1)          # the inject arm
SITE_RIG_TOP = gt6world.Site(388, 66, 276, dy=1)    # the top rig (B)
SITE_B = gt6world.Site(388, 65, 276)                # the machine under it
SITE_RIG_FRONT = gt6world.Site(392, 65, 275, dy=1)  # the front rig (C) — level with the machine, directly north
SITE_C = gt6world.Site(392, 65, 276)                # the machine north of the rig
A, B, C = F(SITE_A), F(SITE_B), F(SITE_C)
RIG_TOP, RIG_FRONT = F(SITE_RIG_TOP), F(SITE_RIG_FRONT)

# the inventory payload per node (the p19_nbt_rebind key-shape ruling): 4 planks in slot 0
STOCK = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:21,Items:[{{Slot:0b,id:"minecraft:oak_planks",Count:4b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:21,Items:[{{Slot:0b,id:"minecraft:oak_planks",count:4}}]}}}}',
}
FACING_NORTH = 'data merge block {p} {{facing:2}}'

steps = [
    phase("A: the smoke row run — <min 拒 then the 64-train to completion"),
    Step(f"setblock {A} gt6:autocrafter", expect="Changed the block"),
    Step(STOCK["1.20.1"].format(p=A), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=A)}),
    Step(f"gt6machine shredder fluid stat {A}", expect="masks fluidIn=127 fluidOut=127 energyIn=67",
         node_expects={"1.21.1": "masks fluidIn=127 fluidOut=127 energyIn=67"}),
    Step(f"gt6machine shredder check {A}", expect="minIn=16 recIn=32 maxIn=64"),
    # <16 拒: 8-size packets are dead below the window floor
    Step(f"gt6machine shredder inject 40 8 {A}", expect="progress=0/0"),
    Step(f"gt6machine shredder check {A}", expect="progress=0/0"),
    # 64-train: budget 512 = 8 ticks of mInputMax, 10 iterations cover it
    Step(f"gt6machine shredder inject 10 64 {A}", expect="outputs=[1x crafting_table",
         node_expects={"1.21.1": "outputs=[1x minecraft:crafting_table"}),

    phase("B: the TOP-face supply — the rig above is the ONLY energy, the poll carries the completion"),
    Step(f"setblock {B} gt6:autocrafter", expect="Changed the block"),
    Step(FACING_NORTH.format(p=B), expect="Modified block data"),
    Step(STOCK["1.20.1"].format(p=B), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=B)}),
    Step(f"gt6energy place {RIG_TOP}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {RIG_TOP} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {RIG_TOP} 32", expect="voltage 32 EU"),
    Step(f"gt6energy mode {RIG_TOP} on", expect="emitting true"),
    Step(f"gt6machine shredder check {B}", expect="out[0]=1x crafting_table",
         node_expects={"1.21.1": "out[0]=1x minecraft:crafting_table"}, poll=20.0),
    Step(f"gt6energy mode {RIG_TOP} off", expect="emitting false"),

    phase("C: the FRONT-face rejection — the rig to the north never moves the machine"),
    Step(f"setblock {C} gt6:autocrafter", expect="Changed the block"),
    Step(FACING_NORTH.format(p=C), expect="Modified block data"),
    Step(STOCK["1.20.1"].format(p=C), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=C)}),
    Step(f"gt6energy place {RIG_FRONT}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {RIG_FRONT} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {RIG_FRONT} 32", expect="voltage 32 EU"),
    Step(f"gt6energy mode {RIG_FRONT} on", expect="emitting true", sleep=3.0),
    Step(f"gt6machine shredder check {C}", expect="energy=0"),
    Step(f"gt6machine shredder check {C}", expect="active=false"),
    Step(f"gt6machine shredder check {C}", expect="progress=0/0"),
    Step(f"gt6energy mode {RIG_FRONT} off", expect="emitting false"),

    phase("D: teardown"),
    Step("fill 383 64 275 393 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w2_eu_special autocrafter",
    slug="p29w2autocrafter",
    sites=gt6world.declare_sites(SITE_A, SITE_B, SITE_RIG_TOP, SITE_C, SITE_RIG_FRONT),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
