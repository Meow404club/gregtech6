#!/usr/bin/env python3
"""p29_w1_squeezer — the Squeezer family (KU, RM.SQUEEZER) live acceptance chain, the
FULL ladder T1-T4 (task p29-w1-kinetic-process-ladder ACCEPTANCE ②: 并行 4/8/16/32
输出×N 断言). The rig is the p26_w1_sifter KU pulse-cycle form verbatim — the
`inject N size -size` pair closes the AC cycle inside ONE command (KU is an
ALL_ALTERNATING member: the output lands on the positive->non-positive TRANSITION
tick, the :815 arm — TileEntityBasicMachine.java:476).

The squeezer row (data/gt6/recipe_maps/squeezer.json): 1 apple -> 1 stick + 50 L
water, eUt 16 duration 16, NBT_PARALLEL_DURATION T — the PARALLEL_4_32 ladder
(:1324-1327), the juice leg lands in the OUTPUT tank (the :144 SBIT_D bottom face):

  T1 parallel 4: 4 apples -> ONE completion consumes all four -> 4x stick + 200 L.
  T2 parallel 8 (packet 256, one x4 overclock): 8 apples -> 8x stick + 400 L.
  T3 parallel 16 (packet 1024): 16 apples -> 16x stick + 800 L.
  T4 parallel 32 (packet 4096): 32 apples -> 32x stick + 1600 L.

The feeds ride the /data merge (the p28 arm-C form; the inventory Size = the family's
1+2 slot shape). The juice verdict reads the output tank back through `fluid stat`
(out[0]=N L of minecraft:water).

check pins: data=-2 (the menu-less carrier), parallel=P parallelDuration=true, and
the T2 TIER_INPUTS ramp {64,128,256}.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_squeezer.py
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

# the fresh z=252 band — the squeezer column x396..403 (family x-disjoint)
SITES = [gt6world.Site(396, 64, 252), gt6world.Site(398, 64, 252),
         gt6world.Site(400, 64, 252), gt6world.Site(402, 64, 252)]
T1, T2, T3, T4 = (F(s) for s in SITES)


def feed_merge(pos, count):
    """Count apples (Count key fork: 1.20.1 `Count:Nb` vs 21.1 `count:N`)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:apple",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:apple",count:{count}}}]}}}}',
    }


def stick_out(n):
    return {"1.20.1": f"outputs=[{n}x stick; ]", "1.21.1": f"outputs=[{n}x minecraft:stick; ]"}


# (tier, pos, path, apples, packet, parallel, ticks)
TIERS = [
    (1, T1, "squeezer", 4, 64, 4, 24),
    (2, T2, "squeezer_t2", 8, 256, 8, 14),
    (3, T3, "squeezer_t3", 16, 1024, 16, 14),
    (4, T4, "squeezer_t4", 32, 4096, 32, 14),
]

steps = [phase("A: the KU pulse-cycle ladder — the PARALLEL_4_32 columns live, outputs xN")]

for tier, pos, path, apples, packet, parallel, ticks in TIERS:
    steps += [
        phase(f"T{tier}: parallel {parallel}, budget 16x16x{parallel} at packet {packet} — outputs x{parallel} + {parallel*50} L"),
        Step(f"setblock {pos} gt6:{path}", expect="Changed the block", sleep=1.0),
        Step(f"gt6machine wiremill check {pos}", expect=f"parallel={parallel} parallelDuration=true"),
        Step(feed_merge(pos, apples)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(pos, apples)),
        Step(f"gt6machine wiremill inject {ticks} {packet} -{packet} {pos}",
             expect=stick_out(parallel)["1.20.1"], node_expects=stick_out(parallel)),
        Step(f"gt6machine wiremill fluid stat {pos}", expect=f"out[0]={parallel * 50} L of minecraft:water"),
        Step(f"gt6machine wiremill check {pos}", expect="data=-2"),
    ]

# the TIER_INPUTS ramp pin on T2 (the {16,32,64} x 2^tier table live)
steps += [
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 395 62 251 404 68 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w1_process squeezer",
    slug="p29w1squeezer",
    sites=gt6world.declare_sites(*SITES),
    preferred_ports=(26190, 26200),      # the cluster's pinned pair (shared session boot)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
