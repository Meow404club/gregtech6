#!/usr/bin/env python3
"""p30-w6-t3-large-veins — the large-vein live acceptance chain (the t1 trees chain shape).

Chain semantics (the task-card RCON arm, the in-chain face): the large-vein Feature is
seed-deterministic per origin chunk — the sweep world's seed is per-boot random, so the
chain pins NO material identity; it pins the INVARIANTS instead:

  A the origin-cell recompute probe: a stone host box covering the full vein y-band in
    chunk (1,1) (blocks x16..31 z16..31, y0..126 — the 1/9 origin cell (1+402653184)%3==1),
    /place feature gt6:large_veins lands the vein slice (the pick and the shape come from
    the boot's world seed — arbitrary but fixed), and the box CHANGED (execute unless
    blocks against the pristine clone). Then the box is erased + re-laid and the SAME
    /place is repeated: the second slice must be BIT-IDENTICAL to the first (execute if
    blocks against the second clone) — the offline tiling test's live twin.
  B the second origin cell: the same probe in chunk (4,1) (origin cell (4,1)) — the
    origin GRID face (every 3x3 cell generates, not "only chunk (1,1)").
  C teardown: box + staging back to air (the pass-open bbox is the backstop).

The NATURAL-generation gate (forceload + region scan: >=3 distinct veins, the
cross-boundary slice continuity, indicator rocks on the surface) rides the card's
separate normal+seed world scan (the p26/p30 biome card form), not this chain.

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its box
first, so the pinned verdicts hold on both passes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w6_t3_veins.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x16..31 z16..31 (chunk 1,1) + x64..79 z16..31 (chunk 4,1), y0..126: the
# fresh overworld box band, clear of every registered machine band. The staging clones
# sit at x80..95 and x112..127 (staging1/2) and x144..159 (staging3), same z/y — all
# five bboxes declared so the pass-open forceload covers them (clone/fill into an
# unloaded chunk silently no-ops).
BOX1 = gt6world.Site(24, 63, 24, dx=8, dy=64, dz=8)      # chunk (1,1) box centre-form
BOX2 = gt6world.Site(72, 63, 24, dx=8, dy=64, dz=8)      # chunk (4,1) box
STAGING = gt6world.Site(120, 63, 24, dx=40, dy=64, dz=8) # staging1 (x80..95) + staging2 (x112..127) + staging3 (x144..159)

BOX = "16 0 16 31 126 31"      # chunk (1,1): 16x16x127 = 32512 <= the 32768 fill/clone cap
BOXB = "64 0 64 79 126 79"     # chunk (4,1)
S1 = "80 0 80 95 126 95"       # the pristine clone of BOX (pass A)
S2 = "112 0 112 127 126 127"   # the post-place clone of BOX (pass A compare)
S3 = "144 0 144 159 126 159"   # the pristine clone of BOXB (pass B)

steps = []

# ------------------------------------------------- A: the origin-cell recompute probe
steps += [
    phase("A: chunk (1,1) — the host box, the first /place, the change proof, the bit-exact recompute"),
    Step(f"fill {BOX} minecraft:stone"),
    Step(f"clone {BOX} 80 0 80"),  # staging1 = pristine stone (the t1 note: fill/clone reply empty)
    Step("place feature gt6:large_veins 24 90 24", expect="Placed"),
    # the vein CHANGED the box (the placement-happened face) — unless all-equal passes on difference
    Step(f"execute unless blocks 16 0 16 31 126 31 80 0 80 all", expect="Test passed"),
    Step(f"clone {BOX} 112 0 112"),  # staging2 = vein state (reply empty, the t1 note)
    Step(f"fill {BOX} minecraft:air"),   # erase, then re-lay the pristine box
    Step(f"fill {BOX} minecraft:stone"),
    Step("place feature gt6:large_veins 24 90 24", expect="Placed"),
    # THE determinism face: the recomputed slice is bit-identical to the first computation
    Step(f"execute if blocks 16 0 16 31 126 31 112 0 112 all", expect="Test passed"),
]

# ------------------------------------------------- B: the second origin cell
steps += [
    phase("B: chunk (4,1) — the origin GRID face (the next 3x3 cell generates too)"),
    Step(f"fill {BOXB} minecraft:stone"),
    Step(f"clone {BOXB} 144 0 144"),  # staging3 = pristine (reply empty, the t1 note)
    Step("place feature gt6:large_veins 72 90 72", expect="Placed"),
    Step(f"execute unless blocks 64 0 64 79 126 79 144 0 144 all", expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — box + staging back to air (the pass-open bbox is the backstop)"),
    Step(f"fill {BOX} minecraft:air"),
    Step(f"fill {BOXB} minecraft:air"),
    Step("fill 80 0 80 95 126 95 minecraft:air"),
    Step("fill 112 0 112 127 126 127 minecraft:air"),
    Step("fill 144 0 144 159 126 159 minecraft:air"),
    Step("execute if block 24 63 24 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w6-t3-veins",
    slug="p29_w6_t3_veins",
    sites=gt6world.declare_sites(BOX1, BOX2, STAGING),
    preferred_ports=(26209, 26219),      # this card's pinned rcon/query pair (ore_mech 26208/26218)
    response_timeout=30.0,               # the wide box fills sync-load chunks on first pass
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
