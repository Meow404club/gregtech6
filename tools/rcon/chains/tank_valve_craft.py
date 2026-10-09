#!/usr/bin/env python3
"""tank-valve-craft — the Tank Main Valve CRAFTING compute chain (task
tank-valve-char-misdecode RCON).

The wood valve row (:1195 " R ","rMs"," R ") keys only 'M'/'R' upstream — the
lowercase 'r' rides the CR.java table as the SOFT HAMMER tool (:355). The w3
landing had materialized 'r' as a second lead ring; this chain proves the FIXED
datapack live on the real RecipeManager through /gt6act compute (the
TileEntityAdvancedCraftingTable :414-415 getRecipeFor(CRAFTING) face):

  A THE RECIPE LOAD: /reload re-parses the datapack — the regenerated
    tank_valve/*.json parse through the REAL RecipeManager (a broken row is a
    loud parse error and the compute arms below go canDo=false).

  B THE WOOD POSITIVE ARM: the ACT grid filled per the FIXED recipe (2x lead
    ring + wood wall + SOFT HAMMER + saw) — canDo=true with output=gt6:tank_wood
    IS the fix's live usability proof (the soft hammer tag matches the grid).

  C THE MISDECODE NEGATIVE ARM: the OLD (extra-ring) grid — 3x lead ring + wood
    wall + saw, no soft hammer — must NOT match anymore (canDo=false): the shape
    the w3 landing shipped is retired from the runtime, not just from datagen.

  D THE FAMILY REPRESENTATIVES: the small adamantium row (2x ring + machine
    wall + hard hammer + saw) and the large stainless steel row (8x plate over
    the small valve) both canDo=true — the unchanged 24 rows still match.

passes=2 is the idempotency proof (the [0, 0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/tank_valve_craft.py
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

# the fresh z=210 band (the 248..500 south bands and 128..144 bee band are taken):
# three x-disjoint ACT columns — wood arm, family arm, spare.
Z = 210
Y = 64

ACT_WOOD = gt6world.Site(384, Y, Z)    # arms B/C — the wood row (positive + the misdecode negative)
ACT_FAMILY = gt6world.Site(392, Y, Z)  # arm D — the small/large representatives


def fill(act, slot, item, count=1):
    return Step(f"gt6act fill {slot} {item} {count} {F(act)}", expect=f"GT6 ACT fill slot {slot}: {count}x")


def clear_grid(act):
    """The 3x3 grid teardown between arms (slots 21..29, one /gt6act clear each)."""
    return [Step(f"gt6act clear {slot} {F(act)}", allow_failed=True) for slot in range(21, 30)]


steps = [
    # (no /reload phase: a GT6_SESSION=off chain boots FRESH — the boot RecipeManager
    # already parsed the regenerated tank_valve/*.json (boot-log evidence: zero
    # tank_valve parse errors), and a reload stacked behind a 33s-lagged cold server
    # thread times the RCON socket out — the first-run lesson; the compute arms below
    # are the live parse proof either way.)

    phase("B: the wood positive arm — the FIXED grid (ring/soft-hammer/wall/saw) computes"),
    Step(f"gt6act place {F(ACT_WOOD)}", expect="GT6 advanced_crafting_table_steel placed", poll=30.0),  # the cold-chunk first-command lag
    # the :1195 grid row-major on SLOTS_CRAFTING 21..29: " R " / "rMs" / " R "
    # (22 + 28 = the lead rings, 24 = the SOFT HAMMER — the CR.java :355 letter,
    # 25 = the wood wall, 26 = the saw)
    fill(ACT_WOOD, 22, "gt6:ring_lead"),
    fill(ACT_WOOD, 24, "gt6:soft_hammer"),
    fill(ACT_WOOD, 25, "gt6:wood_wall"),
    fill(ACT_WOOD, 26, "gt6:saw"),
    fill(ACT_WOOD, 28, "gt6:ring_lead"),
    Step(f"gt6act compute {F(ACT_WOOD)}", expect="output=1x tank_wood", node_expects={"1.21.1": "output=1x gt6:tank_wood"}),  # the 21.1 Holder toString drift (the bee_recipes stat ruling)
    Step(f"gt6act compute {F(ACT_WOOD)}", expect="canDo=true"),

    phase("C: the misdecode negative arm — the OLD extra-ring grid no longer matches"),
    *clear_grid(ACT_WOOD),
    fill(ACT_WOOD, 22, "gt6:ring_lead"),
    fill(ACT_WOOD, 24, "gt6:ring_lead"),
    fill(ACT_WOOD, 25, "gt6:wood_wall"),
    fill(ACT_WOOD, 26, "gt6:saw"),
    fill(ACT_WOOD, 28, "gt6:ring_lead"),
    Step(f"gt6act compute {F(ACT_WOOD)}", expect="canDo=false"),

    phase("D: the family representatives — the small/large rows still compute"),
    Step(f"gt6act place {F(ACT_FAMILY)}", expect="GT6 advanced_crafting_table_steel placed", poll=30.0),
    # the :1201 small grid " R "/"hMs"/" R " over machine_wall_adamantium
    fill(ACT_FAMILY, 22, "gt6:ring_adamantium"),
    fill(ACT_FAMILY, 24, "gt6:hammer"),
    fill(ACT_FAMILY, 25, "gt6:machine_wall_adamantium"),
    fill(ACT_FAMILY, 26, "gt6:saw"),
    fill(ACT_FAMILY, 28, "gt6:ring_adamantium"),
    Step(f"gt6act compute {F(ACT_FAMILY)}", expect="output=1x tank_small_adamantium", node_expects={"1.21.1": "output=1x gt6:tank_small_adamantium"}),
    Step(f"gt6act compute {F(ACT_FAMILY)}", expect="canDo=true"),
    *clear_grid(ACT_FAMILY),
    # the :1210 large grid "PPP"/"hMs"/"PPP" over the small SS valve
    fill(ACT_FAMILY, 21, "gt6:plate_stainless_steel"),
    fill(ACT_FAMILY, 22, "gt6:plate_stainless_steel"),
    fill(ACT_FAMILY, 23, "gt6:plate_stainless_steel"),
    fill(ACT_FAMILY, 24, "gt6:hammer"),
    fill(ACT_FAMILY, 25, "gt6:tank_small_stainless_steel"),
    fill(ACT_FAMILY, 26, "gt6:saw"),
    fill(ACT_FAMILY, 27, "gt6:plate_stainless_steel"),
    fill(ACT_FAMILY, 28, "gt6:plate_stainless_steel"),
    fill(ACT_FAMILY, 29, "gt6:plate_stainless_steel"),
    Step(f"gt6act compute {F(ACT_FAMILY)}", expect="output=1x tank_large_stainless_steel", node_expects={"1.21.1": "output=1x gt6:tank_large_stainless_steel"}),
    Step(f"gt6act compute {F(ACT_FAMILY)}", expect="canDo=true"),

    phase("T: teardown — the bands back to air (the pass-open bbox is the backstop)"),
    Step(f"fill 380 {Y - 2} {Z} 398 {Y + 2} {Z} air"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="tank-valve-char-misdecode tank_valve_craft",
    slug="tankvalvecraft",
    sites=gt6world.declare_sites(ACT_WOOD, ACT_FAMILY),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
