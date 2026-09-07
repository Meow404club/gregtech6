#!/usr/bin/env python3
"""p15-runtime-smoke — the ADR-P15-4 runtime smoke, node-agnostic by construction.

One chain that proves a freshly booted node is a LIVE GT6, run against BOTH nodes
of the stonecutter matrix (1.20.1-forge default; 1.21.1-neoforge via --node). It
is the 1.21.1 counterpart of the p14_loop_closure semantics, prefaced by the
registration face (task p15-rcon-dual-gate spec ③).

  R registration face: every gt6 command root parses in the real dispatcher —
    `help <root>` answers with usage lines "/<root> ..." for a registered root
    (HelpCommand.java:43 prints the parsed usage; an unregistered literal throws
    commands.help.failed instead); gt6gui (a bare literal) is probed by dispatch —
    the console run reaches its player-requirement rejection. 14 roots.

  M basic machine: the shredder arm (GTMachineCommand canon) behind the
    documented fakesource regime switch — place / input / run / check with the
    ContainerData three-state verdict (progress=true done=true idle=true) and
    dust_stone in the output; the flag is global, so it is turned OFF again
    before the D phase (the self-feed would eat the loop arm's pre-ignition
    running=false).

  B boiler: the p13 deterministic immune arm — 800 L distw + 64000 HU →
    efficiency 10000/10000 (PRISTINE, the :119 distilled short-circuit) and
    barometer 13 (800 × 160 steam on the 320000 gauge), plunge ×2 out.

  D distillation loop closure (the p14_loop_closure arm, verbatim semantics):
    burner → dryer over the top-face HU, 1000 L water in through the two rotated
    tank-in faces, 800 L distilled_water out (1000 × 8/10), top-face draw lifts
    the bank, flip down leaves running=false on live blocks (execute if block).

  K keepFilter round-trip (runtime evidence 4b, live face): the logistics tank
    drained to 0 L keeps its identity in the LIVE tank and in the SAVED NBT
    (writeToNBT 0-amount-with-identity); the metal drum control stays a true
    empty. The SAVE/LOAD (read-face codec) half of the evidence lives in
    p15_keepfilter_reboot_probe.py — this chain's faces are shared code, the
    known 21.1 codec delta only bites across a reboot.

Run:  python3 tools/rcon/chains/p15_runtime_smoke.py [--node 1.21.1-neoforge]
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

OVEN = gt6world.Site(60, 64, 60)
RIG = gt6world.Site(70, 64, 70, dx=3, dy=3, dz=3)   # the fire volume too
BOILER = gt6world.Site(80, 64, 80)
LOGI = gt6world.Site(110, 64, 110)
METAL = gt6world.Site(114, 64, 110)

OV = F(OVEN)
BURNER = F(RIG)          # 70 64 70
DRYER = "70 65 70"       # stacked one above the burner
BOIL = F(BOILER)
L = F(LOGI)
M = F(METAL)

# The registration face: all 14 command roots (grep census 'literal("gt6' —
# boiler/burner/chest/cover/energy/engine/gui/machine/multiblock/oven/pipe/
# tank/tool/wire). `help <root>` prints "/<root> ..." usage iff registered;
# gt6gui is a bare literal (no usage children) so it is probed by dispatch:
# the console run must reach the command's own player requirement.
COMMAND_PROBES = [(root, f"/{root}") for root in
                  ["gt6boiler", "gt6burner", "gt6chest", "gt6cover", "gt6energy",
                   "gt6engine", "gt6machine", "gt6multiblock", "gt6oven",
                   "gt6pipe", "gt6tank", "gt6tool", "gt6wire"]]
GT6GUI_DISPATCH = "gt6gui"
GT6GUI_EXPECT = "A player is required"

steps = []

# ------------------------------------------------- R: the registration face
steps += [
    phase("R: registration face — every gt6 command root parses in the dispatcher"),
]
steps += [Step(f"help {root}", expect=expect) for root, expect in COMMAND_PROBES]
steps.append(Step(GT6GUI_DISPATCH, expect=GT6GUI_EXPECT))

# ------------------------------------------------- M: the basic machine
steps += [
    phase("M: basic machine — the shredder arm (GTMachineCommand canon), fake source"),
    # the p8-d3/p11-rotor-source-flip era ships grid-fed by default; the acceptance
    # seam is the documented regime switch (the README §oven fake-power note is
    # pre-p8 legacy — the bare oven arm it describes fails on BOTH nodes today).
    Step("gt6machine fakesource on", expect="ENERGY_FAKE_SOURCE set true"),
    Step(f"gt6machine shredder place {OV}", expect="GT6 shredder placed"),
    Step(f"gt6machine shredder input 8 {OV}", expect="cobblestone into slot 0"),
    # one Step = one expect (framework semantics); a failed run emits the FAILED
    # literal, which the judge counts on its own — the ContainerData three-state
    # line plus output are the sufficient assertions here.
    Step(f"gt6machine shredder run 200 {OV}", expect="progress=true done=true idle=true"),
    # the out[0] column renders via ItemStack.toString: namespaced on 21.1
    # ("63x gt6:dust_stone"), plain on 1.20.1 ("63x dust_stone") — the bare class
    # name is the cross-version substring.
    Step(f"gt6machine shredder check {OV}", expect="dust_stone"),
    # the flag is GLOBAL: turn it off before the D phase — the loop arm's
    # pre-ignition running=false must not be eaten by the self-feed
    Step("gt6machine fakesource off", expect="ENERGY_FAKE_SOURCE set false"),
]

# ------------------------------------------------- B: the boiler arm
steps += [
    phase("B: boiler — distw + 64000 HU: PRISTINE efficiency, barometer 13"),
    Step(f"gt6boiler place {BOIL} steam_boiler_tank_lead", expect="GT6 boiler tank placed"),
    Step(f"gt6boiler fill {BOIL} 800 distw", expect="filled 800/800 L of gt6:distilled_water (ACCEPTED)"),
    Step(f"gt6boiler inject-hu {BOIL} 64000", expect="booked 64000/64000 HU (ACCEPTED)", sleep=3.0),
    Step(f"gt6boiler efficiency {BOIL}", expect="efficiency=10000/10000 (PRISTINE)"),
    Step(f"gt6boiler barometer {BOIL}", expect="barometer=13"),
    Step(f"gt6boiler plunge {BOIL}", expect="trashed "),
    Step(f"gt6boiler plunge {BOIL}"),
]

# ------------------------------------------------- D: the distw loop closure (p14 semantics)
steps += [
    phase("D: loop closure — burner → dryer, 1000 L water in, 800 L distw out"),
    Step(f"gt6machine dryer place {DRYER}", expect="GT6 dryer placed"),
    Step(f"gt6burner place {BURNER} brick_burning_box", expect="GT6 burning box placed"),
    Step(f"gt6machine dryer check {DRYER}", expect="running=false"),
    Step(f"gt6machine dryer fluid fill east minecraft:water 700 {DRYER}",
         expect="filled 700/700 L of minecraft:water (ACCEPTED)"),
    Step(f"gt6machine dryer fluid fill south minecraft:water 300 {DRYER}",
         expect="filled 300/300 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6burner fuel {BURNER} minecraft:coal 6", expect="minecraft:coal x6"),
    Step(f"gt6burner ignite {BURNER}", expect="burning=true"),
    # poll-to-expect (p23, the s35 deterministic red): the old trailing sleep=2.0 ran
    # AFTER the judge — the check's real budget was the previous command's quiet
    # window, and the P18 adaptive decay shrank it below the running-latch flip.
    Step(f"gt6machine dryer check {DRYER}", expect="running=true", poll=10.0),
    # 25600 HU / 16 per tick = 1600 ticks ≈ 84 s. poll-to-expect (the p14_loop_closure
    # conversion): resend the read-only stat until out[0]=800 lands; the verdict steps
    # below re-assert the exact same strings, verbatim
    Step(f"gt6machine dryer fluid stat {DRYER}",
         expect="out[0]=800 L of gt6:distilled_water", poll=150.0),
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="in[0]=0 L of nothing"),
    Step(f"gt6machine dryer fluid stat {DRYER}", expect="out[0]=800 L of gt6:distilled_water"),
    Step(f"gt6machine dryer fluid draw up 800 {DRYER}",
         expect="drawn 800/800 L of gt6:distilled_water (ACCEPTED), output tanks hold 0 L"),
    Step(f"gt6burner extinguish {BURNER}", expect="burning=false"),
    # poll-to-expect (p23, the s41 deterministic red): the drain takes a tick or two
    # past the extinguish, the single shot fired before the latch fell (running=true).
    # Terminal state; resend until it lands.
    Step(f"gt6machine dryer check {DRYER}", expect="running=false", poll=10.0),
    Step(f"execute if block {DRYER} gt6:dryer", expect="Test passed"),
    Step(f"execute if block {BURNER} gt6:brick_burning_box", expect="Test passed"),
    Step(f"execute if block {BOIL} gt6:steam_boiler_tank_lead", expect="Test passed"),
]

# ------------------------------------------------- K: keepFilter round-trip (live + saved faces)
steps += [
    phase("K: keepFilter — drain-to-0 keeps the identity (logistics vs metal control)"),
    Step(f"setblock {L} gt6:barrel_logistics", expect="Changed the block"),
    Step(f"gt6tank stat {L}", expect="melting point 100000 K"),
    Step(f"gt6tank fill {L} minecraft:water 5000", expect="filled 5000/5000"),
    Step(f"gt6tank draw {L} 5000", expect="drawn 5000/5000"),
    Step(f"gt6tank show {L}", expect="holds 0/1000000 L of minecraft:water"),
    Step(f"data get block {L} tank", expect='FluidName: "minecraft:water", Amount: 0'),
    Step(f"gt6tank fill {L} minecraft:water 5000", expect="filled 5000/5000"),
    Step(f"setblock {M} gt6:barrel_metal", expect="Changed the block"),
    Step(f"gt6tank fill {M} minecraft:water 5000", expect="filled 5000/5000"),
    Step(f"gt6tank draw {M} 5000", expect="drawn 5000/5000"),
    Step(f"gt6tank show {M}", expect="holds 0/64000 L of nothing"),
    Step(f"execute unless data block {M} tank", expect="Test passed"),
]

# ------------------------------------------------- teardown
steps += [
    phase("E: teardown — the explicit restore (the pass-open bbox is the backstop)"),
    Step("fill 58 61 58 116 69 116 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p15-runtime-smoke",
    slug="p15smoke",
    sites=gt6world.declare_sites(OVEN, RIG, BOILER, LOGI, METAL),
    preferred_ports=(25783, 25793),      # this card's pinned rcon/query pair; game = rcon-10
    # the boot-health smoke boots ALONE (framework.fresh_boot: the group boundary is
    # a fresh boot — decision 2026-09-04-rcon-gate-split ③), and it flips the GLOBAL
    # fakesource regime switch (on in M, off before D — self-cleaning, but declared
    # so the session mutates-conflict detection sees it)
    fresh_boot=True,
    mutates=("fakesource",),
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
