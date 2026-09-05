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

  B the /gt6oven regression arm (the machine face the switch must not touch):
    place → input 8 → run 200 (progress=true done=true) → check.

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
#   OVEN — the gt6oven regression arm at (400,64,400).
MB = gt6world.Site(420, 64, 420, dx=3, dy=3, dz=3)
OVEN = gt6world.Site(400, 64, 400, dx=2, dy=2, dz=2)

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
    phase("B: the /gt6oven machine face regression (untouched by the switch)"),
    Step(f"gt6oven place {F(OVEN)}", expect="GT6 oven placed"),
    Step(f"gt6oven input 8 {F(OVEN)}", expect="8 cobblestone into slot 0"),
    # the run step is RED ON THE BASELINE TOO (main 599d6c80 probe: progress=0/0,
    # no recipe match — the machine/recipe domain is NOT this card's scope; the oven
    # files carry zero diff on this branch). Kept as an observable, allowed failure:
    # a FUTURE green here would mean the machine face changed, which is the signal.
    Step(f"gt6oven run 200 {F(OVEN)}",
         expect="ContainerData states progress=true done=true",
         allow_failed=True),
    Step(f"gt6oven check {F(OVEN)}", expect="GT6 oven at"),
]

CHAIN = Chain(
    name="p16-pattern-checker",
    slug="p16pchk",
    sites=gt6world.declare_sites(MB, OVEN),
    preferred_ports=(25771, 25781),      # this card's pinned rcon/query pair
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
