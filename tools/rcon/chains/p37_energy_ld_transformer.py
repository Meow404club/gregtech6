#!/usr/bin/env python3
"""p37_energy_ld_transformer — the Long Distance transformer + LD electric wire
live acceptance (sweep group p37_energy_ld; the fresh band x563..575, z453..464
— x/z-disjoint from every registered band: the ignition pair sits x438..514
z452..478, the energy-tail band x520..558 z420..432, the LD pipes z500..506,
the covers band z300..312).

  THE DEVICE (GT6LongDistanceTransformerBlockEntity, the p35 port): the LD
  transformer is NOT a step-up/step-down ladder — it is a SAME-VOLTAGE
  pass-through pair (NBT_INPUT = NBT_OUTPUT = V[i]); the FRONT eats the packet,
  the BFS over same-instance LD wire blocks finds the far endpoint, and the
  packet leaves the TARGET's BACK with the distance loss
  aSize - max(64, mDistance/8) (the 0.125 EU/m column). The wire itself has no
  energy face: it is the passive scan medium, throughput VMAX[tier]; a packet
  OVER the wire rating triggers scanWires(true) — the burn arm, the wires die
  (the port removeBlocks them; the fire half is the declared render-pool face).

  A THE t5 LOSS LINE (x=563): dial -> longdist_transformer_t5[facing=south] ->
  8x long_dist_wire_0 (tier 4) -> longdist_transformer_t5[facing=south] ->
  battery_box_ev + one EMPTY EV battery (the p29_w4 chargeable-carrier form;
  the box band is V[4]/2..V[4]*2 = 1024..4096). The mDistance arithmetic: the
  BFS counts one layer per wire, so 8 wires -> mDistance = 8 -> the loss is
  max(64, 8/8) = 64 exactly. The line pins that 64 from BOTH sides on ONE box:
    - volt 1024: the packet lands at 960 — BELOW the target's output min 1024
      -> refused, the box stays dark (gt.active: 0b, gt.energy: 0L);
    - volt 1088: the packet lands at EXACTLY 1024 — the boundary arm: a larger
      loss would refuse it, a smaller one is excluded by the dark arm above.
  The link metrics ride the sender's NBT (the saveAdditional pair, written only
  on a live link): gt.distance: 8L + gt.throughput: 4096L — the BFS scan and
  the VMAX[4] throughput verbatim; the target lights gt.active: 1b too.

  B THE BURN LINE (x=569): the same tier-4 wire blob under a t6 pair — the
  dial's 8192 packet passes the sender's input band (V[5]/2 = 4096 .. 16384)
  but breaches the wire rating VMAX[4] = 4096 -> scanWires(true): the blob is
  CONSUMED (doInject returns used-all, nothing delivered). Both end probes read
  air (execute if block -> "Test failed" = the wire is GONE, the p12 idiom).

  C THE NO-LINK NEGATIVE (x=575): a t5 sender with the box DIRECTLY on its
  BACK — no wire blob, checkTarget() fails, doInject returns 0: the box stays
  dark over a settled window (the link is the transfer, not adjacency).

  D THE REGISTRATION CENSUS: the five endpoint items t5..t9 (meta 10064..10068)
  + the sixteen wire metas long_dist_wire_0..15 in one chest — the item half of
  both families (the block half rode the setblock/fill arms).

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p37_energy_ld_transformer.py
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

# A: the t5 loss line (x=563; z-descending: dial south, box north)
A_BOX = gt6world.Site(563, 65, 453)      # gt6:battery_box_ev[facing=north]
A_TARGET = gt6world.Site(563, 65, 454)   # gt6:longdist_transformer_t5[facing=south]
A_SENDER = gt6world.Site(563, 65, 463)   # gt6:longdist_transformer_t5[facing=south]
A_DIAL = gt6world.Site(563, 65, 464)     # the dial, on the sender's FRONT (south)
A_WIRES = (563, 65, 455, 563, 65, 462)   # 8x long_dist_wire_0 (tier 4, VMAX 4096)
# B: the burn line (x=569, same z shape, no sink)
B_TARGET = gt6world.Site(569, 65, 454)   # gt6:longdist_transformer_t6[facing=south]
B_SENDER = gt6world.Site(569, 65, 463)   # gt6:longdist_transformer_t6[facing=south]
B_DIAL = gt6world.Site(569, 65, 464)
B_WIRES = (569, 65, 455, 569, 65, 462)
# C: the no-link negative (x=575)
C_BOX = gt6world.Site(575, 65, 462)      # box DIRECTLY on the sender's BACK — no wires
C_SENDER = gt6world.Site(575, 65, 463)   # gt6:longdist_transformer_t5[facing=south]
C_DIAL = gt6world.Site(575, 65, 464)
# D: the census chest
CHEST = gt6world.Site(575, 65, 453)

BAND = (561, 62, 451, 577, 68, 466)

BOX_EV = "gt6:battery_box_ev[facing=north]"
LD_T5 = "gt6:longdist_transformer_t5[facing=south]"
LD_T6 = "gt6:longdist_transformer_t6[facing=south]"


def empty_battery(pos, battery):
    """One EMPTY EV battery merged into the box's slot 0 (the mReceivablePower
    seat — the p35 empty_battery merge form, the Count/count loader fork)."""
    return {
        "1.20.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{battery}",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{battery}",count:1}}]}}}}',
    }

BATTERY_A = empty_battery(A_BOX, "gt6:battery_lead_acid_ev")
BATTERY_C = empty_battery(C_BOX, "gt6:battery_lead_acid_ev")


def wire_fill(coords):
    x1, y1, z1, x2, y2, z2 = coords
    return Step(f"fill {x1} {y1} {z1} {x2} {y2} {z2} gt6:long_dist_wire_0", expect="filled")


steps = [
    Step(f"fill {BAND[0]} {BAND[1]} {BAND[2]} {BAND[3]} {BAND[4]} {BAND[5]} air", expect="filled"),
    Step(f"forceload add {BAND[0]} {BAND[2]} {BAND[3]} {BAND[5]}"),

    phase("A: the t5 loss line — 1024 refused dark, 1088 lands at exactly 1024 (the 64 EU loss pinned from both sides)"),
    Step(f"setblock {F(A_BOX)} {BOX_EV}", expect="Changed the block"),
    Step(BATTERY_A["1.20.1"], expect="Modified block data", node_cmds=BATTERY_A),
    Step(f"data get block {F(A_BOX)}", expect="gt.energy: 0L", sleep=1.0),
    Step(f"setblock {F(A_SENDER)} {LD_T5}", expect="Changed the block"),
    wire_fill(A_WIRES),
    Step(f"setblock {F(A_TARGET)} {LD_T5}", expect="Changed the block"),
    Step(f"gt6energy place {F(A_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(A_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(A_DIAL)} 1024", expect="voltage 1024 EU"),
    # the refusal window: 1024 - 64 = 960 < the target's output min 1024 -> nothing crosses
    Step(f"gt6energy mode {F(A_DIAL)} on", expect="emitting true", sleep=4.0),
    Step(f"data get block {F(A_BOX)}", expect="gt.active: 0b"),
    Step(f"data get block {F(A_BOX)}", expect="gt.energy: 0L"),
    # 1088 - 64 = 1024 = exactly the output min -> the boundary packet crosses
    Step(f"gt6energy volt {F(A_DIAL)} 1088", expect="voltage 1088 EU"),
    Step(f"data get block {F(A_BOX)}", expect="gt.active: 1b", sleep=3.0, poll=25.0),
    Step(f"data get block {F(A_SENDER)}", expect="gt.distance: 8L"),
    Step(f"data get block {F(A_SENDER)}", expect="gt.throughput: 4096L"),
    Step(f"data get block {F(A_TARGET)}", expect="gt.active: 1b"),
    Step(f"gt6energy mode {F(A_DIAL)} off", expect="emitting false"),

    phase("B: the burn line — 8192 over the tier-4 wire (VMAX 4096) consumes the blob"),
    Step(f"setblock {F(B_SENDER)} {LD_T6}", expect="Changed the block"),
    wire_fill(B_WIRES),
    Step(f"setblock {F(B_TARGET)} {LD_T6}", expect="Changed the block"),
    Step(f"gt6energy place {F(B_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(B_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(B_DIAL)} 8192", expect="voltage 8192 EU"),
    Step(f"gt6energy mode {F(B_DIAL)} on", expect="emitting true", sleep=4.0),
    # both end probes: the sender-side wire (z462) and the target-side wire (z455) —
    # GONE ("Test failed" = the block is absent, the p12 execute-if idiom)
    Step("execute if block 569 65 462 gt6:long_dist_wire_0", expect="Test failed"),
    Step("execute if block 569 65 455 gt6:long_dist_wire_0", expect="Test failed"),
    Step(f"gt6energy mode {F(B_DIAL)} off", expect="emitting false"),

    phase("C: the no-link negative — the box on the sender's BACK stays dark (no wire blob, no transfer)"),
    Step(f"setblock {F(C_BOX)} {BOX_EV}", expect="Changed the block"),
    Step(BATTERY_C["1.20.1"], expect="Modified block data", node_cmds=BATTERY_C),
    Step(f"setblock {F(C_SENDER)} {LD_T5}", expect="Changed the block"),
    Step(f"gt6energy place {F(C_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(C_DIAL)} EU", expect="type ENERGY.ELECTRICITY"),
    Step(f"gt6energy volt {F(C_DIAL)} 2048", expect="voltage 2048 EU"),
    Step(f"gt6energy mode {F(C_DIAL)} on", expect="emitting true", sleep=4.0),
    Step(f"data get block {F(C_BOX)}", expect="gt.active: 0b"),
    Step(f"data get block {F(C_BOX)}", expect="gt.energy: 0L"),
    Step(f"gt6energy mode {F(C_DIAL)} off", expect="emitting false"),

    phase("D: the registration census — the five endpoints + the sixteen wire metas"),
    Step(f"setblock {F(CHEST)} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {F(CHEST)} container.0 with gt6:longdist_transformer_t5 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.1 with gt6:longdist_transformer_t6 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.2 with gt6:longdist_transformer_t7 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.3 with gt6:longdist_transformer_t8 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.4 with gt6:longdist_transformer_t9 1", expect="Replaced"),
    *[Step(f"item replace block {F(CHEST)} container.{5 + meta} with gt6:long_dist_wire_{meta} 1", expect="Replaced")
      for meta in range(16)],
    Step(f"data get block {F(CHEST)} Items[0]", expect="gt6:longdist_transformer_t5"),
    Step(f"data get block {F(CHEST)} Items[5]", expect="gt6:long_dist_wire_0"),
    Step(f"data get block {F(CHEST)} Items[20]", expect="gt6:long_dist_wire_15"),

    phase("E: teardown — restore the band"),
    Step(f"fill {BAND[0]} {BAND[1]} {BAND[2]} {BAND[3]} {BAND[4]} {BAND[5]} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p37_energy_ld_transformer long distance transformer + LD wire live",
    slug="p37eldtrans",
    sites=gt6world.declare_sites(A_BOX, A_TARGET, A_SENDER, A_DIAL,
                                 B_TARGET, B_SENDER, B_DIAL,
                                 C_BOX, C_SENDER, C_DIAL, CHEST),
    preferred_ports=(26782, 26792),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
