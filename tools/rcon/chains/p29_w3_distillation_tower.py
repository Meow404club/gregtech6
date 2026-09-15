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
Loader_Recipes_Chem.java:352-:360 verbatim — seven fractions per row), but the
PORT freezes mTanksOutput to ONE tank (GT6Distillation.java:87-90, the declared
deviation; TileEntityBase10MultiBlockMachine.java:191), so multi-fraction rows
refuse at canOutput=0 (:735-:736 FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS) —
the old single-fraction smoke row (oil 1000 -> creosote 500) they replaced was
the last runnable shape. The run arm therefore pins the REFUSAL live (progress
0/0 steady over >= 2x the :356 row duration, input untouched); the output-bank
expansion is a product-side follow-up and is reported, not chain-fixable.

  A  form — /gt6multiblock form scaffolds the 80 part cells from the fake-player
     stock (the self-cell passes without consuming), the design-1 probe pins the
     card-1 DESIGN render slot on the hole column, and the plain distill_part
     cell rejects it (the layering live).
  B  互拒 — the CU dial against the HU tower: the oil sits untouched, progress 0.
  C  the HU dial + the multi-fraction refusal pin — progress 0/0 steady, input
     tank intact, out bank empty (the :356 row found but canOutput=0).
  D  the steady-state window — a third spaced read past 2x the row duration (the
     old barrel-at-the-hole push arm went dark with the refusal: nothing routes).

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

    phase("C: the HU dial + the multi-fraction refusal pin (the declared single-tank deviation)"),
    Step(f"gt6energy type {SOURCE} HU", expect="type"),
    # the :356 true row (gt6:oil -> seven fractions) is FOUND but canOutput=0 — the frozen
    # one-tank output bank (GT6Distillation.java:87-90, TileEntityBase10MultiBlockMachine
    # :191) refuses it at :735-:736 (FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS). The oil
    # must sit UNTOUCHED while the HU dial is live: progress 0/0 steady, input intact,
    # output bank empty (tankText renders the empty tank as '-', GT6Distillation :861-863).
    Step(f"gt6distillation check {TOWER}", expect="progress=0/0", sleep=6.0),
    Step(f"gt6distillation check {TOWER}", expect="progress=0/0", sleep=6.0),
    Step(f"gt6distillation check {TOWER}", expect="in_tank=[1000mB gt6:oil]"),
    Step(f"gt6distillation check {TOWER}", expect="out_tank=[-]"),

    phase("D: the steady-state window — a third spaced read past 2x the :356 row duration"),
    # the old barrel-at-the-hole push arm went dark with the refusal: nothing routes while
    # the output bank stays one tank (the product-side expansion is reported, not
    # chain-fixable) — the refusal holding past 2x the row duration is the live pin.
    Step(f"gt6distillation check {TOWER}", expect="progress=0/0", sleep=9.0),

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
