#!/usr/bin/env python3
"""p29-w5-t6-electric-nineteen FACES chain — the per-form mining-surface reads live
(task p29-w5-t6-electric-nineteen, the chain-4 column x640..646; the offline JVM does
not bind the vanilla mineable tags, so the TAG arms are THIS chain's — the
ElectricNineteenTest offline/live division):

  the drill surface: stone 18.0 (6.0 x the upstream 3.0x multiplier) + sand 18.0
    (the shovel tag arm — the DRILL fold, :57) + glass 18.0 (the Material.glass arm);
  the chainsaw: oak log 12.0 (6.0 x 2.0, :159) + ice 12.0 (the harvest-Ice tooltip);
  the buzzsaw: glass 0.0 ("Not suitable for harvesting Blocks", :167) with the iron
    bars 6.0 riding the jackhammer chain's bars face;
  the machine-tool classes: wrench/screwdriver/hand_drill/hand_mixer read speed=0.0
    on stone (the zero machine face — no port block universe, the RED LINE);
  the trimmer: oak leaves 1.5 (6.0 x the upstream 0.25 multiplier, :174).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t6_electric_faces.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F1 = gt6world.Site(640, 65, 240)  # the drill band
F2 = gt6world.Site(642, 65, 240)  # the chainsaw band
F3 = gt6world.Site(644, 65, 240)  # the zero-face band
F4 = gt6world.Site(646, 65, 240)  # the trimmer band

steps = []

# --------------------------------- A: the drill surface (the pickaxe+shovel fold)
steps += [
    phase("A: the drill face — stone/sand/glass at 18.0"),
    Step(f"setblock {F1.x} {F1.y} {F1.z} minecraft:stone", expect="Changed the block"),
    Step(f"gt6electric speed {F1.x} {F1.y} {F1.z} mining_drill_lv", expect="speed=18.0, correctForDrops=true"),
    Step(f"setblock {F1.x} {F1.y} {F1.z} minecraft:dirt", expect="Changed the block"),  # dirt not sand — the gravity fall would empty the site (the live-run lesson)
    Step(f"gt6electric speed {F1.x} {F1.y} {F1.z} mining_drill_lv", expect="speed=18.0"),
    Step(f"setblock {F1.x} {F1.y} {F1.z} minecraft:glass", expect="Changed the block"),
    Step(f"gt6electric speed {F1.x} {F1.y} {F1.z} mining_drill_lv", expect="speed=18.0"),
]

# --------------------------------- B: the chainsaw surface (axe + ice)
steps += [
    phase("B: the chainsaw face — log/ice at 12.0"),
    Step(f"setblock {F2.x} {F2.y} {F2.z} minecraft:oak_log", expect="Changed the block"),
    Step(f"gt6electric speed {F2.x} {F2.y} {F2.z} chainsaw_lv", expect="speed=12.0, correctForDrops=true"),
    Step(f"setblock {F2.x} {F2.y} {F2.z} minecraft:ice", expect="Changed the block"),
    Step(f"gt6electric speed {F2.x} {F2.y} {F2.z} chainsaw_lv", expect="speed=12.0"),
]

# --------------------------------- C: the zero faces — the buzzsaw cut + the machine-tool classes
steps += [
    phase("C: the zero faces — the buzzsaw glass 0.0, the wrench/screwdriver/hand tools 0.0"),
    Step(f"setblock {F3.x} {F3.y} {F3.z} minecraft:glass", expect="Changed the block"),
    Step(f"gt6electric speed {F3.x} {F3.y} {F3.z} buzzsaw_lv", expect="speed=0.0, correctForDrops=false"),
    Step(f"gt6electric speed {F3.x} {F3.y} {F3.z} wrench_lv", expect="speed=0.0"),
    Step(f"gt6electric speed {F3.x} {F3.y} {F3.z} screwdriver_lv", expect="speed=0.0"),
    Step(f"gt6electric speed {F3.x} {F3.y} {F3.z} hand_drill_lv", expect="speed=0.0"),
    Step(f"gt6electric speed {F3.x} {F3.y} {F3.z} hand_mixer_lv", expect="speed=0.0"),
]

# --------------------------------- D: the trimmer face (leaves at 1.5 = 6.0 x 0.25)
steps += [
    phase("D: the trimmer face — leaves at 1.5"),
    Step(f"setblock {F4.x} {F4.y} {F4.z} minecraft:oak_leaves", expect="Changed the block"),
    Step(f"gt6electric speed {F4.x} {F4.y} {F4.z} trimmer_lv", expect="speed=1.5"),
    Step(f"setblock {F4.x} {F4.y} {F4.z} minecraft:oak_log", expect="Changed the block"),
    Step(f"gt6electric speed {F4.x} {F4.y} {F4.z} trimmer_lv", expect="speed=0.0"),
]

CHAIN = Chain(
    name="p29-w5-t6-electric-faces p29_w5_t6_electric",
    slug="p29w5t6electricfaces",
    sites=gt6world.declare_sites(F1, F2, F3, F4),
    preferred_ports=(26363, 26373),
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
