#!/usr/bin/env python3
"""paintable-storage-sync — the machine paint write-point live acceptance chain.

Chain semantics (task paintable-storage-sync ACCEPTANCE, ADR
2026-09-07-paintable-rulings ruling 1: the spray write-point rides the SAME server
IPaintableTE API the offline GTPaintableTest drives — the /gt6machine paint arm, the
P19 chisel precedent):

  A the direct store (upstream Paintable:85 + the 04:227-235 unpainted half): place
    a shredder, spray dye 1 (Red #FF0000) — the report pins
    RGB #FFFFFF->#FF0000 painted=true (APPLIED); the SAME colour again is the
    upstream no-op (RGB #FF0000->#FF0000 ... NO-OP).

  B the channel-average mix (the GT6 semantics, UT.java:1576-1578; the GTCEu
    overwrite deviation is NOT adopted): spray dye 14 (Orange #FF8000) onto the red
    machine — mix(255,0,0, 255,128,0) = (255,64,0), pinned as
    RGB #FF0000->#FF4000 painted=true (APPLIED).

  C the unpaint (Paintable:83 shape): dye none — unpaint restores the ROW MATERIAL
    colour (machine-material-tint-fidelity f9411efa reshaped unpaint to
    materialColor(materialOf); the old white-return deviation is REVOKED, so the
    former #FFFFFF pins here are obsolete). This chain's shredder is the Kinetic_T[1]
    row (Bronze, MT.java:1705 (210,130,60) = #D2823C, fRGBaSolid via
    GTBasicMachineBlock.materialColor UT.Code.getRGBInt): pinned as
    RGB #FF4000->#D2823C painted=false (APPLIED); the second unpaint is the
    #D2823C->#D2823C no-op.

  D the negative: a plain vanilla block (no IPaintableTE) refuses the spray —
    "No paintable GT6 TileEntity" (an expected-failure step, allow_failed).

The two framework passes are the [0, 0] idempotency proof (every spray lands on a
fresh default machine each pass, so the pinned #old values hold on both passes).
Run with GT6_SESSION=off (the pinned per-chain ports; the session model ignores
chain.preferred_ports — the aqua-fluids lesson).

Run:  GT6_SESSION=off python3 tools/rcon/chains/paintable.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — x 382, z 124: clear of the P13/P14 boiler chains (x 104..172), the P16
# chains (x 184..200 / x 300..360), the P19 drying rig (x 360, z 20) and the P19
# chisel chain (x 372..378, z 124..130).
G = gt6world.Site(382, 64, 124, dx=1)

P = "382 64 124"

steps = []

# --------------------------------- A: the direct store + the same-colour no-op (:85)
steps += [
    phase("A: the direct store — dye 1 (Red #FF0000) lands verbatim, the same spray is the :85 no-op"),
    Step(f"gt6machine shredder place {P}", expect="placed at 382, 64, 124"),
    Step(f"gt6machine paint {P} 1",
         expect="dye 1 (Red #FF0000), RGB #FFFFFF->#FF0000 painted=true (APPLIED)"),
    Step(f"gt6machine paint {P} 1",
         expect="RGB #FF0000->#FF0000 painted=true (NO-OP)"),
]

# ------------------------------------- B: the channel-average mix (UT.java:1576-1578)
steps += [
    phase("B: the GT6 mix — Orange onto Red averages the channels: 255,0,0 + 255,128,0 -> 255,64,0"),
    Step(f"gt6machine paint {P} 14",
         expect="dye 14 (Orange #FF8000), RGB #FF0000->#FF4000 painted=true (APPLIED)"),
]

# ------------------------------------------- C: the unpaint + the second-spray no-op
steps += [
    # unpaint restores the row material colour (Paintable:83 via materialColor, the
    # f9411efa tint-fidelity reshape) — NOT white; the shredder row is Bronze #D2823C.
    phase("C: the unpaint — back to the row material colour (Bronze #D2823C), painted=false; the second call is the no-op"),
    Step(f"gt6machine unpaint {P}",
         expect="dye none (unpaint), RGB #FF4000->#D2823C painted=false (APPLIED)"),
    Step(f"gt6machine unpaint {P}",
         expect="RGB #D2823C->#D2823C painted=false (NO-OP)"),
]

# -------------------------------------------------- D: the non-paintable negative
steps += [
    phase("D: the negative — a vanilla block carries no IPaintableTE face, the spray refuses"),
    Step(f"setblock {P} minecraft:stone", expect="Changed the block"),
    Step(f"gt6machine paint {P} 5", expect="No paintable GT6 TileEntity", allow_failed=True),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step(f"setblock {P} air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="paintable",
    slug="paintable",
    sites=gt6world.declare_sites(G),
    preferred_ports=(26103, 26113),      # this card's pinned rcon/query pair (P21 segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
