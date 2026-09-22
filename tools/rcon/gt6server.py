#!/usr/bin/env python3
"""gt6server — headless runServer lifecycle for the GT6 acceptance chains (layer 1).

Every ops discipline the phase-era hand-written server segments re-copied per
card is baked in here as module behaviour:

- EULA: a missing ``mdk/run/eula.txt`` makes runServer exit silently after
  ~24 s with a zero-error lookalike. :func:`ensure_eula` guarantees the file
  before boot, so the silent exit cannot happen.
- ports: :func:`pick_ports` pre-checks with ``ss`` and yields on a busy
  segment (whole-span flip, or env GT6_RCON_SEGMENT_OFFSET for a manual
  stagger), so parallel cards never fight over a pinned pair.
- boot: :func:`start_server` is the nohup semantics — detached session, log
  file, pid file. The game never exits; never foreground, never a blocking
  wait (process discipline 2026-08-29).
- stop: :func:`stop_server` sends the RCON ``stop`` first, then touches only
  the pid we recorded or the pid that owns OUR rcon port. No ``--stop``, no
  broad pkill/pgrep anywhere (user ruling 2026-09-01; gradle daemons are left
  alone — they are not ours to kill). Since P30 the stop is gated by boot
  ownership: start_server stamps the caller identity into the slot record and
  stop_server refuses a foreign caller (same default password made any
  session's cleanup able to RCON-stop another session's live server,
  ops.p30-rcon-chain-repair); ``--force`` is the manual-cleanup escape.
- artifacts: ``/tmp/gt6_rs_<slug>.log`` + ``/tmp/gt6_rs_<slug>.pid`` — the
  convention every phase-era segment already used, now defined once here.

Import:  sys.path.insert(0, "tools/rcon"); import gt6server
"""

import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

import gt6rcon

# p34-ops-test-gate: server boots share the test wrapper's memory cap. __file__-
# relative (never cwd — /tmp live-run scripts once hijacked sys.path to a stale
# main-checkout copy, p30 lesson).
_TOOLS_DIR = str(Path(__file__).resolve().parents[1])
if _TOOLS_DIR not in sys.path:
    sys.path.insert(0, _TOOLS_DIR)
import gt6testgate

RUN_DIR_RELATIVE = Path("mdk") / "run"
# Since the P15 stonecutter skeleton each loader node is its own gradle project with a
# node-local run dir (mdk/versions/<node>/run — both build scripts' `gameDirectory =
# file("run/")` are versioned-project-relative), so the two nodes never share game state.
NODE_RUN_DIR_TEMPLATE = "mdk/versions/{node}/run"
ARTIFACT_DIR = Path("/tmp")

DONE_MARKER = "Done ("
GRADLE_TASK = ":mdk:runServer"   # pre-stonecutter legacy; unused by the node-aware path

# --- global RCON concurrency gate ------------------------------------------
# User ruling 2026-09-09: concurrent server boots OOM'd the box (five WSL kills
# in one night — every coder running its dual-leg chains at once). The gate is
# a /tmp slot namespace, so it binds across all worktrees; every boot path
# goes through start_server/stop_server, so there is no side door.
RCON_SLOT_DIR = Path("/tmp/gt6_rcon_slots")
RCON_SLOT_STALE_SECONDS = 2 * 3600  # crashed agents leak slots; age reaps them


def rcon_slot_limit():
    """Max concurrent server boots across ALL worktrees (env-overridable)."""
    try:
        return max(1, int(os.environ.get("GT6_RCON_MAX_CONCURRENT", "4")))
    except ValueError:
        return 4


def _reap_stale_slots(log=print):
    now = time.time()
    for slot in RCON_SLOT_DIR.glob("slot.*"):
        try:
            if now - slot.stat().st_mtime > RCON_SLOT_STALE_SECONDS:
                slot.unlink()
                log(f"[gt6server] reaped stale RCON slot {slot.name}")
        except OSError:
            pass  # raced with another reaper/owner


def acquire_rcon_slot(poll=15.0, log=print, owner=None):
    """Claim one global boot slot, actively queueing while full.

    Atomic O_EXCL create over a fixed /tmp namespace — cross-worktree by
    construction. Stale slots (agent died mid-session, WSL crash) are reaped
    by age so a leak cannot wedge the suite forever. The record is born
    complete in the single O_EXCL write (timestamp + optional ``owner`` line)
    so a concurrent reader never sees a half-written ownership stamp. Returns
    the slot path; pair with :func:`release_rcon_slot` (stop_server does this
    via the ``<pid_file>.slot`` marker start_server leaves behind).
    """
    # memory gate layered ON TOP of the slot semaphore (p34-ops-test-gate):
    # waiting for memory must not hold a slot. Semaphore semantics unchanged.
    gt6testgate.wait_memory(tag=f"rcon-boot:{owner or os.getpid()}", log=log)
    RCON_SLOT_DIR.mkdir(exist_ok=True)
    limit = rcon_slot_limit()
    waited = False
    record = f"{time.time()}\n"
    if owner:
        record += f"owner {owner}\n"
    while True:
        _reap_stale_slots(log)
        live = sorted(RCON_SLOT_DIR.glob("slot.*"))
        if len(live) < limit:
            for _ in range(limit):
                candidate = RCON_SLOT_DIR / f"slot.{os.getpid()}.{time.monotonic_ns()}"
                try:
                    fd = os.open(candidate, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                    os.write(fd, record.encode())
                    os.close(fd)
                    if waited:
                        log(f"[gt6server] RCON slot acquired after queueing")
                    return candidate
                except FileExistsError:
                    continue
        if not waited:
            waited = True
            log(f"[gt6server] RCON slots full ({len(live)}/{limit} live) — queueing "
                f"every {poll:.0f}s (cap: env GT6_RCON_MAX_CONCURRENT)")
        time.sleep(poll)


def release_rcon_slot(slot, log=print):
    try:
        Path(slot).unlink(missing_ok=True)
    except OSError as exc:
        log(f"[gt6server] slot release failed ({exc}) — stale reaper will collect it")


# --- stop ownership (P30) ----------------------------------------------------
# ops.p30-rcon-chain-repair: a foreign session's cleanup RCON-stopped another
# session's live neo-leg server through the same default password — stop had
# no owner check (gt6server.py's old :542). The seam: start_server stamps
# caller_identity() into the slot record; stop_server resolves the record via
# the <pid_file>.slot marker and refuses anyone who is not the boot process
# itself or the same worktree session.

def caller_identity(worktree=None):
    """The boot/stop ownership identity: PID + the session's worktree token.

    One card, one worktree (project discipline), so the resolved worktree
    path is the coarsest reliable session identity; GT6_RCON_OWNER overrides
    it for callers whose cwd cannot be trusted. The PID makes the common
    boot-then-stop-in-one-process case an exact match.
    """
    token = os.environ.get("GT6_RCON_OWNER", "").strip()
    if not token:
        base = Path(worktree) if worktree else Path.cwd()
        token = str(base.resolve())
    return f"pid={os.getpid()} {token}"


def _identity_pid(identity):
    match = re.match(r"pid=(\d+)", identity)
    return int(match.group(1)) if match else None


def identity_matches(recorded, caller):
    """True when `caller` may stop a boot whose record says `recorded`.

    Either the very boot process (PID equal — the framework boots and stops
    in one process) or the same worktree session (token equal, or the caller
    token resolves inside the recorded worktree — manual CLI stops from a
    subdirectory).
    """
    if recorded.strip() == caller.strip():
        return True
    recorded_pid, caller_pid = _identity_pid(recorded), _identity_pid(caller)
    if recorded_pid is not None and recorded_pid == caller_pid:
        return True
    recorded_token = recorded.split(" ", 1)[-1]
    caller_token = caller.split(" ", 1)[-1]
    try:
        return Path(caller_token).resolve().is_relative_to(
            Path(recorded_token).resolve())
    except (OSError, ValueError):
        return False


class StopOwnershipError(RuntimeError):
    """A stop was refused: the caller does not match the boot's ownership
    record (P30 — see the stop-ownership block comment)."""


def recorded_stop_owner(pid_file):
    """The boot owner recorded for this pid_file handle, or (None, reason)."""
    marker = Path(str(pid_file) + ".slot")
    try:
        slot = Path(marker.read_text(encoding="utf-8").strip())
    except OSError:
        return None, f"no boot-slot marker {marker} — not a start_server boot handle"
    try:
        text = slot.read_text(encoding="utf-8")
    except OSError:
        return None, f"slot record {slot.name} unreadable or reaped"
    for line in text.splitlines():
        if line.startswith("owner "):
            return line[len("owner "):].strip(), None
    return None, f"slot record {slot.name} predates ownership stamping (legacy boot)"


def gradle_task(node=None):
    """The boot task for a stonecutter node: ``:mdk:<node>:runServer``.

    The bare ``:mdk:runServer`` died with the P15 stonecutter skeleton (the controller
    project's buildFileName is stonecutter.gradle.kts and registers no runs) — every
    node boot must address the versioned subproject.
    """
    return f":mdk:{node}:runServer" if node else GRADLE_TASK


def run_dir(worktree, node=None):
    """The run directory of a boot: node-local since the stonecutter skeleton."""
    relative = NODE_RUN_DIR_TEMPLATE.format(node=node) if node else RUN_DIR_RELATIVE
    return Path(worktree) / relative


def artifact_paths(slug):
    """The per-chain log/pid artifact pair: /tmp/gt6_rs_<slug>.{log,pid}."""
    base = ARTIFACT_DIR / f"gt6_rs_{slug}"
    return base.with_suffix(".log"), base.with_suffix(".pid")


def ensure_eula(run_dir):
    """Guarantee <run_dir>/eula.txt says eula=true (the silent-exit trap).

    Returns the eula path. An existing file lacking eula=true is overwritten —
    these are headless acceptance rigs, the answer is always yes.
    """
    run_dir = Path(run_dir)
    run_dir.mkdir(parents=True, exist_ok=True)
    eula = run_dir / "eula.txt"
    current = ""
    if eula.exists():
        current = eula.read_text(encoding="utf-8", errors="replace")
    if "eula=true" not in current:
        eula.write_text("eula=true\n", encoding="utf-8")
        print(f"[gt6server] eula.txt {'written' if not current else 'forced to eula=true'}: {eula}")
    return eula


# The keys gt6server owns in server.properties. level-type is flat so chains
# get a deterministic empty spawn area (the server escapes the colon itself).
PROVISIONED_KEYS = {
    "enable-rcon": "true",
    "online-mode": "false",       # headless, no premium account
    "level-type": "minecraft\\:flat",
    "spawn-protection": "0",
}


def write_server_properties(run_dir, game_port, rcon_port, query_port, password):
    """Point server.properties at this boot's ports; create a minimal file if absent.

    An existing file is patched key-by-key (everything not in PROVISIONED_KEYS
    and the port/password keys is preserved). Returns the properties path.
    """
    props = Path(run_dir) / "server.properties"
    wanted = dict(PROVISIONED_KEYS)
    wanted.update({
        "server-port": str(game_port),
        "rcon.port": str(rcon_port),
        "rcon.password": password,
        "query.port": str(query_port),
    })
    if not props.exists():
        body = ["# written by gt6server.provision_run_dir (framework bootstrap)\n"]
        body += [f"{key}={value}\n" for key, value in wanted.items()]
        props.write_text("".join(body), encoding="utf-8")
        print(f"[gt6server] server.properties created: {props}")
        return props
    lines = props.read_text(encoding="utf-8", errors="replace").splitlines(keepends=True)
    seen = set()
    for index, line in enumerate(lines):
        key = line.split("=", 1)[0].strip()
        if key in wanted and not line.lstrip().startswith("#"):
            lines[index] = f"{key}={wanted[key]}\n"
            seen.add(key)
    lines += [f"{key}={wanted[key]}\n" for key in wanted if key not in seen]
    props.write_text("".join(lines), encoding="utf-8")
    print(f"[gt6server] server.properties patched: ports game={game_port} "
          f"rcon={rcon_port} query={query_port}")
    return props


def provision_run_dir(worktree, game_port, rcon_port, query_port, password, node=None):
    """Bootstrap the node's run dir: eula + server.properties. Returns the run dir."""
    run_dir_ = run_dir(worktree, node)
    ensure_eula(run_dir_)
    write_server_properties(run_dir_, game_port, rcon_port, query_port, password)
    return run_dir_


def listening_ports():
    """Ports currently in LISTEN, parsed from `ss -ltn` (column 4 = addr:port)."""
    probe = subprocess.run(["ss", "-ltn"], capture_output=True, text=True, check=True)
    ports = set()
    for line in probe.stdout.splitlines():
        columns = line.split()
        if len(columns) < 4 or columns[0] != "LISTEN":
            continue
        try:
            ports.add(int(columns[3].rsplit(":", 1)[1]))
        except ValueError:
            continue
    return ports


# p34-pool-port-stagger: a session boot occupies a port "segment" — the
# session triple spans game=rcon-10 .. query=rcon+10 (the framework
# SESSION_PORTS convention). Two stagger hooks keep parallel sessions off
# each other's segment: GT6_RCON_SEGMENT_OFFSET shifts every preferred start
# by offset*STRIDE (parallel sessions set different values), and the pick
# flips a whole segment when the candidate span holds a listener instead of
# squeezing +1 into the occupied neighbourhood (the incident shape: two
# boots on the default 256xx segment, the loser failing fast in
# assert_ports_free and idling until the winner finished). BootOwnershipError
# stays the correctness backstop — assert_ports_free is untouched.
RCON_SEGMENT_SPAN = 10     # half-width of one session's port span
RCON_SEGMENT_STRIDE = 50   # next-segment jump; > 2*SPAN keeps segments disjoint
# ponytail: fixed stride heuristic, and the flip is per-entry — a partially
# occupied segment can split one triple across two segments (still all-free;
# coordinate the flip over the whole triple only if that ever matters).


def rcon_segment_offset():
    """The env stagger offset (GT6_RCON_SEGMENT_OFFSET, integer, default 0)."""
    try:
        return int(os.environ.get("GT6_RCON_SEGMENT_OFFSET", "0"))
    except ValueError:
        return 0


def _segment_occupied(start, used):
    """True when a listener sits anywhere in [start-SPAN, start+SPAN]."""
    return any(abs(port - start) <= RCON_SEGMENT_SPAN for port in used)


def _free_segment_start(start, used, taken=()):
    """The first segment anchor >= `start` (striding RCON_SEGMENT_STRIDE)
    whose span is listener-free and whose anchor is not already picked."""
    candidate = start
    while _segment_occupied(candidate, used) or candidate in taken:
        candidate += RCON_SEGMENT_STRIDE
    return candidate


def pick_ports(preferred, count=1):
    """ss pre-check with segment stagger: the first listener-free segment
    from `preferred` upward (env GT6_RCON_SEGMENT_OFFSET shifts the start).

    `preferred` may also be an ordered tuple/list (rcon, query, game, ...) — then
    each entry is picked independently from its own start (`count` is ignored),
    which is how a chain pins a conventional pair like (25662, 25672) while still
    yielding when another card took one of them. An occupied span flips the pick
    to the next segment (stride jump) rather than bumping +1 inside the occupied
    neighbourhood; the exact ports are re-checked at boot by assert_ports_free,
    which stays the fail-fast backstop for the pick-to-bind race window.
    """
    offset = rcon_segment_offset() * RCON_SEGMENT_STRIDE
    if isinstance(preferred, (tuple, list)):
        used = listening_ports()
        picked = []
        for start in preferred:
            picked.append(_free_segment_start(start + offset, used, picked))
        return picked
    used = listening_ports()
    picked = []
    candidate = _free_segment_start(preferred + offset, used)
    while len(picked) < count:
        if candidate not in used and candidate not in picked:
            picked.append(candidate)
        candidate += 1
    return picked


def port_owner(port):
    """The pid listening on `port`, or None — from `ss -ltnp`, scoped to that one port."""
    probe = subprocess.run(["ss", "-ltnp"], capture_output=True, text=True, check=True)
    for line in probe.stdout.splitlines():
        columns = line.split()
        if len(columns) < 4 or columns[0] != "LISTEN":
            continue
        try:
            if int(columns[3].rsplit(":", 1)[1]) != port:
                continue
        except ValueError:
            continue
        match = re.search(r"pid=(\d+)", line)
        if match:
            return int(match.group(1))
    return None


def process_alive(pid):
    """kill -0 semantics: True when the pid exists (a foreign-owned pid counts as alive).

    A zombie counts as dead: an unreaped child of this interpreter still answers
    kill -0, and wait_done must not blind-poll a boot that already exited
    (2026-09-04: the first 1.21.1 crash left a defunct wrapper and the watcher
    would have spun for its whole timeout).
    """
    if pid is None:
        return False
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    except OSError:
        return False
    try:
        with open(f"/proc/{pid}/stat", "rb") as handle:
            # stat = "pid (comm) state ..." — the state is the first field after
            # the parenthesized comm (comm itself may contain spaces/parens).
            state = handle.read().rsplit(b")", 1)[-1].split()[0]
        return state != b"Z"
    except (OSError, IndexError):
        return True


def log_tail(log_path, nbytes=8192):
    """The last nbytes of the log, decoded lossily — for failure diagnostics."""
    path = Path(log_path)
    if not path.exists():
        return "<log missing>"
    with path.open("rb") as handle:
        handle.seek(0, os.SEEK_END)
        handle.seek(max(0, handle.tell() - nbytes))
        return handle.read().decode("utf-8", "replace")


# Error-attribution hints for the failure diagnostics: boot crashes announce
# themselves in logback ERROR lines (any logger, not only the server thread)
# and in Java stack traces; the post-crash epilogue can bury them under far
# more than the 8 KiB a plain tail window shows.
ERROR_HINTS = ("ERROR", "Exception", "FATAL")


def error_tail(log_path, nbytes=8192, max_lines=25):
    """Failure diagnostics that survive fast scroll: attribution lines first.

    The plain 8 KiB tail loses the crash ERROR line whenever later output —
    gradle's post-mortem report alone dwarfs the window — pushes it out: the
    same sick-boot blindness wait_done's fixed-window Done check had (P19).
    Abnormal path only, so the whole-log scan costs the hot path nothing:
    the last `max_lines` error-ish lines of the WHOLE log, then the plain
    tail for the death scene.
    """
    try:
        text = Path(log_path).read_bytes().decode("utf-8", "replace")
    except OSError:
        return log_tail(log_path, nbytes)
    errors = [line for line in text.splitlines()
              if any(hint in line for hint in ERROR_HINTS)]
    parts = []
    if errors:
        shown = errors[-max_lines:]
        parts.append(f"error-ish lines (last {len(shown)} of {len(errors)}):")
        parts.extend(shown)
    parts.append("--- plain tail:")
    parts.append(log_tail(log_path, nbytes))
    return "\n".join(parts)


def _new_log_bytes(log_path, offset, carry=b""):
    """The bytes appended to `log_path` since `offset`, for a monotonic scan.

    Returns (chunk, new_offset, new_carry): `chunk` is the carry-prefixed
    appended text, and `new_carry` keeps the last len(DONE_MARKER)-1 raw
    bytes so a marker split across two reads is still matched by the next
    chunk; a truncated/rotated log (size < offset) restarts the scan from
    byte 0. An unreadable log reads as empty — start_server creates the
    file, but wait_done never bets on that inside the boot race.
    """
    path = Path(log_path)
    try:
        size = path.stat().st_size
    except OSError:
        return "", offset, carry
    if size < offset:
        offset, carry = 0, b""
    if size == offset:
        return "", offset, carry
    with path.open("rb") as handle:
        handle.seek(offset)
        chunk = carry + handle.read(size - offset)
    keep = max(len(DONE_MARKER) - 1, 0)
    return chunk.decode("utf-8", "replace"), size, chunk[-keep:]


class ServerStartError(RuntimeError):
    """The boot process died before printing the Done marker."""


class BootOwnershipError(RuntimeError):
    """A boot-ownership gate failed: the framework refuses to boot onto (or
    record a handle for) a foreign server — fail fast instead of connecting
    blind (P16 closeout: the session model once attached to a stale-behavior
    server, tasks.p16-pattern-checker; these gates mechanize the manual boot
    ownership review that closeout ran by hand)."""


def assert_ports_free(ports, label="boot"):
    """Pre-boot ownership gate: every port THIS boot will bind must be free.

    Re-runs `ss -ltn` at the boot site — pick_ports's freeness snapshot ages,
    and a port grabbed between pick and bind would otherwise surface as a deep
    Minecraft 'Failed to bind to port' minutes later. Here it fails fast,
    naming the current owner pid. A busy PREFERRED port never trips this: the
    caller's pick_ports already bumped to a free pick before the gate runs.
    """
    used = listening_ports()
    taken = [port for port in ports if port in used]
    if taken:
        owners = {port: port_owner(port) for port in taken}
        detail = ", ".join(f"{port} (pid {owner})" for port, owner in owners.items())
        raise BootOwnershipError(
            f"{label}: target port(s) not free before boot: {detail} — "
            f"refusing to boot onto a foreign listener")
    return True


def assert_pid_file(pid_path, expected_pid, label="boot"):
    """Post-boot ownership gate: the pid file records THIS boot's pid.

    Read back and compared to the wrapper pid the boot just spawned: a file
    clobbered by a parallel boot (or a stale leftover) would make the recorded
    handle — stop_server's FIRST kill target — point at a foreign process.
    Fail fast; the cleanup must never aim at someone else's pid.
    """
    try:
        recorded = Path(pid_path).read_text(encoding="utf-8").strip()
    except OSError as exc:
        raise BootOwnershipError(
            f"{label}: pid file {pid_path} unreadable after boot ({exc}) — "
            f"refusing an unownable handle") from None
    try:
        recorded_pid = int(recorded)
    except ValueError:
        raise BootOwnershipError(
            f"{label}: pid file {pid_path} holds garbage {recorded!r} — "
            f"refusing an unownable handle") from None
    if recorded_pid != int(expected_pid):
        raise BootOwnershipError(
            f"{label}: pid file {pid_path} records pid {recorded_pid}, expected "
            f"THIS boot's pid {expected_pid} — refusing a foreign handle")
    return True


def wait_done(log_path, timeout=300.0, poll=1.0, pid=None):
    """Poll the log for the `Done (` marker. True = up; False = timed out.

    The scan is monotonic, not a fixed tail window: the first poll reads the
    log from byte 0, every later poll only the appended delta — a marker seen
    once stays seen. The old last-8-KiB `log_tail` check faked a timeout on a
    fast-scrolling sick boot whenever >8 KiB landed after `Done (` between two
    polls, reporting "never printed Done" about a server that was up (P19).
    The normal path does no more work than before: the log is read once in
    total across all polls, versus a fresh 8 KiB tail re-read every poll.

    Raises ServerStartError as soon as the recorded pid dies before the marker
    — never keep polling a dead process (process discipline 2026-08-29); the
    attribution comes from the whole log (error_tail), not from a tail that
    the crash epilogue may have scrolled the ERROR line out of.
    """
    deadline = time.monotonic() + timeout
    offset, carry = 0, b""
    while time.monotonic() < deadline:
        chunk, offset, carry = _new_log_bytes(log_path, offset, carry)
        if chunk and DONE_MARKER in chunk:
            return True
        if pid is not None and not process_alive(pid):
            raise ServerStartError(
                f"boot process {pid} died before '{DONE_MARKER}'; "
                f"error attribution:\n{error_tail(log_path)}")
        time.sleep(poll)
    return False


def start_server(worktree, log_path, pid_path, gradle_task=GRADLE_TASK):
    """nohup semantics: detached `./gradlew <gradle_task>`, log + pid artifacts.

    Returns the recorded (gradle wrapper) pid — the one the phase-era segments
    wrote with `echo $! > $PIDF`. The wrapper is the tracked handle; the server
    JVM itself is reached through stop_server's RCON stop / port-owner lookup.
    Boots are gated by the global RCON slot semaphore (OOM ruling 2026-09-09):
    acquire before boot with the caller identity stamped into the slot record
    (the P30 stop-ownership seam), leave a ``<pid_file>.slot`` marker for
    stop_server to release.
    """
    slot = acquire_rcon_slot(owner=caller_identity(worktree))
    slot_marker = Path(str(pid_path) + ".slot")
    log_path, pid_path = Path(log_path), Path(pid_path)
    log = log_path.open("w")  # truncates, like the segments' `: > "$LOG"`
    try:
        process = subprocess.Popen(
            ["./gradlew", gradle_task],
            cwd=str(worktree),
            stdout=log,
            stderr=subprocess.STDOUT,
            start_new_session=True,  # nohup: survives this shell, own process group
        )
    except BaseException:
        release_rcon_slot(slot)
        raise
    finally:
        log.close()  # the child holds its own dup
    pid_path.write_text(f"{process.pid}\n", encoding="utf-8")
    slot_marker.write_text(f"{slot}\n", encoding="utf-8")
    # post-boot ownership gate: read our own write back — a parallel boot
    # clobbering this exact artifact path fails HERE, not at stop time when
    # the recorded handle points at a foreign process (P17, P16 pid-stomp).
    try:
        assert_pid_file(pid_path, process.pid, label=gradle_task)
    except BaseException:
        release_rcon_slot(slot)
        slot_marker.unlink(missing_ok=True)
        raise
    print(f"[gt6server] gradle pid {process.pid} -> {pid_path}, log -> {log_path}")
    return process.pid


def _wait_gone(pid, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if not process_alive(pid):
            return True
        time.sleep(0.5)
    return not process_alive(pid)


def _terminate(pid, label):
    print(f"[gt6server] SIGTERM {label} pid {pid}")
    try:
        os.kill(pid, signal.SIGTERM)
    except ProcessLookupError:
        pass


def _stop_server_impl(pid_file, rcon=None, grace=60.0, jvm_grace=15.0, term_grace=10.0):
    """Precise shutdown: RCON stop, then by-recorded-pid, then by-port-owner pid.

    `rcon` is (host, rcon_port, password). Escalation order, each scoped to
    processes that are ours:
      1. RCON `stop` — the graceful path; the JVM exits, the gradle run task
         finishes, the wrapper exits on its own.
      2. wait `grace` s for the recorded wrapper pid.
      3. if the rcon port still has an owner, SIGTERM that JVM pid (it is our
         server by construction — the port is this boot's rcon.port).
      4. if the wrapper still lingers, SIGTERM (then SIGKILL) the wrapper pid.
    Never sends --stop to gradle, never pkills, never touches daemons.
    Returns a small report dict.
    """
    pid_file = Path(pid_file)
    report = {"wrapper_pid": None, "rcon_stop": None, "jvm_pid": None, "leftover_ports": []}
    pid = None
    if pid_file.exists():
        pid = int(pid_file.read_text(encoding="utf-8").strip() or 0)
        report["wrapper_pid"] = pid
    elif not rcon:
        print(f"[gt6server] no pid file {pid_file} and no rcon info — nothing to stop")
        return report
    else:
        print(f"[gt6server] no pid file {pid_file} — falling back to the rcon port owner")

    if rcon:
        host, rcon_port, password = rcon
        try:
            with gt6rcon.RconClient(host, rcon_port, password, timeout=5.0) as client:
                client.run_command("stop")
            report["rcon_stop"] = "sent"
            print("[gt6server] stop sent via RCON")
        except Exception as exc:  # server may already be gone; that is fine
            report["rcon_stop"] = f"failed: {exc}"
            print(f"[gt6server] RCON stop unavailable ({exc}) — falling back to pid kill")

    def _port_owner_kill():
        """SIGTERM the JVM that owns our rcon port (ours by construction)."""
        if not rcon:
            return False
        jvm = port_owner(rcon[1])
        if jvm is not None and jvm != pid:
            report["jvm_pid"] = jvm
            _terminate(jvm, f"server JVM (owner of rcon port {rcon[1]})")
            if not _wait_gone(jvm, jvm_grace):
                print(f"[gt6server] JVM {jvm} survived SIGTERM — SIGKILL")
                try:
                    os.kill(jvm, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                _wait_gone(jvm, 5.0)
            return True
        return False

    if pid is not None and _wait_gone(pid, grace):
        print(f"[gt6server] gradle wrapper {pid} exited")
    elif pid is not None:
        if _port_owner_kill() and _wait_gone(pid, term_grace):
            print(f"[gt6server] gradle wrapper {pid} exited after JVM stop")
        elif not process_alive(pid):
            print(f"[gt6server] gradle wrapper {pid} exited")
        else:
            _terminate(pid, "gradle wrapper")
            if not _wait_gone(pid, 5.0):
                print(f"[gt6server] wrapper {pid} survived SIGTERM — SIGKILL")
                try:
                    os.kill(pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
            else:
                print(f"[gt6server] gradle wrapper {pid} terminated")
    else:
        # no recorded pid: just make sure our rcon port is released
        _port_owner_kill()

    if rcon:
        # final closure: whatever the path above did, our rcon port must end free.
        # This is also the orphan-JVM fix (wrapper dead, server still listening).
        if not _port_owner_kill():
            print(f"[gt6server] port {rcon[1]} free")
        leftover = port_owner(rcon[1])
        if leftover is not None:
            report["leftover_ports"].append((rcon[1], leftover))
            print(f"[gt6server] WARNING: port {rcon[1]} still owned by pid {leftover}")
    return report


def stop_server(pid_file, rcon=None, grace=60.0, jvm_grace=15.0, term_grace=10.0,
                owner=None, force=False):
    """:func:`_stop_server_impl` plus the boot-slot release (gate pairing:
    start_server acquired a slot and left a ``<pid_file>.slot`` marker) and
    the P30 ownership gate: only the boot process itself or the same
    worktree session may stop; a foreign caller is refused with both
    identities named, before any RCON/pid action and without releasing the
    boot's slot. ``force=True`` overrides for manual cleanup (prints a
    warning). A pure no-op (no pid file, no rcon) stays ungated.
    """
    pid_file = Path(pid_file)
    if force:
        print("[gt6server] WARNING: --force stop — boot-ownership gate overridden "
              "(manual cleanup)")
    elif pid_file.exists() or rcon:
        recorded, detail = recorded_stop_owner(pid_file)
        caller = owner or caller_identity()
        if recorded is None:
            raise StopOwnershipError(
                f"stop refused: boot ownership unverifiable ({detail}); "
                f"caller is [{caller}] — rerun with --force for manual cleanup")
        if not identity_matches(recorded, caller):
            raise StopOwnershipError(
                f"stop refused: server belongs to [{recorded}], caller is "
                f"[{caller}] — rerun with --force for manual cleanup")
    try:
        return _stop_server_impl(pid_file, rcon, grace, jvm_grace, term_grace)
    finally:
        slot_marker = Path(str(pid_file) + ".slot")
        if slot_marker.exists():
            recorded = slot_marker.read_text(encoding="utf-8").strip()
            if recorded:
                release_rcon_slot(recorded)
            slot_marker.unlink(missing_ok=True)


def server_error_lines(log_path, start_byte=0):
    """Count of [Server thread/ERROR] lines — the segments' closing report line.

    start_byte scopes the count to the log suffix written from there on: the
    session model shares one log across chains, and each chain's ERROR report
    reads only its own slice (framework._run_chain_on_server records the byte
    offset at the chain boundary). Default 0 keeps the whole-file count.
    """
    try:
        with Path(log_path).open("rb") as handle:
            if start_byte:
                handle.seek(max(0, start_byte))
            text = handle.read().decode("utf-8", "replace")
    except OSError:
        return 0
    return sum(1 for line in text.splitlines() if "[Server thread/ERROR]" in line)


def main(argv=None):
    """Manual use: python3 tools/rcon/gt6server.py status|provision|stop ..."""
    parser = __import__("argparse").ArgumentParser(prog="gt6server")
    parser.add_argument("action", choices=["status", "provision", "stop"])
    parser.add_argument("--worktree", default=".")
    parser.add_argument("--slug", default="manual")
    parser.add_argument("--game-port", type=int, default=25652)
    parser.add_argument("--rcon-port", type=int, default=25662)
    parser.add_argument("--query-port", type=int, default=25672)
    parser.add_argument("--password", default="gt6")
    parser.add_argument("--node", default=None,
                        help="stonecutter node (e.g. 1.21.1-neoforge); default = legacy :mdk:runServer")
    parser.add_argument("--force", action="store_true",
                        help="override the boot-ownership gate (manual cleanup; prints a warning)")
    args = parser.parse_args(argv)
    log_path, pid_path = artifact_paths(args.slug)
    if args.action == "status":
        print(f"log {log_path} pid {pid_path}; listening: {sorted(listening_ports())}")
        return 0
    if args.action == "provision":
        provision_run_dir(args.worktree, args.game_port, args.rcon_port,
                          args.query_port, args.password, node=args.node)
        return 0
    try:
        stop_server(pid_path, rcon=("127.0.0.1", args.rcon_port, args.password),
                    force=args.force)
    except StopOwnershipError as exc:
        print(f"[gt6server] {exc}")
        return 3
    return 0


if __name__ == "__main__":
    sys.exit(main())
