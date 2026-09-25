#!/usr/bin/env python3
"""barrel-keepfilter-logistics — the keepFilter acceptance chain.

Test fluid is WATER per the main-session warning: the upstream GASPROOF fill gate
is a pool cut in the port, but a non-gas fluid keeps the chain free of any
upstream gas-legality debate — keepFilter semantics are fluid-agnostic.

The logistics tank is the ONLY keepsFilter()=T barrel (upstream
MultiTileEntityBarrelLogistics.java:40): drained to 0 L it keeps its fluid
identity — the show line keeps naming the fluid at "0 L" and the saved BE NBT
carries the real FluidName at Amount 0 (the FluidTankGT serialization fix this
card ships — pre-card the payload degraded to "minecraft:empty"). The control
arm is the metal drum: the same fill/drain leaves "nothing" and the tank key is
gone entirely (the base :91 remove branch — the zero-drift proof).

Scenario (per pass, the card's acceptance ②):
  A place barrel_logistics -> stat asserts the :2171 row (1000000 L @ 100000 K)
    -> fill minecraft:water 5000 -> draw 5000 -> show asserts
    "holds 0/1000000 L of minecraft:water" (identity retained, amount 0)
    -> data get asserts the saved tank NBT {FluidName:"minecraft:water",Amount:0}
    -> refill the SAME fluid -> filled 5000/5000 (the kept filter accepts it)
  B place barrel_metal (control) -> the same fill/drain -> show asserts
    "holds 0/64000 L of nothing" + `execute unless data block tank` passes
    (the key is gone — keepsFilter=F zero drift)
Pass 2 is the idempotency proof (the framework reruns everything on the
cleaned world).

Run:  python3 tools/rcon/chains/barrel-keepfilter-logistics.py
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

LOGI = gt6world.Site(40, 64, 40)
METAL = gt6world.Site(44, 64, 40)
L = F(LOGI)
M = F(METAL)


CHAIN = Chain(
    name="barrel-keepfilter-logistics",
    slug="bkl",
    sites=gt6world.declare_sites(LOGI, METAL),
    preferred_ports=(25718, 25719),      # (rcon, query) — this card's pinned pair; game defaults to rcon-10 = 25708
    steps=[
        phase("A: the logistics tank — drain-to-0 keeps the identity (keepsFilter=T, :40)"),
        Step(f"setblock {L} gt6:barrel_logistics", expect="Changed the block"),
        Step(f"gt6tank stat {L}", expect="melting point 100000 K"),           # the :2171 NBT_CAPACITY_HU row
        Step(f"gt6tank fill {L} minecraft:water 5000", expect="filled 5000/5000"),
        Step(f"gt6tank show {L}", expect="holds 5000/1000000 L of minecraft:water"),  # the 1000000 L capacity row, live
        Step(f"gt6tank draw {L} 5000", expect="drawn 5000/5000"),
        Step(f"gt6tank show {L}", expect="holds 0/1000000 L of minecraft:water"),     # identity retained at amount 0
        Step(f"data get block {L} tank", expect='FluidName: "minecraft:water", Amount: 0'),  # writeToNBT 0 量含身份
        Step(f"gt6tank fill {L} minecraft:water 5000", expect="filled 5000/5000"),    # the kept filter accepts its own fluid
        Step(f"gt6tank show {L}", expect="holds 5000/1000000 L of minecraft:water"),

        phase("B: the metal drum control — the same ops leave a true empty (keepsFilter=F)"),
        Step(f"setblock {M} gt6:barrel_metal", expect="Changed the block"),
        Step(f"gt6tank fill {M} minecraft:water 5000", expect="filled 5000/5000"),
        Step(f"gt6tank show {M}", expect="holds 5000/64000 L of minecraft:water"),
        Step(f"gt6tank draw {M} 5000", expect="drawn 5000/5000"),
        Step(f"gt6tank show {M}", expect="holds 0/64000 L of nothing"),         # identity cleared
        Step(f"execute unless data block {M} tank", expect="Test passed"),      # the :91 remove branch — no key at all
    ],
)


if __name__ == "__main__":
    main(CHAIN)
