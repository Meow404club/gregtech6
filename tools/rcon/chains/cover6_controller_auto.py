#!/usr/bin/env python3
"""cover6_controller_auto — the Automatic Machine Switch live chain (covers-display-scale-6).

Band column x=602 z=306 (the covers_g2 band sits x560..560+24 z300..312; the
electric-tool column is z=240). One grid-fed oven + an HU rig adjacent east (the
P13 geometry):

  the auto switch OWNS the oven's ON/OFF latch by the running lane
  (CoverControllerAuto :44 — possible || active; the cover tick poll + the
  controller base arms):

  A the IDLE machine (no input, no energy): possible=false active=false -> the
    poll HOLDS THE MACHINE OFF — stopped=true (the idle hold is the auto
    switch's whole point: "Automatically turns Machines ON/OFF when needed").
  B the DISMANTLE (:36-39) releases the machine to ON — stopped=false.
  C the RE-MOUNT (:46-49) re-derives the answer from the still-idle lanes —
    stopped=true again (the mount arm is the load arm's twin).
  D the INPUT lands (8 cobblestone) WITH THE RIG ON: the doWork gate passes
    (booked 32 >= mInputMin 16), so doActive's :798 probe runs EVERY tick
    (the !mRunning arm), sets mCouldUseRecipe (:739) -> possible=true -> the
    poll RELEASES the machine — stopped=false (aApplyRecipe=!mStopped stays
    false while stopped: the probe consumes nothing, the machine sits released
    but idle). Without energy the probe only fires at the aTimer % 1200 == 5
    cadence (doInactive :895), which is the 60s race this rig arm removes.

Arms: the placement gate admits (the oven is machine-form + switchable), the
install asserts the covers NBT, the four state transitions ride /gt6oven check
stopped= readbacks, the teardown pins the store dissolving to null.
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

A = gt6world.Site(602, 65, 306)
RIG = gt6world.Site(603, 65, 306)

OVEN = (602, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-controller-auto",
    slug="cover6auto",
    sites=gt6world.declare_sites(A, RIG),
    preferred_ports=(25924, 25929),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site — the floor and the oven"),
        Step("fill %d %d %d %d %d %d air" % (600, 60, 304, 605, 70, 308), expect="filled"),
        Step("forceload add 600 304 605 308"),
        Step("fill 600 64 304 605 64 308 " + STONE, expect="filled", label="the support floor"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the idle hold — the poll stops the recipe-less machine"),
        Step("gt6cover install %s up gt6:cover_auto_switch" % F(OVEN), expect="ok=true",
             label="the machine-form + switchable gate admits the auto switch"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=true", poll=10.0,
             label=":44 — the idle lanes (possible=F active=F) hold the machine OFF"),

        # ------------------------------------------------------------------
        phase("C: the removal release + the mount re-derive"),
        Step("gt6cover dismantle %s up" % F(OVEN), expect="OK"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=false", poll=10.0,
             label=":36-39 — the removal releases the machine to ON"),
        Step("gt6cover install %s up gt6:cover_auto_switch" % F(OVEN), expect="ok=true"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=true", poll=10.0,
             label=":46-49 — the mount re-derives OFF from the idle lanes"),

        # ------------------------------------------------------------------
        phase("D: the input + the rig — the possible lane releases the machine"),
        Step("gt6energy place %s" % F(RIG), expect="GT6 energy source placed"),
        Step("gt6energy type %s HU" % F(RIG), expect="type ENERGY.HEAT"),
        Step("gt6energy volt %s 32" % F(RIG), expect="voltage 32"),
        Step("gt6energy amp %s 1" % F(RIG), expect="amperage 1"),
        Step("gt6energy mode %s on" % F(RIG), expect="emitting true", sleep=2.0),
        Step("gt6oven input 8 %s" % F(OVEN), expect="cobblestone"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=false", poll=15.0,
             label=":1023 — the per-tick doActive probe sets mCouldUseRecipe, the poll releases the machine"),

        # ------------------------------------------------------------------
        phase("E: teardown — the store dissolves"),
        Step("gt6energy mode %s off" % F(RIG), expect="emitting false"),
        Step("gt6cover dismantle %s up" % F(OVEN), expect="OK"),
        Step("gt6cover check %s" % F(OVEN), expect="store=null",
             label="the all-empty store dissolves to null (06Covers :313-317)"),
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
