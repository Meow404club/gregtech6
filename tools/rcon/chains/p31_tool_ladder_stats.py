#!/usr/bin/env python3
"""p31-blade-ladder STATS chain — the blade material ladder per-material verdicts.

Chain semantics (task p31-blade-ladder acceptance — the "GT6BladeToolCommand 链逐材质
断言（伤害/耐久/tint）" face): the /gt6blade stats arm builds a stack, attaches the
GT.ToolStats identity THE RECIPE WAY (primary, handle = mHandleMaterial, the shape
DURABILITY_MULTIPLIER) through GT6ItemData.set, and reads the item surfaces BACK —
the vanilla max-damage read, the vanilla per-stack ATTACK_DAMAGE attribute map, and
the class tint seam. The report IS the verdict:

  gt6blade stats sword@Steel: attack=6.0, maxDamage=512, tint=0xFF828282

Expected literals (the offline GT6BladeLadderTest locks the same values):
  Steel         8630 qual(3, 6.0, 512, 2) RGBa 130/130/130 → 0xFF828282
  DamascusSteel 8634 qual(3, 8.0, 1280, 2) RGBa 110/110/110 → 0xFF6E6E6E
  TungstenSteel 8635 qual(3, 10.0, 5120, 4) RGBa 100/100/160 → 0xFF6464A0
  Bronze        8610 qual(3, 5.5, 448, 2) RGBa 210/130/60 → 0xFFD2823C

No world sites — the stats arm is a pure command channel (the p29_w6_ore_mech
"content IS the boot" family, minus even the boot dependence: zero blocks touched,
no fresh_boot needed, the chain is rerun-idempotent by construction).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_tool_ladder_stats.py
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

# the cleanup bbox anchor — ONE sky cell the chain never touches (the stats arm is a
# pure command channel; the region machinery just needs a real box to stand on)
ANCHOR = gt6world.declare_sites(gt6world.Site(451, 200, 260))

steps = []

# --------------------------------- A: the sword ladder (the family anchor)
steps += [
    phase("A1: sword@Steel — attack 4+2=6.0, the ADR 512, the 130/130/130 tint"),
    Step("gt6blade stats sword Steel",
         expect="gt6blade stats sword@Steel: attack=6.0, maxDamage=512, tint=0xFF828282"),
    phase("A2: sword@DamascusSteel — 1280 durability, the 110/110/110 tint"),
    Step("gt6blade stats sword DamascusSteel",
         expect="attack=6.0, maxDamage=1280, tint=0xFF6E6E6E"),
    phase("A3: sword@TungstenSteel — quality 4 → attack 8.0, 5120, 100/100/160"),
    Step("gt6blade stats sword TungstenSteel",
         expect="attack=8.0, maxDamage=5120, tint=0xFF6464A0"),
    phase("A4: sword@Bronze — 448 durability, the 210/130/60 tint"),
    Step("gt6blade stats sword Bronze",
         expect="attack=6.0, maxDamage=448, tint=0xFFD2823C"),
]

# --------------------------------- B: the knife + butchery shapes (the ×1.0 payload)
steps += [
    phase("B1: knife@Steel — attack 2+2=4.0, the family 512"),
    Step("gt6blade stats knife Steel",
         expect="attack=4.0, maxDamage=512"),
    phase("B2: butchery_knife@Steel — attack 1+2=3.0"),
    Step("gt6blade stats butchery_knife Steel",
         expect="attack=3.0, maxDamage=512"),
    phase("B3: butchery_knife@TungstenSteel — quality 4 → 5.0, 5120"),
    Step("gt6blade stats butchery_knife TungstenSteel",
         expect="attack=5.0, maxDamage=5120"),
]

# --------------------------------- C: the single-tier ruling negatives
steps += [
    phase("C: the axe pair stays single-tier (the p31 family ruling) — the stats arm rejects"),
    Step("gt6blade stats axe Steel", expect="not a ladder tool", allow_failed=True),
    Step("gt6blade stats axe_double Steel", expect="not a ladder tool", allow_failed=True),
]

# --------------------------------- D: the rerun idempotency face (pass 2 re-runs all)

CHAIN = Chain(
    name="p31-tool-ladder-stats",
    slug="p31toolladderstats",
    sites=ANCHOR,
    preferred_ports=(26245, 26255),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
