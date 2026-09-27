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

Usage:
    python3 tools/gt6testgate.py [--tag <name>] [--role {coder,review}] -- <command...>

The child's stdout/stderr pass through untouched (no capture); the wrapper
exits with the child's exit code.

Import:  sys.path.insert(0, "tools"); import gt6testgate
"""

import argparse
import fcntl
import os
import re
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


def _journal(event, tag, detail, log_file=GATE_LOG):
    """Append one timestamped gate event; best-effort, never fails the workload."""
    try:
        with open(log_file, "a", encoding="utf-8") as fh:
            fh.write(f"{time.strftime('%Y-%m-%dT%H:%M:%S')} {event} tag={tag} {detail}\n")
    except OSError:
        pass


def wait_memory(read_used=None, limit_mib=None, poll=POLL_SECONDS,
                tag="-", log_file=GATE_LOG, log=None):
    """Block until used <= limit; journal every queueing and admission event.

    ``read_used`` is injectable for tests (default: real /proc/meminfo).
    Returns (used_mib_at_admission, queued_seconds).
    """
    if read_used is None:
        read_used = mem_used_mib
    if limit_mib is None:
        limit_mib = mem_limit_mib()
    started = time.monotonic()
    used = read_used()
    if used > limit_mib:
        _journal("mem-queue", tag, f"used={used}MiB limit={limit_mib}MiB", log_file)
        if log:
            log(f"[gt6testgate] memory {used} MiB > limit {limit_mib} MiB — "
                f"queueing every {poll:.0f}s (cap: env GT6_GATE_MEM_LIMIT_MIB)")
        while True:
            time.sleep(poll)
            used = read_used()
            if used <= limit_mib:
                break
    queued = time.monotonic() - started
    _journal("mem-admit", tag, f"used={used}MiB queued={queued:.0f}s", log_file)
    if log and queued >= 1.0:
        log(f"[gt6testgate] memory gate passed after {queued:.0f}s (used {used} MiB)")
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
              full_lock=FULL_LOCK_PATH):
    """Gated exec: role gate, then (full only) global lock, then memory gate,
    then test slot, then passthrough child.

    The child inherits our stdio directly (no capture, no reinterpretation);
    the lock and slot are released in finallys, and the child's exit code is
    returned. A full run by a non-review role is denied immediately — before
    any queueing — returning FULL_DENY_EXIT with guidance.
    """
    if not cmd:
        raise ValueError("empty command")
    if tag is None:
        tag = Path(cmd[0]).name
    mode = command_mode(cmd)
    role = resolve_role(role)
    if mode == "full" and role != "review":
        _journal("full-reject", tag, f"role={role}", log_file)
        log("[gt6testgate] FULL test run denied (role=coder) — "
            "全量归审查席，请用 --tests 过滤模式 "
            "(full runs are review-seat only; rerun with --tests FILTER, or "
            "ask the review seat to replay)")
        return FULL_DENY_EXIT
    lock_fh = None
    try:
        if mode == "full":
            lock_fh = acquire_full_lock(full_lock, tag=tag, log_file=log_file,
                                        log=log)
        wait_memory(poll=poll, tag=tag, log_file=log_file, log=log)
        slot = acquire_slot(poll=poll, slot_dir=slot_dir, tag=tag,
                            log_file=log_file, log=log)
        try:
            return subprocess.run(cmd).returncode
        finally:
            release_slot(slot, log=log)
    finally:
        release_full_lock(lock_fh, log=log)


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="gt6testgate",
        description="GT6 unified heavy-operation gate: memory cap + concurrency "
                    "slots around any command (gradle, sweeps, chain runs).")
    parser.add_argument("--tag", default=None,
                        help="journal tag for /tmp/gt6_gate.log "
                             "(default: the command's basename)")
    parser.add_argument("--role", choices=ROLES, default=None,
                        help="caller role; full (unfiltered test/cleanTest) "
                             "runs are review-seat only (default: coder, or "
                             f"env {ROLE_ENV})")
    parser.add_argument("cmd", nargs=argparse.REMAINDER, metavar="CMD...",
                        help="command to run, after --")
    args = parser.parse_args(argv)
    cmd = args.cmd[1:] if args.cmd[:1] == ["--"] else args.cmd
    if not cmd:
        parser.error("no command — usage: gt6testgate [--tag NAME] -- COMMAND...")
    return run_gated(cmd, tag=args.tag, role=args.role)


if __name__ == "__main__":
    sys.exit(main())
