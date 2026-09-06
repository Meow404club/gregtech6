#!/usr/bin/env python3
"""p19_nbt_rebind_reboot_probe — the NBT_ACCESS dynamic-rebind verdict chain (task
p19-nbtaccess-dynamic-rebind ACCEPTANCE ③, the verdict_gate RCON chain).

The offline unit test (TileEntityBase03TicksAndSyncNbtAccessTest) pins the delegate
contract with a synthetic composite; this probe pins the REAL thing end to end on a
persistent world:

  boot 1: forceload → clean → place a Coke Oven controller → form 64 (the SET
          scaffold) → check (formed) → `data merge block` an ENCHANTED diamond sword
          into the controller's inventory slot 0 (the /give component shape; the
          gt6multiblock input command drops ItemInput components, so the merge is the
          deterministic insertion channel — it rides the server's own composite load
          face) → verify `data get block` shows the enchantment.
          The graceful RCON stop saves the chunk: the save face writes the slot
          through NBT_ACCESS delegated to the rebound server composite.
  boot 2: forceload → the controller BE is re-created by the chunk loader
          (loadStatic → load → deserializeNBT — the parse face through the rebound
          composite; the P18 dead-controller signature would appear as load ERRORs)
          → `data get block` the inventory back.

          ENCHANTMENT SURVIVED: the parse resolved the sharpness holder — the fix.
          ENCHANTMENT LOST:    the P19 regression still open (unit-test-only fix).

Run (this card's pinned ports 25963/25973):

  python3 tools/rcon/chains/p19_nbt_rebind_reboot_probe.py [--node 1.21.1-neoforge]

Default node is 1.21.1-neoforge (the card's verdict_gate leg); --node 1.20.1-forge
runs the same lifecycle as the parity baseline with the forge NBT shapes.
Exit code 0 iff both boots' steps judged clean and the boot-2 verdict is SURVIVED.
"""

import argparse
import sys
import time
from pathlib import Path

# .../tools/rcon/chains/<probe>.py — parents[1] is tools/rcon (the module imports),
# parents[3] is this worktree's root (the p15_keepfilter_reboot_probe convention: the
# probe runs from wherever it is checked out).
_TOOLS_RCON = Path(__file__).resolve().parents[1]
if str(_TOOLS_RCON) not in sys.path:
    sys.path.insert(0, str(_TOOLS_RCON))
import gt6rcon
import gt6server
import gt6world

WORKTREE = str(Path(__file__).resolve().parents[3])
PASSWORD = "gt6"

# The rig: a Coke Oven pilot far from every chain's site bbox (the keepfilter probe's
# (150,64,150) barrel slot; this probe sits a band west of it).
CONTROLLER = (470, 64, 470)
POS = f"{CONTROLLER[0]} {CONTROLLER[1]} {CONTROLLER[2]}"
FORCELOAD = "forceload add 460 460 480 480"

# The enchanted payload per node — the slot-0 item in the ItemStackHandler save shape.
# 1.21.1: {id, count, components:{enchantments:{<id>:level}}} (the flat 1.21 map).
# 1.20.1: {id, Count, tag:{Enchantments:[{id, lvl}]}, Damage} (the legacy shape).
MERGE_NBT = {
    "1.21.1": ('{inventory:{Size:11,Items:[{Slot:0b,id:"minecraft:diamond_sword",count:1,'
               'components:{"minecraft:enchantments":{"minecraft:sharpness":5}}}]}}'),
    "1.20.1": ('{inventory:{Size:11,Items:[{Slot:0b,id:"minecraft:diamond_sword",Count:1b,Damage:0,'
               'tag:{Enchantments:[{id:"minecraft:sharpness",lvl:5s}]}}]}}'),
}
# The judged marker in `data get block` output per node.
EXPECT_MARKERS = {
    "1.21.1": ('"minecraft:enchantments"', '"minecraft:sharpness"'),
    "1.20.1": ('"minecraft:sharpness"', 'lvl: 5s'),
}

# The vanilla / NBT-load signatures that would mean the controller died on the parse
# face (the P18 dead-controller family). Informational: reported, gating stays on the
# verdict markers.
LOAD_ERROR_SIGNATURES = ("Failed to load", "Couldn't load", "Error loading")


def boot(node, slug, rcon_port, query_port, game_port, timeout=900):
    """One provision + start + wait-done pass; returns the live handles."""
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
    """Run commands, print transcripts; return the last non-empty body."""
    body = ""
    for command in commands:
        outs = client.run_command(command)
        body = "\n".join(outs) if isinstance(outs, list) else str(outs)
        print(f"$ {command}\n{body if body else '<no response>'}")
    return body


def judge(label, body, markers, allow_failed=False):
    """The step judge: FAILED marker absent + ANY of the expected substrings present
    (the markers are per-node alternates, the framework judge's single-expect form
    generalised)."""
    missed = "FAILED" in body or not any(marker in body for marker in markers)
    if missed and not allow_failed:
        print(f"   [{label}] FAIL — expected {markers}")
        return 1
    print(f"   [{label}] ok")
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--node", default="1.21.1-neoforge")
    args = parser.parse_args()
    node = args.node
    node_key = node.split("-", 1)[0]
    slug = f"p19rebind{'1211' if '21.1' in node else '1201'}"
    rcon_port, query_port, game_port = gt6server.pick_ports((25963, 25973, 25953))

    print(f"[p19-rebind-probe] node {node} slug {slug}")
    merge_nbt = MERGE_NBT[node_key]
    markers = EXPECT_MARKERS[node_key]

    pid_path = None
    failures = 0
    errors2 = -1
    try:
        # ---------------------------------------------- boot 1: place, form, insert
        print("--- boot 1 (insert arm: the save face writes the rebound view on stop)")
        pid_path, log_path, rcon_port = boot(node, slug, rcon_port, query_port, game_port)
        with gt6rcon.RconClient("127.0.0.1", rcon_port, PASSWORD, first_timeout=30.0) as client:
            run(client, FORCELOAD,
                f"fill 468 62 468 482 68 482 air",
                f"gt6multiblock place {POS}",
                f"gt6multiblock form {POS} 64",
                f"gt6multiblock check {POS}")
            failures += judge("formed", "\n".join(
                client.run_command(f"gt6multiblock check {POS}") or []),
                ("block_formed=true", "linked_parts=25/25"))
            # the enchanted payload into slot 0 (rides the server composite load face)
            merge = run(client, f"data merge block {POS} {merge_nbt}")
            failures += judge("merge", merge, ("Modified block data", "Merged NBT"))
            got = "\n".join(client.run_command(f"data get block {POS} inventory") or [])
            failures += judge("boot1 inventory", got, markers)
        gt6server.stop_server(pid_path, rcon=("127.0.0.1", rcon_port, PASSWORD))
        pid_path = None
        time.sleep(6)

        # ------------------------------- boot 2: the chunk re-creates the controller BE
        print("--- boot 2 (parse arm: loadStatic → deserializeNBT over the rebound view)")
        pid_path, log_path, rcon_port = boot(node, slug, rcon_port, query_port, game_port)
        with gt6rcon.RconClient("127.0.0.1", rcon_port, PASSWORD, first_timeout=30.0) as client:
            run(client, FORCELOAD)
            formed = "\n".join(client.run_command(f"gt6multiblock check {POS}") or [])
            failures += judge("boot2 formed", formed, ("block_formed=true",),
                              allow_failed=True)  # formation is not the verdict; the item is
            got = "\n".join(client.run_command(f"data get block {POS} inventory") or [])
            print(f"boot2 inventory: {got.strip()}")
            survived = all(marker in got for marker in markers)
            for signature in LOAD_ERROR_SIGNATURES:
                if signature in got:
                    print(f"   [boot2] load-error signature in NBT read: {signature}")
            # leave the node world tidy (the probe owns its rig)
            run(client, f"setblock {POS} air")
            errors2 = gt6server.server_error_lines(log_path)
            load_errors = sum(1 for line in gt6server.log_tail(log_path, 200000).splitlines()
                              if any(s in line for s in LOAD_ERROR_SIGNATURES))
            print(f"\nVERDICT [{node}]: {'ENCHANTMENT SURVIVED' if survived else 'ENCHANTMENT LOST'} "
                  f"(server ERROR lines boot2: {errors2}, load-signature lines: {load_errors})")
            if not survived:
                failures += 1
    finally:
        if pid_path is not None:
            gt6server.stop_server(pid_path, rcon=("127.0.0.1", rcon_port, PASSWORD))
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
