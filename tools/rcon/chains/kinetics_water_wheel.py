#!/usr/bin/env python3
"""kinetics-water-wheel — the Water Wheel RU-source acceptance chain (task
chains-water-wheel, pool A6, the P12 kinetics chain form).

The gap (P37-d s4): the wheel is registered (GT6Kinetics.WATER_WHEEL) and its
behaviour is pinned OFFLINE (GT6WaterWheelBlockEntityTest — the four-quadrant
torque table, the 8 RU x 1A packet, the pure-source face truth), but chains/
has zero water-wheel behavioural coverage. This chain closes it LIVE.

Chain semantics — the offline test's chain-closure table walked in-world:

  A the rig: water_wheel[axis=x] at (664,64,520) -> axle_wood_treated_small
    [axis=x] at 665 -> electric_dynamo_ulv[facing=east] at 666 (back=west
    takes the RU, the ulv_chain arm-A form). The wheel's consumer universe
    is the ULV ecosystem ONLY: the 8 RU packet sits dead-centre of the dynamo
    T0 window [1..16] and is BELOW the 16-RU LV machine floor
    (TIER_INPUTS[0][0]) — a shredder is physically unreachable, the dynamo IS
    the chain end (the offline test's design ruling, live here).
    PLACEMENT ORDER IS LOAD-BEARING: dynamo -> axle -> wheel, so the BE ticker
    list (insertion order, same chunk) ticks the dynamo first — the axle's 8 RU
    delivery lands AFTER the dynamo's vent each tick and the between-tick
    `data get` snapshot reads gt.capacitor: 8 at steady state.

  B the dead-wheel control (no water yet): the axle's tachometer reads
    transferred=0 RU/t and the dynamo capacitor is 0 — the wheel produces
    nothing by merely existing (the 无水流 stall, the scan answers 0).

  C the underflow rig + the live spin (vanilla FlowingFluid-exact geometry):
    a fenced source at (664,63,521) on a stone floor at (664,62,521) — stones
    at (663,63,521)/(665,63,521)/(664,63,522) seal the other three sides, so
    the source's ONLY spread (blocked DOWN + isSource -> spreadToSides,
    FlowingFluid :122-129) is NORTH into (664,63,520) — the wheel's DOWN
    neighbour cell, one sheet cell fed level-7 FALLING=FALSE water (the
    calibration finding: falling rides water-above only, getNewLiquid :186 —
    side spreads never set it, which killed the naive over-the-top rig). The
    sheet drains under the wheel (open below -> the DOWN spread of spread
    :122) and the source keeps it at level 7 forever: steady state. The wheel
    scans its six neighbours every 10 ticks (TORQUE_SCAN_PERIOD); the DOWN
    blade's tangent is +Z (offset x axis, the offline quadrant table) and the
    sheet's getFlow points AWAY from the source: (0,0,-1) normalized (the :76
    height gradient, the only fluid neighbour) => torque -1, past the 0.1 dead
    band => sign -1 => the wheel pushes ±8 RU x 1A packets out of its AXIS
    ends, front (east) first. The axle carries |8| RU/t into the dynamo T0
    window; the capacitor books it. Assertions: the axle tachometer reads
    transferred=8 RU/t (the live spin) and the dynamo capacitor reads
    gt.capacitor: 8 (the downstream arrival — the chain-closure link).

  D the bury arm (the live scan teeth): one stone at (664,65,520) — the wheel's
    UP neighbour — is an OPAQUE non-liquid sample: the scan answers 0
    immediately (WaterMill :138 bury, the offline stillWaterDeadBandAndOpaque-
    Bury arm, now in-world) and the tachometer falls back to 0 within its
    per-tick relatch (GTAxleBlockEntity :166-167). Removing the stone re-arms
    the scan (the 10-tick rescan) and the 8 RU/t returns. The sign is asserted
    only through downstream magnitudes — the flow vector's exact angle is
    vanilla's, the port's contract is the dead-band sign.

Run:  python3 tools/rcon/chains/kinetics_water_wheel.py
      python3 tools/rcon/chains/kinetics_water_wheel.py --node 1.21.1-neoforge
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

# The rig — one cell row (x664..666, z520) + the underflow rig south-below the
# wheel (the sheet cell, the fenced source with its floor, the bury cell).
WHEEL = gt6world.Site(664, 64, 520)
AXLE = gt6world.Site(665, 64, 520)
DYNAMO = gt6world.Site(666, 64, 520)
# the DOWN sheet cell + its drain column (y61..63) — cleanup must reach the
# whole stream or pass 2 starts wet
SHEET = gt6world.Site(664, 63, 520, dy=2)
# the fenced source + its floor stone (dy=2 spans the floor at y62)
SOURCE = gt6world.Site(664, 63, 521, dy=2)
FENCE_W = gt6world.Site(663, 63, 521)
FENCE_E = gt6world.Site(665, 63, 521)
FENCE_S = gt6world.Site(664, 63, 522)
BURY = gt6world.Site(664, 65, 520)

AXLE_BLOCK = "gt6:axle_wood_treated_small"
DYNAMO_BLOCK = "gt6:electric_dynamo_ulv[facing=east]"

CAP = f"data get block {F(DYNAMO)}"   # full BE NBT — the gt.capacitor field rides it
TACHO = f"gt6engine stat {F(AXLE)}"


CHAIN = Chain(
    name="kinetics-water-wheel",
    slug="water",
    sites=gt6world.declare_sites(WHEEL, AXLE, DYNAMO, SHEET, SOURCE,
                                 FENCE_W, FENCE_E, FENCE_S, BURY),
    preferred_ports=(25747, 25757),      # this card's pinned rcon/query pair
    game_port=25737,                     # the pinned game port (rcon - 10)
    steps=[
        phase("A: the rig — dynamo -> axle -> wheel (placement order = tick order), "
              "face truth + the dead-wheel control"),
        # the dynamo FIRST: same-chunk BE tickers fire in insertion order, so
        # the dynamo's vent runs BEFORE the axle's delivery each tick and the
        # between-tick capacitor snapshot holds the booked 8
        Step(f"setblock {F(DYNAMO)} {DYNAMO_BLOCK}", expect="Changed the block"),
        Step(f"setblock {F(AXLE)} {AXLE_BLOCK}[axis=x]", expect="Changed the block"),
        Step(f"setblock {F(WHEEL)} gt6:water_wheel[axis=x]", expect="Changed the block"),
        # the face truth (the generic energy-surface dump, GTEngineCommand :257):
        # both X ends emit RU, NOTHING accepts — the pure-source half of the
        # offline faceTruthTable, live
        Step(f"gt6engine stat {F(WHEEL)}", expect="KINETIC_ROTATION@4 ENERGY.KINETIC_ROTATION@5"),
        Step(f"gt6engine stat {F(WHEEL)}", expect="accepts []"),
        # B: the dead-wheel control — no water, no spin, no booking
        Step(TACHO, expect="transferred=0 RU/t"),
        Step(TACHO, expect="axis=x"),
        Step(CAP, expect="gt.capacitor: 0"),

        phase("C: the underflow rig — fenced floor-standing source -> level-7 sheet "
              "under the wheel -> live 8 RU/t spin -> dynamo T0 arrival"),
        # the fence + the floor, THEN the source LAST: with its DOWN blocked and
        # no source neighbours the source's only spread is the wheel's DOWN cell
        Step(f"setblock {F(FENCE_W)} minecraft:stone", expect="Changed the block"),
        Step(f"setblock {F(FENCE_E)} minecraft:stone", expect="Changed the block"),
        Step(f"setblock {F(FENCE_S)} minecraft:stone", expect="Changed the block"),
        Step(f"setblock {F(SOURCE)} minecraft:water", expect="Changed the block"),
        # the spin: the sheet fills within a few fluid ticks, the 10-tick scans
        # find the -Z gradient, the axle pipeline primes — the tachometer pins
        # the wheel's live OUTPUT_PACKET_SIZE=8 through the |packet| it carries
        Step(TACHO, expect="transferred=8 RU/t", poll=20.0),
        Step(TACHO, expect="break pending=false"),
        # the chain-closure link: the wheel's 8 RU x 1A packet books into the
        # dynamo T0 window ([1..16], in8) — the capacitor holds the booked RU
        # between the axle's delivery and the next vent
        Step(CAP, expect="gt.capacitor: 8", poll=15.0),
        # the wheel stayed a pure source the whole drive
        Step(f"gt6engine stat {F(WHEEL)}", expect="accepts []"),

        phase("D: the bury arm — one opaque neighbour stalls the wheel live, "
              "the rescan recovers it"),
        # the UP neighbour is a non-liquid opaque sample: the scan answers 0
        # BEFORE the liquid terms accumulate (the bury gate), the sign drops,
        # the wheel stops pushing
        Step(f"setblock {F(BURY)} minecraft:stone", expect="Changed the block"),
        Step(TACHO, expect="transferred=0 RU/t", poll=15.0),
        Step(CAP, expect="gt.capacitor: 0", poll=15.0),
        # the stone goes, the next scan re-arms the spin (TORQUE_SCAN_PERIOD)
        Step(f"setblock {F(BURY)} minecraft:air", expect="Changed the block"),
        Step(TACHO, expect="transferred=8 RU/t", poll=20.0),
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
