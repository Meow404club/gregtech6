#!/usr/bin/env python3
"""chain runner — execute one declarative Chain end to end (layer 3 glue).

`python3 tools/rcon/chains/<chain>.py` performs, in one pass:

  1. gt6server.pick_ports — ss pre-checked rcon/query/game ports with bump;
  2. gt6server.provision_run_dir — eula + server.properties in THIS worktree,
     in the node's own run dir (mdk/versions/<node>/run since the stonecutter
     skeleton; --node <name> or Chain.node picks the node, default 1.20.1-forge);
  3. gt6server.start_server + wait_done — the nohup semantics, Done polled
     (:mdk:<node>:runServer);
  4. per pass: gt6world forceload + bbox cleanup (the declared sites, structurally
     complete), then the chain's steps judged via gt6rcon.judge_output;
  5. gt6server.stop_server — RCON stop, then precise pid kills; daemons untouched.

Exit code: 0 when every pass judged clean, 1 otherwise.
"""

import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

_TOOLS_RCON = Path(__file__).resolve().parent.parent
if str(_TOOLS_RCON) not in sys.path:
    sys.path.insert(0, str(_TOOLS_RCON))

import gt6rcon
import gt6server
import gt6world

# .../tools/rcon/chains/framework.py -> parents[3] is the worktree root
WORKTREE_ROOT = Path(__file__).resolve().parents[3]

# The stonecutter vcsVersion node — where the in-tree shared sources compile as-is.
# The bare :mdk:runServer task died with the P15 stonecutter skeleton; every boot
# now addresses a versioned subproject (:mdk:<node>:runServer) with a node-local
# run dir (mdk/versions/<node>/run). A chain pins its node via Chain.node; a
# runner-level --node override lets the dual-node sweeps run the same chain on
# both nodes without touching the chain modules.
DEFAULT_NODE = "1.20.1-forge"


def requested_node(argv=None):
    """The --node <name> CLI override, or None when absent (argv-safe for tests)."""
    argv = sys.argv if argv is None else argv
    if "--node" in argv:
        return argv[argv.index("--node") + 1]
    return None


@dataclass
class Step:
    """One declarative step. cmd=None with a label prints a pure section header.

    expect     — substring the response must contain (gt6rcon.judge_output)
    allow_failed — FAILED marker / missed expect reported ALLOWED, not counted
    sleep      — seconds to wait after the command (tick-driven assertions:
                 hopper transfers need real server ticks to happen)
    """
    cmd: str = None
    expect: str = None
    allow_failed: bool = False
    sleep: float = 0.0
    label: str = None


def phase(label):
    """A pure section header step (no command, no judgement)."""
    return Step(label=label)


@dataclass
class Chain:
    """A card's acceptance chain: sites + lifecycle config + steps."""
    name: str
    slug: str                                   # artifact names: /tmp/gt6_rs_<slug>.*
    steps: list = field(default_factory=list)
    sites: tuple = ()                           # gt6world sites; drives the bbox cleanup
    password: str = "gt6"
    preferred_ports: tuple = (25662, 25672)     # (rcon, query); game defaults to rcon-10
    game_port: int = None
    host: str = "127.0.0.1"
    boot_timeout: float = 600.0                 # first boot in a fresh worktree compiles
    response_timeout: float = 15.0              # per-command deadline (atom-era value)
    passes: int = 2                             # pass 2 is the idempotency proof
    node: str = None                            # stonecutter node (None = DEFAULT_NODE;
                                                # --node <name> overrides for dual-node sweeps)


def run_steps(client, steps):
    """Judge the steps of one pass against an authenticated RconClient.

    Returns the failure count. Label-only steps print headers and are skipped
    in numbering; command steps print the gt6rcon transcript and verdicts.
    """
    failure = 0
    index = 0
    for step in steps:
        if step.label is not None and step.cmd is None:
            print(f"\n=== {step.label}")
            continue
        index += 1
        outs = client.run_command(step.cmd)
        body = "\n".join(outs) if isinstance(outs, list) else str(outs)
        print(f"$ {step.cmd}\n{body if body else '<no response>'}")
        failure += gt6rcon.judge_output(index, body, step.expect, step.allow_failed)
        if step.sleep:
            time.sleep(step.sleep)
    return failure


def _run_pass(chain, region_, rcon_port, number, total):
    print(f"\n===== {chain.name} pass {number}/{total} =====")
    with gt6rcon.RconClient(chain.host, rcon_port, chain.password,
                            first_timeout=chain.response_timeout) as client:
        for command in gt6world.forceload_commands(region_):
            print(f"$ {command}")
            client.run_command(command)
        time.sleep(2)
        for command in gt6world.cleanup_commands(region_):
            print(f"$ {command}   # gt6world bbox cleanup over the declared sites")
            client.run_command(command)
        time.sleep(1)
        return run_steps(client, chain.steps)


def run(chain, passes=None):
    """Boot the server, run all passes, stop precisely; return the exit code."""
    passes = chain.passes if passes is None else passes
    node = chain.node or requested_node() or DEFAULT_NODE
    task = gt6server.gradle_task(node)
    rcon_port, query_port, game_port = gt6server.pick_ports(
        chain.preferred_ports + ((chain.game_port,)
                                 if chain.game_port else
                                 (chain.preferred_ports[0] - 10,)))
    log_path, pid_path = gt6server.artifact_paths(chain.slug)
    print(f"[{chain.name}] node {node} ({task})")
    print(f"[{chain.name}] ports rcon={rcon_port} query={query_port} game={game_port}")
    print(f"[{chain.name}] worktree {WORKTREE_ROOT}, artifacts {log_path}")

    gt6server.provision_run_dir(WORKTREE_ROOT, game_port, rcon_port, query_port,
                                chain.password, node=node)
    pid = gt6server.start_server(WORKTREE_ROOT, log_path, pid_path, gradle_task=task)
    per_pass = []
    try:
        if not gt6server.wait_done(log_path, chain.boot_timeout, pid=pid):
            raise RuntimeError(
                f"server never printed '{gt6server.DONE_MARKER}' within "
                f"{chain.boot_timeout:g}s; log tail:\n{gt6server.log_tail(log_path)}")
        print(f"[{chain.name}] server up (Done)")
        time.sleep(2)
        region_ = gt6world.region(chain.sites)
        print(f"[{chain.name}] cleanup region {region_} "
              f"({gt6world.command_blocks_used(region_)} blocks, "
              f"{len(gt6world.cleanup_commands(region_))} fill)")
        for number in range(1, passes + 1):
            per_pass.append(_run_pass(chain, region_, rcon_port, number, passes))
    finally:
        gt6server.stop_server(pid_path, rcon=(chain.host, rcon_port, chain.password))

    errors = gt6server.server_error_lines(log_path)
    print(f"\n===== {chain.name}: pass failures {per_pass}, "
          f"server ERROR lines: {errors} =====")
    return 0 if all(count == 0 for count in per_pass) else 1


def main(chain):
    sys.exit(run(chain))
