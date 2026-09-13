#!/usr/bin/env python3
"""p29-w1-mixer-pair — the SHARED-MAP 对拍 chain (task p29-w1-eu-hu-families ACCEPTANCE ①):
the kinetic Mixer (RU, :1392-1395, efficiency null = the :96 10000 identity) and the
Electric Mixer (EU, :1504-1508, NBT_EFFICIENCY 5000) ride the ONE GT6RecipeMaps.MIXER
map, and the SAME C-Foam base rock row (the :252-253 Stone/Sand/Clay small row — the
p26_kitchen_pot live-proven item set: 6x dust_stone + 2x dust_sand + 1x dust_small_clay
+ water 1000 → gt6:cfoam 1000, eUt 16, duration 128) runs on BOTH.

The efficiency-direction 对拍 (the card-A review erratum — units(a, orig, targ) =
a × targ/orig, UT.java:1677 + LH.java:311 double evidence: 5000 = 2× the REQUIRED
progress = half speed / 2× the energy-time, NOT a 2× speed-up):

  kinetic  T1 budget units(16×128×1, 10000, 10000, T) = 2048 → 32 ticks @ 64 EU/tick
  electric T1 budget units(16×128×1,  5000, 10000, T) = 4096 → 64 ticks @ 64 EU/tick

  after 16 injected ticks each: kinetic progress=1024/2048 (half bar),
  electric progress=1024/4096 (quarter) — the raw maxProgress ratio 2:1;
  after 32 injected ticks: kinetic COMPLETED (gt6:cfoam 1000 in the output tank,
  progress parked 0/0) while electric sits at EXACTLY progress=2048/4096 —
  the same wall-clock covers half the bar (墙钟半程).

  the retained-vs-reset asymmetry the live run pinned (the rcon-session-1201
  960df463 red): the kinetic mixer KEEPS progress across the command-idle ticks
  (RU machines ride mNoConstantEnergy, so doInactive's CONSTANT_ENERGY :894
  reset does NOT apply) — its phase-A 16 ticks are still in the bar when phase
  C re-injects, so the phase-C kinetic arm IS the completion tick (:539
  mProgress -= mMaxProgress parks 0/0 in-report). the ELECTRIC machine does
  idle-reset to 0 between commands (:894), which is exactly why its phase-D
  32-tick arm lands on EXACTLY 2048/4096 regardless of idle-tick jitter.

  the 8 EU wall (the ULV regression face): 40× 8 EU packets into the electric T1
  (window {16,32,64}) never cross mInputMin — progress stays 0/0.

  the T1/T2 windows: minIn=16 recIn=32 maxIn=64 / minIn=64 recIn=128 maxIn=256.

The inventory merges ride the loader-versioned key shapes (the p19_nbt_rebind ruling:
1.20.1 `Count:6b` vs 1.21.1 `count:6`). pass 2 = idempotency. Run:
  GT6_SESSION=off python3 tools/rcon/chains/p29_w1_mixer_pair.py
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

# the fresh z=200 band (the p29 W1 card-D strip, x-disjoint per family)
SITE_MK = gt6world.Site(392, 64, 200, dy=1, dz=1)  # kinetic mixer T1
SITE_EM1 = gt6world.Site(394, 64, 200, dy=1, dz=1) # electric mixer T1
SITE_EM2 = gt6world.Site(396, 64, 200, dy=1, dz=1) # electric mixer T2
MK, EM1, EM2 = F(SITE_MK), F(SITE_EM1), F(SITE_EM2)

# the C-Foam small-row item set (the :252-253 Stone/Sand/Clay combination, the
# p26_kitchen_pot chain's live-proven ids)
def cfoam_merge(pos):
    return {
        "1.20.1": ('data merge block ' + pos + ' {inventory:{Size:7,Items:['
                   '{Slot:0b,id:"gt6:dust_stone",Count:6b},'
                   '{Slot:1b,id:"gt6:dust_sand",Count:2b},'
                   '{Slot:2b,id:"gt6:dust_small_clay",Count:1b}]}}'),
        "1.21.1": ('data merge block ' + pos + ' {inventory:{Size:7,Items:['
                   '{Slot:0b,id:"gt6:dust_stone",count:6},'
                   '{Slot:1b,id:"gt6:dust_sand",count:2},'
                   '{Slot:2b,id:"gt6:dust_small_clay",count:1}]}}'),
    }

MK_MERGE = cfoam_merge(MK)
EM1_MERGE = cfoam_merge(EM1)

steps = [
    phase("A: the kinetic Mixer T1 — the C-Foam row at the 10000 identity, budget 2048"),
    Step(f"gt6machine mixer place {MK}", expect="GT6 mixer placed"),
    Step(MK_MERGE["1.20.1"], expect="Modified block data", node_cmds={"1.21.1": MK_MERGE["1.21.1"]}),
    Step(f"gt6machine mixer fluid fill up minecraft:water 1000 {MK}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine mixer inject 16 64 {MK}", expect="progress=1024/2048"),
    Step(f"gt6machine mixer check {MK}", expect="progress=1024/2048"),

    phase("B: the Electric Mixer T1 — the SAME row at efficiency 5000, budget 4096"),
    Step(f"gt6machine electricmixer place {EM1}", expect="GT6 electricmixer placed"),
    Step(EM1_MERGE["1.20.1"], expect="Modified block data", node_cmds={"1.21.1": EM1_MERGE["1.21.1"]}),
    Step(f"gt6machine electricmixer fluid fill up minecraft:water 1000 {EM1}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine electricmixer check {EM1}", expect="minIn=16 recIn=32 maxIn=64"),
    # the 8 EU wall: 40 packets of 8 EU never cross mInputMin 16 (the ULV regression face)
    Step(f"gt6machine electricmixer inject 40 8 {EM1}", expect="used=40 progress=0/0"),
    Step(f"gt6machine electricmixer check {EM1}", expect="progress=0/0"),

    phase("C: the 对拍 — 16 ticks each, verdicts IN-REPORT (the idle natural ticks "
          "CONSTANT_ENERGY-reset the ELECTRIC cross-command counter; the kinetic "
          "RETAINS, so its arm here is the 32nd tick = the completion tick)"),
    Step(f"gt6machine electricmixer inject 16 64 {EM1}", expect="progress=1024/4096"),
    Step(f"gt6machine mixer inject 16 64 {MK}",
         expect="used=16 progress=0/0"),  # 1024 held from phase A + 1024 = 2048/2048
                                          # → completes in-command (:539 parks 0/0);
                                          # the output lands in the tank (phase D stat)

    phase("D: the completion proof + the exactly-half park — the kinetic arm is now "
          "an inertness probe (inputs consumed, no recipe restart), the electric arm "
          "parks at exactly half (idle reset to 0, then 32 ticks = 2048/4096)"),
    Step(f"gt6machine mixer inject 32 64 {MK}", expect="progress=0/0"),
    Step(f"gt6machine mixer fluid stat {MK}", expect="out[0]=1000 L of gt6:cfoam"),
    Step(f"gt6machine electricmixer inject 32 64 {EM1}", expect="progress=2048/4096"),

    phase("E: the T2 window pin (the {64,128,256} ramp)"),
    Step(f"gt6machine electricmixer_t2 place {EM2}", expect="GT6 electricmixer_t2 placed"),
    Step(f"gt6machine electricmixer_t2 check {EM2}", expect="minIn=64 recIn=128 maxIn=256"),

    phase("F: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 391 62 199 397 67 201 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    # the name carries the band key: sweep --group p29_w1_eu_hu matches this member
    # substring and joins the whole cluster (the select_groups member-substring form)
    name="p29-w1-mixer-pair p29_w1_eu_hu",
    slug="p29w1mixerpair",
    sites=gt6world.declare_sites(SITE_MK, SITE_EM1, SITE_EM2),
    preferred_ports=(26220, 26230),      # the card-D pinned rcon/query pair (the fresh 2622x segment)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
