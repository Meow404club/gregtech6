#!/usr/bin/env python3
"""sifter — the Sifter family (KU) live acceptance chain, the FULL ladder
T1-T4 (the canner_refill shape: place → input → inject → the output-slot
verdict — on the P26 W1 trio the verdict rides the inject/check REPORTS because
`run` refuses menu-less carriers BY DESIGN: the card GUI clause menu=null means
no MenuType, and GTMachineCommand.run sends "use inject+check instead" — the
refusal line IS the clause's live face, pinned here as a step).

Chain semantics (task sifter-compressor-wiremill ACCEPTANCE rcon ① —
"place→input(count≥行输入)→inject(包长≥TIER_INPUTS[tier][0])→run→输出槽断言→teardown"):

  the :224 grass row0 (grass 1 → coarse dirt + four seed identities, chances
  prefix-trimmed; the deterministic getOutputs(count) overload resolves every
  chance>0 slot at full certainty — Recipe.java:186) on all four tiers:

  T1 parallel 4 (PARALLEL_4_32[0]): input 8 grass (1 single-consume + 3 probe
     pairs = 4 processes) → budget 16 EUt x 144 duration x 4 = 9216, no :773
     overclock (16 !< 16) → 144 ticks @ mInputMax 64.
  T2 parallel 8: input 16 → budget 16x144x8 = 18432, one x4 overclock
     (16→64 = mInputMin) doubles it to 36864 → 144 ticks @ 256 (the perfect-
     overclock property: the tick count is tier-invariant).
  T3 parallel 16: input 16 → 36864, two overclocks x4 → 147456 → 144 @ 1024.
  T4 parallel 32: input 32 → 73728, three overclocks → 589824 → 144 @ 4096.

  KU is an ALL_ALTERNATING member (TD.java:216 = (F, KU)): a positive train
  never delivers, the output lands on the positive→non-positive TRANSITION
  tick (:815 arm — TileEntityBasicMachine.java:476). The inject rig closes the
  pulse cycle inside ONE command: `inject N size -size` = N-1 positive packets
  then the negative AC half-cycle (doInject abs()'s the size — the final
  packet still charges AND latches mStateNew=false, :760/:767).

  The input tail after the single process (4 grass left in T1) never re-fires:
  the completion iteration is the LAST iteration, and each rig tick drains
  mEnergy by mInputMax (:443) so the natural tick finds mEnergy 0.

  check pins: data=-2 (the menu-less carrier — the ZERO-new-MenuType clause
  live), parallel=P parallelDuration=true (the PARALLEL_4_32 table + the
  duration-T column), and on T2 the TIER_INPUTS ramp minIn/recIn/maxIn =
  {64,128,256} (GTMachines.TIER_INPUTS[1]).

  D teardown: the explicit fill-air over the band. No global state is touched
  (the grid inject rig needs no fakesource; no mutates — one session boot
  serves the whole W1 cluster).

The item-name expects are PER-LEG via Step.node_expects (the pattern_
checker precedent): the 1.20.1 ItemStack rendering is the bare registry path
("grass_block") while the 21.1 rendering is NAMESPACED ("minecraft:grass_
block") — the server behaviour itself is identical on both legs.

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/sifter.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

F = gt6world.fmt

# the four sifter sites on the 172 band (clear of the z=20 and z=124 rosters) —
# the T1-T4 ladder, spacing 2
SITE_T1 = gt6world.Site(384, 64, 172, dy=1, dz=1)
SITE_T2 = gt6world.Site(386, 64, 172, dy=1, dz=1)
SITE_T3 = gt6world.Site(388, 64, 172, dy=1, dz=1)
SITE_T4 = gt6world.Site(390, 64, 172, dy=1, dz=1)
T1 = F(SITE_T1)
T2 = F(SITE_T2)
T3 = F(SITE_T3)
T4 = F(SITE_T4)

# (tier, site, machine path, input count, packet size, parallel, tier-inputs line)
TIERS = [
    (1, T1, "sifter", 8, 64, 4, "minIn=16 recIn=32 maxIn=64"),
    (2, T2, "sifter_t2", 16, 256, 8, "minIn=64 recIn=128 maxIn=256"),
    (3, T3, "sifter_t3", 16, 1024, 16, "minIn=256 recIn=512 maxIn=1024"),
    (4, T4, "sifter_t4", 32, 4096, 32, "minIn=1024 recIn=2048 maxIn=4096"),
]


def dirt_expect(count):
    """The coarse-dirt verdict per leg (the deterministic full-certainty output)."""
    return {
        "cmd_expect": f"outputs=[{count}x coarse_dirt; ",
        "node_expect": f"outputs=[{count}x minecraft:coarse_dirt; ",
    }


steps = [phase("A: the W1 Sifter ladder T1-T4 — the :224 grass row0, KU pulse cycles")]

for tier, site, path, feed, packet, parallel, tier_inputs in TIERS:
    d = dirt_expect(parallel)
    steps += [
        phase(f"T{tier}: parallel {parallel}, budget 16x144x{parallel} at packet {packet} — 144 ticks + the negative half-cycle"),
        Step(f"gt6machine {path} place {site}", expect=f"GT6 {path} placed"),
        Step(f"gt6machine {path} input {feed} {site}",
             expect=f"GT6 sifter input: {feed}x grass_block into slot 0",
             node_expects={"1.21.1": f"GT6 sifter input: {feed}x minecraft:grass_block into slot 0"}),
        Step(f"gt6machine {path} inject 145 {packet} -{packet} {site}",
             expect=d["cmd_expect"],
             node_expects={"1.21.1": d["node_expect"]}),
        Step(f"gt6machine {path} check {site}",
             expect="data=-2",  # the menu-less carrier: ZERO new MenuType, the GUI clause live
             ),
        Step(f"gt6machine {path} check {site}",
             expect=f"parallel={parallel} parallelDuration=true"),
    ]

# the TIER_INPUTS ramp pin on T2 (the {16,32,64} x 2^tier table live)
steps += [
    Step(f"gt6machine sifter_t2 check {T2}", expect=TIERS[1][6]),
]

steps += [
    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 383 62 171 391 68 174 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="sifter",
    slug="sifter",
    sites=gt6world.declare_sites(SITE_T1, SITE_T2, SITE_T3, SITE_T4),
    preferred_ports=(26150, 26160),      # this card's pinned rcon/query pair (the 2615x segment, fresh)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
