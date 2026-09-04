#!/usr/bin/env python3
"""p13-large-boiler — the Large Boiler multiblock acceptance chain (declarative framework).

Chain semantics (task p13-large-boiler ACCEPTANCE ②):

  A the positive chain — the FULL five-pipe-hole loop:
    place → frame (34 parts) → wand (formed, stock 9+25 consumed) → check
    (linked_parts=34/34) → the FIVE collector pipes around the pipe holes (top-centre
    up + W/E/N/S side holes) → fill 128000 → the W2 firebox arm UNDER the transmitter
    base (the 16 HU/t emit → HeatTransmitter relay → controller booked>0, the live proof
    of the energy-relay chain) → the p14-era cadence: TWO water+HU rounds then a THIRD
    water fill (the vanilla-water scaling branch runs the row at the 5000 efficiency
    floor, ~10.24M steam per round; the SS steam tank half is 40.96M — the half-gate
    needs the tank ABOVE it, and one full water tank alone only reaches 20.48M =
    barometer 7) → stat asserts steam past half → data get on ALL FIVE
    pipes asserting gt6:steam (the live load-balance evidence; the exact split is the
    offline truth table's) → the EngineSteam e2e off the top pipe (8 fills bootstrap,
    active=true) → crusher out (the two-step probe+authoritative form, the p13 W3
    lesson) → in-arm teardown (extinguish/plunge/mode off).

  B the explosion arms, one ISOLATED SITE each (the strongest blast is strength ~6.2,
    a ~7 block crater), each with the server-alive assert:
    B1 the BREAK-ANY-WALL arm (the card's review focus): formed + pressurised
       (barometer 6 via 9M HU) → /gt6multiblock hole on a middle wall → the tick's
       structural probe (:262) detonates → controller gone.
    B2 the dismantle arm (the W3 review note): formed + pressurised →
       /gt6multiblock boiler dismantle → explode(T) instant → controller gone.
    B3 the NEGATIVE: formed + NO pressure (barometer 0) → hole on a wall → the
       structure breaks, the boiler SURVIVES (the strict mBarometer > 4 gate).

Run:  python3 tools/rcon/chains/p13_large_boiler.py
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
#   E2E — the full loop at the boiler (210,64,150) facing north (anchor 210,64,151):
#         transmitters y63, walls y64..66, pipe holes at y65 x/z rims + y66 centre,
#         collectors P1..P5 one cell outside each hole, firebox under the base,
#         engine+crusher east of the top pipe.
#   B1  — the break-any-wall site at (240,64,170), strength ~6.2, the 9-block bbox.
#   B2  — the dismantle site at (262,64,192), ditto.
#   B3  — the low-pressure negative at (284,64,214), no explosion at all.
E2E = gt6world.Site(210, 64, 150, dx=4, dy=4, dz=4)
B1 = gt6world.Site(240, 64, 170, dx=9, dy=9, dz=9)
B2 = gt6world.Site(262, 64, 192, dx=9, dy=9, dz=9)
B3 = gt6world.Site(284, 64, 214, dx=7, dy=7, dz=7)

LB = "210 64 150"
FIREBOX = "210 62 151"
P1 = "210 67 151"  # above the top-centre hole (210,66,151), face DOWN
P2 = "208 65 151"  # west of the west hole (209,65,151), face EAST
P3 = "212 65 151"  # east of the east hole (211,65,151), face WEST
P4 = "210 65 149"  # north of the north hole (210,65,150), face SOUTH
P5 = "210 65 153"  # south of the south hole (210,65,152), face NORTH
ENGINE = "211 67 151"
CRUSHER = "212 67 151"
VARIANT = "large_boiler_stainless_steel"

B1_LB = "240 64 170"
B1_WALL = "239 64 170"      # a middle-ring wall (anchor 240,64,171; the west cell)
B2_LB = "262 64 192"
B3_LB = "284 64 214"
B3_WALL = "283 64 214"

steps = []

# ---------------------------------------------------------------- A: the e2e loop
steps += [
    phase("A: the positive chain — frame, wand, firebox through the transmitter relay, the five-pipe-hole balance, the engine e2e"),
    Step(f"gt6multiblock boiler place {LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock boiler check {LB}", expect="linked_parts=34/34"),
    Step(f"gt6multiblock boiler check {LB}", expect="block_formed=true"),
    # the five collectors, one pipe outside each pipe hole (place <face> links the OPPOSITE
    # support face — face 4 west links east(32), face 2 north links south(8), etc.; the W3
    # place-1-down / toggle-east / output-east form on P1)
    Step(f"gt6pipe place {P1} 1", expect="connections 1"),    # down → the top-centre hole
    Step(f"gt6pipe toggle {P1} 5", expect="connections 33"),  # east → the engine pull face
    Step(f"gt6pipe output {P1} 5", expect="ioMask 32"),       # the east push mouth into the engine
    Step(f"gt6pipe place {P2} 4", expect="connections 32"),   # face west → link east → the west hole
    Step(f"gt6pipe place {P3} 5", expect="connections 16"),   # face east → link west → the east hole
    Step(f"gt6pipe place {P4} 2", expect="connections 8"),    # face north → link south → the north hole
    Step(f"gt6pipe place {P5} 3", expect="connections 4"),    # face south → link north → the south hole
    # the water + the firebox arm (the real HU source THROUGH the transmitter relay)
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6burner place {FIREBOX} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner fuel {FIREBOX} minecraft:coal 12", expect="minecraft:coal x12"),
    Step(f"gt6burner ignite {FIREBOX}", expect="burning=true"),
    # the relay-chain readback: the firebox's 16 HU/t lands on the controller store through
    # the HeatTransmitter relay (the conversion tick may already have eaten some — the exact
    # figure is the offline fixture's; the push evidence below is the live half)
    Step(f"gt6multiblock boiler stat {LB}", expect="Stored Heat Units", sleep=3.0),
    # round 1: 128000 water + 20480000 HU = 128000 conversions × 160 L = 20.48M steam (barometer 7)
    Step(f"gt6multiblock boiler inject-hu {LB} 20480000", expect="booked 20480000/20480000 HU (ACCEPTED)", sleep=2.5),
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    # round 2: past the half tank (40.96M) — the five holes OPEN
    Step(f"gt6multiblock boiler inject-hu {LB} 20480000", expect="booked 20480000/20480000 HU (ACCEPTED)", sleep=2.5),
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    # 2026-09-04 (the p14 immunity era): the vanilla-water scaling branch runs the row at
    # the 5000 efficiency floor — steam yield ≈ 1:1 with the HU, so THREE full rounds
    # park the tank at ~61M (barometer 23) with the water gone and the firebox burning:
    # the dry-fire blast took the boiler on the 21.1 node mid-arm (observed; 1.20.1 only
    # survived the same window by timing). The shape now: THREE fills, TWO injects —
    # 40.96M booked nominal, and the booked HU keeps converting through the sleep
    # windows (~245k L steam per 3 s), carrying the tank past the 40.96M half gate where
    # the five holes open and feed the collectors (the collectors HOLD — no drain face is
    # attached yet); the tank then oscillates around half, far from the blast band.
    Step(f"gt6multiblock boiler fill {LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    # the push runs on CONVERSION ticks: keep water on the tank through the reads (the
    # 21.1 node's instant inject-burn otherwise drains the tank dry and parks the push
    # before the collectors are sampled — observed 2026-09-04)
    Step(f"gt6multiblock boiler stat {LB}", expect="output=8192", sleep=2.0),
    # the live load-balance evidence: every collector holds steam (the exact split is the
    # offline truth table's — the live tick jitter makes ratios non-assertable, the W2 λ-lesson)
    Step(f"data get block {P1}", expect="gt6:steam"),
    Step(f"data get block {P2}", expect="gt6:steam"),
    Step(f"data get block {P3}", expect="gt6:steam"),
    Step(f"data get block {P4}", expect="gt6:steam"),
    Step(f"data get block {P5}", expect="gt6:steam"),
    # the KU e2e: the engine off the top pipe (the W3 bootstrap: eight fills → state 14,
    # packet 60 safely under the crusher maxIn 64 cliff)
    Step(f"setblock {ENGINE} gt6:steam_engine_tungsten[facing=east]", expect="Changed the block"),
    Step(f"gt6machine crusher place {CRUSHER}", expect="GT6 crusher placed"),
    Step(f"gt6machine crusher input 8 {CRUSHER}", expect="gem_glass into slot 0"),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", sleep=0.5),
    # the last landing needs drained head-room first: poll-to-expect (the old sleep=7) —
    # a REJECTED fill changes nothing, so resend until it is ACCEPTED
    Step(f"gt6engine fill {ENGINE} 24000", expect="filled 24000/24000 L of gt6:steam (ACCEPTED)", poll=15.0),
    Step(f"gt6engine stat {ENGINE}", expect="active=true"),
    # the recipe rides the engine's oscillation — probe first, authoritative second (the p13 lesson)
    Step(f"gt6machine crusher check {CRUSHER}", expect="out[0]=", poll=15.0, allow_failed=True),
    Step(f"gt6machine crusher check {CRUSHER}", expect="out[0]=", poll=75.0),  # the product, authoritative
    # the in-arm teardown
    Step(f"gt6burner extinguish {FIREBOX}", expect="burning=false"),
    Step(f"gt6multiblock boiler plunge {LB}", expect="trashed "),
    Step(f"gt6engine mode {ENGINE} off", expect="stopped=true"),
]

# ---------------------------------------------------------------- B1: the break-any-wall arm
steps += [
    phase("B1: the break-any-wall arm — formed + pressurised, hole on a wall → the tick probe detonates"),
    Step(f"gt6multiblock boiler place {B1_LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {B1_LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {B1_LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock boiler fill {B1_LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    # TWO water+HU rounds: the rng(10) scale hits can halve the yield per conversion, and
    # ONE round's 112500 conversions can dip as low as barometer 4 in the worst case — two
    # rounds keep the gauge >= 8 regardless of the rolls (>= 4 strict gate for the :262 arm)
    Step(f"gt6multiblock boiler inject-hu {B1_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)", sleep=3.0),
    # the SECOND fill tops up whatever round one left in the tank (the conversions may not
    # have drained it fully) — the exact litres are a non-assertable function of the rng
    Step(f"gt6multiblock boiler fill {B1_LB} 128000"),
    Step(f"gt6multiblock boiler inject-hu {B1_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)", sleep=3.0),
    Step(f"gt6multiblock boiler stat {B1_LB}"),
    # break ONE middle wall — the part-block playerWillDestroy flags the controller, the
    # NEXT tick's checkStructure(false) fails under pressure → the :262 explode(F)
    Step(f"gt6multiblock hole {B1_WALL}", expect="GT6 part broken", sleep=4.0),
    Step(f"execute unless block {B1_LB} gt6:{VARIANT}", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- B2: the dismantle arm
steps += [
    phase("B2: the dismantle arm (the W3 review note) — pressurised controller dismantle → explode(T) instant"),
    Step(f"gt6multiblock boiler place {B2_LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {B2_LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {B2_LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock boiler fill {B2_LB} 128000", expect="filled 128000/128000 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6multiblock boiler inject-hu {B2_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)", sleep=3.0),
    Step(f"gt6multiblock boiler fill {B2_LB} 128000"),  # the top-up (exact litres ride the rng, see B1)
    Step(f"gt6multiblock boiler inject-hu {B2_LB} 9000000", expect="booked 9000000/9000000 HU (ACCEPTED)", sleep=3.0),
    Step(f"gt6multiblock boiler dismantle {B2_LB}", expect="EXPLODED (instant)", sleep=4.0),
    Step(f"execute unless block {B2_LB} gt6:{VARIANT}", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- B3: the low-pressure negative
steps += [
    phase("B3: the NEGATIVE — formed, NO pressure: hole breaks the structure, the boiler SURVIVES"),
    Step(f"gt6multiblock boiler place {B3_LB} {VARIANT}", expect="GT6 large boiler placed"),
    Step(f"gt6multiblock boiler frame {B3_LB} {VARIANT}", expect="34 parts placed"),
    Step(f"gt6multiblock boiler wand {B3_LB} {VARIANT}", expect="boiler wand check OK"),
    Step(f"gt6multiblock hole {B3_WALL}", expect="GT6 part broken", sleep=2.0),
    Step(f"execute if block {B3_LB} gt6:{VARIANT}", expect="Test passed"),
    Step(f"gt6multiblock boiler stat {B3_LB}", expect="barometer=0"),
    Step("time query daytime", expect="The time is"),
]

# ---------------------------------------------------------------- C: teardown
steps += [
    phase("C: teardown — the explicit restore over every arm site (the pass-open bbox is the backstop)"),
    Step("fill 206 60 146 214 68 154 air", expect="filled"),
    Step("fill 231 55 161 249 73 179 air", expect="filled"),
    Step("fill 253 55 183 271 73 201 air", expect="filled"),
    Step("fill 277 55 207 291 73 221 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p13-large-boiler",
    slug="p13lb",
    sites=gt6world.declare_sites(E2E, B1, B2, B3),
    preferred_ports=(25734, 25744),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
