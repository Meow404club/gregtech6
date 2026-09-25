#!/usr/bin/env python3
"""boule_crusher_gem4 — the boule consumption chain, CRUSHER arm (task
boule-consumption-rcon, RCON group machines_bc):

  A the CRYSTALLISATION arm — the :692 sapphire-boule row on the T2 machine:
    ONE dust_alumina + 1000 L of gt6:helium (the gas(U) leg) + 464 L of
    gt6:alumina_molten (the 29*U9 molten leg, the L=144 mB/unit convention);
    inject 64 ticks @ 64 HU LOCKS the recipe, the fake source keeps it active,
    the merge parks progress 8192 short, and the live ticking walks the tail
    through the completion + output wrap — the output slot holds
    gt6:boule_gt_sapphire.

    The T2 machine (crystallisationcrucible_t2, the invar rung) is LOAD-BEARING:
    the :692 row is 256 EUt and the T1 window is 64 — findRecipe voltage-gates
    the row invisible AND the 64-tick injector size overcharges (doInject :493
    the Root D3 suspend/explode body — the first probe exploded the T1 rig,
    "The target block is not a block entity"). The T2 window is {64,128,256}.

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
    (the CrusherTemplate(":75") shapes, 16 EUt, the measured 65536-unit clock).
    The item transfer rides the rm_backfill hopper-feed idiom: the
    machine-neighbor item auto-push is a CUT face (TileEntityBasicMachine
    :550/:561) and the vanilla hopper only sucks from ABOVE (the crucible's top
    is an INPUT face), so the produced boule cannot be physically extracted —
    the SAME item id the crucible just emitted is hopper-fed into the crusher
    and both ends are pinned live (output slot + input slot), the recipe-chain
    identity proof. The completion is the p8 KU pulse form (finalSize -64): the
    crusher is a KU machine and KU is an ALL_ALTERNATING member — the wrap
    needs the positive->non-positive transition tick, which one inject command
    carries in its last iteration (a plain positive train parks at max+64
    forever, the first probe).

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/boule_crusher_gem4.py
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

Z = 416  # the fresh band — z-disjoint from the machines_bc band z=384 and bumbliary_gui z=376
CC1 = gt6world.Site(408, 65, Z, dz=1)  # crystallisationcrucible T2 (the :692 sapphire row, 256 EUt needs the 256 window)
CR1 = gt6world.Site(412, 65, Z, dz=1)  # crusher T1 (the :75 boule -> gem x4 row)
CR1_HOPPER = "412 66 416"              # stacked on the crusher, facing down into it

steps = [
    phase("A: the T2 crystallisation crucible — the :692 row (dust alumina + helium + molten alumina -> the sapphire boule)"),
    Step(f"gt6machine crystallisationcrucible_t2 place {F(CC1)}", expect="placed at 408, 65, 416"),
    Step(f"gt6machine crystallisationcrucible_t2 input item gt6:dust_alumina 1 {F(CC1)}", expect="dust_alumina into slot"),
    Step(f"gt6machine crystallisationcrucible_t2 fluid fill up gt6:helium 1000 {F(CC1)}",
         expect="filled 1000/1000 L of gt6:helium (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine crystallisationcrucible_t2 fluid fill up gt6:alumina_molten 464 {F(CC1)}",
         expect="filled 464/464 L of gt6:alumina_molten (ACCEPTED), input tanks hold 1464 L"),
    # the T2 window (TIER_INPUTS[1] = {64,128,256}) — the 256 EUt row's voltage seat
    Step(f"gt6machine crystallisationcrucible_t2 check {F(CC1)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"gt6machine crystallisationcrucible_t2 inject 64 64 {F(CC1)}", expect="inject ticks=64 size=64"),
    # the fast-forward arm (the machines_bc chain-B precedent): 256 EUt x 18000 t =
    # 4608000 progress units where the progress unit IS an energy unit (ADR-P4); across
    # RCON calls the idle ticks reset parked progress, and the :503 overcharge body
    # explodes any packet over the window — lock + fake source + merge + live-tick poll
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(f"data merge block {F(CC1)} {{progress: 4599808}}", expect="Modified block data"),
    Step(f"gt6machine crystallisationcrucible_t2 check {F(CC1)}", expect="boule_gt_sapphire", tick_step=40, tick_fallback_poll=10),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),

    phase("B: the T1 crusher — the :75 row live (boule_gt_sapphire 1 -> gem_sapphire x4, hopper-fed)"),
    Step(f"gt6machine crusher place {F(CR1)}", expect="GT6 crusher placed at 412, 65, 416"),
    Step(f"setblock {CR1_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {CR1_HOPPER} container.0 with gt6:boule_gt_sapphire 1",
         expect="Replaced", sleep=4.0),
    # the loader-neutral input-slot pin: bare item on the forge leg, gt6:-qualified on neo
    Step(f"gt6machine crusher check {F(CR1)}", expect="boule_gt_sapphirex1"),
    # the lock beat only; the COMPLETION is the p8 KU pulse form: the crusher is a KU
    # machine (GTMachines CRUSHER_BE TD.Energy.KU) and KU is an ALL_ALTERNATING member —
    # outputs deliver ONLY on the positive->non-positive transition tick (the :815 arm),
    # so a plain positive train parks at max+64 with the wrap unfired (the first probe:
    # 65600/65536, outputs=[], then the idle reset ate the row). One command rides the
    # whole pulse cycle: 1199 positive ticks (the measured 65536-unit clock at 64/tick =
    # 1024 ticks, wide margin) + the final iteration at -64 = the transition pair ->
    # the :502 completion + output wrap land inside the command
    Step(f"gt6machine crusher inject 1200 64 -64 {F(CR1)}", expect="inject ticks=1200 size=64 finalSize=-64"),
    Step(f"gt6machine crusher check {F(CR1)}", expect="out[0]=4x"),
    Step(f"gt6machine crusher check {F(CR1)}", expect="gem_sapphire"),
    Step(f"gt6machine crusher check {F(CR1)}", expect="airx0"),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 406 63 414 414 67 418 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="boule-crusher-gem4 machines_bc",
    slug="boulecrusher",
    sites=gt6world.declare_sites(CC1, CR1),
    preferred_ports=(26692, 26702),      # this card's pinned rcon/query pair (after machinesbp 26672/26682)
    response_timeout=60.0,               # the inject loops run server-side
    mutates=("fakesource",),             # exclusive wave — never shares a boot wave
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
