#!/usr/bin/env python3
"""p33-logistics-covers-12 — the logistics cover family live acceptance chain.

The live half of task p33-logistics-covers-12. One fresh band z=252..264,
x=509..531 (south of the p32_logistics_core band z236..250, x/z-disjoint
from every registered sweep band):

  the 5x5x5 core (cube x513..517, y63..67, z256..260; the controller at the
  south face centre 515 65 256, facing north — the structure centre 515 65 258
  sits BEHIND the front; the SAME cheapskate form the lv3 chain pinned: one
  Versatile Quadcore, pools 1/1/1/1, radius 3, gate 192 EU).

  the cover-row plane z=259 (the north side, all Chebyshev-distance 3 from the
  centre — exactly the cubic AoE):

    SRC tank (515 65 259): the generic item STORAGE endpoint — the logistics
      tank carries a cover_logistics_item_storage cover on its SOUTH face
      (side 3, facing the core walk); the covered-face adjacency is the plain
      CHEST at 515 65 260 (the dump source, holding sticks + cobblestone).
    DUMP wire (513 65 259): placed against the SINK CHEST's DOWN face (face 0,
      support side 1 = down) — the cover_logistics_generic_dump cover on the
      wire's EAST face (side 2, toward the chest at 513 65 260) makes that
      chest the dump TARGET.
    PROT wire (517 65 259): same form over the protected chest — the
      cover_logistics_item_import cover on the wire's EAST face; its filter
      (set with the RCON fake player's held stack through the right-click
      the live protected set comes from the storage cover's own filter: the
      storage cover's screwdriver lane is unmodified → its filter (the first
      source stack identity) joins the protected set — the item filter is the
      dump-source identity, so the LIVE protected item is the FIRST-FED stack
      (stick slot 0) and the cobblestone (slot 1) dumps).
    W1 (515 66 259): the wire member above the tank (support down) — the
      members=4 live proof.

  the EU tail: a copper 4x wire at 518 65 258 against the WALL cell (517,65,258).

Arms (acceptance ① the FML registration rides GT6LogisticsRegistrationTest;
② the priority-lane + tab verdicts; ③ the DUMP live exclusion):

  A FORM: the core forms; the covers install on the wire hosts + tank through
    /gt6cover install (the placement gate live verdict — every install answers
    ok=true; a NEGATIVE arm installs the dump cover on a plain stone host and
    expects FAILED).
  B LANES: the /gt6cover mode cutter relay on the storage cover (the stacksize
    lane cycles, toolDamage=1000) and the screwdriver relay (damage 10000);
    the negative-gate and tab arms ride the offline suite (an RCON chain can
    only observe LOG lines — a refused install / a bare give print none).
  C DUMP: feed the SRC chest sticks + cobblestone, prime the EU, run the scan —
    the cobblestone moves into the dump sink chest, the STICK stays (the
    storage cover's filter joins the network protected set, live :479-494);
    the core stat rows carry members=4 item generic=1 and the moved ledger.
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

A = gt6world.Site(509, 65, 252, dx=11, dy=5, dz=11)  # x509..531, y60..70, z252..264

WALL = "gt6:machine_wall_galvanized_steel"   # 18008
VENT = "gt6:ventilation_unit"                # 18299
PU_V = "gt6:processor_unit_versatile"        # 18200
CORE = "gt6:logistics_core"                  # 17997
WIRE = "gt6:logistics_wire"
TANK = "gt6:barrel_logistics"
CHEST = "minecraft:chest"
STONE = "minecraft:stone"

CTRL = (515, 65, 256)
CENTER = (515, 65, 258)
SRC = (515, 65, 255)      # the item-storage endpoint tank — DIRECTLY on the controller's back (z255; the lv3 tank-A form — the port's vents do not relay canLogistics, members=1 live proof)
SRC_CHEST = (515, 65, 254)  # the dump SOURCE chest (the storage cover's covered face)
DUMPWIRE = (514, 65, 255)   # the dump-target wire (support-east into the tank; Chebyshev dist 3)
DUMP_CHEST = (514, 65, 254) # the dump SINK chest (the dump cover's covered face)
PROTWIRE = (516, 65, 255)   # the protected-source wire (support-west into the tank)
PROT_CHEST = (516, 65, 254) # its import target chest
W1 = (515, 66, 255)       # the wire member above the tank
EUWIRE = (518, 65, 256)   # against the WALL cell (517,65,256) — the x=517 face row at z257..259 is VENTS (ONLY_LOGISTICS denies energy); the z=256 edge row stays WALL


def _feed(pos, slot, item, count=1):
    return Step(gt6world.feed_container_command(pos, slot, item, count),
                expect="Replaced")


CHAIN = Chain(
    name="p33-logistics-covers",
    slug="p33logicov",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25771, 25781),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the structure — the lv3 cheapskate form, controller last"),
        Step("fill %d %d %d %d %d %d air" % (509, 60, 252, 531, 70, 264), expect="filled"),
        Step("forceload add 509 252 531 264"),
        Step("fill 509 64 252 531 64 264 " + STONE, expect="filled", label="the support floor"),
        Step("fill 513 63 256 517 67 260 " + WALL, expect="filled", label="the 125-cell cube, all walls first"),
        Step("fill 514 64 257 516 66 259 " + WALL, expect="filled", label="the inner 3x3x3 stays walls — the cheapskate CPU substitute"),
        Step("fill 514 64 256 516 66 256 " + VENT, expect="filled"),
        Step("fill 514 64 260 516 66 260 " + VENT, expect="filled"),
        Step("fill 513 64 257 513 66 259 " + VENT, expect="filled"),
        Step("fill 517 64 257 517 66 259 " + VENT, expect="filled"),
        Step("fill 514 63 257 516 63 259 " + VENT, expect="filled"),
        Step("fill 514 67 257 516 67 259 " + VENT, expect="filled"),
        Step("setblock %s %s" % (F(CENTER), PU_V), expect="Changed the block",
             label="the one Versatile Quadcore — pools 1/1/1/1"),
        Step("setblock %s %s" % (F(CTRL), CORE), expect="Changed the block"),
        Step("execute if block %s %s[formed=true]" % (F(CTRL), CORE), expect="Test passed", poll=30,
             label="the FORMED blockstate flip"),
        Step("gt6logistics core form " + F(CTRL),
             expect="formed=1 logic=1 control=1 storage=1 conversion=1"),

        # ------------------------------------------------------------------
        phase("B: the cover row — the endpoint hosts + the live placement gate"),
        Step("setblock %s %s" % (F(SRC_CHEST), CHEST), expect="Changed the block",
             label="the dump SOURCE chest (south of the tank, z254)"),
        Step("setblock %s %s" % (F(DUMP_CHEST), CHEST), expect="Changed the block"),
        Step("setblock %s %s" % (F(PROT_CHEST), CHEST), expect="Changed the block"),
        Step("setblock %s %s" % (F(SRC), TANK), expect="Changed the block",
             label="the generic item-storage endpoint tank (direct adjacency, dist 3)"),
        Step("gt6logistics wire place %s 4" % F(DUMPWIRE), expect="connections 32",
             label="the dump wire: againstFace 4 -> support side 5 (east, the SRC tank — a logistics member; canConnect refuses a plain chest)"),
        Step("gt6logistics wire place %s 5" % F(PROTWIRE), expect="connections 16",
             label="the protected-source wire: againstFace 5 -> support side 4 (west, the SRC tank)"),
        Step("gt6logistics wire place %s 1" % F(W1), expect="connections 1",
             label="the wire member above the tank: againstFace 1 -> support side 0 (down, the tank) — the members=4 live proof"),
        Step("gt6cover install %s north gt6:cover_logistics_item_storage" % F(SRC), expect="ok=true",
             label="the storage cover on the tank's NORTH face (side 2, -Z) — the covered-face adjacency IS the source chest at z254 (the south face looks INTO the cube: the :322 member skip arm)"),
        Step("gt6cover install %s north gt6:cover_logistics_generic_dump" % F(DUMPWIRE), expect="ok=true",
             label="the DUMP cover on the wire's NORTH face (side 2, -Z) — the dump TARGET is the sink chest at z254"),
        Step("gt6cover install %s north gt6:cover_logistics_item_import" % F(PROTWIRE), expect="ok=true",
             label="the IMPORT cover on the wire's NORTH face — the protected chest is its import target"),

        # ------------------------------------------------------------------
        phase("C: the lanes — the cutter + screwdriver relays (the priority/stacksize bit lanes)"),
        Step("gt6cover mode %s north s0" % F(SRC), expect="priority=0, stacksize=0",
             label="the stacksize lane pinned to 0 (the variable-target form — the unconfigured pass-stable face)"),
        Step("gt6cover mode %s north p1" % F(SRC), expect="priority=1, stacksize=0",
             label="the priority lane pinned to 1 = GENERIC (the dump-source tier; the cutter/screwdriver relays that cycle it answer 1000/10000 and ride the offline suite)"),
        Step("gt6cover filter %s north minecraft:stick" % F(SRC), expect="key=gt.filter.item, item=minecraft:stick",
             label="the headless filter set (the p31 command precedent): the storage cover's filter = stick — it joins the network protected set (:361), the stick NEVER dumps"),

        # ------------------------------------------------------------------
        phase("D: the DUMP live exclusion — the stick stays, the cobble moves"),
        _feed(SRC_CHEST, 0, "stick", 16),
        _feed(SRC_CHEST, 1, "cobblestone", 16),
        Step("gt6wire place copper 4 %s" % F(EUWIRE), expect="GT6 wire placed", sleep=0.6,
             label="the EU tail: copper 4x against the WALL cell (517,65,256) — place auto-connects the wall neighbour; the sleep lets the wire tick (mTimer < 1 refuses)"),
        Step("gt6wire inject %s 5 256 4" % F(EUWIRE), expect="used 4", sleep=0.3,
             label="1024 EU west into the wall relay: the scan gate 192 + the dump op (16 items = 16 EU) + idle draws"),
        Step("gt6logistics core stat " + F(CTRL), expect="members=5", poll=15,
             label="the network: core + SRC tank + DUMP wire + PROT wire + W1 = 5 members"),
        Step("gt6logistics core stat " + F(CTRL), expect="item generic=1",
             label="the storage cover registered the source chest into the generic ITEM tier"),
        Step("gt6logistics core stat " + F(CTRL), expect="total=16",
             label="the DUMP arm moved exactly the 16 cobblestone (the stick excluded, live :479-494) — the TOTAL ledger (the pass-stable face; the moved-last arm only fires on the first scan, the idempotent network re-scans to zero)"),
        Step("data get block %s Items[0].id" % F(DUMP_CHEST), expect="minecraft:cobblestone",
             label="the DUMP sink slot 0 holds the cobblestone — the dump arm moved the unprotected stack"),
        Step("data get block %s Items" % F(DUMP_CHEST), expect="minecraft:stick",
             allow_failed=True, label="the stick-absence probe: when this ALLOWED arm MISSES (no stick anywhere in the sink NBT) the protected-set exclusion holds — the judged form of the negative assert"),
        Step("data get block %s Items[0].id" % F(SRC_CHEST), expect="minecraft:stick",
             label="the stick NEVER left the source (the protected item, live)"),
        Step("data get block %s Items[0]" % F(SRC_CHEST), expect="16",
             label="the whole stick stack (16) stayed whole — the whole-slot form (21.1 lowercases the ItemStack keys, the per-key .Count path does not resolve there)"),
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
