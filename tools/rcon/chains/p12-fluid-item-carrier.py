#!/usr/bin/env python3
"""p12-fluid-item-carrier — the item-carrier acceptance chain (task p12-fluid-item-carrier).

The break-carries-content fix, live: a filled wood barrel is destroyed with the
real drop path (`fill ... air destroy` — the getDrops seam, the loot-table-less
family dropped NOTHING pre-card), and the dropped barrel item reports its content
through its own FLUID_HANDLER_ITEM capability via `gt6tank show` — the fix's live
proof. The place-back half rides the same-key declaration: the drop item's tank
compound IS the BE's compound (NBT_TANK "tank" both sides), so injecting the exact
drop shape through `data modify block` reloads it via the real BE load path (the
p11 filter-seed precedent); the wood barrel then drains to a true empty (keepsFilter=F).

Scenario (per pass, the card's acceptance ②):
  A place barrel_wood -> fill water 8000 -> show asserts 8000
  B `setblock air destroy` (the real player-mined drop path — destroyBlock ->
    dropResources -> getDrops) -> tp the fresh drop item back into the scan
    window (the drop physics drifts the entity away and this world's floor is
    void — the tp is scenery control, the NBT it carries is the real drop's) ->
    show asserts the DROPPED item holds 8000 (getDrops NBT + the item
    capability, live) -> the item is killed
  C place back + the drop's tank compound through the real load -> show asserts 8000 kept
  D drain 8000 -> show asserts true empty (holds 0, wood keepsFilter=F)
Pass 2 is the idempotency proof (the framework reruns everything on the cleaned world).

Run:  python3 tools/rcon/chains/p12-fluid-item-carrier.py
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

BARREL = gt6world.Site(30, 64, 30)
P = F(BARREL)
# the item-entity kill sweep around the site (items are not touched by the bbox fill)
KILL = f"kill @e[type=item,x={BARREL.x - 2},y={BARREL.y - 2},z={BARREL.z - 2},dx=4,dy=6,dz=4]"
# pin the fresh drop item into the show window: the tp drops it from y=64.2 onto the
# 5x5 stone pad (fill below) — it settles in ~6 ticks and STAYS (an unpedestaled item
# drifts 2+ blocks off a 1x1 pad and this world's floor is a 120-block void fall)
PAD = f"fill {BARREL.x - 2} {BARREL.y - 1} {BARREL.z - 2} {BARREL.x + 2} {BARREL.y - 1} {BARREL.z + 2} stone"
TP_DROP = f"tp @e[type=item,x={BARREL.x},y={BARREL.y},z={BARREL.z},distance=..16,limit=1] {BARREL.x + 0.5} {BARREL.y + 0.2} {BARREL.z + 0.5}"
# the item-entity kill sweep around the site (items are not touched by the bbox fill)
KILL = f"kill @e[type=item,x={BARREL.x - 2},y={BARREL.y - 2},z={BARREL.z - 2},dx=4,dy=6,dz=4]"
# the exact compound shape the drop carries (FluidStack NBT: FluidName + Amount) —
# the same-key round trip through the real BE load. On the 21.1 node the BE load
# face is the codec (FluidStack.parseOptional — the forge shape fails it to EMPTY,
# probe-verified 2026-09-04: legacy keys land "holds 0", codec keys land 8000), so
# the injection command swaps the KEY SHAPE only — same verified target, same
# expect (task p15-dual-gate-closure; the shape rides Step.node_cmds).
DROP_TANK = '{FluidName:"minecraft:water",Amount:8000}'
DROP_TANK_1211 = '{id:"minecraft:water",amount:8000}'


CHAIN = Chain(
    name="p12-fluid-item-carrier",
    slug="p12fic",
    sites=gt6world.declare_sites(BARREL),
    preferred_ports=(25705, 25715),      # this card's pinned rcon/query pair
    steps=[
        phase("A: place + fill 8000 (the BE content baseline)"),
        Step(KILL, allow_failed=True),   # the fill cleanup spares item entities — sweep them
        Step(PAD),                        # the drop-catch pad (re-laid every pass, cleanup clears it)
        Step(f"setblock {P} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank fill {P} minecraft:water 8000", expect="filled 8000/8000"),
        Step(f"gt6tank show {P}", expect="holds 8000/16000 L of minecraft:water"),

        phase("B: the real break — the drop item carries the content (the lost-fluid fix, live)"),
        Step(f"setblock {P} air destroy", expect="Changed the block"),
        Step(TP_DROP, expect="Teleported Wooden Barrel", sleep=1),
        Step(f"gt6tank show {P}", expect="dropped barrel item holds 8000/16000 L of minecraft:water"),
        Step(KILL, expect="Killed"),

        phase("C: place back — the drop's tank compound reloads into the placed barrel"),
        Step(f"setblock {P} gt6:barrel_wood", expect="Changed the block"),
        Step(f"data modify block {P} tank set value {DROP_TANK}", expect="Modified block data", sleep=1,
             node_cmds={"1.21.1": f"data modify block {P} tank set value {DROP_TANK_1211}"}),
        Step(f"gt6tank show {P}", expect="holds 8000/16000 L of minecraft:water"),

        phase("D: drain 8000 — the wood barrel drains to a true empty (keepsFilter=F)"),
        Step(f"gt6tank draw {P} 8000", expect="drawn 8000/8000"),
        Step(f"gt6tank show {P}", expect="holds 0/16000 L of nothing"),
        Step(KILL, allow_failed=True),
    ],
)


if __name__ == "__main__":
    main(CHAIN)
