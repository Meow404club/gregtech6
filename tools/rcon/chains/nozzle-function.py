#!/usr/bin/env python3
"""nozzle-function — the gas-nozzle acceptance chain (task nozzle-function).

The barrel-interaction loop, live, through the two acceptance channels
(`/gt6tank nozzle|capnozzle <pos>` — the deterministic null-player counterfactuals
of "a player holding an empty (gas-proof) metal drum clicks the nozzle" / "a player
holding a gas-filled drum clicks the cap nozzle"; the real interaction is the block
use, DECLARED deviation like the tap/funnel rig):

  A barrel_metal at B (gas-proof — a wood barrel would fizz-trash the gas on its
  next tick), 4000 L of WATER:
  B nozzle_stainless_steel mounted on the barrel's EAST face (facing=west — the
    attachment looks AT its host):
    /gt6tank nozzle  → "refused a non-gas" and the tank keeps its 4000 water
    (the tap's mirror gate, upstream MultiTileEntityFluidNozzle.java:82)
  C the water drawn, 4000 L of gt6:chlorine in (the live registered gas,
    density −100 — GTFluids CHEMICAL row):
    /gt6tank nozzle  → the virtual metal drum takes ALL 4000 (probe MAX_VALUE),
    the barrel pays exactly what landed — 0/64000 left
  D cap_nozzle_stainless_steel mounted on the barrel's WEST face (facing=east),
    the barrel empty:
    /gt6tank capnozzle → the virtual chlorine drum pours when it ALL fits
    (simulate gate :76-77), the barrel is back at 4000: the loop closed (the
    drain side's gas came out through the nozzle, the cap arm refills it)
  E the barrel drawn empty:
    /gt6tank nozzle  → "nothing to draw" (the :81 probe negative)
Pass 2 is the idempotency proof (the framework reruns everything on the cleaned
world).

Run:  python3 tools/rcon/chains/nozzle-function.py            # forge leg
      python3 tools/rcon/chains/nozzle-function.py --node 1.21.1-neoforge
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

BARREL = gt6world.Site(30, 64, 44)
P = F(BARREL)
NOZZLE = f"{BARREL.x + 1} {BARREL.y} {BARREL.z}"        # east of the barrel, facing west
CAP = f"{BARREL.x - 1} {BARREL.y} {BARREL.z}"           # west of the barrel, facing east
# the stone pad under the rig (the void-floor lesson)
PAD = f"fill {BARREL.x - 2} {BARREL.y - 1} {BARREL.z - 2} {BARREL.x + 2} {BARREL.y - 1} {BARREL.z + 2} stone"


CHAIN = Chain(
    name="nozzle-function",
    slug="nozzle",
    sites=gt6world.declare_sites(BARREL),
    preferred_ports=(25711, 25721),      # this card's pinned rcon/query pair
    steps=[
        phase("A: metal barrel + 4000 L of water (the non-gas negative source)"),
        Step(PAD),                        # the floor re-laid every pass (cleanup clears it)
        # the placements stay unasserted — the 21.1 setblock feedback carries a
        # same-state dialect; the gt6tank verbs below are the placement proofs
        Step(f"setblock {P} gt6:barrel_metal"),
        Step(f"gt6tank fill {P} minecraft:water 4000", expect="filled 4000/4000"),

        phase("B: the nozzle refuses the non-gas before anything drains (:82)"),
        Step(f"setblock {NOZZLE} gt6:nozzle_stainless_steel[facing=west]"),
        Step(f"gt6tank nozzle {NOZZLE}", expect="refused a non-gas"),
        Step(f"gt6tank stat {P}", expect="4000/64000 L of minecraft:water"),

        phase("C: swap to chlorine — the drain half moves the gas into the virtual drum"),
        Step(f"gt6tank draw {P} 4000", expect="drawn 4000/4000"),
        Step(f"gt6tank fill {P} gt6:chlorine 4000", expect="filled 4000/4000"),
        Step(f"gt6tank nozzle {NOZZLE}", expect="filled 4000 L of gas into the held container"),
        Step(f"gt6tank stat {P}", expect="0/64000 L of nothing"),

        phase("D: the cap nozzle pours the virtual chlorine drum back (the loop closes)"),
        Step(f"setblock {CAP} gt6:cap_nozzle_stainless_steel[facing=east]"),
        Step(f"gt6tank capnozzle {CAP}", expect="filled 4000 L of gt6:chlorine from the held gas container"),
        Step(f"gt6tank stat {P}", expect="4000/64000 L of gt6:chlorine"),

        phase("E: the empty-tank negative (:81)"),
        Step(f"gt6tank draw {P} 4000", expect="drawn 4000/4000"),
        Step(f"gt6tank nozzle {NOZZLE}", expect="nothing to draw"),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
