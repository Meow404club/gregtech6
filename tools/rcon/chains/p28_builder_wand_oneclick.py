#!/usr/bin/env python3
"""p28-builder-wand-oneclick — the builder-wand ONE-CLICK live acceptance chain.

The user ruling (2026-09-12): the builder wand forms the COMPLETE multiblock on
ONE click — a DECLARED deviation from the upstream 1.7.10 nine-click semantics
(ADR 2026-09-12-p28-builder-wand-oneclick). The research
(research.p28-r-builder-wand-second-root) proved the pre-P28 "wand builds half a
multiblock" report was not a port bug: the player feed (useOn -> the real click
coordinate) was gated by the upstream ±1 Chebyshev click window
(ITileEntityMultiBlockController.java:145-146 == upstream :51), so a single
click only scaffolded the anchor's neighbourhood (16 of the crucible's 24
walls), while the RCON form arm fed checker.form aClickedAt=null (beat 4,
GTMultiBlockStructureChecker.java:322) and formed all 24 in one shot — the
p27 chain's green covered the form arm, never the player feed.

Chain semantics — the wandclick arm (GTMultiBlockCommand, this card) IS the
player-feed coverage: it resolves the scaffold target FROM the clicked cell
(GT6BuilderWandItem.scaffoldTarget, the exact useOn resolution) and drives the
production dispatch with that real coordinate:

  A the one-click story (crucible at C, controller block only, no walls):
    `gt6multiblock wandclick C 24` → formed=true okay=true stock 24 -> 0.
    The discriminators:
    - the y+2 ring cell (C+1, y+2, C-1) holds a WALL — the pre-P28 click window
      left the whole y+2 ring air after a controller click (16 walls, the far
      ring missing; the offline GTMultiBlockFacingIntegrityTest pin documented
      exactly that story pre-rewrite);
    - the y+0 ring cell (C-1, y+0, C+1) holds a wall too (the near ring).
    `gt6multiblock crucible C check` → okay=true linked_parts=24/24.
    The idempotency arm: a SECOND wandclick (stock 1) on the formed structure
    consumes NOTHING — formed=true okay=true stock 1 -> 1 (the SET walk's
    beat-1 diagnosis short-circuit).
  B the form arm regression anchor (crucible at F): the p16/p27 form arm is
    UNTOUCHED by this card — `gt6multiblock form F 24` still forms
    (formed=true okay=true stock 24 -> 0, linked_parts=24/24).
  C teardown: the explicit bbox restore (the declared-site cleanup is the
    structural backstop).

Run:  python3 tools/rcon/chains/p28_builder_wand_oneclick.py --node 1.20.1-forge
      python3 tools/rcon/chains/p28_builder_wand_oneclick.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

C_X, C_Y, C_Z = 300, 64, 100  # the wandclick crucible (arm A)
F_X, F_Y, F_Z = 310, 64, 100  # the form regression anchor (arm B)
C = f"{C_X} {C_Y} {C_Z}"
F = f"{F_X} {F_Y} {F_Z}"
# one site covers both crucibles: x 298..312, y 62..68, z 97..103
SITE = gt6world.Site(305, C_Y + 1, C_Z, dx=7, dy=3, dz=3)

CONTROLLER_BLOCK = "gt6:crucible_steel"
WALL_BLOCK = "gt6:crucible_steel_wall"

steps = []

# ------------------------------------------------- A: the wandclick ONE-CLICK story
steps += [
    phase("A: the player-feed wandclick forms the WHOLE crucible in one click"),
    Step(f"setblock {C} {CONTROLLER_BLOCK}", expect="Changed the block"),
    # the production dispatch fed the REAL clicked coordinate (the useOn path) —
    # 24 walls placed, paid and bound, formed, from exactly 24 stock
    # (the report's toShortString() form is "x, y, z" — commas)
    Step(f"gt6multiblock wandclick {C} 24",
         expect=f"GT6 multiblock wandclick at {C_X}, {C_Y}, {C_Z} (controller {C_X}, {C_Y}, {C_Z}): formed=true okay=true stock 24 -> 0"),
    # the y+2 ring — AIR under the pre-P28 click window (the far ring was the
    # missing eighth), a WALL under the one-click ruling
    Step(f"execute if block {C_X + 1} {C_Y + 2} {C_Z - 1} {WALL_BLOCK} run time query daytime",
         expect="The time is"),
    # the y+0 ring (the controller's own layer)
    Step(f"execute if block {C_X - 1} {C_Y} {C_Z + 1} {WALL_BLOCK} run time query daytime",
         expect="The time is"),
    # the structure verdict: every wall linked to THIS controller
    Step(f"gt6multiblock crucible {C} check", expect="okay=true linked_parts=24/24"),
    # idempotency: a click on the FORMED structure consumes nothing (spare stock 1
    # rides through untouched)
    Step(f"gt6multiblock wandclick {C} 1",
         expect=f"GT6 multiblock wandclick at {C_X}, {C_Y}, {C_Z} (controller {C_X}, {C_Y}, {C_Z}): formed=true okay=true stock 1 -> 1"),
]

# ------------------------------------------------- B: the form arm regression anchor
steps += [
    phase("B: the form arm stays the regression anchor (untouched by this card)"),
    Step(f"setblock {F} {CONTROLLER_BLOCK}", expect="Changed the block"),
    Step(f"gt6multiblock form {F} 24", expect="formed=true okay=true stock 24 -> 0"),
    Step(f"gt6multiblock crucible {F} check", expect="okay=true linked_parts=24/24"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit band restore"),
    Step(f"fill {F_X - 12} {C_Y - 2} {C_Z - 3} {F_X + 2} {C_Y + 4} {C_Z + 3} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p28-builder-wand-oneclick",
    slug="p28wandclick",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26170, 26180),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
