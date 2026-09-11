#!/usr/bin/env python3
"""p27-builder-wand-form-fix — the crucible anchored-walk live acceptance chain.

The bug (user report 2026-09-10 "the wand builds only half a multiblock"): the
crucible is controller-anchored (upstream MultiTileEntityCrucible.java:118-122 —
no facing anywhere), but its pattern walk read the raw mFacing, and the inherited
setFacingFromPlacement overwrites the ctor's mFacing=0 pin with the player's
horizontal look direction on EVERY player placement. For every live facing
(HORIZONTAL_FACING 2..5) the shared checker resolved each declared cell as
cell - OFF[facing] (GTMultiBlockPattern.java:355-357, the side-centred Coke-Oven
convention) — the whole form landed one block off the machine, the wand scaffolded
a displaced half-box and the far column of parts refused the wand relay.

Live reproduction of the trigger state: a plain `setblock` keeps the ctor pin
(mFacing=0 — which is why the p26 chains never saw the displacement), so the
player-placed state is forced the way the server itself carries it — the BE NBT
`/data merge block <C> {facing:5b}` (TileEntityBase10MultiBlockBase.load reads
NBT_FACING = "facing"), the same value setFacingFromPlacement would have written
for a player looking east.

Chain semantics (the form arm is the changed feed —
GTMultiBlockCommand.form rides patternWalkFacing() since the fix):

  A the anchored form: bare controller at C with the forced mFacing=5, then
    `gt6multiblock form C 24` (the SET scaffold, aClickedAt=null = the whole
    structure in one shot) → formed=true okay=true stock 24 -> 0. The wall
    POSITIONS are the discriminator:
    - the probe cell (C+1, y0, C-1) is inside the ANCHORED box (x∈[cx-1,cx+1])
      but OUTSIDE the pre-fix displaced box (facing 5 EAST: x∈[cx-2,cx]) —
      `execute if block` must see the wall (the hard-assert idiom: the feedback
      command rides `execute if ... run time query daytime`, since `say` never
      reaches the RCON peer);
    - the pre-fix-only column x=cx-2 must be UNTOUCHED air — the displaced walk
      would have walled it.
  B the check arm readback: `gt6multiblock crucible C check` → okay=true
    linked_parts=24/24 (every scaffolded wall linked to THIS controller).
  C teardown: the explicit bbox restore (the declared-site cleanup is the
    structural backstop).

Run:  python3 tools/rcon/chains/p27_builder_wand_form_fix.py --node 1.20.1-forge
      python3 tools/rcon/chains/p27_builder_wand_form_fix.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

C_X, C_Y, C_Z = 460, 64, 124
C = f"{C_X} {C_Y} {C_Z}"
SITE = gt6world.Site(C_X, C_Y + 1, C_Z, dx=2, dy=2, dz=2)  # covers x 458..462 (the 458 probe column), y 63..67

CONTROLLER_BLOCK = "gt6:crucible_steel"
WALL_BLOCK = "gt6:crucible_steel_wall"

steps = []

# ------------------------------------------------- A: the anchored form at a forced player facing
steps += [
    phase("A: the forced player-facing controller (mFacing=5) forms ANCHORED — the walls stand around the controller"),
    Step(f"setblock {C} {CONTROLLER_BLOCK}", expect="Changed the block"),
    # the player-placed state, the way the server itself carries it: setFacingFromPlacement
    # writes the look direction into NBT_FACING; merge the same byte (5 = EAST).
    Step(f"data merge block {C} {{facing:5b}}", expect="Modified block data"),
    # the SET scaffold from stocked fake-player stock — the changed feed (patternWalkFacing)
    Step(f"gt6multiblock form {C} 24", expect="formed=true okay=true stock 24 -> 0"),
    # the anchored-only probe cell: a wall under the fix, air under the displaced walk
    Step(f"execute if block {C_X + 1} {C_Y} {C_Z - 1} {WALL_BLOCK} run time query daytime",
         expect="The time is"),
    # the displaced-only column: untouched air under the fix, a wall under the pre-fix walk
    Step(f"execute if block {C_X - 2} {C_Y} {C_Z} minecraft:air run time query daytime",
         expect="The time is"),
]

# ------------------------------------------------- B: the check arm readback
steps += [
    phase("B: the check arm agrees — every scaffolded wall linked"),
    Step(f"gt6multiblock crucible {C} check", expect="okay=true linked_parts=24/24"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit band restore"),
    Step(f"fill {C_X - 3} {C_Y - 2} {C_Z - 3} {C_X + 3} {C_Y + 3} {C_Z + 3} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p27-builder-wand-form-fix",
    slug="p27wandfix",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26150, 26160),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
