#!/usr/bin/env python3
"""hu-steam-foundation — the HU energy-face evidence chain (declarative framework).

Chain semantics (task hu-steam-foundation spec ④ — the existing P8 energy rig only,
this chain pins the evidence; zero production code on the card):

  A the HU retype + dials: /gt6energy type <pos> HU resolves the registered
    TD.Energy.HU shared instance (GTEnergySourceBlockEntity.resolveEnergyType, the
    live arm of the offline HuEnergyHandshakeTest reference-equality pin), and the
    volt/amp dials carry the boiler-side rating figure (80 = the new GTFluids
    EU_PER_WATER) before returning to the oven-acceptable band (32 <= mInputMax 64).

  B the emit pair against the HU oven (task oven-hu-conversion rebased the oven to
    its upstream 20001-04 type, NBT_ENERGY_ACCEPTED = TD.Energy.HU; TileEntityOven.doInject,
    the upstream :501 reference gate): the HU-emitting rig books into the adjacent oven
    (running=true — the doInject mStateNew arm; TileEntityBasicMachine.doWork
    drains mInputMax unconditionally every tick, upstream :791, so the booked energy
    never accumulates and the running flip is the durable booking evidence), then the
    same rig retyped to EU books NOTHING (energy drains back to 0, the machine idles).
    The negative control proves the refusal is the TYPE gate, not a broken emit path.
    The offline mirrors are HuEnergyHandshakeTest.huEmitIsRefusedByTheEuGate (the EU-gate
    refusal) and TileEntityOvenEnergyTest.netMode* (the HU booking pins). The EU
    TagData mName is ENERGY.ELECTRICITY (EU is the alias, TD.java); HU is ENERGY.HEAT.

Run:  python3 tools/rcon/chains/hu_steam_foundation.py
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

# The two sites — the oven consumer and the rig one block to its east (adjacent,
# so the rig's west face (4) feeds the oven's east face (1) through the Util loop).
OVEN = gt6world.Site(10, 64, 10)
RIG = gt6world.Site(11, 64, 10)


CHAIN = Chain(
    name="hu-steam-foundation",
    slug="hu",
    sites=gt6world.declare_sites(OVEN, RIG),
    preferred_ports=(25731, 25741),      # this card's pinned rcon/query pair
    steps=[
        phase("A: the HU retype + the volt/amp dials"),
        Step(f"gt6energy place {F(RIG)}", expect="GT6 energy source placed"),
        Step(f"gt6energy type {F(RIG)} HU", expect="type ENERGY.HEAT"),
        Step(f"gt6energy stat {F(RIG)}", expect="type ENERGY.HEAT"),
        Step(f"gt6energy volt {F(RIG)} 80", expect="voltage 80"),
        Step(f"gt6energy amp {F(RIG)} 1", expect="amperage 1"),
        Step(f"gt6energy volt {F(RIG)} 32", expect="voltage 32"),  # back under the oven mInputMax 64

        phase("B: the emit pair — HU books into the HU oven, the EU retype is the refused type"),
        Step(f"gt6oven place {F(OVEN)}", expect="GT6 oven placed"),
        Step(f"gt6energy mode {F(RIG)} on", expect="emitting true", sleep=2.0),
        Step(f"gt6oven check {F(OVEN)}", expect="running=true"),
        Step(f"gt6energy type {F(RIG)} EU", expect="type ENERGY.ELECTRICITY", sleep=2.0),
        Step(f"gt6oven check {F(OVEN)}", expect="energy=0 minenergy="),
        Step(f"gt6oven check {F(OVEN)}", expect="active=false running=false"),

        phase("C: teardown — the emitter off, the dial persisted"),
        Step(f"gt6energy mode {F(RIG)} off", expect="emitting false"),
        Step(f"gt6energy stat {F(RIG)}", expect="type ENERGY.ELECTRICITY"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
