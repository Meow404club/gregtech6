// 1.21.1 NeoForge 节点构建脚本（由任意 neoforge 节点经 buildFileName ../../ 共享）。
// 1.21.1 必 moddev（LEGACY.md:2-13：legacyforge 插件上限 MinecraftForge 1.20.1）。
// 证据：tmp/harvest/stonecutter-template/build.neoforge.gradle.kts.txt:1-46（neoForge 块/runs/mods register）。
// W2 段补全（与 forge 节点同构）：runs client/server/data + test sourceSet 类路径接线 +
// generateModMetadata（META-INF/neoforge.mods.toml，NeoForge 1.20.5+ 元数据文件名）+ zip64 + 诊断协议 maxerrs。
// W5 段（task p15-jei-dual-wiring）：JEI 1.21.1 三件接线（见 dependencies JEI 注释段）；
// datagen 产物默认共享（下文 srcDir）。
import org.gradle.jvm.tasks.Jar
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    id("net.neoforged.moddev")
}

group = "gregtech6"
version = property("mod_version").toString()

val modId = property("mod_id").toString()
val mcVer = property("deps.minecraft").toString()
val jeiVer = property("jei_version").toString()
val jadeVer = property("jade_version").toString()

// 分发 jar 可辨识名（task p23-jar-naming）：archivesName = <mod_id>-<stonecutter 节点名>，
// jar 任务再追加 project.version（= mod_version）→ gt6-1.21.1-neoforge-0.1.0.jar。
// 旧默认 archivesName = project 名 = 节点目录名，产物 1.21.1-neoforge-0.1.0.jar 认不出是哪个 mod。
// Knob/节点名求证与 forge 节点同源（mdk/build.forge.gradle.kts archivesName 段）：
// BasePluginExtension.getArchivesName（javap gradle-api-8.14.jar，2026-09-07）+
// stonecutter 0.7 TreeBuilderImpl.kt:123-131（节点 project.name = "1.21.1-neoforge"）。
base {
    archivesName.set("${modId}-${project.name}")
}

// 共享锚点：控制器项目目录（mdk/），即节点共享资源/模板/datagen 产物的真实位置。
val sharedDir = parent!!.projectDir

// JEI（task p15-jei-dual-wiring）：blamejared maven，与 forge 节点同仓同源
// （mdk/build.forge.gradle.kts JEI 段同构；坐标来源证据见 dependencies JEI 注释段）。
repositories {
    maven {
        name = "blamejared"
        url = uri("https://maven.blamejared.com/")
    }
    // Jade（task p21-jade-compat）：Modrinth maven，jade 双腿唯一分发渠道。
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    // KubeJS/Rhino（task p34-kjs-bindings）：dev.latvian.mods 组唯一分发渠道
    // （GTCEu gradle/scripts/repositories.gradle:58-63 同源同组过滤；7.x bundle 无
    // architectury，故本节点无需 architectury maven——GTCEu 1.21 toml:131 同口径）。
    maven {
        name = "latvian"
        url = uri("https://maven.latvian.dev/releases")
    }
}

// 1.21.1 节点 = Java 21（本机 java-21-openjdk；ADR-P15-2：gregapi 钉 17，17 产物可被 21 工具链直接消费）
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// p33-ore-overlay-impl：-Pgt6.display=:97 覆盖 game 进程的 DISPLAY——runClient 由常驻
// daemon 执行，launcher 的 inline env 到不了 game（daemon 环境原样继承）；headless 验收机
// 必须钉 Xvfb，物理 :0 禁弹窗（build.forge.gradle.kts 同缝原样移植）。
val display = providers.gradleProperty("gt6.display")
if (display.isPresent) {
    tasks.withType(JavaExec::class).matching { it.name == "runClient" }.configureEach {
        environment("DISPLAY", display.get())
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    // 全量诊断协议（M3 门禁口径，decisions.2026-09-03-p15-m1-gate）：javac 默认 maxerrs=100
    // 会截断错误清单 → maxerrs/maxwarns 100000 全量可数。W1 机制实证五：巨型诊断经 Gradle
    // 消息枢纽回传会在收尾阶段 OOM（client launcher 仅 64m 堆），但诊断流完整落地可按行提取
    // （-Xdiags:compact 实测被 Gradle 编译管线忽略，输出仍 verbose，2026-09-04——不再挂载）。
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "100000", "-Xmaxwarns", "100000"))
}

neoForge {
    version = property("deps.neoforge").toString()

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    // 测试 JVM 基建（task p15-m4-test-infra）：unitTest 让 1.21.1 测试 JVM 走 FML 引导而非裸 JUnit，
    // 根治 FeatureFlags clinit 的 FeatureFlagLoader.loadModdedFlags→LoadingModList.get() null 级联
    // （1.21.1 test 394 实跑 169 红中的 158 clinit + 2 /0 次生，state tmp.m4.final-clear 独立分解）。
    // DSL 形态求证（moddev-gradle-2.0.144.jar javap，非文档记忆）：
    //   NeoForgeExtension.unitTest(Action<UnitTest>) / UnitTest.enable() → ModDevRunWorkflow.configureTesting
    //   → setupTestTask：Test task 挂 dependsOn(prepareNeoForgeTestFiles + writeNeoForgeTestClasspath)、
    //   workingDir=build/minecraft-junit、-Dfml.junit.argsfile + gradle mod folders jvmArgumentProvider；
    //   junit-fml（userdev config.json testLibraries，21.1.249 = fancymodloader:junit-fml:4.0.44）以
    //   JUnit Platform LauncherSessionListener 形态在测试 worker 内引导 FML。NeoForge 21.1.249
    //   userdev config.json runs.junit（BootstrapLauncher forgejunitdev）齐备——MDG 配置期校验该
    //   run type 存在（PrepareTest.resolveRunType 缺失即抛）。loadedMods 约定值 = mods 容器全集。
    unitTest {
        enable()
        // testedMod 必须显式指认（MDG 官方 testproject 同款）：RunUtils.buildModFolders 仅当
        // testedMod 存在时才把 test sourceSet 输出并入被测 mod 的 mod folder——否则 FML 的
        // ModuleClassLoader 看不到测试类，Gradle junitClassLoader（=FML loader）Class.forName
        // 直接 CNFE（2026-09-04 实测：4/394 起步即全红，Executor XML stack = ModuleClassLoader）。
        testedMod = mods.named(modId)
    }

    runs {
        // 节点本地 run 目录（避免 1.20.1/1.21.1 游戏库互相污染；模板 build.neoforge.gradle.kts.txt:28-35 同构）
        register("client") {
            client()
            gameDirectory = file("run/")
            // p33-ore-overlay-impl：quickplay/display 缝与 forge 节点同构（build.forge.gradle.kts
            // client run + runClient JavaExec DISPLAY 门原样移植）——-Pgt6.quickplay="<args>"
            // 追加程序参数（--quickPlaySingleplayer/宽高），-Pgt6.display 覆盖常驻 daemon
            // 里 game 进程的 DISPLAY；默认缺省零变化。
            val quickplay = providers.gradleProperty("gt6.quickplay")
            if (quickplay.isPresent) {
                programArguments.addAll(quickplay.get().split(" ").filter { it.isNotBlank() })
            }
        }
        register("server") {
            server()
            gameDirectory = file("run/")
            // 用户裁定（2026-09-04，随 ADR-P15-4 卡合并）：服务端启动一律 nogui——
            // DedicatedServer 控制台 GUI 不许弹出，headless 验收机的唯一正典形态。
            programArguments.addAll("--nogui")
        }
        // Data run 四参数与 forge 节点同构（GTCEu gradle/scripts/moddevgradle.gradle:111-122 先例，ADR-P2-4）。
        // --existing 指向共享 main/resources：占位贴图存在性校验（Forge 1.20.1 ModelBuilder 同机制）。
        // ADR-P17-1：输出目录按节点参数化。正典生产者唯一 = settings vcsVersion 节点
        // （settings.gradle.kts:35 = 1.20.1-forge，其 runData --output 独写共享 tracked 正典树
        // mdk/src/generated/resources，见 build.forge.gradle.kts data run）；本节点（1.21.1-neoforge）
        // 产物 = 验证产物非入库面，落节点本地 build/datagen-output（.gitignore:18 全局 build/ 规则
        // 已覆盖，零 gitignore 改动）。file() 在共享 buildscript 中解析到当前节点 projectDir
        // （同上 gameDirectory = file("run/") 先例）；第三节点出现时按 vcsVersion 判正典
        // （本卡不预建，YAGNI 声明）。机制因：共享 .cache 键无节点标识且 purgeStaleAndWrite
        // 删未认领文件，双节点共写一树结构性互删（vanilla HashCache.java:46-48/:117-133，ADR-P17-1 §1.2）。
        register("data") {
            data()
            sourceSet = sourceSets["main"]
            programArguments.addAll("--mod", modId)
            programArguments.addAll("--all")
            programArguments.addAll("--output", file("build/datagen-output").absolutePath)
            programArguments.addAll("--existing", sharedDir.resolve("src/main/resources").absolutePath)
        }
    }
}

dependencies {
    // 根项目 = gregapi java-library（Java17 产物）
    implementation(project(":"))
    // moddev run 类路径与 1.20.1 legacyforge 同坑同修（build.forge.gradle.kts:83-89 同源）：
    // 无 neoforge.mods.toml 的普通库不进 run——MDG README "External Dependencies: Runs"
    // 原文即本症状（ClassNotFoundException at run time），per-run classpath 只 extendsFrom
    // additionalRuntimeClasspath（ModDevRunWorkflow.java:100 create("additionalRuntimeClasspath")，
    // 源码 tmp/harvest/moddevgradle-src）。2026-09-04 :mdk:1.21.1-neoforge:runServer 首验实证：
    // FML 自动订阅扫描反射 GT6Mod 方法签名时 NoClassDefFoundError: gregapi/oredict/OreDictPrefix
    // → mod loading crash（/tmp/gt6_rs_p15boot1211.log:91）。
    "additionalRuntimeClasspath"(project(path = ":", configuration = "runtimeElements"))
    // P20④ ModularUI vendored fork jarJar 嵌装（ADR 2026-09-06-p20-modularui-fork-ruling §4，
    // GTCEu 量产姿势 dependencies.gradle:14 对齐：顶层只 jarJar(mui)，EvalEx 由 modularui 自己
    // 嵌装携带；mixinextras 由 NeoForge 加载器自带，vendored 腿不嵌）。MDG JarJarPlugin 把
    // jarJar 配置解析产物经 jarJar 任务写 META-INF/jarjar/ 并入 jar 任务产物（JarJarPlugin.java:22）。
    // 本节点无重映射步（NeoForge 1.20.5+ 生产即 official 命名），嵌装 jar 原样进入分发形态。
    // 依赖面说明：modularui 不进编译/运行类路径（纯嵌装分发；mdk 源码零引用，GUI 采纳另卡）。
    "jarJar"(project(":third-party:modularui:1.21.1-neoforge"))
    // task p24-act-machine C2 前置门：mdk 主树首消费 ModularUI（decisions.p24-act-be-form）。
    // NeoForge 1.20.5+ 发行即 official 映射、无 mod* 重映射配置（JEI 段同论证）——plain
    // compileOnly 即编译面；运行面类由上方 jarJar 嵌装产物承载。
    compileOnly(project(":third-party:modularui:1.21.1-neoforge"))
    // 运行面：runtimeOnly 并入 run 类路径（JEI impl 同论证）——dev run 的 mod 发现走
    // 类路径上的 modularui mod 本体，jarJar 嵌装仅在发布 jar 中存在，dev run 零双装。
    runtimeOnly(project(":third-party:modularui:1.21.1-neoforge"))
    // JEI 1.21.1（task p15-jei-dual-wiring；ADR 2026-09-02-p12-jei-dependency 的跨版本延续）：
    // 坐标三件 mezz.jei:jei-${mcVer}-{common-api,neoforge-api,neoforge}:19.52.0.422——
    // blamejared maven-metadata <latest>（2026-09-03）+ Modrinth "19.52.0.422 for NeoForge 1.21.1"
    // 双源一致；降级预案 19.51.0.418（Modrinth 最新 release 标记位）。
    // ${jeiVer} 解析自节点参数 mdk/versions/1.21.1-neoforge/gradle.properties 的同名键（节点级
    // gradle.properties 遮蔽根值；探针实证 2026-09-04：本节点 19.52.0.422 / 1.20.1-forge 仍
    // 15.56.0.205），故表达式与 forge 节点 property("jei_version") 完全同构。
    //
    // 与 1.20.1 节点（mdk/build.forge.gradle.kts:93-95 modCompileOnly/modRuntimeOnly）的关键差异：
    // mod* 重映射配置是 legacyforge 插件专有（LEGACY.md:68-92 "Remapping Mod Dependencies"——
    // SRG→official、同名标准配置的 child、非传递），本节点 moddev 插件（2.0.144）不注册它们
    // （活体探针实证 2026-09-04：:mdk:1.20.1-forge 配置面含 modApi/modCompileOnly/
    // modCompileOnlyApi/modImplementation/modRuntimeOnly，:mdk:1.21.1-neoforge 仅
    // modDevCompileDependencies/modDevRuntimeDependencies，mod* 全缺）。NeoForge 1.20.5+ 发行 mod
    // 本就以 official（mojmap）命名运行、无 SRG 中间映射步，无需重映射——两代 JEI API 面 javap
    // 实证逐方法全同（IModPlugin 20 方法/IRecipeRegistration/JeiPlugin 注解，15.56.0.205 vs
    // 19.52.0.422），GT6JeiPlugin 零分叉即双节点可编译（RL 两参 ctor 由 stonecutter swap 消化，
    // 生成腿 GT6JeiPlugin.java:49 实证 fromNamespaceAndPath）。
    // API 两件 compileOnly（编译面，不进 run）；impl 一件 runtimeOnly——impl jar 自带全部 api 类
    //（unzip 实证 jei-1.21.1-neoforge-19.52.0.422.jar 含 mezz/jei/api 198 类），经
    // sourceSets["test"].runtimeClasspath += main.runtimeClasspath（下文）进测试 JVM，与
    // 1.20.1 节点 GT6JeiPluginTest 的类加载路径同构。
    compileOnly("mezz.jei:jei-${mcVer}-common-api:${jeiVer}")
    compileOnly("mezz.jei:jei-${mcVer}-neoforge-api:${jeiVer}")
    runtimeOnly("mezz.jei:jei-${mcVer}-neoforge:${jeiVer}")
    // Jade（WAILA 后继，task p21-jade-compat）：与本节点 JEI 段同构——NeoForge 1.20.5+ 发行 mod
    // 本就以 official（mojmap）命名运行、无重映射步，compileOnly（编译面）+ runtimeOnly（run
    // 类路径，专用服务端冒烟依赖它）。版本钉值 mdk/versions/1.21.1-neoforge/gradle.properties
    // jade_version（15.10.6+neoforge，节点遮蔽根钉值，同 jei_version 先例）。
    compileOnly("maven.modrinth:jade:${jadeVer}")
    runtimeOnly("maven.modrinth:jade:${jadeVer}")
    // KubeJS 绑定依赖段（task p34-kjs-bindings）：compileOnly 运行时可选零传染——
    // 本节点无 mod* 重映射配置（NeoForge 1.20.5+ 发行即 official，JEI 段同论证），
    // plain compileOnly 即编译面；不 jarJar 不 modApi 不 runtimeOnly（GTCEu
    // dependencies.gradle:43-46 先例的可选性口径）。可选性机制 = kubejs.plugins.txt 资源根
    // 发现是 KubeJS 侧拉取（KubeJSPlugins.findResource，7.x 源 :38-41）：KubeJS 不在 =
    // 无人读 plugins.txt = kjs 类零类加载。
    // 版本钉值 = 节点 gradle.properties（kubejs_version/rhino_version，与 GTCEu 1.21
    // forge.versions.toml:8-9 同 build）。留空 kubejs_version = 本段整体跳过 + kjs 源从
    // 编译面消失：本节点编译 stonecutter 预处理产物，kjs 常量 false 即把 //? if kjs 包裹源
    // 清空（下方包排光是活动节点裸编译面的对应机制，本节点冗余但对称保留——活动节点归属
    // 只在 mdk/stonecutter.gradle.kts 一行，翻转时洞不重开）＝双态开关，验收①达成。
    val kubejsVer = property("kubejs_version").toString().trim()
    if (kubejsVer.isNotEmpty()) {
        compileOnly("dev.latvian.mods:kubejs-neoforge:${kubejsVer}")
        compileOnly("dev.latvian.mods:rhino:${property("rhino_version").toString().trim()}")
    }
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// kjs OFF 态包排光（与 build.forge.gradle.kts 同构对称；本节点主用途靠 stonecutter 常量，
// 见上依赖段注释——活动节点翻转时此处接管裸编译面）。
if (property("kubejs_version").toString().trim().isEmpty()) {
    sourceSets["main"].java.exclude("gregtech6/integration/kjs/**")
    sourceSets["test"].java.exclude("gregtech6/integration/kjs/**")
}

// test sourceSet 类路径接线（与 forge 节点 build.forge.gradle.kts 同构，双保险）：
// ① modDev* 游戏库配置并入 test 类路径——moddev 与 legacyforge 同名约定（主工程 probeConfigs 实测），
//    但配置注册时机两插件不同：afterEvaluate + findByName 兜底（缺失=静默跳过，由 ② 兜住）。
//    注意不能在 configureEach 里再开容器级配置（DefaultNamedDomainObjectSet 变异守卫，2026-09-03 实证）。
// ② main 的 compile/runtimeClasspath（含 ①的游戏库与 gregapi）追加进 test——测试类路径专属，
//    runs/jar 不受影响。1.21.1 compileTestJava 诊断级走通靠这两层（v1-test 清单见 state）。
afterEvaluate {
    configurations.findByName("modDevCompileDependencies")?.let {
        configurations.getByName("testCompileClasspath") { extendsFrom(it) }
    }
    configurations.findByName("modDevRuntimeDependencies")?.let {
        configurations.getByName("testRuntimeClasspath") { extendsFrom(it) }
    }
}
sourceSets["test"].compileClasspath += sourceSets["main"].output
sourceSets["test"].runtimeClasspath += sourceSets["main"].output
sourceSets["test"].compileClasspath += sourceSets["main"].compileClasspath
sourceSets["test"].runtimeClasspath += sourceSets["main"].runtimeClasspath

// neoforge.mods.toml 模板展开：NeoForge 1.20.5+ 元数据文件为 META-INF/neoforge.mods.toml
// （模板 src/main/resources/META-INF/neoforge.mods.toml：loaderVersion "[2,)" / 依赖 modId=neoforge）。
// 与 forge 节点同构：共享模板目录 mdk/src/main/templates，产物挂 main resources。
// 双模板互斥：各节点 exclude 对方的元数据文件，避免非本节点 toml 进 jar。
val replaceProperties = mapOf(
    "version" to project.version.toString(),
    "mod_id" to modId,
    "minecraft_version" to mcVer,
    "loader_version" to property("deps.fml").toString(),
    "neoforge_version_range" to property("deps.neoforge_range").toString(),
    "mod_license" to property("mod_license").toString(),
    "mod_name" to property("mod_name").toString(),
    "mod_description" to property("mod_description").toString(),
    "mod_authors" to property("mod_authors").toString(),
)

val generateModMetadata = tasks.register("generateModMetadata", ProcessResources::class) {
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    exclude("META-INF/mods.toml") // forge 专有元数据不进 neoforge 产物
    from(sharedDir.resolve("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

// 展开产物挂进 main resources（以任务为 srcDir 自动接线任务依赖）
sourceSets["main"].resources.srcDir(generateModMetadata)

// datagen 产物 srcDir 挂载不动（ADR-P17-1）：1.21.1 runtime 继续消费共享正典树
// mdk/src/generated/resources（模型/blockstate/lang 双节点同名同形；loot 复数带在 21.1 runtime
// 惰性 = 继承现状的声明偏离，ADR §5）。W1 伏笔「若两节点产物 diff → 切节点子目录」已由
// ADR-P17-1 以节点本地 runData --output（见上 runs.data）方式解决，本挂载零改动。
sourceSets["main"].resources.srcDir(sharedDir.resolve("src/generated/resources"))

// tag 运行时面嫁接（task p28-neotag-graft，research.p28-r-neo-tag-wiring 方案 a）：
// 正典树由 1.20.1-forge runData 独写（build.forge.gradle.kts:97），物品 tag 全为 1.20.1 复数形
// data/forge/tags/items/**（7901 文件）+ p27 前瞻孪生 data/c/tags/items/**（26 文件，与 forge 带
// 同相对路径字节同源，cmp 实证 diff=0）；而 1.21.1 数据包只读单数 tags/item（vanilla 1.21.1
// Registries.java:255-257 tagsDirPath=tags/+registry path；mcmeta-1211 data/minecraft/tags/item
// 实物）且消费面已 fork c:（GT6ItemTags.java:132-136）→ 复数带在 neo runtime 整体死亡。
// 本任务把两带原样镜像为 data/c/tags/item/**：目录段 items→item、命名空间 forge→c（24w21a
// 单数化仅目录段，家族名/文件名保持复数），内容字节零改写——与 datagen 侧 gt6/minecraft 单数
// 镜像先例（GT6DualDirectoryFaces.java:45-53）同构，其刻意悬置的 forge→c remap 裁决由本
// build 面闭环。正典树零改动、tree_check 输入零扰动（衍生面 = 打包变换，上方 generateModMetadata
// 同层先例，非 ADR-P17-1 第二生产者）。
val neoforgeTagFaces = tasks.register("neoforgeTagFaces", Copy::class) {
    // 孪生带 26 文件与 forge 带同路径字节同源 → 同目标去重确定性取正本（EXCLUDE = 先到先得）
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sharedDir.resolve("src/generated/resources/data/forge/tags/items")) { into("data/c/tags/item") }
    from(sharedDir.resolve("src/generated/resources/data/c/tags/items")) { into("data/c/tags/item") }
    into(layout.buildDirectory.dir("generated/neoforge-tag-faces"))
}

// 嫁接产物挂进 main resources（以任务为 srcDir 自动接线任务依赖，generateModMetadata 同构）：
// 死的复数带 v1 照挂不 exclude（zip64 已开，死重另卡声明，research.p28 build_change_list knob）。
sourceSets["main"].resources.srcDir(neoforgeTagFaces)

// FML junit 并发竞争根治（task r3-ci-fml-config-race）：maxParallelForks 并发下每个 executor 的
// FML 引导（ModDirTransformerDiscoveror.candidates → FMLConfig.load）都是「读 fml.toml → 无条件
// 重写」（loader-4.0.44 FMLConfig.loadFrom 字节码 :191 两路均 saveConfig，WritingMode.REPLACE=
// Files.newOutputStream truncate 写、非原子；本地该文件 mtime 随每次 test 运行刷新=重写实证，同
// 目录 neoforge-common.toml mtime 恒定=其余 config 一次性写、唯 fml.toml per-run 重写）。并发
// executor 的解析与彼此的重写交叠 → nightconfig 撕裂读（旧前缀+新后缀拼接腐坏视图）→
// ParsingException: Invalid bare key '#Disables' 启动即炸（CI run 36241829942；本地冷模拟「预置
// 完整文件后 6 fork 首跑」同型复现，executor 110 parseTableName 栈——预置方案已证伪，残窗高概率
// 而非 µs 级可忽略）。
// 无旋钮可绕：MDG setupTestTask 所有 fork 共享 workingDir=build/minecraft-junit
// （ModDevRunWorkflow.java:480 afterEvaluate setWorkingDir），Gradle Test 无 per-fork workingDir，
// FMLPaths FMLCONFIG 硬编码 GAMEDIR/config/fml.toml，nightconfig 3.8.3 无原子写旋钮（只读文件
// 会让 saveConfig 抛异常杀 executor，亦不可行）。
// 根治形：fml.toml → /dev/null 符号链接。FML 读=永远空=代码默认值；FML 写=truncate+write 全部落
// /dev/null 消失 → 盘上状态零变化，撕裂读物理不可能。代价=每 executor 一条 "Configuration file
// ... is not correct. Correcting" warn（空解析触发 correct，纯噪音）。
// 语义声明（照准 2026-09-25）：fml.toml 盘上内容=FML 4.0.44 写出的纯默认值（run/config 正本与
// 本节点模板字节同一，无任何自定义项）→ 写入丢弃=零语义损失。逃生门：未来若需自定义 junit FML
// 配置（改 maxThreads/versionCheck 等），必须先移除本符号链接或调整本任务——对 fml.toml 的任何
// 手工编辑都会被静默丢弃，不会有报错提示。
// 回退形（符号链接不可用，异构 FS）：原子预写完整文件——写入源优先 ① 节点 run/config/fml.toml
// 拷贝；② 下方内嵌模板（FML 4.0.44 nightconfig 默认产物逐字拷贝；jar 内无可提取模板——
// neoforge-21.1.249 universal/userdev + loader-4.0.44 三 jar 零 fml.toml/defaultconfigs 资源，
// 默认值由 FMLConfig 代码生成）。回退只消除冷启动 exists-flip 宽窗口，不消除重写-解析残窗。
// 两形均 tmp+ATOMIC_MOVE 同目录原子换入；刻意不声明 outputs/inputs → 每次 test 前强制重建
// （~1ms），自愈任何残留（含被杀运行留下的半写文件/旧实文件）。
// ponytail: 残余理论面——同目录其余 config（neoforge-common.toml/jade）为一次性写（mtime 恒定
// 实证），无 per-run 重写即无撕裂面；若未来 FML 版本引入更多 per-run 重写 config，同形扩展。
val fmlJunitConfigTemplate = """#Disables File Watcher. Used to automatically update config if its file has been modified.
disableConfigWatcher = false
#Should we control the window. Disabling this disables new GL features and can be bad for mods that rely on them.
earlyWindowControl = true
#Max threads for early initialization parallelism,  -1 is based on processor count
maxThreads = -1
#Enable NeoForge global version checking
versionCheck = true
#Default config path for servers
defaultConfigPath = "defaultconfigs"
#Disables Optimized DFU client-side - already disabled on servers
disableOptimizedDFU = true
#Early window provider
earlyWindowProvider = "fmlearlywindow"
#Early window width
earlyWindowWidth = 854
#Early window height
earlyWindowHeight = 480
#Early window framebuffer scale
earlyWindowFBScale = 1
#Early window starts maximized
earlyWindowMaximized = false
#Skip specific GL versions, may help with buggy graphics card drivers
earlyWindowSkipGLVersions = []
#Squir?
earlyWindowSquir = false
#Define dependency overrides below
#Dependency overrides can be used to forcibly remove a dependency constraint from a mod or to force a mod to load AFTER another mod
#Using dependency overrides can cause issues. Use at your own risk.
#Example dependency override for the mod with the id 'targetMod': dependency constraints (incompatibility clauses or restrictive version ranges) against mod 'dep1' are removed, and the mod will now load after the mod 'dep2'
#dependencyOverrides.targetMod = ["-dep1", "+dep2"]
dependencyOverrides = {}
"""

val prepareFmlJunitConfig = tasks.register("prepareFmlJunitConfig") {
    val target = layout.buildDirectory.file("minecraft-junit/config/fml.toml")
    val runConfig = file("run/config/fml.toml") // 配置期解析（config-cache 安全形）
    doLast {
        val targetFile = target.get().asFile
        targetFile.parentFile.mkdirs()
        val tmp = targetFile.resolveSibling("fml.toml.tmp")
        Files.deleteIfExists(tmp.toPath())
        try {
            Files.createSymbolicLink(tmp.toPath(), Path.of("/dev/null"))
        } catch (e: Exception) {
            // 符号链接不可用（异构 FS/权限）→ 回退原子预写完整文件（见上方回退形注释）
            if (runConfig.exists()) tmp.writeBytes(runConfig.readBytes()) else tmp.writeText(fmlJunitConfigTemplate)
        }
        Files.move(
            tmp.toPath(), targetFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE,
        )
    }
}

tasks.withType(Test::class).configureEach {
    // executor 启动前先根治共享 fml.toml 竞争面（/dev/null 符号链接，见 prepareFmlJunitConfig 注释段）
    dependsOn(prepareFmlJunitConfig)
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // 材料系统全量 init 属内存敏感（MT 1273 材料 + 全量物品枚举），与根项目测试同量级；
    // 1536m = CI 内存账让步（cap 6 并行的前提，降堆后双腿全量复测无 OOM）
    maxHeapSize = "1536m"
    // JVM 级并行：每 fork 独立 JVM，fork 内测试仍串行，类内语义零变化。
    // 核数×2：test fork 大头是类加载/静态 init 这类不满核计算，CI 4 vCPU → 6 fork 已让核饱和
    // （availableProcessors 运行时自适应；超订填缝收益 8→6 损失很小）。
    // cap 6 内存账（实测版）：单 fork RSS≈2.3GB@2g 堆（含 metaspace/native）→ 堆降 1536m 后
    // 估 ~1.9GB/fork，6×1.9+daemon ~1.5 ≈ 12.9GB ≤ 16GB runner 可用 ~13.5GB（OS+agent 占
    // ~1.5-2GB）；8×2.3=18.4GB、7×2.3=16.1GB、6×2.3=15.3GB@2g 堆均超可用——确定性 OOM。
    // ②本地 12 核裸公式=24 forks 超物理内存+gt6testgate 30G 闸，cap 6 同护本地。
    // 402 个测试文件已扫描：零 ServerSocket/零文件写（2026-09-25 主会话 grep 实证），fork 间无文件域冲突
    // ——唯一例外是 FML 基建自身 per-run 重写 fml.toml，由下方 prepareFmlJunitConfig 根治（r3 复盘修订）。
    maxParallelForks = minOf(Runtime.getRuntime().availableProcessors() * 2, 6)
}

// jar 条目数越 65535 界（~11.5k datagen JSON，task p8-prefixblock-render 先例）：
// kts 下 `zip64 = true` 不编译（isZip64/setZip64 分属两级，Kotlin 属性合成失败，最小复现 2026-09-03）——
// 用显式 setter。与 forge 节点同构。
tasks.named<Jar>("jar") {
    setZip64(true)
    // gregapi 根项目类打进 mod jar（真机分发自包含）——与 forge 节点同构，2026-09-06 真机加载实测缺口。
    from(project(":").sourceSets.main.get().output)
}

// chisel 生成源接线（模板 build.neoforge.gradle.kts.txt:53-55 同构）：
// MDG 工件任务挂 stonecutterGenerate，保证非活动节点（本节点）的处理后源先于工件产出。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
