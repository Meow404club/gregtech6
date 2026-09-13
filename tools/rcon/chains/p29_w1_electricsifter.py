#!/usr/bin/env python3
"""p29-w1-electricsifter — the ElectricSifter chain (task p29-w1-eu-hu-families
ACCEPTANCE ③: the electric family EATS THE EXISTING SIFTING ROW — the SHARED
RM.Sifting map, the :224 grass row0 the kinetic sifter chain drives, no JSON for
SIFTING; the :1518-1522 EU row, efficiency 5000, NO parallel):

  T1 budget units(16×144×1, 5000, 10000, T) = 4608 → 72 ticks @ 64 EU/tick —
  the raw divisor visible as progress=64/4608 after the first injected tick,
  and the deterministic completion out[0]=1x coarse_dirt at exactly 72 ticks
  (the kinetic T1 4-parallel process finishes the same per-process budget in
  36 ticks — the 2:1 半速 face on the shared row).
  T2 window pin: minIn=64 recIn=128 maxIn=256.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_electricsifter.py
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

SITE_S1 = gt6world.Site(410, 64, 200, dy=1, dz=1)  # electricsifter T1
SITE_S2 = gt6world.Site(412, 64, 200, dy=1, dz=1)  # electricsifter T2
S1, S2 = F(SITE_S1), F(SITE_S2)


def dirt_expect(count):
    """The coarse-dirt verdict per leg (the deterministic full-certainty output)."""
    return {
        "cmd_expect": f"out[0]={count}x coarse_dirt",
        "node_expect": f"out[0]={count}x minecraft:coarse_dirt",
    }


d = dirt_expect(1)

steps = [
    phase("A: the T1 electric sifter — the SHARED :224 grass row at efficiency 5000, budget 4608"),
    Step(f"gt6machine electricsifter place {S1}", expect="GT6 electricsifter placed"),
    Step(f"gt6machine electricsifter input 1 {S1}",
         expect="GT6 electricsifter input: 1x grass_block into slot 0",
         node_expects={"1.21.1": "GT6 electricsifter input: 1x minecraft:grass_block into slot 0"}),
    # the raw 5000 divisor: budget 4608 = units(16 × 144, 5000, 10000, T) — 2× the kinetic 2304
    Step(f"gt6machine electricsifter check {S1}", expect="parallel=1 parallelDuration=false"),
    Step(f"gt6machine electricsifter inject 1 64 {S1}", expect="progress=64/4608"),
    # the deterministic completion at exactly 72 ticks (the remaining 71 + this poll tick)
    Step(f"gt6machine electricsifter inject 70 64 {S1}", expect="progress=4544/4608"),
    Step(f"gt6machine electricsifter inject 1 64 {S1}", expect="progress=0/0"),
    Step(f"gt6machine electricsifter check {S1}", expect=d["cmd_expect"], node_expects={"1.21.1": d["node_expect"]}),

    phase("B: the T2 window pin"),
    Step(f"gt6machine electricsifter_t2 place {S2}", expect="GT6 electricsifter_t2 placed"),
    Step(f"gt6machine electricsifter_t2 check {S2}", expect="minIn=64 recIn=128 maxIn=256"),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 409 62 199 413 67 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w1-electricsifter",
    slug="p29w1electricsifter",
    sites=gt6world.declare_sites(SITE_S1, SITE_S2),
    preferred_ports=(26222, 26232),      # the card-D pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
