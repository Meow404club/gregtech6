#!/usr/bin/env python3
"""fusion — the Fusion Reactor chain (task fusion, the fusion sweep group;
Loader_MultiTileEntities.java:1242, upstream FusionReactor.java:47-126).

  the 17198 controller: 27 Quadcore PUs (3 versatile / 12 logic / 12 control) in the
  d^2<4 core cross + 50 vents + 48 GalvSteel shell walls + the 6 arms (facing skipped)
  + the three OCTAGONS rings (72 glass y-1/y0/y+1, 72-coil ring, 36-cell SS ring) —
  the controller self-cell is NOT part of the structure, the core centre sits 2 cells
  BEHIND the north-facing front (z+2). The hand-written walk has NO declared pattern:
  forming rides `gt6multiblock form` (the checker command) AND the onTickFirst forced
  check after the re-place; the controller is placed LAST (the vanilla build flow).

  the D+T row (Loader_Recipes_Other.java:959, ST.tag(2), eUt -8192, dur 1760):
  deuterium 1000 mB + tritium 1000 mB -> helium 1000 mB. The drive is the input-tank
  NBT merge (input_tank/input_tank_1 — non-energy keys always land, the massfab
  lesson) + the selector circuit in slot 0. Progress rate 1/tick (the :455 TU
  self-generation vs the :791 mInputMax drain), so the run completes in 1760 ticks
  and the generator face emits 8192 EU/tick while progressing (the :761-764 trio +
  the :812 spot).

  the energy faces:
  - the glass ring y0 (design 5 inactive / 6 active, ONLY_ENERGY_IN) relays LU to the
    controller through the part energy relay; the /gt6energy LU dial parked against a
    ring cell proves the face is reachable. SINCE ignition-gate the gate is LIVE
    (Loader:1242 supplies NBT_SPECIAL_IS_START_ENERGY via the readFromNBT2 :112-124
    config injection -> the :755 write is reachable -> the :809 progress gate closes
    until the :497-500 LU decrement pays it), so phase D arms the ledger (the D+T row
    startLU 230,686,720 = 1760 x 8192 x 16 EXACTLY — the :959 setSpecialNumber form,
    port-side GT6RecipesFusion.java:135 START_LU_PER_TICK) and then pays it in one
    tick by bumping the dial to amperage 28160 (the :497 charged arm banks aSize x
    aAmount whole with no size gate — TileEntityFusionReactor.doEnergyInjection —
    and the part relay forwards aAmount verbatim, so one 28160-ampere tick clears
    it; after pay the surplus LU falls to the :501 type gate and is refused,
    harmless).
  - the EU launch: the battery_box_ev sink at the north +-10 point counts the
    :233-236 packets through its OVERCHARGE arm. No EU box can BUFFER an 8192
    packet (only LU crystals are tier-5 and the EU box rejects foreign-type
    batteries in recountBatteries, so mReceivablePower stays 0 -> refuse), but an
    EV box with one empty EV battery has mReceivablePower > 0 (the empty battery
    takes 2048-packets) and the 8192 packet size exceeds the V[4]*2=4096 gate ->
    the :493-496 overcharge arm strikes (soft 100, then the Root explosion — the
    Base10 :140-:148 ladder verbatim). The 101st packet (tick 101 of the run)
    explodes the box: `execute unless block` flips and stays flipped = the live
    +-10 arrival assert, tick-order insensitive.

  the design flip: forming writes design 5 on the ring; the run start flips mActive
  and the base onTickCheck :105 hook (refreshStructureOnActiveStateChange = T,
  upstream :241) re-runs the walk and rewrites the ring to design 6.

acceptance arms this chain carries:
  1 the wrong-part rejection (formed=false) and the all-green form (formed=true)
  2 the D+T run at 1 progress/tick: maxprogress 1760, active 1b -> 0b at completion
  3 the design 5 -> 6 ring rewrite on the active flip
  4 the LU dial against the glass ring (the relayed face answers)
  5 the +-10 EU arrival: the north overcharge sink explodes on the 101st packet

Run:  GT6_SESSION=off python3 tools/rcon/chains/fusion.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

CTRL = gt6world.Site(450, 65, 462)                 # the controller (facing north; NOT a structure cell)
CORE = (450, 65, 464)                              # the core centre (2 cells behind the front)
SINK_N = gt6world.Site(450, 65, 454)               # the +-10 EU sink (north of the core, the first push side)
DIAL = gt6world.Site(441, 65, 461)                 # the LU source dial (adjacent to the ring cell 441,65,462)
RING_CELL = "441 65 462"                           # the dial-adjacent glass ring cell (i=0, j=7)
BAND = (438, 58, 452, 462, 74, 478)                # the fresh band (x438..462, y58..74, z452..478)
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
BOX = "gt6:battery_box_ev"
WRONG = "447 65 464"                                # the X-3 arm cell for the rejection arm


# ---------------------------------------------------------------------------
# the structure placement, generated from the transcribed masks (the same
# OCTAGONS table the BE carries — the upstream :131-191 constants)
# ---------------------------------------------------------------------------

def _octagon_rows():
    OCT = []
    OCT.append([[*map(lambda c: c == "T", row.split(","))] for row in [
        "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F",
        "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F",
        "F,F,F,T,F,F,F,T,T,T,T,T,F,F,F,T,F,F,F", "F,F,T,F,F,F,T,F,F,F,F,F,T,F,F,F,T,F,F",
        "F,T,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,T,F", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T",
        "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T",
        "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T", "T,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,T",
        "F,T,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,T,F", "F,F,T,F,F,F,T,F,F,F,F,F,T,F,F,F,T,F,F",
        "F,F,F,T,F,F,F,T,T,T,T,T,F,F,F,T,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F",
        "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F",
        "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F"]])
    OCT.append([[*map(lambda c: c == "T", row.split(","))] for row in [
        "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F",
        "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,T,F,T,T,T,T,T,F,T,F,F,F,F,F",
        "F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F", "F,F,F,T,F,T,F,F,F,F,F,F,F,T,F,T,F,F,F",
        "F,F,T,F,T,F,F,F,F,F,F,F,F,F,T,F,T,F,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F",
        "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F",
        "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F", "F,T,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,T,F",
        "F,F,T,F,T,F,F,F,F,F,F,F,F,F,T,F,T,F,F", "F,F,F,T,F,T,F,F,F,F,F,F,F,T,F,T,F,F,F",
        "F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F", "F,F,F,F,F,T,F,T,T,T,T,T,F,T,F,F,F,F,F",
        "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F", "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F",
        "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F"]])
    OCT.append([[*map(lambda c: c == "T", row.split(","))] for row in [
        "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F",
        "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F",
        "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F",
        "F,F,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F",
        "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F",
        "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F", "F,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,T,F,F",
        "F,F,F,T,F,F,F,F,F,F,F,F,F,F,F,T,F,F,F", "F,F,F,F,T,F,F,F,F,F,F,F,F,F,T,F,F,F,F",
        "F,F,F,F,F,T,F,F,F,F,F,F,F,T,F,F,F,F,F", "F,F,F,F,F,F,T,F,F,F,F,F,T,F,F,F,F,F,F",
        "F,F,F,F,F,F,F,T,T,T,T,T,F,F,F,F,F,F,F", "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F",
        "F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F"]])
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

    # the 5x5x5 core: the PU quota split (3 V / 12 L / 12 C over the d^2<4 cross in
    # walk order), the GalvSteel shell (d^2>6 + the j==0 axis tips), the 50 vents.
    # Placed cell-by-cell in the :54-70 walk order — the quota assignment is the
    # independent re-derivation (any split >= 3/12/12 forms).
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
    # the six arms (X +-3/+-4, Z+3/+4 — the facing (north, z-) direction skipped)
    for tD in (3, 4):
        tSteps.append(Step("setblock %d %d %d %s" % (tCX - tD, tCY, tCZ, WALL), expect="Changed the block"))
        tSteps.append(Step("setblock %d %d %d %s" % (tCX + tD, tCY, tCZ, WALL), expect="Changed the block"))
        tSteps.append(Step("setblock %d %d %d %s" % (tCX, tCY, tCZ + tD, WALL), expect="Changed the block"))

    # the three OCTAGONS rings — per (x, mask-row) z-run fills at the five y layers
    tRX, tRZ = tCX - 9, tCZ - 9
    OCT = _octagon_rows()
    for tI in range(19):
        for tJStart, tJEnd in _runs(OCT[0][tI]):
            # OCT[0]: y-1/y+1 walls (item/fluid), y0 the glass ring
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
    """The selector circuit stack (the implosion Damage-carrier fork)."""
    return {
        "1.20.1": '{Slot:%db,id:"gt6:integrated_circuit",Count:1b,tag:{Damage:%d}}' % (slot, config),
        "1.21.1": '{Slot:%db,id:"gt6:integrated_circuit",count:1,components:{"minecraft:custom_data":{Damage:%d}}}' % (slot, config),
    }


steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload add 438 452 462 478"),                       # the tick driver
    *structure_steps(),
    # the wrong-part arm: a stone arm cell, then the controller (placed LAST — the
    # onTickFirst forced check is the forming path of a hand-written walk; the
    # gt6multiblock form command REFUSES pattern-less controllers, so the verdicts
    # ride the blockstate polls)
    Step("setblock " + WRONG + " minecraft:stone", expect="Changed the block"),
    Step("setblock " + C + " " + BIG, expect="Changed the block"),
    Step("execute if block " + C + " gt6:fusion_reactor[formed=false]", expect="Test passed", poll=60),
    # the fix arm: the right wall back, the controller re-placed (the fresh onTickFirst)
    Step("setblock " + WRONG + " " + WALL, expect="Changed the block"),
    Step("setblock " + C + " air", expect="Changed the block"),
    Step("setblock " + C + " " + BIG, expect="Changed the block"),
    Step("execute if block " + C + " gt6:fusion_reactor[formed=true]", expect="Test passed", poll=60),

    phase("B: the ring design 5 face + the LU dial"),
    Step("execute if block " + RING_CELL + " " + GLASS + "[design=5]", expect="Test passed", poll=30),
    Step("gt6energy place " + gt6world.fmt(DIAL), expect="GT6 energy source placed"),
    Step("gt6energy type " + gt6world.fmt(DIAL) + " LU", expect="type ENERGY.LIGHT"),
    Step("gt6energy volt " + gt6world.fmt(DIAL) + " 8192", expect="voltage 8192"),
    Step("gt6energy amp " + gt6world.fmt(DIAL) + " 1", expect="amperage 1"),
    # the dial stays OFF until the pay: an emitting dial starts draining the ledger
    # the tick the :755 arm fires, and the exact-value assert below would never see
    # the freshly armed 230,686,720
    Step("gt6energy mode " + gt6world.fmt(DIAL) + " off", expect="emitting false"),

    phase("C: the sink — an EV overcharge counter (the empty battery primes mReceivablePower)"),
    Step("setblock " + S + " " + BOX, expect="Changed the block"),
    Step("data merge block " + S + " {inventory:{Size:4,Items:[{Slot:0b,id:\"gt6:battery_lead_acid_ev\",Count:1b}]}}",
         expect="Modified block data",
         node_cmds={"1.21.1": 'data merge block ' + S + ' {inventory:{Size:4,Items:[{Slot:0b,id:"gt6:battery_lead_acid_ev",count:1}]}}'}),

    phase("D: the D+T run — the input-tank merge, 1760 ticks, maxprogress 1760"),
    # the tank merge shape forks per leg: forge = the legacy FluidName/Amount pair,
    # 21.1 = the codec face (lowercase amount + id — the legacy pair loads nothing and
    # logs "Tried to load invalid fluid", the 18:49:49 server ERROR the first sweep caught)
    Step("data merge block " + C + ' {input_tank:{FluidName:"gt6:deuterium",Amount:1000},input_tank_1:{FluidName:"gt6:tritium",Amount:1000}}',
         expect="Modified block data",
         node_cmds={"1.21.1": 'data merge block ' + C + ' {input_tank:{amount:1000,id:"gt6:deuterium"},input_tank_1:{amount:1000,id:"gt6:tritium"}}'}),
    Step("data merge block " + C + " {inventory:{Size:11,Items:[" + selector_merge(0, 2)["1.20.1"] + "]}}",
         expect="Modified block data",
         node_cmds={"1.21.1": "data merge block " + C + " {inventory:{Size:11,Items:[" + selector_merge(0, 2)["1.21.1"] + "]}}"}),
    # the :755 arm charged the ledger with the row startLU (1760 x 8192 x 16
    # = 230,686,720 exactly — the :959 setSpecialNumber form);
    # the :809 gate is now armed-but-unpaid (active 0b until the pay below)
    Step("data get block " + C, expect="charge_requirement: 230686720L", poll=60),
    # the pay (the ignition laser-leg shape, collapsed to the dial): the :497
    # charged arm banks aSize x aAmount whole with no size gate, and the part relay
    # forwards aAmount verbatim — one 28160-ampere tick of the existing 8192-LU dial
    # clears the ledger exactly (230686720 / 8192 = 28160; the surplus dial LU
    # afterwards falls to the :501 type gate and is refused, harmless)
    Step("gt6energy amp " + gt6world.fmt(DIAL) + " 28160", expect="amperage 28160"),
    Step("gt6energy mode " + gt6world.fmt(DIAL) + " on", expect="emitting true"),
    Step("data get block " + C, expect="active: 1b", poll=120),
    Step("data get block " + C, expect="maxprogress: 1760L", poll=60),
    # the active flip rewrote the ring designs 5 -> 6 (the :241/:105 hook)
    Step("execute if block " + RING_CELL + " " + GLASS + "[design=6]", expect="Test passed", poll=60),

    phase("E: the +-10 EU arrival — the 101st packet explodes the overcharge sink"),
    # 100 soft strikes (ticks 1..100 of the run), the 101st overcharge explodes the box
    Step("execute unless block " + S + " gt6:battery_box_ev", expect="Test passed", poll=300),

    phase("F: teardown — the explicit band restore"),
    Step("gt6energy mode " + gt6world.fmt(DIAL) + " off", expect="emitting false", allow_failed=True),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="fusion fusion",
    slug="fusion",
    sites=gt6world.declare_sites(CTRL, SINK_N, DIAL),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
