#!/usr/bin/env python3
"""fe_inbound — the FE->EU converter live acceptance chain (REWRITE, task
fe-inbound-chain-rewrite).

The cut-eu-fe-bridge card felled the EU->fe_battery outbound bridge, and
with it this chain's A/B/C receipt receiver died: the converter's emit face had
NO legal consumer left (state tasks.cut-eu-fe-bridge.fein-handover,
RCON RED [6,6] both legs, steps {5,6,10,16,17,21} = the dead-booking arms).
The ULV machine ladder (c-ulv-machine-ladder, merged) fixes the endpoint:
wiremill_ulv's window is minIn=4 / recIn=8 / maxIn=16, so the converter's
8 EU x 1 A packet is ACCEPTED by EnergyGate.gateInjection :49 (|8| >= min 4,
doInject fires — the oven T1 min 16 arm at :50 that SWALLOWS the same packet is
the W3 negative control). The chain keeps the four-phase shape; every
fe_battery booking arm is gone (the fixture stays in the world, out of here).

The live economics (the W3 calibration findings, load-bearing here): the
machine's doWork drains mInputMax (16) UNCONDITIONALLY every tick
(TileEntityBasicMachine :465), so a steady 1 A stream of 8 EU packets is
gate-accepted into mEnergy, drained again the same tick, and NEVER banks the
16 EU a shared RM row needs to start (mMinEnergy=16; doActive binds it on the
first packet's tick, :476-488 — after that the :455 gate oscillates doInactive's
CONSTANT_ENERGY progress reset). The acceptance living proof is therefore the
CONVERTER-side accounting plus the machine-side bind:

  A PUSH ACCEPTANCE (z=30; converter 550, wiremill_ulv 551, bare converter 553):
    the mill is data-merged facing 5 (east — the machine BE ignores the
    blockstate facing, mFacing only moves via placement/NBT, the W3 driver
    face) so its BACK (west) meets the converter's all-sides emit DIRECTLY (no
    wire: min wire loss 1 + the :465 drain economics starve any wired hop).
    Copper stick merged, both converters pushed 512 FE, ONE shared 4 s window
    (the drain needs 16 ticks at 1 packet/tick — 5x headroom), then:
    - the FED converter renders "buffer 0 FE" — all 16 packets TAKEN, the
      只扣实收 arm (:87 shape) booked only packets the network actually used;
    - the mill check pins the whole machine-side story in one line:
      "progress=0/128 energy=0 minenergy=16 minIn=4 recIn=8 maxIn=16" — the
      copper row BOUND on the eaten packets (minenergy=16, maxprogress
      16x8=128) and the stick CONSUMED AT BIND (upstream consume-on-bind: the
      slot reads air, the output stays pending in mOutputItems), jammed at
      0/128 because the :455 gate (8 < the bound 16) oscillates doInactive's
      CONSTANT_ENERGY progress reset — acceptance is NOT completion at 1 A,
      the balance ruling live; completion needs a whole >=16 EU packet (the
      W3 dynamo form) or two packets in one tick;
    - the BARE converter (one air gap, no consumer) renders "buffer 512 FE" —
      the explicit negative arm: nothing eats an emitter with no consumer, so
      the fed converter's drain above is CONSUMPTION, not leakage. In-chain
      assertion, no allow_failed.
  B PULL + FLOOR ALIGNMENT (z=38; source 550, converter 551): the extractable
    fe_source fixture is set to 130 FE (4 packets + a 2 FE tail); the
    converter PULLS one packet per tick through root EnergyBridge.extractFe
    (the a seam, the #2089 dual-support half GTCEu lacks). After the drain
    the source keeps "stored 2 FE" — the sub-packet tail NEVER leaves the
    source (the hostile-remainder semantics live) — and the converter's own
    capacitor books exactly "buffer 128 FE": the packet-quantised receipt the
    dead fe_battery arm used to assert, now on the rig's designed dead-end (no
    consumer: the emit arm finds no adjacency and the packets stay buffered).
  C THROUGHPUT CEILING (z=46; source 550, converter 551, wiremill_ulv 552):
    a full 100k FE source feeds the machine for a 5 s window (~100 server
    ticks at the 20 tps cap). The 1 A pull cap drains 3200 +- band FE, so the
    source renders "stored 96xxx" — valid elapsed [3.1..6.2 s] all lands
    96xxx, and ANY two-packets-per-tick regression (the plausible integer-amps
    bug, 16 EU/t) drains 6400+ and lands 92xxx/93xxx = RED. The poll absorbs
    slow-side lag (a lagging window re-reads a moment later, still 96xxx);
    over-drain is monotone, so a real regression can never poll GREEN. The
    old chain's single-shot 6 s window had ~0.15 s of validity on each side —
    the "速率类断言加足窗口" lesson, re-derived with the band arithmetic in
    the docstring where it can be re-checked.
  D OVERLOAD EXPLOSION RETAINED (z=54; converter 550, far from the rigs):
    data merge forces the persisted buffer to 100000 FE — a value both intake
    faces clamp, i.e. only a foreign writer can produce (the upstream "Machine
    overloaded on Chunkload" scenario). Past the 2-tick grace the upstream
    ladder overcharges (TileEntityBase10EnergyConverter :122-126 -> Root :330):
    the machine dies with the suspended explosion. The stat step afterwards
    carries the GT6 "STAT FAILED" marker judged allow_failed -> ALLOWED = the
    block is GONE (allow_failed is the honest absence proof; the failure
    marker text itself is the evidence — not a negative-arm escape).

The pass-2 repeat is the idempotency proof (gt6world bbox cleanup between
passes). Band: x548..555, z28..56 (sites x550..553, arms z-disjoint 30/38/46/
54, margin 2 clear) — disjoint from the whole roster including the W3 band
x518..545 (sweep --plan census).

Run:  python3 tools/rcon/chains/fe_inbound.py
      python3 tools/rcon/chains/fe_inbound.py --node 1.21.1-neoforge
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

# --- arm A: the push acceptance + the bare-consumer negative contrast (z=30)
CONV_A = gt6world.Site(550, 64, 30)    # the FED converter, west of the mill
MILL_A = gt6world.Site(551, 64, 30)    # wiremill_ulv, facing merged east (back = west)
CONV_A2 = gt6world.Site(553, 64, 30)   # the BARE converter — one air gap, no consumer

# --- arm B: pull + floor alignment, the tail stays in the source (z=38)
SRC_B = gt6world.Site(550, 64, 38)
CONV_B = gt6world.Site(551, 64, 38)

# --- arm C: the throughput ceiling, steady 8 EU/t over the window (z=46)
SRC_C = gt6world.Site(550, 64, 46)
CONV_C = gt6world.Site(551, 64, 46)
MILL_C = gt6world.Site(552, 64, 46)    # the sink, east of the converter

# --- arm D: overload explosion, far from the rigs (z=54)
CONV_D = gt6world.Site(550, 64, 54)

CONV_A_P, CONV_A2_P, MILL_A_P = F(CONV_A), F(CONV_A2), F(MILL_A)
SRC_B_P, CONV_B_P = F(SRC_B), F(CONV_B)
SRC_C_P, CONV_C_P, MILL_C_P = F(SRC_C), F(CONV_C), F(MILL_C)
CONV_D_P = F(CONV_D)

MILL_BLOCK = "gt6:wiremill_ulv"

# the inventory data-merge Count key fork (the food_can precedent, W3 form):
# 1.20.1 NBT `Count:1b` vs 21.1 component-era `count:1`.
def feed_merge(pos, item_id):
    return {
        "1.20.1": f"data merge block {pos} {{inventory:{{Size:2,Items:[{{Slot:0b,id:\"{item_id}\",Count:1b}}]}}}}",
        "1.21.1": f"data merge block {pos} {{inventory:{{Size:2,Items:[{{Slot:0b,id:\"{item_id}\",count:1}}]}}}}",
    }

CU_MERGE = feed_merge(MILL_A_P, "gt6:stick_copper")

CHAIN = Chain(
    name="fe-inbound",
    slug="fein",
    sites=gt6world.declare_sites(
        CONV_A, MILL_A, CONV_A2,
        SRC_B, CONV_B,
        SRC_C, CONV_C, MILL_C,
        CONV_D),
    preferred_ports=(26166, 26176),
    passes=2,
    steps=[
        phase("A: push acceptance — the fed converter drains into the wiremill_ulv's back, the bare converter holds (explicit negative)"),
        Step(f"setblock {MILL_A_P} {MILL_BLOCK}", expect="Changed the block"),
        # the machine BE ignores the setblock blockstate facing (mFacing only
        # moves via placement/NBT — the W3 calibration): facing 5 = east puts
        # the SBIT_B back face (west) on the converter. NO WIRE on purpose
        # (min wire loss 1 + the doWork :465 unconditional mInputMax drain
        # starve any wired hop — the W3 finding).
        Step(f"data merge block {MILL_A_P} {{facing:5}}", expect="Modified block data"),
        Step(CU_MERGE["1.20.1"], expect="Modified block data", node_cmds=CU_MERGE),
        Step(f"gt6feconverter place {CONV_A_P}", expect="voltage 8 EU x 1 A"),
        Step(f"gt6feconverter stat {CONV_A_P}", expect="buffer 0 FE"),
        Step(f"gt6feconverter place {CONV_A2_P}", expect="voltage 8 EU x 1 A"),
        Step(f"gt6feconverter stat {CONV_A2_P}", expect="buffer 0 FE"),
        Step(f"gt6feconverter push {CONV_A_P} 512", expect="pushed 512 FE, accepted 512 FE"),
        # one shared window: the fed converter needs 16 ticks to emit its 16
        # packets (1 packet/tick cap) — 4 s is 5x the headroom
        Step(f"gt6feconverter push {CONV_A2_P} 512", expect="pushed 512 FE, accepted 512 FE", sleep=4.0),
        # THE ACCEPTANCE LIVING PROOF: every packet TAKEN (the 只扣实收 arm
        # books only what the network used) — the receipt the dead fe_battery
        # arm used to assert, now on the ULV machine endpoint
        Step(f"gt6feconverter stat {CONV_A_P}", expect="buffer 0 FE"),
        # the machine side, ONE contiguous pin of the whole story (live
        # calibrated): the row BOUND on the eaten packets and CONSUMED the
        # stick at bind (upstream consume-on-bind — the slot reads air), the
        # jam 0/128 = 8 EU/t can never re-meet the bound 16 EU/t start gate
        # (the :455 gate oscillates doInactive's CONSTANT_ENERGY reset; the
        # output stays pending in mOutputItems, never placed) — acceptance is
        # not completion at 1 A, the balance ruling live — plus the ULV
        # window verbatim and the stable bound gate (doInactive never resets
        # mMinEnergy; post-feed mEnergy is a settled 0)
        Step(f"gt6machine wiremill check {MILL_A_P}",
             expect="progress=0/128 energy=0 minenergy=16 minIn=4 recIn=8 maxIn=16"),
        # THE EXPLICIT NEGATIVE ARM: nothing eats an emitter with no consumer
        # — the 512 FE ride in place, proving the drain above is consumption,
        # not leakage. In-chain assertion, no allow_failed.
        Step(f"gt6feconverter stat {CONV_A2_P}", expect="buffer 512 FE"),

        phase("B: pull + floor alignment (source 130 FE = 4 packets + 2 FE tail; the tail never leaves the source)"),
        Step(f"gt6fesource place {SRC_B_P}", expect="stored 100000 FE"),
        Step(f"gt6fesource set {SRC_B_P} 130", expect="stored 130 FE"),
        Step(f"gt6feconverter place {CONV_B_P}", expect="voltage 8 EU x 1 A", sleep=4.0),
        Step(f"gt6fesource stat {SRC_B_P}", expect="stored 2 FE"),
        Step(f"gt6feconverter stat {CONV_B_P}", expect="buffer 128 FE"),

        phase("C: throughput ceiling (full source, 5 s window at the 20 tps cap -> source keeps 96xxx FE; 2 packets/tick would land 92xxx)"),
        Step(f"setblock {MILL_C_P} {MILL_BLOCK}", expect="Changed the block"),
        Step(f"data merge block {MILL_C_P} {{facing:5}}", expect="Modified block data"),
        Step(f"gt6fesource place {SRC_C_P}", expect="stored 100000 FE"),
        Step(f"gt6feconverter place {CONV_C_P}", expect="voltage 8 EU x 1 A"),
        # the ceiling window rides this step's sleep (the machine check is the
        # double-duty rider: the ULV window pin re-read mid-drain). Valid
        # elapsed [3.1..6.2 s] lands 96xxx; the poll below absorbs slow-side lag.
        Step(f"gt6machine wiremill check {MILL_C_P}", expect="minIn=4 recIn=8 maxIn=16", sleep=5.0),
        Step(f"gt6fesource stat {SRC_C_P}", expect="stored 96", poll=6.0),

        phase("D: overload explosion retained (data merge 100000 FE past the intake clamps -> the 2-tick grace -> overcharge -> gone)"),
        Step(f"gt6feconverter place {CONV_D_P}", expect="voltage 8 EU x 1 A"),
        Step(f"data merge block {CONV_D_P} {{fe: 100000}}", expect="Modified block data", sleep=2.0),
        Step(f"gt6feconverter stat {CONV_D_P}", expect="STAT FAILED", allow_failed=True),
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
