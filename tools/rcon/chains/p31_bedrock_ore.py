#!/usr/bin/env python3
"""p31-bedrock-ore — the bedrock-ore live chain (the p31_strata_lens chain shape).

Chain semantics (the task-card RCON arm): the bedrock-ore Feature rolls PER CHUNK with
1/P row gates (the upstream WorldgenOresBedrock.java:142 face) on the boot's world seed,
so a /place is almost always a refusal (the OW roll mass is ~0.5%/chunk — 200:1 against).
The chain therefore pins the faces that hold at EVERY seed:

  A the live REGISTRATION face (the id686 lesson: a registration face needs live proof,
    the census is not enough): three bedrock-ore blocks (large coal, small graphite,
    large diamond) setblock into place and read back — a registered id changes the block,
    an unregistered one would fail the step. The blocks are the noLootTable/unmineable
    band: no drop/arm assertions (the future drill card's face).
  B the /place gate face: a pristine arena (a bedrock floor at y=-64 + a deepslate body,
    the Feature's isBedrockFace gate satisfied) is snapshotted, the feature is placed at
    the arena centre, and the arena is asserted UNCHANGED — the 1/P refusal, the
    overwhelming-likely verdict (a freak hit fails this step; the rerun-precedence
    judgment applies, the p30-w6-t2 single-flake precedent). The place is repeated after
    re-laying the pristine arena and the emptiness is re-asserted (the deterministic
    empty face; the DECISION-level live proof of natural generation rides the card's
    separate scan driver tools/rcon/scan_bedrock_ore.py — fixed seed + the calibrated
    64x64 window — not this chain).
  C teardown: arena + staging + probes back to air.

The arena box x64..79 z96..111 y-64..62 = 16x16x127 = 32512 <= the 32768 fill/clone cap
(the chunk (4,6) band; the staging clones sit at x288..303 and x304..319, same z/y — the
band is disjoint from every registered sweep band, the strata z=64 band stays clear).

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its boxes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_bedrock_ore.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — the chunk (4,6) band, x64..79 z96..111, clear of every registered band.
# The staging clones sit at x288..303 (staging1) and x304..319 (staging2), same z/y —
# all four bboxes declared so the pass-open forceload covers them (clone/fill into an
# unloaded chunk silently no-ops).
ARENA = gt6world.Site(72, 0, 104, dx=8, dy=64, dz=8)     # the arena centre-form (chunk 4,6)
STAGING = gt6world.Site(304, 0, 104, dx=24, dy=64, dz=8) # staging1 (x288..319) + staging2 (x304..319)
PROBE = gt6world.Site(72, 70, 96, dx=4, dy=2, dz=0)      # the three single-block probes at y70

BOX = "64 -64 96 79 62 111"      # the arena: 16x16x127 = 32512 <= the 32768 cap
S1 = "288 -64 96 303 62 111"     # the pristine clone of BOX
FLOOR = "64 -64 96 79 -64 111"   # the bedrock gate layer (the Feature's isBedrockFace reads y=-64)
BODY = "64 -63 96 79 62 111"     # the deepslate body

steps = []

# ------------------------------------------------- A: the live registration face
steps += [
    phase("A: the bedrock-ore registration face — three per-pair blocks setblock + read back"),
    Step("forceload add 64 96 79 111"),
    Step("forceload add 288 96 303 111"),
    Step("forceload add 304 96 319 111"),
    Step("setblock 68 70 96 gt6:ore_bedrock_coal", expect="Changed the block"),
    Step("execute if block 68 70 96 gt6:ore_bedrock_coal", expect="Test passed"),
    Step("setblock 72 70 96 gt6:ore_small_bedrock_graphite", expect="Changed the block"),
    Step("execute if block 72 70 96 gt6:ore_small_bedrock_graphite", expect="Test passed"),
    Step("setblock 76 70 96 gt6:ore_bedrock_diamond", expect="Changed the block"),
    Step("execute if block 76 70 96 gt6:ore_bedrock_diamond", expect="Test passed"),
]

# ------------------------------------------------- B: the /place gate face
steps += [
    phase("B: the arena, the snapshot, the /place refusal (the 1/P gate, ~200:1 against a hit)"),
    Step(f"fill {FLOOR} minecraft:bedrock"),
    Step(f"fill {BODY} minecraft:deepslate"),
    Step(f"clone {BOX} 288 -64 96"),   # staging1 = the pristine arena (reply empty, the t1 note)
    # the 1/P refusal IS the command error: vanilla PlaceCommand.placeFeature throws
    # ERROR_FEATURE_FAILED when place() returns false (PlaceCommand.java:243-244) --
    # the feature rolled its rows on the boot seed and (overwhelmingly likely) placed
    # nothing; the arena must also equal its pristine snapshot
    Step("place feature gt6:bedrock_ores 72 -64 104", expect="Could not place feature"),
    Step(f"execute if blocks {BOX} 288 -64 96 all", expect="Test passed"),
    # the deterministic empty face: re-lay the pristine arena, place again, refused again
    Step(f"fill {FLOOR} minecraft:bedrock"),
    Step(f"fill {BODY} minecraft:deepslate"),
    Step("place feature gt6:bedrock_ores 72 -64 104", expect="Could not place feature"),
    Step(f"execute if blocks {BOX} 288 -64 96 all", expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — arena + staging + probes back to air (the pass-open bbox is the backstop)"),
    Step(f"fill {BOX} minecraft:air"),
    Step("fill 288 -64 96 303 62 111 minecraft:air"),
    Step("fill 304 -64 96 319 62 111 minecraft:air"),
    Step("fill 64 68 96 79 72 96 minecraft:air"),
    Step("execute if block 68 70 96 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p31-bedrock-ore",
    slug="p31_bedrock_ore",
    sites=gt6world.declare_sites(ARENA, STAGING, PROBE),
    preferred_ports=(26211, 26221),      # this card's pinned rcon/query pair (strata 26210/26220)
    response_timeout=30.0,               # the wide box fills sync-load chunks on first pass
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
