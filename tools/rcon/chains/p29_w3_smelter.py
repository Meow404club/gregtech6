#!/usr/bin/env python3
"""p29-w3-smelter — the Smelter HU 4-ladder live acceptance chain (task
p29-w3-heat-smelter, the p29_w2_steam_cracker shape minus the fluid-input leg —
the ice row is ITEM-ONLY, its output is the FLUID face: ice 1 -> water 1000 L,
eut 16 / duration 2000, the Loader_Recipes_Chem.java:501 transcription):

  the window ladder (TIER_INPUTS per row, the ACCEPTANCE-① per-row pin):
    T1 16/32/64, T2 64/128/256, T3 256/512/1024, T4 1024/2048/4096 — and the
    check report pins parallel=1000 parallelDuration=true on every tier.
  the dead-packet wall: 40x 8 HU packets never cross mInputMin 16 -> 0/0.
  the run: units(16 x 2000 x 1, 10000, 10000, T) = 32000 bar -> 500 ticks @ 64
    -> the fluid stat pins out[0]=1000 L of minecraft:water (the fluid output
    face the W3 card drives instead of the item slots).

passes=2 is the idempotency proof. teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w3_smelter.py
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

SM1 = gt6world.Site(396, 65, 304, dz=1)  # smelter T1
SM2 = gt6world.Site(400, 65, 304)        # T2
SM3 = gt6world.Site(404, 65, 304)        # T3
SM4 = gt6world.Site(408, 65, 304)        # T4

steps = [
    phase("A: the T1 smelter — the ice row, the 8-HU wall, then the 32000 bar in one command"),
    Step(f"gt6machine smelter place {F(SM1)}", expect="GT6 smelter placed at 396, 65, 304"),
    Step(f"gt6machine smelter input 1 {F(SM1)}",
         expect="GT6 smelter input: 1x ice into slot 0",
         node_expects={"1.21.1": "GT6 smelter input: 1x minecraft:ice into slot 0"}),
    Step(f"gt6machine smelter check {F(SM1)}",
         expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine smelter check {F(SM1)}",
         expect="parallel=1000 parallelDuration=true"),
    Step(f"gt6machine smelter inject 40 8 {F(SM1)}", expect="progress=0/0"),
    Step(f"gt6machine smelter inject 500 64 {F(SM1)}", expect="progress=0/0, energy=0"),
    Step(f"gt6machine smelter fluid stat {F(SM1)}", expect="out[0]=1000 L of minecraft:water"),

    phase("B: the T2-T4 window ladder (the TIER_INPUTS per-row pin)"),
    Step(f"gt6machine smelter_t2 place {F(SM2)}", expect="GT6 smelter_t2 placed at 400, 65, 304"),
    Step(f"gt6machine smelter_t2 check {F(SM2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine smelter_t3 place {F(SM3)}", expect="GT6 smelter_t3 placed at 404, 65, 304"),
    Step(f"gt6machine smelter_t3 check {F(SM3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"gt6machine smelter_t4 place {F(SM4)}", expect="GT6 smelter_t4 placed at 408, 65, 304"),
    Step(f"gt6machine smelter_t4 check {F(SM4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 394 62 302 410 68 306 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p29_w3_heat_smelter matches this
    # member substring and joins the whole cluster (the p29_w2_hu_tu form)
    name="p29-w3-smelter p29_w3_heat_smelter",
    slug="p29w3smelter",
    sites=gt6world.declare_sites(SM1, SM2, SM3, SM4),
    preferred_ports=(26401, 26411),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
