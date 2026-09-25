#!/usr/bin/env python3
"""selftest — serverless verification of the framework's P17 fixes (no boot).

Every check runs in-process with the boot/RCON surface faked, so the whole
suite is a ~1s dry run usable in any worktree:

  python3 tools/rcon/selftest.py        # exit 0 = all checks green

Covered (one section per P16-closeout framework defect, card
rcon-framework-fixes):

  1 chain.node write-back — GT6_SESSION=off run() through a faked boot with
    --node 1.21.1-neoforge: the chain must come out stamped AND the captured
    merge command must carry the 21.1 {id,amount} key shape, not the forge
    {FluidName,Amount} shape (the P16 side_io 21.1 mis-merge, Deviations 2).
    Function-level stand-in for a live 21.1 boot: the exact step_cmd() wire
    text is asserted, which is what the server would receive.
  2 session artifact identity — slug carries the chain roster + a worktree
    tag; the bare session_<node> name is gone; long rosters fold stably.
  3 session port policy — a declared chain pins the boot triple (identical
    to its per-chain run() triple); defaults/disagreements fall back to the
    SESSION_PORTS node segments (--dual separation, ADR-P15-4).
  4 sweep registry — the eight P16 chains are registered as one cluster.
  5 boot-ownership gates — ports-free trips on a real LISTEN socket; the
    pid-file gate accepts only THIS boot's pid.
  6 sweep result worktree isolation (P18, card rcon-sweep-quietwin) —
    result_path names carry this worktree's tag by default and diverge under
    a foreign tag (the --dual reader's other-side lookup).
  7 adaptive quiet window (P18, same card) — the pure convergence logic:
    single-frame decay to a hard floor, reset on ANY second frame, silence
    keeps the window, and the GT6_RCON_ADAPTIVE_QUIET off-switch parses.
  8 perboot exit aggregate (P18, this card) — run_and_record's result carries
    the top-level "exit" key the --dual reader consumes, the _failed
    aggregation matches main()'s non-dual exit code, and the session model's
    framework-computed exit survives the setdefault untouched.
  9 wait_done monotonic window (P19, card rcon-waitdone-8kb) — the old
    last-8-KiB tail poll faked a boot timeout once >8 KiB scrolled past the
    `Done (` marker, and the dead-pid diagnostic lost the crash ERROR line to
    the same window. Real temp-file logs pin the old-code failure shapes (the
    marker / the ERROR line verifiably absent from log_tail) next to the new
    behaviour: monotonic delta scan finds the beyond-window marker, a marker
    split across read boundaries still matches, a marker landing between
    polls ends the wait early, the dead-pid error attributes from the whole
    log, and "no marker anywhere" still times out False.
"""

import io
import json
import os
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE / "chains")):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import framework
import gt6server
import gt6rcon
import gt6world

# The real wait_done, captured BEFORE any _install_fakes() swap (the fakes are
# process-global and restored by design "never") — section 9 exercises the
# genuine polling loop, not the lambda-True stand-in sections 1/8 run on.
REAL_WAIT_DONE = gt6server.wait_done
# Same for the real stop path: section 11 exercises the genuine P30 ownership
# gate, which the section-1 fake (`stop_server = lambda: {"faked": True}`)
# would otherwise swallow for every later section.
REAL_STOP_SERVER = gt6server.stop_server
# And the real port pick: section 15 exercises the genuine segment stagger
# (pool-port-stagger), not the section-1 `pick_ports = lambda` stand-in.
REAL_PICK_PORTS = gt6server.pick_ports
# Section 8 also swaps assert_ports_free and never restores; section 15's
# backstop check needs the genuine P17 gate.
REAL_ASSERT_PORTS_FREE = gt6server.assert_ports_free

P16_STEMS = ("pattern_checker", "aqua_fluids", "side_io",
             "machine_fluid_gui", "drying_rows", "form_scaffold",
             "chisel_decalcify", "distillery")

FAILURES = []


def check(name, condition, detail=""):
    status = "PASS" if condition else "FAIL"
    print(f"[selftest] {status} {name}" + (f" — {detail}" if detail else ""))
    if not condition:
        FAILURES.append(name)


# ----------------------------------------------------------------- fakes

class FakeClient:
    """RconClient stand-in: records every command, answers a benign body."""

    def __init__(self, *args, **kwargs):
        FakeClient.sent.append(self)

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False

    def run_command(self, cmd):
        FakeClient.commands.append(cmd)
        return ["ok"]


FakeClient.sent = []
FakeClient.commands = []


def _install_fakes():
    """Swap the boot surface for fakes; restore nothing (single-shot process)."""
    gt6server.pick_ports = lambda preferred, count=1: list(preferred)
    gt6server.provision_run_dir = lambda *a, **k: None
    gt6server.start_server = lambda *a, **k: 424242
    gt6server.wait_done = lambda *a, **k: True
    gt6server.stop_server = lambda *a, **k: {"faked": True}
    gt6server.server_error_lines = lambda *a, **k: 0
    gt6rcon.RconClient = FakeClient
    real_sleep = time.sleep
    time.sleep = lambda seconds: None  # the whole process runs fake-paced
    return real_sleep


def _merge_chain():
    """A minimal chain with the forge/21.1 fluid merge key-shape fork."""
    return framework.Chain(
        name="selftest-merge", slug="selftestmerge",
        sites=gt6world.declare_sites(gt6world.Site(0, 64, 0)),
        passes=1,
        steps=[framework.Step(
            'data merge block 0 64 0 {FluidName:"minecraft:water",Amount:920}',
            node_cmds={"1.21.1":
                       'data merge block 0 64 0 {id:"minecraft:water",amount:920}'})],
    )


def check_1_chain_node_writeback():
    print("\n--- 1: chain.node write-back (GT6_SESSION=off run path)")
    real_sleep = _install_fakes()
    chain = _merge_chain()
    old_argv, sys.argv = sys.argv, ["selftest", "--node", "1.21.1-neoforge"]
    old_env_off = __import__("os").environ.get("GT6_SESSION")
    __import__("os").environ["GT6_SESSION"] = "off"
    try:
        code = framework.run(chain)
    finally:
        sys.argv = old_argv
        if old_env_off is None:
            __import__("os").environ.pop("GT6_SESSION", None)
        else:
            __import__("os").environ["GT6_SESSION"] = old_env_off
        time.sleep = real_sleep
    merge_cmds = [c for c in FakeClient.commands if "data merge" in c]
    check("1a run() stamps chain.node", chain.node == "1.21.1-neoforge",
          f"chain.node={chain.node!r}")
    check("1b exit 0", code == 0)
    check("1c merge wire text is the 21.1 {id,amount} shape",
          len(merge_cmds) == 1 and '{id:"minecraft:water",amount:920}' in merge_cmds[0],
          repr(merge_cmds))
    check("1d forge {FluidName,Amount} shape NOT sent on 21.1",
          all("FluidName" not in c for c in merge_cmds))

    # the default-node control: no --node, unstamped chain stays on 1.20.1
    chain2 = _merge_chain()
    old_argv, sys.argv = sys.argv, ["selftest"]
    try:
        framework.run(chain2)
    finally:
        sys.argv = old_argv
    forge_cmds = [c for c in FakeClient.commands if "FluidName" in c]
    check("1e default node keeps forge shape",
          chain2.node == framework.DEFAULT_NODE and len(forge_cmds) >= 1)


def check_2_session_slug():
    print("\n--- 2: session artifact identity")
    a = framework.Chain(name="a", slug="dryrows",
                        preferred_ports=(25775, 25785))
    b = framework.Chain(name="b", slug="af")
    slug = framework.session_slug([a, b], "1.20.1-forge")
    check("2a roster in slug", "af+dryrows" in slug, slug)
    check("2b node suffix in slug", slug.startswith("session_1201-forge_"), slug)
    check("2c worktree tag in slug",
          slug.endswith("-" + framework.worktree_tag()), slug)
    check("2d bare historical name retired",
          slug != f"session_{framework.node_suffix('1.20.1-forge')}", slug)
    big = [framework.Chain(name=f"c{i}", slug=f"slug{i:02d}") for i in range(27)]
    long1, long2 = (framework.session_slug(big, "1.21.1-neoforge") for _ in range(2))
    check("2e long roster folds deterministically, bounded",
          long1 == long2 and len(long1) < 80, f"{long1} ({len(long1)} chars)")
    check("2f distinct rosters -> distinct slugs",
          framework.session_slug([a], "1.20.1-forge")
          != framework.session_slug([b], "1.20.1-forge"))


def check_3_session_ports():
    print("\n--- 3: session port policy")
    declared = framework.Chain(name="d", slug="d",
                               preferred_ports=(25775, 25785))
    declared_game = framework.Chain(name="dg", slug="dg",
                                    preferred_ports=(25717, 25727),
                                    game_port=25707)
    default = framework.Chain(name="f", slug="f")  # the bare dataclass default
    other = framework.Chain(name="o", slug="o", preferred_ports=(25776, 25786))
    check("3a one declared chain pins the boot triple",
          framework.session_ports([declared], "1.20.1-forge") == (25775, 25785, 25765))
    check("3b declared triple == the per-chain run() triple",
          framework.session_ports([declared], "1.20.1-forge")
          == tuple(gt6server.pick_ports(
              declared.preferred_ports + (declared.preferred_ports[0] - 10,))))
    check("3c declared game_port honored",
          framework.session_ports([declared_game], "1.20.1-forge")
          == (25717, 25727, 25707))
    check("3d no declaration -> SESSION_PORTS node segment (21.1)",
          framework.session_ports([default], "1.21.1-neoforge")
          == framework.SESSION_PORTS["1.21.1"])
    check("3e disagreeing declarations -> node segment fallback (--dual safe)",
          framework.session_ports([declared, other], "1.20.1-forge")
          == framework.SESSION_PORTS["1.20.1"])
    check("3f silent+declared mix -> the one declaration anchors",
          framework.session_ports([declared, default], "1.21.1-neoforge")
          == (25775, 25785, 25765))


def check_4_sweep_registry():
    print("\n--- 4: sweep registry carries the P16 cluster")
    import sweep
    stems = sweep.ordered_stems()
    missing = [stem for stem in P16_STEMS if stem not in stems]
    check("4a all eight P16 stems registered", not missing, f"missing={missing}")
    clusters = [group for group in sweep.SESSION_GROUPS
                if set(P16_STEMS) <= set(group)]
    check("4b the eight form exactly one cluster", len(clusters) == 1
          and set(clusters[0]) == set(P16_STEMS))


def check_5_ownership_gates():
    print("\n--- 5: boot-ownership gates")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        sock.listen(1)
        busy = sock.getsockname()[1]
        try:
            gt6server.assert_ports_free((busy,), label="selftest")
            check("5a ports-free trips on a real LISTEN socket", False)
        except gt6server.BootOwnershipError as exc:
            check("5a ports-free trips on a real LISTEN socket", True, str(exc)[:70])
    check("5b ports-free passes on free ports",
          gt6server.assert_ports_free((45998, 45999), label="selftest"))
    pid_file = Path(tempfile.mkdtemp()) / "selftest.pid"
    pid_file.write_text("424242\n")
    check("5c pid gate accepts THIS boot's pid",
          gt6server.assert_pid_file(pid_file, 424242, label="selftest"))
    pid_file.write_text("999\n")
    try:
        gt6server.assert_pid_file(pid_file, 424242, label="selftest")
        check("5d pid gate rejects a foreign pid", False)
    except gt6server.BootOwnershipError as exc:
        check("5d pid gate rejects a foreign pid", True, str(exc)[:70])
    pid_file.write_text("junk\n")
    try:
        gt6server.assert_pid_file(pid_file, 424242, label="selftest")
        check("5e pid gate rejects garbage", False)
    except gt6server.BootOwnershipError:
        check("5e pid gate rejects garbage", True)


def check_6_sweep_result_isolation():
    print("\n--- 6: sweep result JSON worktree isolation (P18)")
    import sweep
    mine = sweep.result_path("session", "1.20.1-forge", 2)
    check("6a default result_path carries THIS worktree's tag",
          mine.name.endswith(f"_{framework.worktree_tag()}.json"), mine.name)
    check("6b perboot label keeps the node-suffix shape",
          sweep.result_path("perboot", "1.20.1-forge").name
          == f"gt6_rs_sweep_perboot_1201-forge_{framework.worktree_tag()}.json")
    foreign = sweep.result_path("session", "1.20.1-forge", 2, tag="0123abcd")
    check("6c foreign-tag name differs (parallel worktrees / dual legs never collide)",
          foreign != mine and "0123abcd" in foreign.name, foreign.name)
    check("6d local tag helper == framework.worktree_tag() on this root",
          sweep._worktree_tag(framework.WORKTREE_ROOT) == framework.worktree_tag())


def check_7_adaptive_quiet_window():
    print("\n--- 7: adaptive quiet window (P18, pure convergence logic)")
    full = gt6rcon.DEFAULT_QUIET_WINDOW
    floor, decay = gt6rcon.QUIET_FLOOR, gt6rcon.QUIET_DECAY
    check("7a single frame decays the tail",
          gt6rcon.next_quiet_window(full, 1) == full * decay)
    check("7b decay never crosses the floor and never raises a smaller window",
          gt6rcon.next_quiet_window(floor, 1) == floor
          and gt6rcon.next_quiet_window(0.02, 1) == 0.02)
    check("7c any second frame resets to the full (configured) window",
          gt6rcon.next_quiet_window(floor, 2) == full
          and gt6rcon.next_quiet_window(floor, 3, reset=0.3) == 0.3)
    check("7d silence (0 frames) keeps the window unchanged",
          gt6rcon.next_quiet_window(0.11, 0) == 0.11)
    window = full
    for _ in range(40):
        window = gt6rcon.next_quiet_window(window, 1)
    check("7e repeated single-frame decay converges to the floor and stays",
          window == floor)
    old = os.environ.get(gt6rcon.ADAPTIVE_QUIET_ENV)
    try:
        os.environ[gt6rcon.ADAPTIVE_QUIET_ENV] = "off"
        disabled = gt6rcon.adaptive_quiet_enabled()
        os.environ[gt6rcon.ADAPTIVE_QUIET_ENV] = ""
        enabled = gt6rcon.adaptive_quiet_enabled()
    finally:
        if old is None:
            os.environ.pop(gt6rcon.ADAPTIVE_QUIET_ENV, None)
        else:
            os.environ[gt6rcon.ADAPTIVE_QUIET_ENV] = old
    check("7f env switch: 'off' disables, empty/unset keeps adaptive on",
          disabled is False and enabled is True)


def check_8_perboot_exit_aggregate():
    """The --dual reader's top-level exit key on the perboot shape (P18).

    run_dual consumed mine_json["exit"], but run_perboot's result carries only
    per-chain exits — a perboot --dual leg KeyError'd at the very end of a
    full sweep (tmp.P18.pool: sweep-run-dual-perboot-exit-keyerror). The fix
    aggregates the exit in run_and_record via setdefault, so the session
    model's framework-computed exit (framework.run_session_recorded) stays
    untouched and perboot records gain the same _failed semantics main() uses
    for the non-dual exit code.
    """
    print("\n--- 8: perboot result carries the --dual exit key (P18)")
    import argparse
    import sweep
    real_sleep = _install_fakes()
    gt6server.assert_ports_free = lambda *a, **k: True
    ok_roster = [framework.Chain(
        name="selftest-ok", slug="sok",
        sites=gt6world.declare_sites(gt6world.Site(0, 64, 0)),
        steps=[framework.Step("say hi", expect="ok")])]
    bad_roster = ok_roster + [framework.Chain(
        name="selftest-bad", slug="sbad",
        sites=gt6world.declare_sites(gt6world.Site(0, 64, 0)),
        steps=[framework.Step("say boom", expect="NOPE")])]
    real_select, real_load_chain, real_path = \
        sweep.select, sweep.load_chain, sweep.result_path
    scratch = Path(tempfile.mkdtemp())
    ok_chain, bad_chain = ok_roster[0], bad_roster[1]
    fake_chains = {"selftest-ok": ok_chain, "selftest-bad": bad_chain}
    sweep.select = lambda stems, only: ["selftest-ok"]
    sweep.load_chain = lambda stem: fake_chains[stem]
    sweep.result_path = lambda mode, node, concurrency=1, tag=None: \
        scratch / f"selftest_sweep_{mode}.json"
    try:
        args = argparse.Namespace(mode="perboot", node="1.20.1-forge",
                                  concurrency=1, only=None)
        green = sweep.run_and_record(args)
        check("8a green perboot run: top-level exit key present and 0",
              green.get("exit") == 0, f"exit={green.get('exit')!r}")
        check("8b the recorded JSON carries the exit key (--dual's other_res read)",
              '"exit": 0' in (scratch / "selftest_sweep_perboot.json")
              .read_text(encoding="utf-8"))
        sweep.select = lambda stems, only: ["selftest-ok", "selftest-bad"]
        red = sweep.run_and_record(args)
        check("8c red perboot run: top-level exit key present and 1",
              red.get("exit") == 1, f"exit={red.get('exit')!r}")
        try:
            code = red["exit"] | green.get("exit", 1)
            check("8d run_dual's reader expression evaluates on the perboot shape",
                  code == 1)
        except KeyError as exc:
            check("8d run_dual's reader expression evaluates on the perboot shape",
                  False, f"KeyError {exc}")
        sess_args = argparse.Namespace(mode="session", node="1.20.1-forge",
                                       concurrency=1, only=None)
        sess = sweep.run_and_record(sess_args)
        check("8e session model keeps its framework-computed exit (setdefault no-op)",
              sess.get("exit") == 1, f"exit={sess.get('exit')!r}")
    finally:
        sweep.select, sweep.load_chain, sweep.result_path = \
            real_select, real_load_chain, real_path
        time.sleep = real_sleep


def _reaped_pid():
    """A pid that is genuinely gone: spawn + reap a trivial child (the selftest
    runs no servers and kills nothing — the child exits and is waited on)."""
    child = subprocess.Popen(["true"])
    child.wait()
    return child.pid


def _scroll_spam(count, tag):
    """Fast-scrolling boot filler: ~56 B lines, none of them error-ish."""
    base = f"[Server thread/INFO]: scroll padding {tag} "
    return "".join(f"{base}{index:05d} 0123456789abcdef\n" for index in range(count))


def check_9_waitdone_monotonic_window():
    """P19: wait_done's old fixed 8 KiB tail window on a sick boot.

    Two failure forms, both pinned against the real old-code primitive
    (`DONE_MARKER in log_tail(...)` / the log_tail-built diagnostic) on real
    temp-file logs, then proven fixed: ① a `Done (` printed and scrolled
    beyond the last 8 KiB by later output faked a timeout ("never printed
    Done" about a server that was up); ② a crash ERROR line beyond the same
    window vanished from the dead-pid ServerStartError diagnostic.
    """
    print("\n--- 9: wait_done monotonic window (P19, sick-boot forms)")
    scratch = Path(tempfile.mkdtemp())
    marker_line = "Done (3.596s)! For help, type \"help\"\n"

    # form 1: the marker, then >8 KiB of post-Done scroll (sick-but-alive boot)
    done_log = scratch / "done_beyond_window.log"
    done_log.write_text("[main/INFO]: starting\n" + marker_line
                        + _scroll_spam(600, "post"), encoding="utf-8")
    check("9a pin (old code): marker in the log but beyond the 8 KiB tail window",
          gt6server.DONE_MARKER in done_log.read_text(encoding="utf-8")
          and gt6server.DONE_MARKER not in gt6server.log_tail(done_log))
    check("9b fix: wait_done finds the beyond-window marker",
          REAL_WAIT_DONE(done_log, timeout=5.0, poll=0.05) is True)

    # the carry: a marker split across two reads must still match
    split_log = scratch / "split_marker.log"
    split_log.write_text("[main/INFO]: boot Don", encoding="utf-8")
    chunk1, offset1, carry1 = gt6server._new_log_bytes(split_log, 0)
    check("9c split marker: first read lacks it and the carry keeps its bytes",
          gt6server.DONE_MARKER not in chunk1 and carry1.endswith(b" Don"),
          f"carry={carry1!r}")
    with split_log.open("a", encoding="utf-8") as handle:
        handle.write("e (2.1s)! For help\n")
    chunk2, _, _ = gt6server._new_log_bytes(split_log, offset1, carry1)
    check("9d split marker: the carried delta completes the match",
          gt6server.DONE_MARKER in chunk2)

    # a marker landing BETWEEN polls ends the wait early (the loop iterates
    # and accumulates; it must not judge the log once and give up)
    late_log = scratch / "late_marker.log"
    late_log.write_text(_scroll_spam(10, "early"), encoding="utf-8")

    def _append_marker_late():
        time.sleep(0.2)
        with late_log.open("a", encoding="utf-8") as handle:
            handle.write(marker_line)

    appending = threading.Thread(target=_append_marker_late, daemon=True)
    appending.start()
    started = time.monotonic()
    found = REAL_WAIT_DONE(late_log, timeout=5.0, poll=0.05)
    elapsed = time.monotonic() - started
    check("9e marker landing between polls is caught by the delta loop",
          found is True and elapsed < 2.5, f"elapsed {elapsed:.2f}s of 5s")
    appending.join(timeout=1.0)

    # form 2: a crash ERROR line, then >8 KiB of post-crash scroll (gradle
    # epilogue) — the old diagnostic's log_tail lost the attribution
    crash_log = scratch / "error_beyond_window.log"
    crash_line = ("[main/ERROR] Failed to start the minecraft server "
                  "GT6RCONCRASH42\n")
    crash_log.write_text("[main/INFO]: booting\n" + crash_line
                         + _scroll_spam(600, "epilogue"), encoding="utf-8")
    check("9f pin (old code): crash ERROR line beyond the 8 KiB tail window",
          "GT6RCONCRASH42" in crash_log.read_text(encoding="utf-8")
          and "GT6RCONCRASH42" not in gt6server.log_tail(crash_log))
    try:
        REAL_WAIT_DONE(crash_log, timeout=5.0, poll=0.05,
                            pid=_reaped_pid())
        check("9g dead pid before Done raises ServerStartError", False)
    except gt6server.ServerStartError as exc:
        message = str(exc)
        check("9g dead pid before Done raises ServerStartError", True)
        check("9h fix: attribution names the beyond-window ERROR line "
              "(whole-log error_tail, tail kept for the death scene)",
              "GT6RCONCRASH42" in message and "--- plain tail:" in message,
              message[:90])

    # the contract keeps its negative: no marker anywhere + live pid -> False
    check("9i genuinely markerless log still times out False (contract kept)",
          REAL_WAIT_DONE(scratch / "absent.log", timeout=0.2, poll=0.05,
                              pid=os.getpid()) is False)


def check_10_node_expects_fork():
    """The node_expects per-node expect fork (the P23 pchk rendering drift).

    The 21.1 oven report namespaces the item names the 1.20.1 leg renders
    plain, so the spanning terminal expect is per-leg (Step.node_expects).
    Pinned twice: step_expect resolves the override by node_key exactly like
    step_cmd does, and the judged run actually forks — the RIGHT leg's expect
    passes the 21.1-rendered body while the WRONG leg's expect goes red (the
    fork is not a no-op). The mid-run shadow probe ('stonex8' is a substring
    of input=cobblestonex8 and hits at zero seconds) stays out by construction:
    both full lines name the input field.
    """
    print("\n--- 10: node_expects per-node expect fork (P23 pchk rendering drift)")
    forge_line = "input=airx0 output=stonex8"
    neo_line = "input=minecraft:airx0 output=minecraft:stonex8"
    step = framework.Step("gt6oven check 400 64 400", expect=forge_line,
                          node_expects={"1.21.1": neo_line})
    check("10a resolution: override on 1.21.1, plain expect elsewhere",
          framework.step_expect(step, "1.21.1-neoforge") == neo_line
          and framework.step_expect(step, "1.20.1-forge") == forge_line
          and framework.step_expect(step, None) == forge_line)

    class _OvenFake:
        """Answers every command with the 21.1-rendered terminal oven report."""

        def __init__(self, *args, **kwargs):
            pass

        def __enter__(self):
            return self

        def __exit__(self, *exc):
            return False

        def run_command(self, cmd):
            return [f"GT6 oven at 400, 64, 400: {neo_line} "
                    "data=20480 progress=0/0 energy=0 minenergy=0"]

    real_sleep = _install_fakes()
    gt6rcon.RconClient = _OvenFake
    chain = framework.Chain(
        name="selftest-nodeexp", slug="selftestnodeexp",
        sites=gt6world.declare_sites(gt6world.Site(400, 64, 400)),
        passes=1, steps=[step])
    old_argv, sys.argv = sys.argv, ["selftest"]
    results = {}
    try:
        for node in ("1.21.1-neoforge", "1.20.1-forge"):
            sys.argv = ["selftest", "--node", node]
            chain.node = None   # run_session write-backs it; reset per leg
            results[node] = framework.run_session([chain])
    finally:
        sys.argv = old_argv
        time.sleep = real_sleep
    check("10b right leg's expect passes the 21.1-rendered report",
          results["1.21.1-neoforge"] == 0)
    check("10c wrong leg's expect goes red (the fork actually swaps the judge)",
          results["1.20.1-forge"] == 1)


def _ownership_fixture(scratch, name, owner, legacy=False):
    """A pid_file + slot marker + slot record shaped exactly like start_server
    leaves them. The recorded pid is genuinely gone, so an ALLOWED stop
    returns immediately instead of waiting out the grace window."""
    pid_file = scratch / f"{name}.pid"
    pid_file.write_text(f"{_reaped_pid()}\n", encoding="utf-8")
    slot = scratch / f"{name}.slot.record"
    slot.write_text(f"{time.time()}\n" + ("" if legacy else f"owner {owner}\n"),
                    encoding="utf-8")
    Path(str(pid_file) + ".slot").write_text(f"{slot}\n", encoding="utf-8")
    return pid_file, slot


def check_11_stop_ownership():
    """The P30 stop-ownership gate (ops.rcon-chain-repair: a foreign
    session's cleanup RCON-stopped a live server through the shared default
    password — stop had no owner check). start_server stamps
    caller_identity() into the slot record; stop_server resolves the record
    via the <pid_file>.slot marker and refuses anyone who is neither the
    boot process nor the same worktree session; --force is the escape.
    Serverless end to end: the RCON surface is FakeClient and the slot
    namespace is swapped to a scratch dir, so acquire/reap/gate never touch
    the shared-box state (and never queue on a full slot set).
    """
    print("\n--- 11: stop-ownership gate (P30)")
    gt6rcon.RconClient = FakeClient  # section 10 left _OvenFake installed
    scratch = Path(tempfile.mkdtemp())
    cli_pid = cli_file = cli_slot = None   # /tmp CLI fixtures, bound in try
    real_slot_dir = gt6server.RCON_SLOT_DIR
    gt6server.RCON_SLOT_DIR = scratch
    cwd_token = Path.cwd().resolve()
    try:
        # record shape: born complete in the single O_EXCL write
        slot = gt6server.acquire_rcon_slot(owner="pid=1 /tmp/foreign")
        lines = slot.read_text(encoding="utf-8").splitlines()
        check("11a acquire stamps the owner line into the slot record",
              len(lines) == 2 and lines[0] and lines[1] == "owner pid=1 /tmp/foreign",
              repr(lines))
        gt6server.release_rcon_slot(slot)
        legacy_slot = gt6server.acquire_rcon_slot()
        check("11b ownerless acquire keeps the legacy single-line record",
              len(legacy_slot.read_text(encoding="utf-8").splitlines()) == 1)
        gt6server.release_rcon_slot(legacy_slot)

        # the marker round-trip: recorded_stop_owner resolves through <pid>.slot
        mine = gt6server.caller_identity()
        pid_file, slot = _ownership_fixture(scratch, "own_mine", mine)
        recorded, detail = gt6server.recorded_stop_owner(pid_file)
        check("11c marker round-trip resolves the recorded owner",
              recorded == mine and detail is None, f"{recorded!r} {detail!r}")
        legacy_file, _ = _ownership_fixture(scratch, "own_legacy", mine, legacy=True)
        recorded, detail = gt6server.recorded_stop_owner(legacy_file)
        check("11d legacy record reports itself unverifiable",
              recorded is None and "legacy" in detail, f"{recorded!r} {detail!r}")
        recorded, detail = gt6server.recorded_stop_owner(scratch / "own_absent.pid")
        check("11e missing marker reports not-a-boot-handle",
              recorded is None and "no boot-slot marker" in detail, f"{detail!r}")

        # matched caller passes and still pairs the slot release
        pid_file, slot = _ownership_fixture(scratch, "own_allow", mine)
        report = REAL_STOP_SERVER(pid_file)
        check("11f matched-identity stop proceeds (report returned)",
              isinstance(report, dict) and report.get("wrapper_pid") is not None)
        check("11g allowed stop still releases the boot slot",
              not slot.exists() and not Path(str(pid_file) + ".slot").exists())
        pid_file, slot = _ownership_fixture(
            scratch, "own_token", f"pid=999999 {cwd_token}")
        check("11h same-worktree token passes with a foreign pid "
              "(manual CLI stop after a crashed sweep)",
              REAL_STOP_SERVER(pid_file).get("wrapper_pid") is not None)

        # the incident vector: foreign identity refused BEFORE any RCON/pid
        # action and without releasing the boot's slot
        foreign = "pid=999999 /tmp/foreign-session-P30"
        pid_file, slot = _ownership_fixture(scratch, "own_deny", foreign)
        stops_before = sum(1 for c in FakeClient.commands if c == "stop")
        try:
            REAL_STOP_SERVER(pid_file, rcon=("127.0.0.1", 45987, "pw"))
            check("11i foreign-identity stop refused", False)
        except gt6server.StopOwnershipError as exc:
            message = str(exc)
            check("11i foreign-identity stop refused", True)
            check("11j refusal names BOTH identities and the --force escape",
                  foreign in message and "caller is" in message
                  and "--force" in message, message[:90])
        check("11k refusal sends no RCON stop (pre-connect gate)",
              sum(1 for c in FakeClient.commands if c == "stop") == stops_before)
        check("11l refusal releases nothing (boot slot + marker intact)",
              slot.exists() and Path(str(pid_file) + ".slot").exists())

        legacy_file, _ = _ownership_fixture(scratch, "own_legacy2", mine, legacy=True)
        try:
            REAL_STOP_SERVER(legacy_file)
            check("11m legacy-record stop refused (unverifiable)", False)
        except gt6server.StopOwnershipError as exc:
            check("11m legacy-record stop refused (unverifiable)",
                  "legacy" in str(exc) and "--force" in str(exc), str(exc)[:90])

        # --force escape: proceeds through RCON and releases the slot
        pid_file, slot = _ownership_fixture(scratch, "own_force", foreign)
        report = REAL_STOP_SERVER(pid_file, rcon=("127.0.0.1", 45987, "pw"),
                                  force=True)
        check("11n --force proceeds: RCON stop sent through the client",
              any(c == "stop" for c in FakeClient.commands))
        check("11o --force completes the slot pairing (slot + marker released)",
              not slot.exists() and not Path(str(pid_file) + ".slot").exists())

        # stale-slot reaping is mtime-based and owner-line agnostic
        stale = scratch / "slot.stale"
        stale.write_text(f"{time.time()}\nowner pid=1 /tmp/foreign\n", encoding="utf-8")
        old = time.time() - gt6server.RCON_SLOT_STALE_SECONDS - 60
        os.utime(stale, (old, old))
        fresh = scratch / "slot.fresh"
        fresh.write_text(f"{time.time()}\nowner pid=1 /tmp/foreign\n", encoding="utf-8")
        gt6server._reap_stale_slots(log=lambda *a, **k: None)
        check("11p stale reaper collects the aged owner-stamped slot, keeps fresh",
              not stale.exists() and fresh.exists())

        # CLI exit codes in a REAL process (in-process stop_server is faked):
        # refusal exits 3 with the message; --force exits 0 — and the rcon
        # port is pre-picked free so the forced fallback never aims at a
        # foreign listener (the incident shape, never re-enacted).
        cli_slug = "selftest_p30own_gate"
        cli_pid = gt6server.artifact_paths(cli_slug)[1]
        cli_file, cli_slot = _ownership_fixture(
            Path("/tmp"), f"gt6_rs_{cli_slug}", foreign)
        safe_port = gt6server.pick_ports((45987,))[0]
        base_cmd = [sys.executable, str(_HERE / "gt6server.py"), "stop",
                    "--slug", cli_slug, "--rcon-port", str(safe_port)]
        refused = subprocess.run(base_cmd, capture_output=True, text=True,
                                 timeout=60)
        check("11q CLI foreign stop exits 3 with the refusal message",
              refused.returncode == 3 and "stop refused" in refused.stdout
              and "Traceback" not in refused.stdout,
              (refused.stdout + refused.stderr)[-90:])
        forced = subprocess.run(base_cmd + ["--force"], capture_output=True,
                                text=True, timeout=60)
        check("11r CLI --force stop exits 0 with the warning",
              forced.returncode == 0 and "WARNING" in forced.stdout,
              (forced.stdout + forced.stderr)[-90:])
    finally:
        gt6server.RCON_SLOT_DIR = real_slot_dir
        shutil.rmtree(scratch, ignore_errors=True)
        leftovers = [cli_pid, cli_file, cli_slot]
        if cli_pid is not None:
            leftovers.append(Path(str(cli_pid) + ".slot"))
        for path in leftovers:
            if path is not None:
                path.unlink(missing_ok=True)


def check_12_sweep_session_lock():
    """The sweep session lock (P32, card ops-sweep-lock) — the sweep-side
    guard of the P31 serialization protocol (S31-1/S31-2 parallel reviews each
    started a sweep; two concurrent same-node sweep sessions poisoned the
    shared session state — "并发 sweep session.lock 毒化"). Serverless: the
    lock dir is swapped to a scratch dir and the run surface faked, so the
    three acceptance shapes run in-process:

      ① a second starter fails fast naming the occupant (pid + session id);
      ② a dead-pid residue is never auto-cleared — --break-lock clears it and
        refuses a live owner (exit 3);
      ③ a single instance runs the full flow acquire -> run -> release.
    """
    print("\n--- 12: sweep session lock (P32, single-instance rule)")
    import sweep
    real_lock_dir, real_run = sweep.SWEEP_LOCK_DIR, sweep.run_and_record
    scratch = Path(tempfile.mkdtemp())
    sweep.SWEEP_LOCK_DIR = scratch
    node = "1.20.1-forge"
    lock_path = sweep.sweep_lock_path(node)
    invoked = []
    seen = {}

    def fake_run(args):
        invoked.append(args.mode)
        seen["lock_held"] = lock_path.exists()
        return {"mode": args.mode, "node": args.node, "wall_s": 0.0,
                "boots": 0, "chains": {}}

    sweep.run_and_record = fake_run
    try:
        # ① second start under a live lock: fail-fast naming the occupant
        lock = sweep.acquire_sweep_lock(node, "session")
        record = json.loads(lock.read_text(encoding="utf-8"))
        check("12a acquire O_EXCL-writes the owner record (pid + session id)",
              record["pid"] == os.getpid() and record["session"]
              == f"{framework.worktree_tag()}-{os.getpid()}"
              and lock_path.name == "gt6_rs_sweep_1201-forge.lock", str(record))
        try:
            code = sweep.main(["--mode", "session", "--node", node])
            check("12b second start fails fast under a live lock", False,
                  f"main returned {code}")
        except SystemExit as exc:
            message = str(exc)
            check("12b second start fails fast under a live lock",
                  "ALREADY RUNNING" in message and str(os.getpid()) in message
                  and record["session"] in message, message[:90])
        check("12c the refusal fired before any run touched the server",
              not invoked and lock_path.exists())
        sweep.release_sweep_lock(lock)

        # ② crash residue (dead pid): fail-fast, never auto-cleared, --break-lock
        residue_pid = _reaped_pid()
        lock_path.write_text(json.dumps(
            {"pid": residue_pid, "session": "deadbeef-42", "node": node,
             "mode": "perboot", "worktree": "/tmp/ghost-wt",
             "started": "2026-09-19T00:00:00"}), encoding="utf-8")
        try:
            sweep.main(["--mode", "perboot", "--node", node])
            check("12d a stale (dead-pid) lock also fails fast", False)
        except SystemExit as exc:
            message = str(exc)
            check("12d a stale (dead-pid) lock also fails fast",
                  "STALE" in message and str(residue_pid) in message
                  and "--break-lock" in message, message[:90])
        check("12e the stale lock is NOT auto-cleared", lock_path.exists())
        check("12f --break-lock clears the dead-pid residue (exit 0)",
              sweep.break_sweep_lock(node) == 0 and not lock_path.exists())
        lock_path.write_text(json.dumps(
            {"pid": os.getpid(), "session": "alive-here", "node": node,
             "mode": "session", "worktree": str(Path.cwd()),
             "started": "now"}), encoding="utf-8")
        check("12g --break-lock refuses a LIVE owner (exit 3, lock intact)",
              sweep.break_sweep_lock(node) == 3 and lock_path.exists())
        sweep.release_sweep_lock(lock_path)
        check("12h --break-lock on an absent lock is a clean no-op (exit 0)",
              sweep.break_sweep_lock(node) == 0 and not lock_path.exists())

        # ③ single instance, full flow: acquire -> run -> release
        code = sweep.main(["--mode", "session", "--node", node])
        check("12i full flow: the run happens under the held lock",
              code == 0 and invoked == ["session"] and seen["lock_held"] is True,
              f"invoked={invoked} lock_held={seen.get('lock_held')}")
        check("12j the lock is released after the run", not lock_path.exists())

        # lock scope is per node: --dual's other-node leg stays legal
        other = sweep.sweep_lock_path("1.21.1-neoforge")
        check("12k per-node scope: another node's lock is a different file",
              other != lock_path and other.name == "gt6_rs_sweep_1211-neoforge.lock",
              other.name)
    finally:
        sweep.SWEEP_LOCK_DIR, sweep.run_and_record = real_lock_dir, real_run
        shutil.rmtree(scratch, ignore_errors=True)


class _CannedClient:
    """run_steps stand-in serving recorded bodies by exact command match."""

    def __init__(self, bodies):
        self.bodies = bodies

    def run_command(self, cmd):
        return [self.bodies[cmd]]


def check_13_structured_judge():
    """The structured step judge (ops-judge-literal, the P31 pool
    rcon_judge_literal_blindspot).

    The raw "FAILED" scan used to override the expect: an expected-rejection
    line ("FUEL FAILED", "FAILED, connections N") counted as a failure even
    with the expect hit (false red), and the only escape — allow_failed —
    ALSO disarmed the expect (a missed expect on an allowed step is ALLOWED:
    false green). Structured: the expect is the assertion (hit -> PASS
    whatever the body spells); the literal scan only decides expect-less
    steps (the server-side RCON contract, GTMultiBlockCommand.java:1046);
    allow_failed keeps its ALLOWED semantics. ① pins the constructed cases,
    ② the allow_failed regression, ③ replays real recorded fusion
    forge-leg bodies (the post-ignition-gate re-recording: the
    /tmp/n_sweep_forge2.log transcript, session f33386e8, frozen
    here) through the REAL chain steps — the sweep recorded 1498/1498 PASS,
    the replay excerpt must judge the same.
    """
    print("\n--- 13: structured step judge (P32, judge-literal blindspot)")

    def judge(step, body):
        expect = framework.step_expect(step, "1.20.1-forge")
        counts = framework.judge_step(1, step, body, expect)
        return framework._step_verdict(step, body, expect), counts

    # ① constructed: the literal must not override the expect
    rejection = framework.Step("gt6pipe toggle 400 64 400 1",
                               expect="FAILED, connections 1")
    rejection_body = "GT6 pipe toggle at 400, 64, 400: FAILED, connections 1"
    check("13a expected-rejection body with the expect hit -> PASS (old code: FAIL)",
          judge(rejection, rejection_body) == ("PASS", 0))
    marker_elsewhere = framework.Step(
        "gt6energy mode 441 65 461 off", expect="emitting false")
    elsewhere_body = ("GT6 energy source mode at 441, 65, 461: emitting false\n"
                      "Tried to load invalid fluid FAILED")
    check("13b FAILED elsewhere in the body, expect hit -> PASS",
          judge(marker_elsewhere, elsewhere_body) == ("PASS", 0))
    miss = framework.Step("gt6multiblock check 450 65 462", expect="formed=true")
    check("13c expect missed -> FAIL (the real-failure red is kept)",
          judge(miss, "formed=false") == ("FAIL", 1))
    check("13d expect-less marker -> FAIL (the sole signal an expect-less step has)",
          judge(framework.Step("gt6machine crusher check 400 64 400"),
                "STAT FAILED: no machine at 400, 64, 400") == ("FAIL", 1))
    check("13e expect-less clean -> PASS, silent contract kept",
          judge(framework.Step("forceload add 438 452 462 478"),
                "Unmarked all force loaded chunks in minecraft:overworld")
          == ("PASS", 0))

    # ② allow_failed regression: ALLOWED-does-not-count, byte for byte
    allowed_miss = framework.Step("gt6machine paint 400 64 400 5",
                                  expect="No paintable GT6 TileEntity",
                                  allow_failed=True)
    check("13f allowed step, expect missed -> ALLOWED (semantics preserved)",
          judge(allowed_miss, "No machine at 400, 64, 400") == ("ALLOWED", 0))
    allowed_marker = framework.Step("kill 400 64 400", allow_failed=True)
    check("13g allowed step, bare marker -> ALLOWED",
          judge(allowed_marker, "Killed FAILED sentinel") == ("ALLOWED", 0))
    check("13h allowed step, expect hit -> PASS",
          judge(allowed_miss, "Painted: No paintable GT6 TileEntity ok")
          == ("PASS", 0))
    check("13i poll gate: expect hit with the marker present stops the poll "
          "(old code polled out the deadline)",
          framework._step_matches(rejection, rejection_body,
                                  framework.step_expect(rejection, None)))
    check("13j poll gate: expect missed keeps polling (allowed or not)",
          not framework._step_matches(allowed_miss, "No machine at 400, 64, 400",
                                      framework.step_expect(allowed_miss, None)))

    # end-to-end run_steps exit: the ① GREEN and ② ALLOWED shapes exit 0
    green = [rejection, marker_elsewhere, allowed_miss]
    ledger = []
    check("13k run_steps: marker+hit and allowed-miss chain exits 0",
          framework.run_steps(_CannedClient({
                rejection.cmd: rejection_body,
                marker_elsewhere.cmd: "GT6 energy source mode at 441, 65, 461: "
                                      "emitting false",
                allowed_miss.cmd: "No machine at 400, 64, 400"}),
              green, ledger, node="1.20.1-forge") == 0
          and [v["verdict"] for v in ledger] == ["PASS", "PASS", "ALLOWED"])
    red = [framework.Step("execute if block 450 65 462 gt6:fusion_reactor[formed=true]",
                          expect="Test passed"),
           framework.Step("gt6machine crusher check 400 64 400")]
    check("13l run_steps: real failure (expect miss + expect-less marker) counts red",
          framework.run_steps(_CannedClient({
                red[0].cmd: "Test failed, predicate was not met",
                red[1].cmd: "STAT FAILED: no machine at 400, 64, 400"}),
              red, node="1.20.1-forge") == 2)

    # ③ recorded fusion replay excerpt (forge leg; re-recorded post
    # ignition-gate from /tmp/n_sweep_forge2.log, session f33386e8):
    # real Step objects of the chain, real recorded bodies, the recorded verdicts
    import fusion
    recorded = [
        ("fill 438 58 452 462 74 478 air", "No blocks were filled"),
        ("setblock 448 63 462 gt6:machine_wall_galvanized_steel",
         "Changed the block at 448, 63, 462"),
        ("execute if block 450 65 462 gt6:fusion_reactor[formed=true]",
         "Test passed"),
        ("data get block 450 65 462",
         '450, 65, 462 has the following block data: {fake_source: 0b, '
         'stopped: 0b, te_name: "multiblock_fusion_reactor", '
         'structure_okay: 1b, facing: 2b, active: 0b, ignited: 0b, '
         'inventory: {Size: 11, Items: [{Slot: 0, id: "gt6:integrated_circuit", '
         'Count: 1b, tag: {Damage: 2}}]}, maxprogress: 1760L, running: 1b, '
         'charge_requirement: 230686720L, output_items: [], x: 450, '
         'minenergy: 0L, progress: 0L, y: 65, z: 462, '
         'id: "gt6:multiblock_fusion_reactor", energy: 0L, '
         'output_fluids: [{FluidName: "gt6:helium", Amount: 1000}]}'),
        ("gt6energy mode 441 65 461 off",
         "GT6 energy source mode at 441, 65, 461: emitting false"),
        ("forceload remove all",
         "Unmarked all force loaded chunks in minecraft:overworld"),
        ("time query daytime", "The time is 5422"),
    ]

    def _chain_step(cmd):
        matches = [s for s in fusion.steps
                   if framework.step_cmd(s, "1.20.1-forge") == cmd]
        return matches[0] if matches else None

    excerpt = [(_chain_step(cmd), cmd, body) for cmd, body in recorded]
    check("13m every recorded excerpt cmd resolves to a real fusion step",
          all(step is not None for step, _, _ in excerpt),
          str([cmd for step, cmd, _ in excerpt if step is None]))
    ledger = []
    failure = framework.run_steps(
        _CannedClient({cmd: body for _, cmd, body in excerpt}),
        [step for step, _, _ in excerpt], ledger, node="1.20.1-forge")
    check("13n recorded fusion bodies replay all-PASS, exit 0 "
          "(the post-P32 re-recording's ledger: 1498/1498 PASS, GREEN)",
          failure == 0 and len(ledger) == len(recorded)
          and all(v["verdict"] == "PASS" for v in ledger))

class _TickServer:
    """Neo-leg /tick primitive model (P32): wall-clock paced, so the framework's
    freeze proof / settle loop / thaw proof exercise the same timing they see
    against a live 1.21.1 server (real sleeps; the suite's fake-paced sections
    are done by the time this runs).

    Unfrozen, gametime advances with real elapsed time at TICK seconds per
    tick — the live world ticks between RCON round trips. Frozen, no advance;
    'tick step N' releases exactly N ticks, unrolled against the same clock,
    and the world refreezes at exactly g0+N (the vanilla gate cannot overshoot
    — TickRateManager.tick :56-61). Every command is recorded with the
    (gametime, frozen) state it executed under, so the checks can assert the
    probe ran INSIDE the frozen window at exactly g0+N — judged once.
    """

    TICK = 0.005

    def __init__(self, probe_body="settled", freeze_works=True,
                 step_works=True, thaw_works=True):
        self.gametime = 10_000
        self.frozen = False
        self.remaining = 0
        self.last = time.monotonic()
        self.probe_body = probe_body
        self.freeze_works = freeze_works
        self.step_works = step_works
        self.thaw_works = thaw_works
        self.sent = []                      # (cmd, gametime, frozen) arrival order

    def _ticks(self):
        now = time.monotonic()
        count = int((now - self.last) / self.TICK)
        self.last += count * self.TICK
        return count

    def run_command(self, cmd):
        self.sent.append((cmd, self.gametime, self.frozen))
        if cmd == "tick freeze":
            if self.freeze_works:
                self.frozen = True
            return ["ok"]
        if cmd == "tick unfreeze":
            if self.thaw_works:
                self.frozen = False
            return ["ok"]
        if cmd.startswith("tick step "):
            n = int(cmd.rsplit(" ", 1)[1])
            if self.frozen and self.step_works:
                self.remaining += n
                return [f"stepped {n}"]
            return ["tick step failed"]
        if cmd == framework.GAMETIME_QUERY:
            ticks = self._ticks()
            if self.frozen:
                moved = min(self.remaining, ticks)
                self.gametime += moved
                self.remaining -= moved
            else:
                self.gametime += ticks
            return [f"{framework.GAMETIME_PREFIX}{self.gametime}"]
        return [self.probe_body]            # the judged probe

    def cmds(self):
        return [cmd for cmd, _, _ in self.sent]

    def probes(self):
        """Non-machinery commands: (cmd, gametime-at-exec, frozen-at-exec)."""
        return [(cmd, gametime, frozen) for cmd, gametime, frozen in self.sent
                if cmd not in ("tick freeze", "tick unfreeze")
                and not cmd.startswith("tick step ")
                and cmd != framework.GAMETIME_QUERY]


def check_14_tick_primitive():
    """The deterministic tick window (P32, card ops-neo-tick-primitive).

    Mock face of the neo /tick primitive (1.20.3+; forge 1.20.1 has no
    TickCommand at all): ① the happy window — freeze proved by two equal
    gametime reads, exactly N ticks settled, the probe judged ONCE inside the
    frozen world at exactly g0+N, thaw proved; ② the failure shapes — a
    freeze that doesn't take (no /tick step sent), a step that never settles
    (deadline red), a thaw that fails (the step reds even though the probe
    passed — a session left frozen would stall every later chain); ③ the leg
    dialect gate — forge 1.20.1 undeclared = fail-visible RED that sends
    nothing, declared tick_fallback_poll = poll 制 degrade with poll's
    judge-once semantics; ④ plan_waves refuses tick-window chains a shared
    wave; ⑤ construction-time validation. Wire assertions only ever touch
    gametime arithmetic — no /tick reply text is matched (locale-proof).
    """
    print("\n--- 14: tick primitive (P32, deterministic neo window / forge gate)")
    import contextlib
    real_gap, real_poll, real_margin = (framework.FREEZE_GAP,
                                        framework.POLL_INTERVAL,
                                        framework.TICK_SETTLE_MARGIN)
    framework.FREEZE_GAP = 0.03          # >= 6 mock ticks: real proof gap
    framework.POLL_INTERVAL = 0.02
    framework.TICK_SETTLE_MARGIN = 0.1
    try:
        # ① the happy window: N=3, probe judged once, frozen, at exactly g0+3
        server = _TickServer(probe_body="landed")
        step = framework.Step("execute if block 0 0 0 minecraft:gravel",
                              expect="landed", tick_step=3)
        ledger = []
        with contextlib.redirect_stdout(io.StringIO()):
            failure = framework.run_steps(_FakeStepClient(server), [step],
                                          ledger, node="1.21.1-neoforge")
        check("14a happy window exits 0 with a PASS verdict",
              failure == 0 and [v["verdict"] for v in ledger] == ["PASS"],
              str(ledger))
        check("14b wire order: freeze, proof reads, step, settle reads, "
              "probe, unfreeze, thaw reads",
              server.cmds()[0] == "tick freeze"
              and server.cmds().count("tick freeze") == 1
              and "tick step 3" in server.cmds()
              and server.cmds()[-3] == "tick unfreeze"
              and server.cmds()[-1] == framework.GAMETIME_QUERY
              and server.cmds().index("tick step 3")
              < server.cmds().index(step.cmd) < server.cmds().index("tick unfreeze"),
              str(server.cmds()))
        step_entry = [(cmd, gametime) for cmd, gametime in
                      zip(server.cmds(), [g for _, g, _ in server.sent])
                      if cmd == "tick step 3"]
        probes = server.probes()
        check("14c probe judged ONCE inside the frozen window at exactly g0+N",
              len(probes) == 1 and probes[0][2] is True
              and probes[0][1] == step_entry[0][1] + 3,
              f"probes={probes} step@{step_entry}")

        # ②a freeze that doesn't take: /tick step never sent, still red
        server = _TickServer(freeze_works=False)
        with contextlib.redirect_stdout(io.StringIO()):
            failure = framework.run_steps(_FakeStepClient(server), [step],
                                          None, node="1.21.1-neoforge")
        check("14d freeze proof failure: red, no /tick step sent, thawed anyway",
              failure == 1 and not any(c.startswith("tick step") for c in server.cmds())
              and "tick unfreeze" in server.cmds(), str(server.cmds()))

        # ②b a step that never settles: deadline red, world still thawed
        server = _TickServer(step_works=False)
        with contextlib.redirect_stdout(io.StringIO()):
            failure = framework.run_steps(_FakeStepClient(server), [step],
                                          None, node="1.21.1-neoforge")
        check("14e never-settling step: deadline red, thaw cleanup ran",
              failure == 1 and "tick unfreeze" in server.cmds())

        # ②c a thaw that fails: the probe passed but the step still reds
        server = _TickServer(probe_body="landed", thaw_works=False)
        transcript = io.StringIO()
        with contextlib.redirect_stdout(transcript):
            failure = framework.run_steps(_FakeStepClient(server), [step],
                                          None, node="1.21.1-neoforge")
        check("14f failed thaw reds the step (probe-pass is not enough)",
              failure == 1 and "THAW PROOF FAILED" in transcript.getvalue(),
              f"failure={failure}")

        # ③ the forge leg dialect gate: undeclared = fail-visible, sends NOTHING
        server = _TickServer()
        with contextlib.redirect_stdout(io.StringIO()):
            failure = framework.run_steps(_FakeStepClient(server), [step],
                                          None, node="1.20.1-forge")
        check("14g forge leg undeclared: red and no wire traffic at all",
              failure == 1 and server.sent == [], str(server.cmds()))

        # ③ declared degradation: poll 制, expect hit on the first resend
        server = _TickServer(probe_body="landed")
        degrade = framework.Step("execute if block 0 0 0 minecraft:gravel",
                                 expect="landed", tick_step=3,
                                 tick_fallback_poll=0.2)
        ledger = []
        with contextlib.redirect_stdout(io.StringIO()):
            failure = framework.run_steps(_FakeStepClient(server), [degrade],
                                          ledger, node="1.20.1-forge")
        check("14h declared degrade: poll hit, exit 0, PASS verdict, no /tick",
              failure == 0 and [v["verdict"] for v in ledger] == ["PASS"]
              and not any(c.startswith("tick") for c in server.cmds()),
              str(server.cmds()))

        # ③ the degraded poll keeps poll's judge-once semantics (miss = red)
        server = _TickServer(probe_body="not yet")
        with contextlib.redirect_stdout(io.StringIO()):
            failure = framework.run_steps(_FakeStepClient(server), [degrade],
                                          None, node="1.20.1-forge")
        check("14i degraded poll miss: red after the declared window",
              failure == 1)

        # ④ plan_waves: a tick-window chain never shares a concurrent wave
        window_chain = framework.Chain(
            name="tickwin", slug="tickwin",
            sites=gt6world.declare_sites(gt6world.Site(0, 64, 0)),
            steps=[step])
        plain_chain = framework.Chain(
            name="plain", slug="plain",
            sites=gt6world.declare_sites(gt6world.Site(400, 64, 400)),
            steps=[framework.Step("say hi")])
        singleton = framework.plan_waves([window_chain, plain_chain], 2)
        shared = framework.plan_waves([plain_chain,
                                       framework.Chain(
                                           name="plain2", slug="plain2",
                                           sites=gt6world.declare_sites(
                                               gt6world.Site(800, 64, 800)))], 2)
        check("14j tick-window chain gets an exclusive wave even at "
              "concurrency 2; plain disjoint chains still co-wave",
              all(len(wave) == 1 for wave in singleton) and len(shared) == 1,
              f"singleton={[[c.name for c in w] for w in singleton]} "
              f"shared={[[c.name for c in w] for w in shared]}")

        # ⑤ construction-time validation (fail-fast, no silent step shapes)
        def _raises(construct):
            try:
                construct()
                return False
            except ValueError:
                return True

        check("14k tick_step validation: >=1, exclusive with poll, needs cmd",
              _raises(lambda: framework.Step("c", tick_step=0))
              and _raises(lambda: framework.Step("c", tick_step=3, poll=2.0))
              and _raises(lambda: framework.Step(tick_step=3))
              and framework.Step("c", tick_step=3).uses_tick_window())
    finally:
        framework.FREEZE_GAP, framework.POLL_INTERVAL, framework.TICK_SETTLE_MARGIN = \
            real_gap, real_poll, real_margin


class _FakeStepClient:
    """run_steps client stand-in forwarding to a shared _TickServer."""

    def __init__(self, server):
        self.server = server

    def run_command(self, cmd):
        return self.server.run_command(cmd)


def check_15_port_segment_stagger():
    """The fallback-segment stagger hooks (pool-port-stagger).

    Two parallel sessions booting the default 256xx segment at once used to
    race into BootOwnershipError and the loser idled until the winner's
    session ended. ① GT6_RCON_SEGMENT_OFFSET shifts the pick (parallel
    sessions set different values -> disjoint segments); ② a listener inside
    the candidate span flips the pick a whole segment instead of squeezing
    +1 into the occupied neighbourhood; ③ the ownership backstop is
    untouched — a grabbed exact target still fails fast in
    assert_ports_free (never double-boot, P17 semantics kept). listening_ports
    is faked (no dependence on this box's live listeners) and restored.
    """
    print("\n--- 15: port segment stagger (P34, offset + occupied flip)")
    triple = framework.SESSION_PORTS["1.20.1"]      # (25662, 25672, 25652)
    stride, span = gt6server.RCON_SEGMENT_STRIDE, gt6server.RCON_SEGMENT_SPAN
    real_listen = gt6server.listening_ports
    real_offset = os.environ.get("GT6_RCON_SEGMENT_OFFSET")
    try:
        os.environ.pop("GT6_RCON_SEGMENT_OFFSET", None)
        gt6server.listening_ports = lambda: set()   # idle box
        base = REAL_PICK_PORTS(triple)
        check("15a idle box, offset 0: the fallback triple passes through",
              tuple(base) == triple, str(base))
        os.environ["GT6_RCON_SEGMENT_OFFSET"] = "1"
        shifted = REAL_PICK_PORTS(triple)
        check("15b offset=1 shifts every start by one stride",
              tuple(shifted) == tuple(p + stride for p in base),
              f"{base} -> {shifted}")
        os.environ["GT6_RCON_SEGMENT_OFFSET"] = "2"
        shifted2 = REAL_PICK_PORTS(triple)
        all_ports = set(base) | set(shifted) | set(shifted2)
        check("15c three parallel sessions (offsets 0/1/2) pick disjoint ports",
              len(all_ports) == 3 * len(triple), str(sorted(all_ports)))
        os.environ["GT6_RCON_SEGMENT_OFFSET"] = "garbage"
        check("15d garbage offset parses as 0 (default behaviour kept)",
              REAL_PICK_PORTS(triple) == base)
        os.environ.pop("GT6_RCON_SEGMENT_OFFSET", None)

        foreign = {25652, 25662, 25672}             # the incident: whole default segment live
        gt6server.listening_ports = lambda: foreign
        flipped = REAL_PICK_PORTS(triple)
        clear = all(abs(port - busy) > span for port in flipped for busy in foreign)
        check("15e occupied segment flips to a fresh one (no +1 squeeze into the span)",
              clear and min(flipped) > max(foreign), f"{sorted(flipped)}")
        check("15f the flip keeps the triple's internal shape (rcon, +10, -10)",
              flipped[1] == flipped[0] + 10 and flipped[2] == flipped[0] - 10,
              str(flipped))

        # the backstop: even with the stagger active, a grabbed exact target
        # is still refused by the P17 gate (BootOwnershipError semantics kept)
        os.environ["GT6_RCON_SEGMENT_OFFSET"] = "1"
        target = REAL_PICK_PORTS(triple)[0]
        os.environ.pop("GT6_RCON_SEGMENT_OFFSET", None)
        gt6server.listening_ports = lambda: {target}   # a racer grabbed OUR pick
        try:
            REAL_ASSERT_PORTS_FREE((target,), label="selftest")
            check("15g BootOwnershipError backstop kept (never double-boot)", False)
        except gt6server.BootOwnershipError:
            check("15g BootOwnershipError backstop kept (never double-boot)", True)
    finally:
        gt6server.listening_ports = real_listen
        if real_offset is None:
            os.environ.pop("GT6_RCON_SEGMENT_OFFSET", None)
        else:
            os.environ["GT6_RCON_SEGMENT_OFFSET"] = real_offset


def main():
    check_1_chain_node_writeback()
    check_2_session_slug()
    check_3_session_ports()
    check_4_sweep_registry()
    check_5_ownership_gates()
    check_6_sweep_result_isolation()
    check_7_adaptive_quiet_window()
    check_8_perboot_exit_aggregate()
    check_9_waitdone_monotonic_window()
    check_10_node_expects_fork()
    check_11_stop_ownership()
    check_12_sweep_session_lock()
    check_13_structured_judge()
    check_14_tick_primitive()
    check_15_port_segment_stagger()
    print(f"\n[selftest] {'ALL GREEN' if not FAILURES else 'FAILURES: ' + str(FAILURES)}")
    return 1 if FAILURES else 0


if __name__ == "__main__":
    sys.exit(main())
