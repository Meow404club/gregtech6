#!/usr/bin/env python3
"""p21-drying-food-fluids — the food-family fluids + Drying rows live chain (declarative framework).

Chain semantics (task p21-drying-food-fluids ACCEPTANCE — "/gt6tank fill 逐新流体臂 +
p19_drying.py 同形 dryer round-trip，两遍 [0,0]"):

  A the four per-fluid registration arms (the p16_aqua_fluids shape): one wood-barrel
    arm per new fluid — setblock gt6:barrel_wood → gt6tank fill 1000 L (ACCEPTED)
    → gt6tank stat: 1000/16000 L still held after the sleep window (NO meltdown)
    at the declared FluidType temperature. All four sit at 300 K (the three
    Loader_Fluids.java:461-463 FL.create(..., 1, 1000, 300) rows plus the honest
    FluidType.java:925 default of the external-name "sap", FL.java:250) — under the
    340 K wood ceiling (GTBarrelCommand.WOOD_MELTING_POINT), so the wood barrel is
    the uniform carrier.

  B the four dryer round-trips (the p19_drying shape — dryer = TileEntityBasicMachine
    x DRYING, no new command arms): one arm per food row, filling through the LIVE
    registration and pinning the verbatim arithmetic of Loader_Recipes_Food.java:655-658
    on the machine stat:
      sap 250 → DistW 100 + Sugar dur200 (:655, the FL.Sap.exists() guard row),
      maplesap 250 → DistW 100 + Sugar dur200 (:656),
      reedwater 200 → DistW 50 + Sugar dur100 (:657),
      cactuswater 200 → DistW 50, ZL_IS dur100 (:658, no item output).
    The sugar legs pin on the below hopper's Items NBT (gt6:dust_sugar); the :658 arm
    pins on the stat + draw alone (nothing may appear — the offline census carries
    the ZL_IS shape). All four are EUt 16; the injected 64/t tick budget covers the
    verbatim durations with the stall backstop.

passes=2 is the idempotency proof (the [0,0] of this chain); the sites bbox cleanup
between passes re-airs every barrel and the rig.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p21_drying_food.py
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

# The four barrel arm sites — one wood barrel each, 12 blocks apart on the x=370 band,
# clear of every earlier chain's bands (the neighbours are 368 and 372).
FLUIDS = [
    "sap"        ,  # FL.java:250 "sap", no FL.create — the honest FluidType.java:925 default
    "maplesap"   ,  # FL.java:252 / Loader_Fluids.java:463
    "reedwater"  ,  # FL.java:233 / Loader_Fluids.java:461
    "cactuswater",  # FL.java:234 / Loader_Fluids.java:462
]

BARREL_SITES = [gt6world.Site(370, 64, 20 + 12 * i, dx=1, dy=1, dz=1) for i in range(len(FLUIDS))]

# The dryer rig: the dryer (378,64,20), the feed hopper above (65) and the extraction
# hopper below (63) — dy=2 covers 62..66. Band x=378, clear of the neighbours (376/380).
DRYER_SITE = gt6world.Site(378, 64, 20, dx=1, dy=2, dz=1)
DRYER = F(DRYER_SITE)          # 378 64 20
HOPPER_TOP = "378 65 20"       # stacked on the dryer, facing down into it
HOPPER_BOT = "378 63 20"       # under the dryer: pulls the item outputs only

steps = []

# ------------------------------------------------- A: the per-fluid registration arms
steps += [phase("A: per-fluid arms — wood barrel, fill 1000 L, stat the 300 K hold (no meltdown)")]

for _site, _path in zip(BARREL_SITES, FLUIDS):
    _pos = F(_site)
    steps += [
        Step(f"setblock {_pos} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank fill {_pos} gt6:{_path} 1000",
             expect=f"filled 1000/1000 L of gt6:{_path} (ACCEPTED)", sleep=2),
        # after a tick window the tank still holds (no 340 K meltdown) at the declared 300 K
        Step(f"gt6tank stat {_pos}",
             expect=f"1000/16000 L of gt6:{_path}, temperature 300 K, melting point 340"),
    ]

# ------------------------------------------------- B: the dryer rig
steps += [
    phase("B: the rig — dryer placed, no fuel, empty banks, both hoppers, carrier arms"),
    Step(f"gt6machine dryer place {DRYER}", expect="GT6 dryer placed"),
    # data=-1 not -2: the gt6:dryer menu supplier is bound since p16-machine-fluid-gui
    Step(f"gt6machine dryer check {DRYER}", expect="data=-1"),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
    Step(f"setblock {HOPPER_TOP} hopper[facing=down]", expect="Changed the block"),
    Step(f"setblock {HOPPER_BOT} hopper[facing=down]", expect="Changed the block"),
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="in[0]=0 L of nothing; out[0]=0 L of nothing"),
    # the 250/200 L rows fit the 1000 L default input tank — no tank_capacity merge;
    # item_sides_in = SBIT_U|SBIT_A (66): the top feed stays, the bottom face loses the
    # input slot and keeps the outputs (the p19 rig shape, kept for parity)
    Step(f"data merge block {DRYER} {{item_sides_in:66b}}", expect="Modified block data"),
]

# ------------------------------------------------- C: the four food-row round-trips
# (fluid, fill litres, DistW litres, duration ticks, has-sugar-output)
FOOD_ARMS = [
    ("sap"        , 250, 100, 200, True ),  # :655 — the :654 FL.Sap.exists() guard row, live over a registered fluid
    ("maplesap"   , 250, 100, 200, True ),  # :656
    ("reedwater"  , 200,  50, 100, True ),  # :657
    ("cactuswater", 200,  50, 100, False),  # :658 — ZL_IS: no item output
]

for _fluid, _inp, _out, _dur, _sugar in FOOD_ARMS:
    _arms = [
        phase(f"C: {_fluid} {_inp} -> {_out}{' + sugar' if _sugar else ' (ZL_IS)'} — fill, full-budget drive, arithmetic"
              + (", sugar pin" if _sugar else ", no-item shape")),
        Step(f"gt6machine dryer fluid fill east gt6:{_fluid} {_inp} {DRYER}",
             expect=f"filled {_inp}/{_inp} L of gt6:{_fluid} (ACCEPTED)"),
        # progress is one tick per driven tick (EUt 16, injecting 64/t) — the tick count
        # covers the verbatim duration plus the idle-stall backstop
        Step(f"gt6machine dryer inject {_dur + 800} 64 {DRYER}", expect="used="),
        Step(f"gt6machine dryer fluid stat {DRYER}",
             expect=f"in[0]=0 L of nothing; out[0]={_out} L of gt6:distilled_water"),
        Step(f"gt6machine dryer fluid draw up {_out} {DRYER}",
             expect=f"drawn {_out}/{_out} L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
    ]
    if _sugar:
        _arms.append(Step(f"data get block {HOPPER_BOT} Items", expect="dust_sugar", sleep=2.0))
    else:
        # the ZL_IS row: the dryer drains the DistW and produces NOTHING — the check pin
        # (running=false, banks empty) is the no-item-output live face
        _arms.append(Step(f"gt6machine dryer check {DRYER}", expect="running=false"))
    steps += _arms

# ------------------------------------------------- teardown
steps += [
    phase("D: teardown — the explicit restore over the rig (the pass-open bbox is the backstop)"),
    Step("fill 367 61 17 383 67 59 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p21-drying-food-fluids",
    slug="p21dryingfood",
    sites=gt6world.declare_sites(*BARREL_SITES, DRYER_SITE),
    preferred_ports=(26104, 26114),      # this card's pinned rcon/query pair (P21 segment, after p21paintable 26103/26113)
    response_timeout=240.0,              # the inject loops run server-side
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
