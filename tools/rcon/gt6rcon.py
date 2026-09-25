#!/usr/bin/env python3
"""gt6rcon — the single canonical RCON client of the GT6 modernization project.

Every acceptance task talks to the headless runServer through THIS module; do not
write task-local RCON scripts anymore (the five P4-era one-off scripts were distilled
into this file, their live-proven behaviours preserved).

Frame protocol (Source RCON, as exercised against the vanilla 1.20.1 dedicated server):

    +------------+------------+------------+-------------------+-----------+
    | int32 LE   | int32 LE   | int32 LE   | payload (UTF-8)   | NUL  NUL  |
    | length     | id         | type       |                   |           |
    +------------+------------+------------+-------------------+-----------+

  - length counts everything AFTER itself (id + type + payload + the two NULs).
  - type 3 = SERVERDATA_AUTH, type 2 = SERVERDATA_EXECCOMMAND; every response is
    type 2 as well (SERVERDATA_RESPONSE_VALUE / SERVERDATA_AUTH_RESPONSE).
  - auth success echoes the request id; auth failure answers id -1.
  - the vanilla server may append a stray 0-id frame after the auth response and may
    split responses into several (late) frames per command — both were observed live
    by the reference scripts, so this client drains non-matching frames and collects
    every frame matching the command id.

Dual use:
  - import:  from gt6rcon import RconClient  (or the connect/auth/run_command functions)
  - CLI:     python3 tools/rcon/gt6rcon.py --host 127.0.0.1 --port 25575 \\
                 --password gt6 "gt6pipe stat 10 64 10" --expect 1:connections

The quiet window adapts per connection (P18): most commands answer in exactly one
frame, so RconClient shrinks the collection tail after each single-frame command
(x QUIET_DECAY, floor QUIET_FLOOR) and resets it to the full DEFAULT_QUIET_WINDOW
the moment ANY second frame shows up (a truncated multi-frame body would forge
FAIL verdicts — the reset is deliberately wasteful). Zero frames = no evidence,
window unchanged. GT6_RCON_ADAPTIVE_QUIET=off (or adaptive_quiet=False) restores
the historical fixed 0.5 s tail; the module-level run_command() signature and the
judge_output() semantics are unchanged.

Exit codes: 0 ok, 1 assertion failure, 2 auth failed, 3 connection failed.
"""

import argparse
import os
import socket
import struct
import sys
import time

SERVERDATA_AUTH = 3
SERVERDATA_EXECCOMMAND = 2

AUTH_REQUEST_ID = 1
FIRST_COMMAND_ID = 100  # far above the stray 0-id frames the server emits after auth

# length field must cover id+type+NUL NUL (10 B); anything past 4 MiB means the
# stream desynced — bail out instead of trying to allocate the lie.
MIN_PACKET = 10
MAX_PACKET = 4 * 1024 * 1024

DEFAULT_CONNECT_TIMEOUT = 10.0   # reference scripts: 10 s create_connection (30 s in one)
DEFAULT_AUTH_DRAIN = 1.0         # reference scripts: 1 s drain window after the auth reply
DEFAULT_FIRST_TIMEOUT = 2.0      # reference scripts: 2.0 s deadline per command response
DEFAULT_QUIET_WINDOW = 0.5       # collects late/multi frames, then stops early on silence

# The adaptive quiet window (P18): the fixed 0.5 s tail was pure per-command
# residue for the (overwhelmingly common) single-frame response — a full-set
# sweep pays it hundreds of times. Decay on single-frame evidence, reset on any
# second frame (truncating a real multi-frame body would forge FAIL verdicts —
# conservative by design), hard floor so a hit still gets its collection tail.
QUIET_DECAY = 0.7                # single-frame response: shrink the tail by this
QUIET_FLOOR = 0.05               # never collect for less than this after a hit
ADAPTIVE_QUIET_ENV = "GT6_RCON_ADAPTIVE_QUIET"  # off/0/false/no = fixed 0.5 s window


class RconError(Exception):
    """Base class for every RCON failure this module raises."""


class RconAuthError(RconError):
    """The server answered id -1 to the auth request (wrong password)."""


def recv_exact(sock, count):
    """Read exactly count bytes; EOF mid-frame is a hard connection error."""
    buf = b""
    while len(buf) < count:
        chunk = sock.recv(count - len(buf))
        if not chunk:
            raise ConnectionError("connection closed mid-frame")
        buf += chunk
    return buf


def send_packet(sock, req_id, ptype, payload):
    """Frame and send one packet; payload is raw bytes."""
    body = struct.pack("<ii", req_id, ptype) + payload + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(body)) + body)


def read_packet(sock):
    """Read one frame; return (id, type, payload-as-str). Raises on desync/EOF."""
    (length,) = struct.unpack("<i", recv_exact(sock, 4))
    if not MIN_PACKET <= length <= MAX_PACKET:
        raise RconError(f"insane frame length {length} — stream desynced")
    body = recv_exact(sock, length)
    rid, rtype = struct.unpack("<ii", body[:8])
    payload = body[8:-2]  # strip the two trailing NULs
    return rid, rtype, payload.decode("utf-8", "replace")


def connect(host, port=25575, timeout=DEFAULT_CONNECT_TIMEOUT):
    """Open the TCP connection; returns the socket (timeout applies to all reads)."""
    return socket.create_connection((host, port), timeout=timeout)


def auth(sock, password, drain_window=DEFAULT_AUTH_DRAIN):
    """SERVERDATA_AUTH handshake. Raises RconAuthError on id -1.

    After the auth reply the vanilla server may push a stray 0-id frame; drain it
    within drain_window seconds (observed live — leaving it in the stream would
    pollute the first command's read).
    """
    send_packet(sock, AUTH_REQUEST_ID, SERVERDATA_AUTH, password.encode("utf-8"))
    rid, _, _ = read_packet(sock)
    if rid == -1:
        raise RconAuthError("server rejected the password (id -1)")
    if drain_window > 0:
        deadline = time.monotonic() + drain_window
        while True:
            sock.settimeout(max(0.01, deadline - time.monotonic()))
            try:
                read_packet(sock)
            except (socket.timeout, TimeoutError):
                break
            except (ConnectionError, RconError, OSError):
                break
            if time.monotonic() >= deadline:
                break
    return rid


# The wire lock (optional, set by the concurrent session executor): vanilla's
# DedicatedServer.runCommand (DedicatedServer.java:517-521) routes EVERY rcon
# command through ONE server-wide RconConsoleSource — prepareForCommand() clears
# the shared output buffer, the command writes into it, getCommandResponse()
# reads it back. Two concurrent rcon connections therefore cross-contaminate
# responses (observed live 2026-09-04: chain A's stat step answered with chain
# B's response text, both commands logged executed). Holding this lock from
# send until THIS connection's first response frame keeps the shared-source
# cycle atomic per command; the quiet-window collection tail that follows runs
# outside the lock, so the client-side overlap — the concurrency payoff — is
# preserved. Never set for the CLI / per-chain boot / serial session paths.
_WIRE_LOCK = None


def set_wire_lock(lock):
    """Install (or clear with None) the cross-thread rcon wire lock."""
    global _WIRE_LOCK
    _WIRE_LOCK = lock


def run_command(sock, command, req_id,
                first_timeout=DEFAULT_FIRST_TIMEOUT, quiet_window=DEFAULT_QUIET_WINDOW):
    """SERVERDATA_EXECCOMMAND; return every response payload for req_id as a list.

    Responses can arrive split and late (live observation), so: wait up to
    first_timeout for the first matching frame, then keep collecting for up to
    quiet_window of silence. Non-matching frames (strays, late neighbours) are
    discarded rather than misattributed. With the wire lock installed, the lock
    is held from the send until the first matching frame — the proof that the
    server's shared RconConsoleSource cycle has completed for this command.
    """
    locked = _WIRE_LOCK is not None
    if locked:
        _WIRE_LOCK.acquire()
    first_matched = False
    try:
        send_packet(sock, req_id, SERVERDATA_EXECCOMMAND, command.encode("utf-8"))
        outs = []
        deadline = time.monotonic() + first_timeout
        quiet_deadline = None
        while True:
            remaining = (deadline if quiet_deadline is None else quiet_deadline) \
                - time.monotonic()
            if remaining <= 0:
                break
            sock.settimeout(remaining)
            try:
                rid, _, out = read_packet(sock)
            except (socket.timeout, TimeoutError):
                continue
            except (ConnectionError, OSError):
                break
            if rid == req_id and out:
                if locked and not first_matched:
                    _WIRE_LOCK.release()
                    locked = False
                first_matched = True
                outs.append(out)
                # after the first hit, stop at the next quiet_window of silence
                quiet_deadline = time.monotonic() + quiet_window
        return outs
    finally:
        if locked:
            _WIRE_LOCK.release()


def adaptive_quiet_enabled():
    """True unless GT6_RCON_ADAPTIVE_QUIET is off/0/false/no — the one-switch
    fallback to the historical fixed-window behaviour. Checked per RconClient
    construction, so tests and long-lived processes can pin either mode."""
    return os.environ.get(ADAPTIVE_QUIET_ENV, "on").strip().lower() \
        not in ("off", "0", "false", "no")


def next_quiet_window(current, frames, reset=DEFAULT_QUIET_WINDOW,
                      decay=QUIET_DECAY, floor=QUIET_FLOOR):
    """The adaptive quiet window after one command (pure; the selftest pins it).

    One frame: the tail was residue — decay it, never below the floor, and
    never RAISE a window that was configured below the floor. Any second frame
    (or third...): reset to the full window — a truncated multi-frame body
    would forge FAIL verdicts, so the reset is deliberately wasteful (card
    rcon-sweep-quietwin: better safe than aggressive). Silence (zero
    frames): no evidence about lateness, keep the window unchanged.
    """
    if frames >= 2:
        return reset
    if frames == 0:
        return current
    return min(current, max(floor, current * decay))


class RconClient:
    """Light connection object bundling connect/auth/run_command for reuse.

    Holds the adaptive quiet-window state per connection (P18): concurrency
    gives every chain its own connection (framework._run_wave), so the state
    is thread-self-contained and converges on what THIS socket's server
    actually does.
    """

    def __init__(self, host="127.0.0.1", port=25575, password="",
                 timeout=DEFAULT_CONNECT_TIMEOUT,
                 first_timeout=DEFAULT_FIRST_TIMEOUT, quiet_window=DEFAULT_QUIET_WINDOW,
                 adaptive_quiet=None):
        self.host = host
        self.port = port
        self.password = password
        self.timeout = timeout
        self.first_timeout = first_timeout
        self.quiet_window = quiet_window
        self._quiet_reset = quiet_window   # decay resets to the CONFIGURED window
        self.adaptive_quiet = adaptive_quiet_enabled() if adaptive_quiet is None \
            else bool(adaptive_quiet)
        self._sock = None
        self._next_id = FIRST_COMMAND_ID

    def connect(self):
        self._sock = connect(self.host, self.port, self.timeout)
        return self._sock

    def auth(self):
        return auth(self._sock, self.password)

    def run_command(self, command):
        if self._sock is None:
            raise RconError("not connected")
        req_id = self._next_id
        self._next_id += 1
        outs = run_command(self._sock, command, req_id,
                           self.first_timeout, self.quiet_window)
        if self.adaptive_quiet:
            self.quiet_window = next_quiet_window(self.quiet_window, len(outs),
                                                  reset=self._quiet_reset)
        return outs

    def close(self):
        if self._sock is not None:
            try:
                self._sock.close()
            finally:
                self._sock = None

    def __enter__(self):
        self.connect()
        self.auth()
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close()
        return False


def judge_output(index, body, expected=None, allowed=False):
    """Print and score one command's output; return 1 when it counts as a failure.

    The single verdict printer shared by run_chain (CLI) and the chains
    framework (tools/rcon/chains/) so both layers report identically: a literal
    "FAILED" marker in the body and a missed expectation each count, an
    `allowed` command reports ALLOWED instead and does not count.
    """
    failure = 0
    if "FAILED" in body:
        if allowed:
            print(f"[command {index}: FAILED marker in output -> ALLOWED]")
        else:
            print(f"[command {index}: FAILED marker in output -> FAIL]")
            failure += 1
    if expected is not None:
        if expected in body:
            print(f"[expect {index}: {expected!r} -> PASS]")
        elif allowed:
            print(f"[expect {index}: {expected!r} -> ALLOWED]")
        else:
            print(f"[expect {index}: {expected!r} -> FAIL]")
            failure += 1
    return failure


def run_chain(host, port, password, commands, expects=None,
              timeout=DEFAULT_CONNECT_TIMEOUT,
              first_timeout=DEFAULT_FIRST_TIMEOUT, quiet_window=DEFAULT_QUIET_WINDOW,
              allow_failed=None):
    """Connect, auth, run the command chain, apply index-keyed expectations.

    expects maps 1-based command index -> expected substring. allow_failed is an
    optional collection of 1-based command indices that MAY fail: a FAILED marker
    or a missed expectation on a marked command is reported as ALLOWED instead of
    FAIL and does not count into the returned failure count (the chain keeps
    executing either way — a coexistence chain pins an expected-REJECTED step in
    the middle without failing the whole acceptance). Default None/empty = every
    failure counts, the historical behaviour byte for byte.

    Returns (failure_count, [(command, [outputs]), ...]). Every output containing
    the literal marker "FAILED" counts as a failure — the GT6 acceptance commands
    send their failure lines with exactly that word.
    """
    allowed = set(allow_failed) if allow_failed else set()
    failure = 0
    transcript = []
    with RconClient(host, port, password, timeout,
                    first_timeout=first_timeout, quiet_window=quiet_window) as client:
        for index, command in enumerate(commands, start=1):
            outs = client.run_command(command)
            transcript.append((command, outs))
            body = "\n".join(outs)
            print(f"$ {command}\n{body if body else '<no response>'}")
            failure += judge_output(index, body, (expects or {}).get(index),
                                    index in allowed)
    return failure, transcript


def _parse_expect(spec):
    head, sep, substring = spec.partition(":")
    if not sep:
        raise ValueError(f"--expect must be N:SUBSTRING, got {spec!r}")
    index = int(head)
    if index < 1:
        raise ValueError(f"--expect index must be >= 1, got {spec!r}")
    return index, substring


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="gt6rcon",
        description="GT6 canonical RCON client: run one command or a chain against "
                    "the headless runServer and assert on the responses.")
    parser.add_argument("--host", default="127.0.0.1", help="server host (default 127.0.0.1)")
    parser.add_argument("--port", type=int, default=25575, help="rcon.port (default 25575)")
    parser.add_argument("--password", required=True, help="rcon.password of server.properties")
    parser.add_argument("--timeout", type=float, default=DEFAULT_CONNECT_TIMEOUT,
                        help=f"connect/auth socket timeout (default {DEFAULT_CONNECT_TIMEOUT:g})")
    parser.add_argument("--response-timeout", type=float, default=DEFAULT_FIRST_TIMEOUT,
                        dest="response_timeout",
                        help=f"per-command response deadline in s (default {DEFAULT_FIRST_TIMEOUT:g}; "
                             "raise it for slow commands like 'gt6oven run <big>')")
    parser.add_argument("--expect", action="append", default=[], metavar="N:SUBSTRING",
                        help="1-based command index and expected substring, repeatable; "
                             "a command without expectation is just printed")
    parser.add_argument("--allow-failed", action="append", type=int, default=[], metavar="N",
                        dest="allow_failed",
                        help="1-based command index that MAY fail, repeatable: a FAILED "
                             "marker or a missed --expect on it is reported as ALLOWED "
                             "instead of FAIL and does not fail the chain (coexistence "
                             "chains pin an expected-REJECTED step in the middle)")
    parser.add_argument("commands", nargs="+", help="server commands, run in order")
    args = parser.parse_args(argv)

    try:
        expects = dict(_parse_expect(spec) for spec in args.expect)
    except ValueError as exc:
        parser.error(str(exc))
    if any(n < 1 for n in args.allow_failed):
        parser.error("--allow-failed index must be >= 1")
    if args.allow_failed and max(args.allow_failed) > len(args.commands):
        parser.error(f"--allow-failed index out of range 1..{len(args.commands)}")

    try:
        failure, _ = run_chain(args.host, args.port, args.password,
                               args.commands, expects, args.timeout,
                               first_timeout=args.response_timeout,
                               allow_failed=args.allow_failed)
    except RconAuthError:
        print("AUTH FAILED")
        return 2
    except (ConnectionError, OSError) as exc:
        print(f"CONNECT FAILED: {exc}")
        return 3
    return 1 if failure else 0


if __name__ == "__main__":
    sys.exit(main())
