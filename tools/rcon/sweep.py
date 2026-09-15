#!/usr/bin/env python3
"""sweep — full-set RCON sweep runner for the GT6 acceptance chains (layer 4).

One command runs every chain against one node, in one of two execution models,
and records a per-step verdict ledger + wall-clock timings to /tmp JSON:

  python3 tools/rcon/sweep.py --mode session            # one boot per group
  python3 tools/rcon/sweep.py --mode perboot            # the GT6_SESSION=off model
  python3 tools/rcon/sweep.py --mode session --only p14loop,p13bb
  python3 tools/rcon/sweep.py --mode session --group p24_dye   # whole cluster(s)
  python3 tools/rcon/sweep.py --mode perboot --probe    # + keepfilter reboot probe
  python3 tools/rcon/sweep.py --diff a.json b.json      # per-step verdict diff
  python3 tools/rcon/sweep.py --dual ../MGT6GA-trees/<other> --other-node 1.21.1-neoforge

The session model boots once per SESSION_GROUPS cluster (coordinate bands of
the chain sites — a chain's leftovers face the next chain's boundary cleanup
within the band; cross-band distances make drift physically impossible), and
framework.plan_groups still splits further on fresh_boot / mutates. The
perboot model calls framework.run per chain — byte for byte the
GT6_SESSION=off fallback path (decision 2026-09-04-rcon-gate-split ④).

Wall is max(nodes), not sum(nodes): --dual runs the other node's sweep in its
own worktree (gradle runServer holds a per-project lock, so same-worktree
dual boots would serialize — ADR-P15-4) and reports both walls. Every /tmp
artifact name carries the node suffix AND the worktree tag (P18, the P17
session_slug mechanism), so parallel worktrees sweeping the identical roster
never overwrite one another's ledgers or logs.
"""

import argparse
import hashlib
import importlib.util
import json
import subprocess
import sys
import time
from pathlib import Path

_HERE = Path(__file__).resolve().parent            # tools/rcon
for _path in (str(_HERE), str(_HERE / "chains")):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import framework
import gt6server
import gt6world

# The full set in session order: coordinate-band clusters (see --plan). The
# p14 dryer and p14 loop bands share the (100, 64, 100) site — same cluster so
# each one's boundary cleanup covers the other's leftovers. The p16 cluster
# (registered P17, card p17-rcon-framework-fixes) is the eight P16-card
# chains; their per-chain 2577x pins are per-boot semantics — a shared session
# boot follows framework.session_ports policy instead.
SESSION_GROUPS = (
    ("p11_cover_shutter_filter", "p12-engine-crank", "p12-axle-family",
     "p12-gearbox-transformer", "p12-engine-diesel", "p12-engine-steam",
     "p12_engine_fuel_fluids"),
    ("p13_hu_steam_foundation", "p13_burning_box_family", "p12-fluid-item-carrier",
     "p12-tap-funnel-attachment", "p12-barrel-keepfilter-logistics",
     "p13_steam_proof_repay"),
    ("p14_dryer_family", "p14_loop_closure", "p13_boiler_tank"),
    ("p14_boiler_distw_immunity", "p13_large_boiler"),
    ("p16_pattern_checker", "p16_aqua_fluids", "p16_side_io",
     "p16_machine_fluid_gui", "p16_drying_rows", "p16_form_scaffold",
     "p16_chisel", "p16_distillery"),
    # Roster backfill (card p27-rcon-roster-backfill): the P19/P21/P23-card
    # chains left out when the roster was first frozen (it stopped at p16) and
    # the remaining P26/P27-card chains join — admission mirroring the
    # established form (bbox-registered sites, no fresh_boot member; the one
    # mutates member keeps its exclusive-wave downgrade). Full band census in
    # README ⑥ "名册补录 III".
    # z=20 west strip x357..381 (three chains): the row-backfill band —
    # p19_drying and p26_rm_backfill share the x=360 column (overlapping
    # bboxes, so one cluster's boundary cleanup covers both), and
    # p21_drying_food sits x-adjacent with its barrel columns reaching z=59
    # (x367..381, clear of the x384 main strip below).
    ("p19_drying", "p26_rm_backfill", "p21_drying_food"),
    # P26 wave1 roster expansion (card p26-rcon-sweep-roster): the nine
    # P24/P25-card chains join in two coordinate bands, admission mirroring
    # the p16 cluster form (bbox-registered, no fresh_boot / mutates member,
    # per-boot port pins yield to framework.session_ports in shared boots).
    # z=20 strip x384..414 (six chains): adjacent band members' bboxes overlap
    # (dye|pipe|taginput|cfoam|canner|act), so one cluster's boundary cleanup
    # covers the neighbours' leftovers — band order = ascending bbox min-x.
    # z=124 strip x390..426 (three chains): grass | hammer | food_can; the
    # grass band is x-disjoint from the hammer+food pair, which touch only at
    # their MARGIN boundary (x418) — same cluster, plan_waves keeps them out
    # of one concurrent wave while session cleanup stays band-local.
    ("p24_dye_chemical_fluids", "p24_pipe_owner", "p25_tag_input_machine_fallback",
     "p25_cfoam_spray", "p26_cfoam_blocks", "p24_canner_refill", "p24_act",
     "p26_cfoam_refill", "p26_kitchen_pot"),
    # task p26-c-foam-block-family joins the z=20 cfoam cluster at x452..458 (clear of the
    # item-pipe band x434..443 claimed by its card state) — sweep --group cfoam picks the
    # pipe face (p25 regression) + the block face (this card) together.
    # task p27-rcon-roster-backfill: p26_cfoam_refill (x410..416, overlapping the
    # act tail) and p26_kitchen_pot (x418..426, x-disjoint from act) join after
    # act — ascending min-x order kept, the refill overlap rides band-local
    # cleanup (the dye|pipe adjacency form).
    # z=124 west strip x369..397 (five chains, roster backfill): the P19/P21/
    # P23 stone/paint face — chisel | stoneblocks | paintable | barrel_paint |
    # chisel_drops in ascending min-x; the drops and barrel_paint bands graze
    # the grass band's x389..397 (the east strip below) — plan_waves refuses
    # the overlap in one concurrent wave and per-chain site cleanup covers the
    # leftovers.
    ("p19_chisel", "p21_stoneblocks_split", "p21_paintable", "p23_barrel_paint",
     "p21_chisel_drops"),
    ("p24_grass_block", "p25_tool_hammer_wrench", "p25_food_can"),
    # Crucible band z=121..140 (three chains, the crucible-chain cards A/B/C,
    # roster backfill): multiblock (x426..443, the wall box + meltdown box) |
    # mold_faucet (x428..438, z-disjoint at z132..140) | row0 (x437..451,
    # overlapping the multiblock east edge — band-local cleanup). The
    # multiblock west edge grazes food_can's x426..427; plan_waves keeps them
    # out of one wave.
    ("p26_crucible_multiblock", "p26_mold_faucet", "p26_crucible_row0"),
    # Sensor band x432..436 z125..137 (single chain, roster backfill):
    # p26_sensors_core is the roster's only session mutates member (fakesource)
    # — plan_waves gives it an exclusive wave and plan_groups splits it out of
    # a shared group (the p16_side_io precedent).
    ("p26_sensors_core",),
    # P27 fix-chain band z=122..128 x456..464 (single chain, roster backfill):
    # the crucible anchored-form regression face, x-disjoint from every other
    # band.
    ("p27_builder_wand_form_fix",),
    # P26 W1 kinetic trio (card p26-w1-sifter-compressor-wiremill): the three
    # family chains share the fresh z=172 band (x383..411 — sifter 384..391 /
    # compressor 394..401 / wiremill 404..411, per-family x-disjoint teardown
    # fills), admission mirroring the p16 cluster form (bbox-registered, no
    # fresh_boot / mutates member — the inject grid rig needs no fakesource).
    ("p26_w1_sifter", "p26_w1_compressor", "p26_w1_wiremill"),
    # P26 W1 card B (p26-w1-press-extruder-molds): the press + extruder ladders —
    # one coordinate band (z=20 strip x429..437, the fresh 430 band clear of the
    # canner 400 strip), both chains bbox-adjacent so one cluster's boundary
    # cleanup covers the other's leftovers (the p24 z=20 cluster form).
    ("p26_w1_press", "p26_w1_extruder"),
    # P29 W1 card B (p29-w1-kinetic-roll-ladder): the four RU roll-ladder chains —
    # one fresh z=180 band (rollingmill x380..394 carries the diesel/axle real-source
    # rig + the p28 ULV rung same-map arm; rollbender x396..406 / rollformer
    # x408..418 / clustermill x420..430, per-family x-disjoint teardown fills),
    # admission mirroring the p26_w1 cluster form (bbox-registered, no fresh_boot /
    # mutates member — the inject grid rig needs no fakesource).
    ("p29_w1_rollingmill", "p29_w1_rollbender", "p29_w1_rollformer", "p29_w1_clustermill"),
    # Static-storage band z=218..232 (two chains, the storage-domain cards,
    # roster backfill): hopper_family (z=220, x400..432) | static_storage
    # (z=230, x458..488) — z-disjoint strips, one cluster keeps the domain's
    # cleanup band-local.
    ("p26_hopper_family", "p26_static_storage"),
    # MUI dispatch band z=98..103 x478..531 (single chain, roster backfill):
    # the four-family menu-dispatch face, disjoint from every other band.
    ("p26_mui_row_dispatch",),
    # Spawn-area band (one chain, roster backfill): the tag dual-tree
    # /gt6tags face (x-2..2) reuses the spawn neighbourhood the p11/p12 band
    # already occupies (x-2..60) — real cross-cluster overlap: plan_waves
    # refuses it in one concurrent wave and per-chain site cleanup covers the
    # leftovers (both faces ran narrow, non-roster legs here before
    # registering). The former EU bridge rig chain was cut with the EU->FE
    # outbound bridge, task p28-cut-eu-fe-bridge.
    ("p27_vanilla_tag_dual_tree",),
    # P28 energy-ecosystem cluster (three chains: card p28-c-ulv-chain's band
    # + the roster backfill of card p28-fe-inbound-chain-rewrite — the two
    # P28-card chains left out when the p28 cut landed, plus the wand-click
    # face): the gt6machine / gt6oven / gt6engine / gt6energy / gt6fe* /
    # gt6multiblock acceptance faces share ONE session boot; every member
    # bbox-disjoint so plan_waves may interleave them.
    # - p28_ulv_chain: the four-arm ULV closure — arm A diesel/axle/dynamo/
    #   wiremill_ulv direct closure (z=40), arm B the 8 EU white-burn wall vs
    #   the LV oven (z=48), arm C the 1375 K melting gate (z=56), arm D the
    #   FE converter -> gt.reversed step-up transformer -> LV oven trickle
    #   (z=64), all in the fresh x519..545 strip; sites pairwise z-disjoint
    #   (margin 2 clear).
    # - p28_fe_inbound: the FE->EU converter chain REWROTE after the
    #   EU->fe_battery bridge fell (task p28-cut-eu-fe-bridge, the old arms
    #   RED [6,6] = dead booking semantics) — re-endpointed on wiremill_ulv:
    #   A push acceptance + bare-consumer negative (z=30), B pull + the 2 FE
    #   floor tail (z=38), C the 8 EU/t ceiling band (z=46), D the overload
    #   explosion (z=54); fresh x548..555 strip, x-disjoint from the W3 band.
    # - p28_builder_wand_oneclick: the one-click crucible form face, spawn-
    #   adjacent x298..312 z97..103 — the grass|hammer split-band precedent:
    #   one cluster, disjoint bands, band-local cleanup each.
    # No fresh_boot / mutates member — no member touches global state; one
    # session boot serves the cluster (per-chain port pins yield to
    # framework.session_ports).
    ("p28_ulv_chain", "p28_fe_inbound", "p28_builder_wand_oneclick"),
    # P29 W1 card C (p29-w1-kinetic-process-ladder): the six process-family chains —
    # buzzsaw | squeezer | centrifuge | sluice | sander | pressurewasher, one fresh
    # z=252 band, per-family x-disjoint columns (x384..393 / x396..403 / x408..418 /
    # x420..428 / x432..441 / x444..454), admission mirroring the p26 W1 cluster form
    # (bbox-registered, no fresh_boot / mutates member — the inject grid rig needs no
    # fakesource; the diesel rigs are per-site setblocks). --group p29_w1_process
    # matches through the chains' embedded name prefix.
    ("p29_w1_buzzsaw", "p29_w1_squeezer", "p29_w1_centrifuge", "p29_w1_sluice",
     "p29_w1_sander", "p29_w1_pressurewasher"),
    # P29 W1 card D (p29-w1-eu-hu-families): the six eu-hu family chains — one
    # coordinate band, the fresh z=200 strip (x383..431, clear of the p26 W1 z=172
    # band and every roster strip below): fermenter firebox 384 / mixer_pair
    # 392..396 / electricloom 402..404 / electricsifter 410..412 / boxinator 418 /
    # unboxinator 426, per-family x-disjoint teardown fills. The card-B roll band
    # and card-C process band join as their own fresh strips (the tail-append form).
    # No fresh_boot / mutates member — no member touches global state (the EU/HU
    # inject rig needs no fakesource; the fermenter firebox is band-local).
    ("p29_w1_mixer_pair", "p29_w1_electricloom", "p29_w1_electricsifter",
     "p29_w1_boxinator", "p29_w1_unboxinator", "p29_w1_fermenter"),
    # P29 W2 card ① (p29-w2-energy-types-5tier): the MU/LU/CU/TU energy-type dial chain —
    # one fresh z=264 band (x384..411: four per-type rigs 384/390/396/402 + the gate-arm
    # rig-under-shredder at 410), single chain in its own cluster (the W2 consumer cards
    # ②③④⑤ each hang their rig arms off this precedent; no fresh_boot / mutates member —
    # the rigs are band-local setblocks, the gate shredder a band-local machine).
    # --group p29_w2_energy matches through the chain's embedded name prefix.
    ("p29_w2_energy_types",),
    # P29 W2 card ③ (p29-w2-eu-special): the Autocrafter/Lightning/Laminator chains —
    # one fresh z=276 band, per-family x-disjoint columns (autocrafter x384..392 rigs
    # and all / lightning x396..402 / laminator x408..429 with the throwaway + the
    # firebox column), admission mirroring the p29 W1 cluster form (bbox-registered,
    # no fresh_boot / mutates member — the EU/HU inject rigs need no fakesource; the
    # laminator's brick burning box is a band-local setblock, the p29_w1_fermenter
    # firebox form). --group p29_w2_eu_special matches through the chains' embedded
    # name prefix (the p29_w1_process form).
    ("p29_w2_autocrafter", "p29_w2_lightning", "p29_w2_laminator"),
    # P29 W2 card ④ (p29-w2-exotic-energy): the six exotic-energy family chains — one
    # fresh z=268 band, per-family x-disjoint columns (polarizer 384..390 (MU, rig under)
    # / magneticseparator 394..400 (MU, rig above) / laserengraver 404..410 (LU, rig
    # above) / laserwelder 414..420 (LU, rig above) / freezer 424..430 (CU, rig behind,
    # the first back-face rig) / cryomixer 434..439 (CU, rig under + the five-column
    # CRYO_PARALLEL ladder)), all band-local setblocks + gt6energy dial rigs (the card-①
    # source-block precedent; no fresh_boot / mutates member). MU/LU/CU each carry one
    # LIVE type-gate arm (refuse EU, accept own type — the acceptance-① trio).
    # --group p29_w2_exotic matches through the chains' embedded name prefix.
    ("p29_w2_polarizer", "p29_w2_magnetic_separator", "p29_w2_laser_engraver",
     "p29_w2_laser_welder", "p29_w2_freezer", "p29_w2_cryo_mixer"),
    # P29 W2 card ② (p29-w2-eu-core-5tier): the five eu-core family chains —
    # electrolyzer | injector | printer | scanner_visuals | slicer, one fresh z=284
    # band (the review-seat rebase re-band: the chain set was authored on z=276 before
    # the card-③ eu-special chains landed on that band — moved to z=284 to keep the
    # one-fresh-band-per-cluster discipline), per-family
    # x-disjoint columns (electrolyzer x383..393 carries the /gt6energy type-gate rig
    # under its T2; injector x395..405 / printer x407..417 / scanner x419..429 /
    # slicer x431..441), admission mirroring the p29 W1 cluster form (bbox-registered,
    # no fresh_boot / mutates member — the EU inject rig needs no fakesource; the
    # electrolyzer rig is a band-local setblock). --group p29_w2_eu_core matches
    # through the chains' embedded name prefix.
    ("p29_w2_electrolyzer", "p29_w2_injector", "p29_w2_printer",
     "p29_w2_scanner_visuals", "p29_w2_slicer"),
    # P29 W2 card ⑤ (p29-w2-hu-tu-piggyback): the seven hu-tu family chains — one fresh
    # z=280 band, per-family x-disjoint columns (steam_cracker 384..390 / catalytic_cracker
    # 394..400 / coagulator 404..408 + its north-face rig at z279 / generifier 414 /
    # bath 420 / autoclave 426 / loom 434..440 with the diesel->axle real-source rig),
    # admission mirroring the p29 W1 cluster form (bbox-registered, no fresh_boot /
    # mutates member — the inject grid rig needs no fakesource; the coagulator north rig
    # is band-local). --group p29_w2_hu_tu matches through the chains' embedded name
    # prefix. The coagulator chain carries the card-① legacy obligation's LIVE half: the
    # NO_CONSTANT_POWER power-gap retention arm on a real TU machine.
    ("p29_w2_steam_cracker", "p29_w2_catalytic_cracker", "p29_w2_coagulator",
     "p29_w2_generifier", "p29_w2_bath", "p29_w2_autoclave", "p29_w2_loom"),
    # P29 W3 card ① (p29-w3-nbtdesign-parts): the part-family expansion chain — one
    # fresh z=292 band, 41 part blocks on x-disjoint even columns x384..464 (the metal
    # walls 11 / dense walls 11 / coils 6 / parts 6 / ventilation+PU+wood 7), the
    # DESIGN render-slot probes riding the dense-wall + distill + coil + centrifuge
    # sites (band-local setblocks only — no fresh_boot / mutates member).
    # --group p29_w3_parts matches through the chain's embedded name prefix.
    ("p29_w3_parts",),
    # P29 W3 card ② (p29-w3-tank-valves): the Tank Main Valve chain — a fresh z=294
    # band, six x-disjoint tank rigs on x381..427 (the wood/SS 3x3x3s, the SS 5x5x5,
    # the wrong-wall and meltdown arms; the auto-emit barrel rides the wood rig), the
    # live form/fill/emit/destruction proofs through /gt6tankvalve + /gt6multiblock form.
    # Band-local setblocks only — no fresh_boot / mutates member.
    # --group p29_w3_tank matches through the chain's embedded name prefix.
    ("p29_w3_tank",),
    # P29 W3 card ③ (p29-w3-turbine-dynamo): the turbine + dynamo converter band —
    # one fresh z=300 strip, three chains on x-disjoint columns (the steam tiers
    # x384..448, the gas pair x448..480, the dynamo tiers x464..560 — the gas band
    # rides the steam/dynamo gap), the STEAM 夹具 = the card's dial short-code
    # precedent, the sinks = the flux dynamo t5 / electric transformer delivery
    # legs (band-local setblocks only — no fresh_boot / mutates member).
    # --group p29_w3_turbine_dynamo matches through the chains' embedded name prefix.
    ("p29_w3_steam_turbine", "p29_w3_gas_turbine", "p29_w3_large_dynamo"),
    # P29 W3 card 4 (p29-w3-distill-crucible): the twin distillation towers + the crucible
    # 8-material ladder — one fresh z=300 band, per-family x-disjoint columns (the HU tower
    # 384..390 / the cryo tower 398..406 / the crucible ladder 414..470: the SS 3x3x3 rig at
    # 429..431 + the census columns 418..446 at z309), admission mirroring the p29 W1 cluster
    # form (bbox-registered, no fresh_boot / mutates member — the gt6energy source-block rigs
    # and the barrel/chest sinks are band-local setblocks). --group p29_w3_distill_crucible
    # matches through the chains' embedded name prefix.
    ("p29_w3_distillation_tower", "p29_w3_cryo_tower", "p29_w3_crucible_ladder"),
    # P29 W3 card 5 (p29-w3-large-12): the twelve large-machine chains — one fresh
    # z=340 band, twelve x-disjoint columns x384..516 (12-wide pitch, the 7-long sluice
    # trough the widest tenant; per-machine teardown fills), admission mirroring the
    # p29 W1/W2 cluster form (bbox-registered, no fresh_boot / mutates member — the
    # rigs are per-site band-local setblocks: the under-controller gt6energy rig, the
    # fermenter's under-transmitter relay rig, the TU three run rig-less).
    # --group p29_w3_large12 matches through the chains' embedded name prefix.
    ("p29_w3_large_centrifuge", "p29_w3_large_electrolyzer", "p29_w3_large_coagulator",
     "p29_w3_large_autoclave", "p29_w3_large_bath", "p29_w3_large_mixer",
     "p29_w3_large_fermenter", "p29_w3_large_oven", "p29_w3_large_sluice",
     "p29_w3_large_crusher", "p29_w3_large_shredder", "p29_w3_large_squeezer"),
    # P29 W3 card ⑥ (p29-w3-heat-smelter): the three heat-family chains — one fresh
    # z=304 band, x-disjoint columns (smelter 396..408 / melter 416 / the HEX+boiler
    # stack 426..434 with the boiler frame+wand arms), admission mirroring the p29 W2
    # cluster form (bbox-registered, no fresh_boot / mutates member; the HU fixture is
    # the inject rig for the machines and the real boiler stack for the exchanger).
    # --group p29_w3_heat_smelter matches through the chains' embedded name prefix.
    ("p29_w3_heat_exchanger", "p29_w3_smelter", "p29_w3_melter"),
    ("p15_runtime_smoke",),   # fresh_boot singleton (decision ③)
)

PROBE_MODULE = "p15_keepfilter_reboot_probe"


def load_chain(stem):
    """Import chains/<stem>.py and return its module-level CHAIN."""
    path = _HERE / "chains" / f"{stem}.py"
    spec = importlib.util.spec_from_file_location(f"sweep_chain_{stem}", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.CHAIN


def ordered_stems(groups=None):
    if groups is None:
        groups = SESSION_GROUPS
    seen, stems = set(), []
    for group in groups:
        for stem in group:
            if stem not in seen:
                seen.add(stem)
                stems.append(stem)
    return stems


def select_groups(groups, only_groups):
    """Filter SESSION_GROUPS clusters by comma keys — the single-group smoke
    / incremental re-verification entry (P26). A key matches a cluster when it
    is a substring of any member's module stem, slug or chain name; a matched
    cluster joins WHOLE, because the unit of admission is the coordinate band
    (boundary cleanup + wave packing), not the single chain — narrow within a
    band with --only. Absent flag = the full roster, byte for byte."""
    if not only_groups:
        return list(groups)
    keys = [part.strip() for part in only_groups.split(",") if part.strip()]
    picked = []
    for group in groups:
        hay = list(group)
        for stem in group:
            chain = load_chain(stem)
            hay += [chain.slug, chain.name]
        if any(key in part for key in keys for part in hay):
            picked.append(group)
    if not picked:
        raise SystemExit(f"sweep: no group matches: {keys}")
    return picked


def select(stems, only):
    if not only:
        return list(stems)
    keys = {part.strip() for part in only.split(",") if part.strip()}
    picked = []
    for stem in stems:
        chain = load_chain(stem)
        if stem in keys or chain.slug in keys or chain.name in keys:
            picked.append(stem)
    missing = keys - {stem for stem in picked}
    for key in list(missing):
        for stem in stems:
            chain = load_chain(stem)
            if key in (chain.slug, chain.name):
                picked.append(stem)
                missing.discard(key)
                break
    if missing:
        raise SystemExit(f"sweep: unknown chain key(s): {sorted(missing)}")
    return [stem for stem in stems if stem in set(picked)]


def _worktree_tag(root):
    """framework.worktree_tag()'s formula, for an arbitrary root.

    The --dual reader must address the OTHER worktree's artifacts, but
    framework pins WORKTREE_ROOT at import time (this worktree) — so the hash
    formula (md5 of the root path, first 8 hex; card 6a0958a2) is reproduced
    here for the peer root. /tmp is global: the directory is the same, only
    the name separates the legs.
    """
    return hashlib.md5(str(root).encode("utf-8")).hexdigest()[:8]


def result_path(mode, node, concurrency=1, tag=None):
    """Per-run result JSON, namespaced by worktree tag; session runs are
    further namespaced by concurrency degree, so the concurrency-time curve's
    points never overwrite one another.

    The worktree tag (P18, `framework.worktree_tag()`) keeps parallel
    worktrees' ledgers disjoint in the shared /tmp — two same-roster sweeps
    used to overwrite each other's JSON mid-run. `tag` overrides for --dual:
    the spawned leg writes under ITS OWN worktree's tag, so the reader must
    pass the OTHER root's hash (run_dual).
    """
    suffix = framework.node_suffix(node)
    label = f"{mode}_c{concurrency}" if mode == "session" else mode
    worktree = framework.worktree_tag() if tag is None else tag
    return gt6server.ARTIFACT_DIR / f"gt6_rs_sweep_{label}_{suffix}_{worktree}.json"


def _failed(res):
    """One chain record's failure: perboot carries 'exit', session carries
    pass_failures / exception."""
    if res.get("exit"):
        return True
    if any(res.get("pass_failures") or []):
        return True
    return bool(res.get("exception"))


def run_perboot(stems, node):
    """The per-chain boot model, in process: framework.run per chain."""
    chains = {}
    started = time.monotonic()
    for stem in stems:
        chain = load_chain(stem)
        chain.node = node                      # sweep-level --node override
        verdicts = []
        t0 = time.monotonic()
        try:
            code = framework.run(chain, verdicts=verdicts)
        except Exception as exc:               # one broken boot must not kill the sweep
            print(f"[sweep] {chain.name}: EXCEPTION {exc!r}")
            code = 1
        log_path, _ = gt6server.artifact_paths(chain.slug)
        chains[chain.name] = {
            "exit": code,
            "seconds": round(time.monotonic() - t0, 1),
            "error_lines": gt6server.server_error_lines(log_path),
            "verdicts": verdicts,
        }
    wall = time.monotonic() - started
    return {"mode": "perboot", "node": node, "wall_s": round(wall, 1),
            "boots": len(stems), "chains": chains}


def run_session_recorded(stems, node, concurrency):
    """The session model via framework.run_session_recorded.

    The stems go in FLAT (registry order): concurrency wants the whole set as
    one admission pool — disjoint-site chains pack across the coordinate bands
    (framework.plan_groups keeps fresh_boot/mutates exclusivity, plan_waves
    enforces the bbox-disjoint admission). The SESSION_GROUPS clusters remain
    only as the perboot order and the --plan documentation of the serial-band
    layout; nothing is re-implemented here."""
    chains = []
    for stem in stems:
        chain = load_chain(stem)
        chain.node = node
        chains.append(chain)
    result = framework.run_session_recorded(chains, node=node,
                                            concurrency=concurrency)
    result["mode"] = "session"
    return result


def show_plan(groups=None):
    if groups is None:
        groups = SESSION_GROUPS
    print(f"worktree {framework.WORKTREE_ROOT}")
    print(f"{len(ordered_stems(groups))} chains in {len(groups)} cluster(s)\n")
    for group in groups:
        print(f"  cluster ({len(group)}): {', '.join(group)}")
        boxes = {}
        for stem in group:
            chain = load_chain(stem)
            boxes[chain.name] = gt6world.region(chain.sites)
        names = list(boxes)
        for i, name in enumerate(names):
            for other in names[i + 1:]:
                if _intersects(boxes[name], boxes[other]):
                    print(f"    note: {name} bbox intersects {other} bbox "
                          f"(boundary cleanup covers the shared band)")
        for name in names:
            x1, y1, z1, x2, y2, z2 = boxes[name]
            print(f"    {name}: bbox x{x1}..{x2} y{y1}..{y2} z{z1}..{z2}")
        print()


def _intersects(a, b):
    return all(a[i] <= b[i + 3] and b[i] <= a[i + 3] for i in range(3))


def _chain_exit(res):
    """The chain-level exit from either record shape (perboot carries 'exit',
    session records carry pass_failures/exception)."""
    if "exit" in res:
        return res["exit"]
    return 1 if (_failed(res)) else 0


def diff_results(old_path, new_path):
    """Per-chain, per-step verdict diff between two sweep JSONs."""
    old = json.loads(Path(old_path).read_text(encoding="utf-8"))
    new = json.loads(Path(new_path).read_text(encoding="utf-8"))
    print(f"diff: {old_path} ({old['mode']}, {old['node']}, wall {old['wall_s']}s, "
          f"{old.get('boots')} boots)  vs  {new_path} ({new['mode']}, {new['node']}, "
          f"wall {new['wall_s']}s, {new.get('boots')} boots)")
    names_old, names_new = set(old["chains"]), set(new["chains"])
    if names_old - names_new:
        print(f"  chains only in OLD: {sorted(names_old - names_new)}")
    if names_new - names_old:
        print(f"  chains only in NEW: {sorted(names_new - names_old)}")
    differences = 0
    hard = 0
    for name in sorted(names_old & names_new):
        o, n = old["chains"][name], new["chains"][name]
        ov, nv = o.get("verdicts") or [], n.get("verdicts") or []
        if _chain_exit(o) != _chain_exit(n):
            print(f"  {name}: EXIT differs {o['exit']} -> {n['exit']}")
            hard += 1
            differences += 1
        for a, b in zip(ov, nv):
            if a["verdict"] != b["verdict"] or a["cmd"] != b["cmd"]:
                differences += 1
                if {a["verdict"], b["verdict"]} & {"FAIL"}:
                    hard += 1
                print(f"  {name} step {a['index']}: {a['verdict']} -> {b['verdict']}"
                      f"\n    old: {a['cmd']}\n    new: {b['cmd']}")
        if len(ov) != len(nv):
            print(f"  {name}: step count differs {len(ov)} vs {len(nv)}")
            differences += 1
    verdict = "IDENTICAL" if differences == 0 else \
              f"{differences} difference(s), {hard} failure-relevant"
    print(f"\nverdict diff: {verdict}")
    return 0 if differences == 0 else 1


def run_dual(args):
    """Run this node's sweep here + the other node's sweep in its worktree."""
    other = Path(args.dual).resolve()
    head = subprocess.run(["git", "-C", str(other), "rev-parse", "HEAD"],
                          capture_output=True, text=True, check=True).stdout.strip()
    mine = subprocess.run(["git", "-C", str(framework.WORKTREE_ROOT), "rev-parse", "HEAD"],
                          capture_output=True, text=True, check=True).stdout.strip()
    if head != mine:
        raise SystemExit(f"--dual: commit mismatch: {other} at {head[:12]}, "
                         f"here {mine[:12]} — same-commit discipline (ADR-P15-4)")
    other_node = args.other_node
    other_tag = _worktree_tag(other)   # the leg writes under its OWN tag
    cmd = [sys.executable, "tools/rcon/sweep.py", "--mode", args.mode,
           "--node", other_node, "--concurrency", str(args.concurrency)]
    if args.only:
        cmd += ["--only", args.only]
    if args.group:
        cmd += ["--group", args.group]
    other_log = gt6server.ARTIFACT_DIR / \
        f"gt6_rs_sweep_dual_{framework.node_suffix(other_node)}_{other_tag}.log"
    print(f"[sweep] dual: spawning {other_node} in {other} "
          f"(log {other_log}, same commit {mine[:12]})")
    with other_log.open("w") as log:
        proc = subprocess.Popen(cmd, cwd=str(other), stdout=log,
                                stderr=subprocess.STDOUT, start_new_session=True)
        try:
            mine_json = run_and_record(args)
        finally:
            print(f"[sweep] dual: waiting for {other_node} (pid {proc.pid}) ...")
            proc.wait()
    other_json = result_path(args.mode, other_node, args.concurrency,
                             tag=other_tag)
    if not other_json.exists():
        print(f"[sweep] dual: {other_node} produced no result json — see {other_log}")
        return 1
    other_res = json.loads(other_json.read_text(encoding="utf-8"))
    print(f"\n[sweep] dual wall: {framework.node_suffix(args.node or framework.DEFAULT_NODE)} "
          f"= {mine_json['wall_s']}s, {other_node} = {other_res['wall_s']}s "
          f"-> max = {max(mine_json['wall_s'], other_res['wall_s'])}s")
    return mine_json["exit"] | other_res.get("exit", 1)


def run_and_record(args):
    """Run the sweep for this node and write the result JSON; return it."""
    node = args.node or framework.DEFAULT_NODE
    # getattr, not args.group: selftest check 8 hands run_and_record a
    # hand-built Namespace predating --group — absent key = full roster.
    stems = select(ordered_stems(select_groups(SESSION_GROUPS,
                                               getattr(args, "group", None))),
                   args.only)
    started = time.monotonic()
    if args.mode == "perboot":
        result = run_perboot(stems, node)
    else:
        result = run_session_recorded(stems, node, args.concurrency)
    result["wall_s"] = round(time.monotonic() - started, 1)
    # The top-level exit key the --dual reader consumes (run_dual's
    # mine_json["exit"] | other_res.get("exit", 1)): run_perboot's shape
    # ({mode, node, wall_s, boots, chains} — sweep.py run_perboot) carries no
    # such key, only the per-chain records do, so a perboot --dual leg died
    # with a KeyError at the very end of a full sweep. run_session_recorded
    # (framework.py:689) computes its own {"exit": overall}; aggregate the
    # same _failed semantics main() uses for the non-dual exit code and let
    # setdefault keep the session model's framework-computed value untouched
    # (tmp.p18.pool: sweep-run-dual-perboot-exit-keyerror).
    result.setdefault("exit",
                      1 if any(_failed(res) for res in result["chains"].values())
                      else 0)
    path = result_path(args.mode, node, args.concurrency)
    path.write_text(json.dumps(result, indent=1), encoding="utf-8")
    failures = [name for name, res in result["chains"].items() if _failed(res)]
    print(f"\n[sweep] {args.mode} wall {result['wall_s']}s, boots {result['boots']}, "
          f"{len(stems)} chains, failures: {failures or 'none'}")
    print(f"[sweep] results -> {path}")
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(prog="sweep")
    parser.add_argument("--mode", choices=["session", "perboot"], default="session")
    parser.add_argument("--node", default=None,
                        help="stonecutter node (default 1.20.1-forge)")
    parser.add_argument("--only", default=None,
                        help="comma list of module stem / slug / chain name")
    parser.add_argument("--group", default=None,
                        help="comma list of cluster keys (substring on any member "
                             "stem / slug / chain name); matched SESSION_GROUPS "
                             "clusters join whole — single-band smoke / incremental "
                             "re-verification; --only narrows within")
    parser.add_argument("--concurrency", type=int, default=None,
                        help="session wave width (default GT6_CONCURRENCY or 1 = serial; "
                             "N>=2 interleaves site-disjoint chains on one boot)")
    parser.add_argument("--plan", action="store_true",
                        help="print the cluster plan + bbox overlaps and exit")
    parser.add_argument("--probe", action="store_true",
                        help="run the keepfilter reboot probe after the sweep")
    parser.add_argument("--diff", nargs=2, metavar=("OLD_JSON", "NEW_JSON"),
                        help="per-step verdict diff between two sweep results")
    parser.add_argument("--dual", default=None, metavar="OTHER_WORKTREE",
                        help="also run OTHER_NODE there in parallel (same commit enforced)")
    parser.add_argument("--other-node", default="1.21.1-neoforge")
    args = parser.parse_args(argv)
    if args.concurrency is None:
        args.concurrency = framework.concurrency_degree()

    if args.diff:
        return diff_results(*args.diff)
    if args.plan:
        show_plan(select_groups(SESSION_GROUPS, args.group))
        return 0

    if args.dual:
        code = run_dual(args)
    else:
        result = run_and_record(args)
        code = 1 if any(_failed(res) for res in result["chains"].values()) else 0

    if args.probe:
        node = args.node or framework.DEFAULT_NODE
        print(f"\n[sweep] keepfilter reboot probe on {node}")
        code |= subprocess.call([sys.executable,
                                 str(_HERE / "chains" / f"{PROBE_MODULE}.py"),
                                 "--node", node])
    return code


if __name__ == "__main__":
    sys.exit(main())
