// 1.21.1 NeoForge 节点构建脚本（由任意 neoforge 节点经 buildFileName ../../ 共享）。
// 1.21.1 必 moddev（LEGACY.md:2-13：legacyforge 插件上限 MinecraftForge 1.20.1）。
// 证据：tmp/harvest/stonecutter-template/build.neoforge.gradle.kts.txt:1-46（neoForge 块/runs/mods register）。
// W2 段补全（与 forge 节点同构）：runs client/server/data + test sourceSet 类路径接线 +
// generateModMetadata（META-INF/neoforge.mods.toml，NeoForge 1.20.5+ 元数据文件名）+ zip64 + 诊断协议 maxerrs。
// W5 段（task p15-jei-dual-wiring）：JEI 1.21.1 三件接线（见 dependencies JEI 注释段）；
// datagen 产物默认共享（下文 srcDir）。
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev")
}

group = "gregtech6"
version = property("mod_version").toString()

val modId = property("mod_id").toString()
val mcVer = property("deps.minecraft").toString()
val jeiVer = property("jei_version").toString()
val jadeVer = property("jade_version").toString()

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
}

// 1.21.1 节点 = Java 21（本机 java-21-openjdk；ADR-P15-2：gregapi 钉 17，17 产物可被 21 工具链直接消费）
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.withType(Test::class).configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // 材料系统全量 init 属内存敏感（MT 1273 材料 + 全量物品枚举），与根项目测试同量级
    maxHeapSize = "2g"
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
