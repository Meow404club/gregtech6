# GregTech 6 (Modern Port)

[![Build](https://github.com/Meow404club/gregtech6/actions/workflows/build.yml/badge.svg)](https://github.com/Meow404club/gregtech6/actions/workflows/build.yml)
<!-- 构建徽章为占位：CI workflow（build.yml）合入后此徽章即自动生效。 -->

GregTech 6（Minecraft 1.7.10 大型科技向 mod）向现代 Minecraft 的移植版：Forge 1.20.1 与 NeoForge 1.21.1 双发布腿，Stonecutter 单树构建。当前版本 0.1.0（早期发布）。

## 项目定位

本仓库是 GregTech 6 的**非官方**现代移植（unofficial port），与原作者 GregoriusT 及 GregTech-6 Team 无隶属关系。结构上分为两层：

- `gregapi`（根项目）：上游 GregTech 6 1.7.10 的 gregapi 源码逐字移植，保持 java-library 形态；
- `mdk`：面向现代加载器的重写实现（Forge 1.20.1 / NeoForge 1.21.1），经 Stonecutter 0.7 单树双腿构建；
- GUI 框架使用自维护的 ModularUI fork（`third-party/modularui`，git submodule）。

## 版本支持矩阵

| 发布腿（构建节点） | Minecraft | 加载器 | Java | 产物 |
|---|---|---|---|---|
| `1.20.1-forge` | 1.20.1 | Forge 47.4.10 | 17 | `gt6-1.20.1-forge-0.1.0.jar` |
| `1.21.1-neoforge` | 1.21.1 | NeoForge 21.1.249 | 21 | `gt6-1.21.1-neoforge-0.1.0.jar` |

## 当前状态

移植完成度（P36 census 定版口径，统计日期 2026-09-24，池清偿进行中）：

- 内容完成度 ≈ 97-98%；
- 可玩完成度 ≈ 93-94%（主链可通，剩余为池化旁支与个别未移植面）。

移植按阶段推进：材料系统 → 注册表 + DataGen → BlockEntity/Menu 框架 → 管线/Cover/多方块渲染。当前处于 P37 生产发布与池清偿期，P38 起转入维护期。进度细节见 [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md)。

## 安装

1. 按上表安装对应加载器（Forge 或 NeoForge）；
2. 从 Releases 取与游戏版本匹配的发布腿 jar，放入 `mods/` 目录；
3. mod id 为 `gt6`。

### KubeJS 适配

配方可经 KubeJS 以 JSON 直接灌入配方图（reload 缝）；`gt6.integration.kjs` 绑定模块为 runtime-optional——未安装 KubeJS 时零影响。

## 从源码构建

前置：

- JDK：forge 腿 toolchain 固定 Java 17，neoforge 腿固定 Java 21（Gradle 按腿选择已安装的 JDK，不自动下载，两套 JDK 需备齐）；
- submodule：`git clone --recurse-submodules`，或克隆后执行 `git submodule update --init third-party/modularui`。

构建（两条发布腿必须分两次 Gradle 调用）：

```bash
./gradlew :mdk:1.20.1-forge:build
./gradlew :mdk:1.21.1-neoforge:build
```

测试：

```bash
./gradlew :test                          # gregapi 材料系统（根项目）
./gradlew :mdk:1.20.1-forge:test         # 各腿测试（节点名见支持矩阵）
```

构建产物位于 `mdk/versions/<node>/build/libs/`。

## 目录导航

| 路径 | 内容 |
|---|---|
| `src/` | gregapi 根项目（上游 1.7.10 gregapi 逐字移植） |
| `mdk/` | 现代实现 + Stonecutter 0.7 双腿构建脚本与节点 |
| `third-party/modularui/` | ModularUI fork（git submodule，LGPL-3.0） |
| `docs/` | [PROJECT_STATE.md](docs/PROJECT_STATE.md)、[TODO.md](docs/TODO.md)、[adr/](docs/adr/)（架构决策记录） |
| `tools/` | 开发工具链：[tools/rcon/](tools/rcon/)（RCON 测试链）、[tools/gt6_rag/](tools/gt6_rag/)（源码检索库）等 |

## 参与贡献

Issue 与 PR 均欢迎（中英文皆可）；工程约定与流程见 [docs/](docs/)。提交前请跑通所改发布腿的 `:mdk:<node>:test`。

## 许可证

- 代码：GNU LGPL-3.0-or-later，全文见 [LICENSE](LICENSE)；
- 资产：CC0-1.0（跟随上游 `LICENSE.assets` 惯例）；
- 未引入上游 GregTech logo 及其衍生（CC BY-NC 4.0，明确排除，本仓不携带该许可资产）；
- 第三方组件台账见 [NOTICE.md](NOTICE.md)。

## 致谢

- Gregorius Techneticies 与 GregTech-6 Team —— GregTech 6 原作；
- brachy84 —— ModularUI-Modern 上游（本仓 fork 见 [third-party/modularui](third-party/modularui/)）；
- GTCEu Modern 团队 —— 现代 Minecraft 加载器实现的重要参考。
