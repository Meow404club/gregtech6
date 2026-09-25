#!/usr/bin/env python3
"""trees-nine — the 9-tree worldgen live acceptance chain (the P24 grass chain shape).

Chain semantics (the task-card RCON arm): the grow semantics option b — each of the 9
GT6TreeFeature instances places a deterministic tree when the configured feature is
placed at a known origin (the /place feature face, the P26 wgen lesson: /place takes a
CONFIGURED feature id; determinism comes from a stone platform with cleared headroom).

  A the 9-tree platform: a stone shelf at y=179, nine 14-wide cells —
    /place feature gt6:tree_<snake> <cell> 180 148 lands "Placed", and the trunk
    origin carries the kind's log (execute if block, the "Test passed" idiom):
    rubber maple willow blue_mahoe hazel cinnamon coconut rainbowood blue_spruce.

  B the tag faces: #minecraft:logs answers with the gt6 log family (the coke oven
    listener's expansion source, GT6CokeOvenTagListener.java:77), #minecraft:saplings
    and #minecraft:leaves answer with their families — the gt6tags dump face
    (substring verdicts: the full member list is vanilla-variable).

Two framework passes are the [0, 0] idempotency proof (every arm re-clears its cell
and re-lays the platform first, so the pinned verdicts hold on both passes).

Run:  GT6_SESSION=off python3 tools/rcon/chains/trees.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# The sites — x 486..626, z 141..155, y 179..199: a fresh sky band, clear of every
# registered machine band (the z=20/z=124 strips live x357..464). The dx spans ALL
# nine 14-wide cells (x493..619) — the pass-open gt6world forceload covers the declared
# bounds, and the first session showed the far cells (x>=570) silently outside a
# short band: /place in an unloaded chunk fails and /fill fills only loaded chunks.
PLATFORM = gt6world.Site(556, 179, 148, dx=70, dy=1, dz=7)  # the stone shelf bbox
TREES = gt6world.Site(556, 190, 148, dx=70, dy=10, dz=7)    # the canopy headroom bbox

KINDS = ["rubber", "maple", "willow", "blue_mahoe", "hazel", "cinnamon",
         "coconut", "rainbowood", "blue_spruce"]

steps = []

# ------------------------------------------------- A: the 9-tree platform probes
# NOTE (the forge-leg live finding): /fill returns an EMPTY RCON body on this server
# build (both success and failure) — so the fills carry expect=None and the platform's
# existence is proven by the per-cell stone probe instead (execute if block).
steps += [
    phase("A: the platform — /fill the y=179 stone shelf, then /place feature per kind"),
    Step("fill 493 179 141 619 179 155 minecraft:stone"),
]
for i, tKind in enumerate(KINDS):
    tX = 500 + i * 14
    steps += [
        Step(f"fill {tX - 6} 180 142 {tX + 6} 199 154 minecraft:air"),
        # the fresh-cell headroom clear (the pass-2 reset); expect=None per the fill note
        Step(f"execute if block {tX} 179 148 minecraft:stone", expect="Test passed"),
        Step(f"place feature gt6:tree_{tKind} {tX} 180 148", expect="Placed"),
        Step(f"execute if block {tX} 180 148 gt6:{tKind}_log", expect="Test passed"),
    ]

# ------------------------------------------------- B: the tag faces (the coke oven dependency)
steps += [
    phase("B: the tag faces — #minecraft:logs carries the gt6 family, saplings + leaves too"),
    Step("gt6tags dump minecraft:logs", expect="gt6:rubber_log"),
    Step("gt6tags dump minecraft:logs", expect="gt6:blue_spruce_log"),
    Step("gt6tags dump minecraft:saplings", expect="gt6:rubber_sapling"),
    Step("gt6tags dump minecraft:saplings", expect="gt6:coconut_sapling"),
    Step("gt6tags dump minecraft:leaves", expect="gt6:maple_leaves"),
    Step("gt6tags dump minecraft:leaves", expect="gt6:rainbowood_leaves"),
]

# --------------------------------------------------------------------- T: teardown
steps += [
    phase("T: teardown — the sky band back to air (the pass-open bbox is the backstop)"),
    Step("fill 493 179 141 619 188 155 minecraft:air"),
    Step("fill 493 189 141 619 199 155 minecraft:air"),
    Step("execute if block 506 179 148 minecraft:air", expect="Test passed"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="trees",
    slug="trees",
    sites=gt6world.declare_sites(PLATFORM, TREES),
    preferred_ports=(26108, 26118),      # this card's pinned rcon/query pair (after grass 26107/26117)
    response_timeout=30.0,               # the wide /fill arms sync-load chunks on first pass — 15s ate the reply
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
