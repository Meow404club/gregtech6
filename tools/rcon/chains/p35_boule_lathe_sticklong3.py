#!/usr/bin/env python3
"""p35_boule_lathe_sticklong3 — the boule consumption chain, LATHE arm (task
p35-boule-consumption-rcon, RCON group p34_machines_bc):

  A the CRYSTALLISATION arm — the :683 Si-boule row (the p34_machines_bc
    chain-B verbatim, fresh band): ONE dust_silicon + 1000 L of gt6:helium +
    560 L of gt6:silicon_molten (the 35*U9 molten leg); inject 64 ticks @ 16 HU
    LOCKS the recipe, the fake source keeps it active, the merge parks progress
    one window short, and the live ticking walks the tail through the :502
    completion — the output slot holds gt6:boule_gt_silicon.

  B the LATHE arm — the :379 row live: boule_gt_silicon 1 -> stick_long_silicon
    x3 (the LatheTemplate(":379") hard arm — Silicon is NOT FURNACE and not
    SOFT (MT.java:342, mp 1687 > 1200 skips the FURNACE tag), so tEasyWorkable
    is false and the multiplier-64 getCosts arm applies; 16 EUt). The item
    transfer rides the p26_rm_backfill hopper-feed idiom (the machine-neighbor
    auto-push is a CUT face, TileEntityBasicMachine :550/:561; the vanilla
    hopper only sucks from ABOVE — the crucible's top is an INPUT face): the
    SAME item id the crucible just emitted is hopper-fed into the lathe and
    both ends are pinned live (output slot + input slot), the recipe-chain
    identity proof.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_boule_lathe_sticklong3.py
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

Z = 424  # the fresh band — z-disjoint from the crusher band z=416 and the p34 bands z=376/384
CC2 = gt6world.Site(408, 65, Z, dz=1)  # crystallisationcrucible T1 (the :683 Si row)
LA1 = gt6world.Site(412, 65, Z, dz=1)  # lathe T1 (the :379 boule -> stickLong x3 row)
LA1_HOPPER = "412 66 424"              # stacked on the lathe, facing down into it

steps = [
    phase("A: the T1 crystallisation crucible — the :683 row (dust silicon + helium + molten silicon -> the Si boule)"),
    Step(f"gt6machine crystallisationcrucible place {F(CC2)}", expect="GT6 crystallisationcrucible placed at 408, 65, 424"),
    Step(f"gt6machine crystallisationcrucible input 1 {F(CC2)}", expect="dust_silicon into slot 0"),
    Step(f"gt6machine crystallisationcrucible fluid fill up gt6:helium 1000 {F(CC2)}",
         expect="filled 1000/1000 L of gt6:helium (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine crystallisationcrucible fluid fill up gt6:silicon_molten 560 {F(CC2)}",
         expect="filled 560/560 L of gt6:silicon_molten (ACCEPTED), input tanks hold 1560 L"),
    Step(f"gt6machine crystallisationcrucible check {F(CC2)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine crystallisationcrucible inject 64 16 {F(CC2)}", expect="inject ticks=64 size=16"),
    # the fast-forward arm (the p34_machines_bc chain-B verbatim): 16 EUt x 72000 t =
    # 1152000 progress units where the progress unit IS an energy unit (ADR-P4)
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(f"data merge block {F(CC2)} {{progress: 1151900}}", expect="Modified block data"),
    Step(f"gt6machine crystallisationcrucible check {F(CC2)}", expect="boule_gt_silicon", tick_step=40, tick_fallback_poll=10),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),

    phase("B: the T1 lathe — the :379 row live (boule_gt_silicon 1 -> stick_long_silicon x3, hopper-fed)"),
    Step(f"gt6machine lathe place {F(LA1)}", expect="GT6 lathe placed at 412, 65, 424"),
    Step(f"setblock {LA1_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {LA1_HOPPER} container.0 with gt6:boule_gt_silicon 1",
         expect="Replaced", sleep=4.0),
    # the loader-neutral input-slot pin: bare item on the forge leg, gt6:-qualified on neo
    Step(f"gt6machine lathe check {F(LA1)}", expect="boule_gt_siliconx1"),
    # the :379 row: duration = getCosts(mult 64) = 9U x 64 x (1+q) at 16 EUt — the 64/t
    # injector's cheap overclock rides it far under the 4000-tick window (the rm_backfill
    # overshoot form: the row stalls honestly once the single boule is consumed)
    Step(f"gt6machine lathe inject 4000 64 {F(LA1)}", expect="inject ticks=4000 size=64"),
    Step(f"gt6machine lathe check {F(LA1)}", expect="out[0]=3x"),
    Step(f"gt6machine lathe check {F(LA1)}", expect="stick_long_silicon"),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 406 63 422 414 67 426 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35-boule-lathe-sticklong3 p34_machines_bc",
    slug="p35boulelathe",
    sites=gt6world.declare_sites(CC2, LA1),
    preferred_ports=(26712, 26722),      # this card's pinned rcon/query pair (after the crusher arm 26692/26702)
    response_timeout=60.0,               # the 4000-tick inject loop runs server-side
    mutates=("fakesource",),             # exclusive wave — never shares a boot wave
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
