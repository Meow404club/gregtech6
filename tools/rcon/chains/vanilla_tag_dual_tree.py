#!/usr/bin/env python3
"""vanilla-tag-dual-tree — the dual-tree tag unification LIVE chain.

Chain semantics (task vanilla-tag-dual-tree acceptance — the RCON runServer
LIVE proof of the unification loop on the /gt6tags debug face, GTToolCommand
"dump" — the RUNTIME merged member set. Leg split DECLARED (the fix-branch
deviation from the original spec ④): the "vanilla default + this port's datagen
members in one answer" face holds on BOTH legs since the P28 graft — see B,
the c: twin runtime face is LIVE on the 1.21.1 leg too (the neoforgeTagFaces
build-side graft, ADR-P17-1 §6 re-ruling); the legs still differ in the
PLATFORM injection half, which is the correct structure, not a defect):

  A the forge-leg canonical loop: on the forge leg forge:ingots/iron lists BOTH
    minecraft:iron_ingot (the Forge-shipped default member, ForgeItemTagsProvider
    :91) AND gt6:ingot_iron (this port's datagen member) — the runtime merge
    closes the coexist+tag unification loop (the GT6RecipeTagFallbackTest.java:53
    precedent, proven live). forge:gems/quartz likewise — the P27 canonical-name
    face (quartz, NOT nether_quartz) joined by the vanilla member (:77). On the
    1.21.1 node the forge: namespace is structurally absent
    (MATERIALS_NAMESPACE=c — "0 members []").

  B the forward twin faces, merged on both legs: on the FORGE leg c:ingots/iron
    and c:gems/quartz answer this port's members (the 26-face data/c/tags/items
    twin tree — the 1.21-canonical namespace served ahead of time). On the
    1.21.1 node the twin face went LIVE with the P28 graft (task
    neotag-graft, commit 9f119234 — the neoforgeTagFaces Copy task mirrors
    the canonical data/forge/tags/items band + the data/c/tags/items twin band
    onto data/c/tags/item, the SINGULAR directory face 1.21.1 actually reads;
    the forge→c remap GT6DualDirectoryFaces.java:45-53 deliberately left out of
    datagen scope, closed at the packaging layer per the ADR-P17-1 §6
    re-ruling — docs/adr/2026-09-05-datagen-tree.md §6, task
    neo-tag-wiring): c:ingots/iron answers 2 members [gt6:ingot_iron,
    minecraft:iron_ingot] — the port graft + the NeoForge default injection,
    the same "vanilla default + this port's members" merge shape as the forge
    leg's A arm. Fully loader-neutral is STRUCTURALLY unreachable (Forge 1.20.1
    ships no c: default injection, so the forge leg stays 1 member) — that is
    the correct structure, and the chain pins each leg's real merged face.

  C the alias twins: forge:gems/nether_quartz (the GT-internal name kept as the
    compatibility face, zero code refs) answers the same member;
    forge:ingots/aluminium + forge:ingots/aluminum (the name-level alias pair,
    unchanged behavior) both answer gt6:ingot_aluminium.

  D the neo-leg canonical switch (node_expects twins): the forge: namespace is
    structurally absent on 1.21.1 ("0 members []") and c: is the canonical
    face — post-graft it answers the merged member set (see B), member-for-
    member the same "port + vanilla" pair the forge leg's A arm proves.

passes=2 is the idempotency proof; the pass-open bbox cleanup re-airs the rig
(one benign anchor site — no step touches it).

Run:  GT6_SESSION=off python3 tools/rcon/chains/vanilla_tag_dual_tree.py
      GT6_SESSION=off python3 tools/rcon/chains/vanilla_tag_dual_tree.py --node 1.21.1-neoforge
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
# MERGED FACE (the P28 graft closure; repinned from the fix-branch "pin reality"
# 1-member pins — those were the live-measured PRE-graft reality: only the
# NeoForge default injection survived, because the tracked plural data/c/tags/items
# tree was unread by the 1.21.1 singular tags/item face). The graft
# (neotag-graft 9f119234, the neoforgeTagFaces Copy task in
# build.neoforge.gradle.kts) mirrors the forge band + the twin band onto
# data/c/tags/item at packaging time, so the neo leg now answers the MERGED
# face — port members (gt6) + the NeoForge default injection (minecraft),
# live-measured post-graft (both passes, /tmp/wt_rcon_neo_red.log):
# c:ingots/iron → 2 members [gt6:ingot_iron, minecraft:iron_ingot],
# c:gems/quartz → 2 members [gt6:gem_nether_quartz, minecraft:quartz] —
# member-for-member the A-arm shape. The forge-leg asserts stay 1 member (its
# twin tree only; Forge 1.20.1 ships no c: default injection — the loader
# asymmetry is the correct structure, ADR-P17-1 §6.2).
steps += [
    phase("B: the forward twin faces — c:ingots/iron + c:gems/quartz answer the MERGED "
          "member set on both legs: the port members plus the platform default injection "
          "(forge leg: the 26-face data/c/tags/items twin tree, 1 member — no c: default "
          "injection on 1.20.1; neo leg: the P28 neoforgeTagFaces graft onto data/c/tags/item "
          "+ the NeoForge default injection, 2 members — ADR-P17-1 §6 re-ruling)"),
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
    name="vanilla-tag-dual-tree",
    slug="tags",
    # the framework's region() refuses an empty site list — one benign anchor keeps the
    # standard forceload + bbox-cleanup pass structure; NO step touches it
    sites=gt6world.declare_sites(gt6world.Site(0, 64, 0)),
    preferred_ports=(26450, 26460),      # this card's pinned rcon/query pair (the 2645x segment, fresh)
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
