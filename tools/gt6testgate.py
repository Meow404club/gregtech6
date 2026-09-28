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
  (/tmp/gt6_rcon_slots): stale slots (crashed agent) reaped by 2 h mtime age.
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
concurrent gradle runs past 30G *between* polls. The gate now predicts
BEFORE spawning: ``used + estimate(task_class) <= limit``, where estimate
comes from a machine-wide peak-RSS ledger (/tmp/gt6_testgate_memory_ledger.json;
unknown classes fall back to conservative cold-start defaults). Every admitted
run is itself the runner: it samples the child's process-tree peak RSS while
the child executes and folds the observation back into the ledger (14-day
half-life decay against staleness). ``--dry-run`` prints used / estimate /
prediction / decision and starts nothing. A PreToolUse hook
(.githooks/guard-heavy-ops.sh) routes bare gradle invocations here, and
GITHUB_ACTIONS bypasses all gates at zero cost (CI boxes are not this WSL
host). Exit-code semantics are unchanged (child passthrough; full+coder
still exit 2 before any queueing).

Hard cap (test-gating-v3c, 2026-09-29 third ruling): the runner wraps the
child in ``systemd-run --user --scope -p Slice=gt6gate.slice`` so every
gated gradle shares one memory envelope — ``systemctl --user set-property
gt6gate.slice MemoryMax=28G MemorySwapMax=4G --runtime`` (28G leaves 2G
headroom for ungated processes; ``--cap``/``--swap`` retune, defaults 28/4).
The aggregate is naturally bounded; on exhaustion the kernel OOM-kills
inside the slice, never the WSL host. Bootstrap is idempotent and
re-asserted per run; if systemctl/systemd-run are unavailable the gate
degrades to a direct exec with one stderr warning (availability over
enforcement). Side benefit: JVMs with UseContainerSupport read the cgroup
cap and size their default heap accordingly (our explicit gradle heap flags
win; forked unconfigured JVMs benefit).

Usage:
    python3 tools/gt6testgate.py run [--tag <name>] [--role {coder,review}]
                              [--class CLASS] [--cap G] [--swap G]
                              [--dry-run] -- <command...>
    (the flags also work without the leading ``run`` — legacy form kept)

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
SLOT_STALE_SECONDS = 2 * 3600   # crashed agents leak slots; age reaps them
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
SLICE_CAP_GIB = 28              # 30G box: 2G headroom for ungated processes
SLICE_SWAP_GIB = 4
SLICE_ENV = "GT6_GATE_SLICE"    # "0" disables the wrap entirely

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


def command_mode(cmd):
    """"full" if cmd is a gradle run of an unfiltered test suite, else "other".

    full = some argv token is gradlew/gradle AND a non-flag token is a bare
    ``test``/``cleanTest`` task (any project path) AND no ``--tests`` filter.
    Filtered runs, compileTestJava, runData and non-gradle commands = "other"
    (open to both roles). Wrapping gradle in ``sh -c "..."`` hides the tokens
    and is treated as "other" — the gate is a discipline backstop.
    """
    if not any(Path(tok).name in ("gradlew", "gradle") for tok in cmd):
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
    if not any(Path(tok).name in ("gradlew", "gradle") for tok in cmd):
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


def mem_used_mib(path=MEMINFO_PATH):
    """used = MemTotal - MemAvailable in MiB (kernel reports both in kB)."""
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
    return (total - avail) // 1024


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


def slice_wrap(cmd, slice_name=SLICE_NAME):
    """``cmd`` → systemd-run scope argv inside the shared gate slice.

    --scope runs the command in-line (no service-manager round trip), so
    stdio passthrough and exit-code forwarding are preserved.
    """
    return ["systemd-run", "--user", "--scope",
            "-p", f"Slice={slice_name}", "--"] + list(cmd)


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

    ``read_used`` is injectable for tests (default: real /proc/meminfo).
    ``estimate_mib`` (v3) is the class's predicted peak: admission needs
    ``used + estimate <= limit`` so concurrent gated runs cannot stack past
    the cap between polls. estimate 0 (gt6server boot path) = old behavior.
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


def reap_stale_slots(slot_dir=SLOT_DIR, log=None):
    now = time.time()
    for slot in Path(slot_dir).glob("slot.*"):
        try:
            if now - slot.stat().st_mtime > SLOT_STALE_SECONDS:
                slot.unlink()
                if log:
                    log(f"[gt6testgate] reaped stale gate slot {slot.name}")
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
        reap_stale_slots(slot_dir, log)
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
              slice_bootstrap_fn=slice_bootstrap):
    """Gated runner: role gate → (full) global lock → predictive memory gate →
    test slot → (v3c slice wrap) → exec child with peak-RSS sampling → fold
    into ledger.

    The child inherits our stdio directly (no capture, no reinterpretation);
    lock/slot are released in finallys and the child's exit code returned.
    A full run by a non-review role is denied immediately — before any
    queueing — returning FULL_DENY_EXIT with guidance. v3: admission
    predicts ``used + estimate(class) <= limit``; after the child exits its
    sampled process-tree peak updates the ledger, so estimates track reality
    without any caller cooperation. v3c: the child runs inside the shared
    gt6gate.slice memory envelope when systemd agrees (pre-flight bootstrap
    + systemd-run on PATH); otherwise a one-line stderr warning and a direct
    exec. GITHUB_ACTIONS set → zero-gate passthrough (CI runners are not
    this WSL host).
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
    mode = command_mode(cmd)
    cls = cls or classify(cmd)
    role = resolve_role(role)
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
        wait_memory(poll=poll, tag=tag, log_file=log_file, log=log,
                    estimate_mib=estimate)
        slot = acquire_slot(poll=poll, slot_dir=slot_dir, tag=tag,
                            log_file=log_file, log=log)
        # v3c shared-slice hard cap. Pre-flight = the degrade gate; a runtime
        # scope-creation failure surfaces as a plain nonzero passthrough and
        # the next run's pre-flight (same failing environment) degrades then.
        wrapped = False
        if slice_enabled():
            try:
                usable = slice_bootstrap_fn(cap_gib, swap_gib)
            except (OSError, subprocess.SubprocessError):
                usable = False
            if usable and shutil.which("systemd-run"):
                wrapped = True
            else:
                print("[gt6testgate] systemd slice cap unavailable — running "
                      "UNCAPPED (degraded; export GT6_GATE_SLICE=0 to silence)",
                      file=sys.stderr)
        proc = subprocess.Popen(slice_wrap(cmd) if wrapped else cmd)
        peak = 0
        try:
            while True:
                peak = max(peak, tree_rss_mib(proc.pid))
                if proc.poll() is not None:
                    break
                time.sleep(SAMPLE_SECONDS)
            return proc.returncode
        finally:
            release_slot(slot, log=log)
            stored = record_observation(cls, peak, ledger_path)
            _journal("peak-sample", tag,
                     f"class={cls} peak={peak}MiB estimate={stored}MiB",
                     log_file)
    finally:
        release_full_lock(lock_fh, log=log)


def dry_run(cmd, cls=None, ledger_path=None, role=None,
            limit_mib=None, log=print, read_used=None, now=None):
    """Predict-only report: current usage / estimate / prediction / decision.

    Starts nothing, journals nothing — for subagents to self-check before
    dispatching and for the coordinator to schedule against. Returns the
    decision word (ADMIT / QUEUE / REJECT).
    """
    if ledger_path is None:
        ledger_path = LEDGER_PATH
    cls = cls or classify(cmd)
    role = resolve_role(role)
    limit = mem_limit_mib() if limit_mib is None else limit_mib
    mode = command_mode(cmd)
    ledger = load_ledger(ledger_path)
    entry = ledger.get(cls)
    estimate = estimate_for(cls, ledger, now=now)
    used = (read_used or mem_used_mib)()
    predicted = used + estimate
    source = "ledger" if isinstance(entry, dict) else "cold default"
    if mode == "full" and role != "review":
        decision = f"REJECT exit {FULL_DENY_EXIT} (full test run is review-seat only)"
    elif predicted > limit:
        decision = (f"QUEUE (predicted {predicted} MiB > limit {limit} MiB — "
                    f"polls until memory frees)")
    else:
        decision = "ADMIT (would run now)"
    log(f"[gt6testgate] dry-run: class={cls} role={role} mode={mode}")
    log(f"  current used:   {used} MiB")
    log(f"  estimate:       {estimate:.0f} MiB ({source}, class {cls})")
    log(f"  predicted peak: used {used} + estimate {estimate:.0f} = "
        f"{predicted} MiB (limit {limit} MiB)")
    log(f"  decision: {decision}")
    return decision.split(" ")[0]


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    # `run` subcommand (v3): the gate IS the runner. Same machinery as the
    # legacy flag form — strip the token only when a `--` follows, so a bare
    # command literally named "run" keeps working.
    if argv[:1] == ["run"] and "--" in argv[1:]:
        argv = argv[1:]
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
    parser.add_argument("--cap", type=int, default=SLICE_CAP_GIB,
                        help="shared gt6gate.slice MemoryMax in GiB "
                             "(default: 28 — 2G headroom on a 30G box)")
    parser.add_argument("--swap", type=int, default=SLICE_SWAP_GIB,
                        help="shared gt6gate.slice MemorySwapMax in GiB "
                             "(default: 4)")
    parser.add_argument("cmd", nargs=argparse.REMAINDER, metavar="CMD...",
                        help="command to run, after --")
    args = parser.parse_args(argv)
    cmd = args.cmd[1:] if args.cmd[:1] == ["--"] else args.cmd
    if not cmd:
        parser.error("no command — usage: gt6testgate run "
                     "[--tag NAME] [--role ROLE] [--class CLASS] [--dry-run] "
                     "-- COMMAND...")
    if args.dry_run:
        dry_run(cmd, cls=args.task_class, role=args.role)
        return 0
    return run_gated(cmd, tag=args.tag, role=args.role, cls=args.task_class,
                     cap_gib=args.cap, swap_gib=args.swap)


if __name__ == "__main__":
    sys.exit(main())
