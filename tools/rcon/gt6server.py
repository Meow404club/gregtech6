#!/usr/bin/env python3
"""gt6server — headless runServer lifecycle for the GT6 acceptance chains (layer 1).

Every ops discipline the phase-era hand-written server segments re-copied per
card is baked in here as module behaviour:

- EULA: a missing ``mdk/run/eula.txt`` makes runServer exit silently after
  ~24 s with a zero-error lookalike. :func:`ensure_eula` guarantees the file
  before boot, so the silent exit cannot happen.
- ports: :func:`pick_ports` pre-checks with ``ss`` and bumps on a busy port,
  so parallel cards never fight over a pinned pair.
- boot: :func:`start_server` is the nohup semantics — detached session, log
  file, pid file. The game never exits; never foreground, never a blocking
  wait (process discipline 2026-08-29).
- stop: :func:`stop_server` sends the RCON ``stop`` first, then touches only
  the pid we recorded or the pid that owns OUR rcon port. No ``--stop``, no
  broad pkill/pgrep anywhere (user ruling 2026-09-01; gradle daemons are left
  alone — they are not ours to kill).
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

RUN_DIR_RELATIVE = Path("mdk") / "run"
ARTIFACT_DIR = Path("/tmp")

DONE_MARKER = "Done ("
GRADLE_TASK = ":mdk:runServer"


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


def provision_run_dir(worktree, game_port, rcon_port, query_port, password):
    """Bootstrap <worktree>/mdk/run: eula + server.properties. Returns the run dir."""
    run_dir = Path(worktree) / RUN_DIR_RELATIVE
    ensure_eula(run_dir)
    write_server_properties(run_dir, game_port, rcon_port, query_port, password)
    return run_dir


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


def pick_ports(preferred, count=1):
    """ss pre-check with bump: the first `count` free ports from `preferred` upward.

    `preferred` may also be an ordered tuple/list (rcon, query, game, ...) — then
    each entry is picked independently from its own start (`count` is ignored),
    which is how a chain pins a conventional pair like (25662, 25672) while still
    yielding when another card took one of them.
    """
    if isinstance(preferred, (tuple, list)):
        used = listening_ports()
        picked = []
        for start in preferred:
            candidate = start
            while candidate in used or candidate in picked:
                candidate += 1
            picked.append(candidate)
        return picked
    used = listening_ports()
    picked = []
    candidate = preferred
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
    """kill -0 semantics: True when the pid exists (a foreign-owned pid counts as alive)."""
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


class ServerStartError(RuntimeError):
    """The boot process died before printing the Done marker."""


def wait_done(log_path, timeout=300.0, poll=1.0, pid=None):
    """Poll the log for the `Done (` marker. True = up; False = timed out.

    Raises ServerStartError as soon as the recorded pid dies before the marker —
    never keep polling a dead process (process discipline 2026-08-29).
    """
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if DONE_MARKER in log_tail(log_path):
            return True
        if pid is not None and not process_alive(pid):
            raise ServerStartError(
                f"boot process {pid} died before '{DONE_MARKER}'; log tail:\n"
                f"{log_tail(log_path)}")
        time.sleep(poll)
    return False


def start_server(worktree, log_path, pid_path):
    """nohup semantics: detached `./gradlew :mdk:runServer`, log + pid artifacts.

    Returns the recorded (gradle wrapper) pid — the one the phase-era segments
    wrote with `echo $! > $PIDF`. The wrapper is the tracked handle; the server
    JVM itself is reached through stop_server's RCON stop / port-owner lookup.
    """
    log_path, pid_path = Path(log_path), Path(pid_path)
    log = log_path.open("w")  # truncates, like the segments' `: > "$LOG"`
    try:
        process = subprocess.Popen(
            ["./gradlew", GRADLE_TASK],
            cwd=str(worktree),
            stdout=log,
            stderr=subprocess.STDOUT,
            start_new_session=True,  # nohup: survives this shell, own process group
        )
    finally:
        log.close()  # the child holds its own dup
    pid_path.write_text(f"{process.pid}\n", encoding="utf-8")
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


def stop_server(pid_file, rcon=None, grace=60.0, jvm_grace=15.0, term_grace=10.0):
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


def server_error_lines(log_path):
    """Count of [Server thread/ERROR] lines — the segments' closing report line."""
    try:
        text = Path(log_path).read_text(encoding="utf-8", errors="replace")
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
    args = parser.parse_args(argv)
    log_path, pid_path = artifact_paths(args.slug)
    if args.action == "status":
        print(f"log {log_path} pid {pid_path}; listening: {sorted(listening_ports())}")
        return 0
    if args.action == "provision":
        provision_run_dir(args.worktree, args.game_port, args.rcon_port,
                          args.query_port, args.password)
        return 0
    stop_server(pid_path, rcon=("127.0.0.1", args.rcon_port, args.password))
    return 0


if __name__ == "__main__":
    sys.exit(main())
