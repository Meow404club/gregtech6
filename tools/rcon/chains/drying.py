#!/usr/bin/env python3
"""drying-rows-backfill-2 — the DRYING backfill round-trip chain (declarative framework).

Chain semantics (task drying-rows-backfill-2 ACCEPTANCE — "矿物行+盐行+clay→terracotta
活体臂，两遍 [0,0]+server ERROR 0"): one T1 dryer driven by the existing dryer RCON shape
(no new command arms), the new-row round-trips of the P19 backfill:

  A the rig: dryer (FACING north, no fuel) + the feed hopper ABOVE it + the extraction
    hopper BELOW it. Two /data merges arm the rig (the side_io:112 precedent):
    tank_capacity:8000 — the port input-tank default is 1000 L and the salt rows carry
    7000/8000 L; the port RecipeMap carries no mMinInputTankSizes, and the load() read of
    NBT_TANK_CAPACITY + applyTankCapacity IS the sanctioned override seam;
    item_sides_in:66b (SBIT_U|SBIT_A) — the item masks are class-default 127 (the row
    itemIn is data-only), so the bottom hopper would otherwise reach the INPUT slot too;
    66 keeps the top feed (SBIT_U) and closes the bottom for inputs, turning the bottom
    face into an outputs-only extraction port for the below hopper.

  B the salt-family arms (the two UNGUARDED rows, Loader_Recipes_Chem.java:548/:553):
    fill gt6:<fluid> <in> → inject the FULL energy budget (progress is 1 tick per driven
    tick, EUt 16 vs 64 injected — the inject tick count covers the verbatim duration with
    the stall backstop) → fluid stat pins the EXACT arithmetic out[0]=<out> L of
    gt6:distilled_water → draw empties the bank; the item output is pinned on the below
    hopper's Items NBT after its pull cadence:
      seawater 7000 → 6750 + 1x gt6:dust_small_salt (:548, OM.dust(NaCl, U4)),
      waterdirty 8000 → 7000 + 1x minecraft:dirt (:553).
    Both arms double as the live registration proof of the gt6:seawater / gt6:waterdirty
    simple-liquid fluids (GTFluids.SIMPLE_LIQUID_SPECS).

  C the mineral arm (:565 Gypsum — dur 2000, the shortest full mineral row): a hopper
    feed of ONE gt6:dust_gypsum → inject 4000 → stat pins out[0]=1000 L of
    gt6:distilled_water → the below hopper pins the ca_so4 dust output → draw.

  D the BlockDiggable arm (BlockDiggable.java:73 — vanilla clay block → hardened_clay,
    dur 64, EUt 16, NO fluid legs): a hopper feed of ONE minecraft:clay → inject → the
    below hopper pins the terracotta output (1.20.1 identity of 1.7.10 hardened_clay).

  The C/D gt6machine check input-slot pins use the LOADER-NEUTRAL substring (id+count):
  the check ITEM reporter is bare-id on 1.20.1-forge (input=dust_gypsumx1,
  input=clayx1) but registry-qualified on 1.21.1-neoforge (input=gt6:dust_gypsumx1,
  input=minecraft:clayx1) — the rm-row-backfill loader-fork lesson, which this
  chain predated; the neo-leg red of the closeout sweep (2026-09-12) was exactly
  that input= prefix mismatch, while the loader-neutral pins (calcium_sulfate,
  minecraft:terracotta) passed both legs all along.

The remaining rows are per-row pinned offline (GT6RecipesDryingTest: transcription walks +
live-universe census + the 35-row pour census); the live pour census for ALL rows is the
server log line "GT6 Drying poured: 35 loaded, 3 skipped" (:530 water_hot + the two
upstream-dead gemChipped/gemFlawed Ice rows).

passes=2 is the idempotency proof (the [0,0] of this chain); the sites bbox cleanup
between passes re-airs the rig.

Run:  GT6_SESSION=off python3 tools/rcon/chains/drying.py
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

# The rig site: the dryer (360,64,20), the feed hopper above (65) and the extraction
# hopper below (63) — dy=2 covers 62..66. Band x=360, clear of every earlier chain's
# sites (drying used x=340, aqua x=300).
DRYER_SITE = gt6world.Site(360, 64, 20, dx=1, dy=2, dz=1)
DRYER = F(DRYER_SITE)          # 360 64 20
HOPPER_TOP = "360 65 20"       # stacked on the dryer, facing down into it
HOPPER_BOT = "360 63 20"       # under the dryer: pulls the item outputs only

steps = []

# ------------------------------------------------- A: the rig
steps += [
    phase("A: the rig — dryer placed, no fuel, empty banks, both hoppers, carrier arms"),
    Step(f"gt6machine dryer place {DRYER}", expect="GT6 dryer placed"),
    # data=-1 not -2: the gt6:dryer menu supplier is bound since machine-fluid-gui
    Step(f"gt6machine dryer check {DRYER}", expect="data=-1"),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
    Step(f"setblock {HOPPER_TOP} hopper[facing=down]", expect="Changed the block"),
    Step(f"setblock {HOPPER_BOT} hopper[facing=down]", expect="Changed the block"),
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="in[0]=0 L of nothing; out[0]=0 L of nothing"),
    # the input-tank carrier arm (7000/8000 L salt rows vs the 1000 L default tank)
    Step(f"data merge block {DRYER} {{tank_capacity:8000}}", expect="Modified block data"),
    # item_sides_in = SBIT_U|SBIT_A (66): the top feed stays, the bottom face loses the
    # input slot and keeps the outputs — the below hopper becomes an outputs-only drain
    Step(f"data merge block {DRYER} {{item_sides_in:66b}}", expect="Modified block data"),
]

# ------------------------------------------------- B: the salt-family arms
SALT_ARMS = [
    ("seawater"  , 7000, 6750, 11200, "dust_small_salt"),  # :548 — verbatim 7000→6750 + OM.dust(NaCl, U4)
    ("waterdirty", 8000, 7000, 16000, "minecraft:dirt" ),  # :553 — verbatim 8000→7000 + vanilla dirt
]

for _fluid, _inp, _out, _dur, _item in SALT_ARMS:
    steps += [
        phase(f"B: {_fluid} {_inp} -> {_out} + {_item} — fill, full-budget drive, arithmetic, item pin, draw"),
        Step(f"gt6machine dryer fluid fill east gt6:{_fluid} {_inp} {DRYER}",
             expect=f"filled {_inp}/{_inp} L of gt6:{_fluid} (ACCEPTED)"),
        # progress is one tick per driven tick (EUt 16, injecting 64/t) — the tick count
        # covers the verbatim duration plus the idle-stall backstop
        Step(f"gt6machine dryer inject {_dur + 800} 64 {DRYER}", expect="used="),
        Step(f"gt6machine dryer fluid stat {DRYER}",
             expect=f"in[0]=0 L of nothing; out[0]={_out} L of gt6:distilled_water"),
        Step(f"gt6machine dryer fluid draw up {_out} {DRYER}",
             expect=f"drawn {_out}/{_out} L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
        Step(f"data get block {HOPPER_BOT} Items", expect=_item, sleep=2.0),
    ]

# ------------------------------------------------- C: the mineral arm (:565 Gypsum)
# NOTE on the expect strings: the gt6machine check ITEM reporter formats item ids
# loader-versioned — bare on 1.20.1-forge (input=dust_gypsumx1), registry-qualified
# on 1.21.1-neoforge (input=gt6:dust_gypsumx1; the rm-row-backfill loader-fork
# lesson). The C/D input-slot pins below use the LOADER-NEUTRAL substring (id+count,
# no input= prefix): this chain predated that lesson and red on the neo leg of the
# closeout sweep (2026-09-12, state progress.closeout_sweep) before being
# re-pinned; the D-arm terracotta output pin was already namespace-neutral.
steps += [
    phase("C: gypsum dust 1 -> 1000 L + ca_so4 — the :565 row, the hopper-fed item input"),
    Step(f"item replace block {HOPPER_TOP} container.0 with gt6:dust_gypsum 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine dryer check {DRYER}", expect="dust_gypsumx1"),
    # dur 2000 x EUt 16 — 4000 driven ticks cover it with the stall backstop
    Step(f"gt6machine dryer inject 4000 64 {DRYER}", expect="used=4000"),
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="in[0]=0 L of nothing; out[0]=1000 L of gt6:distilled_water"),
    Step(f"gt6machine dryer fluid draw up 1000 {DRYER}",
         expect="drawn 1000/1000 L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
    Step(f"data get block {HOPPER_BOT} Items", expect="calcium_sulfate", sleep=2.0),
]

# ------------------------------------------------- D: the BlockDiggable arm (:73 clay → terracotta)
steps += [
    phase("D: clay 1 -> terracotta — the BlockDiggable.java:73 row, no fluid legs"),
    Step(f"item replace block {HOPPER_TOP} container.0 with minecraft:clay 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine dryer check {DRYER}", expect="clayx1"),
    # dur 64 x EUt 16 — the first driven ticks complete it
    Step(f"gt6machine dryer inject 4000 64 {DRYER}", expect="used=4000"),
    Step(f"data get block {HOPPER_BOT} Items", expect="minecraft:terracotta", sleep=2.0),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
]

# ------------------------------------------------- teardown
steps += [
    phase("E: teardown — the explicit restore over the rig (the pass-open bbox is the backstop)"),
    Step("fill 357 61 17 363 67 23 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="drying-rows-backfill-2",
    slug="drying",
    sites=gt6world.declare_sites(DRYER_SITE),
    preferred_ports=(25904, 25914),      # this card's pinned rcon/query pair (P19 segment)
    response_timeout=240.0,              # the 17000-tick inject loop runs server-side
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
