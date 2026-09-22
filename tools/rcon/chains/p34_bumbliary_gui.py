#!/usr/bin/env python3
"""p34_bumbliary_gui — the Bumbliary GUI acceptance chain (task p34-bumbliary-gui,
acceptance ④: place → the scoop-open smoke → the penalty countdown arm, in-step
self-reset).

  A THE PRIMARY PAIR: the /setblock gt6:bumbliary answers `data get` with its te_name
    (the :387 face), takes the princess+drone pair through the vanilla container face
    (the ROYAL slot 13 accepts only a princess — the :357-364 rule — so the crowned
    queen is CROWNED LIVE: `data merge {gt.cooldown:1}` forces the :215 pairing on the
    next tick and the :261 bumbleCrown puts the queen into the ROYAL slot, the p33_bees_b
    crowning walk), and the /gt6bumbliary use arm runs the :285-291 walk — the survival
    penalty stomp to 6000 (:289), the sting through the crowned ROYAL (:290 → the
    bumbleAttack family table, stung=true) and the panel construct half (the MUI network
    half is the sanctioned fake-player SKIP, the client-boundary face).

  B THE ADVANCED PAIR: the 20-slot variant answers its own te_name (the Advanced :388),
    takes the princess into the ROYAL slot 7 + the drone into the main slot 12 and the
    same data-merge crowning runs, and the :289 penalty SURVIVES the tick walk —
    the raisedWindow soft reset (:119) only raises, so the /data get proof reads the full
    6000 after any tick (the primary would stomp to 1200 — the upstream quirk). The
    scoop arm (:307/:308/:309) re-runs the penalty with the exemption gate and opens the
    scoop panel; the in-step /data merge reset returns the band to the pre-poke state.

  C THE OPEN ARMS: the /gt6bumbliary open arms build both variants' panels headless
    (the server half of the open chain verbatim — slots=36/20 + advanced=false/true).

Two framework passes re-lay the whole band (setblock resets the TE, the placement
sequence replays) — the [0, 0] idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p34_bumbliary_gui.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=376 band — z-disjoint from the p33_bees_b band z=368, the p33_bees_a
# band z=360, the p33_gui_distill band z=356 and the p32_qu_usb band z=348.
P1 = "390 65 376"           # the primary bumbliary
P2 = "392 65 376"           # the advanced bumbliary
BAND = "388 63 374 394 67 378"


def dump(pos, key=""):
    """The BE probe (the p32 dotted-key lesson: whole-dump or a single scalar path)."""
    return f"data get block {pos} {key}".rstrip()


steps = [
    phase("A: the primary placement — the 36-slot variant, the crowned-queen sting arm"),
    Step(f"fill {BAND} air", expect="filled"),                       # the band clear (idempotent)
    Step(f"setblock {P1} gt6:bumbliary", expect="Changed the block"),
    Step(dump(P1, "te_name"), expect="gt.multitileentity.bumbliary"),
    Step(f"item replace block {P1} container.13 with gt6:bumble_princess 1", expect="Replaced"),
    Step(f"item replace block {P1} container.22 with gt6:bumble_drone 1", expect="Replaced"),
    Step(f"data merge block {P1} {{gt.cooldown:1}}", expect="Modified block data"),  # the :215 pairing fires next tick
    Step(dump(P1), expect="bumble_queen", tick_step=100, tick_fallback_poll=15),  # the :261 live crowning
    Step(f"gt6bumbliary use {P1}", expect="penalty=6000 stung=true panel=bumbliary slots=36"),
    Step(f"gt6bumbliary open {P1}", expect="panel=bumbliary slots=36 advanced=false"),

    phase("B: the advanced placement — the 6000 penalty survives the soft reset"),
    Step(f"setblock {P2} gt6:bumbliary_advanced", expect="Changed the block"),
    Step(dump(P2, "te_name"), expect="gt.multitileentity.bumbliary.advanced"),
    Step(f"item replace block {P2} container.7 with gt6:bumble_princess 1", expect="Replaced"),
    Step(f"item replace block {P2} container.12 with gt6:bumble_drone 1", expect="Replaced"),
    Step(f"data merge block {P2} {{gt.cooldown:1}}", expect="Modified block data"),
    Step(dump(P2), expect="bumble_queen", tick_step=100, tick_fallback_poll=15),  # the same live crowning
    Step(f"gt6bumbliary use {P2}", expect="penalty=6000 stung=true panel=bumbliary slots=20"),
    Step(dump(P2, "gt.cooldown"), expect="6000"),                    # the :119 raisedWindow face — never lowers
    Step(f"gt6bumbliary scoop {P2}", expect="penalty=6000 stung=true panel=bumbliary_scoop"),
    Step(f"data merge block {P2} {{gt.cooldown:0}}", expect="Modified block data"),  # the in-step self-reset
    Step(dump(P2, "gt.cooldown"), expect="0"),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step(f"fill {BAND} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p34-bumbliary-gui p34_bumbliary_gui",
    slug="p34bumbgui",
    sites=gt6world.declare_sites(gt6world.Site(391, 65, 376, dx=3, dy=2, dz=2)),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
