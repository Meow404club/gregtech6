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
- `gt6/textures/block/crank.png` — Hand Crank block texture (upstream
  `textures/blocks/machines/tools/crank/colored/front.png`, task p12-engine-crank;
  sha256 `a29789c46513a00f1d866a8351c777c8c2b7a365e5a387145f6d6b6dd2abf07a`).
  Upstream tints this grayscale "colored" icon with the material colour
  (ANY.Iron mRGBa via the MTE mRGBa pass) and layers the spin overlay
  (`crank/colored/frontspin.png`) while cranking; the port shows the grayscale
  icon un-tinted with no active visual — declared deviation, the tint and the
  ACTIVE-state model swap ride the render pool card.
- `gt6/textures/gui/machines/cokeoven.png` — Coke Oven GUI background
  (upstream `CokeOven.png`, task p8-cokeoven-gui-menu; sha256
  `a582c690865eb98115a639432731999d5cf13d2163ae8f532b2de10789f77572`)
- `gt6/textures/item/crowbar.png` — crowbar item texture (upstream
  `textures/items/iconsets/CROWBAR.png`, task p9-tool-crowbar; sha256
  `4190549e7662b5fe4657face4097818eb1a4745f16b1740de11e619f8f046894`).
  Upstream tints this grayscale icon with the tool material colour
  (GT_Tool_Crowbar.getRGBa → MT.Steel.mRGBaSolid); the port renders it
  un-tinted at the single steel tier — declared deviation, a runtime tint
  rides the tool-family pool card.
- `gt6/textures/item/cutter.png` — wire cutter item texture (upstream
  `textures/items/iconsets/WIRE_CUTTER.png`, task p10-tool-cutter; sha256
  `3ccb7a752f1b51f6ed7940462098523826491ea595f27cedae8882efa66167c0`).
  Same single-steel-tier story as the crowbar: upstream tints the tool head
  (GT_Tool_WireCutter.getRGBa :95-97, the material pool) and renders only the
  WIRE_CUTTER head layer (the :91 handle layer is VOID); the port shows the
  grayscale head un-tinted — declared deviation, same runtime-tint pool.

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


Redstone Emitter cover textures, task p9-redstone-cover-emitter: the 17 PNGs
under `gt6/textures/block/redstone_emitter/` (`0.png`..`15.png` + `underlay.png`)
derive from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/redstoneemitter/`
(the `CoverRedstoneEmitter.java:116-135` texture table `sTextures[0..15]` +
`sTexturesBase` underlay — the tier digit drawn over the key-pad panel).
UPSTREAM SOURCES sha256 (provenance):

- `0.png`          `da225602274cd7769c7ce7c74e23d29f84cd6e08d77f3a6cedf5cc8f7efa1cc7`
- `1.png`          `a407c167ef8365400016a797e34577a8d9c9b6da6e7049d0b7a280307b7c0731`
- `2.png`          `622d6f2a90b342366a98180f0a4b8a257b4c718908abd2b96deae4f6a9f1a0f1`
- `3.png`          `d0d30c15da3f7bcddadbd347a99756e3c59c17870ba00238dc9e771290ee2b3f`
- `4.png`          `ffb243256575a2e80a6a18cf1aa057241959b745ee02ce50b420a03933d94a6a`
- `5.png`          `751b8ced7db40f0a5b284d1763f4b1d03fb4a559f73277da8c389ca7502bb8bf`
- `6.png`          `bd47d5cd7bb3ed1fc17d30c4c405716c46246f359a276c56be8d5f0282ff33f5`
- `7.png`          `945780fdf0e036b853ac844fdd9e2fc17bb5740098a6b48786c6f1389391b9a2`
- `8.png`          `50577244ea9ba4406d6541eda6e042ec2f284bb1fce673293ae726faf0ab45cc`
- `9.png`          `e09830ea78842c0b11d1e9f0e9be0fb697859177be7e6ab684363c0fc7ab2b20`
- `10.png`         `b4025444ec680bc3cbc8f22bd66fb8c4c64294fbbea8ed932c83cbc917bbf23a`
- `11.png`         `cab01fef8674e5611ec99e5a87b0595ec1e4df8379a10b6c9f4806c30550a592`
- `12.png`         `1eba237ba18bd29f2800e326118adba65a39dbf0dca9fb037d3fd6bbc96aea85`
- `13.png`         `4d02bba819e7368ae9b8ef5441fca9d586ddc37f4a9b3b553af2802fa2e923c3`
- `14.png`         `3ac3ffc4f17a13b6ffdb9ed2c18f845100315854467acdec0c45b7ca772969d0`
- `15.png`         `797d94f589520541dfb74922a86e1f40b48454a3083880e23a78e620504caa48`
- `underlay.png`   `5951db3704bfc3346837196c66207e99e58a26e9079abfaa2376fb3e87962c37`

Composition (declared, the p9-render-d-formed-look option-(b) precedent): the
upstream tier PNGs are 6-8%-opaque digit OVERLAYS — upstream stacks them
`BlockTextureMulti(sTexturesBase, sTextures[tier])` (:111). The 1.20.1 plate
renderer stitches ONE sprite per face (`CoverPlateModel.planQuads` —
`GTCoverRenderSnapshot.sprite(tFace)`), so the 16 shipped tier PNGs are the
offline source-over composition `underlay + digit` (alpha flattened, RGBA,
16x16, no rescaling or redrawing of either layer); `underlay.png` itself is
byte-identical to upstream (sha256 above, re-verified post-borrow). The
upstream `BACKGROUND_COVER` third layer (covers/base.png) is NOT composed — it
belongs to the attachment/holder faces only (:112-113), and per the
CoverTextureSimple fold note the plate background layer stays out of the
single-sprite plate.

- `0.png`          `da225602274cd7769c7ce7c74e23d29f84cd6e08d77f3a6cedf5cc8f7efa1cc7`
- `1.png`          `a407c167ef8365400016a797e34577a8d9c9b6da6e7049d0b7a280307b7c0731`
- `2.png`          `622d6f2a90b342366a98180f0a4b8a257b4c718908abd2b96deae4f6a9f1a0f1`
- `3.png`          `d0d30c15da3f7bcddadbd347a99756e3c59c17870ba00238dc9e771290ee2b3f`
- `4.png`          `ffb243256575a2e80a6a18cf1aa057241959b745ee02ce50b420a03933d94a6a`
- `5.png`          `751b8ced7db40f0a5b284d1763f4b1d03fb4a559f73277da8c389ca7502bb8bf`
- `6.png`          `bd47d5cd7bb3ed1fc17d30c4c405716c46246f359a276c56be8d5f0282ff33f5`
- `7.png`          `945780fdf0e036b853ac844fdd9e2fc17bb5740098a6b48786c6f1389391b9a2`
- `8.png`          `50577244ea9ba4406d6541eda6e042ec2f284bb1fce673293ae726faf0ab45cc`
- `9.png`          `e09830ea78842c0b11d1e9f0e9be0fb697859177be7e6ab684363c0fc7ab2b20`
- `10.png`         `b4025444ec680bc3cbc8f22bd66fb8c4c64294fbbea8ed932c83cbc917bbf23a`
- `11.png`         `cab01fef8674e5611ec99e5a87b0595ec1e4df8379a10b6c9f4806c30550a592`
- `12.png`         `1eba237ba18bd29f2800e326118adba65a39dbf0dca9fb037d3fd6bbc96aea85`
- `13.png`         `4d02bba819e7368ae9b8ef5441fca9d586ddc37f4a9b3b553af2802fa2e923c3`
- `14.png`         `3ac3ffc4f17a13b6ffdb9ed2c18f845100315854467acdec0c45b7ca772969d0`
- `15.png`         `797d94f589520541dfb74922a86e1f40b48454a3083880e23a78e620504caa48`
- `underlay.png`   `5951db3704bfc3346837196c66207e99e58a26e9079abfaa2376fb3e87962c37`

Path mapping (declared): upstream `machines/covers/redstoneemitter/<n>` is
lowercased/underscored to `redstone_emitter/<n>` (1.20.1 `ResourceLocation`
charset), and the group lands under `textures/block/` — the vanilla block atlas
ships a cross-namespace `directory("block")` source (vanilla
`assets/minecraft/atlases/blocks.json`), so the sprite ids
`gt6:block/redstone_emitter/<tier|underlay>` are auto-stitched with zero extra
atlas wiring (the same convention the pump-cover sprites use). The card's
`textures/cover/` sketch would NOT be scanned by any atlas source and
`GT6Atlases.java` is outside this card's FILES_SCOPE, hence the micro-deviation.

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Electric wire family textures, task p9-wire-family-w2: the 7 grayscale wire
icons under `gt6/textures/block/materialicons/<set>/wire.png` (sets
`copper, shiny, metallic, dull, quartz, rad, none` — the exact iconset census of
the 30 wire rows; `none` = upstream `NONE/wire.png`, landing the setless
Superconductor row) come from upstream
`src/main/resources/assets/gregtech/textures/blocks/materialicons/<SET>/wire.png`,
byte-identical (upstream ships the SAME wire.png bytes in every set, sha256
`d9343ea989b6585f8fea8ed6aba7db86f477bc7f63553db1f8d4d06794077bad`; census
2026-09-01 found wire.png present in ALL upstream iconsets — zero coverage
gaps, no placeholder fallback needed). The 6 insulation jacket masks under
`gt6/textures/block/iconsets/insulation_{tiny,small,medium,large,huge,full}.png`
come from upstream `textures/blocks/iconsets/INSULATION_<TIER>.png`
(byte-identical, sha256 below), rendered as the gray-64 tinted overlay layers
(MultiTileEntityWireElectric.java:237-238). The material colour is the runtime
`RegisterColorHandlersEvent` tint on index 0 (`fRGBaSolid`), the jacket rides
tint index 1.

- `materialicons/copper/wire.png` … `materialicons/none/wire.png` (7 copies,
  byte-identical) `d9343ea989b6585f8fea8ed6aba7db86f477bc7f63553db1f8d4d06794077bad`
- `iconsets/insulation_tiny.png`   `7835cf39b86904ee279b7656c1ed080ed736f47efe2e164cb908726c93fd6e42`
- `iconsets/insulation_small.png`  `5332c96d9c4af24985a10a954547d3608fe33bdd803b16ee42cef9bd38a9b207`
- `iconsets/insulation_medium.png` `fb3c6765612def8793fad6fe4c203b38b87226f1044c0ecdb27c2a87cf0aeb97`
- `iconsets/insulation_large.png`  `4154396a428bfd2a4c020b784a877268cb1ac72ddb9c5e7b5a7f6c32f0d53434`
- `iconsets/insulation_huge.png`   `081ae186c680cccc6cd602558cfb4baef9f5a150ba9583d27ad72f2f410e74ce`
- `iconsets/insulation_full.png`   `54a4eb56bf6ce30a93a283e9a14c97dd97d0657bce01da8769d3429b0f631dd0`

Filenames lowercased on borrow (1.20.1 ResourceLocation charset), contents
byte-identical. Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal
Public Domain Dedication** (same upstream `README.md` block as above).
Declared deviation: the painted-foam colour variant (`isPainted() ? mRGBa`)
is not ported — foam is not in this card's scope, the unpainted gray-64 jacket
is rendered.

Redstone Conductor cover textures, task p10-cover-conductor-redstone: the 2
PNGs under `gt6/textures/block/redstone_conductor/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/redstoneconductor/`
(the `CoverRedstoneConductorIN.java:35` / `CoverRedstoneConductorOUT.java:63`
`sTexture` single sprites), byte-identical to upstream, sha256 verified:

- `in.png`           `161950d47ab5c59cecab653dff1b39dba7bf762bdeba34bccd39bf8530a47435`
- `out.png`          `ba5343909a38a13761d5942b1fef5983cdb6be22627c5519803b4114ec9e7ca6`

Path mapping (declared, the redstone-emitter precedent): upstream
`machines/covers/redstoneconductor/<in|out>` is lowercased/underscored to
`redstone_conductor/<in|out>` (1.20.1 `ResourceLocation` charset), landing
under `textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite ids `gt6:block/redstone_conductor/<in|out>` with zero
extra atlas wiring. The upstream `BACKGROUND_COVER` layer
(covers/base.png, the attachment/holder faces) is NOT borrowed — it folds into
the single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults,
the p9 emitter note).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Redstone Machine Switch cover texture, task p10-cover-controller-redstone: the
1 PNG under `gt6/textures/block/redstone_switch/` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/redstoneswitch/`
(the `CoverControllerRedstone.java:65` `sTextureForeground` single sprite,
`machines/covers/redstoneswitch/circuit`), byte-identical to upstream, sha256
verified:

- `circuit.png`       `08926925e9654c93de87fab5dbeeeb1f456ade5cfd414a10c3dfd34ada46d1a6`

Path mapping (declared, the redstone-conductor precedent): upstream
`machines/covers/redstoneswitch/circuit` is lowercased/underscored to
`redstone_switch/circuit` (1.20.1 `ResourceLocation` charset), landing
under `textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite id `gt6:block/redstone_switch/circuit` with zero
extra atlas wiring. The upstream `BACKGROUND_COVER` layer (the attachment/holder
faces, `CoverControllerRedstone.java:62-63`) is NOT borrowed — it folds into
the single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Shutter + Item Filter cover textures, task p11-cover-shutter-filter: the 4 PNGs
under `gt6/textures/block/shutter/` and `gt6/textures/block/filteritem/` come
from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/shutter/`
and `.../covers/filteritem/` (the `CoverShutter.java:93-95` and
`CoverFilterItem.java:145-147` normal/inverted sprite pairs), byte-identical
to upstream, sha256 verified:

- `shutter/normal.png`     `ba4629fca0823fc13e82ae33ecf3fded2fa23ec27f6b9ff8b47fecd8df9caadd`
- `shutter/inverted.png`   `f028857975fd9b231b9d4a79b5c2c18d6d95d41be6bbbecec85fa35ccb9a5db6`
- `filteritem/normal.png`  `142c9ef5b8327f195273557ede3bb2e77b83a1a8881e1b81bd76e27f9c0c7d4c`
- `filteritem/inverted.png` `a61b9b8e49fbd86c10abe840264398de8c71f0f02f1a8ecb092d9231fe00bd39`

Path mapping (declared, the redstone-conductor precedent): upstream
`machines/covers/shutter/<normal|inverted>` and
`machines/covers/filteritem/<normal|inverted>` lowercased/underscored to
`shutter/<...>` and `filteritem/<...>` (1.20.1 `ResourceLocation` charset),
landing under `textures/block/` — the vanilla block atlas `directory("block")`
source auto-stitches the sprite ids `gt6:block/shutter/<normal|inverted>` and
`gt6:block/filteritem/<normal|inverted>` with zero extra atlas wiring. The
upstream `BACKGROUND_COVER` layer (covers/base.png, the attachment/holder
faces) is NOT borrowed — it folds into the single-sprite plate like every
cover (AbstractCoverDefault :71-72 defaults).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Auto Redstone Machine Switch cover texture, task p11-cover-controllers: the
1 PNG under `gt6/textures/block/auto_redstone_switch/` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/autoredstoneswitch/`
(the `CoverControllerAutoRedstone.java:67` `sTextureForeground` single sprite,
`machines/covers/autoredstoneswitch/circuit`), byte-identical to upstream,
sha256 verified:

- `circuit.png`       `637b71f4547bf42f2778c08e1072158d4dfa7c10b68d133fe6a39ea262a40fb0`

Path mapping (declared, the P10 redstone-switch precedent): upstream
`machines/covers/autoredstoneswitch/circuit` is lowercased/underscored to
`auto_redstone_switch/circuit` (1.20.1 `ResourceLocation` charset), landing
under `textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite id `gt6:block/auto_redstone_switch/circuit` with zero
extra atlas wiring. The upstream `BACKGROUND_COVER` layer (the attachment/holder
faces, `CoverControllerAutoRedstone.java:64-65`) is NOT borrowed — it folds into
the single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Cover Controller cover texture, task p11-cover-controllers: the 1 PNG under
`gt6/textures/block/cover_switch/` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/coverswitch/`
(the `CoverControllerCovers.java:104` `sTextureForeground` single sprite,
`machines/covers/coverswitch/circuit`), byte-identical to upstream, sha256
verified:

- `circuit.png`       `c7a5e3ba9a69ee6c43146d098dec8d582be88aff2bc9d6222775bfc050e46bf6`

Path mapping (declared, the P10 redstone-switch precedent): upstream
`machines/covers/coverswitch/circuit` is lowercased/underscored to
`cover_switch/circuit` (1.20.1 `ResourceLocation` charset), landing under
`textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite id `gt6:block/cover_switch/circuit` with zero extra
atlas wiring. The upstream `coverswitch/base` background (`:104`
`sTextureBackground`, the attachment/holder faces, `:101-102`) is NOT borrowed —
it folds into the single-sprite plate like every cover (AbstractCoverDefault
:71-72 defaults, the BACKGROUND_COVER layer note).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Laser Fiber Wire textures, task p11-wire-fiber-texture: the 2 PNGs under
`gt6/textures/block/iconsets/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/iconsets/`
(the `MultiTileEntityWireLaser.java:121-122` fixed texture pair — both
`getTextureSide` and `getTextureConnected` return
`BlockTextureMulti(BlockTextureDefault(FIBER_WIRE, mRGBa),
BlockTextureDefault(FIBER_WIRE_OVERLAY))`, no glow layer), byte-identical
to upstream (16x16 RGBA, `cmp` + sha256 verified per file):

- `fiber_wire.png`          `1b383e640e9882b2cf927dde24cc0a1563c23e025224bb2846946f6a958e731e`
  (upstream `FIBER_WIRE.png`; the sprite id is `gt6:block/iconsets/fiber_wire`)
- `fiber_wire_overlay.png`  `1aa8d9d32a7626d16eec2f54cad4056772a8c6c2abc738c0649994a8ea989add`
  (upstream `FIBER_WIRE_OVERLAY.png`; the sprite id is
  `gt6:block/iconsets/fiber_wire_overlay`)

Path mapping (declared, the p9-wire-family-w2 insulation-mask precedent):
upstream `iconsets/<UPPER_SNAKE>.png` is lowercased to
`iconsets/<lower_snake>.png` (1.20.1 `ResourceLocation` charset), landing under
`textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches both sprite ids with zero extra atlas wiring (the same
convention the insulation masks use). The FIBER_WIRE base layer carries the
upstream `mRGBa` dye through the runtime tint index 0
(`GTWireTint.tintARGB`, the row material is `MT.NULL` per Loader:1815); the
FIBER_WIRE_OVERLAY layer renders un-tinted (upstream `BlockTextureDefault`
without a colour argument). Filenames lowercased on borrow, contents
byte-identical, no rescaling or redrawing.

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Conveyor + Robot Arm cover textures, task p11-cover-conveyor-robotarm: the 4
PNGs + 4 animation mcmeta files under `gt6/textures/block/conveyor/` and
`gt6/textures/block/robotarm/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/conveyor/`
and `.../covers/robotarm/` (the `CoverConveyor.java:92-94` / `CoverRobotArm.java:118-120`
`sTextureIn`/`sTextureOut` pair — `machines/covers/conveyor/<in|out>` and
`machines/covers/robotarm/<in|out>`), byte-identical to upstream, sha256
verified:

- `conveyor/in.png`            `1a98c8952bc685f3a7c4b93bb3b26ad8440b340063d77cd658c072dfe3dfdc8a`
- `conveyor/out.png`           `c7da006b0724127581662e5a853654ef69f92bd909582e74d94d4cb6a0fc11d7`
- `robotarm/in.png`            `9e41ccc13c149f3f82ca52d3b41bcfcfcb831fdbb5665601ccab837e4b61566e`
- `robotarm/out.png`           `b4f0125e39d28a71e5f9d2b16c2abf9fe257c9a4cbf91b5737c0a91ddf91a648`
- `<conveyor|robotarm>/<in|out>.png.mcmeta` (4 copies)
  `920176fe0c003f6f293aab5fc344418356377d273c414a8f5378755e25674891`

Path mapping (declared, the redstone-conductor precedent): upstream
`machines/covers/<conveyor|robotarm>/<in|out>` is lowercased to
`<conveyor|robotarm>/<in|out>` (1.20.1 `ResourceLocation` charset), landing
under `textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite ids `gt6:block/conveyor/<in|out>` and
`gt6:block/robotarm/<in|out>` with zero extra atlas wiring. The PNGs are the
upstream 16x64 four-frame animation strips (frametime 2, the `.mcmeta`
contents copied verbatim), so the plate sprites stay ANIMATED in 1.20.1 — the
vanilla atlas animation system drives the frames, the plate quads sample the
logical 16x16 region. The upstream `BACKGROUND_COVER` layer (covers/base.png,
the attachment/holder faces) is NOT borrowed — it folds into the
single-sprite plate like every cover (AbstractCoverDefault :71-72 defaults).

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Cover plate BACKGROUND layer textures, task p11-render-cover-multilayer: the 2
PNGs under `gt6/textures/block/covers/` and `gt6/textures/block/cover_switch/`
come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/`
byte-identical to upstream, sha256 verified:

- `covers/base.png`           `1e69e5b9205f64da1f3f24b6f717ce5e95d9b11394769cbccc820852789f8bdc`
  (upstream `machines/covers/base.png` — the `AbstractCoverDefault.java:111`
  `BACKGROUND_COVER` shared plate background)
- `cover_switch/base.png`     `29775ad9a45b231e3725fe4336a2f00b35b6ecf1568cf9f66b15b6d646a58db7`
  (upstream `machines/covers/coverswitch/base.png` — the
  `CoverControllerCovers.java:104` `sTextureBackground`, the cover controller's
  OWN background, not the shared base)

This section SUPERSEDES the per-card "the upstream `BACKGROUND_COVER` layer is
NOT borrowed — it folds into the single-sprite plate" notes above (p9 emitter,
p10 conductor/switch, p11 shutter/filter/controllers/conveyor notes): the
p11-render-cover-multilayer snapshot carries a per-face LAYER TABLE now
(`GTCoverRenderSnapshot.layers` — the census `BlockTextureMulti` stack, bottom
first), so every registered cover plate renders `covers/base` (or
`cover_switch/base` for the cover controller) beneath its surface sprite, each
layer offset by the plate epsilon per index against z-fighting. The earlier
folds are thereby undone without re-touching any of those PNGs. The emitter's
16 tier PNGs stay the offline `underlay + digit` composition and now ride one
`covers/base` layer beneath them (upstream paints `BACKGROUND_COVER` under the
surface multi, `CoverRedstoneEmitter.java:112`).

Path mapping (declared, the redstone-conductor precedent):
`machines/covers/base` → `covers/base` and
`machines/covers/coverswitch/base` → `cover_switch/base` (1.20.1
`ResourceLocation` charset), landing under `textures/block/` — the vanilla block
atlas `directory("block")` source auto-stitches the sprite ids
`gt6:block/covers/base` and `gt6:block/cover_switch/base` with zero extra atlas
wiring. Filenames lowercased on borrow, contents byte-identical, no rescaling
or redrawing.

Copied on 2026-09-01. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Axle block texture, task p12-axle-family: the 1 PNG
`gt6/textures/block/axle.png` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/iconsets/AXLE.png`,
byte-identical to upstream, sha256 verified:

- `axle.png`   `12f95f070c3eb11a04e5a5ae5a8e461e88b1f13bc71bed33fad9f4187474a49b`

Path mapping (declared, the p11-wire-fiber-texture precedent): upstream
`iconsets/AXLE.png` is lowercased to `axle.png`, landing under
`textures/block/` (1.20.1 `ResourceLocation` charset). Declared deviation: the
upstream axle picks per-material textures with a rotation animation group
(`Textures.BlockIcons.AXLES[...]` per side x rotationDir, MultiTileEntityAxle
:171-172; the `AXLE_CLOCKWISE`/`AXLE_COUNTERCLOCKWISE` mcmeta animation strips)
— the port ships ONE shared static texture over all 44 material x diameter
rows as the placeholder (the tint/animation/render-family is the render pool
card; the GTWires shared-texture form). The upstream `.mcmeta` files are NOT
borrowed (no animation carrier in the static cube-column model).

Logistics Tank block texture, task p12-barrel-keepfilter-logistics: the 1 PNG
`gt6/textures/block/barrel_logistics.png` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/tanks/logistics/colored/side.png`
(the `MultiTileEntityBarrelLogistics.java:44-46` `getTexture2` colored base
sprite for the TBS sides, 16x16 RGBA), byte-identical to upstream, sha256
verified:

- `barrel_logistics.png` `f348ace8e2d98ed701a060a87c4c66ec25b708449cd96611b66aa01cc9c595f1`

Path mapping (declared): upstream
`machines/tanks/logistics/colored/side` flattens to `barrel_logistics` — the
barrel family's one-PNG-per-row `cube_all` convention (GT6BlockStates
`addBarrel`, the p4/p6 shape; the upstream TESR/lid two-pass form is a
feature-layer omission). Declared deviation (the crank/crowbar single-pass
precedent): the upstream look is `BlockTextureMulti(colored, overlay)` tinted
with the ANY.W material colour; the port renders the grayscale colored base
un-tinted and does NOT borrow the `overlay/{bottom,top,side}.png` group — the
mRGBa tint and the overlay pass ride the render pool with the other barrels.
Filenames lowercased on borrow, contents byte-identical, no rescaling or
redrawing.

Fluid Tap + Fluid Funnel block textures, task p12-tap-funnel-attachment: the 2 PNGs
under `gt6/textures/block/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/tools/` (the
`MultiTileEntityFluidTap.java:130-133` / `MultiTileEntityFluidFunnel.java:130-132`
`machines/tools/<tap|funnel>/colored|overlay` icon stack), byte-identical to upstream,
sha256 verified:

- `tap.png`     `df1d3c727e965deeee4ce13a6670fdfb3f77aaff80e735f2a1ed489f38114412`
  (upstream `machines/tools/tap/colored/side.png`)
- `funnel.png`  `df1d3c727e965deeee4ce13a6670fdfb3f77aaff80e735f2a1ed489f38114412`
  (upstream `machines/tools/funnel/colored/side.png` — the SAME grayscale icon bytes
  as the tap side; the two families were visually separated upstream by the OVERLAY
  pass and the mRGBa material tint, which the port does not carry)

Shared per family: all 6 tap rows' model JSONs reference the one `tap.png`, all 6
funnel rows the one `funnel.png` (the barrel_metal shared-PNG precedent). Path
mapping (declared, the crank precedent): upstream `machines/tools/<family>/colored/side`
is flat-mapped to `textures/block/<family>.png`; the grayscale icon renders UN-TINTED
over the whole cube — the mRGBa material tint (MT.Ceramic/Plastic/StainlessSteel/...)
and the multi-pass overlay/faucet stack (MultiTileEntityFluidTap getRenderPasses2
:178-208) ride the render pool card, the single-model deviation the crank card
declared first.
Steam Engine family textures, task p12-engine-steam: the 3 PNGs under
`gt6/textures/block/steam_engine_{front,back,side}.png` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/engines/kinetic_steam/colored/`
(the `MultiTileEntityEngineSteam.java:276-284` `sColoreds` icon group —
`front` = the KU emit face, `back` = the steam face, `side` = the four
perpendicular faces), byte-identical to upstream, sha256 verified:

- `steam_engine_front.png` `33bcc0c562a18e2c773a9c6259b7751a7a21029befcc79b89c383de7d779495d`
- `steam_engine_back.png`  `9927d1c9d187485779681587e2c0d5cec7198bc29ccfc7e75691730e07a149b5`
- `steam_engine_side.png`  `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`

Path mapping (declared, the crank precedent): upstream
`machines/engines/kinetic_steam/colored/<face>` lowercased to
`steam_engine_<face>.png` under `textures/block/`. The upstream
`overlay/<face>` second layer (the :285-294 `sOverlays` stack) is NOT
borrowed — the port renders the single-pass cube model (the
p8-prefixblock-render single-pass grayscale ruling). Both the Steam Engine
and Strong Steam Engine loader rows share this one texture group upstream
(same MTE class, no per-material icon differentiation), so all 28 blocks
share the three sprites. Upstream tints the grayscale `colored` icons with
the row material's mRGBa and renders the heat gauge via the
`sEngineColors[mState]` engine core (:56, :269) — the port shows the
grayscale icons un-tinted with no active/heat visuals: declared deviation,
the tint and the heat-state model swap ride the render pool card.

Copied on 2026-09-02. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Diesel Engine block texture, task p12-engine-diesel: the 1 PNG
`gt6/textures/block/diesel_engine.png` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/generators/motor_liquid/colored/front.png`,
byte-identical to upstream, sha256 verified:

- `diesel_engine.png`   `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977`

Path mapping (the p12-axle-family precedent): the upstream MTE icon
`machines/generators/motor_liquid/colored/front` lands as
`textures/block/diesel_engine.png` (1.20.1 `ResourceLocation` charset).
Declared deviation: upstream layers per-face colored icons with the
`overlay`/`overlay_active` fronts (MultiTileEntityMotorLiquid :242-254,
getTexture2 :216-221) and tints them per material (mRGBa) — the port ships
ONE shared static texture over all 8 tiers, the FACING property drives the
emit side, not the visuals (the crank ruling; the per-face/active/tint
family is the render pool card). The overlay PNGs are NOT borrowed.

Copied on 2026-09-02. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Gearbox + rotation transformer block textures, task p12-gearbox-transformer: the 3
PNGs under `gt6/textures/block/` come from upstream
`src/main/resources/assets/gregtech/textures/`, byte-identical to upstream, sha256
verified:

- `gearbox.png`                    `6f36d27836e0ee13fc54515e58fac44b75347d7b00d0ead792e24c846f4853f8`  (iconsets/GEARBOX.png)
- `transformer_rotation_front.png` `fd2ac7adc0f7aa8ab2d0434a17847fe747f6b50ec3b16d8499da945f3b7db766`  (machines/transformers/transformer_rotation/colored/front.png)
- `transformer_rotation_side.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (machines/transformers/transformer_rotation/colored/side.png)

Path mapping (declared, the p12-axle-family precedent): upstream iconset/machine
paths flatten to lowercase snake files under `textures/block/`. Declared deviation:
the upstream gearbox renders per-face gear/axle overlays keyed on the connection mask
with clockwise/counterclockwise rotation icons (MultiTileEntityGearBox :331-344) and
the transformer carries colored+overlay two-layer stacks with an active variant
(MultiTileEntityTransformerRotation :36-40) — the port ships the static base
textures only (the shared cube / orientable models; the mask overlay and the active
tint are the render pool card).

Copied on 2026-09-02. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).
