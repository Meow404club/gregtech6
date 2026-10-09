#!/usr/bin/env python3
"""spray-paint-domain-two-way — the paint SYNC-face bidirectional live acceptance chain.

Chain semantics (task spray-paint-domain-two-way; the data-face write point itself is
the paintable chain's territory — this chain pins the face the FIX touched, the
two-channel SYNC payload that TileEntityBase03TicksAndSync.load consumes on the client):

  A the spray half: paint dye 1 (Red) live, then the update-tag bytes carry BOTH keys —
    `execute if data block P "gt.painted"` / `"gt.color"` (the quoted-segment node of
    NbtPathArgument.parseNode — the key names contain dots, the unquoted read stops
    there) and the whole-BE dump pins the verbatim values (gt.color: 16711680 =
    0xFF0000, gt.painted: 1b). This dump IS the ClientboundBlockEntityDataPacket
    payload: getUpdatePacket = ClientboundBlockEntityDataPacket.create(be) packs
    getUpdateTag() = saveWithoutMetadata() (TileEntityBase03TicksAndSync), and
    ChunkHolder.broadcastChanges → broadcastBlockEntity pushes it to every tracker
    (vanilla ChunkHolder.java:244-252) — so `data get block` is byte-level "what the
    client will load".

  B the unpaint half (symptom 34's live input): `gt6machine unpaint P` restores the
    row material, and the NEXT update tag carries NEITHER key —
    `execute unless data block P "gt.painted"` / `"gt.color"` → Test passed. The
    key-less tag is exactly the contract the client load() reads unconditionally
    since this card (the former contains-guard kept the client BE painted forever).

  C the reprise: the channel re-arms after an unpaint (dye 14 lands #FF8000 on the
    freshly unpainted machine — the direct-store half of the 04:227-235 routing, and
    the sync keys return) — bidirectional over the whole life cycle, not one-shot.

  D the negative: a vanilla block carries no IPaintableTE face (the paintable chain's
    D arm, repeated as the boundary of the domain).

The two framework passes are the [0, 0] idempotency proof. The client-consumption half
(tag → load → PAINT ModelData → tint) is pinned offline in
GTPaintableTest.unpaintSyncTagClearsTheStaleClientPaint /
paintSyncTagRetintsTheModelDataThroughTheLoadFace and the rebuild trigger in
GTRenderUpdatesTest (the pair); the pixel face itself is the field_test runClient leg.

Run:  GT6_SESSION=off python3 tools/rcon/chains/spray_paint_two_way.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The site — x 820, z 64: a fresh band, clear of every registered cluster
# (the highest neighbours are x762 z100 and the x700 z204..218 strip).
G = gt6world.Site(820, 64, 64, dx=1)

P = "820 64 64"

steps = []

# --------------------------------- A: the spray half — the update tag carries the keys
steps += [
    phase("A: spray Red — the write point applies and the SYNC tag (the exact BE-data packet payload) carries both keys"),
    Step(f"gt6machine shredder place {P}", expect="placed at 820, 64, 64"),
    Step(f"gt6machine paint {P} 1",
         expect="dye 1 (Red #FF0000), RGB #FFFFFF->#FF0000 painted=true (APPLIED)"),
    # the update-tag keys, addressed as a quoted path node (NbtPathArgument.parseNode:
    # case '"'/'\'' reads the WHOLE quoted string — dots are the unquoted separator)
    Step(f'execute if data block {P} "gt.painted"', expect="Test passed"),
    Step(f'execute if data block {P} "gt.color"', expect="Test passed"),
    Step(f"data get block {P}", expect="gt.color: 16711680"),  # 0xFF0000, the client mRGBa the tint arm consumes
    Step(f"data get block {P}", expect="gt.painted: 1b"),
]

# ----------------------------- B: the unpaint half — the next update tag is KEY-LESS
steps += [
    phase("B: unpaint — the row material returns and the SYNC tag carries NEITHER key (the symptom34 client contract)"),
    Step(f"gt6machine unpaint {P}",
         expect="dye none (unpaint), RGB #FF0000->#D2823C painted=false (APPLIED)"),
    Step(f'execute unless data block {P} "gt.painted"', expect="Test passed"),
    Step(f'execute unless data block {P} "gt.color"', expect="Test passed"),
]

# --------------------------------------------------- C: the reprise — the channel re-arms
steps += [
    phase("C: reprise — Orange on the freshly unpainted machine stores directly and the sync keys return"),
    Step(f"gt6machine paint {P} 14",
         expect="dye 14 (Orange #FF8000), RGB #D2823C->#FF8000 painted=true (APPLIED)"),
    Step(f'execute if data block {P} "gt.color"', expect="Test passed"),
]

# -------------------------------------------------- D: the non-paintable negative
steps += [
    phase("D: the negative — a vanilla block carries no IPaintableTE face, the spray refuses"),
    Step(f"setblock {P} minecraft:stone", expect="Changed the block"),
    Step(f"gt6machine paint {P} 5", expect="No paintable GT6 TileEntity", allow_failed=True),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step(f"setblock {P} air", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="spray_paint_two_way",
    slug="spraypaint2way",
    sites=gt6world.declare_sites(G),
    preferred_ports=(26190, 26200),      # this card's pinned rcon/query pair (fresh segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
