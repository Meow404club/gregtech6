#!/usr/bin/env python3
"""cover6_controller_display — the Machine Status Display live chain (covers-display-scale-6).

Band column x=618 z=306. One grid-fed oven, the status display cover
(CoverControllerDisplay):

  A the VISUAL BUILD (:61-71): the fresh idle oven carries all four capability
    markers (possible B5=32 + passively B6=64 + actively B7=128 + switchable
    B8=256 = 480) plus the ON lamp (B3=8, the machine is not stopped) — the
    covers NBT visual lane reads 488 after the first tickPost.
  B the STYLE CYCLE (:47-53, the chisel arm): the /data modify channel writes
    the style bits (1024s = style 1) through the REAL save/load machinery (the
    P11 filter-seed precedent — the BE reloads through
    TileEntityOven.load/readCoversFromNBT), the tickPost rebuild keeps the style
    bits (only the low 10 bits are rebuilt), and the snapshot art follows the
    top base.

Arms: the placement gate (machine-form OR switchable admits), the lane
readbacks through /gt6cover check, the art follow, the teardown.
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

A = gt6world.Site(618, 65, 306)

OVEN = (618, 65, 306)
STONE = "minecraft:stone"


CHAIN = Chain(
    name="cover6-controller-display",
    slug="cover6disp",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25944, 25949),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site"),
        Step("fill %d %d %d %d %d %d air" % (616, 60, 304, 620, 70, 308), expect="filled"),
        Step("forceload add 616 304 620 308"),
        Step("fill 616 64 304 620 64 308 " + STONE, expect="filled"),
        Step("gt6oven place %s" % F(OVEN), expect="placed"),

        # ------------------------------------------------------------------
        phase("B: the mount + the four-marker lane"),
        Step("gt6cover install %s up gt6:cover_machine_display" % F(OVEN), expect="ok=true",
             label="the machine-form gate admits the display"),
        Step("gt6cover check %s" % F(OVEN), expect="n:488", poll=10.0,
             label=":61-71 — markers 480 (possible|passively|actively|switchable) + the ON lamp 8"),

        # ------------------------------------------------------------------
        phase("C: the style bits — the in-vivo NBT round trip + the art follow"),
        Step("data modify block " + F(OVEN) + " covers.n set value 1024s"),
        Step("gt6cover check %s" % F(OVEN), expect="status_display/top/base", poll=10.0,
             label="style 1 rides the lane (the tickPost rebuild keeps bits 10+, :85 folds mod 2)"),

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
