#!/usr/bin/env python3
"""grass-block — the GT grass family live acceptance chain (the P23 barrel chain shape).

Chain semantics (task grass-block RCON arm): the spray-can GRASS route is LIVE —
the /gt6grass command drives the same colorTarget/decolorTarget verdicts the
GTSprayCanItem.useOn :216-221 server half runs (no player-click RCON seam exists; the
command IS the acceptance channel, the GTBurnerCommand ruling).

  A the six effective dyes (Behavior_Spray_Color.java:154-160 order) recolour a fresh
    vanilla grass_block into exactly the six variants — landed=gt6:grass{,_lime,_black,
    _light_gray,_yellow,_brown} pinned per dye 2/10/0/7/11/3.

  B the family recolour: an already-coloured GT variant takes another effective dye
    (the upstream :164 family face) and lands the new variant.

  C the same-variant no-op: the tenth-dye face — the pinned UNPAID report
    (remaining=5120, the useOn null-target precheck :216-217 never pays).

  D the ten no-op dyes: two spot dyes (Red 1, White 15) on a GT variant AND the
    vanilla block — every one reports NO-OP unpaid.

  E the remover: any GT variant → the vanilla grass block (the Remover :104 swap);
    the vanilla block itself is NOT removable (no reverse row).

  F the negative: stone never enters the grass route (NO-OP unpaid).

Two framework passes are the [0, 0] idempotency proof (every arm re-lays its block
first, so the pinned verdicts hold on both passes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/grass_block.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x 392..394, z 124: clear of the P21 paintable shredder (x 382), the P23
# barrel band (x 388..390) and the P24 dye barrel (x 386, z 20).
V = gt6world.Site(392, 64, 124, dx=1, dy=1, dz=1)   # the vanilla grass_block arms
G = gt6world.Site(393, 64, 124, dx=1, dy=1, dz=1)   # the GT-variant arms
X = gt6world.Site(394, 64, 124, dx=1, dy=1, dz=1)   # the negative arm

VP = "392 64 124"
GP = "393 64 124"
XP = "394 64 124"

steps = []

# --------------------------------- A: the six effective dyes on a vanilla grass block
steps += [
    phase("A: dye 2/10/0/7/11/3 -> exactly the six variants (Behavior_Spray_Color.java:154-160)"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 2", expect="dye 2 hit=true landed=gt6:grass"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 10", expect="dye 10 hit=true landed=gt6:grass_lime"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 0", expect="dye 0 hit=true landed=gt6:grass_black"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 7", expect="dye 7 hit=true landed=gt6:grass_light_gray"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 11", expect="dye 11 hit=true landed=gt6:grass_yellow"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 3", expect="dye 3 hit=true landed=gt6:grass_brown"),
]

# ------------------------------------------------- B: the family recolour on a GT variant
steps += [
    phase("B: the family face — gt6:grass_lime takes Black then LightGray"),
    Step(f"setblock {GP} gt6:grass_lime", expect="Changed the block"),
    Step(f"gt6grass spray {GP} 0", expect="dye 0 hit=true landed=gt6:grass_black"),
    Step(f"gt6grass spray {GP} 7", expect="dye 7 hit=true landed=gt6:grass_light_gray"),
]

# ------------------------------------------------- C: the same-variant no-op (unpaid)
steps += [
    phase("C: the same-variant no-op — LightGray again, remaining stays 5120 (unpaid)"),
    Step(f"gt6grass spray {GP} 7", expect="NO-OP at 393, 64, 124: dye 7 remaining=5120 (unpaid)"),
]

# ------------------------------------------------- D: the ten no-op dyes (spot: Red 1, White 15)
steps += [
    phase("D: the no-op dyes — Red and White on a GT variant and on the vanilla block, all unpaid"),
    Step(f"gt6grass spray {GP} 1", expect="NO-OP at 393, 64, 124: dye 1 remaining=5120 (unpaid)"),
    Step(f"gt6grass spray {GP} 15", expect="NO-OP at 393, 64, 124: dye 15 remaining=5120 (unpaid)"),
    Step(f"setblock {VP} minecraft:grass_block", expect="Changed the block"),
    Step(f"gt6grass spray {VP} 1", expect="NO-OP at 392, 64, 124: dye 1 remaining=5120 (unpaid)"),
]

# ------------------------------------------------- E: the remover (the Remover :104 swap)
steps += [
    phase("E: the remover — a GT variant unpaints to the vanilla grass block; the vanilla block itself is not removable"),
    Step(f"setblock {GP} gt6:grass_yellow", expect="Changed the block"),
    Step(f"gt6grass unpaint {GP}", expect="hit=true landed=minecraft:grass_block"),
    Step(f"gt6grass unpaint {GP}", expect="NO-OP at 393, 64, 124: not removable remaining=2560 (unpaid)"),
]

# ------------------------------------------------- F: the negative — stone never routes
steps += [
    phase("F: the negative — stone is outside the whitelist"),
    Step(f"setblock {XP} minecraft:stone", expect="Changed the block"),
    Step(f"gt6grass spray {XP} 2", expect="NO-OP at 394, 64, 124: dye 2 remaining=5120 (unpaid)"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step(f"setblock {VP} air", expect="Changed the block"),
    Step(f"setblock {GP} air", expect="Changed the block"),
    Step(f"setblock {XP} air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="grass-block",
    slug="grassblock",
    sites=gt6world.declare_sites(V, G, X),
    preferred_ports=(26107, 26117),      # this card's pinned rcon/query pair (the 2610x segment, after dyechemical 26106/26116)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
