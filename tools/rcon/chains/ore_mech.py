#!/usr/bin/env python3
"""ore_mech — the P30 ore registration card 1 face (task ore-1-mech).

A ZERO-COMMAND chain: the chain's content is the fresh boot itself. The ore universe
(GT6OreBlocks: 26 families x 53 materials x 74 form-rows = 3922 per-pair blocks + items
+ the one upstream-visible creative tab) registers during the boot's RegisterEvent stream,
and the three INFO lines land in this chain's log slice:

    GT6 registered 3922 ore blocks (26 families x 53 materials x 74 form-rows per material, per-pair)
    GT6 registered 3922 ore block items
    GT6 registered 1 ore creative tab (the stone family, upstream SHOW_ORE_BLOCK_PREFIXES=false)

The sweep runner's boot-health gate (wait_done) plus its per-slice server ERROR-line scan
over that slice IS the card acceptance ("runServer 注册日志=块+item+tab 三行零 ERROR").
No RCON arms, no sites, no mutates — the ore WORLD face (placement/prospecting) is card 5;
the offline census pins (M=53 / 3922 / 74 non-empty rows) live in
GT6OreBlocksRegistrationTest.

Run:  python3 tools/rcon/chains/ore_mech.py [--node 1.21.1-neoforge]
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

from framework import Chain, main, phase
import gt6world

CHAIN = Chain(
    name="ore-mech",
    slug="ore_mech",
    # one nominal site (fresh z=400 band, clear of the roster's z<=340 strips): the
    # framework requires a declared site for the boundary forceload/cleanup even though
    # this chain never places a block — the ore WORLD face is card 5
    sites=gt6world.declare_sites(gt6world.Site(384, 64, 400)),
    preferred_ports=(26208, 26218),      # this card's pinned rcon/query pair; game = rcon-10
    fresh_boot=True,                     # the registration log lines belong to a fresh boot's slice
    steps=[
        phase("R: registration face — the boot log slice carries the three GT6 ore lines; "
              "no commands: the fresh boot itself is the probe (see module docstring)"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
