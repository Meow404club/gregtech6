#!/usr/bin/env python3
"""p29-w4-f1-chemicals (cracker leg) — the Steam Cracker TRUE-ROW chain (task
p29-w4-f1-chemicals, RCON group p29_w4_chemicals; the W2 smoke row is GONE — the
shipped steamcracking.json true rows ride, Loader_Recipes_Chem.java:368-369):

  A the STEAM_CRACKING run: steam 1000 + propane 100 fill through the up face
    (the two input tanks, fillableAny first-containing-then-empty), ONE inject of
    64 ticks @ 16 HU (the row budget |16 x 64| = 1024 <= the T1 window) completes
    the row; the four-gas output ladder lands in the output tanks and the fluid
    draw arm samples them one by one — gt6:hydrogen 2, gt6:methane 27,
    gt6:ethylene 42, gt6:propylene 19.

  B the CATALYTIC gate: hydrogen 100 + ethanol 100 fill and the feed-slot item
    (the W2 charcoal literal) is NOT the row's gt6:dust_platinum catalyst — the
    run finds NO row, progress stays 0/0. The catalyst slot face itself is pinned
    offline (GT6ChemicalRowsPourTest.catalyticRowsCarryTheCatalystSlotAndInOuts);
    the gt6machine input command carries a per-machine literal feed, so the
    positive catalytic run is a declared RCON gap (the gt6:dust_platinum feed
    slot wiring is a follow-up command-surface item).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_chemicals_cracker.py
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

Z = 320
SC1 = gt6world.Site(448, 65, Z, dz=1)  # steamcracker T1
CC1 = gt6world.Site(456, 65, Z, dz=1)  # catalyticcracker T1 (the gate arm)

steps = [
    phase("A: the T1 steam cracker — the TRUE row (steam + propane -> the four-gas ladder)"),
    Step(f"gt6machine steamcracker place {F(SC1)}", expect="GT6 steamcracker placed at 448, 65, 320"),
    Step(f"gt6machine steamcracker fluid fill up gt6:steam 1000 {F(SC1)}",
         expect="filled 1000/1000 L of gt6:steam (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine steamcracker fluid fill up gt6:propane 100 {F(SC1)}",
         expect="filled 100/100 L of gt6:propane (ACCEPTED), input tanks hold 1100 L"),
    Step(f"gt6machine steamcracker check {F(SC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    # the row budget |16 x 64| = 1024 fits the 64-tick inject — the row COMPLETES inside the
    # command (the four outputs land in the output bank instantly, so progress reads the
    # post-run idle 0/0; the bank volume 90 L in the draw arm is the completion evidence)
    Step(f"gt6machine steamcracker inject 64 16 {F(SC1)}",
         expect="inject ticks=64 size=16 finalSize=null used=64"),
    # the draw arm rides the TANK_SIDE_OUT face (SBIT_R|SBIT_B — facing north: south = BACK);
    # the resource-less drain pulls the FIRST non-empty tank each call = the row's
    # fluidOutputs order (hydrogen 2, methane 27, ethylene 42, propylene 19)
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 2/1000 L of gt6:hydrogen (ACCEPTED)"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 27/1000 L of gt6:methane (ACCEPTED)"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 42/1000 L of gt6:ethylene (ACCEPTED)"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 19/1000 L of gt6:propylene (ACCEPTED)"),

    phase("B: the catalytic gate — the feed literal is now the TRUE row's Pt dust (p33)"),
    Step(f"gt6machine catalyticcracker place {F(CC1)}", expect="GT6 catalyticcracker placed at 456, 65, 320"),
    # task p33-cracker-machines closed the p29 gap: the catalytic feed literal is now the
    # TRUE row's item input gt6:dust_platinum (Loader_Recipes_Chem.java:373-376). The row
    # still cannot complete here (this arm pins the feed face, not the HU run — the
    # p29_w2_catalytic_cracker chain owns the positive completion run), so progress=0/0
    # stays the verdict face.
    Step(f"gt6machine catalyticcracker input 1 {F(CC1)}",
         expect="1x dust_platinum into slot 0",
         node_expects={"1.21.1": "1x gt6:dust_platinum into slot 0"}),
    Step(f"gt6machine catalyticcracker fluid fill up gt6:hydrogen 100 {F(CC1)}",
         expect="filled 100/100 L of gt6:hydrogen (ACCEPTED)"),
    Step(f"gt6machine catalyticcracker fluid fill up gt6:ethanol 100 {F(CC1)}",
         expect="filled 100/100 L of gt6:ethanol (ACCEPTED), input tanks hold 200 L"),
    Step(f"gt6machine catalyticcracker inject 64 16 {F(CC1)}", expect="progress=0/0"),

    phase("C: teardown — restore the band"),
    Step(f"fill 446 62 {Z - 2} 458 68 {Z + 3} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p29_w4_chemicals matches this member
    name="p29-w4-chemicals-cracker p29_w4_chemicals",
    slug="p29w4chemicalscracker",
    sites=gt6world.declare_sites(SC1, CC1),
    preferred_ports=(26343, 26353),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
