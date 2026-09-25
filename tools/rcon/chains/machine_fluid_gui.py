#!/usr/bin/env python3
"""machine-fluid-gui — the dryer menu-open smoke chain (declarative framework).

Chain semantics (task machine-fluid-gui ACCEPTANCE, "place dryer → 打开 menu 臂"):
the gt6:dryer MenuType is bound to the four Dryer rows, so the /gt6machine check/run
menu probe (GTMachineCommand.menuOrNull — the FakePlayerFactory createMenu path,
TileEntityBasicMachine.createMenu :1472-1474) now CONSTRUCTS the real menu instead of
degrading to the data=-2 menu-less marker. Per tier:

  place → fluid stat (the RM 流 1,3 对位 live: the census carries out[2] — the 3-tank
          output bank the display slots pair with) →
  check (data=-1: the fresh-placed idle menu opens — the gt6:dryer MenuType resolves
          through the row supplier and createMenu builds GTBasicMachineMenu, whose
          progress ContainerData reads -1 at mMaxProgress=0) →
  input 1 (the stub feed) → fluid fill east/south minecraft:water 700+300 (the rotated
          SBIT_B|SBIT_L tank-in mask ACCEPTED — the paired input tank pins at 1000 L) →
  inject 40 at the tier mInputMax (HU carrier live; the 8/16/32/64-parallel
          parallelDuration batch cannot complete inside 40 ticks) →
  check (data=0: the menu opens AGAIN post-starvation — progress reset to 0 on the
          recipe-aware dial 2048/8192/32768/73728 normalizes to units(0,max)=0).

The offline half of the slot-tank pairing (the :267-268 display geometry, the
FluidDisplaySlot.tank identity, the inertness face) is GT6MachineFluidDisplayTest; this
chain proves the live registration + the createMenu open path for all four tiers.

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox cleanup
restores the sites between passes. Run with GT6_SESSION=off (the pinned per-chain
ports; the session model ignores chain.preferred_ports and parallel coders on the same
node segment would cross-fire — the aqua-fluids lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/machine_fluid_gui.py
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

# The four dryer sites, x 200-260 — clear of the P13/P14/side_io sites (x 100-160)
# and the aqua-fluids chain (x 300). The placed default facing is north, so the rotated
# tank-in faces are world east (relative left) + world south (relative back).
T1 = gt6world.Site(200, 64, 200, dx=1, dy=2, dz=1)
T2 = gt6world.Site(220, 64, 200, dx=1, dy=2, dz=1)
T3 = gt6world.Site(240, 64, 200, dx=1, dy=2, dz=1)
T4 = gt6world.Site(260, 64, 200, dx=1, dy=2, dz=1)

# (literal, pos, maxIn, inject_progress) — maxIn doubles as the inject packet size
# (the size-then-pos branch of the inject tree); the progress columns are the
# driven-ticks x mInputMax arithmetic observed by the dryer-family chain.
TIERS = [
    ("dryer",    T1, 64,   "used=40 progress=512/2048"),
    ("dryer_t2", T2, 256,  "used=40 progress=2048/8192"),
    ("dryer_t3", T3, 1024, "used=40 progress=8192/32768"),
    ("dryer_t4", T4, 4096, "used=40 progress=32768/73728"),
]

steps = []

# ------------------------------------------- the four tiers, one protocol each
for literal, pos, maxin, inject_progress in TIERS:
    steps += [
        phase(f"{literal}: place, the 流1,3 tank bank census, the menu-open arms"),
        Step(f"gt6machine {literal} place {F(pos)}", expect=f"GT6 {literal} placed"),
        # the RM 流 1,3 对位 live: the output census carries out[2] — the 3-tank bank
        # the :268 display slots pair with (GTBasicMachineMenu FluidDisplaySlot.tank)
        Step(f"gt6machine {literal} fluid stat {F(pos)}", expect="out[2]=0 L of nothing"),
        # the menu-open arm, fresh idle: menuOrNull now CONSTRUCTS gt6:dryer's menu
        # (data=-2 was the menu-less carrier marker; idle progress reads -1)
        Step(f"gt6machine {literal} check {F(pos)}", expect="data=-1"),
        # feed + water: the stub item (never starts alone) and the paired input tank
        Step(f"gt6machine {literal} input 1 {F(pos)}", expect="bricks into slot 0"),
        Step(f"gt6machine {literal} fluid fill east minecraft:water 700 {F(pos)}",
             expect="filled 700/700 L of minecraft:water (ACCEPTED)"),
        Step(f"gt6machine {literal} fluid fill south minecraft:water 300 {F(pos)}",
             expect="filled 300/300 L of minecraft:water (ACCEPTED)"),
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect="in[0]=1000 L of minecraft:water"),
        # the HU carrier live at the tier band (the Drying row starts; the batch cannot
        # complete inside 40 ticks at parallelDuration x parallel)
        Step(f"gt6machine {literal} inject 40 {maxin} {F(pos)}", expect=inject_progress),
        # the menu-open arm, post-starvation: the batch reset leaves progress=0 on the
        # recipe-aware dial — the reopened menu's ContainerData normalizes to 0
        Step(f"gt6machine {literal} check {F(pos)}", expect="data=0"),
        Step(f"gt6machine {literal} check {F(pos)}", expect="progress=0/"),
        Step(f"gt6machine {literal} check {F(pos)}", expect="active=false"),
    ]

# ------------------------------------------- teardown
steps += [
    phase("C: teardown — the explicit restore over the four dryer sites (the pass-open bbox is the backstop)"),
    Step("fill 199 61 199 261 68 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="machine-fluid-gui",
    slug="mfgui",
    sites=gt6world.declare_sites(T1, T2, T3, T4),
    preferred_ports=(25774, 25784),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
