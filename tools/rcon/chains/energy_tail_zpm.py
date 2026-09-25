#!/usr/bin/env python3
"""energy-tail-zpm — the ZPM + Decharger live acceptance (sweep group
energy_tail; the RESERVED energy_tail_zpm seat CLAIMED by task
energy-zpm-dechargers; the fresh x544..558 z416..428 band — x-disjoint
from the P35 band columns x520..540):

  A THE ZPM + THE QUANTUM DECHARGER (the :1103 item + the :1000 row): the
    full ZPM merged into the decharger's slot (the gt.active.energy
    store-as-full carrier, the DungeonData.zpm dungeon lane) — the band-0
    pull drains the artifact, the buffer lands EXACTLY at 80 packets
    (10485760L: 40 + 20 + 20, the bind3 bands 0/1 pull then the band-2 idle
    — no network sink for QU at the V[7] packet band exists in this port:
    the massfab tops at t5 max 2048, the absorber is LU-typed, the boxes
    close at IV — the QU EMIT arm is the OFFLINE pin
    GT6ZpmDechargerTest.theSplitTypeLanesRestoreTheBase202Face, declared)
    and the item's own carrier flips gt.active.energy to 0b and reads
    EXACTLY 1999989514240 (2e12 - 80 x 131072 — the packet algebra verbatim).

  B THE EU DISCHARGE LEG (the :1001 row, QU-in/EU-out): the decharger's
    front feeds the t7 front (the V[7] input, the P35 reversed-leg geometry:
    the FRONT is the INPUT), the t7/t6/t5 ladder steps 131072 -> 32768 ->
    8192 -> 2048 into the battery_box_ev with a chargeable battery_iv twin
    merged in (the :179 headroom carrier). The decharger's gt.active flips 1
    (the buffer >= V[7]); the /data merge monkey-wrench pins gt.stopped (the
    P28 NBT face) which FREEZES the whole column; the score probe pins the
    box buffer >= 2048 (ONE delivered packet) and the decharger buffer lands
    at the same EXACT 10485760 (the emit now off, the pull runs to band 2).

  C THE COVERS ARM (the P35 covers card's unreachable non-zero face): the
    energy display + the energy sensor INSTALL on the decharger (the
    capacitor-host admission — the CoverMachineLanes deviation closed) and
    the check reads the NON-ZERO lanes live: the display gauge n:3s (level 3
    across the whole freeze-parity settle window 80..106 packets) and the
    sensor DOWN-face key 0 present (non-zero lanes only, CoverData :90) —
    the non-zero live reading the card acceptance ⑤ demands.

  D THE REGISTRATION CENSUS: the ZPM + both decharger items in one chest —
    the /give half of the family registration.

passes=2 is the idempotency proof (the [0,0] of the group).
Run:  GT6_SESSION=off python3 tools/rcon/chains/energy_tail_zpm.py
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

Z = 420  # the fresh band column (x544..558, x-disjoint from the P35 band x520..540)
# A: the quantum column — the QU decharger with no emit target (the declared offline emit)
Q_DECH = gt6world.Site(554, 65, Z)
# B: the EU column — box_ev <- t5 <- t6 <- t7 <- decharger_eu (the FRONT = emit into the t7 front)
E_BOX = gt6world.Site(548, 65, Z)          # gt6:battery_box_ev[facing=north] (V[4] band [1024..4096])
E_T5 = gt6world.Site(548, 65, Z + 1)       # electric_transformer_t5[facing=south] (in V[5], out V[4])
E_T6 = gt6world.Site(548, 65, Z + 2)       # electric_transformer_t6[facing=south] (in V[6], out V[5])
E_T7 = gt6world.Site(548, 65, Z + 3)       # electric_transformer_t7[facing=south] (in V[7], out V[6])
E_DECH = gt6world.Site(548, 65, Z + 4)     # gt6:zpm_decharger_electric[facing=north] (front = the emit face)
# D: the registration census chest
CHEST = gt6world.Site(554, 65, Z + 4)

ZPM_CAP = 2_000_000_000_000
PACKET = 131072                              # V[7]
SETTLE = ZPM_CAP - 80 * PACKET               # 1999989514240 — the exact 40+20+20 packet algebra


def zpm_merge(pos):
    """One merged ZPM at the FULL lane (the DungeonData.zpm active face — the 2/3 dungeon
    dice collapsed to always-full, the declared deviation) in the Size:1 decharger slot.
    The per-leg item carrier fork (the charger chain's crystal_merge form)."""
    item = {"1.20.1": f'tag:{{gt.active.energy:1b}}',
            "1.21.1": f'components:{{"minecraft:custom_data":{{gt.active.energy:1b}}}}'}
    return {
        "1.20.1": f'data merge block {F(pos)} {{inventory:{{Size:1,Items:[{{Slot:0b,id:"gt6:zpm",Count:1b,{item["1.20.1"]}}}]}}}}',
        "1.21.1": f'data merge block {F(pos)} {{inventory:{{Size:1,Items:[{{Slot:0b,id:"gt6:zpm",count:1,{item["1.21.1"]}}}]}}}}',
    }


def battery_merge(pos):
    """One EMPTY battery_ev merged into the box's slot 0 (the :179 mReceivablePower seat —
    the P29 W4 EMPTY_BATTERY merge form, the carrier fork)."""
    return {
        "1.20.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:battery_lead_acid_ev",Count:1b}}]}}}}',
        "1.21.1": f'data merge block {F(pos)} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:battery_lead_acid_ev",count:1}}]}}}}',
    }


Q_FULL = zpm_merge(Q_DECH)
E_FULL = zpm_merge(E_DECH)
BOX_BATTERY = battery_merge(E_BOX)

steps = [
    Step(f"fill 544 62 {Z - 2} 558 68 {Z + 6} air", expect="filled"),
    Step(f"forceload add 544 {Z - 2} 558 {Z + 6}"),
    Step("scoreboard objectives add gt6zpm dummy", allow_failed=True),

    # ------------------------------------------------------------------
    phase("A: the quantum decharger — the artifact drains to the exact 80-packet settle"),
    Step(f"setblock {F(Q_DECH)} gt6:zpm_decharger_quantum[facing=north]", expect="Changed the block"),
    Step(Q_FULL["1.20.1"], expect="Modified block data", node_cmds=Q_FULL),
    # the drain ran: the buffer paid 40 + 20 + 20 packets and the band-2 idle froze it —
    # the EXACT settle (the packet algebra verbatim; no QU sink exists at V[7], the emit
    # arm is the offline pin, declared)
    Step(f'data get block {F(Q_DECH)} "gt.energy"', expect="10485760L", poll=30.0,
         label="the settle = exactly 80 packets (40+20+20, the band-2 idle; the path get prints the bare value)"),
    # the artifact carrier wrote the drain back: the active lane flipped off, the exact
    # remainder (2e12 - 80 x 131072)
    Step(f"data get block {F(Q_DECH)} inventory", expect="gt.active.energy: 0b", poll=10.0,
         node_expects={"1.20.1": "gt.active.energy: 0b", "1.21.1": "gt.active.energy"}),
    Step(f"data get block {F(Q_DECH)} inventory", expect=str(SETTLE)),
    Step(f"data get block {F(Q_DECH)}", expect="gt.active: 1b"),

    # ------------------------------------------------------------------
    phase("B: the EU discharge leg — the decharger front feeds the t7-t6-t5 ladder into the box"),
    Step(f"setblock {F(E_BOX)} gt6:battery_box_ev[facing=north]", expect="Changed the block"),
    Step(f"setblock {F(E_T5)} gt6:electric_transformer_t5[facing=south]", expect="Changed the block"),
    Step(f"setblock {F(E_T6)} gt6:electric_transformer_t6[facing=south]", expect="Changed the block"),
    Step(f"setblock {F(E_T7)} gt6:electric_transformer_t7[facing=south]", expect="Changed the block"),
    Step(f"setblock {F(E_DECH)} gt6:zpm_decharger_electric[facing=north]", expect="Changed the block"),
    Step(BOX_BATTERY["1.20.1"], expect="Modified block data", node_cmds=BOX_BATTERY),
    Step(E_FULL["1.20.1"], expect="Modified block data", node_cmds=E_FULL),
    # the buffer crossed mOutput: the discharge ran on the EU row too
    Step(f"data get block {F(E_DECH)}", expect="gt.active: 1b", poll=20.0),
    # the monkey-wrench freeze (the P28 NBT face): the emit arm stops, the column freezes
    Step(f"data merge block {F(E_DECH)} {{gt.stopped: 1b}}", expect="Modified block data", sleep=6.0),
        # (the 6s settle: the pull continues post-freeze to the band-2 stop — two more
        # exchange phases max; the score then samples the settled buffer)
    # the box got paid: at least ONE 2048-sized packet landed (the t5 -> box delivery);
    # the store suppresses the data-get echo (no expect), the if carries the assertion
    Step(f'execute store result score boxE gt6zpm run data get block {F(E_BOX)} "gt.energy"'),
    Step("execute if score boxE gt6zpm matches 2048..", expect="Test passed", poll=10.0),
    # the stopped decharger settles at >= 80 packets (the pull runs to the band-2 idle,
    # the freeze-parity picks 80..106) — the range is the deterministic face
    Step(f'execute store result score dechE gt6zpm run data get block {F(E_DECH)} "gt.energy"'),
    Step("execute if score dechE gt6zpm matches 10485760..", expect="Test passed", poll=30.0),

    # ------------------------------------------------------------------
    phase("C: the covers arm — the capacitor host admits both energy covers, the lanes read non-zero"),
    Step(f"gt6cover install {F(E_DECH)} up gt6:cover_energy_display", expect="ok=true",
         label="the capacitor-host admission — the P35 unreachable face opens"),
    Step(f"gt6cover install {F(E_DECH)} down gt6:cover_scale_energy", expect="ok=true"),
    # the display gauge: level 3 at the settled 80-packet buffer ((320-80)*9/320 -> 9-6)
    Step(f"gt6cover check {F(E_DECH)}", expect="n:3s", poll=15.0,
         label="the display reads the buffer lane NON-ZERO (the P35 card's unreachable face)"),
    # the sensor value: the DOWN-face key 0 written only when non-zero (the freeze-parity
    # settle spans scale 4 (80-91 pkts) or 5 (92-106 pkts) — the key presence IS the
    # non-zero live reading)
    Step(f"gt6cover check {F(E_DECH)}", expect="0:", poll=15.0,
         label="the scale reads the same lane non-zero (key 0 = DOWN face, non-zero lanes only)"),

    # ------------------------------------------------------------------
    phase("D: the registration census — the ZPM + the two decharger items"),
    Step(f"setblock {F(CHEST)} minecraft:chest", expect="Changed the block"),
    Step(f"item replace block {F(CHEST)} container.0 with gt6:zpm 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.1 with gt6:zpm_decharger_electric 1", expect="Replaced"),
    Step(f"item replace block {F(CHEST)} container.2 with gt6:zpm_decharger_quantum 1", expect="Replaced"),
    Step(f"execute if data block {F(CHEST)} Items[{{id:\"gt6:zpm\"}}]", expect="Test passed"),
    Step(f"execute if data block {F(CHEST)} Items[{{id:\"gt6:zpm_decharger_electric\"}}]", expect="Test passed"),
    Step(f"execute if data block {F(CHEST)} Items[{{id:\"gt6:zpm_decharger_quantum\"}}]", expect="Test passed"),

    # ------------------------------------------------------------------
    phase("E: teardown — restore the band"),
    Step(f"fill 544 62 {Z - 2} 558 68 {Z + 6} air", expect="filled"),
    Step("forceload remove all"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="energy_tail_zpm zpm decharger",
    slug="energytailzpm",
    sites=gt6world.declare_sites(Q_DECH, E_BOX, E_T5, E_T6, E_T7, E_DECH, CHEST),
    preferred_ports=(26672, 26682),
    game_port=26492,
    steps=steps,
)

if __name__ == "__main__":
    main(CHAIN)
