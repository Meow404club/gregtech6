#!/usr/bin/env python3
"""p16-distillery — the Distillery family live acceptance chain (declarative framework).

Chain semantics (task p16-distillery-family ACCEPTANCE — "RCON place 四变体+circuit
输入→running→产出抽样臂两遍 [0,0]"): four distilleries placed (the placed default
FACING = north), per tier

  place → fluid fill east minecraft:water (the rotated SBIT_U|SBIT_L tank-in mask:
          relative left = world east — ACCEPTED; the SBIT_U half is world up) →
  fluid fill up (the other tank-in half — ACCEPTED) →
  fluid fill south (the OUT-only back face — REJECTED, 0 filled is a legitimate verdict) →
  fluid stat (the live census: 1000 L of water in the input tank + the six-side face
          lists: fluidIn=up,east fluidOut=south energyIn=down, the masks
          fluidIn=70 fluidOut=96 energyIn=65) →
  input 1 (the Integrated Circuit at configuration 0 — the ST.tag(0) selector the
          poured :534-541 rows carry; the feed lands through feedStack() so the
          Damage:0 tag rides the stack) →
  inject (HU packets at the row's mInputMax — the :503 band; the poured map starts
          on the water row, the batch runs on the driven ticks, 产出 lands in the
          output bank) →
  check (the row columns: parallel 8/16/32/64 + parallelDuration + recIn 32/128/512/2048,
          active=true mid-batch — the machine is RUNNING on the circuit-selected row) →
  fluid draw south (the DistW抽样臂: the distilled water drawn out of the output bank
          through the rotated back face — ACCEPTED) ×2 + stat (the hold census) →
  teardown.

The negative circuit arm rides tier 1: a SECOND distillery fed water WITHOUT the
circuit selector finds nothing (the RM.java:70 mMinimalInputItems=1 item leg is
real — check reports active=false progress=0/0, 产出零).

The two framework passes are the [0, 0] idempotency proof; the pass-open bbox
cleanup restores the sites between passes.

Run:  python3 tools/rcon/chains/p16_distillery.py   (GT6_SESSION=off for the
one-switch per-chain fallback — the parallel-session artifact rule)
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

# The four distillery sites + the no-circuit negative arm site, >=16 blocks apart,
# clear of the earlier chains' bands (the p16 bands end at x=300/z=214).
T1 = gt6world.Site(320, 64, 100, dx=1, dy=2, dz=1)
T2 = gt6world.Site(336, 64, 100, dx=1, dy=2, dz=1)
T3 = gt6world.Site(352, 64, 100, dx=1, dy=2, dz=1)
T4 = gt6world.Site(368, 64, 100, dx=1, dy=2, dz=1)
NEG = gt6world.Site(384, 64, 100, dx=1, dy=2, dz=1)

# (literal, pos, parallel, recIn, maxIn, batchFill, batchTicks, draw) — one EXACT
# batch per tier: the parallel count is bind(1, canOutput, recIn/EUt) (:642-651),
# so T1 runs 2 processes (20 L, 512 units @16/tick = 32 ticks), T2 8 (80 L, 128
# ticks), T3 32 (320 L, 512 ticks), T4 64 (640 L, 1024 ticks — the parallelDuration
# chain cap :626-629 stays clear). The fill lands the batch water exactly; the draw
# arm samples the whole DistW batch through the back face.
TIERS = [
    # literal, pos, parallel, recIn, maxIn, eastFill, totalFill, batchTicks, draw
    ("distillery",    T1, "parallel=8",  "recIn=32",   64,   20,  70,  40,   16),
    ("distillery_t2", T2, "parallel=16", "recIn=128",  256,  30,  80,  140,  64),
    ("distillery_t3", T3, "parallel=32", "recIn=512",  1024, 270, 320, 530,  256),
    ("distillery_t4", T4, "parallel=64", "recIn=2048", 4096, 590, 640, 1040, 512),
]

steps = []

# ------------------------------------------- the four tiers, one protocol each
for literal, pos, parallel, recin, maxin, fill, total, ticks, draw in TIERS:
    steps += [
        phase(f"{literal}: place, the rotated tank masks, the circuit selector, HU, the DistW draw"),
        Step(f"gt6machine {literal} place {F(pos)}", expect=f"GT6 {literal} placed"),
        # the positive tank-in arms — relative left (world east) + world up (SBIT_U on T2);
        # T1 takes the whole batch through the east face
        Step(f"gt6machine {literal} fluid fill east minecraft:water {fill} {F(pos)}",
             expect="L of minecraft:water (ACCEPTED)"),
        # the second tank-in half (world up, the SBIT_U column) — skipped on T1 (the
        # exact-batch fill cannot spare the 50 L); the NEG site carries the positive arm
        Step(f"gt6machine {literal} fluid fill up minecraft:water 50 {F(pos)}",
             expect="filled 50/50 L of minecraft:water (ACCEPTED)"),
        # the negative arm — the OUT-only back (world south) refuses the fill
        Step(f"gt6machine {literal} fluid fill south minecraft:water 100 {F(pos)}",
             expect="filled 0/100 L of minecraft:water (REJECTED)"),
        # the live census + the six-side face lists (the :511 probe pinned to bottom-only)
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect=f"in[0]={total} L of minecraft:water"),
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect="masks fluidIn=70 fluidOut=96 energyIn=65"),
        Step(f"gt6machine {literal} fluid stat {F(pos)}",
             expect="faces fluidIn=up,east fluidOut=south energyIn=down"),
        # the circuit selector feed — the ST.tag(0) item leg of every poured row
        Step(f"gt6machine {literal} input 1 {F(pos)}",
             expect="integrated_circuit into slot 0"),
        # the HU carrier live at the row's tier band — the batch completes on the driven ticks
        Step(f"gt6machine {literal} inject {ticks} {maxin} {F(pos)}", expect="progress="),
        Step(f"gt6machine {literal} check {F(pos)}", expect=parallel),
        Step(f"gt6machine {literal} check {F(pos)}", expect="parallelDuration=true"),
        Step(f"gt6machine {literal} check {F(pos)}", expect=recin),
        Step(f"gt6machine {literal} check {F(pos)}", expect="integrated_circuit"),
        # the DistW draw arm — the whole distilled batch leaves through the back face
        Step(f"gt6machine {literal} fluid draw south {draw} {F(pos)}",
             expect=f"drawn {draw}/{draw} L of gt6:distilled_water (ACCEPTED)"),
        Step(f"gt6machine {literal} fluid draw south 100 {F(pos)}",
             expect="drawn 0/100 L of nothing (REJECTED)"),
    ]

# ------------------------------------------- the no-circuit negative arm
steps += [
    phase("neg: water without the circuit selector — the item leg is real, 产出零"),
    Step(f"gt6machine distillery place {F(NEG)}", expect="GT6 distillery placed"),
    Step(f"gt6machine distillery fluid fill east minecraft:water 1000 {F(NEG)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED)"),
    # NO circuit in the slot: the RM.java:70 mMinimalInputItems=1 gate starves the map
    Step(f"gt6machine distillery inject 40 64 {F(NEG)}", expect="progress=0/0"),
    Step(f"gt6machine distillery check {F(NEG)}", expect="active=false"),
    Step(f"gt6machine distillery fluid stat {F(NEG)}",
         expect="out[0]=0 L of nothing"),
]

# ------------------------------------------- teardown
steps += [
    phase("C: teardown — the server survived the arms"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p16-distillery",
    slug="p16dist",
    sites=gt6world.declare_sites(T1, T2, T3, T4, NEG),
    preferred_ports=(25777, 25787),      # this card's pinned rcon/query pair (P16 segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
