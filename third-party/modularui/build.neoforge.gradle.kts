// P20④ vendored ModularUI fork——1.21.1-neoforge 节点构建脚本（由节点经 buildFileName ../../ 共享）。
// 与 mdk/build.neoforge.gradle.kts 同插件（moddev 2.0.144）同机制，但形态最小化：
//   · 无 runs（modularui 是被 jarJar 嵌入的库 mod，不单独起游戏——卡② packaging 域）；
//   · 无 jar 定制/mod 元数据展开（同卡②）；无 datagen（上游产物 src/main/resources 静态携带）；
//   · 无 test sourceSet 依赖（上游 src/test 不 vendored，验收=compileJava）。
// 版本钉值全部取自上游 1.21.1 分支自带 catalog（tmp/harvest/modularui-upstream/gradle/*.toml，
// 2026-09-06 快照），保持上游编译面 1:1——跟随上游=重 vendored 时照抄新钉值。
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev")
}

// 上游坐标（jarJar 嵌入时 jar 元数据沿用，见卡②；此处先定 group/version 供 idea 索引）。
group = "brachy.modularui"
version = property("mod_version").toString()

val modId = property("mod_id").toString()
val mcVer = property("deps.minecraft").toString()
val jeiVer = property("jei_version").toString()
val reiVer = property("rei_version").toString()
val architecturyVer = property("architectury_version").toString()
val curiosVer = property("curios_version").toString()
val emiVer = property("emi_version").toString()
val sodiumVer = property("sodium_version").toString()
val evalExVer = property("evalex_version").toString()
val jetbrainsVer = property("jetbrains_annotations_version").toString()
val lombokVer = property("lombok_version").toString()

// 共享锚点：控制器项目目录（third-party/modularui/）。
val sharedDir = parent!!.projectDir

repositories {
    mavenCentral() // EvalEx / lombok / jetbrains-annotations
    maven("https://maven.blamejared.com/") { name = "blamejared" } // JEI
    maven("https://maven.terraformersmc.com/releases/") { name = "terraformers" } // EMI
    maven("https://maven.shedaniel.me/") { name = "shedaniel" } // REI + architectury
    maven("https://maven.theillusivec4.top/") { name = "illusivec4" } // Curios
    maven("https://api.modrinth.com/maven") { name = "modrinth" } // Sodium
}

// Java 21（1.21.1 线；本仓 mdk 同款）。
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    // 全量诊断协议（mdk 同款，M3 门禁口径）：maxerrs=100000 保证红文件全量可数。
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "100000", "-Xmaxwarns", "100000"))
    // 上游 build.gradle 同款：mixin AP notes 静音。
    options.compilerArgs.add("-Aquiet=true")
}

// 单腿专属源并入 main sourceSet（settings vcsVersion=本节点，vendored 主树=neoforge 形，
// src/main 原位编译零处理；neoforgeMain 仅 1.21.1-only 文件，零 chisel 语法，原样并入即可）。
sourceSets["main"].java.srcDir(sharedDir.resolve("src/neoforgeMain/java"))
sourceSets["main"].resources.srcDir(sharedDir.resolve("src/neoforgeMain/resources"))

neoForge {
    version = property("deps.neoforge").toString()

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    // NeoForge 接口注入（上游 moddevgradle.gradle 同款：injected_interfaces/interfaces.json）。
    // 注入面：Component←ToModularComponent、RegistryFriendlyByteBuf←IRegistryFriendlyByteBufExtension
    // ——common 树调用点靠注入解析扩展方法，缺了 1.21.1 腿编译红。
    interfaceInjectionData {
        val f = sharedDir.resolve("src/neoforgeMain/injected_interfaces/interfaces.json")
        from(f)
        publish(f)
    }
}

dependencies {
    // 唯一硬 runtime 依赖（ADR-P20 §5：EvalEx 3.6.0）
    implementation("com.ezylang:EvalEx:${evalExVer}")
    compileOnly("org.jetbrains:annotations:${jetbrainsVer}")
    // 上游源码用 lombok（@Setter/@Getter/@Cleanup 等，lombok.config 随仓）
    compileOnly("org.projectlombok:lombok:${lombokVer}")
    annotationProcessor("org.projectlombok:lombok:${lombokVer}")

    // ---- jarJar 嵌装（卡② packaging 域，上游 1.21.1 dependencies.gradle:9-10 原形）----
    // 本 mod jar 被 mdk neoforge 腿 jarJar 嵌套时需自带 EvalEx（GTCEu 量产对齐：GTCEu 顶层只
    // jarJar(mui)，EvalEx 由 modularui 自己携带——ADR-P20 §4 引证）。
    //
    // 版本区间策略（minimum/maximum）：**裸依赖原形，禁 require() 区间**——嵌入 jar=编译钉版
    // 3.6.0（与 implementation 同钉，嵌装与编译面零漂移；require("[x,y)") 会让 jarJar 配置独立
    // 解析区间内最高版，实测漂移到 EvalEx 3.7.0，2026-09-07 首编即证——弃用）；metadata.json
    // 区间由 MDG 自动生成开区间 [3.6.0,)（min=钉版，max=开放=上游量姿势，闭区间上界易造跨 mod
    // JarJar 运行时选版不可满足）。
    // mixinextras 不嵌：NeoForge 1.21.1 加载器自带（上游 1.21.1 dependencies.gradle 无
    // mixinextras jarJar 声明，与 1.20.1 腿的显式携带不同——跟随上游分腿姿势）。
    "jarJar"("com.ezylang:EvalEx:${evalExVer}")

    // Recipe viewers / 可选集成（全 compileOnly，ADR-P20 §5；版本=上游 1.21.1 catalog 钉值）
    // impl 一并 compileOnly：上游 bundles.jei = common-api+neoforge-api+impl 三件——
    // modularui 源引用 JEI 内部包（mezz.jei.common.input / mezz.jei.gui.*，2026-09-06 首编实证 9 红全此因）。
    compileOnly("mezz.jei:jei-${mcVer}-common-api:${jeiVer}")
    compileOnly("mezz.jei:jei-${mcVer}-neoforge-api:${jeiVer}")
    compileOnly("mezz.jei:jei-${mcVer}-neoforge:${jeiVer}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-neoforge:${reiVer}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${reiVer}")
    compileOnly("dev.architectury:architectury-neoforge:${architecturyVer}")
    compileOnly("dev.emi:emi-neoforge:${emiVer}")
    compileOnly("top.theillusivec4.curios:curios-neoforge:${curiosVer}:api")
    compileOnly("maven.modrinth:sodium:${sodiumVer}")
    // MouseTweaks：上游声明 compileOnly 但源码零引用（grep 实证 2026-09-06）——不声明，偏离见 DIVERGE.md。
}

// ---- packaging 段（卡②；上游 jars.gradle + resources.gradle 的本仓等价迁移）----
// 工件名对齐上游发布坐标 brachy.modularui:modularui-mc<mc>（GTCEu catalog 消费名）：
// 上游 jars.gradle base.archivesName = "${project.name}-mc${mc}"，project.name 上游=modularui；
// 本仓节点 project.name=节点名（1.21.1-neoforge），显式钉同值工件名——jar 文件名与
// jarJar 嵌入名（brachy.modularui.modularui-mc1.21.1-<v>.jar）随之确定。
base {
    archivesName = "modularui-mc${mcVer}"
}

// neoforge.mods.toml 模板展开（上游 1.21.1 resources.gradle 同构；模板=上游
// src/main/templates 原文字节拷贝到 sharedDir/templates，sha256 对账见 DIVERGE.md §1.7）。
// 属性名沿上游 1211：forge_version=neoforge 版本去 patch 位（21.1）；loader_version=上游
// catalog libs.versions.loader="4"（FML 大版本，模板 loaderVersion 消费）。
// 双模板互斥（mdk W2 同构）：本节点 exclude mods.toml。
val replaceProperties = mapOf(
    "version" to project.version.toString(),
    "mod_id" to modId,
    "minecraft_version" to mcVer,
    "loader_version" to "4",
    "forge_version" to property("deps.neoforge").toString().substringBeforeLast("."),
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
    exclude("META-INF/mods.toml") // forge 专有元数据不进 neoforge 产物
    from(sharedDir.resolve("templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets["main"].resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.named<Jar>("jar") {
    // zip64 保留（卡① 占位转正）：嵌装 jarJar 后条目面随依赖增长，跟随 mdk 先例
    // （65535 上限条目；zip64 头对 java.util.zip/SecureJar 均可读，mdk 2026-09-03 实证）。
    setZip64(true)
    // jar manifest（上游 1.21.1 jars.gradle 逐键同款；无 MixinConfigs——1.21.1 mixin 配置
    // 经 neoforge.mods.toml [[mixins]] 表发现，模板已携带）。
    // Specification-Title 上游=project.name(=modularui)，本仓节点名不同显式用 mod_id 等值。
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Version" to project.version.toString(),
            "Specification-Vendor" to "brachy",
            "Implementation-Title" to base.archivesName.get(),
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to "brachy",
        )
    }
}

// chisel 生成源接线（mdk 同构）：非活动节点工件任务先于 stonecutterGenerate；
// 本节点=活动节点（原位编译），此接线为对称占位（活动节点 stonecutterGenerate 为恒等/近恒等处理）。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
