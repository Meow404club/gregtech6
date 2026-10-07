#!/usr/bin/env python3
"""tag-residual-convergence — the tag-face live arms + the bare-boot zero census.

Task card ACCEPTANCE: the bare boot loads EVERY shipped tag clean (the 2103
TagLoader "Couldn't load tag" headers of the pre-fix tree are gone) AND the
optional-member face is live-correct (the gated members are genuinely absent
on the bare install — optional there is not a license to reference ghosts).

  A the live tag face: /gt6tags dump answers "0 members" for a gated family
    tag (forge:rods/aluminium — the file LOADS, its only member is
    seed-hidden) and carries the ungated control (forge:ingots/iron answers
    with gt6:ingot_iron — the unification loop still closes);
  B the seed-hide anchor: the gated ACT id is Unknown item (strict) — the
    optional members of the mineable/wrench tag correspond to genuinely
    hidden ids, not to a broken gate;
  C (--census mode) the bare-boot log census: boot the node through the
    framework slot (gt6server, eula + ports provisioned), wait Done, stop,
    then grep run/logs/latest.log — "Couldn't load tag" MUST be 0 (pre-fix
    baseline: 2103 headers / 3448 references, forge leg 2026-10-08).

Run:
  GT6_SESSION=off python3 tools/rcon/chains/tag_residual.py            # live arms
  GT6_SESSION=off python3 tools/rcon/chains/tag_residual.py --census 1.20.1-forge
  GT6_SESSION=off python3 tools/rcon/chains/tag_residual.py --census 1.21.1-neoforge
"""

import re
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

# the fresh x=486 z=20 band (east of the parse-errors x460..476 chest band —
# the one-fresh-band-per-cluster discipline)
CHEST = "486 65 20"

steps = [
    phase("A: the gated family tag loads EMPTY live (the member is seed-hidden)"),
    # the materials namespace is platform-keyed: forge: on 1.20.1, c: on 1.21.1 (the
    # GT6ItemTags MATERIALS_NAMESPACE leg split — the vanilla-tag-dual-tree shape)
    Step("gt6tags dump forge:rods/aluminium",
         expect="gt6tags dump forge:rods/aluminium: 0 members []",
         node_cmds={"1.21.1": "gt6tags dump c:rods/aluminium"},
         node_expects={"1.21.1": "gt6tags dump c:rods/aluminium: 0 members []"}),
    Step("gt6tags dump forge:ingots/iron", expect="gt6:ingot_iron",
         node_cmds={"1.21.1": "gt6tags dump c:ingots/iron"}),
    phase("B: the seed-hide anchor — the gated ACT id is Unknown item (strict)"),
    Step(f"setblock {CHEST} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {CHEST} container.0 with gt6:advanced_crafting_table_aluminium 1",
         expect="Unknown item 'gt6:advanced_crafting_table_aluminium'"),
    Step(f"item replace block {CHEST} container.1 with gt6:ingot_iron 1", expect="Replaced"),
    phase("C: teardown — the explicit band restore"),
    Step("fill 484 62 17 502 68 24 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="tag-residual-convergence",
    slug="tagres",
    sites=gt6world.declare_sites(gt6world.Site(486, 65, 20)),
    preferred_ports=(26506, 26516),
    game_port=26496,
    passes=1,  # stateless chest fill
    steps=steps,
)


def census(node: str) -> int:
    """The bare-boot zero proof: framework-slot boot -> Done -> stop -> grep the log."""
    import os
    worktree = os.environ.get("GT6_TAGRES_WORKTREE", str(_HERE.parent.parent.parent))
    os.chdir(worktree)
    import gt6server
    rcon, query = gt6server.pick_ports((26526, 26536))
    game = rcon - 10
    slug = "tagres_census_" + node.replace(".", "").replace("-", "_")
    print(f"[census] node={node} ports game={game} rcon={rcon} query={query} worktree={worktree}")
    gt6server.provision_run_dir(".", game, rcon, query, "gt6", node=node)
    log, pid_path = gt6server.artifact_paths(slug)
    pid = gt6server.start_server(".", log, pid_path, gradle_task=gt6server.gradle_task(node))
    try:
        if not gt6server.wait_done(log, timeout=900, pid=pid):
            print("[census] FAIL: boot never reached Done")
            return 1
    finally:
        gt6server.stop_server(pid_path, rcon=("127.0.0.1", rcon, "gt6"))
    latest = Path(worktree) / "mdk" / "versions" / node / "run" / "logs" / "latest.log"
    body = latest.read_text(encoding="utf-8", errors="replace")
    headers = len(re.findall(r"Couldn't load tag", body))
    references = 0
    current = False
    for line in body.splitlines():
        if "Couldn't load tag" in line:
            current = True
            references += 1
        elif current and line.startswith("\t") and "(from " in line:
            references += 1
        elif line and not line.startswith("\t"):
            current = False
    parsing = len(re.findall(r"Parsing error", body))
    print(f"[census] {latest}: TagLoader headers={headers} references={references} parsing_errors={parsing}")
    print(f"[census] boot log: {log}")
    if headers == 0 and parsing == 0:
        print("[census] PASS: the bare boot loads every shipped tag clean (pre-fix baseline 2103 headers)")
        return 0
    print("[census] FAIL: TagLoader/parse errors remain")
    return 1


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--census":
        sys.exit(census(sys.argv[2] if len(sys.argv) > 2 else "1.20.1-forge"))
    main(CHAIN)
