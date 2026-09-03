// mdk = Stonecutter 控制器项目：settings 阶段 create(project(":mdk")) 已把本项目的
// buildFileName 覆写为 stonecutter.gradle.kts（TreeBuilderImpl.createWith），
// 原 mdk/build.gradle 的内容按 loader 拆入 build.forge.gradle.kts / build.neoforge.gradle.kts。
// 控制器自身不可 build（StonecutterControllerImpl.configureProject 对 java 插件告警）。
// chisel 共享源 = 控制器目录下的 src/（StonecutterBuildImpl.createProcessingTasks：
// parent.fileTree("src/<sourceSet>")）——即 mdk/src 原位共享，mdk 源零移动挂双节点。

plugins {
    id("dev.kikugie.stonecutter")
    // apply false 只进类路径：节点 buildscript（buildFileName = ../../build.<loader>.gradle.kts，
    // 落在 mdk/ 下）可无版本 apply。legacyforge 与 moddev 同版本成对发布；
    // 2.0.144 = 本仓现役 legacyforge 值（mdk/build.gradle:3，Gradle 8.14 本地实证）。
    id("net.neoforged.moddev") version "2.0.144" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.144" apply false
}

// 活动节点 = 共享源的在盘编译者（configureSource 对 active 节点直接 srcDir mdk/src 原位编译）。
stonecutter active "1.20.1-forge"

// chisel 常量：按节点名后缀给 forge/neoforge 常量（注释分叉用 `//? if forge {` / `//? if neoforge {`）。
// 证据：tmp/harvest/stonecutter-template/stonecutter.gradle.kts.txt:12-15 同构
// （ConstantContainer.match：逐候选 put(name, name == sample)，Containers.kt:13）。
stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "forge", "neoforge")
}
