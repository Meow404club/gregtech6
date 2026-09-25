#!/usr/bin/env python3
"""bees_hive — the BumbleHive live acceptance chain (task bees-lv2, the
strata_lens chain shape).

Chain semantics (the task-card RCON arms):

  A the SCOOP HARVEST (acceptance ③): a /setblock hive (the MTE 32755 loot shell) with
    a data-merged comb in slot 0 (the gt.inv NBT_INV_LIST face) is mined by the
    /gt6scene6 mine channel AS THE SCOOP — the drop report names gt6:bumble_hive x1
    (the BOX, the R2 containment-contract revision: the upstream base getDrops,
    TileEntityBase04MultiTileEntities.java:166-171, leads every break with the block
    item) PLUS gt6:comb_honey x3 (the contents).
    The wrong-tool negative rides the same walk with the plunger: the SHEARS_HARVEST
    gate (canHarvestBlock = the aHive TOOL_scoop semantics, Loader_MultiTileEntities
    .java:111) drops NOTHING (playerDestroy is canHarvestBlock-gated, so neither the
    box nor the mDroppable contents ever fire for a wrong tool).
  B the PAINTABLE storage face: the merge writes gt.color/gt.painted (the born-painted
    worldgen keys, WorldgenHives.java:203) and data get reads the family colour back.
  C the EMBEDDED form live place (chunk 4,4): the stone slab y12..16 + apron makes EVERY
    chunk column pass the 5-of-6 wall check at the slab top (the WorldgenHives.java:127-140
    scan), so /place feature gt6:bumble_hives is deterministically "Placed" for any boot
    seed; the slab CHANGED against the pristine clone, and the erase-refill-replace
    recompute is BIT-IDENTICAL (the coordinate-seeded stream, the offline tiling test's
    live twin — decisions.2026-09-18-strata-lens-determinism-acceptance).
  D the HANGING surface form live place (chunk 13,4): the single-layer slab makes the
    :142-189 descending scan hang the hive under it (a free below-side, the family chain
    → the rock contact). Changed-proof against the pristine clone.

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its block/box
first; the mine discards the drops).

Run:  GT6_SESSION=off python3 tools/rcon/chains/bees_hive.py
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

HIVE = gt6world.Site(412, 64, 240)                       # arm A/B: the scoop+paint hive cell
EMBED = gt6world.Site(72, 14, 72, dx=9, dy=3, dz=9)      # arm C: the embedded slab (core+apron)
HANG = gt6world.Site(216, 64, 72, dx=9, dy=3, dz=9)      # arm D: the hanging slab
STAGING = gt6world.Site(312, 14, 72, dx=28, dy=3, dz=9)  # the three compare clones

# the embedded core box (chunk 4,4 proper): x64..79 z64..79 y12..16
ECORE = "64 12 64 79 16 79"
S1 = "296 12 64 311 16 79"    # the pristine clone of ECORE
S2 = "312 12 64 327 16 79"    # the post-place clone of ECORE
# the hanging compare box (chunk 13,4 + the hive cell below the slab): x207..224 z63..80 y62..65
HBOX = "207 62 63 224 65 80"
S3 = "328 62 64 345 65 81"    # the pristine clone of HBOX


def merge_comb(pos):
    """One comb_honey x3 into slot 0 (the Count key fork: 1.20.1 `Count:Nb` vs 21.1 `count:N`)."""
    return {
        "1.20.1": f'data merge block {pos} {{gt.inv:{{Items:[{{Slot:0b,id:"gt6:comb_honey",Count:3b}}]}}}}',
        "1.21.1": f'data merge block {pos} {{gt.inv:{{Items:[{{Slot:0b,id:"gt6:comb_honey",count:3}}]}}}}',
    }


def merge_paint(pos):
    """The born-painted storage face: gt.color + gt.painted (the CS.java:1161-1162 key names)."""
    return {
        "1.20.1": f"data merge block {pos} {{gt.painted:1b,gt.color:16711680}}",
        "1.21.1": f"data merge block {pos} {{gt.painted:1b,gt.color:16711680}}",
    }


steps = []

# --------------------------------- A/B: the scoop harvest + the paintable storage
steps += [
    phase("A: the scoop mines the loaded hive — box + comb drop (acceptance ③, the P34 R2 box-drop semantics)"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_comb(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_comb(F(HIVE))),
    Step(f"gt6scene6 mine {F(HIVE)} scoop", expect="drops=[gt6:bumble_hive x1, gt6:comb_honey x3]"),

    phase("A-: the wrong tool breaks the hive with NO drops (the TOOL_scoop gate)"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_comb(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_comb(F(HIVE))),
    Step(f"gt6scene6 mine {F(HIVE)} plunger", expect="drops=[]"),

    phase("B: the paintable storage — gt.color/gt.painted ride the BE NBT"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_paint(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_paint(F(HIVE))),
    # the dotted key defeats the data-get path parser (it nests on ".") — the whole-BE
    # dump substring is the readback face (SNBT prints "gt.color: 16711680" flat)
    Step(f"data get block {F(HIVE)}", expect="gt.color: 16711680"),
]

# --------------------------------- C: the embedded form live place + the bit-exact recompute
steps += [
    phase("C: chunk (4,4) — the embedded stone form, deterministically placed for any boot seed"),
    Step("forceload add 63 63 80 80"),
    Step("forceload add 296 64 311 81"),
    Step("forceload add 312 64 327 81"),
    # clear + the y12..16 slab (core) and the one-block apron (the 5-of-6 probes reach x63/x80/z63/z80)
    Step("fill 63 8 63 80 20 80 air"),
    Step("fill 64 12 64 79 16 79 minecraft:stone"),
    Step("fill 63 12 63 80 16 63 minecraft:stone"),
    Step("fill 63 12 80 80 16 80 minecraft:stone"),
    Step("fill 63 12 64 63 16 79 minecraft:stone"),
    Step("fill 80 12 64 80 16 79 minecraft:stone"),
    Step(f"clone {ECORE} 296 12 64"),  # staging1 = pristine slab
    Step("place feature gt6:bumble_hives 72 14 72", expect="Placed"),
    Step(f"execute unless blocks {ECORE} 296 12 64 all", expect="Test passed"),
    Step(f"clone {ECORE} 312 12 64"),  # staging2 = the hive state
    Step("fill 63 8 63 80 20 80 air"),
    Step("fill 64 12 64 79 16 79 minecraft:stone"),
    Step("fill 63 12 63 80 16 63 minecraft:stone"),
    Step("fill 63 12 80 80 16 80 minecraft:stone"),
    Step("fill 63 12 64 63 16 79 minecraft:stone"),
    Step("fill 80 12 64 80 16 79 minecraft:stone"),
    Step("place feature gt6:bumble_hives 72 14 72", expect="Placed"),
    # THE determinism face: the recomputed slab is bit-identical to the first computation
    Step(f"execute if blocks {ECORE} 312 12 64 all", expect="Test passed"),
]

# --------------------------------- D: the hanging surface form live place
steps += [
    phase("D: chunk (13,4) — the hanging surface form under the single-layer slab"),
    Step("forceload add 207 63 224 80"),
    Step("forceload add 328 64 345 81"),
    Step("fill 207 48 63 224 96 80 air"),
    Step("fill 207 64 63 224 64 80 minecraft:stone"),
    Step(f"clone {HBOX} 328 62 64"),  # staging3 = the pristine slab band
    Step("place feature gt6:bumble_hives 216 64 72", expect="Placed"),
    Step(f"execute unless blocks {HBOX} 328 62 64 all", expect="Test passed"),
]

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the bands and stagiings back to air (the pass-open bbox is the backstop)"),
    Step("fill 63 8 63 80 20 80 air"),
    Step("fill 207 48 63 224 96 80 air"),
    Step("fill 296 12 64 345 16 81 air"),
    Step("fill 296 62 64 345 65 81 air"),
    Step("fill 412 62 240 412 66 240 air", expect="filled"),
    Step("execute if block 72 14 72 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="bees_hive",
    slug="bees_hive",
    sites=gt6world.declare_sites(HIVE, EMBED, HANG, STAGING),
    preferred_ports=(26230, 26240),      # this card's pinned rcon/query pair
    response_timeout=30.0,
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
