#!/usr/bin/env python3
"""p29-w3-distillation-tower — the Distillation Tower live acceptance chain (task
p29-w3-distill-crucible ACCEPTANCE ①: 塔 form（底 18101 + 柱 18102 + 背孔 design1）+ 1 行
分馏进出 + 背面孔输出臂).

Geometry (the controller at 386 65 302, FACING NORTH by setblock default —
the structure centre one cell SOUTH, the getOffsetXN "behind the facing"
arithmetic):
  y 64        3x3 Heat Transmitter 18101 (ONLY_ENERGY_IN)
  y 65..72    3x3x8 Distill Part 18102 (y65 ONLY_ITEM_FLUID input layer, above
              ONLY_FLUID_OUT; the back-centre column x386 z304 carries design 1)
  z 305       the output arm column (the item arm at y65, the fluid routing holes
              y66..y72 — the upstream :143 offset-3 cells)
  y 63 z 303  the /gt6energy source block, under the transmitter base (the W2 rig form)

The map rows are the W4 hot-lube card's TRUE oil rows (distillationtower.json,
Loader_Recipes_Chem.java:352-:360 verbatim — seven fractions per row), and since
task p30-distill-output-routing (ruling 2026-09-16 distill-tower option a) the
port RUNS them: the output bank is the upstream NINE-tank library (RM.java:65/:66
fluids 1/9/0 — the W3④ single-tank freeze is undone), so the :356 row passes
canOutput and every fraction routes down its class hole (the upstream :148-170
table: propane/methane y+7 ... default y+1). The C/D refusal pins of the
p30-rcon-chain-repair session are REBOUND onto the live run (the 7-barrel capture
census).

  A  form — /gt6multiblock form scaffolds the 80 part cells from the fake-player
     stock (the self-cell passes without consuming), the design-1 probe pins the
     card-1 DESIGN render slot on the hole column, and the plain distill_part
     cell rejects it (the layering live).
  B  互拒 — the CU dial against the HU tower: the oil sits untouched, progress 0.
  C  the HU dial + the seven-fraction live run — 7 capture barrels ride the
     routing holes y66..y72 and a chest the item arm; the oil drains batch by
     batch (the :743 bind: 8 stages of 25 L per batch), the out bank ends empty
     (everything pushed down the holes), the chest catches the chance dusts so
     no batch parks.
  D  the routing census — each barrel holds EXACTLY its class fraction:
     lube y66 / fuel y67 / diesel y68 / kerosine y69 / petrol y70 / butane y71 /
     propane y72 (the seven-way positive set uniquely pins the table).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  python3 tools/rcon/chains/p29_w3_distillation_tower.py --node 1.20.1-forge
      python3 tools/rcon/chains/p29_w3_distillation_tower.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=300 band, the tower column x384..390
TOWER = "386 65 302"          # the controller (facing north)
SOURCE = "386 63 303"         # the energy rig under the base layer
HOLE = "386 70 304"           # a y+5 hole-column cell (design 1)
PLAIN = "386 70 303"          # the same-layer centre cell (design 0)
ARM_CHEST = "386 65 305"      # the item-arm capture (the offset-3 cell, the cryo chain's chest form)

steps = [
    phase("A: form — the generic scaffold arm builds the 80-cell tower"),
    Step(f"setblock {TOWER} gt6:distillation_tower", expect="Changed the block"),
    Step(f"gt6multiblock form {TOWER} 999",
         expect="formed=true okay=true"),
    # the hole column carries the card-1 DESIGN render slot (the checker's per-cell design write)
    Step(f"execute if block {HOLE} gt6:distill_part[design=1]", expect="Test passed"),
    Step(f"execute if block {PLAIN} gt6:distill_part[design=1]", expect="Test failed"),
    Step(f"execute if block {HOLE} gt6:distill_part[design=0]", expect="Test failed"),
    # 80 of the 81 declared cells linked (the controller's own cell passes unlinked)
    Step(f"gt6distillation check {TOWER}",
         expect="Structure is formed already! okay=true block_formed=true linked_parts=80/81"),

    phase("B: 互拒 — the CU dial is refused by the HU tower (the W2 type-gate live)"),
    Step(f"gt6energy place {SOURCE}", expect="placed at 386, 63, 303"),
    Step(f"gt6energy type {SOURCE} CU", expect=": type "),
    Step(f"gt6energy volt {SOURCE} 32", expect="voltage 32"),
    Step(f"gt6energy amp {SOURCE} 8", expect="amperage 8"),
    Step(f"gt6energy mode {SOURCE} on", expect="emitting true"),
    Step(f"gt6distillation fluid {TOWER} fill gt6:oil 1000", expect="accepted 1000/1000 mB of gt6:oil"),
    Step(f"gt6distillation check {TOWER}", expect="progress=0/0", sleep=5.0),
    Step(f"gt6distillation check {TOWER}", expect="in_tank=[1000mB gt6:oil]"),

    phase("C: the HU dial + the seven-fraction live run (the nine-tank bank, ruling 2026-09-16)"),
    # the capture rig FIRST: a chest on the item arm (the chance dusts — no batch parks)
    # and 7 barrels on the routing holes y66..y72 (the class holes y+1..y+7). Then the HU
    # dial at 1024V: the :356 true row (gt6:oil 25 -> seven fractions) runs 8-stage
    # batches (the :743 bind 512/64), 64 ticks per batch, 5 batches drain the 1000 mB.
    Step(f"setblock {ARM_CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"setblock 386 66 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 386 67 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 386 68 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 386 69 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 386 70 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 386 71 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"setblock 386 72 305 gt6:barrel_metal", expect="Changed the block"),
    Step(f"gt6energy type {SOURCE} HU", expect="type"),
    Step(f"gt6energy volt {SOURCE} 1024", expect="voltage 1024"),
    Step(f"gt6energy amp {SOURCE} 1", expect="amperage 1"),
    # the run: poll until the input tank drains (every batch found its row — canOutput
    # passes on the nine-tank bank). NOTE: this face can light up between the LAST
    # batch's consumption and its completion — the terminal empty-bank gate lives in D.
    Step(f"gt6distillation check {TOWER}", expect="in_tank=[-]", poll=45.0),

    phase("D: the routing census — each fraction landed on its class hole"),
    # RACE NOTE (live-calibrated, first run): out_tank=[-] also matches BETWEEN the last
    # batch's oil consumption and its completion, so the EMPTY faces cannot gate the
    # census. The gate is the FINAL TOTAL on the fuel hole instead — 1000 = 5 batches
    # (the :743 bind 8 stages x 25 L) x the 25 L fuel slot; the poll waits out the last
    # batch's completion+push. The remaining holes are then read as exact amounts
    # (lube 1000, the rest 600 — the :356 slot table x 40 processes).
    Step(f"gt6tank stat 386 67 305", expect="1000/64000 L of gt6:fuel", poll=30.0),
    Step(f"gt6tank stat 386 66 305", expect="1000/64000 L of gt6:lubricant"),
    Step(f"gt6tank stat 386 68 305", expect="600/64000 L of gt6:diesel"),
    Step(f"gt6tank stat 386 69 305", expect="600/64000 L of gt6:kerosine"),
    Step(f"gt6tank stat 386 70 305", expect="600/64000 L of gt6:petrol"),
    Step(f"gt6tank stat 386 71 305", expect="600/64000 L of gt6:butane"),
    Step(f"gt6tank stat 386 72 305", expect="600/64000 L of gt6:propane"),
    # terminal census: the bank really is empty AFTER the last batch landed
    Step(f"gt6distillation fluid {TOWER} stat", expect="out_tank=[-]"),

    phase("E: teardown — the explicit band restore"),
    Step("fill 383 62 300 389 75 307 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w3_distill_crucible p29_w3_distillation_tower",
    slug="p29w3distillationtower",
    sites=gt6world.declare_sites(
        gt6world.Site(385, 63, 302, dx=2, dy=10, dz=3),   # the tower body + arm column + the rig
    ),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
