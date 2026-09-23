#!/usr/bin/env python3
"""cover6_scale_progress — the Progress Sensor live chain (p35-covers-display-scale-6).

Band column x=634 z=306. One grid-fed oven + an HU rig adjacent east:

  A the IDLE scale: no input, no energy — progress 0/max 0 reads value 0 (:42),
    the value lane stays out of the covers NBT, the sensor circuit art rides the
    snapshot.
  B the RUNNING scale: input 8 cobblestone + the rig at volt 32 amp 1 (the p13
    fed-regime geometry) -> the machine actively runs, mProgress grows per tick,
    the :42 scale maps it to a nonzero 1..15 reading — the covers NBT VALUE lane
    (the side-UP key "1") carries it, and the machine-side readback shows
    active=true (the :1025 lane the sensors read).

Arms: the progress-face gate, the idle store, the live value lane, the teardown.
The formula rows (0/15/mid + the mMinEnergy normalisation) are the offline
suite's pins.
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

A = gt6world.Site(634, 65, 306)
RIG = gt6world.Site(635, 65, 306)

OVEN = (634, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-scale-progress",
    slug="cover6sprog",
    sites=gt6world.declare_sites(A, RIG),
    preferred_ports=(25964, 25969),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (632, 60, 304, 637, 70, 308), expect="filled"),
        Step("forceload add 632 304 637 308"),
        Step("fill 632 64 304 637 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the idle scale"),
        Step("gt6cover install %s up gt6:cover_scale_progress" % F(OVEN), expect="ok=true",
             label="the progress-face gate (the machine progress lane) admits the sensor"),
        Step("gt6cover check %s" % F(OVEN), expect="block/progress_redstone/circuit",
             label=":50 — the sensor circuit art rides the snapshot"),

        # ------------------------------------------------------------------
        phase("C: the running scale — the value lane goes live"),
        Step("gt6oven input 8 %s" % F(OVEN), expect="cobblestone"),
        Step("gt6energy place %s" % F(RIG), expect="GT6 energy source placed"),
        Step("gt6energy type %s HU" % F(RIG), expect="type ENERGY.HEAT"),
        Step("gt6energy volt %s 32" % F(RIG), expect="voltage 32"),
        Step("gt6energy amp %s 1" % F(RIG), expect="amperage 1"),
        Step("gt6energy mode %s on" % F(RIG), expect="emitting true", sleep=2.0),
        Step("gt6oven check %s" % F(OVEN), expect="active=true", poll=15.0,
             label=":1025 — the actively-running lane the sensor reads"),
        Step("gt6cover check %s" % F(OVEN), expect="1:", poll=20.0,
             label=":42 — the value lane (the side-UP key 1) carries the live progress scale"),

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
