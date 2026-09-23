#!/usr/bin/env python3
"""p35-energy-tail-machines — the Crystal Charger live acceptance (sweep group
p35_energy_tail; the fresh z=424 band, x520..540 — z-disjoint from the
transformer band z=420 and every roster band):

  A THE CHARGE LEG (the :970 row, T1 = V[1]=32, LU domain): the EMPTY
    energium_red_lv crystal merged into the charger's slot 0 (the p29_w4
    inventory-merge form, the 20.1 tag / 21.1 custom_data carrier fork), the
    charger buffer pinned "gt.energy: 0L" (band 0 pulls from the EMPTY
    crystal — nothing). The LU dial (volt 32) on the south input face fills
    the buffer through the :178-193 intake (the mReceivablePower headroom =
    1 crystal x 32 x 2 = 64, <= 3 packets/tick) — the range probe pins the
    buffer 500..40000 ("Test passed"). The window keeps the dial on past the
    band-7 threshold (35840 = 7 x 5120, the bind3 INTEGER band): the push arm
    (:113-114) drains 1280 per 20-tick phase INTO the crystal — the item-
    carrier range probe pins the crystal charge ABOVE its zero seed (per-leg
    item path: 20.1 tag.gt.energy / 21.1 components custom_data).

  B THE DISCHARGE-EMIT LEG (the :141-151 arm): a charged crystal (64000 LU
    merged) in the second charger, the front face (north) against the T1
    Laser Absorber's BACK input (the absorber = LU->EU, waste=T — the intake
    books even with the EU tail open): band 0 pulls 1280/phase from the
    crystal, the buffer passes mOutput and the emit arm pushes ONE 32-sized
    LU packet per tick (mBatteryCount = 1) into the absorber: the /gt6laser
    stat pins "LU->ENERGY.ELECTRICITY" + "in 32,".

  C THE REGISTRATION CENSUS: seven of the twenty charger items (the T0/t2/t6/
    t10 rungs x small/large) in one chest — the /give half of the family
    registration.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/p35_energy_tail_charger.py
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

Z = 424  # the fresh band (x520..540, z-disjoint from the transformer band z=420)
# A: the charge column — crystal charger [facing=north] + the LU dial on the south input face
A_CH = gt6world.Site(520, 65, Z)         # gt6:crystal_charger_t2 (T1, V[1]=32; front = north = output)
A_DIAL = gt6world.Site(520, 65, Z + 1)   # the 32 LU dial (the south input face)
# B: the discharge-emit column — the charger front feeds the Laser Absorber's back input
B_CH = gt6world.Site(526, 65, Z)         # gt6:crystal_charger_t2 [facing=north]
B_ABS = gt6world.Site(526, 65, Z - 1)    # gt6:laser_absorber[facing=north] (its south back = the LU input)
# C: the registration census chest
CHEST = gt6world.Site(532, 65, Z + 1)

CRYSTAL_ITEM = "gt6:energium_red_lv"  # the T1 red crystal: packet 32, capacity 12800000

def crystal_merge(pos, energy=None):
    """One merged crystal — EMPTY when energy is None (the plain item), else at the
    carrier-known charge. The per-leg item carrier fork (the p29_w4_battery_discharge
    charged_batteries form: 20.1 the stack tag, 21.1 the opaque minecraft:custom_data
    envelope, the SAME gt.energy key)."""
    if energy is None:
        return {
            "1.20.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{CRYSTAL_ITEM}",Count:1b}}]}}}}',
            "1.21.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{CRYSTAL_ITEM}",count:1}}]}}}}',
        }
    return {
        "1.20.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{CRYSTAL_ITEM}",Count:1b,tag:{{gt.energy:{energy}L}}}}]}}}}',
        "1.21.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"{CRYSTAL_ITEM}",count:1,components:{{"minecraft:custom_data":{{gt.energy:{energy}L}}}}}}]}}}}',
    }

def crystal_charge_probe(pos):
    """The crystal charge range probe — per-leg item paths (block inventory NBT is
    the same carrier both legs; the ITEM charge rides tag vs components)."""
    f = F(pos)
    return {
        "1.20.1": f'execute if data block {f} inventory.Items[0].tag.gt.energy match 100..12800000',
        "1.21.1": f'execute if data block {f} inventory.Items[0].components."minecraft:custom_data".gt.energy match 100..12800000',
    }

CRYSTAL_EMPTY = crystal_merge(A_CH, None)       # arm A: the empty crystal (the chargeable seat)
CRYSTAL_FULL = crystal_merge(B_CH, 64000)        # arm B: a 64000 LU crystal (2 packets' worth)
CHARGE_PROBE_A = crystal_charge_probe(A_CH)

steps = [
    Step(f"fill 518 62 {Z - 2} 540 68 {Z + 5} air", expect="filled"),
    Step(f"forceload add 518 {Z - 2} 540 {Z + 5}"),

    phase("A: the charge leg — the LU dial fills the buffer, the band-7 push charges the crystal"),
    Step(f"setblock {F(A_CH)} gt6:crystal_charger_t2[facing=north]", expect="Changed the block"),
    Step(f"data get block {F(A_CH)}", expect="gt.energy: 0L", sleep=1.0),
    Step(CRYSTAL_EMPTY["1.20.1"], expect="Modified block data", node_cmds=CRYSTAL_EMPTY),
    Step(f"gt6energy place {F(A_DIAL)}", expect="GT6 energy source placed"),
    Step(f"gt6energy type {F(A_DIAL)} LU", expect="type ENERGY.LIGHT"),
    Step(f"gt6energy volt {F(A_DIAL)} 32", expect="voltage 32 EU"),
    Step(f"gt6energy mode {F(A_DIAL)} on", expect="emitting true", sleep=6.0),
    # the buffer probe: the <=3-packets/tick intake has banked a bounded window's worth
    Step(f"execute if data block {F(A_CH)} gt.energy match 500..40000", expect="Test passed", sleep=0.5),
    # keep the dial on past the band-7 threshold (35840): the push arm charges the crystal
    Step(f"data get block {F(A_CH)}", expect="gt.energy: ", sleep=45.0),
    Step(f"gt6energy mode {F(A_DIAL)} off", expect="emitting false", sleep=20.0),
    Step(CHARGE_PROBE_A["1.20.1"], expect="Test passed", node_cmds=CHARGE_PROBE_A, sleep=0.5),

    phase("B: the discharge-emit leg — the crystal charge, the front face feeds the absorber"),
    Step(f"setblock {F(B_CH)} gt6:crystal_charger_t2[facing=north]", expect="Changed the block"),
    Step(CRYSTAL_FULL["1.20.1"], expect="Modified block data", node_cmds=CRYSTAL_FULL),
    Step(f"setblock {F(B_ABS)} gt6:laser_absorber[facing=north]", expect="Changed the block"),
    Step(f"gt6laser reset {F(B_ABS)}", expect="GT6 laser accounting reset"),
    Step(f"gt6laser stat {F(B_ABS)}", expect="in 32,", sleep=5.0, poll=30.0),

    phase("C: the registration census — the charger items (the /give half)"),
    Step(f"setblock {F(CHEST)} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {F(CHEST)} container.0 with gt6:crystal_charger 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.1 with gt6:crystal_charger_t2 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.5 with gt6:crystal_charger_t6 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.9 with gt6:crystal_charger_t10 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.10 with gt6:crystal_charger_large 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.11 with gt6:crystal_charger_large_t2 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.19 with gt6:crystal_charger_large_t10 1", expect="Replaced"),
    Step(f"data get block {F(CHEST)} Items[0]", expect="gt6:crystal_charger\""),
    Step(f"data get block {F(CHEST)} Items[19]", expect="gt6:crystal_charger_large_t10"),

    phase("D: teardown — restore the band"),
    Step(f"fill 518 62 {Z - 2} 540 68 {Z + 5} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p35_energy_tail_charger crystal charger",
    slug="p35energytailcharger",
    sites=gt6world.declare_sites(A_CH, A_DIAL, B_CH, B_ABS, CHEST),
    preferred_ports=(26652, 26662),
    game_port=26472,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
