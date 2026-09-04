#!/usr/bin/env python3
"""p13-burning-box-family — the burning-box acceptance chain (declarative framework).

Chain semantics (task p13-burning-box-family ACCEPTANCE):

  A the Solid burn cycle (the Brick row, eff 2500 / 16 HU/t): place → fuel (4 coal,
    the containsInput-gated acceptance channel; the stone probe is the observed
    refusal) → ignite → stat burning=true with the buffer charged (coal 1600t × 25 ×
    0.25 = 10000 HU, the RecipeMapFurnaceFuel bridge) → the burn window → stat asserts
    the FUEL DRAIN (one coal per ~625 ticks at 16 HU/t; the window lands safely after
    the first, before the second) → extinguish → burning=false. The no-sink buffer
    semantic this wave: the emit loop spends exactly mRate per tick into the void
    (upstream :103-105, "打空也扣能") — the W3 boilers become the sink.

  B the fire-spread cluster (the 明火蔓延臂, the isolation-site discipline): 24 Brick
    boxes on an oak-plank platform INSIDE the declared site, all fueled and ignited.
    Every emitting tick each box rolls rng(2500)==0 and drops fire into its 7×5×7
    volume (upstream :106-108); expected fires over ~55 s ≈ 24×1100/2500 ≈ 10.5 → the
    aggregate scan asserts spread=VISIBLE inside the site. The OUTSIDE scan (a
    flammable-free ring far from every site's fire volume) asserts fires=0 — fire can
    only land where the WD.fire(T) flammability gate answers, i.e. never outside the
    wood. teardown: explicit fill air over both site regions (the pass-open bbox
    cleanup is the structural backstop, the sites declare the FULL cluster footprint
    incl. the fire volumes).

Run:  python3 tools/rcon/chains/p13_burning_box_family.py
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

# The sites (footprints include the 7×5×7 fire volumes of every box).
#   BOX    — the single-box burn cycle at (20, 64, 20): box ±(4, 4, 4) covers fire x/z 17..23, y 63..67.
#   CLUSTER — 24 Brick boxes in a row X 31..54 at (·, 64, 30) on a plank platform
#             (31..54, 63, 28..32): the site spans x 26..58, y 60..68, z 25..35.
BOX = gt6world.Site(20, 64, 20, dx=4, dy=4, dz=4)
CLUSTER = gt6world.Site(42, 64, 30, dx=16, dy=4, dz=5)

BRICK = "brick_burning_box"
CLUSTER_XS = list(range(31, 55))  # 24 boxes, spaced 1, all volumes overlapping the row centre

steps = []

# ---------------------------------------------------------------- A: the burn cycle
steps += [
    phase("A: the Solid burn cycle — place, fuel, ignite, burn, extinguish"),
    Step(f"gt6burner place {F(BOX)} {BRICK}", expect="GT6 burning box placed"),
    Step(f"gt6burner stat {F(BOX)}", expect="burning box (solid)"),
    # the row values off the block carrier: eff 2500 / rate 16 (the Brick row, Loader :519)
    Step(f"gt6burner stat {F(BOX)}", expect="efficiency=2500/10000"),
    Step(f"gt6burner fuel {F(BOX)} minecraft:coal 6", expect="minecraft:coal x6"),
    # the negative arm: a non-fuel is refused by the containsInput gate (observed, the P11 probe form)
    Step(f"gt6burner fuel {F(BOX)} minecraft:stone 1", expect="FUEL FAILED", allow_failed=True),
    Step(f"gt6burner stat {F(BOX)}", expect="fuel=minecraft:coal x6"),
    Step(f"gt6burner ignite {F(BOX)}", expect="burning=true"),
    Step(f"gt6burner stat {F(BOX)}", expect="burning=true", sleep=2.0),
]
# the buffer charge: 40000 raw × 2500/10000 = 10000, minus ≤40 ticks × 16 → energy 9344..9984
steps += [Step(f"gt6burner stat {F(BOX)}", expect="energy=9")]
# the burn window: coal deaths land at ~0/625/1250/1875 ticks (the measured ~19 tps),
# so exactly THREE are gone once the third death lands (~66 s; the buffer drains at
# mRate 16 into the void — the no-sink W2 boundary). poll-to-expect: resend the
# read-only stat until the fuel drain shows x3; the verdict step below re-asserts it
steps += [Step(f"gt6burner stat {F(BOX)}", expect="fuel=minecraft:coal x3", poll=100.0)]
steps += [
    Step(f"gt6burner stat {F(BOX)}", expect="fuel=minecraft:coal x3"),
    Step(f"gt6burner stat {F(BOX)}", expect="burning=true"),
    Step(f"gt6burner extinguish {F(BOX)}", expect="burning=false"),
    Step(f"gt6burner stat {F(BOX)}", expect="burning=false"),
]

# ---------------------------------------------------------------- B: the fire cluster
steps += [
    phase("B: the fire-spread cluster — 24 fueled boxes, fire inside the site only"),
    Step(f"fill 31 63 28 54 63 32 oak_planks", expect="filled"),
]
for tX in CLUSTER_XS:
    steps += [Step(f"gt6burner place {tX} 64 30 {BRICK}", expect="GT6 burning box placed")]
for tX in CLUSTER_XS:
    steps += [Step(f"gt6burner fuel {tX} 64 30 minecraft:coal 4", expect="minecraft:coal x4")]
for tX in CLUSTER_XS:
    steps += [Step(f"gt6burner ignite {tX} 64 30", expect="burning=true")]
# the burn window: ~1000 ticks (+ the fuel/ignite loop) → ~10 expected fires; the FIRST
# fire lands within seconds (24 boxes × rng(2500) per tick). poll-to-expect: the
# cumulative family telemetry is monotonic — resend until spread=VISIBLE
steps += [Step("gt6burner fires 42 64 30 16", expect="spread=VISIBLE", poll=90.0)]
steps += [
    Step(f"gt6burner stat 42 64 30", expect="burning=true"),
    # the INSIDE aggregate scan: the VISIBLE verdict rides the CUMULATIVE family
    # telemetry (live fire is transient — it dies as it consumes its support), the
    # fires= live count rides along for the record
    Step(f"gt6burner fires 42 64 30 16", expect="spread=VISIBLE"),
    # the OUTSIDE scan: a flammable-free ring far from every site's fire volume (z 42..58
    # vs the fire volumes' z ≤ 35) — fire can never land there
    Step(f"gt6burner fires 42 64 50 8", expect="fires=0"),
    # the W1-pinned top-face gate still holds on a live cluster member
    Step(f"gt6burner stat 31 64 30", expect="emit-gate=top-face-only"),
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit restore (the pass-open bbox cleanup is the backstop)"),
    Step("fill 16 62 16 24 68 24 air", expect="filled"),
    Step("fill 26 60 25 58 68 35 air", expect="filled"),
    # the teardown live-zero: the cumulative total never resets within the server run,
    # so the assert rides the LIVE count only (fire must be gone from the world)
    Step("gt6burner fires 42 64 30 16", expect="fires=0"),
]

CHAIN = Chain(
    name="p13-burning-box-family",
    slug="p13bb",
    sites=gt6world.declare_sites(BOX, CLUSTER),
    preferred_ports=(25732, 25742),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
