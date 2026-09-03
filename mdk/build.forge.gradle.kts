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

// 共享锚点：控制器项目目录（mdk/），即节点共享资源/模板/datagen 产物的真实位置。
val sharedDir = parent!!.projectDir

// JEI（task p12-jei-integration，ADR 2026-09-02-p12-jei-dependency）：blamejared maven。
repositories {
    maven {
        name = "blamejared"
        url = uri("https://maven.blamejared.com/")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
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
        }
        register("server") {
            server()
            gameDirectory = file("run/")
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
    // JEI（本仓首个第三方 mod 依赖，ADR 2026-09-02-p12-jei-dependency）：坐标三件。
    // MDG legacyforge mod* 配置自动 SRG→official 重映射且非传递（LEGACY.md:68-92）：
    // API 两件 compileOnly（编译面，不进 jar/run）；impl 一件 runtimeOnly —— LEGACY.md L74
    // mod* = 同名标准配置的 child，重映射产物并入 run 类路径 → runClient 可见 JEI。
    // 版本钉值 root gradle.properties jei_version（全局可见）；GTCEu 1.20.1 先例（GTJEIPlugin.java）。
    "modCompileOnly"("mezz.jei:jei-${mcVer}-common-api:${jeiVer}")
    "modCompileOnly"("mezz.jei:jei-${mcVer}-forge-api:${jeiVer}")
    "modRuntimeOnly"("mezz.jei:jei-${mcVer}-forge:${jeiVer}")
    // mdk 单测（p3-be-framework + p3-fullprefix-creativetab 归一）：BE NBT round-trip /
    // onTick 分发 / 材料适配器 / 注册判定与 first-wins 断言；junit-bom 全模块一份。
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

// mods.toml 模板展开，同构 GTCEu gradle/scripts/resources.gradle:34-63（模板本体在共享 mdk/src/main/templates）
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
}

// chisel 生成源接线（模板 build.forge.gradle.kts.txt:64-66 同构）：
// MDG 工件任务挂 stonecutterGenerate，保证非活动节点的处理后源先于工件产出。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
