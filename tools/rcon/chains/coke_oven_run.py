#!/usr/bin/env python3
"""coke-oven-run — the Coke Oven LIVE RUN chain (task chains-coke-oven-run,
closing the KG gap "RCON_chains GAP_OF coke_oven_run_face": the P16 chains stop at
the form/pattern/facing layer, the log -> charcoal + creosote live run had zero
coverage; the p6-era run chain predates the P16 pattern restructure and is gone).

Chain semantics (the card path):

  A the multiblock form arm (the form_scaffold positive-arm semantics,
    byte-identical commands):
    place (controller only) -> check (the missing-parts scene) -> form 64
    ("formed=true okay=true stock 64 -> 39", 25 bricks consumed) -> check
    (block_formed=true linked_parts=25/25).

  B the LIVE RUN arm (the run semantics: 1 log -> 1 charcoal gem + 250 mB
    creosote at 3600 ticks — GT6CokeOvenLogExpansion WoodEntry 2-arg defaults,
    the #minecraft:logs tag family the p6 tag listener poured live):
    input 1 minecraft:oak_log (the gated slot-0 insert) -> ignite (the
    TOOL_igniter branch, mRequiresIgnition asserted) -> `gt6multiblock tick
    4200` (the dispatcher arm drives updateEntity 4200 times — the TU
    self-generation earns 1 energy/tick, 3600 progress at 1/tick, 4200 gives
    the burn-in margin) -> the TWO-SIDED OUTPUT ASSERTION:
    - item side: check reports the output slot holding gem_charcoal;
    - fluid side: fluid stat reports the output tank holding 250mB gt6:creosote
      (no barrel below the structure — getFluidOutputTarget finds nothing, the
      creosote STAYS in mTanksOutput[0]).

  C the s4-6 laser-gas census arm (pure census, no behavior assertion): the two
    GT6LaserGas registrations (gt6:comp_laser_gas_empty / gt6:comp_laser_gas_co2)
    land as real stacks in a chest and read back through the vanilla data face —
    the qu_usb give-chest form (RCON has no player, /give is unreachable;
    `item replace` + `data get` IS the registration-live proof, the P31 id686
    lesson) — zero NPE + registration in place.

Expects are loader-neutral substrings (the P26 lesson: 1.20.1 renders bare ids,
21.1 namespaces them — "gem_charcoal" matches both).

Run:  GT6_SESSION=off python3 tools/rcon/chains/coke_oven_run.py
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

# The fresh z=600 band (x/z-disjoint from every registered band — the roster
# tops out at z=564 x696..724 and z=511/521 x380..397).
#   OVEN  — the run pilot: controller at (500,64,600) facing north (default
#           place), the structure centre at (500,64,601), the 3x3x3 shell
#           spanning z600..602 (the site's dz=2 covers it).
#   CHEST — the laser-gas census chest, x-disjoint from the shell.
OVEN = gt6world.Site(500, 64, 600, dx=1, dy=1, dz=2)
CHEST = gt6world.Site(506, 64, 600)
M = F(OVEN)
C = F(CHEST)

steps = []

# ------------------------------------------------- A: the form arm (P16 semantics)
steps += [
    phase("A: 多方块成型 — place → 缺件现场 → form 64 → 全绑定（P16 form 臂语义）"),
    Step(f"gt6multiblock place {M}", expect="coke oven controller placed"),
    Step(f"gt6multiblock check {M}",
         expect="block_formed=false linked_parts=0/25 first_failed_cell=#0"),
    Step(f"gt6multiblock form {M} 64",
         expect="formed=true okay=true stock 64 -> 39 first_failed_cell=none"),
    Step(f"gt6multiblock check {M}",
         expect="block_formed=true linked_parts=25/25 first_failed_cell=none"),
]

# ------------------------------------------------- B: the live run arm
steps += [
    phase("B: 活运行 — 投 log → 点火 → 驱动 4200t → charcoal + creosote 两侧到账"),
    Step(f"gt6multiblock input 1 minecraft:oak_log {M}", expect="inserted 1"),
    Step(f"gt6multiblock ignite {M}", expect="ignited at"),
    Step(f"gt6multiblock tick 4200 {M}", expect="ticked 4200"),
    # the item side: the output slot holds the charcoal gem (loader-neutral id)
    Step(f"gt6multiblock check {M}", expect="gem_charcoal"),
    # the fluid side: no barrel below -> the creosote stays in the output tank,
    # the fluid-capability stat face reports it (the machineReport tank shape;
    # the fluid subtree grammar is `fluid <pos> stat` — pos BEFORE the literal,
    # GTMultiBlockCommand.java:211-214)
    Step(f"gt6multiblock fluid {M} stat", expect="250mB gt6:creosote"),
]

# ------------------------------------------------- C: the s4-6 laser-gas census arm
steps += [
    phase("C: laser gas census — 两个 comp_laser_gas 注册在位（give-chest 先例，纯 census）"),
    Step(f"setblock {C} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {C} container.0 with gt6:comp_laser_gas_empty 1",
         expect="Replaced"),
    Step(f"item replace block {C} container.1 with gt6:comp_laser_gas_co2 1",
         expect="Replaced"),
    Step(f"data get block {C} Items[0]", expect="gt6:comp_laser_gas_empty"),
    Step(f"data get block {C} Items[1]", expect="gt6:comp_laser_gas_co2"),
]

CHAIN = Chain(
    name="coke-oven-run coke_oven_run",
    slug="coke",
    sites=gt6world.declare_sites(OVEN, CHEST),
    preferred_ports=(25876, 25886),      # this card's pinned rcon/query pair
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
