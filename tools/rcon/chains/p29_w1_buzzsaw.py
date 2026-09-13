#!/usr/bin/env python3
"""p29_w1_buzzsaw — the Buzzsaw family (RU, RM.CUTTER) live acceptance chain, the FULL
ladder T1-T4 (task p29-w1-kinetic-process-ladder ACCEPTANCE ①: 无冷却流体拒收/有则跑通).

Rig split (the card RCON ruling):
  T1 — the LIVE kinetic fixture, diesel engine -> wood-small axle -> machine (the
    engine emitEnergyToNetwork walks the axle's forwarding adjacency; the machine
    report pins the 16 RU packet, the machine's SBIT_B energy face rotated east by
    the NBT facing merge {facing:5} — the p28_ulv_chain arm-A form with the axle hop
    the card ruling names). NO wire in the hop (the arm-A direct-closure finding).
  T2-T4 — the gt6machine inject rig (the p26_w1_wiremill form; doInject ignores the
    side, RU is NOT an ALL_ALTERNATING member so a plain positive train delivers).

The cutter row (data/gt6/recipe_maps/cutter.json): 1 oak_planks + 10 L water -> 4x
stick, eUt 16 duration 16 — the MIN-FLUID 1 gate of the CUTTER map (fluid 1/0/1,
minimal fluids 1) makes the coolant leg MANDATORY: checkRecipe :709 refuses the
dry machine before any consume (the :708/:709/:710 gate trio).

  T1 NEGATIVE: plank in, tank empty, inject 20x64 -> progress=0/0, the plank intact.
  T1 fill top 1000 (the :143 SBIT_U|SBIT_D coolant face rotated east keeps top open)
  -> the diesel completes the row: out[0]=4x stick, water 990.
  T2: packet 256 (window 64/128/256), one x4 overclock -> 64 progress, one inject tick.
  T3: packet 1024, two x4 -> 16 progress.  T4: packet 4096, three x4 -> 4 progress.

The input feeds ride the /data merge (the p28_ulv_chain arm-C form — the feed
supplier of the /gt6machine input command belongs to the literal's family, and the
machine under test is a buzzsaw driven through the wiremill literal): the inventory
Size is the family's exact 1+3 slot shape.

passes=2 is the idempotency proof. No global state is touched — one session boot
serves the p29_w1_process cluster.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_buzzsaw.py
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

# the fresh z=252 band — the buzzsaw column x384..393 (family x-disjoint per the
# p29_w1_process cluster plan; T1 carries the diesel rig, T2-T4 the inject rig)
ENGINE = gt6world.Site(384, 64, 252)
AXLE1 = gt6world.Site(385, 64, 252)
MILL_T1 = gt6world.Site(386, 64, 252)
MILL_T2 = gt6world.Site(388, 64, 252)
MILL_T3 = gt6world.Site(390, 64, 252)
MILL_T4 = gt6world.Site(392, 64, 252)
T1, T2, T3, T4 = F(MILL_T1), F(MILL_T2), F(MILL_T3), F(MILL_T4)

DIESEL = "gt6:diesel_engine_bronze"
AXLE = "gt6:axle_wood_treated_small"


def feed_merge(pos, size, count):
    """The inventory data-merge feed (the Count key fork: 1.20.1 `Count:Nb` vs 21.1 `count:N`)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:oak_planks",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:{size},Items:[{{Slot:0b,id:"minecraft:oak_planks",count:{count}}}]}}}}',
    }


STICK_OUT = {"1.20.1": "out[0]=4x stick", "1.21.1": "out[0]=4x minecraft:stick"}
STICK_INJ = {"1.20.1": "outputs=[4x stick; ]", "1.21.1": "outputs=[4x minecraft:stick; ]"}

steps = [
    phase("A: T1 — the diesel -> axle live rig; the coolant-mandatory gate NEGATIVE then the wet completion"),
    Step(f"gt6machine wiremill check {T1}", expect="progress=0/0"),  # the fresh BE idle
    Step(f'setblock {T1} gt6:buzzsaw', expect="Changed the block", sleep=1.0),
    Step(f"data merge block {T1} {{facing:5}}", expect="Modified block data"),  # back = west toward the axle
    Step(feed_merge(T1, 4, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T1, 4, 1)),
    # the NEGATIVE: dry machine, coolant absent — the :709 MIN-FLUID gate refuses before any consume
    Step(f"gt6machine wiremill inject 20 64 {T1}", expect="progress=0/0"),
    Step(f"gt6machine wiremill check {T1}", expect="progress=0/0"),
    # the wet control: the coolant face fills from the world top (SBIT_U in the rotated :143 mask)
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T1}", expect="filled 1000/1000"),
    Step(f"setblock {F(ENGINE)} {DIESEL}[facing=east]", expect="Changed the block"),
    Step(f"setblock {F(AXLE1)} {AXLE}[axis=x]", expect="Changed the block"),
    Step(f"gt6engine fuel {F(ENGINE)} gt6:diesel 2000", expect="filled"),
    Step(f"gt6machine wiremill check {T1}", expect=STICK_OUT["1.20.1"], node_expects=STICK_OUT, poll=45.0),
    Step(f"gt6machine wiremill fluid stat {T1}", expect="in[0]=990 L of minecraft:water"),
    Step(f"gt6engine stat {F(ENGINE)}", expect="rate=16 RU/t (DC constant-sign)"),

    phase("B: T2-T4 — the inject rig over the 32/128/512/2048 window ramp, each with its coolant"),
    Step(f'setblock {T2} gt6:buzzsaw_t2', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    Step(feed_merge(T2, 4, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T2, 4, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T2}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 256 {T2}", expect=STICK_INJ["1.20.1"], node_expects=STICK_INJ),
    Step(f'setblock {T3} gt6:buzzsaw_t3', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T3}", expect="minIn=256 recIn=512 maxIn=1024"),
    Step(feed_merge(T3, 4, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T3, 4, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T3}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 1024 {T3}", expect=STICK_INJ["1.20.1"], node_expects=STICK_INJ),
    Step(f'setblock {T4} gt6:buzzsaw_t4', expect="Changed the block", sleep=1.0),
    Step(f"gt6machine wiremill check {T4}", expect="minIn=1024 recIn=2048 maxIn=4096"),
    Step(feed_merge(T4, 4, 1)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(T4, 4, 1)),
    Step(f"gt6machine wiremill fluid fill top minecraft:water 1000 {T4}", expect="filled 1000/1000"),
    Step(f"gt6machine wiremill inject 6 4096 {T4}", expect=STICK_INJ["1.20.1"], node_expects=STICK_INJ),

    phase("C: the row-carrier pins — menu-less carriers, parallel 1 (no NBT_PARALLEL keys)"),
    Step(f"gt6machine wiremill check {T1}", expect="data=-2"),
    Step(f"gt6machine wiremill check {T1}", expect="parallel=1 parallelDuration=false"),
    Step(f"gt6machine wiremill check {T4}", expect="parallel=1 parallelDuration=false"),

    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 251 394 68 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w1_process buzzsaw",
    slug="p29w1buzzsaw",
    sites=gt6world.declare_sites(ENGINE, AXLE1, MILL_T1, MILL_T2, MILL_T3, MILL_T4),
    preferred_ports=(26190, 26200),      # this card's pinned rcon/query pair (fresh 2619x segment)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
