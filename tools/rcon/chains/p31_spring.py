#!/usr/bin/env python3
"""p31-spring — the bedrock-spring live chain (the p31_bedrock_ore chain shape).

Chain semantics (the task-card RCON arm): the spring Feature rolls PER CHUNK — the ore
replay (~0.5% refusal) then the first-hit-wins spring draw (the OW band ≈ 3.5%/chunk,
Loader_Worldgen.java:782-788) — so a /place is overwhelmingly a refusal and the chain
pins the faces that hold at EVERY seed:

  A the live REGISTRATION face (the id686 lesson: a registration face needs live proof):
    the six spring block faces setblock + read back — the five NEW LiquidBlocks
    (gt6:liquid_{extra_heavy,heavy,medium,light}_oil_block + gt6:water_geothermal_block,
    task p31-fluid-spring spec ①) plus the p5 gt6:natural_gas_block face. A registered id
    changes the block; an unregistered one fails the step.
  B the gate face, DETERMINISTIC: a body WITHOUT the bedrock floor cannot pass the
    Feature's isBedrockFace gate (WorldgenFluidSpring.java:66-67) regardless of every
    roll — place refused at 100%, arena unchanged. Then the bedrock-floored arena face:
    the place rides the 1/P rolls and is (overwhelmingly) refused — the worldgen roll
    verdict (a freak ~3.5% hit fails the step; the rerun-precedence judgment, the
    p30-w6-t2 single-flake precedent). The DECISION-level positive proof of natural
    generation (fixed seed 6131000569321125127 + world delete: the ~144-spring window,
    the six-kind hits, the ore/spring mutual exclusion, the dome Y-span) rides the
    card's separate scan driver tools/rcon/scan_fluid_spring.py, not this chain.
  C teardown: arena + staging back to air.

The arena box x64..79 z128..143 y-64..62 = 16x16x127 = 32512 <= the 32768 fill/clone cap
(chunk (4,8) — x/z-disjoint from the p31_bedrock_ore band (z96..111) and the p31_nether
band (z64..79); the staging clones sit at x288..303 and x304..319, same z/y — the band is
disjoint from every registered sweep band).

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its boxes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_spring.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — the chunk (4,8) band, x64..79 z128..143, clear of every registered band.
ARENA = gt6world.Site(72, 0, 136, dx=8, dy=64, dz=8)      # the arena centre-form (chunk 4,8)
STAGING = gt6world.Site(304, 0, 136, dx=24, dy=64, dz=8)  # staging1 (x288..303) + staging2 (x304..319)
PROBE = gt6world.Site(72, 70, 128, dx=6, dy=2, dz=0)      # the six single-block probes at y70

BOX = "64 -64 128 79 62 143"      # the arena: 16x16x127 = 32512 <= the 32768 cap
S1 = "288 -64 128 303 62 143"     # the pristine clone of BOX
S2 = "304 -64 128 319 62 143"     # the second pristine clone
FLOOR = "64 -64 128 79 -64 143"   # the bedrock gate layer (the Feature's isBedrockFace reads y=-64)
BODY = "64 -63 128 79 62 143"     # the deepslate body

steps = []

# ------------------------------------------------- A: the live registration face
steps += [
    phase("A: the spring block-face registration — six blocks setblock + read back"),
    Step("forceload add 64 128 79 143"),
    Step("forceload add 288 128 303 143"),
    Step("forceload add 304 128 319 143"),
    Step("setblock 72 70 128 gt6:liquid_extra_heavy_oil_block", expect="Changed the block"),
    Step("execute if block 72 70 128 gt6:liquid_extra_heavy_oil_block", expect="Test passed"),
    Step("setblock 74 70 128 gt6:liquid_heavy_oil_block", expect="Changed the block"),
    Step("execute if block 74 70 128 gt6:liquid_heavy_oil_block", expect="Test passed"),
    Step("setblock 76 70 128 gt6:liquid_medium_oil_block", expect="Changed the block"),
    Step("execute if block 76 70 128 gt6:liquid_medium_oil_block", expect="Test passed"),
    Step("setblock 78 70 128 gt6:liquid_light_oil_block", expect="Changed the block"),
    Step("execute if block 78 70 128 gt6:liquid_light_oil_block", expect="Test passed"),
    Step("setblock 72 72 128 gt6:water_geothermal_block", expect="Changed the block"),
    Step("execute if block 72 72 128 gt6:water_geothermal_block", expect="Test passed"),
    Step("setblock 74 72 128 gt6:natural_gas_block", expect="Changed the block"),
    Step("execute if block 74 72 128 gt6:natural_gas_block", expect="Test passed"),
]

# ------------------------------------------------- B: the gate faces
steps += [
    phase("B1: the deterministic gate refusal — no bedrock floor, the :66-67 gate fails at any roll"),
    Step(f"fill {BODY} minecraft:deepslate"),
    Step(f"clone {BOX} 288 -64 128"),  # staging1 = the pristine body (reply empty, the t1 note)
    Step("place feature gt6:fluid_springs 72 -64 136", expect="Failed to place feature"),
    Step(f"execute if blocks {BOX} 288 -64 128 all", expect="Test passed"),
]
steps += [
    phase("B2: the worldgen roll face — bedrock floor + deepslate body, the ~3.5%/chunk refusal"),
    Step(f"fill {FLOOR} minecraft:bedrock"),
    Step(f"fill {BODY} minecraft:deepslate"),
    Step(f"clone {BOX} 304 -64 128"),  # staging2 = the pristine floored arena
    # the 1/P refusal IS the command error (vanilla PlaceCommand.placeFeature throws
    # ERROR_FEATURE_FAILED when place() returns false); the exclusion replay may also
    # refuse (~0.5%) — either way the arena must equal its pristine snapshot
    Step("place feature gt6:fluid_springs 72 -64 136", expect="Failed to place feature"),
    Step(f"execute if blocks {BOX} 304 -64 128 all", expect="Test passed"),
]

# ------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — arena + staging + probes back to air (the pass-open bbox is the backstop)"),
    Step(f"fill {BOX} minecraft:air"),
    Step("fill 288 -64 128 303 62 143 minecraft:air"),
    Step("fill 304 -64 128 319 62 143 minecraft:air"),
]

CHAIN = Chain(
    name="p31-fluid-spring",
    slug="p31_spring",
    sites=gt6world.declare_sites(ARENA, STAGING, PROBE),
    preferred_ports=(26545, 26555),      # this card's pinned rcon/query pair (bedrock 26211/26221)
    response_timeout=30.0,               # the wide box fills sync-load chunks on first pass
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
