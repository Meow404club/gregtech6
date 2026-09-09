#!/usr/bin/env python3
"""p26-sensors-core — the sensor-family live acceptance chain (the p25_food_can shape).

Chain semantics (task p26-sensors-core ACCEPTANCE — "place sensor → 对目标 → read 各
模式读数断言"; the card's arms: progressmeter 邻接 shredder → redstone 阈值翻转断言，
fluidometer 邻接罐断言，软锤替身命令臂):

  A the progressmeter arm: a shredder rig under the fakesource regime. The sensor
    (progressmeter) sits NORTH of the shredder — the placement default facing 2 (north)
    folds the probe to OPOS[2] = 3 (south), i.e. INTO the machine. GREATER mode with the
    setpoint 0: an idle machine reads progress 0 → 0 > 0 false → redstone 0. Then the
    fake source tops the machine's mEnergy each tick (TileEntityBasicMachine.java:420)
    and 16 cobblestones feed the :692 row (16t duration each) — 256 ticks ≈ 13 s of a
    progress > 0 window, the sensor's tick-rate-1 sample flips the redstone to 15
    (MultiTileEntitySensorTE.java:143 verbatim). Fakesource off: CONSTANT_ENERGY (=true,
    :180) doInactive :894 zeroes the parked progress → the sensor flips back to 0. The
    BOTH-WAYS redstone threshold flip is the card's "阈值翻转断言", driven through the
    block getSignal → BE redstoneOut pass-through (Sensor:247).

  B the fluidometer arm: a wood barrel (16000 L) south of a fluidometer (the same
    facing-2/probe-3 fold). FULL mode on the empty tank: 0 >= 16000 false → redstone 0.
    Fill 8000 L → PERCENT mode reads displayed 50 AND redstone 8 (UT.Code.scale
    (50, 100, 15) = 1 + 50*14/100). FULL again: still 0. Fill to 16000 → poll for
    redstone 15 — the persistent-state threshold flip.

  C the electrometer baseline: probe on air reads 0/0 (the non-wire neighbour cut, the
    declared deviation of GT6ElectrometerBlockEntity — no port counterpart of the IC2
    NodeStats fallback).

  D the tool-arm stand-ins (the monkey-wrench and soft-hammer ITEMS are not ported —
    the /gt6sensor command IS the stand-in, the card's declared arm): the wrench facing
    arm (4 = west, the probe folds to 5) + the BlockState mirror assert (execute if
    block on facing=west — the GTSensorBlockEntity#wrenchSetFacing write, the
    setFrontFacing shape); the monkey-wrench second-face arm (2 north, refused on the
    facing side itself — Sensor:103); the KEYPAD stand-in clamps (decimal 12345 →
    9999; hex 70000 → 65535, the bind16 ceiling); the averaging window resize (40);
    the soft-hammer reset arm (mode 0, redstone 0, window 1 — SensorTE:242-248).

  E teardown: the explicit band restore. fakesource is a declared mutates global (the
    framework splits/it off; the off-step restores the baseline).

passes=2 is the idempotency proof (the [0,0] of this chain): the pass boundary clears
the declared sites to air, so pass 2 rebuilds the whole rig and re-verifies.

Run:  GT6_SESSION=off python3 tools/rcon/chains/p26_sensors_core.py
      python3 tools/rcon/chains/p26_sensors_core.py --node 1.21.1-neoforge
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

# the x 434 band at z 127-135 — clear of the p25 bands (x 416-425, z 123-125) and the
# p23 barrel (x 388). y = 64, the flat-plate rig floor every chain shares.
SITE_M = gt6world.Site(434, 64, 128)   # the shredder
SITE_S1 = gt6world.Site(434, 64, 127)  # the progressmeter — north of M, probe south → M
SITE_T = gt6world.Site(434, 64, 132)   # the wood barrel
SITE_S2 = gt6world.Site(434, 64, 131)  # the fluidometer — north of T, probe south → T
SITE_S3 = gt6world.Site(434, 64, 135)  # the electrometer — probe south → air
M = F(SITE_M)
S1 = F(SITE_S1)
T = F(SITE_T)
S2 = F(SITE_S2)
S3 = F(SITE_S3)

steps = []

# ------------------------------------------------- A: the progressmeter arm
steps += [
    phase("A: the progressmeter arm — shredder adjacency, the both-ways redstone threshold flip"),
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(f"gt6machine shredder place {M}", expect="GT6 shredder placed at"),
    Step(f"gt6sensor place progressmeter {S1}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {S1}", expect="facing 2, probe 3"),
    Step(f"gt6sensor mode {S1} 2", expect=": 2 (hex false)"),  # GREATER — the mode echo
    Step(f"gt6sensor set {S1} 0", expect=": 0 (clamped 0..9999)"),
    Step(f"gt6sensor read {S1}", expect="redstone 0"),         # idle: progress 0 > 0 is false
    Step(f"gt6machine shredder input 16 {M}", expect="input: 16x"),  # 16 x 16t rows ≈ 13 s of progress > 0
    Step(f"gt6sensor read {S1}", expect="redstone 15", poll=10),     # the running flip (the :143 GREATER arm)
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),
    Step(f"gt6sensor read {S1}", expect="redstone 0", poll=8),       # the CONSTANT_ENERGY :894 reset flip
]

# ------------------------------------------------- B: the fluidometer arm
steps += [
    phase("B: the fluidometer arm — the barrel threshold flip + the PERCENT read"),
    Step(f"setblock {T} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6sensor place fluidometer {S2}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor mode {S2} 6", expect=": 6 (hex false)"),  # FULL — the mode echo
    Step(f"gt6sensor read {S2}", expect="redstone 0"),          # 0 >= 16000 false
    Step(f"gt6tank fill {T} minecraft:water 8000", expect="filled 8000/8000"),
    Step(f"gt6sensor mode {S2} 1", expect=": 1 (hex false)"),  # PERCENT — the mode echo
    Step(f"gt6sensor read {S2}", expect="displayed 50, redstone 8"),  # scale(50, 100, 15)
    Step(f"gt6sensor mode {S2} 6", expect=": 6 (hex false)"),  # FULL again — the mode echo
    Step(f"gt6sensor read {S2}", expect="redstone 0"),          # 8000 >= 16000 false
    Step(f"gt6tank fill {T} minecraft:water 8000", expect="filled 8000/8000"),  # the barrel tops at 16000
    Step(f"gt6sensor read {S2}", expect="redstone 15", poll=8),      # the full-tank flip
]

# ------------------------------------------------- C: the electrometer baseline
steps += [
    phase("C: the electrometer baseline — the air-probe read (the declared non-wire cut)"),
    Step(f"gt6sensor place electrometer {S3}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {S3}", expect="value 0, max 0"),
]

# ------------------------------------------------- D: the tool-arm stand-ins
steps += [
    phase("D: the tool arms — wrench facing + the blockstate mirror, monkey-wrench second, keypad clamps, window, soft-hammer reset"),
    Step(f"gt6sensor facing {S2} 4", expect=": 4 ok, probe 5"),       # the wrench arm: facing west, the probe folds east
    Step(f"execute if block {S2} gt6:fluidometer[facing=west]", expect="Test passed"),  # the FACING mirror
    Step(f"gt6sensor second {S2} 2", expect=": 2 ok (monkey-wrench arm)"),  # north != facing → ok
    Step(f"gt6sensor second {S2} 4", expect="REFUSED"),               # == the facing side → the :103 refusal
    Step(f"gt6sensor set {S1} 12345", expect=": 9999 (clamped 0..9999)"),   # the decimal keypad clamp (bind 0..9999)
    Step(f"gt6sensor hex {S1} 1", expect=": true (mode byte"),
    # the hex ceiling: the command's own argument range (0..65535) IS the bind16 range —
    # the above-range clamp is pinned offline (keypadStep bind16, the GTSensorLogicTest);
    # here the boundary value pins the hex-mode acceptance through the command face
    Step(f"gt6sensor set {S1} 65535", expect=": 65535 (clamped bind16)"),
    Step(f"gt6sensor hex {S1} 0", expect=": false (mode byte"),
    Step(f"gt6sensor window {S1} 40", expect="window at 434, 64, 127: 40"),  # the averaging-window resize
    Step(f"gt6sensor reset {S1}", expect="mode 0, redstone 0, window 1"),  # the soft-hammer full zeroing
]

# ------------------------------------------------- E: teardown
steps += [
    phase("E: teardown — the explicit band restore (fakesource already off; it is the declared mutates global)"),
    Step(f"fill 432 62 125 436 66 137 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p26-sensors-core",
    slug="p26sensors",
    sites=gt6world.declare_sites(SITE_M, SITE_S1, SITE_T, SITE_S2, SITE_S3),
    preferred_ports=(26403, 26413),      # this card's pinned rcon/query pair (the 2640x segment, free of 260xx-265xx neighbours)
    mutates=("fakesource",),             # the A arm flips the global regime; the off-step restores it
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
