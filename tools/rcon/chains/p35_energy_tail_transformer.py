#!/usr/bin/env python3
"""p35-energy-tail-machines — the transformer ladder live acceptance (sweep group
p35_energy_tail; the fresh z=420 band, x520..538 — x/z-disjoint from every
registered band: the p28 strip sits z40..64, the ignition band z452..478).

  THE SINK (the p29_w4_battery_charge lesson): the EU sink is the BatteryBox
  with an EMPTY battery merged in (the mReceivablePower headroom needs a
  chargeable carrier — a bare box refuses every packet at the :179 guard).
  The box buffers at <= 3 packets/tick (the :187 headroom limiter), the
  range probes pin the buffer mid-window, and the band-7 push drains the
  buffer into the battery afterwards — the ranges stay live on both probes.
  (The massfab is NOT an EU sink: its mEnergyTypeAccepted is QU — the first
  rig draft starved on that, the bisect lesson.)

  A THE LV-MV STEP-DOWN LEG (the :882 row, V[2]=128 -> V[1]=32): the EU dial
    (volt 128) behind the electric_transformer_t2[facing=south] — the FRONT is
    the INPUT (the Base11 :63 convention, the state is the authority through
    syncFacingFromState) — the box on the north output side. The negative arm
    pins "gt.energy: 0L" with the dial off; the window pins the buffer range.

  B THE REVERSED STEP-UP LEG (the Base11 :80-88 flip, live): the same t2 rung
    [facing=north] starts NORMAL — the west dial's 32-sized offers land on an
    OUTPUT face and die at the gate ("gt.energy: 0L" holds); the /data merge
    flips gt.reversed (the monkey-wrench channel, the p28 NBT face) — the west
    face becomes INPUT, the capacitor accumulates to 128 and the FRONT emits
    ONE 128-sized packet north into the box_mv ("Test passed"). The step-up
    conservation (4x32 in = 1x128 out) is the offline pin; this arm is the
    live flip + the box buffering the transformed packet.

  C THE HV-EV RUNG (the :884 row, V[4]=2048 -> V[3]=512): dial 2048 ->
    electric_transformer_t4[facing=south] -> box_hv (packet band 256..1024).

  D THE REGISTRATION CENSUS (the /give half): the eight ladder items t2..t9
    in one chest — the block/BET half rode the setblock arms, this is the item
    half (the magic-absorber census form).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_energy_tail_transformer.py
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

Z = 420  # the fresh band (x520..538, z-disjoint from every roster band)
# A: the LV-MV step-down column — box(north) <- transformer_t2[facing=south] <- dial(south)
A_BOX = gt6world.Site(520, 65, Z)          # gt6:battery_box_lv (V[1]=32, the 32-sized packet sink)
A_TRANS = gt6world.Site(520, 65, Z + 1)    # gt6:electric_transformer_t2[facing=south] (front = the dial side)
A_DIAL = gt6world.Site(520, 65, Z + 2)     # the 128 EU dial
# B: the reversed step-up column — box(north) <- transformer_t2[facing=north, reversed] <- dial(west)
B_BOX = gt6world.Site(526, 65, Z)          # gt6:battery_box_mv (V[2]=128, the 128-sized packet sink)
B_TRANS = gt6world.Site(526, 65, Z + 1)    # gt6:electric_transformer_t2[facing=north] + the gt.reversed flip
B_DIAL = gt6world.Site(525, 65, Z + 1)     # the 32 EU dial (the west face — an OUTPUT face until the flip)
# C: the HV-EV rung column
C_BOX = gt6world.Site(532, 65, Z)          # gt6:battery_box_hv (V[3]=512)
C_TRANS = gt6world.Site(532, 65, Z + 1)    # gt6:electric_transformer_t4[facing=south]
C_DIAL = gt6world.Site(532, 65, Z + 2)     # the 2048 EU dial
# D: the registration census chest
CHEST = gt6world.Site(538, 65, Z + 1)

BOX_BLOCK = "gt6:battery_box_lv[facing=north]"
BOX_MV_BLOCK = "gt6:battery_box_mv[facing=north]"
BOX_HV_BLOCK = "gt6:battery_box_hv[facing=north]"


LOOM_INPUT = {"1.20.1": "GT6 loom input: 4x string into slot 0", "1.21.1": "GT6 loom input: 4x minecraft:string into slot 0"}
LOOM_DONE = {"1.20.1": "out[0]=1x white_wool", "1.21.1": "out[0]=1x minecraft:white_wool"}

def empty_battery(pos, battery):
    """One EMPTY battery merged into the box's slot 0 (the mReceivablePower seat —
    the p29_w4_battery_charge EMPTY_BATTERY merge form, the carrier fork)."""
    return {
        "1.20.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{battery}",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{battery}",count:1}}]}}}}',
    }

BATTERY_A = empty_battery(A_BOX, "gt6:battery_lead_acid_lv")   # the V[1] carrier
BATTERY_B = empty_battery(B_BOX, "gt6:battery_lead_acid_mv")   # the V[2] carrier
BATTERY_C = empty_battery(C_BOX, "gt6:battery_lead_acid_hv")   # the V[3] carrier

steps = [
    Step(f"fill 518 62 {Z - 2} 540 68 {Z + 5} air", expect="filled"),
    Step(f"forceload add 518 {Z - 2} 540 {Z + 5}"),

    phase("A: the LV-MV rung — 128 in the front, four 32-sized packets into the box sink"),
    Step(f"setblock {F(A_BOX)} {BOX_BLOCK}", expect="Changed the block"),
    Step(BATTERY_A["1.20.1"], expect="Modified block data", node_cmds=BATTERY_A),
    Step(f"data get block {F(A_BOX)}", expect="gt.energy: 0L", sleep=1.0),
    Step(f"setblock {F(A_TRANS)} gt6:electric_transformer_t2[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(A_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(A_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(A_DIAL)} 128", expect="voltage 128 EU"),
    Step(f"data get block {F(A_BOX)}", expect="gt.active: 0b", sleep=2.0),
    Step(f"gt6energy mode {F(A_DIAL)} on", expect="emitting true"),
    # gt.active = the box buffer >= the 32 mOutput — the transformed packets LANDED
    Step(f"data get block {F(A_BOX)}", expect="gt.active: 1b", sleep=3.0, poll=25.0),
    Step(f"gt6energy mode {F(A_DIAL)} off", expect="emitting false"),

    phase("B: the reversed flip — 32 on the west face, ONE 128-sized packet out the front"),
    Step(f"setblock {F(B_BOX)} {BOX_MV_BLOCK}", expect="Changed the block"),
    Step(BATTERY_B["1.20.1"], expect="Modified block data", node_cmds=BATTERY_B),
    Step(f"data get block {F(B_BOX)}", expect="gt.active: 0b", sleep=1.0),
    Step(f"setblock {F(B_TRANS)} gt6:electric_transformer_t2[facing=north]", expect="Changed the block"),
    Step(f"gt6energy place {F(B_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(B_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(B_DIAL)} 32", expect="voltage 32 EU"),
    # NORMAL mode: the west face is an OUTPUT face — the dial's offers die at the gate
    Step(f"gt6energy mode {F(B_DIAL)} on", expect="emitting true"),
    Step(f"data get block {F(B_BOX)}", expect="gt.active: 0b", sleep=3.0, poll=15.0),
    # the Base11 flip (the /data NBT face — the monkey-wrench channel minus the tool)
    Step(f"data merge block {F(B_TRANS)} {{gt.reversed:1b}}", expect="Modified block data"),
    Step(f"data get block {F(B_TRANS)}", expect="reversed: 1b"),
    # the flipped front emits ONE 128-sized packet north — the box_mv buffers it:
    # gt.active (buffer >= the 128 mOutput) pins the step-up live
    Step(f"data get block {F(B_BOX)}", expect="gt.active: 1b", sleep=3.0, poll=25.0),
    Step(f"gt6energy mode {F(B_DIAL)} off", expect="emitting false"),

    phase("C: the HV-EV rung — 2048 in the front, four 512-sized packets into the box sink"),
    Step(f"setblock {F(C_BOX)} {BOX_HV_BLOCK}", expect="Changed the block"),
    Step(BATTERY_C["1.20.1"], expect="Modified block data", node_cmds=BATTERY_C),
    Step(f"data get block {F(C_BOX)}", expect="gt.active: 0b", sleep=1.0),
    Step(f"setblock {F(C_TRANS)} gt6:electric_transformer_t4[facing=south]", expect="Changed the block"),
    Step(f"gt6energy place {F(C_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(C_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(C_DIAL)} 2048", expect="voltage 2048 EU"),
    Step(f"gt6energy mode {F(C_DIAL)} on", expect="emitting true"),
    # the box_hv buffers the 512-sized packets (band 256..1024): gt.active = buffer >= 512
    Step(f"data get block {F(C_BOX)}", expect="gt.active: 1b", sleep=3.0, poll=25.0),
    Step(f"gt6energy mode {F(C_DIAL)} off", expect="emitting false"),

    phase("D: the registration census — the eight ladder items (the /give half)"),
    Step(f"setblock {F(CHEST)} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {F(CHEST)} container.0 with gt6:electric_transformer_t2 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.1 with gt6:electric_transformer_t3 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.2 with gt6:electric_transformer_t4 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.3 with gt6:electric_transformer_t5 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.4 with gt6:electric_transformer_t6 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.5 with gt6:electric_transformer_t7 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.6 with gt6:electric_transformer_t8 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.7 with gt6:electric_transformer_t9 1", expect="Replaced"),
    Step(f"data get block {F(CHEST)} Items[0]", expect="gt6:electric_transformer_t2"),
    Step(f"data get block {F(CHEST)} Items[7]", expect="gt6:electric_transformer_t9"),

    phase("E: teardown — restore the band"),
    Step(f"fill 518 62 {Z - 2} 540 68 {Z + 5} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35_energy_tail_transformer electric transformer ladder",
    slug="p35energytailtransformer",
    sites=gt6world.declare_sites(A_BOX, A_TRANS, A_DIAL, B_BOX, B_TRANS, B_DIAL,
                                 C_BOX, C_TRANS, C_DIAL, CHEST),
    preferred_ports=(26632, 26642),
    game_port=26462,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
