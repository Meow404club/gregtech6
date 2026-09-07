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

// ModularUI fork（P23 2026-09-07 拆独立仓，推翻 ADR-P20 §2 裁决一的 vendored 落点）：
// third-party/modularui/ 是 git submodule（独立仓，历史经 subtree split 保留，见 .gitmodules）；
// 接线零改动——本 include 与下行 stonecutter create 均路径型，gitlink 下照常解析。
// 纪律：每个 worktree 首次需 git -c protocol.file.allow=always submodule update --init
// （CVE-2022-39253 后本地路径协议默认拒）。
include("third-party:modularui")

stonecutter {
    create(project(":mdk")) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { vers("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.20.1", "forge")
        match("1.21.1", "neoforge")

        // VCS 节点 = 现役版本：chisel 共享源在盘状态对应 1.20.1-forge。
        vcsVersion = "1.20.1-forge"
    }

    // ModularUI fork 树（本卡 FILES_SCOPE；与 mdk 零依赖零任务耦合，编译门禁分腿跑：
    //   :third-party:modularui:1.20.1-forge:compileJava
    //   :third-party:modularui:1.21.1-neoforge:compileJava
    // 严禁同一 gradle 调用混跑双腿（lesson id327）。
    create(project(":third-party:modularui")) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { vers("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.20.1", "forge")
        match("1.21.1", "neoforge")

        // VCS 节点 = vendored 主树形态：主树选 1.21.1@c13e141 NeoForge 原生源
        // （fork 身份与选树理由见 third-party/modularui/FORK.md；偏离逐文件见 DIVERGE.md）。
        vcsVersion = "1.21.1-neoforge"
    }
}
