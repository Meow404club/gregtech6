#!/usr/bin/env python3
"""p28_ulv_chain — the ULV wave RCON closure chain (task p28-c-ulv-chain, W3).

Four arms over a fresh band (x519..545), one per SPEC arm, sites pairwise
z-disjoint (z=40/48/56/64, margin 2 clear) so concurrent waves may interleave
them. NO new command arm: every reading rides existing faces (/gt6machine
check|inject, /gt6oven check, /gt6engine stat|fuel, /gt6wire place|connect,
/gt6feconverter, /gt6fesource, /gt6energy) plus vanilla /data get|merge NBT
probes — the GT6FeConverterCommand precedent form was NOT needed.

  A POSITIVE CHAIN (z=40): diesel_engine_bronze[facing=east] fuelled gt6:diesel
    burns 16 RU/t DC into two wood-small axles -> electric_dynamo_ulv
    [facing=east]: back=west takes the 16 RU packet (window [1..16]), the T0
    row (declared 1:1, in8/out8/1A) holds it in the 2x-inRec capacitor and the
    front emits one whole-capacitor 16 EU packet per tick DIRECTLY onto
    wiremill_ulv (the SBIT_B energy face = back, rotated to west by the NBT
    facing merge). The machine report pins the ULV window minIn=4 recIn=8
    maxIn=16 (GTMachines.ULV_TIER_INPUTS verbatim); a data-merged
    gt6:stick_copper runs the shared RM.Wiremill row (eUt 16, copper 1357 K <
    the 1375 K gate) and the poll asserts outputs=[4x wire_fine_copper] —
    real grid packets eaten, real output. THE DIRECT HOP IS THE FINDING
    (calibration runs 1-3): a machine BE ignores the setblock facing (mFacing
    only moves via placement/NBT — hence the data merge), and the doWork
    economics drain mInputMax 16/t BEFORE sub-16 buffers can bank, so with a
    minimum wire loss of 1 no fed-through-a-wire eUt-16 row can ever run —
    the ULV machine closes on the dynamo front-to-back, and the ULV-tier
    carriage of V[0] packets over a wire is proven live in arm B instead.
  B NEGATIVE: THE 8 EU PACKET IS DEAD BELOW LV (z=48): task p34-oven-hu-conversion
    rebased the oven to HU (its upstream 20001-04 type), so the LV-wall EU consumer here
    is the electricloom (the in-registry EU variant, T1 window {16,32,64}, both side
    energy faces — the p29_w1_electricloom form): loom -> copper wire -> /gt6energy
    source at volt 8: EnergyGate gateInjection (:50 small-packet arm) SWALLOWS every
    8 EU packet — two energy=0 checks around a 5 s window + the string input intact
    (data get) prove the white burn (no buffer growth, no progress, zero consumption).
    Then volt 32 on the SAME rig completes the string row — the control leg proving
    the red is the packet size, not the rig.
  C NEGATIVE: THE 1375 K MELTING GATE (z=56): two wiremill_ulv rigs. Iron
    (gt6:stick_iron, MT.Fe mMeltingPoint 1811 K > GTMachines.ULV_MELTING_
    GATE_K 1375) is gate-refused at checkRecipe BEFORE any consume (TileEntity
    BasicMachine :655): inject 60x8 leaves progress=0/0 (no recipe ever
    binds), the iron stick still in slot 0 (data get), outputs empty. Copper
    (gt6:stick_copper, 1357 K) passes: inject 300x16 (>= the row's mMinEnergy
    16 — sub-min packets oscillate doInactive's CONSTANT_ENERGY progress
    reset, calibration run 1) completes wire_fine_copper — the control arm.
    D TRANSFORMER THROUGH-TRICKLE (z=64): gt6fesource (budgeted 24576 FE = 768
    whole 32 FE pulls, 6x the row's 4096-EU budget — the oven-era 12288 covered
    512 EU, the loom row is bigger) feeds the gt6feconverter (auto-pull 1
    packet/tick = steady 8 EU/t emit — the observed trickle rate is exactly
    8 EU/t) DIRECTLY onto electric_transformer[facing=east] — gt.reversed
    flipped by /data merge (the W3 driver face the transformer card's
    NBT_REVERSED javadoc names this chain to drive): step-up input =
    all-but-front (west = the converter face), output = front (east) INTO THE
    ELECTRICLOOM DIRECTLY (the p34 EU consumer swap: default north facing puts
    FACING_ROTATIONS[north][west]=4=right on the transformer front — both side
    faces are energy faces, min 16): THROUGH but rate-pinned. NO WIRE in this
    rig ON PURPOSE — the live probe measured a wire feedback loop (the cable
    returns the step-up packets to the transformer's all-but-front input, the
    capacitor self-locks and the trickle dies) plus the per-segment loss; the
    direct front-to-face hop is the clean closed form and the LV-wire leg is
    already proven live in arm B. Nails: the mode read-back "reversed: 1b";
    the pre-flip energy=0; the completion poll out[0]=1x white_wool (the row
    completes on the trickle — 4096 progress at ~8/t ≈ 26 s, a full-rate
    bypass would close in ~3 s); the source drained to EXACTLY stored 0 FE
    (packet-quantised pulls, zero tail). The oven-era immediate
    input-intact pin rode the per-smelt consume granularity — the loom
    consumes its input at recipe bind, so the trickle rate rides the
    completion-timing evidence instead.

passes=2 is the idempotency proof. The chain-level verdict is [0,0] per leg —
the negative arms' expected-reds are IN-CHAIN explicit assertion steps, never
allow_failed escapes.

Run:  python3 tools/rcon/chains/p28_ulv_chain.py
      python3 tools/rcon/chains/p28_ulv_chain.py --node 1.21.1-neoforge
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

# --- arm A: diesel -> axles -> ULV dynamo -> wiremill_ulv, DIRECT front-to-back (z=40)
ENGINE = gt6world.Site(520, 64, 40)
AXLE1 = gt6world.Site(521, 64, 40)
AXLE2 = gt6world.Site(522, 64, 40)
DYNAMO = gt6world.Site(523, 64, 40)
MILL_A = gt6world.Site(524, 64, 40)

# --- arm B: 8 EU vs the LV electricloom wall, then the 32 V control (z=48)
#     (p34: the oven booked EU here until its HU rebase — the loom is the
#     in-registry EU consumer with the same {16,32,64} LV window)
LOOM_B = gt6world.Site(521, 64, 48)
WIRE_B = gt6world.Site(522, 64, 48)
SRC_B = gt6world.Site(523, 64, 48)

# --- arm C: the melting gate, iron refused / copper passes (z=56)
MILL_FE = gt6world.Site(530, 64, 56)
MILL_CU = gt6world.Site(534, 64, 56)

# --- arm D: converter -> step-up transformer -> LV electricloom trickle (z=64)
FESRC = gt6world.Site(540, 64, 64)
FECONV = gt6world.Site(541, 64, 64)
TRANS = gt6world.Site(542, 64, 64)
LOOM_D = gt6world.Site(543, 64, 64)

MILL_A_P, MILL_FE_P, MILL_CU_P = F(MILL_A), F(MILL_FE), F(MILL_CU)
LOOM_B_P, LOOM_D_P, SRC_B_P = F(LOOM_B), F(LOOM_D), F(SRC_B)
TRANS_P, FESRC_P = F(TRANS), F(FESRC)
MILL_A_P = F(MILL_A)

DIESEL = "gt6:diesel_engine_bronze"
AXLE = "gt6:axle_wood_treated_small"
DYNAMO_B = "gt6:electric_dynamo_ulv[facing=east]"
MILL_B = "gt6:wiremill_ulv"
TRANSFORMER = "gt6:electric_transformer[facing=east]"

# the inventory data-merge Count key fork (the p25_food_can precedent):
# 1.20.1 NBT `Count:1b` vs 21.1 component-era `count:1`.
def feed_merge(pos, item_id):
    return {
        "1.20.1": f"data merge block {pos} {{inventory:{{Size:2,Items:[{{Slot:0b,id:\"{item_id}\",Count:1b}}]}}}}",
        "1.21.1": f"data merge block {pos} {{inventory:{{Size:2,Items:[{{Slot:0b,id:\"{item_id}\",count:1}}]}}}}",
    }

CU_MERGE = feed_merge(MILL_A_P, "gt6:stick_copper")
FE_MERGE = feed_merge(MILL_FE_P, "gt6:stick_iron")
CU_MERGE2 = feed_merge(MILL_CU_P, "gt6:stick_copper")

# the electricloom renders (the p29_w1_electricloom fork pattern): the input
# feed echoes "4x string into slot 0" (plain vs namespaced), the check lists
# the output slot as out[N]=, and the NBT data-get always renders the
# namespaced vanilla id.
LOOM_INPUT = {"1.20.1": "GT6 loom input: 4x string into slot 0", "1.21.1": "GT6 loom input: 4x minecraft:string into slot 0"}
LOOM_SLOT = "minecraft:string"  # the data-get id field — namespaced on BOTH legs
LOOM_DONE = {"1.20.1": "out[0]=1x white_wool", "1.21.1": "out[0]=1x minecraft:white_wool"}

# the wiremill renders: the check report lists the output slots as out[N]=
# <count>x <item>, the inject report as outputs=[<count>x <item>; ] — the
# 21.1 leg namespaces the item path (the p26_w1_wiremill fork pattern)
CU_OUT = {"1.20.1": "outputs=[4x wire_fine_copper; ]", "1.21.1": "outputs=[4x gt6:wire_fine_copper; ]"}
CU_CHECK = {"1.20.1": "out[0]=4x wire_fine_copper", "1.21.1": "out[0]=4x gt6:wire_fine_copper"}
FE_SLOT = {"1.20.1": "stick_iron", "1.21.1": "gt6:stick_iron"}

steps = [
    # ---------------------------------------------------------------- arm A
    phase("A: the positive chain — diesel 16 RU/t -> axles -> electric_dynamo_ulv -> wiremill_ulv direct closure"),
    Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {F(AXLE1)} {AXLE}[axis=x]", expect="Changed the block"),
    Step(f"setblock {F(AXLE2)} {AXLE}[axis=x]", expect="Changed the block"),
    Step(f"setblock {F(DYNAMO)} {DYNAMO_B}", expect="Changed the block"),
    # the sleep + the NBT facing merge: a machine BE ignores the setblock
    # facing (mFacing defaults north and only the placement/NBT path writes it
    # — calibration run 1). The merge makes mFacing=5 (east), so the machine's
    # SBIT_B energy face (back = west) meets the dynamo's front DIRECTLY.
    # NO WIRE IN THIS HOP ON PURPOSE (the calibration-run 3 finding): the T0
    # dynamo's whole-capacitor packet is 16 EU, the doWork economics drain
    # mInputMax 16/t BEFORE the buffer can bank two sub-16 packets, and every
    # wire segment costs >= 1 loss — a fed-through-a-wire ULV row (eUt 16)
    # starves forever. The direct front-to-back hop is the working closure;
    # the ULV-tier wire carriage of V[0] packets is proven live in arm B
    # (the volt-8 copper wire hop).
    Step(f"setblock {MILL_A_P} {MILL_B}", expect="Changed the block", sleep=1.0),
    Step(f"data merge block {MILL_A_P} {{facing:5}}", expect="Modified block data"),
    # the ULV window pin — ULV_TIER_INPUTS {4,8,16} read straight off the BE report
    Step(f"gt6machine wiremill check {MILL_A_P}", expect="minIn=4 recIn=8 maxIn=16"),
    Step(CU_MERGE["1.20.1"], expect="Modified block data", node_cmds=CU_MERGE),
    Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled 160 L"),
    # the grid-fed completion: whole-capacitor 16 EU packets land straight on
    # the SBIT_B face — the recipe's 16/t drain is met 1:1, the shared
    # RM.Wiremill row (copper 1357 K < the 1375 K gate) completes un-overclocked
    Step(f"gt6machine wiremill check {MILL_A_P}", expect=CU_CHECK, node_expects=CU_CHECK, poll=45.0),
    Step(f"gt6engine stat {F(ENGINE)}", expect="rate=16 RU/t (DC constant-sign)"),
    Step(f"gt6engine stat {F(AXLE2)}", expect="break pending=false"),

    # ---------------------------------------------------------------- arm B
    phase("B: negative — 8 EU packets into the LV electricloom are swallowed (white burn), volt 32 on the same rig completes"),
    Step(f"gt6machine electricloom place {LOOM_B_P}", expect="GT6 electricloom placed"),
    Step(f"gt6machine electricloom input 4 {LOOM_B_P}", expect=LOOM_INPUT, node_expects=LOOM_INPUT),
    # the LV window pin — the T1 EU window {16,32,64} read straight off the BE report
    Step(f"gt6machine electricloom check {LOOM_B_P}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine electricloom check {LOOM_B_P}", expect="energy=0"),
    Step(f"gt6energy place {SRC_B_P}", expect="GT6 energy source placed"),
    Step(f"gt6wire place copper 1 {F(WIRE_B)}", expect="GT6 wire placed"),
    Step(f"gt6energy volt {SRC_B_P} 8", expect="voltage 8"),
    Step(f"gt6energy mode {SRC_B_P} on", expect="emitting true", sleep=5.0),
    # after the 5 s swallow window: buffer never grew (the :50 arm ate every packet)
    Step(f"gt6machine electricloom check {LOOM_B_P}", expect="energy=0"),
    Step(f"data get block {LOOM_B_P} inventory", expect=LOOM_SLOT),
    # the control leg: same rig, LV-sized packets complete the string row
    Step(f"gt6energy volt {SRC_B_P} 32", expect="voltage 32"),
    Step(f"gt6machine electricloom check {LOOM_B_P}", expect=LOOM_DONE, node_expects=LOOM_DONE, poll=40.0),

    # ---------------------------------------------------------------- arm C
    phase("C: negative — iron (1811 K) refused by the 1375 K melting gate before any consume; copper (1357 K) passes"),
    Step(f"setblock {MILL_FE_P} {MILL_B}", expect="Changed the block"),
    Step(FE_MERGE["1.20.1"], expect="Modified block data", node_cmds=FE_MERGE),
    Step(f"gt6machine wiremill check {MILL_FE_P}", expect="minIn=4 recIn=8 maxIn=16"),
    # driven + refused: 60 driven ticks, the gate blocks the recipe BEFORE the
    # consume (TileEntityBasicMachine :655) — no recipe ever binds
    Step(f"gt6machine wiremill inject 60 8 {MILL_FE_P}", expect="progress=0/0"),
    Step(f"gt6machine wiremill check {MILL_FE_P}", expect="progress=0/0"),
    # zero item consumption: the iron stick is still sitting in slot 0
    Step(f"data get block {MILL_FE_P} inventory", expect=FE_SLOT, node_expects=FE_SLOT),
    # the control arm: copper crosses the gate and the poured row completes
    # (packets at mMinEnergy 16 keep the machine out of the doInactive reset)
    Step(f"setblock {MILL_CU_P} {MILL_B}", expect="Changed the block"),
    Step(CU_MERGE2["1.20.1"], expect="Modified block data", node_cmds=CU_MERGE2),
    Step(f"gt6machine wiremill inject 300 16 {MILL_CU_P}", expect=CU_OUT, node_expects=CU_OUT),

    # ---------------------------------------------------------------- arm D
    phase("D: transformer through-trickle — converter 8 EU/t -> gt.reversed step-up -> LV electricloom: THROUGH but rate-pinned"),
    Step(f"gt6fesource place {FESRC_P}", expect="stored 100000 FE"),
    Step(f"gt6feconverter place {F(FECONV)}", expect="voltage 8 EU x 1 A"),
    Step(f"setblock {TRANS_P} {TRANSFORMER}", expect="Changed the block", sleep=1.0),
    # the default north facing puts FACING_ROTATIONS[north][west]=4=right on the
    # transformer's east front — both loom side faces are energy faces, no merge
    Step(f"gt6machine electricloom place {LOOM_D_P}", expect="GT6 electricloom placed"),
    # the pre-flip mode pin: upstream default = step-down
    Step(f"data get block {TRANS_P}", expect="reversed: 0b"),
    Step(f"gt6machine electricloom input 4 {LOOM_D_P}", expect=LOOM_INPUT, node_expects=LOOM_INPUT),
    # the headroom budget: 24576 FE = 768 packet-quantised pulls (zero tail),
    # the raw 6144 EU covers the 4096-progress row plus the measured transit loss
    # (the oven-era budget 12288 FE covered 512 EU of smelts)
    Step(f"gt6fesource set {FESRC_P} 24576", expect="stored 24576 FE"),
    Step(f"gt6machine electricloom check {LOOM_D_P}", expect="energy=0"),
    # THE W3 DRIVER FACE: the NBT mode flip (clears the capacitor, Base11 :81)
    Step(f"data merge block {TRANS_P} {{gt.reversed:1b}}", expect="Modified block data"),
    Step(f"data get block {TRANS_P}", expect="reversed: 1b"),
    # the wall acceptance: the stepped-up packets DO complete the LV job — the
    # 8 EU/t trickle walks 4096 progress in ~26 s (a full-rate bypass: ~3 s)
    Step(f"gt6machine electricloom check {LOOM_D_P}", expect=LOOM_DONE, node_expects=LOOM_DONE, poll=40.0),
    # conservation: every FE crossed (packet-quantised pulls drain to exact zero)
    Step(f"gt6fesource stat {FESRC_P}", expect="stored 0 FE", poll=60.0),
]

CHAIN = Chain(
    name="p28-ulv-chain",
    slug="p28ulv",
    sites=gt6world.declare_sites(
        ENGINE, AXLE1, AXLE2, DYNAMO, MILL_A,
        LOOM_B, WIRE_B, SRC_B,
        MILL_FE, MILL_CU,
        FESRC, FECONV, TRANS, LOOM_D),
    preferred_ports=(26170, 26180),      # this card's pinned rcon/query pair (fresh 2617x segment)
    passes=2,
    steps=steps,
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
