#!/usr/bin/env python3
"""cover6_controller_auto — the Automatic Machine Switch live chain (p35-covers-display-scale-6).

Band column x=602 z=306 (the p34_covers_g2 band sits x560..584 z300..312; the
electric-tool column is z=240). One grid-fed oven on a stone floor:

  the auto switch OWNS the oven's ON/OFF latch by the running lane
  (CoverControllerAuto :44 — possible || active; the cover tick poll + the
  controller base arms):

  A the IDLE machine (no input, no energy): possible=false active=false -> the
    poll HOLDS THE MACHINE OFF — stopped=true (the idle hold is the auto
    switch's whole point: "Automatically turns Machines ON/OFF when needed").
  B the DISMANTLE (:36-39) releases the machine to ON — stopped=false.
  C the RE-MOUNT (:46-49) re-derives the answer from the still-idle lanes —
    stopped=true again (the mount arm is the load arm's twin).
  D the INPUT lands (8 cobblestone): the onTickFirst recipe probe sets
    mCouldUseRecipe (:739) -> possible=true -> the poll RELEASES the machine —
    stopped=false (the furnace map, the grid-fed regime: released but idle, the
    energy gate stays shut).

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

OVEN = (602, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-controller-auto",
    slug="cover6auto",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25924, 25929),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site — the floor and the oven"),
        Step("fill %d %d %d %d %d %d air" % (600, 60, 304, 604, 70, 308), expect="filled"),
        Step("forceload add 600 304 604 308"),
        Step("fill 600 64 304 604 64 308 " + STONE, expect="filled", label="the support floor"),
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
        phase("D: the input — the possible lane releases the machine"),
        Step("gt6oven input 8 %s" % F(OVEN), expect="cobblestone"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=false", poll=15.0,
             label=":1023 — mCouldUseRecipe makes possible=true, the poll runs the machine"),

        # ------------------------------------------------------------------
        phase("E: teardown — the store dissolves"),
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
