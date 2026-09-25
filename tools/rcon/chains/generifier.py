#!/usr/bin/env python3
"""generifier — the TU Generifier chain (task hu-tu-piggyback, group
hu_tu; the TU single :1652 with NBT_PARALLEL 100 over the JSON-poured
generifier.json smoke row — 1 sand + water 10 -> 1 clay ball, eUt 16, duration 32):

  the parallel-100 断言 (ACCEPTANCE ③), two faces:
    the check report pins the ROW column parallel=100 (parallelDuration=false — the
    :770-771 energy-scaling arm, and as a TU carrier mMinEnergy stays mEUt);
    the bar itself: a FULL 64-stack of sand in the one input slot consumes to
    outputs=[64x clay_ball; ] in ONE 512-progress bar (units(16x32) = 512 -> 32 ticks
    @ 16 EU/tick) — impossible below parallel 64 (the :647 output-stack cap rides the
    same bar; the offline GT6HuTuPiggybackRowTest pins the 100-stock two-bar face).
  the masks: item+tank in U|L auto LEFT/TOP, out D|R auto RIGHT/BOTTOM.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/generifier.py
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

GEN = gt6world.Site(414, 65, 280, dz=1)

steps = [
    phase("A: the parallel-100 row column + the TU window"),
    Step(f"gt6machine generifier place {F(GEN)}", expect="GT6 generifier placed at 414, 65, 280"),
    Step(f"gt6machine generifier input 64 {F(GEN)}",
         expect="GT6 generifier input: 64x sand into slot 0",
         node_expects={"1.21.1": "GT6 generifier input: 64x minecraft:sand into slot 0"}),
    Step(f"gt6machine generifier fluid fill up minecraft:water 1000 {F(GEN)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine generifier check {F(GEN)}", expect="minIn=1 recIn=1 maxIn=16"),
    Step(f"gt6machine generifier check {F(GEN)}", expect="parallel=100"),

    phase("B: ONE bar consumes the full 64-stack (the parallel live face)"),
    Step(f"gt6machine generifier inject 32 16 {F(GEN)}",
         expect="outputs=[64x clay_ball; ]",
         node_expects={"1.21.1": "outputs=[64x minecraft:clay_ball; ]"}),

    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 413 62 278 415 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="generifier hu_tu",
    slug="generifier",
    sites=gt6world.declare_sites(GEN),
    preferred_ports=(26344, 26354),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
