#!/usr/bin/env python3
"""bmach_teardown — the basic-machine family /fill teardown dist chain
(task basicmachine-onremove-distleak ACCEPTANCE, the misattribution-correction
face).

The family (Shredder/Crusher/Lathe = TileEntityBasicMachine rows) is loaded
with feed (non-empty slots arm the GTEntityBlock drop walk), then the band
fill-restore runs. The RED world (the un-fixed GTEntityBlock.beCanDrop
getMethod walk resolving the GT6MuiMachine createScreen default's ModularScreen
return type) dies mid-command — the judged fill step FAILS and the server log
carries "Attempted to load class brachy/modularui/screen/ModularScreen for
invalid dist DEDICATED_SERVER" plus the zombie-BE "invalid for ticking" WARN
(gt6_rs_session_1201-forge_kinbe-e2e87ecb.log:41215-41216 precedent). The GREEN
world (the mui-fill-teardown-distleak root fix) fills and judges clean. The
chain is the red→green 对照 instrument for that seam, family-scoped.

passes=2 is the idempotency proof (and the recovery proof on the green world).

Run:  python3 tools/rcon/sweep.py --mode session --group bmach_teardown
      (per-chain boot: python3 tools/rcon/chains/bmach_teardown.py --node 1.20.1-forge)
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# a fresh z=494 band — the roster census tops at the gui_bmach z=484 band and the
# ore_overlay floating stage starts at z=508; 492..500 is census-clear and the
# x column (355..363) is disjoint from gui_bmach's x383..389 with margin.
SHREDDER = "357 65 494"   # Loader_Recipes_Vanilla.java:692 feed (cobblestone)
CRUSHER = "359 65 494"    # Loader_Recipes_Handlers.java:72 feed (first gem)
LATHE = "361 65 494"      # Loader_Recipes_Vanilla.java:524 feed (stone)

steps = [
    phase("A: the family placed and LOADED — non-empty slots arm the drop walk"),
    Step(f"gt6machine shredder place {SHREDDER}", expect="GT6 shredder placed at 357, 65, 494"),
    Step(f"gt6machine crusher place {CRUSHER}", expect="GT6 crusher placed at 359, 65, 494"),
    Step(f"gt6machine lathe place {LATHE}", expect="GT6 lathe placed at 361, 65, 494"),
    Step(f"gt6machine shredder input 8 {SHREDDER}", expect="into slot 0"),
    Step(f"gt6machine crusher input 8 {CRUSHER}", expect="into slot 0"),
    Step(f"gt6machine lathe input 8 {LATHE}", expect="into slot 0"),

    phase("B: teardown — the judged fill restore (the dist face: green fills, red dies)"),
    Step("fill 355 62 492 363 67 496 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="bmach_teardown",
    slug="bmachtd",
    sites=gt6world.declare_sites(
        gt6world.Site(359, 64, 494, dx=4, dy=2, dz=2),   # the three-machine row + margins
    ),
    preferred_ports=(26842, 26852),      # after 26822/26832 (the newest roster pair)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
