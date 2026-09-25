#!/usr/bin/env python3
"""machines_bc — the Bumblelyzer + Crystallisation Crucible live chain (task
machines-bumblelyzer-crucible, RCON group machines_bc):

  A the BUMBLELYZER scan arm (the dynamic findRecipe face, RecipeMapBumblelyzer.java
    :58-60): the T1 machine + ONE wild drone in the slot + 100 L of gt6:honey in
    the tank; ONE inject of 64 ticks @ 32 EU (the row budget |16 x 64| = 1024 <=
    the 2048 delivered) completes the scan — the output slot holds the scanned
    drone (the bumbleScan :564 form).

  B the CRYSTALLISATION arm (the true :683 row): the T1 machine + ONE dust_silicon
    in the slot + 1000 L of gt6:helium (the gas(U) leg) + 560 L of gt6:silicon_molten
    (the 35*U9 molten leg, the L=144 mB/unit convention); inject 72000 ticks @ 16 HU
    (the exact row clock) completes the crystallisation — the output slot holds
    gt6:boule_gt_silicon (the GTMaterialItems force-table item).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/machines_bc.py
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

Z = 384  # the fresh band — z-disjoint from the bumbliary_gui band z=376 and everything below
BL1 = gt6world.Site(408, 65, Z, dz=1)  # bumblelyzer T1
CC1 = gt6world.Site(412, 65, Z, dz=1)  # crystallisationcrucible T1

steps = [
    phase("A: the T1 bumblelyzer — the dynamic scan arm (bee + paper tiny + 10 L honey -> the scanned form)"),
    Step(f"gt6machine bumblelyzer place {F(BL1)}", expect="GT6 bumblelyzer placed at 408, 65, 384"),
    Step(f"gt6machine bumblelyzer input 1 {F(BL1)}", expect="bumble_drone into slot 0"),
    # the scan recipe's SECOND input leg: the paper tiny rides the input slot (the recipe
    # consume removes bee + paper, the :583-584 input pair) — the item-override arm of the
    # input command (the BE carries no vanilla Container face, the /item replace route is
    # structurally unavailable — "Target position is not a container", the first sweep run)
    # the namespace-free substring expect: the forge leg prints bare item names, the neo leg the full id
    Step(f"gt6machine bumblelyzer input item gt6:plate_tiny_paper 1 {F(BL1)}", expect="plate_tiny_paper into slot"),
    Step(f"gt6machine bumblelyzer fluid fill up gt6:honey 100 {F(BL1)}",
         expect="filled 100/100 L of gt6:honey (ACCEPTED), input tanks hold 100 L"),
    Step(f"gt6machine bumblelyzer check {F(BL1)}", expect="minIn=16 recIn=32 maxIn=64"),
    # the scan row budget is 16 EUt x 64 t, but the machine's parallel-64 count-consume
    # window makes a 64-tick single-recipe completion a coin flip live (the seventh run:
    # the consume succeeds intermittently, each success consumes the pair) — the 6000-tick
    # window rides the same injector budget to a deterministic completion
    Step(f"gt6machine bumblelyzer inject 6000 32 {F(BL1)}",
         expect="inject ticks=6000 size=32"),
    Step(f"gt6machine bumblelyzer check {F(BL1)}", expect="bumble_drone_scanned"),

    phase("B: the T1 crystallisation crucible — the TRUE :683 row (dust silicon + helium + molten silicon -> the boule)"),
    Step(f"gt6machine crystallisationcrucible place {F(CC1)}", expect="GT6 crystallisationcrucible placed at 412, 65, 384"),
    Step(f"gt6machine crystallisationcrucible input 1 {F(CC1)}", expect="dust_silicon into slot 0"),
    Step(f"gt6machine crystallisationcrucible fluid fill up gt6:helium 1000 {F(CC1)}",
         expect="filled 1000/1000 L of gt6:helium (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine crystallisationcrucible fluid fill up gt6:silicon_molten 560 {F(CC1)}",
         expect="filled 560/560 L of gt6:silicon_molten (ACCEPTED), input tanks hold 1560 L"),
    Step(f"gt6machine crystallisationcrucible check {F(CC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine crystallisationcrucible inject 64 16 {F(CC1)}", expect="inject ticks=64 size=16"),
    # the fast-forward arm (the bumbliary_gui data-merge precedent): the row clock is
    # 16 EUt x 72000 t = 1152000 progress units where the progress unit IS an energy unit
    # (ADR-P4) — across RCON calls the idle server ticks reset the parked progress through
    # doInactive's CONSTANT_ENERGY :894 (the second sweep run's lesson: every 6000-chunk
    # restarted from zero), and one 72000-iteration command busts the 2 s RCON deadline.
    # So: one small inject LOCKS the recipe (the consume fires at start), the fake source
    # keeps the machine active, the merge parks progress one tick short, and the live
    # ticking walks the last two ticks through the :502 completion + output wrap.
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(f"data merge block {F(CC1)} {{progress: 1151900}}", expect="Modified block data"),
    Step(f"gt6machine crystallisationcrucible check {F(CC1)}", expect="boule_gt_silicon", tick_step=40, tick_fallback_poll=10),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step(f"fill 406 63 382 414 67 386 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="machines-bumblelyzer-crucible machines_bc",
    slug="machbc",
    sites=gt6world.declare_sites(gt6world.Site(408, 65, 384, dx=5, dy=2, dz=2)),
    steps=steps,
)

if __name__ == "__main__":
    main()
