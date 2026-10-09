# javac -Xlint:all 豁免台账（compile-warnings-cleanup）

口径：`:mdk:{1.20.1-forge,1.21.1-neoforge}:{compileJava,compileTestJava}` 带
`-Xlint:all` init script 的 mdk 面（不含 third-party/modularui）。数遍于 rebase
main f08d3c27ab 后实测（/tmp/cw_base_*.log 基线，/tmp/cw_A_*.log 本卡扫后）。

进度链：41d1a526e（gregapi 根 109→0）→ 8de69fce（mdk 泛型/注解面 forge 1475→1351 /
neo 553→395）→ 本卡 562c2248+e57280c9（RL ctor 575 removal→0；LOCATION_BLOCKS 15×2
deprecation→0）→ **forge mdk 763（755 dep + 8 rem）/ neo mdk 380（252 dep + 82
this-escape + 46 rem）= 全部声明豁免，见下**。剩余面均已按类核过弃用注记原文，
无「顺手可清」项残留；继续压缩需语义波或平台适配卡，不属零行为改编译面。

政策：javadoc 不逐点撒（700+ 处即噪声）——本文件是单一事实源，按类记豁免理由与
平台证据；行为敏感面禁止用 @SuppressWarnings 静默。

## forge 1.20.1 腿（755 dep + 8 rem）

| 类别 | 站点数 | 豁免理由（证据） |
|---|---|---|
| `BuiltInRegistries.{BLOCK,ITEM,BLOCK_ENTITY_TYPE,FLUID,…}` | ~380 | Forge 注记原文「Use ForgeRegistries instead」（forge-1.20.1-47.4.10-sources.jar BuiltInRegistries.java:139）。迁移=注册模型重构（DeferredRegister/ForgeRegistries 生命周期语义），非机械替换；查询面已由 stonecutter swap 表按需消化（W2/W4）。归注册模型语义波卡。 |
| `BlockBehaviour` 系方法覆盖（use 43/getShape 23/getDrops 12/onPlace 10/getCollisionShape 10/neighborChanged 9/onRemove 6/getSignal 6/…） | ~150 | 覆盖面即 vanilla 调用缝——方块行为必须经这些 override 进入 vanilla 管线；Forge 弃用是为推 IForgeBlock 扩展变体，迁移=逐方法平台手术且调用路径语义在先。禁清。 |
| `ItemColors.register` 31 + `BlockColors.register` 14 | 45 | 客户端着色注册缝，双腿同注记同数；1.21.x 客户端 API 换代（per-state handler），双腿迁移方向不一致。归 client 适配波。 |
| `BakedModel.{getQuads,getTransforms,getParticleIcon}` | 77 | 1.21.1 模型子系统重写的弃用前锋（双腿同数）；本仓 baked model 必须实现这些才进 1.20.1 烘焙管线。禁清。 |
| `Item.getDefaultAttributeModifiers(EquipmentSlot)` | 30 | 双腿弃用方向相反：forge 弃用带槽变体（推 IForgeItem.getAttributeModifiers(slot,stack)），neo 弃用无参变体（推带槽）——任一迁移都加深分叉。禁清。 |
| `CreativeModeTab.builder(Row,int)` | 21 | Forge 注记「use builder()」但 builder() 钉死 Row.TOP,0——BOTTOM/非零列 tab 迁移即行为变更。禁清（CreativeModeTab.Builder 无 row/column setter）。 |
| `SpriteContents` ctor 9、`Block.getExplosionResistance` 8、`WorldGenRegion.getLevel` 6、`BlockModel.getElements` 5、其余散面 | ~50 | vanilla 内部件缝/forge 注记无 1:1 替代；逐点核过（基线 uniq 表），无平台文档化等价替换。 |
| EMI `IRecipeExtrasBuilder` ×6 + `IIngredientRenderer.getTooltip` ×1 | 7 | 第三方 EMI API 弃用面，双腿同弃；EMI 集成归 jei/emi 域卡。 |
| `FMLJavaModLoadingContext.get()` | 1 | Forge 推构造注入 ModContainer（DI 重排），非机械。 |

## neo 1.21.1 腿（252 dep + 82 this-escape + 46 rem）

| 类别 | 站点数 | 豁免理由（证据） |
|---|---|---|
| `sun.misc.Unsafe.{staticFieldOffset,objectFieldOffset,…}`（测试脚手架，19 文件 ×2：GTOfflineTestBase 及 MUI/E2E 测试族） | 38 | 仅 JDK-21 编译的 neo 腿弃用（forge 腿 JDK-17 同调用零警告）；迁移=测试脚手架换 VarHandle，内存语义敏感且双腿 JDK 不同形。 |
| `BakedModel` 系 77 + `ItemColors/BlockColors` 45 + `CreativeModeTab.builder(Row,int)` 21 | 143 | 与 forge 腿同源（上表）；1.21.1 侧弃用即 vanilla 换代推力，1.20.1 无对应替代。 |
| `Item.getDefaultAttributeModifiers()` | 15 | 同 forge 腿「双腿弃用方向相反」条。 |
| `Block.getExplosionResistance` 8、`WorldGenRegion.getLevel` 6、`BlockModel.getElements` 5、`BlockStateBase.{rotate,getSoundType,getLightEmission}` 9、其余散面 | ~56 | 与 forge 腿「其余散面」同族：1.21.x vanilla 换代弃用，1.20.1 无对应替代，逐点核过无 1:1。 |
| `FluidStack.isFluidEqual` | 14 | 候选替换 `isSameFluid`（仅流体）与 `isSameFluidSameComponents`（流体+组件）语义不同——isFluidEqual=流体+组件等、忽略数量；逐点语义裁决属行为面。禁机械换名。 |
| `FluidType.initializeClient` | 12 | NeoForge 客户端扩展注册结构重排（列 IClientFluidTypeExtensions 的机制变更）。 |
| `ModelProvider/Builder.parent(ResourceLocation)` | 9 | 1.21 模型 API 换代（parent 走 Holder<ModelTemplate>），1.20.1 腿无对应形态，跨腿不可同形。 |
| EMI `IModIngredientRegistration/IIngredientHelper/IIngredientRenderer` 等 | ~11 | 第三方 EMI API，双腿同弃。 |
| **this-escape** | 82 | 构造器泄漏 this——修复须工厂/初始化拆分（构造序重排），前卡 8de69fce 已声明为行为风险池非静默。禁编译面清。 |

## third-party/modularui（第三方面；modularui-lint-cleanup 卡清理后残面）

独立卡 modularui-lint-cleanup（2026-10-07）已按 41d1a526ea 同策略清完零行为面：
**forge 80→27 / neo 145→109**（可清面=RL ctor 14 + unchecked/rawtypes/varargs
固有注解 + serialUID serialver 实证 3 类 + 冗余 cast 2 + 机械泛型修 4 文件；
实现面逐文件记录在子仓 `third-party/modularui/DIVERGE.md` §7，commit 链
d91f10e/91d20fc/76a7f33/ac9443d）。残面全部声明豁免：

| 腿 | 残量 | 构成与豁免理由 |
|---|---|---|
| forge | 27 | dep 16（BuiltInRegistries×7=注册模型语义波、IThemeApi.registerWidgetTheme×2 自有主题 API 迁移、LevelReader.hasChunk/getSeaLevel×2、RenderSystem.runAsFancy、ContainerScreenWrapper 弃用 ctor、Fluid.is(TagKey)+BlockBehaviour.use=vanilla/forge 行为缝，均无 1:1 零行为替换）+ removal 6（FMLJavaModLoadingContext/ModLoadingContext.get()×3=构造注入 DI 重排（同 mdk forge 腿条）、JEI IRecipeSlotDrawable×3=第三方 JEI API 归 RV 域卡）+ rawtypes 1（PanelSyncManager.getSyncHandlerFromMapKey 裸返回——raw→<?> 公开 API 收窄有 jarJar 下游源码兼容风险）+ try/overrides/overloads/fallthrough 各 1（修复=行为/API 变更：资源收尾重构、补 hashCode、改名、补 break） |
| neo | 109 | **this-escape 82**（JDK-21 lint，构造序重排面，同 mdk neo 腿「禁编译面清」条 wholesale 豁免）+ dep 13（LevelReader×2、IThemeApi×2、RecipeScreenRenderingUtil×5 自有弃用工具、RenderSystem.runAsFancy、RandomSource.createThreadSafe、Fluid.builtInRegistryHolder×2，无 1:1 或归语义波）+ removal 9（FluidStack.isFluidEqual×3=同 mdk neo 腿条「候选替换语义不同禁机械换名」、ScreenEvent.BackgroundRendered=NeoForge 事件换代、JEI createTypedIngredient/createClickableIngredient/getClickableIngredientUnderMouse/IClickableIngredient.getIngredient×5=第三方 JEI API）+ rawtypes 1（同 forge 条）+ try/overrides/overloads/fallthrough 各 1（同 forge 条） |

口径延续：javac lint 警告行（剔除 AP/classpath 噪声行：forge 1 条
「没有处理程序」、neo 5 条 log4j BaselineIgnore + 2 条 AP 注记），init script
同款（-Xlint:all 注入 JavaCompile），clean 全量编译实测
（/tmp/mu_base_*.log 基线、/tmp/mu_final_*.log 扫后）。modularui 面继续
压缩需 JEI/EMI 适配波、注册模型语义波或 NeoForge 构造序重构卡，均非零行为面。

## 本卡已清面（存档）

- 562c2248：ResourceLocation 两参/单参 ctor → `fromNamespaceAndPath`/`parse`
  （forge 47.4.10 backport 工厂 sources.jar ResourceLocation.java:262/:267，
  javadoc 明示 replacement；neo 侧 swap 表条目转休眠）——forge removal 575→0。
- e57280c9：`TextureAtlas.LOCATION_BLOCKS` → `InventoryMenu.BLOCK_ATLAS`
  （forge 侧 LOCATION_BLOCKS 字段体即该常量别名，TextureAtlas.java:30）——双腿
  deprecation 各 15→0。

## 席54 复测（2026-10-09，merge 前 clean 口径实测）

rebase main cedf692692 后以 -Xlint:all init script 双腿 `--rerun-tasks` 全量
clean 编译实测（/tmp/cw_meas_final2.log；口径同上：mdk 面 javac lint 行，不含
third-party/modularui）：

- **RL ctor 残余 = 0（双腿）**。main 演进（卡①②③合入波）新增的 115 处
  RL ctor 站点（两参→`fromNamespaceAndPath`/单参→`parse`，含 FQ 形与嵌套参
  形）由审查席缝补按本卡语义原位重套——sweep 声明的「forge RL removal→0」
  在 merge 终态成立。
- **实测终态：forge mdk 797（main 398 + test 399）/ neo mdk 419（main 273 +
  test 146）**，较本卡基线（763/380）+34/+39 = f08d3c27a 之后 main 演进新增
  面（新代码的 dep 主体 + 下述 safe-face 池），非本卡语义回退。
- **新 safe-face 池（可清债候选，非豁免）**：cast 24 / rawtypes 12 /
  unchecked 4 / static 2——41d1a526ea/a5a637536b 同类机械面，后继 sweep 卡
  候选。neo this-escape 86 归构序重构卡（上表既有条）。
- **modularui 面复测吻合**：forge 27 / neo 109（与 modularui-lint-cleanup
  残面记账一致；席50 移交的「6/9 real clean 口径」=该残面内的真实可清子集
  JEI/EMI+固有注解面，归 RV 域卡，非本卡 remit）。

## safe-face sweep（2026-10-09，work/safe-face-sweep）

席54 复测标注的 safe-face 池已全数真修清零，无新增豁免。口径同上（mdk 面
javac lint 行，--rerun-tasks 全量 clean 编译实测，/tmp/sfs_base_*.log 基线、
/tmp/sfs_after_*.log 扫后）：

- **before/after 对账**：forge mdk 797→777（cast 12→0 / rawtypes 6→0 /
  unchecked 1→0 / static 1→0；removal 8 持平）；neo mdk 416→394（cast 12→0 /
  rawtypes 6→0 / unchecked 3→0 / static 1→0；removal 46 持平，RL ctor 不回潮）。
  池合计 42 腿站点（cast 24 / rawtypes 12 / unchecked 4 / static 2，两腿求和）
  与席54 移交数吻合。
- **裁决**：全部走真修（a），零台账豁免（b）、零行为红线——逐站证据：
  - 冗余 cast ×12 站点（双腿）：编译器冗余判定即零行为证明——删
    `(int)Math.min(int,int)` ×4（GT6MassStorageBlockEntity :424/:595/:707/:712，
    int 恒等转换无字节码）、`(long)EU_PER_LAVA`（GT6RecyclingProcessing :212，
    常量本就 long :126）、`(BlockEntityType<?>)` 三元 ×2（GT6RotationEngine
    :50/GT6SteamTurbine :109，两臂同型）、registerItemFixture 泛型推断 cast ×4
    （GT6QuMachinesTest :68/:77、GT6QuMachinePairE2eTest :48/:56，
    `<T extends Item>` 推断同型）、getTextureAtlas 返回值 cast（GT6MassStorage
    Renderer :115，双腿签名同 `Function<ResourceLocation,TextureAtlasSprite>`）。
  - rawtypes ×6 站点（双腿）：`new Class[]`→`new Class<?>[]`
    （GTEntityBlockDistLeakPinTest :168/:169/:218/:222/:225/:227，擦除同形）。
  - unchecked ×3 站点：窄域 `@SuppressWarnings("unchecked")`+理由（41d1a526ea
    固有站点同策略；cast 保留在码，注解仅记擦除不可表达性）——GT6MaterialTree
    JeiPanzoomTest :510（Proxy Object→泛型 IFocus，双腿）、GT6MassStorage
    BlockEntity/GT6SteamTurbine 的 21.1 臂 `getCapability` `(T)` 分发（BlockCapability
    无 Class token，平台形态；仅 neo 腿）。
  - static ×1 站点（双腿）：`RECIPES.chargingLockerRecipeId`→`GT6CraftingRecipes.
    chargingLockerRecipeId`（invokestatic owner 不变，GT6ChargingLockerRecipe
    ARulingTest :93）。
- **政策重申**：本段不豁免任何新类目；residual 池仍以上文各既有条为准
  （forge rem 8 / neo rem 46+dep、this-escape 86、modularui 27/109 不动）。
