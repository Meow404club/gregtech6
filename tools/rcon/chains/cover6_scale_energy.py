#!/usr/bin/env python3
"""cover6_scale_energy — the Energy Sensor live chain (p35-covers-display-scale-6).

Band column x=642 z=306. One grid-fed oven + an HU rig adjacent east (the
cover6_display_energy geometry, the SCALE sibling):

  A the IDLE scale: the drained buffer reads value 0 (:44), the value lane stays
    out of the covers NBT, the sensor circuit art rides the snapshot.
  B the FULL scale: the rig books 2 x 64 HU/tick against the unconditional 64
    drain — 64 remains at tickPost, stored >= capacity reads value 15 (:44 —
    tStored >= tCapacity -> 15), the covers NBT VALUE lane (the side-UP key "1")
    carries 15.

Arms: the capacitor-face gate, the idle store, the live full-scale readback,
the teardown. The invert/strong mode bits are the offline suite's pins.
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

A = gt6world.Site(642, 65, 306)
RIG = gt6world.Site(643, 65, 306)

OVEN = (642, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-scale-energy",
    slug="cover6sener",
    sites=gt6world.declare_sites(A, RIG),
    preferred_ports=(25974, 25979),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (640, 60, 304, 645, 70, 308), expect="filled"),
        Step("forceload add 640 304 645 308"),
        Step("fill 640 64 304 645 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the idle scale"),
        Step("gt6cover install %s up gt6:cover_scale_energy" % F(OVEN), expect="ok=true",
             label="the capacitor-face gate (the machine energy lane) admits the sensor"),
        Step("gt6cover check %s" % F(OVEN), expect="block/energy_redstone/circuit",
             label=":52 — the sensor circuit art rides the snapshot"),

        # ------------------------------------------------------------------
        phase("C: the full scale — the value lane reads 15"),
        Step("gt6energy place %s" % F(RIG), expect="GT6 energy source placed"),
        Step("gt6energy type %s HU" % F(RIG), expect="type ENERGY.HEAT"),
        Step("gt6energy volt %s 64" % F(RIG), expect="voltage 64"),
        Step("gt6energy amp %s 2" % F(RIG), expect="amperage 2"),
        Step("gt6energy mode %s on" % F(RIG), expect="emitting true", sleep=2.0),
        Step("gt6cover check %s" % F(OVEN), expect="1:15", poll=20.0,
             label=":44 — stored 64 = capacity at tickPost reads the full-scale 15"),

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
