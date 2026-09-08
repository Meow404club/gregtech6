#!/usr/bin/env python3
"""p25-food-can-row0 — the Canner food-can row0 live acceptance chain (the
p24_canner_refill shape: place → data-merge the two-item input → inject → the output
slot verdict).

Chain semantics (task p25-food-can-row0 ACCEPTANCE ② — "place canner→input
rotten_flesh+food_can_empty→run→check output"):

  A the rotten_flesh row (T1): input slots via the inventory data merge (slot 0 =
    1x minecraft:rotten_flesh, slot 1 = 1x gt6:food_can_empty — the addRecipe2 two-item
    input face, RM.java:744) → inject 40 ticks x 16 EU (EUt 16, duration 16 CONSTANT —
    256 EU total, no :773 overclock on T1) → the verdict pins 1x food_can_rotten_small
    (foodValue 4 → switch(4/2=2) → aCans[1], the tier-1 SMALL rotten can).

  B the spider_eye row (T2): the same rig → 1x food_can_rotten_tiny (foodValue 2 →
    switch(1) → aCans[0], the tier-0 TINY can).

  C the cookie row (T3): slot 0 = 6x minecraft:cookie (the food input carries its
    registered COUNT, MultiItemFood.java:600 ST.make(Items.cookie, 6, W)) + slot 1 =
    1x gt6:food_can_empty → 1x food_can_cookies_huge (foodValue 12 → switch(6) hits NO
    case → the DEFAULT branch RM.java:753: count = 12/12 = 1, tier 5 = the huge-can
    tier — THE Cookie Tin output).

  D teardown: the explicit fill-air over the band. No global state is touched (the
    grid inject rig needs no fakesource).

The inventory merges ride the loader-versioned key shapes (the p24_canner_refill
ruling): 1.20.1 `Count:1b` NBT vs 1.21.1 `count:1` component rendering. The output
expects are PER-LEG via Step.node_expects (the p16_pattern_checker precedent): the
1.20.1 ItemStack.toString renders the bare registry path ("food_can_rotten_small")
while the 21.1 rendering is NAMESPACED ("gt6:food_can_rotten_small") — the server
behaviour itself is identical on both legs.

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p25_food_can.py
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

# the three canner sites on the 124 band (clear of the p25 hammer-wrench chest at
# x 416 and the p24 bands 386-412) — T1/T2/T3 of the canner ladder
SITE_A = gt6world.Site(420, 64, 124, dx=0, dy=1, dz=1)  # canner     T1 — rotten_flesh
SITE_B = gt6world.Site(422, 64, 124, dx=0, dy=1, dz=1)  # canner_t2  T2 — spider_eye
SITE_C = gt6world.Site(424, 64, 124, dx=0, dy=1, dz=1)  # canner_t3  T3 — cookie x6
A = F(SITE_A)
B = F(SITE_B)
C = F(SITE_C)

# the two-item input payloads per node (the p24_canner_refill inventory-merge ruling):
# slot 0 = the food, slot 1 = the empty can. 1.20.1 carries Count:1b NBT; 1.21.1
# renders `count:1` (the p12fic key-shape ruling). The cookie row merges SIX cookies.
ROTTEN_FLESH_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:rotten_flesh",Count:1b}},{{Slot:1b,id:"gt6:food_can_empty",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:rotten_flesh",count:1}},{{Slot:1b,id:"gt6:food_can_empty",count:1}}]}}}}',
}
SPIDER_EYE_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:spider_eye",Count:1b}},{{Slot:1b,id:"gt6:food_can_empty",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:spider_eye",count:1}},{{Slot:1b,id:"gt6:food_can_empty",count:1}}]}}}}',
}
COOKIE_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:cookie",Count:6b}},{{Slot:1b,id:"gt6:food_can_empty",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"minecraft:cookie",count:6}},{{Slot:1b,id:"gt6:food_can_empty",count:1}}]}}}}',
}

steps = []

# ------------------------------------------------- A: the rotten_flesh row (T1)
steps += [
    phase("A: the rotten_flesh row (T1) — foodValue 4 → switch(2) → the tier-1 SMALL rotten can"),
    Step(f"gt6machine canner place {A}", expect="GT6 canner placed"),
    Step(ROTTEN_FLESH_MERGE["1.20.1"].format(p=A), expect="Modified block data",
         node_cmds={"1.21.1": ROTTEN_FLESH_MERGE["1.21.1"].format(p=A)}),
    Step(f"gt6machine canner inject 40 64 {A}",
         expect="outputs=[1x food_can_rotten_small; ]",
         node_expects={"1.21.1": "outputs=[1x gt6:food_can_rotten_small; ]"}),
]

# ------------------------------------------------- B: the spider_eye row (T2)
steps += [
    phase("B: the spider_eye row (T2) — foodValue 2 → switch(1) → the tier-0 TINY rotten can"),
    Step(f"gt6machine canner_t2 place {B}", expect="GT6 canner_t2 placed"),
    Step(SPIDER_EYE_MERGE["1.20.1"].format(p=B), expect="Modified block data",
         node_cmds={"1.21.1": SPIDER_EYE_MERGE["1.21.1"].format(p=B)}),
    Step(f"gt6machine canner_t2 inject 40 256 {B}",
         expect="outputs=[1x food_can_rotten_tiny; ]",
         node_expects={"1.21.1": "outputs=[1x gt6:food_can_rotten_tiny; ]"}),
]

# ------------------------------------------------- C: the cookie row (T3) — the Cookie Tin
steps += [
    phase("C: the cookie x6 row (T3) — foodValue 12 → DEFAULT branch → 1x the Cookie Tin"),
    Step(f"gt6machine canner_t3 place {C}", expect="GT6 canner_t3 placed"),
    Step(COOKIE_MERGE["1.20.1"].format(p=C), expect="Modified block data",
         node_cmds={"1.21.1": COOKIE_MERGE["1.21.1"].format(p=C)}),
    Step(f"gt6machine canner_t3 inject 40 1024 {C}",
         expect="outputs=[1x food_can_cookies_huge; ]",
         node_expects={"1.21.1": "outputs=[1x gt6:food_can_cookies_huge; ]"}),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 419 62 123 425 67 125 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p25-food-can-row0",
    slug="p25foodcan",
    sites=gt6world.declare_sites(SITE_A, SITE_B, SITE_C),
    preferred_ports=(26110, 26120),      # this card's pinned rcon/query pair (the 2610x segment, after p24act 26109/26119)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
