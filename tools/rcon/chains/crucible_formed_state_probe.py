#!/usr/bin/env python3
"""crucible-formed-state-probe — the formed-vanish server-side fork probe (task
crucible-formed-vanish-probe round 5).

The user reports: the LARGE crucible, once FORMED, has its body visually vanish
(interior see-through) while the wall panels stay visible (F3 design:4, Jade
passthrough works). This chain settles the SERVER side of the verdict fork with
blockstate + BE evidence around the forming write:

  1. the controller alone (unformed)      -> `data get block` dump (BE NBT)
  2. the 24 walls placed (unformed)       -> identity+design0 probe + BE dump
  3. `gt6multiblock crucible check` forms -> the structure verdict
  4. controller identity: still gt6:crucible_steel WITH formed=true
     (the fork: a wall/air replacement here = a server-side form bug)
  5. wall identity: gt6:crucible_steel_wall[design=4] (the formed repaint)
  6. the hollow centre column still air (nothing bled into the cavity)
  7. post-form `data get block` dumps of the controller + one wall (BE NBT:
     mStructureOkay, te_name, design sync)

Non-judged steps (expect omitted) are pure evidence dumps — their full body
prints into the run log. The replacement detector (step 4b) is allow_failed:
either outcome is evidence, neither is a chain red.

Run:  python3 tools/rcon/chains/crucible_formed_state_probe.py --node 1.20.1-forge
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

# a fresh site, disjoint from crucible_multiblock's SITE_B (430,65,124) and
# SITE_C (440,65,124) bands — this probe owns x 459..461
B = "460 64 124"                                          # the controller cell (y+0 centre)
WALL_E = "461 64 124"                                     # the y+0 east wall cell (dump target)

CONTROLLER_BLOCK = "gt6:crucible_steel"     # the GT6Crucibles controller row
WALL_BLOCK = "gt6:crucible_steel_wall"      # the steel wall part row


def wall_setblock_steps(controller, expect="Changed the block"):
    """The 24 walls in three rings around `controller` (the y0/y1/y2 rings, the
    centre column open). The ring order matches the pattern declaration."""
    steps = []
    bx, by, bz = controller.split()
    for ring in range(3):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx == 0 and dz == 0:
                    continue
                steps.append(Step(
                    f"setblock {int(bx) + dx} {int(by) + ring} {int(bz) + dz} {WALL_BLOCK}",
                    expect=expect))
    return steps


steps = []

# ------------------------------------------------- A: unformed state evidence
steps += [
    phase("A: the unformed controller + walls — the pre-form state evidence"),
    Step(f"setblock {B} {CONTROLLER_BLOCK}", expect="Changed the block"),
    # the unformed controller BE dump (expect omitted = pure evidence print)
    Step(f"data get block {B}"),
]
steps += wall_setblock_steps(B)
steps += [
    # the unformed wall lands design 0 (the placed default state)
    Step(f"execute if block {WALL_E} {WALL_BLOCK}[design=0] run time query daytime",
         expect="The time is"),
    Step(f"data get block {WALL_E}"),
]

# ------------------------------------------------- B: form + the identity fork
steps += [
    phase("B: form the structure — the server-side identity fork"),
    Step(f"gt6multiblock crucible {B} check", expect="okay=true linked_parts=24/24"),
    # the controller is still the controller, now with the FORMED bit set
    Step(f"execute if block {B} {CONTROLLER_BLOCK}[formed=true] run time query daytime",
         expect="The time is"),
    # REPLACEMENT DETECTOR (allow_failed — both outcomes are evidence): if this
    # PASSes, the form write replaced the controller cell with a wall block.
    Step(f"execute if block {B} {WALL_BLOCK} run time query daytime",
         expect="The time is", allow_failed=True),
    # the formed wall repaint: design 4 in the blockstate
    Step(f"execute if block {WALL_E} {WALL_BLOCK}[design=4] run time query daytime",
         expect="The time is"),
    # the hollow centre column above the controller stays air (y+1; y+2 too)
    Step(f"execute if block 460 65 124 minecraft:air run time query daytime",
         expect="The time is"),
    Step(f"execute if block 460 66 124 minecraft:air run time query daytime",
         expect="The time is"),
    # the post-form BE dumps (mStructureOkay / the wall's mDesign + BE id)
    Step(f"data get block {B}"),
    Step(f"data get block {WALL_E}"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit band restore"),
    Step("fill 456 62 120 464 68 128 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="crucible-formed-state-probe",
    slug="crucible-form-state",
    sites=gt6world.declare_sites(gt6world.Site(460, 65, 124, dx=1, dy=2, dz=1)),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
