#!/usr/bin/env python3
"""p31-strata-lens — the strata-lens live acceptance chain (the p29_w6_t3_veins chain shape).

Chain semantics (the task-card RCON arm, the in-chain face): the strata-lens Feature is
seed-deterministic per origin chunk — the sweep world's seed is per-boot random, so the
chain pins NO material identity; it pins the INVARIANTS instead:

  A the lens-origin recompute probe: a stone host box covering the full lens y-band in
    chunk (4,4) (blocks x64..79 z64..79, y0..126 — the lens lattice cell (4+402653184)
    %9==1 on both axes; cells sit at cc = 4 mod 9), /place feature gt6:strata_lenses
    lands the lens slice (the pick and the shape come from the boot's world seed —
    arbitrary but fixed; every lens origin draws exactly one row, no scarcity gate), and
    the box CHANGED (execute unless blocks against the pristine clone). Then the box is
    erased + re-laid and the SAME /place is repeated: the second slice must be
    BIT-IDENTICAL to the first (execute if blocks against the second clone) — the
    offline tiling test's live twin.
  B the second lens cell: the same probe in chunk (13,4) (cell (13+402653184)%9==1) —
    the origin GRID face (the 9x9 lattice generates, not "only chunk (4,4)").
  C teardown: box + staging back to air (the pass-open bbox is the backstop).

The NATURAL-generation gate (fixed seed 6131000569321125127 + world delete: the 5
marker stones each >= 1 lens hit, the cross-chunk boundary continuity probe, the
same-seed regenerate per-chunk count equality) rides the card's separate scan driver
tools/rcon/scan_strata_lens.py (the p30t3_vein_scan form), not this chain.

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its box
first, so the pinned verdicts hold on both passes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_strata_lens.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x64..79 z64..79 (chunk 4,4) + x208..223 z64..79 (chunk 13,4), y0..126: the
# fresh overworld box band, clear of every registered machine band. The staging clones
# sit at x288..303 (staging1), x304..319 (staging2) and x320..335 (staging3), same z/y —
# all five bboxes declared so the pass-open forceload covers them (clone/fill into an
# unloaded chunk silently no-ops).
BOX1 = gt6world.Site(72, 63, 72, dx=8, dy=64, dz=8)      # chunk (4,4) box centre-form
BOX2 = gt6world.Site(216, 63, 72, dx=8, dy=64, dz=8)     # chunk (13,4) box
STAGING = gt6world.Site(312, 63, 72, dx=24, dy=64, dz=8) # staging1+2 (x288..319) + staging3 (x320..335)

BOX = "64 0 64 79 126 79"      # chunk (4,4): 16x16x127 = 32512 <= the 32768 fill/clone cap
BOXB = "208 0 64 223 126 79"   # chunk (13,4)
S1 = "288 0 64 303 126 79"     # the pristine clone of BOX (pass A)
S2 = "304 0 64 319 126 79"     # the post-place clone of BOX (pass A compare)
S3 = "320 0 64 335 126 79"     # the pristine clone of BOXB (pass B)

steps = []

# ------------------------------------------------- A: the lens-origin recompute probe
steps += [
    phase("A: chunk (4,4) — the host box, the first /place, the change proof, the bit-exact recompute"),
    # the neo-leg live finding (the t3 lesson): the framework's pass-open forceload does
    # NOT reach the staging area on 1.21.1 — the chain forceloads its own five bboxes
    # explicitly (self-reliant, both legs)
    Step("forceload add 64 64 79 79"),
    Step("forceload add 288 64 303 79"),
    Step("forceload add 304 64 319 79"),
    Step("forceload add 320 64 335 79"),
    Step("forceload add 208 64 223 79"),
    Step(f"fill {BOX} minecraft:stone"),
    Step(f"clone {BOX} 288 0 64"),  # staging1 = pristine stone (the t1 note: fill/clone reply empty)
    Step("place feature gt6:strata_lenses 72 90 72", expect="Placed"),
    # the lens CHANGED the box (the placement-happened face) — unless all-equal passes on difference
    Step(f"execute unless blocks {BOX} 288 0 64 all", expect="Test passed"),
    Step(f"clone {BOX} 304 0 64"),  # staging2 = lens state (reply empty, the t1 note)
    Step(f"fill {BOX} minecraft:air"),   # erase, then re-lay the pristine box
    Step(f"fill {BOX} minecraft:stone"),
    Step("place feature gt6:strata_lenses 72 90 72", expect="Placed"),
    # THE determinism face: the recomputed slice is bit-identical to the first computation
    Step(f"execute if blocks {BOX} 304 0 64 all", expect="Test passed"),
]

# ------------------------------------------------- B: the second lens cell
steps += [
    phase("B: chunk (13,4) — the origin GRID face (the next 9x9 lattice cell generates too)"),
    Step(f"fill {BOXB} minecraft:stone"),
    Step(f"clone {BOXB} 320 0 64"),  # staging3 = pristine (reply empty, the t1 note)
    Step("place feature gt6:strata_lenses 216 90 72", expect="Placed"),
    Step(f"execute unless blocks {BOXB} 320 0 64 all", expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — box + staging back to air (the pass-open bbox is the backstop)"),
    Step(f"fill {BOX} minecraft:air"),
    Step(f"fill {BOXB} minecraft:air"),
    Step("fill 288 0 64 303 126 79 minecraft:air"),
    Step("fill 304 0 64 319 126 79 minecraft:air"),
    Step("fill 320 0 64 335 126 79 minecraft:air"),
    Step("execute if block 72 63 72 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p31-strata-lens",
    slug="p31_strata_lens",
    sites=gt6world.declare_sites(BOX1, BOX2, STAGING),
    preferred_ports=(26210, 26220),      # this card's pinned rcon/query pair (t3 veins 26209/26219)
    response_timeout=30.0,               # the wide box fills sync-load chunks on first pass
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
