#!/usr/bin/env python3
"""p26-w1-wiremill — the Wiremill family (RU) live acceptance chain, the FULL
ladder T1-T4 (the p26_w1_sifter shape: place → input → inject → the output-slot
verdict; `run` refuses menu-less carriers BY DESIGN — the card GUI clause
menu=null — so the verdict rides the inject/check reports).

Chain semantics (task p26-w1-sifter-compressor-wiremill ACCEPTANCE rcon ①): the
poured stick→wireFine row for IRON (the :287/:292 template pair — easy arm
fixed duration 8 or hard arm getCosts(128) per the material's FURNACE/SOFT
workability split, GT6RecipesWiremill.buildRecipe:189; output wireFine 4,
eUt 16). One stick in, exactly one process, every tier:

  input 1 stick_iron (the command's feed walk firstPouredWiremillStick pins the
  item identity) → budget 16 x duration (parallel 1 — no :742 multi-process)
  → the overclock loop may x4/x2 it, all well inside the 60-tick budget.

  RU is NOT an ALL_ALTERNATING member (TD.java:216 = (F, KU)): the output
  lands on the FIRST completed tick of a plain positive train (:476 — "RU/Lathe
  Shredder output on every completed tick") — no negative half-cycle needed,
  the Wiremill is the alternating-set's control group in this card.

  With one stick consumed the remaining ticks find NO recipe (did-not-find →
  mProgress 0) — the extra headroom is the arm-ambiguity guard, deterministic
  either way. The verdict pins 4x wire_fine_iron (OP.wireFine snake = wire_fine,
  GTMaterialItems.snakeCase path = prefix_material).

  check pins: data=-2 (the menu-less carrier — the ZERO-new-MenuType clause
  live), parallel=1 parallelDuration=false (the no-NBT_PARALLEL wiremill
  columns), and on T2 the TIER_INPUTS ramp {64,128,256}.

  D teardown: the explicit fill-air over the band. No global state is touched
  (no mutates — one session boot serves the whole W1 cluster).

The item-name expects are PER-LEG via Step.node_expects (the p16_pattern_
checker precedent): bare registry path on 1.20.1 ("stick_iron" /
"wire_fine_iron"), NAMESPACED on 21.1 ("gt6:stick_iron" / "gt6:wire_fine_
iron") — the server behaviour itself is identical on both legs.

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_w1_wiremill.py
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

# the four wiremill sites on the 172 band, x-disjoint from the compressor band
SITE_T1 = gt6world.Site(404, 64, 172, dy=1, dz=1)
SITE_T2 = gt6world.Site(406, 64, 172, dy=1, dz=1)
SITE_T3 = gt6world.Site(408, 64, 172, dy=1, dz=1)
SITE_T4 = gt6world.Site(410, 64, 172, dy=1, dz=1)
T1 = F(SITE_T1)
T2 = F(SITE_T2)
T3 = F(SITE_T3)
T4 = F(SITE_T4)

# (tier, site, path, packet, tier-inputs line) — one stick in, one process out
TIERS = [
    (1, T1, "wiremill", 64, "minIn=16 recIn=32 maxIn=64"),
    (2, T2, "wiremill_t2", 256, "minIn=64 recIn=128 maxIn=256"),
    (3, T3, "wiremill_t3", 1024, "minIn=256 recIn=512 maxIn=1024"),
    (4, T4, "wiremill_t4", 4096, "minIn=1024 recIn=2048 maxIn=4096"),
]

steps = [phase("A: the W1 Wiremill ladder T1-T4 — the iron stick→wireFine row, RU positive train")]

for tier, site, path, packet, tier_inputs in TIERS:
    steps += [
        phase(f"T{tier}: parallel 1, one stick, packet {packet} — the plain positive train completes"),
        Step(f"gt6machine {path} place {site}", expect=f"GT6 {path} placed"),
        Step(f"gt6machine {path} input 1 {site}",
             expect="GT6 wiremill input: 1x stick_iron into slot 0",
             node_expects={"1.21.1": "GT6 wiremill input: 1x gt6:stick_iron into slot 0"}),
        Step(f"gt6machine {path} inject 60 {packet} {site}",
             expect="outputs=[4x wire_fine_iron; ]",
             node_expects={"1.21.1": "outputs=[4x gt6:wire_fine_iron; ]"}),
        Step(f"gt6machine {path} check {site}",
             expect="data=-2"),  # the menu-less carrier: ZERO new MenuType, the GUI clause live
        Step(f"gt6machine {path} check {site}",
             expect="parallel=1 parallelDuration=false"),
    ]

# the TIER_INPUTS ramp pin on T2 (the {16,32,64} x 2^tier table live)
steps += [
    Step(f"gt6machine wiremill_t2 check {T2}", expect=TIERS[1][4]),
]

steps += [
    phase("D: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 403 62 171 411 68 174 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-w1-wiremill",
    slug="p26w1wiremill",
    sites=gt6world.declare_sites(SITE_T1, SITE_T2, SITE_T3, SITE_T4),
    preferred_ports=(26152, 26162),      # this card's pinned rcon/query pair (the 2615x segment, fresh)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
