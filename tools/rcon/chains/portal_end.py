#!/usr/bin/env python3
"""portal-end — the Miniature End Portal live chain (task
portals-mini-nether-end, RCON group portals). The ×128/512m pairing
live proof between the OVERWORLD and the END, the Ender-Eye activation (the
/gt6portal eye stand-in — the onBlockActivated2 arm, End.java:112-123), the
family separation (the Nether-only ignite arm rejects an End portal), the ITEM
face (hopper -> chest under B) and the REDSTONE relay (source west of A -> lamp
west of B — the same-face emission the OPOS double-flip yields).

  Rig:
    OVERWORLD  A=(512,65,384) the portal
               hopper (512,66,384) facing=down (the UP-face item push)
               redstone_block (511,65,384) west of A (the WEST-face read)
    END        B=(4,65,3) the portal
               chest (4,64,3) under B | lamp (5,65,3) east of B
               (the OPOS mirror: source WEST of A -> receiver EAST of B)

  The end band teardown is explicit (`execute in` fill + forceload remove all).

passes=2 is the idempotency proof (the [0,0] of this chain).
Run:  GT6_SESSION=off python3 tools/rcon/chains/portal_end.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)
if str(_HERE.parent) not in sys.path:
    sys.path.insert(0, str(_HERE.parent))

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# the fresh overworld band (east of the nether chain strip; x509..514, z382..386)
A = gt6world.Site(512, 65, 384)
HOPPER = gt6world.Site(512, 66, 384)
RSRC = gt6world.Site(511, 65, 384)

END = "execute in minecraft:the_end run "
B = "4 65 3"
CHEST = "4 64 3"
LAMP = "5 65 3"

steps = [
    phase("A: the overworld rig — portal + hopper + the redstone source west"),
    Step(f"setblock {F(A)} gt6:mini_portal_end", expect="Changed the block"),
    Step(f"setblock {F(HOPPER)} minecraft:hopper[facing=down]", expect="Changed the block"),
    Step(f"item replace block {F(HOPPER)} container.0 with minecraft:cobblestone 16", expect="Replaced"),
    Step(f"setblock {F(RSRC)} minecraft:redstone_block", expect="Changed the block"),

    phase("B: the end rig — forceload + portal + sinks"),
    Step(END + "forceload add 0 0 15 15", expect="to be force loaded", sleep=1.0),
    Step(END + f"setblock {B} gt6:mini_portal_end", expect="Changed the block"),
    Step(END + f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(END + f"setblock {LAMP} minecraft:redstone_lamp", expect="Changed the block"),

    phase("C: the family separation — the Nether ignite arm REJECTS an End portal"),
    Step(END + f"gt6portal ignite {B}", expect="IGNITE FAILED"),
    Step(f"gt6portal ignite {F(A)}", expect="IGNITE FAILED"),

    phase("D: the Ender-Eye activation both sides — the ×128/512m pairing"),
    Step(END + f"gt6portal eye {B}", expect="active=true"),
    Step(f"gt6portal eye {F(A)}", expect="active=true"),
    Step(f"gt6portal check {F(A)}", expect="target=X: 4   Y: 65   Z: 3",
         poll=60.0),  # dx=512-512=0, dz=384-384=0 under the ×128 factor
    Step(END + f"gt6portal check {B}", expect="target=X: 512   Y: 65   Z: 384",
         poll=60.0),

    phase("E: the ITEM leg — hopper -> A's UP face -> the chest under B"),
    Step(END + f"data get block {CHEST} Items",
         expect="cobblestone",
         poll=120.0),

    phase("F: the REDSTONE leg — the WEST-face source lights the lamp on B's OPOS mirror (east)"),
    Step(END + f"execute if block {LAMP} minecraft:redstone_lamp[lit=true]",
         expect="Test passed",
         poll=60.0),

    phase("G: extinguish — the watchdog decays the relay (the End negative tail)"),
    Step(f"gt6portal extinguish {F(A)}", expect="active=false"),
    Step(END + f"execute if block {LAMP} minecraft:redstone_lamp[lit=false]",
         expect="Test passed",
         poll=60.0),

    phase("H: teardown — both dimensions restored"),
    Step(END + f"setblock {B} air", expect="Changed the block"),
    Step(END + f"setblock {CHEST} air", expect="Changed the block"),
    Step(END + f"setblock {LAMP} air", expect="Changed the block"),
    Step(END + "forceload remove all", expect="removed", allow_failed=True),
    Step(f"fill 509 62 381 514 68 387 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="portal-end portals",
    slug="portalend",
    sites=gt6world.declare_sites(A, HOPPER, RSRC),
    preferred_ports=(26712, 26722),      # after the nether chain's 26692/26702 pair
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
