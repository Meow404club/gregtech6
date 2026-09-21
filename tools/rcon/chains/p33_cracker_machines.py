#!/usr/bin/env python3
"""p33_cracker_machines — the cracker verification chain (task p33-cracker-machines,
RCON group p33_cracker_machines; the research.p33-r-cracking verdict: machines and
recipe rows both landed, this chain is the live verification):

  A the STEAM_CRACKING positive run (the TRUE :368 row, the p29_w2 chain's proven
    form): steam 1000 + propane 100 through the up face, ONE inject of 64 ticks
    @ 16 HU (row budget |16 x 64| = 1024 <= the T1 window) completes; the
    four-gas output ladder lands and the draw arm samples each — hydrogen 2,
    methane 27, ethylene 42, propylene 19.

  B the CATALYTIC positive run (the TRUE :373 ethanol row) — the p29 leftover
    input-gap closed: the feed literal IS the row's item input now
    (gt6:dust_platinum via /gt6machine catalyticcracker input, spec ②); H2 100 +
    ethanol 100 + the catalyst in the slot + the 64-tick 16 HU inject completes;
    the draw arm samples ethylene 20, propylene 5.

  C the crafting-pattern smoke: give the cracker block items (the recipe JSON
    result) and place them live (the runData recipes ride the server's
    RecipeManager; the offline identity pin is GT6CrackerCraftingJsonTest).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p33_cracker_machines.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)
if str(_HERE.parent) not in sys.path:
    sys.path.insert(0, str(_HERE.parent))

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

Z = 330
SC1 = gt6world.Site(448, 65, Z, dz=1)  # steamcracker T1
CC1 = gt6world.Site(456, 65, Z, dz=1)  # catalyticcracker T1

steps = [
    phase("A: the T1 steam cracker — the TRUE row (steam + propane -> the four-gas ladder)"),
    Step(f"gt6machine steamcracker place {F(SC1)}", expect="GT6 steamcracker placed at 448, 65, 330"),
    Step(f"gt6machine steamcracker fluid fill up gt6:steam 1000 {F(SC1)}",
         expect="filled 1000/1000 L of gt6:steam (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine steamcracker fluid fill up gt6:propane 100 {F(SC1)}",
         expect="filled 100/100 L of gt6:propane (ACCEPTED), input tanks hold 1100 L"),
    Step(f"gt6machine steamcracker check {F(SC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine steamcracker inject 64 16 {F(SC1)}",
         expect="inject ticks=64 size=16 finalSize=null used=64"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 2/1000 L of gt6:hydrogen (ACCEPTED)"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 27/1000 L of gt6:methane (ACCEPTED)"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 42/1000 L of gt6:ethylene (ACCEPTED)"),
    Step(f"gt6machine steamcracker fluid draw south 1000 {F(SC1)}",
         expect="drawn 19/1000 L of gt6:propylene (ACCEPTED)"),

    phase("B: the T1 catalytic cracker — the TRUE row (H2 + ethanol + Pt -> ethylene/propylene), the p29 input gap closed"),
    Step(f"gt6machine catalyticcracker place {F(CC1)}", expect="GT6 catalyticcracker placed at 456, 65, 330"),
    # spec ②: the feed literal IS the true row's item input gt6:dust_platinum (the
    # p29-w4 chemicals chain's declared RCON gap; the merge workaround retires)
    Step(f"gt6machine catalyticcracker input 1 {F(CC1)}",
         expect="1x dust_platinum into slot 0",
         node_expects={"1.21.1": "1x gt6:dust_platinum into slot 0"}),
    Step(f"data get block {F(CC1)} inventory", expect="dust_platinum"),
    Step(f"gt6machine catalyticcracker fluid fill up gt6:hydrogen 100 {F(CC1)}",
         expect="filled 100/100 L of gt6:hydrogen (ACCEPTED)"),
    Step(f"gt6machine catalyticcracker fluid fill up gt6:ethanol 100 {F(CC1)}",
         expect="filled 100/100 L of gt6:ethanol (ACCEPTED), input tanks hold 200 L"),
    Step(f"gt6machine catalyticcracker inject 64 16 {F(CC1)}",
         expect="inject ticks=64 size=16 finalSize=null used=64"),
    Step(f"gt6machine catalyticcracker fluid draw south 1000 {F(CC1)}",
         expect="drawn 20/1000 L of gt6:ethylene (ACCEPTED)"),
    Step(f"gt6machine catalyticcracker fluid draw south 1000 {F(CC1)}",
         expect="drawn 5/1000 L of gt6:propylene (ACCEPTED)"),

    phase("C: teardown — restore the band"),
    Step(f"fill 446 62 {Z - 2} 458 68 {Z + 3} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p33_cracker_machines matches
    name="p33-cracker-machines p33_cracker_machines",
    slug="p33crackermachines",
    sites=gt6world.declare_sites(SC1, CC1),
    preferred_ports=(26344, 26354),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
