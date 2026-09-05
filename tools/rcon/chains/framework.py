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

import io
import os
import sys
import threading
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


# The concurrency degree (user calibration 2026-09-04: concurrency is THE lever —
# a chain's must-wait window is filled with other chains' steps on the same boot).
# Default 1 = serial waves, the diff-proven path; GT6_CONCURRENCY=N opts in.
CONCURRENCY_ENV = "GT6_CONCURRENCY"


def concurrency_degree():
    """The GT6_CONCURRENCY wave-width cap (default 1; junk falls back to 1)."""
    try:
        return max(1, int(os.environ.get(CONCURRENCY_ENV, "1").strip() or "1"))
    except ValueError:
        return 1


def _boxes_disjoint(a, b):
    """True when the two (x1,y1,z1,x2,y2,z2) bboxes share no volume — i.e. AT
    LEAST ONE axis separates them (the sky-level rigs all overlap on y, so the
    x/z separation is the operative one)."""
    return any(a[i + 3] < b[i] or b[i + 3] < a[i] for i in range(3))


def plan_waves(chains, concurrency=1):
    """Pack a boot group into waves of concurrently-executing chains.

    The structural admission rules (user calibration, the strict form):
      - a chain that mutates global state never shares a wave — it becomes an
        exclusive wave of its own (the downgrade);
      - a chain joins a wave only when its site bbox (gt6world.region, margins
        included) is disjoint from EVERY member's — overlapping sites are
        structurally refused, never "probably fine";
      - fresh_boot chains never share a wave (plan_groups already isolates
        them; refused here too as defence in depth);
      - a wave holds at most `concurrency` chains.
    Waves execute sequentially; the chains inside one wave interleave their
    RCON sessions on the shared boot (one authenticated client per chain).
    """
    waves = []
    for chain in chains:
        placed = False
        if concurrency > 1 and not chain.fresh_boot and not chain.mutates:
            box = gt6world.region(chain.sites)
            for wave in waves:
                if len(wave) >= concurrency:
                    continue
                if all(_boxes_disjoint(box, gt6world.region(member.sites))
                       for member in wave):
                    wave.append(chain)
                    placed = True
                    break
        if not placed:
            waves.append([chain])
    return waves


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
    node_cmds  — per-node command overrides keyed by node_key(node) ("1.20.1",
                 "1.21.1", ...). The chain-level dual-node sweep runs the same
                 steps on both nodes; where a COMMAND's wire shape (not its
                 assertion) is loader-versioned — the 21.1 FluidStack codec
                 NBT keys vs the forge FluidName/Amount shape — the override
                 swaps the key shape and ONLY the key shape: the judged expect,
                 the verified target and the step order stay byte-identical
                 (task p15-dual-gate-closure, the p12fic key-shape ruling).
    """
    cmd: str = None
    expect: str = None
    allow_failed: bool = False
    sleep: float = 0.0
    poll: float = 0.0
    label: str = None
    node_cmds: dict = None


def step_cmd(step, node=None):
    """The command this step runs on `node`: the node_cmds override when the
    node has one, the plain cmd otherwise (the default-node shape)."""
    if step.node_cmds and node is not None:
        override = step.node_cmds.get(node_key(node))
        if override is not None:
            return override
    return step.cmd


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


def _poll_step(client, step, cmd):
    """Resend the command until it matches or the poll deadline; return the final body.

    run_command never raises on a silent server (it returns []), so a resend is
    always safe. Every attempt is a full honest command: probes are read-only
    stats in practice, and the final body — the one judged — is the last one sent.
    """
    print(f"$ {cmd}   # poll <= {step.poll:g}s")
    deadline = time.monotonic() + step.poll
    attempts = 0
    while True:
        attempts += 1
        outs = client.run_command(cmd)
        body = "\n".join(outs) if isinstance(outs, list) else str(outs)
        if _step_matches(step, body):
            return body
        if time.monotonic() >= deadline:
            print(f"  [poll: giving up after {step.poll:g}s, {attempts} attempts]")
            return body
        print(f"  [poll {attempts}: not yet]")
        time.sleep(POLL_INTERVAL)


def run_steps(client, steps, verdicts=None, node=None):
    """Judge the steps of one pass against an authenticated RconClient.

    Returns the failure count. Label-only steps print headers and are skipped
    in numbering; command steps print the gt6rcon transcript and verdicts.
    `verdicts`, when given a list, receives one {"index", "cmd", "verdict"}
    record per command step — the per-step ledger the sweep runner diffs
    between execution models (session vs per-chain boot). `node` resolves the
    per-node command override (Step.node_cmds); the recorded cmd is the one
    actually sent.
    """
    failure = 0
    index = 0
    for step in steps:
        if step.label is not None and step.cmd is None:
            print(f"\n=== {step.label}")
            continue
        index += 1
        cmd = step_cmd(step, node)
        if step.poll:
            body = _poll_step(client, step, cmd)
            print(body if body else "<no response>")
        else:
            outs = client.run_command(cmd)
            body = "\n".join(outs) if isinstance(outs, list) else str(outs)
            print(f"$ {cmd}\n{body if body else '<no response>'}")
        failure += gt6rcon.judge_output(index, body, step.expect, step.allow_failed)
        if verdicts is not None:
            verdicts.append({"index": index, "cmd": cmd,
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
        return run_steps(client, chain.steps, verdicts, node=chain.node)


def run(chain, passes=None, verdicts=None):
    """Boot the server, run all passes, stop precisely; return the exit code.

    This is the per-chain boot model, kept byte for byte as the GT6_SESSION=off
    fallback path (decision 2026-09-04-rcon-gate-split ④). `verdicts`, when given
    a list, receives the per-step verdict ledger of every pass.
    """
    passes = chain.passes if passes is None else passes
    node = chain.node or requested_node() or DEFAULT_NODE
    chain.node = node   # write back (sweep.py:127/:160 precedent) — without it
                        # _run_pass judges with chain.node=None and step_cmd
                        # never applies Step.node_cmds (P16 closeout Deviations
                        # 2: the side_io 21.1 leg merged forge {FluidName,Amount}
                        # NBT on the 21.1 server)
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


def _run_chain_on_server(chain, rcon_port, log_path, session_slug,
                         reset_baseline=True):
    """One chain against an already-running server: boundary isolation + passes.

    Layer 1+2 of the session isolation: restore the global baseline (skipped
    inside a concurrent wave — the wave-start control point owns it), then the
    chain's own forceload + bbox cleanup over its declared sites, then the
    unchanged per-pass structure (each pass still opens with its own cleanup).
    ERROR lines are counted over this chain's slice of the shared session log
    (under concurrency the slice is the chain's active window, so the count is
    the window's total, not solely that chain's).
    """
    print(f"\n##### [{session_slug}] chain {chain.name} "
          f"(sites {len(chain.sites)}, passes {chain.passes})")
    verdicts = []
    per_pass = []
    offset = _log_size(log_path)
    region_ = gt6world.region(chain.sites)
    with gt6rcon.RconClient(chain.host, rcon_port, chain.password,
                            first_timeout=chain.response_timeout) as client:
        if reset_baseline:
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


class _ThreadRouter:
    """sys.stdout stand-in that routes writes by thread: registered wave threads
    write into their own buffer, everyone else passes through. redirect_stdout
    swaps the GLOBAL sys.stdout and would cross-wire concurrent chains — this
    router is the thread-safe form of the same capture."""

    def __init__(self, passthrough):
        self._passthrough = passthrough
        self._routes = {}

    def register(self, buf):
        self._routes[threading.get_ident()] = buf

    def write(self, text):
        buf = self._routes.get(threading.get_ident())
        return self._passthrough.write(text) if buf is None else buf.write(text)

    def flush(self):
        buf = self._routes.get(threading.get_ident())
        (self._passthrough if buf is None else buf).flush()

    def __getattr__(self, name):
        return getattr(self._passthrough, name)


def _run_wave(wave, rcon_port, log_path, session_slug, results, lock):
    """Execute one wave: concurrently when it holds several chains.

    Each chain runs in its own thread with its own authenticated RCON client
    (vanilla RCON is multi-client; the commands serialize server-side but the
    per-command quiet windows overlap client-side — that overlap is the point).
    Transcripts are captured per chain through the thread-routing stdout and
    printed after the join, so the evidence stays line-readable. Any thread
    exception lands in the record as a failed chain instead of killing the boot.
    """
    if len(wave) == 1:
        chain = wave[0]
        t0 = time.monotonic()
        results[chain.name] = _run_chain_on_server(chain, rcon_port, log_path,
                                                   session_slug)
        results[chain.name]["seconds"] = round(time.monotonic() - t0, 1)
        return

    buffers = {chain.name: io.StringIO() for chain in wave}
    router = _ThreadRouter(sys.stdout)

    def _worker(chain):
        buf = buffers[chain.name]
        router.register(buf)
        t0 = time.monotonic()
        try:
            res = _run_chain_on_server(chain, rcon_port, log_path,
                                       session_slug, reset_baseline=False)
        except Exception as exc:  # one broken chain must not kill the wave
            res = {"pass_failures": [1], "error_lines": -1, "verdicts": [],
                   "exception": repr(exc)}
        res["seconds"] = round(time.monotonic() - t0, 1)
        with lock:
            results[chain.name] = res

    print(f"[{session_slug}] wave: {len(wave)} chains interleaved "
          f"({', '.join(c.name for c in wave)})")
    old_stdout = sys.stdout
    sys.stdout = router
    try:
        threads = [threading.Thread(target=_worker, args=(chain,), name=chain.name)
                   for chain in wave]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()
    finally:
        sys.stdout = old_stdout
    for chain in wave:
        print(f"\n----- transcript [{chain.name}] -----")
        print(buffers[chain.name].getvalue(), end="")


def run_session_recorded(chains, node=None, concurrency=None):
    """One boot, N chains — the session model, returning the full record.

    plan_groups splits boot groups (fresh_boot / mutates); within a group,
    plan_waves packs mutually site-disjoint chains into concurrent waves
    (GT6_CONCURRENCY, default 1 = serial). Each wave start is a global-baseline
    control point; chains inside a wave declare no global mutations and own
    disjoint site bboxes. Per-chain verdict semantics are unchanged — same
    steps, same judge, same passes. Returns:
      {"exit": int, "node": str, "boots": int, "wall_s": float,
       "concurrency": int, "chains": {name: {pass_failures, error_lines,
       verdicts, seconds, exception?}}}
    """
    chains = list(chains)
    started = time.monotonic()
    if not chains:
        return {"exit": 0, "node": node or DEFAULT_NODE, "boots": 0, "wall_s": 0.0,
                "concurrency": 1, "chains": {}}
    nodes = {chain.node for chain in chains} - {None}
    if len(nodes) > 1:
        raise SystemExit(f"run_session: mixed nodes in one session: {sorted(nodes)}")
    node = node or (nodes.pop() if nodes else None) or requested_node() or DEFAULT_NODE
    for chain in chains:   # same write-back rule as run(): the steps' node_cmds
        if chain.node is None:          # keying reads chain.node; a --node
            chain.node = node           # override must reach step_cmd too
    concurrency = concurrency if concurrency is not None else concurrency_degree()
    task = gt6server.gradle_task(node)
    groups = plan_groups(chains)
    rcon_port, query_port, game_port = gt6server.pick_ports(
        SESSION_PORTS.get(node_key(node), SESSION_PORTS["1.20.1"]))
    slug = f"session_{node_suffix(node)}"
    log_path, pid_path = gt6server.artifact_paths(slug)
    print(f"[{slug}] node {node} ({task}): {len(chains)} chains in {len(groups)} "
          f"boot group(s), concurrency {concurrency}")
    print(f"[{slug}] ports rcon={rcon_port} query={query_port} game={game_port}")
    print(f"[{slug}] worktree {WORKTREE_ROOT}, artifacts {log_path}")

    password = chains[0].password
    gt6server.provision_run_dir(WORKTREE_ROOT, game_port, rcon_port, query_port,
                                password, node=node)
    boot_timeout = max(chain.boot_timeout for chain in chains)
    if concurrency > 1:
        # vanilla routes every rcon command through one server-wide
        # RconConsoleSource (DedicatedServer.java:517-521) — concurrent streams
        # cross-contaminate responses unless the wire is atomic per command
        gt6rcon.set_wire_lock(threading.Lock())
    pid = gt6server.start_server(WORKTREE_ROOT, log_path, pid_path, gradle_task=task)
    results = {}
    waves_seen = []
    overall = 0
    boot_s = None
    try:
        if not gt6server.wait_done(log_path, boot_timeout, pid=pid):
            raise RuntimeError(
                f"session boot never printed '{gt6server.DONE_MARKER}' within "
                f"{boot_timeout:g}s; log tail:\n{gt6server.log_tail(log_path)}")
        boot_s = time.monotonic() - started
        print(f"[{slug}] server up (Done) in {boot_s:.0f}s")
        time.sleep(2)
        lock = threading.Lock()
        for group in groups:
            for wave in plan_waves(group, concurrency):
                waves_seen.append([chain.name for chain in wave])
                # the wave start is THE global-baseline control point: one
                # client restores time/weather/gamerules before the wave's
                # chains fan out (their boundary cleanups touch disjoint sites)
                with gt6rcon.RconClient(chains[0].host, rcon_port, password,
                                        first_timeout=chains[0].response_timeout
                                        ) as client:
                    for command in SESSION_RESET_COMMANDS:
                        print(f"$ {command}   # wave baseline reset")
                        client.run_command(command)
                _run_wave(wave, rcon_port, log_path, slug, results, lock)
    finally:
        gt6server.stop_server(pid_path, rcon=(chains[0].host, rcon_port, password))
        gt6rcon.set_wire_lock(None)

    for name, res in results.items():
        print(f"[{slug}] {name}: pass failures {res['pass_failures']}, "
              f"server ERROR lines: {res['error_lines']} ({res['seconds']}s)")
        if any(res["pass_failures"]) or res.get("exception"):
            overall = 1
    wall = time.monotonic() - started
    print(f"[{slug}] wall {wall:.0f}s (boot {boot_s:.0f}s), {len(groups)} boot(s), "
          f"{len(waves_seen)} wave(s) at concurrency {concurrency} -> "
          f"{'GREEN' if overall == 0 else 'RED'}")
    return {"exit": overall, "node": node, "boots": len(groups), "wall_s": round(wall, 1),
            "concurrency": concurrency, "waves": waves_seen, "chains": results}


def run_session(chains, node=None, concurrency=None):
    """The session model's exit-code face (main()'s route; see run_session_recorded)."""
    return run_session_recorded(chains, node, concurrency)["exit"]


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
