#!/usr/bin/env python3
"""parse-errors-convergence — the atlas-ABSENT live reconciliation arm (task
parse-errors-registration-convergence; ruling ③ root-cause live evidence).

The bare RCON node (gt6 + system mods only) is exactly the seeded-narrow install:
GT6ModDrivers.seedFromEnvironment pins every atlas PRIMARY domain the mod list
lacks to ABSENT and the material walks drop those pairs — the SAME population the
row-level forge:mod_loaded conditions key on (the conditions and the seed share
ModList.isLoaded, the correspondence is by construction). This chain samples the
matrix live so the offline pin (GT6ForeignRowConvergenceTest) rests on a live
face:

  A the gated matrix: every B2/B5 log-header id + one anchor per heavy domain
    MUST be absent from the live registry — `item replace` errors with the
    vanilla unknown-item message (ItemArgument ERROR_UNKNOWN_ITEM) and the
    judged expect is that exact message (strict, no allow_failed: a
    successful give here would falsify the root cause, not the fix);
  B the ungated controls: the shared-material twins MUST give (expect
    "Replaced") — COMMON_SECONDARY/GT6_SELF rows never hide (ADR-MDH2), so the
    control arms prove the absence is atlas-shaped, not a broken give path.

The recipe-side zero-error half is the boot log itself (grep "Parsing error
loading recipe" == 0 after this chain's node boot — the 1.20.1 RecipeManager
loads the conditioned tree; the pre-fix log carried 12018).

passes=1 (stateless chest fill). teardown: the explicit band restore.

Run:  GT6_SESSION=off python3 tools/rcon/chains/parse_errors_convergence.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh z=332 band, x 460..474 — x-disjoint from the armor-give (384..398),
# wear (410..420) and recipe (430..440) bands of the same cluster (the
# one-fresh-band-per-cluster discipline)
CHEST = "460 65 332"

# the B2/B5 log-header ids verbatim (research.p2-parse-errors-sweep evidence)
# + one anchor per heavy atlas domain. Every id is a gated pair's item.
GATED_IDS = [
    ("gt6:stick_astral_silver", "MET", "log:229019 chisel/astral_silver"),
    ("gt6:tool_head_hammer_spectre_iron", "RT", "log:126662 hard_hammer_from_head"),
    ("gt6:foil_shadow_iron", "MET", "log:264319 foil2wire_fine"),
    ("gt6:tool_head_construction_pickaxe_meteoflame_red_steel", "TF", "log:550004 pickaxe_construction"),
    ("gt6:chemtube_teflon", "hbm", "B1 chemtube/from_dust_tiny"),
    ("gt6:rock_gt_dolamide", "mo", "log:70003 ore_small loot arm"),
    ("gt6:block_ingot_obsidian_refined", "Mek", "log:20000 block loot arm"),
    ("gt6:ingot_energeticalloy", "EnderIO", "the EIO PRIMARY anchor"),
    ("gt6:ingot_manasteel", "Botania", "the BOTA PRIMARY anchor"),
    ("gt6:ingot_draconium_awakened", "DraconicEvolution", "the DE PRIMARY anchor"),
]

# the never-gates: shared materials (COMMON_SECONDARY) register on the bare node
CONTROL_IDS = [
    "gt6:stick_iron",          # unattributed
    "gt6:ingot_wrought_iron",  # TFC COMMON_SECONDARY (:2269)
    "gt6:ingot_sterling_silver",  # TFC COMMON_SECONDARY (:2271)
]

steps = [phase("A: the gated matrix — every seed-hidden id is absent live (Unknown item, strict)")]
for slot, (item_id, domain, source) in enumerate(GATED_IDS):
    steps.append(Step(f"item replace block {CHEST} container.{slot} with {item_id} 1",
                      expect=f"Unknown item '{item_id}'"))

steps += [phase("B: the ungated controls — the shared twins give (the absence is atlas-shaped)")]
base = len(GATED_IDS)
for i, item_id in enumerate(CONTROL_IDS):
    steps.append(Step(f"item replace block {CHEST} container.{base + i} with {item_id} 1",
                      expect="Replaced"))

steps += [
    phase("C: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 458 62 329 476 68 336 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="parse-errors-convergence",
    slug="parseconv",
    sites=gt6world.declare_sites(gt6world.Site(460, 65, 332)),
    preferred_ports=(26426, 26436),
    game_port=26416,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
