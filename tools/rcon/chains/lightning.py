#!/usr/bin/env python3
"""eu-special/lightning — the Lightning Processor chain (task eu-special
ACCEPTANCE ②+④: the NBT_USE_OUTPUT_TANK live regression — the REPO'S FIRST
output-tank-key consumer beyond the Canner — plus the smoke row run).

THE ATTRIBUTION (the card discipline, mirrored from the section doc): NO lightning-
strike mechanism lives here — upstream the strike-into-network face is the LightningRod
MULTIBLOCK (18104, ported, task lightning-rod); the Lightning Processor is a PLAIN
EU consumer (Loader:1582-1586, MultiTileEntityBasicMachineElectric, NBT_ENERGY_ACCEPTED
EU, energy SBIT_B). The chain proves the machine, not the weather.

The USE_OUTPUT_TANK semantics (:716-732, the TileEntityBasicMachine fallback arm): a
failed input-tank recipe lookup re-runs against the OUTPUT tanks and the consume chain
drains THEM. The live proof, on the committed lightning.json smoke row (quartz +
glowstone_dust + water 1000 -> prismarine_crystals, eUt 16, duration 64 — NO fluid
output, so the output tank can only LOSE water):

  A the ordinary leg (T1): place -> stock quartz+glowstone (slots 0/1) -> fluid fill
    through the UP face (the rotated SBIT_U|SBIT_L(+A) tank-in mask (70)) 1000 L of water ->
    inject the 64-train -> completes; fluid stat pins in[0]=0 (the recipe consumed the
    input tank) and out[0]=0 (the row has NO fluid output — the output tank NEVER moved).
  B the fallback leg (the FIRST-INSTANCE key regression): fresh machine, the input
    tank stays EMPTY, the OUTPUT tank is data-merged to 1000 L of water (the per-leg
    NBT payload; fill() only ever targets the fillable input tanks, so the merge is
    the honest writer) -> the same 64-train COMPLETES (the :717/:718 re-lookup found
    the row off the output tanks) -> fluid stat pins out[0]=0 L — the consume chain
    DRAINED THE OUTPUT TANK (out-of-tank proof).
  C the mask line + teardown (the energyIn=96 = SBIT_B|SBIT_A single bottom face pin,
    then the band restore).

No /gt6machine command arm exists for this family (the card FILES_SCOPE excludes
GTMachineCommand) — the setblock + data merge + shared-literal pass-through drive
(the fe_inbound wiremill_ulv form, documented on autocrafter.py).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/lightning.py
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

SITE_A = gt6world.Site(396, 65, 276, dy=1)  # the ordinary leg
SITE_B = gt6world.Site(402, 65, 276, dy=1)  # the output-tank fallback leg
A, B = F(SITE_A), F(SITE_B)

STOCK = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:12,Items:[{{Slot:0b,id:"minecraft:quartz",Count:1b}},{{Slot:1b,id:"minecraft:glowstone_dust",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:12,Items:[{{Slot:0b,id:"minecraft:quartz",count:1}},{{Slot:1b,id:"minecraft:glowstone_dust",count:1}}]}}}}',
}
# the output-tank writer — the machine save key tanks.out.0 (the FluidTankGT NBT form,
# per-leg FluidStack payload: FluidName/Amount vs id/amount)
SEED_OUT_TANK = {
    "1.20.1": 'data merge block {p} {{"tanks.out.0":{{FluidName:"minecraft:water",Amount:1000}}}}',
    "1.21.1": 'data merge block {p} {{"tanks.out.0":{{id:"minecraft:water",amount:1000}}}}',
}

steps = [
    phase("A: the ordinary leg — the row runs off the INPUT tank, the output tank never moves"),
    Step(f"setblock {A} gt6:lightning", expect="Changed the block"),
    Step(STOCK["1.20.1"].format(p=A), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=A)}),
    Step(f"gt6machine shredder fluid stat {A}", expect="masks fluidIn=70 fluidOut=81 energyIn=96"),
    Step(f"gt6machine shredder check {A}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine shredder fluid fill up minecraft:water 1000 {A}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    # budget 1024 = 16 ticks of mInputMax; the 20-train covers it
    Step(f"gt6machine shredder inject 20 64 {A}", expect="outputs=[1x prismarine_crystals",
         node_expects={"1.21.1": "outputs=[1x minecraft:prismarine_crystals"}),
    Step(f"gt6machine shredder fluid stat {A}", expect="in[0]=0 L of nothing"),
    Step(f"gt6machine shredder fluid stat {A}", expect="out[0]=0 L of nothing"),

    phase("B: the fallback leg — water ONLY in the OUTPUT tank, the row still completes"),
    Step(f"setblock {B} gt6:lightning", expect="Changed the block"),
    Step(STOCK["1.20.1"].format(p=B), expect="Modified block data",
         node_cmds={"1.21.1": STOCK["1.21.1"].format(p=B)}),
    Step(SEED_OUT_TANK["1.20.1"].format(p=B), expect="Modified block data",
         node_cmds={"1.21.1": SEED_OUT_TANK["1.21.1"].format(p=B)}),
    Step(f"gt6machine shredder fluid stat {B}", expect="in[0]=0 L of nothing"),
    Step(f"gt6machine shredder fluid stat {B}", expect="out[0]=1000 L of minecraft:water"),
    # the input-tank lookup FAILS (empty) — only the :717 flag-armed re-lookup can bind
    Step(f"gt6machine shredder inject 20 64 {B}", expect="outputs=[1x prismarine_crystals",
         node_expects={"1.21.1": "outputs=[1x minecraft:prismarine_crystals"}),
    Step(f"gt6machine shredder fluid stat {B}", expect="out[0]=0 L of nothing"),

    phase("C: teardown"),
    Step("fill 395 64 275 403 68 277 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="eu_special lightning",
    slug="lightning",
    sites=gt6world.declare_sites(SITE_A, SITE_B),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
