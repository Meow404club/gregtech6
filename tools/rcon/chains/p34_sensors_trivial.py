#!/usr/bin/env python3
"""p34-sensors-trivial-14 — the sensor-batch live acceptance chain (the p26_sensors_core shape).

The card's ACCEPTANCE ③ face: "RCON 新组 p34_sensors 双腿[0,0]（代表件 place+check 臂）".
Ten representative rows of the 15-row batch are placed live and read through /gt6sensor
read (the p26 chain's channel); the other five (kilogibblometer, stackometer, the three
weightometer siblings) share their placed sibling's exact read face and are pinned
offline in GTSensorBatchCensusTest — the representative-arm split, not a coverage gap.

  A the vanilla-world reads: the chronometer (S1) folds its max to the upstream 1440
    (Chronometer:37) and the luminometer (S2) reads the BLOCK light of the dark rig —
    0 with the full-scale 15 (Luminometer:40).
  B the bucketometer pair over a wood barrel (16000 L): 8000 L → value 8 m³ / max 16
    (the /1000 census, Bucketometer:33-40/:47-52); the kilobucketometer on the same
    tank folds to 0/0 (the /1000000 divisor, KiloBucketometer:32-39).
  C the declared baselines: gibblometer 0/0 (the pooled Gibbl producer seam), the
    lightweightometer 0 with the constant 65535 max (WeightometerLight:65), the
    itemometer 0/0 on air, the tpsmeter with the 2000 max (TPSmeter:50), the
    playercounter value 0 (a headless dedicated server has no players), and the
    thermometer whose air-probe max stays 0 (Thermometer:70 — the env arm feeds the
    value ONLY; the biome-dependent value is asserted live but never pinned).

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p34_sensors_trivial.py
      python3 tools/rcon/chains/p34_sensors_trivial.py --node 1.21.1-neoforge
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

# the fresh x=680 band at z 127-141, y = 64 (the flat-plate rig floor every chain
# shares) — clear of the p26 sensor band (x432..436) and every registered neighbour
# (the x=6xx electric/prospector bands sit at z 240/364).
S1 = gt6world.Site(680, 64, 127)  # the chronometer
S2 = gt6world.Site(680, 64, 129)  # the luminometer
T = gt6world.Site(680, 64, 133)   # the wood barrel
S3 = gt6world.Site(680, 64, 132)  # the bucketometer — north of T, probe south → T
S4 = gt6world.Site(680, 64, 134)  # the kilobucketometer — probe flipped north → T
S5 = gt6world.Site(680, 64, 136)  # the gibblometer
S6 = gt6world.Site(680, 64, 137)  # the lightweightometer
S7 = gt6world.Site(680, 64, 138)  # the tpsmeter
S8 = gt6world.Site(680, 64, 139)  # the playercounter
S9 = gt6world.Site(680, 64, 140)  # the itemometer
S10 = gt6world.Site(680, 64, 141) # the thermometer
S1F = F(S1)
T_F = F(T)
S3F = F(S3)
S4F = F(S4)

steps = []

# ------------------------------------------------- A: the vanilla-world reads
steps += [
    phase("A: the vanilla-world reads — chronometer day cycle, luminometer block light"),
    Step(f"gt6sensor place chronometer {S1F}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {S1F}", expect="max 1440"),          # Chronometer:37, the value rides the live clock
    Step(f"gt6sensor place luminometer {F(S2)}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {F(S2)}", expect="value 0, max 15"), # the rig is torch-dark; the full scale is 15
]

# ------------------------------------------------- B: the bucketometer pair
steps += [
    phase("B: the bucketometer pair — the barrel census at the m³ and kilo-m³ grains"),
    Step(f"setblock {T_F} gt6:barrel_wood", expect="Changed the block"),
    Step(f"gt6sensor place bucketometer {S3F}", expect="GT6 sensor placed at"),
    Step(f"gt6tank fill {T_F} minecraft:water 8000", expect="filled 8000/8000"),
    Step(f"gt6sensor read {S3F}", expect="value 8, max 16"),   # 8000/1000 and 16000/1000 (the /1000 census)
    Step(f"gt6sensor place kilobucketometer {S4F}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor facing {S4F} 3", expect=": 3 ok, probe 2"),  # the wrench arm: facing south, the probe folds north → the barrel
    Step(f"gt6sensor read {S4F}", expect="value 0, max 0"),    # 16000/1000000 → the kilo grain reads 0
]

# ------------------------------------------------- C: the declared baselines
steps += [
    phase("C: the declared baselines — the pooled-seam zeros and the constant maxes"),
    Step(f"gt6sensor place gibblometer {F(S5)}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {F(S5)}", expect="value 0, max 0"),  # the pooled Gibbl producer seam
    Step(f"gt6sensor place lightweightometer {F(S6)}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {F(S6)}", expect="value 0, max 65535"),  # WeightometerLight:65 constant
    Step(f"gt6sensor place itemometer {F(S9)}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {F(S9)}", expect="value 0, max 0"),  # air probe, no handler
    Step(f"gt6sensor place tpsmeter {F(S7)}", expect="GT6 sensor placed at",
         sleep=2),  # the sample gate rides aTimer % getTickRate() = aTimer % 20 (GTSensorBlockEntity
                    # :162 + Tpsmeter getTickRate) — the cached pair /gt6sensor read prints
                    # (GTSensorBlockEntity :263/:268) fills at the FIRST sample, >= 1 s after
                    # the place; every other arm samples every tick (getTickRate < 2 gate)
    Step(f"gt6sensor read {F(S7)}", expect="max 2000"),        # TPSmeter:50, the 20 TPS reference
    Step(f"gt6sensor place playercounter {F(S8)}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {F(S8)}", expect="value 0"),         # headless: no players (the max is server-defined)
    Step(f"gt6sensor place thermometer {F(S10)}", expect="GT6 sensor placed at"),
    Step(f"gt6sensor read {F(S10)}", expect="max 0"),          # Thermometer:70 — the env arm never carries a max
]

# ------------------------------------------------- D: teardown
steps += [
    phase("D: teardown — the explicit band restore"),
    Step("fill 678 62 125 684 66 143 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p34-sensors-trivial",
    slug="p34sensors",
    sites=gt6world.declare_sites(S1, S2, T, S3, S4, S5, S6, S7, S8, S9, S10),
    preferred_ports=(26405, 26415),      # this card's pinned rcon/query pair (the free 2640x slot)
    mutates=(),                          # no global regime flips — plain place/read/fill
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
