// P20④ vendored ModularUI fork——Stonecutter 控制器脚本（第二 stonecutter 项目，
// 挂法同构 mdk/stonecutter.gradle.kts；settings.gradle.kts P20④ 段 create(project(":third-party:modularui")))。
//
// 主树方向（FORK.md §2 实测反选）：vendored 源 = 上游 1.21.1@c13e141 NeoForge 原生形，
// 活动节点（原位编译、零处理）= 1.21.1-neoforge；真工作量 = 1.20.1-forge 回向腿，
// 由两套机械消化：
//   1. 本文件 swap 表（regex + reverse 哨兵，仅 forge 节点正向 neoforge→forge）：
//      五缝中的纯包名带（distmarker/bus/items/fluids）+ vanilla 包移位（chunk.status）——
//      命中面 = 36 个 import-only diff 文件（src/main 共享树内，双腿字节级仅差 import/注释）；
//   2. src/main 树内 `//? if neoforge { ... //?} else { /*forge 形*/ //?}` 分叉（251 个
//      body-diff 文件，gen-forks.py 从两腿快照机械生成，逐文件台账 DIVERGE.md）。
// 单腿专属文件不进共享树：src/neoforgeMain（29 文件）/ src/forgeMain（33 文件）按节点
// 并入 main sourceSet（build.<loader>.gradle.kts），零 chisel 语法。
plugins {
    id("dev.kikugie.stonecutter")
    // 与 mdk/stonecutter.gradle.kts 同版成对（2.0.144）：节点 buildscript 无版本 apply 依赖本类路径。
    id("net.neoforged.moddev") version "2.0.144" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.144" apply false
}

// 活动节点 = vendored 主树（1.21.1 NeoForge 形）的原位编译者（与 mdk active=forge 镜像）。
stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    // 常量：forge 节点 `//? if forge` 真，neoforge 节点 `//? if neoforge` 真（mdk 同构）。
    constants.match(node.metadata.project.substringAfterLast('-'), "forge", "neoforge")

    // 卡① 审查遗留修正（2026-09-07）：原形 endsWith("forge") 对 "1.21.1-neoforge" 亦真——
    // 当时靠 neoforge=活动节点原位编译豁免无害；改 !endsWith("neoforge") 精确形，
    // 防 forge 侧预处理规则误命中 neoforge 节点（第三节点或主树方向翻转时即爆雷）。
    val forgeSide = !node.metadata.project.endsWith("neoforge")

    // ---- swap 表：仅 forge 节点正向执行 neoforge→forge（regex + reverse 永不匹配哨兵）----
    // 纪律同 mdk/stonecutter.gradle.kts：regex 一律字面量转义；哨兵 \u0000 使 forge 节点
    // 之外（neoforge=活动节点，本就原位编译）预处理绝对零变化。
    // 命中面普查（tmp/harvest 双腿 diff，2026-09-06）：src/main 共享树中双腿零 diff 的 232 文件
    // 不含任何 net.neoforged 引用；import-only diff 的 36 文件含且仅含下列五族包名 + chunk.status
    // 一处 vanilla 移位（JEI API 漂移例外走 //? 分叉，不入表）。
    val neverMatch = "\u0000stonecutter.never.matched\u0000"
    listOf(
        // 五缝③ distmarker（15 文件 import 行）
        "net\\.neoforged\\.api\\.distmarker\\." to "net.minecraftforge.api.distmarker.",
        // 五缝② bus（2 文件；"bus.api." 段界安全，不吃 "eventbus"——与 mdk 同款条目镜像）
        "net\\.neoforged\\.bus\\.api\\." to "net.minecraftforge.eventbus.api.",
        // 五缝④ items wrapper（6 文件）——wrapper 子路径先于 items 本体（长键先序）
        "net\\.neoforged\\.neoforge\\.items\\.wrapper\\." to "net.minecraftforge.items.wrapper.",
        "net\\.neoforged\\.neoforge\\.items\\." to "net.minecraftforge.items.",
        // fluids（4 文件 FluidStack）
        "net\\.neoforged\\.neoforge\\.fluids\\." to "net.minecraftforge.fluids.",
        // vanilla 包移位（1 文件）：1.21.1 ChunkStatus 迁 chunk.status，1.20.1 在 chunk 下
        "net\\.minecraft\\.world\\.level\\.chunk\\.status\\.ChunkStatus(?![A-Za-z_])" to "net.minecraft.world.level.chunk.ChunkStatus",
        // 1.20.1 与 1.21.1 ItemStack 同义改名（mdk swap 表同款条目的镜像方向）
        "(?<![A-Za-z0-9_])ItemStack\\.isSameItemSameComponents\\(" to "ItemStack.isSameItemSameTags(",
    ).forEach { (pattern, to) ->
        replacements.regex(forgeSide) {
            replace(pattern, to)
            reverse(neverMatch, "stonecutter.never")
        }
    }
}
