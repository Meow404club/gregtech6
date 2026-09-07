// P20④ vendored ModularUI fork——1.20.1-forge 节点构建脚本（由节点经 buildFileName ../../ 共享）。
// 与 build.neoforge.gradle.kts 镜像（最小化形态，差异面）：
//   · legacyforge 插件（1.20.1 上限，mdk/build.forge.gradle.kts 同款）+ Java 17 工具链；
//   · 1.20.1 forge mod 依赖走 modCompileOnly 重映射配置（LEGACY.md:68-92，mdk JEI 先例）；
//   · mixin AP 显式声明（上游 1.20.1 分支同款：mixin :processor + mixinextras-common）；
//   · 单腿专属源并入 main sourceSet = src/forgeMain（1.20.1-only 文件，forge 形原文，零 chisel）。
// 版本钉值取自上游 1.20.1 分支 catalog（tmp/harvest/modularui-upstream-1201/gradle/*.toml）。
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev.legacyforge")
}

group = "brachy.modularui"
version = property("mod_version").toString()

val modId = property("mod_id").toString()
val mcVer = property("deps.minecraft").toString()
val forgeVer = property("deps.forge").toString()
val jeiVer = property("jei_version").toString()
val reiVer = property("rei_version").toString()
val architecturyVer = property("architectury_version").toString()
val clothmathVer = property("clothmath_version").toString()
val curiosVer = property("curios_version").toString()
val emiVer = property("emi_version").toString()
val embeddiumVer = property("embeddium_version").toString()
val evalExVer = property("evalex_version").toString()
val jetbrainsVer = property("jetbrains_annotations_version").toString()
val lombokVer = property("lombok_version").toString()
val mixinVer = property("mixin_version").toString()
val mixinExtrasVer = property("mixinextras_version").toString()

val sharedDir = parent!!.projectDir

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com/") { name = "blamejared" }
    maven("https://maven.terraformersmc.com/releases/") { name = "terraformers" }
    maven("https://maven.shedaniel.me/") { name = "shedaniel" }
    maven("https://maven.theillusivec4.top/") { name = "illusivec4" }
    maven("https://api.modrinth.com/maven") { name = "modrinth" }
}

// Java 17（1.20.1 线；上游 1.20.1 分支 libs java=17 实证）。
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "100000", "-Xmaxwarns", "100000"))
    options.compilerArgs.add("-Aquiet=true")
}

// 单腿专属源并入 main sourceSet（1.20.1-only 文件在盘即 forge 形；本节点非活动节点，
// src/main 共享树经 stonecutter 处理后编译，swap 表 + //? 分叉见 stonecutter.gradle.kts）。
sourceSets["main"].java.srcDir(sharedDir.resolve("src/forgeMain/java"))
sourceSets["main"].resources.srcDir(sharedDir.resolve("src/forgeMain/resources"))

legacyForge {
    version = "${mcVer}-${forgeVer}"

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    // 接口注入（1.20.1 侧注入面 = Component←ToModularComponent，上游 1.20.1 interfaces.json 同款；
    // RegistryFriendlyByteBuf 注入 1.20.1 无目标类，per-leg 文件放 src/forgeMain/injected_interfaces）。
    interfaceInjectionData {
        val f = sharedDir.resolve("src/forgeMain/injected_interfaces/interfaces.json")
        from(f)
        publish(f)
    }
}

// Mixin AP 接线（上游 1.20.1 moddevgradle.gradle 同款 mixin{} 块）：refmap 名 + mixin config 登记。
// 缺此块 AP 拿不到 obfuscation 环境 → @Inject/@Redirect target 全红
//（"Unable to locate obfuscation mapping"，2026-09-06 首编实证）。
mixin {
    add(sourceSets["main"], "modularui.refmap.json")
    config("modularui.mixins.json")
}

dependencies {
    implementation("com.ezylang:EvalEx:${evalExVer}")
    compileOnly("org.jetbrains:annotations:${jetbrainsVer}")
    compileOnly("org.projectlombok:lombok:${lombokVer}")
    annotationProcessor("org.projectlombok:lombok:${lombokVer}")

    // Mixin AP（上游 1.20.1 dependencies.gradle 同款：processor 变体 + mixinextras-common）。
    // refmap 名在 mixin{} 块；mod 元数据（mods.toml 展开与 jar manifest）见下方 packaging 段。
    annotationProcessor("org.spongepowered:mixin:${mixinVer}:processor")
    compileOnly("io.github.llamalad7:mixinextras-common:${mixinExtrasVer}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${mixinExtrasVer}")

    // ---- jarJar 嵌装（卡② packaging 域，上游 1.20.1 dependencies.gradle:9-17 原形）----
    // 本 mod jar 被 mdk forge 腿 jarJar 嵌套时需自带这两件（GTCEu 量产对齐：GTCEu 顶层只
    // jarJar(mui)，EvalEx/mixinextras 由 modularui 自己携带——ADR-P20 §4 引证）。
    //
    // 版本区间策略（minimum/maximum）：**裸依赖原形，禁 require() 区间**——
    //   · 嵌入 jar = 编译钉版本身（evalex 3.6.0 / mixinextras 0.5.0-rc.3，经 implementation
    //     同钉编译，嵌装与编译面零漂移；require("[x,y)") 会让 jarJar 配置独立解析区间内最高版，
    //     实测漂移到 EvalEx 3.7.0 ≠ 编译面，2026-09-07 首编即证——弃用）；
    //   · metadata.json 区间由 MDG 对裸版本自动生成开区间 [3.6.0,)/[0.5.0-rc.3,)（min=钉版，
    //     max=开放）——开区间=上游与 GTCEu require 同款量姿势：跨 mod 各嵌各的升级异步，闭区间
    //     上界易造 JarJar 运行时选版不可满足。
    "jarJar"("com.ezylang:EvalEx:${evalExVer}")
    "jarJar"("io.github.llamalad7:mixinextras-forge:${mixinExtrasVer}")

    // Recipe viewers / 可选集成：mod* 重映射配置（1.20.1 SRG 面），mdk JEI 段同机制。
    // impl 一并 modCompileOnly：上游 bundles.jei 三件含 impl（modularui 源引用 JEI 内部包，
    // neoforge 腿首编实证 2026-09-06）。
    "modCompileOnly"("mezz.jei:jei-${mcVer}-common-api:${jeiVer}")
    "modCompileOnly"("mezz.jei:jei-${mcVer}-forge-api:${jeiVer}")
    "modCompileOnly"("mezz.jei:jei-${mcVer}-forge:${jeiVer}")
    "modCompileOnly"("me.shedaniel:RoughlyEnoughItems-default-plugin-forge:${reiVer}")
    "modCompileOnly"("me.shedaniel:RoughlyEnoughItems-forge:${reiVer}")
    "modCompileOnly"("dev.architectury:architectury-forge:${architecturyVer}")
    // REI 的 me.shedaniel.math 包在独立 cloth-basic-math 构件（上游 catalog clothmath；钉 shedaniel maven latest 0.6.1）
    "modCompileOnly"("me.shedaniel.cloth:basic-math:${clothmathVer}")
    "modCompileOnly"("dev.emi:emi-forge:${emiVer}")
    "modCompileOnly"("top.theillusivec4.curios:curios-forge:${curiosVer}:api")
    "modCompileOnly"("maven.modrinth:embeddium:${embeddiumVer}")

    // 测试 JVM 基建（P21 smoke-gui-headless 卡，mdk 同款三件）：
    // 离线 headless JUnit 冒烟（ADR 2026-09-07-p21-smoke-gui-headless-ruling 路线 b）。
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---- test sourceSet 接线（P21 卡；与 mdk/build.forge.gradle.kts 同构）----
// MDG legacyforge 只把游戏库配置（modDevCompileDependencies/modDevRuntimeDependencies，主工程
// probeConfigs 实测 extendsFrom 列表）挂到 main 的 compile/runtime classpath，test sourceSet
// 需自行 extendsFrom——testRuntimeClasspath 由此携带 MC+forge loader 类，裸 JVM 可实例化
// ModularScreen（其构造仅 :162 MinecraftForge.EVENT_BUS.post 单点 loader 依赖）。
configurations.named("testCompileClasspath") {
    extendsFrom(configurations.named("modDevCompileDependencies").get())
}
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.named("modDevRuntimeDependencies").get())
}

// main 的 output 与 classpath 追加进 test——测试类路径专属，runs/jar 不受影响
//（mdk p3-fullprefix-creativetab 先例）。测试源在共享 src/test/java（零 chisel 语法，
// 双节点同源同断言；vendored src/main 零触碰，gen-forks.py 重放安全不受影响）。
sourceSets["test"].compileClasspath += sourceSets["main"].output
sourceSets["test"].runtimeClasspath += sourceSets["main"].output
sourceSets["test"].compileClasspath += sourceSets["main"].compileClasspath
sourceSets["test"].runtimeClasspath += sourceSets["main"].runtimeClasspath

tasks.withType(Test::class).configureEach {
    useJUnitPlatform()
    // 上游自带裸 JVM 测试钩子（1.12.2 系 FormatTest 血统）：ModularUI.isClientThread/isClientSide
    // 在 isTestEnv() 下短路（neoforgeMain ModularUI.java:116-131），绕开 FMLEnvironment.dist——
    // 否则 widget 类静态链（Widget→RichTooltip→RichText→Spacer:LINE_SPACER）在裸 JVM 必炸
    //（2026-09-07 首跑实证）。
    systemProperty("unit.testing", "true")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// ---- packaging 段（卡②；上游 jars.gradle + resources.gradle 的本仓等价迁移）----
// 工件名对齐上游发布坐标 brachy.modularui:modularui-mc<mc>（GTCEu catalog 消费名）：
// 上游 jars.gradle base.archivesName = "${project.name}-mc${mc}"，project.name 上游=modularui；
// 本仓节点 project.name=节点名（1.20.1-forge），显式钉同值工件名——jar 文件名与
// jarJar 嵌入名（brachy.modularui.modularui-mc1.20.1-<v>.jar）随之确定。
base {
    archivesName = "modularui-mc${mcVer}"
}

// mods.toml 模板展开（上游 resources.gradle generateModMetadata 同构；模板=上游
// src/main/templates 原文字节拷贝到 sharedDir/templates，sha256 对账见 DIVERGE.md §1.7）。
// 双模板互斥（mdk W2 同构）：本节点 exclude neoforge.mods.toml。
val replaceProperties = mapOf(
    "version" to project.version.toString(),
    "mod_id" to modId,
    "minecraft_version" to mcVer,
    "loader_version" to forgeVer.split(".")[0],
    "forge_version" to forgeVer.split(".")[0], // 上游只取 forge major
    "jei_version" to jeiVer,
    "emi_version" to emiVer,
    "rei_version" to reiVer,
    "curios_version" to curiosVer,
    "mod_license" to property("mod_license").toString(),
    "mod_name" to property("mod_name").toString(),
    "mod_description" to property("mod_description").toString(),
    "mod_url" to property("mod_url").toString(),
    "mod_issue_tracker" to property("mod_issue_tracker").toString(),
)

val generateModMetadata = tasks.register("generateModMetadata", ProcessResources::class) {
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    exclude("META-INF/neoforge.mods.toml") // neoforge 专有元数据不进 forge 产物
    from(sharedDir.resolve("templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets["main"].resources.srcDir(generateModMetadata)
legacyForge.ideSyncTask(generateModMetadata)

tasks.named<Jar>("jar") {
    // zip64 保留（卡① 占位转正）：嵌装 jarJar 后条目面随依赖增长，跟随 mdk 先例
    // （65535 上限条目；zip64 头对 java.util.zip/Forge SecureJar 均可读，mdk 2026-09-03 实证）。
    setZip64(true)
    // jar manifest（上游 jars.gradle:27-37 逐键同款；MixinConfigs=Forge 1.20.1 生产环境
    // mixin 配置发现通道，neoforge 腿走 neoforge.mods.toml [[mixins]] 表故无此键）。
    // Specification-Title 上游=project.name(=modularui)，本仓节点名不同显式用 mod_id 等值。
    manifest {
        attributes(
            "MixinConfigs" to "modularui.mixins.json",
            "Specification-Title" to modId,
            "Specification-Version" to project.version.toString(),
            "Specification-Vendor" to "brachy",
            "Implementation-Title" to base.archivesName.get(),
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to "brachy",
        )
    }
}

// chisel 生成源接线（mdk 同构）：本节点非活动节点，工件任务须等 stonecutterGenerate 产出处理后源。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
