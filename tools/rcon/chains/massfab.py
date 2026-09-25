#!/usr/bin/env python3
"""massfab — the Matter Fabricator chain (task massfab, the massfab sweep
group; Loader_MultiTileEntities.java:1241/:1542-1546).

  the large 17199 controller: 98 dense-lead walls (18031) + 26 osmium coils (18044) +
  the air core + 16 vents (18299) + the versatile PU (18200) + the 4+4 Control(18202)/
  Conversion(18204) quota ring — the controller self-cell is the bottom-centre, the
  structure sits BEHIND the north-facing front (z+0..z+4). The hand-written walk has NO
  declared pattern, so forming rides the onTickFirst forced check: place the controller
  LAST (the vanilla build flow); the wrong-part rejection re-places the controller.

  the element-disintegration walk (GT6RecipesMassfab, Loader_Recipes_Other.java:969-987):
  64 iron ingots -> chargedmatter 64x26 = 1664 mB + neutralmatter 64x30 = 1920 mB — the
  PARALLEL 64 face (the :1241 NBT_PARALLEL_DURATION T skips the :743 bind). The drive is
  the per-instance `fake_source` flag (B) + the class-side fakesource regime (D) — the
  Graagg gt.energy NBT merge was retired after proving environmentally unreliable on
  the formed rig (the energy write intermittently never landed while every other key
  did) — see phase B's block comment for the full mechanics.

  the auto-out bottom face (:258-265): a small massfab (T5) sits BELOW the controller;
  its UP fluid face accepts the matter — the charged tank drains 1000 mB into it (the
  default input-tank capacity), leaving 664 in the controller tank; the neutral tank has
  no second sink and stays 1920.

  the small form (20415): 1 iron ingot -> chargedmatter 26 + neutralmatter 30 parked in
  its output_fluids buffer (no sink below the rig; the auto-out face would push them to
  a handler that exists — the C-phase sink proves the accept face).
  (T5 = INPUT 8192 window, efficiency 10000, no overclock).

acceptance arms this chain carries:
  1 the wrong-part rejection (formed=false) and the all-green form (formed=true, the
    FORMED blockstate assert)
  2 the element walk at parallel 64: maxprogress 469762048, chargedmatter 1664 total
    (664 after the 1000 mB sink drain), neutralmatter 1920
  3 the auto-out bottom face: the sink holds chargedmatter 1000
  4 the small T5 run: 1 ingot -> chargedmatter 26 + neutralmatter 30 in output_fluids

Run:  GT6_SESSION=off python3 tools/rcon/chains/massfab.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

CTRL = gt6world.Site(401, 65, 438)                 # the controller: the bottom-centre wall cell
SINK = gt6world.Site(401, 64, 438)                 # the small massfab BELOW (the auto-out sink)
T5 = gt6world.Site(406, 65, 438)                   # the small-form T5 run rig
BAND = (396, 60, 436, 410, 74, 446)                # the fresh band (x396..410, y60..74, z436..446)
C = gt6world.fmt(CTRL)
S = gt6world.fmt(SINK)
T = gt6world.fmt(T5)

WALL = "gt6:dense_wall_lead"
COIL = "gt6:large_osmium_coil"
VENT = "gt6:ventilation_unit"
PU_V = "gt6:processor_unit_versatile"
PU_C = "gt6:processor_unit_control"
PU_X = "gt6:processor_unit_conversion"
BIG = "gt6:large_massfab"
SMALL = "gt6:massfab_t5"

WRONG = "403 65 442"  # the corner wall cell (dy0) for the rejection arm


def merge_inventory(size, items):
    """The inventory data merge — the count key forks per leg (Count b / count)."""
    return {
        k: "data merge block " + C + " {inventory:{Size:%d,Items:[%s]}}" % (
            size, ",".join(
                '{Slot:%db,id:"%s",%s:%d%s}' % (slot, iid, "Count" if k == "1.20.1" else "count", n, "b" if k == "1.20.1" else "")
                for slot, (iid, n) in items))
        for k in ("1.20.1", "1.21.1")
    }


def small_merge(items):
    """The small-machine inventory merge (3 slots = items 2/1/0 map shape)."""
    return {
        k: "data merge block " + T + " {inventory:{Size:3,Items:[%s]}}" % ",".join(
            '{Slot:%db,id:"%s",%s:%d%s}' % (slot, iid, "Count" if k == "1.20.1" else "count", n, "b" if k == "1.20.1" else "")
            for slot, (iid, n) in items)
        for k in ("1.20.1", "1.21.1")
    }


# the vanilla `data get` print is the SNBT spaced `key: value` form on both legs
# (SnbtPrinterTagVisitor: NAME_VALUE_SEPARATOR + " ", ELEMENT_SEPARATOR + " ") — the
# tight `{k:v}` form never matches. The leg-neutral assert rides the GT6 contract pair
# FluidName/Amount — live-proven on both legs: forge stores {FluidName, Amount}; 21.1
# stores {amount, FluidName, Amount, id} (codec face + the ADR-P15-1 contract keys)
# and still prints the pair contiguously. (tank_key is vestigial in the call sites.)
def tank_expect(node_key, tank_key, fluid, amount):
    return "FluidName: \"%s\", Amount: %d" % (fluid, amount)


steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload add 396 436 410 446"),                       # the tick driver
    # the 98 walls (dy0..dy4, the controller cell re-set below)
    Step("fill 399 65 438 403 65 442 " + WALL, expect="filled"),
    # dy1..dy3: the 16-wall rings + the 9/8/9 coils
    Step("fill 399 66 438 403 66 442 " + WALL, expect="filled"),
    Step("fill 400 66 439 402 66 441 " + COIL, expect="filled"),
    Step("fill 399 67 438 403 67 442 " + WALL, expect="filled"),
    Step("fill 400 67 439 402 67 441 " + COIL, expect="filled"),
    Step("setblock 401 67 440 air", expect="Changed the block"),  # the :114 core centre
    Step("fill 399 68 438 403 68 442 " + WALL, expect="filled"),
    Step("fill 400 68 439 402 68 441 " + COIL, expect="filled"),
    # dy4: the last 25 walls
    Step("fill 399 69 438 403 69 442 " + WALL, expect="filled"),
    # dy5: the 16 vents + the versatile centre + the 4+4 quota ring
    Step("fill 399 70 438 403 70 442 " + VENT, expect="filled"),
    Step("setblock 401 70 440 " + PU_V, expect="Changed the block"),
    Step("setblock 400 70 439 " + PU_C, expect="Changed the block"),
    Step("setblock 401 70 439 " + PU_C, expect="Changed the block"),
    Step("setblock 402 70 439 " + PU_C, expect="Changed the block"),
    Step("setblock 400 70 440 " + PU_C, expect="Changed the block"),
    Step("setblock 402 70 440 " + PU_X, expect="Changed the block"),
    Step("setblock 400 70 441 " + PU_X, expect="Changed the block"),
    Step("setblock 401 70 441 " + PU_X, expect="Changed the block"),
    Step("setblock 402 70 441 " + PU_X, expect="Changed the block"),
    # the wrong-part arm: a stone corner wall, then the controller (placed LAST — the
    # onTickFirst forced check is the forming path of a hand-written walk)
    Step("setblock " + WRONG + " minecraft:stone", expect="Changed the block"),
    Step("setblock " + C + " " + BIG, expect="Changed the block"),
    Step("execute if block " + C + " gt6:large_massfab[formed=false]", expect="Test passed", poll=30),
    # the fix arm: the right wall back, the controller re-placed (the fresh onTickFirst)
    Step("setblock " + WRONG + " " + WALL, expect="Changed the block"),
    Step("setblock " + C + " air", expect="Changed the block"),
    Step("setblock " + C + " " + BIG, expect="Changed the block"),
    Step("execute if block " + C + " gt6:large_massfab[formed=true]", expect="Test passed", poll=30),

    phase("B: the element walk at parallel 64 — 64 iron ingots on the fake_source drive"),
    Step("setblock " + S + " " + SMALL, expect="Changed the block"),  # the auto-out sink BELOW
    # THE CONTROLLER DRIVE IS THE PER-INSTANCE fake_source FLAG, NOT AN NBT ENERGY
    # MERGE: two live-proven dead-ends killed the merge drive. (a) the :798 scan gate
    # is (mIgnited>0 || mInventoryChanged || !mRunning || aTimer%1200==5) and the
    # inventory merge does NOT set mInventoryChanged — Forge ItemStackHandler
    # .deserializeNBT calls onLoad(), not onContentsChanged() — so the gate rides
    # !mRunning which the energy merge's first tick consumes empty, and the
    # :790-791 unconditional idle burn (mInputMax/t) eats any merged budget in
    # energy/mInputMax ticks. (b) worse, the formed rig intermittently NEVER applies
    # the energy merge at all (inventory merge sticks, energy reads 0L forever —
    # forgeH/K/L/M, across a fresh world + clean windows). The instance flag
    # (TileEntityBase10MultiBlockMachine.mFakeSource) rides the SAME reliable
    # load face as the inventory (non-energy keys always stuck) and refills
    # mInputMax/t in doWork on EVERY tick — progress 2 M/t completes the 64-row
    # walk (469762048) in 224 ticks with zero race surface. Restored by teardown.
    Step("data merge block " + C + " {fake_source:1b}", expect="Modified block data"),
    Step(merge_inventory(11, [(0, ("minecraft:iron_ingot", 64))])["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": merge_inventory(11, [(0, ("minecraft:iron_ingot", 64))])["1.21.1"]}),
    Step("data get block " + C, expect="active: 1b", poll=120),
    Step("data get block " + C, expect="maxprogress: 469762048L", poll=120),  # 64 x 7340032 — the PARALLEL 64 + DURATION T face
    Step("data get block " + C + " output_tank_1", expect=tank_expect("1.20.1", "x", "gt6:neutralmatter", 1920),
         poll=300, node_expects={"1.21.1": tank_expect("1.21.1", "x", "gt6:neutralmatter", 1920)}),
    Step("data get block " + C + " output_tank", expect=tank_expect("1.20.1", "x", "gt6:chargedmatter", 664),
         poll=60, node_expects={"1.21.1": tank_expect("1.21.1", "x", "gt6:chargedmatter", 664)}),

    phase("C: the auto-out bottom face — the sink holds the drained 1000 mB charge"),
    Step('data get block ' + S + ' \"tanks.in.0\"', expect=tank_expect("1.20.1", "x", "gt6:chargedmatter", 1000),
         poll=60, node_expects={"1.21.1": tank_expect("1.21.1", "x", "gt6:chargedmatter", 1000)}),

    phase("D: the small form (20415) — 1 iron ingot -> 26 + 30 mB in its own output tanks"),
    Step("setblock " + T + " " + SMALL, expect="Changed the block"),
    # the T5 is a TileEntityBasicMachine — its drive is the CLASS-side fakesource
    # regime (gt6machine, the side_io precedent; this arm never flaked in any
    # boot). Restored off in E.
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(small_merge([(0, ("minecraft:iron_ingot", 1))])["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": small_merge([(0, ("minecraft:iron_ingot", 1))])["1.21.1"]}),
    Step("data get block " + T, expect="active: 1b", poll=60),
    # the :773 CHEAP_OC face on the small form (live-proven forge): eUt 1 climbs mMinEnergy
    # 1 -> mInputMin 4096 in six x4 steps = x64 duration — 7340032 x 64 = 469762048
    Step("data get block " + T, expect="maxprogress: 469762048L", poll=30),
    # the small form parks its outputs in the mOutputFluids buffer (the base :1636
    # output_fluids face — tanks.out.* keys only exist while a tank HOLDS content, and
    # with no sink below the buffer is where the matter sits; live-proven both legs).
    # forge prints the GT6 writeToNBT shape; 21.1 the bare codec face (:1638) with the
    # live order amount-then-id.
    Step("data get block " + T + " output_fluids", expect="FluidName: \"gt6:chargedmatter\", Amount: 26",
         poll=300, node_expects={"1.21.1": "amount: 26, id: \"gt6:chargedmatter\""}),
    Step("data get block " + T + " output_fluids", expect="FluidName: \"gt6:neutralmatter\", Amount: 30",
         poll=60, node_expects={"1.21.1": "amount: 30, id: \"gt6:neutralmatter\""}),

    phase("E: teardown — the global-state restore + the explicit band restore"),
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),
    Step("fill %d %d %d %d %d %d air" % BAND, expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="massfab massfab",
    slug="massfab",
    sites=gt6world.declare_sites(CTRL, SINK, T5),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
