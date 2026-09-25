#!/usr/bin/env python3
"""c-foam-block-family — the C-Foam BLOCK family live chain (declarative framework,
the cfoam_spray shape).

Chain semantics (task c-foam-block-family ACCEPTANCE — "喷管面回归不破（P25 链绿）
+ 喷块面 fresh 出现→排干 dried→owned 拆除门 双腿 [0,0]"):

  A the plain spray arm: `spray <pos> <face> <owned> [dye]` drives the SAME static face
    the item's useOn calls (GT6FoamPlacement.foamArm mode 0 over GT6FoamSprayItem.liveSink
    — upstream Behavior_Spray_Foam :137-138). The fresh block appears, the colour rides
    the blockstate (stat 断言), and `dry <pos>` drives the SAME dryFoam face the
    scheduled tick calls (upstream BlockCFoamFresh :110-112 — SIDE_ANY → the full dried
    block, the colour preserved). The plain removeFoam clears the cell (upstream :115-117).

  B the slab leg (the ruling_slab form): `mode <pos> <face> <playerSide> 3` places the
    BOTTOM half (OPOS[UP]=BOTTOM — the slab rests on the clicked floor), the dry carries
    the half AND the colour into the dried slab, the remove clears it.

  C the mode arm live proof (SPEC pin ①): `mode ... 2` lays the 3x3 plane (9 landed,
    90 units) over a DOWN player side — the plane perpendicular to Y at the origin row.

  D the owned chain (the THREE-CLAUSE gate live): `spray <pos> 1 <dye> <uuid>` lands the
    cfoam_owned carrier + the :81-83 NBT bundle (ownable/owner/paint); the wet owned foam
    passes everyone, `dry` arms the lock (upstream :118-123 no-gate), `removefoam` from
    the console (null = non-owner) is REJECTED (upstream :126-130), the owner removes.

passes=2 is the idempotency proof (the [0,0] of this chain); the pass-open bbox cleanup
re-airs the band between passes.

Run:  GT6_SESSION=off python3 tools/rcon/chains/cfoam_blocks.py
Dual: python3 tools/rcon/chains/cfoam_blocks.py --node 1.21.1-neoforge
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

# the lock identity — a fixed, log-readable UUID (the console carries null; this is the
# sprayer/owner the chain records)
UUID_A = "11111111-2222-3333-4444-666666666666"

# the x452..457 z20 band — clear of the neighbours (the P26 refill canners 412/414, the
# item-pipe band 434..443 claimed by its card state, the P16 scaffold at z440/450)
S = gt6world.Site(453, 64, 20, dx=2, dz=2)   # the plain + slab + plane subject band
O = gt6world.Site(457, 64, 20)               # the owned subject
SUPPORT, SU = F(S), F((S.x, S.y + 1, S.z))
OU = F((O.x, O.y + 1, O.z))
PLANE_C = F((S.x, S.y + 1, S.z))             # the 3x3 plane CLICKED cell (the origin rides one UP)
PLANE_A = F((S.x - 1, S.y + 2, S.z - 1))     # the plane corner (the stat probe — y+2: origin = clicked+face)
PLANE_B = F((S.x + 1, S.y + 2, S.z + 1))     # the opposite corner

steps = []

# ------------------------------------------------- A: the plain spray arm (fresh → dried → air)
steps += [
    phase("A: spray — the fresh block appears (mode 0 over the shared liveSink), the colour rides the blockstate"),
    Step(f"setblock {SUPPORT} minecraft:stone", expect="Changed"),
    Step(f"gt6cfoam spray {SUPPORT} 1 0", expect="1 landed (10 units)"),
    Step(f"gt6cfoam stat {SU}", expect="block gt6:cfoam_fresh color 0"),
    Step(f"gt6cfoam spray {SUPPORT} 1 0 3", expect="0 landed (0 units)"),  # the occupied origin cell — the sink air gate
    Step(f"gt6cfoam removefoam {SU}", expect="ok, foam removed"),
    Step(f"gt6cfoam spray {SUPPORT} 1 0 3", expect="1 landed (10 units)"),  # dye index 3
    Step(f"gt6cfoam stat {SU}", expect="block gt6:cfoam_fresh color 3"),
    phase("A2: dry — the same dryFoam face the scheduled tick drives; the colour carries into the dried block"),
    Step(f"gt6cfoam dry {SU}", expect="ok, now gt6:cfoam"),
    Step(f"gt6cfoam stat {SU}", expect="block gt6:cfoam color 3"),
    Step(f"gt6cfoam removefoam {SU}", expect="ok, foam removed"),
    Step(f"gt6cfoam stat {SU}", expect="block minecraft:air"),
]

# ------------------------------------------------- B: the slab leg (the ruling_slab form)
steps += [
    phase("B: slab — mode 3 places the BOTTOM half, the dry carries the half AND the colour"),
    Step(f"gt6cfoam mode {SUPPORT} 1 0 3 5", expect="1 landed (5 units)"),
    Step(f"gt6cfoam stat {SU}", expect="block gt6:cfoam_fresh_slab color 5 slab bottom"),
    Step(f"gt6cfoam dry {SU}", expect="ok, now gt6:cfoam_slab"),
    Step(f"gt6cfoam stat {SU}", expect="block gt6:cfoam_slab color 5 slab bottom"),
    Step(f"gt6cfoam removefoam {SU}", expect="ok, foam removed"),
]

# ------------------------------------------------- C: the mode arm live proof (the 3x3 plane)
steps += [
    phase("C: mode 2 — the 3x3 plane over a DOWN player side (9 landed, 90 units), the corners are fresh foam"),
    Step(f"fill {S.x - 1} {S.y - 1} {S.z - 1} {S.x + 1} {S.y - 1} {S.z + 1} minecraft:stone", expect="filled"),
    Step(f"gt6cfoam mode {PLANE_C} 1 0 2 8", expect="9 landed (90 units)"),
    Step(f"gt6cfoam stat {PLANE_A}", expect="block gt6:cfoam_fresh color 8"),
    Step(f"gt6cfoam stat {PLANE_B}", expect="block gt6:cfoam_fresh color 8"),
    Step(f"fill {S.x - 1} {S.y + 1} {S.z - 1} {S.x + 1} {S.y + 1} {S.z + 1} air", expect="filled"),
    Step(f"fill {S.x - 1} {S.y - 1} {S.z - 1} {S.x + 1} {S.y - 1} {S.z + 1} air", expect="filled"),
]

# ------------------------------------------------- D: the owned chain (the three-clause gate live)
steps += [
    phase("D: owned — the carrier + the NBT bundle; the wet foam passes everyone, the dry arms the lock"),
    Step(f"setblock {F(O)} minecraft:stone", expect="Changed"),
    Step(f"gt6cfoam spray {F(O)} 1 1 5 {UUID_A}", expect="1 landed (10 units)"),
    Step(f"gt6cfoam stat {OU}", expect=f"dried false foamDried false ownable true owner {UUID_A}"),
    Step(f"gt6cfoam dry {OU}", expect="ok, dried true"),
    Step(f"gt6cfoam stat {OU}", expect=f"dried true foamDried true ownable true owner {UUID_A}"),
    Step(f"gt6cfoam removefoam {OU}", expect="REJECTED", allow_failed=True),  # the console is not the owner (upstream :126-130)
    Step(f"gt6cfoam stat {OU}", expect=f"dried true foamDried true ownable true owner {UUID_A}"),
    Step(f"gt6cfoam removefoam {OU} {UUID_A}", expect="ok, foam removed"),
    Step(f"gt6cfoam stat {OU}", expect="block minecraft:air"),
]

# ------------------------------------------------- teardown
steps += [
    phase("E: teardown — the explicit restore over the band (the pass-open bbox is the backstop)"),
    Step(f"fill {S.x - 2} {S.y - 1} {S.z - 2} {S.x + 2} {S.y + 2} {S.z + 2} air", expect="filled"),
    Step(f"fill {O.x - 1} {O.y - 1} {O.z - 1} {O.x + 1} {O.y + 2} {O.z + 1} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="cfoam-blocks",
    slug="cfoamblocks",
    sites=gt6world.declare_sites(S, O),
    preferred_ports=(26150, 26160),      # this card's pinned rcon/query pair (after the refill 2612x + the parallel-session 26130x bumps)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
