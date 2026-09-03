// 1.21.1 NeoForge 节点构建脚本（W1 POC 段：只求管线走通到已知 API 失败清单，编译红允许）。
// 1.21.1 必 moddev（LEGACY.md:2-13：legacyforge 插件上限 MinecraftForge 1.20.1）。
// 证据：tmp/harvest/stonecutter-template/build.neoforge.gradle.kts.txt:1-46（neoForge 块/runs/mods register）。
// 不做（W2 段）：neoforge.mods.toml 双模板、JEI 坐标、runs 类路径补挂、mod* 重映射核对。

plugins {
    id("net.neoforged.moddev")
}

group = "gregtech6"
version = property("mod_version").toString()

val modId = property("mod_id").toString()

// 1.21.1 节点 = Java 21（本机 java-21-openjdk；ADR-P15-2：gregapi 钉 17，17 产物可被 21 工具链直接消费）
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
}

neoForge {
    version = property("deps.neoforge").toString()

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("run/")
        }
        register("server") {
            server()
            gameDirectory = file("run/")
        }
    }
}

dependencies {
    // gregapi 挂法 L2：根项目 java-library（Java17 产物）
    implementation(project(":"))
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// datagen 产物默认共享（POC 初步结论）：与 1.20.1 节点同读 mdk/src/generated/resources；
// 若两节点产物 diff → W2 切节点子目录（模板 build.neoforge.gradle.kts.txt:45 先例）。
sourceSets["main"].resources.srcDir(parent!!.projectDir.resolve("src/generated/resources"))

// chisel 生成源接线（模板 build.neoforge.gradle.kts.txt:53-55 同构）
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}
