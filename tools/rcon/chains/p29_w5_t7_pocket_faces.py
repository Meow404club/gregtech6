#!/usr/bin/env python3
"""p29_w5_t7_pocket_faces — the pocket multitool face chain (task p29-w5-t7-pocket-eight,
the ACCEPTANCE RCON 替身面 arms 2-3: the knife attack face + the file-vs-iron-bars mining
face, plus the live tag arms the offline test cannot pin — the LOGS/PLANKS/LEAVES tags
bind only with a datapack, so /gt6pocket face saw at a setblock oak_log proves them here).

Chain semantics (/gt6pocket face drives the STATIC seams the item's own isCorrectToolForDrops
/ getDestroySpeed / attribute faces run — the same-source probe):

  A the saw arm: oak_log mines=true speed=6.0 (the tag arm, live), ice mines=true
    (the "Can harvest Ice" registration-row wording), stone mines=false.

  B the file arm: iron_bars mines=true speed=3.0 (the GT_Tool_File.java:82-85 ×3 row),
    glass_pane mines=false (the exact-block ruling — 1.20.1 GLASS_PANE IS an
    IronBarsBlock instance; an instanceof would have mined glass).

  C the attack arms: knife attack=2.0 classifies=[] (GT_Tool_Knife.getBaseDamage :56-58,
    no modern knife action yet — the t2 pool), scissors attack=1.0, multitool
    form=0 attack=0.0 (the closed form).

  D the teardown. No other world state.

passes=2 is the idempotency proof.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t7_pocket_faces.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=352 W5 band — faces column x394..397 (disjoint from ring x384 / smoke x404).
SITE = gt6world.Site(395, 64, 352, dx=3, dy=0, dz=0)
LOG = "394 64 352"
ICE = "395 64 352"
STONE = "396 64 352"
BARS = "397 64 352"
PANE = "398 64 352"

steps = []

# ------------------------------------------------- A: the saw arm
steps += [
    phase("A: the saw arm — the wood tag family live + the ice arm"),
    Step(f"setblock {LOG} minecraft:oak_log", expect="Changed the block"),
    Step(f"setblock {ICE} minecraft:ice", expect="Changed the block"),
    Step(f"setblock {STONE} minecraft:stone", expect="Changed the block"),
    Step(f"gt6pocket face saw {LOG}", expect="form=2 classifies=[saw] attack=0.0 block=minecraft:oak_log mines=true speed=6.0"),
    Step(f"gt6pocket face saw {ICE}", expect="block=minecraft:ice mines=true"),
    Step(f"gt6pocket face saw {STONE}", expect="block=minecraft:stone mines=false"),
]

# ------------------------------------------------- B: the file arm
steps += [
    phase("B: the file arm — the iron-bars ×3 face, glass panes stay out"),
    Step(f"setblock {BARS} minecraft:iron_bars", expect="Changed the block"),
    Step(f"setblock {PANE} minecraft:glass_pane", expect="Changed the block"),
    Step(f"gt6pocket face file {BARS}", expect="form=3 classifies=[file] attack=0.0 block=minecraft:iron_bars mines=true speed=3.0"),
    Step(f"gt6pocket face file {PANE}", expect="block=minecraft:glass_pane mines=false speed=1.0"),
]

# ------------------------------------------------- C: the attack arms
steps += [
    phase("C: the attack arms — knife 2.0 / scissors 1.0 / the closed multitool 0"),
    Step(f"gt6pocket face knife {LOG}", expect="form=1 classifies=[] attack=2.0"),
    Step(f"gt6pocket face scissors {LOG}", expect="form=6 classifies=[] attack=1.0"),
    Step(f"gt6pocket face multitool {LOG}", expect="form=0 classifies=[] attack=0.0 mines=false"),
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the site restored to air (the pass-open bbox is the backstop)"),
    Step(f"setblock {LOG} minecraft:air", expect="Changed the block"),
    Step(f"setblock {ICE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {STONE} minecraft:air", expect="Changed the block"),
    Step(f"setblock {BARS} minecraft:air", expect="Changed the block"),
    Step(f"setblock {PANE} minecraft:air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29_w5_t7_pocket_faces",
    slug="p29w5t7pocketfaces",
    sites=gt6world.declare_sites(SITE),
    preferred_ports=(26471, 26481),      # this card's pinned rcon/query pair (shared with the sibling chains)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
