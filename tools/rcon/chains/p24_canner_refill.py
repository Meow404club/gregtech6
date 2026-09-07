#!/usr/bin/env python3
"""p24-canner-machine — the Canning Machine refill e2e + the dynamic arms (declarative
framework, the p16_side_io shape).

Chain semantics (task p24-canner-machine ACCEPTANCE 3 — "place canner→注染液→input
空罐→tick→断言 spray_paint_<color> 出槽（16 色抽 2 色）+除漆行+动态灌装臂各一断言"):

  A the refill unit (T1, dye RED): place → input 8x spray_can_empty → fluid fill up
    gt6:dye_chemical_red 2304 (the row leg, MultiItemRandomTools.java:246 FL.mul(., 16)
    = 16 x 144 mB, ruling R4) → inject 40 x 16 EU (the grid rig; EUt 16, duration 256,
    parallel 1, no :773 overclock on T1 — the process finishes inside the tick budget)
    → the verdict pins the FULL CAN in the output slot (1x spray_paint_red, ruling R5:
    the item identity carries the colour, ZERO NBT) and the input tank drained to 0.

  B the refill unit (T2, dye WHITE): the second pinned colour — the :773 loop overclocks
    EUt 16 to mInputMin 64 (mMaxProgress 512), 16 EU/tick injected → 32 process ticks,
    inject 48 covers it → 1x spray_paint_white.

  C the remover row (T3): chlorine 2304 (MultiItemRandomTools.java:272
    MT.Cl.fluid(16*U)) → the :773 loop reaches mInputMin 256 / mMaxProgress 1024 →
    64 process ticks, inject 80 → 1x spray_paint_remover — the R3 standalone chlorine
    row's live consumer.

  D the DYNAMIC arms (T4 x2, the R1 semantics over the live IFluidHandlerItem seam):
    D1 the FILL arm — an EMPTY gt6:barrel_wood (the live FLUID_HANDLER_ITEM carrier,
    16000 L) merged into slot 0, the machine tank filled with 1000 L of water → inject →
    the output slot carries the FILLED can (1x barrel_wood) and the machine tank drains
    to 0 (the tank fluid was the consumed input leg).
    D2 the EMPTY arm — a FILLED gt6:barrel_wood (tank NBT = water 1000) merged into
    slot 0 → inject → the output slot carries the DRAINED can and the output tank holds
    out[0]=1000 L of minecraft:water (the content rode the fluid-output leg).

  E teardown: the explicit fill-air over the band (the pass-open bbox cleanup is the
  backstop). No global state is touched (the grid inject rig needs no fakesource).

The inventory merges ride the loader-versioned key shapes (the p19_nbt_rebind ruling):
1.20.1 item tag `tag:{tank:{FluidName,Amount}}` vs 1.21.1 the gt6:barrel_content
component payload. The judged expects stay byte-identical. NOTE the port Item.toString
renders the registry PATH only (no gt6: prefix) — the outputs=[1x spray_paint_red; ]
marker shape (the D2 report is the ground truth).

passes=2 is the idempotency proof (the [0,0] of this chain).

Run:  GT6_SESSION=off python3 tools/rcon/chains/p24_canner_refill.py
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

# the four canner sites on the 400 band (clear of p24dyechemical's 386 band)
SITE_A = gt6world.Site(400, 64, 20, dx=0, dy=1, dz=1)  # canner     T1 — red refill
SITE_B = gt6world.Site(402, 64, 20, dx=0, dy=1, dz=1)  # canner_t2  T2 — white refill
SITE_C = gt6world.Site(404, 64, 20, dx=0, dy=1, dz=1)  # canner_t3  T3 — chlorine remover
SITE_D1 = gt6world.Site(406, 64, 20, dx=0, dy=1, dz=1) # canner_t4  T4 — the FILL arm
SITE_D2 = gt6world.Site(408, 64, 20, dx=0, dy=1, dz=1) # canner_t4  T4 — the EMPTY arm
A = F(SITE_A)
B = F(SITE_B)
C = F(SITE_C)
D1 = F(SITE_D1)
D2 = F(SITE_D2)

# the barrel-item payload per node (the p19_nbt_rebind key-shape ruling): an EMPTY
# barrel vs a barrel holding 1000 L of water. 1.20.1 carries the tank compound in the
# item tag; 1.21.1 carries it in the gt6:barrel_content CustomData component (the
# GT6DataComponents registration), the FluidStack keys themselves shift FluidName/Amount
# → id/amount (the p12fic ruling).
EMPTY_CAN_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:barrel_wood",Count:1b}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:barrel_wood",count:1}}]}}}}',
}
FILLED_CAN_MERGE = {
    "1.20.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:barrel_wood",Count:1b,tag:{{tank:{{FluidName:"minecraft:water",Amount:1000}}}}}}]}}}}',
    "1.21.1": 'data merge block {p} {{inventory:{{Size:4,Items:[{{Slot:0b,id:"gt6:barrel_wood",count:1,components:{{"gt6:barrel_content":{{tank:{{id:"minecraft:water",amount:1000}}}}}}}}]}}}}',
}

steps = []

# ------------------------------------------------- A: the red refill unit (T1)
steps += [
    phase("A: the R4 refill unit (T1) — red 2304 mB, the full can lands with ZERO NBT"),
    Step(f"gt6machine canner place {A}", expect="GT6 canner placed"),
    Step(f"gt6machine canner input 8 {A}",
         expect="GT6 canner input: 8x spray_can_empty into slot 0"),
    Step(f"gt6machine canner fluid fill up gt6:dye_chemical_red 2304 {A}",
         expect="filled 2304/2304 L of gt6:dye_chemical_red (ACCEPTED), input tanks hold 2304 L"),
    Step(f"gt6machine canner inject 70 64 {A}",
         expect="outputs=[1x spray_paint_red; ]"),
    Step(f"gt6machine canner fluid stat {A}", expect="in[0]=0 L of nothing"),
]

# ------------------------------------------------- B: the white refill unit (T2)
steps += [
    phase("B: the second pinned colour (T2) — white 2304 mB → spray_paint_white"),
    Step(f"gt6machine canner_t2 place {B}", expect="GT6 canner_t2 placed"),
    Step(f"gt6machine canner_t2 input 8 {B}",
         expect="GT6 canner input: 8x spray_can_empty into slot 0"),
    Step(f"gt6machine canner_t2 fluid fill up gt6:dye_chemical_white 2304 {B}",
         expect="filled 2304/2304 L of gt6:dye_chemical_white (ACCEPTED), input tanks hold 2304 L"),
    Step(f"gt6machine canner_t2 inject 70 256 {B}",
         expect="outputs=[1x spray_paint_white; ]"),
]

# ------------------------------------------------- C: the chlorine remover row (T3)
steps += [
    phase("C: the remover row (T3) — chlorine 2304 mB → spray_paint_remover (the R3 consumer)"),
    Step(f"gt6machine canner_t3 place {C}", expect="GT6 canner_t3 placed"),
    Step(f"gt6machine canner_t3 input 8 {C}",
         expect="GT6 canner input: 8x spray_can_empty into slot 0"),
    Step(f"gt6machine canner_t3 fluid fill up gt6:chlorine 2304 {C}",
         expect="filled 2304/2304 L of gt6:chlorine (ACCEPTED), input tanks hold 2304 L"),
    Step(f"gt6machine canner_t3 inject 70 1024 {C}",
         expect="outputs=[1x spray_paint_remover; ]"),
]

# ------------------------------------------------- D: the dynamic arms (T4 x2)
steps += [
    phase("D1: the FILL arm (T4) — machine water 1000 L fills an EMPTY barrel item"),
    Step(f"gt6machine canner_t4 place {D1}", expect="GT6 canner_t4 placed"),
    Step(EMPTY_CAN_MERGE["1.20.1"].format(p=D1), expect="Modified block data",
         node_cmds={"1.21.1": EMPTY_CAN_MERGE["1.21.1"].format(p=D1)}),
    Step(f"gt6machine canner_t4 fluid fill up minecraft:water 1000 {D1}",
         expect="filled 1000/1000 L of minecraft:water (ACCEPTED), input tanks hold 1000 L"),
    Step(f"gt6machine canner_t4 inject 8 4096 {D1}",
         expect="outputs=[1x barrel_wood; ]"),
    Step(f"gt6machine canner_t4 fluid stat {D1}", expect="in[0]=0 L of nothing"),

    phase("D2: the EMPTY arm (T4) — a FILLED barrel item drains onto the output tank"),
    Step(f"gt6machine canner_t4 place {D2}", expect="GT6 canner_t4 placed"),
    Step(FILLED_CAN_MERGE["1.20.1"].format(p=D2), expect="Modified block data",
         node_cmds={"1.21.1": FILLED_CAN_MERGE["1.21.1"].format(p=D2)}),
    Step(f"gt6machine canner_t4 inject 8 4096 {D2}",
         expect="outputs=[1x barrel_wood; ]"),
    Step(f"gt6machine canner_t4 fluid stat {D2}", expect="out[0]=1000 L of minecraft:water"),
]

# ------------------------------------------------- E: teardown
steps += [
    phase("E: teardown — the explicit band restore (no global state was touched)"),
    Step("fill 399 62 19 409 67 21 air", expect="filled"),
    Step("time query daytime", expect="The time is"),
]

CHAIN = Chain(
    name="p24-canner-refill",
    slug="p24cannerrefill",
    sites=gt6world.declare_sites(SITE_A, SITE_B, SITE_C, SITE_D1, SITE_D2),
    preferred_ports=(26108, 26118),      # this card's pinned rcon/query pair (the 2610x segment, after p24dyechemical 26106/26116)
    steps=steps,
)


if __name__ == "__main__":
    main(CHAIN)
