#!/usr/bin/env python3
"""dye-chemical-fluids — the dye-chemical family + chlorine live chain (declarative
framework, the drying_food shape).

Chain semantics (task dye-chemical-fluids ACCEPTANCE — "/gt6tank 注 2304mB
dye_chemical_red 入 barrel_metal + stat 断言 (p6 桶链先例)，双腿 [0,0]"):

  A the acceptance arm (the R4 refill unit): barrel_metal ← dye_chemical_red 2304 mB
    = 16 canisters x 144 mB (CS.java:129 L = 144, the Canner refill row
    MultiItemRandomTools.java:246 FL.mul(., 16)), stat pins the content + the 300 K
    FluidType temperature, draw-back empties the tank (the p6 metal-chain form).

  B the chlorine arm (ruling R3): chlorine is the standalone carrier — fill 1000 L,
    stat pins temperature 239 K (MT.java:405 boiling point) — metal barrel (gas-proof
    true) carries the density −100 gas; draw-back clean. Chlorine is a DEAD END for
    recipes (the offline family-test walk) — this arm is its only live consumer face.

  C the 16-colour census arms: one 144 mB fill+draw round-trip per dye index
    (DYE_IDS order) — every registration resolves through the LIVE tank handler and
    the ACCEPTED line names the exact gt6:dye_chemical_<colour> id, so a half-registered
    family (fluid without block, tint without type) cannot pass silently.

passes=2 is the idempotency proof (the [0,0] of this chain); the pass-open bbox
cleanup re-airs the barrel band between passes.

Run:  GT6_SESSION=off python3 tools/rcon/chains/dye_chemical_fluids.py
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

# the 16 colours in DYE_IDS order (the fluid paths derive: dye_chemical_<colour>)
COLOURS = [
    "black", "red", "green", "brown", "blue", "purple", "cyan",
    "light_gray", "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white",
]

# the barrel band x=386 — clear of every earlier chain's bands (the neighbours are
# P21's 370/378); one metal barrel site (the never-melts carrier, gas-proof true)
BARREL_SITE = gt6world.Site(386, 64, 20, dx=1, dy=1, dz=1)
BARREL = F(BARREL_SITE)  # 386 64 20

steps = []

# ------------------------------------------------- A: the acceptance arm (red, 2304 mB)
steps += [
    phase("A: the R4 unit — metal barrel, dye_chemical_red 2304 mB fill, stat, draw-back"),
    Step(f"setblock {BARREL} gt6:barrel_metal", expect="Changed the block"),
    Step(f"gt6tank fill {BARREL} gt6:dye_chemical_red 2304",
         expect="filled 2304/2304 L of gt6:dye_chemical_red (ACCEPTED)"),
    Step(f"gt6tank stat {BARREL}",
         expect="2304/64000 L of gt6:dye_chemical_red, temperature 300 K"),
    Step(f"gt6tank draw {BARREL} 2304",
         expect="drawn 2304/2304 L of gt6:dye_chemical_red (ACCEPTED)"),
    Step(f"gt6tank stat {BARREL}", expect="0/64000 L of nothing"),
]

# ------------------------------------------------- B: the chlorine arm (R3, 239 K gas)
steps += [
    phase("B: chlorine — the standalone carrier, metal barrel, 239 K pin, draw-back"),
    Step(f"gt6tank fill {BARREL} gt6:chlorine 1000",
         expect="filled 1000/1000 L of gt6:chlorine (ACCEPTED)"),
    Step(f"gt6tank stat {BARREL}",
         expect="1000/64000 L of gt6:chlorine, temperature 239 K"),
    Step(f"gt6tank draw {BARREL} 1000",
         expect="drawn 1000/1000 L of gt6:chlorine (ACCEPTED)"),
]

# ------------------------------------------------- C: the 16-colour census arms
steps += [phase("C: per-colour round-trips — 144 mB fill/draw per dye index (DYE_IDS order)")]
for _colour in COLOURS:
    _fluid = f"gt6:dye_chemical_{_colour}"
    steps += [
        Step(f"gt6tank fill {BARREL} {_fluid} 144",
             expect=f"filled 144/144 L of {_fluid} (ACCEPTED)"),
        Step(f"gt6tank draw {BARREL} 144",
             expect=f"drawn 144/144 L of {_fluid} (ACCEPTED)"),
    ]

# ------------------------------------------------- teardown
steps += [
    phase("D: teardown — the explicit restore over the band (the pass-open bbox is the backstop)"),
    Step(f"fill 385 61 17 387 67 23 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="dye-chemical-fluids",
    slug="dyechemical",
    sites=gt6world.declare_sites(BARREL_SITE),
    preferred_ports=(26106, 26116),      # this card's pinned rcon/query pair (the 2610x segment, after dryingfood 26104/26114)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
