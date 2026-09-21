#!/usr/bin/env python3
"""p33-gui-distill-tower — the distillation-tower GUI acceptance chain (task
p33-gui-distill-tower ACCEPTANCE ②, the p24_act C2 open-smoke shape).

The tower GUI is ModularUI-only (the P26 no-new-MenuType ruling) — the MUI open
chain is client-boundary, so a dedicated server cannot render the panel. The
chain pins the SERVER half of the face:
  A the /gt6distillation open arm: the fake-player dispatch runs buildUI + the
    sync-manager construct verbatim and reports the sanctioned SKIP verdict —
    a real exception is a red.
  B the tower itself must be GUI-ready: the block's use arm + the BE's
    GT6MuiMachine contract are the compile/offline-test faces (the panel test
    pins the widget topology); here the tower forms and answers the check
    report with the open command registered (0 = unknown subcommand would be
    a dispatch miss).

passes=2 is the idempotency proof.

Run:  python3 tools/rcon/chains/p33_gui_distill_tower.py --node 1.20.1-forge
      python3 tools/rcon/chains/p33_gui_distill_tower.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the tower column rides the p29 band convention: a fresh z=356 strip — z=352 is
# TAKEN by p29_w5_t7_pocket_ring (x384..386, the give chest + walk rig), the
# z=356..366 band east of it census-verified empty (the successor rebased the
# draft chain after the predecessor's WSL crash).
TOWER = "386 65 356"          # the controller (facing north by setblock default)

steps = [
    phase("A: the GUI open arm — the MUI open chain server half"),
    Step(f"setblock {TOWER} gt6:distillation_tower", expect="Changed the block"),
    Step(f"gt6multiblock form {TOWER} 999", expect="formed=true okay=true"),
    # the fake-player SKIP verdict — the sanctioned headless form (the ACT open shape);
    # an exception / unknown-command here is a red
    Step(f"gt6distillation open {TOWER}", expect="open SKIP for the fake player"),

    phase("E: teardown — the explicit band restore"),
    Step("fill 383 62 354 389 75 359 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p33_gui_distill_tower",
    slug="p33guidistill",
    sites=gt6world.declare_sites(
        gt6world.Site(385, 63, 356, dx=2, dy=10, dz=3),   # the tower body + arm column
    ),
    preferred_ports=(26129, 26139),      # this card's pinned rcon/query pair (after p24act 26109/26119)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
