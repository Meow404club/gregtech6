#!/usr/bin/env python3
"""p29-w1-fermenter — the Fermenter chain (task p29-w1-eu-hu-families ACCEPTANCE ④:
the HU window + the burning-box supply): the SINGLE-variant HU machine (:1654,
StainlessSteel, the upstream explicit NBT_INPUT 32 / MIN 16 / MAX 64 window == the
TIER_INPUTS[0] row, RM.Fermenter, energy bottom face), on the JSON-poured fermenter.json
smoke row (1x wheat + water 1000 → 1x sugar, eUt 16, duration 128 — the RM.java:688
EUt=16 semantics):

  budget units(16×128×1, 10000, 10000, T) = 2048 ticks.

  the HU window arms (the direct inject rig):
    <16 拒: 40× 8 HU packets never cross mInputMin 16 → progress stays 0/0;
    32 跑通: 8× 32 HU ticks → progress=256/2048 (the mid-window baseline);
    >64 拒: an 80 HU packet exceeds the :493 packet ceiling 64 → overcharge →
    the block is GONE (the STAT FAILED absence proof, the p28_fe_inbound arm-D form).

  the burning-box 供热贯通 arm (the p13 boiler firebox adjacency form): the brick
  box UNDER the fermenter (top-face emit into the bottom-face SBIT_D acceptor), fueled
  + ignited, is the ONLY energy source from here on — every poll is the READ-ONLY check
  (no injected energy), so the completion out[0]=1x sugar is delivered entirely by the
  16 HU/t box train over the full 2048-tick budget (~110 s real; poll 300 covers it).

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_fermenter.py
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

# the fresh z=200 band: the box at (384,64), the main fermenter ABOVE it (the firebox
# adjacency form), the throwaway two blocks east (the overcharge arm)
SITE_BOX = gt6world.Site(384, 64, 200)
SITE_FER = gt6world.Site(384, 65, 200, dy=1)
SITE_FER2 = gt6world.Site(386, 65, 200, dy=1)
BOX, FER, FER2 = F(SITE_BOX), F(SITE_FER), F(SITE_FER2)

steps = [
    phase("A: the window arms — place, stock the smoke row, <16 拒 then 32 跑通"),
    Step(f"gt6machine fermenter place {FER}", expect="GT6 fermenter placed"),
    Step(f"gt6machine fermenter check {FER}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine fermenter input 1 {FER}",
         expect="GT6 fermenter input: 1x wheat into slot 0",
         node_expects={"1.21.1": "GT6 fermenter input: 1x minecraft:wheat into slot 0"}),
    # the tank-in mask is the ROTATED SBIT_B|SBIT_L pair (the p14 dryer chain's east-form:
    # "up" is the tank-OUT face and REJECTS -- the fill-side arm doubles as the mask check)
    Step(f"gt6machine fermenter fluid fill east minecraft:water 1000 {FER}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    # <16 拒: 8 HU packets are dead below the window floor
    Step(f"gt6machine fermenter inject 40 8 {FER}", expect="progress=0/0"),
    Step(f"gt6machine fermenter check {FER}", expect="progress=0/0"),
    # 32 跑通: the mid-window train binds the row and advances (budget 2048)
    Step(f"gt6machine fermenter inject 8 32 {FER}", expect="progress=256/2048"),

    phase("B: >64 拒 — the overcharge arm on the throwaway (the :493 ceiling 64)"),
    Step(f"gt6machine fermenter place {FER2}", expect="GT6 fermenter placed"),
    # used=1: the :495 overcharge return convention (the packet counts consumed, the
    # machine explodes) -- the honest face is the STAT FAILED absence below
    Step(f"gt6machine fermenter inject 1 80 {FER2}", expect="used=1 progress=0/0"),
    Step(f"gt6machine fermenter check {FER2}", expect="STAT FAILED", allow_failed=True),

    phase("C: the burning-box 供热贯通 — the box is the ONLY energy for the full budget"),
    Step(f"gt6burner place {BOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner fuel {BOX} minecraft:coal 4", expect="minecraft:coal x4"),
    Step(f"gt6burner ignite {BOX}", expect="burning=true"),
    # every poll is the READ-ONLY check: the 16 HU/t train covers the remaining 1792 ticks
    Step(f"gt6machine fermenter check {FER}",
         expect="out[0]=1x sugar",
         node_expects={"1.21.1": "out[0]=1x minecraft:sugar"},
         poll=300.0),
    Step(f"gt6machine fermenter check {FER}", expect="progress=0/0"),
    Step(f"gt6burner stat {BOX}", expect="burning=true"),

    phase("D: teardown — extinguish, restore the band (no global state was touched)"),
    Step(f"gt6burner extinguish {BOX}", expect="burning=false"),
    Step("fill 383 62 199 387 68 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w1-fermenter",
    slug="p29w1fermenter",
    sites=gt6world.declare_sites(SITE_BOX, SITE_FER, SITE_FER2),
    preferred_ports=(26225, 26235),      # the card-D pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
