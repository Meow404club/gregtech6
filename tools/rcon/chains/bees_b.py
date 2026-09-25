#!/usr/bin/env python3
"""bees_b — the Bumbliary breeding live chain (task bees-lv3-b-bumbliary,
acceptance ③: princess+drone in → tick advance → drone/princess offspring asserted).

  A THE PLACEMENT: a /setblock gt6:bumbliary (the MTE 32741 port) answers `data get`
    with its te_name (the :387 face) and takes a princess into the ROYAL slot 13
    through the vanilla container face (the item-replace walk over the TE's Container
    implementation — the insert rule accepts only a princess there).

  B THE NO-DRONE WINDOW (the state-machine pin): a princess alone never crowns. The
    deterministic neo window advances 650 ticks (> the 600 retry, MultiTileEntityBumbliary
    .java:198-199) and the frozen-window probe still reads bumble_princess in the ROYAL
    slot; the forge leg degrades to the declared poll against the same probe.

  C THE PAIRING (the 1200 window): a drone in the DRONE slot 22 pairs the princess —
    1300 ticks later the ROYAL slot carries the CROWNED queen (:261 bumbleCrown) and the
    drone died into a DEAD slot (:256-259).

  D THE OFFSPRING (the getOffspring formula): the gt.invout array (the :79-84 NBT face)
    carries both faces — bumble_princess (the :218 princess share) and bumble_drone —
    under the heredity gene tags (:252).

Two framework passes re-lay the whole band (setblock resets the TE, the placement
sequence replays) — the [0, 0] idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/bees_b.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=368 band — z-disjoint from the bees_a band z=360, the gui_distill
# band z=356 and the qu_usb band z=348.
P = "390 65 368"          # the Bumbliary
BAND = "386 63 366 394 67 370"


def dump():
    """The full-BE dump probe (the P32 dotted-key lesson: the gt.invout.".<i>" children
    are read through the whole-dump substring, never a dotted data path)."""
    return "data get block " + P


steps = [
    phase("A: the placement — the TE lives and takes a princess into the ROYAL slot"),
    Step(f"fill {BAND} air", expect="filled"),                       # the band clear (idempotent)
    Step(f"setblock {P} gt6:bumbliary", expect="Changed the block"),
    Step(f"data get block {P} te_name", expect="gt.multitileentity.bumbliary"),
    Step(f"item replace block {P} container.13 with gt6:bumble_princess 1", expect="Replaced"),
    Step(dump(), expect="bumble_princess"),                          # the insert rule accepted the princess

    phase("B: the no-drone window — 650 ticks alone and the princess never crowns"),
    Step(dump(), expect="bumble_princess", tick_step=650, tick_fallback_poll=45),

    phase("C: the pairing — a drone in slot 22 flips the princess to the crowned queen"),
    Step(f"item replace block {P} container.22 with gt6:bumble_drone 1", expect="Replaced"),
    Step(dump(), expect="bumble_queen", tick_step=1300, tick_fallback_poll=150),

    phase("D: the offspring — the dead drone and both brood faces ride the NBT"),
    Step(dump(), expect="bumble_dead"),                              # the :256-259 drone death
    Step(dump(), expect="gt.invout"),                                # the :79-84 offspring array
    Step(dump(), expect="bumble_princess"),                          # the princess share of the brood
    Step(dump(), expect="bumble_drone"),                             # the drone share of the brood

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step(f"fill {BAND} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="bees-lv3-b-bumbliary bees_b",
    slug="beesb",
    sites=gt6world.declare_sites(gt6world.Site(390, 65, 368)),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
