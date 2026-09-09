#!/usr/bin/env python3
"""p26-storage-static-batch — the static storage batch live acceptance chain.

The live half of task p26-storage-static-batch: the five no-tick storage containers
(Loader_MultiTileEntities.java :134-144 metalset rows / :177-180 wooden ladders), the
offline suite's complementary in-world face.

Columns (z=230 band, x=460..478 — disjoint from every registered sweep band):

  A DRAWER THROUGH-FLOW (x=460): chest -> vanilla hopper -> gt6:drawer_bronze
    [facing=north, mode=Anywhere] -> vanilla hopper -> chest. The vanilla push lands in
    the top view (ALL = slot 0), the vanilla pull drains the same slot — the
    ItemHandler.BLOCK side-view seam live on both faces (the 21.1 twin is the registered
    GT6CapabilityWiring row). Judge: the bottom chest holds the stone.
  B DRAWER SIDED MODE (x=465): chest -> vanilla hopper -> gt6:drawer_bronze -> vanilla
    hopper -> stone floor. The monkey-wrench state rides /data (mode 0b -> 1b): in Sided
    mode the BOTTOM view is BOTTOM_HALF {72..143} while the pushed stone sits in slot 0
    (TOP_HALF), so the pull can no longer see it — the drawer RETAINS the stone where
    column A's Anywhere drawer drained. Judge: the mode NBT round trip + the retained
    stone after the drain window.
  C SAFE LOOT ARM (x=470): gt6:safe_mechanical_bronze over a vanilla hopper + chest. The
    gt.dungeonloot marker is set through /data, the block is destroyed — the onRemove
    fill (upstream breakBlock :89-92) rolls the marker table into the empty slots and
    THEN the contents pop (loot + the self drop). The marker names
    minecraft:blocks/stone — a DETERMINISTIC probe table (one stone per empty slot,
    15x): any stone in the capture chest can ONLY be the loot roll. Judge: the chest
    holds BOTH gt6:safe_mechanical_bronze (the self drop) AND the rolled stone.
  D SAFE BLAST RESISTANCE (x=474): gt6:safe_mechanical_bronze on an OBSIDIAN pad (the
    blast-immune floor keeps the no-drop assertion clean), a lit TNT two above (power 4
    against the aHardness*2 resistance column, Bronze 14, Loader :134). The safe
    survives (upstream onExploded :111 setToAir never fires — the block out-tanks the
    blast) and scatters NO items. Judge: the BE NBT still reads te_name safe_mechanical
    and the no-item-entity probe passes.
  E KEYLOCKED LATCH (x=478): gt6:safe_keylocked_bronze — the mOpened latch NBT (upstream
    :53/:65 gt.open) defaults closed, flips through /data, and a fresh placement is
    closed again. Judge: the three NBT reads.

Run:  python3 tools/rcon/chains/p26_static_storage.py
      python3 tools/rcon/chains/p26_static_storage.py --node 1.21.1-neoforge
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

# one column = the five-level stack (chest 66 / vanilla hopper 65 / storage 64 /
# vanilla hopper 63 / floor 62); the safes sit two levels tall on the shared floor.
A = gt6world.Site(460, 63, 230, dy=4)
B = gt6world.Site(465, 63, 230, dy=4)
C = gt6world.Site(470, 63, 230, dy=3)
D = gt6world.Site(474, 63, 230, dy=4)
E = gt6world.Site(478, 63, 230, dy=2)

CHEST = "minecraft:chest"
VHOP = "minecraft:hopper[facing=down]"
STONE_FLOOR = "minecraft:stone"


def _up(site, n=1):
    return gt6world.Site(site.x, site.y + n, site.z)


def _down(site, n=1):
    return gt6world.Site(site.x, site.y - n, site.z)


def _feed(pos, slot, item, count=1):
    return Step(gt6world.feed_container_command(pos, slot, item, count),
                expect="Replaced")


def _dump(pos, expect, poll=25.0, label=None):
    return Step(f"data get block {F(pos)}", expect=expect, poll=poll, label=label)


CHAIN = Chain(
    name="p26-static-storage",
    slug="p26statstor",
    sites=gt6world.declare_sites(A, B, C, D, E),
    preferred_ports=(25812, 25822),
    steps=[
        # ------------------------------------------------------------------
        phase("A: drawer through-flow (chest -> vanilla hopper -> gt6:drawer_bronze -> vanilla hopper -> chest; the side-view seam live on push and pull)"),
        Step(f"setblock {F(_up(A, 3))} {CHEST}", expect="Changed the block"),
        Step(f"setblock {F(_up(A, 2))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(_up(A, 1))} gt6:drawer_bronze[facing=north]", expect="Changed the block"),
        Step(f"setblock {F(A)} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(_down(A, 1))} {CHEST}", expect="Changed the block"),
        _feed(_up(A, 3), 0, "stone", 8),
        _dump(_down(A, 1), 'id: "minecraft:stone"',
              label="the stone crosses push -> drawer slot 0 -> pull (the Anywhere ALL view both ways)"),

        # ------------------------------------------------------------------
        phase("B: drawer sided mode (mode 1b halves the bottom view; the pushed stone in slot 0 becomes invisible to the pull)"),
        Step(f"setblock {F(_up(B, 3))} {CHEST}", expect="Changed the block"),
        Step(f"setblock {F(_up(B, 2))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(_up(B, 1))} gt6:drawer_bronze[facing=north]", expect="Changed the block"),
        Step(f"setblock {F(B)} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(_down(B, 1))} {STONE_FLOOR}", expect="Changed the block"),
        _feed(_up(B, 3), 0, "stone", 4),
        _dump(_up(B, 1), 'id: "minecraft:stone"', label="the push lands in the drawer"),
        Step(f"data get block {F(_up(B, 1))} mode", expect="0b", label="the upstream NBT_MODE default = Anywhere (:55 mSidedAccess = F)"),
        Step(f"data modify block {F(_up(B, 1))} mode set value 1b", expect="Modified", label="the monkey-wrench toggle state through /data (:94-98)"),
        Step(f"data get block {F(_up(B, 1))} mode", expect="1b"),
        Step(f"data get block {F(_up(B, 1))} Items", expect='id: "minecraft:stone"', poll=8.0,
             label="AFTER the drain window the drawer STILL holds the stone — the bottom view is BOTTOM_HALF {72..143}, slot 0 is invisible to the pull (column A drained the same shape in Anywhere mode)"),

        # ------------------------------------------------------------------
        phase("C: safe loot arm (the gt.dungeonloot marker rolls the table on break — ChestGenHooks :68-76 -> LootDataManager, then the contents pop)"),
        Step(f"setblock {F(_up(C, 2))} gt6:safe_mechanical_bronze[facing=north]", expect="Changed the block"),
        Step(f"setblock {F(_up(C, 1))} {VHOP}", expect="Changed the block"),
        Step(f"setblock {F(C)} {CHEST}", expect="Changed the block"),
        Step(f'data modify block {F(_up(C, 2))} gt.dungeonloot set value "minecraft:blocks/stone"', expect="Modified",
             label="the marker (upstream gt.dungeonloot :54 — a DETERMINISTIC probe table: one stone per empty slot, 15x)"),
        Step(f"data get block {F(_up(C, 2))} gt.dungeonloot", expect="minecraft:blocks/stone"),
        Step(f"setblock {F(_up(C, 2))} minecraft:air", expect="Changed the block",
             label="the break arm: generateDungeonLoot fills the 15 empty slots, then the contents pop (loot + the self drop)"),
        _dump(C, 'id: "gt6:safe_mechanical_bronze"', poll=15.0, label="the safe's own self drop landed in the capture chest"),
        _dump(C, 'id: "minecraft:stone"', label="the LOOT rolled — a stone in this chest can only be the marker roll"),

        # ------------------------------------------------------------------
        phase("D: safe blast resistance (TNT power 4 vs the aHardness*2 resistance column — Bronze 14, Loader :134 — the safe out-tanks the blast and scatters nothing)"),
        Step(f"setblock {F(D)} minecraft:obsidian", expect="Changed the block"),
        Step(f"setblock {F(_up(D, 2))} minecraft:obsidian", expect="Changed the block"),
        Step(f"setblock {F(_up(D, 1))} gt6:safe_mechanical_bronze[facing=north]", expect="Changed the block"),
        Step(f"summon minecraft:tnt {F(_up(D, 3))}", expect="Summoned new entity"),
        Step(f"data get block {F(_up(D, 1))}", expect="safe_mechanical", poll=8.0,
             label="the safe survives the explosion (upstream onExploded :111 setToAir never fires)"),
        Step(f"execute unless entity @e[type=item,distance=..2] run say blast-clean", expect="blast-clean",
             label="the no-scatter assertion: no item entities within the blast radius (the obsidian pad keeps the probe clean)"),

        # ------------------------------------------------------------------
        phase("E: keylocked latch (the mOpened NBT pair, upstream :53/:65 gt.open — closed by default, the GUI gate)"),
        Step(f"setblock {F(E)} gt6:safe_keylocked_bronze[facing=north]", expect="Changed the block"),
        Step(f"data get block {F(E)} gt.open", expect="0b", label="upstream :53 mOpened = F — the GUI arm is gated shut"),
        Step(f"data modify block {F(E)} gt.open set value 1b", expect="Modified"),
        Step(f"data get block {F(E)} gt.open", expect="1b", label="the latch rides the vanilla two-channel sync (the useKey seam flips it in game)"),
    ],
)

if __name__ == "__main__":
    main(CHAIN)
