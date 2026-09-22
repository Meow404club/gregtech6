// 1.20.1 Forge 节点构建脚本（由任意 forge 节点经 buildFileName ../../ 共享）。
// 内容 = 原 mdk/build.gradle（现役 1173 绿基线）的 Kotlin DSL 等价迁移，两处位移：
//   1. 节点 projectDir = mdk/versions/<node>，共享路径一律锚 parent!!.projectDir（= mdk/）；
//   2. gregapi 挂法 L2：implementation(project(":")) 消费根项目 java-library（禁 includeBuild 自指）。
// 证据：tmp/harvest/stonecutter-template/build.forge.gradle.kts.txt:2（legacyforge 插件）/:35-52（runs/mods）/:64-66（stonecutterGenerate 接线）。
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev.legacyforge")
}

group = "gregtech6"
version = property("mod_version").toString()

val modId = property("mod_id").toString()
val mcVer = property("deps.minecraft").toString()
val forgeVer = property("deps.forge").toString()
val forgeMajor = forgeVer.split(".")[0]
val jeiVer = property("jei_version").toString()
val jadeVer = property("jade_version").toString()

// 分发 jar 可辨识名（task p23-jar-naming）：archivesName = <mod_id>-<stonecutter 节点名>，
// jar 任务再追加 project.version（= mod_version）→ gt6-1.20.1-forge-0.1.0.jar。
// 旧默认 archivesName = project 名 = 节点目录名，产物 1.20.1-forge-0.1.0.jar 认不出是哪个 mod。
// Knob 求证：org.gradle.api.plugins.BasePluginExtension.getArchivesName(): Property<String>
// （javap gradle-api-8.14.jar，2026-09-07；wrapper = gradle-8.14-bin）；`base` 扩展由 java 插件
// 贡献（本脚本 java{}/sourceSets 在位即证 java 插件经 legacyforge 传递应用）。
// 节点名 = project.name：stonecutter 0.7 建节点 project ":mdk:<node>"（projectDir =
// versions/<node>）——TreeBuilderImpl.kt:123-131（tmp/harvest/stonecutter-src-07 收割源，
// createNode 用 "${parent.path}:${data.project}"，data.project 即 match() 的 "1.20.1-forge"）。
base {
    archivesName.set("${modId}-${project.name}")
}

// 共享锚点：控制器项目目录（mdk/），即节点共享资源/模板/datagen 产物的真实位置。
val sharedDir = parent!!.projectDir

// JEI（task p12-jei-integration，ADR 2026-09-02-p12-jei-dependency）：blamejared maven。
repositories {
    maven {
        name = "blamejared"
        url = uri("https://maven.blamejared.com/")
    }
    // Jade（task p21-jade-compat）：Modrinth maven，jade 双腿唯一分发渠道（GTCEu 1.20.1
    // gradle/forge.versions.toml:90 同源先例 maven.modrinth:jade）。
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    // KubeJS/Rhino（task p34-kjs-bindings）：dev.latvian.mods 组唯一分发渠道
    // （GTCEu gradle/scripts/repositories.gradle:58-63 同源同组过滤）。
    maven {
        name = "latvian"
        url = uri("https://maven.latvian.dev/releases")
    }
    // Architectury（dev.architectury 组，kubejs forge 腿运行时前置三件之一）：自家 maven。
    maven {
        name = "architectury"
        url = uri("https://maven.architectury.dev")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    // 全量诊断协议（M3 门禁口径，decisions.2026-09-03-p15-m1-gate）：javac 默认 maxerrs=100
    // 会截断错误清单，maxerrs=100000 保证红文件数/错误数全量可数（对 1.20.1 全绿面零影响）。
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "100000", "-Xmaxwarns", "100000"))
}

legacyForge {
    // MinecraftForge userdev 坐标（files.minecraftforge.net promotions recommended，2026-08-29 查证）
    version = "${mcVer}-${forgeVer}"

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        // 节点本地 run 目录（避免 1.20.1/1.21.1 游戏库互相污染；模板 build.forge.gradle.kts.txt:36-43 同构）
        register("client") {
            client()
            gameDirectory = file("run/")
            // p32-render-embeddium-tint 验证缝：-Pgt6.quickplay="<args>" 把额外程序参数
            // （--quickPlaySingleplayer/宽高）追加入 runClient——默认缺省零变化。
            val quickplay = providers.gradleProperty("gt6.quickplay")
            if (quickplay.isPresent) {
                programArguments.addAll(quickplay.get().split(" ").filter { it.isNotBlank() })
            }
        }
        // 同缝：-Pgt6.display=:97 显式覆盖 game 进程的 DISPLAY——runClient 由常驻 daemon
        // 执行，launcher 的 inline env 到不了 game（daemon 环境原样继承）；headless 验收机
        // 必须钉 Xvfb，物理 :0 禁弹窗（2026-09-20 纠正令）。
        val display = providers.gradleProperty("gt6.display")
        if (display.isPresent) {
            tasks.withType(JavaExec::class).matching { it.name == "runClient" }.configureEach {
                environment("DISPLAY", display.get())
            }
        }
        register("server") {
            server()
            gameDirectory = file("run/")
            // 用户裁定（2026-09-04，随 ADR-P15-4 卡合并）：服务端启动一律 nogui——
            // DedicatedServer 控制台 GUI 不许弹出，headless 验收机的唯一正典形态。
            // --nogui 两写法 forge 启动器都认，取带杠的显式形。
            programArguments.addAll("--nogui")
        }
        // Data run 四参数照搬 GTCEu gradle/scripts/moddevgradle.gradle:111-122 先例（ADR-P2-4）。
        // --existing 指向共享 main/resources：ModelBuilder.texture()（Forge 1.20.1 ModelBuilder.java:145-146）
        // 对 layer0 做 ExistingFileHelper 存在性校验，占位贴图必须已在 src/main/resources。
        register("data") {
            data()
            sourceSet = sourceSets["main"]
            programArguments.addAll("--mod", modId)
            programArguments.addAll("--all")
            programArguments.addAll("--output", sharedDir.resolve("src/generated/resources").absolutePath)
            programArguments.addAll("--existing", sharedDir.resolve("src/main/resources").absolutePath)
        }
    }
}

dependencies {
    // 根项目 = gregapi java-library（L2 挂法：ADR-P2-1 不 shadow，发布形态后续再定）
    implementation(project(":"))
    // MDG legacyforge（1.20.1）的 run 类路径只含游戏库：per-run legacyClasspath 配置仅
    // extendsFrom additionalRuntimeClasspath（ModDevRunWorkflow.configureLegacyClasspath），
    // implementation 依赖不会进 BootstrapLauncher 的 -DlegacyClassPath.file。
    // 2026-08-29 :mdk:runServer 实证 gregapi NoClassDefFoundError，按 MDG 官方机制补挂。
    // （插件自建配置用字符串 invoke 形式，不赌 kts accessor 生成）
    "additionalRuntimeClasspath"(project(path = ":", configuration = "runtimeElements"))
    // P20④ ModularUI vendored fork jarJar 嵌装（ADR 2026-09-06-p20-modularui-fork-ruling §4，
    // GTCEu 量产姿势 dependencies.gradle:14 对齐：顶层只 jarJar(mui)，EvalEx/mixinextras 由
    // modularui 自己嵌装携带）。MDG JarJarPlugin 把 jarJar 配置解析产物经 jarJar 任务写
    // META-INF/jarjar/ 并入 jar 任务产物（JarJarPlugin.java:22）。
    // legacyforge 消费者对 jarJar 内 ProjectDependency 自动加 MinecraftMappings=SRG 属性
    // （LegacyForgeModDevPlugin.configureDependencyRemapping:215-229），vendored 项目经
    // obf.reobfuscate 发布的 reobfRuntimeElements（SRG）变体被选中——嵌装的 modularui jar
    // 已是生产 SRG 形（含 refmap 重映射），无需再重映射。
    // 依赖面说明：modularui 不进编译/运行类路径（纯嵌装分发；mdk 源码零引用，GUI 采纳另卡）。
    "jarJar"(project(":third-party:modularui:1.20.1-forge"))
    // task p24-act-machine C2 前置门：mdk 主树首消费 ModularUI（decisions.p24-act-be-form）。
    // modCompileOnly = MDG legacyforge 重映射 child 配置（SRG→official，LEGACY.md:68-92）——
    // vendored 项目发布 reobfRuntimeElements（SRG）变体，编译面重映射回 official 供 mdk 源码
    // import brachy.modularui.*；运行面类仍由上方 jarJar 嵌装产物承载（dev run 经 jarJar
    // 嵌套发现，零双装）。
    "modCompileOnly"(project(":third-party:modularui:1.20.1-forge"))
    // 运行面：modRuntimeOnly = 重映射 child of runtimeOnly，产物并入 run 类路径（JEI impl
    // 同机制）——dev run 的 mod 发现走类路径上的 modularui mod 本体；jarJar 嵌装仅在发布
    // jar 中存在，dev run 零双装。
    "modRuntimeOnly"(project(":third-party:modularui:1.20.1-forge"))
    // JEI（本仓首个第三方 mod 依赖，ADR 2026-09-02-p12-jei-dependency）：坐标三件。
    // MDG legacyforge mod* 配置自动 SRG→official 重映射且非传递（LEGACY.md:68-92）：
    // API 两件 compileOnly（编译面，不进 jar/run）；impl 一件 runtimeOnly —— LEGACY.md L74
    // mod* = 同名标准配置的 child，重映射产物并入 run 类路径 → runClient 可见 JEI。
    // 版本钉值 root gradle.properties jei_version（全局可见）；GTCEu 1.20.1 先例（GTJEIPlugin.java）。
    "modCompileOnly"("mezz.jei:jei-${mcVer}-common-api:${jeiVer}")
    "modCompileOnly"("mezz.jei:jei-${mcVer}-forge-api:${jeiVer}")
    "modRuntimeOnly"("mezz.jei:jei-${mcVer}-forge:${jeiVer}")
    // Jade（WAILA 后继，task p21-jade-compat）：与 JEI mod* 同机制——MDG legacyforge mod*
    // 配置自动 SRG→official 重映射且非传递（LEGACY.md:68-92），同件挂 compileOnly（编译面）
    // + runtimeOnly（run 类路径，专用服务端冒烟依赖它）。版本钉值 root gradle.properties
    // jade_version（1.20.1 = 11.13.3+forge）；GTCEu 1.20.1 先例 dependencies.gradle:29
    // modCompileOnly(forge.jade)。SRG 泄漏预案：降级 fg.deobf 形（arch 卡 build_wiring.fallback）。
    "modCompileOnly"("maven.modrinth:jade:${jadeVer}")
    // p32-render-embeddium-tint 缝：-Pgt6.nojade=true 可把 Jade 摘出 run 类路径——Jade
    // 11.13.3 在 dev 环境逢 TitleScreen.init 硬断言全部插件 config 翻译在案
    // （JadeClient.onGui:156 translationChecked + isDevEnv 双门；GT6 的
    // config.jade.plugin_gt6.* lang 键缺失 = p21-jade 面的既有缝隙，另卡不清）。
    // 视觉验证腿不需要 Jade；默认缺省 = Jade 照旧 rides 每个 dev client。
    if (!providers.gradleProperty("gt6.nojade").isPresent) {
        "modRuntimeOnly"("maven.modrinth:jade:${jadeVer}")
    }
    // p32-render-embeddium-tint 验证缝（P26 冒烟钉版 0.3.31+mc1.20.1）：-Pgt6.embeddium=true
    // 才挂 modRuntimeOnly——mod 只进 runClient 类路径（MDG mod* 重映射 child 配置，JEI 同
    // 机制），编译面与 jar 产物零触碰；默认缺省 = 全部既有 runClient 行为不变。
    if (providers.gradleProperty("gt6.embeddium").isPresent) {
        "modRuntimeOnly"("maven.modrinth:embeddium:0.3.31+mc1.20.1")
    }
    // KubeJS 绑定依赖段（task p34-kjs-bindings）：modCompileOnly 运行时可选零传染
    // （GTCEu dependencies.gradle:43-46 先例——不 jarJar 不 modApi 不 modRuntime）。
    // 可选性机制 = kubejs.plugins.txt 资源根发现（KubeJSPlugins.findResource，7.x 源 :38-41）
    // 是 KubeJS 侧拉取：KubeJS 不在 = 无人读 plugins.txt = kjs 类零类加载 = NoClassDef 面不存在。
    // 版本钉值 = 节点 gradle.properties（kubejs_version/rhino_version/architectury_version，
    // 与 GTCEu forge.versions.toml:11-13 同 build）。留空 kubejs_version = 本段整体跳过 +
    // kjs 源从编译面消失（下方 OFF 态包排光；stonecutter kjs 常量同步 false——非活动节点的
    // 预处理面由它清空）＝runtime-optional 双态开关，验收①「拔依赖 compile 过」由此达成。
    val kubejsVer = property("kubejs_version").toString().trim()
    if (kubejsVer.isNotEmpty()) {
        "modCompileOnly"("dev.latvian.mods:kubejs-forge:${kubejsVer}")
        "modCompileOnly"("dev.latvian.mods:rhino-forge:${property("rhino_version").toString().trim()}")
        "modCompileOnly"("dev.architectury:architectury-forge:${property("architectury_version").toString().trim()}")
    }
    // mdk 单测（p3-be-framework + p3-fullprefix-creativetab 归一）：BE NBT round-trip /
    // onTick 分发 / 材料适配器 / 注册判定与 first-wins 断言；junit-bom 全模块一份。
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// kjs OFF 态包排光（task p34-kjs-bindings 双态验收①）：本节点是 stonecutter 活动节点，对共享源
// mdk/src 原位裸编译——chisel `//? if kjs` 行在裸面上只是注释，常量置 false 不会让包裹源从裸
// 编译消失（实证 /tmp/p34_duotest_off_forge.log：裸编译撞 dev.latvian.mods import 10 错）。
// 包排光是活动节点 OFF 态的唯一机制；非活动节点（1.21.1-neoforge，编译 stonecutter 预处理产物）
// 由 kjs 常量清空，两者互不依赖。ON 态本段不跑，行为零变化。
if (property("kubejs_version").toString().trim().isEmpty()) {
    sourceSets["main"].java.exclude("gregtech6/integration/kjs/**")
    sourceSets["test"].java.exclude("gregtech6/integration/kjs/**")
}

// test sourceSet 复用 main 的输出与类路径：MDG（moddevgradle 2.0.144）只把游戏库配置
// modDevCompileDependencies/modDevRuntimeDependencies 挂到 main 的 compile/runtime
// classpath（主工程 probeConfigs 实测 extendsFrom 列表），test sourceSet 需自行 extendsFrom，
// implementation project(':') 的 gregapi 经 java 插件默认传导。
configurations.named("testCompileClasspath") {
    extendsFrom(configurations.named("modDevCompileDependencies").get())
}
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.named("modDevRuntimeDependencies").get())
}

// ---- Test sourceSet 类路径补全（p3-fullprefix-creativetab：注册判定/first-wins/判据收敛断言）----
// 测试 JVM 需要加载 gregapi（root jar，经 main 类路径）与 main 类引用的 MC 类型
// （GTMaterialItems 的字节码校验需要 net.minecraft 类在测试类路径上），
// 故把 main 的 compile/runtimeClasspath 追加进 test——仅测试类路径配置，runs/jar 不受影响。
sourceSets["test"].compileClasspath += sourceSets["main"].output
sourceSets["test"].runtimeClasspath += sourceSets["main"].output
sourceSets["test"].compileClasspath += sourceSets["main"].compileClasspath
sourceSets["test"].runtimeClasspath += sourceSets["main"].runtimeClasspath

// mods.toml 模板展开，同构 GTCEu gradle/scripts/resources.gradle:34-63（模板本体在共享 mdk/src/main/templates）。
// 双模板互斥（W2）：本节点 exclude neoforge.mods.toml（NeoForge 1.20.5+ 专有文件名），
// neoforge 节点对称 exclude mods.toml——避免非本节点元数据进 jar。
val replaceProperties = mapOf(
    "version" to project.version.toString(),
    "mod_id" to modId,
    "minecraft_version" to mcVer,
    "loader_version" to forgeMajor,
    "forge_version" to forgeMajor,
    "mod_license" to property("mod_license").toString(),
    "mod_name" to property("mod_name").toString(),
    "mod_description" to property("mod_description").toString(),
    "mod_authors" to property("mod_authors").toString(),
)

val generateModMetadata = tasks.register("generateModMetadata", ProcessResources::class) {
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    exclude("META-INF/neoforge.mods.toml") // neoforge 专有元数据不进 forge 产物
    from(sharedDir.resolve("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

// 展开产物挂进 main resources（以任务为 srcDir 自动接线任务依赖）
sourceSets["main"].resources.srcDir(generateModMetadata)
legacyForge.ideSyncTask(generateModMetadata)

// DataGen 产物挂进 main resources（GTCEu build.gradle:24-28 同构，datagen 代码留 main sourceSet，
// 产物目录零手写 JSON——红线：所有 JSON 由 DataProvider 生成）。
// POC 节点策略初步结论：默认共享 generated（mdk/src/generated/resources 双节点同读；
// 若两节点产物 diff → W2 切节点子目录 versions/<node>/src/main/generated，模板 :51 先例）。
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

// task p8-prefixblock-render 声明偏离（FILES_SCOPE 一行例外，验收门禁所迫）：
// 生成树挂进 main resources（既有接线）+ 本卡 ~11.5k datagen JSON 后，
// jar 条目数越过 zip 上限 65535（"Archive contains more than 65535 entries"）。
// zip64 打开扩展头，java.util.zip/Forge SecureJar 均可读；仅影响打包产物，
// runs 走 exploded classpath 不受影响。
tasks.named<Jar>("jar") {
    // 注意：Gradle 8.14 kts 下 `zip64 = true` 不编译（isZip64/setZip64 分属 Zip/AbstractArchiveTask
    // 两级，Kotlin 属性合成失败，最小复现实证 2026-09-03）——用显式 setter。
    setZip64(true)
    // gregapi 根项目类打进 mod jar（真机分发自包含）：runs 走 exploded classpath 双项目不受影响，
    // 但玩家 mods/ 目录只有本 jar 一个类加载域——缺 gregapi 类即 NoClassDefFoundError
    // （2026-09-06 真机加载实测）。取代 P2「mdk 不 shadow gregapi」开发期口径（仅分发形态，依赖仍 compileOnly 面）。
    from(project(":").sourceSets.main.get().output)
}

// chisel 生成源接线（模板 build.forge.gradle.kts.txt:64-66 同构）：
// MDG 工件任务挂 stonecutterGenerate，保证非活动节点的处理后源先于工件产出。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
