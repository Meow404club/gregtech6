# FORK NOTICE — vendored ModularUI fork（本目录是 fork，非原版源码）

本目录是 [brachy84/ModularUI-Modern](https://github.com/brachy84/ModularUI-Modern) 的**仓内自维护
跨版本 fork**（用户裁定 2026-09-06，ADR：`docs/adr/2026-09-06-p20-modularui-fork-ruling.md`）。
本仓含修改（**contains modifications**）；逐文件偏离台账见 [DIVERGE.md](DIVERGE.md)，
上游身份与基线见 [THIRD_PARTY.md](THIRD_PARTY.md)。许可证 LGPL-3.0（[LICENSE](LICENSE)，原文未删改）。

## 1. fork 形态（单树 + 双腿）

- **主树（共享）** `src/main/` = 上游 1.21.1 分支源码原文（common 交集 371 文件：232 零改 + 7 注释形 + 132 `//?` 分叉）。
  条件编译语法为 Stonecutter chisel（`//? if neoforge { ... //?} else { /*forge 形*/ //?}`），
  由本仓 Gradle 构建（root `settings.gradle.kts` P20④ 段 + `stonecutter.gradle.kts` swap 表）双腿消化。
- **单腿专属** `src/neoforgeMain/`（141 java = 仅 1.21.1 存在的 29 + leg-split 112）/ `src/forgeMain/`
  （145 java = 仅 1.20.1 存在的 33 + leg-split 112）+ 各自 mixin json / atlas / injected_interfaces，
  按节点并入 main sourceSet，零 chisel 语法。leg-split = 结构性漂移文件（漂移率 ≥0.15 或 hunk ≥10）
  整体按腿放原文，避免 hunk 分叉把方法体切成碎片。
- 包名保持上游原样（`brachy.modularui.*`）——下游 API 兼容面（GT6 经 jarJar 消费，卡②）。

## 2. 主树方向实测反选（ADR §5 留白项的落地理由）

主树选 **1.21.1@c13e141（NeoForge 原生）**，1.20.1 为回向腿。实测依据（双腿快照 diff，2026-09-06）：

1. 双腿 common 交集 483 文件中 232 文件字节级零 diff——方向对称，回向工作量与正向相同；
2. 1.21.1 腿接入本仓 moddev 2.0.144 工具链**首编即绿**（唯一缺口 = JEI impl 进编译类路径），
   实证 ADR §5「1.21.1 腿≈零移植」；
3. 上游活跃分支 = 1.21.1（protected，最新提交）；跟随上游 = 重 vendored 1.21.1 + 重放 DIVERGE.md，
   主树与活跃分支同形使重放成本最小；
4. 回向腿的机械缝（包名 swap）与 `//?` 分叉均为声明式，可由 `tools/gen-forks.py` 从双腿快照再生成；
   结构性漂移文件（112）leg-split 双树原文，跟随上游 = 整文件重拷。

## 3. 接线定形（ADR 裁决一留白项：stonecutter 多项目 vs mdk 第二 sourceSet）

**probe 结论：stonecutter 多项目**（root settings 双 `create()`，本目录自带控制器 + 双节点 +
独立 buildscript）。落选方案与理由：

- **mdk 第二 sourceSet**：mdk 的 swap 表方向为 forge→neoforge（GT6 源是 forge 形），vendored 主树
  是 neoforge 形——同表同源集方向相反必互撞；且 modularui 类进 mdk jar 会与卡② 的独立 mod jar
  形成 split-package 双装（FML module 冲突）。落选。
- **普通子项目（无 stonecutter）**：无 `//?`/swap 消化机制，只能双份源树 = fork 失去单树语义。落选。

probe 成本：settings.gradle.kts +14 行（第二 `create()`）；任务路径
`:third-party:modularui:1.20.1-forge:compileJava` / `:third-party:modularui:1.21.1-neoforge:compileJava`。

## 4. 编译门禁（铁律 id327：双腿分开命令，严禁同一调用混跑）

```bash
./gradlew :third-party:modularui:1.21.1-neoforge:compileJava --no-build-cache   # 活动节点（原位编译）
./gradlew :third-party:modularui:1.20.1-forge:compileJava --no-build-cache      # 回向腿（swap+//? 处理后）
```

## 5. 跟随上游（ADR §2 对冲姿势）

跟随事件 = GTCEu 换钉版（监视 maven.gtceu.com 两 artifact 的 maven-metadata.xml）。动作 =
重 vendored 新基线 + `tools/gen-forks.py` 从双腿快照重生成 `//?` 分叉 + 重放 DIVERGE.md 手写偏离。
无 git merge（两仓无共同历史语义）。
