#!/usr/bin/env python3
"""p15_keepfilter_reboot_probe — the keepFilter SAVE/LOAD face, live (evidence 4b).

The p12 keepFilter chain and the p15_runtime_smoke K phase cover the LIVE and the
WRITE faces (drain-to-0 keeps the identity; writeToNBT saves FluidName+Amount 0).
The known 21.1 delta lives on the READ face: FluidTankGT.readFromNBT's 21.1 leg
parses through FluidStack.parseOptional (the codec face), and a 0-amount-with-
identity payload collapses to EMPTY (FluidTankGT.java:100 "KNOWN 21.1 DELTA").
That face only executes when a BlockEntity is re-created from saved NBT — i.e.
across a reboot. No chain framework pass can see it (each pass re-fills the rig
after the bbox cleanup), so this probe runs its own two-boot lifecycle:

  boot 1: place barrel_logistics, fill 5000 water, draw 5000, record show/data,
          RCON stop (the stop saves the world — the write face lands on disk).
  boot 2: read the same barrel back from the re-created BE —
          IDENTITY RETAINED: show names water at 0 L, data key still there.
          IDENTITY LOST:    show says nothing / data key gone (the codec fold).

The probe is EVIDENCE, not a gate: it exits 0 either way and prints a verdict
line. Run per node and diff:

  python3 tools/rcon/chains/p15_keepfilter_reboot_probe.py [--node 1.21.1-neoforge]

1.20.1 expectation: RETAINED (the raw-fluid rebuild leg). 21.1 expectation per
the KNOWN DELTA note: LOST (player-visible difference for architect adjudication).
"""

import argparse
import sys
import time

sys.path.insert(0, "/home/brokestar/workspace/MGT6GA/MGT6GA-trees/p15-rcon-dual-gate/tools/rcon")
import gt6rcon
import gt6server

WORKTREE = "/home/brokestar/workspace/MGT6GA/MGT6GA-trees/p15-rcon-dual-gate"
PASSWORD = "gt6"
BARREL = "150 64 150"          # far from every chain's site bbox
FORCELOAD = "forceload add 128 128 160 160"


def boot(node, slug, rcon_port, query_port, game_port, timeout=900):
    log_path, pid_path = gt6server.artifact_paths(slug)
    gt6server.provision_run_dir(WORKTREE, game_port, rcon_port, query_port,
                                PASSWORD, node=node)
    pid = gt6server.start_server(WORKTREE, log_path, pid_path,
                                 gradle_task=gt6server.gradle_task(node))
    if not gt6server.wait_done(log_path, timeout, poll=2.0, pid=pid):
        gt6server.stop_server(pid_path, rcon=("127.0.0.1", rcon_port, PASSWORD))
        raise RuntimeError(f"no Done; tail:\n{gt6server.log_tail(log_path, 8192)}")
    time.sleep(2)
    return pid_path, log_path, rcon_port


def run(client, *commands):
    for command in commands:
        outs = client.run_command(command)
        body = "\n".join(outs) if isinstance(outs, list) else str(outs)
        print(f"$ {command}\n{body if body else '<no response>'}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--node", default="1.20.1-forge")
    args = parser.parse_args()
    node = args.node
    slug = f"p15kfr{'1211' if '21.1' in node else '1201'}"
    rcon_port, query_port, game_port = gt6server.pick_ports((25784, 25794, 25774))

    print(f"[keepfilter-probe] node {node} slug {slug}")
    pid_path = None
    try:
        pid_path, log_path, rcon_port = boot(node, slug, rcon_port, query_port, game_port)
        with gt6rcon.RconClient("127.0.0.1", rcon_port, PASSWORD, first_timeout=30.0) as client:
            run(client, FORCELOAD,
                f"fill 148 62 148 152 66 152 air",
                f"setblock {BARREL} gt6:barrel_logistics",
                f"gt6tank fill {BARREL} minecraft:water 5000",
                f"gt6tank draw {BARREL} 5000")
            print("--- boot 1 (live/write face):")
            run(client, f"gt6tank show {BARREL}", f"data get block {BARREL} tank")
        # the graceful stop saves the world (the write face lands on disk);
        # stop_server owns the RCON stop — never send it in-band (a closing
        # socket must not crash the evidence run).
        gt6server.stop_server(pid_path, rcon=("127.0.0.1", rcon_port, PASSWORD))
        pid_path = None
        time.sleep(6)

        print("--- boot 2 (read face: the BE re-created from saved NBT):")
        pid_path, log_path, rcon_port = boot(node, slug, rcon_port, query_port, game_port)
        with gt6rcon.RconClient("127.0.0.1", rcon_port, PASSWORD, first_timeout=30.0) as client:
            run(client, FORCELOAD)
            show = "\n".join(client.run_command(f"gt6tank show {BARREL}") or [])
            data = "\n".join(client.run_command(f"data get block {BARREL} tank") or [])
            print(f"show: {show.strip()}")
            print(f"data: {data.strip()}")
            retained = "of minecraft:water" in show and "Amount: 0" in data
            print(f"\nVERDICT [{node}]: {'IDENTITY RETAINED' if retained else 'IDENTITY LOST'}")
            # leave the node world tidy
            run(client, f"setblock {BARREL} air")
    finally:
        if pid_path is not None:
            gt6server.stop_server(pid_path, rcon=("127.0.0.1", rcon_port, PASSWORD))
    return 0


if __name__ == "__main__":
    sys.exit(main())
