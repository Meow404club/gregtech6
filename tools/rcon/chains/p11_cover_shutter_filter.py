#!/usr/bin/env python3
"""p11-cover-shutter-filter — pilot declarative RCON acceptance chain.

Pilot migration of tmp/p11-rcon-scripts/p11shutterfilter_atom.sh (the 33/33
proven chain) onto the framework; the chain text below is the bash original
step for step, with two declared framework deviations:

- the bash `if [ $? -ne 0 ]; then break; fi` early aborts are gone: every step
  runs so one red gives the full diagnostic picture (the exit code still
  reflects every failure);
- the per-section `fill <pos> air` sweeps are gone: gt6world derives the bbox
  cleanup from the declared sites at the start of EVERY pass — the review-fixed
  fill 18..26 lesson (the 24-site was once left out of a hand-written box and
  round 2 ran polluted) is structural now, sites in, cleanup complete.

Chain semantics (from the atom header):
  A shutter INSERT gate (oven 0 64 0, cover UP): the hopper above pushes into
    the oven's UP face (vanilla HopperBlockEntity.ejectItems :139 —
    FACING.getOpposite() = UP). visual 0 normal + running = OPEN → the stack
    lands; screwdriver → visual 1 inverted = CLOSED → it stays in the hopper.
  B shutter EXTRACT gate (oven 10 64 10, cover DOWN): the hopper below sucks
    from the container above through the oven's DOWN face
    (HopperBlockEntity.suckInItems :179). normal = OPEN → input drains;
    inverted = CLOSED → it stays.
  C item filter whitelist (oven 20 64 20): an empty whitelist refuses all
    (:118); the filter is seeded through the REAL save/load machinery —
    /data modify on the covers NBT lane ("t" = side UP) reloads the BE
    (TileEntityOven.load :737 readCoversFromNBT), the in-vivo NBT round trip —
    and the still-pending refused batch draining after the seed IS the admit
    assertion (no second fill, no race).
  D item filter blacklist (oven 24 64 24): an EMPTY blacklist admits all, so
    the iron seed lands on the empty hopper before any item is in flight;
    the filter item itself is then refused.
  No energy rig: the grid-fed oven never smelts, input counts stay stable.

Run:  python3 tools/rcon/chains/p11_cover_shutter_filter.py
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

# The eight declared sites — ovens plus their hoppers; the bbox cleanup union
# (margin 2) covers every one of them on every pass.
A_OVEN, A_HOPPER = gt6world.Site(0, 64, 0), gt6world.Site(0, 65, 0)
B_OVEN, B_HOPPER = gt6world.Site(10, 64, 10), gt6world.Site(10, 63, 10)
C_OVEN, C_HOPPER = gt6world.Site(20, 64, 20), gt6world.Site(20, 65, 20)
D_OVEN, D_HOPPER = gt6world.Site(24, 64, 24), gt6world.Site(24, 65, 24)


def _feed(pos, item, count):
    return gt6world.feed_container_command(F(pos), 0, item, count)


CHAIN = Chain(
    name="p11-cover-shutter-filter",
    slug="p11ssf",                       # the atom-era artifact names, kept
    sites=gt6world.declare_sites(
        A_OVEN, A_HOPPER, B_OVEN, B_HOPPER, C_OVEN, C_HOPPER, D_OVEN, D_HOPPER),
    preferred_ports=(25662, 25672),      # this card's pinned rcon/query pair
    steps=[
        phase("A: shutter INSERT gate (oven 0 64 0, cover UP, hopper above pushing into UP)"),
        Step(f"gt6oven place {F(A_OVEN)}", expect="placed"),
        Step(f"gt6cover install {F(A_OVEN)} up gt6:cover_shutter", expect="OK"),
        Step(gt6world.hopper_command(F(A_HOPPER), facing="down")),
        Step(_feed(A_HOPPER, "iron_ingot", 8), sleep=4),
        Step(f"gt6oven check {F(A_OVEN)}", expect="iron_ingotx8"),
        Step(f"gt6cover mode {F(A_OVEN)} up", expect="visual=1"),
        Step(_feed(A_HOPPER, "iron_ingot", 4), sleep=4),
        Step(f"gt6oven check {F(A_OVEN)}", expect="iron_ingotx8"),
        Step(f"gt6cover mode {F(A_OVEN)} up", expect="visual=0"),
        Step(f"gt6cover dismantle {F(A_OVEN)} up", expect="OK"),

        phase("B: shutter EXTRACT gate (oven 10 64 10, cover DOWN, hopper below sucking DOWN face)"),
        Step(f"gt6oven place {F(B_OVEN)}", expect="placed"),
        Step(f"gt6oven input 8 {F(B_OVEN)}", expect="cobblestone"),
        Step(f"gt6cover install {F(B_OVEN)} down gt6:cover_shutter", expect="OK"),
        Step(gt6world.hopper_command(F(B_HOPPER), facing="down"), sleep=4),
        Step(f"gt6oven check {F(B_OVEN)}", expect="airx0"),
        # flip to inverted (= closed while running) BEFORE re-filling the input —
        # the fill lands atomically inside one command execution, so nothing can
        # drain through the gate between the fill and the check (race-proof order)
        Step(f"gt6cover mode {F(B_OVEN)} down", expect="visual=1"),
        Step(f"gt6oven input 8 {F(B_OVEN)}", expect="cobblestone", sleep=4),
        Step(f"gt6oven check {F(B_OVEN)}", expect="cobblestonex8"),
        Step(f"gt6cover dismantle {F(B_OVEN)} down", expect="OK"),

        phase("C: item filter WHITELIST (oven 20 64 20, cover UP, hopper above)"),
        Step(f"gt6oven place {F(C_OVEN)}", expect="placed"),
        Step(f"gt6cover install {F(C_OVEN)} up gt6:cover_item_filter", expect="OK"),
        Step(gt6world.hopper_command(F(C_HOPPER), facing="down")),
        Step(_feed(C_HOPPER, "cobblestone", 8), sleep=4),
        Step(f"gt6oven check {F(C_OVEN)}", expect="airx0"),
        # seed the filter through the real save/load: covers.t = side-UP mNBTs
        # lane; the reload (TileEntityOven.load :737) is the in-vivo NBT round trip
        Step("data modify block " + F(C_OVEN)
             + ' covers.t set value {gt.filter.item:{id:"minecraft:cobblestone",Count:1b}}'),
        Step(f"gt6cover check {F(C_OVEN)}", expect="gt.filter.item", sleep=4),
        # the still-pending refused batch now matches the seeded whitelist and
        # drains — that pending batch IS the admit assertion (no second fill)
        Step(f"gt6oven check {F(C_OVEN)}", expect="cobblestonex8"),
        # the hopper is empty now — a fresh non-matching fill is refused deterministically
        Step(_feed(C_HOPPER, "iron_ingot", 8), sleep=4),
        Step(f"gt6oven check {F(C_OVEN)}", expect="cobblestonex8"),
        Step(f"gt6cover dismantle {F(C_OVEN)} up", expect="OK"),

        phase("D: item filter BLACKLIST (oven 24 64 24, cover UP, hopper above)"),
        Step(f"gt6oven place {F(D_OVEN)}", expect="placed"),
        Step(f"gt6cover install {F(D_OVEN)} up gt6:cover_item_filter", expect="OK"),
        Step(f"gt6cover mode {F(D_OVEN)} up", expect="visual=1"),
        # seed the IRON filter BEFORE any items are in flight — race-free; the
        # blacklist with an EMPTY filter admits all, so the seed happens on the
        # empty hopper
        Step("data modify block " + F(D_OVEN)
             + ' covers.t set value {gt.filter.item:{id:"minecraft:iron_ingot",Count:1b}}'),
        Step(f"gt6cover check {F(D_OVEN)}", expect="gt.filter.item"),
        Step(gt6world.hopper_command(F(D_HOPPER), facing="down")),
        Step(_feed(D_HOPPER, "cobblestone", 8), sleep=4),
        Step(f"gt6oven check {F(D_OVEN)}", expect="cobblestonex8"),
        # the filter item itself is refused: nothing enters, the input stays 8 cobble
        Step(_feed(D_HOPPER, "iron_ingot", 8), sleep=4),
        Step(f"gt6oven check {F(D_OVEN)}", expect="cobblestonex8"),
        Step(f"gt6cover dismantle {F(D_OVEN)} up", expect="OK"),

        phase("teardown: all four stores dissolve to null"),
        Step(gt6world.store_null_command(A_OVEN), expect=gt6world.STORE_NULL_EXPECT),
        Step(gt6world.store_null_command(B_OVEN), expect=gt6world.STORE_NULL_EXPECT),
        Step(gt6world.store_null_command(C_OVEN), expect=gt6world.STORE_NULL_EXPECT),
        Step(gt6world.store_null_command(D_OVEN), expect=gt6world.STORE_NULL_EXPECT),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
