#!/usr/bin/env python3
"""p29-w3-cryo-tower — the Cryo Distillation Tower live acceptance chain (task
p29-w3-distill-crucible ACCEPTANCE ②: Cryo CU 供能互拒臂（W2 守卫活体）).

The cryo row (:1227) is the verbatim-clone structure of the HU tower on the CU
energy domain over the committed cryodistillationtower.json smoke row (water
1000 -> ice 1, eUt 8, duration 64, total power 512). The chain proves the W2
type-gate LIVE on a multiblock: the EU dial is refused (progress frozen, the
water stays), the CU dial runs the row, and the ITEM output arm lands the ice
in a chest at the offset-3 cell (the upstream :143 doOutputItems target — the
port restores the SIDE_BACK auto-out column this row declares).

Geometry (the controller at 402 65 302, FACING NORTH default — the tower body
z302..304, the arm cell z305, the source rig under the base at y63 z303):
  the ice lands in the chest at 402 65 305 (the item arm target, the vanilla
  IItemHandler capability the insert walks).

The CU run needs ceil(512/256) = 2 ticks at the 32V x 8A dial; the chain sleeps
4 after the dial. passes=2 is the idempotency proof.
Run:  python3 tools/rcon/chains/p29_w3_cryo_tower.py --node 1.20.1-forge
      python3 tools/rcon/chains/p29_w3_cryo_tower.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=300 band, the cryo column x398..406
TOWER = "402 65 302"
SOURCE = "402 63 303"
ARM_CHEST = "402 65 305"

steps = [
    phase("A: form — the cryo tower scaffolds identical to its HU twin"),
    Step(f"setblock {TOWER} gt6:cryo_distillation_tower", expect="Changed the block"),
    Step(f"gt6multiblock form {TOWER} 999", expect="formed=true okay=true"),
    Step(f"gt6distillation check {TOWER}",
         expect="Structure is formed already! okay=true block_formed=true linked_parts=80/81"),

    phase("B: 互拒 — the EU dial is refused, the CU dial runs (the W2 guard live on a multiblock)"),
    Step(f"gt6energy place {SOURCE}", expect="placed at 402, 63, 303"),
    Step(f"gt6energy type {SOURCE} EU", expect=": type "),
    Step(f"gt6energy volt {SOURCE} 32", expect="voltage 32"),
    Step(f"gt6energy amp {SOURCE} 8", expect="amperage 8"),
    Step(f"gt6energy mode {SOURCE} on", expect="emitting true"),
    Step(f"gt6distillation fluid {TOWER} fill minecraft:water 1000", expect="accepted 1000/1000 mB of minecraft:water"),
    Step(f"gt6distillation check {TOWER}", expect="progress=0/0", sleep=5.0),
    Step(f"gt6distillation check {TOWER}", expect="in_tank=[1000mB minecraft:water]"),
    Step(f"gt6energy type {SOURCE} CU", expect=": type "),
    Step(f"gt6distillation check {TOWER}", expect="in_tank=[-]", poll=30.0),  # the charge cadence: the tower and the source alternate ticks — ~80 ticks for the 512-power row

    phase("C: the item output arm — the ice lands in the offset-3 chest"),
    Step(f"setblock {ARM_CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"gt6distillation check {TOWER}", expect="out_tank=[-]", poll=15.0),
    # the ice sits in the chest's item handler (the doOutputItems insert landed)
    Step(f"execute if data block {ARM_CHEST} Items[{{id:\"minecraft:ice\"}}]", expect="Test passed"),

    phase("D: teardown — the explicit band restore"),
    Step("fill 399 62 300 405 75 307 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w3_distill_crucible p29_w3_cryo_tower",
    slug="p29w3cryotower",
    sites=gt6world.declare_sites(
        gt6world.Site(401, 63, 302, dx=2, dy=10, dz=3),
    ),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
