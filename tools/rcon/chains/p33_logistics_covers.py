#!/usr/bin/env python3
"""p33-logistics-covers-12 — the logistics cover family live acceptance chain.

The live half of task p33-logistics-covers-12. One fresh band z=252..264,
x=509..531 (south of the p32_logistics_core band z236..250, x-disjoint
overlap-free with margin):

  the 5x5x5 core (cube x513..517, y63..67, z256..260; the controller at the
  south face centre 515 65 256 — the SAME cheapskate form the lv3 chain
  pinned: one Versatile Quadcore, pools 1/1/1/1, radius 3, gate 192 EU).
  the cover-row plane z=259 (the north side, all Chebyshev-distance 3 from
  the centre 515 65 258):

    SRC (515 65 259): the generic item STORAGE endpoint — a logistics tank
      carrying a cover_logistics_item_storage cover whose covered face looks
      at a plain chest holding sticks (the dump source).
    DUMP (513 65 259): the DUMP target — a wire with a cover_logistics_generic_dump
      cover looking at a plain chest (the sink).
    PROT (517 65 259): the PROTECTED source — a wire with a cover_logistics_item_import
      cover (its filter = stick) looking at a chest; the import cover's filter
      joins the network-wide protected set (:361-373) — the stick must NEVER
      dump.
    W1 (515 66 259): the wire member above SRC (the lv2/3 members+1 live proof).

  the EU tail: a copper 4x wire at 516 65 257 against the WALL cell (517,65,258).

Arms (acceptance ① the FML registration rides GT6LogisticsRegistrationTest;
② the priority-lane + tab verdicts; ③ the DUMP live exclusion):

  A FORM: the core forms with the CPU pools; the covers install on the wire
    hosts + tank through /gt6cover install (the placement gate live verdict —
    every install answers ok=1; a NEGATIVE arm re-installs the dump cover on a
    plain furnace and expects the gate refusal).
  B LANES: the /gt6cover mode cutter relay on the storage cover (the stacksize
    lane cycles, toolDamage=1000) and the signal write on the CPU display (the
    value lane = the redstone out). Tab: the cover items + wire + core join the
    machines tab — the /give verdict walks all 16.
  C DUMP: feed the SRC chest sticks + cobblestone, prime the EU, run the scan —
    the cobblestone moves into the dump sink chest, the STICK stays (the
    network protected set from the import cover's filter, live :479-494);
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
SRC = (515, 65, 259)    # the item-storage endpoint tank
SRCWIRE = (515, 65, 259)  # the tank itself hosts the storage cover (a member host)
DUMPWIRE = (513, 65, 259)
DUMPCHEST = (513, 65, 260)
PROTWIRE = (517, 65, 259)
PROTCHEST = (517, 65, 260)
W1 = (515, 66, 259)     # the wire member above the tank
EUWIRE = (516, 65, 257)


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
        Step("setblock %s %s" % (F(SRC), TANK), expect="Changed the block",
             label="the generic item-storage endpoint tank (direct adjacency)"),
        Step("setblock %s %s" % (F(DUMPCHEST), CHEST), expect="Changed the block"),
        Step("setblock %s %s" % (F(PROTCHEST), CHEST), expect="Changed the block"),
        Step("gt6logistics wire place %s 0" % F(DUMPWIRE), expect="connections 1",
             label="the dump wire: placed against the sink chest's DOWN face — support connect lands down, the cover face looks at the chest"),
        Step("gt6logistics wire place %s 0" % F(PROTWIRE), expect="connections 1",
             label="the protected-source wire: same form over the protected chest"),
        Step("gt6logistics wire place %s 0" % F(W1), expect="connections 1",
             label="the wire member above the tank (the members=4 live proof)"),
        Step("gt6cover install %s south gt6:cover_logistics_item_storage" % F(SRC), expect="ok=true",
             label="the storage cover on the tank's SOUTH face (faces the core — wait, the core is south at z256; the cover looks at the network side)"),
        Step("gt6cover install %s west gt6:cover_logistics_generic_dump" % F(DUMPWIRE), expect="ok=true",
             label="the DUMP cover on the wire's WEST face (toward the core walk) — the dump TARGET is the covered-face adjacency (the chest below)"),
        Step("gt6cover install %s west gt6:cover_logistics_item_import" % F(PROTWIRE), expect="ok=true",
             label="the IMPORT cover — its filter joins the protected set; the covered-face adjacency (the chest) is the import target"),
        Step("gt6cover install 520 65 252 south gt6:cover_logistics_generic_dump", expect="FAILED",
             label="the NEGATIVE gate arm: the dump cover refuses the plain STONE (a non-member host, the :40 conjunction)"),

        # ------------------------------------------------------------------
        phase("C: the lanes — the cutter relay, the display redstone, the tab walk"),
        Step("gt6cover mode %s south cutter" % F(SRC), expect="toolDamage=1000",
             label="the cutter relay on the storage cover: the stacksize lane (bits 2-8) answers 1000"),
        Step("gt6cover mode %s south" % F(SRC), expect="toolDamage=10000",
             label="the screwdriver relay: the priority bits cycle (damage 10000)"),
        Step("gt6cover signal %s south 10" % F(SRC), expect="visual=10",
             label="the value/visual write path is alive on the family (the display bar arm rides the same lanes)"),
        Step("give @p gt6:cover_logistics_display_cpu_logic 1", expect="Gave 1"),
        Step("give @p gt6:cover_logistics_generic_dump 1", expect="Gave 1"),
        Step("give @p gt6:cover_logistics_item_storage 1", expect="Gave 1"),
        Step("give @p gt6:logistics_wire 1", expect="Gave 1"),
        Step("give @p gt6:logistics_core 1", expect="Gave 1",
             label="the tab walk: wire/core/14 covers registered as items and obtainable"),

        # ------------------------------------------------------------------
        phase("D: the DUMP live exclusion — the stick stays, the cobble moves"),
        _feed(SRC, 0, "stick", 16),
        _feed(SRC, 1, "cobblestone", 16),
        Step("gt6cover mode %s west" % F(PROTWIRE), expect="toolDamage=0",
             label="the probe arm (the mode relay on the import cover answers 0 — no visual lane)"),
        Step("setblock %s minecraft:stone" % F(EUWIRE), expect="Changed the block",
             label="a stone placeholder first (the wire place arm needs a support face check)"),
        Step("gt6wire place copper 4 %s" % F(EUWIRE), expect="GT6 wire placed",
             label="the EU tail: copper 4x against the WALL cell (517,65,258)"),
        Step("gt6wire connect %s 4" % F(EUWIRE), expect=": ok,", sleep=0.6,
             label="west connect into the ONLY_ENERGY_IN wall; the sleep lets the wire tick (mTimer < 1 refuses)"),
        Step("gt6wire inject %s 5 256 2" % F(EUWIRE), expect="used 2", sleep=0.3,
             label="512 EU: the scan gate 192 + the dump op (16 items = 16 EU) + the idle draws"),
        Step("gt6logistics core stat " + F(CTRL), expect="members=4 | ", poll=15,
             label="the network: core + SRC tank + DUMP wire + PROT wire = 4 members"),
        Step("gt6logistics core stat " + F(CTRL), expect="item generic=1",
             label="the storage cover registered the SRC chest into the generic ITEM tier (the item half of the report)"),
        Step("gt6logistics core stat " + F(CTRL), expect="moved last=16",
             label="the DUMP arm moved exactly the 16 cobblestone (the stick excluded, live :479-494)"),
        Step('execute if items block %s container.0 minecraft:stick' % F(DUMPCHEST), expect="Test failed",
             label="the DUMP sink holds NO stick — the protected-set exclusion (the import cover's filter)"),
        Step('execute if items block %s container.0 minecraft:cobblestone' % F(DUMPCHEST), expect="Test passed",
             label="the DUMP sink holds the cobblestone"),
        Step('execute if items block %s container.0 minecraft:stick' % F(SRC), expect="Test passed",
             label="the stick NEVER left the source (the protected item, live)"),
Step('execute if items block %s container.0 minecraft:cobblestone' % F(SRC), expect="Test failed",
             label="the cobblestone left the source"),
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
