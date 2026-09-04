#!/usr/bin/env python3
"""p13-boiler-tank — the Steam Boiler Tank acceptance chain (declarative framework).

Chain semantics (task p13-boiler-tank ACCEPTANCE ②):

  A the positive chain, BOTH HU arms + the full e2e loop:
    place (lead boiler, 32 SU/t) → fill water 4000 through the BOTTOM capability door
    → the /gt6boiler inject-hu arm (3×40000 HU direct) → stat asserts the steam tank
    grew past half (barometer=2x, store drained to 0 = the :124 exact accounting)
    → the W2 firebox arm: the brick box BELOW the boiler ignited (16 HU/t = exactly the
    boiler's demand = the design point) → the engine heat bootstrap (one /gt6engine fill,
    the p12 declared channel — the lead engine's own heat ramp is a ~7 min afk) → the
    full loop runs: firebox→boiler→P1 (top push, connections 33=down+east)→P2 (18=west+up,
    ioMask 32 east)→EngineSteam back (pull + the io push)→KU→crusher out + distilled water
    byproduct in the north barrel → extinguish/plunge/mode-off teardown inside the arm.
    The collector (P1, the top neighbour) holding gt6:steam is the push-gate live proof;
    the numeric mOutput/t rate is pinned by the offline truth table (the ~19 tps jitter
    makes exact live rates non-assertable — the p13 W2 λ-lesson, presence + cross-pass).

  B the explosion arms, one ISOLATED SITE each (≥16 blocks spacing — the strongest arm
    detonates at strength ~5.7, a ~7 block crater), each with the server-alive assert:
    B1 overheat: inject-hu past mCapacity → the :148 explode(F) buffered → block gone.
    B2 tank full: water + energy, no collector → the :148 isFull arm → block gone.
    B3 pressurised decalcify: 25×160 HU pulses (conversion ticks calcify, rng(10)) + a
       150000 seed → barometer 24..30 (> 15) → /gt6boiler decalcify → OVERPRESSURE echo
       → block gone. (The seed's own ~15 conversion ticks double the calcification odds:
       P(no hit) ≈ 0.9^40 ≈ 1.5%/pass — declared, the W2 fire-λ precedent.)
    B4 the dry-burn NEGATIVE (the GTCEu SteamBoilerMachine :188-99 semantics NOT ported):
       HU income with NO water → conversions merely stop → the block SURVIVES, the store
       holds exactly the injected HU.

Run:  python3 tools/rcon/chains/p13_boiler_tank.py
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

# The sites.
#   E2E — the full loop at x 104..107, y 63..65, z 59..60 (the boiler at (104,64,60)).
#   B1 — the overheat site at (120,64,60), strength 1 (empty tank), dy 4.
#   B2 — the tank-full site at (136,64,76), strength ~5.7 (FULL tank), the 8-block bbox.
#   B3 — the decalcify site at (152,64,92), strength ~5.6, ditto.
#   B4 — the dry-burn site at (168,64,108), no explosion at all.
E2E = gt6world.Site(105, 64, 60, dx=3, dy=2, dz=2)
B1 = gt6world.Site(120, 64, 60, dx=4, dy=4, dz=4)
B2 = gt6world.Site(136, 64, 76, dx=8, dy=8, dz=8)
B3 = gt6world.Site(152, 64, 92, dx=8, dy=8, dz=8)
B4 = gt6world.Site(168, 64, 108, dx=4, dy=4, dz=4)

FIREBOX = "104 63 60"
BOILER = "104 64 60"
P1 = "104 65 60"
P2 = "105 65 60"
ENGINE = "106 65 60"
CRUSHER = "107 65 60"
DWTANK = "106 65 59"

steps = []

# ---------------------------------------------------------------- A: the e2e loop
steps += [
    phase("A: the positive chain — place, fill, inject-hu + firebox, the full steam loop"),
    Step(f"gt6burner place {FIREBOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6boiler place {BOILER} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler stat {BOILER}", expect="efficiency=10000/10000"),
    # the pipe run: boiler top → P1 → P2 → engine back (pipes never auto-connect —
    # every side is placed or toggled explicitly, the p4 baseline)
    Step(f"gt6pipe place {P1} 1", expect="connections 1"),          # P1 down → the boiler
    Step(f"gt6pipe toggle {P1} 5", expect="connections 33"),        # P1 east → P2
    Step(f"gt6pipe place {P2} 5", expect="connections 16"),         # P2 west → P1
    Step(f"gt6pipe toggle {P2} 1", expect="connections 18"),        # P2 up → the engine pull face
    Step(f"gt6pipe toggle {P2} 5", expect="connections 50"),        # P2 east → the io push mouth
    Step(f"gt6pipe output {P2} 5", expect="ioMask 32 (side 5 marked)"),  # the east arrow, connected
    Step(f"setblock {ENGINE} gt6:steam_engine_tungsten[facing=east]", expect="Changed the block"),
    Step(f"gt6machine crusher place {CRUSHER}", expect="GT6 crusher placed"),
    Step(f"setblock {DWTANK} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6machine crusher input 8 {CRUSHER}", expect="gem_glass into slot 0"),
    # the water arm: 4000 L through the BOTTOM capability door (the :262 intake)
    Step(f"gt6boiler fill {BOILER} 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
    # the direct HU arm: 3×40000 = 1500 conversions exactly → the store drains to 0
    Step(f"gt6boiler inject-hu {BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=0.6),
    Step(f"gt6boiler inject-hu {BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=0.6),
    Step(f"gt6boiler inject-hu {BOILER} 40000", expect="booked 40000/40000 HU (ACCEPTED)", sleep=1.0),
    Step(f"gt6boiler stat {BOILER}", expect="booked=0/320000", sleep=1.0),  # the :124 exact accounting
    Step(f"gt6boiler stat {BOILER}", expect="barometer=2"),                 # the tank grew past half
    # the firebox arm: the real-machine HU source, ignited under the boiler
    Step(f"gt6burner fuel {FIREBOX} minecraft:coal 12", expect="minecraft:coal x12"),
    Step(f"gt6burner ignite {FIREBOX}", expect="burning=true"),
    # the engine heat bootstrap (the p12 declared channel): the :141 gate needs state ≥ 8
    # ≈ 28.9k KU on the tungsten row; EIGHT fills ≈ 55.7k KU → state 14 (packet 60, SAFELY
    # under the crusher's maxIn 64 overcharge cliff at state 16 — the p12 warning), an
    # ~27 s active window covering the gem_glass recipe (16384 progress at ~48/t ≈ 18 s)
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=7.0),
    Step(f"gt6engine stat {ENGINE}", expect="active=true"),
    # the recipe rides the engine's oscillation (the state-8 deactivation gate re-arms on
    # the pipe income), so the FIRST check is the informational probe and the AUTHORITATIVE
    # one comes after the extra window below
    Step(f"gt6machine crusher check {CRUSHER}", expect="out[0]=", sleep=15.0, allow_failed=True),
    # the collector read is phase-racy against the engine's pull cycle (the 21.1 node's
    # faster RCON cadence drained P1 at the single-instant read): first sample lenient,
    # then a 20 s production window on THIS step, then the settled read below.
    Step(f"data get block {P1}", expect="gt6:steam", allow_failed=True, sleep=20.0),
    Step(f"data get block {P1}", expect="gt6:steam"),               # the top-neighbour collector holds steam
    Step(f"gt6tank stat {DWTANK}", expect="L of gt6:distilled_water"),  # the byproduct
    Step(f"gt6boiler stat {BOILER}", expect="barometer=2"),         # the equilibrium held
    Step(f"gt6machine crusher check {CRUSHER}", expect="out[0]=", sleep=60.0),  # the product, authoritative
    # the in-arm teardown (the equilibrium would eventually fill the tank: by design)
    Step(f"gt6burner extinguish {FIREBOX}", expect="burning=false"),
    Step(f"gt6boiler plunge {BOILER}", expect="trashed "),
    Step(f"gt6engine mode {ENGINE} off", expect="stopped=true"),
]

# ---------------------------------------------------------------- B1: the overheat arm
steps += [
    phase("B1: the overheat trigger — HU past mCapacity → the buffered explode(F)"),
    Step(f"gt6boiler place 120 64 60 steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler inject-hu 120 64 60 400000", expect="booked 400000/400000 HU (ACCEPTED)", sleep=3.0),
    Step("execute unless block 120 64 60 gt6:steam_boiler_tank_lead", expect="Test passed"),
    Step("time query daytime", expect="The time is"),  # the server survived the blast
]

# ---------------------------------------------------------------- B2: the tank-full arm
steps += [
    phase("B2: the tank-full trigger — water + energy, no collector → the isFull explode(F)"),
    Step(f"gt6boiler place 136 64 76 steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler fill 136 64 76 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6boiler inject-hu 136 64 76 200000", expect="booked 200000/200000 HU (ACCEPTED)", sleep=3.0),
    Step("execute unless block 136 64 76 gt6:steam_boiler_tank_lead", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- B3: the decalcify arm
steps += [
    phase("B3: the pressurised decalcify — calcify, seed past 15/31, chisel → OVERPRESSURE"),
    Step(f"gt6boiler place 152 64 92 steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler fill 152 64 92 4000", expect="filled 4000/4000 L of minecraft:water (ACCEPTED)"),
]
# 40 conversion ticks (2 conversions each, 1 tick apiece) — the rng(10) scale rolls
# (with the seed's own ~15 conversion ticks: P(no hit) ≈ 0.9^55 ≈ 0.3%/pass)
for _ in range(40):
    steps += [Step(f"gt6boiler inject-hu 152 64 92 160", expect="booked 160/160 HU (ACCEPTED)", sleep=0.2)]
steps += [
    # the seed: ~1875 conversions → the tank lands 24..31/31 (always > 15), never full
    Step(f"gt6boiler inject-hu 152 64 92 150000", expect="booked 150000/150000 HU (ACCEPTED)", sleep=2.0),
    Step(f"gt6boiler decalcify 152 64 92", expect="OVERPRESSURE", sleep=3.0),
    Step("execute unless block 152 64 92 gt6:steam_boiler_tank_lead", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- B4: the dry-burn negative
steps += [
    phase("B4: the dry-burn negative — HU with NO water: conversions stop, NOTHING explodes"),
    Step(f"gt6boiler place 168 64 108 steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler inject-hu 168 64 108 16000", expect="booked 16000/16000 HU (ACCEPTED)", sleep=3.0),
    Step("execute if block 168 64 108 gt6:steam_boiler_tank_lead", expect="Test passed"),
    Step(f"gt6boiler stat 168 64 108", expect="steam=empty"),
    Step(f"gt6boiler stat 168 64 108", expect="booked=16000/320000"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit restore over every arm site (the pass-open bbox is the backstop)"),
    Step("fill 102 62 58 108 66 62 air", expect="filled"),
    Step("fill 116 60 56 124 68 64 air", expect="filled"),
    Step("fill 128 56 68 144 72 84 air", expect="filled"),
    Step("fill 144 56 84 160 72 100 air", expect="filled"),
    Step("fill 164 60 104 172 68 112 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p13-boiler-tank",
    slug="p13bt",
    sites=gt6world.declare_sites(E2E, B1, B2, B3, B4),
    preferred_ports=(25733, 25743),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
