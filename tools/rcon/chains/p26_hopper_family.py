#!/usr/bin/env python3
"""p26-hopper-family — the storage-hopper family live acceptance chain.

The live half of task p26-storage-hopper-family: the seven-column walk over the
Loader_MultiTileEntities.java:145-146 pair (Bronze aHopperSize 3 :191 / Steel 5
:202 × hopper/queue kinds), the offline suite's complementary tick-world face.

Columns (z=220 band, x=402..430 — disjoint from every registered sweep band):

  A BRONZE THROUGH-FLOW (x=402): chest -> vanilla hopper -> gt6:hopper_bronze
    [facing=down] -> chest. The vanilla push into the GT block below walks the
    level item-handler capability — the 1.20.1 getCapability wiring arm live
    (the 21.1 twin is the registered GT6CapabilityWiring row). Judge: the stone
    lands in the bottom chest, the GT hopper dump carries NBT_INV_SIZE = 3.
  B STEEL RETENTION (x=406): the 5-slot anchor sealed by a plain stone floor.
    Five distinct single items descend one vanilla-hopper cadence at a time;
    all five land in five distinct slots. Judge: the dump holds all five ids
    and Size: 5 (the :202 aHopperSize column live).
  B2 BRONZE RETENTION (x=430): the same shape over the 3-slot anchor with FOUR
    distinct items — three land, the fourth is refused. Judge: the dump holds
    stone/dirt/oak_planks and Size: 3, and the vanilla hopper above still holds
    the cobblestone (the backed-up queue the slot count causes).
  C MODE NBT ARM (x=410): a standalone bronze hopper driven through /data
    modify + /data get — the BE.load/saveAdditional round trip live. Judge:
    mode 16 reads back 16b; the over-limit 100 clamps to 64b (the declared
    defensive clamp; the screwdriver cycle 0..64 shares the bound).
  D QUEUE FIFO ORDER (x=414): chest -> vanilla hopper -> gt6:queue_hopper_steel
    [facing=down] -> chest. Stone enters first, dirt second; the fixed point
    pushes older items toward the last slot and the head drains first. Judge:
    the bottom chest's Items list reads Slot 0 = stone then Slot 1 = dirt —
    the (Slot, id) pairings pin the ORDER, not just the membership.
  E TOP ITEM SUCTION (x=418): a dropped item entity over the hopper's open top
    (no container above, air not opaque — the moveInPhase else-arm). Judge: the
    whole entity stack (7 dirt) lands in one slot, the WD.suck :93-105 port.
  F MINECART INTERCEPTION (x=422): a rail over the hopper + a chest minecart on
    it — the handlerAt rail arm (BaseRailBlock + CONTAINER_ENTITY_SELECTOR, the
    1.20.1 equivalent of upstream :173-176). Judge: the cart's iron ingot lands
    in the hopper.

Run:  python3 tools/rcon/chains/p26_hopper_family.py
      python3 tools/rcon/chains/p26_hopper_family.py --node 1.21.1-neoforge
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


def _col(x, y, z):
    """One column's four levels (chest 65 / hopper 64 / GT hopper 63 / floor 62)."""
    return gt6world.Site(x, 63, z, dy=3)


def _up(site, n=1):
    return gt6world.Site(site.x, site.y + n, site.z)


def _down(site, n=1):
    return gt6world.Site(site.x, site.y - n, site.z)


# the seven columns (x spread on the z=220 line)
A, B, B2, C, D, E, F_COL = (_col(x, 63, 220) for x in (402, 406, 430, 410, 414, 418, 422))

CHEST = "minecraft:chest"
VHOP = "minecraft:hopper[facing=down]"
STONE_FLOOR = "minecraft:stone"


def _feed(pos, slot, item, count=1):
    return Step(gt6world.feed_container_command(pos, slot, item, count),
                expect="Replaced")


def _dump(pos, expect, poll=25.0, label=None):
    return Step(f"data get block {F(pos)}", expect=expect, poll=poll, label=label)


CHAIN = Chain(
    name="p26-hopper-family",
    slug="p26hopfam",
    sites=gt6world.declare_sites(A, B, B2, C, D, E, F_COL),
    preferred_ports=(25702, 25712),
    steps=[
        # ------------------------------------------------------------------
        phase("A: bronze through-flow (chest -> vanilla hopper -> gt6:hopper_bronze -> chest; the capability push seam live)"),
        Step(f"setblock {F(_up(A, 2))} {CHEST}", expect="Changed the block"),
        Step(f"setblock {F(_up(A, 1))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(A)} gt6:hopper_bronze[facing=down]", expect="Changed the block"),
        Step(f"setblock {F(_down(A, 1))} {CHEST}", expect="Changed the block"),
        _feed(_up(A, 2), 0, "stone", 16),
        _dump(_down(A, 1), 'id: "minecraft:stone"', label="stone crosses the vanilla-push -> GT-handler -> GT-output path"),
        _dump(A, "Size: 3", label="bronze NBT_INV_SIZE = aHopperSize 3 (Loader :191)"),

        # ------------------------------------------------------------------
        phase("B: steel retention (5-slot anchor, five distinct items, stone floor seals the output face)"),
        Step(f"setblock {F(_up(B, 2))} {CHEST}", expect="Changed the block"),
        Step(f"setblock {F(_up(B, 1))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(B)} gt6:hopper_steel[facing=down]", expect="Changed the block"),
        Step(f"setblock {F(_down(B, 1))} {STONE_FLOOR}", expect="Changed the block"),
        _feed(_up(B, 2), 0, "stone"),
        _feed(_up(B, 2), 1, "dirt"),
        _feed(_up(B, 2), 2, "oak_planks"),
        _feed(_up(B, 2), 3, "cobblestone"),
        _feed(_up(B, 2), 4, "iron_ingot"),
        _dump(B, 'id: "minecraft:iron_ingot"', label="the fifth item arrives — all five fit the aHopperSize 5 column (:202)"),
        _dump(B, "Size: 5"),
        _dump(B, 'id: "minecraft:oak_planks"'),
        _dump(B, 'id: "minecraft:cobblestone"'),

        # ------------------------------------------------------------------
        phase("B2: bronze retention (3-slot anchor, four distinct items — the fourth backs up into the vanilla hopper)"),
        Step(f"setblock {F(_up(B2, 2))} {CHEST}", expect="Changed the block"),
        Step(f"setblock {F(_up(B2, 1))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(B2)} gt6:hopper_bronze[facing=down]", expect="Changed the block"),
        Step(f"setblock {F(_down(B2, 1))} {STONE_FLOOR}", expect="Changed the block"),
        _feed(_up(B2, 2), 0, "stone"),
        _feed(_up(B2, 2), 1, "dirt"),
        _feed(_up(B2, 2), 2, "oak_planks"),
        _feed(_up(B2, 2), 3, "cobblestone"),
        _dump(B2, 'id: "minecraft:oak_planks"', label="the third item lands — the bronze column is now full"),
        _dump(B2, "Size: 3"),
        _dump(_up(B2, 1), 'id: "minecraft:cobblestone"', label="the fourth item stays in the vanilla hopper — the 3-slot refusal"),
        _dump(B2, 'id: "minecraft:stone"'),
        _dump(B2, 'id: "minecraft:dirt"'),

        # ------------------------------------------------------------------
        phase("C: mode NBT arm (data modify -> BE.load clamp -> data get fresh save)"),
        Step(f"setblock {F(C)} gt6:hopper_bronze[facing=north]", expect="Changed the block"),
        Step(f"data modify block {F(C)} mode set value 16", expect="Modified"),
        _dump(C, "mode: 16b", poll=5.0),
        Step(f"data modify block {F(C)} mode set value 100", expect="Modified"),
        _dump(C, "mode: 64b", poll=5.0, label="the load-time clamp 0..64 (the screwdriver cycle's upper bound)"),
        Step(f"data modify block {F(C)} mode set value 0", expect="Modified"),

        # ------------------------------------------------------------------
        phase("D: queue FIFO order (stone enters first, leaves first — the (Slot, id) pairs pin the order)"),
        Step(f"setblock {F(_up(D, 2))} {CHEST}", expect="Changed the block"),
        Step(f"setblock {F(_up(D, 1))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(D)} gt6:queue_hopper_steel[facing=down]", expect="Changed the block"),
        Step(f"setblock {F(_down(D, 1))} {CHEST}", expect="Changed the block"),
        _feed(_up(D, 2), 0, "stone", 2),
        _dump(_down(D, 1), 'Slot: 0b, id: "minecraft:stone"', label="the oldest exits the head first"),
        _feed(_up(D, 2), 1, "dirt", 2),
        _dump(_down(D, 1), 'Slot: 1b, id: "minecraft:dirt"', label="the younger follows — FIFO, not LIFO"),

        # ------------------------------------------------------------------
        phase("E: top item suction (a dropped entity over the open top — the WD.suck port)"),
        Step(f"setblock {F(E)} gt6:hopper_steel[facing=north]", expect="Changed the block"),
        Step(f"summon minecraft:item {E.x + 0.5} {E.y + 1.3} {E.z + 0.5} "
             '{Item:{id:"minecraft:dirt",Count:7b},PickupDelay:0s}', expect="Summoned"),
        _dump(E, "Count: 7b", poll=15.0, label="the whole entity stack lands in one slot"),
        _dump(E, 'id: "minecraft:dirt"', poll=5.0),

        # ------------------------------------------------------------------
        phase("F: minecart interception (rail over the hopper + chest minecart — the CONTAINER_ENTITY_SELECTOR arm)"),
        Step(f"setblock {F(_up(F_COL, 1))} minecraft:rail", expect="Changed the block"),
        Step(f"setblock {F(F_COL)} gt6:hopper_bronze[facing=north]", expect="Changed the block"),
        Step(f"summon minecraft:chest_minecart {F_COL.x + 0.5} {F_COL.y + 1.0} {F_COL.z + 0.5} "
             '{Items:[{Slot:0b,id:"minecraft:iron_ingot",Count:8b}]}', expect="Summoned"),
        _dump(F_COL, 'id: "minecraft:iron_ingot"', poll=15.0, label="the cart drains through the rail intercept"),

        # ------------------------------------------------------------------
        phase("cleanup: the entities a fill-air cannot reach (the pass-2 hygiene)"),
        Step(f"kill @e[type=minecraft:chest_minecart,x={F_COL.x - 2},y={F_COL.y},z={F_COL.z - 2},dx=4,dy=4,dz=4]",
             expect="Killed", allow_failed=True),
        Step(f"kill @e[type=minecraft:item,x={E.x - 8},y={E.y},z={E.z - 2},dx=36,dy=4,dz=4]",
             expect="Killed", allow_failed=True),
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
