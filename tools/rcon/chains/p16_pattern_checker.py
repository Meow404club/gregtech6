#!/usr/bin/env python3
"""p16-pattern-checker — the shared structure checker acceptance chain.

Chain semantics (task p16-pattern-checker ACCEPTANCE):

  A the checker pilot (cokeoven now checks THROUGH the bound pattern):
    place → check on the EMPTY world (block_formed=false AND the new
    first_failed_cell= diagnostic pointing at the first declared miss) →
    frame (25 bricks) → check formed (first_failed_cell=none) → hole on the
    (1,0,0) shell cell → check broken with first_failed_cell=#21 — the exact
    declaration index of (1,0,0) in the upstream loop order — → wand (the
    placing+linking two-pass) → check formed again (first_failed_cell=none).
    Each check asserts ONE spanning substring of the single-line report
    (block_formed → linked_parts → first_failed_cell), so one Step pins all
    three report fields byte-exactly.

  B the /gt6oven regression arm (the machine face the switch must not touch),
    grid-fed since p8-d3 (ENERGY_FAKE_SOURCE defaults false) — the p8 e2e
    gen->wire->oven idiom, all through the existing command faces:
    place -> input 8 -> check energy=0 (the no-fake-source regime nail) ->
    gen place/volt -> 2x wire (placed LAST: GTWireBlock has no retro-scan,
    so the energy neighbours must exist first) -> neighbors asserts BOTH
    sides connected -> mode on -> poll check until output=stonex8. The poll
    is THE hard assertion: the continuous source must finish all eight
    smelts on natural ticks or the chain is RED (the diagnostic's 8x stone
    discriminator, no NBT priming). The old `run 200` trio assertion
    (progress=true done=true idle=true) was a fake-source-era artifact and
    is unsatisfiable on grid-fed: `run` drives ONLY the oven's own
    dispatcher (GTOvenCommand.java:143), the gen/wire pumps ride the real
    ticker, and done (mSuccessful) is a completion instant that cannot
    co-occur with idle (input exhausted, mMaxProgress cleared) inside one
    run loop — probed live both ways (mode-on-immediate and post-smelt run,
    both FAILED while the same feed completes 8/8 stone on natural ticks).

Run:  python3 tools/rcon/chains/p16_pattern_checker.py
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

# The sites.
#   MB   — the multiblock checker pilot at (420,64,420) facing north
#          (structure centre (420,64,421), the 3x3x3 shell around it).
#   OVEN — the gt6oven regression arm at (400,64,400); the feed rig fills
#          the arm's own footprint (wire 401, gen 402 — both inside the
#          oven's declared dx=2 bounds, declared explicitly anyway).
MB = gt6world.Site(420, 64, 420, dx=3, dy=3, dz=3)
OVEN = gt6world.Site(400, 64, 400, dx=2, dy=2, dz=2)
WIRE = gt6world.Site(401, 64, 400)
GEN = gt6world.Site(402, 64, 400)

HOLE = "421 64 421"  # the shell cell centre+(1,0,0) — declaration index #21

steps = []

# ------------------------------------------------- A: the checker pilot
steps += [
    phase("A1: greenfield — the check reports the first declared miss"),
    Step(f"gt6multiblock place {F(MB)}", expect="coke oven controller placed"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=false linked_parts=0/25 first_failed_cell=#0"),
    phase("A2: frame → formed, the diagnostic goes quiet"),
    Step(f"gt6multiblock frame {F(MB)}", expect="25 bricks placed"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=true linked_parts=25/25 first_failed_cell=none"),
    phase("A3: one hole — the diagnostics point at the exact declaration index"),
    Step(f"gt6multiblock hole {HOLE}", expect="GT6 part broken"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=false linked_parts=24/25 first_failed_cell=#21"),
    phase("A4: wand → the two-pass recovery, diagnostics quiet again"),
    Step(f"gt6multiblock wand {F(MB)}", expect="GT6 multiblock wand check OK"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=true linked_parts=25/25 first_failed_cell=none"),
]

# ------------------------------------------------- B: the gt6oven regression
steps += [
    phase("B: the /gt6oven machine face regression (grid-fed, the p8 e2e idiom)"),
    Step(f"gt6oven place {F(OVEN)}", expect="GT6 oven placed"),
    Step(f"gt6oven input 8 {F(OVEN)}", expect="8 cobblestone into slot 0"),
    # the regime nail: before ANY feed exists the oven reports energy=0 —
    # the ENERGY_FAKE_SOURCE=false default is what makes this chain honest.
    Step(f"gt6oven check {F(OVEN)}",
         expect="progress=0/0 energy=0 minenergy=0"),
    # the feed rig: energy neighbours BEFORE the wire (no retro-scan), and
    # the neighbors report proves the chain is wired, not NBT-primed.
    Step(f"gt6energy place {F(GEN)}", expect="GT6 energy source placed"),
    Step(f"gt6energy volt {F(GEN)} 32", expect="voltage 32"),
    Step(f"gt6wire place 2x {F(WIRE)}", expect="GT6 wire placed"),
    Step(f"gt6wire neighbors {F(WIRE)}",
         expect="west=TileEntityOven(connected) east=GTEnergySourceBlockEntity(connected)"),
    Step(f"gt6energy mode {F(GEN)} on", expect="emitting true"),
    # THE hard assertion: the continuous source must finish all eight smelts
    # (32 EU x 1 A through the 2x wire = 32 progress/tick, 8 x 256 = 2048
    # progress = ~64 ticks). Poll-to-expect, chain RED on timeout.
    # The spanning expect pins input exhausted AND output full in one substring,
    # so the mid-run line (input=cobblestoneNx ...) cannot shadow it. The out
    # column renders via ItemStack.toString — plain on 1.20.1, namespaced on
    # 21.1 — so the full line is per-leg (Step.node_expects, the p23 s17
    # 21.1-only rendering red; a bare-name expect is banned here: "stonex8" is
    # a substring of input=cobblestonex8 and hits at zero seconds).
    Step(f"gt6oven check {F(OVEN)}",
         expect="input=airx0 output=stonex8",
         node_expects={"1.21.1": "input=minecraft:airx0 output=minecraft:stonex8"},
         poll=30.0),
]

CHAIN = Chain(
    name="p16-pattern-checker",
    slug="p16pchk",
    sites=gt6world.declare_sites(MB, OVEN, WIRE, GEN),
    preferred_ports=(25771, 25781),      # this card's pinned rcon/query pair
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
