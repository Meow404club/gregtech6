#!/usr/bin/env python3
"""p31-implosion — the Implosion Compressor chain (task p31-implosion, the
p31_implosion sweep group; Loader_MultiTileEntities.java:1228).

  TU self-generating, window 1..16, PARALLEL 64, NO_CONSTANT_POWER, no ignition; the
  3x3x3 hollow of 25 Dense Tungstensteel Walls + the controller as the back-bottom-centre
  shell cell, the shell centred front+up of the controller (the upstream Autoclave
  geometry with wall 18023); the vanilla-TNT diamond rows (dust1+8TNT+tag0 ->
  plateGem DiamondIndustrial, tag1 -> gem; eUt 0, dur 256 = the 1/tick self-gen pace).

acceptance arms this machine carries:
  1 form all-green + the wrong-part rejection (the 26-cell shell, the controller
    self-cell pass, the hollow air centre)
  2 the TNT-branch recipe walk on both tiers: tag(0) and tag(1) route by the selector
    Damage tag, the outputs land on the map-aware output slots (slot 3+), eUt0/256t
  3 the CUT arm: dust + TNT without the selector fails the 3-item minimum (active
    stays 0b) — the mMinimalInputItems = 3 gate live

passes=2 is the idempotency proof.
Run:  GT6_SESSION=off python3 tools/rcon/chains/p31_implosion.py
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

CTRL = gt6world.Site(388, 65, 380)   # the controller: the shell's back-bottom-centre cell
SHELL = gt6world.Site(388, 66, 381, dx=1, dy=1, dz=1)  # the hollow centre ±1 = the shell band
WRONG = gt6world.Site(388, 66, 380)  # a wall cell of the shell (the z-min face centre)
C, W = F(CTRL), F(WRONG)

MACHINE = "implosion_compressor"

# the per-leg selector stack: the Integrated Circuit at configuration N rides the
# Damage key on 1.20.1 and the minecraft:custom_data component on 1.21.1
# (GT6Circuits.applyConfiguration; the p24_act chain form)
def selector(config, slot=2):
    # the selector rides slot 2 (the third input slot). 1.20.1: the Damage key lives
    # INSIDE the item's `tag:{}` compound (the raw data-merge NBT does NOT redirect
    # item arguments like /give does; a top-level Damage key is dead weight —
    # ItemStack.of only carries "tag" into the stack's metadata) and is an INT
    # (the setDamageValue carrier, GT6Circuits.applyConfiguration — the recipe's
    # exact-tag match then sees {Damage:0} on BOTH sides). 1.21.1: the
    # minecraft:custom_data component (the applyConfiguration 21.1 branch).
    return {
        "1.20.1": '{Slot:%db,id:"gt6:integrated_circuit",Count:1b,tag:{Damage:%d}}' % (slot, config),
        "1.21.1": '{Slot:%db,id:"gt6:integrated_circuit",count:1,components:{"minecraft:custom_data":{Damage:%d}}}' % (slot, config),
    }

def merge(items, selector_config):
    # the count key forks per leg (Count b / count); the selector rides the last slot
    sel = selector(selector_config) if isinstance(selector_config, int) else None
    out = {}
    for k in ("1.20.1", "1.21.1"):
        parts = ",".join(
            '{Slot:%db,id:"%s",%s:%d%s}' % (slot, iid, "Count" if k == "1.20.1" else "count", n, "b" if k == "1.20.1" else "")
            for slot, (iid, n) in items)
        if sel is not None:
            parts += "," + sel[k]
        payload = "inventory:{Size:11,Items:[" + parts + "]}"
        out[k] = "data merge block " + C + " {" + payload + "}"
    return out

# tier 0: dust1 + TNT x8 + tag(0) -> plate_gem_diamond_industrial
FEED_T0 = merge([(0, ("gt6:dust_diamond", 1)), (1, ("minecraft:tnt", 8))], 0)
# tier 1: dust1 + TNT x8 + tag(1) -> gem_diamond_industrial
FEED_T1 = merge([(0, ("gt6:dust_diamond", 1)), (1, ("minecraft:tnt", 8))], 1)
# the CUT arm: dust + TNT, NO selector — the 3-item minimum gate
def merge_no_selector(items):
    out = {}
    for k in ("1.20.1", "1.21.1"):
        parts = ",".join(
            '{Slot:%db,id:"%s",%s:%d%s}' % (slot, iid, "Count" if k == "1.20.1" else "count", n, "b" if k == "1.20.1" else "")
            for slot, (iid, n) in items)
        payload = "inventory:{Size:11,Items:[" + parts + "]}"
        out[k] = "data merge block " + C + " {" + payload + "}"
    return out

FEED_NOSEL = merge_no_selector([(0, ("gt6:dust_diamond", 1)), (1, ("minecraft:tnt", 8))])

steps = [
    phase("A: the site — the wrong-part rejection, then the all-green form"),
    Step("fill 385 60 377 391 70 384 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("setblock " + W + " gt6:machine_wall_lead", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=false"),
    Step("setblock " + W + " air", expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step("execute if block " + C + " gt6:" + MACHINE + "[formed=true]", expect="Test passed"),

    phase("B: the tier-0 run — dust + 8 TNT + tag(0) -> plateGem DiamondIndustrial"),
    Step(FEED_T0["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED_T0["1.21.1"]}),
    Step("data get block " + C, expect="active: 1b", poll=60),          # the self-generated run
    Step("data get block " + C, expect="maxprogress: 256L", poll=20),   # the eUt0/256t bar
    Step("data get block " + C, expect="gt6:plate_gem_diamond_industrial", poll=300),  # the completed run lands the output

    phase("C: the tier-1 arm — fresh machine (the tier-0 plate output would block the gem output: the canOutput blockage semantics), then the tag(1) run"),
    Step("fill 385 60 377 391 70 384 air", expect="filled"),
    Step("setblock " + C + " gt6:" + MACHINE, expect="Changed the block"),
    Step("gt6multiblock form " + C, expect="formed=true okay=true"),
    Step(FEED_T1["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED_T1["1.21.1"]}),
    Step("data get block " + C, expect="active: 1b", poll=60),
    Step("data get block " + C, expect="gt6:gem_diamond_industrial", poll=300),

    phase("D: the CUT arm — dust + TNT without the selector fails the 3-item minimum"),
    Step(FEED_NOSEL["1.20.1"], expect="Modified block data",
         node_cmds={"1.21.1": FEED_NOSEL["1.21.1"]}),
    Step("data get block " + C, expect="active: 0b", poll=20),          # no recipe without the selector

    phase("E: teardown — the explicit band restore"),
    Step("fill 385 60 377 391 70 384 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p31-implosion p31_implosion",
    slug="p31implosion",
    sites=gt6world.declare_sites(CTRL, SHELL),
    preferred_ports=(26346, 26356),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
