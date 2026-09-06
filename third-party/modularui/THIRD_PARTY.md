# THIRD PARTY — ModularUI（vendored fork 声明）

## 上游身份

- **项目**：ModularUI-Modern（"Port of ModularUI to modern Minecraft versions"）
- **上游仓库**：https://github.com/brachy84/ModularUI-Modern
- **作者/版权**：brachy84 及上游贡献者（jar 元数据 Specification-Vendor: brachy，沿用上游
  `gradle/scripts/jars.gradle:20-27` 姿势）
- **vendored 基线（双分支，per-MC）**：
  - 主树：分支 `1.21.1` @ commit **c13e141**（protected，2026-09-06 快照收割）
  - 回向腿参照：分支 `1.20.1` @ commit **909cda2**（protected，同日快照；单腿文件与 `//?` 分叉
    forge 腿内容取自该快照）
- **依赖链佐证**：GTCEu Modern 双腿同钉 `brachy.modularui:modularui-mc1.20.1` /
  `modularui-mc1.21.1` = 3.3.1-SNAPSHOT（GTCEu gradle/forge.versions.toml:10,62；
  maven.gtceu.com latest 同版）——本 fork 上游身份与 GTCEu 消费面同源。

## 许可证

- 上游许可证 **LGPL-3.0**，全文随本目录 [LICENSE](LICENSE) 保留，未删改（165 行）。
- 本 fork **包含对上游的修改**（contains modifications）——逐文件清单见 [DIVERGE.md](DIVERGE.md)；
  除列明偏离外，`src/main` 零改文件与上游 1.21.1 基线字节级一致（`src/neoforgeMain`、`src/forgeMain`
  为上游对应分支的原文）。
- 本仓主 mod（Combined Work）自身许可依 LGPL-3.0 §4 不受传染；分发时本声明 + LICENSE 副本
  随附，modularui 子 jar 保留来源标识元数据（卡② packaging 落地）。
- 对应源义务：本仓公开仓内即含完整可构建源（本目录），满足 LGPL §4/§6。

## 修改声明（census 摘要，2026-09-06 基线）

| 类别 | 文件数 | 说明 |
|---|---|---|
| 双腿字节级零改（src/main） | 232 | 与两腿快照均一致 |
| 仅注释形状漂移（src/main，1.21.1 形保留） | 7 | compile-neutral（DIVERGE.md §4） |
| `//?` hunk 分叉（src/main） | 132 | 逐文件台账 DIVERGE.md §2（含 1 手工 hunk §3） |
| leg-split 整体按腿（双树原文） | 112 | 结构性漂移（DIVERGE.md §5） |
| 仅 1.21.1 存在 | 29 | `src/neoforgeMain`（原文） |
| 仅 1.20.1 存在 | 33 | `src/forgeMain`（原文） |
| vendored java 总计 | 657 | src/main 371 + forgeMain 145 + neoforgeMain 141 |

构建脚本（`build.*.gradle.kts` / `stonecutter.gradle.kts` / settings 接线 / 节点 gradle.properties）
为本仓原创（非上游文件），依赖钉值取自上游 catalog。
