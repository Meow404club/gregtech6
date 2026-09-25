#!/usr/bin/env python3
"""cover6_auto_timer — the Auto Reboot Switch live chain (covers-display-scale-6).

Band column x=610 z=306. One grid-fed oven, the 1-minute ladder item
(cover_auto_timer_1m, the 1200-tick cycle, CoverControllerAutoTimer):

  A the TIMER HOLDS THE MACHINE OFF: mid-cycle (ticks 0..1189 of every 1200)
    the :49 formula {@code active || timer % mTime >= mTime - 10} answers OFF
    for the idle machine — the poll latches stopped=true within a tick of the
    mount (the 10-tick ON pulse is 0.8% duty, the poll lands mid-cycle).
  B the DISMANTLE (:36-39) releases the machine — stopped=false.

Arms: the placement gate (the base controller's switchable gate), the mount,
the two state readbacks, the store dissolution. The window pulse itself and the
active-hold arm are the offline suite's pins (CoverDisplayScaleControllerTest).
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

A = gt6world.Site(610, 65, 306)

OVEN = (610, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-auto-timer",
    slug="cover6timer",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25934, 25939),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (608, 60, 304, 612, 70, 308), expect="filled"),
        Step("forceload add 608 304 612 308"),
        Step("fill 608 64 304 612 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the mount — the timer takes the cycle over"),
        Step("gt6cover install %s up gt6:cover_auto_timer_1m" % F(OVEN), expect="ok=true",
             label="the base controller gate (switchable host) admits the timer"),
        Step("gt6cover check %s" % F(OVEN), expect="gt6:block/auto_timer_switch/circuit",
             label="the shared ladder art rides the snapshot (the declared art fold)"),

        # ------------------------------------------------------------------
        phase("C: the hold — the idle machine is OFF 1190 ticks of every 1200"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=true", poll=15.0,
             label=":48-50 — the mid-cycle poll holds the idle machine OFF"),

        # ------------------------------------------------------------------
        phase("D: the removal release"),
        Step("gt6cover dismantle %s up" % F(OVEN), expect="OK"),
        Step("gt6oven check %s" % F(OVEN), expect="stopped=false", poll=10.0,
             label=":36-39 — the removal releases the machine"),
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
