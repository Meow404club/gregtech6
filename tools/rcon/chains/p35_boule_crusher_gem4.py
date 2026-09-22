#!/usr/bin/env python3
"""p35_boule_crusher_gem4 — the boule consumption chain, CRUSHER arm (task
p35-boule-consumption-rcon, RCON group p34_machines_bc):

  A the CRYSTALLISATION arm — the :692 sapphire-boule row: the T1 machine +
    ONE dust_alumina in the slot + 1000 L of gt6:helium (the gas(U) leg) +
    464 L of gt6:alumina_molten (the 29*U9 molten leg, the L=144 mB/unit
    convention); inject 64 ticks @ 256 HU LOCKS the recipe, the fake source
    keeps it active, the merge parks progress one window short, and the live
    ticking walks the tail through the :502 completion — the output slot holds
    gt6:boule_gt_sapphire.

    SAPPHIRE, NOT SILICON — the declared spec amendment (coordinator-approved
    2026-09-22): the :75 crusher row resolves BOTH sides (GT6RecipesShCL
    .buildCrusherRecipe :837-848), and the gem items exist only for the seven
    sapphire-family boules — the Si/Ge/RedstoneAlloy/NikolineAlloy quartet has
    NO gem item (GTMaterialItemsForceTest pins only the plateGem cascade for
    the quartet; the generated item models carry gem_ruby/gem_sapphire but no
    gem_silicon), so a boule_gt_silicon input would find NO crusher row. The
    :692 sapphire row is a real crucible row (the :75 consumer's true pour
    set), so the arm proves MORE: the sapphire production leg live.

  B the CRUSHER arm — the :75 row live: boule_gt_sapphire 1 -> gem_sapphire x4
    (the CrusherTemplate(":75") shapes, 16 EUt). The item transfer rides the
    p26_rm_backfill hopper-feed idiom: the machine-neighbor item auto-push is
    a CUT face (TileEntityBasicMachine :550/:561) and the vanilla hopper only
    sucks from ABOVE (the crucible's top is an INPUT face), so the produced
    boule cannot be physically extracted — the SAME item id the crucible just
    emitted is hopper-fed into the crusher and both ends are pinned live
    (output slot + input slot), the recipe-chain identity proof.

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_boule_crusher_gem4.py
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

Z = 416  # the fresh band — z-disjoint from the p34_machines_bc band z=384 and p34_bumbliary_gui z=376
CC1 = gt6world.Site(408, 65, Z, dz=1)  # crystallisationcrucible T1 (the :692 sapphire row)
CR1 = gt6world.Site(412, 65, Z, dz=1)  # crusher T1 (the :75 boule -> gem x4 row)
CR1_HOPPER = "412 66 416"              # stacked on the crusher, facing down into it

steps = [
    phase("A: the T1 crystallisation crucible — the :692 row (dust alumina + helium + molten alumina -> the sapphire boule)"),
    Step(f"gt6machine crystallisationcrucible place {F(CC1)}", expect="GT6 crystallisationcrucible placed at 408, 65, 416"),
    Step(f"gt6machine crystallisationcrucible input item gt6:dust_alumina 1 {F(CC1)}", expect="dust_alumina into slot"),
    Step(f"gt6machine crystallisationcrucible fluid fill up gt6:helium 1000 {F(CC1)}",
         expect="filled 1000/1000 L of gt6:helium (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine crystallisationcrucible fluid fill up gt6:alumina_molten 464 {F(CC1)}",
         expect="filled 464/464 L of gt6:alumina_molten (ACCEPTED), input tanks hold 1464 L"),
    Step(f"gt6machine crystallisationcrucible check {F(CC1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine crystallisationcrucible inject 64 256 {F(CC1)}", expect="inject ticks=64 size=256"),
    # the fast-forward arm (the p34_machines_bc chain-B precedent): 256 EUt x 18000 t =
    # 4608000 progress units (the progress unit IS an energy unit, ADR-P4); across RCON
    # calls the idle ticks reset parked progress through doInactive, and an 18000-iteration
    # command risks the 2 s RCON deadline — lock + fake source + merge + live-tick poll.
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(f"data merge block {F(CC1)} {{progress: 4607900}}", expect="Modified block data"),
    Step(f"gt6machine crystallisationcrucible check {F(CC1)}", expect="boule_gt_sapphire", tick_step=40, tick_fallback_poll=10),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),

    phase("B: the T1 crusher — the :75 row live (boule_gt_sapphire 1 -> gem_sapphire x4, hopper-fed)"),
    Step(f"gt6machine crusher place {F(CR1)}", expect="GT6 crusher placed at 412, 65, 416"),
    Step(f"setblock {CR1_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {CR1_HOPPER} container.0 with gt6:boule_gt_sapphire 1",
         expect="Replaced", sleep=4.0),
    # the loader-neutral input-slot pin: bare item on the forge leg, gt6:-qualified on neo
    Step(f"gt6machine crusher check {F(CR1)}", expect="boule_gt_sapphirex1"),
    # the :75 row budget: units 9U x (256 + 256*q) at 16 EUt — the 64/t injector's cheap
    # overclock rides it far under the 4000-tick window (the rm_backfill overshoot form:
    # the row stalls honestly once the single boule is consumed)
    Step(f"gt6machine crusher inject 4000 64 {F(CR1)}", expect="inject ticks=4000 size=64"),
    Step(f"gt6machine crusher check {F(CR1)}", expect="out[0]=4x"),
    Step(f"gt6machine crusher check {F(CR1)}", expect="gem_sapphire"),
    Step(f"gt6machine crusher check {F(CR1)}", expect="airx0"),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 406 63 414 414 67 418 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35-boule-crusher-gem4 p34_machines_bc",
    slug="p35boulecrusher",
    sites=gt6world.declare_sites(CC1, CR1),
    preferred_ports=(26692, 26702),      # this card's pinned rcon/query pair (after p34machinesbp 26672/26682)
    response_timeout=60.0,               # the 4000-tick inject loop runs server-side
    mutates=("fakesource",),             # exclusive wave — never shares a boot wave
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
