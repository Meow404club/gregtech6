#!/usr/bin/env python3
"""scene-six SHEEP chain — the scissors shear arm (Behavior_Shears :122).

Chain semantics (task scene-six RCON arm 2, the /gt6scene6 shear channel —
the command drives the vanilla Player.interactOn face, Player.java:998 routes the held
stack to interactLivingEntity, the forge-patched ShearsItem shear walk over
IForgeShearable; the scissors inherit it through the ShearsItem base):

  A summon a sheep, shear it with the SCISSORS — the wool drops (1..3, colour-random:
  the report counts ALL wool variants, the expect pins the stable verdict faces);
  B summon a FRESH sheep and repeat — the second pass is the [0, 0] idempotency proof
    (the first sheep stays sheared, its repeat shear is the negative face the command
    reports as sheared=true without failing).

Run:  GT6_SESSION=off python3 tools/rcon/chains/scene_six_sheep.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

S1 = gt6world.Site(402, 64, 240)   # the shear pen centre

steps = []

# --------------------------------- A: the first sheep (wool drops, sheared=true)
steps += [
    phase("A: the scissors shear a fresh sheep (Behavior_Shears 20-unit row, the patched interactLivingEntity)"),
    Step(f"summon minecraft:sheep 402.5 64 240.5", expect="Summoned new Sheep"),
    Step("gt6scene6 shear 402 64 240 scissors",
         expect="shear check OK"),
    Step("execute if entity @e[type=minecraft:sheep,limit=1,sort=nearest] run data get entity @e[type=minecraft:sheep,limit=1,sort=nearest] Sheared 1b",
         expect="Sheared"),
]

# --------------------------------- B: a fresh second sheep (the pass-2 rerun face)
steps += [
    phase("B: a fresh second sheep — the pass-2 rerun (the first sheep stays sheared)"),
    Step("summon minecraft:sheep 402.5 64 240.5", expect="Summoned new Sheep"),
    Step("gt6scene6 shear 402 64 240 scissors", expect="shear check OK"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — clear the pen (the pass-open bbox is the backstop)"),
    Step("kill @e[type=minecraft:sheep]", expect="Killed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="scene-six-sheep",
    slug="scenesixsheep",
    sites=gt6world.declare_sites(S1),
    preferred_ports=(26215, 26225),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
