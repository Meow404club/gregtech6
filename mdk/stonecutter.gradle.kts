// mdk = Stonecutter 控制器项目：settings 阶段 create(project(":mdk")) 已把本项目的
// buildFileName 覆写为 stonecutter.gradle.kts（TreeBuilderImpl.createWith），
// 原 mdk/build.gradle 的内容按 loader 拆入 build.forge.gradle.kts / build.neoforge.gradle.kts。
// 控制器自身不可 build（StonecutterControllerImpl.configureProject 对 java 插件告警）。
// chisel 共享源 = 控制器目录下的 src/（StonecutterBuildImpl.createProcessingTasks：
// parent.fileTree("src/<sourceSet>")）——即 mdk/src 原位共享，mdk 源零移动挂双节点。
//
// 双节点任务矩阵（根目录执行；另见根 gradle.properties 版本矩阵段）：
//   :mdk:1.20.1-forge:test            1.20.1 永续红线（968 绿，--no-build-cache 实测）
//   :mdk:1.21.1-neoforge:compileJava  1.21.1 门禁遥测（红但走通到编译诊断，ADR-P15-10 r1）
//   :mdk:1.21.1-neoforge:compileTestJava  1.21.1 测试面清单（v1-test，配置面绿/源错误允许）
//   :mdk:1.20.1-forge:stonecutterPrepare  1.20.1 侧预处理输出（swap 零变化断言的比对面；
//   任务实名无 Main 后缀——W4 r1 勘正，SCPrepareTask 注册名 stonecutterPrepare）

plugins {
    id("dev.kikugie.stonecutter")
    // apply false 只进类路径：节点 buildscript（buildFileName = ../../build.<loader>.gradle.kts，
    // 落在 mdk/ 下）可无版本 apply。legacyforge 与 moddev 同版本成对发布；
    // 2.0.144 = 本仓现役 legacyforge 值（mdk/build.gradle:3，Gradle 8.14 本地实证）。
    id("net.neoforged.moddev") version "2.0.144" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.144" apply false
}

// 活动节点 = 共享源的在盘编译者（configureSource 对 active 节点直接 srcDir mdk/src 原位编译）。
stonecutter active "1.20.1-forge"

// chisel 常量：按节点名后缀给 forge/neoforge 常量（注释分叉用 `//? if forge {` / `//? if neoforge {`）。
// 证据：tmp/harvest/stonecutter-template/stonecutter.gradle.kts.txt:12-15 同构
// （ConstantContainer.match：逐候选 put(name, name == sample)，Containers.kt:13）。
// ---- 0.7 swap 表初版（W2 合入段；M1 裁决 decisions.2026-09-03-p15-m1-gate）----
// 消化密度 POC 机械符号带（tmp.poc.p15-chisel-density：28 文件/187 行 = ForgeRegistries/
// 内联全限定名/DeferredRegister/RegisterEvent）+ tier1 import 带（纯包名 shift）。
// 结构级符号不入表（禁夹带语义重构，ADR-P15-3 r1）：ForgeCapabilities→Capabilities、
// LazyOptional（类删除）、RegistryObject→DeferredHolder（泛型参数位变化）、ItemStack NBT 面、
// NetworkHooks/SimpleChannel、FluidType/ForgeMod/FMLJavaModLoadingContext/IDynamicBakedModel
// （删除/重构面）——留给 W3 seam / W4 语义波。
//
// 方向语义（0.7 ContainersImpl.ReplacementContainerImpl）：direction=true（neoforge 节点）
// 执行 from→to；false（forge 节点）执行 to→from 的对称反向。1.20.1 侧零变化证明分两级：
//   · string 条目：反向检索 neoforge 形——本仓 forge 源 `net\.neo` 出现 0 次（2026-09-03 grep
//     实证）→ 反向天然 no-op；
//   · regex 条目：ForgeRegistries 族 to 形是 forge 源既有 token 的子串（"ForgeRegistries.FLUIDS"
//     ⊃ "Registries.FLUID"；且本仓 17 处 vanilla `import ...Registries;`）——反向必撞，
//     reverse() 钉永不匹配哨兵（\u0000 哨兵不可能出现在源文件）。
// 执行序：string 先于 regex（ReplacementExecutor.replace 先 replaceString 后 replaceRegex）；
// 同表内插入序即执行序，特定（长键）条目先于一般条目。
// 锚定纪律：regex 一律字面量转义 + (?![A-Za-z_]) 边界断言，禁裸宽正则。
stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "forge", "neoforge")

    val neoforgeSide = node.metadata.project.endsWith("neoforge")

    // -- string 表：纯包名 shift / 全限定名锚定改名（反向在 forge 源零命中 → no-op）--
    // 特定条目在前（存在子串包含关系时保证长键先执行）。
    listOf(
        // 常见 data 类：三类 21.1 真值同在 common.data（p15-adapt-datagen 勘正：W2 初版的
        // client.data/data.language 目标在 neoforge 21.1.249 不存在——javap universal jar
        // 实证 net/neoforged/neoforge/common/data/{ExistingFileHelper,SpriteSourceProvider,
        // LanguageProvider}.class 三件齐，compileJava 报"程序包不存在"复现；1.21.2+ 才迁出）。
        "net.minecraftforge.common.data.ExistingFileHelper" to "net.neoforged.neoforge.common.data.ExistingFileHelper",
        "net.minecraftforge.common.data.SpriteSourceProvider" to "net.neoforged.neoforge.common.data.SpriteSourceProvider",
        "net.minecraftforge.common.data.LanguageProvider" to "net.neoforged.neoforge.common.data.LanguageProvider",
        // 菜单扩展接口改名（IForgeMenuType → IMenuTypeExtension；import 与代码体全限定名两用）
        "net.minecraftforge.common.extensions.IForgeMenuType" to "net.neoforged.neoforge.common.extensions.IMenuTypeExtension",
        "IForgeMenuType.create" to "IMenuTypeExtension.create",
        // fml 命名空间整体移位（@Mod / EventBusSubscriber / lifecycle 事件同名；javafmlmod.* 目标不存在 → 相关文件留红 W4）
        "net.minecraftforge.fml." to "net.neoforged.fml.",
        // eventbus 独立库（net.neoforged.bus；"event." 前缀不误伤 "eventbus"——段界安全）
        "net.minecraftforge.eventbus.api." to "net.neoforged.bus.api.",
        "net.minecraftforge.api.distmarker." to "net.neoforged.api.distmarker.",
        "net.minecraftforge.event." to "net.neoforged.neoforge.event.",
        "net.minecraftforge.client.event." to "net.neoforged.neoforge.client.event.",
        "net.minecraftforge.client.model." to "net.neoforged.neoforge.client.model.",
        "net.minecraftforge.client.extensions.common." to "net.neoforged.neoforge.client.extensions.common.",
        "net.minecraftforge.client.ChunkRenderTypeSet" to "net.neoforged.neoforge.client.ChunkRenderTypeSet",
        // fluids 子树整体（含 fluids.capability；FluidType/FluidActionResult 目标不存在 → 留红 W4）
        "net.minecraftforge.fluids." to "net.neoforged.neoforge.fluids.",
        "net.minecraftforge.items." to "net.neoforged.neoforge.items.",
        "net.minecraftforge.common.capabilities." to "net.neoforged.neoforge.common.capabilities.",
        "net.minecraftforge.common.util." to "net.neoforged.neoforge.common.util.",
        // W5 client 批（p15-adapt-client）：ToolActions 21.1 删除 → ItemAbilities 改名替代
        // （javap universal 21.1.249：net/neoforged/neoforge/common/ItemAbilities.class 在、
        // ToolActions 无；HOE_DIG 常量同名同义，canPerformAction(ItemAbility) 语义同）。
        // 必须先于下一条 ToolAction（单数）条目——ToolActions 含子串 ToolAction，长键先序，
        // 否则复数条目永远找不到 forge 形文本（首轮实测：import 行只换了包留下 ToolActions 类名）。
        // M4 勘正（p15-m4-final-clear）：单数 ToolAction 在 21.1 同样不存在（javap 实证
        // net/neoforged/neoforge/common/ToolAction.class 缺席），自研动作走 ItemAbility.get(name)——
        // 目标勘正为 ItemAbility（W5 原目标 net...common.ToolAction 在 21.1 不存在，GT6ToolActions
        // 首行 import 红）。简单名 ToolAction→ItemAbility 落 regex 表（下段）。
        "net.minecraftforge.common.ToolActions" to "net.neoforged.neoforge.common.ItemAbilities",
        "net.minecraftforge.common.ToolAction" to "net.neoforged.neoforge.common.ItemAbility",
        "net.minecraftforge.data.event." to "net.neoforged.neoforge.data.event.",
        // 注册类同名换包（DeferredRegister/RegisterEvent；RegistryObject/ITagManager 语义面 W4）
        "net.minecraftforge.registries.DeferredRegister" to "net.neoforged.neoforge.registries.DeferredRegister",
        "net.minecraftforge.registries.RegisterEvent" to "net.neoforged.neoforge.registries.RegisterEvent",
        "net.minecraftforge.registries.tags." to "net.neoforged.neoforge.registries.tags.",
    ).forEach { (from, to) ->
        replacements.string(neoforgeSide) { replace(from, to) }
    }

    // -- regex 表：ForgeRegistries 键名族 + ResourceLocation 构造器 + EventBusSubscriber --
    // 反向哨兵使 forge 侧预处理零变化（子串碰撞：Registries.X ⊂ ForgeRegistries.X*）。
    // DeferredRegister.create(Registries.X, modid) 走 (ResourceKey, modid) 重载——本仓既有
    // Registries.ITEM/MENU/CREATIVE_MODE_TAB 同型用法（编译实证），1.20.1/1.21.1 双侧同重载。
    // W4 r1 勘正+消化（p15-adapt-registry-core）：查询面成员 .getValue→.get / .getKey 的
    // 旧目标 Registries.X 是错的——Registries.X 是 ResourceKey（非 Registry），其上没有
    // .get/.getKey（W3 capability-core 的 swap 残余实测）；正解 BuiltInRegistries.X =
    // vanilla Registry，1.20.1/1.21.1 双侧同名同字段（双侧 recompile jar javap 实证），
    // .get(ResourceLocation)→T、.getKey(T)→ResourceLocation 与 ForgeRegistries 查询面同型。
    // ResourceLocation 两参构造器 21.1 删除（private 化），Forge 1.20.1 backport 了同名
    // 工厂 fromNamespaceAndPath——双侧 recompile jar javap 实证；保守正则见条目注。
    val neverMatch = "\u0000stonecutter.never.matched\u0000"
    listOf(
        // import 行先行（其余条目命中代码体；import 无键名后缀，需独立锚定整行）
        "import net\\.minecraftforge\\.registries\\.ForgeRegistries;" to "import net.minecraft.core.registries.Registries;",
        // 代码体全限定形态（net.minecraftforge.registries.ForgeRegistries.X，run1 第 2 轮补）：
        // 必须先于简单名条目——否则简单名条目把尾段换成 Registries.X 后残留未换包前缀
        // （run1 实证产生 13 处 "net.minecraftforge.registries.Registries" 缝合错误）。
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.FLUIDS\\.getValue(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.FLUID.get",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.ITEMS\\.getValue(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.ITEM.get",
        // FQ ITEMS.getKey（W5 命令面补）：FQ FLUIDS.getKey 早有同名条目而 ITEMS 漏配——
        // FQ 裸 ITEMS 条目先执行吃掉前缀，留下 Registries.ITEM.getKey（ResourceKey 无
        // getKey，GTMultiBlockCommand:348/415 实测"找不到符号 getKey(Item)"）。补齐同型。
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.ITEMS\\.getKey(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.FLUIDS\\.getKey(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.BLOCK_ENTITY_TYPES(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.FLUIDS(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.FLUID",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.BLOCKS(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.BLOCK",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.ITEMS(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.ITEM",
        // 特定 .getValue → .get / .getKey（Registry 查询面；先于一般键名条目；目标一律 FQ
        // BuiltInRegistries——不依赖源文件既有 import）
        "ForgeRegistries\\.FLUIDS\\.getValue(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.FLUID.get",
        "ForgeRegistries\\.ITEMS\\.getValue(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.ITEM.get",
        "ForgeRegistries\\.FLUIDS\\.getKey(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey",
        "ForgeRegistries\\.ITEMS\\.getKey(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey",
        "ForgeRegistries\\.BLOCKS\\.getKey(?![A-Za-z_])" to "net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey",
        // 键名单复数漂移（边界断言防 BLOCKS 吃 BLOCK_ENTITY_TYPES、FLUIDS 吃 FLUID_TYPES 等前缀误伤）
        "ForgeRegistries\\.BLOCK_ENTITY_TYPES(?![A-Za-z_])" to "Registries.BLOCK_ENTITY_TYPE",
        "ForgeRegistries\\.FLUIDS(?![A-Za-z_])" to "Registries.FLUID",
        "ForgeRegistries\\.BLOCKS(?![A-Za-z_])" to "Registries.BLOCK",
        "ForgeRegistries\\.ITEMS(?![A-Za-z_])" to "Registries.ITEM",
        // ResourceLocation 两参构造器 → fromNamespaceAndPath（W4 r1 消化；census：简单名 44 处 +
        // FQ 5 处可消化）。保守正则：实参位禁逗号与括号——嵌套调用/带括号表达式实参一律不命中
        // （实测 3 处漏网：GT6Covers:221/GTOvenOverlayModel:212/GTWireBakedModel:233，留语义波
        // //?），组引用不会缝合进任何带括号的实参。Kotlin \$1 转义组引用。FQ 形态先行。
        "new\\s+net\\.minecraft\\.resources\\.ResourceLocation\\(([^(),]+),\\s*([^(),]+)\\)" to "net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(\$1, \$2)",
        "new\\s+ResourceLocation\\(([^(),]+),\\s*([^(),]+)\\)" to "ResourceLocation.fromNamespaceAndPath(\$1, \$2)",
        // @Mod.EventBusSubscriber → 独立注解 @EventBusSubscriber（run2 第 3 轮补；21.1 rework：
        // 注解移 net.neoforged.fml.common.EventBusSubscriber，bus 属性删除——总线按事件类型自动判定，
        // docs.neoforged.net/docs/1.21.1/concepts/events/）。目标用全限定注解（免 import 行管理，W4 可润色）。
        // 顺序：位置参数形态必须先于注解头形态，否则头替换后残留 Bus.MOD 位置参数。
        "@Mod\\.EventBusSubscriber\\(Bus\\.MOD\\)" to "@net.neoforged.fml.common.EventBusSubscriber()",
        // 注解头（@ 锚定 + 转义点分；@Mod.EventBusSubscriber(Dist.CLIENT) 位置 value 形态同批消化）
        "@Mod\\.EventBusSubscriber\\(" to "@net.neoforged.fml.common.EventBusSubscriber(",
        // 尾随 bus 子句删除（census 勘正 W4 r1：实测 54 注解位 / 34 尾随 bus=；run2 原记
        // "51 注解位 22 MOD + 3 FORGE" 口径有误，勘正见 state tmp.w4.registry-core）
        ", bus = Mod\\.EventBusSubscriber\\.Bus\\.(MOD|FORGE)\\)" to ")",
        // FQ 注解形态（census 实测 2 文件：GT6Covers:68/GTMaterialBlocks:59——
        // @net.minecraftforge.fml.common.Mod.EventBusSubscriber 全限定写法；fml 包移位 string
        // 条目先行换掉 net.minecraftforge.fml.common.Mod 后，这里锚定 neoforge 形收尾。顺序：
        // 尾随 bus 子句先删（FQ 形），再换注解头——与简单名条目同序原则。
        ", bus = net\\.neoforged\\.fml\\.common\\.Mod\\.EventBusSubscriber\\.Bus\\.(MOD|FORGE)\\)" to ")",
        "@net\\.neoforged\\.fml\\.common\\.Mod\\.EventBusSubscriber\\(" to "@net.neoforged.fml.common.EventBusSubscriber(",
        // ---- W4 registry-blocks 批（p15-adapt-registry-blocks）：RegistryObject→DeferredHolder
        // 换装 + GTFluids 流体模板面。全部 regex+reverse 哨兵（单侧安全）——这些换装若走 string
        // 条目，forge 侧反向会撞 //? 注释腿里的 to 形文本（GTMaterialItems:21/GTMenuTypes:20
        // 已有 DeferredHolder import 注释腿；GT6DataComponents 惰性先例），regex 哨兵使 forge
        // 侧预处理绝对零变化。证据一律 javap（universal 21.1.249 / recompile 21.1 jar）：
        // · DeferredRegister.register = <I extends T> DeferredHolder<T, I>——supplier 具体型经
        //   目标类型/lambda 推断保留；RegistryObject<T> 机械映射到 DeferredHolder<DR元素宽型, T>
        //   （泛型参数位变化 = W2 不入表的原因，现按 DR 元素型逐条列目）；
        // · DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T>；getId()/get() 与
        //   RegistryObject 同义（GT6CapabilityWiringSeamTest 的 getId() pin 不变）；
        // · FluidType 21.1 在 net.neoforged.neoforge.fluids 存在（fluids 子树 string 移位已覆盖
        //   其 import）；ForgeFlowingFluid 21.1 改名 BaseFlowingFluid（Properties 三 supplier
        //   构造同型）；IClientFluidTypeExtensions 在 client.extensions.common（移位已覆盖）；
        // · LiquidBlock 21.1 vanilla 只有 (FlowingFluid, Properties) 构造（1.20.1 Forge 的
        //   Supplier 重载不在）；vanilla 注册序 FLUID 先于 BLOCK（Registries.java 挂载序），
        //   Block 注册事件内 .get() 已解析。
        // 条目序：FQ 换包先行（防后半段换型留旧包前缀的缝合错误，W2 run1 教训），嵌套 BET
        // 先于简单名，具体元素型先于块类兜底（兜底 [A-Za-z0-9]+ 两位起，不吃泛型字母 T）。
        // FQ 参数化形态必须先于下方 FQ 类名换包条目：类名先换则泛型实参只剩 1 个
        // （DeferredHolder 需要 2 个，GTMultiBlockCommand:393/402 "类型变量数目错误" 实测；
        // W4 lesson id258 同源——to 串组引用条目与换包条目的执行序洞）。
        "net\\.minecraftforge\\.registries\\.RegistryObject<net\\.minecraft\\.world\\.item\\.Item(?![A-Za-z_])>" to "net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item>",
        "net\\.minecraftforge\\.registries\\.RegistryObject(?![A-Za-z_])" to "net.neoforged.neoforge.registries.DeferredHolder",
        "RegistryObject<BlockEntityType<([^<>()]+)>>" to "DeferredHolder<BlockEntityType<?>, BlockEntityType<$1>>",
        "RegistryObject<net\\.minecraft\\.world\\.item\\.Item(?![A-Za-z_])>" to "DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item>",
        "RegistryObject<net\\.minecraft\\.world\\.level\\.block\\.Block(?![A-Za-z_])>" to "DeferredHolder<Block, net.minecraft.world.level.block.Block>",
        "RegistryObject<FlowingFluid(?![A-Za-z_])>" to "DeferredHolder<Fluid, FlowingFluid>",
        "RegistryObject<FluidType(?![A-Za-z_])>" to "DeferredHolder<FluidType, FluidType>",
        "RegistryObject<LiquidBlock(?![A-Za-z_])>" to "DeferredHolder<net.minecraft.world.level.block.Block, LiquidBlock>",
        // M4 勘正：通配条目已删——to 串 "DeferredHolder<Fluid, \$1>" 吞掉 "\? extends " 字面段
        //（\$1 只捕元素型，wildcard 丢失 → FluidBridge 的 Map 值型被改写成 DeferredHolder<Fluid,Fluid>，
        // 与 FlowingFluid 实参不兼容）；FluidBridge 3 位点已改 //? 双腿（id258 同源教训：
        // to 串组引用会吞字面段，入表前必须核产物全文）。
        "RegistryObject<Fluid(?![A-Za-z_])>" to "DeferredHolder<Fluid, Fluid>",
        "RegistryObject<Item(?![A-Za-z_])>" to "DeferredHolder<Item, Item>",
        "RegistryObject<CreativeModeTab(?![A-Za-z_])>" to "DeferredHolder<CreativeModeTab, CreativeModeTab>",
        "RegistryObject<([A-Z][A-Za-z0-9]+)>" to "DeferredHolder<Block, \$1>",
        "RegistryObject::get" to "DeferredHolder::get",
        // GTFluids：FLUID_TYPES 注册键与查询面（NeoForgeRegistries.FLUID_TYPES 是 Registry 本尊
        // 非 Supplier，.get() 链一并消化）；ForgeMod 水类型常量 21.1 = NeoForgeMod 同名
        // Holder<FluidType>（.value()）。三条目加左边界 (?<![A-Za-z_])：目标 NeoForgeRegistries
        // 含子串 "ForgeRegistries"——无左断言会自撞（首轮实测：Neo + FQ 双前缀缝合）。
        // ForgeMod 条目目标不带 ()：源 .get 后随的 () 残留即成 .value()（首轮 value()() 实测）。
        "(?<![A-Za-z_])ForgeRegistries\\.FLUID_TYPES\\.get\\(\\)\\.getKey(?![A-Za-z_])" to "net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES.getKey",
        "(?<![A-Za-z_])ForgeRegistries\\.Keys\\.FLUID_TYPES(?![A-Za-z_])" to "net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES",
        "(?<![A-Za-z_])ForgeRegistries\\.FLUID_TYPES(?![A-Za-z_])" to "net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES",
        "import net\\.minecraftforge\\.common\\.ForgeMod;" to "import net.neoforged.neoforge.common.NeoForgeMod;",
        "ForgeMod\\.(WATER_TYPE|LAVA_TYPE)\\.get(?![A-Za-z_])" to "NeoForgeMod.\$1.value",
        "ForgeFlowingFluid(?![A-Za-z_])" to "BaseFlowingFluid",
        "new\\s+LiquidBlock\\(([A-Z][A-Z_0-9]*)," to "new LiquidBlock(\$1.get(),",
        // ---- W5 client 批（p15-adapt-client）：VertexConsumer 1.21 改名 + bakeQuad 八参化 +
        // ToolActions 简单名。javap compiledWithNeoForge 21.1 jar 实证：
        // · VertexConsumer：vertex→addVertex、color→setColor、normal→setNormal、uv→setUv、
        //   uv2→setUv2（签名同型），endVertex 删除（1.21 即写 builder）。全仓 census：
        //   五符号仅 GTMultiBlockPreviewRenderer/GTWrenchGridRenderer 代码体命中（9 链），
        //   测试 stub 的 `public void endVertex()` 无点前缀天然不命中；
        // · normal 必须长锚 `([A-Za-z0-9]+)\.normal\(\),`——裸 \.normal\( 会吃掉
        //   PoseStack.Pose.normal()（取法线矩阵，1.21 同名存在，绝不可换）；
        // · FaceBakery.bakeQuad 21.1 八参（删 1.20.1 尾参 ResourceLocation name），
        //   4 处调用尾参两形态精确锚定（GTWireBakedModel:256 aSprite.contents().name() /
        //   CoverPlateModel:208·GTOvenOverlayModel:247·GTFluidPipeFlowModel:152 aPlan.sprite()；
        //   census：`, null, true, ` 其余命中全是 setCoverItem/assertTrue 实参，尾型不匹配）；
        // · ToolActions 简单名换 ItemAbilities：左边界 (?<![A-Za-z0-9_]) 挡自研
        //   GT6ToolActions（GTCutterItem/CoverControllerCoversTest 域）子串自撞。
        "\\.vertex\\(" to ".addVertex(",
        "\\.color\\(" to ".setColor(",
        // normal 长锚 + 实参换型：1.20.1 Forge 的 normal(Matrix3f, x, y, z) 重载（实参
        // Pose.normal() 取法线矩阵）21.1 删除，setNormal(Pose, x, y, z) 直接收 Pose 本体
        // （javap IBakedModelExtension 同 jar：VertexConsumer default setNormal(Pose,float,
        // float,float)）——捕获 Pose 变量名，剥掉 .normal() 取矩阵链。裸 \.normal\( 会误伤
        // PoseStack.Pose.normal() 调用本身（1.21 同名存在，绝不可换）。
        "\\.normal\\(([A-Za-z0-9]+)\\.normal\\(\\)," to ".setNormal(\$1,",
        "\\.uv\\(" to ".setUv(",
        // 0.7 拒收空串替换（"Replacing with an empty string is not reversible"）——
        // endVertex 删除落成单空格（链尾 `;` 前残留空格对 javac 无意义）
        "\\.endVertex\\(\\)" to " ",
        // uv2 单参打包形 → setLight：1.20.1 Forge uv2(int packed) 便利重载 21.1 删除，
        // vanilla default setLight(int) 实现就是拆包喂 setUv2（1.21.1 decompile 实证：
        // setLight(p) => setUv2(p & 65535, p >> 16 & 65535)，语义逐字节同构）。
        "\\.uv2\\(([^(),]+)\\)" to ".setLight(\$1)",
        ", null, true, [a-zA-Z]+\\.sprite\\(\\)\\);" to ", null, true);",
        ", null, true, [a-zA-Z]+\\.contents\\(\\)\\.name\\(\\)\\);" to ", null, true);",
        "(?<![A-Za-z0-9_])ToolActions\\." to "ItemAbilities.",
        // ToolAction 简单名（M4 批）：自研动作常量 GT6ToolActions.CROWBAR/CUTTER 走
        // ItemAbility.get(name)（javap ItemAbility：get(String) 工厂在、ToolAction 类 21.1 删除）。
        // 左边界挡 GT6ToolActions/右边界 ?![A-Za-z0-9_] 防吃 ToolActions 前缀（复数条目先行）。
        "(?<![A-Za-z0-9_])ToolAction(?![A-Za-z0-9_])" to "ItemAbility",
        // ---- M4 批（p15-m4-final-clear）----
        // ItemStack.isSameItemSameTags → isSameItemSameComponents（1.20.5 DataComponents 改名，
        // javap ItemStack 21.1.249；10 位点普查全同形，机械改名入表）。
        "(?<![A-Za-z0-9_])ItemStack\\.isSameItemSameTags\\(" to "ItemStack.isSameItemSameComponents(",
        // hurtAndBreak 的 Consumer lambda → EquipmentSlot 实参（21.1 (int,LivingEntity,Consumer)
        // 重载删除，(int,LivingEntity,EquipmentSlot) 内部播 broadcastBreakEvent；javap）。
        // 锚定 lambda 体自引用形式 p -> p.broadcastBreakEvent(MAINHAND)（GTCutterItem:186/
        // GTCrowbarItem:197,205 三位点；GTCokeOvenBlock:68 条件形不入表走 //?）。
        "([a-zA-Z][a-zA-Z0-9]*) -> \\1\\.broadcastBreakEvent\\(EquipmentSlot\\.MAINHAND\\)" to "EquipmentSlot.MAINHAND",
    ).forEach { (pattern, to) ->
        replacements.regex(neoforgeSide) {
            replace(pattern, to)
            reverse(neverMatch, "stonecutter.never")
        }
    }
}
