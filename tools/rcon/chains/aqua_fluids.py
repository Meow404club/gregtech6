#!/usr/bin/env python3
"""aqua-fluids — the six aqua fluids live chain (declarative framework).

Chain semantics (task aqua-fluids ACCEPTANCE — "RCON /gt6tank fill 逐新流体臂
两遍 [0,0]"): one wood-barrel arm per new fluid, each arm

  setblock gt6:barrel_wood → gt6tank fill <pos> gt6:<fluid> 1000 (ACCEPTED)
  → gt6tank stat: still 1000/16000 L after the sleep window (NO meltdown) at the
    declared FluidType temperature (the live census verdict: spdew/mnwtr 300 K =
    Loader_Fluids.java:362/:371, water_geothermal 320 K = :373, water_boiling 300 K
    = the honest FluidType.java:925 default, hot_water 333 K / cold_water 288 K =
    the FL.java:115/:114 // 60°C / // 15°C annotations).

All six sit under the 340 K wood ceiling (GTBarrelCommand.WOOD_MELTING_POINT), so
the wood barrel is the uniform carrier and the post-sleep stat doubles as the
meltdown-negative proof. passes=2 is the idempotency proof (the [0,0] of this
chain); the sites bbox cleanup between passes re-airs every barrel.

Run:  python3 tools/rcon/chains/aqua_fluids.py
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

# The six arm sites — one wood barrel each, 12 blocks apart on the x=300 band,
# clear of every earlier chain's sites (the used bands end at x=284/z=214).
FLUIDS = [
    ("spdew"           , 300),  # Loader_Fluids.java:362 FL.create(..., 1, 1000, 300)
    ("mnwtr"           , 300),  # Loader_Fluids.java:371 FL.create(..., 1, 1000, 300)
    ("water_geothermal", 320),  # Loader_Fluids.java:373 FL.create(..., 1, 1000, 320)
    ("water_boiling"   , 300),  # no upstream definition — the FluidType.java:925 default
    ("hot_water"       , 333),  # FL.java:115 // 60°C
    ("cold_water"      , 288),  # FL.java:114 // 15°C
]

SITES = [gt6world.Site(300, 64, 20 + 12 * i, dx=1, dy=1, dz=1) for i in range(len(FLUIDS))]

steps = [phase("A: per-fluid arms — wood barrel, fill 1000 L, stat the temperature + no-meltdown hold")]

for _site, (_path, _temp) in zip(SITES, FLUIDS):
    _pos = F(_site)
    steps += [
        Step(f"setblock {_pos} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank fill {_pos} gt6:{_path} 1000",
             expect=f"filled 1000/1000 L of gt6:{_path} (ACCEPTED)", sleep=2),
        # after a tick window the tank still holds (no 340 K meltdown) at the declared K
        Step(f"gt6tank stat {_pos}",
             expect=f"1000/16000 L of gt6:{_path}, temperature {_temp} K, melting point 340"),
    ]

steps += [
    phase("B: teardown probe — the server survived the six arms"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="aqua-fluids",
    slug="af",
    sites=gt6world.declare_sites(*SITES),
    preferred_ports=(25772, 25782),      # this card's pinned rcon/query pair (P16 segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
