#!/usr/bin/env python3
"""p33_circuits_craft — the circuits C-column crafting chain (task
p33-circuits-crafting-c RCON: the 26 integrated-circuit rows + the
ventilation/processor-unit six, the datapack-domain face this card ships).

The offline face is pinned by GT6CircuitsCraftingJsonTest (both legs green:
the generated recipe JSONs read off the classpath and asserted against the
Loader_MultiTileEntities.java:1184-1189 / ItemIntegratedCircuit.java:58-85
transcriptions). What a dedicated server node adds on top is the LIVE face:

  A the recipe load: /reload re-parses the datapack — a missing/unparseable
    row is a loud RecipeManager parse error and the id absent from the
    manager (the headless channel has no player selector for /recipe give,
    the p16_chisel give-face ruling).

  B the craft face: the ACT (the p24 rig) crafts the base circuit row —
    3x iron small gears + 9x iron rods; the ACT's findCraftingResult routes
    through the REAL RecipeManager, so canDo=true IS the load proof, and
    craft once emits the circuit (the stamped base identity).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p33_circuits_craft.py
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

# the fresh z=290 band (south of p33_circuits_parts z=284, north of p29_w3_parts
# z=292 — the tight gap holds one chest+ACT column pair on x-disjoint even columns).
CHEST = gt6world.Site(420, 64, 290, dx=1)                # the gear/rod give site
SITE = gt6world.Site(422, 64, 290, dx=0, dy=1, dz=1)     # the ACT
T = F(SITE)
CHEST_POS = "420 64 290"

# the base-row grid (the :58 shape): G=small gears x3, S=rods x6 + the h/w tool slots
# — the ACT's direct-grid fill face (the SLOTS_CRAFTING band 21..29; the base row has
# NO selector, so the pattern-ghost arm does not ride — the fill is the real stock).
GRID = gt6world.Site(422, 64, 291, dx=0)  # informational only; the ACT fills ride T

steps = [
    phase("A: the recipe load — the datapack re-parse"),
    # /recipe give needs an online player selector — the headless RCON channel has none
    # (the p16_chisel give-face ruling). The load face is /reload: a missing/unparseable
    # row is a loud RecipeManager parse error and the id absent from the manager; the
    # ACT live-craft below routes through the REAL RecipeManager, so canDo=true IS the
    # load proof (the row was parsed and matched).
    Step("reload", expect="Reloading"),

    phase("B: the craft face — the ACT crafts the base circuit row"),
    Step(f"setblock {CHEST_POS} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST_POS} container.0 with minecraft:iron_ingot 32", expect="Replaced"),
    Step(f"gt6act place {T}", expect="GT6 advanced_crafting_table placed"),
    # stock the grid: 3x small gears + 9x rods across the 3x3 (the :58 grid needs the
    # h/w tool tags only as AUX — the vanilla grid consumes the tag members too, so the
    # ACT grid stocks the REAL carriers: gears/rods, the tools ride the tag ports)
    Step(f"gt6act fill 21 gt6:gear_gt_small_iron 1 {T}", expect="GT6 ACT fill slot 21: 1x"),
    Step(f"gt6act fill 22 gt6:stick_iron 1 {T}", expect="GT6 ACT fill slot 22: 1x"),
    Step(f"gt6act fill 23 gt6:gear_gt_small_iron 1 {T}", expect="GT6 ACT fill slot 23: 1x"),
    Step(f"gt6act fill 24 gt6:stick_iron 1 {T}", expect="GT6 ACT fill slot 24: 1x"),
    Step(f"gt6act fill 25 gt6:stick_iron 1 {T}", expect="GT6 ACT fill slot 25: 1x"),
    Step(f"gt6act fill 26 gt6:stick_iron 1 {T}", expect="GT6 ACT fill slot 26: 1x"),
    Step(f"gt6act fill 27 gt6:gear_gt_small_iron 1 {T}", expect="GT6 ACT fill slot 27: 1x"),
    Step(f"gt6act fill 28 gt6:stick_iron 1 {T}", expect="GT6 ACT fill slot 28: 1x"),
    Step(f"gt6act fill 29 gt6:gear_gt_small_iron 1 {T}", expect="GT6 ACT fill slot 29: 1x"),
    Step(f"gt6act compute {T}", expect="canDo=true"),
    Step(f"gt6act craft once {T}", expect="crafted=true, hold=[1x"),
    # the stat line pins the crafted stack in the output slot 31 (the stamped base identity)
    Step(f"gt6act stat {T}", expect="output=1x"),

    phase("C: teardown — restore the band"),
    Step(f"fill 418 63 288 426 68 293 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p33_circuits_craft circuits-craft",
    slug="p33circuitscraft",
    sites=gt6world.declare_sites(*[CHEST, SITE]),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
