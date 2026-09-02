#!/usr/bin/env python3
"""p12-engine-fuel-fluids — server-boot smoke chain (acceptance c, the minimal face).

The card ruling: LIVE fill/drain assertions are NOT built here — the single-source
commands belong to the p12-fluid-item-carrier chain (/gt6tank fill/show) and the
p12-engine-steam chain, which re-run them against this card's fluids (总 ADR ③,
"命令注汽先行+单源命令不双建"). What THIS chain pins:

  1. the worktree boots clean with the nine engine fluids registered
     (GTFluids.onCommonSetup logs one "GT6 fluid registered: gt6:<id> ..." line per
     fluid — greppable evidence after the run, see the gate);
  2. zero server ERROR lines (the framework gate);
  3. the RCON round-trip answers on this card's pinned port pair (25716/25706).

The offline assertions (declared values, row values, skip semantics, idempotency)
live in the mdk test suite; the JSON face (fluids carry no blockstate/lang regressions)
is the runData written:0 gate.

Run:  python3 tools/rcon/chains/p12_engine_fuel_fluids.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

from framework import Chain, Step, main, phase

CHAIN = Chain(
    name="p12-engine-fuel-fluids",
    slug="p12eff",                       # /tmp/gt6_rs_p12eff.* artifacts
    sites=(),                            # a pure smoke: no world sites, no fills
    preferred_ports=(25716, 25707),      # (rcon, query) — the card's pinned pair
    game_port=25706,                     # the card's server port
    steps=[
        phase("smoke: boot + RCON round-trip (fluid registration evidence = the per-fluid "
              "'GT6 fluid registered: gt6:...' server log lines; live fill/drain = the "
              "carrier/engine-steam chains, not this one)"),
        Step("list", expect="players online"),          # the vanilla RCON round-trip
        Step("time query daytime", expect="The time is"),  # the world is ticking
    ],
)

if __name__ == "__main__":
    main(CHAIN)
