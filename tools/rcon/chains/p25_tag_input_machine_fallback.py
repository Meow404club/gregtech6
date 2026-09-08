#!/usr/bin/env python3
"""p25-tag-input-machine-fallback — the machine-input tag fallback live chain
(declarative framework, the p16_drying_rows shape).

Chain semantics (task p25-tag-input-machine-fallback ACCEPTANCE 2 — the RCON
runServer LIVE proof of the two-stage equality: "真 registry tag 路径：机器输入放
foreign 物品命中 tag fallback + 精确分支回归臂"):

  A the rig: T1 shredder placed, hopper stacked above (the p16 hopper-push feed),
    idle baseline.

  B the exact-branch regression arm: 1x gt6:stick_blaze (the :707 Shredder row's own
    input, Loader_Recipes_Vanilla transcription GT6RecipesShCL shredderTable ":707" —
    1 stick Blaze -> 2x dustSmall Blaze, 32 t @ 16 EUt = 512 EU budget) -> inject
    exactly 32x16=512 -> out[0]=2x gt6:dust_small_blaze. The exact stage must keep
    matching the row's own item with the tag seam untouched.

  C the FOREIGN fallback arm (the live positive): 1x minecraft:blaze_rod — a member
    of forge:rods/blaze through the FORGE-SHIPPED default tag (forge-1.20.1 generated
    tree: rods/blaze = [minecraft:blaze_rod]), while the row input gt6:stick_blaze is
    the GT datagen member of the SAME family tag. The row's recipe input is a
    MaterialPrefixItem -> itemTagFamily(ingot-band rod family) -> the real bound
    registry tag answers TRUE -> the row runs: out[0]=4x (2 accumulated + 2 new, the
    stacking side-proof). This is the exact live analog of the spec's "vanilla
    IRON_INGOT hits the GT iron ingot recipe" — the port carries NO (ingot, Iron)
    machine row yet (the poured maps are ShCL/CokeOven/OreChain/Drying/Distillery/
    Canner), so the twin-tag pair forge:rods/blaze stands in for forge:ingots/iron;
    the offline suite proves the ingots/iron face through the injected stub
    (GT6RecipeTagFallbackTest).

  D the IRON_INGOT negative arm: 1x minecraft:iron_ingot — NOT a forge:rods/blaze
    member (the real bound tag set answers false, the live mirror of the offline
    production binding) -> the row must NOT run: input stays iron_ingotx1, out[0]
    stays 4x, running=false.

  E the direction-rule arm (card acceptance 5, live face): 1x gt6:dust_blaze — the
    SAME material, the SIBLING prefix (its family face is dusts/blaze, NOT
    rods/blaze) -> deriving the tag from the machine input side would match; the
    rule says the tag derives from the recipe input only -> the row must NOT run.

The item names in every /gt6machine report are PATH-ONLY (Item.toString() renders
the registry path, no namespace) — the expects pin the path-only shape. The
inject report itself lists the output slots, so each drive arm pins its verdict
on the inject line's `outputs=[...]` (the strongest deterministic anchor):
positive arms grow the dust count, the negative arms repeat the same count.

passes=2 is the idempotency proof; the pass-open bbox cleanup re-airs the rig.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p25_tag_input_machine_fallback.py
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

# The rig sites: the B/C shredder (394,64,20) plus one FRESH machine per negative
# arm — a DECLINED item legitimately blocks the input slot (it is never consumed),
# so the iron-ingot and the dust_blaze negatives cannot share a machine with each
# other; each gets its own shredder + hopper. Band x=394..398, clear of every
# earlier chain's bands (the neighbour is p24's 386).
SHREDDER_SITE = gt6world.Site(394, 64, 20, dx=1, dy=2, dz=1)
NEG_IRON_SITE = gt6world.Site(396, 64, 20, dx=1, dy=2, dz=1)
NEG_DUST_SITE = gt6world.Site(398, 64, 20, dx=1, dy=2, dz=1)
SHREDDER = F(SHREDDER_SITE)    # 394 64 20
HOPPER = "394 65 20"           # stacked on the shredder, facing down into it
NEG_IRON = F(NEG_IRON_SITE)    # 396 64 20
NEG_IRON_HOPPER = "396 65 20"
NEG_DUST = F(NEG_DUST_SITE)    # 398 64 20
NEG_DUST_HOPPER = "398 65 20"

steps = []

# ------------------------------------------------- A: the rig
steps += [
    phase("A: the rig — shredder placed, no energy, empty slots"),
    Step(f"gt6machine shredder place {SHREDDER}", expect="GT6 shredder placed"),
    Step(f"gt6machine shredder check {SHREDDER}", expect="active=false"),
    Step(f"gt6machine shredder check {SHREDDER}", expect="running=false"),
    Step(f"setblock {HOPPER} hopper[facing=down]", expect="Changed the block"),
]

# ------------------------------------------------- B: the exact-branch regression arm
steps += [
    phase("B: exact arm — 1x gt6:stick_blaze, the :707 row's own item, 2x dust_small_blaze out"),
    Step(f"item replace block {HOPPER} container.0 with gt6:stick_blaze 1",
         expect="Replaced", sleep=4.0),
    # the hopper push cadence (8 game ticks) landed the stick in input slot 0
    # item-name drift: 1.20.1 reports print the PATH-ONLY id, 1.21.1 the FULL
    # namespace:id — the node_expects twin carries the 21.1 shape (the p16 side_io
    # drift convention)
    Step(f"gt6machine shredder check {SHREDDER}", expect="input=stick_blazex1",
         node_expects={"1.21.1": "input=gt6:stick_blazex1"}),
    # the inject report lists the outputs — the row completed inside the drive window
    Step(f"gt6machine shredder inject 32 16 {SHREDDER}", expect="outputs=[2x dust_small_blaze; ]",
         node_expects={"1.21.1": "outputs=[2x gt6:dust_small_blaze; ]"}),
    Step(f"gt6machine shredder check {SHREDDER}", expect="out[0]=2x dust_small_blaze",
         node_expects={"1.21.1": "out[0]=2x gt6:dust_small_blaze"}),
    Step(f"gt6machine shredder check {SHREDDER}", expect="running=false"),
]

# ------------------------------------------------- C: the foreign fallback arm
steps += [
    phase("C: fallback arm — 1x minecraft:blaze_rod (forge:rods/blaze member) hits the :707 row"),
    Step(f"item replace block {HOPPER} container.0 with minecraft:blaze_rod 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine shredder check {SHREDDER}", expect="input=blaze_rodx1",
         node_expects={"1.21.1": "input=minecraft:blaze_rodx1"}),
    # the foreign member fed the same row: 2 accumulated + 2 new = 4x
    Step(f"gt6machine shredder inject 32 16 {SHREDDER}", expect="outputs=[4x dust_small_blaze; ]",
         node_expects={"1.21.1": "outputs=[4x gt6:dust_small_blaze; ]"}),
    Step(f"gt6machine shredder check {SHREDDER}", expect="out[0]=4x dust_small_blaze",
         node_expects={"1.21.1": "out[0]=4x gt6:dust_small_blaze"}),
    Step(f"gt6machine shredder check {SHREDDER}", expect="running=false"),
]

# ------------------------------------------------- D: the IRON_INGOT negative arm
steps += [
    phase("D: negative arm — 1x minecraft:iron_ingot is NO rods/blaze member, the row must not run"),
    Step(f"gt6machine shredder place {NEG_IRON}", expect="GT6 shredder placed"),
    Step(f"setblock {NEG_IRON_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {NEG_IRON_HOPPER} container.0 with minecraft:iron_ingot 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine shredder check {NEG_IRON}", expect="input=iron_ingotx1",
         node_expects={"1.21.1": "input=minecraft:iron_ingotx1"}),
    # the bound tag set answers false: the drive window produces NOTHING (outputs=[]),
    # and the declined item BLOCKS the input slot — it is never consumed
    Step(f"gt6machine shredder inject 32 16 {NEG_IRON}", expect="outputs=[]"),
    Step(f"gt6machine shredder check {NEG_IRON}", expect="input=iron_ingotx1",
         node_expects={"1.21.1": "input=minecraft:iron_ingotx1"}),
    Step(f"gt6machine shredder check {NEG_IRON}", expect="running=false"),
]

# ------------------------------------------------- E: the direction-rule arm
steps += [
    phase("E: direction arm — 1x gt6:dust_blaze (sibling prefix, dusts/blaze) must not run the rods row"),
    Step(f"gt6machine shredder place {NEG_DUST}", expect="GT6 shredder placed"),
    Step(f"setblock {NEG_DUST_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {NEG_DUST_HOPPER} container.0 with gt6:dust_blaze 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine shredder check {NEG_DUST}", expect="input=dust_blazex1",
         node_expects={"1.21.1": "input=gt6:dust_blazex1"}),
    # the tag derives from the RECIPE input (rods) only — the sibling prefix declines
    Step(f"gt6machine shredder inject 32 16 {NEG_DUST}", expect="outputs=[]"),
    Step(f"gt6machine shredder check {NEG_DUST}", expect="input=dust_blazex1",
         node_expects={"1.21.1": "input=gt6:dust_blazex1"}),
    Step(f"gt6machine shredder check {NEG_DUST}", expect="running=false"),
]

# ------------------------------------------------- teardown
steps += [
    phase("F: teardown — the explicit restore over the rig (the pass-open bbox is the backstop)"),
    Step(f"fill 393 61 17 399 67 23 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p25-tag-input-machine-fallback",
    slug="p25taginput",
    sites=gt6world.declare_sites(SHREDDER_SITE, NEG_IRON_SITE, NEG_DUST_SITE),
    preferred_ports=(26108, 26118),      # this card's pinned rcon/query pair (after p24dyechemical 26106/26116)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
