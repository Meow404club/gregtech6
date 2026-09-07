#!/usr/bin/env python3
"""p21-chisel-drop-conversion — the mining-drop chisel conversion live chain.

Chain semantics (task p21 ACCEPTANCE, the GT_Tool_Chisel.java:73-77 BlockStones arm
landed as a loot face — alternatives[match_tool(gt6:chisel) -> the CHISEL_MAPPINGS
variant item, otherwise the BlockStones.java:731 baseline]):

  A the POSITIVE arm: the fake player holds gt6:chisel and MINES the block
    (/gt6chisel mine drives Block.playerDestroy + removeBlock — the
    ServerPlayerGameMode.destroyBlock drop face, dropResources consults the table
    with THIS_ENTITY + TOOL context):
      - gt6:marble_bricks yields gt6:marble_bricks_cracked (the CHISEL_MAPPINGS[3]=4
        BRICK->CRACK row);
      - gt6:marble yields gt6:marble_smooth (the [0]=7 STONE->SMOTH row, the chisel
        arm REPLACING the :731 cobble swap when the chisel is the tool).

  B the NEGATIVE arm: the same blocks with an EMPTY hand fall to the :731 baseline —
    gt6:marble_bricks drops itself, gt6:marble drops gt6:marble_cobble.

  C the no-TOOL context face (the explosion semantics): `loot spawn` consults
    gt6:blocks/marble_bricks with no TOOL param at all — match_tool is false and the
    baseline arm answers (the upstream no-HarvestDropsEvent equivalence, live).

The command self-checks the observed drops against the census decision
(GT6StoneBlockLoot chiselTarget/baselineItem) and DISCARDS what it spawned, so the
two framework passes are the [0, 0] idempotency proof; the pass-open bbox cleanup
restores the sites between passes. Run with GT6_SESSION=off (the pinned per-chain
ports; the session model ignores chain.preferred_ports — the p16-aqua-fluids lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p21_chisel_drops.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x 388..394, z 124..130: clear of the p13/p14 boiler chains (x 104..172),
# the p16 bands (x 184..200 / x 300..360), the p19 drying rig (x 360, z 20), the p19
# chisel band (x 372..377) and the p21 stoneblocks band (x 380..386, z 124..130).
M = gt6world.Site(388, 64, 124, dx=1)   # the chisel-mined bricks site
S = gt6world.Site(390, 64, 124, dx=1)   # the chisel-mined variant-0 site
N = gt6world.Site(392, 64, 124, dx=1)   # the bare-hand bricks site
V = gt6world.Site(394, 64, 124, dx=1)   # the bare-hand variant-0 site
L = gt6world.Site(394, 64, 128, dx=1)   # the loot-spawn (no-TOOL context) site

M_POS = "388 64 124"
S_POS = "390 64 124"
N_POS = "392 64 124"
V_POS = "394 64 124"
L_POS = "394 64 128"

ITEMS = "@e[type=minecraft:item,distance=..8,x=391,y=64,z=126]"

steps = []

# ------------------------------------------- A: the positive (chisel) mining arm
steps += [
    phase("A: the chisel mining arm — CHISEL_MAPPINGS[3]=4: gt6:marble_bricks -> gt6:marble_bricks_cracked"),
    Step(f"setblock {M_POS} gt6:marble_bricks", expect="Changed the block"),
    Step(f"gt6chisel mine {M_POS}",
         expect="hand=false on gt6:marble_bricks: drops=[gt6:marble_bricks_cracked x1], check=OK"),
    Step(f"execute if block {M_POS} minecraft:air", expect="Test passed"),
    phase("A2: [0]=7 — gt6:marble -> gt6:marble_smooth (the chisel arm replaces the :731 cobble swap)"),
    Step(f"setblock {S_POS} gt6:marble", expect="Changed the block"),
    Step(f"gt6chisel mine {S_POS}",
         expect="hand=false on gt6:marble: drops=[gt6:marble_smooth x1], check=OK"),
    Step(f"execute if block {S_POS} minecraft:air", expect="Test passed"),
]

# ------------------------------------------- B: the negative (bare-hand) arm
steps += [
    phase("B: the negative arm — an empty hand falls to the :731 baseline"),
    Step(f"setblock {N_POS} gt6:marble_bricks", expect="Changed the block"),
    Step(f"gt6chisel mine {N_POS} hand",
         expect="hand=true on gt6:marble_bricks: drops=[gt6:marble_bricks x1], check=OK"),
    Step(f"setblock {V_POS} gt6:marble", expect="Changed the block"),
    Step(f"gt6chisel mine {V_POS} hand",
         expect="hand=true on gt6:marble: drops=[gt6:marble_cobble x1], check=OK"),
]

# ------------------------------------------- C: the no-TOOL context face (explosion semantics)
steps += [
    phase("C: the no-TOOL context — loot spawn consults the table without a TOOL param, the baseline answers"),
    Step(f"loot spawn {L_POS} loot gt6:blocks/marble_bricks", expect="Dropped 1 [Marble Bricks]"),
    Step(f"loot spawn {L_POS} loot gt6:blocks/marble", expect="Dropped 1 [Marble Cobblestone]"),
    Step(f"kill {ITEMS}", expect="Killed"),  # the two spawned stacks — the mine arms discard their own
]

# ---------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore over every site (the pass-open bbox is the backstop)"),
    Step("fill 387 62 123 395 66 129 air", expect="filled"),
    Step(f"kill {ITEMS}", expect="No entity was found"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p21-chisel-drop-conversion",
    slug="p21chiseldrops",
    sites=gt6world.declare_sites(M, S, N, V, L),
    preferred_ports=(26303, 26313),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
