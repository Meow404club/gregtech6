#!/usr/bin/env python3
"""p26-w1-press-extruder-molds — the Press (KU) ladder live chain (the declarative
framework, the p24_canner_refill shape).

Chain semantics (task p26-w1-press-extruder-molds ACCEPTANCE rcon ① — "双腿 press 链：
place→input（含模具类输入）→KU inject≥mInputMin→run→输出断言"):

  A the plate forming row (press T1): place → the inventory merge carries the mold-class
    input (gt6:shape_extruder_plate at slot 0) + one gt6:block_ingot_iron (slot 1) →
    inject 40 x 64 KU (size >= mInputMin 16, TIER_INPUTS[0][0]; EUt 16 duration 32 →
    2 overclocks → 8 process ticks; the id1680 silent-no-start lesson: the packet must
    clear mInputMin) → the verdict pins 9x plate_iron in the output slot (the RM.java:405
    face via the GT6RecipeMapFormingPress row0 FORMING arm, the output multiplier 9) and
    THE CROWN: check reports input=shape_extruder_plate x1 — the MOLD STAYED IN ITS SLOT
    (the never-consumed semantic through Recipe.sNotConsumable, the live tag read).

  B the rod forming row (press T4): place → merge (rod mold + block_ingot_iron) →
    inject 40 x 4096 (size >= mInputMin 1024, TIER_INPUTS[3][0]; the T4 overclock ladder
    flattens the 32-tick duration inside the rig) → 18x stick_iron out (the :407
    multiplier face) → check crown: input=shape_extruder_rod x1.

The inventory merges ride the loader-versioned key shapes (the p19_nbt_rebind ruling):
1.20.1 Count:1b vs 1.21.1 count:1 — the p24_canner_refill D-arm shapes verbatim. The
check/inject name renderings are PER-LEG via Step.node_expects (the 1.20.1 ItemStack
renders the bare registry path, the 21.1 rendering is NAMESPACED — the p24 ruling).

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_w1_press.py
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

# the two press sites on the fresh 430 band (clear of the p24 canner 400 band)
SITE_A = gt6world.Site(430, 64, 20, dx=0, dy=1, dz=1)  # press     T1 — the plate row
SITE_B = gt6world.Site(432, 64, 20, dx=0, dy=1, dz=1)  # press_t4  T4 — the rod row
A = F(SITE_A)
B = F(SITE_B)

# the [mold, block] two-item input merge per node (the canner D-arm key-shape ruling):
# slot 0 = the mold (the family feed), slot 1 = one storage block (the consumed leg).
# The press inventory is 3 input + 1 output = Size:4 (the RecipeMap shape).
PLATE_MOLD_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_plate",Count:1b}},{{Slot:1b,id:"gt6:block_ingot_iron",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_plate",count:1}},{{Slot:1b,id:"gt6:block_ingot_iron",count:1}}]}}}}',
}
ROD_MOLD_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_rod",Count:1b}},{{Slot:1b,id:"gt6:block_ingot_iron",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:shape_extruder_rod",count:1}},{{Slot:1b,id:"gt6:block_ingot_iron",count:1}}]}}}}',
}

steps = []

# ------------------------------------------------- A: the plate forming row (T1)
steps += [
    phase("A: the plate forming row (press T1, KU) — [plate mold, iron block] -> 9 plates, mold stays"),
    Step(f"gt6machine press place {A}", expect="GT6 press placed"),
    Step(PLATE_MOLD_MERGE["1.20.1"].format(p=A), expect="Modified block data",
         node_cmds={"1.21.1": PLATE_MOLD_MERGE["1.21.1"].format(p=A)}),
    Step(f"gt6machine press check {A}",
         expect="input=shape_extruder_platex1",
         node_expects={"1.21.1": "input=gt6:shape_extruder_platex1"}),
    Step(f"gt6machine press inject 40 64 {A}",
         expect="outputs=[9x plate_iron; ]",
         node_expects={"1.21.1": "outputs=[9x gt6:plate_iron; ]"}),
    Step(f"gt6machine press check {A}",
         expect="input=shape_extruder_platex1",
         node_expects={"1.21.1": "input=gt6:shape_extruder_platex1"}),
    Step(f"data get block {A} inventory", expect="shape_extruder_plate"),
]

# ------------------------------------------------- B: the rod forming row (T4)
steps += [
    phase("B: the rod forming row (press T4, KU) — [rod mold, iron block] -> 18 sticks, mold stays"),
    Step(f"gt6machine press_t4 place {B}", expect="GT6 press_t4 placed"),
    Step(ROD_MOLD_MERGE["1.20.1"].format(p=B), expect="Modified block data",
         node_cmds={"1.21.1": ROD_MOLD_MERGE["1.21.1"].format(p=B)}),
    Step(f"gt6machine press_t4 inject 40 4096 {B}",
         expect="outputs=[18x stick_iron; ]",
         node_expects={"1.21.1": "outputs=[18x gt6:stick_iron; ]"}),
    Step(f"gt6machine press_t4 check {B}",
         expect="input=shape_extruder_rodx1",
         node_expects={"1.21.1": "input=gt6:shape_extruder_rodx1"}),
]

# ------------------------------------------------- teardown
steps += [
    phase("teardown — the explicit band restore (no global state was touched)"),
    Step("fill 429 62 19 437 67 21 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-w1-press",
    slug="p26w1press",
    sites=gt6world.declare_sites(SITE_A, SITE_B),
    preferred_ports=(26111, 26121),      # this card's pinned rcon/query pair (the 2610x segment, after p25cfoamspray 26110/26120)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
