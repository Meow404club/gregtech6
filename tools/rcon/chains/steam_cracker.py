#!/usr/bin/env python3
"""steam-cracker — the Steam Cracker chain (task hu-tu-piggyback, group
hu_tu; re-pinned by rcon-chain-repair onto the TRUE row — the W2 smoke row
(coal + water -> charcoal) was replaced by the f1 chemicals card with the
upstream verbatim rows, Loader_Recipes_Chem.java:368: steam 1000 + propane 100 ->
hydrogen 2 + methane 27 + ethylene 42 + propylene 19, eUt 16 x duration 64):

  T1 budget units = 4096; the row budget |16 x 64| = 1024 completes inside ONE
  inject of 64 ticks @ 16 HU (the outputs land in the output bank, so the inject
  report reads the post-run idle — the fluid draw arm is the completion evidence).
  the HU window ladder (TIER_INPUTS per row, the ACCEPTANCE-① per-row pin):
    T1 16/32/64, T2 64/128/256, T3 256/512/1024, T4 1024/2048/4096.
  the dead-packet wall: 40x 8 HU packets never cross mInputMin 16 -> 0/0.

The catalytic_cracker chain runs the mirror re-pin on the twin map (the :373 row).
passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/steam_cracker.py
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

SC1 = gt6world.Site(384, 65, 280, dz=1)  # steamcracker T1
SC2 = gt6world.Site(386, 65, 280)        # T2
SC3 = gt6world.Site(388, 65, 280)        # T3
SC4 = gt6world.Site(390, 65, 280)        # T4

steps = [
    phase("A: the T1 cracker — the TRUE row (steam + propane -> the four-gas ladder), the 8-HU wall, the row budget in one command"),
    Step(f"gt6machine steamcracker place {F(SC1)}", expect="GT6 steamcracker placed at 384, 65, 280"),
    # the f1 chemicals card replaced the smoke row with the upstream true rows
    # (2153094c, Loader_Recipes_Chem.java:368 verbatim) — the chain re-pins onto the :368
    # row; the two-tank fill shapes are the chemicals_cracker proven forms.
    Step(f"gt6machine steamcracker fluid fill up gt6:steam 1000 {F(SC1)}",
         expect="filled 1000/1000 L of gt6:steam (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine steamcracker fluid fill up gt6:propane 100 {F(SC1)}",
         expect="filled 100/100 L of gt6:propane (ACCEPTED), input tanks hold 1100 L"),
    Step(f"gt6machine steamcracker check {F(SC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine steamcracker inject 40 8 {F(SC1)}", expect="progress=0/0"),
    # the row budget |16 x 64| = 1024 fits the 64-tick inject — the row COMPLETES inside
    # the command (progress reads the post-run idle 0/0; the draw arm is the completion
    # evidence, pulling the FIRST non-empty output tank each call = the row's order)
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

    phase("B: the T2-T4 window ladder (the TIER_INPUTS per-row pin)"),
    Step(f"gt6machine steamcracker_t2 place {F(SC2)}", expect="GT6 steamcracker_t2 placed at 386, 65, 280"),
    Step(f"gt6machine steamcracker_t2 check {F(SC2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine steamcracker_t3 place {F(SC3)}", expect="GT6 steamcracker_t3 placed at 388, 65, 280"),
    Step(f"gt6machine steamcracker_t3 check {F(SC3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine steamcracker_t4 place {F(SC4)}", expect="GT6 steamcracker_t4 placed at 390, 65, 280"),
    Step(f"gt6machine steamcracker_t4 check {F(SC4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 278 391 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group hu_tu matches this member
    # substring and joins the whole cluster (the mixer_pair form)
    name="steam-cracker hu_tu",
    slug="steamcracker",
    sites=gt6world.declare_sites(SC1, SC2, SC3, SC4),
    preferred_ports=(26341, 26351),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
