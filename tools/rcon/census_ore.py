#!/usr/bin/env python3
"""census_ore — the p30 ore close-out card's census boot driver (task p30-ore-5-census).

The roster boots flat (gt6server.PROVISIONED_KEYS); this card's acceptance mandates the
p30 worldgen card form instead: a NORMAL world at the fixed seed 6131000569321125127,
world dirs deleted before the boot. The framework's per-chain runner has no knob for
that, so this driver owns the boot — everything else is the stock framework semantics:

  gt6server.provision_run_dir -> patch level-type/level-seed -> delete world dirs
  -> gt6server.start_server (nohup semantics + the RCON slot semaphore) -> wait_done
  (the Done poll) -> N passes of framework._run_pass (byte-identical pass structure:
  forceload + bbox cleanup + the census chain's judged steps) -> stop_server
  -> the census summary (the fresh boot slice's ore registration trio grepped from
  the log = ledger 1 live evidence, plus the per-pass verdicts and the ERROR scan).

Usage:
  python3 tools/rcon/census_ore.py [--node 1.20.1-forge|1.21.1-neoforge]
                                   [--passes 2] [--seed 6131000569321125127]

Exit code 0 when every pass judged clean and zero server ERROR lines landed in the
census slice (the framework's verdict semantics, byte for byte).
"""

import argparse
import importlib
import re
import sys
import time
from pathlib import Path

_HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(_HERE))
sys.path.insert(0, str(_HERE / "chains"))

import gt6server
import gt6world
import framework

CHAIN_STEM = "p29_w6_ore_census"
DEFAULT_SEED = "6131000569321125127"
# the fixed worldgen form of the p30 worldgen cards (normal + the pinned seed)
LEVEL_TYPE_NORMAL = "minecraft\\:normal"


def patch_world_form(props_path, seed):
    """level-type normal + the fixed seed, post-provision (provision owns flat)."""
    lines = props_path.read_text(encoding="utf-8").splitlines(keepends=True)
    seen = set()
    wanted = {"level-type": LEVEL_TYPE_NORMAL, "level-seed": seed}
    for index, line in enumerate(lines):
        key = line.split("=", 1)[0].strip()
        if key in wanted:
            lines[index] = f"{key}={wanted[key]}\n"
            seen.add(key)
    lines += [f"{key}={value}\n" for key, value in wanted.items() if key not in seen]
    props_path.write_text("".join(lines), encoding="utf-8")
    print(f"[census_ore] world form: level-type=normal seed={seed} ({props_path})")


def delete_worlds(run_dir_):
    """The 删世界 face: a fresh world so the boot census is generation-clean."""
    for world in sorted(run_dir_.glob("world*")):
        if world.is_dir():
            import shutil
            shutil.rmtree(world)
            print(f"[census_ore] deleted world dir {world.name}")


def census_summary(log_path, offset_bytes):
    """Grep the fresh boot slice for the ore registration trio (ledger 1 live)."""
    log = log_path.read_bytes()[offset_bytes:].decode("utf-8", errors="replace")
    trio = []
    for pattern in (r"GT6 registered (\d+) ore blocks \([^)]*\)",
                    r"GT6 registered (\d+) ore block items",
                    r"GT6 registered (\d+) ore creative tab \([^)]*\)"):
        match = re.search(pattern, log)
        trio.append(f"  {match.group(0)}" if match else f"  MISSING: /{pattern}/")
    print("\n===== census summary: the fresh-boot registration trio =====")
    print("\n".join(trio))
    return all("MISSING" not in line for line in trio)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--node", default=framework.DEFAULT_NODE)
    parser.add_argument("--passes", type=int, default=2)
    parser.add_argument("--seed", default=DEFAULT_SEED)
    args = parser.parse_args()

    chain = importlib.import_module(CHAIN_STEM).CHAIN
    chain.node = args.node  # the framework write-back rule (run()/run_session_recorded)
    task = gt6server.gradle_task(args.node)
    rcon_port, query_port, game_port = gt6server.pick_ports(chain.preferred_ports
                                                            + (chain.preferred_ports[0] - 10,))
    # node-suffixed artifacts: the per-leg log paths are the card's evidence pair
    log_path = gt6server.ARTIFACT_DIR / f"gt6_rs_{chain.slug}_census_{framework.node_suffix(args.node)}.log"
    pid_path = log_path.with_suffix(".pid")
    run_dir_ = gt6server.run_dir(framework.WORKTREE_ROOT, args.node)
    print(f"[census_ore] node {args.node} ({task})")
    print(f"[census_ore] ports rcon={rcon_port} query={query_port} game={game_port}")
    print(f"[census_ore] worktree {framework.WORKTREE_ROOT}, artifacts {log_path}")
    gt6server.assert_ports_free((rcon_port, query_port, game_port), label=CHAIN_STEM)

    gt6server.provision_run_dir(framework.WORKTREE_ROOT, game_port, rcon_port,
                                query_port, chain.password, node=args.node)
    patch_world_form(run_dir_ / "server.properties", args.seed)
    delete_worlds(run_dir_)
    offset = 0  # a fresh boot owns the whole log
    pid = gt6server.start_server(framework.WORKTREE_ROOT, log_path, pid_path,
                                 gradle_task=task)
    per_pass = []
    try:
        if not gt6server.wait_done(log_path, chain.boot_timeout, pid=pid):
            raise RuntimeError(f"server never reached Done; log tail:\n"
                               f"{gt6server.log_tail(log_path)}")
        print(f"[census_ore] server up (Done)")
        trio_ok = census_summary(log_path, offset)
        time.sleep(2)
        region_ = gt6world.region(chain.sites)
        print(f"[census_ore] cleanup region {region_} "
              f"({gt6world.command_blocks_used(region_)} blocks)")
        for number in range(1, args.passes + 1):
            per_pass.append(framework._run_pass(chain, region_, rcon_port, number,
                                                args.passes))
    finally:
        gt6server.stop_server(pid_path, rcon=(chain.host, rcon_port, chain.password))

    errors = gt6server.server_error_lines(log_path, start_byte=offset)
    print(f"\n===== census_ore {args.node}: pass failures {per_pass}, "
          f"server ERROR lines: {errors} =====")
    ok = all(count == 0 for count in per_pass) and errors == 0 and trio_ok
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
