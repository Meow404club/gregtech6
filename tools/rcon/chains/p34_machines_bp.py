#!/usr/bin/env python3
"""p34-machines-bp — the Burner Mixer + Plantalyzer live chain (task
p34-machines-burner-plantalyzer, RCON group p34_machines_bp):

  A the Burner Mixer (20521, the RU ignition family, RM.BurnMixer) — the :220
    formation row RUNS LIVE (fluid-only: hydrogen 1000 + oxygen 500 -> distilled
    water 1500, the 1000 mB/U conversion the p31 fusion card calibrated):
    place -> hydrogen through the top fill face -> oxygen (the second input tank) ->
    the /gt6energy RU rig (the p29_w1_sander form, the SBIT_D bottom face) ->
    /gt6machine ignite (the TOOL_igniter stand-in — the cold machine PROVES the
    negative gate first: before ignite the poll pins running=false with inputs
    armed; after ignite the poll pins the out-tank 1500 L). The keep-alive face:
    the second charge (2000 H + 1000 O = two rows) runs OFF the FIRST ignite —
    the mIgnited=40 refresh on the :816/:851 output sites carries the second
    process (the card's 成功出料续燃 assertion), pinned at 3000 L accumulated.

  B the T2-T4 inject ramp (the sander form): the :221 tritium row chemistry rides
    the same map — the inject rig pins the per-tick RU window ladder
    (minIn/recIn/maxIn = 32/128/512->2048... the TIER_INPUTS conversion).

  C the Plantalyzer (20531, the EU 5-ladder, RM.PLANTALYZER) — the DECLARED-EMPTY
    live face: place -> the EU rig armed -> the poll pins running=false (zero
    rows never start a process, the P10 compat cut) -> the single tank-in face
    fills water (the p34-gui fluid seat's live carrier) -> the open smoke arm
    SKIP verdict (the fake-player boundary).

  D teardown: the explicit fill-air over the band.

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p34_machines_bp.py
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

# the fresh z=350 band (east of the p33 food 340 strip; the x-column 448-460 reused
# at dz+10 — the bbox admission is band-disjoint)
Z = 350
BM = gt6world.Site(448, 65, Z)        # the Burner Mixer T1
BMF = gt6world.fmt(BM)
BM_RIG = gt6world.Site(449, 65, Z)    # the RU source (the SBIT_D bottom face feeds up? —
                                      # the rig sits beside; doInject reaches the D face)
BM2 = gt6world.Site(452, 65, Z)       # the Burner Mixer T2 (the inject ramp)
BM3 = gt6world.Site(454, 65, Z)
BM4 = gt6world.Site(456, 65, Z)
PL = gt6world.Site(460, 65, Z)        # the Plantalyzer T1
PL_RIG = gt6world.Site(461, 65, Z)    # the EU source (the SBIT_B back face)

steps = [
    phase("A: the Burner Mixer — the :220 formation row on the RU rig + the ignition gate"),
    Step(f"setblock {BMF} gt6:burner_mixer", expect="Changed the block", sleep=1.0),
    # the negative gate FIRST: inputs armed + power on, NO ignite -> the machine never starts
    Step(f"gt6machine burner_mixer fluid fill up gt6:hydrogen 1000 {F(BM)}",
         expect="filled 1000/1000 L of gt6:hydrogen (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine burner_mixer fluid fill up gt6:oxygen 500 {F(BM)}",
         expect="filled 500/500 L of gt6:oxygen (ACCEPTED), input tanks hold 1500 L"),
    Step(f"gt6energy place {F(BM_RIG)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(BM_RIG)} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6energy volt {F(BM_RIG)} 32", expect="voltage 32"),  # the T1 window mid (min 16 / max 64)
    Step(f"gt6energy mode {F(BM_RIG)} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine burner_mixer check {F(BM)}", expect="running=false",
         poll=40.0),  # the ignition gate: a cold machine with armed inputs NEVER starts (the :724/:737 fold)
    Step(f"gt6machine burner_mixer ignite {F(BM)}", expect="GT6 machine ignite OK (mIgnited=40)"),
    Step(f"gt6machine burner_mixer fluid stat {F(BM)}",
         expect="out[0]=1500 L of gt6:distilled_water",  # the :220 row completed: H 1000 + O2 500 -> 1500
         poll=300.0),
    # the keep-alive face: a SECOND row off the FIRST ignite (the :816/:851 refresh
    # carries it; the initial 40-tick window would have expired). The bank split:
    # the second charge rides BOTH empty tanks (hydrogen 1000 into tank0, oxygen 1000
    # into tank1); the row consumes H 1000 + O 500 -> out 3000 accumulated.
    Step(f"gt6machine burner_mixer fluid fill up gt6:hydrogen 1000 {F(BM)}",
         expect="filled 1000/1000 L of gt6:hydrogen (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine burner_mixer fluid fill up gt6:oxygen 1000 {F(BM)}",
         expect="filled 1000/1000 L of gt6:oxygen (ACCEPTED), input tanks hold 2000 L"),
    Step(f"gt6machine burner_mixer fluid stat {F(BM)}",
         expect="out[0]=3000 L of gt6:distilled_water",  # 1500 + 1500: the second row completed WITHOUT re-ignite
         poll=300.0),

    phase("B: the T2-T4 inject ramp — the window ladder (the sander form; the row chemistry rides the map)"),
    Step(f"setblock {F(BM2)} gt6:burner_mixer_t2", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine burner_mixer check {F(BM2)}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(f"setblock {F(BM3)} gt6:burner_mixer_t3", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine burner_mixer check {F(BM3)}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(f"setblock {F(BM4)} gt6:burner_mixer_t4", expect="Changed the block", sleep=1.0),
    Step(f"gt6machine burner_mixer check {F(BM4)}", expect="minIn=1024 recIn=2048 maxIn=4096"),

    phase("C: the Plantalyzer — the declared-empty live face (place + power + idle)"),
    Step(f"setblock {F(PL)} gt6:plantalyzer", expect="Changed the block", sleep=1.0),
    Step(f"gt6energy place {F(PL_RIG)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(PL_RIG)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(PL_RIG)} 32", expect="voltage 32"),
    Step(f"gt6energy mode {F(PL_RIG)} on", expect="emitting true", sleep=2.0),
    Step(f"gt6machine plantalyzer input 1 {F(PL)}", expect="oak_sapling into slot 0"),
    Step(f"gt6machine plantalyzer check {F(PL)}",
         expect="running=false",  # zero rows never start a process (the P10 compat cut)
         poll=40.0),
    Step(f"gt6machine plantalyzer fluid fill up minecraft:water 1000 {F(PL)}",
         expect="(ACCEPTED)"),  # the single tank-in face: the p34-gui fluid seat's live carrier
    Step(f"gt6machine plantalyzer fluid draw up 1000 {F(PL)}", expect="drained"),
    Step(f"gt6machine plantalyzer open {F(PL)}", expect="open SKIP for the fake player"),

    phase("D: teardown — restore the band"),
    Step(f"fill 447 62 {Z - 1} 462 68 {Z + 2} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the group key: sweep --group p34_machines_bp matches
    name="p34-machines-bp p34_machines_bp",
    slug="p34machinesbp",
    sites=gt6world.declare_sites(BM, BM_RIG, BM2, BM3, BM4, PL, PL_RIG),
    preferred_ports=(26672, 26682),      # this card's pinned rcon/query pair (after p34guibmach 26652/26662)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
