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
     "p25_cfoam_spray", "p24_canner_refill", "p24_act"),
    ("p24_grass_block", "p25_tool_hammer_wrench", "p25_food_can"),
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
