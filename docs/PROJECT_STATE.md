# 项目状态（镜像）

> 权威数据在 MCP `gt6-brain` 的 state 里（`state_read()`）。本文件是人可读镜像，
> 由各 Agent 在重大状态变更时同步。模板如下，随进度填充。

## 当前阶段

`第 4 阶段：管线 / Cover / 多方块渲染（BakedModel）+ 首台加工机器`（**2026-08-30 收官**：八卡全数合入 main HEAD 0d21a59，根 188 + mdk 199 单测全绿，首台真加工机器 Oven / 流体管线 / 多方块框架 / 粘性罐桶 / Cover 域 / C 档渲染基建 / 逐面流向控制全落地，ADR-P4 六条验收线全满足）

> **平台修正 2026-08-29**：原目标"NeoForge 1.20.1"被证伪——NeoForge 官方 maven 从未发布 20.1.x 产物（versions API `filter=20.1` 返回空，主会话独立复核），NeoForged 自家 ModDevGradle 把 1.20.1 路由给 `legacyforge` 变体，文档站最早只到 1.20.3。用户裁决：目标平台 = **MinecraftForge 1.20.1（47.4.10）**，构建插件 = MDG legacyforge 2.0.144。1.20.1 的 API 面即 `net.minecraftforge.*` + RegistryObject（DeferredHolder 是 20.2+ 才有），第 1 阶段的所有调研结论不受影响。

## 里程碑

- [x] 第 1 阶段：材料系统纯逻辑抽取（无 MC 依赖，含单元测试）——2026-08-29 收官
- [x] 第 2 阶段：Forge 1.20.1 MDK 挂载 + 注册桥 + DataGen——2026-08-29 收官（188 测全绿；2469 材料物品注册；runData 管线可复现）
- [x] 第 3 阶段：BlockEntity + AbstractContainerMenu + Screen 框架——2026-08-30 收官（221 测全绿；56253 物品 + 96 tab；BE/Menu/Screen/chest 全链）
- [x] 第 4 阶段：管线 / Cover / 多方块渲染（BakedModel）+ 首台加工机器——2026-08-30 收官（387 测全绿；Oven/管线/多方块/桶/Cover/渲染基建/流向控制八卡）

## 关键决策

（由 /architect 与 /researcher 持续写入，同步至此）

| 日期 | 主题 | 决策 | 证据 |
|---|---|---|---|
| 2026-08-28 | 基础设施 | 检索采用混合语义+BM25+RRF；原版 1.20.1 反编译源码入索引 | tools/README.md |
| 2026-08-28 | 材料系统边界 | GT6 材料系统核心在 `gregapi/oredict/`（模型）+ `gregapi/data/`（MT/AM/ANY/OP/TD 数据表），非旧资料所称 `gregtech/api/enums/` | gregapi/oredict/OreDictMaterial.java:48-52；gregapi/data/MT.java:43-47 |
| 2026-08-28 | 第1阶段拆解 | 材料 5 卡：foundation → model → (dataset ∥ ore-prefix) → graph | OreDictMaterial.java:209-330；OreDictPrefix.java:54-133；MT.java 4118 行 |
| 2026-08-28 | Gradle 骨架 | 单模块纯 java-library（Java17+JUnit5）先行，保留 gregapi 包名；第 2 阶段多项目挂载 MDK | 第1阶段验收=无 net.minecraft import 的 gradle check |
| 2026-08-29 | 目标平台 | "NeoForge 1.20.1"证伪（官方 maven 无 20.1.x），用户裁决改用 **MinecraftForge 1.20.1（47.4.10）**，构建插件 MDG legacyforge 2.0.144 + Gradle 8.14 + Java 17 | maven versions API filter=20.1 空（双独立查询 2026-08-29）；MDG 插件注册表 "legacyforge: Forge platform, up to 1.20.1"；GTCEu Modern 1.20.1 用 Forge 47.4.0（libs.versions.toml:2-4） |
| 2026-08-29 | 模块挂载 | 多项目 include（根目录保留 gregapi java-library，`settings.gradle include 'mdk'`，mdk 依赖 `implementation project(':')`）；composite 后备，mavenLocal 否决 | IDE 单工程同步/单任务图增量/`:check` 独立跑根模块测 |
| 2026-08-29 | 注册桥 | 方案 A 两段桥：FMLConstructModEvent.enqueueWork 跑 MT.init()（open→closed）→ 直接监听 RegisterEvent 遍历 MaterialRegistry×OP 动态灌入 → FMLCommonSetupEvent.enqueueWork 调 applyCrucibleAlloyReferences（postInit 等价物）；DeferredRegister 逐条句柄只适合少量手写物品；Registrate 留第 3 阶段 | GTRegistrate.java:148-151（LOW 优先级监听 RegisterEvent）；DeferredRegister.java:177-178（窗外抛 ISE）；FMLCommonSetupEvent.java:24-28 |
| 2026-08-28 | MC 耦合剥离 | NBT→Serializer 接口；FluidStack/Enchantment/Achievement→字符串/枚举 ID；TextureSet/IIconContainer→名称引用；ItemStack 字段删除 | OreDictMaterialStack.java:31；OreDictMaterial.java:252-254,315,319 |
| 2026-08-28 | 红线-静态初始化 | 禁复刻 MT.java 巨型静态块（65536 上限）；数据分批注册；注册表可重置；GAPI.mStartedInit 改注册表状态机 | MT.java:45 作者自注；OreDictMaterial.java:153-155 |
| 2026-08-30 | BE 形态 | 共享 BlockEntityType + validBlocks 多挂为默认（独立 BET 仅单 Block 最小面例外）；Capability（ForgeCapabilities.ITEM_HANDLER）缝合进 BE 框架卡，Forge patch 在 setRemoved/onChunkUnloaded 自动插 invalidateCaps，只需覆写 invalidateCaps | ADR-P3-1/P3-2；RegisterCapabilitiesEvent.java:14-16；IItemHandler.java:15（@AutoRegisterCapability）；BlockEntity.java.patch:45/:51 |
| 2026-08-30 | 注册接线 | 新注册一律卡内自持监听，GT6Mod/GTModBusListener 全阶段冻结（并行前提）；自持取 mod bus=Bus.MOD.bus().get()（Mod.java:81）；1.20.1 无 RegisterMenuScreensEvent，MenuScreens.register（Forge AT 提权 public）唯一挂法 FMLClientSetupEvent.enqueueWork | ADR-P3-3/P3-4；accesstransformer.cfg:73；forge-docs gui/screens.md:314 |
| 2026-08-30 | 注册宇宙 | 全量前缀注册宇宙=上游物品路径 105 前缀（Loader_Items.java:57-171 逐行核验恰 105），468×isGeneratingItem naive 展开 476,183 对否决；ore/block/pipe/wire 走 PrefixBlock/MTE 非物品路径；id 撞车 first-wins | ADR 2026-08-30-p3-fullprefix-registration-universe；runServer 日志 56253/0 撞键/945 归并/363 跳过 |

## 第 1 阶段模块卡（2026-08-28）

> 材料系统实际位置：`gregapi/oredict/` + `gregapi/data/`。路径均已源码验证。依赖序：1 → 2 → (3 ∥ 4) → 5。

| # | slug | scope（做什么 / 不做什么） | GT6 源文件 | 依赖 | 风险 |
|---|---|---|---|---|---|
| 1 | gt-material-foundation | TagData/条件系统、单位常量、OreDictMaterialStack 算术；不做任何注册表 | gregapi/code/TagData.java:37-78；gregapi/oredict/OreDictMaterialStack.java:20-40；gregapi/data/CS.java（U/UD） | 无 | Stack 耦合 NBT（:31）→ 剥为 Serializer 接口 |
| 2 | gt-material-model | OreDictMaterial 模型 + createMaterial 注册（名称校验/ID/覆盖链）+ 组分加权化学计算；不做 MC 侧字段 | gregapi/oredict/OreDictMaterial.java:142-330（注册/字段）、:469-499（组分计算/化学式） | 卡1 | 耦合 GAPI.mStartedInit（:154）→ 注册表 open/closed 状态机 |
| 3 | gt-material-dataset | 元素周期表 + 全材料 + 反物质 + 别名组分批移植与保真校验；不做流体/物品生成 | gregapi/data/MT.java（4118 行）、AM.java（485 行）、ANY.java（158 行） | 卡2 | 65536 静态块上限（MT.java:45 自注）；TextureSet/TC/Enchantment 引用改字符串 ID |
| 4 | gt-ore-prefix | OreDictPrefix 模型：长前缀优先解析、材料量（U 单位）、命名模板、副产物；不做物品/配方注册 | gregapi/oredict/OreDictPrefix.java:54-150；gregapi/data/OP.java | 卡2 | 耦合 ItemStack/CreativeTabs/recipes/thaumcraft（:37-42）→ 全裁剪 |
| 5 | gt-material-graph | 12 类转换链（mTarget*/mTargeted*）查询 + 合金组分引用图；不改卡 2 文件本体 | OreDictMaterial.java:283-310、:260-268 | 卡3、卡4 | 图遍历需全量数据就绪；保真单测排最后 |

验收基线（每卡）：`gradle check` 全绿 + 卡内单测 + 源码 grep 无 `net.minecraft` import。
首个 coder 任务：**gt-material-foundation**（建 Gradle 骨架 + TagData/OreDictMaterialStack + 单测）。

> **合入记录 2026-08-28**：卡1 gt-material-foundation 审查通过合入 main（22e7532 e685d28 e42f069 → merge 2f3be03）。34 单测全绿、零 `net.minecraft` 命中；语义抽查与 1.7.10 原文逐字一致（含 equals 怪癖 :82/:84、U 家族截断除法）。NBT 剥离为 MaterialStackSerializer 缝隙，`a`/`i`/`m` 键语义保留，Phase-2 NBT 绑定须用 short 写 `i`（GT6 存档兼容）。OreDictMaterial/MT 为标注 placeholder 最小支柱，归卡2/卡3重写。

> **合入记录 2026-08-29**：卡2 gt-material-model 审查通过合入 main（5599317 5860336 46fbcb4 52ade84 + 审查修复 0ab2bf1 → merge 1b7251c）。73 单测全绿、主仓库门禁复跑通过；createMaterial 与上游 :142-176 逐字（W=32767/黑名单/覆盖链，isOpen 仅约束 aID>=0）；TDG 14 键名对齐 TD.java:305-571。审查层两处修复：NUM_SUB 按上游 CS.java:169-201 修正为 301 项（index 300 = "₃₀₀₊" 截断）；setMcfg/12 set* 改走上游 OM.java:485 null→null Stack 语义。插曲：首任 reviewer 被 GPG 口令阻塞失联，暂存修复经二任 reviewer 独立核验后由主会话代签提交。

> **合入记录 2026-08-29**：卡3 gt-material-dataset 审查通过合入 main（12d8eda a693645 590f68d → merge e408927）。97 单测全绿（新增 MTTableFidelityTest 19 项）、零 `net.minecraft` 命中、主仓库门禁复跑通过。MT/AM/ANY 全量材料表移植：1273 材料按上游声明顺序拆 reg0000..reg0038 批次（尊重上游 MT.java:45 65536 静态初始化红线），MT.NULL 经 createMaterial(-1,"NULL") 归位、卡1 桩退役、OreDictMaterial 无注册构造器私有化，addAlloyingRecipe :455-466 逐字。MC 剥离取舍 7 项全部核验成立：TextureSet→同名两变体名引用、ModData→MDRef 经 putWithMods 递归漏斗（Iterable 死角不可达）、field_151370_z→"luckOfTheSea"、9 个 setter helper 逐字复刻（setPriorityPrefix 仅赋索引，OreDictManager 副作用 :1338-1341 归 prefix/manager 卡）、setTextures 上游 :1025 无限递归 bug 确认不可达后改空默认基例、aspects/lens(44)/visDefault(192)/MT.DATA 缓办注明闭环、TDG 同一性活表证明。清点复算：ores 308、steal 45、setDensity 42、setOreMultiplier 48、setPriorityPrefix 72、Enchantment 507、TD createTagData 键名多重集合与上游一致。已知瑕疵：测试注释/提交信息中 "Diamond upstream :2183" 行号引用有误（实为 :1358），断言语义不受影响。

> **合入记录 2026-08-29**：卡4 gt-ore-prefix 审查通过（内容预审 pass）合入 main（30a499e d5fe13d d611eef 8a47124 → merge 3d0aac3）。rebase 至 ae78dc7 零冲突（mPriorityPrefix 字段区与卡3 构造器私有化不相邻，两处改动共存核实于 OreDictMaterial.java:134-136/:185,194）；重写后 4 提交 `git verify-commit` 全验。133 单测全绿（97 既有 + 36 卡4）、零 `net.minecraft` 命中、worktree 与主仓库门禁均复跑通过；OP 依赖的 TD 键由卡3 真实 TD 实例承接，TagData 幂等自动统一，MT/AM/ANY 与 OP 共存无回归。

> **合入记录 2026-08-29（第 1 阶段收官）**：卡5 gt-material-graph 审查通过合入 main（1bffca5 aa8a130 → merge 972dbad）。rebase 至 8cf8ba1 零冲突（仅新增 2 文件，重写后提交 `git verify-commit` 全验）。161 单测全绿（133 既有 + 28 卡5）、零 `net.minecraft` 命中、worktree 与主仓库门禁均复跑通过。MaterialGraph 为移植侧新增查询层（上游无独立链查询助手）：targeting 三重谓词 UT.java:1047 逐字、applyCrucibleAlloyReferences 逐字 GT_API_Post.java:820（保留无去重怪癖并以测试固化，合金反向引用两层结构差分验证）、expandChain visited+深度上限终止性全表扫描证明。卡3 遗留缺口（reset 后 AM/ANY 不重注册、TECH 别名静态字段）不阻塞本卡，由主会话 halt 阶段落账。**至此第 1 阶段 5 卡全部合入，材料系统纯逻辑层收官。**

## 第 2 阶段并行批 1（2026-08-29，批量合并会话）

> **合入记录 2026-08-29**：卡① p2-mdk-skeleton 审查通过合入 main（fe82a6e 74493d1 → rebase 9f162e4 5833a8a → merge 69b9d39）。GPG 全验（含 rebase 重写后复验）；FILES_SCOPE 11 文件零越界（settings.gradle/gradle wrapper 8.14/mdk/**/.gitignore），根 build.gradle 与 src/ 零触碰。mdk/ 子项目：legacyforge 2.0.144 + Forge 1.20.1-47.4.10 + Java 17，`implementation project(':')` 消费根 gregapi 不 shadow；generateModMetadata 展开实证 jar 内 `loaderVersion="[47,)"`、`modId=gt6`、LGPL-3.0-or-later；mdk-0.1.0.jar 仅含 mods.toml/pack.mcmeta/GT6Mod.class 零 gregapi 类。门禁：worktree 与主仓库 `clean check --no-build-cache` 161 测 0 FROM-CACHE + `:mdk:build` 全绿。**注意：门禁 runner 自此必须用 `./gradlew`（8.14），系统 gradle 8.7 过不了 MDG legacyforge 的 ≥8.8 检查。**

> **合入记录 2026-08-29**：卡② p2-registry-reset-idempotency 审查通过合入 main（d360507 3e37ba0 → rebase 15e0499 bd0f461 → merge 5564226）。三缺口（卡3 缺口①②③）同根因修复核验：批次搬运全表归一化比对（非抽查）OREMATS 87/87、STONES 68/68、WOODS 110/110、UNUSED 123/123 与 main 基线逐元素一致（卡注 378 为记账笔误，实 388 条）；ANY 两相 create(54)/init(59) 与 main 逐字一致，身份守卫与 MT.init 身份检查家族同型，两相必要性成立（MT.create:315 等 20+ 助手洪泛期引用 ANY 字段）；STONES.init() 前置于 reg0037 为 reg0037 内 `SpaceRock = STONES.SpaceRock` 别名语句的必要前提，时点对齐上游 :1919-1945 别名块触发序（上游 init() 内 STONES.Basalt.getClass() 此时已是 no-op）；OREMATS/WOODS 对齐 :1890-1900 强加载点、fixups 对齐 :1879-1883、UNUSED 原位；MT.NULL static 块回退 + 每代重注册（同名 -1 复用防首代重复注册）；ALL_MATERIALS_REGISTERED_HERE 每代清零；addAlloyingRecipe 区零触碰（无去重保真）。MTInitResetIdempotencyTest 7 项闭环缺口数字：2200 全量回灌、Any 组、Anti 组、TECH/OREMATS 别名绑定当前代、Invar 单配方两次洪泛、postInit 对称。168 测全绿 0 FROM-CACHE。审查 minor：MT.NULL.mHandleMaterial 上游 AnyWoodPlastic → 移植 null（两相后 static 块期 ANY 字段未建，现无读取方；工具卡落地时注意）；TECH tMake* 串冻结首代（内容代间不变，惰性）。

> **合入记录 2026-08-29**：卡③ p2-material-condition-system 审查通过合入 main（3846fc2 086f321 c50223b → rebase 59111e5 bad07ff 783387c → merge 21ee363）。OreDictMaterialCondition.java 127 行 vs 上游 :1-126 全 diff 仅头注来源行与空行空白归一，14 谓词零语义偏差（零补齐项证实）；47 条 create 归一化比对独立复跑 47/47 零偏差（空白/`aspects()` 剥离/TD.Creative.HIDDEN→HIDDEN/行尾终止符归一），ingotHot 插位 = 上游声明序（ingotDouble/ingot 之间），toolHead 族 43 条入 regArmor rotor 后、arrow 3 条入 regPipes bulletGtLarge 后（上游 :281-283 紧邻序保持）；TD seam 6 常量键名+显示名实测与上游一致（INGOTS_HOT/WOOD/TOOL_HEAD/NEEDS_HANDLE/NEEDS_SHARPENING/WEAPON_ALIKE；port 注释所引 TD 行号偏移约 3 行，仅注释错）；`ingotHot.mHeatDamage = 3.0F` 对齐上游 :578（familiar 段前，前置 MC 耦合行保持缓办）。OPTest 406→453、421→468 闭环，OP 前缀 421→468 全量就位（ADR-P2-6 验收线⑤）。与卡②组合核查：条件卡全部文件零 final 性依赖（仅 1 处 javadoc 提及 ANY/TECH），前缀注册序的 chemtube 位移为卡4 批次架构既有形态非本卡回归（VALUES_SORTED_INTERNAL 为长度序，OP.get 歧义匹配不受影响）。20 项新测试行为级断言。**188 测（161+7+20）全绿 0 FROM-CACHE，worktree 与主仓库门禁均复跑通过，主仓库 grep net.minecraft 零命中。**

## 第 2 阶段串行批 2（2026-08-29，单卡审查合并）

> **合入记录 2026-08-29**：卡 p2-registration-bridge 审查通过合入 main（c3500b5 5776f30 ecaa096 2122ce8 零 rebase → merge 308f85e）。GPG 全验；isGeneratingItem 判据与上游 PrefixItem.run():104 一致（OreDictPrefix:364-366 forced‖!blacklist∧mCondition，vs canGenerateItem 选型正确），四前缀条件逐字（dust=Or(DUSTS,DIRTY_DUSTS)/ingot=INGOTS/gem=GEMS/plate=And(Or(ingot,gem.NOT),PLATES)）；%s 填参=Component.translatable(模板键, mNameLocal)（GTCEu TagPrefix.getLocalizedName 同构）；特例键 Language.has 隔离进 @OnlyIn ClientSeam+dist 守卫（GTCEu :1321 为无守卫调用，移植更保守；服务端仅服务端侧取名回退模板名，客户端显示不受影响）；eventbus-6.0.5 unregister(Object) 实例键移除经 javap 实证；同名材料经 MaterialRegistry.get() mTargetRegistration 链归并。jshell 独立复算：2200 材料/468 前缀/ingot483+dust1096+gem217+plate673=2469（19 对同名双槽被归并、id 零碰撞）/合金反链 91 全一致。**ecaa096 FILES_SCOPE 越界裁决接受**：mdk/build.gradle `additionalRuntimeClasspath project(':runtimeElements')` 经 MDG 2.0.144 字节码实证——per-run legacyClasspath 配置（ModDevRunWorkflow lambda$setupRunInGradle$19）唯一 extendsFrom 即 RunModel.getAdditionalRuntimeClasspathConfiguration()，`implementation project(':')` 不进 BootstrapLauncher `-DlegacyClassPath.file` 且 gregapi 非 mdk mods 块 sourceSet（mod 自身类走 fml.modFolders）；该配置为 MDG 官方扩展点，仅影响 runs 不影响 jar，ADR-P2-1 不 shadow 不变。门禁：worktree+主仓库 188 测 0 失败 0 FROM-CACHE + `:mdk:build` 绿；合并态 `:mdk:runServer` headless 到 **Done (12.384s)** 零 GT6 告警（2200 材料/468 前缀/2469 物品/91 反链与计数全对账）。datagen 接口：lang 键 `gt6.tagprefix.<prefix_snake>`='%s 名' + `gt6.material.<material_snake>`（MaterialPrefixItem.snakeCase），iconset 取 mTextureSetsItems，tint=RegisterColorHandlersEvent.Item+MaterialPrefixItem.tintColor()，句柄查询=GTMaterialItems.get(prefix,material)。

> **合入记录 2026-08-29**：卡 p2-datagen-pipeline 审查通过合入 main（6a20688 55b1019 7c98d2a 78e37a0 零 rebase → merge 139c8c4；.cache 出库裁决另落 1676a38）。GPG 4/4。**iconset 考古链独立复核全证实**：上游 PrefixItem.java:136-138 取 `material.mTextureSetsItems.get(prefix.mIconIndexItem)`——贴图集是材料属性（.setTextures 赋予），mIconIndexItem 是 TextureSet.addToAll(:73-80) 维护的全局同名索引（对所有 set 同槽位有效）；OP.mNameTextureSet 全树仅 声明:84/默认:101/setter:322/addToAll 入参:399,408，确非图标源（注册桥 handoff 提示有误，coder 裁决正确）；上游 OreDictMaterial.java:252 默认 `mTextureSetsItems = SET_NONE[1].mList`，移植侧空表回退 "none" 保真（TextureSet.java:188）。抽查 12 模型与上游 SET_* 逐一对上（Fe=METALLIC/Au=SHINY/Coal=LIGNITE/Sand=SAND/Diamond/Emerald/Lapis/Glass/NetherStar/Rubber/Wood）。产物双向审计：2469 模型=注册集合（items() 主源 + RegistryObject.getId() 零漂移）→103 (iconset,prefix) 组合/37 集合/none 零命中/PNG 缺失 0；103 PNG→零冗余；lang 2241=467 tagprefix+1773 material+1 itemGroup，模板=mMaterialPre+%s+mMaterialPost（gem 上游 OP.java:173 pre/post 全空，卡注 :1218 系笔误）；compressed/Compressed（OP.java:201 addIdenticalNames 别名进 VALUES，host 先注册）唯一 snake 撞键，first-wins 保 host 模板语义正确，别名键（pulp/item_dust/ore_gem 等）空模板 "%s" 与上游 null pre/post 行为一致；运行时 MaterialPrefixItem 模板键同 snakeCase 同源，四前缀零影响。**.cache 裁决**：HashCache 本地账本内嵌运行时间戳（跨机器伪 diff），移出版本库+gitignore（`mdk/src/generated/resources/.cache/`），产物 JSON 保留入库；净克隆模拟（删 .cache 跑 runData）written:2470 且 git status 干净=入库产物逐字节可复现，二跑 written:0 幂等。门禁：worktree `:mdk:build` 绿；主仓库 `:clean :check --no-build-cache` 188 测 0 失败（test-results XML 复核）+ `:mdk:build` + runData 双跑幂等 + `:mdk:runServer` **Done (3.407s)** 零 GT6 ERROR（8 WARN 全环境固有：FML language jar/终端/LanServerPinger/union schema）。遗留：游戏内模型+tint 可见性需 runClient（归 p2-phase-closeout）；新 iconset 出现时 `gen_textures.py --scan` 补 PNG 再重跑 runData。

## 已知 Bug

（见 `state_read("known_bugs")`）
- 2026-08-28（已修复）：gt6-brain `state_update` 故障（`name 'json' is not defined`，根因 `tools/gt6_rag/search.py` 缺 `import json`；`state_read` 空表时侥幸不炸）。已修复并回填 state：decisions(5 条)/progress/tasks(5 卡)/known_bugs 四键读写验证通过。
- 2026-08-29（已修复）：`remember`/`recall` 间歇性超 60s MCP 预算。根因：共享 GPU 嵌入服务（embed 8937 恒定 ~232% CPU，另有 meow-translator 负载）饱和时，`embed.py` 交互式小请求仍走 240s 超时×7 重试（最长 ~30 分钟隐形挂死）。修复：`_post_batch` 按请求规模分流预算——小请求 15s×1 重试（总预算 <45s，快速报错），大批量（索引）保留 240s×7。负载下实测小请求 0.26s 通过。

## 已知陷阱（KG 摘录）

（见 `kg_query()`）

## 基础设施备忘

- 反编译脚本：`tmp/get_vanilla.sh`（1.20.1 客户端 + SpecialSource 重映射 + Vineflower）
- 全量重建索引：`cd tools && .venv/bin/python -m gt6_rag.index all`
- 新资料源登记：`tools/sources.json` → `refresh_index`
- **服务总线 `tools/gt6_services.sh {start|stop|restart|status} [brain|embed|rerank|all]`**：统一管理三个常驻服务，按端口精准启停（llama 加载期 /health 503，探活只认 200）。
- **GT6 Brain MCP 已换 Streamable HTTP 常驻守护**（弃用 FastMCP/stdio——anyio 线程层在长驻进程中出现过调用卡死）：端点 `http://127.0.0.1:8939/mcp`（`/health` 探活），日志 `tmp/index/brain-server.log`；协议壳手写 JSON-RPC（`tools/gt6_rag/server.py`），kwargs 派发（防可选参左移错位），15 工具语义与原版逐一对齐。ZCode 配置 `.zcode/config.json` 已切 `type: http`。
- GPU/嵌入服务：llama-server（8937 嵌入 / 8938 精排）已换 ROCm 修复库 `libhsa-runtime64.so.1.21.0`（rocm-systems PR #7898，修 WSL2 dxg thunk AsyncEventsLoop 轮询自旋，空闲 232%→5% CPU）。重启脚本 `tools/embed_server.sh`（8937）；8938 精排命令记录于 `/tmp/llama-rerank.log`。回滚：`ln -sfn libhsa-runtime64.so.1.18.0 /opt/rocm/lib/libhsa-runtime64.so.1 && sudo ldconfig`。WSL 下 HIP 枚举设备须 `HSA_ENABLE_DXG_DETECTION=1`。

## 第 2 阶段收官记录（2026-08-29，主会话 phase-closeout）

> 五卡合入链：69b9d39（骨架）→ 5564226（幂等）→ 21ee363（条件系统）→ 308f85e（注册桥）→ 139c8c4（DataGen），docs 镜像至 a6312ab。另有骨架前置 docs 提交与 .cache 出库 1676a38。

**ADR-P2-6 验收线核验**（主会话逐条）：
1. ✅ 根 `gradle clean check --no-build-cache` 188 测（161+7 幂等+20 条件）全绿 0 FROM-CACHE，根 src 零 net.minecraft。
2. ✅ `:mdk:build` 出 jar；`:mdk:runServer` headless 到 Done（三次不同形态验证：桥卡 3.471s / DataGen 卡 11.703s / 合并态 3.407s）；`:mdk:runData` 净克隆模拟 written:2470 逐字节可复现 + 二跑 written:0 幂等。
3. ✅ 游戏内可见性：runClient 冒烟到资源加载完成——blocks 图集 1024x512x4 烘焙成功（2469 模型 + 103 占位贴图全过解析），gt6 零模组错误（仅环境噪音：narrator libflite 缺失 / ALSA 无声卡 / Realms 离线）。**交互级 /give + tint 目视验证留给用户下次 runClient**。
4. ✅ 幂等回灌：reset→MT.init() 全量重灌 2200 条（Anti*/Any*/TECH 别名/NULL 全量），Invar 恰 1 条配方，postInit 两代对称（MTInitResetIdempotencyTest 7 项固化）。
5. ✅ OP 全量 468 注册（453 字段，条件系统解锁 47 条）。
6. ✅ 六卡 GPG 验签 + review-merge 合入 + 落账齐全（state/KG/remember/docs）。

**阶段成果数字**：注册 2469 物品（ingot 483 / dust 1096 / gem 217 / plate 673，判据=isGeneratingItem 上游逐字）；91 条合金反向引用接线；2470 生成 JSON（模型 2469 + lang 2241 条两表零组合爆炸）；103 占位 PNG（37 图标集×前缀组合）；iconset 语义定论=材料属性（mTextureSetsItems.get(prefix.mIconIndexItem)，上游 PrefixItem.java:136-138）。

**继承遗留（进第 3 阶段池）**：
- 全量前缀注册（421×材料）+ creative tab 按前缀分组（现仅白名单 4 前缀单 tab，ADR-P2-3 有意收缩）
- PrefixRegistry 未 close（注册桥只裁 MaterialRegistry）
- MT.NULL.mHandleMaterial=null（两相化副作用，上游 AnyWoodPlastic；工具卡落地时注意）
- TECH tMake* 串冻结首代（内容代间不变，惰性）
- 服务端侧特例键回退模板名（@OnlyIn 隔离的已知取舍）
- GT6DatagenItems 与注册桥白名单两处判据（items() 主路径天然同步，扩前缀时留意）

## 第 3 阶段收官记录（2026-08-30，主会话 phase-closeout）

> 合入链：43fcb1a（BE 框架，rebase 后 5550c8a…fb759de）→ b49d3e2（Menu/Screen 基建）→ 0348638（全量前缀 + creative tab，内含审查归一 9e230cd 与产物重生成）→ b6b01eb（chest 示例机器，WAVE-2 串行），docs/gitignore 收官镜像 2a04936。WAVE-1 三卡并行（files_scope 零交叠 + ADR-P3-4 GT6Mod 冻结）。

**ADR-P3-8 验收线核验**：
1. ✅ 根 `clean check --no-build-cache` 221 测（root 188 + mdk 33）0 失败 0 FROM-CACHE，根 src 零 net.minecraft。
2. ✅ `:mdk:build` 绿；runServer 合并态 Done（2.903s）零 GT6 ERROR；runData 两跑 written 53785→0 逐字节可复现。
3. ✅ 打开链自动化：RCON `/gt6machine check` → createMenu 90 槽（54 内容 + 36 背包）、stillValid、openers 0→1→0（审查官合并态重放）；chest NBT round-trip 单测 6 项。交互级 GUI 目视留用户。
4. ✅ BE 持久化单测：mdk test sourceSet 33 项（BE 框架 17 + 前缀注册 10 + chest 6）。
5. ✅ GPG 全验（原提交 + rebase 重写 + 审查归一提交逐个 verify-commit）；FILES_SCOPE 零越界；GT6Mod/GTModBusListener diff 为空（每分支核验 + 主会话 0348638..b6b01eb 区间复核）。
6. ✅ 共享 BET+validBlocks 默认（chest 独立 BET 为 ADR-P3-1 授权例外）；capability 缝合不独立成卡；两处 SPEC 偏离（menu 自持注册走 RegisterEvent、注册宇宙 105 item-path）经独立证据裁决升格 ADR。

**阶段成果数字**：56,253 材料物品（105 上游物品路径前缀 × isGeneratingItem，first-wins 零 id 撞键，945 重复对归并，363 非物品前缀留痕跳过）；96 creative tab；56,285 模型 JSON + lang 2,336 键；2,785 占位 PNG（40 iconset）。BE 框架：TileEntityBase01Root/03TicksAndSync 最小面移植 + onTick 八段分发逐字 + vanilla 双通道同步（getUpdateTag/ClientboundBlockEntityDataPacket）+ 共享 BET + capability 缝合 + MaterialStackNBT short 存档兼容。Menu/Screen 基建：GTGuiMenu/GTGuiScreen（bindPlayerInventory 直译）+ Slot 三件套能力模型 + gt6:debug /gt6gui + MenuScreens 唯一挂法。chest 示例机器：54 槽 + 动态行布局（ContainerCommonChest:39-43 逐字）+ gt6:chests tab + /gt6machine 打开链验证指令。

**重大修正（研究期）**：GT6 1.7.10 不把材料身份写进常规 ItemStack NBT——"a"/"i"/"m" 键仅坩埚/熔炼类 BE 内部 NBT（OreDictMaterialStack save/load，消费点 Crucible/Smeltery/Mold/DustFunnel）；常规材料物品身份=注册物品本身，BE 读槽位按 item 反查。

**遗留池（进第 4 阶段+）**：
- 特性层：trapped/comparator/涂装/TESR/lid 动画/getOpenGUIs 1200t 重同步、chest BlockItem loot table（掉落空，归 loot datagen 卡）
- ADR-P3-6 延后池：slotClick 全局拦截（Slot 能力模型重构）、rebootGUIs、cover 负 GUIID
- ContainerData 进度条业务面（第 4 阶段首台真加工机器落地）；第 4 阶段主线=管线/Cover/多方块渲染（BakedModel）
- 交互级验证：/give+tint（P2）与 GUI 目视（P3）留用户 runClient
- 旧池沿用：PrefixRegistry 未 close、MT.NULL.mHandleMaterial=null、TECH tMake 冻结首代、服务端特例键回退、移植进度看板

## 第 4 阶段收官记录（2026-08-30，主会话 phase-closeout）

> 合入链：02b7b8b（recipe-core）→ 5ae1d6f（fluid-pipes，W1 并行）→ 5b07953（machine-oven+f82a104 归一）∥ aa9679b（render-foundation，W2）→ 400732c（multiblock）→ 27e8cce（barrel）→ 87badb3（cover-core+2e89501 归一接线，W3 串行合入）→ 0d21a59（pipe-flow-control，用户追加需求第 8 卡）。

**ADR-P4 验收线核验**：
1. ✅ 根 `clean check` 188 测全绿 + 根 src 零 net.minecraft；mdk 199 测全绿（配方 30+管线 20+渲染 22+oven 18+多方块 20+桶 8+cover 28+流向 14+基建存量，分波累进 83→105→123→143→151→179→199）。
2. ✅ `:mdk:build` 绿；runServer 合并态 Done（3.408s）零 GT6 ERROR；runData 各卡二跑 written:0 幂等。
3. ✅ 服务端自动化验收=RCON 指令链全过：oven place→input→run 三态+8x stone；pipe place 贴靠放置→toggle 断开 inject REJECTED→output 外推恢复→clear 不外推→accept 均衡；multiblock frame→FORMED→拆→did-not-form→wand 自动放置→610tick 保持；cover install→hoe dismantle 掉落；barrel accept 16000L→melt 熔毁。交互级目视（oven GUI/cover 板/管道箭头）留用户 runClient。
4. ✅ NBT round-trip 单测扩面：FluidTankGT LAmount 溢出/CoverData 6 面/mTargetPos/ioMask/mProgress/mEnergy。
5. ✅ GPG 全验（含 rebase 重签与审查归一提交）；FILES_SCOPE 零越界；GT6Mod/GTModBusListener diff 为空；datagen W3 显式合并序 multiblock→barrel→cover 执行。

**域语义定论**：配方=findRecipe 只查+isRecipeInputEqual 两段式消耗+RecipeMapFurnace vanilla RecipeManager 桥（Recipe 全落 mdk，根 gregapi 零 MC import）；流体=逐段 BE 泵送无网络对象+FluidTankGT long 内部量/LAmount 溢出键+防回流一轮掩码+分相错峰；机器=进度=能量单位（mProgress+=min(mInputMax,mEnergy)）+A 常量满压假电源+C 红石独立闩锁+**mIgnited 是 post-action 复检窗口不可裁**；渲染=RENDER_SNAPSHOT 快照契约（禁 BE 引用）+GTDynamicBakedModel+scheduleRenderUpdate 双件套（**requestModelDataUpdate 单独不触发 chunk rebuild**）；多方块=代码即 pattern 逐字直译（否决 GTCEu DSL）+四路触发+checkAndSetTarget 两遍 wand 语义+FORMED property；cover=CoverData 6 面并行数组+零 GUI+CoverPlateModel epsilon 0.002；barrel=16000L 粘性罐+熔毁双支；管道（用户特性）=**绝不自动握手**（onPlaced 仅贴靠面 OPOS 连接）+右键逐面连接切换+shift 右键逐面输出箭头（ioMask XOR）+isOutputFace 外推门控。

**承重教训**：BaseEntityBlock 禁自创 onRemove 覆写（BE kill+recreate 循环，LevelChunk:292 CHECK 分支保 BE）；headless 验收 spawn 外 chunk 已加载不实体 tick，须 `/forceload add 0 0` 前置；FluidType 温度/密度离线不可查表（RegistryObject NPE），判定逻辑留原始值比较缝。

**遗留移交（进第 5 阶段池，state key=p5-pool）**：
- 管道语义修正（用户已确认）：ioMask==0 时恢复 GT6 原版全外推；箭头面拒绝回流（canAcceptFluidsFrom=connected&&!isOutputFace）
- 桶重力侧规则（注入六面全开，抽液=底面重流体/顶面轻流体，FL.lighter=density<0→FluidType.getDensity）+泵盖 CoverPump 正统移植
- GTCEu 式扳手交互 UI（9 宫格红绿图标覆盖层）+箭头等贴图从 GTCEu 资产直接借用
- RCON 验收工具链合入主项目 tools/rcon/（现各 agent /tmp 一次性脚本五份重复）
- 旧池沿用：CokeOven 加工业务（RM.CokeOven 配方考古）、机器族、C/D 档渲染升级、D 完整能量网、cover intercept 族、FluidTankGT keepFilter 0 量持久化缺口（归 Logistics 罐卡）、chest loot table、ADR-P3-6 池、PrefixRegistry 未 close、移植进度看板
