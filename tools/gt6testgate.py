#!/usr/bin/env python3
"""gt6testgate — the single gated entry point for every heavy GT6 operation.

WSL crashed three times in one day (ops triage 2026-09-22): coders running
gradle tests and RCON boots in parallel piled the box past its memory. User
ruling: ALL heavy operations (gradle test/compile/runData, sweeps, chain runs)
go through THIS wrapper, and every spawn passes two machine-wide gates:

- memory gate: while ``MemTotal - MemAvailable`` exceeds the limit (default
  30720 MiB, env ``GT6_GATE_MEM_LIMIT_MIB``), the spawn queues, polling every
  10 s; every queueing/admission event is journalled to /tmp/gt6_gate.log
  (time / used / queued duration).
- concurrency gate: ``GT6_GATE_MAX_CONCURRENT`` test slots (default 4) as
  O_EXCL files in /tmp/gt6_gate_slots, same shape as the gt6server boot slots
  (/tmp/gt6_rcon_slots): stale slots are reaped when the holding PID is dead
  or was reused by a non-gate process (gate-slot-pid-reap, 2026-10-03
  incident: dead slots faked a full board while the 2 h mtime reaper was
  still pending); the 2 h mtime age stays as the fallback for slots whose
  name carries no usable PID. A holder whose cmdline still names the gate
  is never reaped, however old — long gate runs are legal.
- role gate (test-gating-v2, 2026-09-27 OOM ruling): a FULL test run — any
  gradle invocation naming an unfiltered ``test``/``cleanTest`` task (no
  ``--tests``) — is review-seat only. Roles: ``--role {coder,review}`` or env
  ``GT6_TESTGATE_ROLE`` (default coder, fail-closed on bad values); a coder
  asking for full is rejected immediately (exit 2 + guidance) BEFORE any
  queueing. Filtered runs (``--tests``), compile, runData and sweeps are
  unrestricted for both roles. While a full run executes it holds a
  machine-wide flock on /tmp/gt6_testgate_full.lock; a second full request
  prints a queueing notice and blocks until the lock frees (kernel releases
  it if the holder dies, so no stale reaping is needed).

The gt6server boot path calls :func:`wait_memory` on top of its own slot
semaphore (ops-test-gate), so server boots obey the same memory cap; the
slot semaphore semantics there are unchanged.

Predictive admission (test-gating-v3, 2026-09-29 WSL crash ruling): the
reactive "used > limit" check let six coders + the review seat pile
concurrent gradle runs past 30G *between* polls. The gate predicts BEFORE
spawning using a machine-wide peak-RSS ledger
(/tmp/gt6_testgate_memory_ledger.json; unknown classes fall back to
conservative cold-start defaults). Every admitted run is itself the runner:
it samples the child's process-tree peak RSS while the child executes and
folds the observation back into the ledger (14-day half-life decay against
staleness). ``--dry-run`` prints the full prediction and starts nothing. A
PreToolUse hook (.githooks/guard-heavy-ops.sh) routes bare gradle
invocations here, and GITHUB_ACTIONS bypasses all gates at zero cost (CI
boxes are not this WSL host). Exit-code semantics are unchanged (child
passthrough; full+coder still exit 2 before any queueing).

Envelope admission (test-gating-v3.2, 2026-09-29 fifth ruling): the v3a
formula ``system_used + estimate <= 30G`` is RETIRED. It double-counted the
run's own slice usage as "system used" and, fed by a ledger estimate
polluted with shared-daemon RSS (a filtered-test estimate sat at 11.6G vs a
real 2.9G peak), starved a review run for an hour. Admission is now
computed INSIDE the envelope: ``slice memory.current + estimate(class)
<= cap`` (22G — the slice's own MemoryMax, see the v3.6 cap revision
below). An empty slice admits instantly. The ledger stays as
the estimate source (info + envelope math); v3.1's --no-daemon keeps future
samples clean (no daemon RSS inside the sampled tree) and the 14-day
half-life decays any historical pollution.

Outside guard retired (test-gating-v3.5, 2026-09-29 user ruling "keep the
25G envelope honest, the system side is not our business — the embedding
service's ~8G is fixed overhead"): the v3.2 one system-side guard
``used - slice_current <= MemTotal - cap - 2G`` is retired. On this host
it was pure arithmetic deadlock: MemTotal 40099 - the then-25G cap - 2G
headroom puts the ceiling at ~12451 MiB, while the fixed ungated baseline
(embedding service + resident ZCode sessions + OS) sits around
15069 MiB — the predicate was permanently false and every run queued
forever (five wrappers stuck 2026-09-29 15:09-15:35, gate-queue
outside=20025→23396 vs limit=12452). Admission is envelope-only;
``outside`` survives as an informational figure in journals and
``--dry-run``. Ops can restore a guard by exporting
``GT6_GATE_MEM_LIMIT_MIB`` to a valid integer (an absolute MiB
threshold, opt-in); unset or invalid values keep the guard disabled.

Cap revision (test-gating-v3.6, 2026-09-29 user ruling after the third WSL
crash that day, ~16:3x): ``SLICE_CAP_GIB`` 25 → 22. The host carries
MemTotal 40099 MiB with a ~15 G ungated baseline (embedding service ~8 G +
resident ZCode sessions + OS), so the 25 G cap + baseline ≈ 40 G ≈ taut;
22 G + 15 G = 37 G leaves ~2.5 G of headroom. Value-only change — the
v3.2-v3.5 semantics (envelope-only predicate, retired outside guard,
budgets, slots, watchdog, reaping) are untouched. Earlier paragraphs keep
their historical figures as of their own dates.

Filtered budget 12G (test-gating-v3.7, 2026-09-29): the filtered-test
per-task budget 8G → 12G after six independent multi-class FML junit-boot
domain runs peaked at 8733 / 9329 / 9521 / 9900 / 10800 / 12600 MiB —
systematically over the old 8G budget (watchdog rc=97 or in-envelope memcg
rc=247), forcing manual ``--task-cap 12`` overrides on five+ occasions
(coders and the review seat alike). 12G deliberately equals the full-test
tier: a multi-class filtered domain run on the 21.1 FML boot leg has the
same footprint class as a full suite. The 12.6G outlier still exceeds it —
such runs keep using ``--task-cap 16`` (the knob stays). Value-only change:
admission cold estimates (filtered 6G) are untouched — the ledger feeds the
envelope predicate, not the budget.

Serial filtered runs (test-gating-v3.8, 2026-09-30): filtered-test gradle
runs — any ``--tests`` filter — get ``--max-workers=1`` injected by default.
Root cause (state research.fml-test-memory): the build script's
``maxParallelForks = min(cpu*2, 6)`` saturates six FML-boot test forks
(~1.9G each) ≈ 13G on any ≥6-test-class filter — structurally over the
filtered 12G budget, no code growth involved. With one worker the measured
peak is 3.4G (-75%) for +25% wall clock on the same domain. An explicit
``--max-workers`` in the command wins untouched; full-test, compile,
rundata, rcon and non-gradle commands are not touched. The injection is
journalled (``inject-workers`` line in the gate log).

Per-task budget + script watchdog (test-gating-v3.3, 2026-09-29 sixth
ruling): the ruling holds memory.max cannot be RELIED UPON on this host
(Brokestar kernel, custom reclaim logic), so the script watchdog is the
first enforcer. Empirical note (same-day probe): scope-level MemoryMax IS
enforced here — a 300M scope running a 1G malloc was kernel-OOM-killed
(rc 137, oom_kill counter up); the SLICE level (gt6gate.slice 22G) is
unverified — the destructive probe is deferred to the r8 closeout when the
slice is idle. The original non-enforcement observation is of uncertain
origin (slice level, or pre-v3.3 scopes that never carried a per-task
max). Both layers therefore stay: each task's scope gets
``MemoryMax=<class budget>`` at creation (kernel backstop where enforced)
and the runner's sampling loop doubles as the watchdog — every 2 s tick it
reads the task's own memory.current (same read feeds the ledger peak — one
read, no /proc walk) and, past the per-task budget (TASK_CAP_MIB: full 12G
/ filtered 12G / compile 6G / rundata 8G; ``--task-cap`` overrides), TERMs →
2 s → KILLs the task's own cgroup and exits BUDGET_EXIT (97) so callers can
distinguish "over budget" (watchdog) from an ordinary failure — a kernel
OOM kill surfaces as the usual negative signal code instead. The tick also
sums every sub-cgroup under gt6gate.slice; past the 22G project cap it
kills the LARGEST sub-cgroups first (max reclaim per kill → fewest victims,
fastest return under cap; fresh runs are naturally spared — they are still
small) until back under cap. Kills are per-task cgroups: siblings keep
running.

Hard cap (test-gating-v3c, 2026-09-29 third ruling; 22G since v3.6): the
runner wraps the child in ``systemd-run --user --scope
-p Slice=gt6gate.slice`` so every gated gradle shares one memory envelope —
``systemctl --user set-property gt6gate.slice MemoryMax=22G
MemorySwapMax=4G --runtime`` (22G + the ~15G ungated baseline ≈ 37G on
this 40099 MiB host, ~2.5G headroom; ``--cap``/``--swap`` retune,
defaults 22/4).
The aggregate is naturally bounded; on exhaustion the kernel OOM-kills
inside the slice, never the WSL host. Bootstrap is idempotent and
re-asserted per run; if systemctl/systemd-run are unavailable the gate
degrades to a direct exec with one stderr warning (availability over
enforcement). Side benefit: JVMs with UseContainerSupport read the cgroup
cap and size their default heap accordingly (our explicit gradle heap flags
win; forked unconfigured JVMs benefit).

Residue reaping (test-gating-v3.1, 2026-09-29 fourth ruling): gradle builds
outliving their gate run were the last leak — a leader-exited scope was
found hiding a 2G+ daemon (killing it freed 8G). Three layers:
(1) *root fix* — the runner injects ``--no-daemon`` into every gradle
command (idempotent; ``--keep-daemon`` / env ``GT6_GATE_KEEP_DAEMON=1`` opt
out) so each build owns a single-use daemon that exits with it. Nothing
lingers to reap, and concurrent gates can no longer share one daemon across
cgroups and kill each other's build. Cost: one JVM start per invocation.
(2) *backstop* — the scope gets the deterministic name
``gt6gate-run-<pid>-<ts>``; after the child exits and the ledger is written
the gate TERMs the whole scope cgroup, polls cgroup.procs for
``REAP_GRACE_SECONDS``, SIGKILLs any straggler, and only then frees the
concurrency slot (reap failures warn on stderr, exit-code passthrough is
untouched). (3) *sweeper* — ``reap [--dry-run]`` stops gate-slice scopes
whose leader exited but whose cgroup still holds processes (the crashed-
runner case: MainPID empty/0 + non-empty cgroup.procs).

Usage:
    python3 tools/gt6testgate.py run [--tag <name>] [--role {coder,review}]
                              [--class CLASS] [--cap G] [--swap G]
                              [--keep-daemon] [--dry-run] -- <command...>
    python3 tools/gt6testgate.py reap [--dry-run]
    (the run flags also work without the leading ``run`` — legacy form kept)

The child's stdout/stderr pass through untouched (no capture); the wrapper
exits with the child's exit code.

Import:  sys.path.insert(0, "tools"); import gt6testgate
"""

import argparse
import fcntl
import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

GATE_LOG = Path("/tmp/gt6_gate.log")
SLOT_DIR = Path("/tmp/gt6_gate_slots")
SLOT_STALE_SECONDS = 2 * 3600   # mtime fallback: reaps slots whose name has
                                # no usable PID; PID liveness handles the rest
DEFAULT_MEM_LIMIT_MIB = 30720
MEMINFO_PATH = Path("/proc/meminfo")
POLL_SECONDS = 10.0

# --- v3 predictive admission (ops-testgate-v3-memory-predict) -------------
LEDGER_PATH = Path("/tmp/gt6_testgate_memory_ledger.json")
LEDGER_DECAY_DAYS = 14.0        # half-life: 14-day-old estimates halve
SAMPLE_SECONDS = 2.0            # process-tree RSS sampling cadence
# Conservative cold-start peak-RSS estimates (MiB) used when the ledger has
# no (fresh) record for a class — ops triage 2026-09-29. Any ledger
# observation for the class overrides these (see estimate_for).
COLD_ESTIMATE_MIB = {
    "full-test": 12000,     # unfiltered gradle test suite (heaviest)
    "filtered-test": 6000,  # gradle test --tests FILTER
    "compile": 4000,        # compileJava/compileTestJava/classes/jar
    "rundata": 6000,        # gradle runData datagen
    "rcon-boot": 5000,      # server boot / rcon sweeps (JVM client + server)
    "other": 8000,          # unknown gradle shape (build/check transitively
                            # run tests) and uncategorized commands
}
CLASSES = tuple(COLD_ESTIMATE_MIB)

# --- v3c shared-slice hard cap (test-gating-v3c, 2026-09-29) ---------------
SLICE_NAME = "gt6gate.slice"
SLICE_CAP_GIB = 22              # MemTotal 40099MiB: ~15G ungated baseline
                                # + 22G ≈ 37G, ~2.5G headroom (v3.6, was 25)
SLICE_SWAP_GIB = 4
SLICE_ENV = "GT6_GATE_SLICE"    # "0" disables the wrap entirely

# --- v3.1 residue reaping (test-gating-v3.1, 2026-09-29 fourth ruling) -----
KEEP_DAEMON_ENV = "GT6_GATE_KEEP_DAEMON"   # set (≠"0"/"") to skip injection
REAP_GRACE_SECONDS = 2.0        # TERM → grace → KILL, our own timing
UNIT_PREFIX = "gt6gate-run"     # scope units: gt6gate-run-<pid>-<ts>.scope

# --- v3.2 envelope admission (test-gating-v3.2, 2026-09-29 fifth ruling) ---
# v3.5 (2026-09-29 ruling): the outside guard (MemTotal - cap - 2G) is
# RETIRED by default; env GT6_GATE_MEM_LIMIT_MIB=<MiB> opts back in —
# see outside_limit_mib.

# --- v3.3 per-task budget + script watchdog (2026-09-29 sixth ruling) ------
# This kernel does not enforce memory.max; the watchdog below is the
# enforcer. Budgets = cold estimates × ~1.3 headroom (a run may legitimately
# touch its forecast before it is a runaway); filtered-test is the v3.7
# exception — 12G by measurement (see docstring), level with full-test on
# purpose.
TASK_CAP_MIB = {
    "full-test": 12288,
    "filtered-test": 12288,  # v3.7: was 8192; six-sample peaks 8.7-12.6G
    "compile": 6144,
    "rundata": 8192,
    "rcon-boot": 6144,
    "other": 8192,
}
BUDGET_EXIT = 97                # watchdog kill ≠ ordinary failure

ROLE_ENV = "GT6_TESTGATE_ROLE"
ROLES = ("coder", "review")
FULL_LOCK_PATH = Path("/tmp/gt6_testgate_full.lock")
FULL_DENY_EXIT = 2
# a bare test-suite task under any project path, e.g. :test / :mdk:cleanTest /
# :mdk:1.20.1-forge:test — but NOT compileTestJava / runData / build.
# [\w.-]* allows the leading bare ":" of gradle task paths.
# ponytail: task-name matching only; gradle dep-graph full runs triggered via
# build/check are NOT intercepted (discipline gate, not a security boundary).
_TEST_TASK_RE = re.compile(r"(?:[\w.-]*:)*(?:clean)?[Tt]est")


def mem_limit_mib():
    """Memory gate threshold in MiB (env GT6_GATE_MEM_LIMIT_MIB overridable)."""
    try:
        return int(os.environ.get("GT6_GATE_MEM_LIMIT_MIB", DEFAULT_MEM_LIMIT_MIB))
    except ValueError:
        return DEFAULT_MEM_LIMIT_MIB


def max_concurrent():
    """Max concurrent gated children across ALL worktrees (env overridable)."""
    try:
        return max(1, int(os.environ.get("GT6_GATE_MAX_CONCURRENT", "4")))
    except ValueError:
        return 4


def resolve_role(cli_role=None):
    """coder|review — CLI arg > env GT6_TESTGATE_ROLE > coder.

    Unknown values fall back to coder, i.e. full runs stay denied (fail-closed).
    """
    role = cli_role if cli_role is not None else os.environ.get(ROLE_ENV)
    return role if role in ROLES else ROLES[0]


def _is_gradle_cmd(cmd):
    """True if any argv token is a gradle launcher (exact basename match)."""
    return any(Path(tok).name in ("gradlew", "gradle") for tok in cmd)


def command_mode(cmd):
    """"full" if cmd is a gradle run of an unfiltered test suite, else "other".

    full = some argv token is gradlew/gradle AND a non-flag token is a bare
    ``test``/``cleanTest`` task (any project path) AND no ``--tests`` filter.
    Filtered runs, compileTestJava, runData and non-gradle commands = "other"
    (open to both roles). Wrapping gradle in ``sh -c "..."`` hides the tokens
    and is treated as "other" — the gate is a discipline backstop.
    """
    if not _is_gradle_cmd(cmd):
        return "other"
    if any(tok == "--tests" or tok.startswith("--tests=") for tok in cmd):
        return "other"
    if any(not tok.startswith("-") and _TEST_TASK_RE.fullmatch(tok) for tok in cmd):
        return "full"
    return "other"


_TASK_PATH = r"(?:[\w.-]*:)*"   # leading gradle project path, e.g. :mdk:1.20.1-forge:
_RUN_DATA_RE = re.compile(_TASK_PATH + r"runData")
_COMPILE_TASK_RE = re.compile(_TASK_PATH + r"(?:compile\w*|classes|jar)")


def classify(cmd):
    """Task class for memory estimation, from argv shape (--class overrides).

    gradle:  bare test/cleanTest without --tests → full-test (same rule as
             command_mode); with --tests → filtered-test; runData → rundata;
             compile*/classes/jar → compile; anything else (build/check —
             their dep graph transitively runs the test suite, so keep the
             conservative full-test estimate) → other.
    non-gradle: anything mentioning rcon (tools/rcon/sweep.py, gt6server.py
             boots) → rcon-boot; everything else → other.
    """
    if not _is_gradle_cmd(cmd):
        if any("rcon" in tok.lower() for tok in cmd):
            return "rcon-boot"
        return "other"
    if any(tok == "--tests" or tok.startswith("--tests=") for tok in cmd):
        return "filtered-test"
    if any(not tok.startswith("-") and _TEST_TASK_RE.fullmatch(tok) for tok in cmd):
        return "full-test"
    if any(not tok.startswith("-") and _RUN_DATA_RE.fullmatch(tok) for tok in cmd):
        return "rundata"
    if any(not tok.startswith("-") and _COMPILE_TASK_RE.fullmatch(tok) for tok in cmd):
        return "compile"
    return "other"


def mem_fields_mib(path=MEMINFO_PATH):
    """(MemTotal, MemAvailable) in MiB — kernel reports both in kB."""
    total = avail = None
    with open(path, encoding="ascii") as fh:
        for line in fh:
            if line.startswith("MemTotal:"):
                total = int(line.split()[1])
            elif line.startswith("MemAvailable:"):
                avail = int(line.split()[1])
            if total is not None and avail is not None:
                break
    if total is None or avail is None:
        raise RuntimeError(f"{path} lacks MemTotal/MemAvailable")
    return total // 1024, avail // 1024


def mem_used_mib(path=MEMINFO_PATH):
    """used = MemTotal - MemAvailable in MiB (kernel reports both in kB).

    v3.4: MemAvailable already excludes the reclaimable page cache, so the
    system-side outside guard needs no cache deduction of its own — the
    v3.4 subtraction is cgroup-face only (memory.current/memory.stat).
    """
    total, avail = mem_fields_mib(path)
    return total - avail


def mem_total_mib(path=MEMINFO_PATH):
    """MemTotal in MiB (the outside guard's base, v3.2)."""
    return mem_fields_mib(path)[0]


def cgroup_usage_mib(rel, cgroup_root="/sys/fs/cgroup", uid=None):
    """memory.current (MiB) of a user-session cgroup ``rel``; 0 when absent.

    User cgroups sit at the systemd session layout (verified live
    2026-09-29: scopes resolve to
    /user.slice/user-<uid>.slice/user@<uid>.service/<rel>). A missing file
    means the cgroup is empty or gone — for admission that means "nothing
    inside", so absence reads as 0, not error.
    """
    uid = os.getuid() if uid is None else uid
    path = (Path(cgroup_root) / "user.slice" / f"user-{uid}.slice"
            / f"user@{uid}.service" / rel / "memory.current")
    try:
        return int(path.read_text(encoding="ascii")) // (1024 * 1024)
    except (OSError, ValueError):
        return 0


_RECLAIM_NOTE_DONE = False     # v3.4: the conservative-fallback stderr note fires ONCE


def cgroup_reclaimable_mib(rel, cgroup_root="/sys/fs/cgroup", uid=None):
    """The reclaimable cache slice of one cgroup: memory.stat ``file`` +
    ``slab_reclaimable`` in MiB (v3.4); ``None`` on any parse failure.

    cgroup v2 memory.stat reports in bytes like memory.current. Both keys are
    optional-presence but well-formed when present; a missing/unparsable stat
    file returns None so the caller can subtract nothing (conservative) —
    stderr noted once per process, never per tick (the watchdog reads this
    every SAMPLE_SECONDS; a chatty note would drown real diagnostics).
    """
    global _RECLAIM_NOTE_DONE
    uid = os.getuid() if uid is None else uid
    path = (Path(cgroup_root) / "user.slice" / f"user-{uid}.slice"
            / f"user@{uid}.service" / rel / "memory.stat")
    try:
        file_mib = slab_mib = None
        for line in path.read_text(encoding="ascii").splitlines():
            parts = line.split()
            if len(parts) == 2:
                if parts[0] == "file":
                    file_mib = int(parts[1]) // (1024 * 1024)
                elif parts[0] == "slab_reclaimable":
                    slab_mib = int(parts[1]) // (1024 * 1024)
        if file_mib is None or slab_mib is None:
            raise ValueError("memory.stat lacks file/slab_reclaimable")
        return file_mib + slab_mib
    except (OSError, ValueError):
        if not _RECLAIM_NOTE_DONE:
            _RECLAIM_NOTE_DONE = True
            print("[gt6testgate] memory.stat unreadable — admission/watchdog "
                  "read RAW memory.current (reclaimable cache NOT deducted; "
                  "conservative)", file=sys.stderr)
        return None


def cgroup_effective_usage_mib(rel, cgroup_root="/sys/fs/cgroup", uid=None):
    """v3.4 effective in-group usage = memory.current − reclaimable cache.

    The kernel counts the task's page cache (gradle file IO) in
    memory.current, but that cache is reclaimable under pressure — counting
    it queues admission and fires the watchdog on a "full" cgroup that would
    evaporate on demand (the 2026-09-29 11430-vs-104 pollution class).
    Floor at 0 (a current smaller than its own reclaimable cache is legal).
    """
    cur = cgroup_usage_mib(rel, cgroup_root, uid)
    reclaimable = cgroup_reclaimable_mib(rel, cgroup_root, uid)
    if reclaimable is None:
        return cur
    return max(cur - reclaimable, 0)


def slice_usage_mib(slice_name=SLICE_NAME, cgroup_root="/sys/fs/cgroup",
                    uid=None):
    """Effective memory of the gate slice (v3.2 admission input; v3.4 =
    memory.current minus reclaimable cache — see cgroup_effective_usage_mib)."""
    return cgroup_effective_usage_mib(slice_name, cgroup_root, uid)


def slice_children_usage(slice_name=SLICE_NAME, cgroup_root="/sys/fs/cgroup",
                         uid=None):
    """[(sub-cgroup name, effective usage MiB)] over slice children (v3.3).

    Each child directory of the slice is one task scope (v3.1's deterministic
    --unit names); their sum is the project aggregate the watchdog enforces
    the 22G cap against, and the per-unit own read is the budget comparator.
    v3.4: both faces read EFFECTIVE usage (memory.current minus reclaimable
    cache — the anon view) so a cache-padded cgroup neither fires the budget
    kill nor skews the aggregate; the kernel MemoryMax itself is untouched
    (the kernel reclaims cache before OOM, its semantics need no mirror).
    """
    uid = os.getuid() if uid is None else uid
    base = (Path(cgroup_root) / "user.slice" / f"user-{uid}.slice"
            / f"user@{uid}.service" / slice_name)
    try:
        children = sorted(p for p in base.iterdir() if p.is_dir())
    except OSError:
        return []
    return [(p.name, cgroup_effective_usage_mib(f"{slice_name}/{p.name}",
                                                cgroup_root, uid))
            for p in children]


def watchdog_tick(unit, budget_mib, cap_mib, slice_name=SLICE_NAME,
                  stop_fn=None, cgroup_root="/sys/fs/cgroup", uid=None):
    """One watchdog pass over the project slice (v3.3 core).

    (1) own budget: this task's cgroup over ``budget_mib`` → kill it.
    (2) project cap: the slice's sub-cgroup sum over ``cap_mib`` → kill
        LARGEST donors first until back under cap. Largest-first rationale:
        max reclaim per kill — fewest victims and fastest return under cap;
        a fresh run is naturally spared because it has not ballooned yet.
    Every kill is one task cgroup (TERM→grace→KILL via ``stop_fn``):
    siblings are never touched. Returns (own_mib, kills) with kills = list
    of (kind, victim, mib), kind "task" (budget) or "aggregate" (cap); a
    self-kill shows victim == unit so the caller can exit BUDGET_EXIT.
    """
    if stop_fn is None:
        stop_fn = stop_unit
    children = slice_children_usage(slice_name, cgroup_root, uid)
    own = 0
    total = 0
    for name, usage in children:
        total += usage
        if name == unit:
            own = usage
    kills = []
    if own > budget_mib:
        stop_fn(unit)
        return own, [("task", unit, own)]
    if total > cap_mib:
        for name, usage in sorted(children, key=lambda c: c[1], reverse=True):
            if total <= cap_mib or usage <= 0:
                continue
            stop_fn(name)
            kills.append(("aggregate", name, usage))
            total -= usage
            if name == unit:
                break
    return own, kills


def outside_limit_mib():
    """Outside-pressure guard threshold in MiB, or None = guard retired.

    v3.5 (user ruling 2026-09-29: "cgroup 内部算好 25g 就行，系统的不用管
    了，嵌入服务固定开销是 8g"): the v3.2 formula MemTotal - cap - 2G is
    retired — on this host it computed a ~12451 MiB ceiling against a
    fixed ~15069 MiB ungated baseline, so the predicate was permanently
    false and every run queued forever (pure arithmetic deadlock, five
    wrappers stuck 2026-09-29 15:09-15:35). env GT6_GATE_MEM_LIMIT_MIB
    set to a valid integer = explicit ops opt-in restoring the guard as
    an absolute MiB threshold; unset or invalid values leave it disabled.
    """
    try:
        return int(os.environ.get("GT6_GATE_MEM_LIMIT_MIB", ""))
    except ValueError:
        return None


def wait_admission(estimate_mib, cap_gib=SLICE_CAP_GIB, tag="-",
                   log_file=GATE_LOG, log=None, poll=POLL_SECONDS,
                   read_used=None, read_slice=None):
    """v3.2 admission, envelope-only since v3.5: the envelope is the math.

    envelope: slice_current + estimate ≤ cap — concurrent gated runs share
              the slice's MemoryMax; overflowing it means in-group OOM
              churn, so queue. Empty slice admits instantly.
    outside:  INFORMATIONAL since v3.5 — the blocking guard
              (used - slice_current ≤ MemTotal - cap - 2G) was pure
              arithmetic deadlock on this host (fixed ungated baseline
              ~15069 MiB above the ~12451 MiB ceiling, forever false;
              user ruling "system side is not our business"), so it only
              survives as a journal figure on admit. Ops can restore the
              guard via env GT6_GATE_MEM_LIMIT_MIB (see outside_limit_mib).
    The v3a system-side formula (used + estimate ≤ 30G) is retired: it
    double-counted this run's own envelope usage and starved big estimates
    (review seat blocked an hour on a polluted 11.6G figure). Readers are
    injectable for tests. Journals gate-queue once per block and gate-admit
    on pass.
    """
    if read_used is None:
        read_used = mem_used_mib
    if read_slice is None:
        read_slice = slice_usage_mib
    cap_mib = cap_gib * 1024
    limit = outside_limit_mib()
    started = time.monotonic()
    queued = False
    while True:
        slice_cur = read_slice()
        used = read_used()
        envelope = slice_cur + estimate_mib
        outside = used - slice_cur
        if envelope <= cap_mib and (limit is None or outside <= limit):
            break
        if limit is not None and outside > limit:
            reason = (f"outside used={used}MiB slice={slice_cur}MiB "
                      f"outside={outside}MiB limit={limit}MiB")
        else:
            reason = (f"envelope slice={slice_cur}MiB est={estimate_mib}MiB "
                      f"predicted={envelope}MiB cap={cap_mib}MiB")
        if not queued:
            queued = True
            _journal("gate-queue", tag, reason, log_file)
        if log:
            log(f"[gt6testgate] {reason} — queueing every {poll:.0f}s")
        time.sleep(poll)
    wait = time.monotonic() - started
    _journal("gate-admit", tag,
             f"slice={slice_cur}MiB est={estimate_mib}MiB "
             f"outside={outside}MiB queued={wait:.0f}s", log_file)
    if log and queued:
        log(f"[gt6testgate] admission passed after {wait:.0f}s "
            f"(slice {slice_cur} + estimate {estimate_mib} <= cap; "
            f"outside {outside})")


def load_ledger(path=LEDGER_PATH):
    """class → {"estimate": MiB, "updated": epoch, ...}; corrupt/missing → {}.

    A corrupt file (truncated write, hand edit) rebuilds from empty — the
    ledger is a cache of observations, cold defaults cover the gap.
    """
    try:
        with open(path, encoding="utf-8") as fh:
            data = json.load(fh)
    except (OSError, ValueError):
        return {}
    if not isinstance(data, dict):
        return {}
    return {k: v for k, v in data.items()
            if isinstance(v, dict)
            and isinstance(v.get("estimate"), (int, float))
            and isinstance(v.get("updated"), (int, float))}


def _decay(estimate_mib, updated_epoch, now):
    """14-day half-life: stale estimates halve per LEDGER_DECAY_DAYS."""
    age_days = max(0.0, now - updated_epoch) / 86400.0
    return estimate_mib * 0.5 ** (age_days / LEDGER_DECAY_DAYS)


def estimate_for(cls, ledger, now=None):
    """Predicted peak RSS in MiB: max(cold default, decayed ledger record).

    The cold default floors the decay so a stale ledger can only relax the
    prediction down to the class's conservative prior, never below it.
    Returns whole MiB — decay is a days-scale factor, sub-second noise in
    the exponent would otherwise leak float dust into predictions.
    """
    now = time.time() if now is None else now
    estimate = COLD_ESTIMATE_MIB[cls]
    entry = ledger.get(cls)
    if isinstance(entry, dict) and isinstance(entry.get("updated"), (int, float)):
        estimate = max(estimate, _decay(entry["estimate"], entry["updated"], now))
    return int(estimate + 0.5)


def record_observation(cls, observed_mib, path=LEDGER_PATH, now=None):
    """Fold a sampled peak into the ledger: estimate = max(decayed, observed).

    flock'd read-modify-write — /tmp ledger is shared by all worktrees and
    concurrent gated runs. Atomic replace so a crash never truncates it.
    Returns the stored estimate.
    """
    now = time.time() if now is None else now
    path = Path(path)
    with open(f"{path}.lock", "a+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        try:
            ledger = load_ledger(path)
            entry = ledger.get(cls)
            old = entry.get("estimate", 0) if isinstance(entry, dict) else 0
            updated = entry.get("updated", now) if isinstance(entry, dict) else now
            estimate = round(max(_decay(old, updated, now), observed_mib), 1)
            ledger[cls] = {"estimate": estimate, "updated": now,
                           "last_peak": observed_mib}
            tmp = f"{path}.{os.getpid()}.tmp"
            with open(tmp, "w", encoding="utf-8") as fh:
                json.dump(ledger, fh)
            os.replace(tmp, path)
            return estimate
        finally:
            fcntl.flock(lock, fcntl.LOCK_UN)


def tree_rss_mib(root_pid, proc_dir="/proc"):
    """Sum RSS over the process tree rooted at root_pid, in MiB.

    v3.4 ledger note: this UNWRAPPED-mode peak source is process RSS —
    statm RSS counts anonymous + mapped file pages resident, NOT the
    writeback/streaming page cache the cgroup's memory.current carries, so
    the ledger's peak column mixes two views (wrapped runs feed the
    effective/anon-ish cgroup read, unwrapped runs this RSS walk); the
    estimate only needs cross-run comparability per mode, which holds.

    ponytail: ppid-chain walk + statm RSS double-counts shared pages (gradle
    daemon + workers share the JVM) and a detached daemon escapes the tree —
    both errors are consistent in direction, which is all an estimate needs;
    switch to cgroup memory.peak per-task only if samples prove too low.
    """
    page = os.sysconf("SC_PAGE_SIZE")
    ppid, rss = {}, {}
    try:
        entries = os.listdir(proc_dir)
    except OSError:
        return 0
    for name in entries:
        if not name.isdigit():
            continue
        try:
            with open(f"{proc_dir}/{name}/stat", encoding="utf-8",
                      errors="replace") as fh:
                text = fh.read()
            # after the final ')' fields start at state; ppid is the next one
            fields = text[text.rfind(")") + 1:].split()
            ppid[int(name)] = int(fields[1])
            with open(f"{proc_dir}/{name}/statm", encoding="ascii") as fh:
                rss[int(name)] = int(fh.read().split()[1]) * page
        except (OSError, ValueError, IndexError):
            continue    # process died mid-read — skip it
    total, seen, stack = 0, set(), [root_pid]
    while stack:
        pid = stack.pop()
        if pid in seen:
            continue
        seen.add(pid)
        total += rss.get(pid, 0)
        stack.extend(k for k, p in ppid.items() if p == pid)
    return total // (1024 * 1024)


def slice_enabled(env=None):
    """GT6_GATE_SLICE: "0" = off; anything else (incl. unset) = on.

    "On" still degrades at pre-flight if systemctl/systemd-run fail — this
    only controls whether the wrap is attempted.
    """
    return (env if env is not None else os.environ).get(SLICE_ENV) != "0"


def slice_wrap(cmd, slice_name=SLICE_NAME, unit=None, memory_max_mib=None):
    """``cmd`` → systemd-run scope argv inside the shared gate slice.

    --scope runs the command in-line (no service-manager round trip), so
    stdio passthrough and exit-code forwarding are preserved. ``unit`` pins
    a deterministic scope name (v3.1) so the post-run reap can address
    exactly this run's cgroup instead of an anonymous one. ``memory_max_mib``
    (v3.3) writes the per-task budget onto the scope AT CREATION — no
    set-property race; advisory on the Brokestar kernel, real elsewhere.
    """
    argv = ["systemd-run", "--user", "--scope"]
    if unit:
        argv.append(f"--unit={unit}")
    if memory_max_mib:
        argv += ["-p", f"MemoryMax={memory_max_mib}M"]
    return argv + ["-p", f"Slice={slice_name}", "--"] + list(cmd)


def slice_bootstrap(cap_gib=SLICE_CAP_GIB, swap_gib=SLICE_SWAP_GIB, run=None):
    """(Re-)assert runtime caps on the shared gate slice; True = usable.

    set-property is idempotent — same values, no state change — so calling
    before every run is cheap insurance against a rebooted/reseeded systemd
    user instance. ``run`` is injectable for tests.
    """
    if run is None:
        def run(argv):
            return subprocess.run(argv, capture_output=True,
                                  timeout=15).returncode
    return run(["systemctl", "--user", "set-property", SLICE_NAME,
                f"MemoryMax={cap_gib}G", f"MemorySwapMax={swap_gib}G",
                "--runtime"]) == 0


# --- v3.1 residue reaping ---------------------------------------------------

def inject_no_daemon(cmd, keep_daemon=False, env=None):
    """Append ``--no-daemon`` to gradle commands; every other command as-is.

    v3.1 root fix for the daemon leak: --no-daemon gives each build its own
    single-use daemon that exits with the build, so nothing lingers in the
    run's scope cgroup — and concurrent gates can no longer share (and
    post-run-kill each other's) daemons across cgroups. Cost: one JVM start
    per invocation. Idempotent; an explicit ``--daemon`` wins untouched (no
    contradictory flags); interactive hot-daemon workflows opt out via
    ``keep_daemon`` (CLI --keep-daemon) or env GT6_GATE_KEEP_DAEMON (any
    non-empty value but "0"). CI passthrough never reaches this (the runner
    returns before injecting).
    """
    if keep_daemon or (env if env is not None else os.environ).get(
            KEEP_DAEMON_ENV) not in (None, "", "0"):
        return cmd
    if not _is_gradle_cmd(cmd):
        return cmd
    if any(tok in ("--no-daemon", "--daemon") for tok in cmd):
        return cmd
    return cmd + ["--no-daemon"]


def inject_max_workers(cmd):
    """Append ``--max-workers=1`` to filtered-test gradle runs; else as-is.

    v3.8 serial default (root cause in state research.fml-test-memory,
    2026-09-30): maxParallelForks=min(cpu*2, 6) (build.neoforge.gradle.kts)
    saturates six FML-boot test forks ~1.9G each ≈ 13G on any ≥6-test-class
    ``--tests`` filter — structurally over the filtered 12G budget.
    --max-workers=1 clamps fork concurrency: measured peak 13.3G → 3.4G
    (-75%) for +25% wall clock. Reuses classify() — the same "filtered-test"
    verdict that picks the budget picks this injection, so the two stay in
    lockstep. An explicit ``--max-workers`` (either ``=N`` or space form)
    wins untouched, like inject_no_daemon's --daemon respect; non-filtered
    and non-gradle commands pass through unchanged.
    """
    if classify(cmd) != "filtered-test":
        return cmd
    if any(tok == "--max-workers" or tok.startswith("--max-workers=")
           for tok in cmd):
        return cmd
    return cmd + ["--max-workers=1"]


def unit_name():
    """Deterministic per-run scope name — addressable for the post-run reap.

    Full name WITH the .scope suffix: systemd-run uses a suffixed --unit
    verbatim, while a bare name would come back as <name>.scope but every
    later systemctl show would look up <name>.service (its own default
    suffix) and miss. One spelling everywhere.
    """
    return f"{UNIT_PREFIX}-{os.getpid()}-{time.monotonic_ns()}.scope"


def unit_property(unit, prop, run=None):
    """``systemctl show -p PROP --value`` → stripped str ("" on any failure).

    A leader-exited scope reports MainPID as an EMPTY value, not "0" — both
    are leader-gone for the callers.
    """
    if run is None:
        def run(argv):
            return subprocess.run(argv, capture_output=True, text=True,
                                  timeout=15)
    try:
        proc = run(["systemctl", "--user", "show", unit, "-p", prop,
                    "--value"])
    except (OSError, subprocess.SubprocessError):
        return ""
    if proc is None or proc.returncode != 0:
        return ""
    return (proc.stdout or "").strip()


def unit_pids(unit, run=None, cgroup_root="/sys/fs/cgroup"):
    """PIDs still alive in the unit's cgroup ([] = empty or released).

    Reads the cgroup.procs map the ControlGroup property points at; a gone
    directory means systemd already released the scope — same as empty.
    """
    cg = unit_property(unit, "ControlGroup", run)
    if not cg:
        return []
    try:
        with open(f"{cgroup_root}/{cg}/cgroup.procs", encoding="ascii") as fh:
            return [int(x) for x in fh.read().split()]
    except (OSError, ValueError):
        return []


def stop_unit(unit, grace=REAP_GRACE_SECONDS, run=None, log=None,
              sleep=time.sleep, pids_fn=unit_pids):
    """TERM the unit's whole cgroup → ≤grace s → SIGKILL any straggler.

    v3.1 backstop: with the --no-daemon injection this is normally a no-op
    (one show + one procs read), but a forked JVM ignoring TERM still dies
    here before the concurrency slot is released. ``kill --signal`` instead
    of ``stop``: stop blocks on TERM-immune processes until systemd's own
    ~90 s timeout, which would defeat the 2 s grace; kill is the same signal
    to the same group, async (verified live 2026-09-29, systemd 261). Once
    the last process is gone the leader-less scope deactivates by itself.
    Never raises — a failed reap warns and lets the child's exit code pass
    through untouched.
    """
    if run is None:
        def run(argv):
            return subprocess.run(argv, capture_output=True, text=True,
                                  timeout=15)
    try:
        run(["systemctl", "--user", "kill", "--signal=SIGTERM", unit])
        deadline = time.monotonic() + grace
        while time.monotonic() < deadline and pids_fn(unit, run):
            sleep(0.05)
        if not pids_fn(unit, run):
            return True
        run(["systemctl", "--user", "kill", "--signal=SIGKILL", unit])
        return not pids_fn(unit, run)
    except (OSError, subprocess.SubprocessError) as exc:
        if log:
            log(f"[gt6testgate] cgroup reap of {unit} failed ({exc}) — "
                f"leftovers may linger; `gt6testgate reap` sweeps them later")
        return False


def reap_scopes(slice_name=SLICE_NAME, run=None, dry_run=False, log=print,
                grace=REAP_GRACE_SECONDS, pids_fn=unit_pids,
                stop_fn=stop_unit):
    """Sweep gate-slice scopes whose leader exited but that still hold PIDs.

    The field case (2026-09-29): a scope whose leader was long gone hiding a
    2G+ daemon. Candidate = Slice matches AND MainPID 0/empty (leader gone)
    AND cgroup.procs non-empty. Active scopes are never touched; empty
    shells are skipped. --dry-run only lists. Returns the reaped unit names
    (dry run: the would-be list).
    """
    if run is None:
        def run(argv):
            return subprocess.run(argv, capture_output=True, text=True,
                                  timeout=15)
    try:
        listing = run(["systemctl", "--user", "list-units", "--all",
                       "--type=scope", "--plain", "--no-legend"])
        if listing.returncode != 0:
            raise RuntimeError((listing.stderr or "").strip()
                               or "list-units failed")
        units = [line.split()[0]
                 for line in (listing.stdout or "").splitlines() if line.strip()]
    except (OSError, subprocess.SubprocessError) as exc:
        log(f"[gt6testgate] reap unavailable ({exc})")
        return []
    reaped = []
    for unit in units:
        if unit_property(unit, "Slice", run) != slice_name:
            continue
        try:
            main_pid = int(unit_property(unit, "MainPID", run) or 0)
        except ValueError:
            main_pid = 0
        if main_pid:
            continue
        pids = pids_fn(unit, run)
        if not pids:
            continue
        if dry_run:
            log(f"[gt6testgate] would reap {unit} (leader gone, "
                f"{len(pids)} process(es) left)")
            reaped.append(unit)
            continue
        log(f"[gt6testgate] reaping {unit} (leader exited, "
            f"{len(pids)} process(es) left)")
        if stop_fn(unit, grace=grace, run=run, log=log):
            reaped.append(unit)
        else:
            log(f"[gt6testgate] {unit} survived the reap — inspect manually")
    return reaped


def _journal(event, tag, detail, log_file=GATE_LOG):
    """Append one timestamped gate event; best-effort, never fails the workload."""
    try:
        with open(log_file, "a", encoding="utf-8") as fh:
            fh.write(f"{time.strftime('%Y-%m-%dT%H:%M:%S')} {event} tag={tag} {detail}\n")
    except OSError:
        pass


def wait_memory(read_used=None, limit_mib=None, poll=POLL_SECONDS,
                tag="-", log_file=GATE_LOG, log=None, estimate_mib=0):
    """Block until used + estimate <= limit; journal every queue/admission.

    Legacy reactive gate — since v3.2 only the gt6server boot path lives
    here (its slot semaphore stacks on top; estimate defaults 0, signature
    unchanged). The gate runner's own admission is :func:`wait_admission`
    (envelope arithmetic). ``read_used`` is injectable for tests.
    Returns (used_mib_at_admission, queued_seconds).
    """
    if read_used is None:
        read_used = mem_used_mib
    if limit_mib is None:
        limit_mib = mem_limit_mib()
    started = time.monotonic()
    used = read_used()
    if used + estimate_mib > limit_mib:
        _journal("mem-queue", tag,
                 f"used={used}MiB limit={limit_mib}MiB est={estimate_mib}MiB "
                 f"predicted={used + estimate_mib}MiB", log_file)
        if log:
            log(f"[gt6testgate] memory used {used} + estimate {estimate_mib} = "
                f"{used + estimate_mib} MiB > limit {limit_mib} MiB — "
                f"queueing every {poll:.0f}s (cap: env GT6_GATE_MEM_LIMIT_MIB)")
        while True:
            time.sleep(poll)
            used = read_used()
            if used + estimate_mib <= limit_mib:
                break
    queued = time.monotonic() - started
    _journal("mem-admit", tag,
             f"used={used}MiB queued={queued:.0f}s est={estimate_mib}MiB",
             log_file)
    if log and queued >= 1.0:
        log(f"[gt6testgate] memory gate passed after {queued:.0f}s "
            f"(used {used} + estimate {estimate_mib} = "
            f"{used + estimate_mib} MiB)")
    return used, queued


def holder_stale_reason(pid):
    """Why ``pid`` can no longer be a legitimate gate slot holder, or None
    while it plausibly still is one (never reap those — long gate runs are
    legal). ``/proc/<pid>`` gone → dead; a live cmdline naming neither the
    gate nor a jvm means the PID was reused and the slot is leaked
    (gate-slot-pid-reap, 2026-10-03)."""
    try:
        raw = Path(f"/proc/{pid}/cmdline").read_bytes()
    except OSError:
        return "pid-dead"
    if not raw:
        return "pid-dead"  # zombie: exited, not yet wait()ed
    argv = raw.replace(b"\0", b" ")
    if b"gt6testgate" in argv or b"gradlew" in argv or b"java" in argv:
        return None
    return "pid-reused"


def reap_stale_slots(slot_dir=SLOT_DIR, log=None, log_file=GATE_LOG):
    """Free slots whose holder is provably gone: PID dead, or PID reused by
    a non-gate process. The 2 h mtime age only covers slots whose name
    carries no usable PID. Live gate callers are never reaped."""
    now = time.time()
    for slot in Path(slot_dir).glob("slot.*"):
        try:
            try:
                pid = int(slot.name.split(".")[1])
            except (IndexError, ValueError):
                pid = None  # malformed name — mtime fallback decides
            if pid is not None:
                reason = holder_stale_reason(pid)
                if reason is None:
                    continue  # live gate caller — red line, never reap
            elif now - slot.stat().st_mtime <= SLOT_STALE_SECONDS:
                continue
            else:
                reason = "mtime-age"
            slot.unlink()
            _journal("slot-reap", slot.name, f"reason={reason}", log_file)
            if log:
                log(f"[gt6testgate] reaped stale gate slot {slot.name} ({reason})")
        except OSError:
            pass  # raced with another reaper/owner


def acquire_slot(poll=POLL_SECONDS, slot_dir=SLOT_DIR, tag="-",
                 log_file=GATE_LOG, log=None):
    """Claim one global test slot (atomic O_EXCL file), queueing while full.

    /tmp namespace — binds across all worktrees by construction. Returns the
    slot path; pair with :func:`release_slot`.
    """
    slot_dir = Path(slot_dir)
    slot_dir.mkdir(exist_ok=True)
    limit = max_concurrent()
    started = time.monotonic()
    queued = False
    while True:
        reap_stale_slots(slot_dir, log, log_file)
        live = sorted(slot_dir.glob("slot.*"))
        if len(live) < limit:
            candidate = slot_dir / f"slot.{os.getpid()}.{time.monotonic_ns()}"
            try:
                fd = os.open(candidate, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                os.write(fd, f"{time.time()} {tag}\n".encode())
                os.close(fd)
            except FileExistsError:
                continue
            waited = time.monotonic() - started
            _journal("slot-admit", tag,
                     f"live={len(live) + 1}/{limit} queued={waited:.0f}s", log_file)
            if log and queued:
                log(f"[gt6testgate] test slot acquired after {waited:.0f}s")
            return candidate
        if not queued:
            queued = True
            _journal("slot-queue", tag, f"full {len(live)}/{limit}", log_file)
            if log:
                log(f"[gt6testgate] test slots full ({len(live)}/{limit}) — "
                    f"queueing every {poll:.0f}s (cap: env GT6_GATE_MAX_CONCURRENT)")
        time.sleep(poll)


def release_slot(slot, log=None):
    try:
        Path(slot).unlink(missing_ok=True)
    except OSError as exc:
        if log:
            log(f"[gt6testgate] slot release failed ({exc}) — stale reaper will collect it")


def acquire_full_lock(path=FULL_LOCK_PATH, tag="-", log_file=GATE_LOG, log=print):
    """Machine-wide exclusive lock around full test runs (fcntl.flock).

    Non-blocking attempt first; on contention print a queueing notice and block
    on LOCK_EX. The kernel drops the lock if the holder dies, so unlike slots
    there is nothing to reap. Returns the open lock file handle; pair with
    :func:`release_full_lock`. /tmp namespace binds all worktrees.
    """
    fh = open(path, "a+")
    try:
        try:
            fcntl.flock(fh, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            _journal("full-queue", tag, f"lock={path}", log_file)
            if log:
                log(f"[gt6testgate] another full test run holds {path} — "
                    f"queueing until it finishes")
            fcntl.flock(fh, fcntl.LOCK_EX)
        _journal("full-admit", tag, f"lock={path}", log_file)
        return fh
    except BaseException:
        fh.close()
        raise


def release_full_lock(fh, log=None):
    if fh is None:
        return
    try:
        fcntl.flock(fh, fcntl.LOCK_UN)
    except OSError as exc:
        if log:
            log(f"[gt6testgate] full lock release failed ({exc})")
    finally:
        fh.close()


def run_gated(cmd, tag=None, poll=POLL_SECONDS, slot_dir=SLOT_DIR,
              log_file=GATE_LOG, log=print, role=None,
              full_lock=FULL_LOCK_PATH, cls=None, ledger_path=None,
              cap_gib=SLICE_CAP_GIB, swap_gib=SLICE_SWAP_GIB,
              slice_bootstrap_fn=slice_bootstrap, keep_daemon=False,
              stop_fn=stop_unit, task_cap_gib=None, watchdog_fn=watchdog_tick):
    """Gated runner: role gate → (full) global lock → envelope admission →
    test slot → (slice wrap) → exec child under the v3.3 watchdog → ledger →
    reap the scope cgroup → free the slot.

    The child inherits our stdio directly (no capture, no reinterpretation);
    lock/slot are released in finallys and the child's exit code returned.
    A full run by a non-review role is denied immediately — before any
    queueing — returning FULL_DENY_EXIT with guidance. v3.2: admission is
    envelope arithmetic (wait_admission). v3c: the child runs inside the
    shared gt6gate.slice memory envelope when systemd agrees; otherwise a
    one-line stderr warning and a direct exec. v3.1: gradle commands get
    ``--no-daemon`` (root fix; keep_daemon opts out) and the named scope
    cgroup is TERM→grace→KILL-reaped after the ledger write, before the slot
    frees. v3.3: the scope carries a per-task MemoryMax budget at creation
    (TASK_CAP_MIB[cls] or task_cap_gib) and the sampling loop doubles as the
    enforcing watchdog — own budget over → kill own cgroup, exit
    BUDGET_EXIT; project aggregate over cap → kill largest sub-cgroups
    (siblings spared). One cgroup read per tick feeds the ledger peak and
    both checks; without a scope the legacy /proc tree walk stays the ledger
    source. v3.8: filtered-test commands further get ``--max-workers=1``
    (serial default, journalled; an explicit --max-workers wins).
    GITHUB_ACTIONS set → zero-gate passthrough (no injection, no
    watchdog).
    """
    if ledger_path is None:
        ledger_path = LEDGER_PATH
    if not cmd:
        raise ValueError("empty command")
    if tag is None:
        tag = Path(cmd[0]).name
    if os.environ.get("GITHUB_ACTIONS"):
        _journal("ci-passthrough", tag, "GITHUB_ACTIONS set — gates skipped",
                 log_file)
        return subprocess.run(cmd).returncode
    # v3.1 root fix — after the CI branch so CI keeps its managed daemons.
    cmd = inject_no_daemon(cmd, keep_daemon=keep_daemon)
    # v3.8 serial filtered default — same CI-branch placement (CI keeps its
    # parallelism); the journal line is the audit trail for "why so serial".
    serial = inject_max_workers(cmd)
    if serial != cmd:
        _journal("inject-workers", tag,
                 "injecting --max-workers=1 "
                 "(filtered-test serial default, v3.8)", log_file)
    cmd = serial
    mode = command_mode(cmd)
    cls = cls or classify(cmd)
    role = resolve_role(role)
    budget_mib = (task_cap_gib * 1024) if task_cap_gib else TASK_CAP_MIB[cls]
    if mode == "full" and role != "review":
        _journal("full-reject", tag, f"role={role}", log_file)
        log("[gt6testgate] FULL test run denied (role=coder) — "
            "全量归审查席，请用 --tests 过滤模式 "
            "(full runs are review-seat only; rerun with --tests FILTER, or "
            "ask the review seat to replay)")
        return FULL_DENY_EXIT
    estimate = estimate_for(cls, load_ledger(ledger_path))
    lock_fh = None
    try:
        if mode == "full":
            lock_fh = acquire_full_lock(full_lock, tag=tag, log_file=log_file,
                                        log=log)
        # v3.2 envelope arithmetic (slice + estimate ≤ cap); the outside
        # guard retired with v3.5 (informational only — see outside_limit_mib).
        wait_admission(estimate, cap_gib=cap_gib, tag=tag, log_file=log_file,
                       log=log, poll=poll)
        slot = acquire_slot(poll=poll, slot_dir=slot_dir, tag=tag,
                            log_file=log_file, log=log)
        # v3c shared-slice hard cap. Pre-flight = the degrade gate; a runtime
        # scope-creation failure surfaces as a plain nonzero passthrough and
        # the next run's pre-flight (same failing environment) degrades then.
        wrapped = False
        unit = None
        if slice_enabled():
            try:
                usable = slice_bootstrap_fn(cap_gib, swap_gib)
            except (OSError, subprocess.SubprocessError):
                usable = False
            if usable and shutil.which("systemd-run"):
                wrapped = True
                unit = unit_name()      # v3.1: deterministic, reap-addressable
            else:
                print("[gt6testgate] systemd slice cap unavailable — running "
                      "UNCAPPED (degraded; export GT6_GATE_SLICE=0 to silence)",
                      file=sys.stderr)
        proc = subprocess.Popen(
            slice_wrap(cmd, unit=unit, memory_max_mib=budget_mib)
            if wrapped else cmd)
        peak = 0
        self_killed = False
        try:
            while True:
                if wrapped:
                    # v3.3 watchdog — one cgroup read feeds the ledger peak
                    # and both limit checks (own budget, project aggregate).
                    own, kills = watchdog_fn(unit, budget_mib,
                                             cap_gib * 1024, stop_fn=stop_fn)
                    peak = max(peak, own)
                    for kind, victim, mib in kills:
                        if kind == "task":
                            print(f"[gt6testgate] task exceeded {mib} MiB "
                                  f"budget ({budget_mib} MiB, class {cls}) "
                                  f"— cgroup killed", file=sys.stderr)
                            _journal("budget-kill", tag,
                                     f"unit={unit} mib={mib}", log_file)
                        elif victim == unit:
                            print(f"[gt6testgate] aggregate over {cap_gib}G "
                                  f"cap — killed own cgroup ({mib} MiB)",
                                  file=sys.stderr)
                            _journal("aggregate-kill", tag,
                                     f"unit={unit} self mib={mib}", log_file)
                        else:
                            print(f"[gt6testgate] aggregate over {cap_gib}G "
                                  f"cap — killed {victim} (largest, "
                                  f"{mib} MiB)", file=sys.stderr)
                            _journal("aggregate-kill", tag,
                                     f"victim={victim} mib={mib}", log_file)
                        if victim == unit:
                            self_killed = True
                else:
                    peak = max(peak, tree_rss_mib(proc.pid))
                if proc.poll() is not None:
                    break
                time.sleep(SAMPLE_SECONDS)
            # v3.3: a watchdog kill is not an ordinary failure — dedicated
            # exit code so callers/CI can tell them apart.
            return BUDGET_EXIT if self_killed else proc.returncode
        finally:
            # v3.1 kill order per ruling: ledger write → reap cgroup →
            # release slot. Reap failures warn only (stop_unit never raises).
            stored = record_observation(cls, peak, ledger_path)
            _journal("peak-sample", tag,
                     f"class={cls} peak={peak}MiB estimate={stored}MiB",
                     log_file)
            if wrapped:
                stop_fn(unit, log=log)
            release_slot(slot, log=log)
    finally:
        release_full_lock(lock_fh, log=log)


def dry_run(cmd, cls=None, ledger_path=None, role=None, cap_gib=SLICE_CAP_GIB,
            log=print, read_used=None, read_slice=None, now=None):
    """Predict-only report on v3.5 envelope arithmetic.

    slice current / estimate / envelope vs cap / outside (informational) /
    decision. The decision is envelope-only since v3.5 (the outside guard
    is retired; the outside line stays as a diagnostic figure). Starts
    nothing, journals nothing — for subagents to self-check before
    dispatching and for the coordinator to schedule against. Returns the
    decision word (ADMIT / QUEUE / REJECT).
    """
    if ledger_path is None:
        ledger_path = LEDGER_PATH
    cls = cls or classify(cmd)
    role = resolve_role(role)
    mode = command_mode(cmd)
    ledger = load_ledger(ledger_path)
    entry = ledger.get(cls)
    estimate = estimate_for(cls, ledger, now=now)
    slice_cur = (read_slice or slice_usage_mib)()
    used = (read_used or mem_used_mib)()
    cap_mib = cap_gib * 1024
    envelope = slice_cur + estimate
    outside = used - slice_cur
    limit = outside_limit_mib()
    source = "ledger" if isinstance(entry, dict) else "cold default"
    if mode == "full" and role != "review":
        decision = f"REJECT exit {FULL_DENY_EXIT} (full test run is review-seat only)"
    elif envelope > cap_mib:
        decision = (f"QUEUE (envelope slice {slice_cur} + estimate "
                    f"{estimate:.0f} = {envelope} MiB > cap {cap_mib} MiB — "
                    f"in-slice OOM churn risk)")
    else:
        decision = "ADMIT (would run now)"
    log(f"[gt6testgate] dry-run: class={cls} role={role} mode={mode}")
    log(f"  slice current:  {slice_cur} MiB")
    log(f"  estimate:       {estimate:.0f} MiB ({source}, class {cls})")
    log(f"  envelope:       slice {slice_cur} + estimate {estimate:.0f} = "
        f"{envelope} MiB (cap {cap_mib} MiB)")
    if limit is None:
        log(f"  outside:        used {used} - slice {slice_cur} = {outside} MiB "
            f"(informational — guard retired v3.5; env "
            f"GT6_GATE_MEM_LIMIT_MIB opts in)")
    else:
        log(f"  outside:        used {used} - slice {slice_cur} = {outside} MiB "
            f"(informational; opt-in guard limit {limit} MiB)")
    log(f"  decision: {decision}")
    return decision.split(" ")[0]


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    # `run` subcommand (v3): the gate IS the runner. Same machinery as the
    # legacy flag form — strip the token only when a `--` follows, so a bare
    # command literally named "run" keeps working.
    if argv[:1] == ["run"] and "--" in argv[1:]:
        argv = argv[1:]
    # `reap` subcommand (v3.1): sweep leader-exited gate scopes that still
    # hold processes. Ops command — no admission machinery involved.
    if argv[:1] == ["reap"]:
        rp = argparse.ArgumentParser(
            prog="gt6testgate reap",
            description="Sweep gt6gate.slice scopes whose leader exited but "
                        "whose cgroup still holds processes (hidden "
                        "daemons from crashed runners).")
        rp.add_argument("--dry-run", action="store_true",
                        help="list reaping candidates without stopping them")
        rargs = rp.parse_args(argv[1:])
        reap_scopes(dry_run=rargs.dry_run)
        return 0
    parser = argparse.ArgumentParser(
        prog="gt6testgate",
        description="GT6 unified heavy-operation gate and runner: predictive "
                    "memory cap + concurrency slots + peak-RSS ledger around "
                    "any command (gradle, sweeps, chain runs).")
    parser.add_argument("--tag", default=None,
                        help="journal tag for /tmp/gt6_gate.log "
                             "(default: the command's basename)")
    parser.add_argument("--role", choices=ROLES, default=None,
                        help="caller role; full (unfiltered test/cleanTest) "
                             "runs are review-seat only (default: coder, or "
                             f"env {ROLE_ENV})")
    parser.add_argument("--class", dest="task_class", choices=CLASSES,
                        default=None,
                        help="task class for the memory estimate "
                             "(default: inferred from the command shape)")
    parser.add_argument("--dry-run", action="store_true",
                        help="print used/estimate/predicted/decision and "
                             "start nothing")
    parser.add_argument("--keep-daemon", action="store_true",
                        help="skip the --no-daemon injection (interactive "
                             "hot-daemon workflows; also env "
                             f"{KEEP_DAEMON_ENV}=1)")
    parser.add_argument("--cap", type=int, default=SLICE_CAP_GIB,
                        help="shared gt6gate.slice MemoryMax in GiB "
                             "(default: 22 — ~2.5G headroom over the ~15G "
                             "ungated baseline on this 40099MiB host)")
    parser.add_argument("--swap", type=int, default=SLICE_SWAP_GIB,
                        help="shared gt6gate.slice MemorySwapMax in GiB "
                             "(default: 4)")
    parser.add_argument("--task-cap", type=int, default=None, metavar="G",
                        help="per-task memory budget in GiB, written on this "
                             "run's scope and enforced by the watchdog "
                             "(default: class table, e.g. full-test 12G)")
    parser.add_argument("cmd", nargs=argparse.REMAINDER, metavar="CMD...",
                        help="command to run, after --")
    args = parser.parse_args(argv)
    cmd = args.cmd[1:] if args.cmd[:1] == ["--"] else args.cmd
    if not cmd:
        parser.error("no command — usage: gt6testgate run "
                     "[--tag NAME] [--role ROLE] [--class CLASS] [--dry-run] "
                     "-- COMMAND...")
    if args.dry_run:
        dry_run(cmd, cls=args.task_class, role=args.role, cap_gib=args.cap)
        return 0
    return run_gated(cmd, tag=args.tag, role=args.role, cls=args.task_class,
                     cap_gib=args.cap, swap_gib=args.swap,
                     keep_daemon=args.keep_daemon, task_cap_gib=args.task_cap)


if __name__ == "__main__":
    sys.exit(main())
