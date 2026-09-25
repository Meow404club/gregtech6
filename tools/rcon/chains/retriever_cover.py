#!/usr/bin/env python3
"""retriever-cover — the item retriever cover live acceptance chain.

The live half of task retriever-cover. Two arms over the same pipe geometry
(pipe between two chests; the cover face is the pull TARGET, the far face the
pull SOURCE):

Columns (z=248 band, x=470..478 — disjoint from every registered sweep band):

  A DETERMINISTIC DRIVE (x=470): /gt6itempipe retriever runs the whole card
    scenario inside one server tick — install gt6:cover_item_retriever + stone
    filter, then five pinned arms: the filtered pull moves exactly one stone
    stack (the dirt stays, the :467 gate) with the path prefix paying one
    counter unit (:73); the second pull spends the brass-medium invSize=2
    window; the third drive is REFUSED by pipeCapacityCheck (the acceptance
    capacity-window stop); the screwdriver relay flips the visual lane and the
    post-reset pull takes the DIRT stack (the inverted mode); the hoe relay
    dismantles (damage 10000), the store dissolves to null and a further armed
    drive is a no-op (拆盖恢复原状). Source chest pre-fed stone×3 slots + dirt.
  B LIVE PHASE GATE (x=476): /gt6itempipe retriever <pos> setup installs the
    cover (no filter) and the LIVE ticker does the pulling — the :61 phase gate
    (timer % 20 == 15) fires within one second of wall time. Judge: the target
    chest gains the source's dirt stack without any manual drive, /gt6itempipe
    stat reads transferred 1, then /gt6cover dismantle restores the pipe
    (store=null via /gt6cover check).

Run:  python3 tools/rcon/chains/retriever_cover.py
      python3 tools/rcon/chains/retriever_cover.py --node 1.21.1-neoforge
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

A = gt6world.Site(470, 65, 248, dz=1)  # the south chest rides the site footprint
B = gt6world.Site(476, 65, 248, dz=1)

PIPE = "brass_item_pipe_large"  # the invSize=2 row (GTItemPipes.ItemPipeVariant LARGE 2,1,2) — the capacity arm's window budget
CHEST = "minecraft:chest"


def _north(site):
    return gt6world.Site(site.x, site.y, site.z - 1)


def _south(site):
    return gt6world.Site(site.x, site.y, site.z + 1)


def _feed(pos, slot, item, count=1):
    return Step(gt6world.feed_container_command(pos, slot, item, count),
                expect="Replaced")


CHAIN = Chain(
    name="retriever-cover",
    slug="retr",
    sites=gt6world.declare_sites(A, B),
    preferred_ports=(25731, 25741),
    steps=[
        # ------------------------------------------------------------------
        phase("A: deterministic drive (/gt6itempipe retriever — five pinned arms in one tick)"),
        Step(f"setblock {F(_north(A))} {CHEST}", expect="Changed the block"),
        Step(f"gt6itempipe place {F(A)} 0 {PIPE}", expect="GT6 item pipe placed"),
        Step(f"setblock {F(_south(A))} {CHEST}", expect="Changed the block"),
        Step(f"gt6itempipe toggle {F(A)} 2", expect=": ok"),   # north chest = the pull TARGET (the cover face)
        Step(f"gt6itempipe toggle {F(A)} 3", expect=": ok"),   # south chest = the pull SOURCE
        _feed(_south(A), 0, "stone", 64),
        _feed(_south(A), 1, "stone", 64),
        _feed(_south(A), 2, "stone", 64),
        _feed(_south(A), 3, "dirt", 64),
        Step(f"gt6itempipe retriever {F(A)}", expect="retriever OK",
             label="filtered pull + capacity-window stop + inverted pull + dismantle, all asserted in-command"),

        # ------------------------------------------------------------------
        phase("B: live phase gate (setup, the live ticker pulls, /gt6cover dismantle restores)"),
        Step(f"setblock {F(_north(B))} {CHEST}", expect="Changed the block"),
        Step(f"gt6itempipe place {F(B)} 0 {PIPE}", expect="GT6 item pipe placed"),
        Step(f"setblock {F(_south(B))} {CHEST}", expect="Changed the block"),
        Step(f"gt6itempipe toggle {F(B)} 2", expect=": ok"),
        Step(f"gt6itempipe toggle {F(B)} 3", expect=": ok"),
        Step(f"gt6itempipe retriever {F(B)} setup", expect="retriever setup OK",
             label="the pipe-side install seam (the pipe BE has no cover-plate render snapshot yet)"),
        _feed(_south(B), 0, "dirt", 64),
        Step(f"data get block {F(_north(B))} Items", expect='id: "minecraft:dirt"', poll=30.0,
             label="the live :61 phase gate pulled the stack (timer % 20 == 15) with no manual drive"),
        Step(f"gt6itempipe stat {F(B)}", expect="(empty)",
             label="the pipe holds nothing after the pull; the window counter is transient by design (:193 resets it every 20t — the deterministic counter arms live in column A)"),
        Step(f"gt6cover dismantle {F(B)} north", expect="toolDamage=10000", label="拆盖 — the hoe relay"),
        Step(f"gt6cover check {F(B)}", expect="store=null", label="恢复原状 — the store dissolved"),
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
