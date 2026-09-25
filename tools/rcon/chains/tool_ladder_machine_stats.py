#!/usr/bin/env python3
"""machine-ladder MACHINE STATS chain — the machine-family ladder per-material verdicts.

Chain semantics (task machine-ladder acceptance — the "逐材质 wrench/cutter 等身份
断言 + cover 拆装 crowbar 逐材质" face): the /gt6tool stats arm builds a stack, attaches
the GT.ToolStats identity THE RECIPE WAY (primary, the form DURABILITY_MULTIPLIER)
through GT6ToolLadder.stampIdentity, and reads the item surfaces BACK — the vanilla
max-damage read, the class tint seam and the composed display name. The per-material
crowbar cover-dismantle arm (/gt6tool dismantle <pos> up <material>) drives the REAL
item dispatch with a stamped crowbar over a live cover rig (oven + shutter, the
cover_shutter_filter form). The report IS the verdict:

  gt6tool stats wrench@Bronze: maxDamage=448, tint=0xFFD2823C, name=Wrench (Bronze)

Expected literals (MT.java qual rows, the offline MachineLadderTest locks the same values):
  Steel         8630 qual(3, 6.0, 512, 2)  RGBa 130/130/130 → 0xFF828282
  Bronze        8610 qual(3, 5.5, 448, 2)  RGBa 210/130/60  → 0xFFD2823C
  TungstenSteel 8635 qual(3, 10.0, 5120, 4) RGBa 100/100/160 → 0xFF6464A0
  soft hammer   ×8 multiplier (GT_Tool_SoftHammer :79-81) → Steel 4096 / TungstenSteel 40960

The A/band: one oven rig column, the cover installed/dismantled per material arm
(each dismantle removes the cover, the next arm re-installs — rerun idempotent).
No fresh_boot / mutates member (band-local setblocks + cover store ops).

Run:  GT6_SESSION=off python3 tools/rcon/chains/tool_ladder_machine_stats.py
      (dual-leg: add --node 1.21.1-neoforge)
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

def F(pos):
    return f"{pos.x} {pos.y} {pos.z}"

# the cover rig: ONE oven column (x460 y200 z260) — the P11 form (the oven is an
# ICoverableTE); plus a sky anchor cell the chain never touches
RIG_OVEN = gt6world.Site(460, 200, 260)
ANCHOR = gt6world.declare_sites(RIG_OVEN, gt6world.Site(451, 200, 260))

steps = []

# --------------------------------- A: the ×1.0 family identity (wrench the anchor)
steps += [
    phase("A1: wrench@Bronze — 448, the 210/130/60 tint, the composed name"),
    Step("gt6tool stats wrench Bronze",
         expect="maxDamage=448, tint=0xFFD2823C, name=Wrench"),
    phase("A2: wrench@TungstenSteel — 5120, the 100/100/160 tint"),
    Step("gt6tool stats wrench TungstenSteel",
         expect="maxDamage=5120, tint=0xFF6464A0"),
    phase("A3: wrench@Steel — the ADR 512 fallback"),
    Step("gt6tool stats wrench Steel",
         expect="maxDamage=512, tint=0xFF828282"),
]

# --------------------------------- B: the family sweep at TungstenSteel (per-material identity)
steps += [
    phase("B1: cutter@TungstenSteel — 5120"),
    Step("gt6tool stats cutter TungstenSteel", expect="maxDamage=5120"),
    phase("B2: screwdriver@Bronze — 448"),
    Step("gt6tool stats screwdriver Bronze", expect="maxDamage=448"),
    phase("B3: saw@Bronze — 448"),
    Step("gt6tool stats saw Bronze", expect="maxDamage=448"),
    phase("B4: chisel@TungstenSteel — 5120"),
    Step("gt6tool stats chisel TungstenSteel", expect="maxDamage=5120"),
    phase("B5: hammer@Bronze — 448"),
    Step("gt6tool stats hammer Bronze", expect="maxDamage=448"),
    phase("B6: monkey_wrench@TungstenSteel — 5120 (the subclass inherits the wrench face)"),
    Step("gt6tool stats monkey_wrench TungstenSteel", expect="maxDamage=5120, tint=0xFF6464A0"),
    phase("B7: pincers@Bronze — 448"),
    Step("gt6tool stats pincers Bronze", expect="maxDamage=448"),
    phase("B8: crowbar@DamascusSteel — 1280 (the identity-seam ladder value, unchanged by the unify)"),
    Step("gt6tool stats crowbar DamascusSteel", expect="maxDamage=1280"),
]

# --------------------------------- C: the ×8 soft hammer (the form multiplier is LIVE)
steps += [
    phase("C1: soft_hammer@Steel — 512 ×8 = 4096 (the pre-ladder flat 512 was the pool-cut shell)"),
    Step("gt6tool stats soft_hammer Steel", expect="maxDamage=4096"),
    phase("C2: soft_hammer@TungstenSteel — 5120 ×8 = 40960"),
    Step("gt6tool stats soft_hammer TungstenSteel", expect="maxDamage=40960"),
]

# --------------------------------- D: the per-material crowbar COVER DISMANTLE (the real dispatch)
steps += [
    phase("D1: the cover rig — oven + shutter UP (the P11 form)"),
    Step(f"gt6oven place {F(RIG_OVEN)}", expect="placed"),
    Step(f"gt6cover install {F(RIG_OVEN)} up gt6:cover_shutter", expect="OK"),
    phase("D2: Bronze crowbar dismantles the cover — the ladder max 448 on the live payment read"),
    Step(f"gt6tool dismantle {F(RIG_OVEN)} up Bronze",
         expect="material=Bronze: toolDamage=10000, crowbarDamage=1/448, coverInInventory=true"),
    phase("D3: re-install, Steel crowbar — the ADR 512 arm"),
    Step(f"gt6cover install {F(RIG_OVEN)} up gt6:cover_shutter", expect="OK"),
    Step(f"gt6tool dismantle {F(RIG_OVEN)} up Steel",
         expect="material=Steel: toolDamage=10000, crowbarDamage=1/512, coverInInventory=true"),
    phase("D4: re-install, TungstenSteel crowbar — 5120"),
    Step(f"gt6cover install {F(RIG_OVEN)} up gt6:cover_shutter", expect="OK"),
    Step(f"gt6tool dismantle {F(RIG_OVEN)} up TungstenSteel",
         expect="material=Tungstensteel: toolDamage=10000, crowbarDamage=1/5120, coverInInventory=true"),
]

# --------------------------------- E: the negative (unknown tool id)
steps += [
    phase("E: the stats arm rejects the unconverted family (the P31 scope ruling)"),
    Step("gt6tool stats bending_cylinder_small Steel", expect="not a ladder tool", allow_failed=True),
]

# --------------------------------- F: the rerun idempotency face (pass 2 re-runs all)

CHAIN = Chain(
    name="tool-ladder-machine-stats",
    slug="toolladdermachinestats",
    sites=ANCHOR,
    preferred_ports=(26246, 26256),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
