#!/usr/bin/env python3
"""p13-steam-proof-repay — the steam-proof deviation repayment acceptance chain.

Closes the 2026-09-02-p12-steam-proof-deviation window: pre-card a gas-proof drum
STORED gt6:steam (the deviation); post-card the tick fizz pair voids it. The arms:

  A metal drum + gt6:steam  -> fill ACCEPTED (the world face has NO fill-time gate,
     upstream fills first and the tick judgment is the protection) -> one tick ->
     stat asserts 0 L: the gas gate is skipped (gasProof T) and the :184 allowFluid
     gate (POWER_CONDUCTING list) fizzes the tank (upstream :181-182 trash).
  B wood barrel + gt6:steam -> fill ACCEPTED -> the 373 K >= 340 K MELT fires BEFORE
     the gas gate (upstream :162 first) — the barrel burns down, the BE is gone:
     the melt chain and the name-list chain are orthogonal, order pinned.
  C metal drum + water      -> both gates pass, zero consumption after ticks.
  D natural gas vs wood     -> the GAS-list gate alone voids it (300 K < 340 K: no
     melt; natural_gas is GAS but not POWER_CONDUCTING).
  E the four-family gas-proof stat values (wood F / plastic T / metal T / logistics T)
     — the live carrier read, offline tests pin the class-truth fallback.

Pass 2 is the idempotency proof (the framework reruns everything on the cleaned
world). Ports are this card's pinned pair (25730 rcon / 25740 query).

Run:  python3 tools/rcon/chains/p13_steam_proof_repay.py
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

METAL = gt6world.Site(40, 64, 40)
WOOD = gt6world.Site(44, 64, 40)
NATGAS = gt6world.Site(48, 64, 40)
PLASTIC = gt6world.Site(52, 64, 40)
LOGI = gt6world.Site(56, 64, 40)
M, W, G, P, L = F(METAL), F(WOOD), F(NATGAS), F(PLASTIC), F(LOGI)


CHAIN = Chain(
    name="p13-steam-proof-repay",
    slug="p13steamrepay",
    sites=gt6world.declare_sites(METAL, WOOD, NATGAS, PLASTIC, LOGI),
    preferred_ports=(25730, 25740),      # (rcon, query) — this card's pinned pair; game defaults to rcon-10 = 25720
    steps=[
        phase("A: 金属鼓注汽 — fill 无门收下、tick 后 powerconducting fizz 清零（本卡前的偏离窗口关闭）"),
        Step(f"setblock {M} gt6:barrel_metal", expect="Changed the block"),
        Step(f"gt6tank stat {M}", expect="gas-proof true"),                    # the :2151 carrier row, live
        Step(f"gt6tank fill {M} gt6:steam 1000", expect="filled 1000/1000 L of gt6:steam", sleep=2),
        Step(f"gt6tank stat {M}", expect="0/64000 L of nothing"),              # the :184 allowFluid fizz voided it

        phase("B: 木桶注汽 — 373K≥340K 熔毁先于 gas 查（名单链与熔毁链正交性证明）"),
        Step(f"setblock {W} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank stat {W}", expect="gas-proof false"),                   # the :2136-2149 wood rows
        Step(f"gt6tank fill {W} gt6:steam 1000", expect="filled 1000/1000 L of gt6:steam", sleep=2),
        Step(f"execute if block {W} gt6:barrel_wood", expect="Test failed"),   # melted down — the block is gone
        Step(f"gt6tank stat {W}", expect="No GT6 barrel BlockEntity"),         # the BE burned with it

        phase("C: 水回归臂 — 金属鼓装水静置不销毁（两查全过零消耗）"),
        Step(f"gt6tank fill {M} minecraft:water 5000", expect="filled 5000/5000", sleep=2),
        Step(f"gt6tank stat {M}", expect="5000/64000 L of minecraft:water"),

        phase("D: natural_gas 气查臂 — GAS 名单独立于 POWER_CONDUCTING（300K<340K 不熔，气查 fizz）"),
        Step(f"setblock {G} gt6:barrel_wood", expect="Changed the block"),
        Step(f"gt6tank fill {G} gt6:natural_gas 1000", expect="filled 1000/1000 L of gt6:natural_gas", sleep=2),
        Step(f"gt6tank stat {G}", expect="0/16000 L of nothing"),              # the :180 gas gate, not the melt

        phase("E: 四族 gasproof 载体值 live 断言"),
        Step(f"setblock {P} gt6:barrel_plastic", expect="Changed the block"),
        Step(f"gt6tank stat {P}", expect="gas-proof true"),                    # the :2150 row
        Step(f"setblock {L} gt6:barrel_logistics", expect="Changed the block"),
        Step(f"gt6tank stat {L}", expect="melting point 100000 K, gas-proof true"),  # the :2171 row
    ],
)


if __name__ == "__main__":
    main(CHAIN)
