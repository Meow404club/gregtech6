#!/usr/bin/env python3
"""catalytic-cracker — the Catalytic Cracker chain (task hu-tu-piggyback,
group hu_tu; re-pinned by rcon-chain-repair onto the TRUE row — the W2 smoke
row (charcoal + water -> coal) was replaced by the f1 chemicals card with the
upstream verbatim rows, Loader_Recipes_Chem.java:373: hydrogen 100 + ethanol 100 +
dust_platinum x1 -> ethylene 20 + propylene 5, eUt 16 x duration 64):

  T1 budget units = 4096; the row budget |16 x 64| = 1024 completes inside ONE
  inject of 64 ticks @ 16 HU (the fluid draw arm is the completion evidence).
  the T1 window + the full run in one command; the T2-T4 window ladder.

The catalyst gt6:dust_platinum rides the inventory merge (Size 4 = 1 input + 3
output slots, the CATALYTIC_CRACKING map constants GT6RecipeMaps.java:1047-1055)
— the feed literal carries the W2-era charcoal (GTMachineCommand :271), which
the f1 chain declared an RCON gap for the positive run; the merge closes
it. passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/catalytic_cracker.py
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

# the catalyst merge (the press key-shape ruling): the feed literal carries the
# W2-era charcoal (GTMachineCommand :271), the true row's gt6:dust_platinum rides the
# inventory merge — Size 4 = 1 input + 3 output slots (the CATALYTIC_CRACKING map
# constants IN-OUT-MIN-ITEM 1/3/0, GT6RecipeMaps.java:1047-1055; the handler shrink
# makes the fixed-slot report reads throw — the repair1 live calibration).
PT_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:dust_platinum",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:dust_platinum",count:1}}]}}}}',
}

CC1 = gt6world.Site(394, 65, 280, dz=1)  # catalyticcracker T1
CC2 = gt6world.Site(396, 65, 280)        # T2
CC3 = gt6world.Site(398, 65, 280)        # T3
CC4 = gt6world.Site(400, 65, 280)        # T4

steps = [
    phase("A: the T1 catalytic cracker — the TRUE row (H2 + ethanol + Pt -> ethylene/propylene), the 8-HU wall, the row budget in one command"),
    Step(f"gt6machine catalyticcracker place {F(CC1)}", expect="GT6 catalyticcracker placed at 394, 65, 280"),
    # the f1 chemicals card replaced the smoke row with the upstream true rows
    # (2153094c, Loader_Recipes_Chem.java:373 verbatim) — the chain re-pins onto the :373
    # ethanol row; the catalyst rides the inventory merge (the W4 gate arm proved the
    # charcoal feed literal finds NO row — the declared feed-slot RCON gap, closed here).
    Step(PT_MERGE["1.20.1"].format(p=F(CC1)), expect="Modified block data",
         node_cmds={"1.21.1": PT_MERGE["1.21.1"].format(p=F(CC1))}),
    Step(f"data get block {F(CC1)} inventory", expect="dust_platinum"),
    Step(f"gt6machine catalyticcracker fluid fill up gt6:hydrogen 100 {F(CC1)}",
         expect="filled 100/100 L of gt6:hydrogen (ACCEPTED)"),
    Step(f"gt6machine catalyticcracker fluid fill up gt6:ethanol 100 {F(CC1)}",
         expect="filled 100/100 L of gt6:ethanol (ACCEPTED), input tanks hold 200 L"),
    Step(f"gt6machine catalyticcracker check {F(CC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine catalyticcracker inject 40 8 {F(CC1)}", expect="progress=0/0"),
    # the row budget |16 x 64| = 1024 fits the 64-tick inject — the row COMPLETES inside
    # the command (progress reads the post-run idle 0/0; the draw arm is the completion
    # evidence, pulling the FIRST non-empty output tank each call = the row's order)
    Step(f"gt6machine catalyticcracker inject 64 16 {F(CC1)}",
         expect="inject ticks=64 size=16 finalSize=null used=64"),
    Step(f"gt6machine catalyticcracker fluid draw south 1000 {F(CC1)}",
         expect="drawn 20/1000 L of gt6:ethylene (ACCEPTED)"),
    Step(f"gt6machine catalyticcracker fluid draw south 1000 {F(CC1)}",
         expect="drawn 5/1000 L of gt6:propylene (ACCEPTED)"),

    phase("B: the T2-T4 window ladder (the TIER_INPUTS per-row pin)"),
    Step(f"gt6machine catalyticcracker_t2 place {F(CC2)}", expect="GT6 catalyticcracker_t2 placed at 396, 65, 280"),
    Step(f"gt6machine catalyticcracker_t2 check {F(CC2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine catalyticcracker_t3 place {F(CC3)}", expect="GT6 catalyticcracker_t3 placed at 398, 65, 280"),
    Step(f"gt6machine catalyticcracker_t3 check {F(CC3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine catalyticcracker_t4 place {F(CC4)}", expect="GT6 catalyticcracker_t4 placed at 400, 65, 280"),
    Step(f"gt6machine catalyticcracker_t4 check {F(CC4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 393 62 278 401 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="catalytic-cracker hu_tu",
    slug="catalyticcracker",
    sites=gt6world.declare_sites(CC1, CC2, CC3, CC4),
    preferred_ports=(26342, 26352),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
