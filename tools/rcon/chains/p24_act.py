#!/usr/bin/env python3
"""p24-act-machine — the Advanced Crafting Table e2e (declarative framework, the
p24_canner_refill shape; C1 GUI-free, everything rides /gt6act).

Chain semantics (task p24-act-machine ACCEPTANCE — "give selector → place ACT → fill
存料带 + 塞 selector → compute 断言 pattern+产出 → craft 断言消耗+产出 → stat; 开关
翻转臂（mBlocked16/36 字段）"):

  A the give arm (the p16_chisel headless-give shape): one chest + `item replace
    block ... container.0 with gt6:integrated_circuit{Damage:5}` — the 1.20.1 Damage
    NBT form vs the 21.1 custom_data component envelope (the GT6Circuits carrier
    ruling); data get asserts the payload landed.

  B the pattern arm (config 2 = vertical 2, the stick recipe): place → fill 存料带
    (8x oak_planks into the grid destination slot 24 + 40x on belt 70) → selector 5
    REJECTED by the whitelist (the band is [2, 9], the five-arm negative) → selector
    2 accepted → compute: the ghost lands on grid cell 6 (slot 27), the recipe
    lookup finds 4x stick, canDo=true (grid=[.G.R....]: G=ghost, R=real dest).

  C the craft arm: craft once → hold=4x stick, the grid 8→6 (two plank legs); craft
    cursor → the hold fills to the 64 cap (16 crafts); stat pins belt 70 drained.

  D the toggles arm (deviation ⑤ — the fields flip while the tool-click arm rides the
    tool pool): belt16/belt36 on → stat pins blocked16/36 + accessible=[33];
    flush/filter16/filter36 on → stat pins them; all back off.

  E teardown: the explicit fill-air over the band. No global state touched.

passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p24_act.py
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

# the ACT site on the 410 band (clear of p24cannerrefill's 400 band)
CHEST = gt6world.Site(410, 64, 20, dx=1)                 # the give site
SITE = gt6world.Site(412, 64, 20, dx=0, dy=1, dz=1)      # the ACT
T = F(SITE)
CHEST_POS = "410 64 20"

# the selector give payload per node (the GT6Circuits carrier ruling): the 1.20.1
# Damage NBT key vs the 21.1 CUSTOM_DATA envelope with the same payload key inside.
SELECTOR_REPLACE = {
    "1.20.1": f"item replace block {CHEST_POS} container.0 with gt6:integrated_circuit{{Damage:5}} 1",
    "1.21.1": f"item replace block {CHEST_POS} container.0 with gt6:integrated_circuit[minecraft:custom_data={{Damage:5}}] 1",
}

steps = []

# ------------------------------------------------- A: the give arm
steps += [
    phase("A: the give arm — the selector payload as a real registered stack"),
    Step(f"setblock {CHEST_POS} minecraft:chest", expect="Changed the block"),
    Step(SELECTOR_REPLACE["1.20.1"], expect="Replaced",
         node_cmds={"1.21.1": SELECTOR_REPLACE["1.21.1"]}),
    Step(f"data get block {CHEST_POS} Items[0]", expect="Damage:5"),
]

# ------------------------------------------------- B: the pattern arm (config 2 = vertical 2)
steps += [
    phase("B: the pattern arm — place, 存料带, selector whitelist, compute"),
    Step(f"gt6act place {T}", expect="GT6 advanced_crafting_table placed"),
    Step(f"gt6act fill 24 minecraft:oak_planks 8 {T}", expect="GT6 ACT fill slot 24: 8x"),
    Step(f"gt6act fill 70 minecraft:oak_planks 40 {T}", expect="GT6 ACT fill slot 70: 40x"),
    # the whitelist negatives: a NON-selector item refuses slot 30 (the fill face), an
    # out-of-band config refuses the selector face (the band is [2, 9])
    Step(f"gt6act fill 30 minecraft:oak_planks 1 {T}",
         expect="Slot 30 REJECTED minecraft:oak_planks"),
    Step(f"gt6act selector 1 {T}", expect="Slot 30 REJECTED selector config 1 (the whitelist band is [2, 9])"),
    Step(f"gt6act selector 2 {T}", expect="GT6 ACT selector: config 2 into slot 30"),
    # the compute verdict: grid[3]=R (the swept real destination), grid[6]=G (the config-2
    # ghost on slot 27) — item-name-free, identical on both legs; the second compute run
    # pins the recipe-found + gate face (substring expects avoid the per-leg Item.toString
    # namespace drift, the p16_distillery "integrated_circuit" precedent)
    Step(f"gt6act compute {T}", expect="grid=[...R..G..]"),
    Step(f"gt6act compute {T}", expect="canDo=true"),
]

# ------------------------------------------------- C: the craft arm
steps += [
    phase("C: the craft arm — once + cursor, the consumption priority over the belts"),
    Step(f"gt6act craft once {T}", expect="crafted=true, hold=[4x"),
    Step(f"gt6act compute {T}", expect="grid=[...R..G..]"),
    Step(f"gt6act craft cursor {T}", expect="crafts=16, hold=[64x"),
    # 17 crafts x 2 legs = 34 plank legs: slot 24 pays 7 (leave-one), belt 70 pays 27 (40→13,
    # the :499 walk head) — the exact count pins the consumption order
    Step(f"gt6act stat {T}", expect="70=13x"),
]

# ------------------------------------------------- D: the toggles arm
steps += [
    phase("D: the toggles arm — the mBlocked16/36/flush/filter fields (deviation ⑤)"),
    Step(f"gt6act mode belt16 on {T}", expect="GT6 ACT mode belt16=true"),
    Step(f"gt6act mode belt36 on {T}", expect="GT6 ACT mode belt36=true"),
    Step(f"gt6act stat {T}", expect="blocked16=true blocked36=true"),
    Step(f"gt6act stat {T}", expect="accessible=[33,]"),
    Step(f"gt6act mode flush on {T}", expect="GT6 ACT mode flush=true"),
    Step(f"gt6act mode filter16 on {T}", expect="GT6 ACT mode filter16=true"),
    Step(f"gt6act mode filter36 on {T}", expect="GT6 ACT mode filter36=true"),
    Step(f"gt6act stat {T}", expect="filter16=true filter36=true flush=true"),
    Step(f"gt6act mode belt16 off {T}", expect="GT6 ACT mode belt16=false"),
    Step(f"gt6act mode belt36 off {T}", expect="GT6 ACT mode belt36=false"),
    Step(f"gt6act mode flush off {T}", expect="GT6 ACT mode flush=false"),
    Step(f"gt6act mode filter16 off {T}", expect="GT6 ACT mode filter16=false"),
    Step(f"gt6act mode filter36 off {T}", expect="GT6 ACT mode filter36=false"),
]

# ------------------------------------------------- E: teardown
steps += [
    phase("E: teardown — the explicit band restore"),
    Step(f"setblock {CHEST_POS} air", expect="Changed the block"),
    Step("fill 409 62 19 413 66 21 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p24-act",
    slug="p24act",
    sites=gt6world.declare_sites(CHEST, SITE),
    preferred_ports=(26109, 26119),      # this card's pinned rcon/query pair (the 2610x segment, after p24cannerrefill 26108/26118)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
