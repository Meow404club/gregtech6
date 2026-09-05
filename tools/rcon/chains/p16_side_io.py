#!/usr/bin/env python3
"""p16-machine-side-io — the two-machine face-to-face push/pull arms, live (declarative framework).

Chain semantics (task p16-machine-side-io ACCEPTANCE): the fluid auto-IO arms of
TileEntityBasicMachine close a round trip over LIVE neighbor capabilities, no
command-arm on the fluid path:

  A the rig: two T1 Dryers face-to-face — B (100,64,99, facing north, the
    default) directly north of A (100,64,100). /data merge arms the
    registration-config keys the port reads but never persists (the upstream
    readFromNBT2 :132-158 family — the port form of the registry's placement-NBT
    re-feed, BlockDataAccessor.java:51-54 runs entity.load):
      A {facing:3, fluid_sides_auto_out:2}  — A turns south; its push-only face
        (relative left → world west, FACING_TO_SIDE[3][2]) points AWAY from B,
        leaving A's north face (mask 100 bit5 at facing 3) open for B's push;
      B {fluid_sides_auto_in:1, fluid_sides_auto_out:5} — B pulls from its top
        face, pushes through its back (world south) into A.

  B the :565 push-only refuse: with A's auto-output side configured, a west-face
    `fluid fill` is REJECTED even though the rotated row mask (100) admits that
    face — the mask gate passes, getFluidTankFillable's auto-output refuse leg
    returns null. The proof the refusal is the NEW leg, not the p14 mask.

  C NBT_TANK_CAPACITY (the ③ key): dryer C (120,64,120) merged
    {tank_capacity:5000} takes `fluid fill east 6000` as filled 5000/6000 —
    the :157-160 capacity re-arm applied before the content read, live.

  D the pull arm (:696-705): the wood barrel stacked on B (100,65,99) is FILLED
    through a /data merge of its tank NBT — the canned `gt6tank accept` is a
    fill-AND-drain-back scenario and leaves the barrel EMPTY (live-proven in the
    first run). The merge key shape is loader-versioned (node_cmds, the p12fic
    key-shape ruling): forge {FluidName, Amount} vs the 21.1 codec {id, amount};
    the judged expect stays byte-identical. The barrel is queried on its BOTTOM
    face (density>0 drains from the bottom — the P5 barrel side rules), B's input
    tank pulls to its 1000 L default capacity; the tick-END ledger reads 920 L —
    the pull refills to the 1000 L cap at the head of every checkRecipe beat and
    the 8-parallel Drying round then consumes 80 L (8 × 10) in the SAME beat, so
    920 is the stable sampled value (RCON answers run between ticks).

  E the round trip: fakesource on powers B; the LIVE Drying row (water 10 L →
    distilled water 8 L, EUt 16, t 16, 8-parallel) runs inside B — in[0] pinned
    at 1000 by the pull, out[0] produced in 64 L rounds, the :459/:994-996 push
    arm drains B's output tank through the back face into A's input tank until
    A pins at ITS 1000 L capacity (the partial final push proves the ledger is
    capability-exact). The verdict pins both tanks AT capacity, exactly:
    A in[0]=1000 L of gt6:distilled_water, B in[0]=1000 L of minecraft:water.

  F teardown: fakesource off (the global-state restore) + the explicit fill-air
    over both sites (the pass-open bbox cleanup is the backstop).

Run:  python3 tools/rcon/chains/p16_side_io.py
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

# The two face-to-face machines + the barrel stacked on B; the isolated
# tank_capacity probe dryer 20 blocks east.
POS_A = (100, 64, 100)
POS_B = (100, 64, 99)
POS_BARREL = (100, 65, 99)
POS_C = (120, 64, 120)

RIG = gt6world.Site(100, 64, 100, dx=0, dy=1, dz=1)   # A + B + the barrel above B
PROBE = gt6world.Site(120, 64, 120)

A = F(RIG)              # 100 64 100
B = "100 64 99"
BARREL = "100 65 99"
C = F(PROBE)            # 120 64 120

steps = []

# ------------------------------------------------- A: the rig + the NBT-config merges
steps += [
    phase("A: the rig — B north of A (face to face), the barrel stacked on B; the registration-config merges"),
    Step(f"gt6machine dryer place {A}", expect="GT6 dryer placed"),
    Step(f"gt6machine dryer place {B}", expect="GT6 dryer placed"),
    Step(f"setblock {BARREL} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6machine dryer place {C}", expect="GT6 dryer placed"),
    # A turns south and arms its push-only face (relative left → world west, AWAY from B)
    Step(f"data merge block {A} {{facing:3b, fluid_sides_auto_out:2b}}",
         expect="Modified block data"),
    # B: pull from the top (the barrel's bottom face), push through the back (into A's north)
    Step(f"data merge block {B} {{fluid_sides_auto_in:1b, fluid_sides_auto_out:5b}}",
         expect="Modified block data"),
    # the row geometry sanity, before anything moves (the p14 dryer row masks)
    Step(f"gt6machine dryer fluid stat {B}", expect="masks fluidIn=100 fluidOut=66 energyIn=65"),
    Step(f"gt6machine dryer fluid stat {B}", expect="faces fluidIn=south,east fluidOut=up energyIn=down"),
]

# ------------------------------------------------- B: the :565 push-only refuse
steps += [
    phase("B: the :565 leg — the mask admits A's west face, the auto-output config refuses the fill"),
    Step(f"gt6machine dryer fluid fill west minecraft:water 100 {A}",
         expect="filled 0/100 L of minecraft:water (REJECTED)"),
]

# ------------------------------------------------- C: NBT_TANK_CAPACITY live
steps += [
    phase("C: NBT_TANK_CAPACITY — the merged key re-arms the input tank before the content read"),
    Step(f"data merge block {C} {{tank_capacity:5000}}", expect="Modified block data"),
    Step(f"gt6machine dryer fluid fill east minecraft:water 6000 {C}",
         expect="filled 5000/6000 L of minecraft:water (ACCEPTED), input tanks hold 5000 L"),
]

# ------------------------------------------------- D: the water source + the pull arm
steps += [
    phase("D: the pull — the barrel feeds B's input tank through its bottom face, capped at 1000 L"),
    # fill the barrel through its own tank NBT (the canned gt6tank accept is a
    # fill-AND-drain-back scenario — live-proven it leaves the barrel empty); the
    # FluidStack key shape is loader-versioned, the expect is not (the p12fic ruling)
    Step(f"data merge block {BARREL} {{tank:{{FluidName:\"minecraft:water\",Amount:16000}}}}",
         expect="Modified block data",
         node_cmds={"1.21.1": f"data merge block {BARREL} {{tank:{{id:\"minecraft:water\",amount:16000}}}}"}),
    # no energy yet: the pull arm cannot fire (checkRecipe only beats with energy) —
    # the barrel stays intact and B stays empty
    Step(f"gt6machine dryer fluid stat {B}", expect="in[0]=0 L of nothing", sleep=2.0),
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    # the doActive re-check beat runs checkRecipe(T, T): the :696-705 pull drains the
    # barrel capability into B's input tank up to the 1000 L default capacity — the
    # tick-end ledger is 920 L (the same beat's 8-parallel round consumed 80 L, see
    # the phase doc above)
    Step(f"gt6machine dryer fluid stat {B}",
         expect="in[0]=920 L of minecraft:water", poll=60.0),
]

# ------------------------------------------------- E: production + the push arm round trip
steps += [
    phase("E: the round trip — B processes water→distw, the :994-996 push fills A to ITS capacity"),
    # the live Drying row (8-parallel, parallelDuration): 80 L water → 64 L distw per
    # 32-tick round at the fake source's 64 E/t; A receives in 64 L pushes until the
    # final partial push pins it at exactly 1000 L (capability-exact ledger)
    Step(f"gt6machine dryer fluid stat {A}",
         expect="in[0]=1000 L of gt6:distilled_water", poll=420.0),
    # B's input tank still rides the pull cycle at the 920 L tick-end ledger
    # (the barrel holds 16000 L, far from dry)
    Step(f"gt6machine dryer fluid stat {B}", expect="in[0]=920 L of minecraft:water"),
]

# ------------------------------------------------- F: teardown
steps += [
    phase("F: teardown — the global-state restore + the explicit fill-air (the bbox backstop)"),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),
    Step("fill 99 62 98 101 67 101 air", expect="filled"),
    Step(f"fill 119 62 119 121 66 121 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p16-machine-side-io",
    slug="p16sideio",
    sites=gt6world.declare_sites(RIG, PROBE),
    preferred_ports=(25773, 25783),      # this card's pinned rcon/query pair
    steps=steps,
    mutates=("fakesource",),
)


if __name__ == "__main__":
    main(CHAIN)
