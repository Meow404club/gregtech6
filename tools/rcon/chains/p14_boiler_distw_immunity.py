#!/usr/bin/env python3
"""p14-boiler-distw-immunity — the distilled-water immunity live chain (declarative framework).

Chain semantics (task p14-boiler-distw-immunity ACCEPTANCE — the :119 criterion is
`rng(10) == 0 && mEfficiency > 5000 && mTanks[0].has() && !FL.distw(...)`, whose LAST
conjunct short-circuits the whole scaling branch for a distilled tank: the immune arm is
rng-DETERMINISTIC, the water arm is the random control):

  A the distilled-water immune arm: place a lead boiler → 16 rounds of
    [fill 2000 L of gt6:distilled_water → inject 160000 HU → sleep ~71 ticks → plunge ×2].
    One round converts exactly 2000 L (250 conversions/tick = the 640000/2560 lattice
    bound; 2000 × 80 HU = the exact injection) into 320000 L of steam = EXACTLY half tank
    (barometer 15, no push, no isFull — the plunge then clears it; water-first, so the
    second plunge is the timing-jitter backstop). 16 rounds = 128 conversion ticks over
    ~1200 ticks of powered window. The round-1 barometer=15 + efficiency probes prove the
    conversions RAN on distilled water; the final verdict is efficiency=10000/10000
    (PRISTINE) — deterministic, no dice.

  B the plain-water control arm: the identical protocol with minecraft:water through the
    BOTTOM capability door (the :262 canonical intake). Each conversion tick rolls a true
    rng(10); a hit decrements mEfficiency by tConversions=250 (the :120). Over the 128
    conversion ticks P(NO hit) = 0.9^128 ≈ 1.1e-6 — under the architect's 3e-6 note
    (0.9^120) — so the final verdict "(SCALED" is assert-able. The exact efficiency value
    is NOT pinned (the live dice), only the scaled verdict.

  The distw intake rides `gt6boiler fill <pos> <amount> distw` — the distilled half of the
  :262 door as the acceptance channel: upstream the gate is FL.water and DistW carries the
  WATER tag (FL.java:111), but the P13 live door seam (mWaterMatch) is vanilla-water-only
  and /gt6tank fill is barrel-locked (GTBarrelCommand.java:196 instanceof
  TileEntityBase08Barrel) — declared in the command's javadoc (GTBoilerCommand.fillDistw).

Run:  python3 tools/rcon/chains/p14_boiler_distw_immunity.py
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

# The two arm sites — one boiler each, ≥16 blocks apart, clear of the p13 chains' sites.
DISTW = gt6world.Site(200, 64, 40, dx=1, dy=2, dz=1)
WATER = gt6world.Site(216, 64, 56, dx=1, dy=2, dz=1)

DB = F(DISTW)   # the distilled arm's boiler
WB = F(WATER)   # the water control's boiler

ROUNDS = 16          # × 8 conversion ticks = 128 conversion ticks per arm
ROUND_SLEEP = 3.75   # ~71 ticks: the 8 conversion ticks plus idle — the arm spans ~1200 ticks

steps = []

# ------------------------------------------------- A: the distilled-water immune arm
steps += [
    phase("A: the distw immune arm — 16 rounds of fill distw + 160000 HU; efficiency stays PRISTINE"),
    Step(f"gt6boiler place {DB} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
]
for _round in range(ROUNDS):
    steps += [
        Step(f"gt6boiler fill {DB} 2000 distw",
             expect="filled 2000/2000 L of gt6:distilled_water (ACCEPTED)"),
        Step(f"gt6boiler inject-hu {DB} 160000",
             expect="booked 160000/160000 HU (ACCEPTED)", sleep=ROUND_SLEEP),
    ]
    if _round == 0:
        # the round-1 live proof: 2000 conversions happened (320000 L = exactly half tank,
        # barometer 15) and the distilled criterion already held through them
        steps += [
            Step(f"gt6boiler barometer {DB}", expect="barometer=15"),
            Step(f"gt6boiler efficiency {DB}", expect="efficiency=10000/10000 (PRISTINE)"),
        ]
    steps += [
        # water tank is empty (exact conversion accounting) → this clears the 320000 L of
        # steam; the second plunge is the timing-jitter backstop (water-first ordering)
        Step(f"gt6boiler plunge {DB}", expect="trashed "),
        Step(f"gt6boiler plunge {DB}"),
    ]
steps += [
    Step(f"execute if block {DB} gt6:steam_boiler_tank_lead", expect="Test passed"),  # survived, no explosion
    Step(f"gt6boiler efficiency {DB}", expect="efficiency=10000/10000 (PRISTINE)"),   # the immune verdict
    Step("time query daytime", expect="The time is"),                                 # the server survived
]

# ------------------------------------------------- B: the plain-water control arm
steps += [
    phase("B: the water control arm — the identical protocol; efficiency lands SCALED"),
    Step(f"gt6boiler place {WB} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
]
for _round in range(ROUNDS):
    steps += [
        Step(f"gt6boiler fill {WB} 2000",
             expect="filled 2000/2000 L of minecraft:water (ACCEPTED)"),
        Step(f"gt6boiler inject-hu {WB} 160000",
             expect="booked 160000/160000 HU (ACCEPTED)", sleep=ROUND_SLEEP),
    ]
    if _round == 0:
        steps += [
            # conversions ran; the dice may or may not have hit yet — no efficiency probe here
            Step(f"gt6boiler barometer {WB}", expect="barometer=15"),
        ]
    steps += [
        Step(f"gt6boiler plunge {WB}", expect="trashed "),
        Step(f"gt6boiler plunge {WB}"),
    ]
steps += [
    Step(f"execute if block {WB} gt6:steam_boiler_tank_lead", expect="Test passed"),
    # the random control: P(no hit in 128 conversion ticks) = 0.9^128 ≈ 1.1e-6 — the SCALED
    # verdict is the assertion, the exact number rides the transcript unpinned
    Step(f"gt6boiler efficiency {WB}", expect="(SCALED"),
    Step("time query daytime", expect="The time is"),
]

# ------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit restore over both arm sites (the pass-open bbox is the backstop)"),
    Step(f"fill 197 60 37 219 68 59 air", expect="filled"),
    Step(f"fill 213 60 53 235 68 75 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p14-boiler-distw-immunity",
    slug="p14bdi",
    sites=gt6world.declare_sites(DISTW, WATER),
    preferred_ports=(25750, 25760),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
