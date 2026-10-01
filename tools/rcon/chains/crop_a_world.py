#!/usr/bin/env python3
"""crop_a_world — the crop-stick live smoke chain (card cbc-1-cropstick-base;
the GTBurnerCommand ruling: no player-click RCON seam exists, the /gt6crop
admin command IS the acceptance channel, driving the SAME server halves the
interaction runs).

Chain semantics (the ADR-CB5 ruling in force: the 256t growth cycle is NOT
asserted here — `time set`/randomTickSpeed do not accelerate BE ticks and a
tier-1 maturity run is thousands of ticks; growth correctness is the offline
seeded pin suite CropBlockEntityTest. The live chain only pins the placement,
planting, crossing, harvest and readout faces, with the tryPlantIn size
argument carrying the mature arm — the upstream :464 signature, not a
growth shortcut):

  A place: farmland + gt6:crop_sticks lands as the empty single stick
    ([crossing=false], readout "empty").
  B plant: /gt6crop plant <pos> rye -- the base-seed arm over the real
    gt6:food_crop_rye binding; readout "crop=rye size=1".
  C crossing: /gt6crop stick <pos> -- the rightClick stick arm's command twin;
    the blockstate carrier flips to [crossing=true].
  D mature arm: plant rye at size 7 (the :464 signature), /gt6crop harvest
    -- the produce drops as a real item entity (gt6:food_crop_rye pinned via
    execute-if-entity), the size resets to the after-harvest 2.
  E negative: an immature (size 1) harvest refuses -- harvested=false, the
    plant survives.

The two framework passes are the [0, 0] idempotency proof (every arm re-lays
its rig first; the pass-open bbox cleanup restores the sites between passes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/crop_a_world.py --node 1.20.1-forge
      GT6_SESSION=off python3 tools/rcon/chains/crop_a_world.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites -- a fresh z=616 band, x384..390 (x/z-disjoint from every roster
# band; the roster's former top was the z=600 x500..506 coke-oven strip).
FARM = gt6world.Site(384, 64, 616, dz=1)   # the place/plant arm (farmland 64 + stick 65)
CROSS = gt6world.Site(386, 64, 616, dz=1)  # the crossing arm
MATURE = gt6world.Site(388, 64, 616, dz=1) # the mature harvest arm
NEG = gt6world.Site(390, 64, 616, dz=1)    # the immature negative arm

FP = "384 64 616"
FS = "384 65 616"
CP = "386 64 616"
CS = "386 65 616"
MP = "388 64 616"
MS = "388 65 616"
NP = "390 64 616"
NS = "390 65 616"

ITEM_RYE = ('@e[type=minecraft:item,nbt={Item:{id:"gt6:food_crop_rye"}},'
            'distance=..6,x=388,y=65,z=616]')
ITEMS_MATURE = "@e[type=minecraft:item,distance=..6,x=388,y=65,z=616]"

steps = []


def rig(pad: str, stick: str) -> list:
    """One arm's rig: the farmland seat + the stick (re-laid every pass)."""
    return [
        Step(f"setblock {pad} minecraft:farmland", expect="Changed the block"),
        Step(f"setblock {stick} gt6:crop_sticks", expect="Changed the block"),
    ]


steps += [
    phase("A: place -- the empty single stick on farmland"),
    *rig(FP, FS),
    Step(f"execute if block {FS} gt6:crop_sticks[crossing=false] run gamerule keepInventory",
         expect="Gamerule keepInventory is currently set to"),
    Step(f"gt6crop readout {FS}", expect="crossing=false empty"),

    phase("B: plant -- the base-seed arm over the real gt6:food_crop_rye binding"),
    Step(f"gt6crop plant {FS} rye", expect="planted=rye size=1 ok=true"),
    Step(f"gt6crop readout {FS}", expect="crop=rye size=1"),

    phase("C: crossing -- the stick arm flips the blockstate carrier"),
    *rig(CP, CS),
    Step(f"gt6crop stick {CS}", expect="crossing=true ok=true"),
    Step(f"execute if block {CS} gt6:crop_sticks[crossing=true] run gamerule keepInventory",
         expect="Gamerule keepInventory is currently set to"),

    phase("D: the mature arm -- plant rye at 7 (the :464 size face) and harvest the produce"),
    *rig(MP, MS),
    Step(f"gt6crop plant {MS} rye 7", expect="planted=rye size=7 ok=true"),
    Step(f"gt6crop harvest {MS}", expect="harvested=true size=2"),
    Step(f"execute if entity {ITEM_RYE} run gamerule keepInventory",
         expect="Gamerule keepInventory is currently set to"),
    Step(f"kill {ITEMS_MATURE}", expect="Killed"),

    phase("E: negative -- an immature harvest refuses and the plant survives"),
    *rig(NP, NS),
    Step(f"gt6crop plant {NS} rye 1", expect="planted=rye size=1 ok=true"),
    Step(f"gt6crop harvest {NS}", expect="harvested=false size=1"),
    Step(f"execute if block {NS} gt6:crop_sticks run gamerule keepInventory",
         expect="Gamerule keepInventory is currently set to"),
]

CHAIN = Chain(
    name="crop_a_world",
    slug="cropw",
    sites=gt6world.declare_sites(FARM, CROSS, MATURE, NEG),
    preferred_ports=(25984, 25994),      # this card's pinned rcon/query pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
