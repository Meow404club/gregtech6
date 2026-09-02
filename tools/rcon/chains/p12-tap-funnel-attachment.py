#!/usr/bin/env python3
"""p12-tap-funnel-attachment — the wall-attachment acceptance chain (task p12).

The barrel-interaction loop, live, through the two acceptance channels
(`/gt6tank tap|funnel <pos>` — the deterministic null-player counterfactuals of
"an empty-handed player clicks the tap" / "a player holding a water bucket clicks
the funnel"; the real interaction is the block use, DECLARED deviation):

  A barrel_wood at B, filled with 8000 L of water
  B tap_stainless_steel mounted on the barrel's EAST face (facing=west — the
    attachment looks AT its host), an empty CAULDRON below the tap:
    /gt6tank tap  drains the barrel by the upstream :95 full-tier branch
    (8000 >= 1000 available, cauldron empty) — the cauldron climbs to
    water_cauldron[level=3] and the barrel drops to 7000 (the 334/667/1000 tier
    table itself is the offline truth table in TapFunnelTest; 8000 L available
    hits the 1000 branch, not the 334 one — upstream-faithful)
  C funnel_stainless_steel mounted on the barrel's SOUTH face (facing=north):
    /gt6tank funnel pours a virtual water bucket (1000 L) into the barrel — the
    simulate-then-execute pour with the empty container returned — the barrel is
    back at 8000: the loop closed (the cauldron side's water came out through the
    tap, the funnel arm refills the barrel)
Pass 2 is the idempotency proof (the framework reruns everything on the cleaned
world).

Run:  python3 tools/rcon/chains/p12-tap-funnel-attachment.py
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

BARREL = gt6world.Site(30, 64, 30)
P = F(BARREL)
TAP = f"{BARREL.x + 1} {BARREL.y} {BARREL.z}"        # east of the barrel, facing west
CAULDRON = f"{BARREL.x + 1} {BARREL.y - 1} {BARREL.z}"  # below the tap
FUNNEL = f"{BARREL.x} {BARREL.y} {BARREL.z + 1}"     # south of the barrel, facing north
# the stone pad under the rig (the void-floor lesson: the cauldron needs a floor)
PAD = f"fill {BARREL.x - 2} {BARREL.y - 1} {BARREL.z - 2} {BARREL.x + 2} {BARREL.y - 1} {BARREL.z + 2} stone"
# the cauldron verdict via the vanilla execute-if trick (execute if block has no
# output on success; `gamerule` prints "The gamerule ... is currently ...")
CAULDRON_IS_FULL = f"execute if block {CAULDRON} minecraft:water_cauldron[level=3] run gamerule keepInventory"


CHAIN = Chain(
    name="p12-tap-funnel-attachment",
    slug="p12tfa",
    sites=gt6world.declare_sites(BARREL),
    preferred_ports=(25711, 25721),      # this card's pinned rcon/query pair
    steps=[
        phase("A: barrel + 8000 L of water (the tap's source)"),
        Step(PAD),                        # the floor re-laid every pass (cleanup clears it)
        Step(f"setblock {P} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank fill {P} minecraft:water 8000", expect="filled 8000/8000"),

        phase("B: the tap on the east face — the empty-hand draw fills the cauldron below"),
        Step(f"setblock {TAP} gt6:tap_stainless_steel[facing=west]", expect="Changed the block"),
        Step(f"setblock {CAULDRON} minecraft:cauldron", expect="Changed the block"),
        Step(f"gt6tank tap {TAP}", expect="cauldron level 0 -> 3, drained 1000"),
        Step(CAULDRON_IS_FULL, expect="The gamerule keepInventory"),
        Step(f"gt6tank stat {P}", expect="holds 7000/16000 L of minecraft:water"),

        phase("C: the funnel on the south face — the virtual bucket refills the barrel (the loop closes)"),
        Step(f"setblock {FUNNEL} gt6:funnel_stainless_steel[facing=north]", expect="Changed the block"),
        Step(f"gt6tank funnel {FUNNEL}", expect="poured 1000 L of minecraft:water"),
        Step(f"gt6tank stat {P}", expect="holds 8000/16000 L of minecraft:water"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
