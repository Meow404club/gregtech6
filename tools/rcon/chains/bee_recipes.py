#!/usr/bin/env python3
"""bee_recipes — the Bumbliary pair crafting + hive-carry live chain (task
bumbliary-recipes, acceptance ④: craft 冒烟臂; acceptance ②: 挖取野生蜂窝得箱).

Chain semantics:

  A THE RECIPE LOAD: /reload re-parses the datapack — the two new rows
    gt6:bumbliary / gt6:bumbliary_advanced parse through the REAL RecipeManager
    (the circuits_craft arm shape; a missing/unparseable row is a loud parse
    error and the ACT canDo=false below).

  B THE CRAFT FACE: the ACT (the P24 rig) crafts the :2222 Bumbliary row —
    PPP/PBP/TdT over the wood-treated plates + the R2 hive BlockItem (the 'B'
    key, aRegistry.getItem(32755)) + the iron screws + the screwdriver tool.
    canDo=true IS the load proof; craft once emits gt6:bumbliary and the
    screwdriver survives as the worn tool (the crafting-remaining face).

  C THE LOOT WALK (the bees_c arm, now carrying the ③ box): the merged
    fillLoot shape (comb x5 / princess x1 / drones x3, the gt.bumble gene NBT)
    mined AS THE SCOOP drops the BOX + the contents — the upstream base getDrops
    (TileEntityBase04MultiTileEntities.java:166-171) always leads the drop list
    with the block item. The wrong-tool negative keeps drops=[] (the
    SHEARS_HARVEST gate blocks playerDestroy entirely — no box either).

passes=2 is the idempotency proof (the [0, 0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/bee_recipes.py
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

# the fresh band: x-disjoint from the bees_c band 479..520 with margin — the ACT
# column at z=144 and the loot-walk cell at z=128 (setblock-driven rigs, no forceload:
# the /place feature arm belongs to the bees_c chain, this card only mines).
HIVE = gt6world.Site(536, 64, 128)                       # the loot-walk cell
ACT = gt6world.Site(532, 64, 144)                        # the craft rig


def merge_loot(pos):
    """The fillLoot output shape into gt.inv (the species-0 honey family, work 4500 →
    comb x5, offspring 3 → drones x3; the Count key fork: 1.20.1 `Count:Nb`+`tag` vs
    21.1 `count:N`+`components.minecraft:custom_data`)."""
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


steps = [
    phase("A: the recipe load — the datapack re-parse over the two new rows"),
    Step("reload", expect="Reloading"),

    phase("B: the craft face — the ACT crafts the :2222 Bumbliary row (PPP/PBP/TdT)"),
    Step(f"gt6act place {F(ACT)}", expect="GT6 advanced_crafting_table placed"),
    # the :2222 grid row-major on the SLOTS_CRAFTING band 21..29: P/P/P / P/B/P / T/d/T
    # ('B' = the hive, the CENTER cell slot 25 — the pattern row "PBP", the json pin)
    Step(f"gt6act fill 21 gt6:plate_wood_treated 1 {F(ACT)}", expect="GT6 ACT fill slot 21: 1x"),
    Step(f"gt6act fill 22 gt6:plate_wood_treated 1 {F(ACT)}", expect="GT6 ACT fill slot 22: 1x"),
    Step(f"gt6act fill 23 gt6:plate_wood_treated 1 {F(ACT)}", expect="GT6 ACT fill slot 23: 1x"),
    Step(f"gt6act fill 24 gt6:plate_wood_treated 1 {F(ACT)}", expect="GT6 ACT fill slot 24: 1x"),
    Step(f"gt6act fill 25 gt6:bumble_hive 1 {F(ACT)}", expect="GT6 ACT fill slot 25: 1x"),
    Step(f"gt6act fill 26 gt6:plate_wood_treated 1 {F(ACT)}", expect="GT6 ACT fill slot 26: 1x"),
    Step(f"gt6act fill 27 gt6:screw_iron 1 {F(ACT)}", expect="GT6 ACT fill slot 27: 1x"),
    Step(f"gt6act fill 28 gt6:screwdriver 1 {F(ACT)}", expect="GT6 ACT fill slot 28: 1x"),
    Step(f"gt6act fill 29 gt6:screw_iron 1 {F(ACT)}", expect="GT6 ACT fill slot 29: 1x"),
    Step(f"gt6act compute {F(ACT)}", expect="canDo=true"),
    Step(f"gt6act craft once {F(ACT)}", expect="crafted=true, hold=[1x"),
    # the consumed grid leaves the worn screwdriver ALONE at slot 28 (the
    # crafting-remaining face) — no next-slot boundary exists (the circuits row had
    # hammer at 27 + wrench at 28, its boundary-spanning pin shape does not apply);
    # the 21.1 leg namespaces the item renders (the node_expects drift ruling)
    Step(f"gt6act stat {F(ACT)}", expect="28=1x screwdriver",
         node_expects={"1.21.1": "28=1x gt6:screwdriver"}),

    phase("C: the loot walk — the scoop harvest drops the BOX + the contents"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_loot(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_loot(F(HIVE))),
    Step(f"data get block {F(HIVE)}", expect="gt.bumble"),
    Step(f"gt6scene6 mine {F(HIVE)} scoop",
         expect="drops=[gt6:bumble_drone x3, gt6:bumble_hive x1, gt6:bumble_princess x1, gt6:comb_honey x5]"),

    phase("C-: the wrong tool breaks the hive with NO drops (box included — the SHEARS_HARVEST gate)"),
    Step(f"setblock {F(HIVE)} gt6:bumble_hive", expect="Changed the block"),
    Step(merge_loot(F(HIVE))["1.20.1"], expect="Modified block data", node_cmds=merge_loot(F(HIVE))),
    Step(f"gt6scene6 mine {F(HIVE)} plunger", expect="drops=[]"),

    phase("T: teardown — the bands back to air (the pass-open bbox is the backstop)"),
    Step("fill 528 62 128 544 66 128 air"),
    Step("fill 528 62 142 536 66 146 air"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="bumbliary-recipes bee_recipes",
    slug="beerecipes",
    sites=gt6world.declare_sites(HIVE, ACT),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
