#!/usr/bin/env python3
"""p33-food-machines — the kitchen machine family live chain (task
p33-food-machines-kitchen, RCON group p33_food_machines):

  A the Juicer (32722, the 07Paintable manual block, RM.Juicer) — the MANUAL OUTPUT
    live proof: place → input the honey-comb row's item (gt6:comb_honey, the
    juicer.json :266 row) → the top-face interact arm reports "processed:" and the
    check census pins the output — the null-player RCON arm of the same
    activateChain the p26 kitchen family drives.

  B the Fermenter (22003, the single-variant HU basic machine, RM.Fermenter) — the
    fermenter.json smoke row (wheat + water → sugar) RUNS: place → input wheat →
    water through the east fill face → burning box UNDER (the p29_w1_fermenter
    supply form) → the poll pins out[0]=1x sugar (the row completes on the box's
    16 HU/t train).

  C the Oven (20001, the RM.Furnace HU tier — the p4 machine) — a REAL row runs:
    place → input 8 cobblestone (the Loader_Recipes_Vanilla :692 stone-family row's
    furnace idiom: cobblestone → stone) → the ADJACENT HU emitter rig (task
    p34-oven-hu-conversion rebased the oven to its upstream 20001-04 type,
    NBT_ENERGY_ACCEPTED = TD.Energy.HU — the p33-era EU rig adaptation is retired;
    the p13_hu_steam_foundation source form stays, retyped HU) → the poll pins
    output=stonex8.

  D teardown: the explicit fill-air over the band.

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p33_food_machines.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)
if str(_HERE.parent) not in sys.path:
    sys.path.insert(0, str(_HERE.parent))

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# the fresh z=340 band (east of the p33-cracker 330 strip with margin; the west
# 448-456 x-column reused at dz+10 — the bbox admission is band-disjoint)
Z = 340
JUICER = gt6world.Site(448, 65, Z)
FER = gt6world.Site(452, 65, Z)
FER_BOX = gt6world.Site(452, 64, Z)
OVEN = gt6world.Site(456, 65, Z)
OVEN_RIG = gt6world.Site(457, 65, Z)

steps = [
    phase("A: the Juicer — the manual top-face round with a REAL RM.Juicer row (juicer.json :266 honey-comb)"),
    Step(f"gt6kitchen juicer place {F(JUICER)}", expect="GT6 juicer placed at"),
    Step(f"gt6kitchen juicer input gt6:comb_honey 1 {F(JUICER)}",
         expect="GT6 juicer input: 1x gt6:comb_honey into slot 0"),
    # the slot census rides BEFORE the manual round: check prints non-empty slots
    # only, and the interact consumes the comb — a post-interact slot0= pin can
    # never pass (the forge-leg chain-defect fix; the machine behavior was correct)
    Step(f"gt6kitchen juicer check {F(JUICER)}", expect="slot0=1xgt6:comb_honey"),
    # the top-face manual round: findRecipe + isRecipeInputEqual pay the comb and
    # land the 90 mB honey output — the MANUAL OUTPUT live proof
    Step(f"gt6kitchen juicer interact {F(JUICER)}", expect="GT6 juicer interact: processed:"),
    Step(f"gt6kitchen juicer check {F(JUICER)}", expect="tank0=gt6:honey:90/1000000L"),

    phase("B: the Fermenter — the smoke row (wheat + water -> sugar) completes on the burning-box train"),
    Step(f"gt6machine fermenter place {F(FER)}", expect="GT6 fermenter placed"),
    Step(f"gt6machine fermenter input 1 {F(FER)}",
         expect="1x wheat into slot 0",
         node_expects={"1.21.1": "1x minecraft:wheat into slot 0"}),
    Step(f"gt6machine fermenter fluid fill east minecraft:water 1000 {F(FER)}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6burner place {F(FER_BOX)} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6burner fuel {F(FER_BOX)} minecraft:coal 4", expect="minecraft:coal x4"),
    Step(f"gt6burner ignite {F(FER_BOX)}", expect="burning=true"),
    Step(f"gt6machine fermenter check {F(FER)}",
         expect="out[0]=1x sugar",
         node_expects={"1.21.1": "out[0]=1x minecraft:sugar"},
         poll=300.0),

    phase("C: the Oven — the RM.Furnace row (cobblestone -> stone) on the p13 HU-rig form"),
    Step(f"gt6energy place {F(OVEN_RIG)}", expect="GT6 energy source placed"),
    # p34-oven-hu-conversion: the oven books HU (its upstream 20001-04 type,
    # NBT_ENERGY_ACCEPTED = TD.Energy.HU) — the p33-era EU rig retires with it.
    Step(f"gt6energy type {F(OVEN_RIG)} HU", expect="type ENERGY.HEAT"),
    Step(f"gt6energy volt {F(OVEN_RIG)} 32", expect="voltage 32"),  # under the oven mInputMax 64
    Step(f"gt6oven place {F(OVEN)}", expect="placed"),
    Step(f"gt6oven input 8 {F(OVEN)}", expect="cobblestone"),
    Step(f"gt6energy mode {F(OVEN_RIG)} on", expect="emitting true", sleep=2.0),
    Step(f"gt6oven check {F(OVEN)}", expect="running=true"),
    Step(f"gt6oven check {F(OVEN)}",
         expect="output=stonex8",
         node_expects={"1.21.1": "output=minecraft:stonex8"},
         poll=300.0),

    phase("D: teardown — restore the band"),
    Step(f"fill 447 62 {Z - 1} 457 68 {Z + 2} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p33_food_machines matches
    name="p33-food-machines p33_food_machines",
    slug="p33foodmachines",
    sites=gt6world.declare_sites(JUICER, FER, FER_BOX, OVEN, OVEN_RIG),
    preferred_ports=(26364, 26374),      # this card's pinned rcon/query pair (after p33cracker 26344/26354)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
