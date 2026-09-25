#!/usr/bin/env python3
"""ignition-gate — the fusion start-LU ignition gate live acceptance (sweep group
ignition; the fusion rig geometry, shifted +52 x into the fresh x490..514 band):

  the same 17198 controller rig (27 PUs / 50 vents / 48+6 walls / three OCTAGONS rings),
  now with the :1242 NBT_SPECIAL_IS_START_ENERGY gate LIVE (task ignition-gate, the
  S31-7 waiver flipped once the laser domain landed the LU economy):

  A THE ARM (:755): the :955 H+C -> C13 row (ST.tag(2), eUt -8192, dur 315, startLU
    315*131072 = 41,287,680 — the cheapest of the 18 rows) arms the charge_requirement
    ledger in the block NBT the moment the inputs land (data get pins
    "charge_requirement: 41287680L") — and the gate CLOSES: maxprogress 315 but
    progress 0 / active 0b, the ring stays design 5. THE FREEZE LEG (acceptance ②):
    200 deterministic ticks later still progress: 0L (neo /tick step window; the forge
    leg degrades to the declared poll).

  B THE PAY (:497, acceptance ① mid-leg): four T5 CO2 Lasers parked on the four
    orthogonal ring IN cells (each backed by a 16384-EU dial) pump 4 x 8192 LU/tick
    through the glass ring relay into the charged arm — the ledger banks whole packets
    and the TU buffer never stirs. 41,287,680 / 32,768 = 1260 ticks EXACTLY.

  C THE START (acceptance ①): at ~tick 1261 the gate opens — active 1b, the ring
    rewrites design 5 -> 6 (the :241/:105 hook), the generator face launches 8192-EU
    packets at the north +-10 point (the EV overcharge sink explodes on the 101st,
    the fusion oracle), and ~315 ticks later the run completes: active 0b,
    maxprogress 0, the output tank holds gt6:carbon13_molten (the :847 ledger clear).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/ignition.py
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

CTRL = gt6world.Site(502, 65, 462)                 # the controller (facing north; NOT a structure cell)
CORE = (502, 65, 464)                              # the core centre (2 cells behind the front)
SINK_N = gt6world.Site(502, 65, 454)               # the +-10 EU sink (north of the core, the first push side)
BAND = (490, 58, 452, 514, 74, 478)                # the fresh band (x490..514, y58..74, z452..478)
C = gt6world.fmt(CTRL)
S = gt6world.fmt(SINK_N)

WALL = "gt6:machine_wall_galvanized_steel"         # 18008
GLASS = "gt6:machine_wall_tungstensteel"           # 18003 (the design 0/2/5/6 carrier)
SS = "gt6:machine_wall_stainless_steel"            # 18002
COIL = "gt6:large_iridium_coil"                    # 18045
VENT = "gt6:ventilation_unit"                      # 18299
PU_V = "gt6:processor_unit_versatile"              # 18200
PU_L = "gt6:processor_unit_logic"                  # 18201
PU_C = "gt6:processor_unit_control"                # 18202
BIG = "gt6:fusion_reactor"
LASER = "gt6:co2_laser_t5"                         # the T5 CO2 Laser: 16384 EU in -> 8192 LU out/tick
BOX = "gt6:battery_box_ev"

# the four orthogonal ring IN cells (i,j) with their outward laser/dial columns:
#   W (i=0, j=7)  -> ring 493,65,462; laser 492 facing east ; dial 491
#   E (i=18, j=7) -> ring 511,65,462; laser 512 facing west ; dial 513
#   N (i=7, j=0)  -> ring 500,65,455; laser 454 facing south; dial 500,65,453
#   S (i=7, j=18) -> ring 500,65,473; laser 474 facing north; dial 500,65,475
# each laser emits ONE 8192-LU packet per tick (the units(storage, 8192, 4096) core at
# full 16384 storage), so the rig pays 32768 LU/tick: 41,287,680 / 32,768 = 1260 ticks.
RING_W, LASER_W, DIAL_W = "493 65 462", gt6world.Site(492, 65, 462), gt6world.Site(491, 65, 462)
RING_E, LASER_E, DIAL_E = "511 65 462", gt6world.Site(512, 65, 462), gt6world.Site(513, 65, 462)
RING_N, LASER_N, DIAL_N = "500 65 455", gt6world.Site(500, 65, 454), gt6world.Site(500, 65, 453)
RING_S, LASER_S, DIAL_S = "500 65 473", gt6world.Site(500, 65, 474), gt6world.Site(500, 65, 475)
WRONG = "499 65 464"                               # the X-3 arm cell for the rejection arm


# ---------------------------------------------------------------------------
# the structure placement, generated from the transcribed masks (the fusion
# generator re-based on this band's CORE)
# ---------------------------------------------------------------------------

def _octagon_rows():
    OCT = []
    OCT.append([[*map(lambda c: c == "T", row.split(","))] for row in [
        "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F", "F,F,F,T,F,F,F,T,T,T,T,T,F,F,F,T,F,F,F", "F,F,T,F,F,F,T,F,F,F,F,F,T,F,F,F,T,F,F", "F,T,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,T,F", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "F,T,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,T,F", "F,F,T,F,F,F,T,F,F,F,F,F,T,F,F,F,T,F,F", "F,F,F,T,F,F,F,T,T,T,T,T,F,F,F,T,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F", "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F"]])
    OCT.append([[*map(lambda c: c == "T", row.split(","))] for row in [
        "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,T,F,T,T,T,T,T,F,T,F,F,F,F,F", "F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F", "F,F,F,T,F,T,F,F,F,F,F,F,F,T,F,T,F,F,F", "F,F,T,F,T,F,F,F,F,F,F,F,F,F,T,F,T,F,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,F,T,F,T,F,F,F,F,F,F,F,F,F,T,F,T,F,F", "F,F,F,T,F,T,F,F,F,F,F,F,F,T,F,T,F,F,F", "F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F", "F,F,F,F,F,T,F,T,T,T,T,T,F,T,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F"]])
    OCT.append([[*map(lambda c: c == "T", row.split(","))] for row in [
        "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F", "F,F,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F", "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F"]])
    return OCT


def _runs(oct_row):
    """The consecutive true-index runs of one mask row (the fill-z segmentation)."""
    rRuns, tStart = [], None
    for tI, tCell in enumerate(oct_row):
        if tCell and tStart is None:
            tStart = tI
        if not tCell and tStart is not None:
            rRuns.append((tStart, tI - 1))
            tStart = None
    if tStart is not None:
        rRuns.append((tStart, len(oct_row) - 1))
    return rRuns


def structure_steps():
    """The generated placement steps: core, arms, then the three rings as z-run fills."""
    tSteps = []
    tCX, tCY, tCZ = CORE

    tPU = 0
    for tI in range(-2, 3):
        for tJ in range(-2, 3):
            for tK in range(-2, 3):
                tD2 = tI * tI + tJ * tJ + tK * tK
                if tD2 < 4:
                    tBlock = PU_V if tPU < 3 else (PU_L if tPU < 15 else PU_C)
                    tPU += 1
                elif tD2 > 6 or (tJ == 0 and (((tI in (-2, 2)) and tK == 0) or ((tK in (-2, 2)) and tI == 0))):
                    tBlock = WALL
                else:
                    tBlock = VENT
                tSteps.append(Step("setblock %d %d %d %s" % (tCX + tI, tCY + tJ, tCZ + tK, tBlock), expect="Changed the block"))
    for tD in (3, 4):
        tSteps.append(Step("setblock %d %d %d %s" % (tCX - tD, tCY, tCZ, WALL), expect="Changed the block"))
        tSteps.append(Step("setblock %d %d %d %s" % (tCX + tD, tCY, tCZ, WALL), expect="Changed the block"))
        tSteps.append(Step("setblock %d %d %d %s" % (tCX, tCY, tCZ + tD, WALL), expect="Changed the block"))

    tRX, tRZ = tCX - 9, tCZ - 9
    OCT = _octagon_rows()
    for tI in range(19):
        for tJStart, tJEnd in _runs(OCT[0][tI]):
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY - 1, tRZ + tJStart, tRX + tI, tCY - 1, tRZ + tJEnd, GLASS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY + 1, tRZ + tJStart, tRX + tI, tCY + 1, tRZ + tJEnd, GLASS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY, tRZ + tJStart, tRX + tI, tCY, tRZ + tJEnd, GLASS), expect="filled"))
        for tJStart, tJEnd in _runs(OCT[1][tI]):
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY - 2, tRZ + tJStart, tRX + tI, tCY - 2, tRZ + tJEnd, GLASS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY - 1, tRZ + tJStart, tRX + tI, tCY - 1, tRZ + tJEnd, GLASS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY, tRZ + tJStart, tRX + tI, tCY, tRZ + tJEnd, COIL), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY + 1, tRZ + tJStart, tRX + tI, tCY + 1, tRZ + tJEnd, GLASS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY + 2, tRZ + tJStart, tRX + tI, tCY + 2, tRZ + tJEnd, GLASS), expect="filled"))
        for tJStart, tJEnd in _runs(OCT[2][tI]):
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY - 2, tRZ + tJStart, tRX + tI, tCY - 2, tRZ + tJEnd, GLASS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY - 1, tRZ + tJStart, tRX + tI, tCY - 1, tRZ + tJEnd, COIL), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY, tRZ + tJStart, tRX + tI, tCY, tRZ + tJEnd, SS), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY + 1, tRZ + tJStart, tRX + tI, tCY + 1, tRZ + tJEnd, COIL), expect="filled"))
            tSteps.append(Step("fill %d %d %d %d %d %d %s" % (tRX + tI, tCY + 2, tRZ + tJStart, tRX + tI, tCY + 2, tRZ + tJEnd, GLASS), expect="filled"))
    return tSteps


def selector_merge(slot, config):
    """The selector circuit stack (the fusion fork)."""
    return {
        "1.20.1": '{Slot:%db,id:"gt6:integrated_circuit",Count:1b,tag:{Damage:%d}}' % (slot, config),
        "1.21.1": '{Slot:%db,id:"gt6:integrated_circuit",count:1,components:{"minecraft:custom_data":{Damage:%d}}}' % (slot, config),
    }


LASER_COLS = ((LASER_W, DIAL_W, "east"), (LASER_E, DIAL_E, "west"),
              (LASER_N, DIAL_N, "south"), (LASER_S, DIAL_S, "north"))

laser_place_steps = [
    Step("setblock %s %s[facing=%s]" % (F(tLaser), LASER, tFacing), expect="Changed the block")
    for tLaser, _tDial, tFacing in LASER_COLS
] + [
    Step("gt6energy place " + F(tDial), expect="GT6 energy source placed")
    for _tLaser, tDial, _tFacing in LASER_COLS
] + [
    Step("gt6energy volt " + F(tDial) + " 16384", expect="voltage 16384")
    for _tLaser, tDial, _tFacing in LASER_COLS
] + [
    Step("gt6energy amp " + F(tDial) + " 1", expect="amperage 1")
    for _tLaser, tDial, _tFacing in LASER_COLS
]

laser_on_steps = [Step("gt6energy mode " + F(tDial) + " on", expect="emitting true")
                  for _tLaser, tDial, _tFacing in LASER_COLS]
laser_off_steps = [Step("gt6energy mode " + F(tDial) + " off", expect="emitting false", allow_failed=True)
                   for _tLaser, tDial, _tFacing in LASER_COLS]

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload add 490 452 514 478"),                       # the tick driver
    *structure_steps(),
    Step("setblock " + WRONG + " minecraft:stone", expect="Changed the block"),
    Step("setblock " + C + " " + BIG, expect="Changed the block"),
    Step("execute if block " + C + " gt6:fusion_reactor[formed=false]", expect="Test passed", poll=60),
    Step("setblock " + WRONG + " " + WALL, expect="Changed the block"),
    Step("setblock " + C + " air", expect="Changed the block"),
    Step("setblock " + C + " " + BIG, expect="Changed the block"),
    Step("execute if block " + C + " gt6:fusion_reactor[formed=true]", expect="Test passed", poll=60),

    phase("B: the ignition arms — the :755 ledger is VISIBLE in the block NBT, the :809 gate CLOSES"),
    # the :955 H+C -> C13 row: hydrogen gas 1000 + molten carbon 1000, selector tag 2
    Step("data merge block " + C + ' {input_tank:{FluidName:"gt6:hydrogen",Amount:1000},input_tank_1:{FluidName:"gt6:carbon_molten",Amount:1000}}',
         expect="Modified block data",
         node_cmds={"1.21.1": 'data merge block ' + C + ' {input_tank:{amount:1000,id:"gt6:hydrogen"},input_tank_1:{amount:1000,id:"gt6:carbon_molten"}}'}),
    Step("data merge block " + C + " {inventory:{Size:11,Items:[" + selector_merge(0, 2)["1.20.1"] + "]}}",
         expect="Modified block data",
         node_cmds={"1.21.1": "data merge block " + C + " {inventory:{Size:11,Items:[" + selector_merge(0, 2)["1.21.1"] + "]}}"}),
    # the :755 arm charged the ledger with 315*131072 = 41,287,680 startLU
    Step("data get block " + C, expect="charge_requirement: 41287680L", poll=60),
    Step("data get block " + C, expect="maxprogress: 315L", poll=30),
    # the :809 gate: armed but UNPAID -> frozen (progress 0, inactive, the ring still design 5)
    Step("data get block " + C, expect="progress: 0L", poll=10),
    Step("data get block " + C, expect="active: 0b", poll=10),
    Step("execute if block " + RING_N + " " + GLASS + "[design=5]", expect="Test passed", poll=10),
    # acceptance ② — the deterministic freeze window: 200 more ticks and STILL progress 0
    Step("data get block " + C, expect="progress: 0L",
         tick_step=200, tick_fallback_poll=30),
    # the +-10 sink seats BEFORE the pay (the run starts inside phase C's stepped window;
    # the 101st EU packet explodes it mid-run, the fusion oracle)
    Step("setblock " + S + " " + BOX, expect="Changed the block"),
    Step("data merge block " + S + " {inventory:{Size:4,Items:[{Slot:0b,id:\"gt6:battery_lead_acid_ev\",Count:1b}]}}",
         expect="Modified block data",
         node_cmds={"1.21.1": 'data merge block ' + S + ' {inventory:{Size:4,Items:[{Slot:0b,id:"gt6:battery_lead_acid_ev",count:1}]}}'}),

    phase("C: the pay — four T5 lasers bank 32768 LU/tick through the glass ring (:497)"),
    *laser_place_steps,
    *laser_on_steps,
    # 41,287,680 / 32,768 = 1260 ticks exactly; +40 margin lands INSIDE the 315-tick run
    Step("data get block " + C, expect="active: 1b", tick_step=1300, tick_fallback_poll=150),
    # the active flip rewrote the ring designs 5 -> 6 (the :241/:105 hook)
    Step("execute if block " + RING_N + " " + GLASS + "[design=6]", expect="Test passed", poll=30),

    phase("D: the start — the generator face launches, the EV overcharge sink dies, the run completes"),
    # completion: ~315 ticks of progress at 1/tick; the 101st EU packet exploded the sink
    Step("execute unless block " + S + " gt6:battery_box_ev", expect="Test passed", tick_step=400, tick_fallback_poll=60),
    Step("data get block " + C, expect="active: 0b", poll=30),
    Step("data get block " + C, expect="maxprogress: 0L", poll=30),
    # the :847 completion arm cleared the paid ledger; the product landed molten
    Step("data get block " + C, expect="charge_requirement: 0L", poll=30),
    Step("data get block " + C, expect="carbon13_molten", poll=30),

    phase("E: teardown — the explicit band restore"),
    *laser_off_steps,
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="ignition-gate ignition",
    slug="ignition",
    sites=gt6world.declare_sites(CTRL, SINK_N,
                                 LASER_W, DIAL_W, LASER_E, DIAL_E,
                                 LASER_N, DIAL_N, LASER_S, DIAL_S),
    preferred_ports=(26604, 26614),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
