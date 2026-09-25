#!/usr/bin/env python3
"""compressor — the Compressor family (KU) live acceptance chain, the FULL
ladder T1-T4 (the sifter shape: place → input → inject → the output-slot
verdict; `run` refuses menu-less carriers BY DESIGN — the card GUI clause
menu=null — so the verdict rides the inject/check reports).

Chain semantics (task sifter-compressor-wiremill ACCEPTANCE rcon ①): the
:654 vanilla sand row (sand 4 → sandstone 1, eUt 16, duration 32 —
GT6RecipesCompressor.java:186, pinned by GT6KineticRecipesPourTest) on all four
tiers; the parallel column follows PARALLEL_4_32 with NBT_PARALLEL_DURATION T:

  T1 parallel 4: input 16 sand (4 single-consume + 3 probe pairs x4 = 16) →
     budget 16x32x4 = 2048, no :773 overclock (16 !< 16) → 32 ticks @ 64.
  T2 parallel 8: input 32 → budget 16x32x8 = 4096, one x4 overclock (16→64)
     doubles it to 8192 → 32 ticks @ 256 (the perfect-overclock invariance).
  T3 parallel 16: input 64 (the slot cap; 4 + 15 probes x4 = 64 exactly) →
     8192, two overclocks x4 → 32768 → 32 ticks @ 1024.
  T4 mParallel 32 but the input stack caps the count at 16 (64 sand = 4 + 15x4)
     → 8192, three overclocks x8 → 65536 → 16 ticks @ 4096 → the verdict pins
     16x sandstone (the input-limited parallel, NOT the table's 32).

  KU closes the pulse cycle inside ONE command: `inject N size -size` = N-1
  positive packets then the negative AC half-cycle — the :815 transition tick
  (TileEntityBasicMachine.java:476) delivers; the rig drains mEnergy by
  mInputMax per tick (:443) so the leftover sand never re-fires a second
  process within the command.

  check pins: data=-2 (the menu-less carrier — the ZERO-new-MenuType clause
  live), parallel=P parallelDuration=true, and on T2 the TIER_INPUTS ramp
  {64,128,256} (GTMachines.TIER_INPUTS[1]).

  D teardown: the explicit fill-air over the band. No global state is touched
  (no mutates — one session boot serves the whole W1 cluster).

The item-name expects are PER-LEG via Step.node_expects (the pattern_
checker precedent): bare registry path on 1.20.1, NAMESPACED ("minecraft:
sandstone") on 21.1 — the server behaviour itself is identical on both legs.

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/compressor.py
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

# the four compressor sites on the 172 band, x-disjoint from the sifter band
SITE_T1 = gt6world.Site(394, 64, 172, dy=1, dz=1)
SITE_T2 = gt6world.Site(396, 64, 172, dy=1, dz=1)
SITE_T3 = gt6world.Site(398, 64, 172, dy=1, dz=1)
SITE_T4 = gt6world.Site(400, 64, 172, dy=1, dz=1)
T1 = F(SITE_T1)
T2 = F(SITE_T2)
T3 = F(SITE_T3)
T4 = F(SITE_T4)

# (tier, site, path, input count, packet, ticks, parallel, expected output count,
#  the TIER_INPUTS line)
TIERS = [
    (1, T1, "compressor", 16, 64, 33, 4, 4, "minIn=16 recIn=32 maxIn=64"),
    (2, T2, "compressor_t2", 32, 256, 33, 8, 8, "minIn=64 recIn=128 maxIn=256"),
    (3, T3, "compressor_t3", 64, 1024, 33, 16, 16, "minIn=256 recIn=512 maxIn=1024"),
    (4, T4, "compressor_t4", 64, 4096, 17, 32, 16, "minIn=1024 recIn=2048 maxIn=4096"),
]

steps = [phase("A: the W1 Compressor ladder T1-T4 — the :654 sand row, KU pulse cycles")]

for tier, site, path, feed, packet, ticks, parallel, out_count, tier_inputs in TIERS:
    steps += [
        phase(f"T{tier}: mParallel {parallel}, input {feed} sand, packet {packet} — {ticks - 1} ticks + the negative half-cycle"),
        Step(f"gt6machine {path} place {site}", expect=f"GT6 {path} placed"),
        Step(f"gt6machine {path} input {feed} {site}",
             expect=f"GT6 compressor input: {feed}x sand into slot 0",
             node_expects={"1.21.1": f"GT6 compressor input: {feed}x minecraft:sand into slot 0"}),
        Step(f"gt6machine {path} inject {ticks} {packet} -{packet} {site}",
             expect=f"outputs=[{out_count}x sandstone; ]",
             node_expects={"1.21.1": f"outputs=[{out_count}x minecraft:sandstone; ]"}),
        Step(f"gt6machine {path} check {site}",
             expect="data=-2"),  # the menu-less carrier: ZERO new MenuType, the GUI clause live
        Step(f"gt6machine {path} check {site}",
             expect=f"parallel={parallel} parallelDuration=true"),
    ]

# the TIER_INPUTS ramp pin on T2 (the {16,32,64} x 2^tier table live)
steps += [
    Step(f"gt6machine compressor_t2 check {T2}", expect=TIERS[1][8]),
]

steps += [
    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 393 62 171 401 68 174 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="compressor",
    slug="compressor",
    sites=gt6world.declare_sites(SITE_T1, SITE_T2, SITE_T3, SITE_T4),
    preferred_ports=(26151, 26161),      # this card's pinned rcon/query pair (the 2615x segment, fresh)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
