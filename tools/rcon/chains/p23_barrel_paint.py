#!/usr/bin/env python3
"""p23-barrel-paint-render — the barrel paint consumer-face live acceptance chain.

Chain semantics (task p23-barrel-paint-render ACCEPTANCE ④): the /gt6machine paint
write arm is UNCHANGED — the paint capability already rode the 03 base into every
barrel BE (TileEntityBase08Barrel extends TileEntityBase03TicksAndSync, IPaintableTE),
and this card only added the render consumption face (the tinted single-element
models + the Block/Item Color double registration over GTBarrels.paintableBlockArray
16). The server-side report text therefore matches the p21 machine chain verbatim:

  A the wood barrel (gt6:barrel_wood): spray dye 1 (Red #FF0000) — the report pins
    RGB #FFFFFF->#FF0000 painted=true (APPLIED); the SAME colour again is the
    upstream no-op; the unpaint returns to white painted=false.

  B the family rides: plastic canister + one high-tier drum (the shared
    barrel_metal.png carrier) each take one paint/unpaint round — the registration
    census covers all 16 blocks, the chain spot-checks two more rows.

  C the negative: a vanilla block carries no IPaintableTE face — "No paintable GT6
    TileEntity" (an expected-failure step, allow_failed).

Two framework passes are the [0, 0] idempotency proof (every spray lands on a
fresh default barrel each pass, so the pinned #old values hold on both passes).
Run with GT6_SESSION=off (the pinned per-chain ports; the session model ignores
chain.preferred_ports — the p16-aqua-fluids lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p23_barrel_paint.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x 388..390, z 124: clear of the p21 paintable shredder (x 382), the
# p13/p14 boiler chains (x 104..172), the p16 chains (x 184..200 / x 300..360), the
# p19 drying rig (x 360, z 20) and the p19 chisel chain (x 372..378, z 124..130).
W = gt6world.Site(388, 64, 124)   # barrel_wood
P = gt6world.Site(389, 64, 124)   # barrel_plastic
D = gt6world.Site(390, 64, 124)   # barrel_infinity (the top drum row)

WP = "388 64 124"
PP = "389 64 124"
DP = "390 64 124"

steps = []

# --------------------------------- A: the wood barrel — the same report as the p21 machine chain
steps += [
    phase("A: barrel_wood — dye 1 (Red #FF0000) lands verbatim, the same spray is the no-op"),
    Step(f"setblock {WP} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6machine paint {WP} 1",
         expect="dye 1 (Red #FF0000), RGB #FFFFFF->#FF0000 painted=true (APPLIED)"),
    Step(f"gt6machine paint {WP} 1",
         expect="RGB #FF0000->#FF0000 painted=true (NO-OP)"),
    Step(f"gt6machine unpaint {WP}",
         expect="dye none (unpaint), RGB #FF0000->#FFFFFF painted=false (APPLIED)"),
]

# ------------------------------------------------- B: the family rides — plastic + a top drum
steps += [
    phase("B: the family rides — barrel_plastic and barrel_infinity each take a paint/unpaint round"),
    Step(f"setblock {PP} gt6:barrel_plastic", expect="Changed the block"),
    Step(f"gt6machine paint {PP} 14",
         expect="dye 14 (Orange #FF8000), RGB #FFFFFF->#FF8000 painted=true (APPLIED)"),
    Step(f"gt6machine unpaint {PP}",
         expect="dye none (unpaint), RGB #FF8000->#FFFFFF painted=false (APPLIED)"),
    Step(f"setblock {DP} gt6:barrel_infinity", expect="Changed the block"),
    Step(f"gt6machine paint {DP} 5",
         expect="dye 5 (Purple #800080), RGB #FFFFFF->#800080 painted=true (APPLIED)"),
    Step(f"gt6machine unpaint {DP}",
         expect="dye none (unpaint), RGB #800080->#FFFFFF painted=false (APPLIED)"),
]

# -------------------------------------------------- C: the non-paintable negative
steps += [
    phase("C: the negative — a vanilla block carries no IPaintableTE face, the spray refuses"),
    Step(f"setblock {WP} minecraft:stone", expect="Changed the block"),
    Step(f"gt6machine paint {WP} 5", expect="No paintable GT6 TileEntity", allow_failed=True),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step(f"setblock {WP} air", expect="Changed the block"),
    Step(f"setblock {PP} air", expect="Changed the block"),
    Step(f"setblock {DP} air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p23-barrel-paint",
    slug="p23barrelpaint",
    sites=gt6world.declare_sites(W, P, D),
    preferred_ports=(26503, 26513),      # this card's pinned rcon/query pair (P23 segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
