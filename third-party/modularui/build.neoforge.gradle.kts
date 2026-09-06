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
    // 唯一硬 runtime 依赖（ADR-P20 §5：EvalEx 3.6.0；嵌套/jarJar 存活归卡②，此处只声明编译面）
    implementation("com.ezylang:EvalEx:${evalExVer}")
    compileOnly("org.jetbrains:annotations:${jetbrainsVer}")
    // 上游源码用 lombok（@Setter/@Getter/@Cleanup 等，lombok.config 随仓）
    compileOnly("org.projectlombok:lombok:${lombokVer}")
    annotationProcessor("org.projectlombok:lombok:${lombokVer}")

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

// zip64：datagen 资产树并入后条目量级与 mdk 同（卡②打包域先占位，mdk 先例 65535 上限）。
tasks.named<Jar>("jar") {
    setZip64(true)
}

// chisel 生成源接线（mdk 同构）：非活动节点工件任务先于 stonecutterGenerate；
// 本节点=活动节点（原位编译），此接线为对称占位（活动节点 stonecutterGenerate 为恒等/近恒等处理）。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
