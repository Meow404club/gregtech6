#!/usr/bin/env python3
"""blade-six TREE chain — the axe whole-tree felling live arm.

Chain semantics (task blade-six RCON arm 1, the /gt6blade chop channel —
the REAL ServerPlayerGameMode.destroyBlock face the felling modifier walks; every
assertion rides the command's SELF-CONTAINED report — the execute...run say broadcast
never rides the RCON response, the live-calibration lesson):

  A the POSITIVE: a 5-log oak column; the axe chops the BASE — the modifier fells the
    4 logs above (upstream GT_Tool_Axe.java:112-122 walk): remainingLogs=0 (the whole
    face), all 5 logs land as drops (counts merged per item), and the durability
    payment equals the tree height (one point per log through the vanilla mineBlock
    inside destroyBlock: base 1 + felled 4 = 5);
  B the NEGATIVE: a fresh 3-log column; the BARE hand mines the base — no felling (the
    gt6:holds_tool gate), one log drop — and the axe PROBE on the next log proves both
    upper logs were still standing (remainingLogs=1 above it, damage=2 for it + its
    one felled top).

Run:  GT6_SESSION=off python3 tools/rcon/chains/blade_six_tree.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

T1 = gt6world.Site(384, 64, 208, dy=2)   # the felled 5-log column (64..68)
T2 = gt6world.Site(386, 64, 208, dy=2)   # the bare-hand negative 3-log column (64..66)

steps = []

# --------------------------------- A: the felling (the modifier walks the trunk)
steps += [
    phase("A: axe chops the base of a 5-log column -> remainingLogs=0, 5 drops, damage=5 (the tree-height face)"),
    Step("fill 384 64 208 384 68 208 minecraft:oak_log", expect="filled"),
    Step("gt6blade chop 384 64 208 axe",
         expect="broke=true, remainingLogs=0, drops=[minecraft:oak_log x5], toolDamage=5/512"),
]

# --------------------------------- B: the bare-hand negative (the holds_tool gate)
steps += [
    phase("B: the bare hand mines the base of a 3-log column -> one drop, no felling; "
          "the axe probe on the next log proves both upper logs stood"),
    Step("fill 386 64 208 386 66 208 minecraft:oak_log", expect="filled"),
    Step("gt6blade mine 386 64 208 bare hand",
         expect="drops=[minecraft:oak_log x1]"),
    Step("gt6blade chop 386 65 208 axe",
         expect="broke=true, remainingLogs=0, drops=[minecraft:oak_log x2], toolDamage=2/512"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 384 64 208 386 68 208 minecraft:air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="blade-six-tree",
    slug="bladesixtree",
    sites=gt6world.declare_sites(T1, T2),
    preferred_ports=(26241, 26251),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
