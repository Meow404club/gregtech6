#!/usr/bin/env python3
"""selftest — serverless verification of the framework's P17 fixes (no boot).

Every check runs in-process with the boot/RCON surface faked, so the whole
suite is a ~1s dry run usable in any worktree:

  python3 tools/rcon/selftest.py        # exit 0 = all checks green

Covered (one section per P16-closeout framework defect, card
p17-rcon-framework-fixes):

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
  4 sweep registry — the eight p16 chains are registered as one cluster.
  5 boot-ownership gates — ports-free trips on a real LISTEN socket; the
    pid-file gate accepts only THIS boot's pid.
"""

import socket
import sys
import tempfile
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

P16_STEMS = ("p16_pattern_checker", "p16_aqua_fluids", "p16_side_io",
             "p16_machine_fluid_gui", "p16_drying_rows", "p16_form_scaffold",
             "p16_chisel", "p16_distillery")

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
    a = framework.Chain(name="a", slug="p16dryrows",
                        preferred_ports=(25775, 25785))
    b = framework.Chain(name="b", slug="p16af")
    slug = framework.session_slug([a, b], "1.20.1-forge")
    check("2a roster in slug", "p16af+p16dryrows" in slug, slug)
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
    print("\n--- 4: sweep registry carries the p16 cluster")
    import sweep
    stems = sweep.ordered_stems()
    missing = [stem for stem in P16_STEMS if stem not in stems]
    check("4a all eight p16 stems registered", not missing, f"missing={missing}")
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


def main():
    check_1_chain_node_writeback()
    check_2_session_slug()
    check_3_session_ports()
    check_4_sweep_registry()
    check_5_ownership_gates()
    print(f"\n[selftest] {'ALL GREEN' if not FAILURES else 'FAILURES: ' + str(FAILURES)}")
    return 1 if FAILURES else 0


if __name__ == "__main__":
    sys.exit(main())
