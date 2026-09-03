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
//   :mdk:1.20.1-forge:stonecutterPrepareMain  1.20.1 侧预处理输出（swap 零变化断言的比对面）

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
        "net.minecraftforge.common.ToolAction" to "net.neoforged.neoforge.common.ToolAction",
        "net.minecraftforge.data.event." to "net.neoforged.neoforge.data.event.",
        // 注册类同名换包（DeferredRegister/RegisterEvent；RegistryObject/ITagManager 语义面 W4）
        "net.minecraftforge.registries.DeferredRegister" to "net.neoforged.neoforge.registries.DeferredRegister",
        "net.minecraftforge.registries.RegisterEvent" to "net.neoforged.neoforge.registries.RegisterEvent",
        "net.minecraftforge.registries.tags." to "net.neoforged.neoforge.registries.tags.",
    ).forEach { (from, to) ->
        replacements.string(neoforgeSide) { replace(from, to) }
    }

    // -- regex 表：ForgeRegistries → vanilla Registries（键名单复数漂移 + .getValue→.get API 漂移）--
    // 反向哨兵使 forge 侧预处理零变化（子串碰撞：Registries.X ⊂ ForgeRegistries.X*）。
    // DeferredRegister.create(Registries.X, modid) 走 (ResourceKey, modid) 重载——本仓既有
    // Registries.ITEM/MENU/CREATIVE_MODE_TAB 同型用法（编译实证），1.20.1/1.21.1 双侧同重载。
    // .getKey 方法名双侧同名（返回 ResourceKey vs ResourceLocation 的类型漂移是 W4 语义面，
    // 相关文件本就在红清单内，此处只做机械键名换）。
    val neverMatch = "\u0000stonecutter.never.matched\u0000"
    listOf(
        // import 行先行（其余条目命中代码体；import 无键名后缀，需独立锚定整行）
        "import net\\.minecraftforge\\.registries\\.ForgeRegistries;" to "import net.minecraft.core.registries.Registries;",
        // 代码体全限定形态（net.minecraftforge.registries.ForgeRegistries.X，run1 第 2 轮补）：
        // 必须先于简单名条目——否则简单名条目把尾段换成 Registries.X 后残留未换包前缀
        // （run1 实证产生 13 处 "net.minecraftforge.registries.Registries" 缝合错误）。
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.FLUIDS\\.getValue(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.FLUID.get",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.ITEMS\\.getValue(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.ITEM.get",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.BLOCK_ENTITY_TYPES(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.FLUIDS(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.FLUID",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.BLOCKS(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.BLOCK",
        "net\\.minecraftforge\\.registries\\.ForgeRegistries\\.ITEMS(?![A-Za-z_])" to "net.minecraft.core.registries.Registries.ITEM",
        // 特定 .getValue → .get（Registry API 名漂移；先于一般键名条目）
        "ForgeRegistries\\.FLUIDS\\.getValue(?![A-Za-z_])" to "Registries.FLUID.get",
        "ForgeRegistries\\.ITEMS\\.getValue(?![A-Za-z_])" to "Registries.ITEM.get",
        // 键名单复数漂移（边界断言防 BLOCKS 吃 BLOCK_ENTITY_TYPES、FLUIDS 吃 FLUID_TYPES 等前缀误伤）
        "ForgeRegistries\\.BLOCK_ENTITY_TYPES(?![A-Za-z_])" to "Registries.BLOCK_ENTITY_TYPE",
        "ForgeRegistries\\.FLUIDS(?![A-Za-z_])" to "Registries.FLUID",
        "ForgeRegistries\\.BLOCKS(?![A-Za-z_])" to "Registries.BLOCK",
        "ForgeRegistries\\.ITEMS(?![A-Za-z_])" to "Registries.ITEM",
        // @Mod.EventBusSubscriber → 独立注解 @EventBusSubscriber（run2 第 3 轮补；21.1 rework：
        // 注解移 net.neoforged.fml.common.EventBusSubscriber，bus 属性删除——总线按事件类型自动判定，
        // docs.neoforged.net/docs/1.21.1/concepts/events/）。目标用全限定注解（免 import 行管理，W4 可润色）。
        // 顺序：位置参数形态必须先于注解头形态，否则头替换后残留 Bus.MOD 位置参数。
        "@Mod\\.EventBusSubscriber\\(Bus\\.MOD\\)" to "@net.neoforged.fml.common.EventBusSubscriber()",
        // 注解头（@ 锚定 + 转义点分；@Mod.EventBusSubscriber(Dist.CLIENT) 位置 value 形态同批消化）
        "@Mod\\.EventBusSubscriber\\(" to "@net.neoforged.fml.common.EventBusSubscriber(",
        // 尾随 bus 子句删除（census：bus 恒为末位属性，51 注解位 22 MOD + 3 FORGE）
        ", bus = Mod\\.EventBusSubscriber\\.Bus\\.(MOD|FORGE)\\)" to ")",
    ).forEach { (pattern, to) ->
        replacements.regex(neoforgeSide) {
            replace(pattern, to)
            reverse(neverMatch, "stonecutter.never")
        }
    }
}
