#!/usr/bin/env python3
"""squeezer — the Squeezer family (KU, RM.SQUEEZER) live acceptance chain, the
FULL ladder T1-T4 (task kinetic-process-ladder ACCEPTANCE ②: 并行 4/8/16/32
输出×N 断言). The rig is the sifter KU pulse-cycle form verbatim — the
`inject N size -size` pair closes the AC cycle inside ONE command (KU is an
ALL_ALTERNATING member: the output lands on the positive->non-positive TRANSITION
tick, the :815 arm — TileEntityBasicMachine.java:476).

The pinned row (data/gt6/recipe_maps/squeezer.json): 1 sunflower -> 2x yellow_dye +
100 L sunfloweroil, eUt 16 duration 16, NBT_PARALLEL_DURATION T — the
PARALLEL_4_32 ladder (:1324-1327), the oil leg lands in the OUTPUT tank (the :144
SBIT_D bottom face). The ORIGINAL p29 smoke row (1 apple -> 1 stick + 50 L water)
was REPLACED by the recipe-data cards (53c36c07c3 rebuilt the map from the
Loader_Recipes_Vanilla walk; the chain kept pinning the dead row — task
process-cluster-diesel-rig-red), so the chain re-pins to the live canon row — the
:816 sunflower statement, byte-identical energy/duration to the old smoke row, so
the whole tick ladder carries over unchanged:

  T1 parallel 4: 4 sunflowers -> ONE completion consumes all four -> 8x dye + 400 L.
  T2 parallel 8 (packet 256, one x4 overclock): 8 -> 16x dye + 800 L.
  T3 parallel 16 (packet 1024): 16 -> 32x dye + 1600 L.
  T4 parallel 32 (packet 4096): 32 -> 64x dye + 3200 L.

The feeds ride the /data merge (the P28 arm-C form; the inventory Size = the family's
1+2 slot shape). The oil verdict reads the output tank back through `fluid stat`
(out[0]=N L of gt6:sunfloweroil). The dye stacks cap at exactly 64 on T4 — the
PARALLEL_4_32 ceiling lands on the vanilla stack limit, the canOutput gate holds.

check pins: data=-1 (the true idle dial since the vanilla leg retired — task basic-machines-mui-migration), parallel=P parallelDuration=true, and
the T2 TIER_INPUTS ramp {64,128,256}.

passes=2 is the idempotency proof. No global state is touched.

Run:  GT6_SESSION=off python3 tools/rcon/chains/squeezer.py
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
    """Count sunflowers (Count key fork: 1.20.1 `Count:Nb` vs 21.1 `count:N`)."""
    return {
        "1.20.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:sunflower",Count:{count}b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{inventory:{{Size:3,Items:[{{Slot:0b,id:"minecraft:sunflower",count:{count}}}]}}}}',
    }


def stick_out(n):
    """One completion xN parallel = 2N dye (the row carries 2x yellow_dye)."""
    return {"1.20.1": f"outputs=[{2 * n}x yellow_dye; ]", "1.21.1": f"outputs=[{2 * n}x minecraft:yellow_dye; ]"}


# (tier, pos, path, apples, packet, parallel, ticks)
TIERS = [
    (1, T1, "squeezer", 4, 64, 4, 24),
    # ticks 24 (was 14): the round-1 live empirics — the completions land at exactly
    # 16 positive packet-ticks (maxProgress = packet x16 at the maxIn packet:
    # 3584/4096, 14336/16384, 57344/65536 after 14 ticks) + the negative close tick;
    # the 24-tick T1 form is the proven margin (overshoot pairs are harmless).
    (2, T2, "squeezer_t2", 8, 256, 8, 24),
    (3, T3, "squeezer_t3", 16, 1024, 16, 24),
    (4, T4, "squeezer_t4", 32, 4096, 32, 24),
]

steps = [phase("A: the KU pulse-cycle ladder — the PARALLEL_4_32 columns live, outputs xN")]

for tier, pos, path, apples, packet, parallel, ticks in TIERS:
    steps += [
        phase(f"T{tier}: parallel {parallel}, budget 16x16x{parallel} at packet {packet} — outputs x{parallel} + {parallel*100} L"),
        Step(f"setblock {pos} gt6:{path}", expect="Changed the block", sleep=1.0),
        Step(f"gt6machine wiremill check {pos}", expect=f"parallel={parallel} parallelDuration=true"),
        Step(feed_merge(pos, apples)["1.20.1"], expect="Modified block data", node_cmds=feed_merge(pos, apples)),
        Step(f"gt6machine wiremill inject {ticks} {packet} -{packet} {pos}",
             expect=stick_out(parallel)["1.20.1"], node_expects=stick_out(parallel)),
        Step(f"gt6machine wiremill fluid stat {pos}", expect=f"out[0]={parallel * 100} L of gt6:sunfloweroil"),
        Step(f"gt6machine wiremill check {pos}", expect="data=-1"),
    ]

# the TIER_INPUTS ramp pin on T2 (the {16,32,64} x 2^tier table live)
steps += [
    Step(f"gt6machine wiremill check {T2}", expect="minIn=64 recIn=128 maxIn=256"),
    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 395 62 251 404 68 254 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="process squeezer",
    slug="squeezer",
    sites=gt6world.declare_sites(*SITES),
    preferred_ports=(26190, 26200),      # the cluster's pinned pair (shared session boot)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
