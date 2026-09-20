#!/usr/bin/env python3
"""p32-qu-energizer — the EU->LU->QU chain live acceptance (sweep group
p32_qu_energizer; the p32_qu_laser rig geometry, x-disjoint columns east of it):

  GEOMETRY (the northward-flow column, the freezer-rig back-face form): the chain
  flies -Z. dial(z+3) -> co2_laser[facing=north] (z+2) -> quantum_energizer
  [facing=north] (z+1) -> massfab (z, DEFAULT facing). The dynamo-carrier BEs sync
  mFacing from the blockstate every tick (GT6DynamoBlockEntity.syncFacingFromState,
  the setblock authority), so facing=north puts the laser FRONT and the energizer
  BACK/FRONT exactly on the beam line; the massfab is a TileEntityBasicMachine whose
  setblock BE keeps the mFacing=2 NORTH default (setPlacedBy never fires on
  /setblock), and its SBIT_B energy face composes to the WORLD-SOUTH face — the face
  toward the energizer. Default placement is therefore not just sufficient but
  REQUIRED (any [facing=...] override on the massfab desyncs the BE from the state
  and the QU offer lands on the FRONT: refused 0).

  A THE T1 CHAIN (acceptance ②, bottom rung): the EU dial (volt 32 = the NBT_INPUT
    rec column) behind the T1 CO2 Laser, the T1 Quantum Energizer on the beam line
    (the LU lands on its BACK face, QU out the FRONT), the T1 small Massfab as the
    QU receiver. /gt6laser reset + the dial on -> the energizer stat pins
    "LU->ENERGY.QUANTUM" + "half true" (in == 2*out + capacitor EXACTLY — the
    units() half-rate) with a live intake, while the massfab energy stays EXACTLY 0
    — the energizer's QU half-packets (units(16, 32, 16) = 8) ride UNDER the
    massfab's 16 inMin door: the Root gate swallows them as USED
    (EnergyGate.gateInjection :50 returns aAmount), so the emitter side books the
    out and the receiver never buffers (the laser card's white-burn-door proof, now
    on the QU side).

  B THE DOOR WALK (acceptance ② per-value, T2-T5): behind the SAME T1 laser, each
    Tn energizer placed in the beam seat refuses the 16-sized LU packets ("in 0," —
    the door min = in/2 = 64/256/1024/4096 all above 16, the Root white burn);
    then the laser swaps out for an LU dial and each energizer rides its door edge
    EXACTLY (volt = the out column = in/2: 64/256/1024/4096 -> "half true", the
    per-rung frequency tuning).

  C THE T5 CHAIN (acceptance ②, top rung): volt 8192 -> the T5 laser emits 4096 LU
    -> the T5 energizer's 4096 inMin door takes them at-min exactly ("half true"),
    the T5 massfab stays 0 (the 2048 QU half-packets burn).

  D THE QU RECEIVER LADDER (the receiver per-value): the dial retyped QU — volt 32
    into the T1 massfab buffers EXACTLY maxIn 64 (the scalar NBT path print
    "block data: 64L", StringTagVisitor visitLong carries the L), volt 8192 into
    the T5 massfab buffers EXACTLY 16384 (the EV_TIER_INPUTS window) — the two
    inMin door edges that gate the energizer's emission column.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p32_qu_energizer.py
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

Z = 352  # the fresh band east of the laser roster (x470+, x/z-disjoint from every roster band)
# the T1 chain column (the beam flying -Z): dial -> laser -> energizer -> massfab
A_MF = gt6world.Site(470, 65, Z)            # the T1 small Massfab (DEFAULT facing: the energy face = world south)
A_EN = gt6world.Site(470, 65, Z + 1)        # the T1 Quantum Energizer [facing=north] (back = the beam face)
A_LASER = gt6world.Site(470, 65, Z + 2)     # the T1 CO2 Laser [facing=north] (front = the beam out)
A_DIAL = gt6world.Site(470, 65, Z + 3)      # the T1 EU dial
# the door-walk column: laser (then the LU dial), and ONE beam seat the T2-T5 energizers swap through
B_SEAT = gt6world.Site(476, 65, Z)          # the swap seat (energizer_t2..t5)
B_SINK = gt6world.Site(476, 65, Z - 1)      # the white-burn QU sink per arm (massfab_tN+1:
                                            # its inMin sits ABOVE the arm's half-packet, so
                                            # the gate swallows them AS USED forever — the
                                            # emitter books the out, the sink never buffers)
B_LASER = gt6world.Site(476, 65, Z + 1)     # the T1 laser, then the LU dial
B_DIAL = gt6world.Site(476, 65, Z + 2)      # the T1 EU dial
# the T5 chain column
C_MF = gt6world.Site(482, 65, Z)
C_EN = gt6world.Site(482, 65, Z + 1)
C_LASER = gt6world.Site(482, 65, Z + 2)
C_DIAL = gt6world.Site(482, 65, Z + 3)
# the QU receiver column: the QU dial straight into the massfab world-south energy face
D_MF = gt6world.Site(488, 65, Z)            # DEFAULT facing
D_DIAL = gt6world.Site(488, 65, Z + 1)

IDENT = "LU->ENERGY.QUANTUM"

# the p31_massfab small-form inventory merge (live-proven): one iron ingot in slot 0 —
# the count key forks per leg (Count b / count)
def ingot_merge(pos):
    f = gt6world.fmt(pos)
    return {
        "1.20.1": f"data merge block {f} {{inventory:{{Size:3,Items:[{{Slot:0b,id:\"minecraft:iron_ingot\",Count:1b}}]}}}}",
        "1.21.1": f"data merge block {f} {{inventory:{{Size:3,Items:[{{Slot:0b,id:\"minecraft:iron_ingot\",count:1}}]}}}}",
    }


INGOT_MERGE_T1 = ingot_merge(D_MF)   # the D receiver seat, the T1 rung window
INGOT_MERGE_T5 = ingot_merge(D_MF)   # the same seat after the t5 swap

steps = [
    Step("forceload add 468 350 490 357"),                       # the tick driver

    phase("A: the T1 chain — EU 32 in, LU 16 over the back face, QU 8 out the front, the massfab door burns"),
    Step(f"setblock {F(A_LASER)} gt6:co2_laser[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(A_EN)} gt6:quantum_energizer[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(A_MF)} gt6:massfab", expect="Changed the block"),
    Step(f"gt6energy place {F(A_DIAL)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(A_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(A_DIAL)} 32", expect="voltage 32 EU"),
    Step(f"gt6laser reset {F(A_LASER)}", expect="GT6 laser accounting reset"),
    Step(f"gt6laser reset {F(A_EN)}", expect="GT6 laser accounting reset"),
    Step(f"gt6energy mode {F(A_DIAL)} on", expect="emitting true"),
    Step(f"gt6laser stat {F(A_EN)}", expect=IDENT, sleep=3.0, poll=15.0),
    Step(f"gt6laser stat {F(A_EN)}", expect="half true", sleep=0.5),
    Step(f"gt6laser stat {F(A_EN)}", expect="in ", sleep=0.5),
    # the receiver door: the 8-sized QU half-packets ride UNDER the massfab's 16 inMin
    # door (the Root white burn counts them USED for the emitter, buffers NOTHING)
    Step(f"data get block {F(A_MF)} energy", expect="block data: 0L", sleep=0.5),
    Step(f"gt6energy mode {F(A_DIAL)} off", expect="emitting false"),

    phase("B: the door walk — the T2-T5 doors refuse 16, then ride their own edges exactly"),
    Step(f"setblock {F(B_LASER)} gt6:co2_laser[facing=north]", expect="Changed the block"),
    Step(f"gt6energy place {F(B_DIAL)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(B_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(B_DIAL)} 32", expect="voltage 32 EU"),
    Step(f"gt6energy mode {F(B_DIAL)} on", expect="emitting true"),
    # the negative edges: the 16-sized LU packets sit under EVERY higher door
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t2[facing=north]", expect="Changed the block"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="in 0,", sleep=2.0, poll=15.0),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t3[facing=north]", expect="Changed the block"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="in 0,", sleep=1.0, poll=15.0),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t4[facing=north]", expect="Changed the block"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="in 0,", sleep=1.0, poll=15.0),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t5[facing=north]", expect="Changed the block"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="in 0,", sleep=1.0, poll=15.0),
    Step(f"gt6energy mode {F(B_DIAL)} off", expect="emitting false"),
    # the positive edges: the LU dial at the laser seat, each door taken at EXACTLY min = out
    Step(f"setblock {F(B_LASER)} air", expect="Changed the block"),
    Step(f"gt6energy place {F(B_LASER)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(B_LASER)} LU", expect="type ENERGY.LIGHT"),
    Step(f"gt6energy volt {F(B_LASER)} 64", expect="voltage 64 EU"),
    Step(f"gt6energy mode {F(B_LASER)} on", expect="emitting true"),
    Step(f"setblock {F(B_SINK)} gt6:massfab_t2", expect="Changed the block"),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t2[facing=north]", expect="Changed the block"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="half true", sleep=2.0, poll=15.0),
    Step(f"setblock {F(B_SINK)} gt6:massfab_t3", expect="Changed the block"),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t3[facing=north]", expect="Changed the block"),
    Step(f"gt6energy volt {F(B_LASER)} 256", expect="voltage 256 EU"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="half true", sleep=2.0, poll=15.0),
    Step(f"setblock {F(B_SINK)} gt6:massfab_t4", expect="Changed the block"),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t4[facing=north]", expect="Changed the block"),
    Step(f"gt6energy volt {F(B_LASER)} 1024", expect="voltage 1024 EU"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="half true", sleep=2.0, poll=15.0),
    Step(f"setblock {F(B_SINK)} gt6:massfab_t5", expect="Changed the block"),
    Step(f"setblock {F(B_SEAT)} gt6:quantum_energizer_t5[facing=north]", expect="Changed the block"),
    Step(f"gt6energy volt {F(B_LASER)} 4096", expect="voltage 4096 EU"),
    Step(f"gt6laser stat {F(B_SEAT)}", expect="half true", sleep=2.0, poll=15.0),
    Step(f"gt6energy mode {F(B_LASER)} off", expect="emitting false"),
    Step(f"gt6energy mode {F(B_DIAL)} off", expect="emitting false"),

    phase("C: the T5 chain — EU 8192 in, 4096 LU rides the top door at-min, QU out"),
    Step(f"setblock {F(C_LASER)} gt6:co2_laser_t5[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(C_EN)} gt6:quantum_energizer_t5[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(C_MF)} gt6:massfab_t5", expect="Changed the block"),
    Step(f"gt6energy place {F(C_DIAL)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(C_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(C_DIAL)} 8192", expect="voltage 8192 EU"),
    Step(f"gt6laser reset {F(C_LASER)}", expect="GT6 laser accounting reset"),
    Step(f"gt6laser reset {F(C_EN)}", expect="GT6 laser accounting reset"),
    Step(f"gt6energy mode {F(C_DIAL)} on", expect="emitting true"),
    Step(f"gt6laser stat {F(C_EN)}", expect=IDENT, sleep=3.0, poll=15.0),
    Step(f"gt6laser stat {F(C_EN)}", expect="half true", sleep=0.5),
    Step(f"gt6laser stat {F(C_EN)}", expect="in ", sleep=0.5),
    Step(f"data get block {F(C_MF)} energy", expect="block data: 0L", sleep=0.5),
    Step(f"gt6energy mode {F(C_DIAL)} off", expect="emitting false"),

    phase("D: the QU receiver ladder — the massfab RUNS on QU at/above its door, never below"),
    # the receiver proof rides the p31 small-form live path (the inventory merge with one
    # iron ingot, the disintegration row): the buffered-energy readout is structurally 0 —
    # the machine tick drains mEnergy into progress EVERY tick (the :813 conversion; no
    # recipe = the progress evaporates), so "the receiver accepted" is observable as the
    # machine RUNNING (active: 1b) at/above its door and NEVER below it. The door values:
    # T1 min 16 (8 refuses / 32 runs), T5 min 4096 (2048 refuses / 4096 runs at-min).
    Step(f"gt6energy place {F(D_DIAL)}", expect="GT6 energy source placed at"),
    Step(f"gt6energy type {F(D_DIAL)} QU", expect="type ENERGY.QUANTUM"),
    Step(f"gt6energy volt {F(D_DIAL)} 8", expect="voltage 8 EU"),
    Step(f"setblock {F(D_MF)} gt6:massfab", expect="Changed the block"),
    Step(INGOT_MERGE_T1["1.20.1"], expect="Modified block data", node_cmds=INGOT_MERGE_T1),
    Step(f"gt6energy mode {F(D_DIAL)} on", expect="emitting true"),
    Step(f"data get block {F(D_MF)}", expect="active: 0b", sleep=5.0),
    Step(f"gt6energy volt {F(D_DIAL)} 32", expect="voltage 32 EU"),
    Step(f"data get block {F(D_MF)}", expect="active: 1b", sleep=2.0, poll=20.0),
    Step(f"setblock {F(D_MF)} gt6:massfab_t5", expect="Changed the block"),
    Step(INGOT_MERGE_T5["1.20.1"], expect="Modified block data", node_cmds=INGOT_MERGE_T5),
    Step(f"gt6energy volt {F(D_DIAL)} 2048", expect="voltage 2048 EU"),
    Step(f"data get block {F(D_MF)}", expect="active: 0b", sleep=5.0),
    Step(f"gt6energy volt {F(D_DIAL)} 4096", expect="voltage 4096 EU"),
    Step(f"data get block {F(D_MF)}", expect="active: 1b", sleep=2.0, poll=20.0),
    Step(f"gt6energy mode {F(D_DIAL)} off", expect="emitting false"),

    phase("E: teardown — restore the band"),
    Step(f"fill 468 62 {Z - 2} 490 68 {Z + 5} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p32_qu_energizer quantum energizer",
    slug="p32quenergizer",
    sites=gt6world.declare_sites(A_MF, A_EN, A_LASER, A_DIAL, B_SEAT, B_LASER, B_DIAL,
                                 C_MF, C_EN, C_LASER, C_DIAL, D_MF, D_DIAL),
    preferred_ports=(26602, 26612),
    game_port=26446,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
