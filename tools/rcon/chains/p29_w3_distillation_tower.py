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

The smoke row is the committed distillationtower.json (oil 1000 -> creosote 500,
eUt 120, duration 160, total power 19200): at the 32V x 8A = 256 HU/t dial the
run completes in ceil(19200/256) = 75 ticks (~4 s; the chain sleeps 8).

  A  form — /gt6multiblock form scaffolds the 80 part cells from the fake-player
     stock (the self-cell passes without consuming), the design-1 probe pins the
     card-1 DESIGN render slot on the hole column, and the plain distill_part
     cell rejects it (the layering live).
  B  互拒 — the CU dial against the HU tower: the oil sits untouched, progress 0.
  C  run — the HU dial: the fraction row completes, the output tank holds the
     creosote (the live "1 行分馏进出").
  D  the back-hole routing arm — a barrel at the creosote hole (y+1) receives the
     push (the upstream :152-166 default layer).

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
ARM_BARREL = "386 66 305"     # the creosote routing hole target (controller Y + 1)

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

    phase("C: run — the HU dial drives the fraction row to completion"),
    Step(f"gt6energy type {SOURCE} HU", expect="type"),
    Step(f"gt6distillation check {TOWER}", expect="out_tank=[500mB gt6:creosote]", poll=15.0),
    Step(f"gt6distillation check {TOWER}", expect="in_tank=[-]"),

    phase("D: the back-hole routing arm — the barrel at the y+1 hole receives the push"),
    Step(f"setblock {ARM_BARREL} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6tank stat {ARM_BARREL}", expect="L of gt6:creosote", poll=10.0),

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
