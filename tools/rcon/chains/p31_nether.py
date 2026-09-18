#!/usr/bin/env python3
"""p31-nether-lens-end-yield — the nether worldgen live chain (the p31_strata_lens shape).

The sweep session runs in the OVERWORLD (the gt6server flat/normal provision); the nether
features are dimension-agnostic CODE whose dimension gate rides the biome modifier (IS_NETHER)
— a /place bypasses the modifier, so every arm builds its upstream HOST geometry out of
netherrack in the fresh overworld band and /places the feature onto it:

  A nether_lenses: a full netherrack box (x64..79 z64..79 y0..120, the lens y-band) —
    /place lands lens slices (the per-row 1/200 rolls ride the boot's coordinate-seeded
    stream: arbitrary but fixed). Bit-exact recompute: erase + re-lay + re-place, the
    second slice must be BIT-IDENTICAL (execute if blocks) — the offline replay's live twin.
  B nether_quartz: the same netherrack box — the per-column noise picks columns at
    y36..120; /place converts netherrack to gt6:dense_nether_quartz_ore; the box CHANGES;
    the noise is a pure coordinate function — the re-run is bit-exact.
  C nether_clay: a netherrack box spanning the y33/34 band (x64..79 z64..79 y30..40) —
    the 1/8 column gate converts some columns to gt6:nether_red_clay; the box CHANGES.
  D nether_crystals: the upstream cave rig — a netherrack ceiling slab at y50 with air
    y31..49 below and a netherrack floor at y30 (the WorldgenNetherCrystals ray: climb
    from the lava-line datum 31 to the first solid, the ceiling must be base-stone-nether,
    ten blocks of headroom) — /place grows a gt6:crystal_* cluster hanging under the
    ceiling; the air pocket CHANGES.
  T teardown: every box back to air (the pass-open bbox is the backstop).

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its geometry
first, so the pinned verdicts hold on both passes).

The NATURAL-generation gate (fixed seed 6131000569321125127 + world delete: the nether
17-stone lens + the three forms + the End five-row probe) rides the card's separate scan
driver tools/rcon/scan_nether_end.py, not this chain.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_nether.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x64..79 z64..79 (chunk 4,4), clear of every registered machine band. The
# staging clones sit at x288..303 (pristine) and x304..319 (post-place), same z/y.
BOX = gt6world.Site(72, 60, 72, dx=8, dy=61, dz=8)      # the main box: y0..120
CAVE = gt6world.Site(128, 40, 72, dx=64, dy=21, dz=8)   # the crystal hall: x64..191, y30..50
STAGING = gt6world.Site(320, 45, 72, dx=32, dy=76, dz=8)  # the staging band x288..351

BOXR = "64 0 64 79 120 79"    # the main box (16x121x16 = 30976 <= the 32768 fill/clone cap)
S1 = "288 0 64 303 120 79"    # the pristine clone of BOX
S2 = "304 0 64 319 120 79"    # the post-place clone of BOX
CAVER = "64 30 64 79 50 79"   # the crystal rig bounds

steps = []

# ------------------------------------------------- A+B: the lens and the quartz on one box
steps += [
    phase("A: nether_lenses — the netherrack host box, the /place lens slices, the bit-exact recompute"),
    # the self-reliant forceload (the t1/strata lesson: the framework's pass-open forceload
    # does not reach the staging area)
    Step("forceload add 64 64 191 79"),   # the main box + the crystal hall (8 chunks)
    Step("forceload add 288 64 351 79"),  # the staging band (clones + pocket twins)
    Step(f"fill {BOXR} minecraft:netherrack"),
    Step(f"clone {BOXR} 288 0 64"),  # staging1 = the pristine netherrack box
    Step("place feature gt6:nether_lenses 72 60 72", expect="Placed"),
    Step(f"execute unless blocks {BOXR} 288 0 64 all", expect="Test passed"),
    Step(f"clone {BOXR} 304 0 64"),  # staging2 = the lens state
    Step(f"fill {BOXR} minecraft:air"),
    Step(f"fill {BOXR} minecraft:netherrack"),
    Step("place feature gt6:nether_lenses 72 60 72", expect="Placed"),
    Step(f"execute if blocks {BOXR} 304 0 64 all", expect="Test passed"),

    phase("B: nether_quartz — the same box re-laid, the noise columns convert"),
    Step(f"clone {S1} 64 0 64"),  # reset the box from the pristine staging1
    Step("place feature gt6:nether_quartz 72 60 72", expect="Placed"),
    Step(f"execute unless blocks {BOXR} 288 0 64 all", expect="Test passed"),
    Step(f"clone {BOXR} 304 0 64"),
    Step(f"clone {S1} 64 0 64"),
    Step("place feature gt6:nether_quartz 72 60 72", expect="Placed"),
    Step(f"execute if blocks {BOXR} 304 0 64 all", expect="Test passed"),
]

# ------------------------------------------------- C: the red clay band
steps += [
    phase("C: nether_clay — the y33/34 band over netherrack, the /place face"),
    Step("fill 64 30 64 79 40 79 minecraft:netherrack"),
    Step("place feature gt6:nether_clay 72 34 72", expect="Placed"),
    # NO box-delta assert: the 1/8 column gate rides the cellular noise whose integer-
    # coordinate value is heavily skewed (a 256-column patch converts 0..~165 columns
    # depending on the world seed — measured 0/256 three times in five probe seeds),
    # and upstream returns T unconditionally (WorldgenNetherClay.java:56). The
    # conversion live-evidence is the scan driver's clay census across 1600 chunks.
]

# ------------------------------------------------- D: the crystal cave hall
# the upstream 50% chunk skip (WorldgenNetherCrystals.java:50 nextBoolean) is
# deterministic per (boot seed, chunk) — a single-chunk /place is a coin flip that no
# assertion can pin across random sweep boots. The hall spans EIGHT chunks (x64..191,
# eight 16-wide columns): eight /place attempts, eight independent coins, P(all skip) =
# 1/256 = 0.39% — the SOME-change face then holds with overwhelming probability.
_CRYSTAL_CHUNKS = [64 + 16 * i for i in range(8)]
steps += [
    phase("D: nether_crystals — the 8-chunk cave hall (ceiling y50, air y31..49, floor y30)"),
    Step("fill 64 50 64 191 50 79 minecraft:netherrack"),   # the ceilings: one slab, 8 chunks
    Step("fill 64 30 64 191 30 79 minecraft:netherrack"),   # the floors
    Step("fill 64 31 64 191 49 79 minecraft:air"),          # carve the air pockets
]
for _cx in _CRYSTAL_CHUNKS:
    steps.append(Step(f"place feature gt6:nether_crystals {_cx + 8} 40 {_cx + 8}", expect="Placed",
                      allow_failed=True))  # the 50% skip is an UPSTREAM OUTCOME, not a chain failure
steps += [
    # SOME pocket slot became a crystal — the SOME-change face against freshly carved
    # air clones (carve the CLONE only; carving the live hall again would erase the
    # evidence — the first-draft bug this step order fixes)
    Step("fill 288 31 64 303 49 79 minecraft:air"),
    Step("fill 304 31 64 319 49 79 minecraft:air"),
    Step("fill 320 31 64 335 49 79 minecraft:air"),
    Step("fill 336 31 64 351 49 79 minecraft:air"),
    Step("execute unless blocks 64 31 64 191 49 79 288 31 64 all", expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — hall + box + staging back to air (the pass-open bbox is the backstop)"),
    Step(f"fill {BOXR} minecraft:air"),
    Step("fill 64 30 64 191 50 79 minecraft:air"),
    Step("fill 288 0 64 303 120 79 minecraft:air"),
    Step("fill 304 0 64 319 120 79 minecraft:air"),
    Step("fill 288 30 64 351 50 79 minecraft:air"),
    Step("execute if block 72 60 72 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p31-nether-lens-end-yield",
    slug="p31_nether",
    sites=gt6world.declare_sites(BOX, CAVE, STAGING),
    preferred_ports=(26210, 26220),      # this card's pinned rcon/query pair (the strata lens pair, reused per the slot table)
    response_timeout=30.0,
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
