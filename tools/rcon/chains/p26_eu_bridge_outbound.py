#!/usr/bin/env python3
"""p26-eu-bridge-outbound — the EU->FE outbound bridge live acceptance chain.

The live half of the acceptance the offline tests cannot cover (the capability
query arm cannot class-init offline — the TileEntityBase01Root:457 record):
a GT energy source pushes EU packets at the FE battery fixture and the battery
actually fills, through root EnergyBridge.insertFe at the 4:1 ratio.

Phases:
  A DIRECT-ADJACENCY BRIDGE (gen 0 64 0, battery 1 64 0): the generator's
    live adjacency probe finds the battery BE (not an ITileEntityEnergy), the
    dispatch falls through root ITileEntityEnergy.Util.insertEnergyInto to the
    registered EnergyBridge handler, the handler resolves the battery's FE
    capability and inserts. Judge: stat flips charged false -> true -> false
    (reset), the zero-baseline assertion proving the fill came from the bridge
    and not from registration-time state.
  B WIRE-FED BRIDGE (gen 10 64 10, wire 11 64 10, battery 12 64 10): the same
    bridge behind the GT6 wire relay (the wire's transferElectricity ends in
    the same Util.insertEnergyInto at GTWireBlockEntity:582) — 32 EU minus the
    1 EU wire loss arrives as 124 FE per packet.
  C OVERCHARGE-TRIM PROOF (gen 20 64 20, battery 21 64 20): volt 1000 packets
    (above the upstream VMAX[3] = 512 foreign-receiver explosion threshold,
    EnergyCompat.java:129-137) fill the battery and the battery BE SURVIVES —
    the declared deviation (the GTCEu native-outbound precedent) live.

The pass-2 repeat is the idempotency proof: after the phase-A reset the second
pass re-fills from zero through the same wiring.

Run:  python3 tools/rcon/chains/p26_eu_bridge_outbound.py
      python3 tools/rcon/chains/p26_eu_bridge_outbound.py --node 1.21.1-neoforge
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

GEN_A, BAT_A = gt6world.Site(0, 64, 0), gt6world.Site(1, 64, 0)
GEN_B, WIRE_B, BAT_B = gt6world.Site(10, 64, 10), gt6world.Site(11, 64, 10), gt6world.Site(12, 64, 10)
GEN_C, BAT_C = gt6world.Site(20, 64, 20), gt6world.Site(21, 64, 20)


def _arm_and_fill(gen, battery, sleep_s=4.0):
    """volt 32 x amp 1 armed for ~80 server ticks, then disarmed; the caller
    judges the battery stat afterwards (the fill lands between arm and stat —
    the armed-window stat step doubles as the fixed wait)."""
    return [
        Step(f"gt6energy volt {F(gen)} 32", expect="voltage 32"),
        Step(f"gt6energy amp {F(gen)} 1", expect="amperage 1"),
        Step(f"gt6energy mode {F(gen)} on", expect="emitting true"),
        Step(f"gt6energy stat {F(gen)}", expect="emitting true", sleep=sleep_s),
        Step(f"gt6energy mode {F(gen)} off", expect="emitting false"),
    ]


CHAIN = Chain(
    name="p26-eu-bridge-outbound",
    slug="p26eufo",
    sites=gt6world.declare_sites(
        GEN_A, BAT_A, GEN_B, WIRE_B, BAT_B, GEN_C, BAT_C),
    preferred_ports=(25682, 25692),
    steps=[
        phase("A: direct-adjacency bridge (gen 0 64 0 -> battery 1 64 0, 32 EU x 1 A = 128 FE/t)"),
        Step(f"gt6energy place {F(GEN_A)}", expect="placed at"),
        Step(f"gt6febattery place {F(BAT_A)}", expect="placed at"),
        Step(f"gt6febattery stat {F(BAT_A)}", expect="stored 0 FE"),
        *_arm_and_fill(GEN_A, BAT_A),
        Step(f"gt6febattery stat {F(BAT_A)}", expect="charged true"),
        Step(f"gt6febattery reset {F(BAT_A)}", expect="stored 0 FE"),
        Step(f"gt6febattery stat {F(BAT_A)}", expect="charged false"),

        phase("B: wire-fed bridge (gen 10 64 10 -> wire 11 64 10 -> battery 12 64 10; 31 EU after the 1 EU wire loss = 124 FE)"),
        Step(f"gt6energy place {F(GEN_B)}", expect="placed at"),
        Step(f"gt6wire place 1x {F(WIRE_B)}", expect="placed at"),
        Step(f"gt6febattery place {F(BAT_B)}", expect="placed at"),
        Step(f"gt6febattery stat {F(BAT_B)}", expect="stored 0 FE"),
        *_arm_and_fill(GEN_B, BAT_B),
        Step(f"gt6febattery stat {F(BAT_B)}", expect="charged true"),

        phase("C: overcharge-trim proof (volt 1000 > the upstream VMAX[3]=512 explosion threshold; the battery survives, the declared deviation)"),
        Step(f"gt6energy place {F(GEN_C)}", expect="placed at"),
        Step(f"gt6febattery place {F(BAT_C)}", expect="placed at"),
        Step(f"gt6energy volt {F(GEN_C)} 1000", expect="voltage 1000"),
        Step(f"gt6energy mode {F(GEN_C)} on", expect="emitting true"),
        Step(f"gt6energy stat {F(GEN_C)}", expect="emitting true", sleep=3.0),
        Step(f"gt6energy mode {F(GEN_C)} off", expect="emitting false"),
        Step(f"gt6febattery stat {F(BAT_C)}", expect="charged true"),
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
