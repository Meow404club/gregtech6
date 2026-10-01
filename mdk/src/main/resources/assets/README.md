## task cbc-6-crop-test-faces (2026-10-01) — the two magic-flower placeholder icons

two COMPOSED placeholders: `gt6/textures/item/food/cerublossom.png` + `gt6/textures/item/food/desertnova.png` (the P20
stdlib generator convention, 16x16 RGBA): the two magic-flower fallback items (MultiItemFood
meta 12010/12011, the crop-row :643-644 seat) carry NO borrowable upstream icon — the
`flowerCerublossom`/`flowerDesertNova` sprites are not in any repo snapshot (the
tmp/gt6-1.7.10 assets prune item art; the port asset library never borrowed them). Hand-rolled
8-petal flower pixels per flower (cerublossom = the 暗影花 violet petals + gold heart;
desertnova = the 沙漠新星 desert-orange nova). NOT byte-identical to upstream — declared
placeholder, swap for a borrow the day a snapshot surfaces. sha256
`657bf0063261eb4b558db556dfc2ff3a8aeff9d0e2946446cb1e5fd83378bea3` /
`6df7307f062bfa12d2e60ad81a3b1d821ce69e2872236d32fb2d248eb909c88e`.

## task chem-fluids-unlock (2026-09-30) — the B2 chemical-blocker fluid batch: zero
## texture files, declared-tint ledger

- NO borrows this card — 34 fluid rows (32 CHEMICAL_SPECS material-walk rows + the
  brine/spruceresin simple-liquid pair), all fluid-only, all rendering through the
  family vanilla-water layers over the row tint (the chemical-family initializeClient
  convention). The upstream `assets/gregtech/textures/blocks/fluids/` directory
  (686 pngs, audited) carries NO face for any of the 34 ids — the only hits are the
  already-ported nitrofuel/nitrogenplasma — and the material-walk fluids upstream
  render through the material BLOCK textures (`mTextureSetsBlock.get(
  INDEX_BLOCK_MOLTEN/INDEX_BLOCK_GAS)`, FL.java:1072/:1077/:1080), not per-fluid pngs,
  so there is nothing to borrow byte-identically.
- Tint ledger (per row, the JetFuel/aqua port-owned-declared precedent where no
  material anchor exists): the 32 chemical rows tint = the material RGBa verbatim
  (MT.java upstream lines cited on each GTFluids row comment; e.g. bromine 80,10,10
  MT.java:424, UF6 family 66,98,85 MT.java:1181-1185, the vitriol family MT.java
  :1169-1177). The 2 honest-default rows (brine/spruceresin — external-mod fluid
  names with no GT6 FL.create and no material) declare port-owned tints: brine
  0xFFC8D8D0 pale salt grey-green, spruceresin 0xFFD8A848 amber resin (the
  sap/maplesap amber family).

Advanced Crafting Table GUI backgrounds, task act-dual-gui (2 PNGs, CC0 from
the upstream craftingtables machine set, sha256 verified):

- `gt6/textures/gui/machines/advancedcraftingtable.png` — the crafting panel
  background, upstream
  `src/main/resources/assets/gregtech/textures/gui/machines/AdvancedCraftingTable.png`
  (256x256 panel canvas), byte-identical, sha256
  `af774ea0631d00b61242ea8d18deaba148c9fc581fd90201c615091381729a5a`.
- `gt6/textures/gui/machines/advancedcraftingtablecharging.png` — the
  charging-row panel background, upstream
  `src/main/resources/assets/gregtech/textures/gui/machines/AdvancedCraftingTableCharging.png`
  (256x256 panel canvas), byte-identical, sha256
  `cd57524d0fcb72efd3519e4a0e9a62fe8e67b7422aff61194985da9a500f198a`.

Path mapping (the dryer.png entry's lowercase convention): upstream
`machines/AdvancedCraftingTable{,Charging}` →
`machines/advancedcraftingtable{,charging}`. Upstream picks the sheet per
VARIANT, not per GUI id — the plain row's `mGUITexture` defaults to the plain
sheet (MultiTileEntityAdvancedCraftingTable.java:74) and the charging row's
registration column carries the charging sheet via `NBT_GUI`
(Loader_MultiTileEntities.java:137), so the port panels take the sheet from the
BE kind the same way (both panels of one table share it, upstream :585/:750).
The upstream canvas paints its slot frames at the same coordinates the port
panel widgets use — the crafting GUI's :691-731 seats and the charging GUI's
ContainerCommon case-36 9x4 belt (8,8)..(152,62) — so the borrowed canvas and
the port slot geometry align by construction (the dryer.png rationale).

Copied on 2026-09-30. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

## GT6 bottle items (task food-bottles-min, 2026-10-01)
- `gt6/textures/item/bottle/*.png` — the bottles-domain minimum subset (3 textures,
  task food-bottles-min), byte-identical borrows renamed to the registered item ids:
  - `ketchup.png`          `textures/items/gt.multiitem.bottles/3101.png`
    (`40a479ed0ec585c5d46e4565482cb4dc579a995788a6e79b4cd5f8e98f34f58c` — the Tomato
    Ketchup bottle, MultiItemBottles.java:257, oredict foodKetchup)
  - `barbecuesauce.png`    `textures/items/gt.multiitem.bottles/805.png`
    (`e5c36805512fd0dfe0d8a691868f60af15eae4d694c90d57264494a4b226b6f9` — the Barbecue
    Sauce bottle, MultiItemBottles.java:111, oredict foodBarbecuesauce)
  - `heavycream.png`       `textures/items/gt.multiitem.bottles/1101.png`
    (`6838344186dea7e2b216755a555197fd1bf2ab1c56f2152d1aeab8118fe4ad7d` — the Heavy
    Cream bottle, MultiItemBottles.java:139, oredict bottleCream re-registered to
    foodHeavycream, LoaderOreDictReRegistrations.java:870)
  All 16x16 RGBA, CC0 1.0 per the upstream README block (the bottle faces on the
  MultiItemBottles meta item; cmp-verified 2026-10-01, 3/3 byte-identical). The fourth
  registered bottle (bottle_empty) borrows NOTHING: upstream it is the OP.bottle
  material-prefix technical container (OP.java:229, IS_CONTAINER/SELF_REFERENCING) whose
  MT.Empty face has no own sprite anywhere in the upstream resources, so the port binds
  the vanilla glass_bottle model (the declared deviation).
## task cbc-6-crop-test-faces (2026-10-01) — the two magic-flower placeholder icons

two COMPOSED placeholders: `gt6/textures/item/food/cerublossom.png` + `gt6/textures/item/food/desertnova.png` (the P20
stdlib generator convention, 16x16 RGBA): the two magic-flower fallback items (MultiItemFood
meta 12010/12011, the crop-row :643-644 seat) carry NO borrowable upstream icon — the
`flowerCerublossom`/`flowerDesertNova` sprites are not in any repo snapshot (the
tmp/gt6-1.7.10 assets prune item art; the port asset library never borrowed them). Hand-rolled
8-petal flower pixels per flower (cerublossom = the 暗影花 violet petals + gold heart;
desertnova = the 沙漠新星 desert-orange nova). NOT byte-identical to upstream — declared
placeholder, swap for a borrow the day a snapshot surfaces. sha256
`657bf0063261eb4b558db556dfc2ff3a8aeff9d0e2946446cb1e5fd83378bea3` /
`6df7307f062bfa12d2e60ad81a3b1d821ce69e2872236d32fb2d248eb909c88e`.

## task chem-fluids-unlock (2026-09-30) — the B2 chemical-blocker fluid batch: zero
## texture files, declared-tint ledger

- NO borrows this card — 34 fluid rows (32 CHEMICAL_SPECS material-walk rows + the
  brine/spruceresin simple-liquid pair), all fluid-only, all rendering through the
  family vanilla-water layers over the row tint (the chemical-family initializeClient
  convention). The upstream `assets/gregtech/textures/blocks/fluids/` directory
  (686 pngs, audited) carries NO face for any of the 34 ids — the only hits are the
  already-ported nitrofuel/nitrogenplasma — and the material-walk fluids upstream
  render through the material BLOCK textures (`mTextureSetsBlock.get(
  INDEX_BLOCK_MOLTEN/INDEX_BLOCK_GAS)`, FL.java:1072/:1077/:1080), not per-fluid pngs,
  so there is nothing to borrow byte-identically.
- Tint ledger (per row, the JetFuel/aqua port-owned-declared precedent where no
  material anchor exists): the 32 chemical rows tint = the material RGBa verbatim
  (MT.java upstream lines cited on each GTFluids row comment; e.g. bromine 80,10,10
  MT.java:424, UF6 family 66,98,85 MT.java:1181-1185, the vitriol family MT.java
  :1169-1177). The 2 honest-default rows (brine/spruceresin — external-mod fluid
  names with no GT6 FL.create and no material) declare port-owned tints: brine
  0xFFC8D8D0 pale salt grey-green, spruceresin 0xFFD8A848 amber resin (the
  sap/maplesap amber family).

Advanced Crafting Table GUI backgrounds, task act-dual-gui (2 PNGs, CC0 from
the upstream craftingtables machine set, sha256 verified):

- `gt6/textures/gui/machines/advancedcraftingtable.png` — the crafting panel
  background, upstream
  `src/main/resources/assets/gregtech/textures/gui/machines/AdvancedCraftingTable.png`
  (256x256 panel canvas), byte-identical, sha256
  `af774ea0631d00b61242ea8d18deaba148c9fc581fd90201c615091381729a5a`.
- `gt6/textures/gui/machines/advancedcraftingtablecharging.png` — the
  charging-row panel background, upstream
  `src/main/resources/assets/gregtech/textures/gui/machines/AdvancedCraftingTableCharging.png`
  (256x256 panel canvas), byte-identical, sha256
  `cd57524d0fcb72efd3519e4a0e9a62fe8e67b7422aff61194985da9a500f198a`.

Path mapping (the dryer.png entry's lowercase convention): upstream
`machines/AdvancedCraftingTable{,Charging}` →
`machines/advancedcraftingtable{,charging}`. Upstream picks the sheet per
VARIANT, not per GUI id — the plain row's `mGUITexture` defaults to the plain
sheet (MultiTileEntityAdvancedCraftingTable.java:74) and the charging row's
registration column carries the charging sheet via `NBT_GUI`
(Loader_MultiTileEntities.java:137), so the port panels take the sheet from the
BE kind the same way (both panels of one table share it, upstream :585/:750).
The upstream canvas paints its slot frames at the same coordinates the port
panel widgets use — the crafting GUI's :691-731 seats and the charging GUI's
ContainerCommon case-36 9x4 belt (8,8)..(152,62) — so the borrowed canvas and
the port slot geometry align by construction (the dryer.png rationale).

Copied on 2026-09-30. Upstream license: **CC0 1.0 Universal Public Domain
Dedication** (same upstream `README.md` block as above).

## GT6 bottle items (task food-bottles-min, 2026-10-01)
- `gt6/textures/item/bottle/*.png` — the bottles-domain minimum subset (3 textures,
  task food-bottles-min), byte-identical borrows renamed to the registered item ids:
  - `ketchup.png`          `textures/items/gt.multiitem.bottles/3101.png`
    (`40a479ed0ec585c5d46e4565482cb4dc579a995788a6e79b4cd5f8e98f34f58c` — the Tomato
    Ketchup bottle, MultiItemBottles.java:257, oredict foodKetchup)
  - `barbecuesauce.png`    `textures/items/gt.multiitem.bottles/805.png`
    (`e5c36805512fd0dfe0d8a691868f60af15eae4d694c90d57264494a4b226b6f9` — the Barbecue
    Sauce bottle, MultiItemBottles.java:111, oredict foodBarbecuesauce)
  - `heavycream.png`       `textures/items/gt.multiitem.bottles/1101.png`
    (`6838344186dea7e2b216755a555197fd1bf2ab1c56f2152d1aeab8118fe4ad7d` — the Heavy
    Cream bottle, MultiItemBottles.java:139, oredict bottleCream re-registered to
    foodHeavycream, LoaderOreDictReRegistrations.java:870)
  All 16x16 RGBA, CC0 1.0 per the upstream README block (the bottle faces on the
  MultiItemBottles meta item; cmp-verified 2026-10-01, 3/3 byte-identical). The fourth
  registered bottle (bottle_empty) borrows NOTHING: upstream it is the OP.bottle
  material-prefix technical container (OP.java:229, IS_CONTAINER/SELF_REFERENCING) whose
  MT.Empty face has no own sprite anywhere in the upstream resources, so the port binds
  the vanilla glass_bottle model (the declared deviation).
