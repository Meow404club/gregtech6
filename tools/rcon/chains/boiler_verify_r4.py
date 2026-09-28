#!/usr/bin/env python3
"""boiler-verify-r4 — issue #17 live verdict chain (the RCON reproduction card).

A VERDICT card, not a feature: GitHub #17 "steam boiler produces no steam /
does not output" on the Strong Steam Boiler Tank (Titanium) over a brick
burning box. The r4-17 static audit found no seam; the live hypotheses:

  H1 (perceived freeze, not a bug) — the brick box supplies 16 HU/t, the tank
     converts ~32 L/s of steam, and the push gate only opens ABOVE HALF tank.
     On the strong titanium row (mOutput 896 SU/t -> capacity 8,960,000 L)
     half is 4,480,000 L: at ~32 L/t that is ~39 hours away. Numerically
     alive, visibly dead — matches the user screenshot (water 4000 L, tank
     barely seeded).
  H2 (real break) — the HU push line firebox->boiler is severed: booked stays
     exactly 0 AND steam stays flat (break at emit->inject), or booked grows
     while steam stays flat (break at conversion).

Two topologies, the user's own shape (firebox below, boiler, steam pipe on
top), differing only in the boiler row:

  A control — steam_boiler_tank_steel (standard row, cap 640,000 L, half
     320,000 L) + brick box + top pipe: the fast full-chain control.
  B repro   — strong_steam_boiler_tank_titanium + brick box + top pipe.

Per topology: fill 4000 water, ignite, then sample /gt6boiler stat every 15 s
for ~150 s (the verdict time series: booked / water / steam / barometer).
Then the FORCE arm crosses the half gate quickly via inject-hu (the proven p13
acceptance channel) and reads the top pipe — the pipe holding gt6:steam is
the live proof that conversion + the half gate + the top-face push all work
on that row.

Verdict rules (read off the series; the chain hard-asserts only the gates):
  A steam grows, B steam grows              -> H1 (gear mismatch, by design)
  A booked == 0 and A steam flat            -> H2 (emit->inject severed)
  A booked grows, A steam flat              -> H2 (conversion severed)
  A healthy, B flat                          -> strong-row-specific break

Run:  python3 tools/rcon/chains/boiler_verify_r4.py        (1.20.1-forge leg)
      python3 tools/rcon/chains/boiler_verify_r4.py --node 1.21.1
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The two topology sites, 16 blocks apart (z 60 vs z 76), no explosion arms.
SITE_A = gt6world.Site(104, 64, 60, dx=1, dy=2, dz=1)
SITE_B = gt6world.Site(104, 64, 76, dx=1, dy=2, dz=1)

A_FIREBOX = "104 63 60"
A_BOILER = "104 64 60"
A_PIPE = "104 65 60"
B_FIREBOX = "104 63 76"
B_BOILER = "104 64 76"
B_PIPE = "104 65 76"

SAMPLES = 10      # 10 reads x 15 s sleep = a ~150 s series per topology
SAMPLE_SLEEP = 15.0


def series(pos):
    """The sampling ladder: one stat per 15 s, capture-only (no expect — the
    numbers ARE the evidence; the gates are asserted in the force arms)."""
    return [Step(f"gt6boiler stat {pos}", sleep=SAMPLE_SLEEP)
            for _ in range(SAMPLES)]


steps = []

# ---------------------------------------------------------------- A: control
steps += [
    phase("A control: brick box + STEEL boiler tank + top pipe (the fast full-chain control)"),
    Step(f"gt6burner place {A_FIREBOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6boiler place {A_BOILER} steam_boiler_tank_steel", expect="steam_boiler_tank_steel"),
    Step(f"gt6pipe place {A_PIPE} 1", expect="connections 1"),   # face 1 = down -> the boiler
    Step(f"gt6boiler fill {A_BOILER} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6boiler stat {A_BOILER}", expect="water=4000/4000L"),  # the t=0 baseline
    Step(f"gt6burner fuel {A_FIREBOX} minecraft:coal 32", expect="minecraft:coal x32"),
    Step(f"gt6burner ignite {A_FIREBOX}", expect="burning=true"),
]
steps += series(A_BOILER)

# the force arm: refill (sampling drained part of the 4000 L), then 5x40000 HU
# -> 2500 conversions -> +400,000 L steam, past half (320,000) -> the push.
# rng-safe without a decalcify guard: A's burst runs 250 conversions/tick, so
# even ten rng(10) hits floor the efficiency loss at 2500/10000 and the worst
# case still lands ~386k > half
steps += [
    phase("A force arm: inject-hu past half -> the top pipe must hold steam"),
    Step(f"gt6boiler fill {A_BOILER} 4000", expect="(ACCEPTED)"),
    Step(f"gt6boiler inject-hu {A_BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=0.6),
    Step(f"gt6boiler inject-hu {A_BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=0.6),
    Step(f"gt6boiler inject-hu {A_BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=0.6),
    Step(f"gt6boiler inject-hu {A_BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=0.6),
    Step(f"gt6boiler inject-hu {A_BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=2.0),
    Step(f"data get block {A_PIPE}", expect="gt6:steam"),
    Step(f"gt6boiler stat {A_BOILER}", sleep=1.0),
    Step(f"gt6burner extinguish {A_FIREBOX}", expect="burning=false"),
]

# ---------------------------------------------------------------- B: repro
steps += [
    phase("B repro: brick box + STRONG TITANIUM boiler tank + top pipe (the user topology)"),
    Step(f"gt6burner place {B_FIREBOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6boiler place {B_BOILER} strong_steam_boiler_tank_titanium", expect="strong_steam_boiler_tank_titanium"),
    Step(f"gt6pipe place {B_PIPE} 1", expect="connections 1"),
    Step(f"gt6boiler fill {B_BOILER} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6boiler stat {B_BOILER}", expect="water=4000/4000L"),  # t=0; also pins demand/output live
    Step(f"gt6burner fuel {B_FIREBOX} minecraft:coal 32", expect="minecraft:coal x32"),
    Step(f"gt6burner ignite {B_FIREBOX}", expect="burning=true"),
]
steps += series(B_BOILER)

# the force arm: ONE 3,000,000 HU packet FIRST (conversions then drain the
# water tank at cap/2560 = 3500 L/t, so every fill finds headroom — fills
# BEFORE the inject only top up a full tank, the pass-1 lesson), 8x4000 L ->
# ~36k conversions -> ~5.8M L steam, past half (4,480,000) -> the push; the
# cooldown drain (~57k L/t once conversions stop) re-closes the gate within
# ~15 s, so the reads ride right behind the last fill
steps += [
    phase("B force arm: plunge + distw + inject-hu past half -> the pipe must hold steam"),
    # plunge first: the distw fills need an EMPTY water tank (distilled poured
    # onto residual water is a fluid mismatch), and a distw-fed burst is
    # CALCIFICATION-PROOF (the :119 mDistwMatch half skips the rng(10) scale
    # roll entirely — the one nondeterminism that bit runs 2-4) -> efficiency
    # stays ~pristine through all ~36k conversions, deterministically ~5.2M L
    # steam, past half (4,480,000), safely under the 8,960,000 full arm
    Step(f"gt6boiler plunge {B_BOILER}", expect="trashed "),
    Step(f"gt6boiler inject-hu {B_BOILER} 3000000", expect="booked 3000000/3000000 HU (ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"gt6boiler fill {B_BOILER} 4000 distw", expect="(ACCEPTED)", sleep=0.3),
    Step(f"data get block {B_PIPE}", expect="gt6:steam"),
    Step(f"data get block {B_PIPE}", expect="gt6:steam", sleep=1.0),
    Step(f"gt6boiler stat {B_BOILER}"),
    Step(f"gt6burner extinguish {B_FIREBOX}", expect="burning=false"),
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — explicit restore over both sites (the pass-open bbox is the backstop)"),
    Step("fill 103 62 59 105 66 61 air", expect="filled"),
    Step("fill 103 62 75 105 66 77 air", expect="filled"),
    Step("time query daytime", expect="The time is"),   # the server survived
]

CHAIN = Chain(
    name="boiler-verify-r4",
    slug="bv4",
    sites=gt6world.declare_sites(SITE_A, SITE_B),
    steps=steps,
    boot_timeout=1200.0,     # fresh-worktree first boot compiles mdk
)


if __name__ == "__main__":
    main(CHAIN)
