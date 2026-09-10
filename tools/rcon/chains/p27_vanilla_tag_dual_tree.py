#!/usr/bin/env python3
"""p27-vanilla-tag-dual-tree — the dual-tree tag unification LIVE chain.

Chain semantics (task p27-vanilla-tag-dual-tree acceptance — the RCON runServer
LIVE proof of the unification loop on the /gt6tags debug face, GTToolCommand
"dump" — the RUNTIME merged member set: platform-shipped defaults + this port's
datagen members in one answer):

  A the forge-leg canonical loop: forge:ingots/iron must list BOTH
    minecraft:iron_ingot (the Forge-shipped default member, ForgeItemTagsProvider
    :91) AND gt6:ingot_iron (this port's datagen member) — the runtime merge
    closes the coexist+tag unification loop (the GT6RecipeTagFallbackTest.java:53
    precedent, proven live). forge:gems/quartz likewise — the p27 canonical-name
    face (quartz, NOT nether_quartz) joined by the vanilla member (:77).

  B the forward twin faces: c:ingots/iron and c:gems/quartz exist on the forge
    leg with this port's members (the 26-face data/c/tags/items twin tree) — the
    1.21-canonical namespace served ahead of time.

  C the alias twins: forge:gems/nether_quartz (the GT-internal name kept as the
    compatibility face, zero code refs) answers the same member;
    forge:ingots/aluminium + forge:ingots/aluminum (the name-level alias pair,
    unchanged behavior) both answer gt6:ingot_aluminium.

  D the neo-leg canonical switch (node_expects twins): the forge: namespace is
    structurally absent on 1.21.1 (MATERIALS_NAMESPACE=c — "0 members []") and
    c:ingots/iron is the canonical face; whether NeoForge ships the vanilla
    default member into c: is pinned by the node_expects after the live leg.

passes=2 is the idempotency proof; the pass-open bbox cleanup re-airs the rig
(one benign anchor site — no step touches it).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p27_vanilla_tag_dual_tree.py
      GT6_SESSION=off python3 tools/rcon/chains/p27_vanilla_tag_dual_tree.py --node 1.21.1-neoforge
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

steps = []

# ------------------------------------------------- A: the canonical loop
steps += [
    phase("A: the canonical loop — forge:ingots/iron and forge:gems/quartz carry the vanilla "
          "default AND the gt6 member (the runtime merge, live); the 1.21.1 node twins pin "
          "the canonical switch (forge: namespace structurally absent there)"),
    Step("gt6tags dump forge:ingots/iron",
         expect="gt6tags dump forge:ingots/iron: 2 members [gt6:ingot_iron, minecraft:iron_ingot]",
         node_expects={"1.21.1": "gt6tags dump forge:ingots/iron: 0 members []"}),
    Step("gt6tags dump forge:gems/quartz",
         expect="gt6tags dump forge:gems/quartz: 2 members [gt6:gem_nether_quartz, minecraft:quartz]",
         node_expects={"1.21.1": "gt6tags dump forge:gems/quartz: 0 members []"}),
]

# ------------------------------------------------- B: the forward twin faces
steps += [
    phase("B: the forward twin faces — c:ingots/iron + c:gems/quartz answer the port members "
          "(the 26-face data/c/tags/items product); the 1.21.1 node answers the c: canonical "
          "face (NeoForge default-member injection pinned after the live leg)"),
    Step("gt6tags dump c:ingots/iron",
         expect="gt6tags dump c:ingots/iron: 1 members [gt6:ingot_iron]",
         node_expects={"1.21.1": "gt6tags dump c:ingots/iron: 2 members [gt6:ingot_iron, minecraft:iron_ingot]"}),
    Step("gt6tags dump c:gems/quartz",
         expect="gt6tags dump c:gems/quartz: 1 members [gt6:gem_nether_quartz]",
         node_expects={"1.21.1": "gt6tags dump c:gems/quartz: 2 members [gt6:gem_nether_quartz, minecraft:quartz]"}),
]

# ------------------------------------------------- C: the alias twins
steps += [
    phase("C: the alias faces — the GT-internal nether_quartz twin and the aluminium pair"),
    Step("gt6tags dump forge:gems/nether_quartz",
         expect="gt6tags dump forge:gems/nether_quartz: 1 members [gt6:gem_nether_quartz]",
         node_expects={"1.21.1": "gt6tags dump forge:gems/nether_quartz: 0 members []"}),
    Step("gt6tags dump forge:ingots/aluminium",
         expect="gt6tags dump forge:ingots/aluminium: 1 members [gt6:ingot_aluminium]",
         node_expects={"1.21.1": "gt6tags dump forge:ingots/aluminium: 0 members []"}),
    Step("gt6tags dump forge:ingots/aluminum",
         expect="gt6tags dump forge:ingots/aluminum: 1 members [gt6:ingot_aluminium]",
         node_expects={"1.21.1": "gt6tags dump forge:ingots/aluminum: 0 members []"}),
]

# ------------------------------------------------- teardown
steps += [
    phase("F: teardown — the vanilla round-trip (the pass-open bbox is the backstop)"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p27-vanilla-tag-dual-tree",
    slug="p27tags",
    # the framework's region() refuses an empty site list — one benign anchor keeps the
    # standard forceload + bbox-cleanup pass structure; NO step touches it
    sites=gt6world.declare_sites(gt6world.Site(0, 64, 0)),
    preferred_ports=(26450, 26460),      # this card's pinned rcon/query pair (the 2645x segment, fresh)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
