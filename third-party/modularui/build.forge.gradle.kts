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
    // refmap/mod 元数据接线（mixin{} 块、mods.toml）= 卡② packaging 域。
    annotationProcessor("org.spongepowered:mixin:${mixinVer}:processor")
    compileOnly("io.github.llamalad7:mixinextras-common:${mixinExtrasVer}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${mixinExtrasVer}")

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
}

tasks.named<Jar>("jar") {
    setZip64(true)
}

// chisel 生成源接线（mdk 同构）：本节点非活动节点，工件任务须等 stonecutterGenerate 产出处理后源。
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
