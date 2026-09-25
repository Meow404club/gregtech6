#!/usr/bin/env python3
"""logistics-lv2 — the logistics wire live acceptance chain.

The live half of task logistics-lv2. One column over the z=248 band
(x=486..490 — east of the P31 retriever band x470..478, x-disjoint with
margin), four entities in a row with a furnace head:

  x=486 furnace (the NON-MEMBER head) | x=487 W0 | x=488 W1 | x=489 W2
  | x=490 an item pipe (the FOREIGN-FAMILY tail, placed after the stats)

Arms (acceptance ② wire place/stat + ③ cover gate):

  A ADJACENCY SPREAD: three wires placed back-to-back through the onPlaced
    handshake — W0 against face 4 (its east support connect lands on the W1
    spot as the open end, W1's placement back-connects it symmetrically), W1
    against face 5, W2 against face 5. The /gt6logistics wire stat rows pin
    the masks (W0=32, W1=48, W2=16) and the per-side verdicts: every
    wire-facing side answers connected=1 member=1 attach=1 — the member
    answer PROPAGATES through the connected sides (upstream :47), the
    adjacency-spread observable. The furnace-facing side of W0 stays
    connected=0 attach=0 (upstream :42-45 — a non-member never attaches) and,
    after the item pipe lands east of W2, its east side reads attach=0
    (GTItemPipeBlockEntity) — different connector families never intersect
    (the base :118 gate).
  B COVER GATE (acceptance ③): /gt6logistics gate on the furnace REFUSES
    (not an ITileEntityLogistics member), on the W1 wire ALLOWS (the wire's
    canLogistics(SIDE_ANY)=T answer — upstream :40). The offline truth tables
    ride AbstractCoverAttachmentLogisticsTest; the canConnect member arm with
    a non-connector member BE (the Lv3 Core/Storage shape) rides the offline
    MemberBlockEntity fixture — no such host exists live yet (declared).

Run:  python3 tools/rcon/chains/logistics_lv2.py
      python3 tools/rcon/chains/logistics_lv2.py --node 1.21.1-neoforge
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

A = gt6world.Site(488, 65, 248, dx=2)  # x486..490: furnace + three wires + the item-pipe tail

FURNACE = "minecraft:furnace"
PIPE = "gt6:brass_item_pipe_medium"
Y = 65
Z = 248
W0 = gt6world.Site(487, Y, Z)
W1 = gt6world.Site(488, Y, Z)
W2 = gt6world.Site(489, Y, Z)
FURN = gt6world.Site(486, Y, Z)
PIPEPOS = gt6world.Site(490, Y, Z)


def _side_row(side, connected, member, attach, neighbour):
    return f"side {side}: connected={connected} member={member} attach={attach} ({neighbour})"


CHAIN = Chain(
    name="logistics-lv2",
    slug="logi",
    sites=gt6world.declare_sites(A),
    preferred_ports=(25751, 25761),
    steps=[
        # ------------------------------------------------------------------
        phase("A: adjacency spread + non-member rejection (wire place/stat)"),
        Step(f"setblock {F(FURN)} {FURNACE}", expect="Changed the block"),
        Step(f"gt6logistics wire place {F(W0)} 4", expect="connections 32",
             label="W0: the east support connect lands as the open end on the W1 spot (upstream :84/:141)"),
        Step(f"gt6logistics wire place {F(W1)} 5", expect="connections 16",
             label="W1: against face 5 → connect west into W0; the :90-94 back-connect loop makes it symmetric"),
        Step(f"gt6logistics wire place {F(W2)} 5", expect="connections 16",
             label="W2: same form west into W1 — the chain grows by placement order alone (the upstream no-rescan shape)"),

        Step(f"gt6logistics wire stat {F(W1)}", expect=_side_row(4, 1, 1, 1, "GTLogisticsWireBlockEntity"),
             label="W1 west: connected member, the answer propagates (upstream :47)"),
        Step(f"gt6logistics wire stat {F(W1)}", expect=_side_row(5, 1, 1, 1, "GTLogisticsWireBlockEntity"),
             label="W1 east: the W2 back-connect landed — the adjacency spread, live"),
        Step(f"gt6logistics wire stat {F(W1)}", expect="connections 48"),

        Step(f"gt6logistics wire stat {F(W0)}", expect=_side_row(5, 1, 1, 1, "GTLogisticsWireBlockEntity"),
             label="W0 east: member toward W1"),
        Step(f"gt6logistics wire stat {F(W0)}", expect=_side_row(4, 0, 0, 0, "FurnaceBlockEntity"),
             label="W0 west: the NON-MEMBER rejection — canConnect :42-45 refuses the furnace"),
        Step(f"gt6logistics wire stat {F(W2)}", expect=_side_row(4, 1, 1, 1, "GTLogisticsWireBlockEntity"),
             label="W2 west: member toward W1"),

        Step(f"setblock {F(PIPEPOS)} {PIPE}", expect="Changed the block",
             label="the foreign-family tail: an item pipe east of W2"),
        Step(f"gt6logistics wire stat {F(W2)}", expect=_side_row(5, 0, 0, 0, "GTItemPipeBlockEntity"),
             label="W2 east: the pipe family never type-intersects WIRE_LOGISTICS (the base :118 gate)"),

        # ------------------------------------------------------------------
        phase("B: cover placement gate (acceptance ③ live)"),
        Step(f"gt6logistics gate {F(FURN)}", expect="refused (host FurnaceBlockEntity)",
             label="a non-member host refuses the logistics plate (upstream :40)"),
        Step(f"gt6logistics gate {F(W1)}", expect="allowed (host GTLogisticsWireBlockEntity)",
             label="a member host mounts — the wire answers canLogistics(SIDE_ANY)=T"),
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
