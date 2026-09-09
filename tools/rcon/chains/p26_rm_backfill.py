#!/usr/bin/env python3
"""p26-rm-row-backfill — the SHREDDER/LATHE backfill round-trip chain (declarative framework).

Chain semantics (task p26-rm-row-backfill ACCEPTANCE): three new-row round-trips through
the p26 handler-template backfill, one per row family, each a menu-less carrier (check
reports data=-2 — the row family binds no MenuType, GTMachines null-menu rows):

  A the rigs: one shredder + one lathe placed (fresh band x=360), both data=-2.

  B the SHREDDER crushed-array arm (:145 — iron carries MORTAR, so the multiplier-16 arm;
    Loader_Recipes_Handlers.java:145 verbatim): a hopper above the shredder feeds ONE
    gt6:crushed_iron, the row runs (crushed x1 -> dust + dustTiny of the mTargetPulver
    target — RecipeMapHandlerPrefixShredding getOutputMaterial override) -> the check pins
    BOTH output items (the :145 transcription is the two-output array [dust, dustTiny];
    the dustDiv72 leg belongs to the :138/:147 rows, not this one).

  C the LATHE hard-arm ingot row (:377 — iron is NEVER_FURNACE and not SOFT, the
    tEasyWorkable.NOT arm; :377 verbatim): ONE gt6:ingot_iron through the hopper, the row
    runs (ingot U x1 -> stick U2 x1, duration = the getCosts multiplier-64 arithmetic)
    -> the check pins gt6:stick_iron.

  D the SHREDDER RECYCLABLE-ring arm (:154 — the MORTAR twin of the :152-155 ring; stick is
    RECYCLABLE and passes the four exclusion tags): ONE gt6:stick_iron, the ring row runs
    (output = the OM.pulverize transcription: stick U2 -> dustSmall x2 of iron)
    -> the check pins 2x gt6:dust_small_iron.

  NOTE on the card's "dustImpure -> dust(+dustTiny)" wording: the dustImpure templates
  (:114/:126) pour as DATA but expand to ZERO rows — no port material carries the
  ITEMGENERATOR.DIRTY_DUSTS tag, so the port registers no dust_impure items at all. The
  measured live proxy for the same handler family is the crushed-array arm above (the other
  resolving template family of the :114-150 batch).

passes=2 is the idempotency proof (the [0,0] of this chain); the sites bbox cleanup between
passes re-airs the rigs.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_rm_backfill.py [--node 1.21.1-neoforge]
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

# The rig sites: the fresh x=360 band (p16 used x=340). Each machine gets a hopper stacked
# one above (dy=2 covers it), the p16_drying_rows feed idiom.
SHREDDER_SITE = gt6world.Site(360, 64, 20, dx=1, dy=2, dz=1)
SHREDDER = F(SHREDDER_SITE)            # 360 64 20
SHREDDER_HOPPER = "360 65 20"          # stacked on the shredder, facing down into it

LATHE_SITE = gt6world.Site(362, 64, 20, dx=1, dy=2, dz=1)
LATHE = F(LATHE_SITE)                  # 362 64 20
LATHE_HOPPER = "362 65 20"

RING_SITE = gt6world.Site(364, 64, 20, dx=1, dy=2, dz=1)
RING_SHREDDER = F(RING_SITE)           # 364 64 20 — the second shredder (the ring arm)
RING_HOPPER = "364 65 20"

steps = []

# ------------------------------------------------- A: the rigs
steps += [
    phase("A: the rigs — shredder + lathe + second shredder placed, all menu-less (data=-2)"),
    Step(f"gt6machine shredder place {SHREDDER}", expect="GT6 shredder placed"),
    Step(f"gt6machine shredder check {SHREDDER}", expect="data=-2"),
    Step(f"gt6machine lathe place {LATHE}", expect="GT6 lathe placed"),
    Step(f"gt6machine lathe check {LATHE}", expect="data=-2"),
    Step(f"gt6machine shredder place {RING_SHREDDER}", expect="GT6 shredder placed"),
    Step(f"gt6machine shredder check {RING_SHREDDER}", expect="data=-2"),
    Step(f"setblock {SHREDDER_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"setblock {LATHE_HOPPER} hopper[facing=down]", expect="Changed the block"),
    Step(f"setblock {RING_HOPPER} hopper[facing=down]", expect="Changed the block"),
]

# ------------------------------------------------- B: the crushed-array arm (:145)
# NOTE on the expect strings: the gt6machine check ITEM reporter prints bare item ids
# (no registry namespace: crushed_ironx1) and the empty input slot as airx0 — the
# namespace-carrying form is the FLUID stat reporter only (p16 idiom). Measured live
# on the forge leg, 2026-09-10.
steps += [
    phase("B: crushed_iron 1 -> dust + dustTiny — the :145 row, hopper-fed"),
    Step(f"item replace block {SHREDDER_HOPPER} container.0 with gt6:crushed_iron 1",
         expect="Replaced", sleep=4.0),
    # the hopper push cadence (8 game ticks) landed the crushed ore in the input slot
    Step(f"gt6machine shredder check {SHREDDER}", expect="input=crushed_ironx1"),
    # duration = getCosts(mult 16) ~= 55 t for iron (x16 EUt) — 4000 overshoots, the row
    # stalls honestly once the single item is consumed
    Step(f"gt6machine shredder inject 4000 64 {SHREDDER}", expect="used=4000"),
    Step(f"gt6machine shredder check {SHREDDER}", expect="out[0]=1x dust_iron"),
    Step(f"gt6machine shredder check {SHREDDER}", expect="out[1]=1x dust_tiny_iron"),
    Step(f"gt6machine shredder check {SHREDDER}", expect="input=airx0"),
]

# ------------------------------------------------- C: the lathe hard-arm ingot row (:377)
steps += [
    phase("C: ingot_iron 1 -> stick_iron — the :377 row (tEasyWorkable.NOT arm), hopper-fed"),
    Step(f"item replace block {LATHE_HOPPER} container.0 with gt6:ingot_iron 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine lathe check {LATHE}", expect="input=ingot_ironx1"),
    # duration = units(U, U, 64+64*q, T) — 192 t for iron (q=2), x16 EUt
    Step(f"gt6machine lathe inject 4000 64 {LATHE}", expect="used=4000"),
    Step(f"gt6machine lathe check {LATHE}", expect="out[0]=1x stick_iron"),
    Step(f"gt6machine lathe check {LATHE}", expect="input=airx0"),
]

# ------------------------------------------------- D: the RECYCLABLE-ring arm (:154)
steps += [
    phase("D: stick_iron 1 -> 2x dust_small_iron — the :154 ring row, the OM.pulverize output"),
    Step(f"item replace block {RING_HOPPER} container.0 with gt6:stick_iron 1",
         expect="Replaced", sleep=4.0),
    Step(f"gt6machine shredder check {RING_SHREDDER}", expect="input=stick_ironx1"),
    # duration = units(U2, U, 16+16*q, T) = 24 t for iron, x16 EUt
    Step(f"gt6machine shredder inject 4000 64 {RING_SHREDDER}", expect="used=4000"),
    Step(f"gt6machine shredder check {RING_SHREDDER}", expect="out[0]=2x dust_small_iron"),
    Step(f"gt6machine shredder check {RING_SHREDDER}", expect="input=airx0"),
]

# ------------------------------------------------- teardown
steps += [
    phase("E: teardown — the explicit restore over the rig (the pass-open bbox is the backstop)"),
    Step("fill 357 61 17 367 67 23 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-rm-row-backfill",
    slug="p26rmbackfill",
    sites=gt6world.declare_sites(SHREDDER_SITE, LATHE_SITE, RING_SITE),
    preferred_ports=(25875, 25885),      # this card's pinned rcon/query pair
    response_timeout=60.0,               # the 4000-tick inject loops run server-side
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
