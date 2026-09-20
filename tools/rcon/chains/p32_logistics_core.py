#!/usr/bin/env python3
"""p32-logistics-lv3 — the Logistics Core live acceptance chain.

The live half of task p32-logistics-lv3. One fresh band z=236..250,
x=509..531 (east of the lv2 wire band x486..490, x-disjoint with margin):

  the 5x5x5 core structure (cube x513..517, y63..67, z242..246; the
  controller at the south face centre 515 65 242, facing north — the
  structure centre 515 65 244 sits BEHIND the front), the CPU cells are
  the CHEAPSKATE form: one Versatile Quadcore at the centre, the other 26
  inner cells stay walls (the upstream :132 legal substitute) — pools
  1/1/1/1, radius 3, startup gate 192 EU, capacity 384 EU.
  the endpoint row on the z=241 plane (all Chebyshev-distance 3 from the
  centre — exactly the cubic AoE): tank A (515 65 241, generic, direct to
  the controller), tank C (516 65 241, semi) and tank B (517 65 241,
  filtered) chained by tank-tank adjacency — the :442 joiner-back rule
  makes a wire BETWEEN two tanks unreachable (each wire carries exactly
  ONE support connect, so its far side can never answer the joiner's back
  query), while tank-tank walks answer T on both arms. The wire member w1
  (515 66 241) hangs above A (support connect down) and joins the BFS as
  a member — the members=5 stat row is its live proof. The EU tail: a
  copper 4x wire at 518 65 242 against the WALL cell (517,65,242) — the
  east-face CENTRE (517,65,244) is a vent whose ONLY_LOGISTICS mode
  denies energy (live-proven: the wall relay is the only energy door,
  MultiBlockPartBlockEntity doEnergyInjection -> the core).

Arms (acceptance ① FML registration rides GT6LogisticsCoreRegistrationTest;
② the form/wire/filter live proof; ③ the defrag face):

  A FORM: the /gt6logistics core form verdict with the four CPU pools, the
    FORMED blockstate assert, the wire-chain symmetric handshake (w2
    connections 48 — the lv2 spread form over the tank hop).
  B FILTERS: the tank priority overrides (generic/semi/filtered), the
    identity arm (fill 1 L water, drain it — keepsFilter keeps the filter
    at 0 L), the /gt6logistics core accept verdicts per tier.
  C ROUTING: 512 EU injected over the wall relay, the defrag arm
    generic->Filtered moves 16000 L (the Conversion budget) then 4000 L —
    the semi tank stays EMPTY while the filtered tier accepts (the
    Filtered-before-Semi order, live); the moved totals and the EU move
    cost (divup(16000,250)=64 + divup(4000,250)=16 = 80, the deterministic
    idle-free ledger).

  DUMP: the dump sources/sinks are the cover family (the declared next
  card) — the exclusion face rides the offline
  dumpArmRespectsTheProtectedSet table; live Dump proof defers with the
  covers.

Run:  python3 tools/rcon/chains/p32_logistics_core.py
      python3 tools/rcon/chains/p32_logistics_core.py --node 1.21.1-neoforge
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

A = gt6world.Site(520, 65, 243, dx=11, dy=5, dz=7)  # x509..531, y60..70, z236..250

WALL = "gt6:machine_wall_galvanized_steel"   # 18008
VENT = "gt6:ventilation_unit"                # 18299
PU_V = "gt6:processor_unit_versatile"        # 18200
CORE = "gt6:logistics_core"                  # 17997
TANK = "gt6:barrel_logistics"
STONE = "minecraft:stone"

CTRL = (515, 65, 242)
CENTER = (515, 65, 244)
TANK_A = (515, 65, 241)
TANK_B = (517, 65, 241)
TANK_C = (516, 65, 241)
W1 = (515, 66, 241)   # above A — the wire-member proof (support connect down)
EUWIRE = (518, 65, 242)
EUWIRE = (518, 65, 242)


def _side_row(side, connected, member, attach, neighbour):
    return f"side {side}: connected={connected} member={member} attach={attach} ({neighbour})"


CHAIN = Chain(
    name="p32-logistics-core",
    slug="p32logicore",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25771, 25781),
    steps=[
        # ------------------------------------------------------------------
        phase("A: the structure — the 44 walls + 53 vents + the cheapskate core, controller last"),
        Step("fill %d %d %d %d %d %d air" % (509, 60, 236, 531, 70, 250), expect="filled"),
        Step("forceload add 509 236 531 250"),
        Step("fill 509 64 236 531 64 250 " + STONE, expect="filled", label="the support floor"),
        Step("fill 513 63 242 517 67 246 " + WALL, expect="filled", label="the 125-cell cube, all walls first"),
        Step("fill 514 64 243 516 66 245 " + WALL, expect="filled", label="the inner 3x3x3 stays walls — the cheapskate CPU substitute (:132)"),
        Step("fill 514 64 242 516 66 242 " + VENT, expect="filled", label="the six 3x3 vent faces (54 cells; the controller replaces the south centre)")
        ,
        Step("fill 514 64 246 516 66 246 " + VENT, expect="filled"),
        Step("fill 513 64 243 513 66 245 " + VENT, expect="filled"),
        Step("fill 517 64 243 517 66 245 " + VENT, expect="filled"),
        Step("fill 514 63 243 516 63 245 " + VENT, expect="filled"),
        Step("fill 514 67 243 516 67 245 " + VENT, expect="filled"),
        Step("setblock %s %s" % (F(CENTER), PU_V), expect="Changed the block",
             label="the one Versatile Quadcore — pools 1/1/1/1 (radius 3, gate 192, capacity 384)"),
        Step("setblock %s %s" % (F(CTRL), CORE), expect="Changed the block",
             label="the controller LAST (the onTickFirst forced check is the forming path)"),
        Step("execute if block %s %s[formed=true]" % (F(CTRL), CORE), expect="Test passed", poll=30,
             label="the FORMED blockstate flip"),
        Step("gt6logistics core form " + F(CTRL),
             expect="formed=1 logic=1 control=1 storage=1 conversion=1",
             label="acceptance ②: the form verdict with the four CPU pools"),

        # ------------------------------------------------------------------
        phase("B: the endpoint row — the one-wire hops, the priority overrides"),
        Step("setblock %s %s" % (F(TANK_A), TANK), expect="Changed the block",
             label="tank A: directly adjacent to the controller (the core's south walk, dist 3)"),
        Step("setblock %s %s" % (F(TANK_C), TANK), expect="Changed the block",
             label="tank C: semi — tank-tank adjacency (passive members: both walk arms answer T)"),
        Step("setblock %s %s" % (F(TANK_B), TANK), expect="Changed the block",
             label="tank B: filtered — the C hop (the :442 joiner-back rule makes a wire BETWEEN two tanks unreachable: each wire carries exactly ONE support connect, so tanks chain directly and wires hang off them)"),
        Step("gt6logistics wire place %s 1" % F(W1), expect="connections 1",
             label="the wire member: placed against A's TOP face — the support connect lands down (the :442 back side toward A); the wire itself joins the BFS as a member (members=5 in the stat)"),
        Step("gt6logistics tank priority %s 1" % F(TANK_A), expect="effective fluid priority 1"),
        Step("gt6logistics tank priority %s 3" % F(TANK_B), expect="effective fluid priority 3"),
        Step("gt6logistics tank priority %s 2" % F(TANK_C), expect="effective fluid priority 2"),

        # ------------------------------------------------------------------
        phase("C: the filter arms — the identity, the tier verdicts, the idle-zero gate"),
        Step('gt6logistics core import %s "minecraft:water" 20000' % F(TANK_A), expect="filled 20000/20000"),
        Step('gt6logistics core import %s "minecraft:water" 1' % F(TANK_B), expect="filled 1/1"),
        Step("gt6logistics core export %s 1" % F(TANK_B), expect="drained 1/1 L of minecraft:water",
             label="the identity arm — keepsFilter holds the water filter at 0 L"),
        Step('gt6logistics core accept %s "minecraft:water"' % F(TANK_A),
             expect="priority=1 filter=minecraft:water match=1"),
        Step('gt6logistics core accept %s "minecraft:water"' % F(TANK_B),
             expect="priority=3 filter=minecraft:water match=1",
             label="the FILTERED tier — the kept identity answers at 0 L"),
        Step('gt6logistics core accept %s "minecraft:water"' % F(TANK_C),
             expect="priority=2 filter=null match=1",
             label="the SEMI tier, unfiltered content"),
        Step("gt6logistics core stat " + F(CTRL), expect="power=0",
             label="pre-injection: the :216 startup gate (192 EU) holds the network idle at zero"),

        # ------------------------------------------------------------------
        phase("D: 512 EU over the wall relay — the defrag route, the moved ledger, the EU cost"),
        Step("gt6wire place copper 4 %s" % F(EUWIRE), expect="GT6 wire placed",
             label="the EU tail: copper 4x (256 EU tier) against the WALL cell (517,65,242) — the east-face centre is a VENT whose ONLY_LOGISTICS mode denies energy"),
        Step("gt6wire connect %s 4" % F(EUWIRE), expect=": ok,", sleep=0.6,
             label="west connect into the ONLY_LOGISTICS & ONLY_ENERGY_IN wall (:138); the sleep lets the wire tick — transferElectricity refuses unticked wires (mTimer < 1, upstream :171)"),
        Step("gt6wire inject %s 5 256 2" % F(EUWIRE), expect="used 2", sleep=0.3,
             label="two 256-packets; the inject side is the ENTRY face — transferElectricity floods every connected side BUT it (:175 ALL_SIDES_VALID_BUT), so side 5 (east) drives the flood WEST into the wall relay. 512 EU buys BOTH moves (64+16) plus two idle draws (48) with the second-scan gate (192) still armed"),
        Step("gt6logistics core stat " + F(CTRL), expect="total=20000", poll=15,
             label="both scans: the DEFRAG arm generic->Filtered (:473) moved the 16000 L budget then the 4000 L tail into the FILTERED tier"),
        Step("gt6logistics core export %s 16000" % F(TANK_B), expect="drained 16000/16000 L of minecraft:water",
             label="the filtered tier took the moves (drain 1 of 2)"),
        Step("gt6logistics core export %s 1" % F(TANK_C), expect="drained 0/1",
             label="acceptance ③/priority: the SEMI tank stayed EMPTY — Filtered evaluated first"),
        Step("gt6logistics core export %s 1" % F(TANK_A), expect="drained 0/1",
             label="the generic source is empty"),
        Step("gt6logistics core export %s 3000" % F(TANK_B), expect="drained 3000/3000",
             label="the tail move landed (20000 moved - 17000 drawn = 3000 left)"),
        Step("gt6logistics core stat " + F(CTRL), expect="members=5 | fluid generic=1 semi=1 filtered=1",
             label="the tier registration + the wire member (core + 3 tanks + w1) stayed stable across the scans"),
        Step("gt6logistics core stat " + F(CTRL), expect="total=80",
             label="the EU ledger: divup(16000,250)=64 + divup(4000,250)=16 — idle-free, deterministic"),
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
