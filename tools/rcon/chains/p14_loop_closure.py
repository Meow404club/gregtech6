#!/usr/bin/env python3
"""p14-loop-closure-chain — the canonical water→steam loop, end to end (declarative framework).

Chain semantics (task p14-loop-closure-chain ACCEPTANCE): the P14 canonical chain
firebox → Dryer → distilled water → boiler closes over LIVE blocks, no command-arm
energy fakes on the production path:

  A the rig: a T1 Dryer stacked one block ABOVE a brick burning box — the burner's
    top-face HU emission (MultiTileEntityGeneratorSolid.java:104 emit / :270
    SIDES_TOP gate) lands on the dryer row's SBIT_D bottom energy face (the
    GTMachines.java:225 mask; the :511 isEnergyAcceptingFrom probe answers
    energyIn=down in the fluid stat). Before fuel: running=false.

  B the production: 1000 L of vanilla water through the two rotated tank-in faces
    (700 east = relative left, 300 south = relative back), coal into the burner,
    ignite → running=true (the doWork flip, TileEntityBasicMachine.java:380-394).
    The :525 row runs 8-parallel (the T1 row parallel=8, parallelDuration): each
    batch = 80 L → 64 L, 2048 progress units = 2048 HU; the whole 1000 L needs
    12×2048 + 1×1024 (the 40 L tail at 4-parallel) = 25600 HU = 1600 burner ticks
    at 16 HU/t ≈ 84 s. The verdict is EXACT arithmetic, no dice anywhere on the
    machine path: in[0]=0 (every litre consumed), out[0]=800 L of
    gt6:distilled_water (1000 × 8/10 — the real production row, replacing the W2
    declared-empty arm), then one top-face draw lifts 800/800 L out.

  C the boiler leg: the drawn distw enters the lead boiler through the W1c
    acceptance channel (`gt6boiler fill <pos> 800 distw` — the :262 WATER-tag door),
    64000 HU booked (800 × EU_PER_WATER 80) converts it to 800 ×
    STEAM_PER_WATER_GLOBAL 160 (CS.java:242, the boiler-side scope — NOT the
    engine-private 200) = 128000 L steam → barometer 1+128000×30/320000 = 13, and
    the efficiency reads 10000/10000 (PRISTINE) — the :119 distilled criterion
    short-circuits the scaling branch, the deterministic immune arm. plunge ×2
    clears the steam (the W1c half-tank budget discipline).

  D the flip down + survival: extinguish → the burner stops emitting, doWork's
    :791 drain empties the machine buffer within a tick or two → running=false
    (the energy VALUE is never asserted — it drains every tick; the running bit is
    the criterion). execute-if-block pins all three machines in place; the server
    answers time query.

Run:  python3 tools/rcon/chains/p14_loop_closure.py
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

# The two sites: burner (100,64,100) + the dryer stacked on top (100,65,100); the
# lead boiler (120,64,120). The dy=1 on the rig site covers the stacked dryer; the
# burner's rng fire drops (7×5×7, y-1..+3) land on non-flammable superflat only —
# nothing in either site burns, and the pass-open bbox cleanup is the backstop.
RIG = gt6world.Site(100, 64, 100, dx=3, dy=3, dz=3)   # the fire volume too
BOILER = gt6world.Site(120, 64, 120, dx=1, dy=1, dz=1)

BURNER = F(RIG)          # 100 64 100
DRYER = "100 65 100"     # stacked one above the burner
BOIL = F(BOILER)         # 120 64 120

steps = []

# ------------------------------------------------- A: the rig + the face geometry
steps += [
    phase("A: the rig — burner below, dryer above; the bottom energy face, no fuel yet"),
    Step(f"gt6machine dryer place {DRYER}", expect="GT6 dryer placed"),
    Step(f"gt6burner place {BURNER} brick_burning_box", expect="GT6 burning box placed"),
    # the T1 row columns + the menu-open arm, before any fuel — flipped by
    # p16-machine-fluid-gui: the gt6:dryer supplier is bound, so the check probe
    # CONSTRUCTS the menu (the old data=-2 menu-less marker is gone) and the idle
    # ContainerData (progress=0/0, never started) reads -1
    Step(f"gt6machine dryer check {DRYER}", expect="data=-1"),
    Step(f"gt6machine dryer check {DRYER}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
    Step(f"gt6machine dryer check {DRYER}", expect="progress=0/0"),
    # the row geometry: the tank-in masks, the OUT top, the :511 energy probe = bottom only
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="in[0]=0 L of nothing; out[0]=0 L of nothing"),
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="masks fluidIn=100 fluidOut=66 energyIn=65"),
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="faces fluidIn=south,east fluidOut=up energyIn=down"),
]

# ------------------------------------------------- B: water in, fire in, the exact pour
steps += [
    phase("B: the production — 1000 L water, coal, ignite; 800 L distilled water land in the output tank"),
    Step(f"gt6machine dryer fluid fill east minecraft:water 700 {DRYER}",
         expect="filled 700/700 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6machine dryer fluid fill south minecraft:water 300 {DRYER}",
         expect="filled 300/300 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6burner fuel {BURNER} minecraft:coal 6", expect="minecraft:coal x6"),
    Step(f"gt6burner ignite {BURNER}", expect="burning=true"),
    # the flip up: the burner feeds 16 HU/t over the top face, doWork :791 drains it —
    # the machine runs (energy VALUES are never asserted; they drain every tick).
    # poll-to-expect (p23, the s14 deterministic red): the old trailing sleep=2.0 ran
    # AFTER the judge — the check's real budget was the previous command's quiet
    # window, and the P18 adaptive decay shrank it below the running-latch flip.
    # The flip is the waited-for condition; resend until it lands.
    Step(f"gt6machine dryer check {DRYER}", expect="running=true", poll=10.0),
    Step(f"gt6machine dryer check {DRYER}", expect="active=true"),
    # the production window: 25600 HU / 16 per tick = 1600 ticks ≈ 84 s at the measured
    # ~19 tps (the tail batch burns the last 40 L at 4-parallel). poll-to-expect: the
    # read-only stat IS the completion probe — resend until out[0]=800 lands; the two
    # verdict steps below re-assert the exact same strings, verbatim
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="out[0]=800 L of gt6:distilled_water", poll=150.0),
    # the exact pour verdict: everything consumed, 1000 × 8/10 = 800 L of distilled water
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="in[0]=0 L of nothing"),
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="out[0]=800 L of gt6:distilled_water"),
    # the top-face draw lifts the whole output bank
    Step(f"gt6machine dryer fluid draw up 800 {DRYER}",
         expect="drawn 800/800 L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
    # the dryer stalls honestly with an empty tank: no recipe, no phantom water
    Step(f"gt6machine dryer fluid draw up 100 {DRYER}", expect="drawn 0/100 L of nothing (REJECTED)"),
]

# ------------------------------------------------- C: the boiler leg of the loop
steps += [
    phase("C: the loop closes — distw into the lead boiler; PRISTINE efficiency, 128000 L steam"),
    Step(f"gt6boiler place {BOIL} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler fill {BOIL} 800 distw", expect="filled 800/800 L of gt6:distilled_water (ACCEPTED)"),
    # 800 L × EU_PER_WATER 80 = the exact conversion budget (the W1c ledger form)
    Step(f"gt6boiler inject-hu {BOIL} 64000", expect="booked 64000/64000 HU (ACCEPTED)", sleep=3.0),
    # the deterministic immune arm: the :119 distilled criterion short-circuits the
    # scaling branch — efficiency stays 10000/10000 with zero dice
    Step(f"gt6boiler efficiency {BOIL}", expect="efficiency=10000/10000 (PRISTINE)"),
    # 800 × STEAM_PER_WATER_GLOBAL 160 (CS.java:242, the BOILER-side scope) = 128000 L →
    # the UT.Code.scale gauge 1+(128000×30)/320000 = 13
    Step(f"gt6boiler barometer {BOIL}", expect="barometer=13"),
    # the W1c budget discipline: clear the steam, water-first, plunge twice
    Step(f"gt6boiler plunge {BOIL}", expect="trashed "),
    Step(f"gt6boiler plunge {BOIL}"),
]

# ------------------------------------------------- D: the flip down + survival
steps += [
    phase("D: the flip down — no HU, doWork drains, running=false; the loop rig survived"),
    Step(f"gt6burner extinguish {BURNER}", expect="burning=false"),
    # poll-to-expect (p23, the s29 deterministic red): same shape as the flip up —
    # the drain takes a tick or two past the extinguish, the single shot fired before
    # the latch fell (running=true). Terminal state; resend until it lands.
    Step(f"gt6machine dryer check {DRYER}", expect="running=false", poll=10.0),
    Step(f"execute if block {DRYER} gt6:dryer", expect="Test passed"),
    Step(f"execute if block {BURNER} gt6:brick_burning_box", expect="Test passed"),
    Step(f"execute if block {BOIL} gt6:steam_boiler_tank_lead", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

# ------------------------------------------------- teardown
steps += [
    phase("E: teardown — the explicit restore over both sites (the pass-open bbox is the backstop)"),
    Step("fill 95 60 95 105 69 105 air", expect="filled"),
    Step("fill 117 60 117 123 67 123 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p14-loop-closure-chain",
    slug="p14loop",
    sites=gt6world.declare_sites(RIG, BOILER),
    preferred_ports=(25755, 25765),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
