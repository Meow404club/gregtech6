#!/usr/bin/env python3
"""p29-w5-t6-electric-nineteen SWITCH chain — the sneak bare-target twin swap live arm
(task p29-w5-t6-electric-nineteen, the chain-2 column x620..626; the :162/:171
Behavior_Switch_Metadata transcription):

  A wrench_lv -> monkey_wrench_lv on a bare stone target (x620): the swapped stack
    carries the SAME shell wear (damage 5) and the SAME EU charge (7777, set through
    the real face) — the 耐久/EU 保留 acceptance;
  B the swap BACK (x622): monkey_wrench_lv -> wrench_lv — the 对偶 edge;
  C the BE-negative (x624): a chest target (a TileEntity) — mCheckTarget leaves the
    face to the tool interaction, the tool does NOT switch (switched=False, the t3
    mCheckTarget negative arm; the verdict rides the switched= literal inside the OK
    line, the t3 FAILED-marker lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t6_electric_switch.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

A = gt6world.Site(620, 65, 240)  # wrench_lv -> monkey_wrench_lv
B = gt6world.Site(622, 65, 240)  # the swap back
C = gt6world.Site(624, 65, 240)  # the chest BE-negative
D = gt6world.Site(626, 65, 240)  # the jackhammer mode swap

steps = []

# --------------------------------- A: wrench_lv -> monkey_wrench_lv, the charge/wear kept
steps += [
    phase("A: the sneak swap to the monkey wrench keeps damage 5 + eu 7777"),
    Step(f"setblock {A.x} {A.y} {A.z} minecraft:stone", expect="Changed the block"),
    Step(f"gt6electric switch {A.x} {A.y} {A.z} wrench_lv",
         expect="held=gt6:monkey_wrench_lv (expect gt6:monkey_wrench_lv), damage=5 (5), eu=7777 (7777), switched=true"),
]

# --------------------------------- B: the swap back (the :171 edge)
steps += [
    phase("B: the monkey wrench swaps back to the wrench (the 对偶 edge)"),
    Step(f"setblock {B.x} {B.y} {B.z} minecraft:stone", expect="Changed the block"),
    Step(f"gt6electric switch {B.x} {B.y} {B.z} monkey_wrench_lv",
         expect="held=gt6:wrench_lv (expect gt6:wrench_lv), damage=5 (5), eu=7777 (7777), switched=true"),
]

# --------------------------------- C: the BE-negative — the chest target lets the tool face win
steps += [
    phase("C: the chest target (a TileEntity) — mCheckTarget refuses the swap"),
    Step(f"setblock {C.x} {C.y} {C.z} minecraft:chest", expect="Changed the block"),
    Step(f"gt6electric switch {C.x} {C.y} {C.z} wrench_lv",
         expect="held=gt6:wrench_lv (expect gt6:monkey_wrench_lv), damage=5 (5), eu=7777 (7777), switched=false"),
]

# --------------------------------- D: the jackhammer mode swap (the :165-166 edge)
steps += [
    phase("D: the jackhammer normal <-> no-ores mode swap"),
    Step(f"setblock {D.x} {D.y} {D.z} minecraft:stone", expect="Changed the block"),
    Step(f"gt6electric switch {D.x} {D.y} {D.z} jackhammer_hv_normal",
         expect="held=gt6:jackhammer_hv_no_ores (expect gt6:jackhammer_hv_no_ores), damage=5 (5), eu=7777 (7777), switched=true"),
    Step(f"gt6electric switch {D.x} {D.y} {D.z} jackhammer_hv_no_ores",
         expect="held=gt6:jackhammer_hv_normal (expect gt6:jackhammer_hv_normal), damage=5 (5), eu=7777 (7777), switched=true"),
]

CHAIN = Chain(
    name="p29-w5-t6-electric-switch p29_w5_t6_electric",
    slug="p29w5t6electricswitch",
    sites=gt6world.declare_sites(A, B, C, D),
    preferred_ports=(26363, 26373),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
