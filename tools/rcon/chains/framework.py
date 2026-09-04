#!/usr/bin/env python3
"""chain runner — execute one declarative Chain end to end (layer 3 glue).

Two execution models, routed by GT6_SESSION (default on):

- session (run_session): ONE boot runs N chains (per decision 2026-09-04-
  rcon-gate-split; boot was 52-59 s each, the wall-clock king). Chain
  isolation is structural, three layers: the chain's own bbox cleanup at the
  boundary, the session global baseline (time set day / weather clear /
  vanilla gamerules) at every boundary, and boot groups split on
  fresh_boot / mutates conflicts. Per-chain verdict semantics unchanged.
- per-chain boot (run): the historical model, one boot per chain — the
  GT6_SESSION=off fallback, byte for byte.

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

import os
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

# ---------------------------------------------------------------------------
# the session execution model (decision 2026-09-04-rcon-gate-split ①③④):
# one boot runs N chains. Boot was the wall-clock king (52-59 s per boot, ~20
# boots per full sweep); run_session reuses the running server across chains
# and isolates the chains with three structural layers:
#   1. chain boundary: the chain's OWN bbox cleanup over its declared sites
#      (gt6world — an undeclared site is a red, so the coverage is structural);
#   2. the session global-state baseline (below) restored at every boundary,
#      so later chains never run under the night/rain a per-chain boot never
#      sees either;
#   3. fresh_boot chains and mutates conflicts split the boot groups
#      (plan_groups) — the group boundary IS a fresh boot.
# GT6_SESSION=off routes main() back to run(), the historical per-chain boot
# path, byte for byte — the gate must never be blocked by the optimization.
# ---------------------------------------------------------------------------
SESSION_ENV = "GT6_SESSION"


def session_enabled():
    """True unless GT6_SESSION is off/0/false/no (the one-switch fallback)."""
    return os.environ.get(SESSION_ENV, "on").strip().lower() not in ("off", "0", "false", "no")


# A fresh boot starts at time 0, clear sky, vanilla gamerules. A session that
# reuses one boot across chains restores exactly that baseline at every chain
# boundary. doFireTick=true matters structurally: the p13 burning-box fire-
# spread arm needs it on, exactly as on a fresh boot.
SESSION_RESET_COMMANDS = (
    "time set day",
    "weather clear",
    "gamerule doDaylightCycle true",
    "gamerule doWeatherCycle true",
    "gamerule doFireTick true",
    "gamerule doMobSpawning true",
    "gamerule randomTickSpeed 3",
)

# Node-keyed preferred (rcon, query, game) port triples for the session boot;
# pick_ports bumps on busy. 1.20.1 keeps the 256xx status quo; 1.21.1 gets its
# own segment so the dual-worktree dual-node sweeps never fight (ADR-P15-4).
SESSION_PORTS = {
    "1.20.1": (25662, 25672, 25652),
    "1.21.1": (25762, 25772, 25752),
}


def node_key(node):
    """'1.20.1-forge' -> '1.20.1' (the SESSION_PORTS key; loader suffix stripped)."""
    return (node or DEFAULT_NODE).split("-", 1)[0]


def node_suffix(node):
    """Artifact-slug-safe node tag: '1.20.1-forge' -> '1201-forge'."""
    return (node or DEFAULT_NODE).replace(".", "")


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
    poll       — poll-to-expect: RESEND the command until the response matches
                 (the single-shot judge's pass condition) or this many seconds
                 elapse; the final response is then judged exactly once, so the
                 verdict semantics are byte-identical to a single shot and only
                 the timing mechanism changes. Replaces worst-case fixed sleeps
                 where the expect itself is the waited-for condition.
    """
    cmd: str = None
    expect: str = None
    allow_failed: bool = False
    sleep: float = 0.0
    poll: float = 0.0
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
    mutates: tuple = ()                         # global-state keys this chain flips (e.g.
                                                # "fakesource"); a same-group conflict splits
                                                # the boot group in plan_groups
    fresh_boot: bool = False                    # boot alone: the group boundary IS a fresh
                                                # boot (the boot-health smoke, decision ③)


POLL_INTERVAL = 1.0      # seconds between poll resends (server ticks pace the state)


def _step_matches(step, body):
    """The single-shot judge's pass condition, without printing — the poll gate."""
    if "FAILED" in body and not step.allow_failed:
        return False
    return step.expect is None or step.expect in body


def _step_verdict(step, body):
    """The verdict string for the record: the same two conditions judge_output scores."""
    missed = "FAILED" in body or (step.expect is not None and step.expect not in body)
    if missed and not step.allow_failed:
        return "FAIL"
    return "ALLOWED" if missed else "PASS"


def _poll_step(client, step):
    """Resend the command until it matches or the poll deadline; return the final body.

    run_command never raises on a silent server (it returns []), so a resend is
    always safe. Every attempt is a full honest command: probes are read-only
    stats in practice, and the final body — the one judged — is the last one sent.
    """
    print(f"$ {step.cmd}   # poll <= {step.poll:g}s")
    deadline = time.monotonic() + step.poll
    attempts = 0
    while True:
        attempts += 1
        outs = client.run_command(step.cmd)
        body = "\n".join(outs) if isinstance(outs, list) else str(outs)
        if _step_matches(step, body):
            return body
        if time.monotonic() >= deadline:
            print(f"  [poll: giving up after {step.poll:g}s, {attempts} attempts]")
            return body
        print(f"  [poll {attempts}: not yet]")
        time.sleep(POLL_INTERVAL)


def run_steps(client, steps, verdicts=None):
    """Judge the steps of one pass against an authenticated RconClient.

    Returns the failure count. Label-only steps print headers and are skipped
    in numbering; command steps print the gt6rcon transcript and verdicts.
    `verdicts`, when given a list, receives one {"index", "cmd", "verdict"}
    record per command step — the per-step ledger the sweep runner diffs
    between execution models (session vs per-chain boot).
    """
    failure = 0
    index = 0
    for step in steps:
        if step.label is not None and step.cmd is None:
            print(f"\n=== {step.label}")
            continue
        index += 1
        if step.poll:
            body = _poll_step(client, step)
            print(body if body else "<no response>")
        else:
            outs = client.run_command(step.cmd)
            body = "\n".join(outs) if isinstance(outs, list) else str(outs)
            print(f"$ {step.cmd}\n{body if body else '<no response>'}")
        failure += gt6rcon.judge_output(index, body, step.expect, step.allow_failed)
        if verdicts is not None:
            verdicts.append({"index": index, "cmd": step.cmd,
                             "verdict": _step_verdict(step, body)})
        if step.sleep:
            time.sleep(step.sleep)
    return failure


def _run_pass(chain, region_, rcon_port, number, total, verdicts=None):
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
        return run_steps(client, chain.steps, verdicts)


def run(chain, passes=None, verdicts=None):
    """Boot the server, run all passes, stop precisely; return the exit code.

    This is the per-chain boot model, kept byte for byte as the GT6_SESSION=off
    fallback path (decision 2026-09-04-rcon-gate-split ④). `verdicts`, when given
    a list, receives the per-step verdict ledger of every pass.
    """
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
            per_pass.append(_run_pass(chain, region_, rcon_port, number, passes,
                                      verdicts))
    finally:
        gt6server.stop_server(pid_path, rcon=(chain.host, rcon_port, chain.password))

    errors = gt6server.server_error_lines(log_path)
    print(f"\n===== {chain.name}: pass failures {per_pass}, "
          f"server ERROR lines: {errors} =====")
    return 0 if all(count == 0 for count in per_pass) else 1


def plan_groups(chains):
    """Split chains into same-boot groups, input order preserved.

    Two structural split rules (decision ③): a fresh_boot chain is a singleton
    group (the group boundary is a fresh boot), and a chain whose mutates keys
    collide with the running group's starts a new group. Deterministic; the
    sweep runner additionally pre-clusters chains by coordinate bands.
    """
    groups = []
    current, used = [], set()
    for chain in chains:
        if chain.fresh_boot:
            if current:
                groups.append(current)
            groups.append([chain])
            current, used = [], set()
        elif current and set(chain.mutates) & used:
            groups.append(current)
            current, used = [chain], set(chain.mutates)
        else:
            current.append(chain)
            used |= set(chain.mutates)
    if current:
        groups.append(current)
    return groups


def _log_size(log_path):
    try:
        return os.path.getsize(log_path)
    except OSError:
        return 0


def _run_chain_on_server(chain, rcon_port, log_path, session_slug):
    """One chain against an already-running server: boundary isolation + passes.

    Layer 1+2 of the session isolation: restore the global baseline, then the
    chain's own forceload + bbox cleanup over its declared sites, then the
    unchanged per-pass structure (each pass still opens with its own cleanup).
    ERROR lines are counted over this chain's slice of the shared session log.
    """
    print(f"\n##### [{session_slug}] chain {chain.name} "
          f"(sites {len(chain.sites)}, passes {chain.passes})")
    verdicts = []
    per_pass = []
    offset = _log_size(log_path)
    region_ = gt6world.region(chain.sites)
    with gt6rcon.RconClient(chain.host, rcon_port, chain.password,
                            first_timeout=chain.response_timeout) as client:
        for command in SESSION_RESET_COMMANDS:
            print(f"$ {command}   # session baseline reset")
            client.run_command(command)
        for command in gt6world.forceload_commands(region_):
            client.run_command(command)
        for command in gt6world.cleanup_commands(region_):
            print(f"$ {command}   # chain-boundary bbox cleanup over the declared sites")
            client.run_command(command)
        time.sleep(2)
        for number in range(1, chain.passes + 1):
            per_pass.append(_run_pass(chain, region_, rcon_port, number,
                                      chain.passes, verdicts))
    errors = gt6server.server_error_lines(log_path, start_byte=offset)
    print(f"\n===== {chain.name}: pass failures {per_pass}, "
          f"server ERROR lines: {errors} =====")
    return {"pass_failures": per_pass, "error_lines": errors, "verdicts": verdicts}


def run_session(chains, node=None):
    """One boot, N chains: the session execution model (decision ①③).

    Chains are grouped by plan_groups; every group reuses the running server;
    each chain boundary restores the baseline and re-cleans the chain's own
    sites. Per-chain verdict semantics are unchanged — same steps, same judge,
    same passes — only the boot count drops (20 -> the group count). Returns
    the overall exit code: 0 iff every chain judged every pass clean.
    """
    chains = list(chains)
    if not chains:
        return 0
    nodes = {chain.node for chain in chains} - {None}
    if len(nodes) > 1:
        raise SystemExit(f"run_session: mixed nodes in one session: {sorted(nodes)}")
    node = node or (nodes.pop() if nodes else None) or requested_node() or DEFAULT_NODE
    task = gt6server.gradle_task(node)
    groups = plan_groups(chains)
    rcon_port, query_port, game_port = gt6server.pick_ports(
        SESSION_PORTS.get(node_key(node), SESSION_PORTS["1.20.1"]))
    slug = f"session_{node_suffix(node)}"
    log_path, pid_path = gt6server.artifact_paths(slug)
    print(f"[{slug}] node {node} ({task}): {len(chains)} chains in {len(groups)} boot group(s)")
    print(f"[{slug}] ports rcon={rcon_port} query={query_port} game={game_port}")
    print(f"[{slug}] worktree {WORKTREE_ROOT}, artifacts {log_path}")

    password = chains[0].password
    gt6server.provision_run_dir(WORKTREE_ROOT, game_port, rcon_port, query_port,
                                password, node=node)
    boot_timeout = max(chain.boot_timeout for chain in chains)
    started = time.monotonic()
    pid = gt6server.start_server(WORKTREE_ROOT, log_path, pid_path, gradle_task=task)
    results = {}
    boot_s = None
    try:
        if not gt6server.wait_done(log_path, boot_timeout, pid=pid):
            raise RuntimeError(
                f"session boot never printed '{gt6server.DONE_MARKER}' within "
                f"{boot_timeout:g}s; log tail:\n{gt6server.log_tail(log_path)}")
        boot_s = time.monotonic() - started
        print(f"[{slug}] server up (Done) in {boot_s:.0f}s")
        time.sleep(2)
        for group in groups:
            for chain in group:
                t0 = time.monotonic()
                results[chain.name] = _run_chain_on_server(
                    chain, rcon_port, log_path, slug)
                results[chain.name]["seconds"] = round(time.monotonic() - t0, 1)
    finally:
        gt6server.stop_server(pid_path, rcon=(chains[0].host, rcon_port, password))

    overall = 0
    for name, res in results.items():
        print(f"[{slug}] {name}: pass failures {res['pass_failures']}, "
              f"server ERROR lines: {res['error_lines']} ({res['seconds']}s)")
        if any(res["pass_failures"]):
            overall = 1
    wall = time.monotonic() - started
    print(f"[{slug}] wall {wall:.0f}s (boot {boot_s:.0f}s), "
          f"{len(groups)} boot(s) for {len(chains)} chains -> "
          f"{'GREEN' if overall == 0 else 'RED'}")
    return overall


def main(chain):
    """The per-chain entry point, routed by GT6_SESSION (default on).

    on  — run_session([chain]): a single chain is a one-group session (one boot,
          the baseline reset and boundary cleanup included, node-suffixed
          artifacts).
    off — run(chain): the historical per-chain boot path, byte for byte.
    """
    if session_enabled():
        sys.exit(run_session([chain]))
    sys.exit(run(chain))
