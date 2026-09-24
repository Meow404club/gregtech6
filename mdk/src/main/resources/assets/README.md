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

- `gt6/textures/item/chisel.png` — chisel item texture (upstream
  `textures/items/iconsets/HANDLE_CHISEL.png`, task p16-chisel-decalcify; sha256
  `fef0d79fe8722697dae9175426db103b29478be033ed06a186f65fddc6e5bd2e`).
  Same single-steel-tier story as the crowbar/cutter: upstream composes the material
  head layer (`toolHeadChisel`) over this handle icon and tints both with the tool
  material colour (GT_Tool_Chisel.getIcon :88, the material pool); the port shows the
  grayscale handle silhouette un-tinted — declared deviation, same runtime-tint pool
  (spec ③ resolves to no-tint, no client listener row).

- `gt6/textures/item/file.png` — file item texture (upstream
  `textures/items/iconsets/HANDLE_FILE.png`, task p24-tool-system; sha256
  `fef0d79fe8722697dae9175426db103b29478be033ed06a186f65fddc6e5bd2e`).
  Same single-steel-tier story as the chisel: upstream composes the material
  head layer (`toolHeadFile`) over this handle icon and tints both with the tool
  material colour (GT_Tool_File.getIcon :91-92, the material pool); the port shows the
  grayscale handle silhouette un-tinted — declared deviation, same runtime-tint pool.
- `gt6/textures/item/saw.png` — saw item texture (upstream
  `textures/items/iconsets/HANDLE_SAW.png`, task p24-tool-system; sha256
  `3ab930729112f139863462767c026f4e327a62b23e81bf3ae7b60d57e6dbe952`).
  Same story as the file: the upstream head layer (`toolHeadSaw`) rides the material
  pool (GT_Tool_Saw.getIcon :186-187); the port shows the grayscale handle silhouette
  un-tinted — declared deviation, same runtime-tint pool.
- `gt6/textures/item/builder_wand.png` — builder wand item texture (upstream
  `textures/items/materialicons/EMERALD/toolHeadBuilderwand.png`, task
  p24-builder-wand; sha256
  `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`).
  Upstream has no iconsets face for this tool: GT_Tool_Builderwand.getIcon :52 renders
  the primary material's texture-set `toolHeadBuilderwand` icon, the default primary
  being MT.Heliodor (the :52 fallback) = the emerald factory (MT.java:210,
  SET_EMERALD) — hence the EMERALD set borrow. Upstream tints it with the tool
  material colour (getRGBa :56-58); the port shows the grayscale head un-tinted at
  the single tier — declared deviation, the family runtime-tint pool.
- `gt6/textures/item/screwdriver.png` / `screwdriver_overlay.png` — screwdriver handle
  textures (upstream `textures/items/iconsets/HANDLE_SCREWDRIVER.png` +
  `_OVERLAY.png`, task p38-issue6-tool-4layer-tint; base sha256
  `fef0d79fe8722697dae9175426db103b29478be033ed06a186f65fddc6e5bd2e` — the shared
  handle sprite, byte-identical to the HANDLE_CHISEL/HANDLE_FILE borrows above; overlay
  sha256 `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510` — the same
  overlay bytes as the METALLIC head overlay). Byte-identical borrows, filenames
  lowercased on borrow. SUPERSEDES the p24-screwdriver-item composed-single ruling
  (GitHub issue #6): the offline composite is retired for the four-layer model — the
  head half rides the in-tree toolHeadScrewdriver metallic-set pair, the handle tint =
  the secondary with the Spruce fallback (GT_Tool_Screwdriver.getIcon :115-117 +
  getRGBa :120-122, GT6ToolLadder.fourPassTintARGB).
- `gt6/textures/item/sword.png` / `sword_overlay.png` — sword handle textures (upstream
  `textures/items/iconsets/HANDLE_SWORD.png` + `_OVERLAY.png`, task p29-w5-t2-blade-six;
  base sha256 `f678c93377fcd7799c37429c06182f18e20563125f285bbfe437e3956dfb74e5`).
  Byte-identical borrows, filenames lowercased on borrow; the head half rides the
  in-tree toolHeadSword metallic-set pair (upstream GT_Tool_Sword.getIcon :113-115).
- `gt6/textures/item/knife.png` / `knife_overlay.png` — knife icon borrows (upstream
  `textures/items/iconsets/KNIFE.png` + `_OVERLAY.png`, task p29-w5-t2-blade-six; base
  sha256 `e0a3ac8791620ccf66e17e319914e4105b8578c58bae43d3a4d0048da84e227b`), the VOID
  handle face (GT_Tool_Knife.getIcon :85-87).
- `gt6/textures/item/butchery_knife.png` / `butchery_knife_overlay.png` — butchery-knife
  icon borrows (upstream `BUTCHERYKNIFE.png` + `_OVERLAY.png`, task p29-w5-t2-blade-six;
  base sha256 `8d69d7ea6ac203a5b550aede86362656252678ba30c19c39aadbd9db358623f3`), the
  VOID handle face (GT_Tool_ButcheryKnife.getIcon :97-99).
- `gt6/textures/item/club.png` / `club_overlay.png` — club icon borrows (upstream
  `CLUB.png` + `_OVERLAY.png`, task p29-w5-t2-blade-six; base sha256
  `b8ed2f3b2170421783419e88981018d4fe5ed3692ff5b3039a54b457f1e576d8`), the VOID handle
  face (GT_Tool_Club.getIcon :113-115).
- `gt6/textures/item/wrench.png` — wrench item texture (upstream
  `textures/items/iconsets/WRENCH.png`, task p25-tool-hammer-wrench; sha256
  `7c5b36f19b5607c9cc122f72b5796512db091e5baa2d815ef649d1761d9351cc`).
  Byte-identical borrow, filename lowercased on borrow. The standalone full-silhouette
  wrench face (upstream's per-material render composes `toolHeadWrench` head over a
  stick handle — GT_Tool_Wrench.getIcon, the material pool); the port shows the
  grayscale silhouette un-tinted at the single steel tier — declared deviation, the
  family runtime-tint pool.
- `gt6/textures/item/hammer.png` — RETIRED (task p38-issue6-tool-4layer-tint
  SUPERSEDES the p25-tool-hammer-wrench composition ruling, GitHub issue #6): the
  offline-composed flat icon is gone — the hard hammer rides the four-layer model over
  in-tree sprites only (the toolHeadHammer metallic-set pair for the head + the WOOD-set
  stick pair for the handle, the secondary MT.WOODS.Spruce ride,
  GT_Tool_HardHammer.getIcon :123; the tint = getRGBa :127-129 via
  GT6ToolLadder.fourPassTintARGB). Zero new texture files; the old compose sources
  (stick.png + METALLIC/toolHeadHammer.png, sha256 verified at compose time) both live
  in-tree under material_sets.
- `gt6/textures/item/bending_cylinder_small.png` — small bending cylinder item texture
  (upstream `textures/items/iconsets/BENDING_CYLINDER_SMALL.png`, task
  p25-food-can-row0; sha256
  `686b961d1477d133a1a16abc8c292f9648f3c3d92e3fa1b32ae725b7132ea570`).
  Byte-identical borrow, filename lowercased on borrow. The upstream tool head icon
  (GT_Tool_BendingCylinderSmall.getIcon — the head half; the handle half is
  `Textures.ItemIcons.VOID`, i.e. NOTHING to compose — a single-layer icon); the port
  shows the grayscale icon un-tinted at the single steel tier — declared deviation,
  the family runtime-tint pool.
- `gt6/textures/item/{scissors,scoop,plunger,flint_tinder,rolling_pin,bending_cylinder}.png`
  (+ the `_overlay` twins, 12 textures, task p29-w5-t5-scene-six) — byte-identical
  borrows from upstream `textures/items/iconsets/{SCISSORS,SCOOP,PLUNGER,FLINT_TINDER,
  ROLLING_PIN,BENDING_CYLINDER}{,_OVERLAY}.png`, filenames lowercased on borrow; sha256
  heads: scissors `e03dde3e0ac705ff`, scoop `7fe747cd488395e6`, plunger `39e8d99b127e6d7c`,
  flint_tinder `9c5bef3c9c4e6e47`, rolling_pin `dc17057249cad058`,
  bending_cylinder `173e914c8435f202`. The upstream tool head icons
  (each GT_Tool_*.getIcon head half; the handle half is `Textures.ItemIcons.VOID`
  for the crafting trio — a single-layer icon composed over its own shadow overlay);
  the port shows the grayscale icons un-tinted at the single steel tier — declared
  deviation, the family runtime-tint pool.
- `gt6/textures/item/food_can/*.png` — the food-can row0 subset (8 textures, task
  p25-food-can-row0), byte-identical borrows renamed to the registered item ids:
  - `empty.png`            `textures/items/gt.multiitem.randomtools/998.png`
    (`3d9075d45ec657759fedfc71a95a03a85fe399ba3537d04e8daec0172f18c63e` — the empty
    can lives on the RANDOMTOOLS multiitem upstream, MultiItemRandomTools.java:234)
  - `rotten_tiny.png`      `textures/items/gt.multiitem.cans/11.png`
    (`93ad271d40903b016710e9c68f7c5afc276cde7175a91c936008a415a645a788`)
  - `rotten_small.png`     `textures/items/gt.multiitem.cans/12.png`
    (`86a2397da14f7fc57dea5793b91cb3eb36e904159a90e017144d1c3183d95a34`)
  - `rotten_tall.png`      `textures/items/gt.multiitem.cans/13.png`
    (`0b4e13e051fb55149c86ad9596b5bccb8b975b5e6055af1b7a273567dd87abee`)
  - `rotten_wide.png`      `textures/items/gt.multiitem.cans/14.png`
    (`f083ee50be1d1ec2712ccb14239f7e2505010c805b8973020ae84715f23bb282`)
  - `rotten_large.png`     `textures/items/gt.multiitem.cans/15.png`
    (`be159c3caeb62c43a469bc82b1f5fedadcf9bef0ea44f9296145185f6295b562`)
  - `rotten_huge.png`      `textures/items/gt.multiitem.cans/16.png`
    (`1daa9b014ce22c4c24562f654b903df3c4b8b3039c67c4ffce6a87867c7ea2f0`)
  - `cookies_huge.png`     `textures/items/gt.multiitem.cans/86.png`
    (`667ceb61f34ab0ad82bf1536aa0771b90158323883ca448a0af96336fa644531` — the Cookie
    Tin output; the other five Cookies tiers stay pooled)
  All 16x16 RGBA, CC0 1.0 per the upstream README block.

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

Copied on 2026-08-31; the chisel item texture (task p16-chisel-decalcify) on
2026-09-05. Upstream license: **CC0 1.0 Universal Public Domain
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

Item Retriever cover textures, task p31-retriever-cover: the 2 PNGs under
`gt6/textures/block/retrieveritem/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/retrieveritem/`
(the `CoverRetrieverItem.java:147-148` normal/inverted sprite pair),
byte-identical to upstream, sha256 verified:

- `retrieveritem/normal.png`   `c4b860dcc9d973fe0d457204d5e0e106e86e09e4c96d7758004d64a09ceff3bd`
- `retrieveritem/inverted.png` `6b6b96a216f6b914c0d21758314902fd5e6a24191f0d5398c694a8404722e1aa`

Path mapping (declared, the filter-item precedent): upstream
`machines/covers/retrieveritem/<normal|inverted>` lowercased/underscored to
`retrieveritem/<...>` (1.20.1 `ResourceLocation` charset), landing under
`textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite ids `gt6:block/retrieveritem/<normal|inverted>` with
zero extra atlas wiring. The upstream `BACKGROUND_COVER` layer
(covers/base.png, the attachment/holder faces) is NOT borrowed — it folds into
the single-sprite plate like every cover (AbstractCoverDefault :71-72
defaults).

Copied on 2026-09-17. Upstream license: **CC0 1.0 Universal Public Domain
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

- `gearbox.png`                    `6f36d27836e0ee13fc54515e58fac44b75347d7b00d0ead792e24c846f4853f8`  (blocks/iconsets/GEARBOX.png)
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

Burning Box family block textures, task p13-burning-box-family: the 4 PNGs under
`gt6/textures/block/burning_box_*.png` are GENERATED grayscale placeholders — the
upstream burning-box iconsets
(`machines/generators/burning_{solid,liquid,gas,fluidbed}/colored/*.png` +
`overlay`/`overlay_active` fronts) have no borrowable source in this repo's
snapshot tree. Declared deviation: upstream layers per-face colored icons with the
active overlay keyed on the synced mBurning bit (getTexture2,
MultiTileEntityGeneratorSolid :233-family) and tints per material mRGBa — the port
ships ONE shared static texture per FAMILY over all rows, the FACING property
drives the front (the fuel/ignite face) semantics, and the tint/active-overlay
family is the render pool card (the crank/steam-engine ruling repeated).

Generated on 2026-09-03 (no upstream bytes; nothing to sha256 — the placeholder
glyph is a dark border + box outline + glowing core, deterministic generator in
the card's tooling).

- `gt6/textures/block/large_boiler/{wall,transmitter,main}.png` — generated grayscale
  placeholders (task p13-large-boiler). Upstream ships no borrowable
  `machines/multiblockmains/largeboiler/` or `metalwalldense` texture group in this
  snapshot (the cokeoven census repeated); the borrow-or-declare rule keeps the
  script-generated placeholders.

Dryer GUI background texture, task p16-machine-fluid-gui: the 1 PNG
`gt6/textures/gui/machines/dryer.png` comes from upstream
`src/main/resources/assets/gregtech/textures/gui/machines/Dryer.png`
(256x256, the GTGuiScreen 176x166 panel canvas), byte-identical to upstream,
sha256 verified:

- `dryer.png`   `8bb2f8a89129ba44112d02ed2168c47df9f098d9098303e0505acc688090ac5e`

Path mapping (the p7-gui-family / p8-cokeoven-gui-menu precedents): upstream
`machines/Dryer` is lowercased to `machines/dryer` (1.20.1 `ResourceLocation`
charset) — the lowercased path is exactly what the DRYING RecipeMap declares
(`gt6:textures/gui/machines/dryer`, GT6RecipeMaps the p14 W1b card), parsed by
`GTBasicMachineScreen.backgroundOf`. The upstream canvas paints its slot frames
at the same coordinates the port's menu code uses (input (53,25), the 1-output
arm (107,25), the :267-268 fluid display banks (53,63)/(107,125,143,63)), so
the borrowed canvas and the port slot geometry align by construction.

Copied on 2026-09-05. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Canner family textures, task p24-canner-machine (5 PNGs, CC0 from the upstream
canner iconset, sha256 verified):

- `gt6/textures/gui/machines/canner.png` — the GUI background, upstream
  `src/main/resources/assets/gregtech/textures/gui/machines/Canner.png`
  (256x256 panel canvas), byte-identical, sha256
  `1b3fb6a8a3500fcca8880852c766d7946988e7fad0c63fa4fd56c87b29c33a28`.
  Path mapping: `machines/Canner` → `machines/canner` (the lowercase
  ResourceLocation convention, the dryer.png entry above); the declared path is
  exactly what the CANNER RecipeMap carries (`gt6:textures/gui/machines/canner`,
  the RM.java:148 RES_PATH_GUI+"machines/Canner" row). The upstream canvas
  paints its slot frames at the same coordinates the port menu uses — the
  2-input case-2 pair (35,25)+(53,25) and the 2-output arm (107/125,25), the
  :267-268 fluid display banks below — so the borrowed canvas and the port
  slot geometry align by construction (the dryer.png rationale, the 2-input
  ContainerCommonBasicMachine.java:57-60 case).

- `gt6/textures/block/canner_{colored,overlay,overlay_active,overlay_running}_front.png`
  — the p22 split-front four-layer borrow from upstream
  `src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/canner/`,
  produced by `mdk/tools/bake_machine_fronts.py --split-fronts
  --machine basicmachines/canner` (the p22 ACTIVE mode; single-frame sources
  byte-identical, the 16x64 `overlay_active` strip cropped to FRAME 0 and
  re-encoded — the P20 "animation stays retired" deviation):

  - `canner_colored_front.png`          `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (byte-identical to the six-family generic plate above)
  - `canner_overlay_front.png`          `c022d8c0cdfa583c643aa6e74cbd8f226961f73b3e6c0fde1117e7ae27c769c6` (byte copy)
  - `canner_overlay_front_active.png`   `2371d923ca5a3ec8b74a2b98e2d394ecfc5d167f258dd42ffeda3a08b6bdfb62` (FRAME 0 of 8 frames)
  - `canner_overlay_front_running.png`  `9adee5ae99074244c0fb95455d21fc5aac473154e0d969960c6dc0515e67c30f` (byte copy)

Copied on 2026-09-07. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).


Press + Extruder family fronts, task p26-w1-press-extruder-molds:

- `gt6/textures/block/press_{colored,overlay,overlay_active,overlay_running}_front.png`
  and `gt6/textures/block/extruder_{colored,overlay,overlay_active,overlay_running}_front.png`
  — the p22 split-front four-layer borrow from upstream
  `src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/press/`
  and `.../extruder/`, produced by `mdk/tools/bake_machine_fronts.py --split-fronts
  --machine basicmachines/press --machine basicmachines/extruder` (single-frame
  sources byte-identical; animation strips cropped to FRAME 0 — the P20 "animation
  stays retired" deviation). Product digests (sha256):

  - `press_colored_front.png`            `4d8ee0b3a3b6d5979caeecb74e7044d79f3e4b9abf3f19e0dc36ffe0ea2d0be2` (borrowed)
  - `press_overlay_front.png`            `b6fd3ae7a44bb1b25a28f70b1ebba42625cda93e796d35ee5be6c8f18f753dd2` (borrowed)
  - `press_overlay_front_active.png`     `236c0f2f57cf49b094a024a9c9c35e034fcb327863e87642da22b7d0267987be` (FRAME 0)
  - `press_overlay_front_running.png`    `016292a072f18b7b9cb149c4a937835eeae980949b6986846835245d8247cb88` (FRAME 0)
  - `extruder_colored_front.png`         `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (borrowed — the family-generic plate)
  - `extruder_overlay_front.png`         `8e42c51db2a77fb0b88d3ca083ffe35b50d4ee9d3b5b947a69774a419e1dd927` (borrowed)
  - `extruder_overlay_front_active.png`  `33bc918a0a541c8e42a3b82c32e8126ba22a80c4cc3785909499e7ad9f9167a2` (FRAME 0 of 6 frames)
  - `extruder_overlay_front_running.png` `1aef3ed947ed362664674372b3ed0602da79dcf3303a4cadf40ba072dc182805` (borrowed)

- `gt6/textures/item/shape_extruder/{plate,rod}.png` — COMPOSED placeholders (the P20
  stdlib generator convention, 16x16 RGBA): the upstream Shape_Extruder_* icons are
  meta-atlas tiles on the MultiItemTechnological spritesheet with no standalone sprite
  file to borrow, so the two row0 mold icons are hand-rolled mold-blank pixels. NOT
  byte-identical to upstream — declared placeholder.

- `gt6/textures/item/shape_slicer/{grid,split}.png` — COMPOSED placeholders (the P20
  stdlib generator convention, 16x16 RGBA, task p35-slicer-row-domain): the upstream
  Shape_Slicer_* icons are meta-atlas tiles with no standalone sprite file to borrow, so
  the two row0 blade icons are hand-rolled steel-frame pixels over the shape_extruder
  palette (grid = the 3x3 cutting lattice, split = the center split line). NOT
  byte-identical to upstream — declared placeholder. sha256
  `7d113c19a3a1790c8871b09c34f80b79f54b2edcfcde392cc6096890af050f70` /
  `1a636c548873b02229d006ba70aa36bf00ac1d70796c0f07ce101cd8c2d65e0e`.

- `gt6/textures/item/shape_slicer/{empty,flat,eigths,eigths_hollow,quarters,quarters_hollow}.png`
  — COMPOSED placeholders (the same generator convention, 16x16 RGBA, task
  p36-recipes-obtainability ruling B): the six census-completion icons over the same
  steel palette (empty = the bare frame interior, flat = the solid face, eigths = the
  2x4 cut lattice, quarters = the center cross, the two hollow forms carry the 2x2
  see-through center hole). NOT byte-identical to upstream — declared placeholder.
  sha256 `173c6041132dbddebd42b38f3c22a23b323e37f0f0a3cf391575cc72d8176f10` (empty) /
  `7729948ada0cac519c8b5dff7fe45323e2074a9326a1b2f101493b2c8c5a1b4f` (flat) /
  `086274fc8e48b3a56a4c3767d9f1dcb51a3702a47c4ea62bc53a4a56fee74cb6` (eigths) /
  `d8a6142ebbad2e125aa7fd430a462aacd9da275e3df8e3680c6430b5effb5029` (eigths_hollow) /
  `fe5b8fa14ececa7802846ba97746e8512b2d2c3267d08d13afeb7d42dde15149` (quarters) /
  `ac70dabecc977563ca1ab10a6256389f46ac33ce4971793e422d84e5cc689648` (quarters_hollow).

Copied on 2026-09-09. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Distillery family + Integrated Circuit textures, task p16-distillery-family:

- `gt6/textures/item/integrated_circuit.png` — the Integrated Circuit item icon
  (upstream `src/main/resources/assets/gregapi/textures/items/gt.integrated_circuit/0.png`,
  config-0 icon of the 256-icon damage ladder, ItemIntegratedCircuit.java:118), 16x16,
  byte-identical to upstream, sha256 verified:

  - `integrated_circuit.png`   `ce72e7832572432152196b3f3a96bc0efd5ed9da41878b436e9022d0090837f2`

  Declared deviation: the upstream damage ladder registers 25 icons (configs 0-24) and
  swaps them by stack damage — the research card cuts that to ONE model + the
  "Configuration: N" tooltip; the config-0 icon is the one the creative surface and the
  acceptance feed ever show.

- `gt6/textures/block/distillery_front{,_active,_running}.png` — BAKED from the
  upstream Distillery iconset
  (`src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/distillery/`,
  CC0 1.0 per the upstream `README.md` block above), task p19-distillery-front-canonical.
  Upstream is a MULTI-LAYER per-face stack (the MultiTileEntityBasicMachine getTexture2
  pass system): an opaque grayscale `colored/` material base with transparent-bearing
  `overlay*` decals drawn on top at runtime — borrowing an overlay alone would leave the
  hollow background pixels see-through, so each front is baked with the standard
  src-over operator (what GL_SRC_ALPHA / ONE_MINUS_SRC_ALPHA does when the engine stacks
  the passes; overlay on top, output alpha = 255):
  `front = colored + overlay`, `front_active = colored + overlay_active`,
  `front_running = colored + overlay_running`. Baking is reproducible via
  `mdk/tools/bake_distillery_fronts.py` (pure stdlib, deterministic bytes, prints every
  sha256). Source and product digests:

  - `colored/front.png`         `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
  - `overlay/front.png`         `a36ecca013dbd36ddc02f10d2ce0ec640030147ddb3c180c299fac67ad57e4d3`
  - `overlay_active/front.png`  `822b52fc5f647891438ad6467c58f05135946e6272b4e3826ccd9ffa7ce07ab6` (16x64 four-frame strip)
  - `overlay_running/front.png` `88b5c302301a4773e5b880878f9423a79d3e12c2ac05dbb2a3c31dd84c961a57`
  - `distillery_front.png`         `8a6ac14f9b121618afff00b3bf7372484adbe7a0307848ee9f96188536b3ce45`
  - `distillery_front_active.png`  `6fc656f9cd0a57f4023da022bb7056a6e05c93ac61f4369e7e785360ce5e7a4b`
  - `distillery_front_running.png` `f91ced667197de43286fee72872c960ef044defaffc1ed27acac70bb2f738e64`

  Declared deviations: (1) `overlay_active/front.png` is a 16x64 four-frame animation
  strip (1.7.10 auto-slices square frames) and the bake takes FRAME 0 as the static
  representative frame — the faithful four-frame animation stays in the render pool;
  (2) the colored base keeps its neutral grayscale, the per-material tint is not baked;
  (3) this bake is the canonical flat look, not the render end-state — the faithful
  multi-layer per-face pass system stays the render pool card.

Copied / generated on 2026-09-05. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

GT6 stone-family block textures, task p19-stoneblocks-render: the 272 PNGs under
`gt6/textures/block/stones/<stone>/<variant>.png` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/stones/gt.stone.<name>/<VARIANT>.png`
(the BlockStones.java:93-108 16-icon table, one folder per stone —
Loader_Rocks.java:56-139, the 17-stone CS.java:1668 set the registry card landed),
byte-identical to upstream, sha256 verified per file (manifest below). The stone
folder name maps `gt.stone.<a.b>` -> `a_b` (underscores; the vanilla sub-folders
`gt.stone.granite.black` etc. flatten), and the icon segment lowercases
(`BRICKS_CHISELED` -> `bricks_chiseled`, the StoneVariant.snake serialization) per
the 1.20.1 `ResourceLocation` charset — the same declared lowercase deviation as
every borrow above. These are the upstream DEDICATED per-variant color PNGs (not the
grayscale+tint route of the P8 materialicons): the datagen models carry NO tintindex.
Census 2026-09-06: 17 folders x 16 PNGs = 272 files, zero gaps, no `.mcmeta`
animations; 14 content-level duplicates exist WITHIN each stone
(`bricks.png` == `bricks_redstone.png` for 14 stones — upstream ships the identical
file for both variants), borrowed 1:1 anyway to keep the one-to-one path mapping.
Copied on 2026-09-06. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

- `gt.stone.` -> `gt6/textures/block/stones/granite_black/`:
  - `` `765d48c31f362adf290d8059b127d70f233972ee417a6aaac383c153876d7fd7`
  - `` `a614d26460f4ea7ea0abe3a4a0550137b60daf2889133535b8295e926fd5aab3`
  - `` `c434af7a6b6f03b68a64f4f260a558b5d5827fb00e37a237a217b8030378a410`
  - `` `e47399a3fd84551d3a561cf456bab049d8231f85d945f59264c27530c92dd585`
  - `` `45e807352c9835c47f71969b7f1a63d55cc8bbcf013de61a65cb915c3755dba5`
  - `` `29a1d3b11a3851e6862978aaea6e626bbcc0955ab90cc3f95558dd56a3699fa0`
  - `` `1faf6934e019aa60ab52f74e1616e6d0f865b279a8e1fd10f858dc79ddce654e`
  - `` `045426439cf1760be9ef78919296a3ed9784fb28dabc570a3e16813417848378`
  - `` `c305f3ae8da0a522ee6aec0a7245cbe55e8ae3c8243100b8eb08fcd8a77af040`
  - `` `e47399a3fd84551d3a561cf456bab049d8231f85d945f59264c27530c92dd585`
  - `` `008ca5fcf6cdad084ffd3115c9ebc8ce5884aab0437b8a70c57d90b486ddc2ef`
  - `` `e06eed0f58131eea68d030d743ec05be913ea177df9f8606ff6d632965cf62e9`
  - `` `76fce71f945a76a883a7bff69ececdfee8a77013a573f16c380fb8b5aeb2ec09`
  - `` `b6d1b35e5d063b83eceae4737ff3b9e4f38260b37d1047598017c9a5012091b2`
  - `` `dbc18d3f78db5cbfb6b9005724240bb7238c2f313b77ec541742b6579fd76c5f`
  - `` `a81865cdd1da1daa33b4c4f87f3d2a339a4561f05a91697a4af459ed6a4fa064`
- `gt.stone.` -> `gt6/textures/block/stones/granite_red/`:
  - `` `7b3b20a1907f03ba29f76b2dd7b6455e6a294fb36f78841c3564af4cd37f27ca`
  - `` `e036bc8a5f424d1627cf70d729ea36144b081bd0deebef57512a00b4c3ae61c1`
  - `` `382c60f5bd4e4d42239d932bf15ad2b481caf540b76b71aacb59429c7bde5588`
  - `` `a5a0e52972e3f87e4560cf244fceab5d0a96c9cf8d62a21acaa8bfd4cda1a0de`
  - `` `51724e0ca2683141ce781d38d609f317174bee694f430bc95454cdba98f69461`
  - `` `9e52ec17a0267172693c44f2b680c8a0a5c84b468da598ef6e25023b96a4e2b1`
  - `` `1e86462c8be3f63f9ebfd5c61ecffa922772ad6dff9452c8d9ba45a05c698b39`
  - `` `fb010736381e25754fb4d9872c1dd493c79e52f54a5972bc98918aa766a336b7`
  - `` `604cba1623c7d4b8e5ac84c56f89713b0e166d6e2539beff31c55204279311ff`
  - `` `a5a0e52972e3f87e4560cf244fceab5d0a96c9cf8d62a21acaa8bfd4cda1a0de`
  - `` `f1a12da2c24de2bafa4a9ff356e77cc09e74ee160391b5817faa323665749c53`
  - `` `6240292390576fa63a97635f0fa18ee52ada32a2edf2cecc8fa316cf00b3ace1`
  - `` `caf668a9a341288cda68e011cbb1cb616d71bf5ffede3f3ef273ebd5131db76b`
  - `` `0e770dc2d88afa3db6b07875ff2cc97dd0c726a11802aff526ab33e9cb69b293`
  - `` `c65def2bb0770aeee7dc938af51218367047d5f8486ddb53274ce16465aaf49b`
  - `` `5b16e56f7e89d5cc313677433abcb32e06075ccb590c39347354da14595bf4a7`
- `gt.stone.` -> `gt6/textures/block/stones/basalt/`:
  - `` `4d993a27bd065ed71babe249405dc43609b02feb24cf7843510ce3e128287462`
  - `` `7527c356f4d456d430ceb373e73cd79610da05951b243c6d006fcea18029c072`
  - `` `81f99264eea8fb40df664f4286bc15f16292ad3d76d7cacb8f19b3af0a590f4d`
  - `` `c41e08bbe277145a514a32f0371947fcb3760e1bb1c411c69e90c446e54423df`
  - `` `df3298e2a1ece7940698aa3416e13a36f5ab8f8bb4d78072c1d509201d884ef3`
  - `` `29eefb37a45da1ea10622b95221ffdc059c345efcccd16ab1523725eea5e7b17`
  - `` `176cb5afd7d9989b256f344e8e21776ed1697d176c774175663625633fc98fab`
  - `` `795fd85ec0782d96ff7645a475bed72ecc5a84c6bd187864a3472480cb4273db`
  - `` `6d717e0bc41f47f5f45d4bfb3f5859ce259870d2e762b534ba0a480dc61ad4ce`
  - `` `c41e08bbe277145a514a32f0371947fcb3760e1bb1c411c69e90c446e54423df`
  - `` `70014ce5f2635da10fbf31f233668f4a1436ac7e740362ed884cb7f75a3fa01a`
  - `` `7d1abb7d7e004a0671133d8c6f0a7e8a6fc4276681108e86f06978c3807f8c7b`
  - `` `42cce79a6cd6028385f836f17532f6eeef2264a6944501a93b0ee9403ed3528e`
  - `` `f3b3c7d2f21263e47179ae00cab0e2b39c3ac5854f843261f35f574c74a7818c`
  - `` `2f523462a7b18265f17a53dabc3594bf4cb1ff13241536de218fb8c3dee78a8d`
  - `` `ec86de1f1bcd38ce91b64f8248e51074dcd99b9eea65b2e5aae2318c2c89a894`
- `gt.stone.` -> `gt6/textures/block/stones/marble/`:
  - `` `148be8d82038f33601a5a8bd80383d979e50467303a02902536337918e72a907`
  - `` `46f62629be41e93de516614b0f880fd405979a5d1a5eee9368c2e2c702706327`
  - `` `00b1f8152025faf1f569a8559c099e83676f6b47c1744bdbdec2e3ab1b018bf2`
  - `` `52ec285b0739d28a1d8cb00391e5bfddeec576a0fca22ea7a0d6509ab41cac8e`
  - `` `6d380756a571a2eaf1b7ceaa084c02502ed18770d63e4c6098d096feb5980192`
  - `` `7ad2faa19a6c62dc367610ac0a6dc73adc55f60d519b149833fa46be272822fb`
  - `` `f57d734395f192522125e1333346d45310c594cba742997e968dff47ea44a3c4`
  - `` `62c4f63d491da95fcb5bfba53462f5ecddc5a100738ae0867b0364a460ac3b69`
  - `` `62bb919d5f47f8d93907915785d52403d092e3a65f34cc9d54c6d26a92033e9d`
  - `` `52ec285b0739d28a1d8cb00391e5bfddeec576a0fca22ea7a0d6509ab41cac8e`
  - `` `e37c45de56dd0e8fd3f5c3aebc05efb0dc5e728c2798db989ffb73036995eb70`
  - `` `1267f2a0e668cd0cddb69727ca463bd61a0f882129fbd802504f3ea7693ef699`
  - `` `a8049413f6d5386da3508349eed1939aa334c06f87cd9ef7185634b7e4e5ddc8`
  - `` `d6270ecb53fcd592abe8a4a782ef0c6c485c6a8d75f42ddcf2bbe56e9001f7c6`
  - `` `6ed82a902561af30ecdcf518da1bfc8c5eb38abfbdab859ebf3949d6dfe6d4f0`
  - `` `b6be9e3cf4e4dd421034071f8183f65c2bce1394d76a28d1ec89c35499d8af6c`
- `gt.stone.` -> `gt6/textures/block/stones/limestone/`:
  - `` `a9c2ce7b7d76c21eb61875703f4b2f8f2347546a4ea903d7dd7b9f7bbbb7e4f0`
  - `` `4223677ddc53692dd4944eafe14dc7abe89a01df0ca6a50eeb654713d4cd3425`
  - `` `b33af7ed9660d25a65e04eb24fa16b0dd903d868ddac1ea11d16b74dc0c83b11`
  - `` `7d0f375f23a2d2700da05cba18c1f7f4bd01ee9c016660010237b1a994aa51ae`
  - `` `4bb9f1bf6ad01654ef237f73960b4d189b9ee34ad0f6457a7bf41307ca7cc214`
  - `` `40be9607ef251b98eaffdb358c8a040f8675141674b95f83827ade9f80018ac2`
  - `` `3afd20555bd92053dc60e2dec27be01cfecd579c8cadea28ed175c9443683626`
  - `` `7bec658ab9fd7f57f2a2ea7529c58e115dc1bc70cdb1cebfb93e58234e81309d`
  - `` `b64544e99eb348ca45e9139c5fb41ac31cdd11959af9cc4e5490326896e80d37`
  - `` `7d0f375f23a2d2700da05cba18c1f7f4bd01ee9c016660010237b1a994aa51ae`
  - `` `9727f48eb48321586e331b82e807221e45a899349cfd452476f3d276f6d786da`
  - `` `dd9c50f14c246c359a79acb4c00aeb1f9c57d8fa2617173f8f1ba9ae8140cc76`
  - `` `b776180c5fbc465576bba9405293c87a407508102618e216199e1be53edffd6e`
  - `` `acb74aad858efc034b8a7475d638b05b2a2aab6535ad6f59fb0a88ddbd63313e`
  - `` `b981c19ef4dfb46de306db933f3069a984e233adea9f2bdecd8a2b8bcfeb50f5`
  - `` `32337a355fbcff7c6dc870bd18849c3e6068925fc2572214e71df9c5525aef92`
- `gt.stone.` -> `gt6/textures/block/stones/granite/`:
  - `` `6b725510dcfd435b9eec1f90b23ca93dc438850c4c055583f055a21c61455f30`
  - `` `ebba3397e583246d3f61d7dd69530dd1417952ebdfe32c3aec8b8d11fe8ca6dc`
  - `` `04e512b15b77538747b1717adb3a33f50c84d1a32769e9dc65ee2e6dfff71adf`
  - `` `007003029468d88e9ec2f2ce9445a935cf59482c642a51ff09a0511f067634be`
  - `` `b122d45d805748096cf3067f4d59f4222c90ba532490086a747ec820f87e8ea3`
  - `` `f21e6c3de3196907e70b9aafd9d1d65113b701c83d72a4620a960ea56449a41a`
  - `` `8c8973b782229c65559e4866fca02a36bb3708b4ba71d407dd33f336583c2fc7`
  - `` `f89df47749ea16885e4a5068edd22658623e1ccd6db7f341f7e1f31d1fbf18fe`
  - `` `1c5f1632d7385ec5b9ec678bb5c92c0d3b3c00eaff3222f40d0111cedf458ebf`
  - `` `007003029468d88e9ec2f2ce9445a935cf59482c642a51ff09a0511f067634be`
  - `` `f181353b57b8550150ec9f97f4594cec5e7b7ff3db03e333a99c627d09dd72ea`
  - `` `613d9e3ef6a721b95a7a3b6580d6f3280dfbeaca000bb182559a5948e5defdf1`
  - `` `a491a1b7e32312aab11f3cef4edc8d4bda4fbec95d00839d0afaf812e447b1a0`
  - `` `1fd4be87cfaa7739adfcf3fd22ab5bf4e860aafe728882207e45ea15ced54716`
  - `` `00876ec1e813c13785cd42f44233621a2ed0d6056c8bd2aca0660b2a8a45d4de`
  - `` `2474335e645f302928db6a602a636cd4bc55d03466c3f806b3900e8af7f942f5`
- `gt.stone.` -> `gt6/textures/block/stones/diorite/`:
  - `` `7a93dcd388fef5759b6e6fee82c28fda816463aeee0430963555ea7735bf5d61`
  - `` `6c652328ded8921f2bf41c808f0b62e1f12e8e7085b5c4b1c4c815f2d118b6c8`
  - `` `cafb53a8c3eb7c97b7682104e4b9c21370651a7e4b50d66603e5b7f09563b706`
  - `` `0160de770b302274c2ac10bdbc8bbaf64dfcd7281eeeeb88e8909e1528e2a8ec`
  - `` `f272889f0070ff253be545a2e64e43901b3699cff906e6eb6a232cc3277e9e4f`
  - `` `f92f8ad270d444ef7c650057529c0b4440cc4648e5af1c8357fb8e919365195e`
  - `` `64138751dbddfb7217aba8b0cf91ffab56ec81939781612e46e6e181688188a4`
  - `` `7cf435a2835e4bad9b714a2e98838ec9b05a72a56407803cb195fbb779cd8097`
  - `` `4bb7fa469985a0154761aeeff35be0ab1d006f49f7e6d7fb7ab7877f79bd8db5`
  - `` `0160de770b302274c2ac10bdbc8bbaf64dfcd7281eeeeb88e8909e1528e2a8ec`
  - `` `665ee9329f2fad809ab070ef4e53806c1c67cfd60f857358d9c73949656b5dd3`
  - `` `ecac9818a8d8c473deb49dfad90e5e565daead36cf96b81b8254f8e225548036`
  - `` `e59d937136016ec4d967dd0186bf87aa38824ab8c7d6552f16b77aa81554f78d`
  - `` `d015278afeeffeb431dca89c7b05c44b286e6c3e367a8fc60b616c663f713aff`
  - `` `56413380b779527bbfb9fe70bb6c97af2f5e7604d3310f6f4f4874daf089bd27`
  - `` `3e67412faec43c4dd1bad64314686ebc1946a062c9092c4e0682f00eb571a9d8`
- `gt.stone.` -> `gt6/textures/block/stones/andesite/`:
  - `` `8f297386f7f2992fda88e2ee2236a3c36cbaab475bbcf64397ef447a01426a2d`
  - `` `b305aa1c2e8556ea2ac4ab5fc7d42d4abf9e179910dd9f76cca87035575f2321`
  - `` `abecf45c6d9d1c4bac56e534f83bf77a5e13ffa639398a96268ceba6e9fc492a`
  - `` `4c0eb7836db2423db08a5f3cc7813e4dd0b5ba23c4d5ed90dcd2b318bb382bb4`
  - `` `b4d70ed0082eaa371b4fa844875fa468e509767e58e77e90c9c283fbbaf875c9`
  - `` `f06b9d7e4bd453aedb8ce235952c7c49eeabf2caf25e1b950719561ab172a2a6`
  - `` `8619435f93b5c968f7a063304992e190b73e835fb21fa189f42bfa7ed9c42e0d`
  - `` `8c7b2100eedff1208d9be30c8382b9a2b69824b541bccc0310d1dc096c5e4e54`
  - `` `c28fbe4c63d22cbdeaf45deaac7f4eb17f7c161540bfa48819cab5f3f5a609d3`
  - `` `4c0eb7836db2423db08a5f3cc7813e4dd0b5ba23c4d5ed90dcd2b318bb382bb4`
  - `` `d7b521a00125caa6d21a273a798418f68011b88223140550dcad65cebcc08e45`
  - `` `50c434eeba77bfc028d22997e328334b0d731b2045152aa89ae17930113d7ba1`
  - `` `afb8b424e4aa321b4471b4fa5ad5f1b9f79edf5635b7964488c746003af40a2a`
  - `` `9d73fd7edb4e0bb450c6df892c0d1707e99cb3d8a624f9f120a0152fa6d1d18e`
  - `` `d2a12b9a46554e2058679ce493220bd2f2d7c9c5ea8995f0c3dc7c3e2f6a4d07`
  - `` `81fd2db0fe99ee098c05b644d44c4c5ea7f3eed21ec757dcd8d2ef95cdcf4f4f`
- `gt.stone.` -> `gt6/textures/block/stones/komatiite/`:
  - `` `02bf1697f01ba2978938d53cad78ee96e6cb7eea9f5681420a314d3660f2be0f`
  - `` `75f5f1f45969f880e81dd7f974967022dc7f1c3b2a0b357b6b15cc712f0b815a`
  - `` `4a58033b620830adaddb8bacf8f78b0670d54ebb27ca35eebaa0aeb86ce7fcc5`
  - `` `5c47f922e3ab552f5d562ddfb635266c466865a81eab24fcfe8c6b824fa00930`
  - `` `5c24bf03f221db56e6f5753cd0b67b3d834202a2dca6746485a67e02591bd73c`
  - `` `99f6dc9aa2313a7d36e877664971b25a559ff467b9b5f2b76c67d2150c223e1b`
  - `` `161b89413a4043aa9e26769df1ec470b071475fa9677be34b2d35a5fd0b3441d`
  - `` `43ca3cb5c377d8ae83217764395a6dc826b7b378d6048f966d75ad3560095372`
  - `` `da66e8230c6d4557815c776e1da34c16dd2bec604130ae88c88d2b52c4d02df2`
  - `` `5c47f922e3ab552f5d562ddfb635266c466865a81eab24fcfe8c6b824fa00930`
  - `` `229005e84f250706123ed3d8f986a022fae9e5804c83ed2178c851c60f6105c5`
  - `` `c3559fc368f19e6c44b4117f0d2b5f600f10441efc3934b83ca2869469a8b9ad`
  - `` `e574e4221d29d4d91c382175866942d6615e945aae8d7abaa0ebf1e11551a004`
  - `` `04110012c785f0a56ba5370e17f002c3dbb5fa59dca6d7a853d2afd44f4c8e95`
  - `` `4adc31ceb0e63c7e4a36d74403a42d6dfa7f5e74945511b27257a79c95781150`
  - `` `b72cd3946f29223e8fad8998fe7f2777f77069b3f72c9097f06073d4c57f99a1`
- `gt.stone.` -> `gt6/textures/block/stones/greenschist/`:
  - `` `7698c77866fb535711362bd00bf0faf892cba3de9b8b8eeb1ffbee8f9d17884e`
  - `` `16760b190263d8e2d39ba16452023e4c20c26c43abad3f20e4a69a35dc8b30bf`
  - `` `6a62deeb0cabd3daf8b26c1f96aedd5feed695c491914dfa85fddbe345fe3c9e`
  - `` `2216533e6b8531ed41843d86abd87991a02a677a6177b6cb9ec3a40b4505d65a`
  - `` `eeb86d3b62cb6b3c4e3aa947242699fdc6ce724ac37162a239dcf73d61b0ae5b`
  - `` `feb3d1999f6920e9092f493ee8c9e661cd61cb37b47f1fb4bb56bf9907ebe2eb`
  - `` `973f8cd56d1d1adb8ee4f85754adc6f956e0aa6fe53b072f0c6dd25bbf013228`
  - `` `183048010eb6ca47fa37dbd8a5916a751ab420ab909e1a9d64e55f163876fdfc`
  - `` `a95bc76dfd5bb854866fe796cba5b1d0d6d8ca76c34583978fb9100e1e256a57`
  - `` `2216533e6b8531ed41843d86abd87991a02a677a6177b6cb9ec3a40b4505d65a`
  - `` `b6095a91fb6621b1ad97eb410363d27571d0a6b31b3f15b76a27007338098da5`
  - `` `ddc6ea5cb18a192953c78ddad128c0ec9dd734fa66560f107bf054b2e6aa7c78`
  - `` `6b2acee2582c29e73b7afb6a44d99add3da5d91a815ec7d2c2aef5776f5b5940`
  - `` `392517c845de338f0d49097ec6256e43aaaa03f3f959e5a845ddd7eb71147d9d`
  - `` `3a285e6ef21d26c69acd107df2e409fe945f7e47431b1cd23ecd6650a1f5ff55`
  - `` `a8a17ca5c9437692601266c3844f0b6e6cd06e420bea6e1b5e2f390feaffebd0`
- `gt.stone.` -> `gt6/textures/block/stones/blueschist/`:
  - `` `6646775562f171f738352794c8363a823c98c0ffd39798cc9fe526b375d8a54a`
  - `` `3644d0a93165d3edbd115a6eb5b7757ffa1256c0fc4eff6f6daee91d1ce35080`
  - `` `45a653fd9677f0725a13d0c93f475c5663b1c97ec844e0297447b443c0b2e206`
  - `` `9c0d2eca1bd6550a93a0be0027d6fd17295d672ff947ecf4dae6b96ea73f2fcc`
  - `` `e09b2776c482027f76ce61f7d252817da587d381d37f771f2aa774d902647881`
  - `` `48a0107a3892f6ddc335eb9c599e749991dbd8a81d7600f8e7ab6fdea7b8507b`
  - `` `e00025dcf5f12d8d9d4b4d91aa1b72914c2577c0bf98b2bb8863fb58f49c9376`
  - `` `9a4bfdd26d627fda93b2a1e698db90b294b544da2a382ddeffd6f7d609a78ac8`
  - `` `cab6b442c0551fc953c04a3962dedfb6fd30956ae8646d92d7e11e2d7ffe3dfd`
  - `` `9c0d2eca1bd6550a93a0be0027d6fd17295d672ff947ecf4dae6b96ea73f2fcc`
  - `` `988d0118ff179109053f6a37b971cb49f426e38b0ddc8bdc1061ae12a4b598ca`
  - `` `6032e86732784667774b040b994727d5fc1514d51b42734cba725be5d3578d60`
  - `` `9aad5f2ab4b644ec25d02895c1f91d3d59410acce8fb170102cdca174167c35a`
  - `` `bef86c7f5d18deb5fab52d27cd1490c1c16e66142da37b0f90482e5a8b23a3d2`
  - `` `416bae78d06433274814b809d84c7a91eba9c4ab0ccefb933d9680abc78e6008`
  - `` `3ec96b4983c06657895b170b7bf2feb1320f252dce468165672ac7ede6712979`
- `gt.stone.` -> `gt6/textures/block/stones/kimberlite/`:
  - `` `52f36c42d42d349e74196bedaf281978620da51bb4cfee3c00f0bc9c54852035`
  - `` `c71b219a69a09a42ec06fb9446a5072803dba2c018b4a6514ab1894ac68c520f`
  - `` `be97ce4156786df10da98c1ef190871c84cd9656594018e63650ca94203ef767`
  - `` `500c2f58c49b091ef178e15821959105cbfa66733886babf2317228e3c1cd886`
  - `` `29f774277e23ea34e5eb3c2f6d4f40e5a9626d0d8065d8394beedd659a12afb1`
  - `` `d876cc6b68cd08f9347557411b0ac5a920839bac4ab7dc44dd8fa206768908c5`
  - `` `087da0145a0949cedb1e6051f3bbdf57d029ac12789d44afa2684c1d0f8bd1fe`
  - `` `bbc09d7b4caf480ffc7f7ac0ef71a9372c4fa265a64a48cb65089ef6532d3961`
  - `` `fb12500ad463d2be86b0e96f37e9cb3b5a57390985262c5ee53f34df64e27e55`
  - `` `500c2f58c49b091ef178e15821959105cbfa66733886babf2317228e3c1cd886`
  - `` `d4d13ef790a46ca8473d2483d401783f2a116cb7988101455781b29a9f8dabfd`
  - `` `0ffc87e00057c435f436db72f60c091a0af596f6ce793a1ba440ac26a6bd2d6e`
  - `` `e1f5673ab75cd5763d21de74987729b5f62d526fbe39b476754a04b387ebb5a7`
  - `` `3b319075b53bf796753f01405e1697277a79ebf43633b7a573196afacedc4fa5`
  - `` `6f7ab81a4f383c1458138db976557fe0ff9107a8922a1b472c757fc65516f0d2`
  - `` `61522a8cd9534c79ff3c2acd856f976d267df54780526546224e17f2a3482110`
- `gt.stone.` -> `gt6/textures/block/stones/quartzite/`:
  - `` `17fe39389031675a50abcc1558abc219392f7a31cc302d115084db4367ff0816`
  - `` `3bdbe7599e23c599c32bce6d129be4e33c90a5ac95aaf495526f68abd9ea9c56`
  - `` `d6b9cd837dc8ebe66c8b506525d9fab5cdf0cab80ec051230f9fab64a0da3424`
  - `` `db19ed8ecbf686b9bbb621a1533606f4f63dd53bbbce729005ea2b36f9fa3860`
  - `` `1c2048ffc962a5aa2841d9c11a4198dc8750074dc0719211ffbbd74643e71309`
  - `` `d0f8d28cfea35824443f2006ff3d2743cfcf57d8007f365900dc4198becbea41`
  - `` `5234e07d643cea0504c530113924fdfa3609bd83f3628e0c802810402626b8c9`
  - `` `79046bcaddbef4edab84a7657b6f93f6c9b591c323546f8ac53794077ea97f3a`
  - `` `6dc8856330f18019f715fd32e2d2f6c334c2249d1f97a5c1a6101afea2623249`
  - `` `db19ed8ecbf686b9bbb621a1533606f4f63dd53bbbce729005ea2b36f9fa3860`
  - `` `2a9245e1505aa644324dbbfa6fe6d95d369e04f460e74ef5361cf0d18840e52c`
  - `` `97a5178503377d2f6b9d656d096e4957f7f191690013ff525e5336607ccb3401`
  - `` `c1c64ffdc13288b235e1d118641337051c961baf1b21b70d8bd4b621ac44e83b`
  - `` `ccffd2b5da643162b1c0a4586c04c1f160d991371b7e344849b5c6d69fe2b7d1`
  - `` `6cb2db30e96243d5045be4eb3761a61e318211be3a19fac22bf4d424465dc294`
  - `` `e408ca32e75e66c5a014c6d09236ff01ba0a11b28a7d19afeb87a6d843780184`
- `gt.stone.` -> `gt6/textures/block/stones/prismarine_light/`:
  - `` `e53bb0c5cf348201551c8cd1495970f490fe89b5eedc2805810084bdcbb833b4`
  - `` `a4e5b52554a3ae56f4260593e28d0ce99fa1b5f462b1f7c2bbfe0fb487c03c5a`
  - `` `c41a1e07a389d6eeaf5711bec717be61710830241b1488a10fe4d905f9087d7f`
  - `` `79da20bfe79a47c8f39fcf311e6bb76a375b91577595199eb795e5bea903d200`
  - `` `96029695441ac8fe4319998661bb14f790c29dedfc35287dce1fd60542f9f9b5`
  - `` `37b24eb5e6f0ee1f9c48b7ca963af2d6574b8718494dbc815305fe0550ffdd38`
  - `` `2db987c5f3c7ec65ca8922cc065557ac35a6f13ef657961896f0dd285ec941f6`
  - `` `f5e34c6eb03c5cc742089d77adc8e746d2d3e9ce68404c9dedbccd80710dfaa2`
  - `` `8ceb3e7fc8277f34c605ab8f98490bed957a6279d0e7f9350ba8a6051ab1c55f`
  - `` `cbd0f06b1f6500fb3f0c0bc2b017d8ebe7a602edc9defddbca39c3678c86a209`
  - `` `a59171f75f436887f0ad2ff3de88d6cbded148440cae000df2a4e7d98d59fc93`
  - `` `7fee7e08b0d071e8a21258155904077ffdd73c91d2d333d4cb9ae3bd031e4c93`
  - `` `942053791d6c0e60e1aa57651bddf40485be75bfec77df64f1045f8bb5cecce8`
  - `` `4b2a033d87229921a70ea0bedfefcb44a5f4bf37189360f26ac74a8639c90817`
  - `` `315d91c78db831e6d7b0f237c0767dfe2f986a1bcc93e45de989fa02ac353e92`
  - `` `da38c9413e6c70a8d694f8d27684f2c4bb6c751b092575bc851e0e74a5725f5d`
- `gt.stone.` -> `gt6/textures/block/stones/prismarine_dark/`:
  - `` `c5c1242c6121985798e0b46d1f575776b99ab0632927262ba2fa999cf970557b`
  - `` `9325a48042cf0f99ded696d0777f70a2f40eac3b057722cce45224cf18e6bb5b`
  - `` `0b9ae299ce2475b9edcf67a8edad1e94e8ad42e3436db60253f4d10d7a946c03`
  - `` `73c463cba1d032b716e2a13150eb81d93ab92ea3224457538df941b49105d7c8`
  - `` `874588e5f319c3ff3d41b1cbfe78c6ede5455d6b771db757a63bb33560b109d5`
  - `` `490032f31a39e93ba05245fbea48a21a0b76dfdbac8c087d27a5d6e8c67f8b87`
  - `` `917cc9ee58a86d13826abf6bfb1f992256401de180bddeb1336139be6f535900`
  - `` `30853efd9df5918b094321826c18cefd296dd6567bca0fc7944847750cc6722d`
  - `` `0c5d7bac3fe696a76983ff291a3e872dc2311bc36d85539311a5240d5d38af35`
  - `` `73c463cba1d032b716e2a13150eb81d93ab92ea3224457538df941b49105d7c8`
  - `` `38f4a01b03666ccb3a5e67111443e93d226f1976259a62662661f19dfa3d1c38`
  - `` `88fb3f9f0b6ffd7243ce79aedeac98f930efa4e06740b096042ce8d3a4ca97f6`
  - `` `cfc0e66bc244dd403f90bb521a1555777bd5b19ed150f917022613223a7afa9f`
  - `` `e50d9f148e65ab31e7a0c5ee60be08d90d30537e4ffa369229b3c60840c67ccc`
  - `` `f142c98ec5c8cf8433d9008f95933d9fe15287401bc075926e228aa34b2c3e27`
  - `` `50998ebff69e12755c703a761923fb291c11f2d60b00ec8d11afa79a56971e5d`
- `gt.stone.` -> `gt6/textures/block/stones/slate/`:
  - `` `bf280e4bcdb5e92e297e0f81c925bd74227ea3d832f2ab772d00189edfcac943`
  - `` `0f538ab47d63aabfe4eeb8e88569bbe4fc51592bb85288f39206add558cbf299`
  - `` `f6baefc2aba3d8d4ca23a1ed88d2886eea5655c77a61cd67db553a24be46d127`
  - `` `34eca9b8671cbf1cf88b985dd486d46fbcdbfb60ab3838478f29425cc4dd5216`
  - `` `6158d7f6997ec1ce2cc2905cfd0187c7f4f9b8b7ae12af0e9f154ae0b372957e`
  - `` `28e0f954de3e7d17f800d405816ba557c358121a416b006362152360228275e6`
  - `` `397997fc85c70a9fb9d1bb9a3a1f1833b2f3020029409cf6ba586b88e6889554`
  - `` `cf828131376f7ed1d881870bb9d22c0b077ea52f821f664ff7b994fd04f29c1e`
  - `` `e6175fa627b194177d7f22c2291cd748a616bd76a933bf4d8ae308b66d3a6359`
  - `` `fdd4970c5554b2a080064cb465f469e0c19cd422db8b3079340d84fca274cf88`
  - `` `8b126aa19b4be5c90c53be303cf19f42887d7448d38da6f676e0f3bc014f3807`
  - `` `326eab6cf23baad3719affb6419989a881457dc7c048a66d6846e20f1a793b6d`
  - `` `af56f7c254edf62db16c5f424f0ef58b708882e9e5ea0403a14894c086a75adf`
  - `` `d35e8acf4e7753c58d102f20bec4c4af016590b7caf273b7efbc895ddd5029a1`
  - `` `2b6e8ebbd638bf84b654a7a2a5684942d8b19b350a9a11d0d8ad28efc24fe910`
  - `` `558b31db583b721699dc3ce0780323494e68d0c940fa8544fc044c4092d71e31`
- `gt.stone.` -> `gt6/textures/block/stones/shale/`:
  - `` `559d62f1cadfd2d8e9561f18c60c23608dcef954597a9f23c932d45c1596c93d`
  - `` `127bc2d95054a7a4ba30242a61d42801762711d247421252c8aa8374c2bb768f`
  - `` `b0dfd1e235d7c07ea394ceafa3d5b6d195f29c38562914532727f8ef10c57060`
  - `` `dda7a8465c878dc7b4c6c4cfdfd4692399ad12924ab62d6bd6cc2ee73451089e`
  - `` `842f958bcd22a20069300d8a96583cd76d309e708f0466b93b87ab066ae29e8a`
  - `` `3b6439fbdfd6dab88e935bcc463924064b094a5537777f90eca1f1b1f5c9fb8a`
  - `` `073f672eeef82a5c8fa9ba3ba97b66d17874fbf26d65cd35c4a354a464200889`
  - `` `1e6b853fc31b63ef6055fcf4b21536883d4a6ac3568cf77ea6eab45f6d1bc816`
  - `` `23a93ffd4830655c3db299c290ea9268a66d5362fc84f6212bb52ca808f11f41`
  - `` `f76f7ff2dc4f261dc14c705cf549d40c0f9f1e2c493787de757cf928ae3a5494`
  - `` `9b2f627f7c2af502040ed0109211fbab0b2b1b49deab682a34c46a951d4dcb7a`
  - `` `3016e87f4f2dfcc29856b203d9f048a2118daa17f91712df19d04f2808241818`
  - `` `48e86981e202e4227ea7aea3c24642e77d0c2d78f0d7f68d8dd1a1439017c075`
  - `` `0ad64029594db0e360325fb3666c52aa5d524facacf03f8bfc0d5b1ed0a33804`
  - `` `7bc82814a3fb35982779453555799daa7f7f46a930abb46fd6a133e58c6faf58`
  - `` `b7cfff5afad72b0359bd810564bf876f5930b3c525bfab5b6e2367640c858dc0`

GT6 stone-family block textures, task p19-stoneblocks-render: the 272 PNGs under
`gt6/textures/block/stones/<stone>/<variant>.png` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/stones/gt.stone.<name>/<VARIANT>.png`
(the BlockStones.java:93-108 16-icon table, one folder per stone —
Loader_Rocks.java:56-139, the 17-stone CS.java:1668 set the registry card landed),
byte-identical to upstream, sha256 verified per file (manifest below). The stone
folder name maps `gt.stone.<a.b>` -> `<a_b>` (dot sub-folders flatten to
underscores) and the icon segment lowercases (`BRICKS_CHISELED` ->
`bricks_chiseled`, the StoneVariant.snake serialization) per the 1.20.1
`ResourceLocation` charset — the same declared lowercase deviation as every borrow
above. These are the upstream DEDICATED per-variant color PNGs (not the
grayscale+tint route of the P8 materialicons): the datagen models carry NO
tintindex. Census 2026-09-06: 17 folders x 16 PNGs = 272 files, zero gaps, no
`.mcmeta` animations; 14 content-level duplicates exist WITHIN single stones
(`bricks.png` == `bricks_redstone.png` for 14 stones — upstream ships the
identical file for both variants), borrowed 1:1 anyway to keep the one-to-one
upstream path mapping the census pins.
Copied on 2026-09-06. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

- `gt.stone.granite.black` -> `gt6/textures/block/stones/granite_black/`:
  - `STONE.PNG` `765d48c31f362adf290d8059b127d70f233972ee417a6aaac383c153876d7fd7`
  - `COBBLE.PNG` `a614d26460f4ea7ea0abe3a4a0550137b60daf2889133535b8295e926fd5aab3`
  - `COBBLE_MOSSY.PNG` `c434af7a6b6f03b68a64f4f260a558b5d5827fb00e37a237a217b8030378a410`
  - `BRICKS.PNG` `e47399a3fd84551d3a561cf456bab049d8231f85d945f59264c27530c92dd585`
  - `BRICKS_CRACKED.PNG` `45e807352c9835c47f71969b7f1a63d55cc8bbcf013de61a65cb915c3755dba5`
  - `BRICKS_MOSSY.PNG` `29a1d3b11a3851e6862978aaea6e626bbcc0955ab90cc3f95558dd56a3699fa0`
  - `BRICKS_CHISELED.PNG` `1faf6934e019aa60ab52f74e1616e6d0f865b279a8e1fd10f858dc79ddce654e`
  - `SMOOTH.PNG` `045426439cf1760be9ef78919296a3ed9784fb28dabc570a3e16813417848378`
  - `BRICKS_REINFORCED.PNG` `c305f3ae8da0a522ee6aec0a7245cbe55e8ae3c8243100b8eb08fcd8a77af040`
  - `BRICKS_REDSTONE.PNG` `e47399a3fd84551d3a561cf456bab049d8231f85d945f59264c27530c92dd585`
  - `TILES.PNG` `008ca5fcf6cdad084ffd3115c9ebc8ce5884aab0437b8a70c57d90b486ddc2ef`
  - `SMALL_TILES.PNG` `e06eed0f58131eea68d030d743ec05be913ea177df9f8606ff6d632965cf62e9`
  - `SMALL_BRICKS.PNG` `76fce71f945a76a883a7bff69ececdfee8a77013a573f16c380fb8b5aeb2ec09`
  - `WINDMILL_TILES_A.PNG` `b6d1b35e5d063b83eceae4737ff3b9e4f38260b37d1047598017c9a5012091b2`
  - `WINDMILL_TILES_B.PNG` `dbc18d3f78db5cbfb6b9005724240bb7238c2f313b77ec541742b6579fd76c5f`
  - `SQUARE_BRICKS.PNG` `a81865cdd1da1daa33b4c4f87f3d2a339a4561f05a91697a4af459ed6a4fa064`
- `gt.stone.granite.red` -> `gt6/textures/block/stones/granite_red/`:
  - `STONE.PNG` `7b3b20a1907f03ba29f76b2dd7b6455e6a294fb36f78841c3564af4cd37f27ca`
  - `COBBLE.PNG` `e036bc8a5f424d1627cf70d729ea36144b081bd0deebef57512a00b4c3ae61c1`
  - `COBBLE_MOSSY.PNG` `382c60f5bd4e4d42239d932bf15ad2b481caf540b76b71aacb59429c7bde5588`
  - `BRICKS.PNG` `a5a0e52972e3f87e4560cf244fceab5d0a96c9cf8d62a21acaa8bfd4cda1a0de`
  - `BRICKS_CRACKED.PNG` `51724e0ca2683141ce781d38d609f317174bee694f430bc95454cdba98f69461`
  - `BRICKS_MOSSY.PNG` `9e52ec17a0267172693c44f2b680c8a0a5c84b468da598ef6e25023b96a4e2b1`
  - `BRICKS_CHISELED.PNG` `1e86462c8be3f63f9ebfd5c61ecffa922772ad6dff9452c8d9ba45a05c698b39`
  - `SMOOTH.PNG` `fb010736381e25754fb4d9872c1dd493c79e52f54a5972bc98918aa766a336b7`
  - `BRICKS_REINFORCED.PNG` `604cba1623c7d4b8e5ac84c56f89713b0e166d6e2539beff31c55204279311ff`
  - `BRICKS_REDSTONE.PNG` `a5a0e52972e3f87e4560cf244fceab5d0a96c9cf8d62a21acaa8bfd4cda1a0de`
  - `TILES.PNG` `f1a12da2c24de2bafa4a9ff356e77cc09e74ee160391b5817faa323665749c53`
  - `SMALL_TILES.PNG` `6240292390576fa63a97635f0fa18ee52ada32a2edf2cecc8fa316cf00b3ace1`
  - `SMALL_BRICKS.PNG` `caf668a9a341288cda68e011cbb1cb616d71bf5ffede3f3ef273ebd5131db76b`
  - `WINDMILL_TILES_A.PNG` `0e770dc2d88afa3db6b07875ff2cc97dd0c726a11802aff526ab33e9cb69b293`
  - `WINDMILL_TILES_B.PNG` `c65def2bb0770aeee7dc938af51218367047d5f8486ddb53274ce16465aaf49b`
  - `SQUARE_BRICKS.PNG` `5b16e56f7e89d5cc313677433abcb32e06075ccb590c39347354da14595bf4a7`
- `gt.stone.basalt` -> `gt6/textures/block/stones/basalt/`:
  - `STONE.PNG` `4d993a27bd065ed71babe249405dc43609b02feb24cf7843510ce3e128287462`
  - `COBBLE.PNG` `7527c356f4d456d430ceb373e73cd79610da05951b243c6d006fcea18029c072`
  - `COBBLE_MOSSY.PNG` `81f99264eea8fb40df664f4286bc15f16292ad3d76d7cacb8f19b3af0a590f4d`
  - `BRICKS.PNG` `c41e08bbe277145a514a32f0371947fcb3760e1bb1c411c69e90c446e54423df`
  - `BRICKS_CRACKED.PNG` `df3298e2a1ece7940698aa3416e13a36f5ab8f8bb4d78072c1d509201d884ef3`
  - `BRICKS_MOSSY.PNG` `29eefb37a45da1ea10622b95221ffdc059c345efcccd16ab1523725eea5e7b17`
  - `BRICKS_CHISELED.PNG` `176cb5afd7d9989b256f344e8e21776ed1697d176c774175663625633fc98fab`
  - `SMOOTH.PNG` `795fd85ec0782d96ff7645a475bed72ecc5a84c6bd187864a3472480cb4273db`
  - `BRICKS_REINFORCED.PNG` `6d717e0bc41f47f5f45d4bfb3f5859ce259870d2e762b534ba0a480dc61ad4ce`
  - `BRICKS_REDSTONE.PNG` `c41e08bbe277145a514a32f0371947fcb3760e1bb1c411c69e90c446e54423df`
  - `TILES.PNG` `70014ce5f2635da10fbf31f233668f4a1436ac7e740362ed884cb7f75a3fa01a`
  - `SMALL_TILES.PNG` `7d1abb7d7e004a0671133d8c6f0a7e8a6fc4276681108e86f06978c3807f8c7b`
  - `SMALL_BRICKS.PNG` `42cce79a6cd6028385f836f17532f6eeef2264a6944501a93b0ee9403ed3528e`
  - `WINDMILL_TILES_A.PNG` `f3b3c7d2f21263e47179ae00cab0e2b39c3ac5854f843261f35f574c74a7818c`
  - `WINDMILL_TILES_B.PNG` `2f523462a7b18265f17a53dabc3594bf4cb1ff13241536de218fb8c3dee78a8d`
  - `SQUARE_BRICKS.PNG` `ec86de1f1bcd38ce91b64f8248e51074dcd99b9eea65b2e5aae2318c2c89a894`
- `gt.stone.marble` -> `gt6/textures/block/stones/marble/`:
  - `STONE.PNG` `148be8d82038f33601a5a8bd80383d979e50467303a02902536337918e72a907`
  - `COBBLE.PNG` `46f62629be41e93de516614b0f880fd405979a5d1a5eee9368c2e2c702706327`
  - `COBBLE_MOSSY.PNG` `00b1f8152025faf1f569a8559c099e83676f6b47c1744bdbdec2e3ab1b018bf2`
  - `BRICKS.PNG` `52ec285b0739d28a1d8cb00391e5bfddeec576a0fca22ea7a0d6509ab41cac8e`
  - `BRICKS_CRACKED.PNG` `6d380756a571a2eaf1b7ceaa084c02502ed18770d63e4c6098d096feb5980192`
  - `BRICKS_MOSSY.PNG` `7ad2faa19a6c62dc367610ac0a6dc73adc55f60d519b149833fa46be272822fb`
  - `BRICKS_CHISELED.PNG` `f57d734395f192522125e1333346d45310c594cba742997e968dff47ea44a3c4`
  - `SMOOTH.PNG` `62c4f63d491da95fcb5bfba53462f5ecddc5a100738ae0867b0364a460ac3b69`
  - `BRICKS_REINFORCED.PNG` `62bb919d5f47f8d93907915785d52403d092e3a65f34cc9d54c6d26a92033e9d`
  - `BRICKS_REDSTONE.PNG` `52ec285b0739d28a1d8cb00391e5bfddeec576a0fca22ea7a0d6509ab41cac8e`
  - `TILES.PNG` `e37c45de56dd0e8fd3f5c3aebc05efb0dc5e728c2798db989ffb73036995eb70`
  - `SMALL_TILES.PNG` `1267f2a0e668cd0cddb69727ca463bd61a0f882129fbd802504f3ea7693ef699`
  - `SMALL_BRICKS.PNG` `a8049413f6d5386da3508349eed1939aa334c06f87cd9ef7185634b7e4e5ddc8`
  - `WINDMILL_TILES_A.PNG` `d6270ecb53fcd592abe8a4a782ef0c6c485c6a8d75f42ddcf2bbe56e9001f7c6`
  - `WINDMILL_TILES_B.PNG` `6ed82a902561af30ecdcf518da1bfc8c5eb38abfbdab859ebf3949d6dfe6d4f0`
  - `SQUARE_BRICKS.PNG` `b6be9e3cf4e4dd421034071f8183f65c2bce1394d76a28d1ec89c35499d8af6c`
- `gt.stone.limestone` -> `gt6/textures/block/stones/limestone/`:
  - `STONE.PNG` `a9c2ce7b7d76c21eb61875703f4b2f8f2347546a4ea903d7dd7b9f7bbbb7e4f0`
  - `COBBLE.PNG` `4223677ddc53692dd4944eafe14dc7abe89a01df0ca6a50eeb654713d4cd3425`
  - `COBBLE_MOSSY.PNG` `b33af7ed9660d25a65e04eb24fa16b0dd903d868ddac1ea11d16b74dc0c83b11`
  - `BRICKS.PNG` `7d0f375f23a2d2700da05cba18c1f7f4bd01ee9c016660010237b1a994aa51ae`
  - `BRICKS_CRACKED.PNG` `4bb9f1bf6ad01654ef237f73960b4d189b9ee34ad0f6457a7bf41307ca7cc214`
  - `BRICKS_MOSSY.PNG` `40be9607ef251b98eaffdb358c8a040f8675141674b95f83827ade9f80018ac2`
  - `BRICKS_CHISELED.PNG` `3afd20555bd92053dc60e2dec27be01cfecd579c8cadea28ed175c9443683626`
  - `SMOOTH.PNG` `7bec658ab9fd7f57f2a2ea7529c58e115dc1bc70cdb1cebfb93e58234e81309d`
  - `BRICKS_REINFORCED.PNG` `b64544e99eb348ca45e9139c5fb41ac31cdd11959af9cc4e5490326896e80d37`
  - `BRICKS_REDSTONE.PNG` `7d0f375f23a2d2700da05cba18c1f7f4bd01ee9c016660010237b1a994aa51ae`
  - `TILES.PNG` `9727f48eb48321586e331b82e807221e45a899349cfd452476f3d276f6d786da`
  - `SMALL_TILES.PNG` `dd9c50f14c246c359a79acb4c00aeb1f9c57d8fa2617173f8f1ba9ae8140cc76`
  - `SMALL_BRICKS.PNG` `b776180c5fbc465576bba9405293c87a407508102618e216199e1be53edffd6e`
  - `WINDMILL_TILES_A.PNG` `acb74aad858efc034b8a7475d638b05b2a2aab6535ad6f59fb0a88ddbd63313e`
  - `WINDMILL_TILES_B.PNG` `b981c19ef4dfb46de306db933f3069a984e233adea9f2bdecd8a2b8bcfeb50f5`
  - `SQUARE_BRICKS.PNG` `32337a355fbcff7c6dc870bd18849c3e6068925fc2572214e71df9c5525aef92`
- `gt.stone.granite` -> `gt6/textures/block/stones/granite/`:
  - `STONE.PNG` `6b725510dcfd435b9eec1f90b23ca93dc438850c4c055583f055a21c61455f30`
  - `COBBLE.PNG` `ebba3397e583246d3f61d7dd69530dd1417952ebdfe32c3aec8b8d11fe8ca6dc`
  - `COBBLE_MOSSY.PNG` `04e512b15b77538747b1717adb3a33f50c84d1a32769e9dc65ee2e6dfff71adf`
  - `BRICKS.PNG` `007003029468d88e9ec2f2ce9445a935cf59482c642a51ff09a0511f067634be`
  - `BRICKS_CRACKED.PNG` `b122d45d805748096cf3067f4d59f4222c90ba532490086a747ec820f87e8ea3`
  - `BRICKS_MOSSY.PNG` `f21e6c3de3196907e70b9aafd9d1d65113b701c83d72a4620a960ea56449a41a`
  - `BRICKS_CHISELED.PNG` `8c8973b782229c65559e4866fca02a36bb3708b4ba71d407dd33f336583c2fc7`
  - `SMOOTH.PNG` `f89df47749ea16885e4a5068edd22658623e1ccd6db7f341f7e1f31d1fbf18fe`
  - `BRICKS_REINFORCED.PNG` `1c5f1632d7385ec5b9ec678bb5c92c0d3b3c00eaff3222f40d0111cedf458ebf`
  - `BRICKS_REDSTONE.PNG` `007003029468d88e9ec2f2ce9445a935cf59482c642a51ff09a0511f067634be`
  - `TILES.PNG` `f181353b57b8550150ec9f97f4594cec5e7b7ff3db03e333a99c627d09dd72ea`
  - `SMALL_TILES.PNG` `613d9e3ef6a721b95a7a3b6580d6f3280dfbeaca000bb182559a5948e5defdf1`
  - `SMALL_BRICKS.PNG` `a491a1b7e32312aab11f3cef4edc8d4bda4fbec95d00839d0afaf812e447b1a0`
  - `WINDMILL_TILES_A.PNG` `1fd4be87cfaa7739adfcf3fd22ab5bf4e860aafe728882207e45ea15ced54716`
  - `WINDMILL_TILES_B.PNG` `00876ec1e813c13785cd42f44233621a2ed0d6056c8bd2aca0660b2a8a45d4de`
  - `SQUARE_BRICKS.PNG` `2474335e645f302928db6a602a636cd4bc55d03466c3f806b3900e8af7f942f5`
- `gt.stone.diorite` -> `gt6/textures/block/stones/diorite/`:
  - `STONE.PNG` `7a93dcd388fef5759b6e6fee82c28fda816463aeee0430963555ea7735bf5d61`
  - `COBBLE.PNG` `6c652328ded8921f2bf41c808f0b62e1f12e8e7085b5c4b1c4c815f2d118b6c8`
  - `COBBLE_MOSSY.PNG` `cafb53a8c3eb7c97b7682104e4b9c21370651a7e4b50d66603e5b7f09563b706`
  - `BRICKS.PNG` `0160de770b302274c2ac10bdbc8bbaf64dfcd7281eeeeb88e8909e1528e2a8ec`
  - `BRICKS_CRACKED.PNG` `f272889f0070ff253be545a2e64e43901b3699cff906e6eb6a232cc3277e9e4f`
  - `BRICKS_MOSSY.PNG` `f92f8ad270d444ef7c650057529c0b4440cc4648e5af1c8357fb8e919365195e`
  - `BRICKS_CHISELED.PNG` `64138751dbddfb7217aba8b0cf91ffab56ec81939781612e46e6e181688188a4`
  - `SMOOTH.PNG` `7cf435a2835e4bad9b714a2e98838ec9b05a72a56407803cb195fbb779cd8097`
  - `BRICKS_REINFORCED.PNG` `4bb7fa469985a0154761aeeff35be0ab1d006f49f7e6d7fb7ab7877f79bd8db5`
  - `BRICKS_REDSTONE.PNG` `0160de770b302274c2ac10bdbc8bbaf64dfcd7281eeeeb88e8909e1528e2a8ec`
  - `TILES.PNG` `665ee9329f2fad809ab070ef4e53806c1c67cfd60f857358d9c73949656b5dd3`
  - `SMALL_TILES.PNG` `ecac9818a8d8c473deb49dfad90e5e565daead36cf96b81b8254f8e225548036`
  - `SMALL_BRICKS.PNG` `e59d937136016ec4d967dd0186bf87aa38824ab8c7d6552f16b77aa81554f78d`
  - `WINDMILL_TILES_A.PNG` `d015278afeeffeb431dca89c7b05c44b286e6c3e367a8fc60b616c663f713aff`
  - `WINDMILL_TILES_B.PNG` `56413380b779527bbfb9fe70bb6c97af2f5e7604d3310f6f4f4874daf089bd27`
  - `SQUARE_BRICKS.PNG` `3e67412faec43c4dd1bad64314686ebc1946a062c9092c4e0682f00eb571a9d8`
- `gt.stone.andesite` -> `gt6/textures/block/stones/andesite/`:
  - `STONE.PNG` `8f297386f7f2992fda88e2ee2236a3c36cbaab475bbcf64397ef447a01426a2d`
  - `COBBLE.PNG` `b305aa1c2e8556ea2ac4ab5fc7d42d4abf9e179910dd9f76cca87035575f2321`
  - `COBBLE_MOSSY.PNG` `abecf45c6d9d1c4bac56e534f83bf77a5e13ffa639398a96268ceba6e9fc492a`
  - `BRICKS.PNG` `4c0eb7836db2423db08a5f3cc7813e4dd0b5ba23c4d5ed90dcd2b318bb382bb4`
  - `BRICKS_CRACKED.PNG` `b4d70ed0082eaa371b4fa844875fa468e509767e58e77e90c9c283fbbaf875c9`
  - `BRICKS_MOSSY.PNG` `f06b9d7e4bd453aedb8ce235952c7c49eeabf2caf25e1b950719561ab172a2a6`
  - `BRICKS_CHISELED.PNG` `8619435f93b5c968f7a063304992e190b73e835fb21fa189f42bfa7ed9c42e0d`
  - `SMOOTH.PNG` `8c7b2100eedff1208d9be30c8382b9a2b69824b541bccc0310d1dc096c5e4e54`
  - `BRICKS_REINFORCED.PNG` `c28fbe4c63d22cbdeaf45deaac7f4eb17f7c161540bfa48819cab5f3f5a609d3`
  - `BRICKS_REDSTONE.PNG` `4c0eb7836db2423db08a5f3cc7813e4dd0b5ba23c4d5ed90dcd2b318bb382bb4`
  - `TILES.PNG` `d7b521a00125caa6d21a273a798418f68011b88223140550dcad65cebcc08e45`
  - `SMALL_TILES.PNG` `50c434eeba77bfc028d22997e328334b0d731b2045152aa89ae17930113d7ba1`
  - `SMALL_BRICKS.PNG` `afb8b424e4aa321b4471b4fa5ad5f1b9f79edf5635b7964488c746003af40a2a`
  - `WINDMILL_TILES_A.PNG` `9d73fd7edb4e0bb450c6df892c0d1707e99cb3d8a624f9f120a0152fa6d1d18e`
  - `WINDMILL_TILES_B.PNG` `d2a12b9a46554e2058679ce493220bd2f2d7c9c5ea8995f0c3dc7c3e2f6a4d07`
  - `SQUARE_BRICKS.PNG` `81fd2db0fe99ee098c05b644d44c4c5ea7f3eed21ec757dcd8d2ef95cdcf4f4f`
- `gt.stone.komatiite` -> `gt6/textures/block/stones/komatiite/`:
  - `STONE.PNG` `02bf1697f01ba2978938d53cad78ee96e6cb7eea9f5681420a314d3660f2be0f`
  - `COBBLE.PNG` `75f5f1f45969f880e81dd7f974967022dc7f1c3b2a0b357b6b15cc712f0b815a`
  - `COBBLE_MOSSY.PNG` `4a58033b620830adaddb8bacf8f78b0670d54ebb27ca35eebaa0aeb86ce7fcc5`
  - `BRICKS.PNG` `5c47f922e3ab552f5d562ddfb635266c466865a81eab24fcfe8c6b824fa00930`
  - `BRICKS_CRACKED.PNG` `5c24bf03f221db56e6f5753cd0b67b3d834202a2dca6746485a67e02591bd73c`
  - `BRICKS_MOSSY.PNG` `99f6dc9aa2313a7d36e877664971b25a559ff467b9b5f2b76c67d2150c223e1b`
  - `BRICKS_CHISELED.PNG` `161b89413a4043aa9e26769df1ec470b071475fa9677be34b2d35a5fd0b3441d`
  - `SMOOTH.PNG` `43ca3cb5c377d8ae83217764395a6dc826b7b378d6048f966d75ad3560095372`
  - `BRICKS_REINFORCED.PNG` `da66e8230c6d4557815c776e1da34c16dd2bec604130ae88c88d2b52c4d02df2`
  - `BRICKS_REDSTONE.PNG` `5c47f922e3ab552f5d562ddfb635266c466865a81eab24fcfe8c6b824fa00930`
  - `TILES.PNG` `229005e84f250706123ed3d8f986a022fae9e5804c83ed2178c851c60f6105c5`
  - `SMALL_TILES.PNG` `c3559fc368f19e6c44b4117f0d2b5f600f10441efc3934b83ca2869469a8b9ad`
  - `SMALL_BRICKS.PNG` `e574e4221d29d4d91c382175866942d6615e945aae8d7abaa0ebf1e11551a004`
  - `WINDMILL_TILES_A.PNG` `04110012c785f0a56ba5370e17f002c3dbb5fa59dca6d7a853d2afd44f4c8e95`
  - `WINDMILL_TILES_B.PNG` `4adc31ceb0e63c7e4a36d74403a42d6dfa7f5e74945511b27257a79c95781150`
  - `SQUARE_BRICKS.PNG` `b72cd3946f29223e8fad8998fe7f2777f77069b3f72c9097f06073d4c57f99a1`
- `gt.stone.greenschist` -> `gt6/textures/block/stones/greenschist/`:
  - `STONE.PNG` `7698c77866fb535711362bd00bf0faf892cba3de9b8b8eeb1ffbee8f9d17884e`
  - `COBBLE.PNG` `16760b190263d8e2d39ba16452023e4c20c26c43abad3f20e4a69a35dc8b30bf`
  - `COBBLE_MOSSY.PNG` `6a62deeb0cabd3daf8b26c1f96aedd5feed695c491914dfa85fddbe345fe3c9e`
  - `BRICKS.PNG` `2216533e6b8531ed41843d86abd87991a02a677a6177b6cb9ec3a40b4505d65a`
  - `BRICKS_CRACKED.PNG` `eeb86d3b62cb6b3c4e3aa947242699fdc6ce724ac37162a239dcf73d61b0ae5b`
  - `BRICKS_MOSSY.PNG` `feb3d1999f6920e9092f493ee8c9e661cd61cb37b47f1fb4bb56bf9907ebe2eb`
  - `BRICKS_CHISELED.PNG` `973f8cd56d1d1adb8ee4f85754adc6f956e0aa6fe53b072f0c6dd25bbf013228`
  - `SMOOTH.PNG` `183048010eb6ca47fa37dbd8a5916a751ab420ab909e1a9d64e55f163876fdfc`
  - `BRICKS_REINFORCED.PNG` `a95bc76dfd5bb854866fe796cba5b1d0d6d8ca76c34583978fb9100e1e256a57`
  - `BRICKS_REDSTONE.PNG` `2216533e6b8531ed41843d86abd87991a02a677a6177b6cb9ec3a40b4505d65a`
  - `TILES.PNG` `b6095a91fb6621b1ad97eb410363d27571d0a6b31b3f15b76a27007338098da5`
  - `SMALL_TILES.PNG` `ddc6ea5cb18a192953c78ddad128c0ec9dd734fa66560f107bf054b2e6aa7c78`
  - `SMALL_BRICKS.PNG` `6b2acee2582c29e73b7afb6a44d99add3da5d91a815ec7d2c2aef5776f5b5940`
  - `WINDMILL_TILES_A.PNG` `392517c845de338f0d49097ec6256e43aaaa03f3f959e5a845ddd7eb71147d9d`
  - `WINDMILL_TILES_B.PNG` `3a285e6ef21d26c69acd107df2e409fe945f7e47431b1cd23ecd6650a1f5ff55`
  - `SQUARE_BRICKS.PNG` `a8a17ca5c9437692601266c3844f0b6e6cd06e420bea6e1b5e2f390feaffebd0`
- `gt.stone.blueschist` -> `gt6/textures/block/stones/blueschist/`:
  - `STONE.PNG` `6646775562f171f738352794c8363a823c98c0ffd39798cc9fe526b375d8a54a`
  - `COBBLE.PNG` `3644d0a93165d3edbd115a6eb5b7757ffa1256c0fc4eff6f6daee91d1ce35080`
  - `COBBLE_MOSSY.PNG` `45a653fd9677f0725a13d0c93f475c5663b1c97ec844e0297447b443c0b2e206`
  - `BRICKS.PNG` `9c0d2eca1bd6550a93a0be0027d6fd17295d672ff947ecf4dae6b96ea73f2fcc`
  - `BRICKS_CRACKED.PNG` `e09b2776c482027f76ce61f7d252817da587d381d37f771f2aa774d902647881`
  - `BRICKS_MOSSY.PNG` `48a0107a3892f6ddc335eb9c599e749991dbd8a81d7600f8e7ab6fdea7b8507b`
  - `BRICKS_CHISELED.PNG` `e00025dcf5f12d8d9d4b4d91aa1b72914c2577c0bf98b2bb8863fb58f49c9376`
  - `SMOOTH.PNG` `9a4bfdd26d627fda93b2a1e698db90b294b544da2a382ddeffd6f7d609a78ac8`
  - `BRICKS_REINFORCED.PNG` `cab6b442c0551fc953c04a3962dedfb6fd30956ae8646d92d7e11e2d7ffe3dfd`
  - `BRICKS_REDSTONE.PNG` `9c0d2eca1bd6550a93a0be0027d6fd17295d672ff947ecf4dae6b96ea73f2fcc`
  - `TILES.PNG` `988d0118ff179109053f6a37b971cb49f426e38b0ddc8bdc1061ae12a4b598ca`
  - `SMALL_TILES.PNG` `6032e86732784667774b040b994727d5fc1514d51b42734cba725be5d3578d60`
  - `SMALL_BRICKS.PNG` `9aad5f2ab4b644ec25d02895c1f91d3d59410acce8fb170102cdca174167c35a`
  - `WINDMILL_TILES_A.PNG` `bef86c7f5d18deb5fab52d27cd1490c1c16e66142da37b0f90482e5a8b23a3d2`
  - `WINDMILL_TILES_B.PNG` `416bae78d06433274814b809d84c7a91eba9c4ab0ccefb933d9680abc78e6008`
  - `SQUARE_BRICKS.PNG` `3ec96b4983c06657895b170b7bf2feb1320f252dce468165672ac7ede6712979`
- `gt.stone.kimberlite` -> `gt6/textures/block/stones/kimberlite/`:
  - `STONE.PNG` `52f36c42d42d349e74196bedaf281978620da51bb4cfee3c00f0bc9c54852035`
  - `COBBLE.PNG` `c71b219a69a09a42ec06fb9446a5072803dba2c018b4a6514ab1894ac68c520f`
  - `COBBLE_MOSSY.PNG` `be97ce4156786df10da98c1ef190871c84cd9656594018e63650ca94203ef767`
  - `BRICKS.PNG` `500c2f58c49b091ef178e15821959105cbfa66733886babf2317228e3c1cd886`
  - `BRICKS_CRACKED.PNG` `29f774277e23ea34e5eb3c2f6d4f40e5a9626d0d8065d8394beedd659a12afb1`
  - `BRICKS_MOSSY.PNG` `d876cc6b68cd08f9347557411b0ac5a920839bac4ab7dc44dd8fa206768908c5`
  - `BRICKS_CHISELED.PNG` `087da0145a0949cedb1e6051f3bbdf57d029ac12789d44afa2684c1d0f8bd1fe`
  - `SMOOTH.PNG` `bbc09d7b4caf480ffc7f7ac0ef71a9372c4fa265a64a48cb65089ef6532d3961`
  - `BRICKS_REINFORCED.PNG` `fb12500ad463d2be86b0e96f37e9cb3b5a57390985262c5ee53f34df64e27e55`
  - `BRICKS_REDSTONE.PNG` `500c2f58c49b091ef178e15821959105cbfa66733886babf2317228e3c1cd886`
  - `TILES.PNG` `d4d13ef790a46ca8473d2483d401783f2a116cb7988101455781b29a9f8dabfd`
  - `SMALL_TILES.PNG` `0ffc87e00057c435f436db72f60c091a0af596f6ce793a1ba440ac26a6bd2d6e`
  - `SMALL_BRICKS.PNG` `e1f5673ab75cd5763d21de74987729b5f62d526fbe39b476754a04b387ebb5a7`
  - `WINDMILL_TILES_A.PNG` `3b319075b53bf796753f01405e1697277a79ebf43633b7a573196afacedc4fa5`
  - `WINDMILL_TILES_B.PNG` `6f7ab81a4f383c1458138db976557fe0ff9107a8922a1b472c757fc65516f0d2`
  - `SQUARE_BRICKS.PNG` `61522a8cd9534c79ff3c2acd856f976d267df54780526546224e17f2a3482110`
- `gt.stone.quartzite` -> `gt6/textures/block/stones/quartzite/`:
  - `STONE.PNG` `17fe39389031675a50abcc1558abc219392f7a31cc302d115084db4367ff0816`
  - `COBBLE.PNG` `3bdbe7599e23c599c32bce6d129be4e33c90a5ac95aaf495526f68abd9ea9c56`
  - `COBBLE_MOSSY.PNG` `d6b9cd837dc8ebe66c8b506525d9fab5cdf0cab80ec051230f9fab64a0da3424`
  - `BRICKS.PNG` `db19ed8ecbf686b9bbb621a1533606f4f63dd53bbbce729005ea2b36f9fa3860`
  - `BRICKS_CRACKED.PNG` `1c2048ffc962a5aa2841d9c11a4198dc8750074dc0719211ffbbd74643e71309`
  - `BRICKS_MOSSY.PNG` `d0f8d28cfea35824443f2006ff3d2743cfcf57d8007f365900dc4198becbea41`
  - `BRICKS_CHISELED.PNG` `5234e07d643cea0504c530113924fdfa3609bd83f3628e0c802810402626b8c9`
  - `SMOOTH.PNG` `79046bcaddbef4edab84a7657b6f93f6c9b591c323546f8ac53794077ea97f3a`
  - `BRICKS_REINFORCED.PNG` `6dc8856330f18019f715fd32e2d2f6c334c2249d1f97a5c1a6101afea2623249`
  - `BRICKS_REDSTONE.PNG` `db19ed8ecbf686b9bbb621a1533606f4f63dd53bbbce729005ea2b36f9fa3860`
  - `TILES.PNG` `2a9245e1505aa644324dbbfa6fe6d95d369e04f460e74ef5361cf0d18840e52c`
  - `SMALL_TILES.PNG` `97a5178503377d2f6b9d656d096e4957f7f191690013ff525e5336607ccb3401`
  - `SMALL_BRICKS.PNG` `c1c64ffdc13288b235e1d118641337051c961baf1b21b70d8bd4b621ac44e83b`
  - `WINDMILL_TILES_A.PNG` `ccffd2b5da643162b1c0a4586c04c1f160d991371b7e344849b5c6d69fe2b7d1`
  - `WINDMILL_TILES_B.PNG` `6cb2db30e96243d5045be4eb3761a61e318211be3a19fac22bf4d424465dc294`
  - `SQUARE_BRICKS.PNG` `e408ca32e75e66c5a014c6d09236ff01ba0a11b28a7d19afeb87a6d843780184`
- `gt.stone.prismarine.light` -> `gt6/textures/block/stones/prismarine_light/`:
  - `STONE.PNG` `e53bb0c5cf348201551c8cd1495970f490fe89b5eedc2805810084bdcbb833b4`
  - `COBBLE.PNG` `a4e5b52554a3ae56f4260593e28d0ce99fa1b5f462b1f7c2bbfe0fb487c03c5a`
  - `COBBLE_MOSSY.PNG` `c41a1e07a389d6eeaf5711bec717be61710830241b1488a10fe4d905f9087d7f`
  - `BRICKS.PNG` `79da20bfe79a47c8f39fcf311e6bb76a375b91577595199eb795e5bea903d200`
  - `BRICKS_CRACKED.PNG` `96029695441ac8fe4319998661bb14f790c29dedfc35287dce1fd60542f9f9b5`
  - `BRICKS_MOSSY.PNG` `37b24eb5e6f0ee1f9c48b7ca963af2d6574b8718494dbc815305fe0550ffdd38`
  - `BRICKS_CHISELED.PNG` `2db987c5f3c7ec65ca8922cc065557ac35a6f13ef657961896f0dd285ec941f6`
  - `SMOOTH.PNG` `f5e34c6eb03c5cc742089d77adc8e746d2d3e9ce68404c9dedbccd80710dfaa2`
  - `BRICKS_REINFORCED.PNG` `8ceb3e7fc8277f34c605ab8f98490bed957a6279d0e7f9350ba8a6051ab1c55f`
  - `BRICKS_REDSTONE.PNG` `cbd0f06b1f6500fb3f0c0bc2b017d8ebe7a602edc9defddbca39c3678c86a209`
  - `TILES.PNG` `a59171f75f436887f0ad2ff3de88d6cbded148440cae000df2a4e7d98d59fc93`
  - `SMALL_TILES.PNG` `7fee7e08b0d071e8a21258155904077ffdd73c91d2d333d4cb9ae3bd031e4c93`
  - `SMALL_BRICKS.PNG` `942053791d6c0e60e1aa57651bddf40485be75bfec77df64f1045f8bb5cecce8`
  - `WINDMILL_TILES_A.PNG` `4b2a033d87229921a70ea0bedfefcb44a5f4bf37189360f26ac74a8639c90817`
  - `WINDMILL_TILES_B.PNG` `315d91c78db831e6d7b0f237c0767dfe2f986a1bcc93e45de989fa02ac353e92`
  - `SQUARE_BRICKS.PNG` `da38c9413e6c70a8d694f8d27684f2c4bb6c751b092575bc851e0e74a5725f5d`
- `gt.stone.prismarine.dark` -> `gt6/textures/block/stones/prismarine_dark/`:
  - `STONE.PNG` `c5c1242c6121985798e0b46d1f575776b99ab0632927262ba2fa999cf970557b`
  - `COBBLE.PNG` `9325a48042cf0f99ded696d0777f70a2f40eac3b057722cce45224cf18e6bb5b`
  - `COBBLE_MOSSY.PNG` `0b9ae299ce2475b9edcf67a8edad1e94e8ad42e3436db60253f4d10d7a946c03`
  - `BRICKS.PNG` `73c463cba1d032b716e2a13150eb81d93ab92ea3224457538df941b49105d7c8`
  - `BRICKS_CRACKED.PNG` `874588e5f319c3ff3d41b1cbfe78c6ede5455d6b771db757a63bb33560b109d5`
  - `BRICKS_MOSSY.PNG` `490032f31a39e93ba05245fbea48a21a0b76dfdbac8c087d27a5d6e8c67f8b87`
  - `BRICKS_CHISELED.PNG` `917cc9ee58a86d13826abf6bfb1f992256401de180bddeb1336139be6f535900`
  - `SMOOTH.PNG` `30853efd9df5918b094321826c18cefd296dd6567bca0fc7944847750cc6722d`
  - `BRICKS_REINFORCED.PNG` `0c5d7bac3fe696a76983ff291a3e872dc2311bc36d85539311a5240d5d38af35`
  - `BRICKS_REDSTONE.PNG` `73c463cba1d032b716e2a13150eb81d93ab92ea3224457538df941b49105d7c8`
  - `TILES.PNG` `38f4a01b03666ccb3a5e67111443e93d226f1976259a62662661f19dfa3d1c38`
  - `SMALL_TILES.PNG` `88fb3f9f0b6ffd7243ce79aedeac98f930efa4e06740b096042ce8d3a4ca97f6`
  - `SMALL_BRICKS.PNG` `cfc0e66bc244dd403f90bb521a1555777bd5b19ed150f917022613223a7afa9f`
  - `WINDMILL_TILES_A.PNG` `e50d9f148e65ab31e7a0c5ee60be08d90d30537e4ffa369229b3c60840c67ccc`
  - `WINDMILL_TILES_B.PNG` `f142c98ec5c8cf8433d9008f95933d9fe15287401bc075926e228aa34b2c3e27`
  - `SQUARE_BRICKS.PNG` `50998ebff69e12755c703a761923fb291c11f2d60b00ec8d11afa79a56971e5d`
- `gt.stone.slate` -> `gt6/textures/block/stones/slate/`:
  - `STONE.PNG` `bf280e4bcdb5e92e297e0f81c925bd74227ea3d832f2ab772d00189edfcac943`
  - `COBBLE.PNG` `0f538ab47d63aabfe4eeb8e88569bbe4fc51592bb85288f39206add558cbf299`
  - `COBBLE_MOSSY.PNG` `f6baefc2aba3d8d4ca23a1ed88d2886eea5655c77a61cd67db553a24be46d127`
  - `BRICKS.PNG` `34eca9b8671cbf1cf88b985dd486d46fbcdbfb60ab3838478f29425cc4dd5216`
  - `BRICKS_CRACKED.PNG` `6158d7f6997ec1ce2cc2905cfd0187c7f4f9b8b7ae12af0e9f154ae0b372957e`
  - `BRICKS_MOSSY.PNG` `28e0f954de3e7d17f800d405816ba557c358121a416b006362152360228275e6`
  - `BRICKS_CHISELED.PNG` `397997fc85c70a9fb9d1bb9a3a1f1833b2f3020029409cf6ba586b88e6889554`
  - `SMOOTH.PNG` `cf828131376f7ed1d881870bb9d22c0b077ea52f821f664ff7b994fd04f29c1e`
  - `BRICKS_REINFORCED.PNG` `e6175fa627b194177d7f22c2291cd748a616bd76a933bf4d8ae308b66d3a6359`
  - `BRICKS_REDSTONE.PNG` `fdd4970c5554b2a080064cb465f469e0c19cd422db8b3079340d84fca274cf88`
  - `TILES.PNG` `8b126aa19b4be5c90c53be303cf19f42887d7448d38da6f676e0f3bc014f3807`
  - `SMALL_TILES.PNG` `326eab6cf23baad3719affb6419989a881457dc7c048a66d6846e20f1a793b6d`
  - `SMALL_BRICKS.PNG` `af56f7c254edf62db16c5f424f0ef58b708882e9e5ea0403a14894c086a75adf`
  - `WINDMILL_TILES_A.PNG` `d35e8acf4e7753c58d102f20bec4c4af016590b7caf273b7efbc895ddd5029a1`
  - `WINDMILL_TILES_B.PNG` `2b6e8ebbd638bf84b654a7a2a5684942d8b19b350a9a11d0d8ad28efc24fe910`
  - `SQUARE_BRICKS.PNG` `558b31db583b721699dc3ce0780323494e68d0c940fa8544fc044c4092d71e31`
- `gt.stone.shale` -> `gt6/textures/block/stones/shale/`:
  - `STONE.PNG` `559d62f1cadfd2d8e9561f18c60c23608dcef954597a9f23c932d45c1596c93d`
  - `COBBLE.PNG` `127bc2d95054a7a4ba30242a61d42801762711d247421252c8aa8374c2bb768f`
  - `COBBLE_MOSSY.PNG` `b0dfd1e235d7c07ea394ceafa3d5b6d195f29c38562914532727f8ef10c57060`
  - `BRICKS.PNG` `dda7a8465c878dc7b4c6c4cfdfd4692399ad12924ab62d6bd6cc2ee73451089e`
  - `BRICKS_CRACKED.PNG` `842f958bcd22a20069300d8a96583cd76d309e708f0466b93b87ab066ae29e8a`
  - `BRICKS_MOSSY.PNG` `3b6439fbdfd6dab88e935bcc463924064b094a5537777f90eca1f1b1f5c9fb8a`
  - `BRICKS_CHISELED.PNG` `073f672eeef82a5c8fa9ba3ba97b66d17874fbf26d65cd35c4a354a464200889`
  - `SMOOTH.PNG` `1e6b853fc31b63ef6055fcf4b21536883d4a6ac3568cf77ea6eab45f6d1bc816`
  - `BRICKS_REINFORCED.PNG` `23a93ffd4830655c3db299c290ea9268a66d5362fc84f6212bb52ca808f11f41`
  - `BRICKS_REDSTONE.PNG` `f76f7ff2dc4f261dc14c705cf549d40c0f9f1e2c493787de757cf928ae3a5494`
  - `TILES.PNG` `9b2f627f7c2af502040ed0109211fbab0b2b1b49deab682a34c46a951d4dcb7a`
  - `SMALL_TILES.PNG` `3016e87f4f2dfcc29856b203d9f048a2118daa17f91712df19d04f2808241818`
  - `SMALL_BRICKS.PNG` `48e86981e202e4227ea7aea3c24642e77d0c2d78f0d7f68d8dd1a1439017c075`
  - `WINDMILL_TILES_A.PNG` `0ad64029594db0e360325fb3666c52aa5d524facacf03f8bfc0d5b1ed0a33804`
  - `WINDMILL_TILES_B.PNG` `7bc82814a3fb35982779453555799daa7f7f46a930abb46fd6a133e58c6faf58`
  - `SQUARE_BRICKS.PNG` `b7cfff5afad72b0359bd810564bf876f5930b3c525bfab5b6e2367640c858dc0`

Machine front + shared body textures, task p20-borrow-machine-fronts (the
`<family>_front{,_active,_running}.png` COMPOSITES are RETIRED by task
p22-paint-front-overlay-split — see the split-borrow section below; the
`oven_{bottom,top,side}.png` body borrows remain ACTIVE): the 21
PNGs under `gt6/textures/block/` (`<family>_front{,_active,_running}.png` for
the six families the model enumeration carries — oven, shredder, crusher,
lathe, dryer, distillery (GT6BlockStates.java:85-140; the T2-T4 ladder rows
share the T1 set, zero extra PNGs) — plus the shared body key set
`oven_{bottom,top,side}.png` the cube models hard-code, :303-308) are BAKED /
borrowed from the upstream family iconsets
`src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/<family>/`,
CC0 1.0 per the upstream `README.md` block above. The P19 bake recipe
(`bake_distillery_fronts.py`, distillery section above) is generalized by
`mdk/tools/bake_machine_fronts.py` (deterministic, idempotent, `--machine
group/name`); the re-run over distillery reproduces the committed P19 products
BYTE-IDENTICALLY (sha256-verified against the digests below/above), the
new-vs-old pipeline acceptance gate.

Recipe per family (src-over compositing, output alpha 255, FRAME 0 of any
animation strip — census 2026-09-06: strips are 16xN, N/16 ∈ {4, 6, 8} where
stripped, NOT just the P19 "16x64 four-frame" distillery case):

- `<family>_front.png`         = `colored/front` + `overlay/front`
- `<family>_front_active.png`  = `colored/front` + `overlay_active/front` frame 0
- `<family>_front_running.png` = `colored/front` + `overlay_running/front` frame 0

The colored base is the SAME opaque grayscale PNG in all six families and in
the oven body faces (`colored/{front,bottom,top,left,right,back}.png` are all
byte-identical to each other, the generic machine plate):

- `colored/front.png` (all 6 families) and `colored/{bottom,top,left}.png`
  (oven body sources) `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`

Overlay source digests per family (`overlay/front`, `overlay_active/front`,
`overlay_running/front`):

- oven        `3da4d0f7d578f6e08102a0b7615160035997d58affc26c7d13813b678c4bd7fd`
              `fb7896fcaf05092c8ce62895b940d779af1d73b5bf9d6d912f0173e9c8b40e36` (16x128, 8 frames)
              `cd5857690ac01a9fc10e3aadd4fe3afe3c2372d685ef0bcf14b680756b92b85e`
- shredder    `92aba8f65d8955a5eddb687e3852d68457409be2d540983903fe8f17ef6708c8`
              `8592fa60fdc687e7585ff8e900f1c6ada9798219172296c12e03fd5389973d99` (16x64, 4 frames)
              `e8b1c5126229ad9c05a2ddd03cefa6093f1767133f6181ad3ebbad48ce775bbb` (16x64, 4 frames)
- crusher     `8306618d4de778652178fb8761174baeebbefb8bae4c80b61598618af6ae7fd0`
              `84e4deec9a2653627feb48946343b839d73709d8614f57adf1d3a19001ae12c3` (16x128, 8 frames)
              `8b1448355e209cb60c894b42b564302bd12b5f960738c89eb022fca01667694b` (16x128, 8 frames)
- lathe       `71134a5929e77c46002534aed767578e250381c981454583759a4ba1e57f9132`
              `4a47271828abd1910a30fe4863fdd0c2180a8099aa48672bbd0688934a4ec6c` (16x96, 6 frames)
              `15d955d59a6d1a33a75203672f977b709f3081c2febe16910bad58fa97e085c9` (16x96, 6 frames)
- dryer       `5b17e8425cccdec0932990c7a31db6ef1a7d4ae96b6905ed1ca268d2cfb54a5f`
              `bcbdd663b5fe66ea453245f93a89f7987066b42185d43d602dd738f303d83ee3` (16x128, 8 frames)
              `b37a675b2e2ed105118a1b3de15332dc92459da03084b2310ce5d7efc7b049a0`
- distillery  `a36ecca013dbd36ddc02f10d2ce0ec640030147ddb3c180c299fac67ad57e4d3`
              `822b52fc5f647891438ad6467c58f05135946e6272b4e3826ccd9ffa7ce07ab6` (16x64, 4 frames)
              `88b5c302301a4773e5b880878f9423a79d3e12c2ac05dbb2a3c31dd84c961a57`

Baked product digests (16x16, fully opaque) — RETIRED products, REMOVED from
the tree by task p22-paint-front-overlay-split (kept as the historical audit
trail of the P19/P20 bake; no model references them any more):

- `oven_front.png`         `a7e6ff28615bd73a017a9e59518023b80e823a6178117d58fecfc606053d5ee6`
- `oven_front_active.png`  `6af996a09c044b943dc6d0fc69cbe7ac74340e860b952c10429cff28426a5675`
- `oven_front_running.png` `40ac47fac76cd859703bde605b0095cb33f55ffa9da9323fe5299f4dec75aee9`
- `shredder_front.png`         `7987357d4fd1205431cfde328f9e442cc454d178bb87a2d05ca3659aa4d7eec3`
- `shredder_front_active.png`  `ca114cacd51c1d662cd0ee30eab3cbfeb9df03d5afa5fb09de8bb54916f1e891`
- `shredder_front_running.png` `7987357d4fd1205431cfde328f9e442cc454d178bb87a2d05ca3659aa4d7eec3`
- `crusher_front.png`         `634a425059a530e1a2279342717cf77e3e1d0ff6eac2933dee0492e2118c62be`
- `crusher_front_active.png`  `2efd256b8f70385118ecff73075b3b20d62ae9c6a6f7e674c0d7253b0c89eb70`
- `crusher_front_running.png` `634a425059a530e1a2279342717cf77e3e1d0ff6eac2933dee0492e2118c62be`
- `lathe_front.png`         `6b6100db65c96a4185f1563bdc0657f67f87c9c04f7d76a6fd9ebe8ae6620903`
- `lathe_front_active.png`  `4c787010041ea75483b4eedffb17e402c8763de27e2271a4eacccf960972ec71`
- `lathe_front_running.png` `6b6100db65c96a4185f1563bdc0657f67f87c9c04f7d76a6fd9ebe8ae6620903`
- `dryer_front.png`         `f075bba0be3c71dfc2a9f463b9be8ae717350e1675a4531f38a25b2e1c74358c`
- `dryer_front_active.png`  `cdbbc989598b9955868e457728dc88f4cca6e317b90e3ba3fc0d117252cc327d`
- `dryer_front_running.png` `c02c6dda70cb39555251d5de281cefecc420c5c83c91f123ac98e402508fc81a`
- `distillery_front.png`         `8a6ac14f9b121618afff00b3bf7372484adbe7a0307848ee9f96188536b3ce45`
- `distillery_front_active.png`  `6fc656f9cd0a57f4023da022bb7056a6e05c93ac61f4369e7e785360ce5e7a4b`
- `distillery_front_running.png` `f91ced667197de43286fee72872c960ef044defaffc1ed27acac70bb2f738e64`
  (the distillery trio is UNCHANGED from the P19 bake — listed for the
  byte-identity gate; no new bytes committed for it)

Shared body byte-identical copies (no bake — single opaque layer, the landed
single-cube model carries no body overlay pass; the P9 oven-overlay precedent):

- `oven_bottom.png` `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
  (upstream `basicmachines/oven/colored/bottom.png`)
- `oven_top.png`    `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
  (upstream `basicmachines/oven/colored/top.png`)
- `oven_side.png`   `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
  (upstream `basicmachines/oven/colored/left.png`; left == right == back
  byte-identical upstream, so the one side key over south+east+west carries
  NO divergence deviation)

Declared deviations (the P19 rulings repeated over the six families):
(1) animation strips are baked at FRAME 0 — the faithful multi-frame animation
stays in the render pool; (2) the colored base keeps its neutral grayscale,
the per-material mRGBa tint is not baked (the W3 tint card); (3) this bake is
the canonical flat look, not the render end-state — the faithful multi-layer
per-face pass system stays the render pool card.

Baked / copied on 2026-09-06. Upstream license: **CC0 1.0 Universal Public
Domain Dedication** (same upstream `README.md` block as above).

Split machine front layers, task p22-paint-front-overlay-split (2026-09-07):
the baked composites above are RETIRED — the machine model is now the upstream
TWO-LAYER form (datagen TWO elements per model: the tinted body cube keeps the
plain grayscale `colored/front` north face; the state decal is a separate thin
untinted front quad, mirroring MultiTileEntityBasicMachine.java:1014 +
BlockTextureDefault.java:179-180 where the overlay layer is UNCOLOURED and is
never multiplied by mRGBa — a painted machine no longer re-tints its
active/running decal). All 18 `<family>_front{,_active,_running}.png`
composites were REMOVED (their digests above stay as the retired bake's audit
trail); the bake mode of `mdk/tools/bake_machine_fronts.py` is kept only as the
byte-reproducible audit path, its ACTIVE mode is `--split-fronts`.

The split borrows (24 PNGs under `gt6/textures/block/`, CC0 from the same
upstream family iconsets; single-frame sources are byte-identical copies,
16xN strips are cropped to FRAME 0 and re-encoded — the P20 "animation stays
retired" deviation carries over verbatim):

- `<family>_colored_front.png` — the generic machine plate, byte-identical
  across all six families AND to the oven body faces (upstream
  `colored/front.png` per family):
  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `<family>_overlay_front.png` (inactive decal), byte-identical copies of
  upstream `overlay/front.png`:
  - oven        `3da4d0f7d578f6e08102a0b7615160035997d58affc26c7d13813b678c4bd7fd`
  - shredder    `92aba8f65d8955a5eddb687e3852d68457409be2d540983903fe8f17ef6708c8`
  - crusher     `8306618d4de778652178fb8761174baeebbefb8bae4c80b61598618af6ae7fd0`
  - lathe       `71134a5929e77c46002534aed767578e250381c981454583759a4ba1e57f9132`
  - dryer       `5b17e8425cccdec0932990c7a31db6ef1a7d4ae96b6905ed1ca268d2cfb54a5f`
  - distillery  `a36ecca013dbd36ddc02f10d2ce0ec640030147ddb3c180c299fac67ad57e4d3`
- `<family>_overlay_front_active.png` (active decal, FRAME 0 of the 16xN
  strips — 8/4/8/6/8/4 frames respectively; cropped products, NOT byte copies):
  - oven        `f5d2e58d185c1f4abcf65b7fa8bfcc3267d0c09c9a08b1a8c17916d1dba82209`
  - shredder    `9cb70f16c77d6d24ad7f9f286a1ad08d35046a30a820ef914df8e45f9baeba82`
  - crusher     `7edf49a0e0546f3014fb614311f1b2502138cdc541d6d5e11365073d5005a260`
  - lathe       `5a227c7e98ca34ad5be4964f433ae0229e4ee63ba60d5a5829bf39a8bfeab88a`
  - dryer       `5147699d6c5e1dead58a7c5a389f53fa18fde768de2e0e9489903b2bc5ea5f60`
  - distillery  `50813e576e4ac5be015283b7dcd57c5dc667ffa0b2acc8b2d6899ae2bea70883`
- `<family>_overlay_front_running.png` (running decal; oven/dryer/distillery
  are single-frame byte copies, shredder/crusher/lathe are FRAME-0 crops of
  4/8/6-frame strips):
  - oven        `cd5857690ac01a9fc10e3aadd4fe3afe3c2372d685ef0bcf14b680756b92b85e`
  - shredder    `8807bef2098c6fb163431c0c59f8c2d67a045fa68e5d53bf102c5c2c789809a4`
  - crusher     `0e90f0c0dbc13b7ca66224eaa213deda4abfd26930f2fa7287c03a902b0bcb0a`
  - lathe       `9758ec8e367e2fbc12e6cec3e1f03f77f358226eaeaae3155981f64ff47a22c2`
  - dryer       `b37a675b2e2ed105118a1b3de15332dc92459da03084b2310ce5d7efc7b049a0`
  - distillery  `88b5c302301a4773e5b880878f9423a79d3e12c2ac05dbb2a3c31dd84c961a57`

The P9 GTOvenOverlayModel strip borrows (`oven_overlay_active_front.png`,
`oven_overlay_running_front.png`, full 16xN strips, digests in the P20 overlay
source table above) are a DIFFERENT namespace consumed by the oven render
snapshot system and are untouched by this split.
GT6 item materialicon textures, task p20-borrow-item-material-sets-a: the 1373
PNGs under `gt6/textures/item/material_sets/<set>/<prefix>.png` — the
alphabetical first half of the 40-set census (`brick`..`lignite`; the second
half rides task p20-borrow-item-material-sets-b) — come from upstream
`src/main/resources/assets/gregtech/textures/items/materialicons/<SET>/<Prefix>.png`,
byte-identical to upstream, sha256 verified per file (manifest below). The set
folder lowercases (`CUBE_SHINY` -> `cube_shiny`) and the icon segment maps
camelCase -> snake_case (`rockGt` -> `rock_gt`, the MaterialPrefixItem.snake
serialization) per the 1.20.1 `ResourceLocation` charset — the same declared
lowercase deviation as every borrow above. Per ADR-P20 §4.1 the gen_textures.py
grayscale placeholders always lived in this static tree, so the borrow is an
in-place overwrite (path unchanged); the census's exactly-once union face
(GT6TextureCensusTest) keeps guarding it. The upstream `*_OVERLAY` pass
textures are NOT borrowed (P8 precedent — the port renders single-pass
grayscale + runtime ItemColor tint); no `.mcmeta` animations exist in these
folders. Census 2026-09-06: 20 sets x {6..102} PNGs = 1373 files, zero gaps —
every placeholder had an upstream counterpart, nothing declared.
Copied on 2026-09-06. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

- `BRICK` -> `gt6/textures/item/material_sets/brick/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `dust.png` `2dde58a86ce32a463561992f9e702864435eab9382d448cad088147bad517882`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `f64458a5f51d2242cac983a986910f3fcb7cace1cfc87d76f6ba7e31c0598e49`
  - `dust_tiny.png` `22b40798f41a685cc4ba57757093ffbc98a8f7c1870441d0fa23e251599b8587`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `4ad58d783b364414a17d32a2c43ac0916a29582f9d29572232be95508c827e43`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `COPPER` -> `gt6/textures/item/material_sets/copper/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `48348804354d5eeab7046ef60b63d1a7481785a09792f37e92e09015bff414c2`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `dfa6ae381743bc43947869dd56325b315c08f38f2228b7dba5f848e74d8696e3`
  - `dust_tiny.png` `8d4a526a3a2a64abd4b58dbc32501fe5827a5ab5175275f19469e7db0d24530e`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `814e1c68fc1ac57e373d8fb28bf948e84a684c44ddadb736cefbc16c2387cc88`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `52d59351dad1ed9891fb5dfdb915f33dbb4002b58f7947c1716c5d65b9c12cc5`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `35cb46c66b84758bc2dec56402f6e2456d518da54b2b8c63235d697c81c3f83a`
  - `ore_raw.png` `a6e7271f10d00d03899b6084f799f74c6b4f0dd876ac3addfb963568bdd3f0a9`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `e4afea515ce7f47bea27fe50fdfe72701f115f2b2fb0480a2cfde96ea97d9b24`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `b9e6d410996a3f3e49b2e11031faec86313adce8e7a6b403418b45b09959c473`
  - `plate_quadruple.png` `7f27cdce14020bd6a15bdbe46d0ecf2330ed99f0b47a459bb68e697ea75ab735`
  - `plate_quintuple.png` `e63af70e3e8008471cf5c93631887c8b915faee06d5d6460ce781d73b6638152`
  - `plate_tiny.png` `3c6cfc8f8994dd17f7dbb838bc806227dba35f7ed563dfa7055510e5c504a9bb`
  - `plate_triple.png` `58b3fb7cc1e4f5d5874590638cdc0c00fb9ce944a4ad50719ccfc18d7a331405`
  - `rail_gt.png` `6dd3ecb92744218a440b86eda678fb7cdb9e5d7862defc3920229af16ce40049`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `CUBE` -> `gt6/textures/item/material_sets/cube/`:
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `ingot.png` `67d090abbb60bae7215e6c42e3ff7e36f9f099842ee6ebaae476134bcd299ee8`
  - `nugget.png` `35cb46c66b84758bc2dec56402f6e2456d518da54b2b8c63235d697c81c3f83a`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
- `CUBE_SHINY` -> `gt6/textures/item/material_sets/cube_shiny/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `2bc5a1cf9762e0a31785895bf4a14a58373ba6c95d596a876e4adb588ccf14a3`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
- `DIAMOND` -> `gt6/textures/item/material_sets/diamond/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `83a68cd7fcf41cfc075acd0663139aa679e5ff600adda47a652545aa9445c6dd`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `ingot.png` `48c6cd3ffb2570da277a798fc58977418a2737a124a69627cbbf88dcb135b74c`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `88912895ac085ce25f9024e58865d80d2106ce891a5389a4b238efe47aee7d41`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `DULL` -> `gt6/textures/item/material_sets/dull/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `09ae6d06850aeba95bc49d101060015aeebc508f832f752002b626f9a1a49a42`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `707f345aed695549848f8c741b68935a8f920b604d96533b3c7ba717268070fd`
  - `dust_tiny.png` `7bbb7b1782b608c5d49bf4514f34c678c64c7db8a5c813ab6039044218757c96`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `431e102c8bad21809c9e5260497a03ef7aa129f0cabbcd8993827ee0b66c71b8`
  - `ingot.png` `84821f634f16f50ec5d64133e9fc572505ebdc6915a49397251c0594234a18c9`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `53502e11de9f55e2a9a1936a0ee8448ad4f6ed550dd7c8515c9f393da8fb767c`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `5c4fe86b5fa7a3a443039bf15e220245a54c12071d81bd80b197485ef0db4cd2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_gem.png` `9b994966d16b8002f69248217b110dc74750a39e7089b6f5e41c20919600b7b8`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `rail_gt.png` `6dd3ecb92744218a440b86eda678fb7cdb9e5d7862defc3920229af16ce40049`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `EMERALD` -> `gt6/textures/item/material_sets/emerald/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `d022b117f4d2c7c89d9f8d6d4f02abe382a63d02f242e669459953ec2fc6804b`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `FIERY` -> `gt6/textures/item/material_sets/fiery/`:
  - `arrow_gt_plastic.png` `3455a876a86479546a3bf114b9cab01faaa46e42915d81da89f22c4076068435`
  - `arrow_gt_wood.png` `b343391913fd95b91572c472503a695b969110b1a7855ab23b53f867d283b53b`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `ingot.png` `48c6cd3ffb2570da277a798fc58977418a2737a124a69627cbbf88dcb135b74c`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `88912895ac085ce25f9024e58865d80d2106ce891a5389a4b238efe47aee7d41`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `FINE` -> `gt6/textures/item/material_sets/fine/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `431e102c8bad21809c9e5260497a03ef7aa129f0cabbcd8993827ee0b66c71b8`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_gem.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `FLINT` -> `gt6/textures/item/material_sets/flint/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `2dde58a86ce32a463561992f9e702864435eab9382d448cad088147bad517882`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `f64458a5f51d2242cac983a986910f3fcb7cace1cfc87d76f6ba7e31c0598e49`
  - `dust_tiny.png` `22b40798f41a685cc4ba57757093ffbc98a8f7c1870441d0fa23e251599b8587`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `20e923a70a8d7765449f1e8489018b8a90a7078503ea49ea84d4dff41ad5c89b`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `9b994966d16b8002f69248217b110dc74750a39e7089b6f5e41c20919600b7b8`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `FLUID` -> `gt6/textures/item/material_sets/fluid/`:
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
- `FOOD` -> `gt6/textures/item/material_sets/food/`:
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `dust.png` `ec5c68d9a2db80852fee6da046378a731d1ad18891319287c473cc3d034b0c0e`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `7f324c03b0d7460554a96535d07f929bef8d00001bd25493142d0f5922a5f273`
  - `dust_tiny.png` `0c16d8f27a783b49b39a31b0ecfc4a26d31d783cf081bfd1d959f400ed34f47a`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
- `GAS` -> `gt6/textures/item/material_sets/gas/`:
  - `chemtube.png` `2f31fca929719d864bf5eda2ca065352805998b54b112a856e88890e662e9279`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
- `GEM_HORIZONTAL` -> `gt6/textures/item/material_sets/gem_horizontal/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `7c65ead11e25c5d23aa8f2e1ab6b20216a6b63ef817bc75f08574dd80387a159`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `GEM_VERTICAL` -> `gt6/textures/item/material_sets/gem_vertical/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `e3c16acc4d625d10f0f8a06da353a284759a833480969a59da973b180391f3c3`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `GLASS` -> `gt6/textures/item/material_sets/glass/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `7cdfa8a3c9f5d6847a42f3ed65c5915788303c2b9483e8adb7f64e75b3ff4f84`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `9fe9a179bf260a13375748c49d8ba15b08551f41513c0c4ff4798d8fde3bc469`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `HEX` -> `gt6/textures/item/material_sets/hex/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `f3de6a2d71d3955a57b150a963f4bdfd4152a9a9bad991a7f13b914e0c5292e7`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `e170aa06c9ce4ec51eb0c41fd03a40be6c9dfe20a342c81b17e9a1ece3792eae`
  - `gear_gt_small.png` `07f0903a9dc8d6fc7672e896cecedbc4ce11e0a48d5ee1f75bdbff13ceee857b`
  - `gem.png` `8360c570f30ac3d5614093b802de2f1935a8d3fc2b15d824456ea67aded17cad`
  - `gem_chipped.png` `3b571ce2ad3cf40407052a0819c30457bd7d66bcc630a8471e58d45dd5ee0796`
  - `gem_exquisite.png` `83205c9ee3f5659abccffd8e8f3b976c404b3dccd79f40ab15ffd37109ee7931`
  - `gem_flawed.png` `e9f18a68e857804e72d22c3493e18d2b6fdcb170fbf6cd621b623f26403ae5da`
  - `gem_flawless.png` `ec27578a764d8bf5832033f06aabd461b9e080b5d056b8c04fd41f0e87934f1c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `e681987ae55938f61e3e5452db79ded39c4721a39252510d222baff156a01f80`
  - `plate_gem_tiny.png` `85897d1304d1bb4206b4f80211cc791e43ede96fceb4fd1a7d481fac36adfb56`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `LAPIS` -> `gt6/textures/item/material_sets/lapis/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `ad8c41aced5aa33ce3116a9bb82f04e37db033280fb4bf3d4ed932481f77ebc6`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_gem.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `LEAF` -> `gt6/textures/item/material_sets/leaf/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `1cdba4d6ef4b0e09935a4b99d80288b17761d9057931bc239e4d939dfb2a450d`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `73b2dda626aed7240b5afd3631cfb5219f74a61ca5a318df19cd8107442b0d2d`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `90e3d52f97d1f4d77af40529eb7530e1b4f73eb3be6e0fac66f40a330dda603f`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `90aa415037f77e47f638844a20360f821ec682ac663600811cae81cf7459c5f5`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `80b55559b3f896ae1b9028d8e0f1c4d5ff162fd9f6b74ffd936428a36cd77367`
  - `tool_head_axe.png` `81344ff4928fd2cfbd6585cbd19bf3786d710a4c3e3c74e5585f246e960855d1`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `a9fcb57476a5be0549fa07f748c2ef716c94554d2a92b29e0731c4dfeb572f08`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `80b55559b3f896ae1b9028d8e0f1c4d5ff162fd9f6b74ffd936428a36cd77367`
  - `tool_head_raw_axe.png` `81344ff4928fd2cfbd6585cbd19bf3786d710a4c3e3c74e5585f246e960855d1`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `a9fcb57476a5be0549fa07f748c2ef716c94554d2a92b29e0731c4dfeb572f08`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `3c0aaf29ea15b96fa39342650d88da376be146a3bcba7853244d0abd7a35aa82`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `3c0aaf29ea15b96fa39342650d88da376be146a3bcba7853244d0abd7a35aa82`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `LIGNITE` -> `gt6/textures/item/material_sets/lignite/`:
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `2dde58a86ce32a463561992f9e702864435eab9382d448cad088147bad517882`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `f64458a5f51d2242cac983a986910f3fcb7cace1cfc87d76f6ba7e31c0598e49`
  - `dust_tiny.png` `22b40798f41a685cc4ba57757093ffbc98a8f7c1870441d0fa23e251599b8587`
  - `gem.png` `da5290755005eb6937e3c4d0c2226c1004016c6f9c58fa4931ca2163e0dd1665`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_gem.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
Steam Boiler Tank block textures, task p20-borrow-tank-barrel-pipe: the 3 PNGs
under `gt6/textures/block/boiler_steam/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/tanks/boiler_steam/colored/`
(the `MultiTileEntityLargeBoiler.java:348-350` `sColoreds` icon stack — the same
group the 26 Steam Boiler Tank rows render through), byte-identical to upstream,
sha256 verified:

- `boiler_steam/bottom.png` `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `boiler_steam/colored/bottom.png`)
- `boiler_steam/top.png`    `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `boiler_steam/colored/top.png`)
- `boiler_steam/side.png`   `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `boiler_steam/colored/side.png`)

Upstream fact (the tap/funnel same-bytes precedent): the three colored faces are
byte-identical to each other and to the `kinetic_steam/colored/side.png` bytes the
p12-engine-steam entry already carries (`steam_engine_side.png` above). Declared
gap: `boiler_steam/front.png` KEEPS its generated placeholder — upstream ships no
`colored/front.png`; the front face renders the side sprite plus the `BI.BAROMETER`
gauge overlay at runtime (`getTexture2`, MultiTileEntityLargeBoiler :358-361), an
overlay pass that is NOT borrowed (the p8 single-pass ruling). The p13-era census
note that this group had "no borrowable source" is superseded by this probe
(the research-card id333 correction). The `overlay/{bottom,top,side}.png` group is
NOT borrowed; the grayscale colored base renders un-tinted, the mRGBa tint and the
barometer visual ride the render pool card.

Barrel family block textures, task p20-borrow-tank-barrel-pipe: the 3 PNGs
`gt6/textures/block/barrel_{wood,plastic,metal}.png` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/tanks/`, byte-identical
to upstream, sha256 verified:

- `barrel_wood.png`    `24c4d75618258809715e41527b5189351ef4870e0372a07463bfd266a883e6d0`
  (upstream `machines/tanks/barrel/colored/side.png`, the
  `MultiTileEntityBarrelWood.java:48-50` colored stack)
- `barrel_plastic.png` `c696979c208e967f0fb43e2fc1d15d70209c4854b4b61aa3608eabf03f85c754`
  (upstream `machines/tanks/plasticcan/colored/side.png`, the
  `MultiTileEntityBarrelPlastic.java:46-48` colored stack)
- `barrel_metal.png`   `deec36f927d4d298723212a1a70c6e465465e82ed07d522fbd6b41b186a59b4a`
  (upstream `machines/tanks/drum/colored/side.png`, the
  `MultiTileEntityBarrelMetal.java:43-45` colored stack)

Path mapping (the barrel_logistics p12 precedent, unchanged): upstream
`machines/tanks/<group>/colored/side` flattens to `barrel_<material>` per the
one-PNG-per-row `cube_all` convention (GT6BlockStates `addBarrel`); the `side`
sprite is the borrowed face because the cube_all key set has no top/bottom keys —
upstream `colored/{bottom,top}.png` exist but have no repo counterpart (declared
simplification, not a gap). The twelve high-tier metal drums keep sharing the one
`barrel_metal.png` (the p7 ruling). `barrel_logistics.png` is NOT re-borrowed: the
p12 entry stays byte-exact (sha256 re-verified `f348ace8e2d98ed701a060a87c4c66ec25b708449cd96611b66aa01cc9c595f1`).
The `overlay/{bottom,top,side}.png` groups are NOT borrowed; the grayscale colored
base renders un-tinted (the mRGBa material tint is the render pool card).

Burning Box family block textures, task p20-borrow-tank-barrel-pipe: the 4 PNGs
under `gt6/textures/block/burning_box_*.png` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/generators/`, byte-identical
to upstream, sha256 verified:

- `burning_box_solid.png`    `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `burning_solid/colored/front.png`)
- `burning_box_liquid.png`   `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `burning_liquid/colored/front.png`)
- `burning_box_gas.png`      `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `burning_gas/colored/front.png`)
- `burning_box_fluidbed.png` `4f8004f09dc2ef5f20eca50efd1e691b2650b7b15e173fb95997619f79719332`
  (upstream `burning_fluidbed/colored/front.png`)

This section SUPERSEDES the p13-burning-box-family declaration above (the
"no borrowable source" claim): the probe found the five
`machines/generators/burning_{solid,liquid,gas,fluidbed,brick}/` iconsets in the
snapshot, each with a 6-face `colored` set plus `overlay`/`overlay_active`
(consumed by `MultiTileEntityGeneratorMetal.java:40-59`, `...GeneratorGas.java:57-76`,
`...GeneratorFluidBed.java:282-301`). Upstream fact: within every family all six
colored faces are byte-identical, and all four families share the SAME grayscale
bytes as the boiler_steam group above — the visual family differentiation lives
entirely in the overlay/overlay_active passes and the mRGBa tint (the render pool
card). The `front` face is the borrowed representative because the repo cube model
applies one texture to all six faces (GT6BlockStates `addBurningBoxes`) and the
FACING face carries the front semantics. The `burning_brick` group exists upstream
but is NOT borrowed: the repo Brick row shares the SOLID model per the p13 ruling,
so there is no brick key to fill. The `overlay`/`overlay_active` groups are NOT
borrowed (the p8 single-pass ruling).

Fluid pipe block texture — DECLARED GAP, task p20-borrow-tank-barrel-pipe:
`gt6/textures/block/fluid_pipe_wood.png` KEEPS its generated placeholder. The
upstream wood fluid pipes render through the material icon system, not a dedicated
block tile: `TileEntityBase10ConnectorRendered.getIconIndexConnected` (:265) picks
`OP.pipe{Tiny,Small,Medium,...}.mIconIndexBlock` and `getTextureSide` (:522) resolves
it against `mMaterial` with the mRGBa tint — i.e. the grayscale
`blocks/materialicons/WOOD/pipe{Small,Medium}.png` slices drawn by the dedicated
pipe renderer. REVIEW CORRECTION (merge audit 2026-09-06, d40d2dd8 follow-up):
those bytes are RGBA but FULLY OPAQUE (alpha=255 on all 256 px, avg RGB
136/136/136 — a mini pipe-segment sprite on a plank-textured background), so the
original "transparent cross-section / see-through holes" wording was wrong. The
gap ruling itself stands on the two verified facts: (a) the renderer applies the
mRGBa tint at runtime (`BlockTextureDefault.get(mMaterial, …, mRGBa)`), the icon
is grayscale, and the repo `cube_all` model has no tint path — a borrow renders
gray; (b) the sprite is a diameter-scaled renderer piece, not a per-face block
tile — on every `cube_all` face it would show the same miniature pipe-on-planks
drawing, which corresponds to no face of the p4 solid shape. Borrow-or-declare
therefore declares the gap; the faithful pipe cross-render family
is the render pool card.

Copied on 2026-09-06. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

GT6 item materialicon textures, task p20-borrow-item-material-sets-b: the 1412
PNGs under `gt6/textures/item/material_sets/<set>/<prefix>.png` — the
alphabetical second half of the 40-set census (`magnetic`..`wood`; the first
half rode task p20-borrow-item-material-sets-a) — come from upstream
`src/main/resources/assets/gregtech/textures/items/materialicons/<SET>/<Prefix>.png`,
byte-identical to upstream, sha256 verified per file (manifest below). The set
folder lowercases (`CUBE_SHINY` -> `cube_shiny`) and the icon segment maps
camelCase -> snake_case (`rockGt` -> `rock_gt`, the MaterialPrefixItem.snake
serialization) per the 1.20.1 `ResourceLocation` charset — the same declared
lowercase deviation as every borrow above. Per ADR-P20 §4.1 the gen_textures.py
grayscale placeholders always lived in this static tree, so the borrow is an
in-place overwrite (path unchanged); the census's exactly-once union face
(GT6TextureCensusTest) keeps guarding it. The upstream `*_OVERLAY` pass
textures are NOT borrowed (P8 precedent — the port renders single-pass
grayscale + runtime ItemColor tint); no `.mcmeta` animations exist in these
folders. Census 2026-09-06: 20 sets x {6..102} PNGs = 1412 files,
zero gaps — every placeholder had an upstream counterpart, nothing declared.
Copied on 2026-09-06. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

- `MAGNETIC` -> `gt6/textures/item/material_sets/magnetic/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `67d090abbb60bae7215e6c42e3ff7e36f9f099842ee6ebaae476134bcd299ee8`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `35cb46c66b84758bc2dec56402f6e2456d518da54b2b8c63235d697c81c3f83a`
  - `plate.png` `5c4fe86b5fa7a3a443039bf15e220245a54c12071d81bd80b197485ef0db4cd2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `METALLIC` -> `gt6/textures/item/material_sets/metallic/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `48348804354d5eeab7046ef60b63d1a7481785a09792f37e92e09015bff414c2`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `dfa6ae381743bc43947869dd56325b315c08f38f2228b7dba5f848e74d8696e3`
  - `dust_tiny.png` `8d4a526a3a2a64abd4b58dbc32501fe5827a5ab5175275f19469e7db0d24530e`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `b3b1bcae9646a7a116921c2d63189d0d12f4e85f7c12ff978c78481bc4bfe0c6`
  - `ingot.png` `67d090abbb60bae7215e6c42e3ff7e36f9f099842ee6ebaae476134bcd299ee8`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `35cb46c66b84758bc2dec56402f6e2456d518da54b2b8c63235d697c81c3f83a`
  - `ore_raw.png` `a6e7271f10d00d03899b6084f799f74c6b4f0dd876ac3addfb963568bdd3f0a9`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `5c4fe86b5fa7a3a443039bf15e220245a54c12071d81bd80b197485ef0db4cd2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `rail_gt.png` `6dd3ecb92744218a440b86eda678fb7cdb9e5d7862defc3920229af16ce40049`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `NETHERSTAR` -> `gt6/textures/item/material_sets/netherstar/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `3b793557b0c42345574b596184ea6729451e9e8a77c2c6dc30bee4fcf211bcfd`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `NONE` -> `gt6/textures/item/material_sets/none/`:
  - `arrow_gt_plastic.png` `55bda5d49b731525522093b57afa085d3b7f5118d68cb40e0cc952171eec819f`
  - `arrow_gt_wood.png` `a22b66fb2d888b6a054e931641156ea3359a11333fa8c6bf3650e53f21ae829f`
  - `bullet_gt_large.png` `3d954808dea93a9567138fb1247cea34a9cd2c4a2c542dc6eda3832504ab5a65`
  - `bullet_gt_medium.png` `fb7294ffde727a4707ec55770a497dd1bb46a34104782b12ad4d1d43aa8eff02`
  - `bullet_gt_small.png` `99120128d46a3b3f7394ca28dd083f7f6fd599e5dd67090f66fdf6f642695361`
  - `chemtube.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `OPAL` -> `gt6/textures/item/material_sets/opal/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `431e102c8bad21809c9e5260497a03ef7aa129f0cabbcd8993827ee0b66c71b8`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `PAPER` -> `gt6/textures/item/material_sets/paper/`:
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_double.png` `46ab9c2298b2adbedff0a92bd448635d77668906689da3d10a58b6bf0b7f8b79`
  - `plate_quadruple.png` `5c5aebc8291eae79a29f518670fe52ed5f1cfc0b284208b9bdefce1cc15d256d`
  - `plate_quintuple.png` `bdf0482ac3b0f8786c54b860d1ef13a5e8f3e464615424250c1a4000174b3f2f`
  - `plate_triple.png` `8b995ef0a9d8262cd9daa73e35f3c38e224b84113a5fd0e45bb88ad874b09757`
  - `scrap_gt.png` `d01a57048108841cdbf2cf8eaa23a5850a5667aff4bcd802842c9a19b9c59061`
- `POWDER` -> `gt6/textures/item/material_sets/powder/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `411a66061344ff7f11466c4ef08be81f5b825313d7d7ec56d2989cde31af9f9d`
  - `dust_div72.png` `a4c494d78399d34d5418b5266965a83a0d70997be81158bd9a8178f949d7246e`
  - `dust_small.png` `a2a3366302b7dc8b53c7d03f8e5ff5685098c3d6a37835ee94297c03c46fd99a`
  - `dust_tiny.png` `443101fbb94bb64802f545e5d963bb2db09374fb1d118b88991f5e180c1dea2b`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
- `PRISMARINE` -> `gt6/textures/item/material_sets/prismarine/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `2ac5f879aef3ef274f5dc6df993d84cf0a5b5c5c05389a9e9240bb3b7d723c92`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `53018eb26c703232f06447288ef8bfb145395afa7a8f0c79e82fc634fcc9c5b4`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
- `QUARTZ` -> `gt6/textures/item/material_sets/quartz/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `ebde84e046748dab633b5e0ce2d6f5e83bfa687a9687cfdf607fc12e7fd63b60`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `9fe9a179bf260a13375748c49d8ba15b08551f41513c0c4ff4798d8fde3bc469`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_gem.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `95dbae416dfb756c10b94a2403417ca4c9ce07c7cd9895197a2e82246ec63d78`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `RAD` -> `gt6/textures/item/material_sets/rad/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `cfc9510619943b6ba413e4c6e9de040511bc0a835e0af809aa83dea900ecf813`
  - `crushed.png` `c6da838b8881beb89f9aee1d8e650b51eeefe0cfd1faa2ea5af256beac25de5d`
  - `crushed_centrifuged.png` `96fee0826beaf4179c332453f2724fec431615298d52bdcd4516cf8e45ff00f1`
  - `crushed_centrifuged_tiny.png` `8ecccac875aeb1a8a5c50f192f128d494f73401aa45c64bdd7528a8b404761ca`
  - `crushed_purified.png` `cc1eecf62c707791d0e94bd03527808f7409205c3de7f6106cdcd81ee4d536b8`
  - `crushed_purified_tiny.png` `666c95c2044ce3f8a212639f13a6af66ccd560e2967bf1a5c77358bec468cd5e`
  - `crushed_tiny.png` `c587497d38458b96636fa4ab7a75c630cda616b08ede54267ef352844a3f21b8`
  - `dust.png` `ac61f65dc7bf1d17cc1fa07f0635a5f32e7fcb1bf196b314f0f61de0bedf2a2e`
  - `dust_div72.png` `70eee41c08c5b51d162892a242bdbb1641bc4e85de9b9c526f88dae050618eb4`
  - `dust_small.png` `04929803255d638a7e0d8f6f6dc10eb6ec3ea4adc1d49d727f04400b9fc446b6`
  - `dust_tiny.png` `022759e4cfe505077e08fa12e7238857c5265783e3c758b3f1c05c017e4da094`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `5d85f4194643a4daca4a45a07cb8d2fbb9a5243c8298cab02b852863634c78b6`
  - `ingot_double.png` `863c46bd527fa5023636a3e30a4681469030eaca0f4165347f0dc72cebab56aa`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `5d24861f0a9b12890ad75bf0eab65fbc91c32c5e164e245a7525cb6a392eb9cf`
  - `ingot_quintuple.png` `739c022df2efd8bf46f5d0f9180a3f0dc3acb994ad36c0de749d510f723a110a`
  - `ingot_triple.png` `f702a96c8dd476276278e988509df2f9158c5adc5fb5f947ff93167bbb6afef8`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `a955ecb02da25bd4ec29429114d0bc77efa833e18efc519f607409db261f306e`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `5c4fe86b5fa7a3a443039bf15e220245a54c12071d81bd80b197485ef0db4cd2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `REDSTONE` -> `gt6/textures/item/material_sets/redstone/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `fd681c4474e0550b4608cf3cf577070b425f3c486571959fb6fcda22c84376f8`
  - `dust_div72.png` `e9ad1adf6bdc51413d221e4aef9566f41abae1612c3e6f42e2fb6b7619f227cc`
  - `dust_small.png` `b24ffd9c8255f17b2d10a14773adb150a587414901edb3ef59c94699901ca9ed`
  - `dust_tiny.png` `6f3f55aef3ca78a55875b6b2b8db8701028924313a077f2cc1846d04da88cec4`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `9374452ca67248b7f0a0aa755985af87eb3258ce7ad2f69c07e513a8f1bea8fb`
  - `gem_chipped.png` `612bf9989728b1e734e4e90d5c635117cc2428b47cbced31ae059a07517ae412`
  - `gem_exquisite.png` `7009407663c177f3d1894487cc6bb6ffd3f069ae5db4448ec5ecaa5de3c72eb4`
  - `gem_flawed.png` `444ec6f7591a3b950dc05cfe55c2a738a0ca291178dcef8fe5716644292fd9db`
  - `gem_flawless.png` `79659ec12016c44870267d43d20d84533157719f74e6cc2688091c096561f016`
  - `gem_legendary.png` `107ade532a543ea4c48c12262398930262c18bc864f624f89844e00ff56e3038`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_gem.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `ROUGH` -> `gt6/textures/item/material_sets/rough/`:
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `2dde58a86ce32a463561992f9e702864435eab9382d448cad088147bad517882`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `f64458a5f51d2242cac983a986910f3fcb7cace1cfc87d76f6ba7e31c0598e49`
  - `dust_tiny.png` `22b40798f41a685cc4ba57757093ffbc98a8f7c1870441d0fa23e251599b8587`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `gem.png` `431e102c8bad21809c9e5260497a03ef7aa129f0cabbcd8993827ee0b66c71b8`
  - `ingot.png` `27895d5e3bf7b5262681909aa0fbb5836078a6fd241b218549ef2dcc0f5d218e`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `f76060f2f51cf6872e43956228239d6fe87e32b512461c56d9c0d3d3ecdcc189`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `RUBBER` -> `gt6/textures/item/material_sets/rubber/`:
  - `arrow_gt_plastic.png` `db75909d094b62f339d1baa339ed736a90afbb63cd0976fe6a0e04366be5154f`
  - `arrow_gt_wood.png` `3ca7a9cad97163ab926ac5bf98979bb03e9864ae44d371defa94834dab88172b`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `4ed665cf0aa930a3a4e14ee94d360e24e177bba101d306c1997dff345ecf0341`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `ingot.png` `8ed995c1577b0caccfe7a25e021f96d2a8dda6710bd2a6f6b944fea54d874cb1`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `nugget.png` `e0339b5463d94e1ff5da7e7828b3018441cf976d03d6ffeea2a0f95151ad2191`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `8c07aab5ece43996de1ffddd09ca3b829afc6d2ed1ef0b8eeffdce5931d473eb`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `71b902eb8467282d06f4ab7039b6298d9ff9ff78dea42334ec3405983131fd5a`
  - `stick_long.png` `eadbd58fa0d1c96779875533f9c583d7239e5b4a592e85202286d62313259a12`
  - `tool_head_arrow.png` `e1ca3498e602c70e438d56723e5172dee21b871266023943c1b120b21e4d02a4`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_hammer.png` `1eff67177215d3726a169f268ae2b9cdeb92ffd650a54ecde4184fc4ddd0be4f`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `RUBY` -> `gt6/textures/item/material_sets/ruby/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `431e102c8bad21809c9e5260497a03ef7aa129f0cabbcd8993827ee0b66c71b8`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `ingot.png` `48c6cd3ffb2570da277a798fc58977418a2737a124a69627cbbf88dcb135b74c`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `88912895ac085ce25f9024e58865d80d2106ce891a5389a4b238efe47aee7d41`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `SAND` -> `gt6/textures/item/material_sets/sand/`:
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `858d9c533edab21fed9f47fa535c17f35f8fc42f5c0c97542dcc39e64a3e5141`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
- `SHARDS` -> `gt6/textures/item/material_sets/shards/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `bb7811642c728751d889e27e59e6e7a118d56f07a302187c2be54cff5c873e0f`
  - `gem_chipped.png` `b8036a31c22d7bce89bc43f24bf020ea9d9f761152e7608aac69e0ef3c162353`
  - `gem_exquisite.png` `a09df815ff6c59427b798a3122bd7e86218d1fabc32e55acc5c2098dd7091a54`
  - `gem_flawed.png` `cf0c82824d562407db929823db3a96e061c09db8f510e6f7ea207213d542ad60`
  - `gem_flawless.png` `bb9c1042176e195354e64a9525f4d1499f553ae065c2faeb07a0af34f75cbd8c`
  - `gem_legendary.png` `9cb2f4d01a586e12c4521d011e1b915125c85813163dbfe7075dacc8c825f0fc`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `ore_raw.png` `b56dbf21549efa408d90db7e3f4b06ded5c44c6014858c435de5fd2ef201e313`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `9da57956621ace07f7a2dde55c54ececdb5728d8d54073e35b7183635f10e14a`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_pickaxe_gem.png` `fd096fba72eb2408321ba9b714390e383153cf5f356a8c38441656da5c2ab321`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
- `SHINY` -> `gt6/textures/item/material_sets/shiny/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `da34f75bb9994e5f008b42c4120edc4a2d0c0910c5d119be34cc23e400400751`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `d143eb60c51be9d34d323fd61e61f4b105cc2c6ceeb43a4b756ffb9618c5110b`
  - `dust_tiny.png` `ce93376df3327c62f934b9c4979f4236d28c4e9c402ca82621ecd32bfc247346`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `a74668c6883500747aef771610491866c140a3a57f712e0a7467344881dc584a`
  - `gear_gt_small.png` `24330bfa328023399297a05570ff3571668cac71570815df8a0748989d1db866`
  - `gem.png` `431e102c8bad21809c9e5260497a03ef7aa129f0cabbcd8993827ee0b66c71b8`
  - `ingot.png` `48c6cd3ffb2570da277a798fc58977418a2737a124a69627cbbf88dcb135b74c`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `lens.png` `48509535120e8bd267dbde4ce63f9bbda7b6f1c69597ba16152c677be8cec3e7`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `88912895ac085ce25f9024e58865d80d2106ce891a5389a4b238efe47aee7d41`
  - `ore_raw.png` `9fe9a179bf260a13375748c49d8ba15b08551f41513c0c4ff4798d8fde3bc469`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `5c4fe86b5fa7a3a443039bf15e220245a54c12071d81bd80b197485ef0db4cd2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_gem.png` `a0d975e14d4db36578d8ab57e6e5bf29ac884ca2bcf54af263968fabd6435478`
  - `plate_gem_tiny.png` `ff92226cfb485d4bd4dc2a9365d3cda5618cd2f4b322f78efe99a7c0bd394c6e`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `rail_gt.png` `6dd3ecb92744218a440b86eda678fb7cdb9e5d7862defc3920229af16ce40049`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `a62d4e9e8782a8df1b6d7a44ff6439d0b831697f14edb4ac3822113004287a7e`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `f75985abc55969a7976caf05985edb01765a522728569c963dd397e8feed165e`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ce17dcf541ef55c530107ad7ae4c57be9cfa92f21559405c36bc95b6dd5d6e22`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `b13c2a573fdc9a3e805e271ade0f4341cd121fa30b21d92efc2a2a25bf32e6ec`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `bf3981d8bdacab09a3ae298abfbec1da55aa2d87ac9717b77a4844d5f8ce793e`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `SPACE` -> `gt6/textures/item/material_sets/space/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `48348804354d5eeab7046ef60b63d1a7481785a09792f37e92e09015bff414c2`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `dfa6ae381743bc43947869dd56325b315c08f38f2228b7dba5f848e74d8696e3`
  - `dust_tiny.png` `8d4a526a3a2a64abd4b58dbc32501fe5827a5ab5175275f19469e7db0d24530e`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `637246406671be7ec500b8d4bda36e5fc8ba4d7662d6df33a496404fb3148f1b`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `67d090abbb60bae7215e6c42e3ff7e36f9f099842ee6ebaae476134bcd299ee8`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `35cb46c66b84758bc2dec56402f6e2456d518da54b2b8c63235d697c81c3f83a`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `5c4fe86b5fa7a3a443039bf15e220245a54c12071d81bd80b197485ef0db4cd2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `990fca0d0d25bca7a571b6dad656570a465336640dc89ff5efc2d6bf4d416ab4`
  - `plate_double.png` `dbec350c1f69fa0856e81355525b48253b5ad60bc72013911a381c33fff99364`
  - `plate_quadruple.png` `aabf9fe78e3702ce0dd3ec62e37f494a3c4cad55d2896b5d81305247494633a7`
  - `plate_quintuple.png` `477abb3631ecc8f9ce60f489834afe1fc5b82a4548626a6858fdc44ed8d7b1ca`
  - `plate_tiny.png` `bbe1d942f92a3d3d1f1d55c5256593c64daae9631e610bddadcee249dc205e9f`
  - `plate_triple.png` `178b189f52774ed5d0b39ef3bfcf4baeae12674abda713474c347d0522bbc67c`
  - `rail_gt.png` `6dd3ecb92744218a440b86eda678fb7cdb9e5d7862defc3920229af16ce40049`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `c958481ea42a7acfe5c164db8213b824c38133529c657d7c2ee2c503fac72860`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`
- `STONE` -> `gt6/textures/item/material_sets/stone/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `crushed.png` `8ac9a306d27947001fa4eaa004693b8720580e5eb7a038378eb0e5c0d94b7bd2`
  - `crushed_centrifuged.png` `1331649cbf52d61b292bedb7b36859a0f73b2c357ff9b9d119956c49b0e49a39`
  - `crushed_centrifuged_tiny.png` `f80c666be49cab3ef9f905080303c0b9c79816365307db7f4a227f343bf1e714`
  - `crushed_purified.png` `2604562599b074fb556ae25c4452894815f43815a550eb8b7873cb07709a2614`
  - `crushed_purified_tiny.png` `e34207b9c264c1226cf9f0b2c3e65d01fb61c84dd01104605420308fd6bed70e`
  - `crushed_tiny.png` `d223429ccf19af6bd7264a3273b6e886c96ad8d8ed2c8e20796ae6ad22230828`
  - `dust.png` `2dde58a86ce32a463561992f9e702864435eab9382d448cad088147bad517882`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `f64458a5f51d2242cac983a986910f3fcb7cace1cfc87d76f6ba7e31c0598e49`
  - `dust_tiny.png` `22b40798f41a685cc4ba57757093ffbc98a8f7c1870441d0fa23e251599b8587`
  - `gear_gt.png` `75e1cc4b513377571ee9a7d9f258471894600fc2cc2de46b3dd9f2fd50eccb05`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ore_raw.png` `fa5d1f0b11cc2ca856a20e46c70272091170b171bb80a7c5ffe0c0e6243f6138`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `48f613bd536e45008e5311599410d27f672ac2a7b8339c29b923d02064d75874`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `95dbae416dfb756c10b94a2403417ca4c9ce07c7cd9895197a2e82246ec63d78`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `061d327ca0edd260bf5ffe558fe5c45ee06832204dbae41b40b50fd4501833b7`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `stick.png` `63345f9324d024e6ba7463f5f31af8ebd78c06a8f8e3bee8280e98c0a4bf3c2c`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `ab35c02fe037b01770948d18632fcdf0d8e3380da3ee936a713e6f7fb9286a68`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
- `WOOD` -> `gt6/textures/item/material_sets/wood/`:
  - `arrow_gt_plastic.png` `ed479411dd270dedb4e8e6bab757306fcdd821e255f5644f39573fca6b79b3f4`
  - `arrow_gt_wood.png` `f2ae44101978ce2e482887134a3d97add11917eec704023d74f7f3db76282b24`
  - `billet.png` `0f5de87e89ecf6de486f0cd1a9a9411f87c8cac6d55d4d7f9d8939a793a5ac5a`
  - `bolt.png` `7c59f0f89d0fcbb2a37b56af66b60ec61cd5e75504b452be5ff75d0ae2d640b4`
  - `bullet_gt_large.png` `7654d3752528a5d55792d35d8ac80a2c3154a4953f912a32c03001bf6eb2fc2e`
  - `bullet_gt_medium.png` `e65c202f6b05ceb3019ab9c93f52296e3afdfde990c6023c5f24746d76c0e86c`
  - `bullet_gt_small.png` `305369659f75d0da43a63a6f962744960d1507ffc25cbeb0aa7be49b5873651d`
  - `casing_small.png` `3af9b8552056ede4202c4089caacfe262873eae3572dd8d83b286c503e8186ba`
  - `chain.png` `55154c2cdb1fc059fd08d804dd6ce6abfea8e6df53d12d37bba5628f8b1ebbc3`
  - `chemtube.png` `a871ab186ca15905e824bcb00821f90b0548564884438a898a074ed9b6a7418e`
  - `chunk_gt.png` `da40c45e525d04d396f9b30b728a8cc1d502c14853f66cb0ccd856275366892e`
  - `dust.png` `d731c116dd507deaddf591e2af296f26fc7eba8b75ef32edabf7354f2ce8dbb1`
  - `dust_div72.png` `c7987df9df336a602bfa97d75886366a712cc20b541d98c1fe497391f4043d54`
  - `dust_small.png` `c17448b39767d59b677da1bd8dc92a1185ffff6441d2353c5e43cedb8034415d`
  - `dust_tiny.png` `331eca30e8d5f550662e77fb7faaadba249e9e2442f585bbcf5f465e2bf5379c`
  - `foil.png` `8b60af9215ae3f976e63584bd689daf335ed2de1c79c97311d8f2ad59da85cd4`
  - `gear_gt.png` `1cdba4d6ef4b0e09935a4b99d80288b17761d9057931bc239e4d939dfb2a450d`
  - `gear_gt_small.png` `87b9d8898cefbe5167a3e31b7d872fea2302196c5915ce36801949a14b05147f`
  - `ingot.png` `cf1946ef82b0c88e14d06cf68c37bdcc3b4ac489b9b6276e81cdc83d23127422`
  - `ingot_double.png` `7df121675f9151876eefa0325eb7b1124018f59b22e17bf3c854e6d142b1d7c7`
  - `ingot_hot.png` `048d18835905c837b0f07a5db0382ee1d16ba155364ae45870d230d254c7b230`
  - `ingot_quadruple.png` `269ad5b12f171838f7b0a664f8c2cb459e681ddd237a8080338f16d151acd472`
  - `ingot_quintuple.png` `bed023b3551338841d93c17acb274106252f7095258c6981b514138d415552f9`
  - `ingot_triple.png` `cb2ae4831fb7626a12c27b205c86e280489127572360af00f94e660624011f55`
  - `minecart_wheels.png` `96d58c6f0d0de08d074f5463b0c2a7e560dc068a21188d9a6535041c8201eab6`
  - `nugget.png` `90e3d52f97d1f4d77af40529eb7530e1b4f73eb3be6e0fac66f40a330dda603f`
  - `plant_gt_berry.png` `245aca41c0896d13fd7eb754ecc72955f31eeeb715714ee957a5fe48c29f8684`
  - `plant_gt_blossom.png` `c6be772b065fc9260f3af8f203bae73a68b532fb91df49a322e472ea8fe33b7a`
  - `plant_gt_fiber.png` `35a161de8fd124d57f21ad8589a2daac128b5ff198b1b775a6fd42d59725e05d`
  - `plant_gt_twig.png` `9bbbe2894e9d5636b312cdc1b5a831be594a199281576f7973f531da584e2da6`
  - `plant_gt_wart.png` `5507e5aea6d8ce2db29affd12eaf76c3383fa6b0a0354ad0082258add0c95e62`
  - `plate.png` `8e27e88843765b2f830e2602c5a1e66cc1618a08bf1d80d9de946f83560f93c2`
  - `plate_curved.png` `47ed8bc95c50dd698608390667a8a258a0c2b81f697a7422662ac78c29efbf0e`
  - `plate_dense.png` `61c3fb30aaf44f2174978aaada247e38d765a8ef6a5b01058911be2cc8bc57d6`
  - `plate_double.png` `49ccdf70e2af8dcaf9c6389e030bc2fc8af6c99ea9324d4f5f1086cf9410d868`
  - `plate_quadruple.png` `68b620f80b28c6b4c174daad53d4abae7b3fb17f0b53ecb7b63178a364a84321`
  - `plate_quintuple.png` `66bb3e28ca3bb2499e20e213539aac0479b249e10ee1ace2ebc8c9309acdbe3d`
  - `plate_tiny.png` `c44b7210bd79d511a3a8b1af52503a35e7315aa8ffbb6002a95e60bc0b932b18`
  - `plate_triple.png` `f7d33ce8c74e21137e2f1f5ea5cdb8ca01ebc00f651b4de95adeb34317d8c327`
  - `ring.png` `3871af090b423efd57bbdc0198a616bd374d7163ee0e50f4de02c0f6c40407bb`
  - `rock_gt.png` `fcf6953e34b4fc3a7f0b6c95fde43adaac60b26bb6040902b781afe0347f4f48`
  - `rotor.png` `119aa906e0e98fafc92e102fd7ab6f034c68f4c89c1b93059067812c5487dd41`
  - `round.png` `24c6c7a85ec5589aaf36ec50c0ed4924e687015a913b8187691803574440c5a2`
  - `scrap_gt.png` `76fd1aacea41f68883c4bc147ba788cb174f9f2a24a11fea437b70057eddb7fc`
  - `screw.png` `94e94ed6ead3d8353caaa0c293ff3269213d677794c1b94c87f57f9dc119311d`
  - `spring.png` `ad0febaf3b9c58f8175d55bc4a3e690ccd633850ffbf20001235a0f037ab77e5`
  - `spring_small.png` `9c518889f3d38c186c664d8b9734b5545c8c1107b4db793089cfbf23432bb0e8`
  - `stick.png` `ee2306110d3da12e5340607760cbefaf8d17b047a84d6202780c735dc74dc85d`
  - `stick_long.png` `44e5500af3a30267caeefa2109110687f9e051986b9d13f47542501bca923642`
  - `tool_head_arrow.png` `25fac85dbd3efa407a730923a1dc9b64443533b230cd67c6b011490eaa940859`
  - `tool_head_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_builderwand.png` `4f3d7f68653d508e974da81beb1bd499d5c3ba0f0a588c4e4ddcddcdf6181c18`
  - `tool_head_buzz_saw.png` `c60605c74d91a6f768b61eac6a408ebc84a712525e66f5bd21148e3b59a12d1f`
  - `tool_head_chainsaw.png` `d1452fb9dda15ab2d6c87f26af3e6b8e4bd66f35367b127d5acbf842e7cf98f8`
  - `tool_head_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_construction_pickaxe.png` `b60e5c108580077c026973e9012f73dfd1b2c2a945dbc4b26289c8ff4fcdcf1b`
  - `tool_head_drill.png` `e82f5f3a62b078ecf9f32800338ab69bf00e6c0dc2f3488f9bc06aa411eeeabb`
  - `tool_head_file.png` `cae4602dd96749d326a003279047d23f7f0aa5d55db5b17a46049dbf7fec3d7f`
  - `tool_head_hammer.png` `94da2a0305f36b5bcad45b9662739a3e98884196d335362e29bc56598fe5a2f7`
  - `tool_head_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_arrow.png` `25fac85dbd3efa407a730923a1dc9b64443533b230cd67c6b011490eaa940859`
  - `tool_head_raw_axe.png` `99e56967c6cd7d1fa85f8a0bed3bc84beebb9ce625b645fb0b259d7171b49609`
  - `tool_head_raw_axe_double.png` `445e3fe192471304766d72e86ae1c68251efa97b9a056eeb2f2b9fc0dd5599c3`
  - `tool_head_raw_chisel.png` `0b46d47994d97976427a379b66d7f179a6be13ab44cd6d54e1e646851ec6873d`
  - `tool_head_raw_hoe.png` `c69a257a61301d60bb78712f8ad9757bd80f2acf5394047b048f29ec1ee30264`
  - `tool_head_raw_pickaxe.png` `055819046fa2a7b05f5adace72149d688ba0f4731dab3a65396565158aef453b`
  - `tool_head_raw_plow.png` `634d08e03b578cae60749935bbca52fd228e2f39154ac5fae11fe8536866041a`
  - `tool_head_raw_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_raw_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_raw_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_raw_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_raw_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_raw_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_saw.png` `8b940b68be8492d5e32831d7bcfdbb041d35f4dfa66df8de151cc6dc4cdda76f`
  - `tool_head_screwdriver.png` `4dbe6513b63bc6442307d632b288985b0be2d6be3799d4573d0c7b820a32634a`
  - `tool_head_sense.png` `648f0c302713e00436a875b395c2e8b16aae6afb571b6f7a1dce0962e11e825e`
  - `tool_head_shovel.png` `3084d27881c6661c0e53e7dea99886347c3040b7cdec79ba1288db70cc077c39`
  - `tool_head_spade.png` `f26b3c80c74ca10a09d2c69e468ad18b913f61448374a7d9cd364711cd58501d`
  - `tool_head_sword.png` `8c6e5a55e11f06aa354bbd5514d23565bfde45193bad836254b8b03f0695f81b`
  - `tool_head_universal_spade.png` `e6ba243503560723f026c846555f000d7341419a2e0bc175d38e918458f72724`
  - `tool_head_wrench.png` `8d5a94bccbdc5594c9a1b4bc85e276597a4899f3163580c9cb9867c5d8bfced7`
  - `wire_fine.png` `448903897a3949381b86f0887b65636cd4bfbb71ef76151d7ee7e0abc88b40cd`

Spray-can item textures, task p22-spraycan-items: the 18 16x16 icons under
`gt6/textures/item/spray/` come from upstream
`src/main/resources/assets/gregtech/textures/items/gt.multiitem.randomtools/`
(meta ids on the MultiItemRandomTools meta item, the MultiItemRandomTools.java:243
`:243 "Spray Paint ("+DYE_NAMES[i]+")"` / `:269 "Paint Removal Spray"` / `:235 "Empty
Spray Can"` rows), sha256 verified byte-identical, filenames re-derived per port item id
(`spray_paint_<dye>` = meta `1000+2i` in DYE_NAMES order, `spray_paint_remover` = meta
1096, `spray_can_empty` = meta 999). Pixel-verified: the meta `1000+2i` icon order
matches `DYE_NAMES[i]` (red spot on 1002=Red, green on 1004=Green, ... white/black body
tones on 1030/1000). Upstream license CC0 (see above).

- `paint_black.png` (1000) `288fb7cac6003aa9a9ba089d904799d294bdc19c940dce7f2e4d60975ff5e641`
- `paint_red.png` (1002) `7e66ca032500cfecc7ce3218b61653bca824b8fa1669a3aa2a3413dd4d823d84`
- `paint_green.png` (1004) `733f04e4eacd196ee863bb8842bbe9bb04df3801abe8831c3f862b58449d0cc5`
- `paint_brown.png` (1006) `235677b37d420ed909f35add97ff5689a91750430150d612ab805b5dbd0f841d`
- `paint_blue.png` (1008) `e95bbc146c1ab03968155c1e095e991909933f378f97e093c7e13da874b23243`
- `paint_purple.png` (1010) `6e5a2c001370abe1a8ac618cba835f55a1bf70e9e2ea3ee49c0515a36fdb645b`
- `paint_cyan.png` (1012) `1a4fa0663f77ab26f11f408b25df0d7409b26bb5c9237107115c6d96e709352d`
- `paint_light_gray.png` (1014) `145308c38d63ffb2ec30edda782f5caac5de2e26579ffca5bcdee25899fab979`
- `paint_gray.png` (1016) `1b3c0c8800e102655fa08ffb1d05a4976a226fca9d515ba379a89e3a87ed94d5`
- `paint_pink.png` (1018) `4a661d2f8dcf8b34a349e56106aa45e4d3fa8f86b791e38915f5e6c759f56e97`
- `paint_lime.png` (1020) `f554198029fb201e9b4ad9d844c9427fcb005a5911111d1f48d3884518e516b0`
- `paint_yellow.png` (1022) `f032f8f9d0d143bd6fd69830bc9469f32b70178112af3354d713d7f783969183`
- `paint_light_blue.png` (1024) `19596f95b110f1da3bf98f85efcaa0e89b764f1088c72c918182d67f9235716e`
- `paint_magenta.png` (1026) `5a11c6b9f294863336ea1849c6db2ec7148a3c08334faf55fb7a398d324976c1`
- `paint_orange.png` (1028) `9dd0a74dd85fb6d29dfea91dcf2d8a7cf58682592a7109d2c52bebb69fb390e4`
- `paint_white.png` (1030) `679dc1a016ca7d9aed9c31e30035f6ac8d1162e54aeda566c25467cd98814a8a`
- `remover.png` (1096) `e7abadec9a0165a34204abbc6d16654ad93bdb48d6552d990280bd40a686f9a3`
- `empty.png` (999) `b2efa1cdc63efa06f3677bc165f1bb51774b262881e4170927d3df48f5efdbe1`

Copied on 2026-09-07 (task p22-spraycan-items).

Dye-chemical fluid carrier texture, task p24-dye-chemical-fluids: ONE grayscale
tint-carrier borrowed from upstream
`src/main/resources/assets/gregtech/textures/blocks/fluids/dyes.chemical.png`,
byte-identical copy at `gt6/textures/block/fluids/dyes_chemical.png`, sha256
`41deac6c1edb079155918ef549b44203a5c5520b3af2cd4d66c4bc11dd43d1fb`. Upstream is the
single-texture shared still=flow form (Loader_Fluids.java:115-117, one
`tDyeChemical` IIconContainer for all 16 `dye.chemical.*` rows); the 16x512 strip is
the 32-frame animation (upstream `.mcmeta` frametime 2, borrowed byte-identical as
`dyes_chemical.png.mcmeta`, sha256
`20c309d9ff4175bf0f04b4c57efbd58fa1915f1c69907abd3bd91d6d01b70138`). The per-colour
identity rides the runtime FluidType tint = `GTSprayCanItem.DYES_INT[i]` (CS.java:470)
— zero new colour data, the same single-colour-source discipline as the spray-can
items (P22) and the machine paint tints (P21). Chlorine borrows NOTHING (vanilla water
textures + the MT.java:405 material colour tint, ruling R3). Copied on 2026-09-07.

C-Foam fluid carrier texture, task p26-c-foam-fluid-refill: ONE grayscale tint-carrier
borrowed from upstream
`src/main/resources/assets/gregtech/textures/blocks/fluids/cfoam.png`, byte-identical
copy at `gt6/textures/block/fluids/cfoam.png`, sha256
`05c5eacf3a77dbbc69853fdf528bb39d7ed1fc79116bd959f8d4935590429257`. Upstream is the
single-texture shared still=flow form (Loader_Fluids.java:118, one `tDyedCFoam`
IIconContainer for all 32 `cfoam.*` rows); the 16x512 strip is the 32-frame animation
(upstream `.mcmeta` frametime 2, borrowed byte-identical as `cfoam.png.mcmeta`, sha256
`20c309d9ff4175bf0f04b4c57efbd58fa1915f1c69907abd3bd91d6d01b70138`). The per-colour
identity rides the runtime FluidType tint = `GTSprayCanItem.DYES_INT[i]` (CS.java:470)
for the 32 family rows; the base `gt6:cfoam` row rides the carrier untinted (the
construction-foam grey IS the base look, the decisions.p26-cfoam-fluid-naming ruling).
Copied on 2026-09-08.

GT grass block family textures, task p24-grass-block: TWELVE 16x16 RGBA PNGs borrowed
byte-identical from upstream
`src/main/resources/assets/gregtech/textures/blocks/iconsets/`, renamed to the
VARIANT-SEMANTICS tail. The upstream FILE names are the trap (research pin: meta 3
"LightGray" renders the NORMAL PNG, meta 0 "Green" the MEDIUM PNG, Textures.java
:530-565) — the borrow followed the CODE mapping, so each pair below is named for the
variant it actually serves:

- `gt6/textures/block/grass/top_green.png` (upstream `GRASSBLOCK_TOP_MEDIUM.png`, meta 0
  Green) sha256 `ca41b36c194284e765d5ee74e3fa49a4e64d919dd231b0aadc87481346713d44`
- `gt6/textures/block/grass/side_green.png` (upstream `GRASSBLOCK_SIDE_MEDIUM.png`) sha256 `9fac0d91238732fb9fa34bae2ea401546a4a8aba6fe267f00812f11e10617e0c`
- `gt6/textures/block/grass/top_lime.png` (upstream `GRASSBLOCK_TOP_LIGHT.png`, meta 1
  Lime) sha256 `fc0acede35295b3e304814dedae7faad948d218adb16a4a88c31bd64d7516ed7`
- `gt6/textures/block/grass/side_lime.png` (upstream `GRASSBLOCK_SIDE_LIGHT.png`) sha256 `8262c70dfe9685682ce2e51890b960528b504cd7447c105bbacc5b4df9246f2a`
- `gt6/textures/block/grass/top_black.png` (upstream `GRASSBLOCK_TOP_DARK.png`, meta 2
  Black) sha256 `477608e080f2da52680ba9a0479519af7541500bfb79c5600d066057793a5e2a`
- `gt6/textures/block/grass/side_black.png` (upstream `GRASSBLOCK_SIDE_DARK.png`) sha256 `76d54b8cd36effb5e57f2bb9be067e2b64622dc542b170fc205811ee146ed2b8`
- `gt6/textures/block/grass/top_light_gray.png` (upstream `GRASSBLOCK_TOP_NORMAL.png`,
  meta 3 LightGray — the misnomer pair) sha256 `51f405ddedd1d4d73a43f75690ca8b151c2dd4a680bcf9774fe8656d194066be`
- `gt6/textures/block/grass/side_light_gray.png` (upstream `GRASSBLOCK_SIDE_NORMAL.png`) sha256 `2c9436e8152ccf2902ec72cbefae73720c9360c09591c9e6218adf30d2709167`
- `gt6/textures/block/grass/top_yellow.png` (upstream `GRASSBLOCK_TOP_YELLOW.png`, meta 4
  Yellow) sha256 `438e35fd72e0882beb17fb79e85bc5413ecfc19e469627574aa23fa723252762`
- `gt6/textures/block/grass/side_yellow.png` (upstream `GRASSBLOCK_SIDE_YELLOW.png`) sha256 `634158d62e035b39de0dad8ec8867e55e12a335bcd07b978a71a4e239f23a191`
- `gt6/textures/block/grass/top_brown.png` (upstream `GRASSBLOCK_TOP_BROWN.png`, meta 5
  Brown) sha256 `63368030541c97bf7cc00dee716f21487600dacfe427d930655a534841186577`
- `gt6/textures/block/grass/side_brown.png` (upstream `GRASSBLOCK_SIDE_BROWN.png`) sha256 `3c785319e73d56cb3b0fec7b24eb74d835eb8b7d16860f99abb10340797b252a`

The bottom face borrows NOTHING (the models reference the vanilla
`minecraft:block/dirt` texture directly — the upstream
`IconContainerCopied(Blocks.dirt, 0, SIDE_BOTTOM)` semantics, BlockGrass.java:102-104).
The PNGs are pre-coloured (zero biome tint, zero tintindex). Copied on 2026-09-07.

Lightning Rod family block textures, task p24-lightning-rod: the four faces
borrowed from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/`, snapshot
`v6.17.06-22-g3703e4030`, copied on 2026-09-08:

- `gt6/textures/block/lightningrod/wall.png` — the Tungsten Wall part (upstream
  `multiblockparts/metalwall/0/colored/side.png`, the Loader :1151 texture key
  "metalwall"; sha256
  `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`)
- `gt6/textures/block/lightningrod/coil.png` — the Large Niobium-Titanium Coil part
  (upstream `multiblockparts/coil/0/colored/side.png`, the :1168 texture key "coil";
  sha256
  `6e4ab5f32dc13a15c65c700bbeafaa683927b587c4c9bfc772d49b79cf7021cb`)
- `gt6/textures/block/lightningrod/rod.png` — the Lightning Rod pillar part (upstream
  `multiblockparts/lightningrod/0/colored/side.png`, the :1179 texture key
  "lightningrod"; sha256
  `a6b988c32e964fcafeb425c3f4a409f028102d7b75597f62bc97de166dd3cb54`)
- `gt6/textures/block/lightningrod/main.png` — the controller face (upstream
  `multiblockmains/lightningrod/colored/*` alpha-over `overlay_front/*`, composited at
  borrow time; all three upstream faces composite to the SAME visible pixels so one
  texture serves all six cube faces; the overlay decal is what visually separates the
  controller from the plain tungsten wall — the two colored layers are byte-identical
  37dab1b9; composited sha256
  `08391c4ebde4d220ec35774751a496475ac02b1b117b0e22f19bffa316acac39`).

The "colored" groups are grayscale tint-carriers (the ANY.W tungsten tint is the gray
itself); the port shows them un-tinted — the same declared deviation as the crank
(task p12-engine-crank) and the tool family.

C-Foam spray item textures, task p25-c-foam-pipe-spray: the 32 16x16 icons under
`gt6/textures/item/spray/` (`foam_<dye>.png` / `foam_owned_<dye>.png`) come from the same
upstream `gt.multiitem.randomtools/` directory (the MultiItemRandomTools.java:251-264
"C-Foam Spray"/"Advanced C-Foam Spray" rows: `foam_<dye>` = meta `1100+2i`, `foam_owned_<dye>`
= meta `1132+2i`, both in DYE_NAMES order). Pixel-verified: the colour tints follow
DYE_NAMES[i] on both ladders (red spot on 1102/1134, green on 1104/1136, blue on
1108/1140, white brightest on 1130; the owned ladder is the darker blue-bodied can).
Upstream license CC0 (see above).

- `foam_black.png` (1100) `00229a94769e646f763449be382fcf4ef106426bac56fff121bfa8cb06ce8859`
- `foam_owned_black.png` (1132) `32ab61ad28f0948132d2ac1b5cc685b6e7dc8bed988d0284a59d7e05e2d89868`
- `foam_red.png` (1102) `5139fcf8b217758dc9fb19d304fe65b13d633d8cc7a1d1ff092068f23f177600`
- `foam_owned_red.png` (1134) `10807c357f9196efd94b51969d2e12c8e5156501583042fca594c5a32f813771`
- `foam_green.png` (1104) `bd384054abe96fad3c43c58607fbcaebb9a4ade65b74e4f3cc7b916595110c8b`
- `foam_owned_green.png` (1136) `24ded1a7f5614a8052afb948eeb761110e3d259fee014f989afda1766e344c7f`
- `foam_brown.png` (1106) `3bc1b6191ed002896dfa2784c2a95d4430ad2d6fd97e6d5bdb3e4e68107d22e4`
- `foam_owned_brown.png` (1138) `359635c717ece2008310967600425ffcdb95850d89c0e6d88f872a563ba739d2`
- `foam_blue.png` (1108) `a513f0c366d96c873f00a72954f7cfb5654f3f2f9a9f602c7d9896888e7acdb7`
- `foam_owned_blue.png` (1140) `c4c62c4735a636001c3fdc9bcc81d368fc5317c39fc83f7b37cfb4179461cd3a`
- `foam_purple.png` (1110) `8f1379e822af9de50d99d06093ed9eda1ad6a717bdb65571b20c7e2a3311ab90`
- `foam_owned_purple.png` (1142) `e3e62ff8fa52fa756c8df1ac9b1a0d0d4c8a003bfa7af8c4eeca5c5bfd31f41e`
- `foam_cyan.png` (1112) `a8dff4a4166ecb508dee4b2be0ad92605a7ddafbc4aa467367dc52d2fdd2ace3`
- `foam_owned_cyan.png` (1144) `b979127c3cd87aa404a96ac629a72607d5477ce6b0d0acca87226df927ea1e9b`
- `foam_light_gray.png` (1114) `b5d3be331f3cecbe42a5839d8fe2a3ada717bc7c8c555e55d0753acd30b79407`
- `foam_owned_light_gray.png` (1146) `a60fc8d102f86c02f8727f473fa87ab4fbffc7e90667acb651a3097a867e0088`
- `foam_gray.png` (1116) `e45f87a9338245fcd1dfcfcc0c4c0b68fb6cd6798547c0e7ca598d849eef49cf`
- `foam_owned_gray.png` (1148) `a37d8255f182999031cc73b2ecd084c7421c541778631491e092c33665e2f4ce`
- `foam_pink.png` (1118) `74a3d93d7d6a37824077a80aec2aa2afb068ef9b1a227eaa472828ca4a979613`
- `foam_owned_pink.png` (1150) `1a70348ad8d2301cd64df8a0ef283022df236172624a2caa6e80db4ef9b3aa35`
- `foam_lime.png` (1120) `00fbaccfa2a106b9685934dea715d0647b06eea85fd7437813209c438c368928`
- `foam_owned_lime.png` (1152) `1fa164f7fa1356ed22012e7bc08b2031f80d06bd1a449221c2481a983990da86`
- `foam_yellow.png` (1122) `0bbfeca0886ca497e471581245c0ece0cc589a60a8babd4424b2b547e9b318cd`
- `foam_owned_yellow.png` (1154) `aa60b805619d3ba9eb4b5b7cc8fa8adfce22cf1c6a2989a73db0092577123df3`
- `foam_light_blue.png` (1124) `aa87552d12d3124c13151064afae3ff702b4c48a5a6f14c51ad2ff17bbc43904`
- `foam_owned_light_blue.png` (1156) `331595e0b8756eee648e1ea8ca9461150db4cf766a86265493079b7913689693`
- `foam_magenta.png` (1126) `8f740064c38edec17a7bdc6d9aed88a30ca7630453f4f9f4614065989148d504`
- `foam_owned_magenta.png` (1158) `533cd05e1c8c62e000742938618f7f51eab4d27183e4c4ececded3c7f4eb2d10`
- `foam_orange.png` (1128) `aab83686ab63b4103cc8a4cbc25ccc270d7d034cf2a365de6a664e0397d3883d`
- `foam_owned_orange.png` (1160) `098f00e92ba293728a041b4ca48264d7772ca6e4a28daf784d7b2efd628df88d`
- `foam_white.png` (1130) `15fe6c3e22d845a3e41f622e5b18c2ec87354484243f3b1a26caec0104256cb3`
- `foam_owned_white.png` (1162) `29f55f0e2b67541b1f8d25289573484a34ddcf64e4449bba468f7d67c89c6006`

W1 Kinetic trio machine textures, task p26-w1-sifter-compressor-wiremill: the
Sifter / Compressor / Wiremill family sets (15 PNG). The block fronts land via
`mdk/tools/bake_machine_fronts.py --split-fronts --machine basicmachines/{sifter,
compressor,wiremill}` over the same upstream snapshot (`v6.17.06-22-g3703e4030`,
upstream `textures/blocks/machines/basicmachines/<machine>/`): `{machine}_colored_
front` / `{machine}_overlay_front` / `{machine}_overlay_front_running` are byte
copies of the upstream `colored|overlay|overlay_running/front.png`; the
`overlay_active` strips (sifter 4 frames, compressor 9, wiremill 10/2) are cropped
to their FRAME 0 16x16 representative and re-encoded — the P20 "animation retired"
declared deviation carried over verbatim. The three GUI backgrounds are byte
copies of the upstream `gui/machines/` files, lowercased (the 1.20.1
ResourceLocation charset convention); no GUI ships them while the rows carry
menu=null — they serve the recipe-map `mGUIPath` string and the future GUI pool
card. Upstream license CC0 (see above).

- `gui/machines/sifter.png` (upstream `Sifter.png`) `742981361d6ecead6306dfcf457a3f139961b3da2ef538cd2d06384c38bab44a`
- `gui/machines/compressor.png` (upstream `Compressor.png`) `cb5c612a308385724ea8724419e9eeae4105744c7b06fae6b1deb53ffe86a80a`
- `gui/machines/wiremill.png` (upstream `Wiremill.png`) `a0108deb57cb5da42b9fa1f3e9104cd41744dfcebde8ea928001f1f7ad249581`
- `block/sifter_colored_front.png` `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `block/sifter_overlay_front.png` `f736601956519d68b411807353cb25a200db9a190a45e9f5c6c920191f9743bc`
- `block/sifter_overlay_front_active.png` (FRAME 0 of 4) `1a0a8dee55f0fe6e20432affc75b82892270c4f0a7b08dcdf39bf71fea10f315`
- `block/sifter_overlay_front_running.png` (FRAME 0 of 4) `097a175a6e3de977218a128683fe99be7695f82ffccd3cb31b03b735173c79f5`
- `block/compressor_colored_front.png` `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `block/compressor_overlay_front.png` `9b0af1a60316b9b150a2c793bd3a942674cecfd86ee1ce60d58a81c5af6ae1d6`
- `block/compressor_overlay_front_active.png` (FRAME 0 of 9) `a16f79ca4ef725116fac391ab7ff41532cec77510ee05a6cd540214f8e601fe9`
- `block/compressor_overlay_front_running.png` (FRAME 0 of 9) `be94d0bd1de231456ec56eb57f9586325cd0bc69d4e8b8f928f039dcb423569c`
- `block/wiremill_colored_front.png` `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `block/wiremill_overlay_front.png` `12172025f9f74ef92ac3de2b46af0bdfb60024700ad3fe55602ceb1b85e8ca25`
- `block/wiremill_overlay_front_active.png` (FRAME 0 of 10) `f69cdb02d3fdc1d8a7ea11d678964b42e6d6436e6d076c08852fef06811f3b8d`
- `block/wiremill_overlay_front_running.png` (FRAME 0 of 2) `88be632c759d7cfee9a881d6efdaca871a58351ba6407e4c2f3d4e9dc7964453`

Sensor block textures borrowed from **GregTech 6**
(https://github.com/GregTech6/gregtech6), snapshot
`v6.17.06-22-g3703e4030`, files
`src/main/resources/assets/gregtech/textures/blocks/machines/redstone/sensors/<name>/` —
task p26-sensors-core. Each port texture is the upstream `colored/front.png`
(16x16 plate) with the sensor's own `overlay/front.png` (64x64 digit-strip,
box-downscaled 4x, straight-alpha) baked src-over into ONE 16x16 canonical PNG
(the p19 distillery-front bake precedent); upstream renders the two layers
separately and tints the colored layer with the material colour — the port
renders the baked result un-tinted on all six faces of the oriented cube
(declared deviation, the thin-plate + live-digit render stack
(MultiTileEntitySensor.java:143-261) is the render pool):

- `gt6/textures/block/progressmeter.png` `c11390a9093b09f43680ba324d18e97f0bc0925968ae921bb6a0e2b19c3e5a3f`
- `gt6/textures/block/fluidometer.png` `521fe66700236205a9fa447283260992d2865e6cee7a8fbacbdb4d54c36f944c`
- `gt6/textures/block/electrometer.png` `faf796a2de3d874b247ee98c585b1c881c97bf0a36f9bcd6b6797a0388725fd2`


GT6 item OVERLAY pass textures, task p27-tool-overlay-assets: the 2785
`gt6/textures/item/material_sets/<set>/<prefix>_overlay.png` files and the
7 `gt6/textures/item/<stem>_overlay.png` iconset overlays come from
upstream `https://github.com/GregTech6/gregtech6` snapshot `v6.17.06-22-g3703e4030`, files
`src/main/resources/assets/gregtech/textures/items/materialicons/<SET>/<Prefix>_OVERLAY.png`
and `.../iconsets/<NAME>_OVERLAY.png`, byte-identical to upstream, sha256
verified per file (manifest below). This completes the base+overlay icon
pairs: upstream registers every item icon as `<NAME>` + `<NAME>_OVERLAY`
(TextureSet.java:113-116 materialicons, Textures.java:856-859 iconsets) and
draws the overlay as the un-tinted second pass (ToolStats.java:247-265 tool
passes 1/3, PrefixItem.java:134-150 pass 1). The P20 borrows took only the
base halves and explicitly deferred the overlays (the "NOT borrowed" note in
the materialicon section above) — superseded here for the item tree. Naming
follows the P20 rules: set folder lowercased, icon segment camelCase ->
snake_case (MaterialPrefixItem.snakeCase), `_OVERLAY` -> `_overlay`; iconset
overlays take the port base stem + `_overlay` (`HANDLE_SAW_OVERLAY.png` ->
`saw_overlay.png`, paired with the byte-identical `saw.png` base).
Conditional fallback is exists() semantics: a combo whose upstream overlay is
missing is listed by the script, nothing written, nothing invented — census
2026-09-10: 2785/2785 material_sets pairs and
7/7 iconset pairs present upstream,
zero conditional fallbacks. `item/screwdriver.png` is deliberately un-paired: it is the
p24 four-layer composite (its entry above; the HANDLE_SCREWDRIVER_OVERLAY
pass already baked in), not a byte-identical base. Five of the seven iconset
overlays are fully transparent upstream (WRENCH, BENDING_CYLINDER_SMALL,
HANDLE_SAW, HANDLE_FILE, HANDLE_CHISEL — as is the un-borrowed
HANDLE_SCREWDRIVER, six transparent icons in the family); CROWBAR and
WIRE_CUTTER carry the real shadow layers.
No `.mcmeta` animations exist in the borrowed folders. No generated model
references these files yet — the C2/C3 datagen waves (tool + prefix item
models gain the overlay layers) consume them. Copied on 2026-09-10.
Upstream license: **CC0 1.0 Universal Public Domain Dedication** (same
upstream `README.md` block as above).
- iconset overlays -> `assets/gt6/textures/item/`:
  - `bending_cylinder_small_overlay.png` `f138a491eb7b6e4572a64075d57276a36470724c0b57e6f3c42da424ee31398f`
  - `chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crowbar_overlay.png` `07620fc4098e31d7236eba60a09bb9b53de01e25b361627a9845449fbf19f131`
  - `cutter_overlay.png` `f1d3662d511714a9954b42c1b2d2d7d0fc4f669abd16a7ab407c8a8ef0dd48be`
  - `file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `BRICK` -> `assets/gt6/textures/item/material_sets/brick/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `503dbaf736063cf0074b31c648e49272e6ab27927dc02b3c175c08ba581f297b`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `COPPER` -> `assets/gt6/textures/item/material_sets/copper/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `rail_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `CUBE` -> `assets/gt6/textures/item/material_sets/cube/`:
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `CUBE_SHINY` -> `assets/gt6/textures/item/material_sets/cube_shiny/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `DIAMOND` -> `assets/gt6/textures/item/material_sets/diamond/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `8b3a73552fee59f3ad7f42e3dcc3577b4aa87ae9dc309265ea91d921ccc448e4`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `519b6a07fe5d29c3590b92bd4d8b1b6bab862faff5c3654862dc212696fb89e1`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `DULL` -> `assets/gt6/textures/item/material_sets/dull/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `rail_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `EMERALD` -> `assets/gt6/textures/item/material_sets/emerald/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `FIERY` -> `assets/gt6/textures/item/material_sets/fiery/`:
  - `arrow_gt_plastic_overlay.png` `ab868563585e8fe18a9a53ae46e50a22bb6858a36df78a79c06d6c6df7c96e6d`
  - `arrow_gt_wood_overlay.png` `ae6ef5fd708dc373f8ed70fcbfe4c40599ed37f57174bc6413f29ae3afce6d88`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `2defb6282ebbc2e0d0077c147f6bfc6fdf2a97565bb841e7b592fa0df2c08248`
  - `bullet_gt_large_overlay.png` `198a24e701688bc0ead51ebb3a6ebc2a385a36352eb7dd061d3aae405f7881ef`
  - `bullet_gt_medium_overlay.png` `52bb2f76731bcc8f0784cb264fefb30deb972935ef85e2c149f12b7d8c6bca73`
  - `bullet_gt_small_overlay.png` `82cda6dfe11aab98d5fa833f9318028abb9bc08e6a165f51f91ddb8cad36c1f3`
  - `casing_small_overlay.png` `199cb01bfbb82d48764f72a3f4c2fd84b08942ffc18bde5bc3d8325d4c63f660`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6abcbe249a7e95d1cc7e952f2c78e5206e0e011c8378e19cbfb3f865c4c4078b`
  - `dust_overlay.png` `60aae90d06a81e4b9050e13705037fe1c3ee7283a85a93b7e6df3de0166514c9`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `92edc74f1e149ffdbc994c5fc064dbd8f1959f0c3564702d3282dc164c400cc1`
  - `dust_tiny_overlay.png` `34a6d87feb6f7ccb474f6a42df1637d4af6461df92146b85ca8ee4eec7ea9bb6`
  - `foil_overlay.png` `b21f5abc20093e11cf35b76f483b730765ac4fa8bc92548a046918ad7bf3a49c`
  - `gear_gt_overlay.png` `f5d03ac3c9622cb3cb6ff6260dd2a3420297b5b37226363d1d939e6270d3e0c7`
  - `gear_gt_small_overlay.png` `c643f9199966954b86a576d9cfde8d6a1ca4784bfd3cf0561a784f97328b94cf`
  - `ingot_overlay.png` `e23c95d2bfeb5bd9fb9fb71e7d9d86dbc67a824709f87e4b580aaba7d86a7007`
  - `ingot_double_overlay.png` `40bef06549ab33a815e2657e968320e911674b573b81647183e4d8e43025d65f`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `bca6ae3a49675071f457db749c9b01b1c0e75b5a0421340a824ffb5877078b7a`
  - `ingot_quintuple_overlay.png` `fde959c95bf9d86810b7ebd9228e7e5059812fe9f9849a553d4bc7833e8065c7`
  - `ingot_triple_overlay.png` `731c80e0e5319435fa1b63e62da7e7adfec14eafde6a408cb0628f3c8f4127b8`
  - `minecart_wheels_overlay.png` `a53ed2ee9d8d308422e69b4ad3662f9e24c147477cec0bda6854ffd0a454fb46`
  - `nugget_overlay.png` `c064500f4ed62dca2012a725719277d3990f7ba1fd3aa4621e5a6ab7ea105153`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `0b0911c896476b014c00f0a3e1c2a2e47fef0fe5b1e146c3e95dde3a61161124`
  - `plate_curved_overlay.png` `6f09d1a60e08d4a0ef092d2d5a064cc28a3fc653ab41869bf6755a243f444491`
  - `plate_dense_overlay.png` `70b544019b6f7bae0e89e2bf109685a5662fd885db4a442ce39c44b6bd23c17e`
  - `plate_double_overlay.png` `49fe885793b0fac68cbed51e5b5735943e3d26cda85c4582a36b13ce2fa6c045`
  - `plate_quadruple_overlay.png` `66d0f909e572b3f49772af64187f9c167ff353f6871c5fa262d2a37885e3fc3a`
  - `plate_quintuple_overlay.png` `b2b2cec3aa12bd84711b126fd45e657c8b0b6656670c6a3581e13377952e62dc`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `f9713c4883c75f4e4b59903ab8e9b05e002de69174007f1ab0b74a05c2a9791d`
  - `ring_overlay.png` `dba4fe353672e53aa9ff993d84a7e79e20510316f6cbf5ef9a08c66fd75852c3`
  - `rotor_overlay.png` `a64a746826e17c0dd67f99f6c55eec9438e3ef168b60049569ad4eaf05deabc3`
  - `round_overlay.png` `f33aa889beefd5bcc7c7dc4cb82641185d4682793c4bae98cd07287565a6cbd6`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `261525047c5f187df48449e7c3120e7f7d1a1ff86e8736f7a08ff93c2f60f4b4`
  - `spring_overlay.png` `ecc793f8373f2938ee9bcb35910fc342b1f13903455dbacaa9061b3d58e727d3`
  - `spring_small_overlay.png` `185806e3bddb88e31227a0b16185d887b5ea7d4799f0a1b150afbf3b9b694045`
  - `stick_overlay.png` `4dde22ba82fe76604c98052487c304db0a8276eb497bb8579cd0a59bb1318b60`
  - `stick_long_overlay.png` `4d104318a0db238d681e6337748fc696727cc26a0578d9ac1ef6eaa9d67add21`
  - `tool_head_arrow_overlay.png` `8617861d21faec8c2fad08e0237868b09a1d6597641b1eb44ac4c8732c7edf87`
  - `tool_head_axe_overlay.png` `c12cc4b2e49b77c9a6dead9a7d6fbf1eba01532dd45a5f15f7f56eb11b5663ac`
  - `tool_head_axe_double_overlay.png` `c58e45f8d41bd6909aae557c0f7eca919a725017e34dee21cacf9887524d2757`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `ac484f39d82c014ac23b9b79c486ba4bfcae0ea18aca5f36acc82ea23697acf8`
  - `tool_head_chainsaw_overlay.png` `10c5926fe5a299237e819caf6fa9b52459325f517fdb5261184024d696adcbff`
  - `tool_head_chisel_overlay.png` `6fe68a87526d2adf884d5d015edcbaa5362c20842194d3e6274d2e32779ae932`
  - `tool_head_construction_pickaxe_overlay.png` `b0823e2528635ba7b1ce900689ea63e3c163eb11ae235e3c27d33f28fef4a4c6`
  - `tool_head_drill_overlay.png` `050688bc26ba75cf2548dc729d81dea5f7bb1b1aa7d8b18c47070c06174a14cd`
  - `tool_head_file_overlay.png` `aa8fb5361f7d39c2d2068eb523b2e4f3c0c3c12650c291aad6b85e529f3666c5`
  - `tool_head_hammer_overlay.png` `fd332970af5cb281a7e4639bae11d7b4f5c6d312e26db0760b7e03580af3bc02`
  - `tool_head_hoe_overlay.png` `67ef3aef6a0b181b64b50c013f2c3a8f60f80e4e9959d500e95f02dcba36fd85`
  - `tool_head_pickaxe_overlay.png` `720b0630a288aae702fb630efdc9e9e28518943ea4b35b5933f0378ee983b7ca`
  - `tool_head_plow_overlay.png` `c450720b71b94d94cec32fb17ec343eafdc425dd706018792b1b988407fea2c2`
  - `tool_head_raw_arrow_overlay.png` `8617861d21faec8c2fad08e0237868b09a1d6597641b1eb44ac4c8732c7edf87`
  - `tool_head_raw_axe_overlay.png` `c12cc4b2e49b77c9a6dead9a7d6fbf1eba01532dd45a5f15f7f56eb11b5663ac`
  - `tool_head_raw_axe_double_overlay.png` `c58e45f8d41bd6909aae557c0f7eca919a725017e34dee21cacf9887524d2757`
  - `tool_head_raw_chisel_overlay.png` `6fe68a87526d2adf884d5d015edcbaa5362c20842194d3e6274d2e32779ae932`
  - `tool_head_raw_hoe_overlay.png` `67ef3aef6a0b181b64b50c013f2c3a8f60f80e4e9959d500e95f02dcba36fd85`
  - `tool_head_raw_pickaxe_overlay.png` `720b0630a288aae702fb630efdc9e9e28518943ea4b35b5933f0378ee983b7ca`
  - `tool_head_raw_plow_overlay.png` `c450720b71b94d94cec32fb17ec343eafdc425dd706018792b1b988407fea2c2`
  - `tool_head_raw_saw_overlay.png` `1a9ceba5784b1de59b641c78635d2153cc4e8bf0a90aab12acc4fdd4f6aaea1b`
  - `tool_head_raw_sense_overlay.png` `02db78469121345b79b2b0569abfa256b5e4cbb3e014b1be890b9b7c28cb3959`
  - `tool_head_raw_shovel_overlay.png` `ce5b0cb2cb07d059583841e3fa086e1cc1113950c1b77ebf0b5952112300f55e`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `8b0bea59d3b8ead1621c721dfa6f1c436df8a733c4085c9c90c7819333d154ee`
  - `tool_head_raw_universal_spade_overlay.png` `b61ad222f8e6ab81d00afce9d119a011f3889e3c64ca181e344493542907d616`
  - `tool_head_saw_overlay.png` `1a9ceba5784b1de59b641c78635d2153cc4e8bf0a90aab12acc4fdd4f6aaea1b`
  - `tool_head_screwdriver_overlay.png` `e7bd25978c74aed8d45e5e85e91cd45345f96141eb7c6770c2d02ce00241e01c`
  - `tool_head_sense_overlay.png` `02db78469121345b79b2b0569abfa256b5e4cbb3e014b1be890b9b7c28cb3959`
  - `tool_head_shovel_overlay.png` `ce5b0cb2cb07d059583841e3fa086e1cc1113950c1b77ebf0b5952112300f55e`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `8b0bea59d3b8ead1621c721dfa6f1c436df8a733c4085c9c90c7819333d154ee`
  - `tool_head_universal_spade_overlay.png` `b61ad222f8e6ab81d00afce9d119a011f3889e3c64ca181e344493542907d616`
  - `tool_head_wrench_overlay.png` `836968de0359a75b5ebc5dd8f272575cd993f9587bf595d428d06bbe12a532e8`
  - `wire_fine_overlay.png` `85a5f01a0bd4cc1f7c0dfcded04c6449b903c26983bb9e8bd705480932407847`
- `FINE` -> `assets/gt6/textures/item/material_sets/fine/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `FLINT` -> `assets/gt6/textures/item/material_sets/flint/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `FLUID` -> `assets/gt6/textures/item/material_sets/fluid/`:
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
- `FOOD` -> `assets/gt6/textures/item/material_sets/food/`:
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `GAS` -> `assets/gt6/textures/item/material_sets/gas/`:
  - `chemtube_overlay.png` `99479ead1cda30c67cc2452c2a0cfdbe4cf49452013bef6c389797f5cf9090fa`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
- `GEM_HORIZONTAL` -> `assets/gt6/textures/item/material_sets/gem_horizontal/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `GEM_VERTICAL` -> `assets/gt6/textures/item/material_sets/gem_vertical/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `GLASS` -> `assets/gt6/textures/item/material_sets/glass/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `5d2cf2a8c121873656021502a86f0851e24f0a36ba5069f999bed457784cbbd7`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `HEX` -> `assets/gt6/textures/item/material_sets/hex/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `cdd6b23a074d7a7b00553a31ec93a25b4a767576177ecbcfaa3c9ec4de75a90b`
  - `gear_gt_small_overlay.png` `cf0bea967a1e45a8827c51cf12680c65c827a837cb3d968131f1a9cd5daf70d0`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `6889367ec6482ed69d98bb92f9649824c21aebe60343d2276b0cb4f6a143e7f5`
  - `plate_gem_tiny_overlay.png` `8df0df4040e6e993fea4f132efe8730ade458aa61fc5297e79c3ed16863f93a2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `LAPIS` -> `assets/gt6/textures/item/material_sets/lapis/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `LEAF` -> `assets/gt6/textures/item/material_sets/leaf/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `LIGNITE` -> `assets/gt6/textures/item/material_sets/lignite/`:
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `MAGNETIC` -> `assets/gt6/textures/item/material_sets/magnetic/`:
  - `arrow_gt_plastic_overlay.png` `0f183035b052f54c6dc351909c58f2f7738423f4b792555a72a0cbeea75d3cb6`
  - `arrow_gt_wood_overlay.png` `2db19c7234177611fb66891a8b4a01dbdeb9682eeb71e494d7757a3526d76e64`
  - `billet_overlay.png` `e3f561a791554c835122559f7af604620c69f29e584889f80a67b23e990f9d9e`
  - `bolt_overlay.png` `c97809394f93669fac71bbc3c6012635ca6a64f8e82252f840156bf720e437f2`
  - `bullet_gt_large_overlay.png` `86fd2ed4969a692102088493204c8f8fa21b4f5818f279db407c6982bcbf4abf`
  - `bullet_gt_medium_overlay.png` `26d5c8e005c288bc6a666a4c9b27b14133b067e0b357d5f6796bd254375313f0`
  - `bullet_gt_small_overlay.png` `fbdc9ba6175e0f937bc9f5df48ba778e6c3b02cfb36ebf0f6b789ea35c05ae22`
  - `casing_small_overlay.png` `c97809394f93669fac71bbc3c6012635ca6a64f8e82252f840156bf720e437f2`
  - `chain_overlay.png` `c97809394f93669fac71bbc3c6012635ca6a64f8e82252f840156bf720e437f2`
  - `chemtube_overlay.png` `d18ff471735665871e7cadcebe82b77a0ee7c414520149665d24b4ad987fa585`
  - `chunk_gt_overlay.png` `d11627da717028a69fee38bbc32fd5fd97b506889b1d30782d25db42682babfe`
  - `foil_overlay.png` `f1557b218455167766828ec8463a64e9fb96e5e52311ebb0128df0298eaaf017`
  - `gear_gt_overlay.png` `a404db43c9f12a7ee097c6a9fb9bf8e6eb9eaf65e9a19ff65df2081def5a83e2`
  - `gear_gt_small_overlay.png` `75be0cf16a3592f7f720a2fca49778d75e20cc6534ac1dfa5b81bfaad01f6978`
  - `ingot_overlay.png` `e3f561a791554c835122559f7af604620c69f29e584889f80a67b23e990f9d9e`
  - `ingot_double_overlay.png` `97d5e2378e14e3df1c0544ce55537f86d8d24e8a2da59939c1560dde850c609c`
  - `ingot_hot_overlay.png` `0154d9ada65c988c426cb85c31103b797d8e4b1774023ced72a6f7b11fbd5fbf`
  - `ingot_quadruple_overlay.png` `9a70b479b85f65b05c95d89eb7ee186d3ad33cec32b484c1ddb369e364919804`
  - `ingot_quintuple_overlay.png` `c331dd27bb1cd2a2022122ef9de9002f35fd6b314987ebcd586c41ab100f9def`
  - `ingot_triple_overlay.png` `2ab7075fd0890047bd9120053525a0b185ef0da6e4e96473cd9b11a818e8cd44`
  - `minecart_wheels_overlay.png` `966f50eb549796a5e59be607bdd76c70aae83efb9dd2c66b94569b1124a86059`
  - `nugget_overlay.png` `1a4c63552436392d0c02a8b64bd252fe4fd0df188cdfcf314082afe77a481852`
  - `plate_overlay.png` `0daa874c9db0fc2552086805705354a9eb4413c3985ae5e3ad97924bcb4207c8`
  - `plate_curved_overlay.png` `0daa874c9db0fc2552086805705354a9eb4413c3985ae5e3ad97924bcb4207c8`
  - `plate_dense_overlay.png` `6647c2af9aae4cc11e0094642c3de81edc8d7558c96658e96e5fb00eeba4f615`
  - `plate_double_overlay.png` `f4e6bb2a8ef2fecf4dca30b40a7bf05784192f836c395bb8c93c134fb883fec1`
  - `plate_quadruple_overlay.png` `607c82749a640ac41c893a47420ee060db693a5db8e281b67d4f635cd282074e`
  - `plate_quintuple_overlay.png` `db42addb11e8436551d2ed69f23904149fc56624a6495b531733d5ce551faf1d`
  - `plate_tiny_overlay.png` `607c82749a640ac41c893a47420ee060db693a5db8e281b67d4f635cd282074e`
  - `plate_triple_overlay.png` `6fc22fe9f56eb9e436d192951bcada560fbe45d9bbd1bd58ff8f83f26a140fb8`
  - `ring_overlay.png` `11aadae2641f310e53907ab56d2e717eee3fd25ea25a992c494d75e7d5d50fe9`
  - `rotor_overlay.png` `63ef5b7355051de393878575a99c2512594f6ae45f433950eedc68e40355a56d`
  - `round_overlay.png` `9a5fdcf5dfffc22afc4c7a2faa540c3acf4fa29ee792807f63b44539c7d9e080`
  - `scrap_gt_overlay.png` `607c82749a640ac41c893a47420ee060db693a5db8e281b67d4f635cd282074e`
  - `screw_overlay.png` `cf7dd8afc27c1954c8cf1129abebbd52e9d7de9a3466411e5a7c8787917d3427`
  - `spring_overlay.png` `2e1cbdb14d011437bd80b6b51537131d28dee0dd0e1f25dd13f93ff4e493f63f`
  - `spring_small_overlay.png` `3f971ab01983b15c057a831151e37b521d27f8a76b5f20e89f9f2fe9b08301da`
  - `stick_overlay.png` `0b61713ce897bebcafac6c89dc805d7ae70067450729615baaa83eaa0268a495`
  - `stick_long_overlay.png` `770a1af694fc6ade3afc2bd2946c1d581758c5902b35fd4fa6a62e1ecb291a8f`
  - `tool_head_arrow_overlay.png` `7da82c3d5b0bb6531ff90fb4eb4f970ce8fcaac5fe2824eef0a713fef11735b1`
  - `tool_head_axe_overlay.png` `19f1e02ff509d1bae24176651bee0e37bf75213519fa2f428ce7f6d555578bbd`
  - `tool_head_axe_double_overlay.png` `0b61713ce897bebcafac6c89dc805d7ae70067450729615baaa83eaa0268a495`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `8aba73b25090ffb7bd017d7ac6f864de0665dca20729ea8be81549878f7b8f07`
  - `tool_head_chainsaw_overlay.png` `93e9113c4eee745beaa72245a92103de52bbf5ff79b24ed7c6a5a1f293145e96`
  - `tool_head_chisel_overlay.png` `d11627da717028a69fee38bbc32fd5fd97b506889b1d30782d25db42682babfe`
  - `tool_head_construction_pickaxe_overlay.png` `0b61713ce897bebcafac6c89dc805d7ae70067450729615baaa83eaa0268a495`
  - `tool_head_drill_overlay.png` `8d5a3798434b78eba4813673d56daaa0e0e146066e2dd0a04a179963f8688bc9`
  - `tool_head_file_overlay.png` `d11627da717028a69fee38bbc32fd5fd97b506889b1d30782d25db42682babfe`
  - `tool_head_hammer_overlay.png` `1bb2e7e1791c021c6a85082ccc6ba91467c3b75f5fba0de6a553e0bb4b715f8a`
  - `tool_head_hoe_overlay.png` `5101e046ec122b5e6cd6017e10833d24b016b0840539d766eccf06331326ac1d`
  - `tool_head_pickaxe_overlay.png` `1a47c73b2c0570197f11a4845010a59f1d423612ac74db91efad6aaecda88196`
  - `tool_head_plow_overlay.png` `8aba73b25090ffb7bd017d7ac6f864de0665dca20729ea8be81549878f7b8f07`
  - `tool_head_raw_arrow_overlay.png` `7da82c3d5b0bb6531ff90fb4eb4f970ce8fcaac5fe2824eef0a713fef11735b1`
  - `tool_head_raw_axe_overlay.png` `19f1e02ff509d1bae24176651bee0e37bf75213519fa2f428ce7f6d555578bbd`
  - `tool_head_raw_axe_double_overlay.png` `0b61713ce897bebcafac6c89dc805d7ae70067450729615baaa83eaa0268a495`
  - `tool_head_raw_chisel_overlay.png` `9433b988fe96d407daaca9548fbf04ebe4fb49cfb2e18d1c395dc7ec5423e894`
  - `tool_head_raw_hoe_overlay.png` `5101e046ec122b5e6cd6017e10833d24b016b0840539d766eccf06331326ac1d`
  - `tool_head_raw_pickaxe_overlay.png` `1a47c73b2c0570197f11a4845010a59f1d423612ac74db91efad6aaecda88196`
  - `tool_head_raw_plow_overlay.png` `8aba73b25090ffb7bd017d7ac6f864de0665dca20729ea8be81549878f7b8f07`
  - `tool_head_raw_saw_overlay.png` `9433b988fe96d407daaca9548fbf04ebe4fb49cfb2e18d1c395dc7ec5423e894`
  - `tool_head_raw_sense_overlay.png` `1249f8c28d4de5c2d914033942e15fc2a3bac65b8f1af241a08ffaded7bae575`
  - `tool_head_raw_shovel_overlay.png` `d3eec375f9f65427ccaff8b18476956b099d12f92303d324e3c7010eaa2663ea`
  - `tool_head_raw_spade_overlay.png` `024ddd9aac664b129c5906a4d8889f2214403fdf6c635feed74e8e4d85df129a`
  - `tool_head_raw_sword_overlay.png` `fea98e4547026a1ae7a13acf7729e97d64d4d31ec3bd7ab4d60eccecced39148`
  - `tool_head_raw_universal_spade_overlay.png` `277605e5e2834d7e1421dc57cc5b61b92850ae7dcb1a669a82b66e4f63869f3e`
  - `tool_head_saw_overlay.png` `9433b988fe96d407daaca9548fbf04ebe4fb49cfb2e18d1c395dc7ec5423e894`
  - `tool_head_screwdriver_overlay.png` `fb240564003d0a90f001d291445feb53a94101e29955f85260c376f8037e4810`
  - `tool_head_sense_overlay.png` `1249f8c28d4de5c2d914033942e15fc2a3bac65b8f1af241a08ffaded7bae575`
  - `tool_head_shovel_overlay.png` `d3eec375f9f65427ccaff8b18476956b099d12f92303d324e3c7010eaa2663ea`
  - `tool_head_spade_overlay.png` `9e068a910a926e9eee209732d8aa4fc69a4ac2855115eb0a0c3426f66ba23bf8`
  - `tool_head_sword_overlay.png` `fea98e4547026a1ae7a13acf7729e97d64d4d31ec3bd7ab4d60eccecced39148`
  - `tool_head_universal_spade_overlay.png` `277605e5e2834d7e1421dc57cc5b61b92850ae7dcb1a669a82b66e4f63869f3e`
  - `tool_head_wrench_overlay.png` `b8fa1fe47f2dfb55daf7339d0d5a9a232b97cfa8848d6ccf11be8db75d37403b`
  - `wire_fine_overlay.png` `71ff3cc2733815e5dfe7ad0e9318afa395e2636493fac7e774c6eaa5693141e1`
- `METALLIC` -> `assets/gt6/textures/item/material_sets/metallic/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `rail_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `NETHERSTAR` -> `assets/gt6/textures/item/material_sets/netherstar/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `NONE` -> `assets/gt6/textures/item/material_sets/none/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
- `OPAL` -> `assets/gt6/textures/item/material_sets/opal/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `034b3ef7f28f7b9721ee07c6e0b9493e6703a7d81b858ecd1eadc9e9f232b73d`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `PAPER` -> `assets/gt6/textures/item/material_sets/paper/`:
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `217fd04b01991fd56f4e1bc499b321a2cc851db6619d534be0a0e1147dc15c92`
  - `plate_quintuple_overlay.png` `9f84058ac218236dc6c1be24f640a922cc7d044cd345009649740d180db93c79`
  - `plate_triple_overlay.png` `f6c3ffffe619dbb816841b7e30deab4fc8cc816c23a83d3f22ce0fce25ae42b2`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `POWDER` -> `assets/gt6/textures/item/material_sets/powder/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `PRISMARINE` -> `assets/gt6/textures/item/material_sets/prismarine/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6accfd214920e4c56f7848d20cc09cb4ae4cf6e781799faa9de95f1a39186636`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `c4629e9027354835de2abcd0d2c5e8b2c0331f11527c0590af86e3b85d5ef1be`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `QUARTZ` -> `assets/gt6/textures/item/material_sets/quartz/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `503dbaf736063cf0074b31c648e49272e6ab27927dc02b3c175c08ba581f297b`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `RAD` -> `assets/gt6/textures/item/material_sets/rad/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `3512700e235de9801036d2616136640e0b3abd46e1bbcf6a8aab8926b69366ef`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `REDSTONE` -> `assets/gt6/textures/item/material_sets/redstone/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `a0cb31a6781958d896a8c4a0fddb31984f0e092a177dccb02cf1867e0e4af9fa`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `ROUGH` -> `assets/gt6/textures/item/material_sets/rough/`:
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `RUBBER` -> `assets/gt6/textures/item/material_sets/rubber/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `dc48bc505678e4c7962e4f45887c5fbf63e7c9c423cb516d6897ad04e23af663`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `ingot_overlay.png` `60747a752b1931c8a5af051d62d4c2ef6a96d020974478434ac3ab7395ccae12`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `nugget_overlay.png` `940b1f2f0b31732a719df79e9e8d50790556d974ee9285e53917fac6edc1f8c0`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `83de1f05e930b653720b455716cc7c17231f17d2cb187700ba1a6abd3eab2f47`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `62bf059f832f0975eec5fcc5f3966e0fbaf499e17394573c9fc933fd965378dc`
  - `stick_long_overlay.png` `3c9400d3dcfe6498d83bf77fcd1ddf6d9e88a85df91c9b6660c09149233c1461`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `RUBY` -> `assets/gt6/textures/item/material_sets/ruby/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `8b3a73552fee59f3ad7f42e3dcc3577b4aa87ae9dc309265ea91d921ccc448e4`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `519b6a07fe5d29c3590b92bd4d8b1b6bab862faff5c3654862dc212696fb89e1`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `SAND` -> `assets/gt6/textures/item/material_sets/sand/`:
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `e29ed5ea461a0c719af206bb35bbd5821749bfbc626eac55edc270720b8a4111`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `SHARDS` -> `assets/gt6/textures/item/material_sets/shards/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_chipped_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_exquisite_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawed_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_flawless_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gem_legendary_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `ore_raw_overlay.png` `b7ab407b4d4d1f221d21fbcb48e75de79e6774786066acb95dfa8ad55cfd609f`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_gem_overlay.png` `89d2c38f10b44ae730ed2b7dfb46a9c5fa975f8d2aa441b990eea124ea1da4e3`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `SHINY` -> `assets/gt6/textures/item/material_sets/shiny/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `27ea2d30452a65b7d39eeb55adfb839f8ee102ffd38f6f42b600a5770f0427c4`
  - `bullet_gt_medium_overlay.png` `51bf0693d58fd83df16b6bc103d35f155389389263015380c510ba4c36f9ca8e`
  - `bullet_gt_small_overlay.png` `6f0991c67fe338d0b6096ce379af0cf6e5a85504bc32ff5c4c0e429e048eac99`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `90a0b61029bb55b04339a737980128cacbd655fc3a80d7d6a944a543fa7dba45`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `ccbf9304a619b43191f923c91b71f4ffe20ad52253ece8a38fb603241f32afd3`
  - `dust_tiny_overlay.png` `a030139084aa64bb559ace5014d92800529d9795e94cbfc6d0f6d48d7976d95c`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `2b314d4b80ee0409dff242ca4e98435c571e1c6703db37baf918ad3ae3832d87`
  - `gear_gt_small_overlay.png` `0e2c919e4dd8ee525ab419ab0c69497c59b741e02d6c9765724bc12ead565f02`
  - `gem_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `8b3a73552fee59f3ad7f42e3dcc3577b4aa87ae9dc309265ea91d921ccc448e4`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `lens_overlay.png` `c4dcc78d96b585a46276c68a7876c629058fd2c5e4609ba13052f0ab213c1927`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `519b6a07fe5d29c3590b92bd4d8b1b6bab862faff5c3654862dc212696fb89e1`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_gem_overlay.png` `78551bff3a91da795971a3cc5b8421d0e14b84e6e1301b562cca9cb4cf38362f`
  - `plate_gem_tiny_overlay.png` `af6a98531185f35bd5c06541e8000e26546de77ef90d378449e11389bacc38e2`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `rail_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `feb8c44ceaee957b2cab734d8145bb385799513c9790ded71c6a82ce5104cb8d`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `ea1caf24eb9ea77ab4d194db2d4d5d90d650dab50b0cad83ff735eebef7fc77e`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `f666652c6773d202d20b22ef4981425e6c6b9d7b9710d3ca7334eb6acde36ccc`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `SPACE` -> `assets/gt6/textures/item/material_sets/space/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `rail_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `503dbaf736063cf0074b31c648e49272e6ab27927dc02b3c175c08ba581f297b`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`
- `STONE` -> `assets/gt6/textures/item/material_sets/stone/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `crushed_overlay.png` `42f6a19b09f828648ba417a155c36b41b5a1025c5cd75ba9aa035cce7c8203c2`
  - `crushed_centrifuged_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_centrifuged_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_purified_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `crushed_tiny_overlay.png` `e1fcc7603d828a6f91d383001aa64e006c66f633c9d4eb7b5dfbfae3b9dbc5f0`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ore_raw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `503dbaf736063cf0074b31c648e49272e6ab27927dc02b3c175c08ba581f297b`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
- `WOOD` -> `assets/gt6/textures/item/material_sets/wood/`:
  - `arrow_gt_plastic_overlay.png` `388376001ef7ccbb6108289a3d35a8edb318138cca1d39eb0c61e7cdd17753ce`
  - `arrow_gt_wood_overlay.png` `df08fa29262eb15dc777ccffaea4fd02d52a94a244627d2b5a8a344ab230728b`
  - `billet_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bolt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `bullet_gt_large_overlay.png` `aa42b343546e7ac05aeea9d3da7b5d6af82e2e33cbe55fb7e44f4641b4602684`
  - `bullet_gt_medium_overlay.png` `f0e19a64164013d7b50b5ef1edb3c740f04245efea7e0fe908982f954dfd4aa3`
  - `bullet_gt_small_overlay.png` `6cedc51e6dcc32e8f96e56aa8a0ef1df06d6d9e360f0ffada27d193c2c479a91`
  - `casing_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `chain_overlay.png` `9a8136434004ec3a3d54f0474bac6b85ea86cc7ee869976cfd0c18b2a753019b`
  - `chemtube_overlay.png` `415feeff73ef49de10ecc2fe4b3973a112af5ae1c37395969f081138f4579d5d`
  - `chunk_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_div72_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `dust_tiny_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `foil_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `gear_gt_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_hot_overlay.png` `d45218b67f4d9fc4721bfe67404de3600f4c64ff56d4e94892507339092a97ba`
  - `ingot_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ingot_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `minecart_wheels_overlay.png` `053c86f7705c7994a95adfb8cccce39441bb6428f66df96da9bbf5682a647f7e`
  - `nugget_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plant_gt_berry_overlay.png` `755a53640a8314e62a1df28d92e02fb10d9d32b921b18786c4f8280a9f4bd6ca`
  - `plant_gt_blossom_overlay.png` `233209d61a98be5aea82fccb6222ae546f4ec24ed9f9bad05559b41733094d5f`
  - `plant_gt_fiber_overlay.png` `40d20c613fb00894e5d29b4305b5bba6c2e50ea95a9e65658c0c0aa1d3cf5c75`
  - `plant_gt_twig_overlay.png` `d433e0784308d1f89412ecb4f67b230543e76ad5fdc06f5ee55a7dc89beb03ce`
  - `plant_gt_wart_overlay.png` `7f47ef647cd88c87d4cef131a6bf58f9b0447a8a84b29111a8ba7827643cf4d4`
  - `plate_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_curved_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_dense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quadruple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_quintuple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `plate_tiny_overlay.png` `87240e379447cb24a4b8e9333c8db4df86b78139644fe2774b7e4de21d85b8ce`
  - `plate_triple_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `ring_overlay.png` `a8d05eb70dd1fcdeb43035b2f25495e9902d39fb2e008a97c15def544ba84691`
  - `rock_gt_overlay.png` `bfa5c640bbc8e7f93fb98facf9d2d2288f3a5bf232ff556c588b96ecf1167c64`
  - `rotor_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `round_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `scrap_gt_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `screw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `spring_small_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `stick_long_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_builderwand_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_buzz_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chainsaw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_construction_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_drill_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_file_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hammer_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_arrow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_axe_double_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_chisel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_hoe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_pickaxe_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_plow_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_raw_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_saw_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_screwdriver_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sense_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_shovel_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_sword_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_universal_spade_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `tool_head_wrench_overlay.png` `6c34b65e82b634baf68ee538e487fbf0753e6f7642963bbe28afc2af589cb510`
  - `wire_fine_overlay.png` `e7706aff0582ee538eb20996e3142e729d0512bdb2eb4ee6b2bbd0d17750c83a`


Machine port side-face overlay borrows, task p28-a-port-overlay-assets
(copied 2026-09-12). The port-face art set for the B render card: per
basicmachines family the colored six-face body plus the five NON-front side
faces of the three state overlay layers. The front face is the WORK face — its
overlay fronts are the p22 split-front borrows, re-verified here byte-for-byte,
not re-borrowed. Script: `mdk/tools/borrow_port_overlays.py` (pure stdlib, the
`bake_machine_fronts.py` decode/encode idiom verbatim; plain 16x16 sources are
byte-identical copies, 16xN strips are cropped to FRAME 0 and re-encoded
filter-0 — the P20/P22 "animation stays retired" deviation; mcmeta files are
not borrowed).

Family census (script-parsed, nothing hand-picked): the borrow set is the
intersection of the upstream `NBT_TEXTURE` registrations
(`Loader_MultiTileEntities.java`, 122 texture literals) ∩ the locally
registered texture families (the `TD.Energy.<KIND>, "<family>",` row-factory
literals in `GTMachines.java` + the `addMachine(..., "<family>")` literals in
`GT6BlockStates.java`, 12 families total) ∩ the upstream disk tree
`basicmachines/<family>/` (79 groups). Result: all 12 local families are
upstream-registered and disk-present:

  canner, compressor, crusher, distillery, dryer, extruder, lathe, oven, press, shredder, sifter, wiremill

Roll-ladder append, task p29-w1-kinetic-roll-ladder (copied 2026-09-13): the
same pipeline re-run after the four RU roll-ladder families joined the local
registration face — `rollingmill` was already in the P28 set (the p28 ULV rung
shares it), `rollbender` / `rollformer` / `clustermill` join here: 3 new
families x (colored 6 + 3 states x 5 side faces) = 63 new borrows, byte
copies of plain 16x16 sources (snapshot v6.17.06-22-g3703e4030, the same
generic machine plate bases — `colored/front.png`
`db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` and the
rollingmill overlay set are byte-shared across the roll families).

Reconciliation grid: 12 families x (colored 6 faces + 3 states x 5 side
faces) = 252 products = 243 new borrows + 12 in-place p22 re-verifications +
0 declared (every expected upstream source exists in snapshot
v6.17.06-22-g3703e4030). Output naming is the p22 split-front shape
generalized to the side faces — `<family>_colored_<face>.png`,
`<family>_overlay_<face>.png`, `<family>_overlay_<face>_active.png`,
`<family>_overlay_<face>_running.png` (state tail LAST — distinct from the
retired P9 full-strip namespace `oven_overlay_<state>_<face>.png`, whose 12
16xN strips stay in place untouched). Eight side faces are strip sources
cropped at FRAME 0: oven/lathe/compressor/press/extruder `overlay_active/back`
and lathe/compressor/press `overlay_*` back variants — their upstream mcmeta
carry sequential frames (frametime 2, no explicit reordering), so FRAME 0 is
the first frame by construction.

The 12 `in-place` entries (one `<family>_colored_front.png` per family) are
the p22 borrow sha256 re-verifications, listed here so the census grid is
complete in one place:

  - `canner_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/canner/colored/front.png`)
  - `compressor_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/compressor/colored/front.png`)
  - `crusher_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/crusher/colored/front.png`)
  - `distillery_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/distillery/colored/front.png`)
  - `dryer_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/dryer/colored/front.png`)
  - `extruder_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/extruder/colored/front.png`)
  - `lathe_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/lathe/colored/front.png`)
  - `oven_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/oven/colored/front.png`)
  - `press_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/press/colored/front.png`)
  - `shredder_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/shredder/colored/front.png`)
  - `sifter_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/sifter/colored/front.png`)
  - `wiremill_colored_front.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/wiremill/colored/front.png`)

The 243 new borrows (byte copies unless noted FRAME 0):

  - `canner_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/canner/colored/bottom.png`)
  - `canner_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/canner/colored/top.png`)
  - `canner_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/canner/colored/left.png`)
  - `canner_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/canner/colored/right.png`)
  - `canner_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/canner/colored/back.png`)
  - `canner_overlay_bottom.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/canner/overlay/bottom.png`)
  - `canner_overlay_top.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/canner/overlay/top.png`)
  - `canner_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/canner/overlay/left.png`)
  - `canner_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/canner/overlay/right.png`)
  - `canner_overlay_back.png`  `a326d5e7732ed7297a4def6b1af9a7f5e2a6da035dab62d3a7e38b7c1a46a482`  (byte copy; upstream `basicmachines/canner/overlay/back.png`)
  - `canner_overlay_bottom_active.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/canner/overlay_active/bottom.png`)
  - `canner_overlay_top_active.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/canner/overlay_active/top.png`)
  - `canner_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/canner/overlay_active/left.png`)
  - `canner_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/canner/overlay_active/right.png`)
  - `canner_overlay_back_active.png`  `eb0b3a8c6bae86e319d153af758c8094695dd63ed2d2991ca72920dab1d76428`  (byte copy; upstream `basicmachines/canner/overlay_active/back.png`)
  - `canner_overlay_bottom_running.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/canner/overlay_running/bottom.png`)
  - `canner_overlay_top_running.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/canner/overlay_running/top.png`)
  - `canner_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/canner/overlay_running/left.png`)
  - `canner_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/canner/overlay_running/right.png`)
  - `canner_overlay_back_running.png`  `eb0b3a8c6bae86e319d153af758c8094695dd63ed2d2991ca72920dab1d76428`  (byte copy; upstream `basicmachines/canner/overlay_running/back.png`)
  - `compressor_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/compressor/colored/bottom.png`)
  - `compressor_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/compressor/colored/top.png`)
  - `compressor_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/compressor/colored/left.png`)
  - `compressor_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/compressor/colored/right.png`)
  - `compressor_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/compressor/colored/back.png`)
  - `compressor_overlay_bottom.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/compressor/overlay/bottom.png`)
  - `compressor_overlay_top.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/compressor/overlay/top.png`)
  - `compressor_overlay_left.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/compressor/overlay/left.png`)
  - `compressor_overlay_right.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/compressor/overlay/right.png`)
  - `compressor_overlay_back.png`  `0fea01697877dd0666a1e5a6a7f31292a3deedc41d9510b8dfdae8f7f8e243c1`  (byte copy; upstream `basicmachines/compressor/overlay/back.png`)
  - `compressor_overlay_bottom_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/compressor/overlay_active/bottom.png`)
  - `compressor_overlay_top_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/compressor/overlay_active/top.png`)
  - `compressor_overlay_left_active.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/compressor/overlay_active/left.png`)
  - `compressor_overlay_right_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/compressor/overlay_active/right.png`)
  - `compressor_overlay_back_active.png`  `ec6f5d9a947ca0b94dcc356b808e30976577634f2d3f92aefa1de493fb7295f2`  (FRAME 0 of 9; upstream `basicmachines/compressor/overlay_active/back.png`)
  - `compressor_overlay_bottom_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/compressor/overlay_running/bottom.png`)
  - `compressor_overlay_top_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/compressor/overlay_running/top.png`)
  - `compressor_overlay_left_running.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/compressor/overlay_running/left.png`)
  - `compressor_overlay_right_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/compressor/overlay_running/right.png`)
  - `compressor_overlay_back_running.png`  `644319970ea18872364826dbebaa1160eda0294cf5c880ba22d8d9624c591801`  (FRAME 0 of 9; upstream `basicmachines/compressor/overlay_running/back.png`)
  - `crusher_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/crusher/colored/bottom.png`)
  - `crusher_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/crusher/colored/top.png`)
  - `crusher_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/crusher/colored/left.png`)
  - `crusher_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/crusher/colored/right.png`)
  - `crusher_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/crusher/colored/back.png`)
  - `crusher_overlay_bottom.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/crusher/overlay/bottom.png`)
  - `crusher_overlay_top.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/crusher/overlay/top.png`)
  - `crusher_overlay_left.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/crusher/overlay/left.png`)
  - `crusher_overlay_right.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/crusher/overlay/right.png`)
  - `crusher_overlay_back.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/crusher/overlay/back.png`)
  - `crusher_overlay_bottom_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/crusher/overlay_active/bottom.png`)
  - `crusher_overlay_top_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/crusher/overlay_active/top.png`)
  - `crusher_overlay_left_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/crusher/overlay_active/left.png`)
  - `crusher_overlay_right_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/crusher/overlay_active/right.png`)
  - `crusher_overlay_back_active.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/crusher/overlay_active/back.png`)
  - `crusher_overlay_bottom_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/crusher/overlay_running/bottom.png`)
  - `crusher_overlay_top_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/crusher/overlay_running/top.png`)
  - `crusher_overlay_left_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/crusher/overlay_running/left.png`)
  - `crusher_overlay_right_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/crusher/overlay_running/right.png`)
  - `crusher_overlay_back_running.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/crusher/overlay_running/back.png`)
  - `distillery_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/distillery/colored/bottom.png`)
  - `distillery_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/distillery/colored/top.png`)
  - `distillery_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/distillery/colored/left.png`)
  - `distillery_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/distillery/colored/right.png`)
  - `distillery_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/distillery/colored/back.png`)
  - `distillery_overlay_bottom.png`  `1d116dec67f49dc22a130120f186fbbf7e9f963f4212629ea46a963a6f2feacb`  (byte copy; upstream `basicmachines/distillery/overlay/bottom.png`)
  - `distillery_overlay_top.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/distillery/overlay/top.png`)
  - `distillery_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/distillery/overlay/left.png`)
  - `distillery_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/distillery/overlay/right.png`)
  - `distillery_overlay_back.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/distillery/overlay/back.png`)
  - `distillery_overlay_bottom_active.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/distillery/overlay_active/bottom.png`)
  - `distillery_overlay_top_active.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/distillery/overlay_active/top.png`)
  - `distillery_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/distillery/overlay_active/left.png`)
  - `distillery_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/distillery/overlay_active/right.png`)
  - `distillery_overlay_back_active.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/distillery/overlay_active/back.png`)
  - `distillery_overlay_bottom_running.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/distillery/overlay_running/bottom.png`)
  - `distillery_overlay_top_running.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/distillery/overlay_running/top.png`)
  - `distillery_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/distillery/overlay_running/left.png`)
  - `distillery_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/distillery/overlay_running/right.png`)
  - `distillery_overlay_back_running.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/distillery/overlay_running/back.png`)
  - `dryer_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/dryer/colored/bottom.png`)
  - `dryer_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/dryer/colored/top.png`)
  - `dryer_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/dryer/colored/left.png`)
  - `dryer_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/dryer/colored/right.png`)
  - `dryer_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/dryer/colored/back.png`)
  - `dryer_overlay_bottom.png`  `1d116dec67f49dc22a130120f186fbbf7e9f963f4212629ea46a963a6f2feacb`  (byte copy; upstream `basicmachines/dryer/overlay/bottom.png`)
  - `dryer_overlay_top.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/dryer/overlay/top.png`)
  - `dryer_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/dryer/overlay/left.png`)
  - `dryer_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/dryer/overlay/right.png`)
  - `dryer_overlay_back.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/dryer/overlay/back.png`)
  - `dryer_overlay_bottom_active.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/dryer/overlay_active/bottom.png`)
  - `dryer_overlay_top_active.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/dryer/overlay_active/top.png`)
  - `dryer_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/dryer/overlay_active/left.png`)
  - `dryer_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/dryer/overlay_active/right.png`)
  - `dryer_overlay_back_active.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/dryer/overlay_active/back.png`)
  - `dryer_overlay_bottom_running.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/dryer/overlay_running/bottom.png`)
  - `dryer_overlay_top_running.png`  `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`  (byte copy; upstream `basicmachines/dryer/overlay_running/top.png`)
  - `dryer_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/dryer/overlay_running/left.png`)
  - `dryer_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/dryer/overlay_running/right.png`)
  - `dryer_overlay_back_running.png`  `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`  (byte copy; upstream `basicmachines/dryer/overlay_running/back.png`)
  - `extruder_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/extruder/colored/bottom.png`)
  - `extruder_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/extruder/colored/top.png`)
  - `extruder_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/extruder/colored/left.png`)
  - `extruder_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/extruder/colored/right.png`)
  - `extruder_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/extruder/colored/back.png`)
  - `extruder_overlay_bottom.png`  `1d116dec67f49dc22a130120f186fbbf7e9f963f4212629ea46a963a6f2feacb`  (byte copy; upstream `basicmachines/extruder/overlay/bottom.png`)
  - `extruder_overlay_top.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/extruder/overlay/top.png`)
  - `extruder_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/extruder/overlay/left.png`)
  - `extruder_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/extruder/overlay/right.png`)
  - `extruder_overlay_back.png`  `97a428fc12cb31f4f18114567ec6ba06f33dd73813cbd2d2017b7b66b63a859f`  (byte copy; upstream `basicmachines/extruder/overlay/back.png`)
  - `extruder_overlay_bottom_active.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/extruder/overlay_active/bottom.png`)
  - `extruder_overlay_top_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/extruder/overlay_active/top.png`)
  - `extruder_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/extruder/overlay_active/left.png`)
  - `extruder_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/extruder/overlay_active/right.png`)
  - `extruder_overlay_back_active.png`  `299a7f0616df27328cec4e71124aeee8040829aabd26e60e4ad208ec64d28b29`  (FRAME 0 of 6; upstream `basicmachines/extruder/overlay_active/back.png`)
  - `extruder_overlay_bottom_running.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/extruder/overlay_running/bottom.png`)
  - `extruder_overlay_top_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/extruder/overlay_running/top.png`)
  - `extruder_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/extruder/overlay_running/left.png`)
  - `extruder_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/extruder/overlay_running/right.png`)
  - `extruder_overlay_back_running.png`  `fc56cfebfca63bb4f88e26be3d03bd57ed42b8d0e2d2bc6dd9f3f2263a09c99d`  (byte copy; upstream `basicmachines/extruder/overlay_running/back.png`)
  - `lathe_colored_bottom.png`  `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977`  (byte copy; upstream `basicmachines/lathe/colored/bottom.png`)
  - `lathe_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/lathe/colored/top.png`)
  - `lathe_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/lathe/colored/left.png`)
  - `lathe_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/lathe/colored/right.png`)
  - `lathe_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/lathe/colored/back.png`)
  - `lathe_overlay_bottom.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/lathe/overlay/bottom.png`)
  - `lathe_overlay_top.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/lathe/overlay/top.png`)
  - `lathe_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/lathe/overlay/left.png`)
  - `lathe_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/lathe/overlay/right.png`)
  - `lathe_overlay_back.png`  `406b11e8a6e88d68f1d654c67f20311b5b6d74ce93940183309ca0e8ce0cd155`  (byte copy; upstream `basicmachines/lathe/overlay/back.png`)
  - `lathe_overlay_bottom_active.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/lathe/overlay_active/bottom.png`)
  - `lathe_overlay_top_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/lathe/overlay_active/top.png`)
  - `lathe_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/lathe/overlay_active/left.png`)
  - `lathe_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/lathe/overlay_active/right.png`)
  - `lathe_overlay_back_active.png`  `a90fafa3c10cfb6eb532a0488fcc06adc73ea0295566559111e9549ef4b34289`  (FRAME 0 of 6; upstream `basicmachines/lathe/overlay_active/back.png`)
  - `lathe_overlay_bottom_running.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/lathe/overlay_running/bottom.png`)
  - `lathe_overlay_top_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/lathe/overlay_running/top.png`)
  - `lathe_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/lathe/overlay_running/left.png`)
  - `lathe_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/lathe/overlay_running/right.png`)
  - `lathe_overlay_back_running.png`  `a07a82dc3e9f481e2ee8e14f03d7f6c8128fa36765250c81cbb483e3380dcef3`  (FRAME 0 of 6; upstream `basicmachines/lathe/overlay_running/back.png`)
  - `oven_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/oven/colored/bottom.png`)
  - `oven_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/oven/colored/top.png`)
  - `oven_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/oven/colored/left.png`)
  - `oven_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/oven/colored/right.png`)
  - `oven_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/oven/colored/back.png`)
  - `oven_overlay_bottom.png`  `1d116dec67f49dc22a130120f186fbbf7e9f963f4212629ea46a963a6f2feacb`  (byte copy; upstream `basicmachines/oven/overlay/bottom.png`)
  - `oven_overlay_top.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/oven/overlay/top.png`)
  - `oven_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/oven/overlay/left.png`)
  - `oven_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/oven/overlay/right.png`)
  - `oven_overlay_back.png`  `3da4d0f7d578f6e08102a0b7615160035997d58affc26c7d13813b678c4bd7fd`  (byte copy; upstream `basicmachines/oven/overlay/back.png`)
  - `oven_overlay_bottom_active.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/oven/overlay_active/bottom.png`)
  - `oven_overlay_top_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/oven/overlay_active/top.png`)
  - `oven_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/oven/overlay_active/left.png`)
  - `oven_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/oven/overlay_active/right.png`)
  - `oven_overlay_back_active.png`  `55ee0c7bd3a569150a80390a3e96c74b9993b2896161a0b46c9eef93daefa821`  (FRAME 0 of 8; upstream `basicmachines/oven/overlay_active/back.png`)
  - `oven_overlay_bottom_running.png`  `b7b0e3f66530c15165ba8d44c2b05ef14986624987636a5c2caea818c670183a`  (byte copy; upstream `basicmachines/oven/overlay_running/bottom.png`)
  - `oven_overlay_top_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/oven/overlay_running/top.png`)
  - `oven_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/oven/overlay_running/left.png`)
  - `oven_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/oven/overlay_running/right.png`)
  - `oven_overlay_back_running.png`  `cd5857690ac01a9fc10e3aadd4fe3afe3c2372d685ef0bcf14b680756b92b85e`  (byte copy; upstream `basicmachines/oven/overlay_running/back.png`)
  - `press_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/press/colored/bottom.png`)
  - `press_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/press/colored/top.png`)
  - `press_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/press/colored/left.png`)
  - `press_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/press/colored/right.png`)
  - `press_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/press/colored/back.png`)
  - `press_overlay_bottom.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/press/overlay/bottom.png`)
  - `press_overlay_top.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/press/overlay/top.png`)
  - `press_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/press/overlay/left.png`)
  - `press_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/press/overlay/right.png`)
  - `press_overlay_back.png`  `0f36acaa757a9c82b7960f88e7a905dc45364121959b44ffad534b8848fbccc5`  (byte copy; upstream `basicmachines/press/overlay/back.png`)
  - `press_overlay_bottom_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/press/overlay_active/bottom.png`)
  - `press_overlay_top_active.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/press/overlay_active/top.png`)
  - `press_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/press/overlay_active/left.png`)
  - `press_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/press/overlay_active/right.png`)
  - `press_overlay_back_active.png`  `b56f809af495a9241d54ce6087cef250d405fbdb78ae33666655e611f0986b37`  (FRAME 0 of 16; upstream `basicmachines/press/overlay_active/back.png`)
  - `press_overlay_bottom_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/press/overlay_running/bottom.png`)
  - `press_overlay_top_running.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/press/overlay_running/top.png`)
  - `press_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/press/overlay_running/left.png`)
  - `press_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/press/overlay_running/right.png`)
  - `press_overlay_back_running.png`  `016292a072f18b7b9cb149c4a937835eeae980949b6986846835245d8247cb88`  (FRAME 0 of 16; upstream `basicmachines/press/overlay_running/back.png`)
  - `shredder_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/shredder/colored/bottom.png`)
  - `shredder_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/shredder/colored/top.png`)
  - `shredder_colored_left.png`  `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977`  (byte copy; upstream `basicmachines/shredder/colored/left.png`)
  - `shredder_colored_right.png`  `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977`  (byte copy; upstream `basicmachines/shredder/colored/right.png`)
  - `shredder_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/shredder/colored/back.png`)
  - `shredder_overlay_bottom.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/shredder/overlay/bottom.png`)
  - `shredder_overlay_top.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/shredder/overlay/top.png`)
  - `shredder_overlay_left.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/shredder/overlay/left.png`)
  - `shredder_overlay_right.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/shredder/overlay/right.png`)
  - `shredder_overlay_back.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/shredder/overlay/back.png`)
  - `shredder_overlay_bottom_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/shredder/overlay_active/bottom.png`)
  - `shredder_overlay_top_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/shredder/overlay_active/top.png`)
  - `shredder_overlay_left_active.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/shredder/overlay_active/left.png`)
  - `shredder_overlay_right_active.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/shredder/overlay_active/right.png`)
  - `shredder_overlay_back_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/shredder/overlay_active/back.png`)
  - `shredder_overlay_bottom_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/shredder/overlay_running/bottom.png`)
  - `shredder_overlay_top_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/shredder/overlay_running/top.png`)
  - `shredder_overlay_left_running.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/shredder/overlay_running/left.png`)
  - `shredder_overlay_right_running.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/shredder/overlay_running/right.png`)
  - `shredder_overlay_back_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/shredder/overlay_running/back.png`)
  - `sifter_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/sifter/colored/bottom.png`)
  - `sifter_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/sifter/colored/top.png`)
  - `sifter_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/sifter/colored/left.png`)
  - `sifter_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/sifter/colored/right.png`)
  - `sifter_colored_back.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/sifter/colored/back.png`)
  - `sifter_overlay_bottom.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/sifter/overlay/bottom.png`)
  - `sifter_overlay_top.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/sifter/overlay/top.png`)
  - `sifter_overlay_left.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/sifter/overlay/left.png`)
  - `sifter_overlay_right.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/sifter/overlay/right.png`)
  - `sifter_overlay_back.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/sifter/overlay/back.png`)
  - `sifter_overlay_bottom_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/sifter/overlay_active/bottom.png`)
  - `sifter_overlay_top_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/sifter/overlay_active/top.png`)
  - `sifter_overlay_left_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/sifter/overlay_active/left.png`)
  - `sifter_overlay_right_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/sifter/overlay_active/right.png`)
  - `sifter_overlay_back_active.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/sifter/overlay_active/back.png`)
  - `sifter_overlay_bottom_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/sifter/overlay_running/bottom.png`)
  - `sifter_overlay_top_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/sifter/overlay_running/top.png`)
  - `sifter_overlay_left_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/sifter/overlay_running/left.png`)
  - `sifter_overlay_right_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/sifter/overlay_running/right.png`)
  - `sifter_overlay_back_running.png`  `79587c544b954621e675e9da925d6f5bf9bcacfd7a2a4c6bbab7c8933b75df09`  (byte copy; upstream `basicmachines/sifter/overlay_running/back.png`)
  - `wiremill_colored_bottom.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/wiremill/colored/bottom.png`)
  - `wiremill_colored_top.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/wiremill/colored/top.png`)
  - `wiremill_colored_left.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/wiremill/colored/left.png`)
  - `wiremill_colored_right.png`  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`  (byte copy; upstream `basicmachines/wiremill/colored/right.png`)
  - `wiremill_colored_back.png`  `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977`  (byte copy; upstream `basicmachines/wiremill/colored/back.png`)
  - `wiremill_overlay_bottom.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/wiremill/overlay/bottom.png`)
  - `wiremill_overlay_top.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/wiremill/overlay/top.png`)
  - `wiremill_overlay_left.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/wiremill/overlay/left.png`)
  - `wiremill_overlay_right.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/wiremill/overlay/right.png`)
  - `wiremill_overlay_back.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/wiremill/overlay/back.png`)
  - `wiremill_overlay_bottom_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/wiremill/overlay_active/bottom.png`)
  - `wiremill_overlay_top_active.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/wiremill/overlay_active/top.png`)
  - `wiremill_overlay_left_active.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/wiremill/overlay_active/left.png`)
  - `wiremill_overlay_right_active.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/wiremill/overlay_active/right.png`)
  - `wiremill_overlay_back_active.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/wiremill/overlay_active/back.png`)
  - `wiremill_overlay_bottom_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/wiremill/overlay_running/bottom.png`)
  - `wiremill_overlay_top_running.png`  `02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d`  (byte copy; upstream `basicmachines/wiremill/overlay_running/top.png`)
  - `wiremill_overlay_left_running.png`  `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`  (byte copy; upstream `basicmachines/wiremill/overlay_running/left.png`)
  - `wiremill_overlay_right_running.png`  `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`  (byte copy; upstream `basicmachines/wiremill/overlay_running/right.png`)
  - `wiremill_overlay_back_running.png`  `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`  (byte copy; upstream `basicmachines/wiremill/overlay_running/back.png`)
  - `transformer_rotation_overlay_front.png`  `d3e79dc81f8bd97609ad0e3dcda2656d4b0a085811466f46801db3e19e57f04a`  (byte copy [ART-DUBIOUS: all-black art, entity dubious]; upstream `transformers/transformer_rotation/overlay/front.png`)
  - `transformer_rotation_overlay_back.png`  `d3e79dc81f8bd97609ad0e3dcda2656d4b0a085811466f46801db3e19e57f04a`  (byte copy [ART-DUBIOUS: all-black art, entity dubious]; upstream `transformers/transformer_rotation/overlay/back.png`)
  - `transformer_rotation_overlay_side.png`  `d3e79dc81f8bd97609ad0e3dcda2656d4b0a085811466f46801db3e19e57f04a`  (byte copy [ART-DUBIOUS: all-black art, entity dubious]; upstream `transformers/transformer_rotation/overlay/side.png`)

`transformer_rotation` flag — the three `overlay` PNGs are visually ALL-BLACK
upstream; whether that is intentional port art or a dead layer is unproven
(the art-entity question from the p28 census). Borrowed as-is per the card
spec and flagged `[ART-DUBIOUS]` above; the B render card must not wire them
into a live model without a runClient visual check first. The family's
`colored_active`/`overlay_active` layers stay unborrowed (the port transformer
model has no active-state visuals; outside the card scope).

`engines/` domain — colored-only per the p28 census (the upstream code
registers only the colored trio there, EngineSteam.java:277-279; the disk
`overlay/` group is NOT borrowed — spec edge, "no guessing overlays"). The
port's steam engine already carries those three bytes from p12 as
`steam_engine_front/back/side.png`; the script re-verified all three
byte-identical to upstream `engines/kinetic_steam/colored/` and borrowed
nothing (CONFIRM lines in the script output).

Idempotence: a second `--check` run over the landed tree reports
borrowed=0 in-place=255 declared=0 errors=0.

Copied on 2026-09-12. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Electric Transformer family block textures, task p28-c-ulv-lv-transformer: the 2 PNGs
under `gt6/textures/block/electric_transformer_{front,side}.png` are BAKED composites of
the upstream transformer_electric iconset — `colored/front.png` (or `/side.png`, the
colored trio is one byte-identical grayscale base) src-over `overlay/front.png` (or
`/side.png`), the p19 distillery-front canonical treatment (colored+overlay bake into a
single-layer opaque PNG; the upstream runtime mRGBa tint and the two-layer stack are the
render pool card — the p12 rotation-transformer posture repeated). The upstream
`overlay_active/` trio stays unborrowed (the port model has no active-state visuals;
the W2 render card owns it).

Source layers (borrowed bytes, NOT landed as files):
- colored/front.png `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- colored/side.png  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (identical base)
- overlay/front.png `5c728ec16e0f33fd41d656f32d8f697d9622e324666e909d37ed0af386530594`
- overlay/side.png  `48b6811ef825b2be50dcbda29ef5e2a3f707708a499a2d3796561ddb29770185`

Baked products (the landed files):
- electric_transformer_front.png `97f5c41837e8c9f94bbaec4873d3d7f0529f1146974569d90d7fffecadc06503`
- electric_transformer_side.png  `a982cea2f5d79df29801e1b21349e617488c324ea79a64b35712658bb60a4c9f`

Baked on 2026-09-13 (bake_distillery_fronts.py src_over/encode_png functions verbatim,
inline pass). Upstream license: **CC0 1.0 Universal Public Domain Dedication** (same
upstream `README.md` block as above).
- `gt6/textures/block/parts/<family>/<design>/{colored,overlay}/{bottom,top,side}.png` —
  the multiblock part-family design textures (372 files, task p29-w3-nbtdesign-parts;
  upstream `textures/blocks/machines/multiblockparts/<family>/<design>/...` verbatim,
  families: metalwall 0-7, metalwalldense 0-7, coil 0-1, woodwall, centrifugeparts 0-8,
  electrolyzerparts 0-7, distillationtowerparts 0-1, sluiceparts 0-7, crusherwheels 0-3,
  shredderblades 0-3, ventilationunit, processorversatile/logic/control/storage/conversion,
  heatacceptor, firebricks). Upstream tints the colored layer with the part material's
  mRGBa and alpha-overlays the overlay layer; the port renders the borrowed grayscale
  colored layer un-tinted and carries the overlay as a separate 0.01-offset decal element
  (the familyMachineModel two-layer form) — declared deviation, the material tint rides
  the render pool card.

  Bedrock-drill append, task p30-pool-drillhead-18103 (copied 2026-09-16): the
  `bedrockdrill/0` family (6 files — colored 3 + overlay 3, byte copies of the upstream
  `multiblockparts/bedrockdrill/0/` layers, snapshot v6.17.06) joined for the missed
  18103 Bedrock Mining Drill Head part row (Loader :1178, NBT_TEXTURE "bedrockdrill",
  NBT_DESIGNS 0). Upstream license: **CC0 1.0 Universal Public Domain Dedication**.
- `gt6/textures/block/turbine_mains/<family>/<group>/{bottom,top,side}.png` — the Large
  Turbine / Large Dynamo controller main textures (36 files, task p29-w3-turbine-dynamo;
  upstream `textures/blocks/machines/multiblockmains/{largeturbine,gasturbine,largedynamo}/
  <group>/...` verbatim, groups: colored, colored_front, overlay, overlay_front). The
  <family> dir holds the four upstream groups verbatim; the blockstate models compose the
  front face from the *_front pair and the other five faces from the plain pair (the
  familyMachineModel two-layer form, the card ① convention). Aggregate sha256 of the 36
  files (find | sha256sum): `46b815afba72c38d...` — per-file hashes omitted per the
  372-file parts precedent. Copied on 2026-09-14. Upstream license: **CC0 1.0 Universal
  Public Domain Dedication** (same upstream `README.md` block as above).

Heat-smelter append, task p29-w3-heat-smelter (copied 2026-09-14): two new
basicmachines families joined the local registration face — `smelter` (the HU
4-ladder :1431-1434) and `melter` (the single :1657) — via the same pipeline:
borrow_port_overlays census self-discovery (the GTMachines
`TD.Energy.HU, "smelter"/"melter",` literal pairs) landed 2 x (colored 6 + 3
states x 5 side faces) = 42 borrows, byte copies of the upstream
`basicmachines/{smelter,melter}/` layers (snapshot v6.17.06-22-g3703e4030;
the two families share the same generic machine plate bases — e.g.
`colored/front.png`
`db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`); and
bake_machine_fronts --split-fronts landed the 2 x 4 front products
(`<fam>_colored_front` + the three overlay front states, strips cropped to
FRAME 0, `overlay/front.png` sha256
`60fef49fb5c540732ed7153fab55864ad8756954ddaeee1051b6209e739785a8` shared).
Upstream tints the colored layer with the machine material's mRGBa and
alpha-overlays the state layers; the port renders the grayscale colored layer
through the runtime paint tint (tintindex 0) and carries the overlays as
untinted decal elements (the familyMachineModel two-layer form).

Heat-exchanger append, task p29-w3-heat-smelter (composited 2026-09-14): the
Large Heat Exchanger controller borrows the upstream
`multiblockmains/largeheatexchanger/` group (snapshot v6.17.06-22-g3703e4030)
— the colored base alpha-over the overlay layer composited into the single
`large_heat_exchanger/main.png` (sha256 `bdb119c163a62dae92c6cfde9f5c40c4fe1
ba1965e5ded6231824d1c67ad88ee`, all three upstream faces composite to the
SAME visible pixels, the lightningrod one-texture ruling).

- `gt6/textures/item/battery/{lead_acid,alkaline,nicd,licoo2,limn,energium_red,energium_cyan}.png`
  and `gt6/textures/item/battery/cell.png` — the battery-family item sprites (task
  p29-w4-battery-storage), byte-copy borrows from upstream
  `src/main/resources/assets/gregtech/textures/blocks/machines/batteries/…/sides.png|top.png`
  (eu/standard 8/32/128, eu/advanced 8/32, lu/8/32; the cell = eu/standard/8/top).
  Upstream tints the grayscale colored icons per battery family via the MTE NBT_COLOR
  mRGBa lane; the port BAKES the per-family sprites instead (the crank un-tinted borrow
  posture one declared step further — the runtime tint lane is the render-pool card).
  Produced by `mdk/tools/bake_battery_textures.py` (pure stdlib, byte-copy mode).
- `gt6/textures/block/battery_box.png` and `battery_box_large.png` — the BatteryBox side
  sprites (task p29-w4-battery-storage), BAKED src-over composites of upstream
  `textures/blocks/machines/energystorages/battery_electric[_large]/colored/side.png`
  + `overlay/side.png` (the transformer bake treatment). Produced by
  `mdk/tools/bake_battery_textures.py`; the circuit-carrier items reuse the in-repo
  `item/integrated_circuit.png` (no new file, the declared placeholder).
Hot-lube append, task p29-w4-hot-lube (2026-09-15): the Lubricant Bucket item
icon borrows the VANILLA `item/bucket.png` (1.20.1 client jar, 16x16,
byte-identical, sha256 verified) — the port's declared single-item-container
face needs a neutral bucket glyph; the tinted/filled upgrade rides the
fluid-container capability card.

- `gt6/textures/item/lubricant_bucket.png` — vanilla
  `assets/minecraft/textures/item/bucket.png` verbatim,
  sha256 `3f14980d6d2dea8d547e44104a55a51d62bee71bd45f16068113b279b3d2d042`
Machine-face-four append, task p29-w5-t3-machine-face-four (2026-09-16): the
soft hammer model carries NO new sprite — all four layers are existing
material_sets borrows (rubber/tool_head_hammer + overlay = the upstream
ANY.Rubber primary default, GT_Tool_SoftHammer.getIcon :120; wood/stick +
overlay = the MT.WOODS.Spruce secondary, the builder-wand borrow posture).

- `gt6/textures/item/monkey_wrench.png` + `monkey_wrench_overlay.png` —
  byte-copy borrows of upstream
  `textures/items/iconsets/MONKEYWRENCH[_OVERLAY].png` (snapshot
  v6.17.06-22-g3703e4030, the wrench row shape).
- `gt6/textures/item/magnifying_glass.png` + `magnifying_glass_overlay.png` —
  byte-copy borrows of upstream `textures/items/iconsets/MAGNIFYING_GLASS[_OVERLAY].png`.
- `gt6/textures/item/pincers.png` + `pincers_overlay.png` — byte-copy borrows
  of upstream `textures/items/iconsets/PINCERS[_OVERLAY].png`.

Electric nineteen append, task p29-w5-t6-electric-nineteen (2026-09-16): the
19 electric-tool models borrow the 12 upstream iconset sprites (one borrow
per icon face; the drill/chainsaw/wrench ladders share the POWER_UNIT_LV/MV/HV
top pass, the monkey-wrench rows reuse the wrench head art — the upstream
MonkeyWrench getIcon inherits GT_Tool_Wrench_LV verbatim).

- `gt6/textures/item/electric/*.png` — byte-identical upstream borrows from
  `assets/gregtech/textures/items/iconsets/` (power_unit_lv/mv/hv, jackhammer,
  handle_buzzsaw, handle_electric_screwdriver, tip/handle_electric_drill,
  tip/handle_electric_mixer, tip/handle_electric_trimmer). The head faces
  (layer0 of the drill/chainsaw/wrench/buzzsaw/screwdriver/trimmer rows) ride
  the IN-REPO `item/material_sets/metallic/tool_head_*` sprites (no new file,
  the chisel/file/saw row convention). The OVERLAY passes are cut — the port
  draws the base borrow untinted (the family single-tier deviation).
Pocket append, task p29-w5-t7-pocket-eight (2026-09-16): the eight pocket-multitool
item icons + their OVERLAY passes borrow the upstream 1.7.10 iconset sprites
byte-identical (16 files, POCKET_MULTITOOL_* + POCKET_MULTITOOL_*_OVERLAY.png from
`textures/items/iconsets/`, the closed multitool sprite = the "Multitool" form per
Textures.ItemIcons.POCKET_MULTITOOL_CLOSED, GT_Tool_Pocket_Multitool.getIcon :42-44;
the CUTTER pair serves the wire-cutter form). Renamed to the snake id tails under
`gt6/textures/item/pocket/` (multitool/knife/saw/file/screwdriver/wire_cutter/
scissors/chisel + _overlay), 16x16 RGBA, sha256s of the copies match the sources.


GT6 ore block overlay textures, task p30-ore-2-textures: the 60
`gt6/textures/block/materialicons/<set>/{ore,ore_small,ore_overlay,ore_small_overlay}.png`
files come from upstream `https://github.com/GregTech6/gregtech6` snapshot `v6.17.06-22-g3703e4030`, files
`src/main/resources/assets/gregtech/textures/blocks/materialicons/<SET>/<Name>.png`,
byte-identical to upstream, sha256 verified per file (manifest below). Consumers:
the ore block atlas sources `gt6:block/materialicons/<set>/{ore,ore_small}`
(GT6Atlases <- GTOreBakedModel.overlaySprites(), 15 SETs over the 53-material
registration axis, task p30-ore-1-mech) — the missingno intermediate state of
p30-ore-3-datagen closes with this wave; the `_overlay` pair is the upstream
pass-1 extension (TextureSet.java:113-116), borrowed for pair completeness
(the p27 posture), no modern consumer yet. Naming follows the P20/P27 rules:
set folder lowercased, icon segment camelCase -> snake_case
(oreSmall -> ore_small), `_OVERLAY` -> `_overlay`. Audit census
(audit_ore_textures.py): 5 vanilla families x SET x 2 forms all present,
zero defers — 100% upstream presence over the whole matrix. GT17 stone bases need NO borrow — block/stones/<snake>/{stone,cobble}.png
already in repo (the 28 shared base models covered: 11 vanilla anchors free +
17 GT stones), the card's ore_bases/ 34-borrow plan dedupes to zero. No
`.mcmeta` animations exist in the borrowed cells. Upstream license: **CC0 1.0
Universal Public Domain Dedication** (same upstream `README.md` block as above).

- `COPPER` -> `gt6/textures/block/materialicons/copper/`:
  - `ore.png` `406b03465bbf84a9fb1d1be0a0aa750d11572e5e72e44eed466f8b6906c57081`
  - `ore_small.png` `da732a986333a1f319175ba007eabf7db01d2c3509ac3bdeaf2f098c802df6d3`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `CUBE` -> `gt6/textures/block/materialicons/cube/`:
  - `ore.png` `406b03465bbf84a9fb1d1be0a0aa750d11572e5e72e44eed466f8b6906c57081`
  - `ore_small.png` `da732a986333a1f319175ba007eabf7db01d2c3509ac3bdeaf2f098c802df6d3`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `DIAMOND` -> `gt6/textures/block/materialicons/diamond/`:
  - `ore.png` `5a358784e55d023adf5da914c548dd6e71b664b0751a8d66635772a48b348789`
  - `ore_small.png` `8c021e43df7c644075a9402b8a5bc735583f6f17e3ec2f0f5e566699f5feb305`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `DULL` -> `gt6/textures/block/materialicons/dull/`:
  - `ore.png` `120fcc8c75f8f90c25e095f6bb8549c25d7b9e54c2428318c157f76c9b793501`
  - `ore_small.png` `e90c89a6c16dcbb54c0bc7f14fd82881b721a8a5a5058ee05ef1d3c41ea3a8ee`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `FINE` -> `gt6/textures/block/materialicons/fine/`:
  - `ore.png` `7c7d321145547b556739f43f1187fd75d9614913ddc8bb6c4c43ff067f916b9b`
  - `ore_small.png` `e2ea0bbee3f2ef1295a68142cc015f5c4f32fece0929edddbc183ac063840774`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `FLINT` -> `gt6/textures/block/materialicons/flint/`:
  - `ore.png` `7e409880ff4df0945dd757f094bc688a507e6dfc18145cb11ed23c5e678fdd81`
  - `ore_small.png` `66b1ff5ef23aebbf83549a4788e197c4e21c76be4fc6289cec790e932d3221e1`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `LAPIS` -> `gt6/textures/block/materialicons/lapis/`:
  - `ore.png` `7e409880ff4df0945dd757f094bc688a507e6dfc18145cb11ed23c5e678fdd81`
  - `ore_small.png` `66b1ff5ef23aebbf83549a4788e197c4e21c76be4fc6289cec790e932d3221e1`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `LIGNITE` -> `gt6/textures/block/materialicons/lignite/`:
  - `ore.png` `7c7d321145547b556739f43f1187fd75d9614913ddc8bb6c4c43ff067f916b9b`
  - `ore_small.png` `e2ea0bbee3f2ef1295a68142cc015f5c4f32fece0929edddbc183ac063840774`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `METALLIC` -> `gt6/textures/block/materialicons/metallic/`:
  - `ore.png` `406b03465bbf84a9fb1d1be0a0aa750d11572e5e72e44eed466f8b6906c57081`
  - `ore_small.png` `da732a986333a1f319175ba007eabf7db01d2c3509ac3bdeaf2f098c802df6d3`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `QUARTZ` -> `gt6/textures/block/materialicons/quartz/`:
  - `ore.png` `7c7d321145547b556739f43f1187fd75d9614913ddc8bb6c4c43ff067f916b9b`
  - `ore_small.png` `e2ea0bbee3f2ef1295a68142cc015f5c4f32fece0929edddbc183ac063840774`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `RAD` -> `gt6/textures/block/materialicons/rad/`:
  - `ore.png` `7309820ac6ea8ac18fa37da6b580058d80b8ecc9b4cf0a0de5820220b8b9ed2b`
  - `ore_small.png` `7354b58b970e04a4f3aa477fd14c6cad4642b526a344eb825d0ef65b05a753bf`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `REDSTONE` -> `gt6/textures/block/materialicons/redstone/`:
  - `ore.png` `406b03465bbf84a9fb1d1be0a0aa750d11572e5e72e44eed466f8b6906c57081`
  - `ore_small.png` `da732a986333a1f319175ba007eabf7db01d2c3509ac3bdeaf2f098c802df6d3`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `RUBY` -> `gt6/textures/block/materialicons/ruby/`:
  - `ore.png` `5a358784e55d023adf5da914c548dd6e71b664b0751a8d66635772a48b348789`
  - `ore_small.png` `8c021e43df7c644075a9402b8a5bc735583f6f17e3ec2f0f5e566699f5feb305`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `SHINY` -> `gt6/textures/block/materialicons/shiny/`:
  - `ore.png` `95322b721304c5b4bd9798bb61461aef7a2e1e95c0f1b9f75396368af9c6412c`
  - `ore_small.png` `570570a46ae308c82cf041630c1474f6968eeb89c73794056c14f2f73920de6d`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
- `SPACE` -> `gt6/textures/block/materialicons/space/`:
  - `ore.png` `406b03465bbf84a9fb1d1be0a0aa750d11572e5e72e44eed466f8b6906c57081`
  - `ore_small.png` `da732a986333a1f319175ba007eabf7db01d2c3509ac3bdeaf2f098c802df6d3`
  - `ore_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`
  - `ore_small_overlay.png` `0940268eecf5efbfa5d2ead4e701af85aad1c77849d06850003add66e433b5d4`

## Surface-plants + fallen-woods block textures (task p30-w6-t2-surface-blocks)

13 upstream block-texture borrows from `src/main/resources/assets/gregtech/textures/blocks/`
(GregTech 6, snapshot `v6.17.06-22-g3703e4030`) onto `gt6/textures/block/`:

- `gt6/textures/block/glowtus.png` — the glowtus water plant (upstream
  `iconsets/GLOWTUS_RED.png`, one sample of the 16-colour dye ladder; the collapse is
  declared in GT6GlowtusBlock; sha256
  `97bea0cd9618fcec9ce67c235afbd83b11fec326068864afd35afe6f617a4326`)
- `gt6/textures/block/berry_bush.png` — the berry bush (upstream
  `machines/plants/bush/colored/bush.png`, the grayscale tintable icon PRE-COLOURED at
  borrow time with the leaf-green multiplier (0.35, 0.62, 0.22) — the grass-card
  pre-coloured-PNG precedent, the upstream tint rode the per-berry NBT colour;
  sha256 `4bd8830da402ce8f636a6cd4bc9e293e72fb64d927d65221c63979071a8d8c23`)
- `gt6/textures/block/black_sand.png` — the magnetite river sand (upstream
  `iconsets/SAND_MAGNETITE.png`; sha256
  `f512f0776c14d38f5a8c40e8277513e0c569cf2ddb460a0da5d1e9535e6529f8`)
- `gt6/textures/block/turf.png` — the swamp turf (upstream `iconsets/TURF.png`;
  sha256 `832642f9834839f0396c01c9b6bc3b4b18b933cd6b6b6169cc9cc28d582db347`)
- `gt6/textures/block/tree/log_side_dead.png` / `log_top_dead.png` — the Dead log
  (upstream `iconsets/LOG_SIDE_DRY.png`/`LOG_TOP_DRY.png`, renamed to the block id;
  sha256 `274cc0f0c6b4e8535a591f4cbd1f4dcb22bb4debe6124169f381614845341025` /
  `db21f8801d35f47dc5b2f6b1d0d6b1305faeec6b4a8defa76d6380b55c956981`)
- `gt6/textures/block/tree/log_side_rotten.png` / `log_top_rotten.png` — the Rotten log
  (upstream `iconsets/LOG_SIDE_ROTTEN.png`/`LOG_TOP_ROTTEN.png`; sha256
  `1ba5ad5760379f71de9bcc1240bdad244213abcdee510b7a62b139d4c72ed5f3` /
  `b6cdd4f2791a2c5f36ed9167ae70f4267c7cc81759ee25a9ac35a9cfd757529b`)
- `gt6/textures/block/tree/log_side_mossy.png` / `log_top_mossy.png` — the Mossy log
  (upstream `iconsets/LOG_SIDE_MOSSY.png`/`LOG_TOP_MOSSY.png`; sha256
  `2189d4da69b901344922fc046eb00dac065428619f1ea838b6c06508a40ab355` /
  `14fd2ed60eeba5e99c529c9cb340064d0af3aa29355f7fab29d091649f991b77`)
- `gt6/textures/block/tree/log_side_frozen.png` / `log_top_frozen.png` — the Frozen log
  (upstream `iconsets/LOG_SIDE_FROZEN.png`/`LOG_TOP_FROZEN.png`; sha256
  `3d3b5a8f1973c6714f859cec1381e042f58b3ee9621e9bd97532502f10142366` /
  `ca3053f9843ff4f3475051e21667ccce90a4c575dc7dd7fbf7b59e207771b33c`)

- `gt6/textures/item/comb/comb_<name>.png` (×20) — the bee-comb item icons (task
  p31-bees-lv1). PORT-GENERATED ART (not borrowed): one 16×16 five-cell honeycomb
  silhouette, per-comb tinted — the upstream 1.7.10 combs carry no dedicated item PNGs
  (the MultiItemFood icons composite from the FOOD material icon set at runtime), so the
  port declares the tint family: honey e8b84a / water 6fa8e8 / magic b070e0 / nether
  c04030 / end 60c8c0 / rock 909090 / jungle 70a840 / frozen b8d8f0 / shroom c08070 /
  sandy d8c890 / clay a0a8b8 / sticky e8c860 / royal f0d040 / soul 504858 / amnesic
  c8a0e8 / military 808850 / pyro e87830 / cryo 90c8e8 / aero f0e8a0 / tera 986838.
  Regeneration recipe: the five-cell ellipse grid (rim = tint, cell interior = 55%,
  top edge = 125%, bottom edge = 45%), PIL, RGBA 16×16.

- `gt6/textures/item/bumble/bumble_{drone,princess,queen,dead}.png` (×4) — the
  bumblebee face icons (task p33-bees-lv3-a-items). PORT-GENERATED ART (not
  borrowed): one 16×16 bee silhouette per fractal face — the scanned items reuse
  the base-face sprite (the scan state is the item id, not the art). drone = the
  yellow/black striped bee (white wings, dark head, antenna), princess = the
  orange-bodied form, queen = the gold body + the three-pixel gold crown, dead =
  the grey-brown belly-up form with the X eyes. Regeneration recipe: the wing
  pair (translucent white ellipses) + the body ellipse with three vertical
  stripe bands + the dark head ellipse + the antenna pixels; the queen adds the
  crown pixels, the dead form flips vertically and greys; PIL, RGBA 16×16.

Massfab machine + controller textures, task p31-massfab: the 24 small-family PNGs
under `gt6/textures/block/massfab_{colored,overlay}_{face}.png` and
`gt6/textures/block/massfab_overlay_{face}_{active,running}.png` plus the 6
large-controller PNGs under `gt6/textures/block/largemassfab_colored_{face}.png`
(face ∈ bottom/top/front/back/left/right) come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/massfab/{colored,overlay,overlay_active,overlay_running}/<face>.png`
and `.../basicmachines/largemassfab/colored/<face>.png` (the NBT_TEXTURE "massfab" /
"largemassfab" name form of Loader_MultiTileEntities.java:1542-1546/:1241),
byte-identical to upstream, sha256 verified per file:
- `massfab_colored_bottom.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_overlay_bottom.png` — `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`
- `massfab_overlay_bottom_active.png` — `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`
- `massfab_overlay_bottom_running.png` — `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`
- `largemassfab_colored_bottom.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_colored_top.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_overlay_top.png` — `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`
- `massfab_overlay_top_active.png` — `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`
- `massfab_overlay_top_running.png` — `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`
- `largemassfab_colored_top.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_colored_front.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_overlay_front.png` — `1533e6e9610e98ed2ba9163b39d6370be0c97eee9808be971abd8103665d78be`
- `massfab_overlay_front_active.png` — `2efd0b5fc366ddcfaf89231730e01571ccf91dfcfc0fca8b8749b8483b656c50`
- `massfab_overlay_front_running.png` — `dc48b3c85dc70970c32d980a10184f918de415336e1f2886a53d891145553999`
- `largemassfab_colored_front.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_colored_back.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_overlay_back.png` — `d1152f690913d11bc286fec25b44a44cf4ce38ded8c72c3da2b404f353ebdbeb`
- `massfab_overlay_back_active.png` — `8aa1e3925d65fd057270593e8f56b445ebb8287f872fe12f743c695a28aae8db`
- `massfab_overlay_back_running.png` — `8aa1e3925d65fd057270593e8f56b445ebb8287f872fe12f743c695a28aae8db`
- `largemassfab_colored_back.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_colored_left.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_overlay_left.png` — `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `massfab_overlay_left_active.png` — `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `massfab_overlay_left_running.png` — `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `largemassfab_colored_left.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_colored_right.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `massfab_overlay_right.png` — `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `massfab_overlay_right_active.png` — `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `massfab_overlay_right_running.png` — `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `largemassfab_colored_right.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`

Fusion reactor / Implosion Compressor / Von da Graagg controller faces, task
p32-hygiene-lang-assets (attribution backfill for the P31 machine wave): the 18
controller PNGs `gt6/textures/block/fusionreactor_colored_{face}.png`,
`gt6/textures/block/implosioncompressor_colored_{face}.png` and
`gt6/textures/block/vondagraagg_colored_{face}.png` (face ∈
bottom/top/front/back/left/right; borrow commits p31-implosion 35830a9eb,
p31-fusion 6f118eef7, p31-graagg d4e418beb) come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/`:

- `basicmachines/fusionreactor/colored/<face>.png` (the NBT_TEXTURE
  "fusionreactor" name form of Loader_MultiTileEntities.java:1242)
- `basicmachines/implosioncompressor/colored/<face>.png` (NBT_TEXTURE
  "implosioncompressor", Loader_MultiTileEntities.java:1228)
- `multiblockmains/vondagraagg/colored/{side,top,bottom}.png` (NBT_TEXTURE
  "vondagraagg", Loader_MultiTileEntities.java:1280)

byte-identical to upstream, sha256 verified per file (upstream ships one
uniform tile per machine, so a family shares one digest):

- `fusionreactor_colored_back.png` —
  `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `fusionreactor_colored_bottom.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `fusionreactor_colored_front.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `fusionreactor_colored_left.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `fusionreactor_colored_right.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `fusionreactor_colored_top.png` — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `implosioncompressor_colored_back.png` —
  `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `implosioncompressor_colored_bottom.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `implosioncompressor_colored_front.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `implosioncompressor_colored_left.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `implosioncompressor_colored_right.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `implosioncompressor_colored_top.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `vondagraagg_colored_back.png` — `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`
- `vondagraagg_colored_bottom.png` — `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`
- `vondagraagg_colored_front.png` — `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`
- `vondagraagg_colored_left.png` — `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`
- `vondagraagg_colored_right.png` — `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`
- `vondagraagg_colored_top.png` — `37dab1b9c5e4acbb51dd8df66cb4ffb391d9eae3c6ee95c8fc5d15b3969e8810`

(vondagraagg mapping: back/front/left/right ← upstream `side.png`, top ←
`top.png`, bottom ← `bottom.png` — upstream's three Graagg tiles are
byte-identical, one digest covers the six faces.)

Naming follows the massfab precedent: upstream lowercase face names verbatim,
`<machine>_colored_<face>` flat under `textures/block/`. The upstream
`overlay`/`overlay_active`/`overlay_running` state bands of the two
basicmachines families and the Graagg `colored_front`/`overlay_front` formed
faces are NOT borrowed — the port machines declare the plain colored controller
look (the multi-block formed look rides the frame-face blocks, the p31 card
specs). License: upstream GregTech 6 assets are CC0 (see the Public Domain
Dedication block above).

## USB Stick item textures (task p32-usb-data)

The four USB Stick item icons under `gt6/textures/item/usb_stick_{1,2,3,4}.png` come
from upstream `src/main/resources/assets/gregtech/textures/items/gt.multiitem.technological/`
(item ids 32001-32004, the MultiItemTechnological.java:791-794 rows), byte-identical
to upstream, sha256 verified per file:

- `usb_stick_1.png` — `07f868d00725f1c738752ca84831b82b939f1a30d667ad29482209e830022cbe` (upstream `32001.png`)
- `usb_stick_2.png` — `4ec7896cca8accb80512d1dee55b3021cb491c15cd598e73f97a1c4dd287691b` (upstream `32002.png`)
- `usb_stick_3.png` — `795405d6c96c7a91cc1ed70c38413bab64d7b459051461d97ca93c437302d446` (upstream `32003.png`)
- `usb_stick_4.png` — `c08a8b8789e8e48b93e0cec5a8f49fb419c968d5fb918f52e3db48ece69180eb` (upstream `32004.png`)
Bumble-hive block art borrowed from **GregTech 6**
(task p32-bees-lv2, upstream
`textures/blocks/nature/bumblehive/{colored,overlay}/{bottom,top,side}.png`,
byte-identical, filenames flattened to `bumblehive_colored_<face>` /
`bumblehive_overlay_<face>`): the MTE 32755 colored-grayscale + overlay pair —
the colored layer rides tintindex 0 (the p21 paint seat; worldgen paints the
biome-family colour, GTMachinePaintTint resolves the BE PAINT), the overlay
layer is the untinted decal (the machineModel two-layer grammar).

## Laser domain textures (task p32-qu-laser-domain)

The two gas laser emitter item icons under `gt6/textures/item/` and the four converter
block faces under `gt6/textures/block/` come from upstream
`src/main/resources/assets/gregtech/textures/` (items/gt.multiitem.technological/
item ids 11000/11008, the MultiItemTechnological.java:384/:394 rows;
blocks/machines/lasers/laser_electric/colored/ and
blocks/machines/laserabsorbers/electric_laser/colored/, the Loader:930-934/:976-980
art rows), byte-identical to upstream, sha256 verified per file. The colored layer is
landed alone (the overlay/overlay_active trios stay unborrowed — the port machines
declare the plain colored look, the bridge-family posture; the beam is not rendered,
the task card 光束 defer). License: upstream GregTech 6 assets are CC0 (see the Public
Domain Dedication block above).

- `item/comp_laser_gas_empty.png` — `027d334701463e1452c2ac835bde864848b505fe5d8bd43284685f636929620c` (upstream `11000.png`)
- `item/comp_laser_gas_co2.png` — `29280d669152b8d3b4bd955a75f6bc9919e8afcc8eb655d5171564f91bb03fad` (upstream `11008.png`)
- `block/laser_electric_front.png` — `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977` (upstream `lasers/laser_electric/colored/front.png`)
- `block/laser_electric_side.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (upstream `lasers/laser_electric/colored/side.png`)
- `block/laser_absorber_front.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (upstream `laserabsorbers/electric_laser/colored/front.png`)
- `block/laser_absorber_side.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (upstream `laserabsorbers/electric_laser/colored/side.png`)

## Magic absorber texture (task p32-magic-absorber)

The absorber block face under `gt6/textures/block/` comes from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/magicenergyabsorber/colored/side.png`
(the Loader:1005 art row), byte-identical to upstream, sha256 verified. The four
upstream colored faces (side_facing/side/top/bottom) are ONE byte-identical grayscale
file — the port borrows it once as `magic_absorber_base.png` and maps every cube face
to it (the in-game look rides the machine tint seat + the overlay trios, both
unborrowed: the overlay/overlay_active activity visual is the W2 render pool, the
static-face posture of the bridge/laser families). License: upstream GregTech 6 assets
are CC0 (see the Public Domain Dedication block above).

- `block/magic_absorber_base.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e` (upstream `magicenergyabsorber/colored/side.png`; side_facing/top/bottom byte-identical)

## Quantum Energizer textures (task p32-qu-energizer)

The two converter block faces under `gt6:textures/block/` are DERIVED, not borrowed:
both are the committed laser-absorber face (`laser_absorber_{front,side}.png`, the
upstream `laserabsorbers/electric_laser/colored/` pair) with a per-pixel
luminance-preserving amber-gold tint (the grayscale noise mapped to r×1.10/g×0.95/
b×0.52 +38/+18/−12 on the front, ×1.05/×0.98/×0.70 +18/+10/−8 on the side) — the
upstream quantumenergizer art (`machines/quantumenergizer/quantum_laser/`) is an
icon-container composite the port's static-face datagen has no seat for (the laser
card's colored-layer-alone posture). The tint is the rung identity: one machine family,
one hue. License: the base faces are upstream GregTech 6 CC0 (see above), the tint is
this project's.

- `block/quantum_energizer_front.png` — `f79d1113d3d95de0466b0eea881a4d8a0fc002b0fd356371e7f12fa125ba48eb` (derived: tinted `laser_absorber_front.png`)
- `block/quantum_energizer_side.png` — `c257afe7a8678434de349e86f0be010abc78cbcfd4405f2126a784e8715df5d3` (derived: tinted `laser_absorber_side.png`)

## task p32-placeables (2026-09-19) — the placeables band

Borrowed from **GregTech 6** (https://github.com/GregTech6/gregtech6), snapshot
`v6.17.06-22-g3703e4030`, `src/main/resources/assets/gregtech/textures/`:

- `block/greg_o_lantern.png` — the carved GREG face (upstream
  `blocks/iconsets/GREG_O_LANTERN.png`, Loader_MultiTileEntities.java:2031; sha256
  `d75a0aac2658b96a42238244dd9f2c5ef4fc9bf35080eee6decc4aa61f3dccfd`). Byte-identical;
  the other five lantern faces are the vanilla lit_pumpkin (the upstream
  BlockTextureCopied borrow). Upstream tints the icon with mRGBa; the port ships the
  UNCOLORED face (no paint face — the declared embeddium-tint no-touch).
- `block/placeable/ingot_sides.png` / `ingot_top.png` — the placed-ingot pile faces
  (upstream `blocks/machines/placeables/ingot/{sides,top}.png`; sha256 below).
- `block/placeable/plate_sides.png` / `plate_top.png` — the placed-plate faces
  (upstream `blocks/machines/placeables/plate/{sides,top}.png`).
- `block/placeable/plate_gem_sides.png` / `plate_gem_top.png` — the placed gem-plate faces
  (upstream `blocks/machines/placeables/plateGem/{sides,top}.png`).
- `block/placeable/scrap_sides.png` / `scrap_top.png` — the placed-scrap faces
  (upstream `blocks/machines/placeables/scrap/{sides,top}.png`).

License: upstream GregTech 6 assets are CC0 (see the Public Domain Dedication block
above). The placed ROCK/STICK piles carry NO new PNG — the upstream copies
Blocks.stone/Blocks.log verbatim (MultiTileEntityRock.java:55, MultiTileEntityStick
.java:51), so the port models reference the vanilla textures directly (the W6
surface-band form). `block/sandwich.png` is PORT-ORIGINAL art (the upstream sandwich
renders per-ingredient icons; the port collapses to one layered profile — the declared
render cut), no attribution owed.

Per-file sha256 (byte-identical copies):

- `block/placeable/ingot_sides.png` `c15a59ebd63b49c5a4e031fc1f84f3efd66ca5d4770e4cf3ffad6fe7dbaf3156`
- `block/placeable/ingot_top.png` `30088271d7b7eef72f1778139dea109942cfec536218dd30e66feb846589fb9b`
- `block/placeable/plate_sides.png` `4b8045e02d7f7c2af3136ea1cab4b8032f6271c4b4d719f8183bf28f403f7db6`
- `block/placeable/plate_top.png` `ea1db3d6c20ff3d18b588a9bc9c71a51bb3a361d528ea1a8385e18e33bc9f9d6`
- `block/placeable/plate_gem_sides.png` `fbe0854aa49345595159f1cc41b998765ec66d7733990ccf3315b9128bbecebb`
- `block/placeable/plate_gem_top.png` `ea87db37aa367b54b98f4bcb33f460ef55b92f628a80364c8907941c61b963b4`
- `block/placeable/scrap_sides.png` `f78b93bf6759ddda850e5a5f713b15bcc8d72abd03a5fc965e10cb663c4d9836`
- `block/placeable/scrap_top.png` `f78b93bf6759ddda850e5a5f713b15bcc8d72abd03a5fc965e10cb663c4d9836` (upstream ships sides==top byte-identical; the port renames the camelCase upstream
  pair to the modern lowercase-snake resource namespace)
QU machine pair textures, task p32-qu-scanner-replicator: the 48 PNGs under
`gt6/textures/block/{scannermolecular,replicator}_{colored,overlay}_{face}.png`
(face ∈ bottom/top/front/back/left/right; the overlay fronts carry the
`_active`/`_running` state tails) come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/{scannermolecular,replicator}/{colored,overlay,overlay_active,overlay_running}/<face>.png`
(the NBT_TEXTURE "scannermolecular"/"replicator" name form of
Loader_MultiTileEntities.java:1551/:1556-1558), the p22 split-front naming —
byte-identical to upstream except the 16xN strips cropped to their frame 0 (the
P20/P22 "animation stays retired" deviation), sha256 verified per file (grouped
by content — upstream shares the grayscale body and the side decal strips
across both families and their state layers):
- every `*_colored_*.png` (12) — `d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e`
- `*_overlay_bottom.png` + `*_overlay_bottom_active.png` + `*_overlay_bottom_running.png` (6) — `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`
- `*_overlay_top.png` + `*_overlay_top_active.png` + `*_overlay_top_running.png` (6) — `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`
- `*_overlay_back.png` (2) — `d1152f690913d11bc286fec25b44a44cf4ce38ded8c72c3da2b404f353ebdbeb`
- `*_overlay_back_active.png` + `*_overlay_back_running.png` (4) — `8aa1e3925d65fd057270593e8f56b445ebb8287f872fe12f743c695a28aae8db`
- `*_overlay_left*.png` (6) — `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `*_overlay_right*.png` (6) — `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `scannermolecular_overlay_front.png` — `8d007159a7fd52242635fc9be0086b57477386bb9ac4360c2b40617f0ce851f3`
- `scannermolecular_overlay_front_active.png` — `fdd80a6a847ffd6ba74f1cfa3556b6481d2fd2a754e9edca0ffc203a3988f1a7`
- `scannermolecular_overlay_front_running.png` — `291662854eb9fc65e1f235713b0281503baa348ccbd93a1193958f0a9f1a0dce`
- `replicator_overlay_front.png` — `7e104008bd35b377838d1cf1b6422a110a395ea3dfe07ed3cb8cafdf21859c88`
- `replicator_overlay_front_active.png` — `e7d764c6722b0230aecff5d84cfa8361f9d295336565391f721f571d7da10cf8`
- `replicator_overlay_front_running.png` — `6a60b16caa227f4b5652ab9444c0818e29a542e69ccd93cae7d00a6a7113d212`

Distillation Tower GUI pair textures, task p33-gui-distill-tower:
`gt6/textures/gui/machines/distillationtower.png` and
`gt6/textures/gui/machines/cryodistillationtower.png` come from upstream
`src/main/resources/assets/gregtech/textures/gui/machines/{DistillationTower,CryoDistillationTower}.png`
(the mGUITexture = mRecipes.mGUIPath fallback, MultiTileEntityBasicMachine.java:114;
RM.java:65/:66), byte-identical to upstream, sha256 verified:
- `distillationtower.png` — `3c91fb1c405f64651f2502f44ab2fc0e588c018e0616a5d8d094acf11596c593`
- `cryodistillationtower.png` — `165f2cb65e4f1d892d6f403327ac9ae33d61084fb73faacaf99385aa5a98bd53`


Burner Mixer + Plantalyzer family textures, task p34-machines-burner-plantalyzer:
the 48 PNGs under `gt6/textures/block/{burnmixer,plantalyzer}_{colored,overlay}_{face}.png`
(face ∈ bottom/top/front/back/left/right; the overlay fronts carry the
`_active`/`_running` state tails) come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/basicmachines/{burnmixer,plantalyzer}/{colored,overlay,overlay_active,overlay_running}/<face>.png`
(the NBT_TEXTURE "burnmixer"/"plantalyzer" name form of
Loader_MultiTileEntities.java:1595-1598/:1601-1605), the p22 split-front naming —
byte-identical to upstream except the 16xN strips cropped to their frame 0 (the
P20/P22 "animation stays retired" deviation), sha256 verified per file (grouped
by content):
- `burnmixer_colored_back.png`, `burnmixer_colored_front.png`, `burnmixer_colored_left.png`, `burnmixer_colored_right.png`, `burnmixer_colored_top.png`, `plantalyzer_colored_back.png`, `plantalyzer_colored_bottom.png`, `plantalyzer_colored_front.png`, `plantalyzer_colored_left.png`, `plantalyzer_colored_right.png`, `plantalyzer_colored_top.png` — `db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e`
- `burnmixer_colored_bottom.png` — `b94934858d010e0381fd5056dbc0ec00e964cad00c152944b2e6e0918991f977`
- `burnmixer_overlay_back.png`, `burnmixer_overlay_back_active.png`, `burnmixer_overlay_back_running.png` — `f44eaecf74644cdd03d37fca05abe06c76d88769a5bd419d27bc4ce9f4885f1c`
- `burnmixer_overlay_bottom.png`, `burnmixer_overlay_bottom_active.png`, `burnmixer_overlay_bottom_running.png` — `4da8c35319cd5b640d5d2363df857edcef1c58a26545a6f1a689f5d0b8d196cb`
- `burnmixer_overlay_front.png` — `e6fad8d9272518ba9d22ab05985954ae44a18671ecad5226288d310f860909c8`
- `burnmixer_overlay_front_active.png` — `98ae5f57257e83995807065be84fe45bd4ca94fdb029b67961b69ac5752a6be5`
- `burnmixer_overlay_front_running.png` — `d2955b05a6fcbc143b07355ff9cfcde63d1f67169d03c17b1b57f5e89c2c0243`
- `burnmixer_overlay_left.png`, `burnmixer_overlay_left_active.png`, `burnmixer_overlay_left_running.png` — `a8ebffe7a5f7e4fe6b9cb606d0142372441bac9cba5b32b7be456a6bef4e241a`
- `burnmixer_overlay_right.png`, `burnmixer_overlay_right_active.png`, `burnmixer_overlay_right_running.png` — `c7a4765d842b42d645ebce6eb0848c4d792d44f87117b02399520f299027cfc3`
- `burnmixer_overlay_top.png`, `burnmixer_overlay_top_active.png`, `burnmixer_overlay_top_running.png` — `bd7c41e71dd8ab1b5fc83460af850e53ae1e7e29c48ad376f458484d1de6a00f`
- `plantalyzer_overlay_back.png` — `6f81204cb604b141dc4dc71267e918ccbb424e55ac8877848c971c4416777bae`
- `plantalyzer_overlay_back_active.png` — `162fa3c071183c7f37b3c76e8e9d60144548ca7b090665334a3d0b0598ce42e5`
- `plantalyzer_overlay_back_running.png` — `18951f0f17dde4a0687c97d2a8a82c7d79c0a0db51923f73109c84a7e67bcfbd`
- `plantalyzer_overlay_bottom.png` — `2c23a7b5cb98ff7ebc4e83946a39f35a48f0d57deb531660af73d3cf5636c18d`
- `plantalyzer_overlay_bottom_active.png` — `b0216b93095885d2f9366ed7b2942a6b266769750576ef912e794db6cfe9225e`
- `plantalyzer_overlay_bottom_running.png` — `2405a5a3e6ebdb63851e21bbada4aa0cc917b43ee4697958bc18a1bed613e78e`
- `plantalyzer_overlay_front.png` — `62b2390c4dba8a1e627eed397836dad4cb05a8d261094dba17a4bbe0766a2818`
- `plantalyzer_overlay_front_active.png` — `fb16103a318fb5d00b9cbc0e779fec1e5de6bbbd2937bad9d971312276b54c28`
- `plantalyzer_overlay_front_running.png` — `2cb35696d56aebd0ee0f18988ae8826b80ed88142c5d56e60d15293eff6d959c`
- `plantalyzer_overlay_left.png` — `0e80bbc63cdd37cf55742784232a0f6f9e6fdd2ae1ced3afc5cc8b56004cd8ce`
- `plantalyzer_overlay_left_active.png` — `465244f8043c67b2b7649ee665bacc196e633b84cd0b8a3e984b47c023b72cab`
- `plantalyzer_overlay_left_running.png` — `42aed2c29aa8d8c3d1d860f859d4a3cb4c4ffe09b092478a596f31e546460576`
- `plantalyzer_overlay_right.png` — `57a1f1f9a51d3739c53ee24abf3e4f23efe70239cccf765d2ea656acd5b2042e`
- `plantalyzer_overlay_right_active.png` — `4e044a132575f4c8c618bb28fe4b3e14c6f8deb0b7ebb7574d9ffbd06dce27e0`
- `plantalyzer_overlay_right_running.png` — `ea7ddbb40ad0aa2eff7233403e58f71ba4752f2cf829106c66e92e945e5508ac`
- `plantalyzer_overlay_top.png` — `c6e08c2c22a3ee285f9cf480716bab3506048cfb2e7502cc6f5d4f230bf6fb27`
- `plantalyzer_overlay_top_active.png` — `c8b55df2525bff5559a4e54090f1a886ed18890191c1c4eac012b5e990bddcb9`
- `plantalyzer_overlay_top_running.png` — `281b4be07d722a32965bcb7c24b0f83c082f1d70d09337afcbd83b87af5c2d0f`

Sensor block textures, batch 2 (15 rows), task p34-sensors-trivial-14 — same
borrow/bake recipe as the three pioneer rows above (upstream snapshot
`v6.17.06-22-g3703e4030`, `src/main/resources/assets/gregtech/textures/blocks
/machines/redstone/sensors/<name>/`): the upstream `colored/front.png` with the
sensor's own `overlay/front.png` (box-downscaled 4x, straight-alpha) baked
src-over into ONE 16x16 canonical PNG, pixel-identical to the pioneer bake
(PIL alpha_composite form re-verified against the three ledger hashes before
the batch run):

- `gt6/textures/block/thermometer.png` `5c8786895888a6369aadcaa6bd098a7aea1b693663125b5e2f0146a67b318b90`
- `gt6/textures/block/luminometer.png` `285ab4f92673149366770be6926f23f9c1e38824fc9ee93dbbaadb1736e26eed`
- `gt6/textures/block/chronometer.png` `65c20ba26c48cb23bdf3ad98f2d3edd3dd32f345fdecefb51278e1669ccb36df`
- `gt6/textures/block/gibblometer.png` `06b8c076747878002c91f85bf3449e662e6132ebc5ac3bdd711468d09d0d8d51`
- `gt6/textures/block/kilogibblometer.png` `854e40cf70b82c2d64615e0d5e6cbdf071ee83371ec4f73fa8e4cc5bd8bd2ecf`
- `gt6/textures/block/itemometer.png` `ba7c08807d668e3281371534becd66488461cdb98dc3a52baae75f3ac3cef14e`
- `gt6/textures/block/stackometer.png` `4e5381ca39b2589b493688c38a63e1df7c18b605fba3a3eff747717656995a9c`
- `gt6/textures/block/bucketometer.png` `ba12c22cb3593f9557a5743bdb8fbea78dded541d2e6ded976cd8048f972f1dc`
- `gt6/textures/block/kilobucketometer.png` `71524688233e99fcb5618c4e1cfe74d7ff61f23a7b3a54dab7973f8ea2c3ec7e`
- `gt6/textures/block/lightweightometer.png` `744408246f3e7ff58bd962a332356688f6d5f68dad3bc46be399a300c6201871`
- `gt6/textures/block/mediumweightometer.png` `7ae51a34cac58c2828f95265a387530e6e2278615cec706c80145b8b495c7b1a`
- `gt6/textures/block/heavyweightometer.png` `3bdd5c49cdb0c8701f9cbbc796768d84cd9df9244c00485dea72dc76a9f392e2`
- `gt6/textures/block/superheavyweightometer.png` `d8199fb95821db4132842b9c15d86863ac3bb437e76167145c2755a1c1249a05`
- `gt6/textures/block/tpsmeter.png` `7571093ede617a3d1c88579d50faf419df52bc6c991337551084d8c840b0de01`
- `gt6/textures/block/playercounter.png` `2013a9e65bb3a28603c390fe73f5f1a2d0feab28048b3410837c6fcb45ba93ed`

Sensor block textures, batch 3 (3 rows, the pool closure), task p37-sensors-3 —
same borrow/bake recipe as the pioneer + batch-2 rows above (upstream snapshot
`v6.17.06-22-g3703e4030`, `src/main/resources/assets/gregtech/textures/blocks
/machines/redstone/sensors/<name>/`): the upstream `colored/front.png` with the
sensor's own `overlay/front.png` (box-downscaled 4x, straight-alpha) baked
src-over into ONE 16x16 canonical PNG (PIL alpha_composite form re-verified
against the batch-2 tpsmeter ledger hash before the batch run):

- `gt6/textures/block/tachometer.png` `d04a519057b41bc531e855101ed14ac247b62d0b53566b9db19447721f4a6e6f`
- `gt6/textures/block/geigercounter.png` `2cf5b552cd5b61124eeaac3dfaf6e42ff630a4967053f8f03c4f6706e3aad2cb`
- `gt6/textures/block/laserometer.png` `56e118d3de4ddf627473f80a143d017d5fc7cc9b56378851da11d7cfe69f007c`
Bumbliary GUI pair textures, task p34-bumbliary-gui:
`gt6/textures/gui/machines/bumbliary.png` and
`gt6/textures/gui/machines/bumbliaryadvanced.png` come from upstream
`src/main/resources/assets/gregtech/textures/gui/machines/{Bumbliary,BumbliaryAdvanced}.png`
(the ContainerClient background pair, MultiTileEntityBumbliary.java:500/:507 and
MultiTileEntityBumbliaryAdvanced.java:469), byte-identical to upstream, sha256 verified:
- `bumbliary.png` — `16e7d4156a0e2fd74240120b479136f0c6db8c757f68f3900df08e2ac2f3a4c3`
- `bumbliaryadvanced.png` — `298d5d298dc2b5d46cc2865a81342276c2d66c3a12935f811d8d443451fb7f14`

## Bumblelyzer + Crystallisation Crucible machine textures (task p34-machines-bumblelyzer-crucible)

The two machine families' block texture sets under `gt6/textures/block/` are upstream
borrows with the established conversion: `basicmachines/<family>/colored|overlay/
overlay_running/<side>.png` → `<family>_<set>_<side>.png` byte-identical, and the
`overlay_active` 16xN animation strips cropped to FRAME 0 and re-encoded — the P20
"animation stays retired" deviation (the `.mcmeta` files NOT borrowed). License:
upstream GregTech 6 assets are CC0 (see the Public Domain Dedication block above).

- `block/bumblelyzer_colored_{front,back,left,right,top,bottom}.png` — upstream `bumblelyzer/colored/`
- `block/bumblelyzer_overlay_{front,back,left,right,top,bottom}.png` — upstream `bumblelyzer/overlay/`
- `block/bumblelyzer_overlay_running_{front,back,left,right,top,bottom}.png` — upstream `bumblelyzer/overlay_running/`
- `block/bumblelyzer_overlay_active_{front,back,left,right,top,bottom}.png` — upstream `bumblelyzer/overlay_active/` (front: 16x64 strip → FRAME 0)
- `block/crystallisationcrucible_colored_{front,back,left,right,top,bottom}.png` — upstream `crystallisationcrucible/colored/`
- `block/crystallisationcrucible_overlay_{front,back,left,right,top,bottom}.png` — upstream `crystallisationcrucible/overlay/`
- `block/crystallisationcrucible_overlay_running_{front,back,left,right,top,bottom}.png` — upstream `crystallisationcrucible/overlay_running/`
- `block/crystallisationcrucible_overlay_active_{front,back,left,right,top,bottom}.png` — upstream `crystallisationcrucible/overlay_active/` (front: 16x160 strip → FRAME 0)

Copied on 2026-09-22. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

Display/scale cover family textures, task p35-covers-display-scale-6: the PNGs
under `gt6/textures/block/{auto_switch,auto_timer_switch,energy_display,
energy_redstone,progress_redstone,status_display}/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/machines/covers/`.

Byte-identical borrows, sha256 verified:

- `auto_switch/circuit.png`            `cfcac4c474506318d20d4b5b17eb4d89079301f534911018ff4cc46f115926c3` (upstream `autoswitch/`)
- `auto_timer_switch/circuit.png`      `85291f1199c52ce4b5e0ed4b6f8bdb94b122e60960d1cd2d1e5069199d2de559` (upstream `autotimerswitch/6000/`)
- `energy_redstone/circuit.png`        `8e281fde79aafbecf29b2a70b4f37046fbdcd2aeb1a6567ca2ace05d3f6b0086` (upstream `energyredstone/`)
- `progress_redstone/circuit.png`      `c920850ee52e694dbe82cdfc64e6404af00f15f1e82b9dd66d51fcd3eb460259` (upstream `progressredstone/`)
- `status_display/bottom/base.png`     `2a1e4c8c5b49f4535283e0f8b4db25a3eabafee9c4f1c4d7e5f48f3e03d05ba9` (upstream `statusdisplay/bottom/`)
- `status_display/top/base.png`        `7b623fb350d327009db1edd187eb122f6066579076356253816ac41057841fda` (upstream `statusdisplay/top/`)

Path mapping (declared, the P10/P11 precedent): upstream cover paths are
lowercased/underscored to the 1.20.1 `ResourceLocation` charset, landing under
`textures/block/` — the vanilla block atlas `directory("block")` source
auto-stitches the sprite ids with zero extra atlas wiring.

Pre-composited plates: `energy_display/0..10.png` are `energydisplay/underlay.png`
alpha-composited with `energydisplay/<level>.png` at borrow time (the
CoverSelectorTag underlay+digit precedent — the CoverDisplayEnergy upstream
BlockTextureMulti two-layer pick :48 folds into one sprite per level; the plate
renderer paints a single sprite).

Declared folds: the `autotimerswitch` art ships upstream ONLY as the 6000 face —
the whole reboot-switch ladder shares it (`CoverControllerAutoTimer` class doc);
the `statusdisplay` indicator overlay sprites (`1_off..4_on`, both styles) are NOT
borrowed — the CoverControllerDisplay lamp composition is the declared deviation
(the plate shows the style base, the live states ride the visual lane).

Crafting + asphalt cover pair textures, task p37-covers-crafting-asphalt: the two
PNGs under `gt6/textures/block/` come from upstream
`src/main/resources/assets/gregtech/textures/blocks/`. Byte-identical borrows,
sha256 verified:

- `crafting/0.png`      `d403c86476790756560098c83ab374be268510434f88669f66b31e7cde22a59a` (upstream `machines/covers/crafting/0`)
- `asphalt.png`         `2b51ec88d0be3a57588a04d886e5902dcbc8b9de0afa70e2ea181bdcbb6925a0` (upstream `blocks/iconsets/ASPHALT`)

Path mapping (declared, the P10/P11 precedent): upstream cover paths lowercase to
`textures/block/`, the vanilla block atlas `directory("block")` source auto-stitches.
Declared folds: the crafting cover's upstream 6-variant texture row (`0..5`) keeps
variant 0 (the single sprite the plate renderer paints, the p35 declared-fold
posture); the asphalt plate rides the plain `ASPHALT` icon — the 16 dye-variant
panel row (`Textures.BlockIcons.ASPHALT` + `DYES[i]`) collapses onto the one
sprite (the worldgen streets face family, DYE_INDEX_Gray).

## Render pool stand-in retirement (task p36-render-texture-bake)

Eighteen block textures under `gt6/textures/block/` land the three stand-ins
self-declared in GT6BlockStates.java (the p35 crystal-charger battery-box cube, the
p35 LD-wire wire_electric cube, the p35 LD-transformer electric-transformer model
share) plus the ZPM Decharger art ahead of its consumer card
(p36-energy-zpm-dechargers binds `block/zpm_decharger{,_quantum}_{front,back,side}`
on its rebase). The composites are the battery-box/transformer bake treatment:
src-over(colored, overlay) via `mdk/tools/bake_render_pool_textures.py` (idempotent,
deterministic bytes). The overlay_active/overlay_blinking/overlay_unloaded trios and
the ZPM_TOP active-top decal stay UNBORROWED — the port energy blocks carry no
ACTIVE property (the standing static-face posture; they land with whatever card adds
the property). Sources: machines/energystorages/crystal_laser{,_large} (the
MultiTileEntityCrystalCharger.java:39-51 iconsets), blocks/iconsets/LONG_DIST_WIRE_*
(the five distinct sprites of the LONG_DIST_WIRES_01 meta table, Textures.java:638-655
over Loader_Blocks.java:160), machines/transformers/longdistancetransformer_electric
(MultiTileEntityLongDistanceTransformer.java:302-322 — front = INPUT face, back =
OUTPUT face), machines/energystorages/zpm_electricity|zpm_quantum
(MultiTileEntityZPMDechargerEU.java:47-63 / QU.java:47-54). License: upstream GregTech
6 assets are CC0 (see the Public Domain Dedication block above).

Baked composites (derived; the colored casing layer is one byte-identical grayscale
across every family here, only the overlays differ):

- `block/crystal_charger_front.png` — `7d9e6d2d06eb51240f66667051bc3082adc3ddb8eb814e072d12d7e60708effa` (src-over `crystal_laser/colored/front + overlay/front`; byte-identical to the large-front composite — the two families share the front art)
- `block/crystal_charger_side.png` — `700ab3cc98ef1146b9638af23d0d84c81ccc31507b1184163b6219ee55fcb786` (src-over `crystal_laser/colored/side + overlay/side`)
- `block/crystal_charger_large_front.png` — `7d9e6d2d06eb51240f66667051bc3082adc3ddb8eb814e072d12d7e60708effa` (src-over `crystal_laser_large/colored/front + overlay/front`)
- `block/crystal_charger_large_side.png` — `91a0687c6be7ba2f683fb679122c7dba6eca01334c898129724516e737248e67` (src-over `crystal_laser_large/colored/side + overlay/side`)
- `block/long_distance_transformer_front.png` — `97f5c41837e8c9f94bbaec4873d3d7f0529f1146974569d90d7fffecadc06503` (src-over `longdistancetransformer_electric/colored/front + overlay/front`)
- `block/long_distance_transformer_back.png` — `88177900197bd182593d1d9d21ea3e7888f7ec3f52d816a03a97923cd57c516f` (src-over `.../colored/back + overlay/back`)
- `block/long_distance_transformer_side.png` — `b5f21ab6ca30f0d98327859d946c599182ef88fca8707933ff0cdff850658433` (src-over `.../colored/side + overlay/side`)
- `block/zpm_decharger_front.png` — `a982cea2f5d79df29801e1b21349e617488c324ea79a64b35712658bb60a4c9f` (src-over `zpm_electricity/colored/front + overlay/front`)
- `block/zpm_decharger_back.png` — `f9b323b0b87a687865b32b0908316fe11958210c822d4ded4ece0da99acbf2bc` (src-over `zpm_electricity/colored/back + overlay/back`; byte-identical to the quantum-back composite — the two families share the back overlay)
- `block/zpm_decharger_side.png` — `27dac6bf9e5d2cfeafb11dd83d89bf8317ed3b05ae3dc35caa7fdf3f3b718fec` (src-over `zpm_electricity/colored/side + overlay/side`; byte-identical to the quantum-side composite)
- `block/zpm_decharger_quantum_front.png` — `3d4a5b55a3300f6949bfa9a564615304fc3795c1eb99a52874becaf494150c4c` (src-over `zpm_quantum/colored/front + overlay/front`)
- `block/zpm_decharger_quantum_back.png` — `f9b323b0b87a687865b32b0908316fe11958210c822d4ded4ece0da99acbf2bc` (src-over `zpm_quantum/colored/back + overlay/back`)
- `block/zpm_decharger_quantum_side.png` — `27dac6bf9e5d2cfeafb11dd83d89bf8317ed3b05ae3dc35caa7fdf3f3b718fec` (src-over `zpm_quantum/colored/side + overlay/side`)

Byte copies:

- `block/long_dist_wire_ev.png` — `2127f4db5a5862f969fba65e8ee1f0e4401e5d9e6f6a6b9b1cefb060a3d1dd18` (upstream `blocks/iconsets/LONG_DIST_WIRE_EV.png`)
- `block/long_dist_wire_iv.png` — `3ec70820b019c7e0baa1425e79a7a37280cf94c22b1d96a46b4640d7a5675f6e` (upstream `blocks/iconsets/LONG_DIST_WIRE_IV.png`)
- `block/long_dist_wire_luv.png` — `f0cda63127eb12c65c9f0c9dec08374f9ed4a4395bd67dee49fd3fc6edd92037` (upstream `blocks/iconsets/LONG_DIST_WIRE_LuV.png`)
- `block/long_dist_wire_zpm.png` — `8ff978fa6328e9b293cb46a9a09e1b3055e97c3dc86040953b36301939826a49` (upstream `blocks/iconsets/LONG_DIST_WIRE_ZPM.png`)
- `block/long_dist_wire_uv.png` — `9105d68018de154d8b59950e8fea1c73b28d3d1fc6b1de80422d05618d5eee80` (upstream `blocks/iconsets/LONG_DIST_WIRE_UV.png`)

## Kitchen tool block textures (task p38-issue7-kitchen-models)

The kitchen family's hollow-tub element models (`GT6BlockStates.addKitchen`) borrow the
upstream grayscale `colored/` tile sets — the mRGBa tint-me variants of
`textures/blocks/machines/tools/<family>/colored/*`, byte-identical copies, filenames
unchanged (the `overlay/` detail passes and the pot/bowl Table faces `tablebottom`/
`tableside` stay unborrowed — the overlay layer and the Table rows are the pool cuts).
Upstream tints these tiles at runtime with the row material's colour (mRGBa:
WoodTreated / StainlessSteel / Ceramic); the port shows the grayscale tiles un-tinted
(tintindex 0 reserved on every model face, no BlockColor registered) — declared
deviation, the family runtime-tint pool. Three of the four families ship byte-identical
tiles upstream (only `bathing_pot_wood` differs), which is why the digests repeat:

- `block/tools/bathing_pot_wood/sides.png` — `0fb1440b5a5ebce82ea7cb15dd9d6a6b7a1c184bf92b1aa0aa0c9fed74201607` (upstream `blocks/machines/tools/bathing_pot_wood/colored/sides.png`)
- `block/tools/bathing_pot_wood/insides.png` — `904c928817e06e344e8f517f463af9a3f93654ce889d657d926457f10bc2980b` (upstream `blocks/machines/tools/bathing_pot_wood/colored/insides.png`)
- `block/tools/bathing_pot_wood/top.png` — `be4dbb03c7036fdf7ebd0229ca7ed28ee428daaa410995730047a3ee1b9d8dba` (upstream `blocks/machines/tools/bathing_pot_wood/colored/top.png`)
- `block/tools/bathing_pot_wood/bottom.png` — `8289841b503320b79a818ead17021e2e3e3bf8c9946af30eb13104d919b4c929` (upstream `blocks/machines/tools/bathing_pot_wood/colored/bottom.png`)
- `block/tools/bathing_pot/sides.png` — `1944346bd9064a4960f5e28a1393a23aeeb88c219ddd14526e72676e8c1ea334` (upstream `blocks/machines/tools/bathing_pot/colored/sides.png`; byte-identical to the mixing_bowl + juicer side tiles)
- `block/tools/bathing_pot/insides.png` — `e6ce4f96c5fbf7f4fe3088e72899827608d9b46f5d0948873e5089c864197251` (upstream `blocks/machines/tools/bathing_pot/colored/insides.png`; byte-identical to the mixing_bowl + juicer inside tiles)
- `block/tools/bathing_pot/top.png` — `59880aac68573de2af64d3b5c661ec11b00226d1ea783d0619c6a8ca97c95b60` (upstream `blocks/machines/tools/bathing_pot/colored/top.png`; byte-identical to the mixing_bowl + juicer top tiles)
- `block/tools/bathing_pot/bottom.png` — `c256f5702a40de69120972a020024ef47be7d6041b3bdc163d0df4c289e971a1` (upstream `blocks/machines/tools/bathing_pot/colored/bottom.png`; byte-identical to the mixing_bowl bottom and the juicer bottom/middleside/middletop tiles)
- `block/tools/mixing_bowl/sides.png` — `1944346bd9064a4960f5e28a1393a23aeeb88c219ddd14526e72676e8c1ea334` (upstream `blocks/machines/tools/mixing_bowl/colored/sides.png`; byte-identical to the bathing_pot side tile)
- `block/tools/mixing_bowl/insides.png` — `e6ce4f96c5fbf7f4fe3088e72899827608d9b46f5d0948873e5089c864197251` (upstream `blocks/machines/tools/mixing_bowl/colored/insides.png`; byte-identical to the bathing_pot inside tile)
- `block/tools/mixing_bowl/top.png` — `59880aac68573de2af64d3b5c661ec11b00226d1ea783d0619c6a8ca97c95b60` (upstream `blocks/machines/tools/mixing_bowl/colored/top.png`; byte-identical to the bathing_pot top tile)
- `block/tools/mixing_bowl/bottom.png` — `c256f5702a40de69120972a020024ef47be7d6041b3bdc163d0df4c289e971a1` (upstream `blocks/machines/tools/mixing_bowl/colored/bottom.png`; byte-identical to the bathing_pot bottom tile)
- `block/tools/juicer/sides.png` — `1944346bd9064a4960f5e28a1393a23aeeb88c219ddd14526e72676e8c1ea334` (upstream `blocks/machines/tools/juicer/colored/sides.png`; byte-identical to the bathing_pot side tile)
- `block/tools/juicer/insides.png` — `e6ce4f96c5fbf7f4fe3088e72899827608d9b46f5d0948873e5089c864197251` (upstream `blocks/machines/tools/juicer/colored/insides.png`; byte-identical to the bathing_pot inside tile)
- `block/tools/juicer/top.png` — `59880aac68573de2af64d3b5c661ec11b00226d1ea783d0619c6a8ca97c95b60` (upstream `blocks/machines/tools/juicer/colored/top.png`; byte-identical to the bathing_pot top tile)
- `block/tools/juicer/bottom.png` — `c256f5702a40de69120972a020024ef47be7d6041b3bdc163d0df4c289e971a1` (upstream `blocks/machines/tools/juicer/colored/bottom.png`; byte-identical to the bathing_pot bottom tile)
- `block/tools/juicer/middleside.png` — `c256f5702a40de69120972a020024ef47be7d6041b3bdc163d0df4c289e971a1` (upstream `blocks/machines/tools/juicer/colored/middleside.png`; the same uniform tile as the bottom — upstream ships it un-detailed, the mRGBa tint carries the face)
- `block/tools/juicer/middletop.png` — `c256f5702a40de69120972a020024ef47be7d6041b3bdc163d0df4c289e971a1` (upstream `blocks/machines/tools/juicer/colored/middletop.png`; the same uniform tile as the bottom)

## owned (generated, not borrowed)

- `gt6/textures/block/mini_portal_end.png` — the Miniature End Portal's active
  face (task p35-portals-mini-nether-end). NOT a borrow: vanilla ships no
  end-portal block texture (the vanilla end-portal effect is a tile renderer,
  not a texture), so the card generates a 16x16 near-black starfield (10
  seeded purple/blue star pixels over an (8,4,16) base — the upstream
  DYE_Black-tinted portal-texture intent, MiniPortalEnd.java:128). The frame
  face and the whole Nether portal ride vanilla textures referenced in place
  (obsidian / end_stone / nether_portal — zero borrowed files).
