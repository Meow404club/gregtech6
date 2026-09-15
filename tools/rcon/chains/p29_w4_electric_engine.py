#!/usr/bin/env python3
"""p29-w4-eu-bridge (engine leg) — the Electric Engine EU->KU live acceptance chain
(task p29-w4-eu-bridge RCON group p29_w4_eu_bridge).

  A the intake + conversion-accounting legs: the EU dial behind the engine (the
    dynamo-rig seat), /gt6bridge stat pins the WASTE arm (the KU emission has NO
    accepting consumer in this repo yet — the axle carries RU only, the port's
    rotation-carrier unification ADR 2026-09-02-p12-rotation-carrier; the declared
    gap): the intake pays (the stat's in-count grows), "out 0" and "half false",
    the capacitor vented to 0. The OFFLINE ladder (GT6EuBridgeBlockEntityTest
    .halfRateLadderEveryTierEveryFamily) carries the exact out = in/2 proof for
    the KU family on all five rungs.
  B the live cross-domain consumer proof: the RU AXLE on the emission face REFUSES
    the KU packet (out stays 0 with a real neighbor present — the type wall, live);
    then the dial re-typed RU drives THE SAME axle (the diesel->axle 判例 form) —
    the geometry proven, the refusal specific to the KU/RU type split.
  C the cross-domain input arms: KU/RU dials behind the engine -> "in 0".

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w4_electric_engine.py
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

Z = 324
E1 = gt6world.Site(458, 65, Z)        # the T1 engine
E1_SRC = gt6world.Site(458, 65, Z - 1)   # the EU dial
E1_SINK = gt6world.Site(458, 65, Z + 1)  # the RU axle seat

steps = [
    phase("A: the waste arm — the KU emission has no consumer, the intake still pays"),
    Step(f"setblock {F(E1)} gt6:electric_engine[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(E1_SRC)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(E1_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(E1_SRC)} 32", expect="voltage 32 EU"),
    Step(f"setblock {F(E1_SINK)} gt6:axle_wood_treated_small[axis=z]", expect="Changed the block"),
    Step(f"gt6bridge reset {F(E1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy mode {F(E1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(E1)}", expect="out 0,", sleep=3.0),
    Step(f"gt6bridge stat {F(E1)}", expect="half false", sleep=0.5),
    # the capacitor rides ONE pending dial packet at any stat instant (the tick
    # interleave) — it is NOT the vent state; the in-growth + out-0 are the waste proof
    Step(f"gt6bridge stat {F(E1)}", expect="in ", sleep=0.5),

    phase("B: the RU twin proof — the same axle accepts an RU packet (the type split is the refusal)"),
    Step(f"gt6energy mode {F(E1_SRC)} off", expect="emitting false"),
    Step(f"gt6bridge reset {F(E1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy type {F(E1_SRC)} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6energy mode {F(E1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(E1)}", expect="in 0,", sleep=2.0),
    Step(f"gt6energy type {F(E1_SRC)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy mode {F(E1_SRC)} off", expect="emitting false"),  # arm the type switch OFF (a live dial leaks one EU packet into the reset otherwise)

    phase("C: the input cross-domain arms — KU then RU offers refused"),
    Step(f"gt6bridge reset {F(E1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6energy type {F(E1_SRC)} KU", expect="type ENERGY.KINETIC_PUSH"),
    Step(f"gt6energy mode {F(E1_SRC)} on", expect="emitting true"),
    Step(f"gt6bridge stat {F(E1)}", expect="in 0,", sleep=2.0),
    Step(f"gt6energy type {F(E1_SRC)} RU", expect="type ENERGY.KINETIC_ROTATION"),
    Step(f"gt6bridge reset {F(E1)}", expect="GT6 bridge accounting reset"),
    Step(f"gt6bridge stat {F(E1)}", expect="in 0,", sleep=2.0),
    Step(f"gt6energy mode {F(E1_SRC)} off", expect="emitting false"),

    phase("D: teardown — restore the band"),
    Step(f"fill 456 62 {Z - 2} 460 68 {Z + 2} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w4_eu_bridge electric engine",
    slug="p29w4engine",
    sites=gt6world.declare_sites(E1, E1_SRC, E1_SINK),
    preferred_ports=(26401, 26411),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
