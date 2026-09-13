#!/usr/bin/env python3
"""p29-w2-loom — the kinetic Loom chain (task p29-w2-hu-tu-piggyback, group
p29_w2_hu_tu; the RU 4-ladder :1412-1415, the SAME-MAP cross-proof with the W1
ElectricLoom — both carriers ride the ONE GT6RecipeMaps.LOOM instance and the ONE
loom.json smoke row the p29_w1_electricloom chain drives on the EU side):

  arm A — the REAL RU SOURCE (the gt6rcon diesel->axle fixture, the p29_w1_rollingmill
    rig verbatim): diesel_engine_bronze[facing=east] burns 16 RU/t DC through one
    wood-small axle onto loom T1 (default facing north -> the WEST approach is the
    machine's local L|R energy pair — the :1412 SBIT_L|SBIT_R BOTH-side faces). The
    poured loom.json row (4 string -> 1 white_wool, eUt 16, duration 128, budget 2048)
    completes on grid packets, the 16 RU/t train meeting the 16/t drain 1:1.
  arm B — the inject rig: 8 RU packets sit below mInputMin 16 (the wall), then
    32x 64 RU ticks complete the row in one command -> out white_wool.
  arm C — the T2 window pin (minIn=64 recIn=128 maxIn=256).

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_loom.py
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

ENGINE = gt6world.Site(434, 64, 280)   # the diesel engine (facing east)
AXLE = gt6world.Site(435, 64, 280)     # the wood-small axle (axis=x)
LOOM_A = gt6world.Site(436, 64, 280)   # loom T1 (default facing north; fed from the west)
LOOM_B = gt6world.Site(438, 65, 280)   # loom T1b (the inject arm)
LOOM_T2 = gt6world.Site(440, 65, 280)  # loom T2 (the window pin)

DIESEL = "gt6:diesel_engine_bronze"
AXLE_B = "gt6:axle_wood_treated_small"

WOOL_OUT = {"1.20.1": "out[0]=1x white_wool", "1.21.1": "out[0]=1x minecraft:white_wool"}

steps = [
    phase("A: the real RU source — diesel 16 RU/t -> axle -> loom T1, the SAME loom.json row the W1 ElectricLoom drives"),
    Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {F(AXLE)} {AXLE_B}[axis=x]", expect="Changed the block"),
    Step(f"setblock {F(LOOM_A)} gt6:loom", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine loom input 4 {F(LOOM_A)}",
         expect="GT6 loom input: 4x string into slot 0",
         node_expects={"1.21.1": "GT6 loom input: 4x minecraft:string into slot 0"}),
    Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled 160 L"),
    # the grid-fed completion: the 16 RU/t train meets the row's 16/t drain 1:1 (budget 2048 = 128 ticks)
    Step(f"gt6machine loom check {F(LOOM_A)}", expect=WOOL_OUT, node_expects=WOOL_OUT, poll=45.0),
    Step(f"gt6engine stat {F(ENGINE)}", expect="rate=16 RU/t (DC constant-sign)"),

    phase("B: the inject rig — the 16-floor wall then the full 2048 bar in one command"),
    Step(f"gt6machine loom place {F(LOOM_B)}", expect="GT6 loom placed at 438, 65, 280"),
    Step(f"gt6machine loom input 4 {F(LOOM_B)}",
         expect="GT6 loom input: 4x string into slot 0",
         node_expects={"1.21.1": "GT6 loom input: 4x minecraft:string into slot 0"}),
    Step(f"gt6machine loom inject 8 8 {F(LOOM_B)}", expect="progress=0/0"),
    Step(f"gt6machine loom inject 32 64 {F(LOOM_B)}",
         expect="outputs=[1x white_wool; ]",
         node_expects={"1.21.1": "outputs=[1x minecraft:white_wool; ]"}),

    phase("C: the T2 window pin"),
    Step(f"gt6machine loom_t2 place {F(LOOM_T2)}", expect="GT6 loom_t2 placed at 440, 65, 280"),
    Step(f"gt6machine loom_t2 check {F(LOOM_T2)}", expect="minIn=64 recIn=128 maxIn=256"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 433 62 278 441 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w2-loom p29_w2_hu_tu",
    slug="p29w2loom",
    sites=gt6world.declare_sites(ENGINE, AXLE, LOOM_A, LOOM_B, LOOM_T2),
    preferred_ports=(26347, 26357),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
