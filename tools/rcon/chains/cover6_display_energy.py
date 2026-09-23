#!/usr/bin/env python3
"""cover6_display_energy — the Energy Display Cover live chain (p35-covers-display-scale-6).

Band column x=626 z=306. One grid-fed oven + an HU rig adjacent east (the p13
geometry — the rig's west face feeds the oven):

  A the EMPTY gauge: the grid-fed buffer sits at 0 at the oven's tickPost (the
    doWork :791 drain is unconditional) — the gauge pins at level 0, the
    snapshot art reads energy_display/0.
  B the CHARGED gauge: the rig books 2 x 64 HU/tick (volt 64 == the oven's
    mInputMax 64, amp 2) against the unconditional 64 drain — 64 REMAINS at
    tickPost regardless of the two BEs' tick order, stored >= capacity reads
    gauge 10 (:44 — tStored >= tCapacity -> 10), the art follows
    energy_display/10.

Arms: the capacitor-face gate (the machine-form mapping), both gauge ends, the
store dissolution. The mid-gauge fractions are the offline suite's pins.
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, phase

F = gt6world.fmt

A = gt6world.Site(626, 65, 306)
RIG = gt6world.Site(627, 65, 306)

OVEN = (626, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-display-energy",
    slug="cover6edisp",
    sites=gt6world.declare_sites(A, RIG),
    preferred_ports=(25954, 25959),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (624, 60, 304, 629, 70, 308), expect="filled"),
        Step("forceload add 624 304 629 308"),
        Step("fill 624 64 304 629 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the empty gauge"),
        Step("gt6cover install %s up gt6:cover_energy_display" % F(OVEN), expect="ok=true",
             label="the capacitor-face gate (the machine energy lane) admits the display"),
        Step("gt6cover check %s" % F(OVEN), expect="energy_display/0", poll=10.0,
             label=":44 — stored 0 at tickPost (the unconditional drain) reads gauge 0"),

        # ------------------------------------------------------------------
        phase("C: the rig — the charged gauge"),
        Step("gt6energy place %s" % F(RIG), expect="GT6 energy source placed"),
        Step("gt6energy type %s HU" % F(RIG), expect="type ENERGY.HEAT"),
        Step("gt6energy volt %s 64" % F(RIG), expect="voltage 64"),
        Step("gt6energy amp %s 2" % F(RIG), expect="amperage 2"),
        Step("gt6energy mode %s on" % F(RIG), expect="emitting true", sleep=2.0),
        Step("gt6cover check %s" % F(OVEN), expect="energy_display/10", poll=20.0,
             label=":44 — booked 2x64 vs the 64 drain leaves 64 = capacity, gauge 10"),

        # ------------------------------------------------------------------
        phase("D: teardown"),
        Step("gt6energy mode %s off" % F(RIG), expect="emitting false"),
        Step("gt6cover dismantle %s up" % F(OVEN), expect="OK"),
        Step("gt6cover check %s" % F(OVEN), expect="store=null"),
    ],
)

if __name__ == "__main__":
    chain = CHAIN
    node = None
    if "--node" in sys.argv:
        node = sys.argv[sys.argv.index("--node") + 1]
    import framework
    if framework.session_enabled():
        sys.exit(framework.run_session([chain], node=node))
    chain.node = node or framework.requested_node()
    sys.exit(framework.run(chain))
