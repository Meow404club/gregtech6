# DIVERGE — vendored ModularUI 逐文件偏离台账

基线：上游 `1.21.1@c13e141`（主树）/ `1.20.1@909cda2`（回向腿参照），2026-09-06 快照。
本台账对账口径：vendored java 总数 657 = src/main 371 + src/forgeMain 145 + src/neoforgeMain 141。

## §0 census 总账

| 类别 | 文件数 | 说明 |
|---|---|---|
| 双腿字节级零改（src/main） | 232 | 与两腿快照均一致，零触碰 |
| 仅注释形状漂移（src/main，b 形保留） | 7 | §4；compile-neutral |
| `//?` hunk 分叉（src/main） | 132 | §2；132 中 131 由 tools/gen-forks.py 机械生成，1 处手工 hunk（§3）|
| leg-split（整体按腿，双树原文） | 112 | §5；漂移率 ≥0.15 或 hunk ≥10 的结构性漂移 |
| 仅 1.20.1 存在（src/forgeMain） | 33 | 原文 |
| 仅 1.21.1 存在（src/neoforgeMain） | 29 | 原文 |

重放程序：`git checkout <基线> -- third-party/modularui/src` → `tools/gen-forks.py --apply`（自动面）→ 回放 §3 手工 hunk 与 §1 构建级偏离。

## §1 构建级偏离（非上游文件，全部为本仓原创或声明改动）

1. **构建脚本替换**：上游 gradle 多脚本（moddevgradle/jars/resources/publishing/docs/formatting）→ 本仓 stonecutter 双节点 buildscript；runs/jar/mods.toml 生成不在本卡（卡② packaging 域）。
2. **swap 表**（stonecutter.gradle.kts，仅 forge 节点正向）：五缝包名带 distmarker/bus/items(+wrapper)/fluids + chunk.status 包移位 + isSameItemSameComponents 改名；regex + reverse 永不匹配哨兵。
3. **依赖声明差异**：JEI impl 进编译类路径（上游 bundles.jei 三件，modularui 引 JEI 内部包）；`me.shedaniel.cloth:basic-math:0.6.1` 补 REI 的 me.shedaniel.math 包（上游 catalog clothmath="+"，钉 latest）；MouseTweaks 上游声明 compileOnly 但源码零引用（grep 实证）→ 不声明。
4. **资源树拆分**：modularui.mixins.json 双腿各自成 json（src/{forge,neoforge}Main/resources）；atlas gui.json per-leg；injected_interfaces 按腿拆分（forge 侧仅 Component 注入）。
5. **mixin AP**：forge 腿显式 mixin{} 块 + mixin:processor + mixinextras-common（上游同款）；neoforge 腿由 moddev 自带。
6. **上游 dev-test 源集**（src/test）不 vendored（无测试面，任务卡验收=compileJava）。
7. **parchment 不引入**（上游编译用官方 mojmap 面，与本仓双腿一致）。

## §2 `//?` hunk 分叉文件（132）

### RV 集成 API 漂移（8）

- `brachy/modularui/integration/emi/handler/EmiScreenHandler.java`
- `brachy/modularui/integration/jei/JeiIngredientHandler.java`
- `brachy/modularui/integration/jei/handler/JeiContainerHandler.java`
- `brachy/modularui/integration/recipeviewer/entry/fluid/FluidEntryList.java`
- `brachy/modularui/integration/recipeviewer/entry/fluid/FluidStackList.java`
- `brachy/modularui/integration/recipeviewer/handlers/IngredientProvider.java`
- `brachy/modularui/integration/recipeviewer/handlers/RecipeTransferError.java`
- `brachy/modularui/integration/rei/handler/REIScreenHandler.java`

### mixin 目标漂移（3）

- `brachy/modularui/core/mixins/client/AbstractContainerScreenMixin.java`
- `brachy/modularui/core/mixins/client/MinecraftMixin.java`
- `brachy/modularui/core/mixins/emi/RecipeScreenMixin.java`

### vanilla API/逻辑漂移（68）

- `brachy/modularui/GuiErrorHandler.java`
- `brachy/modularui/animation/SequentialAnimator.java`
- `brachy/modularui/api/ITreeNode.java`
- `brachy/modularui/api/IUIHolder.java`
- `brachy/modularui/api/UIFactory.java`
- `brachy/modularui/api/value/IValue.java`
- `brachy/modularui/api/widget/IWidget.java`
- `brachy/modularui/api/widget/Interactable.java`
- `brachy/modularui/factory/BlockEntityUIFactory.java`
- `brachy/modularui/factory/PosGuiData.java`
- `brachy/modularui/factory/SimpleUIFactory.java`
- `brachy/modularui/factory/UIFactories.java`
- `brachy/modularui/factory/inventory/CuriosHandler.java`
- `brachy/modularui/factory/inventory/InventoryType.java`
- `brachy/modularui/factory/inventory/InventoryTypes.java`
- `brachy/modularui/factory/inventory/ItemHandler.java`
- `brachy/modularui/overlay/DebugOverlay.java`
- `brachy/modularui/overlay/OverlayStack.java`
- `brachy/modularui/screen/BuildPanelEvent.java`
- `brachy/modularui/screen/ContainerScreenWrapper.java`
- `brachy/modularui/screen/CustomModularScreen.java`
- `brachy/modularui/screen/RecipeViewerSettingsImpl.java`
- `brachy/modularui/screen/SecondaryPanel.java`
- `brachy/modularui/screen/UISettings.java`
- `brachy/modularui/screen/viewport/GuiContext.java`
- `brachy/modularui/screen/viewport/GuiViewportStack.java`
- `brachy/modularui/screen/viewport/ModularGuiContext.java`
- `brachy/modularui/theme/ThemeManager.java`
- `brachy/modularui/theme/WidgetThemeKey.java`
- `brachy/modularui/utils/Alignment.java`
- `brachy/modularui/utils/Color.java`
- `brachy/modularui/utils/FluidTextureType.java`
- `brachy/modularui/utils/KeyboardData.java`
- `brachy/modularui/utils/MatrixUtils.java`
- `brachy/modularui/utils/MutableSingletonList.java`
- `brachy/modularui/utils/Rectangle.java`
- `brachy/modularui/utils/SpriteHelper.java`
- `brachy/modularui/utils/Stencil.java`
- `brachy/modularui/utils/TreeUtil.java`
- `brachy/modularui/utils/math/FAM.java`
- `brachy/modularui/utils/math/MathUtils.java`
- `brachy/modularui/widget/AbstractParentWidget.java`
- `brachy/modularui/widget/AbstractScrollWidget.java`
- `brachy/modularui/widget/scroll/HorizontalScrollData.java`
- `brachy/modularui/widget/scroll/ScrollArea.java`
- `brachy/modularui/widget/scroll/ScrollData.java`
- `brachy/modularui/widget/scroll/VerticalScrollData.java`
- `brachy/modularui/widgets/AbstractFluidDisplayWidget.java`
- `brachy/modularui/widgets/ButtonWidget.java`
- `brachy/modularui/widgets/ColorPickerDialog.java`
- `brachy/modularui/widgets/DynamicSyncedWidget.java`
- `brachy/modularui/widgets/FluidDisplayWidget.java`
- `brachy/modularui/widgets/RichTextWidget.java`
- `brachy/modularui/widgets/SchemaWidget.java`
- `brachy/modularui/widgets/SliderWidget.java`
- `brachy/modularui/widgets/SlotGroupWidget.java`
- `brachy/modularui/widgets/SortableListWidget.java`
- `brachy/modularui/widgets/dynamic/DynamicWidget.java`
- `brachy/modularui/widgets/layout/Flow.java`
- `brachy/modularui/widgets/menu/AbstractMenuButton.java`
- `brachy/modularui/widgets/menu/DropdownWidget.java`
- `brachy/modularui/widgets/slot/CraftingContainerWrapper.java`
- `brachy/modularui/widgets/slot/ModularSlot.java`
- `brachy/modularui/widgets/slot/PhantomItemSlot.java`
- `brachy/modularui/widgets/slot/SlotGroup.java`
- `brachy/modularui/widgets/textfield/TextFieldHandler.java`
- `brachy/modularui/widgets/textfield/TextFieldRenderer.java`
- `brachy/modularui/widgets/textfield/TextFieldWidget.java`

### 上游 dev-test 面漂移（2）

- `brachy/modularui/test/TestGuis.java`
- `brachy/modularui/test/TestHandler.java`

### 同步面漂移（23）

- `brachy/modularui/api/value/sync/IBoolSyncValue.java`
- `brachy/modularui/api/value/sync/IValueSyncHandler.java`
- `brachy/modularui/value/sync/AbstractGenericSyncValue.java`
- `brachy/modularui/value/sync/BigDecimalSyncValue.java`
- `brachy/modularui/value/sync/BigIntegerSyncValue.java`
- `brachy/modularui/value/sync/BinaryEnumSyncValue.java`
- `brachy/modularui/value/sync/BooleanSyncValue.java`
- `brachy/modularui/value/sync/ByteArraySyncValue.java`
- `brachy/modularui/value/sync/ByteSyncValue.java`
- `brachy/modularui/value/sync/DoubleSyncValue.java`
- `brachy/modularui/value/sync/DynamicLinkedSyncHandler.java`
- `brachy/modularui/value/sync/EnumSyncValue.java`
- `brachy/modularui/value/sync/FloatSyncValue.java`
- `brachy/modularui/value/sync/ISyncRegistrar.java`
- `brachy/modularui/value/sync/IntSyncValue.java`
- `brachy/modularui/value/sync/ItemSlotSyncHandler.java`
- `brachy/modularui/value/sync/LongArraySyncValue.java`
- `brachy/modularui/value/sync/LongSyncValue.java`
- `brachy/modularui/value/sync/ModularSyncManager.java`
- `brachy/modularui/value/sync/PanelSyncHandler.java`
- `brachy/modularui/value/sync/ShortSyncValue.java`
- `brachy/modularui/value/sync/StringSyncValue.java`
- `brachy/modularui/value/sync/SyncedAction.java`

### 渲染/GuiGraphics 漂移（25）

- `brachy/modularui/api/drawable/IDrawable.java`
- `brachy/modularui/api/drawable/IRichTextBuilder.java` **[含手工 hunk]**
- `brachy/modularui/api/drawable/Text.java`
- `brachy/modularui/drawable/Circle.java`
- `brachy/modularui/drawable/ColorType.java`
- `brachy/modularui/drawable/DynamicDrawable.java`
- `brachy/modularui/drawable/FlowDrawable.java`
- `brachy/modularui/drawable/Icon.java`
- `brachy/modularui/drawable/InteractableIcon.java`
- `brachy/modularui/drawable/Rectangle.java`
- `brachy/modularui/drawable/SubAreaDrawable.java`
- `brachy/modularui/drawable/TooltipComponentIcon.java`
- `brachy/modularui/drawable/graph/GraphAxis.java`
- `brachy/modularui/drawable/graph/Plot.java`
- `brachy/modularui/drawable/progress/BaseProgressDrawable.java`
- `brachy/modularui/drawable/progress/CompositeProgress.java`
- `brachy/modularui/drawable/schema/ArraySchema.java`
- `brachy/modularui/drawable/schema/BlockHighlight.java`
- `brachy/modularui/drawable/schema/Camera.java`
- `brachy/modularui/drawable/schema/RenderLevel.java`
- `brachy/modularui/drawable/text/FontRenderHelper.java`
- `brachy/modularui/drawable/text/RichText.java`
- `brachy/modularui/drawable/text/RichTextCompiler.java`
- `brachy/modularui/drawable/text/TextIcon.java`
- `brachy/modularui/drawable/text/TextRenderer.java`

### 网络/序列化漂移（RegistryFriendlyByteBuf/StreamCodec）（3）

- `brachy/modularui/utils/serialization/codec/CodecUtil.java`
- `brachy/modularui/utils/serialization/codec/Field.java`
- `brachy/modularui/utils/serialization/codec/MutableObjectCodec.java`

## §3 手工 hunk 清单

- `brachy/modularui/api/drawable/IRichTextBuilder.java` addMultiLine：forge 腿 = 上游注释停用形（整方法注释），neoforge 腿 = LangUtil 活代码；含 `*/` 的内容不能进块注释包裹（stitcher 解析界），改为块包裹内 // 行注释形式。

## §4 仅注释形状漂移（7，b 形保留不包分叉）

- `brachy/modularui/api/IPanelHandler.java`
- `brachy/modularui/api/IThemeApi.java`
- `brachy/modularui/api/drawable/IIcon.java`
- `brachy/modularui/api/value/ISyncOrValue.java`
- `brachy/modularui/api/widget/ISynced.java`
- `brachy/modularui/api/widget/ITooltip.java`
- `brachy/modularui/theme/ThemeAPI.java`

## §5 leg-split（112，forgeMain=1.20.1 原文 / neoforgeMain=1.21.1 原文）

### RV 集成 API 漂移（19）

- `brachy/modularui/integration/embeddium/SodiumCompat.java`
- `brachy/modularui/integration/emi/EmiRecipeViewerSlot.java`
- `brachy/modularui/integration/emi/EmiStackConverter.java`
- `brachy/modularui/integration/emi/ModularUIEmiPlugin.java`
- `brachy/modularui/integration/emi/recipe/ModularUIEmiRecipe.java`
- `brachy/modularui/integration/jei/JeiRecipeViewerSlot.java`
- `brachy/modularui/integration/jei/ModularUIJeiPlugin.java`
- `brachy/modularui/integration/jei/ModularUIJeiProperties.java`
- `brachy/modularui/integration/jei/handler/JeiScreenHandler.java`
- `brachy/modularui/integration/jei/recipe/package-info.java`
- `brachy/modularui/integration/recipeviewer/RecipeViewerSlotWidget.java`
- `brachy/modularui/integration/recipeviewer/entry/fluid/FluidHolderSetList.java`
- `brachy/modularui/integration/recipeviewer/entry/fluid/FluidTagList.java`
- `brachy/modularui/integration/recipeviewer/entry/item/ItemHolderSetList.java`
- `brachy/modularui/integration/recipeviewer/entry/item/ItemStackList.java`
- `brachy/modularui/integration/recipeviewer/entry/item/ItemTagList.java`
- `brachy/modularui/integration/recipeviewer/handlers/RecipeViewerHandler.java`
- `brachy/modularui/integration/rei/REIStackConverter.java`
- `brachy/modularui/integration/rei/ReiRecipeViewerSlot.java`

### mixin 目标漂移（2）

- `brachy/modularui/core/mixins/common/CombinedInvWrapperAccessor.java`
- `brachy/modularui/core/mixins/jei/IngredientListOverlayAccessor.java`

### vanilla API/逻辑漂移（44）

- `brachy/modularui/ModularUI.java`
- `brachy/modularui/ModularUIConfig.java`
- `brachy/modularui/ModularUIMenuTypes.java`
- `brachy/modularui/animation/AnimatorManager.java`
- `brachy/modularui/api/IMuiScreen.java`
- `brachy/modularui/api/IPacketWriter.java`
- `brachy/modularui/api/ISyncedAction.java`
- `brachy/modularui/api/MCHelper.java`
- `brachy/modularui/api/widget/IGuiAction.java`
- `brachy/modularui/core/ModularUIMixinPlugin.java`
- `brachy/modularui/factory/ClientGUI.java`
- `brachy/modularui/factory/EntityUIFactory.java`
- `brachy/modularui/factory/GuiData.java`
- `brachy/modularui/factory/GuiManager.java`
- `brachy/modularui/factory/PlayerInventoryUIFactory.java`
- `brachy/modularui/factory/SidedBlockEntityUIFactory.java`
- `brachy/modularui/factory/SidedPosGuiData.java`
- `brachy/modularui/screen/ClientScreenHandler.java`
- `brachy/modularui/screen/EmbedHandler.java`
- `brachy/modularui/screen/ModularContainerMenu.java`
- `brachy/modularui/screen/ModularPanel.java`
- `brachy/modularui/screen/ModularScreen.java`
- `brachy/modularui/screen/PanelManager.java`
- `brachy/modularui/screen/RichTooltip.java`
- `brachy/modularui/screen/ScreenWrapper.java`
- `brachy/modularui/screen/event/RichTooltipEvent.java`
- `brachy/modularui/theme/ReloadThemeEvent.java`
- `brachy/modularui/utils/CursorHandler.java`
- `brachy/modularui/utils/FormattingUtil.java`
- `brachy/modularui/utils/HoveredWidgetList.java`
- `brachy/modularui/utils/ICopy.java`
- `brachy/modularui/utils/ImageUtil.java`
- `brachy/modularui/utils/ItemStackHashStrategy.java`
- `brachy/modularui/utils/MUIRenderTypes.java`
- `brachy/modularui/utils/MouseData.java`
- `brachy/modularui/utils/NetworkUtils.java`
- `brachy/modularui/utils/TooltipLines.java`
- `brachy/modularui/utils/math/DAM.java`
- `brachy/modularui/widget/Widget.java`
- `brachy/modularui/widgets/SortButtons.java`
- `brachy/modularui/widgets/slot/FluidSlot.java`
- `brachy/modularui/widgets/slot/ItemSlot.java`
- `brachy/modularui/widgets/slot/ModularCraftingSlot.java`
- `brachy/modularui/widgets/slot/PlayerSlotType.java`

### 上游 dev-test 面漂移（5）

- `brachy/modularui/test/TestBlock.java`
- `brachy/modularui/test/TestBlockEntity.java`
- `brachy/modularui/test/TestItem.java`
- `brachy/modularui/test/TestMachine.java`
- `brachy/modularui/test/TestRegistration.java`

### 同步面漂移（20）

- `brachy/modularui/api/value/sync/IByteSyncValue.java`
- `brachy/modularui/api/value/sync/IDoubleSyncValue.java`
- `brachy/modularui/api/value/sync/IFloatSyncValue.java`
- `brachy/modularui/api/value/sync/IIntSyncValue.java`
- `brachy/modularui/api/value/sync/ILongSyncValue.java`
- `brachy/modularui/api/value/sync/IShortSyncValue.java`
- `brachy/modularui/api/value/sync/IStringSyncValue.java`
- `brachy/modularui/value/sync/DynamicSyncHandler.java`
- `brachy/modularui/value/sync/FluidSlotSyncHandler.java`
- `brachy/modularui/value/sync/GenericCollectionSyncHandler.java`
- `brachy/modularui/value/sync/GenericListSyncHandler.java`
- `brachy/modularui/value/sync/GenericMapSyncHandler.java`
- `brachy/modularui/value/sync/GenericSetSyncHandler.java`
- `brachy/modularui/value/sync/GenericSyncValue.java`
- `brachy/modularui/value/sync/InteractionSyncHandler.java`
- `brachy/modularui/value/sync/PanelSyncManager.java`
- `brachy/modularui/value/sync/PhantomItemSlotSyncHandler.java`
- `brachy/modularui/value/sync/SyncHandler.java`
- `brachy/modularui/value/sync/SyncHandlers.java`
- `brachy/modularui/value/sync/ValueSyncHandler.java`

### 渲染/GuiGraphics 漂移（13）

- `brachy/modularui/drawable/DrawableStack.java`
- `brachy/modularui/drawable/FluidDrawable.java`
- `brachy/modularui/drawable/GuiDraw.java`
- `brachy/modularui/drawable/GuiTextures.java`
- `brachy/modularui/drawable/ItemDrawable.java`
- `brachy/modularui/drawable/UITexture.java`
- `brachy/modularui/drawable/progress/CircularProgressDrawable.java`
- `brachy/modularui/drawable/schema/BaseSchemaRenderer.java`
- `brachy/modularui/drawable/schema/DummyChunk.java`
- `brachy/modularui/drawable/schema/DummyChunkSource.java`
- `brachy/modularui/drawable/schema/LiquidVertexConsumer.java`
- `brachy/modularui/drawable/schema/SchemaLevel.java`
- `brachy/modularui/drawable/text/ModularComponent.java`

### 网络/序列化漂移（RegistryFriendlyByteBuf/StreamCodec）（9）

- `brachy/modularui/network/ModularNetwork.java`
- `brachy/modularui/network/ModularNetworkSide.java`
- `brachy/modularui/network/NetworkHandler.java`
- `brachy/modularui/network/packets/CloseGuiPacket.java`
- `brachy/modularui/network/packets/OpenGuiPacket.java`
- `brachy/modularui/network/packets/ReopenGuiPacket.java`
- `brachy/modularui/network/packets/SyncHandlerPacket.java`
- `brachy/modularui/utils/serialization/network/ByteBufAdapters.java`
- `brachy/modularui/utils/serialization/network/IByteBufAdapter.java`

## §6 单腿专属（原文）

### 仅 1.20.1（33）

- `brachy/modularui/ClientProxy.java`
- `brachy/modularui/CommonProxy.java`
- `brachy/modularui/core/mixins/common/ComponentSerializerMixin.java`
- `brachy/modularui/core/mixins/emi/EmiRenderHelperMixin.java`
- `brachy/modularui/core/mixins/jei/RecipeGuiLogicMixin.java`
- `brachy/modularui/core/mixins/jei/RecipeLayoutAccessor.java`
- `brachy/modularui/core/mixins/jei/RecipeLayoutBuilderAccessor.java`
- `brachy/modularui/core/mixins/jei/RecipeLayoutBuilderMixin.java`
- `brachy/modularui/core/mixins/jei/RecipeLayoutMixin.java`
- `brachy/modularui/core/mixins/jei/RecipeSlotAccessor.java`
- `brachy/modularui/core/mixins/rei/DefaultDisplayViewingScreenMixin.java`
- `brachy/modularui/core/mixins/rei/EntryWidgetAccessor.java`
- `brachy/modularui/integration/emi/recipe/ModularUIEmiCategory.java`
- `brachy/modularui/integration/jei/recipe/ModularUIJeiCategory.java`
- `brachy/modularui/integration/recipeviewer/RecipeViewerUtils.java`
- `brachy/modularui/integration/recipeviewer/util/RecipeDebugDecoratorUtil.java`
- `brachy/modularui/integration/rei/ModularUIReiPlugin.java`
- `brachy/modularui/integration/rei/recipe/ModularUIReiCategory.java`
- `brachy/modularui/integration/rei/recipe/ModularUIReiDisplay.java`
- `brachy/modularui/network/packets/CloseAllGuiPacket.java`
- `brachy/modularui/screen/OpenScreenEvent.java`
- `brachy/modularui/test/TestRecipeViewerGuis.java`
- `brachy/modularui/utils/ObjectList.java`
- `brachy/modularui/utils/handlers/fluid/CombinedFluidHandlerWrapper.java`
- `brachy/modularui/utils/handlers/fluid/CycleFluidEntryHandler.java`
- `brachy/modularui/utils/handlers/fluid/EmptyFluidTank.java`
- `brachy/modularui/utils/handlers/fluid/FluidTankHandler.java`
- `brachy/modularui/utils/handlers/fluid/IMultiTankFluidHandler.java`
- `brachy/modularui/utils/handlers/fluid/MultiTankFluidHandler.java`
- `brachy/modularui/utils/handlers/item/CycleItemEntryHandler.java`
- `brachy/modularui/utils/serialization/network/IByteBufDeserializer.java`
- `brachy/modularui/utils/serialization/network/IByteBufMemberSerializer.java`
- `brachy/modularui/utils/serialization/network/IByteBufSerializer.java`

### 仅 1.21.1（29）

- `brachy/modularui/api/value/sync/IServerMouseScrollAction.java`
- `brachy/modularui/client/ModularUIClient.java`
- `brachy/modularui/core/extensions/IRegistryFriendlyByteBufExtension.java`
- `brachy/modularui/core/mixins/client/AbstractWidgetMixin.java`
- `brachy/modularui/core/mixins/client/GuiGraphicsMixin.java`
- `brachy/modularui/core/mixins/common/ComponentSerializationMixin.java`
- `brachy/modularui/core/mixins/common/RegistryFriendlyByteBufMixin.java`
- `brachy/modularui/core/mixins/common/ReloadableServerResourcesMixin.java`
- `brachy/modularui/core/mixins/common/ServerPlayerAccessor.java`
- `brachy/modularui/core/mixins/common/WorldLoaderMixin.java`
- `brachy/modularui/integration/jei/recipe/ModularUIRecipeCategory.java`
- `brachy/modularui/integration/recipeviewer/RecipeViewerScreenWrapper.java`
- `brachy/modularui/integration/recipeviewer/handlers/fluid/CycleFluidEntryHandler.java`
- `brachy/modularui/integration/recipeviewer/handlers/fluid/EmptyFluidTank.java`
- `brachy/modularui/integration/recipeviewer/handlers/item/CycleItemEntryHandler.java`
- `brachy/modularui/integration/recipeviewer/util/RecipeScreenRenderingUtil.java`
- `brachy/modularui/integration/rei/ModularUIREIPlugin.java`
- `brachy/modularui/integration/rei/recipe/ModularUIREIDisplay.java`
- `brachy/modularui/integration/rei/recipe/ModularUIREIDisplayCategory.java`
- `brachy/modularui/network/packets/CloseAllGuisPacket.java`
- `brachy/modularui/network/packets/package-info.java`
- `brachy/modularui/screen/event/OpenScreenEvent.java`
- `brachy/modularui/utils/FluidTankHandler.java`
- `brachy/modularui/utils/IMultiFluidTankHandler.java`
- `brachy/modularui/utils/LangUtil.java`
- `brachy/modularui/utils/MultiFluidTankHandler.java`
- `brachy/modularui/utils/RegistryAccessContainer.java`
- `brachy/modularui/utils/sides/ClientCallWrapper.java`
- `brachy/modularui/utils/sides/SidedAccessHelper.java`
