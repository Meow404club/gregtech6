#!/usr/bin/env python3
"""p32_placeables — the placeables live acceptance chain (task p32-placeables, the
p32_bees_hive chain shape).

Chain semantics (the task-card RCON arms):

  A the LANTERN (acceptance ①): a /setblock Greg o'Lantern reports
    lightEmission=15 + blockLightAt=15 (the 光级 assertion, the upstream getLightValue
    :41 lightLevel fold) through /gt6placeables stat, and /gt6scene6 mine drops the
    block itself (the canDrop F :51 drop-self loot face).
  B the SANDWICH (acceptance ②): the placed default sandwich reads size=10 /
    comparator=9 (the bites comparator bind4(mSize-1), MultiTileEntitySandwich :194),
    one /gt6placeables eat bites to size=9 / comparator=8 with the persisted
    gt6.size riding the BE NBT, and the mine drops the sandwich item.
  C the SIX PLACEMENT FACES (acceptance ③): /gt6placeables place drives the unified
    GT6PlaceablePlacement dispatch (the exact event-path code, the sneak fake player)
    over the six faces — ingot (whole stack x12 consumed, handLeft=0), plate,
    plateGem, scrap, rock (one item, handLeft tracks the surplus) and stick — plus the
    two vanilla aliases (minecraft:stick, minecraft:flint). Each pile then mines back
    its stored stack (the playerDestroy loot shell).

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its
pedestal/block first; the mine discards the drops).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p32_placeables.py
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

LANTERN = gt6world.Site(470, 64, 350)                    # arm A: the lantern cell
SANDWICH = gt6world.Site(470, 64, 354)                   # arm B: the sandwich cell
PILES = gt6world.Site(474, 63, 348, dx=7, dy=3, dz=4)    # arm C: the six pedestal columns
BAND = (468, 60, 346, 484, 70, 364)                      # the pass-open bbox backstop


def pedestal(x, z):
    """One stone pedestal at y63 — the clicked block; the pile lands on its top face."""
    return Step(f"setblock {x} 63 {z} minecraft:smooth_stone", expect="Changed the block")


def place_pile(x, z, item, count, expect_block, expect_stack, expect_left):
    """The unified-dispatch arm + the loot-shell mineback (the acceptance ③ pair)."""
    count_cmd = f" {count}" if count else ""
    return [
        pedestal(x, z),
        Step(f"gt6placeables place {x} 63 {z} up {item}{count_cmd}",
             expect=f"result=SUCCESS, target={expect_block}, stack={expect_stack}, handLeft={expect_left}"),
        Step(f"gt6scene6 mine {x} 64 {z} plunger", expect=f"drops=[{expect_stack}]"),
    ]


steps = []

# --------------------------------- A: the lantern light + drop-self
steps += [
    phase("A: the lantern — the 光级 assertion + the drop-self loot (acceptance ①)"),
    Step(f"forceload add {BAND[0]} {BAND[2]} {BAND[3]} {BAND[5]}"),
    Step(f"fill {BAND[0]} {BAND[1]} {BAND[2]} {BAND[3]} {BAND[4]} {BAND[5]} air"),
    Step(f"setblock {F(LANTERN)} gt6:greg_o_lantern", expect="Changed the block"),
    Step(f"gt6placeables stat {F(LANTERN)}",
         expect="facing=north, lightEmission=15, blockLightAt=15"),
    Step(f"gt6scene6 mine {F(LANTERN)} plunger", expect="drops=[gt6:greg_o_lantern x1]"),
]

# --------------------------------- B: the sandwich bites + comparator
steps += [
    phase("B: the sandwich — the placed bites face + the bites comparator (acceptance ②)"),
    Step(f"setblock {F(SANDWICH)} gt6:sandwich", expect="Changed the block"),
    Step(f"gt6placeables stat {F(SANDWICH)}", expect="size=10, comparator=9"),
    Step(f"gt6placeables eat {F(SANDWICH)}", expect="bit=true, sizeNow=9, comparatorNow=8"),
    Step(f"data get block {F(SANDWICH)}", expect="gt6.size: 9"),
    Step(f"gt6placeables eat {F(SANDWICH)}", expect="bit=true, sizeNow=8, comparatorNow=7"),
    Step(f"gt6scene6 mine {F(SANDWICH)} plunger", expect="drops=[gt6:sandwich x1]"),
]

# --------------------------------- C: the six placement faces + the vanilla aliases
steps += [
    phase("C: the six placement faces — the unified dispatch, whole-stack vs one-item consumption"),
    Step(f"forceload add {BAND[0]} {BAND[2]} {BAND[3]} {BAND[5]}"),
]
# ingot: the WHOLE x12 stack consumed (GT_Proxy :306-:312), handLeft=0
steps += place_pile(474, 348, "gt6:ingot_iron", 12, "placed_ingot", "gt6:ingot_iron x12", 0)
steps += place_pile(475, 348, "gt6:plate_iron", 4, "placed_plate", "gt6:plate_iron x4", 0)
steps += place_pile(476, 348, "gt6:plate_gem_diamond", 2, "placed_gem_plate", "gt6:plate_gem_diamond x2", 0)
steps += place_pile(477, 348, "gt6:scrap_gt_iron", 6, "placed_scrap", "gt6:scrap_gt_iron x6", 0)
# rock/stick: ONE item consumed per placement (GT_Proxy :294-:299), the surplus stays in hand
steps += place_pile(478, 348, "gt6:rock_gt_stone", 3, "placed_rock", "gt6:rock_gt_stone x1", 2)
steps += place_pile(479, 348, "gt6:stick_wood", 3, "placed_stick", "gt6:stick_wood x1", 2)

phase("C-: the vanilla aliases — the Items.stick / Items.flint early arms (:294-:299)")
steps += place_pile(474, 352, "minecraft:stick", 2, "placed_stick", "minecraft:stick x1", 1)
steps += place_pile(475, 352, "minecraft:flint", 2, "placed_rock", "minecraft:flint x1", 1)

# --------------------------------- T: teardown
steps += [
    phase("T: teardown — the band back to air (the pass-open bbox is the backstop)"),
    Step(f"fill {BAND[0]} {BAND[1]} {BAND[2]} {BAND[3]} {BAND[4]} {BAND[5]} air"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p32_placeables",
    slug="p32_placeables",
    sites=gt6world.declare_sites(LANTERN, SANDWICH, PILES),
    preferred_ports=(26582, 26592),      # this card's pinned rcon/query pair (the usb-data neighbour band)
    response_timeout=30.0,
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
