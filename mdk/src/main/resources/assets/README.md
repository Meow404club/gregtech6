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

Task p9-render-d-formed-look (2026-09-01) — coke oven controller texture
census, NEGATIVE result, declared deviation: the plan was to borrow upstream
`textures/blocks/machines/multiblockmains/cokeoven/` colored+overlay groups
(the controller look of `TileEntityBase10MultiBlockBase.getTexture2`,
:192-194 — front/side groups picked by `aSide == mFacing` over a colored+
overlay two-layer stack; the icon paths themselves are built at :66-81 from
NBT_TEXTURE "cokeoven", Loader_MultiTileEntities.java:1193). Census found NO
such PNG group upstream at all — neither `colored/` nor `overlay/` nor the
`colored_front/`/`overlay_front/` variants ship in the snapshot (the
`multiblockmains/` directory contains only bedrockdrill, crucible, gasturbine,
largeboiler, largedynamo, largeheatexchanger, largeturbine, lightningrod,
logisticscore, tankmetal, tankwood, vondagraagg), so the upstream controller
itself registers missing-resource icons. Nothing to borrow, and per the
borrow-or-declare rule nothing was redrawn: the script-generated placeholder
PNGs (`multiblock_coke_oven*.png`) remain. The only upstream `cokeoven`
texture family that DOES exist is `basicmachines/cokeoven/` — a
basicmachine-layout group (back/bottom/front/left/right/top naming, machine
state overlays) belonging to a different machine family; it is NOT the
multiblock controller look and was not borrowed, out of this card's scope.
The addMultiBlocks javadoc in `GT6BlockStates.java` carries the accompanying
getTexture2 erratum (front by facing, never by formed; the FORMED dual model
is a declared port enhancement beyond upstream).

Oven state-overlay textures, task p9-render-c-oven-overlay: the 12 cutout
overlay PNGs under `gt6/textures/block/oven_overlay_active_*.png` and
`gt6/textures/block/oven_overlay_running_*.png` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/oven/overlay_active/<face>.png`
and `.../overlay_running/<face>.png` (face ∈ bottom/top/left/front/right/back,
the MultiTileEntityBasicMachine.java:174-203 NBT_TEXTURE name form for
`NBT_TEXTURE, "oven"`, Loader_MultiTileEntities.java:1288-1291), byte-identical
to upstream, sha256 verified per file:

- `oven_overlay_active_back.png`     `c39ed9a7e4479b8abcc8cbbd38b10061fb7263b22fc06dc722f7f381a70e194c`
- `oven_overlay_active_bottom.png`   `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`
- `oven_overlay_active_front.png`    `fb7896fcaf05092c8ce62895b940d779af1d73b5bf9d6d912f0173e9c8b40e36`
- `oven_overlay_active_left.png`     `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `oven_overlay_active_right.png`    `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `oven_overlay_active_top.png`      `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`
- `oven_overlay_running_back.png`    `cd5857690ac01a9fc10e3aadd4fe3afe3c2372d685ef0bcf14b680756b92b85e`
- `oven_overlay_running_bottom.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`
- `oven_overlay_running_front.png`   `cd5857690ac01a9fc10e3aadd4fe3afe3c2372d685ef0bcf14b680756b92b85e`
- `oven_overlay_running_left.png`    `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `oven_overlay_running_right.png`   `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `oven_overlay_running_top.png`     `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`

Filenames were lowercased/flat-mapped onto the `textures/block/` convention on
borrow (1.20.1 `ResourceLocation` charset; the sprite ids are
`gt6:block/oven_overlay_<group>_<face>`); the PNG contents are byte-identical
to upstream (sha256 above, no rescaling or redrawing).

Scope trim (declared, census-backed): the task card's "colored6+overlay12"
enumeration is trimmed to the 12 overlay PNGs above. The `colored/<face>`
grayscale material group exists upstream but is unused by the landed design —
ADR ② pins the material quads to the A-tier fallback model ("材质 quad 沿
fallback"), whose baked textures carry no tintindex, so there is no mRGBa tint
carrier in this card; the upstream inactive `overlay/<face>` group is likewise
not borrowed because the inactive state plans no overlay quad (the A-tier
front texture already carries the inactive face). All 24 upstream oven PNGs
were census-verified to exist; only these 12 are consumed.

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

