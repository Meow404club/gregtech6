#!/usr/bin/env python3
"""p26-c-foam-fluid-refill — the C-Foam Canner refill e2e (declarative framework, the
p24_canner_refill shape).

Chain semantics (task p26-c-foam-fluid-refill ACCEPTANCE 3 — "空喷剂罐+CFOAM 流体→
Canner→满罐（扩或新链，参照 p25_cfoam_spray 链形态）"):

  A the foam refill unit (T1, dye RED): place → input 8x spray_can_empty → fluid fill up
    gt6:cfoam_red 25600 (the row leg, MultiItemRandomTools.java:254 FL.mul(DYED_C_FOAMS[i],
    256) = 256 x the 100-unit bucket, FL.java:432 "// 100 per Unit") → inject 70 x 64 EU
    (EUt 16, duration 256, parallel 1 — the same 4096 EU budget the p24 refill unit used)
    → the verdict pins the FULL CAN in the output slot (1x foam_spray_red, the R5
    colour-as-identity ruling, ZERO NBT) and the input tank drained to 0.

  B the Advanced owned refill unit (T2, dye WHITE): gt6:cfoam_owned_white 25600 (the :262
    FL.mul(DYED_C_FOAMS_OWNED[i], 256) ladder) → inject 70 256 → 1x foam_spray_owned_white.
    The T2 machine PERFECT-OVERCLOCKS the LV row (duration 256→128, the EU budget
    4096→8192 — the live inject probe: progress=4480/8192 under 70x64), so the B leg rides
    the p24_canner_refill T2 shape (inject 70 256 = 17920 EU, ample).

  C teardown: the explicit fill-air over the band. No global state is touched.

The 4 item-naming expects are PER-LEG via Step.node_expects (the p24_canner_refill
ruling): 1.20.1 renders the bare registry path ("foam_spray_red") while 21.1 renders
NAMESPACED ("gt6:foam_spray_red").

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_cfoam_refill.py
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

# the two canner sites on the 412 band (clear of p24cannerrefill's 400-408 band and
# p25cfoamspray's 397-398 band)
SITE_A = gt6world.Site(412, 64, 20, dx=0, dy=1, dz=1)  # canner     T1 — red refill
SITE_B = gt6world.Site(414, 64, 20, dx=0, dy=1, dz=1)  # canner_t2  T2 — white owned refill
A = F(SITE_A)
B = F(SITE_B)

steps = []

# ------------------------------------------------- A: the dyed refill unit (T1)
steps += [
    phase("A: the C-Foam refill unit (T1) — red 25600 mB (256 x the 100-unit bucket), the full can lands with ZERO NBT"),
    Step(f"gt6machine canner place {A}", expect="GT6 canner placed"),
    Step(f"gt6machine canner input 8 {A}",
         expect="GT6 canner input: 8x spray_can_empty into slot 0",
         node_expects={"1.21.1": "GT6 canner input: 8x gt6:spray_can_empty into slot 0"}),
    Step(f"gt6machine canner fluid fill up gt6:cfoam_red 25600 {A}",
         expect="filled 25600/25600 L of gt6:cfoam_red (ACCEPTED), input tanks hold 25600 L"),
    Step(f"gt6machine canner inject 70 64 {A}",
         expect="outputs=[1x foam_spray_red; ]",
         node_expects={"1.21.1": "outputs=[1x gt6:foam_spray_red; ]"}),
    Step(f"gt6machine canner fluid stat {A}", expect="in[0]=0 L of nothing"),
]

# ------------------------------------------------- B: the owned refill unit (T2)
steps += [
    phase("B: the Advanced owned refill unit (T2) — cfoam_owned_white 25600 mB → foam_spray_owned_white"),
    Step(f"gt6machine canner_t2 place {B}", expect="GT6 canner_t2 placed"),
    Step(f"gt6machine canner_t2 input 8 {B}",
         expect="GT6 canner input: 8x spray_can_empty into slot 0",
         node_expects={"1.21.1": "GT6 canner input: 8x gt6:spray_can_empty into slot 0"}),
    Step(f"gt6machine canner_t2 fluid fill up gt6:cfoam_owned_white 25600 {B}",
         expect="filled 25600/25600 L of gt6:cfoam_owned_white (ACCEPTED), input tanks hold 25600 L"),
    Step(f"gt6machine canner_t2 inject 70 256 {B}",
         expect="outputs=[1x foam_spray_owned_white; ]",
         node_expects={"1.21.1": "outputs=[1x gt6:foam_spray_owned_white; ]"}),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 411 62 19 415 67 21 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-cfoam-refill",
    slug="p26cfoamrefill",
    sites=gt6world.declare_sites(SITE_A, SITE_B),
    preferred_ports=(26111, 26121),      # this card's pinned rcon/query pair (the 2610x segment, after p25foodcan/p25cfoamspray 26110/26120)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
