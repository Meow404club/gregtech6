#!/usr/bin/env python3
"""p28-b-fe-inbound — the FE->EU converter live acceptance chain (task p28-b-fe-converter-machine).

The ULV machine (8 EU x 1 A, buffer 512 FE, lossless 4:1) under push and pull, the
throughput ceiling and the retained overload explosion. NOTE on the receipt receiver:
NO ported EU consumer accepts an 8 EU packet (the oven T1 demands 16 minimum and the
EnergyGate.gateInjection :50 small-packet arm SWALLOWS the offer), so the GT-side emit
is made tangible through the p26 outbound bridge receiver — the fe_battery fixture —
which books every 8 EU packet as exactly 32 FE at the same 4:1 (the emit face is a GT
machine face; the bridge dispatch IS what a foreign sink receives).

Phases:
  A PUSH ROUND TRIP (converter 0 64 0, battery 1 64 0): the /gt6feconverter push dial
    plays the foreign cable — it resolves the block's platform FE capability through the
    LEVEL query (the exact face an external cable calls) and pushes 512 FE in. The
    machine's own tick converts and emits one 8 EU packet per tick; the battery books
    16 x 32 = 512 FE — the push/consume/receive round trip closes LOSSLESSLY at 4:1
    (zero-baseline asserted first, exact total asserted after the drain). A second dose
    of 128 FE books exactly "implied EU 32".
  B PULL + FLOOR ALIGNMENT (source 9 64 10, converter 10 64 10, battery 11 64 10):
    the extractable fe_source fixture is set to 130 FE (4 packets + a 2 FE tail); the
    converter PULLS one packet per tick through root EnergyBridge.extractFe (the
    p28-a seam, the #2089 dual-support half GTCEu lacks) and emits the same rate. After
    the drain the source keeps "stored 2 FE" — the sub-packet tail NEVER leaves the
    source (the hostile-remainder semantics live) — and the battery holds exactly
    128 FE.
  C THROUGHPUT CEILING (source 19 64 20, converter 20 64 20, battery 21 64 20): a
    full 100k FE source feeds the machine for a 6 s window (~120 server ticks at the
    20 tps cap). The 1 A pull cap drains 3840 +- band FE, so the source renders
    "stored 96xxx" — the judged band [94000..96999] maps to ticks [94..125] and ANY
    two-packet-per-tick regression (the plausible integer-amps bug, 16 EU/t) drains
    7680+ and lands "stored 92" or lower = RED. The ULV balance ruling
    (decisions.p28-eu-inbound-converter, RF enters the energy chain at its bottom)
    live.
  D OVERLOAD EXPLOSION RETAINED (converter 110 64 110, far from the rigs): data merge
    forces the persisted buffer to 100000 FE — a value both intake faces clamp, i.e.
    only a foreign writer can produce (the upstream "Machine overloaded on Chunkload"
    scenario). Past the 2-tick grace the upstream ladder overcharges
    (TileEntityBase10EnergyConverter :122-126 -> Root :330): the machine dies with the
    suspended explosion. The stat step afterwards carries the GT6 "STAT FAILED" marker
    judged allow_failed -> ALLOWED = the block is GONE (allow_failed is the honest
    absence proof; the failure marker text itself is the evidence).

The pass-2 repeat is the idempotency proof (gt6world bbox cleanup between passes).

Run:  python3 tools/rcon/chains/p28_fe_inbound.py
      python3 tools/rcon/chains/p28_fe_inbound.py --node 1.21.1-neoforge
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

CONV_A, BAT_A = gt6world.Site(0, 64, 0), gt6world.Site(1, 64, 0)
SRC_B, CONV_B, BAT_B = gt6world.Site(9, 64, 10), gt6world.Site(10, 64, 10), gt6world.Site(11, 64, 10)
SRC_C, CONV_C, BAT_C = gt6world.Site(19, 64, 20), gt6world.Site(20, 64, 20), gt6world.Site(21, 64, 20)
CONV_D = gt6world.Site(110, 64, 110)


CHAIN = Chain(
    name="p28-b-fe-inbound",
    slug="p28fein",
    sites=gt6world.declare_sites(
        CONV_A, BAT_A,
        SRC_B, CONV_B, BAT_B,
        SRC_C, CONV_C, BAT_C,
        CONV_D),
    preferred_ports=(26166, 26176),
    passes=2,
    steps=[
        phase("A: push round trip (cable push 512 FE -> 16 x 8 EU packets -> battery books 512 FE at 4:1)"),
        Step(f"gt6feconverter place {F(CONV_A)}", expect="voltage 8 EU x 1 A"),
        Step(f"gt6febattery place {F(BAT_A)}", expect="placed at"),
        Step(f"gt6febattery stat {F(BAT_A)}", expect="stored 0 FE"),
        Step(f"gt6feconverter push {F(CONV_A)} 512", expect="pushed 512 FE, accepted 512 FE", sleep=4.0),
        Step(f"gt6febattery stat {F(BAT_A)}", expect="stored 512 FE"),
        Step(f"gt6feconverter stat {F(CONV_A)}", expect="buffer 0 FE"),
        Step(f"gt6feconverter reset {F(CONV_A)}", expect="buffer 0 FE"),
        Step(f"gt6febattery reset {F(BAT_A)}", expect="stored 0 FE"),
        Step(f"gt6feconverter push {F(CONV_A)} 128", expect="pushed 128 FE, accepted 128 FE", sleep=3.0),
        Step(f"gt6febattery stat {F(BAT_A)}", expect="implied EU 32"),

        phase("B: pull + floor alignment (source 130 FE = 4 packets + 2 FE tail; the tail never leaves the source)"),
        Step(f"gt6fesource place {F(SRC_B)}", expect="stored 100000 FE"),
        Step(f"gt6fesource set {F(SRC_B)} 130", expect="stored 130 FE"),
        Step(f"gt6febattery place {F(BAT_B)}", expect="placed at"),
        Step(f"gt6feconverter place {F(CONV_B)}", expect="voltage 8 EU x 1 A", sleep=4.0),
        Step(f"gt6fesource stat {F(SRC_B)}", expect="stored 2 FE"),
        Step(f"gt6febattery stat {F(BAT_B)}", expect="stored 128 FE"),
        Step(f"gt6feconverter stat {F(CONV_B)}", expect="buffer 0 FE"),

        phase("C: throughput ceiling (full source, 6 s window at the 20 tps cap -> source keeps 96xxx FE; 2 packets/tick would land 92xxx or lower)"),
        Step(f"gt6fesource place {F(SRC_C)}", expect="stored 100000 FE"),
        Step(f"gt6feconverter place {F(CONV_C)}", expect="voltage 8 EU x 1 A"),
        # the battery's own place line prints "stored 0 FE" at placement (the baseline);
        # a separate baseline stat step would flake — the machine fills one packet within
        # a single tick of the placement. The 6 s ceiling window rides this step's sleep.
        Step(f"gt6febattery place {F(BAT_C)}", expect="placed at", sleep=6.0),
        Step(f"gt6fesource stat {F(SRC_C)}", expect="stored 96"),

        phase("D: overload explosion retained (data merge 100000 FE past the intake clamps -> the 2-tick grace -> overcharge -> gone)"),
        Step(f"gt6feconverter place {F(CONV_D)}", expect="voltage 8 EU x 1 A"),
        Step(f"data merge block {F(CONV_D)} {{fe: 100000}}", expect="Modified block data", sleep=2.0),
        Step(f"gt6feconverter stat {F(CONV_D)}", expect="STAT FAILED", allow_failed=True),
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
