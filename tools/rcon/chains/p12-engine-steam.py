#!/usr/bin/env python3
"""p12-engine-steam — the Steam Engine KU-source acceptance chain (declarative framework).

Chain semantics (task p12-engine-steam acceptance b) — the P12 fluid-engine chain's first
full loop: steam -> KU -> crusher.

  A /gt6energy rig regression: the p8-d4/p11 source rig still runs verbatim next to the
    command root; /gt6engine stat reads the rig through the generic energy-surface dump.

  B the e2e pair: a Tungsten Steam Engine lands via /setblock with an explicit facing
    (gt6:steam_engine_tungsten[facing=north] — the state is the facing authority, the BE
    mirror re-syncs at the tick head), a steam-filled METAL drum sits on the BACK (south)
    face and a crusher on the FRONT (north) face. /gt6tank fill is the carrier card's
    steam-injection channel (DEPENDENCY SATISFIED: p12-fluid-item-carrier merged c00d3b3).

    ENGINE CHOICE (budget-driven, first pass lesson): the crusher's input band is
    minIn=16 / maxIn=64 (the T1 row the machine command ships), so the Lead engine's
    tOutput = 8*(mState+1)/16 only clears the band floor AT state 31 — razor-thin against
    its own 16,000 KU overheat clamp. The Tungsten row (mOutput 64, eff 5800 -> 58 KU per
    200 L batch, cap 128,000 KU) reaches the :141 gate at state 8 = ~28,900 KU with
    tOutput = 36 KU/t already mid-band. Two metal-drum fills = 128,000 L = 640 batches =
    37,120 KU: above the ramp, far below the 128,000 KU clamp — no overheat on this arm.

    BARREL CHOICE (first pass lesson): the WOOD barrel MELTS at 373 K steam (the 340 K
    wood ceiling, TileEntityBase08Barrel meltdown) — the first chain run's barrel turned
    to fire mid-drive. The metal drum's 1696 K ceiling holds steam safely.

  C the byproduct + consumption proofs: the side barrel (east, on the FACING-perpendicular
    ring) receives distilled_water (1 L per 200 L batch, :125); a fresh fill of the steam
    drum reports (ACCEPTED) — only possible if the engine CONSUMED steam, the
    deterministic drum-drop proof.

  D the overheat arm: a second engine (the Lead row) + metal drum, no crusher — nothing
    discharges the store, so mEnergy climbs to capacity while mState pins past 30: the
    :155 stop + :156 tank vent. 16,000 KU needs ~107,000 L at the Lead row's 30 KU/batch
    = 1.7 drum fills; two fills (128,000 L -> 19,200 KU) force it. After the stop the
    pull gate refuses (the :239 !mStopped half), so the drum keeps its remainder — a
    fresh fill REJECTS, and stat shows stopped=true + tank=empty.

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

# The declared sites — the /gt6energy rig, the engine+crusher+drum+byproduct e2e quad,
# and the overheat pair; the bbox cleanup union covers all of them per pass.
RIG = gt6world.Site(0, 64, 0)
ENGINE, CRUSHER, DRUM, DWTANK = (gt6world.Site(4, 64, 8), gt6world.Site(4, 64, 7),
                                 gt6world.Site(4, 64, 9), gt6world.Site(5, 64, 8))
ENGINE2, DRUM2 = gt6world.Site(8, 64, 8), gt6world.Site(8, 64, 9)

FILL = f"gt6tank fill {F(DRUM)} gt6:steam 64000"
FILL2 = f"gt6tank fill {F(DRUM2)} gt6:steam 64000"


CHAIN = Chain(
    name="p12-engine-steam",
    slug="p12engsteam",
    sites=gt6world.declare_sites(RIG, ENGINE, CRUSHER, DRUM, DWTANK, ENGINE2, DRUM2),
    preferred_ports=(25719, 25729),      # this card's pinned rcon/query pair
    game_port=25709,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: /gt6energy rig regression (the p8-d4/p11 command chain, verbatim)"),
        Step(f"gt6energy place {F(RIG)}", expect="GT6 energy source placed at 0, 64, 0"),
        Step(f"gt6energy stat {F(RIG)}", expect="voltage 32 EU"),
        Step(f"gt6engine stat {F(RIG)}", expect="accepts []"),

        phase("B: the e2e quad — tungsten engine(facing) + back drum + front crusher + mode on"),
        Step(f"setblock {F(ENGINE)} gt6:steam_engine_tungsten[facing=north]", expect="Changed the block"),
        Step(f"execute if block {F(ENGINE)} gt6:steam_engine_tungsten[facing=north]", expect="Test passed"),
        Step(f"setblock {F(DRUM)} gt6:barrel_metal", expect="Changed the block"),
        Step(FILL, expect="filled 64000/64000 L of gt6:steam"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="facing=north(2) emit-side"),
        Step(f"gt6engine stat {F(ENGINE)}", expect="output=64 KU/t"),
        Step(f"gt6machine crusher place {F(CRUSHER)}", expect="GT6 crusher placed at 4, 64, 7"),
        Step(f"gt6machine crusher input 8 {F(CRUSHER)}", expect="8x gem_glass into slot 0"),
        Step(f"gt6engine mode {F(ENGINE)} on", expect=": on (stopped=false)"),

        phase("C: the ramp — a second drum through the back-face pull, then the crusher verdict"),
        Step(FILL, expect="(ACCEPTED)", sleep=2.0),
        Step(FILL, expect="(ACCEPTED)", sleep=2.0),
        Step(FILL, expect="(ACCEPTED)", sleep=16.0),
        Step(f"gt6machine crusher check {F(CRUSHER)}", expect="out[0]=", sleep=8.0),
        Step(f"gt6tank stat {F(DWTANK)}", expect="L of gt6:distilled_water"),
        Step(FILL, expect="(ACCEPTED)"),  # a full drum would REJECT — the drop proof

        phase("D: the overheat arm — two drums into a dead-ended Lead store, then the stop + vent"),
        Step(f"setblock {F(ENGINE2)} gt6:steam_engine_lead[facing=north]", expect="Changed the block"),
        Step(f"setblock {F(DRUM2)} gt6:barrel_metal", expect="Changed the block"),
        Step(FILL2, expect="filled 64000/64000 L of gt6:steam", sleep=2.0),
        Step(FILL2, expect="(ACCEPTED)", sleep=4.0),
        Step(f"gt6engine stat {F(ENGINE2)}", expect="stopped=true"),
        Step(f"gt6engine stat {F(ENGINE2)}", expect="tank=empty"),
        Step(FILL2, expect="(REJECTED)"),  # the stopped engine never drank the remainder
    ],
)


if __name__ == "__main__":
    main(CHAIN)
