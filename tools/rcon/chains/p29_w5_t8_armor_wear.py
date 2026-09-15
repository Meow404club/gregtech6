#!/usr/bin/env python3
"""p29-w5-t8-armor-wear — the Hazmat universal wear + hazard-tag readout arm (task
p29-w5-t8-armor-24; the card's 替身 face — RCON has no player, the armor stand IS the
substitute, and the card asserts the EQUIPMENT STATE against the judgment seam's read,
never a damage reduction, the guard face being unwired by ruling):

  A THE WEAR SMOKE (穿脱): a stand takes the four universal pieces; ArmorItems reads
    them back; the helmet comes off (the head entry reads minecraft:air) and goes on.
  B THE FIRE PROXIMITY (岩浆外火): the suited stand idles next to fire for a beat;
    the equipment state persists (NO damage-reduction assertion — the card cut).
  C THE JUDGMENT-SEAM READOUT: /gt6tags dump of #gt6:hazmat/lightning (universal
    ONLY, the :106 join), #gt6:hazmat/radiation and #gt6:hazmat/gas (base suit +
    universal, 8 members) and #gt6:hazmat/insects — the tag face of the seam, whose
    parity with the API face the offline ArmorSetTest pins; the worn ids being
    exactly the universal members is what makes the API answer TRUE for the stand.
  D teardown: the stand dies, the band restores.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p29_w5_t8_armor_wear.py
"""

import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
for _path in (str(_HERE), str(_HERE.parent)):
    if _path not in sys.path:
        sys.path.insert(0, _path)

import gt6world
from framework import Chain, Step, main, phase

STAND = "410 65 332"          # the fresh z=332 band, x 410..420
FIRE = "412 65 332"           # one air block east of the stand (adjacency, not underfoot)
SEL = "@e[type=armor_stand,limit=1,sort=nearest]"

UNIVERSAL = {
    "head": "gt6:hazmat_universal_helmet",
    "chest": "gt6:hazmat_universal_chestplate",
    "legs": "gt6:hazmat_universal_leggings",
    "feet": "gt6:hazmat_universal_boots",
}

steps = [
    phase("A: the wear smoke — the stand takes the full universal set"),
    Step(f"summon minecraft:armor_stand {STAND} {{ShowArms:1b}}", expect="Summoned new entity"),
    Step(f"item replace entity {SEL} armor.head with {UNIVERSAL['head']} 1", expect="Replaced slot"),
    Step(f"item replace entity {SEL} armor.chest with {UNIVERSAL['chest']} 1", expect="Replaced slot"),
    Step(f"item replace entity {SEL} armor.legs with {UNIVERSAL['legs']} 1", expect="Replaced slot"),
    Step(f"item replace entity {SEL} armor.feet with {UNIVERSAL['feet']} 1", expect="Replaced slot"),
    Step(f"data get entity {SEL} ArmorItems", expect="gt6:hazmat_universal_helmet"),
    Step(f"data get entity {SEL} ArmorItems", expect="gt6:hazmat_universal_boots"),
    # the doff half: ArmorItems is ordered feet/legs/chest/head, so entry [3] IS the head
    # slot — after the doff it reads the empty minecraft:air stack
    Step(f"item replace entity {SEL} armor.head with minecraft:air 1", expect="Replaced slot"),
    Step(f"data get entity {SEL} ArmorItems[3]", expect="minecraft:air"),
    Step(f"item replace entity {SEL} armor.head with {UNIVERSAL['head']} 1", expect="Replaced slot"),
    Step(f"data get entity {SEL} ArmorItems[3]", expect="gt6:hazmat_universal_helmet"),

    phase("B: the fire proximity — the suited stand idles beside open fire (equipment state persists)"),
    Step(f"setblock {FIRE} minecraft:fire", expect="Changed the block"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step("time query daytime", expect="The time is"),
    Step(f"data get entity {SEL} ArmorItems", expect="gt6:hazmat_universal_chestplate"),
    Step(f"setblock {FIRE} minecraft:air", expect="Changed the block"),

    phase("C: the judgment-seam readout — the hazard tag faces the worn state must satisfy"),
    Step("gt6tags dump gt6:hazmat/lightning",
         expect="4 members [gt6:hazmat_universal_boots, gt6:hazmat_universal_chestplate, gt6:hazmat_universal_helmet, gt6:hazmat_universal_leggings]"),
    Step("gt6tags dump gt6:hazmat/radiation", expect="8 members"),
    Step("gt6tags dump gt6:hazmat/radiation", expect="gt6:hazmat_radiation_helmet"),
    Step("gt6tags dump gt6:hazmat/gas", expect="8 members"),
    Step("gt6tags dump gt6:hazmat/gas", expect="gt6:hazmat_biochemgas_helmet"),
    Step("gt6tags dump gt6:hazmat/insects", expect="8 members"),

    phase("D: teardown — the stand dies, the band restores (no global state was touched)"),
    Step("kill @e[type=armor_stand]", expect="Killed"),
    Step("fill 408 62 329 420 68 336 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p29-w5-t8-armor-wear p29_w5_t8_armor",
    slug="p29w5t8armorwear",
    sites=gt6world.declare_sites(gt6world.Site(410, 65, 332)),
    preferred_ports=(26424, 26434),
    game_port=26414,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
