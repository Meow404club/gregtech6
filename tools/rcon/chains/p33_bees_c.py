#!/usr/bin/env python3
"""p33_bees_c — the hive LOOT live chain (task p33-bees-lv3-c-hive-loot, acceptance ②:
worldgen 放 hive → 打破 → princess/drone/comb 掉落断言).

Chain semantics:

  A THE WORLDGEN PLACE (the p32_bees_hive arm-C form, now covering the loot rolls): a
    chunk (30,6) stone slab y12..16 + apron makes EVERY column pass the 5-of-6 wall
    check, so /place feature gt6:bumble_hives is deterministically "Placed" for any
    boot seed; the fill-count probe (`fill ... air replace gt6:bumble_hive`) counts
    EXACTLY ONE hive into the slab (the worldgen walk really wrote it), and the
    erase-replace-recompute `execute if blocks` is BIT-IDENTICAL against the placed
    clone — the coordinate-seeded stream now includes the gene-roll consumption
    (GT6HiveFeature.placeHive → wildGenes → fillLoot).

  B THE LOOT WALK (the drop table the scoop harvest): a /setblock hive data-merged
    with the fillLoot output shape (slot 0 the family comb x5, slot 1 the princess
    x1, slot 2 the drones x3, the royals carrying the gt.bumble gene compound — the
    same slots/codes/genes the worldgen fill writes) is mined by the /gt6scene6 mine
    channel AS THE SCOOP — the drop report names all three stacks in the TreeSet
    order (drone, princess, comb) and the whole-BE dump shows the gt.bumble gene
    NBT riding the inventory before the break. The wrong-tool negative rides the
    same walk with the plunger: drops=[] (the SHEARS_HARVEST gate).

  Note on the locate face: the feature's column pick is seed-agnostic (the sweep
  boots random-seed flat worlds), so the placed hive's exact cell is unknown to a
  declarative chain — the A arm proves the worldgen fill happened (count + the
  bit-identical recompute over the gene-roll stream) and the B arm proves the drop
  walk over the IDENTICAL shape the fill writes (the offline GT6HiveLootTest
  reconciliation runs the real fillLoot on the FML-booted leg).

Two framework passes re-lay the whole band — the [0, 0] idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p33_bees_c.py
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

EMBED = gt6world.Site(488, 14, 104, dx=8, dy=6, dz=8)    # arm A: the embedded slab (core+apron)
STAGING = gt6world.Site(512, 14, 104, dx=8, dy=6, dz=8)  # arm A: the placed-state clone
HIVE = gt6world.Site(488, 64, 128)                       # arm B: the loot-walk cell

# the embedded core box (chunk 30,6 proper): x480..495 z96..111 y8..20
ECORE = "480 8 96 495 20 111"
S2 = "504 8 96"               # the placed-state clone destination (same 16x13x16 shape)


def merge_loot(pos):
    """The fillLoot output shape into gt.inv (the species-0 honey family, work 4500 →
    comb x5, offspring 3 → drones x3; the Count key fork: 1.20.1 `Count:Nb`+`tag` vs
    21.1 `count:N`+`components.minecraft:custom_data`; the royals carry the
    gt.bumble.meta species code + the gt.bumble gene compound)."""
    return {
        "1.20.1": (f'data merge block {pos} {{gt.inv:{{Items:['
                   '{Slot:0b,id:"gt6:comb_honey",Count:5b},'
                   '{Slot:1b,id:"gt6:bumble_princess",Count:1b,tag:{"gt.bumble.meta":0,"gt.bumble":{work:4500L,life:12000L}}},'
                   '{Slot:2b,id:"gt6:bumble_drone",Count:3b,tag:{"gt.bumble.meta":0,"gt.bumble":{work:4500L,life:12000L}}}]}}'),
        "1.21.1": (f'data merge block {pos} {{gt.inv:{{Items:['
                   '{Slot:0b,id:"gt6:comb_honey",count:5},'
                   '{Slot:1b,id:"gt6:bumble_princess",count:1,components:{"minecraft:custom_data":{"gt.bumble.meta":0,"gt.bumble":{work:4500L,life:12000L}}}},'
                   '{Slot:2b,id:"gt6:bumble_drone",count:3,components:{"minecraft:custom_data":{"gt.bumble.meta":0,"gt.bumble":{work:4500L,life:12000L}}}}]}}'),
    }


steps = []

# --------------------------------- A: the worldgen place — one hive, bit-identical recompute
steps += [
    phase("A: chunk (30,6) — the embedded form places ONE filled hive for any boot seed"),
    Step("forceload add 479 95 520 130"),
    # clear + the y12..16 slab (core) and the one-block apron (the 5-of-6 probes reach the ring)
    Step("fill 479 8 95 496 20 112 air"),
    Step("fill 480 12 96 495 16 111 minecraft:stone"),
    Step("fill 479 12 95 496 16 95 minecraft:stone"),
    Step("fill 479 12 112 496 16 112 minecraft:stone"),
    Step("fill 479 12 96 479 16 111 minecraft:stone"),
    Step("fill 496 12 96 496 16 111 minecraft:stone"),
    Step("place feature gt6:bumble_hives 488 14 104", expect="Placed"),
    Step(f"clone {ECORE} {S2}"),                       # staging = the placed (filled-hive) state
    Step(f"fill {ECORE} air replace gt6:bumble_hive", node_expects={
        "1.20.1": "1 block(s)",                        # "Successfully filled 1 block(s)"
        "1.21.1": "1 blocks",                          # the 21.1 fill.success rendering
    }),
    Step("place feature gt6:bumble_hives 488 14 104", expect="Placed"),
    # THE determinism face: the recomputed slab is bit-identical to the first
    # computation — the walk now includes the gene rolls feeding fillLoot
    Step(f"execute if blocks {ECORE} {S2} all", expect="Test passed"),
]

# --------------------------------- B: the loot walk — the scoop harvest drops the three stacks
steps += [
    phase("B: the loot shell — the merged fillLoot shape dumps its genes and drops all three"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_loot(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_loot(F(HIVE))),
    # the dotted-key lesson: the whole-BE dump substring is the readback face — the
    # gene compound rides the inventory BEFORE the break (the NBT-attach live face)
    Step(f"data get block {F(HIVE)}", expect="gt.bumble"),
    Step(f"gt6scene6 mine {F(HIVE)} scoop",
         expect="drops=[gt6:bumble_drone x3, gt6:bumble_princess x1, gt6:comb_honey x5]"),

    phase("B-: the wrong tool breaks the hive with NO drops (the TOOL_scoop gate)"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_loot(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_loot(F(HIVE))),
    Step(f"gt6scene6 mine {F(HIVE)} plunger", expect="drops=[]"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the bands back to air (the pass-open bbox is the backstop)"),
    Step("fill 479 8 95 496 20 112 air"),
    Step("fill 504 8 96 519 20 111 air"),
    Step("fill 480 62 128 496 66 128 air"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p33-bees-lv3-c-hive-loot p33_bees_c",
    slug="p33beesc",
    sites=gt6world.declare_sites(EMBED, STAGING, HIVE),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
