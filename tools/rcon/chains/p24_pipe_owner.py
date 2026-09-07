#!/usr/bin/env python3
"""p24-pipe-owner — the pipe ownership live chain (declarative framework, the
p24_dye_chemical_fluids shape).

Chain semantics (task p24-pipe-owner ACCEPTANCE — "RCON /gt6pipe 扩 ownable 子命令
（applyFoam 替身强制写点）+ stat 增 ownable/owner + 新链四门逐门验证+回归臂，双腿 [0,0]"):

  A the baseline arm (the default-pipe zero-regression row): a fresh pipe stats
    "ownable false owner none" and toggles both ways — ownable=false short-circuits
    allowInteraction (upstream 10ConnectorRendered:154-156 !mOwnable opening), so the
    plain pipe behaves byte-for-byte as before the card.

  B the lock arm (the break-... the self gate, upstream 06Covers:141 counterpart):
    `ownable <pos> 1 <uuid>` is the applyFoam stand-in — the ONLY live owner write point
    while the foam family sleeps in the P10 pool (upstream 10ConnectorRendered:159-166,
    :161-162 mOwnable/mOwner). The console toggle (null identity = nobody, the :107
    `aEntity != null` arm) is REJECTED on the locked pipe — the verify arm本体.

  C the predicate arms on the toggles: null-owner (ownable persisted, owner cleared)
    passes everyone (upstream 03TicksAndSync:107 `mOwner == null` arm); re-locking
    re-denies; `ownable <pos> 0` resets both fields (the removeFoam :177-183 form) and
    the toggle succeeds again — the 复原 arm.

  D the placement support-side neighbour gate (upstream 09Connector:85-86 return T):
    a pipe placed against a LOCKED support stays connections 0 (and stays itself
    unlocked/ownerless); placed against the same support after `ownable 0` it connects
    (connections 32 = SBIT[5], the OPOS[4]=5 flip) — the placement regression arm.

The break/use gates are client-player paths (no Player is constructible over RCON) —
their semantics live in GTPipeOwnerTest (the static destroy seam + the use PASS) over
the same allowInteraction predicate this chain exercises live.

passes=2 is the idempotency proof (the [0,0] of this chain); the pass-open bbox
cleanup re-airs the pipe band between passes.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p24_pipe_owner.py
Dual: python3 tools/rcon/chains/p24_pipe_owner.py --node 1.21.1-neoforge
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

# the lock identity — a fixed, log-readable UUID (no Player is ever constructed;
# the console toggles carry null and this is the pipe's recorded owner)
UUID_A = "11111111-2222-3333-4444-555555555555"

# the pipe band x=393..394, z=20 — clear of the neighbours (p21 370/378, p24dye 386);
# A = the lock subject, B = the placement-gate probe west of A (A sits at B.relative(OPOS[4]=5))
A = gt6world.Site(394, 64, 20)
B = gt6world.Site(393, 64, 20)
PA, PB = F(A), F(B)

steps = []

# ------------------------------------------------- A: the baseline (ownable=false)
steps += [
    phase("A: baseline — the default pipe is unlocked and toggles both ways"),
    Step(f"gt6pipe place {PA} 1", expect="GT6 pipe placed at"),
    Step(f"gt6pipe stat {PA}", expect="ownable false owner none"),
    Step(f"gt6pipe toggle {PA} 1", expect="ok, connections 2"),   # open end UP (SBIT[1]=2)
    Step(f"gt6pipe toggle {PA} 1", expect="ok, connections 0"),   # ...and back down — zero regression
]

# ------------------------------------------------- B: the lock arm (the applyFoam stand-in)
steps += [
    phase("B: ownable 1 <uuid> — the forced write; the console toggle is REJECTED (self gate)"),
    Step(f"gt6pipe ownable {PA} 1 {UUID_A}", expect=f"ownable true, owner {UUID_A} (FORCED write"),
    Step(f"gt6pipe stat {PA}", expect=f"ownable true owner {UUID_A}"),
    Step(f"gt6pipe toggle {PA} 1", expect="FAILED, connections 0", allow_failed=True),
]

# ------------------------------------------------- C: the predicate arms + the 复原 arm
steps += [
    phase("C: null-owner passes everyone; re-lock re-denies; ownable 0 resets (removeFoam form)"),
    Step(f"gt6pipe ownable {PA} 1", expect="ownable true, owner none"),
    Step(f"gt6pipe toggle {PA} 2", expect="ok, connections 4"),   # NORTH open end (SBIT[2]=4)
    Step(f"gt6pipe ownable {PA} 1 {UUID_A}", expect=f"ownable true, owner {UUID_A}"),
    Step(f"gt6pipe toggle {PA} 3", expect="FAILED, connections 4", allow_failed=True),
    Step(f"gt6pipe ownable {PA} 0", expect="ownable false, owner none (reset, the removeFoam form)"),
    Step(f"gt6pipe toggle {PA} 3", expect="ok, connections 12"),  # 4|8 — the 复原 arm
]

# ------------------------------------------------- D: the placement neighbour gate
steps += [
    phase("D: place against a LOCKED support → connections 0; against the unlocked one → 32"),
    Step(f"gt6pipe ownable {PA} 1 {UUID_A}", expect=f"ownable true, owner {UUID_A}"),
    Step(f"gt6pipe place {PB} 4", expect="connections 0"),        # the upstream :86 return T
    Step(f"gt6pipe stat {PB}", expect="connections 0 ioMask 0 ownable false owner none"),
    Step(f"gt6pipe ownable {PA} 0", expect="ownable false, owner none"),
    Step(f"setblock {PB} air", expect="Changed the block"),
    Step(f"gt6pipe place {PB} 4", expect="connections 32"),       # OPOS[4]=5 EAST — the placement regression arm
]

# ------------------------------------------------- teardown
steps += [
    phase("E: teardown — the explicit restore over the band (the pass-open bbox is the backstop)"),
    Step(f"fill 392 62 19 396 66 22 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p24-pipe-owner",
    slug="p24pipeowner",
    sites=gt6world.declare_sites(A, B),
    preferred_ports=(26108, 26118),      # this card's pinned rcon/query pair (after p24dye 26106/26116)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
