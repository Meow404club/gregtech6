#!/usr/bin/env python3
"""p16-form-scaffold — the SET scaffold acceptance chain (/gt6multiblock form).

Chain semantics (task p16-form-scaffold ACCEPTANCE):

  A the positive arm (give 64 bricks → missing-parts scene → form):
    place (controller only, the 25-brick shell MISSING) → check (the 缺件现场:
    block_formed=false linked_parts=0/25 first_failed_cell=#0) → form 64 →
    "formed=true okay=true stock 64 -> 39 first_failed_cell=none" — 25 consumed
    (the pattern declares 26 forming cells; the 26th is the controller's own
    cell and passes free, so a from-scratch form spends exactly 25) → check
    (block_formed=true linked_parts=25/25 first_failed_cell=none — 缺件格全绑定).

  B idempotence: form again on the formed structure — the fake player stock is
    refreshed per command (clearContent + 64), the early return consumes
    nothing: "stock 64 -> 64".

  C the negative arm (背包不足→不成型不扣): a second controller, form 10 →
    "formed=false okay=false stock 10 -> 10 first_failed_cell=insufficient
    stock: needs 25, has 10" and the follow-up check shows NOTHING was placed
    (block_formed=false linked_parts=0/25) — the scaffold is transactional.

  D the single-missing-cell shape: hole on the formed pilot → form 1 →
    "stock 1 -> 0", formed again.

  E the /gt6multiblock check-arm regression over the same scene (the check
    report the p16-pattern-checker chain pinned, asserted once here too).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p16_form_scaffold.py
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
#   MB  — the form pilot at (440,64,440) facing north (centre (440,64,441)).
#   NEG — the short-stock arm at (450,64,450) facing north (centre (450,64,451)).
#   HOLE — a shell brick of MB, y below the centre (within MB's bbox).
MB = gt6world.Site(440, 64, 440, dx=3, dy=3, dz=3)
NEG = gt6world.Site(450, 64, 450, dx=3, dy=3, dz=3)

HOLE = "440 63 441"  # MB shell cell centre+(0,-1,0)

steps = []

# ------------------------------------------------- A: the positive arm
steps += [
    phase("A: give 64 → 缺件现场 → form → formed + 扣 25 + 全绑定"),
    Step(f"gt6multiblock place {F(MB)}", expect="coke oven controller placed"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=false linked_parts=0/25 first_failed_cell=#0"),
    Step(f"gt6multiblock form {F(MB)} 64",
         expect="formed=true okay=true stock 64 -> 39 first_failed_cell=none"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=true linked_parts=25/25 first_failed_cell=none"),
]

# ------------------------------------------------- B: idempotence
steps += [
    phase("B: form on the formed structure consumes nothing"),
    Step(f"gt6multiblock form {F(MB)} 64",
         expect="formed=true okay=true stock 64 -> 64 first_failed_cell=none"),
]

# ------------------------------------------------- C: the negative arm
steps += [
    phase("C: 背包不足 → 不成型不扣（事务性）"),
    Step(f"gt6multiblock place {F(NEG)}", expect="coke oven controller placed"),
    Step(f"gt6multiblock form {F(NEG)} 10",
         expect="formed=false okay=false stock 10 -> 10"),
    Step(f"gt6multiblock form {F(NEG)} 10",
         expect="insufficient stock: needs 25, has 10"),
    Step(f"gt6multiblock check {F(NEG)}",
         expect="block_formed=false linked_parts=0/25"),
]

# ------------------------------------------------- D: the one-hole recovery
steps += [
    phase("D: hole → form 1 → the single missing cell scaffolded"),
    Step(f"gt6multiblock hole {HOLE}", expect="GT6 part broken"),
    Step(f"gt6multiblock check {F(MB)}", expect="block_formed=false"),
    Step(f"gt6multiblock form {F(MB)} 1",
         expect="formed=true okay=true stock 1 -> 0 first_failed_cell=none"),
    Step(f"gt6multiblock check {F(MB)}",
         expect="block_formed=true linked_parts=25/25 first_failed_cell=none"),
]

CHAIN = Chain(
    name="p16-form-scaffold",
    slug="p16form",
    sites=gt6world.declare_sites(MB, NEG),
    preferred_ports=(25776, 25786),      # this card's pinned rcon/query pair
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
