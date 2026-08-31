# assets — borrowed textures attribution

Machine GUI backgrounds borrowed from **GregTech 6**
(https://github.com/GregTech6/gregtech6), snapshot
`v6.17.06-22-g3703e4030`, files
`src/main/resources/assets/gregtech/textures/gui/machines/`:

- `gt6/textures/gui/machines/shredder.png` — Shredder GUI background
  (upstream `Shredder.png`, task p7-gui-family)
- `gt6/textures/gui/machines/crusher.png` — Crusher GUI background
  (upstream `Crusher.png`, task p7-gui-family)
- `gt6/textures/gui/machines/lathe.png` — Lathe GUI background
  (upstream `Lathe.png`, task p7-gui-family)
- `gt6/textures/block/energy_source.png` — test energy source block texture
  (upstream `textures/blocks/machines/solarpanels/solarpanel_electric_8eu/colored/side.png`,
  task p8-d4-energy-source — the SolarPanelElectric family the rig ports)
- `gt6/textures/gui/machines/cokeoven.png` — Coke Oven GUI background
  (upstream `CokeOven.png`, task p8-cokeoven-gui-menu; sha256
  `a582c690865eb98115a639432731999d5cf13d2163ae8f532b2de10789f77572`)

Material prefix BLOCK textures, task p8-prefixblock-render: the 175 grayscale
base icons under `gt6/textures/block/materialicons/<set>/<prefix>.png` come from
upstream `src/main/resources/assets/gregtech/textures/blocks/materialicons/<SET>/<Prefix>.png`
(exactly the prefix x live-texture-set intersection the datagen models reference,
sha256 verified byte-identical; the `_OVERLAY` pass textures are NOT borrowed —
the port renders single-pass grayscale + tint). The material colour comes from
the runtime `RegisterColorHandlersEvent` tint (`fRGBa[prefix.mState]`), mirroring
upstream `PrefixBlock.getRenderColor` (PrefixBlock.java:279-282).

Filenames were lowercased on borrow: 1.20.1 `ResourceLocation` paths only
accept `[a-z0-9_.-/]`. The PNG contents are byte-identical to upstream
(sha256 verified, no rescaling or redrawing).

Copied on 2026-08-31. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (upstream `README.md`: "All assets, unless otherwise stated, are
dedicated to the public domain according to the CC0 1.0 Universal Public
Domain Dedication"). The upstream logo exception (CC BY-NC) covers the
GregTech logo and its derivatives only; machine GUI backgrounds are not
logos, so CC0 applies.

Wrench-grid icons in `gt6/textures/gui/overlay/` come from GregTech CEu
Modern under LGPL-3.0-or-later — see `gt6/textures/gui/overlay/README.md`
for that attribution.
