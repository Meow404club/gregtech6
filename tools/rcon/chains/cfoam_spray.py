#!/usr/bin/env python3
"""c-foam-pipe-spray — the pipe C-Foam live chain (declarative framework, the
pipe_owner shape).

Chain semantics (task c-foam-pipe-spray ACCEPTANCE ② — "place→spray owned→stat 断言
foam/owner→dry→非 owner break 进度 0→removefoam reject（非 owner）→owner removefoam ok→
拆管再置回流断言；pipe_owner 链回归 [0,0]"):

  A the baseline arm: a fresh pipe stats "foam false dried false foamOwned false" — the
    unfoamed default (upstream 10ConnectorRendered:57 mFoam=F/mFoamDried=F).

  B the spray arm (spec ⑧): `spray <pos> <owned> [dye] [ownerUuid]` drives the SAME gated
    applyFoam face the item's useOn calls (upstream :159-166): the owned spray sets
    foam/dried=false/ownable/owner (stat 断言) — the runtime owner write point. A second
    spray is REJECTED while wet (the :160 mFoam arm — even the owner cannot re-spray).

  C the dry arm: `dry <pos>` drives the no-gate dryFoam (:169-174) — the foam hardens.
    The dried lock is then verified LIVE through the hoe path: the console toggle
    (null = non-owner) is REJECTED (the third clause !mFoamDried fell away, :153-156).
    NOTE the break-progress arm ("非 owner break 进度 0") is a client-Player path — no
    Player is constructible over RCON; its semantics live in GTPipeFoamTest
    .thirdClauseFlipsOnDrying (the ownerDestroyProgress deny-to-0 seam) over the same
    allowInteraction predicate this chain exercises live (the P24 chain's identical
    deferral, documented in its docstring).

  D the removefoam arms: console (null = non-owner) → REJECTED; the owner → ok, and the
    stat shows the full four-field reset + unpaint (foam false ownable false owner none,
    upstream :177-183).

  E the round-trip arm (spec ⑦): spray owned + dry again, /data get block asserts the
    world keys gt.foamed/gt.foamdried/gt.ownable, then setblock air (the console destroy
    seam — ownership does not gate /setblock, the P24 pattern) and /data get entity on
    the dropped item asserts the loot carry function delivered the three keys into the
    BlockEntityTag compound (the GT6PipeBlockLoot foam carry). The carrier is per-leg:
    1.20.1 copies through the item tag NBT (Item.tag.BlockEntityTag) while the 21.1 leg
    reads the components envelope (Item.components."minecraft:custom_data"
    .BlockEntityTag) — the copy_custom_data mirror rewrite (task neo-loot-copy-
    custom-data) lands the SAME BlockEntityTag.'gt.*' relative paths inside the
    minecraft:custom_data component, so the wire path is the only fork (Step.node_cmds,
    the act component-envelope precedent; the value rendering "gt.foamed: 1b" is
    leg-identical, the shared expect stays). The RE-PLACE half (BlockEntityTag merge →
    onPlaced records the new placer) is vanilla machinery + the GTFluidPipeBlockItem
    pre-merge — its semantics live in
    GTPipeFoamTest.foamNbtRoundTripsAndOwnerDoesNotRideItems; the trailing kill is then a
    POSITIVE probe on both legs (pre-P28 the neo leg hit 'No entity was found' here —
    the loot parse death meant the drop never existed).

passes=2 is the idempotency proof (the [0,0] of this chain); the pass-open bbox cleanup
re-airs the pipe band between passes, and each pass kills the leftover item entities.

Run:  GT6_SESSION=off python3 tools/rcon/chains/cfoam_spray.py
Dual: python3 tools/rcon/chains/cfoam_spray.py --node 1.21.1-neoforge
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

# the lock identity — a fixed, log-readable UUID (no Player is ever constructed;
# the console carries null and this is the sprayer/owner the chain records)
UUID_A = "11111111-2222-3333-4444-555555555555"

# the pipe band x=397..398, z=20 — clear of the neighbours (P21 370/378, dye 386,
# pipe 393/394); A = the foam subject, B = an unused support probe west of A
A = gt6world.Site(398, 64, 20)
B = gt6world.Site(397, 64, 20)
PA, PB = F(A), F(B)

# the item-entity selector around A (the drop lands inside the block)
ITEMS_NEAR_A = f"@e[type=item,limit=1,sort=nearest,distance=..6,x={A.x},y={A.y},z={A.z}]"
steps = []

# ------------------------------------------------- A: the baseline (no foam)
steps += [
    phase("A: baseline — the fresh pipe carries no foam"),
    Step(f"gt6pipe place {PA} 1", expect="GT6 pipe placed at"),
    Step(f"gt6pipe stat {PA}", expect="ownable false owner none foam false dried false foamOwned false"),
]

# ------------------------------------------------- B: the spray arm (owned)
steps += [
    phase("B: spray owned — the applyFoam write point sets foam/ownable/owner; re-spray rejected while wet"),
    Step(f"gt6pipe spray {PA} 1 0 {UUID_A}", expect="foam applied, foam true dried false foamOwned true"),
    Step(f"gt6pipe stat {PA}", expect=f"ownable true owner {UUID_A} foam true dried false foamOwned true"),
    Step(f"gt6pipe spray {PA} 1", expect="REJECTED", allow_failed=True),  # the :160 mFoam arm — wet refuses everyone
]

# ------------------------------------------------- C: the dry arm + the dried lock live
steps += [
    phase("C: dry — the no-gate dryFoam; the dried lock rejects the console toggle (third clause live)"),
    Step(f"gt6pipe dry {PA}", expect="ok, foam true dried true"),
    Step(f"gt6pipe stat {PA}", expect="foam true dried true foamOwned true"),
    Step(f"gt6pipe toggle {PA} 1", expect="FAILED", allow_failed=True),  # dried lock denies the non-owner (upstream :155)
    # the break-progress half of the lock is a client-Player path (see the docstring note)
]

# ------------------------------------------------- D: the removefoam arms
steps += [
    phase("D: removefoam — non-owner REJECTED, owner ok, the four-field reset + unpaint"),
    Step(f"gt6pipe removefoam {PA}", expect="REJECTED, foam true dried true", allow_failed=True),
    Step(f"gt6pipe removefoam {PA} {UUID_A}", expect="ok, foam false dried false ownable false owner none"),
    Step(f"gt6pipe stat {PA}", expect="ownable false owner none foam false dried false foamOwned false"),
]

# ------------------------------------------------- E: the round-trip (world NBT + the dropped item)
# the per-leg item path: 1.20.1 = the tag NBT face, 1.21.1 = the components envelope —
# the copy_custom_data rewrite (neo-loot-copy-custom-data) keeps the BlockEntityTag
# compound INSIDE the minecraft:custom_data component, so only the wire path forks
# (Step.node_cmds, the act SELECTOR_REPLACE precedent; the expect is the leg-neutral
# value substring — neither leg echoes the path in the query response).
ITEM_CARRIER = {
    "1.20.1": f"{ITEMS_NEAR_A} Item.tag.BlockEntityTag",
    "1.21.1": f'{ITEMS_NEAR_A} Item.components."minecraft:custom_data".BlockEntityTag',
}
steps += [
    phase("E: round-trip — spray+dry, the world keys, 拆管, the item carries the three keys"),
    Step(f"kill {ITEMS_NEAR_A}", expect="Killed", allow_failed=True),  # clear stale drops first (limit=1 also keeps the kill narrow)
    Step(f"gt6pipe spray {PA} 1 0 {UUID_A}", expect="foam applied"),
    Step(f"gt6pipe dry {PA}", expect="ok, foam true dried true"),
    Step(f"data get block {PA}", expect="gt.foamed: 1b"),  # the whole-BE dump (the dotted keys ride unquoted)
    Step(f"setblock {PA} air destroy", expect="Changed the block"),  # console destroy — the destroy mode drops loot (the /setblock seam)
    Step(f"data get entity {ITEM_CARRIER['1.20.1']}", expect="gt.foamed: 1",
         node_cmds={"1.21.1": f"data get entity {ITEM_CARRIER['1.21.1']}"}),
    Step(f"data get entity {ITEM_CARRIER['1.20.1']}", expect="gt.foamdried: 1",
         node_cmds={"1.21.1": f"data get entity {ITEM_CARRIER['1.21.1']}"}),
    Step(f"data get entity {ITEM_CARRIER['1.20.1']}", expect="gt.ownable: 1",
         node_cmds={"1.21.1": f"data get entity {ITEM_CARRIER['1.21.1']}"}),
    # gt.owner does NOT ride the item — the absence is pinned offline
    # (GTPipeFoamTest.foamNbtRoundTripsAndOwnerDoesNotRideItems), a data-get of an absent
    # path is not a stable cross-leg assertion shape.
    # the positive drop-existence probe: 'No entity was found' was the neo leg's steady
    # state while the 51 mirrored tables died in LootDataType parsing (pre-P28); with the
    # copy_custom_data band live the drop exists on BOTH legs, so the probe is strict —
    # no allow_failed to hide a loot regression behind (the kill also cleans up for pass 2).
    Step(f"kill {ITEMS_NEAR_A}", expect="Killed"),
]

# ------------------------------------------------- teardown
steps += [
    phase("F: teardown — the explicit restore over the band (the pass-open bbox is the backstop)"),
    Step(f"fill 396 62 19 400 66 22 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="c-foam-pipe-spray",
    slug="cfoamspray",
    sites=gt6world.declare_sites(A, B),
    preferred_ports=(26110, 26120),      # this card's pinned rcon/query pair (the 2610x segment, after pipeowner 26109/26119)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
