#!/usr/bin/env python3
"""tank-valves — the Tank Main Valve chain (task tank-valves RCON).

The 25-valve family is a REGISTRATION + STRUCTURE face, so the live proof is the
hollow-wall form semantics and the tank physics, driven through the family command
(/gt6tankvalve) over the generic /gt6multiblock arms where they exist:

A. wood 3x3x3 forms over its 25 wood_wall shell cells (the design=wall-choice
   semantics live: the row's NBT_DESIGN wall is the shell material), takes 5000 L
   of water (the onlySimple gate passes — "water" is a SIMPLE name) and auto-emits
   into the barrel north of the valve (the :123-124 emit, horizontal facing);
B. the gas flag: a wood tank voids a natural_gas fill (gasProof F, content-only
   destruction) while the SS small tank keeps it (gasProof T);
C. the SS small dense? — no: the plain SS 5x5x5 forms over its 97 machine-wall
   cells (125 - 27 hollow - 1 controller), takes 100000 L, reports the 8M row
   capacity;
D. the wrong-wall arm: an SS valve with a WOOD shell refuses to form (the form
   rejection is the design=wall-choice semantics' live verdict);
E. the meltdown arm: a wood tank with 2000 L of lava (1300 K >= 500 K
   WoodTreated) leaves the :136 lava block in the hollow centre and the valve in
   fire (:138).

The destruction family runs its acid branch offline (the Categories.ACIDS
injection test); no acid fluid exists in the port to pour here (the declared
deviation), so the live protection pair rides the gas flag (arm B).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/tank.py
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

# the fresh z=294 band (card ① parts chain sits z=292): the six tank rigs on
# x-disjoint columns — 3x3x3 rigs 3 wide, the 5x5x5 rig 5 wide, 5+ spare columns
# between families.
Z = 294
Y = 65

V_WOOD = (384, Y, Z)          # arm A — the wood 3x3x3 + the auto-emit barrel
V_GAS_WOOD = (392, Y, Z)      # arm B1 — the unprotected gas arm (wood)
V_GAS_SS = (400, Y, Z)        # arm B2 — the protected gas arm (SS small)
V_LARGE_SS = (408, Y, Z)      # arm C — the SS 5x5x5 (spans x 406..410)
V_WRONG = (418, Y, Z)         # arm D — the wrong-wall rejection
V_MELT = (424, Y, Z)          # arm E — the meltdown


def centre(valve):
    """The structure centre of a north-facing valve: one cell SOUTH of it (the
    getOffsetXN subtraction — NORTH offset (0,0,-1) negated)."""
    return (valve[0], valve[1], valve[2] + 1)


def shell3(valve, block):
    """The 25 shell setblocks around the centre (26 band cells minus the hollow
    centre minus the valve's own cell — the self-cell arm)."""
    cx, cy, cz = centre(valve)
    out = []
    for i in (-1, 0, 1):
        for j in (-1, 0, 1):
            for k in (-1, 0, 1):
                if (i, j, k) == (0, 0, 0):
                    continue
                p = (cx + i, cy + j, cz + k)
                if p == valve:
                    continue
                out.append((p, block))
    return out


def shell5(valve, block):
    """The 97 shell setblocks of the 5x5x5 (125 cells - 27 hollow - 1 valve cell)."""
    cx, cy, cz = (valve[0], valve[1], valve[2] + 2)  # two cells south (the distance-2 anchor)
    out = []
    for i in (-2, -1, 0, 1, 2):
        for j in (-2, -1, 0, 1, 2):
            for k in (-2, -1, 0, 1, 2):
                if abs(i) <= 1 and abs(j) <= 1 and abs(k) <= 1:
                    continue  # the :66 inner hollow
                p = (cx + i, cy + j, cz + k)
                if p == valve:
                    continue
                out.append((p, block))
    return out


BARREL = (V_WOOD[0], V_WOOD[1], V_WOOD[2] - 1)  # north of the valve — the emit target (the facing direction)

steps = [
    phase("A: the wood 3x3x3 — form, fill, the auto-emit into the barrel"),
    Step(f"setblock {F(V_WOOD)} gt6:tank_wood", expect="Changed the block"),
    *[Step(f"setblock {F(p)} gt6:{b}", expect="Changed the block") for (p, b) in shell3(V_WOOD, "wood_wall")],
    Step(f"gt6tankvalve form {F(V_WOOD)}", expect="formed=true"),
    Step(f"gt6tankvalve stat {F(V_WOOD)}", expect="capacity=432000"),
    Step(f"gt6tankvalve fill {F(V_WOOD)} minecraft:water 5000", expect="filled 5000/5000 L"),
    Step(f"setblock {F(BARREL)} gt6:barrel_metal", expect="Changed the block"),
    Step(f"gt6tankvalve tick {F(V_WOOD)} 3", expect="tank=0/432000"),
    Step(f"gt6tank stat {F(BARREL)}", expect="5000/64000 L of minecraft:water"),

    phase("B: the gas flag — the unprotected wood voids, the gas-proof SS keeps"),
    Step(f"setblock {F(V_GAS_WOOD)} gt6:tank_wood", expect="Changed the block"),
    *[Step(f"setblock {F(p)} gt6:{b}", expect="Changed the block") for (p, b) in shell3(V_GAS_WOOD, "wood_wall")],
    Step(f"gt6tankvalve form {F(V_GAS_WOOD)}", expect="formed=true"),
    Step(f"gt6tankvalve fill {F(V_GAS_WOOD)} gt6:natural_gas 2000", expect="filled 2000/2000 L"),
    Step(f"gt6tankvalve tick {F(V_GAS_WOOD)} 2", expect="tank=0/432000"),
    Step(f"execute if block {F(V_GAS_WOOD)} gt6:tank_wood", expect="Test passed"),
    Step(f"setblock {F(V_GAS_SS)} gt6:tank_small_stainless_steel", expect="Changed the block"),
    *[Step(f"setblock {F(p)} gt6:{b}", expect="Changed the block") for (p, b) in shell3(V_GAS_SS, "machine_wall_stainless_steel")],
    Step(f"gt6tankvalve form {F(V_GAS_SS)}", expect="formed=true"),
    Step(f"gt6tankvalve fill {F(V_GAS_SS)} gt6:natural_gas 2000", expect="filled 2000/2000 L"),
    Step(f"gt6tankvalve tick {F(V_GAS_SS)} 2", expect="tank=2000/1728000"),

    phase("C: the SS 5x5x5 — the 97-wall form and the 8M row capacity"),
    Step(f"setblock {F(V_LARGE_SS)} gt6:tank_large_stainless_steel", expect="Changed the block"),
    *[Step(f"setblock {F(p)} gt6:{b}", expect="Changed the block") for (p, b) in shell5(V_LARGE_SS, "machine_wall_stainless_steel")],
    Step(f"gt6tankvalve form {F(V_LARGE_SS)}", expect="formed=true"),
    Step(f"gt6tankvalve stat {F(V_LARGE_SS)}", expect="capacity=8000000"),
    Step(f"gt6tankvalve fill {F(V_LARGE_SS)} minecraft:water 100000", expect="filled 100000/100000 L"),

    phase("D: the wrong-wall arm — the SS valve over a wood shell refuses"),
    Step(f"setblock {F(V_WRONG)} gt6:tank_small_stainless_steel", expect="Changed the block"),
    *[Step(f"setblock {F(p)} gt6:{b}", expect="Changed the block") for (p, b) in shell3(V_WRONG, "wood_wall")],
    Step(f"gt6tankvalve form {F(V_WRONG)}", expect="formed=false"),

    phase("E: the meltdown — lava over the wood melting point"),
    Step(f"setblock {F(V_MELT)} gt6:tank_wood", expect="Changed the block"),
    *[Step(f"setblock {F(p)} gt6:{b}", expect="Changed the block") for (p, b) in shell3(V_MELT, "wood_wall")],
    Step(f"gt6tankvalve form {F(V_MELT)}", expect="formed=true"),
    Step(f"gt6tankvalve fill {F(V_MELT)} minecraft:lava 2000", expect="filled 2000/2000 L"),
    # NO manual tick step here: the LIVE dispatcher melts the tank down within the
    # server ticks between two commands (live-proven: the manual-tick probe came back
    # "No GTTankValveBlockEntity" while the lava/fire assertions below passed — the
    # valve's own BE was already consumed by its :130-140 meltdown).
    Step(f"execute if block {F((V_MELT[0], V_MELT[1], V_MELT[2] + 1))} minecraft:lava", expect="Test passed"),
    Step(f"execute if block {F(V_MELT)} minecraft:fire", expect="Test passed"),

    phase("F: teardown — restore the band"),
    Step(f"fill {V_WOOD[0] - 3} {Y - 3} {Z - 3} {V_MELT[0] + 3} {Y + 4} {Z + 4} air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="tank_valves",
    slug="tankvalves",
    sites=gt6world.declare_sites(
        gt6world.Site(*V_WOOD, dx=2, dz=2),
        gt6world.Site(*V_GAS_WOOD, dx=2, dz=2),
        gt6world.Site(*V_GAS_SS, dx=2, dz=2),
        gt6world.Site(*V_LARGE_SS, dx=3, dz=3),
        gt6world.Site(*V_WRONG, dx=2, dz=2),
        gt6world.Site(*V_MELT, dx=2, dz=2),
        gt6world.Site(*BARREL),
    ),
    steps=steps,
)

if __name__ == "__main__":
    main()
