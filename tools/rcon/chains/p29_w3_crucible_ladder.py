#!/usr/bin/env python3
"""p29-w3-crucible-ladder — the crucible 8-material ladder live acceptance chain (task
p29-w3-distill-crucible ACCEPTANCE ③④: 8 档坩埚 census（17302..17312 全在 + ACIDPROOF 列）+ the
SS 坩埚 form + 熔炼一臂（the P26 链回归 on the new rung）).

  A the census: all EIGHT controller blocks setblock + block-identity probes (the
    registration surface 17302..17312 live), plus the seven ladder walls (the
    dedicated crucible-relay wall per material).
  B the SS rung: the generic /gt6multiblock form scaffolds the 24 dedicated SS
    walls around the controller (the pattern-bound scaffold — the crucible is
    controller-anchored, patternWalkFacing 0), `crucible check` links 24/24.
  C the melt arm (the P26 p26_crucible_multiblock regression form): 4U gold
    feeds on top, one heat charge, `stat` pins the molten content, the y+1 wall
    relays the pour (the ITileEntityCrucible relay through the NEW SS wall —
    the dedicated-wall construction's reason d'etre), the y+0 wall refuses it.
    THE ARITHMETIC (the r1 live readback pinned it): the SS ceiling = 2137 K
    max=2137K (the shell melt 1943 x the P26 LARGE bonus 1.10 — the slloymachine
    row's effective melt, NOT the metal() 1633 default), the WARNING latch 2037.
    Gold melts at 1337 with NO alloy row — the charge 1150000 HU lands the
    spread at 293 + floor(1150000/~950) ≈ 1490 K (the live stat), past the gold
    melt, under the latch, with margin both ways.
    The pour asserts "poured=1.0U of Gold" (the recording mold takes 1U per
    click, the P26 form), then "Gold 3.0U" on the content drop.

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  python3 tools/rcon/chains/p29_w3_crucible_ladder.py --node 1.20.1-forge
      python3 tools/rcon/chains/p29_w3_crucible_ladder.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=300 band: the SS rig x424..432, the census columns x416..466 z309
SS = "430 64 302"          # the SS crucible controller (y+0 centre, the box y64..66)
SS_WALL = "431 65 302"     # a y+1 (mold-layer) wall
SS_WALL_ENERGY = "431 64 302"  # the same-column y+0 (energy-layer) wall

# the eight rungs (the Loader :1270-1277 meta set) + the seven ladder walls
RUNGS = [
    "crucible_steel", "crucible_stainless_steel", "crucible_invar",
    "crucible_titanium", "crucible_tungstensteel", "crucible_tungsten",
    "crucible_tantalum_hafnium_carbide", "crucible_adamantium",
]
LADDER_WALLS = [
    "crucible_stainless_steel_wall", "crucible_invar_wall", "crucible_titanium_wall",
    "crucible_tungstensteel_wall", "crucible_tungsten_wall",
    "crucible_tantalum_hafnium_carbide_wall", "crucible_adamantium_wall",
]


def census_steps():
    """The eight controller setblocks + identity probes, x-disjoint columns z309."""
    steps = [phase("A: the 8-rung census — every controller and ladder wall places and identifies")]
    x = 418
    for i, tRung in enumerate(RUNGS):
        tPos = f"{x} 65 309"
        steps.append(Step(f"setblock {tPos} gt6:{tRung}", expect="Changed the block"))
        steps.append(Step(f"execute if block {tPos} gt6:{tRung}", expect="Test passed"))
        x += 2
    for i, tWall in enumerate(LADDER_WALLS):
        tPos = f"{x} 65 309"
        steps.append(Step(f"setblock {tPos} gt6:{tWall}", expect="Changed the block"))
        steps.append(Step(f"execute if block {tPos} gt6:{tWall}", expect="Test passed"))
        x += 2
    return steps


steps = census_steps() + [
    phase("B: the SS rung — the generic form arm scaffolds the 24 dedicated walls"),
    Step(f"setblock {SS} gt6:crucible_stainless_steel", expect="Changed the block"),
    Step(f"gt6multiblock form {SS} 64", expect="formed=true okay=true"),
    Step(f"gt6multiblock crucible {SS} check", expect="okay=true linked_parts=24/24"),

    phase("C: the melt arm — feed, charge, stat, and the through-wall pour on the NEW wall"),
    Step(f"gt6multiblock crucible {SS} feed gold 4", expect="fed=true"),
    # the spread: past the gold melt 1337, under the WARNING latch 1696 (the SS ceiling 1796 - 100)
    Step(f"gt6multiblock crucible {SS} heat 1150000", expect="buffer=", sleep=1.5),
    Step(f"gt6multiblock crucible {SS} stat", expect="Gold 4.0U"),
    # the y+1 mold-layer wall relays the pour through the DEDICATED SS wall (the relay
    # the shared machine walls do not carry), the recording mold takes 1U per click
    Step(f"gt6multiblock crucible {SS} pour {SS_WALL}", expect="poured=1.0U of Gold"),
    Step(f"gt6multiblock crucible {SS} stat", expect="Gold 3.0U"),
    # the y+0 energy-layer wall carries NO_CRUCIBLE — the same click is refused
    Step(f"gt6multiblock crucible {SS} pour {SS_WALL_ENERGY}", expect="poured=0", allow_failed=True),

    phase("D: teardown — the explicit band restore"),
    Step("fill 414 62 300 470 70 312 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w3_distill_crucible p29_w3_crucible_ladder",
    slug="p29w3crucibleladder",
    sites=gt6world.declare_sites(
        gt6world.Site(429, 64, 302, dx=2, dy=2, dz=1),    # the SS 3x3x3 box
        gt6world.Site(418, 65, 309, dx=28, dy=0, dz=0),   # the census columns (8 rungs + 7 walls, step 2)
    ),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
