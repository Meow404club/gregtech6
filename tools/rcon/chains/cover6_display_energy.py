#!/usr/bin/env python3
"""cover6_display_energy — the Energy Display Cover live chain (covers-display-scale-6).

Band column x=626 z=306. One grid-fed oven + an HU rig adjacent east (the P13
geometry — the rig's west face feeds the oven):

  A the LIVE READING: the cover reads the machine's energy buffer lane every
    tickPost, and on the grid-fed oven that buffer is STRUCTURALLY 0 at
    tickPost — doInject caps the booking at mInputMax (:497-508
    min(mInputMax - mEnergy, ...)) while doWork drains mInputMax
    unconditionally (:791) — so the gauge pins deterministically at level 0
    and the snapshot art reads energy_display/0: the cover demonstrably reads
    the live lane and the art follows it.
  B THE NONZERO GAUGE FACES ARE STRUCTURALLY UNREACHABLE LIVE on the only
    coverable host this port has (the drain caps every steady state at 0 —
    booking <= mInputMax <= drain), so the 1..10 rows (the mid fractions, the
    full reading) are pinned OFFLINE in CoverDisplayScaleScaleTest over the
    real oven-probe lanes. Upstream these covers target the battery-box
    hosts (persistent storage); no coverable capacitor host exists in this
    port yet (the declared CoverMachineLanes mapping deviation) — the W2
    energy-tail battery-box cards are the follow-up that re-arms the live
    nonzero face.

Arms: the capacitor-face gate (the machine-form mapping admits, the plain
coverable refuses is the offline pin), the live zero reading, the store
dissolution.
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

OVEN = (626, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-display-energy",
    slug="cover6edisp",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25954, 25959),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (624, 60, 304, 628, 70, 308), expect="filled"),
        Step("forceload add 624 304 628 308"),
        Step("fill 624 64 304 628 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the empty gauge"),
        Step("gt6cover install %s up gt6:cover_energy_display" % F(OVEN), expect="ok=true",
             label="the capacitor-face gate (the machine energy lane) admits the display"),
        Step("gt6cover check %s" % F(OVEN), expect="energy_display/0", poll=10.0,
             label=":44 — stored 0 at tickPost (the unconditional drain) reads gauge 0"),

        # ------------------------------------------------------------------
        phase("C: the reading survives the tick — the gauge follows the lane live"),
        Step("gt6cover check %s" % F(OVEN), expect="energy_display/0", poll=10.0,
             label="the drained steady state holds — the gauge is a live read, not a mount-time snapshot"),

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
