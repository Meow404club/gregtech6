#!/usr/bin/env python3
"""p35_slicer_leather — the Slicer vanilla-face live chain (task p35-slicer-row-domain,
RCON group p35_slicer_rows, the single slicer-leather chain per the W3 card spec):

  the :638 helmet row LIVE on the T0 machine — leather_helmet 1 + the split blade
  (gt6:shape_slicer_split, the never-consumed second leg) -> leather 1, EUt 16,
  duration 16.

    feed order: the default `input` leg drops the leather helmet into slot 0 (the
    p35 card's GTMachineCommand feed), the `input item` override leg drops the blade
    into slot 1 (the p34 bumblelyzer multi-input form — the map is
    mMinimalInputItems=2, TileEntityBasicMachine.java:626, so the single-input probe
    never matches).

    energy: EU positive train, the p24_canner_refill grid form — 16 EUt x 16 t = 256
    units, inject 20 x 16 EU covers it with margin (TIER_INPUTS[0] = {16, 32, 64},
    the injected 16 rides the min seat; no overclock, 16 !< 16).

    the blade crown: the post-completion probe pins shape_slicer_split STILL in the
    machine — the upstream stack-size-0 marker (IL.Shape_Slicer_Split.get(0)) carried
    by Recipe.sNotConsumable's isBlade arm (the consume pass Recipe.java:375 skips the
    shrink), live. The probe is `data get block <pos>
    inventory.Items[{id:"gt6:shape_slicer_split"}]` — the id FILTER, not an index:
    the ItemStackHandler Items list is COMPACTED on both legs (empty slots skipped,
    forge-1.20.1 == neoforge-1.21.1 serializeNBT verbatim, Slot as putInt), so after
    completion index 1 is the leather output on both. `gt6machine check` cannot
    carry the crown either (it appends ONLY SLOT_INPUT into input=,
    GTMachineCommand.java:1264). An eaten blade leaves the filter empty → the expect
    misses = the FAIL is the consumption signal.

  BLADE OBTAINABILITY NOTE (the card-pool obligation): the blades are currently
  CREATIVE-obtainable only — the upstream crafting face (the frame + plateTiny
  stainless-steel strokes, MultiItemTechnological.java:362-376) is POOLED with the
  recipe-domain card; this chain /give's the blade into the machine.

The item-name expects are PER-LEG via Step.node_expects (the p26_w1_sifter
precedent): the 1.20.1 ItemStack rendering is the bare registry path while the 21.1
rendering is NAMESPACED.

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_slicer_leather.py
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

Z = 448  # the fresh band — z-disjoint from the p35 boule bands z=416/424 and every registered band
SL1 = gt6world.Site(408, 64, Z, dy=1, dz=1)  # slicer T0 (the :638 row, 16 EUt @ the TIER_INPUTS[0] window)

steps = [
    phase("A: the T0 slicer — the :638 helmet row live (helmet + split blade -> leather)"),
    Step(f"gt6machine slicer place {F(SL1)}", expect="GT6 slicer placed"),
    Step(f"gt6machine slicer input 1 {F(SL1)}",
         expect="GT6 slicer input: 1x leather_helmet into slot 0",
         node_expects={"1.21.1": "GT6 slicer input: 1x minecraft:leather_helmet into slot 0"}),
    Step(f"gt6machine slicer input item gt6:shape_slicer_split 1 {F(SL1)}",
         expect="GT6 slicer input: 1x shape_slicer_split",
         node_expects={"1.21.1": "GT6 slicer input: 1x gt6:shape_slicer_split"}),
    Step(f"gt6machine slicer check {F(SL1)}", expect="minIn=16 recIn=32 maxIn=64"),
    Step(f"gt6machine slicer inject 20 16 {F(SL1)}", expect="1x leather",
         node_expects={"1.21.1": "1x minecraft:leather"}),
    Step(f'data get block {F(SL1)} inventory.Items[{{id:"gt6:shape_slicer_split"}}]',
         expect="shape_slicer_split"),

    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 406 62 446 410 68 450 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35-slicer-leather p35_slicer_rows",
    slug="p35slicerleather",
    sites=gt6world.declare_sites(SL1),
    preferred_ports=(26732, 26742),      # this card's pinned rcon/query pair (after the lathe arm 26712/26722)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
