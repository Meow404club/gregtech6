#!/usr/bin/env python3
"""electric-nineteen JACKHAMMER chain — the rockGt conversion and the no-ores
zero-break arm (task electric-nineteen, the chain-3 column x630..638):

  A the ROCKGT conversion (x630): the jackhammer breaks cobblestone and the drops are
    FOUR gt6:rock_gt_stone — the GLM gt6:jackhammer_hv_normal_rocks, mode
    JACKHAMMER_ROCKS (the RM.pack Loader_Recipes_Vanilla :152 column, x4); the drain
    = the upstream 200/break column (GT_Tool_JackHammer_HV :46);
  B the NO-ORES surface gate (x634): the no-ores form on an IRON ORE reads speed=0.0
    (the getDigSpeed-0 gate — 对矿石块零破坏, the SURFACE-GATED refusal with the block
    staying put);
  C the NORMAL form on the same ore (x636): speed=72.0 (6.0 x the upstream 12.0
    multiplier) — the ores stay breakable on the normal mode;
  D the wear sampling (programmatic): 10000 seeded rolls through the live seam — the
    ratio lands at 1-in-10 for the quality-0 drill (the deterministic seed makes the
    report byte-stable across legs).

Run:  GT6_SESSION=off python3 tools/rcon/chains/electric_jackhammer.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

A = gt6world.Site(630, 65, 240)  # the rockGt conversion
B = gt6world.Site(634, 65, 240)  # the no-ores gate
C = gt6world.Site(636, 65, 240)  # the normal form on the same ore

steps = []

# --------------------------------- A: the rockGt conversion — cobblestone -> 4x rock_gt_stone
steps += [
    phase("A: the jackhammer breaks rocks into pieces (GLM JACKHAMMER_ROCKS, rockGt x4)"),
    Step(f"setblock {A.x} {A.y} {A.z} minecraft:cobblestone", expect="Changed the block"),
    Step(f"gt6electric mine {A.x} {A.y} {A.z} jackhammer_hv_normal",
         expect="drained 200), speed=72.0, drops=[gt6:rock_gt_stone x4]"),
]

# --------------------------------- B: the no-ores form — the ore block is zero-break
steps += [
    phase("B: the no-ores form — speed=0.0 on iron ore, the surface gate refuses"),
    Step(f"setblock {B.x} {B.y} {B.z} minecraft:iron_ore", expect="Changed the block"),
    Step(f"gt6electric speed {B.x} {B.y} {B.z} jackhammer_hv_no_ores",
         expect="speed=0.0, correctForDrops=false"),
    Step(f"gt6electric mine {B.x} {B.y} {B.z} jackhammer_hv_no_ores",
         expect="broke=false, block stays=true, speed=0.0, eu=1024000"),
]

# --------------------------------- C: the normal form — the same ore breaks
steps += [
    phase("C: the normal form — the ore reads the full jackhammer speed"),
    Step(f"setblock {C.x} {C.y} {C.z} minecraft:iron_ore", expect="Changed the block"),
    Step(f"gt6electric speed {C.x} {C.y} {C.z} jackhammer_hv_normal",
         expect="speed=72.0"),
]

# --------------------------------- D: the wear sampling — the seeded ratio report
steps += [
    phase("D: the random-wear sampling — 10000 seeded rolls at 1-in-10 (q0)"),
    Step("gt6electric wear mining_drill_lv 10000",
         expect="hits=1039, ratio=0.1039, denominator=10 (max(10,quality*20))"),
    Step("gt6electric wear mining_drill_hv 20000",
         expect="denominator=40 (max(10,quality*20))"),
]

CHAIN = Chain(
    name="electric_tools electric-jackhammer",
    slug="electricjackhammer",
    sites=gt6world.declare_sites(A, B, C),
    preferred_ports=(26363, 26373),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
