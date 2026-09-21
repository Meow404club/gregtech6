#!/usr/bin/env python3
"""p33_circuits_parts — the circuits-part registration chain (task
p33-circuits-parts RCON: the Ventilation Unit 18299 + the five Quadcore
Processor Units 18200-18204 live-registered face).

The registration face itself landed on main in fc1feb8c2 (the part-family
expansion; the id686 containment is pinned offline by
GT6CircuitPartsRegistrationTest, both legs green). What a dedicated server
node adds on top is the LIVE placement face: every one of the six part ids
parses and places (setblock "Changed the block" — the registry + blockstate
JSON variant parse, the p29_w3_parts form), the DESIGNS-0 rows carry NO design
property (the property argument is unknown), and the FML-booted FML
registration rows exist as block+item on the running server.

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p33_circuits_parts.py
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

# the fresh z=284 band: six part blocks on x-disjoint even columns x384..394 —
# x/z-disjoint from the p29_w3_parts band z=292 (x384..464) with margin.
X0, Z = 384, 284

PARTS = [
    "ventilation_unit",                # 18299 (Loader :1184)
    "processor_unit_versatile",        # 18200 (:1185)
    "processor_unit_logic",            # 18201 (:1186)
    "processor_unit_control",          # 18202 (:1187)
    "processor_unit_storage",          # 18203 (:1188)
    "processor_unit_conversion",       # 18204 (:1189)
]

SITES = {name: gt6world.Site(X0 + i * 2, 65, Z, dy=1) for i, name in enumerate(PARTS)}
P = {name: F(site) for name, site in SITES.items()}
VENT = P["ventilation_unit"]

X_LAST = X0 + (len(PARTS) - 1) * 2

steps = [
    phase("A: ventilation + the five processor units — every id parses and places"),
    *[Step(f"setblock {P[name]} gt6:{name}", expect="Changed the block") for name in PARTS],
    # the DESIGNS-0 rows carry NO design property (a property argument is unknown)
    Step(f"setblock {VENT} gt6:ventilation_unit[design=0]", expect="does not have property"),
    # the placements persisted
    *[Step(f"execute if block {P[name]} gt6:{name}", expect="Test passed") for name in PARTS],

    phase("B: teardown — restore the band"),
    Step(f"fill {X0 - 1} 63 {Z - 1} {X_LAST + 1} 68 {Z + 1} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p33_circuits_parts circuits-parts",
    slug="p33circuitsparts",
    sites=gt6world.declare_sites(*SITES.values()),
    steps=steps,
)
