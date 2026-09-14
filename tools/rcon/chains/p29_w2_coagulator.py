#!/usr/bin/env python3
"""p29-w2-coagulator — the TU Coagulator chain (task p29-w2-hu-tu-piggyback, group
p29_w2_hu_tu; the TU single :1651 over the JSON-poured coagulator.json FLUID-ONLY smoke
row — water 1000 -> 1 snowball, eUt 1, duration 64):

  the TU window {1, 1, 16}: minIn=1 recIn=1 maxIn=16; budget
  units(1x64x1, 10000, 10000, T) = 64 -> 64 ticks @ 1 EU/tick (the :770 TU arm,
  mMinEnergy = mEUt, no parallel).

  the NO_CONSTANT_POWER LIVE resume arm (the card-①遗留 obligation, double-recorded on
  the p29_w2_energy_types chain doc and the GT6EnergyDynamicsTest doc — the machine-side
  completion of "the RETAINED half is the offline suite's, no NO_CONSTANT_POWER machine
  exists live yet"): 20 in-window ticks bank 20/64; a 3 s command-idle gap (the real
  server ticking the parked machine UNGENERATED) leaves progress EXACTLY 20/64 — the
  :894 reset gate is SKIPPED (mNoConstantEnergy). The card-① shredder arm pinned the
  reset twin LIVE on the same rig shape; the offline GT6HuTuPiggybackRowTest pins this
  row's retain half. The resume completes on the north-face rig supply (volt 16 from a
  HORIZONTAL face — the six-face energy mask's live half), then the mask arm pins
  energyIn=down,up,north,south,west,east (all six, the :511 theoretical probe).
  the 17-包 arm: one 17-EU packet exceeds mInputMax 16 -> overcharge -> the throwaway
  is GONE (the STAT FAILED absence proof, the p29_w1_fermenter arm-B form).

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w2_coagulator.py
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

COAG = gt6world.Site(404, 65, 280, dz=1)     # the main coagulator
RIG_N = gt6world.Site(404, 65, 279, dz=1)    # the north-face rig (the horizontal supply arm)
COAG2 = gt6world.Site(408, 65, 280)          # the throwaway (the 17-包 overcharge arm)

steps = [
    phase("A: the TU window edges + the 1-EU packet train banks exact progress"),
    Step(f"gt6machine coagulator place {F(COAG)}", expect="GT6 coagulator placed at 404, 65, 280"),
    Step(f"gt6machine coagulator fluid fill up minecraft:water 1000 {F(COAG)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine coagulator check {F(COAG)}", expect="minIn=1 recIn=1 maxIn=16"),
    Step(f"gt6machine coagulator inject 20 1 {F(COAG)}", expect="progress=20/64"),

    phase("B: the LIVE power gap — 3 s of idle ticks RETAIN the parked progress exactly "
          "(the card-①遗留 obligation; the constant-power shredder resets on the same rig shape)"),
    Step(f"gt6machine coagulator check {F(COAG)}", expect="progress=20/64", sleep=3.0),
    Step(f"gt6machine coagulator check {F(COAG)}", expect="progress=20/64"),

    phase("C: the six-face supply — the north-face rig (a HORIZONTAL face) completes the "
          "process, then the mask arm pins every world face"),
    Step(f"gt6energy place {F(RIG_N)}", expect="GT6 energy source placed at 404, 65, 279"),
    Step(f"gt6energy type {F(RIG_N)} TU", expect="type ENERGY.TIME"),
    Step(f"gt6energy volt {F(RIG_N)} 16", expect="voltage 16 EU"),
    Step(f"gt6energy mode {F(RIG_N)} on", expect="emitting true", sleep=1.5),
    Step(f"gt6machine coagulator check {F(COAG)}",
         expect="out[0]=1x snowball",
         node_expects={"1.21.1": "out[0]=1x minecraft:snowball"},
         poll=30.0),
    Step(f"gt6energy mode {F(RIG_N)} off", expect="emitting false"),
    Step(f"gt6machine coagulator fluid stat {F(COAG)}",
         expect="energyIn=down,up,north,south,west,east"),

    phase("D: the 17-包 wall — one packet over mInputMax 16 overcharges the throwaway away"),
    Step(f"gt6machine coagulator place {F(COAG2)}", expect="GT6 coagulator placed at 408, 65, 280"),
    Step(f"gt6machine coagulator inject 1 17 {F(COAG2)}", expect="used=1 progress=0/0"),
    Step(f"gt6machine coagulator check {F(COAG2)}", expect="STAT FAILED", allow_failed=True),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 403 62 278 409 68 282 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w2-coagulator p29_w2_hu_tu",
    slug="p29w2coagulator",
    sites=gt6world.declare_sites(COAG, RIG_N, COAG2),
    preferred_ports=(26343, 26353),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
