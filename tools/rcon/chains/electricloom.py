#!/usr/bin/env python3
"""electricloom — the ElectricLoom chain (task eu-hu-families, the EU
RM.Loom consumer :1511-1515, efficiency 5000, NO parallel, top-in/bottom-out, the
BOTH-side energy faces SBIT_L|SBIT_R):

  T1 on the JSON-poured loom.json smoke row (4x string → 1x white_wool, eUt 16,
  duration 128 — the datapack-domain face this card ships):
    budget units(16×128×1, 5000, 10000, T) = 4096 → 64 ticks @ 64 EU/tick.
  the window arm: 8× 8 EU packets never cross mInputMin 16 → progress stays 0/0;
  the control: 64× 64 EU ticks complete the process → out[0]=1x white_wool.
  T2 window pin: minIn=64 recIn=128 maxIn=256.

The `input 4` feed walks the LIVE RM.Loom subset (the JSON smoke row's string face —
the firstPouredLoomInput walk). Item-name expects are PER-LEG via Step.node_expects
(the pattern_checker precedent). passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/electricloom.py
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

SITE_L1 = gt6world.Site(402, 64, 200, dy=1, dz=1)  # electricloom T1
SITE_L2 = gt6world.Site(404, 64, 200, dy=1, dz=1)  # electricloom T2
L1, L2 = F(SITE_L1), F(SITE_L2)

steps = [
    phase("A: the T1 loom — the JSON smoke row at efficiency 5000, budget 4096"),
    Step(f"gt6machine electricloom place {L1}", expect="GT6 electricloom placed"),
    # the report name is the BET id "loom" (getTileEntityName), not the block literal
    Step(f"gt6machine electricloom input 4 {L1}",
         expect="GT6 loom input: 4x string into slot 0",
         node_expects={"1.21.1": "GT6 loom input: 4x minecraft:string into slot 0"}),
    # the 8 EU wall: 8 EU packets below the LV min 16 are dead
    Step(f"gt6machine electricloom inject 8 8 {L1}", expect="progress=0/0"),
    Step(f"gt6machine electricloom check {L1}", expect="progress=0/0"),
    # the control: the full 64-tick budget in ONE command (the report reads the live
    # progress; across commands the idle natural ticks CONSTANT_ENERGY-reset mProgress
    # to 0 — the cross-command face is the PERSISTING output, not the counter)
    Step(f"gt6machine electricloom inject 64 64 {L1}", expect="progress=0/0"),
    Step(f"gt6machine electricloom check {L1}",
         expect="out[0]=1x white_wool",
         node_expects={"1.21.1": "out[0]=1x minecraft:white_wool"}),

    phase("B: the T2 window pin"),
    Step(f"gt6machine electricloom_t2 place {L2}", expect="GT6 electricloom_t2 placed"),
    Step(f"gt6machine electricloom_t2 check {L2}", expect="minIn=64 recIn=128 maxIn=256"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 401 62 199 405 67 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="electricloom",
    slug="electricloom",
    sites=gt6world.declare_sites(SITE_L1, SITE_L2),
    preferred_ports=(26221, 26231),      # the card-D pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
