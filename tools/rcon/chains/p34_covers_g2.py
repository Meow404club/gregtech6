#!/usr/bin/env python3
"""p34-covers-gameplay-10 — the gameplay cover family live acceptance chain.

The live half of task p34-covers-gameplay-10. One fresh band z=300..312,
x=560..584 (x/z-disjoint from every registered sweep band; the p33 covers band
sits z252..264 x509..531, the sensors column x680 z127..137):

  three METAL DRUMS on a stone floor row (the metal drum admits every cover —
  the p6 ruling; the pump-tank seam is the Vent/Drain/FilterFluid host gate):

    VENT drum (566 65 306):   gt6:cover_vent   on the NORTH face — the air
      intake face; the live fill arm rides the declared-minimal gt6:air seam
      (the port registers no air fluid — the offline suite pins the no-op), so
      the live proof is the MOUNT + the covers NBT lane.
    DRAIN drum (570 65 306):  gt6:cover_drain  on the UP face — the rain/
      source-collection face; the live proof is the MOUNT + the NBT lane (the
      world-sweep arms are level-bound, the offline suite pins the beats).
    FILTER drum (574 65 306): gt6:cover_fluid_filter on the NORTH face — the
      whitelist face gate; the live proof is the MOUNT, the SCREWDRIVER mode
      flip (visual 0 -> 1, the /gt6cover mode relay) and the covers NBT
      readback carrying the flipped lane.

  the NEGATIVE arm: the same /gt6cover install driving gt6:cover_selector_manual
  against the VENT drum — the selector placement gate refuses every
  non-SwitchableMode host (the live verdict of the shared gate; the first
  switchable-mode host is the declared host-composition follow-up card, and the
  torch/repeater/valve gates ride the same follow-up: the wire/pipe carriers
  implement no cover surface yet).

Arms:
  A SITE: the clear + the floor + the three drums.
  B MOUNTS: the three installs answer ok=true; the selector install answers
    FAILED (the live placement-gate negative); the drum NBT carries the covers
    tag with the right item id per face.
  C LANES: the /gt6cover mode screwdriver relay flips the filter plate
    (whitelist -> blacklist, the visual lane 0 -> 1 in the covers NBT).
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, phase

F = gt6world.fmt

A = gt6world.Site(560, 65, 300, dx=12, dy=5, dz=12)  # x560..584, y60..70, z300..312

STONE = "minecraft:stone"
DRUM = "gt6:barrel_metal"

VENT = (566, 65, 306)
DRAIN = (570, 65, 306)
FILTER = (574, 65, 306)


CHAIN = Chain(
    name="p34-covers-g2",
    slug="p34covg2",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25772, 25782),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the site — the floor row and the three metal drums"),
        Step("fill %d %d %d %d %d %d air" % (560, 60, 300, 584, 70, 312), expect="filled"),
        Step("forceload add 560 300 584 312"),
        Step("fill 560 64 300 584 64 312 " + STONE, expect="filled", label="the support floor"),
        Step("setblock %s %s" % (F(VENT), DRUM), expect="Changed the block",
             label="the vent drum (the pump-tank seam host)"),
        Step("setblock %s %s" % (F(DRAIN), DRUM), expect="Changed the block"),
        Step("setblock %s %s" % (F(FILTER), DRUM), expect="Changed the block"),

        # ------------------------------------------------------------------
        phase("B: the mounts — three live installs + the placement-gate negative"),
        Step("gt6cover install %s north gt6:cover_vent" % F(VENT), expect="ok=true",
             label="the vent cover mounts on the drum's NORTH face (the pump-seam gate admits)"),
        Step("gt6cover install %s up gt6:cover_drain" % F(DRAIN), expect="ok=true",
             label="the drain cover mounts on the drum's UP face (the rain/source face)"),
        Step("gt6cover install %s north gt6:cover_fluid_filter" % F(FILTER), expect="ok=true",
             label="the fluid filter mounts on the drum's NORTH face (the whitelist gate)"),
        Step("gt6cover install %s north gt6:cover_selector_manual" % F(VENT), expect="install FAILED",
             label="the NEGATIVE arm: the manual selector REFUSES the drum (no ITileEntitySwitchableMode — the live selector gate verdict)"),
        Step("gt6cover check %s" % F(VENT), expect="block/vent/front",
             label="the vent drum's snapshot carries the vent plate sprite (the live cover identity)"),
        Step("gt6cover check %s" % F(DRAIN), expect="block/drain/front",
             label="the drain drum's snapshot carries the drain plate sprite"),
        Step("gt6cover check %s" % F(FILTER), expect="block/filterfluid/normal",
             label="the filter drum's snapshot carries the whitelist plate sprite (visual 0)"),

        # ------------------------------------------------------------------
        phase("C: the lane — the screwdriver relay flips the filter plate"),
        Step("gt6cover mode %s north" % F(FILTER), expect="toolDamage=1000",
             label="the screwdriver relay: the filter whitelist flips to blacklist, damage 1000 (upstream :62-65)"),
        Step("gt6cover mode %s north" % F(FILTER), expect="visual=1",
             label="the relay report reads the flipped visual lane 1 (the second pin: the flip landed)"),
        Step("gt6cover mode %s north" % F(FILTER), expect="visual=0",
             label="and back — the whitelist/blacklist cycle is symmetric (upstream :63)"),
        Step("gt6cover check %s" % F(FILTER), expect="block/filterfluid/normal",
             label="the plate art follows the restored whitelist mode"),
    ],
)

if __name__ == "__main__":
    chain = CHAIN
    node = None
    if "--node" in sys.argv:
        node = sys.argv[sys.argv.index("--node") + 1]
    import framework
    if framework.session_enabled():
        sys.exit(framework.run_session([chain], node=node))
    chain.node = node or framework.requested_node()
    sys.exit(framework.run(chain))
