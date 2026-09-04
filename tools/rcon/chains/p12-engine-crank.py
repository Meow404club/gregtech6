#!/usr/bin/env python3
"""p12-engine-crank — the Hand Crank RU-source acceptance chain (declarative framework).

Chain semantics (task p12-engine-crank acceptance b):

  A /gt6energy rig regression: the p8-d4/p11 source rig (place → stat → mode on/off
    → stat) still runs verbatim next to the new command root, and /gt6engine stat
    reads the rig through the generic energy-surface dump (accepts [] — the pure
    source).

  B crank placement + stat: the crank lands via /setblock with an EXPLICIT facing
    (gt6:crank[facing=west] — the BlockState is the command-side facing authority,
    the BE mirror re-syncs at the tick head), a Shredder lands adjacent on the emit
    side via /gt6machine (its own facing is placement-owned; the machine accepts
    RU from every side — the TileEntityBasicMachine :511 all-sides Root default —
    so the "facing aligned" pairing that carries semantics is crank-emit-side →
    shredder-cell, asserted via execute if block on the crank state and the
    stat-facing readback). stat shows the signed RU packet: size=-16
    (counterclockwise DC, the potionless :78 default).

  C the e2e drive: /gt6engine crank 200 arms the server-driven window; real server
    ticks push -16x1 RU per tick into the shredder; the check asserts output
    (out[0]=) and an active machine; stat re-asserts the negative sign.

  D the stop gate (zero-increment proof): a SECOND pair with a full input stack
    (64 cobble — far beyond any window's energy) runs a 300-tick window; the check
    shows it mid-drive (active=true), /gt6engine crank 0 stops it, and two checks
    5 s apart both show energy=0 + active=false. Teeth: had the stop failed, the
    window would still have ~4.5 s left at the first check and the machine would
    still be active (input remains).

Run:  python3 tools/rcon/chains/p12-engine-crank.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# The five declared sites — the /gt6energy rig, the crank+shredder e2e pair, and the
# crank+shredder stop-gate pair; the bbox cleanup union covers all of them per pass.
RIG = gt6world.Site(0, 64, 0)
CRANK, SHREDDER = gt6world.Site(4, 64, 8), gt6world.Site(3, 64, 8)
CRANK2, SHREDDER2 = gt6world.Site(4, 64, 12), gt6world.Site(3, 64, 12)


CHAIN = Chain(
    name="p12-engine-crank",
    slug="p12crank",
    sites=gt6world.declare_sites(RIG, CRANK, SHREDDER, CRANK2, SHREDDER2),
    preferred_ports=(25717, 25727),      # this card's pinned rcon/query pair
    game_port=25707,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: /gt6energy rig regression (the p8-d4/p11 command chain, verbatim)"),
        Step(f"gt6energy place {F(RIG)}", expect="GT6 energy source placed at 0, 64, 0"),
        Step(f"gt6energy stat {F(RIG)}", expect="voltage 32 EU"),
        Step(f"gt6energy mode {F(RIG)} on", expect="emitting true"),
        Step(f"gt6energy stat {F(RIG)}", expect="emitting true"),
        Step(f"gt6engine stat {F(RIG)}", expect="accepts []"),
        Step(f"gt6energy mode {F(RIG)} off", expect="emitting false"),

        phase("B: crank placement (setblock with explicit facing) + the shredder cell + stat"),
        Step(f"setblock {F(CRANK)} gt6:crank[facing=west]", expect="Changed the block"),
        Step(f"execute if block {F(CRANK)} gt6:crank[facing=west]", expect="Test passed"),
        Step(f"gt6machine shredder place {F(SHREDDER)}", expect="GT6 shredder placed at 3, 64, 8"),
        Step(f"gt6machine shredder input 8 {F(SHREDDER)}", expect="cobblestone into slot 0"),
        Step(f"gt6engine stat {F(CRANK)}", expect="facing=west(4) emit-side"),
        Step(f"gt6engine stat {F(CRANK)}", expect="RU packet size=-16"),

        phase("C: the e2e drive — 200 ticks of server-driven cranking into the shredder"),
        # the 8-cobble stack completes inside the window (256 RU per cobble < the 3200 RU
        # window) and parks output-blocked at 63x dust_stone (the 64-slot cap refuses the
        # 8th 9x yield) — the deterministic e2e artifact; the mid-drive active=true proof
        # lives in phase D (the 64-cobble stack outlasts any window)
        Step(f"gt6engine crank {F(CRANK)} 200", expect="armed 200 ticks", sleep=6.0),
        Step(f"gt6machine shredder check {F(SHREDDER)}", expect="out[0]="),
        Step(f"gt6machine shredder check {F(SHREDDER)}", expect="dust_stone"),
        Step(f"gt6engine stat {F(CRANK)}", expect="RU packet size=-16"),

        phase("D: the stop gate — a live window, stopped, zero progress increment"),
        Step(f"setblock {F(CRANK2)} gt6:crank[facing=west]", expect="Changed the block"),
        Step(f"gt6machine shredder place {F(SHREDDER2)}", expect="GT6 shredder placed at 3, 64, 12"),
        Step(f"gt6machine shredder input 64 {F(SHREDDER2)}", expect="cobblestone into slot 0"),
        Step(f"gt6engine crank {F(CRANK2)} 300", expect="armed 300 ticks", sleep=3.0),
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="active=true"),
        Step(f"gt6engine crank {F(CRANK2)} 0", expect="stopped (drive=0 ticks)"),
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="energy=0", sleep=5.0),
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="active=false"),
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="energy=0", sleep=5.0),
        Step(f"gt6machine shredder check {F(SHREDDER2)}", expect="active=false"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
