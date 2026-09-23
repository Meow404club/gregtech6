#!/usr/bin/env python3
"""cover6_scale_energy — the Energy Sensor live chain (p35-covers-display-scale-6).

Band column x=642 z=306. One grid-fed oven + an HU rig adjacent east (the
cover6_display_energy geometry, the SCALE sibling):

  A the LIVE READING: on the grid-fed oven the energy buffer is STRUCTURALLY 0
    at tickPost (doInject caps the booking at mInputMax :497-508, doWork drains
    mInputMax unconditionally :791), so the value lane stays out of the covers
    NBT (a zero lane is not written) and the sensor circuit art rides the
    snapshot: the sensor demonstrably mounts and reads the live lane.
  B THE NONZERO SCALE FACES ARE STRUCTURALLY UNREACHABLE LIVE on the only
    coverable host this port has — the full-scale 15 row, the mid rows and the
    mMinEnergy normalisation are pinned OFFLINE in CoverDisplayScaleScaleTest
    over the real oven-probe lanes. Upstream these covers target the
    battery-box hosts; no coverable capacitor host exists in this port yet
    (the declared CoverMachineLanes mapping deviation) — the W2 energy-tail
    battery-box cards re-arm the live nonzero face.

Arms: the capacitor-face gate, the live zero state, the teardown. The
invert/strong mode bits are the offline suite's pins.
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

OVEN = (642, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-scale-energy",
    slug="cover6sener",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25974, 25979),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (640, 60, 304, 644, 70, 308), expect="filled"),
        Step("forceload add 640 304 644 308"),
        Step("fill 640 64 304 644 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the idle scale"),
        Step("gt6cover install %s up gt6:cover_scale_energy" % F(OVEN), expect="ok=true",
             label="the capacitor-face gate (the machine energy lane) admits the sensor"),
        Step("gt6cover check %s" % F(OVEN), expect="block/energy_redstone/circuit",
             label=":52 — the sensor circuit art rides the snapshot"),

        # ------------------------------------------------------------------
        phase("C: the reading survives the tick — the store holds the mounted sensor"),
        Step("gt6cover check %s" % F(OVEN), expect="store=alive", poll=10.0,
             label="the sensor stays mounted and reads the live (drained) lane"),

        # ------------------------------------------------------------------
        phase("D: teardown"),
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
