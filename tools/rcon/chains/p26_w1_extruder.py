#!/usr/bin/env python3
"""p26-w1-press-extruder-molds — the Extruder (HU) ladder live chain (the declarative
framework, the p26_w1_press shape).

Chain semantics (task p26-w1-press-extruder-molds ACCEPTANCE rcon ② — "双腿 extruder 链：
place→input（mold+block）→HU inject≥mInputMin→run→9 plate/rod 输出+模具仍在槽断言
（模具不消耗的活体加冕验收）"):

  A the rod static row (extruder T1, the Low Heat Extruder): place → `input 1` feeds the
    rod mold (the family stub) into slot 0 — the acceptance's mold-class input arm → the
    merge re-states slot 0 and adds one gt6:block_ingot_iron at slot 1 → inject 40 x 64 HU
    (size >= mInputMin 16, TIER_INPUTS[0][0]; EUt 16 duration 32 → 8 process ticks) →
    the verdict pins 18x stick_iron in the output slot (the RM.java:407 static pour face,
    the GT6RecipesExtruder FMLCommonSetup rows) and THE CROWN: check reports
    input=shape_extruder_rod x1 — THE MOLD STAYED IN ITS SLOT (the never-consumed
    semantic through Recipe.sNotConsumable reading the live gt6:extruder_shapes tag —
    the offline stub's positives re-proven against the real registry).

  B the plate static row (extruder T2): place → input 1 (the plate mold) → merge + block →
    inject 40 x 256 (size >= mInputMin 64, TIER_INPUTS[1][0]) → 9x plate_iron out (the
    :405 face) → check crown.

The inventory merges ride the loader-versioned key shapes (the p19_nbt_rebind ruling);
the name renderings are PER-LEG via Step.node_expects (the p24 ruling). The extruder
inventory is 2 input + 2 output = Size:4 (the RecipeMap shape).

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_w1_extruder.py
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

# the two extruder sites on the fresh 430 band (the press chain holds 430/432)
SITE_A = gt6world.Site(434, 64, 20, dx=0, dy=1, dz=1)  # extruder     T1 — the rod row (Low Heat)
SITE_B = gt6world.Site(436, 64, 20, dx=0, dy=1, dz=1)  # extruder_t2  T2 — the plate row
A = F(SITE_A)
B = F(SITE_B)

# the [mold, block] two-item input merge per node (the canner D-arm key-shape ruling).
ROD_MOLD_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_rod",Count:1b}},{{Slot:1b,id:"gt6:block_ingot_iron",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_rod",count:1}},{{Slot:1b,id:"gt6:block_ingot_iron",count:1}}]}}}}',
}
PLATE_MOLD_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_plate",Count:1b}},{{Slot:1b,id:"gt6:block_ingot_iron",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_plate",count:1}},{{Slot:1b,id:"gt6:block_ingot_iron",count:1}}]}}}}',
}

steps = []

# ------------------------------------------------- A: the rod static row (T1, Low Heat)
steps += [
    phase("A: the rod static row (extruder T1 Low Heat, HU) — [iron block, rod mold] -> 18 sticks, mold stays"),
    Step(f"gt6machine extruder place {A}", expect="GT6 extruder placed"),
    Step(f"gt6machine extruder input 1 {A}",
         expect="GT6 extruder input: 1x shape_extruder_rod into slot 0",
         node_expects={"1.21.1": "GT6 extruder input: 1x gt6:shape_extruder_rod into slot 0"}),
    Step(ROD_MOLD_MERGE["1.20.1"].format(p=A), expect="Modified block data",
         node_cmds={"1.21.1": ROD_MOLD_MERGE["1.21.1"].format(p=A)}),
    Step(f"gt6machine extruder inject 40 64 {A}",
         expect="outputs=[18x stick_iron; ]",
         node_expects={"1.21.1": "outputs=[18x gt6:stick_iron; ]"}),
    Step(f"gt6machine extruder check {A}",
         expect="input=shape_extruder_rodx1",
         node_expects={"1.21.1": "input=gt6:shape_extruder_rodx1"}),
    Step(f"data get block {A} inventory", expect="shape_extruder_rod"),
]

# ------------------------------------------------- B: the plate static row (T2)
steps += [
    phase("B: the plate static row (extruder T2, HU) — [iron block, plate mold] -> 9 plates, mold stays"),
    Step(f"gt6machine extruder_t2 place {B}", expect="GT6 extruder_t2 placed"),
    Step(f"gt6machine extruder_t2 input 1 {B}",
         expect="GT6 extruder input: 1x shape_extruder_plate into slot 0",
         node_expects={"1.21.1": "GT6 extruder input: 1x gt6:shape_extruder_plate into slot 0"}),
    Step(PLATE_MOLD_MERGE["1.20.1"].format(p=B), expect="Modified block data",
         node_cmds={"1.21.1": PLATE_MOLD_MERGE["1.21.1"].format(p=B)}),
    Step(f"gt6machine extruder_t2 inject 40 256 {B}",
         expect="outputs=[9x plate_iron; ]",
         node_expects={"1.21.1": "outputs=[9x gt6:plate_iron; ]"}),
    Step(f"gt6machine extruder_t2 check {B}",
         expect="input=shape_extruder_platex1",
         node_expects={"1.21.1": "input=gt6:shape_extruder_platex1"}),
]

# ------------------------------------------------- teardown
steps += [
    phase("teardown — the explicit band restore (no global state was touched)"),
    Step("fill 429 62 19 437 67 21 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-w1-extruder",
    slug="p26w1extruder",
    sites=gt6world.declare_sites(SITE_A, SITE_B),
    preferred_ports=(26112, 26122),      # this card's pinned rcon/query pair (after p26w1press 26111/26121)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
