#!/usr/bin/env python3
"""gui-basicmachine-fluids — the shared basic-machine panel fluid-seat live chain
(task gui-basicmachine-fluids ACCEPTANCE ③, the gui_distill_tower open-smoke
shape).

The fluid seats ride the shared MUI panel (GTBasicMachineMUI.buildPanel) — the open
chain is client-boundary, so a dedicated server cannot render the seats. The chain pins
the SERVER half on a fluid-banked shared-panel machine, in-册 verified: the FERMENTER
(id 22003, RM fluids 1/1, menu=null → the GT6MuiMachine.tryOpen MUI carrier,
GTMachines.java fermenter row + GT6RecipeMaps.FERMENTER):

  A the machine places and reports its menu-less MUI-carrier form (data=-2, the
    mui_row_dispatch verdict shape — the vanilla-menu byte-identical face untouched);
  B the new /gt6machine open arm: the fake-player dispatch reports the sanctioned SKIP
    verdict — a real exception / unknown-command here is a red. The open arm's server
    half (BE buildUI → GTBasicMachineMUI.buildPanel over hostOf, the fluid seats +
    named bm_fluid_* sync keys) is pinned offline by GT6BasicMachineMUIPanelTest.

passes=2 is the idempotency proof.

Run:  python3 tools/rcon/chains/gui_bmach.py --node 1.20.1-forge
      python3 tools/rcon/chains/gui_bmach.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fermenter column rides the P29 band convention: a fresh z=484 band — the roster
# census tops out at z=475 (the fusion band 452..478) and the ore_overlay
# floating stage starts at z=508, so 480..490 is census-clear.
FERMENTER = "386 65 484"      # facing north by place default

steps = [
    phase("A: the fluid-banked shared-panel carrier — place + the menu-less verdict"),
    Step(f"gt6machine fermenter place {FERMENTER}", expect="GT6 fermenter placed"),
    Step(f"gt6machine fermenter check {FERMENTER}", expect="data=-2"),

    phase("B: the open smoke arm — the MUI open chain server half (the sanctioned SKIP)"),
    Step(f"gt6machine fermenter open {FERMENTER}", expect="open SKIP for the fake player"),

    phase("E: teardown — the explicit band restore"),
    Step("fill 383 62 482 389 75 487 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="gui_bmach",
    slug="guibmach",
    sites=gt6world.declare_sites(
        gt6world.Site(386, 64, 484, dx=1, dy=2, dz=1),   # the fermenter + arm column
    ),
    preferred_ports=(26652, 26662),      # this card's pinned rcon/query pair (after oreoverlay 26632/26642)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
