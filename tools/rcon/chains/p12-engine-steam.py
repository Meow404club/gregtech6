#!/usr/bin/env python3
"""p12-engine-steam — the Steam Engine KU-source acceptance chain (declarative framework).

Chain semantics (task p12-engine-steam acceptance b) — the P12 fluid-engine chain's first
full loop: steam -> KU -> crusher.

  A /gt6energy rig regression: the p8-d4/p11 source rig still runs verbatim next to the
    command root; /gt6engine stat reads the rig through the generic energy-surface dump.

  B the e2e quad — DIRECT intake-face injection (ADR 2026-09-02-p12-steam-proof-deviation,
    the coordinator ruling that supersedes the drum arm): a Tungsten Steam Engine lands
    via /setblock with an explicit facing (gt6:steam_engine_tungsten[facing=north] — the
    state is the facing authority, the BE mirror re-syncs at the tick head), a crusher on
    the FRONT (north) face, a byproduct barrel on the EAST side ring, and the steam goes
    STRAIGHT into the engine's BACK intake face via /gt6engine fill — the capability-door
    push, the exact 1.7.10 pipe-into-getFluidTankFillable2(:239) canonical form. NO tank
    intermediate: upstream NO tank holds steam (the POWER_CONDUCTING destruction chain —
    wood/plastic melt, metal/Logistics fizz, the first chain runs reproduced the melt),
    and the port carries that destruction only as a declared deviation parked for the P13
    boiler card; the injection channel here can never break when P13 lands.

    ENGINE CHOICE (budget, second pass lesson — the OVERCHARGE gate is live): a packet
    ABOVE the consumer's maxIn=64 detonates the crusher (TileEntityBasicMachine.doInject
    :493-494 -> the Root overcharge/explode body — a blown-off machine was observed when
    the store crested state 18 / tOutput 76). The Tungsten row (mOutput 64, eff 5800 ->
    58 KU per 200 L batch, cap 128,000 KU, tank 25,600 L) fed 8 x 24,000 L = 192,000 L =
    960 batches = 55,680 KU store -> state 14 -> tOutput 60, inside the band with the
    state-16 cliff at +2 fills of margin. Emission tail ~26,800 KU at ~60 KU/t covers the
    8-gem job (2 x ~16,384 progress cycles at book-rate 60/t) with zero-crossings every
    ~36 ticks; the byproduct barrel collects 960 L of distilled_water (1 L per batch).

  C the door's stopped-refusal proof: mode off, a fill echoes "filled 0" (the :239
    !mStopped half through the SAME door the steam came in), mode on resumes.

  D /gt6tank water regression (the carrier command face, unchanged by the ruling): a wood
    barrel takes 16,000 L of water through the side-less capability path.

  E the overheat arm (Invar row — the fastest overheat budget: eff 6400 = 64 KU per
    200 L batch against the 16,000 KU clamp = 250 batches = 50,000 L): a dead-ended
    engine, nothing discharges the store, mEnergy climbs past the clamp while mState
    pins past 30 — the :155 stop + :156 tank vent. 15 fills (48,000 L = 15,360 KU gross,
    under the clamp) assert ACCEPTED, 3 un-asserted fills force the crossing, stat shows
    stopped=true + tank=empty, and the post-stop fill echoes "filled 0" — the :239 gate
    holds forever.

Run:  python3 tools/rcon/chains/p12-engine-steam.py
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

# The declared sites — the /gt6energy rig, the engine+crusher+byproduct e2e quad, the
# water-regression barrel, and the overheat pair; the bbox cleanup union covers them all.
RIG = gt6world.Site(0, 64, 0)
ENGINE, CRUSHER, DWTANK = gt6world.Site(4, 64, 8), gt6world.Site(4, 64, 7), gt6world.Site(5, 64, 8)
WATER = gt6world.Site(4, 64, 12)
ENGINE2 = gt6world.Site(8, 64, 8)

FILL = f"gt6engine fill {F(ENGINE)} 24000"
FILL2 = f"gt6engine fill {F(ENGINE2)} 3200"

# The working arm's metered store: 8 x 24,000 L (one drum-batch less than the tank's
# 25,600 L ceiling per landing, converted within the tick between fills). The LAST
# landing waits for drained head-room: poll-to-expect (the old worst-case sleep=20) —
# a REJECTED fill changes nothing, so resend until it is ACCEPTED.
RAMP_FILLS = [Step(FILL, expect="filled 24000/24000 L of gt6:steam (ACCEPTED)",
                   poll=30.0 if i == 7 else 0.5)
              for i in range(8)]
# The overheat arm: 15 accepted fills stay under the clamp, 5 un-asserted ones cross it
# (the state-31 emission bleeds 16 KU/tick, so the last ~240 KU take two extra fills —
# observed 15,759/16,000 at 18 fills).
OVERHEAT_FILLS = ([Step(FILL2, expect="filled 3200/3200 L of gt6:steam (ACCEPTED)", sleep=0.3) for _ in range(15)]
                  + [Step(FILL2, sleep=0.3) for _ in range(5)])


CHAIN = Chain(
    name="p12-engine-steam",
    slug="p12engsteam",
    sites=gt6world.declare_sites(RIG, ENGINE, CRUSHER, DWTANK, WATER, ENGINE2),
    preferred_ports=(25719, 25729),      # this card's pinned rcon/query pair
    game_port=25709,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: /gt6energy rig regression (the p8-d4/p11 command chain, verbatim)"),
        Step(f"gt6energy place {F(RIG)}", expect="GT6 energy source placed at 0, 64, 0"),
        Step(f"gt6energy stat {F(RIG)}", expect="voltage 32 EU"),
        Step(f"gt6engine stat {F(RIG)}", expect="accepts []"),

        phase("B: the e2e quad — tungsten engine(facing) + front crusher + side byproduct barrel + direct intake fills"),
        Step(f"setblock {F(ENGINE)} gt6:steam_engine_tungsten[facing=north]", expect="Changed the block"),
        Step(f"execute if block {F(ENGINE)} gt6:steam_engine_tungsten[facing=north]", expect="Test passed"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="output=64 KU/t"),
        Step(f"gt6machine crusher place {F(CRUSHER)}", expect="GT6 crusher placed at 4, 64, 7"),
        Step(f"gt6machine crusher input 8 {F(CRUSHER)}", expect="gem_glass into slot 0"),
        Step(f"setblock {F(DWTANK)} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6engine mode {F(ENGINE)} on", expect=": on (stopped=false)"),
        *RAMP_FILLS,
        # poll-to-expect: out[0]= IS the completion condition. The R1-full run
        # measured the real production window: the old shape gave 28 s (the fill-8
        # sleep=20 + this check's sleep=8) and the crusher needs ~18 s + spin-up, so
        # the poll bound is 35 s — the old effective window restored with margin.
        Step(f"gt6machine crusher check {F(CRUSHER)}", expect="out[0]=", poll=35.0),
        Step(f"gt6tank stat {F(DWTANK)}", expect="L of gt6:distilled_water", poll=10.0),

        phase("C: the intake door's stopped-refusal proof (mode off -> filled 0 -> mode on)"),
        Step(f"gt6engine mode {F(ENGINE)} off", expect=": off (stopped=true)"),
        Step(f"gt6engine fill {F(ENGINE)} 1000", expect="filled 0/1000 L of gt6:steam (REJECTED)"),
        Step(f"gt6engine mode {F(ENGINE)} on", expect=": on (stopped=false)"),

        phase("D: /gt6tank water regression (the carrier command face, unchanged by the ruling)"),
        Step(f"setblock {F(WATER)} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank fill {F(WATER)} minecraft:water 16000", expect="filled 16000/16000 L of minecraft:water"),

        phase("E: the overheat arm — 18 direct fills into a dead-ended Invar store, then the stop + vent"),
        Step(f"setblock {F(ENGINE2)} gt6:steam_engine_invar[facing=north]", expect="Changed the block"),
        *OVERHEAT_FILLS,
        Step(f"gt6engine stat {F(ENGINE2)}", expect="stopped=true"),
        Step(f"gt6engine stat {F(ENGINE2)}", expect="tank=empty"),
        Step(FILL2, expect="filled 0/3200 L of gt6:steam (REJECTED)"),  # the :239 gate holds
    ],
)


if __name__ == "__main__":
    main(CHAIN)
