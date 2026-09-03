// settings 石匠容器化定稿（W2 合入段）：ADR-P2-1 的「root gregtech6 + include 'mdk'」演进为
// 「root = gregapi java-library（build.gradle 零改动）+ mdk = Stonecutter 控制器子项目」。
// 钉版依据：state tmp.research.stonecutter-version-pin——钉 0.7（模板同款零翻译），
// 0.9.8 强制 Gradle 9 而 MDG 无 Gradle 9 声明，升版=独立卡；wrapper 8.14 零动。
// 证据：tmp/harvest/stonecutter-template/settings.gradle.kts.txt:14-30（0.7 + match 布局 + vcsVersion）。

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7"
}

rootProject.name = "gregtech6"

// gregapi 挂法 L2（W1 POC 钉版）：根项目保持 gregapi java-library 原样（build.gradle 零改动），
// 节点经 `implementation(project(":"))` 消费——禁 includeBuild 自指（复合构建指向本仓自身）。
include("mdk")

stonecutter {
    create(project(":mdk")) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { vers("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.20.1", "forge")
        match("1.21.1", "neoforge")

        // VCS 节点 = 现役版本：chisel 共享源在盘状态对应 1.20.1-forge。
        vcsVersion = "1.20.1-forge"
    }
}
