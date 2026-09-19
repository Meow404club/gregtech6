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
     complete), then the chain's steps judged via the structured judge_step
     (p32: the expect is the assertion; gt6rcon.judge_output stays the CLI
     layer's judge);
  5. gt6server.stop_server — RCON stop, then precise pid kills; daemons untouched.

Exit code: 0 when every pass judged clean, 1 otherwise.
"""

import io
import hashlib
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

# The bare Chain dataclass default pair — a chain carrying it has made no
# statement of its own, so session mode may apply the node table above.
DEFAULT_PREFERRED_PORTS = (25662, 25672)


def session_ports(chains, node=None):
    """The session boot's (rcon, query, game) port-start triple, from the chains.

    A session binds ONE port triple no matter how many chains share the boot —
    the chains connect through the session's rcon port (_run_chain_on_server
    takes it as a parameter), so the per-chain `preferred_ports` pins are
    per-boot semantics by design. Policy (P17, fixing the hardwired 25662
    lookup the P16 closeout flagged):

    - exactly one distinct declared pair among the chains (beyond the bare
      Chain default) anchors the boot — a one-chain session now addresses the
      same triple its per-chain boot (run) would; game = rcon-10, or the one
      distinct declared game_port when a chain pins it;
    - otherwise — nobody declared, or the declarations disagree (the norm:
      every chain pins its own pair so PARALLEL per-chain boots never fight) —
      the historical SESSION_PORTS node segments apply unchanged; they are
      what keeps the --dual legs apart by node (ADR-P15-4).
    """
    fallback = SESSION_PORTS.get(node_key(node), SESSION_PORTS["1.20.1"])
    declared = ({chain.preferred_ports for chain in chains}
                - {DEFAULT_PREFERRED_PORTS})
    if len(declared) != 1:
        return fallback
    rcon, query = declared.pop()
    game_pins = {chain.game_port for chain in chains if chain.game_port}
    game = game_pins.pop() if len(game_pins) == 1 else rcon - 10
    return (rcon, query, game)


def node_key(node):
    """'1.20.1-forge' -> '1.20.1' (the SESSION_PORTS key; loader suffix stripped)."""
    return (node or DEFAULT_NODE).split("-", 1)[0]


def node_suffix(node):
    """Artifact-slug-safe node tag: '1.20.1-forge' -> '1201-forge'."""
    return (node or DEFAULT_NODE).replace(".", "")


def worktree_tag():
    """Eight hex chars naming THIS worktree (path hash) — parallel worktrees
    running the identical roster still get disjoint artifact pairs (the
    --dual legs; gradle's per-project lock only serializes same-worktree)."""
    return hashlib.md5(str(WORKTREE_ROOT).encode("utf-8")).hexdigest()[:8]


def session_slug(chains, node=None):
    """The session boot's artifact slug: session_<node>_<roster>-<worktree tag>.

    The historical bare session_<node> gave every same-node session ONE shared
    log/pid pair: parallel same-segment boots stomped each other's pid files —
    the recorded handle (stop_server's first kill target) pointed at a foreign
    boot, and the P16 first run died to an external SIGTERM mid-stomp (ADR-P16
    closeout lesson 8). The roster (sorted unique chain slugs) identifies the
    boot's contents; a long roster folds to a stable hash so the name stays
    bounded. No reader of the old name exists (repo grep 2026-09-05), so this
    is a clean rename without a compat shim.
    """
    roster = "+".join(sorted({chain.slug for chain in chains}))
    if len(roster) > 40:
        digest = hashlib.md5(roster.encode("utf-8")).hexdigest()[:8]
        roster = f"{roster[:24]}-{digest}"
    return f"session_{node_suffix(node)}_{roster}-{worktree_tag()}"


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


def uses_tick_window(chain):
    """True when any step carries tick_step (Step.uses_tick_window). A tick
    window freezes the WHOLE boot for ~N/20 s — concurrent wave-mates' polls
    and sleeps would starve inside it (the exact false-red family the tick
    primitive exists to kill), so such chains never share a wave. Applies on
    both legs: the forge degrade is a plain poll and would be wave-safe, but
    one conservative rule beats two (ponytail: the freeze, not the leg, is
    the hazard — and the leg can change without the chain noticing)."""
    return any(step.tick_step is not None for step in chain.steps)


def plan_waves(chains, concurrency=1):
    """Pack a boot group into waves of concurrently-executing chains.

    The structural admission rules (user calibration, the strict form):
      - a chain that mutates global state never shares a wave — it becomes an
        exclusive wave of its own (the downgrade);
      - a chain running tick windows (uses_tick_window) likewise never shares
        a wave — the freeze halts every co-tenant's timing;
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
        if concurrency > 1 and not chain.fresh_boot and not chain.mutates \
                and not uses_tick_window(chain):
            box = gt6world.region(chain.sites)
            for wave in waves:
                if len(wave) >= concurrency:
                    continue
                if any(uses_tick_window(member) for member in wave):
                    continue   # a tick-window member owns its wave alone
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
    node_expects — per-node expect overrides keyed by node_key(node); the twin
                 of node_cmds for ASSERTION-shape drift. Where a REPORT's
                 rendering is loader-versioned — the 21.1 ItemStack.toString
                 namespaces the item names the 1.20.1 leg renders plain — a
                 single spanning expect cannot be substring-exact on both
                 legs, so each leg pins its own exact line. The cmd, the
                 target, the step order and the judged-once poll contract stay
                 byte-identical. Resolved from chain.node at judge time (the
                 same write-back step_cmd reads), so a sweep-level --node
                 override forks exactly like a per-chain boot.
    tick_step   — the deterministic tick advance (P32, card
                 p32-ops-neo-tick-primitive; neo-leg only). On the TICK_NODE
                 leg the window runs: freeze → prove stillness (two gametime
                 reads) → /tick step N → settle until gametime is exactly
                 g0+N → run and judge THIS step's cmd+expect ONCE inside the
                 frozen world → thaw (proved). The single frozen-window read
                 replaces the poll resend loop — no starvation, no mid-flight
                 reads; the assert is a terminal value at an exact tick.
                 Requires cmd (a judged probe; a bare advance has no judge).
                 Exclusive with poll. Stepping paces at vanilla 20 tps
                 (MinecraftServer.java:687 — only sprint unpaces, :682-684):
                 N ticks cost ~N/20 s wall — determinism, not speed.
    tick_fallback_poll — the DECLARED forge-leg degradation: on a node without
                 the /tick primitive (1.20.1 has no TickCommand at all) the
                 step downgrades to poll-to-expect for this many seconds.
                 Absent → the step is a fail-visible RED (never a silent
                 downgrade onto a leg that cannot honor it).
    """
    cmd: str = None
    expect: str = None
    allow_failed: bool = False
    sleep: float = 0.0
    poll: float = 0.0
    label: str = None
    node_cmds: dict = None
    node_expects: dict = None
    tick_step: int = None
    tick_fallback_poll: float = None

    def __post_init__(self):
        if self.tick_step is not None:
            if self.tick_step < 1:
                raise ValueError(f"tick_step must be >= 1, got {self.tick_step}")
            if self.poll:
                raise ValueError("tick_step and poll are exclusive timing "
                                 "mechanisms (exact window vs resend loop)")
            if not self.cmd:
                raise ValueError("tick_step rides a judged probe (cmd+expect); "
                                 "a bare advance has no frozen-window judge")

    def uses_tick_window(self):
        """True when this step runs the deterministic tick window (any leg —
        plan_waves uses this for the exclusive-wave rule)."""
        return self.tick_step is not None


def step_cmd(step, node=None):
    """The command this step runs on `node`: the node_cmds override when the
    node has one, the plain cmd otherwise (the default-node shape)."""
    if step.node_cmds and node is not None:
        override = step.node_cmds.get(node_key(node))
        if override is not None:
            return override
    return step.cmd


def step_expect(step, node=None):
    """The expect this step is judged with on `node`: the node_expects override
    when the node has one, the plain expect otherwise (the default-node shape).

    The twin of step_cmd; see Step.node_expects. Resolved per JUDGEMENT (not at
    module import) from the same chain.node the run paths write back — a
    sweep-level --node override forks here exactly as it does in step_cmd."""
    if step.node_expects and node is not None:
        override = step.node_expects.get(node_key(node))
        if override is not None:
            return override
    return step.expect


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

# ---------------------------------------------------------------------------
# the deterministic tick window (P32, card p32-ops-neo-tick-primitive) —
# every semantic below is source-proven against the vanilla 1.21.1 tree:
#   - /tick exists there only: TickCommand.java:21-56 (literal "tick",
#     requires permission 3; query/rate/step/sprint/freeze/unfreeze). The
#     1.20.1 tree has no TickCommand at all — no server/commands/TickCommand
#     and no registration in Commands.java — hence the leg dialect gate.
#   - stepping REQUIRES frozen: ServerTickRateManager.stepGameIfPaused
#     (:40-48) returns false unless frozen (TickCommand.java:138 failure);
#     it just sets frozenTicksToRun=N and returns — the reply is immediate
#     (TickCommand.java:132-142), so gt6rcon's per-command timeouts are
#     untouched; the wait is CLIENT-side, until the world settled.
#   - freezing stops the world: TickRateManager.tick (:56-61) gates
#     runGameElements; ServerLevel.tick (:337-360) gates tickTime/blockTicks/
#     fluidTicks on runsNormally() — gametime does not advance while frozen
#     and advances EXACTLY +N across a step. That arithmetic (read via
#     `time query gametime`, TimeCommand.java:57 "commands.time.query") is
#     the only assertion the machinery makes about /tick — no /tick reply
#     text is matched, nothing is locale- or string-fragile.
#   - stepping is paced, not blocked, not sped: the loop keeps
#     nanosecondsPerTick (MinecraftServer.java:687; only SPRINT unpaces,
#     :682-684) — N ticks cost ~N/20 s wall, deterministically.
#   - commands still execute while frozen: rcon/console commands are drained
#     via runAllTasks in waitUntilNextTick (MinecraftServer.java:824-827;
#     DedicatedServer.java:517-519 executeBlocking) — every loop iteration,
#     frozen or not. The frozen-window probe is an ordinary command.
# ---------------------------------------------------------------------------
TICK_NODE = "1.21.1"                       # node_key that owns /tick (1.20.3+)
GAMETIME_QUERY = "time query gametime"
GAMETIME_PREFIX = "The time is "           # commands.time.query rendering
TICK_SECONDS_PER_TICK = 0.05               # vanilla 20 tps pacing of a step
TICK_SETTLE_MARGIN = 2.0                   # flat slack over N*0.05s
FREEZE_GAP = 0.3                           # >3 ticks: two equal reads this far
                                           # apart prove stillness (and, after
                                           # thaw, that the world moves again)


def _body(outs):
    """The judged/printable body of one command's response frames."""
    return "\n".join(outs) if isinstance(outs, list) else str(outs)


def _query_gametime(client):
    """`time query gametime` -> int, or None when the reply isn't parseable
    (never an exception: a None read drives the proof/red paths instead)."""
    body = _body(client.run_command(GAMETIME_QUERY))
    head, sep, tail = body.rpartition(GAMETIME_PREFIX)
    if not sep:
        return None
    token = tail.split(maxsplit=1)[0] if tail else ""
    try:
        return int(token)
    except ValueError:
        return None


def _tick_window(client, step, cmd, expect, index):
    """freeze → settle exactly N ticks → judge once inside the frozen window
    → thaw (proved). Returns (body, failure_count).

    Cleanup guarantee: the thaw runs on EVERY path — a session left frozen
    would stall every later chain's sleeps/polls (the exact false-red family
    this primitive kills), so a failed thaw reds the step too. Every /tick
    claim is behavioral: the freeze proof is two equal gametime reads
    FREEZE_GAP apart, the settle is gametime == g0+N (exact — the frozen gate
    cannot overshoot, TickRateManager.tick :56-61), the thaw proof is a
    moving read pair. No /tick reply text is matched anywhere.
    """
    failure = 0
    body = ""
    try:
        print("$ tick freeze   # tick_step window: deterministic "
              f"{step.tick_step}-tick advance")
        client.run_command("tick freeze")
        frozen_at = _query_gametime(client)
        time.sleep(FREEZE_GAP)
        still = _query_gametime(client)
        if frozen_at is None or still != frozen_at:
            print(f"  [tick_step {index}: freeze proof failed (gametime "
                  f"{frozen_at} -> {still}); /tick step NOT sent]")
            failure = 1
        else:
            target = frozen_at + step.tick_step
            print(f"$ tick step {step.tick_step}   # settle until gametime {target}")
            client.run_command(f"tick step {step.tick_step}")
            deadline = time.monotonic() + step.tick_step * TICK_SECONDS_PER_TICK \
                + TICK_SETTLE_MARGIN
            while _query_gametime(client) != target:
                if time.monotonic() >= deadline:
                    print(f"  [tick_step {index}: gametime never settled at "
                          f"{target} within deadline; window red]")
                    failure = 1
                    break
                time.sleep(POLL_INTERVAL)
            else:
                outs = client.run_command(cmd)
                body = _body(outs)
                print(f"$ {cmd}\n{body if body else '<no response>'}"
                      f"   # judged frozen at gametime {target}")
                failure = judge_step(index, step, body, expect)
    finally:
        print("$ tick unfreeze   # window cleanup (never leave a frozen session)")
        client.run_command("tick unfreeze")
        thawed_at = _query_gametime(client)
        time.sleep(FREEZE_GAP)
        moving = _query_gametime(client)
        if thawed_at is None or moving == thawed_at:
            print(f"  [tick_step {index}: THAW PROOF FAILED (gametime "
                  f"{thawed_at} -> {moving}) — the session may stay frozen; "
                  "every later chain would stall]")
            failure += 1
    return body, failure


def _step_judgement(step, body, expect):
    """The structured judge (p32-ops-judge-literal): (verdict, failure_count).

    The expect is THE assertion — a hit is a PASS even when the body spells
    FAILED elsewhere. The p31 pool rcon_judge_literal_blindspot: the raw
    "FAILED" scan used to override the expect, so an expected-rejection line
    ("FUEL FAILED", "STAT FAILED", "FAILED, connections N" — pinned via
    expect) counted as a failure, and every such chain had to pin
    allow_failed just to neutralize the literal — which ALSO disarmed its
    expect (a missed expect on an allowed step is ALLOWED: the false-green
    half of the blindspot). The literal scan survives only for expect-less
    steps, where it is the sole failure signal (the server-side RCON
    contract: every failure line carries FAILED — GTMultiBlockCommand.java
    :1046; arms that deliberately don't, like the form arm :463, MUST carry
    an expect). allow_failed keeps its semantics (gt6rcon.judge_output's
    contract — that judge remains the CLI layer's face): a missed expect or
    a bare marker on an allowed step is ALLOWED and does not count.
    """
    if expect is not None:
        if expect in body:
            return "PASS", 0
        if step.allow_failed:
            return "ALLOWED", 0
        return "FAIL", 1
    if "FAILED" in body:
        if step.allow_failed:
            return "ALLOWED", 0
        return "FAIL", 1
    return "PASS", 0


def _step_matches(step, body, expect):
    """The single-shot judge's pass condition, without printing — the poll gate."""
    if expect is not None:
        return expect in body
    return not ("FAILED" in body and not step.allow_failed)


def _step_verdict(step, body, expect):
    """The verdict string for the record: the same judgement run_steps counts."""
    return _step_judgement(step, body, expect)[0]


def judge_step(index, step, body, expect):
    """Print and score one step's output; return 1 when it counts as a failure.

    The framework's structured face of gt6rcon.judge_output — same printing
    shapes, same ALLOWED-does-not-count contract, minus the expect-blind
    literal (see _step_judgement). A clean expect-less step stays silent,
    as judge_output always did.
    """
    verdict, counts = _step_judgement(step, body, expect)
    if expect is not None:
        print(f"[expect {index}: {expect!r} -> {verdict}]")
    elif "FAILED" in body:
        print(f"[command {index}: FAILED marker in output -> {verdict}]")
    return counts


def _poll_step(client, step, cmd, expect, poll=None):
    """Resend the command until it matches or the poll deadline; return the final body.

    run_command never raises on a silent server (it returns []), so a resend is
    always safe. Every attempt is a full honest command: probes are read-only
    stats in practice, and the final body — the one judged — is the last one sent.
    `poll` overrides step.poll (the tick_fallback_poll degrade passes its
    declared window without cloning the step — replace() would re-run
    __post_init__ and trip the tick_step/poll exclusivity rule).
    """
    if poll is None:
        poll = step.poll
    print(f"$ {cmd}   # poll <= {poll:g}s")
    deadline = time.monotonic() + poll
    attempts = 0
    while True:
        attempts += 1
        outs = client.run_command(cmd)
        body = _body(outs)
        if _step_matches(step, body, expect):
            return body
        if time.monotonic() >= deadline:
            print(f"  [poll: giving up after {poll:g}s, {attempts} attempts]")
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
    per-node command override (Step.node_cmds) AND the per-node expect
    override (Step.node_expects); the recorded cmd is the one actually sent.

    A step carrying tick_step routes by leg: TICK_NODE runs the deterministic
    freeze/step/judge-frozen/thaw window (_tick_window); a leg without the
    /tick primitive runs the DECLARED tick_fallback_poll degrade (poll 制,
    judge-once semantics identical to poll) — or, undeclared, a fail-visible
    RED that sends nothing (never a silent downgrade).
    """
    failure = 0
    index = 0
    for step in steps:
        if step.label is not None and step.cmd is None:
            print(f"\n=== {step.label}")
            continue
        index += 1
        cmd = step_cmd(step, node)
        expect = step_expect(step, node)
        hard_red = False
        if step.tick_step is not None and node_key(node) != TICK_NODE:
            if step.tick_fallback_poll is None:
                print(f"[tick_step {index}: node {node} lacks the /tick "
                      "primitive (1.20.1 has no TickCommand at all) and "
                      "declares no tick_fallback_poll — RED; the forge poll "
                      "degradation must be declared in the chain]")
                body = ""
                failure += 1
                hard_red = True
            else:
                print(f"[tick_step {index}: node {node} lacks /tick — declared "
                      f"degrade to poll {step.tick_fallback_poll:g}s]")
                body = _poll_step(client, step, cmd, expect,
                                  poll=step.tick_fallback_poll)
                print(body if body else "<no response>")
                failure += judge_step(index, step, body, expect)
        elif step.tick_step is not None:
            body, counts = _tick_window(client, step, cmd, expect, index)
            failure += counts
        elif step.poll:
            body = _poll_step(client, step, cmd, expect)
            print(body if body else "<no response>")
            failure += judge_step(index, step, body, expect)
        else:
            outs = client.run_command(cmd)
            body = _body(outs)
            print(f"$ {cmd}\n{body if body else '<no response>'}")
            failure += judge_step(index, step, body, expect)
        if verdicts is not None:
            verdict = "FAIL" if hard_red else _step_verdict(step, body, expect)
            verdicts.append({"index": index, "cmd": cmd, "verdict": verdict})
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
    gt6server.assert_ports_free((rcon_port, query_port, game_port), label=chain.name)

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
    ports = session_ports(chains, node)
    rcon_port, query_port, game_port = gt6server.pick_ports(ports)
    slug = session_slug(chains, node)
    log_path, pid_path = gt6server.artifact_paths(slug)
    print(f"[{slug}] node {node} ({task}): {len(chains)} chains in {len(groups)} "
          f"boot group(s), concurrency {concurrency}")
    port_note = "" if ports == SESSION_PORTS.get(node_key(node), SESSION_PORTS["1.20.1"]) \
        else " (the chains' declared preferred_ports)"
    print(f"[{slug}] ports rcon={rcon_port} query={query_port} game={game_port}"
          f"{port_note}")
    print(f"[{slug}] worktree {WORKTREE_ROOT}, artifacts {log_path}")
    gt6server.assert_ports_free((rcon_port, query_port, game_port), label=slug)

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
