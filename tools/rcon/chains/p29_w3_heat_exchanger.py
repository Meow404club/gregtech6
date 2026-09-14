#!/usr/bin/env python3
"""p29-w3-heat-exchanger — the Large Heat Exchanger live acceptance chain (task
p29-w3-heat-smelter; the p13_large_boiler boiler-stack form — the HU fixture
IS the boiler):

  A THE FORM: /gt6heatexchanger place -> the 16-part shell by plain setblocks
    (8 dense_wall_tungsten y65 ring ONLY_ITEM_FLUID_ENERGY_IN + 8
    heat_transmitter y66 NOTHING + the centre wall y66) -> stat formed=true.
  B THE FUEL DOOR: /gt6heatexchanger fill 500000 pushes gt6:hot_water through
    the CAPABILITY door — the ACCEPTED echo proves the poured fuels_hot row
    (the :223 isFuel gate is the poured row, live).
  C THE BOILER STACK (the 18101 proxy face, ACCEPTANCE ③): the SS large
    boiler anchored at y68 — its base 3x3 transmitters sit at y67, ONE layer
    above the HEX's transmitter ring, exactly the six-of-eight y+2 emission
    targets the :152-159 loop pushes (the two z=305 ring corners hit air, the
    upstream decrement-regardless face) — the relay chain HEX controller ->
    transmitter part -> boiler controller books HU, the boiler converts to
    steam and the stat pins barometer>=1 (the 16384 HU/t emission rate over
    ~61 ticks of 500 kL fuel = 1M HU = 12500 conversions = 2M steam).
    fillraw is the acceptance-channel direct-tank fill twin.

passes=2 is the idempotency proof. teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_heat_exchanger.py
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

# the HEX stack: controller (430, 65, 306); ring y65/y66; emission layer y67;
# the boiler POS (430, 68, 306) facing north — the anchor sits one cell in front
# (430, 68, 307), so the base 3x3 of transmitters lands at y67 centred on
# (430, 307): five of the eight y+2 emission targets overlap the boiler base
# (the three z=305 ring corners hit air — the decrement-regardless face).
HEX = gt6world.Site(430, 65, 306, dx=2, dy=6, dz=4)
LB = gt6world.Site(430, 68, 307, dx=3, dy=4, dz=4)

HEX_P = "430 65 306"
LB_P = "430 68 306"
VARIANT = "large_boiler_stainless_steel"

WALL = "gt6:dense_wall_tungsten"
TX = "gt6:heat_transmitter"

# the 16 shell cells relative to the controller (dx, dy, dz), the check order
RING_Y0 = [(dx, 0, dz) for dz in (-1, 0, 1) for dx in (-1, 0, 1) if (dx, dz) != (0, 0)]
RING_Y1 = [(dx, 1, dz) for dz in (-1, 0, 1) for dx in (-1, 0, 1) if (dx, dz) != (0, 0)]

shell_steps = []
for dx, dy, dz in RING_Y0:
    shell_steps.append(Step(f"setblock {430 + dx} {65 + dy} {306 + dz} {WALL}", expect="Changed the block"))
for dx, dy, dz in RING_Y1:
    shell_steps.append(Step(f"setblock {430 + dx} {66 + dy} {306 + dz} {TX}", expect="Changed the block"))
shell_steps.append(Step(f"setblock 430 66 306 {WALL}", expect="Changed the block"))  # :106 the centre wall

steps = [
    phase("A: the form — controller + the 16-part shell, then the formed verdict"),
    Step(f"gt6heatexchanger place {HEX_P}", expect="GT6 large heat exchanger placed at 430, 65, 306"),
] + shell_steps + [
    Step(f"gt6heatexchanger stat {HEX_P}", expect="formed=true rate=16384 efficiency=10000", sleep=2.0),

    phase("B: the fuel door — hot_water through the capability gate (the poured fuels_hot row, live)"),
    Step(f"gt6heatexchanger fill {HEX_P} 500000",
         expect="filled 500000/500000 L of gt6:hot_water (ACCEPTED)"),

    phase("C: the boiler stack — the 18101 proxy relay to the SS large boiler"),
    Step(f"gt6multiblock boiler place {LB_P} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {LB_P} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {LB_P} {VARIANT}", expect="formed", sleep=1.0),
    Step(f"gt6multiblock boiler fill {LB_P} 128000", expect="ACCEPTED"),
    Step(f"gt6multiblock boiler stat {LB_P}", expect="formed=true", sleep=2.0),
    Step(f"gt6heatexchanger stat {HEX_P}", expect="offered=16384"),
    Step(f"gt6multiblock boiler stat {LB_P}", expect="barometer=1", sleep=6.0),
    # the raw twin: the acceptance-channel direct-tank fill (the boiler distw form)
    Step(f"gt6heatexchanger fillraw {HEX_P} 100000", expect="filled 100000/100000 L of gt6:hot_water (ACCEPTED)"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 426 62 302 434 76 312 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w3-heat-exchanger p29_w3_heat_smelter",
    slug="p29w3heatexchanger",
    sites=gt6world.declare_sites(HEX, LB),
    preferred_ports=(26400, 26410),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
