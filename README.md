# GregTech 6 Modern

[![Build](https://github.com/Meow404club/gregtech6/actions/workflows/build.yml/badge.svg)](https://github.com/Meow404club/gregtech6/actions/workflows/build.yml)

GregTech 6 的非官方现代版移植。原作是 Minecraft 1.7.10 上经典的硬核科技生存 mod，本仓库把它带到了 **Minecraft 1.20.1（Forge）** 和 **Minecraft 1.21.1（NeoForge）**，两个版本都在持续维护。

当前版本 v0.1.0，主线玩法可以完整走通。想直接开玩请跳到[下载与安装](#下载与安装)，想了解移植了哪些内容请往下看。

## 这是个什么样的 mod

GregTech 6 是一套把 Minecraft 生存彻底"工业化"的玩法：从烧锅炉起步，一路发展到聚变反应；矿不是挖到就完事，而是要经过一整套处理流程；机器讲究电压匹配、热量管理和结构搭建。节奏慢、上手陡，但每一步都有回报——如果你喜欢把生产线越铺越大，这个 mod 就是为你准备的。

### 游戏内容一览

移植完整保留了 GregTech 6 的原版体验，包括但不限于：

- **从蒸汽时代到终极电压**：开局靠燃烧室烧锅炉、用蒸汽引擎顶起第一批机器，之后逐步进入电力时代。从超低压（ULV）到终极电压（UV）共九档电压，每一档都有匹配功率的机器、发电机和变压器。机器超载会炸，量入为出是必修课。

- **庞大的材料与矿物系统**：上百种金属、合金、宝石与化学品。矿脉埋在世界各处，同一种矿会以多种基底岩石的形态出现（花岗岩、闪长岩、安山岩、玄武岩等），地表还能捡到对应岩石的小碎块。

- **完整的矿石处理链**：矿石要经过破碎、洗净、筛选、熔炼等多道工序，每一步都会产出副产品——坏的矿石直接烧锭是极大的浪费，搭好处理线收益翻几倍。

- **多方块大型机器**：焦炉、大型锅炉、坩埚、蒸汽轮机、聚变反应堆、内爆压缩机、质量发生器、避雷针、矿脉钻井……按图纸搭好结构、点燃控制器才能开工。游戏内 JEI/EMI 的信息页附有多方块搭建说明。

- **管道与物流**：物品、流体、电力各有专属管线，远距离管道负责跨区域传输，物流核心承担分拣与调度。

- **世界生成**：矿脉、大型矿脉、基岩矿按规则埋藏在世界各处，野外还有蜂巢、倒木等小惊喜等着探索。

- **工具与机器交互**：扳手、锤子、螺丝刀、放大镜等一整套专属工具，用来旋转、拆卸、给机器调速，或加装覆盖板（cover）改变面板行为。顺手还可以养蜜蜂。

- **查询与联动**：完整支持 JEI 与 EMI 查看配方和材料树；装了 Jade 的话，准星对着机器就能看到储量和运行状态；进阶玩家可以用 KubeJS 修改或新增配方。

- **中英双语**：内置简体中文与英文，游戏内文本齐全。

### 给新玩家的建议

- 别急着挖矿——先搭蒸汽设备攒出第一台电力机器，前期节奏慢是正常的；
- 善用 JEI/EMI：配方、材料树、矿石生成分布、多方块结构说明都能在里面查到；
- 矿石处理链值得尽早建，副产品攒起来是后期的重要原料；
- 机器不出力时，用 Jade 或鼠标悬停看看它缺什么——多数情况下是缺电、缺水或堵了输出。

## 下载与安装

### 下载

- **正式版**：到 [Releases](https://github.com/Meow404club/gregtech6/releases/latest) 下载与游戏版本对应的 jar；
- **开发版**：main 分支每次合入新代码都会自动发布一个标记为 Pre-release 的 Development build，同样在 [Releases](https://github.com/Meow404club/gregtech6/releases) 页面。适合想尝鲜的玩家，但不保证稳定。

jar 命名规则是 `gt6-<游戏版本>-<加载器>-<版本号>.jar`，例如 `gt6-1.20.1-forge-0.1.0.jar`，按名字对号入座即可。

### 安装

1. 先按下面的版本表安装对应加载器：Forge 去官网[文件页](https://files.minecraftforge.net)下载安装器，NeoForge 去官网[下载页](https://neoforged.net)；
2. 把下载的 jar 放进 `.minecraft/mods/` 目录；
3. 完成，进游戏开玩。本 mod 的 mod id 是 `gt6`。

### 版本支持

| Minecraft | 加载器 | Java |
|-----------|---------------------|------|
| 1.20.1 | Forge 47.4.10 | 17 |
| 1.21.1 | NeoForge 21.1.249 | 21 |

### 常见问题

**和 GregTech CEu Modern 是什么关系？**

没有关系。GregTech CEu Modern 是另一个独立的移植项目；本仓库移植的是 Gregorius Techneticies 的原版 GregTech 6（1.7.10），两者各有侧重，可以按喜好选择。

**需要装前置 mod 吗？**

不需要。JEI/EMI、Jade、KubeJS 都是可选的联动 mod，装了有额外功能，不装也不影响游戏本体。

**配方和多方块结构在哪查？**

装上 JEI 或 EMI 后即可查看配方；材料树、矿石生成分布、焦炉等多方块的结构说明在 JEI/EMI 的信息页（info 页签）里，游戏内都能翻到。

**两个游戏版本内容一样吗？**

两条版本线共用同一套代码，游戏内容一致，按你的客户端或整合包版本选就行。

**jar 能混着装吗？**

不能。1.20.1 和 1.21.1 的 jar 各自只适配对应加载器，客户端和服务端都要用同一条版本线的 jar。

## 反馈问题

遇到 bug 或者有新想法，欢迎[提 issue](https://github.com/Meow404club/gregtech6/issues/new/choose)。请使用仓库自带的模板（bug 反馈 / 功能建议 / RFC 提案），标题带 `[Bug]` / `[Feat]` / `[RFC]` 前缀，并写清游戏版本、加载器和复现步骤。优先级标签由维护者评估后添加。

## 参与开发

Issue 与 PR 均欢迎（中英文皆可）。

- **环境**：两条版本线各需一套 JDK——1.20.1 线用 Java 17，1.21.1 线用 Java 21，Gradle 不会自动下载，两套都要装好；
- **克隆**：仓库带 submodule，用 `git clone --recurse-submodules` 拉取，或克隆后补一句 `git submodule update --init`；
- **构建**：两条版本线分开构建：

  ```bash
  ./gradlew :mdk:1.20.1-forge:build      # 产物在 mdk/versions/1.20.1-forge/build/libs/
  ./gradlew :mdk:1.21.1-neoforge:build   # 产物在 mdk/versions/1.21.1-neoforge/build/libs/
  ```

- **结构**：根目录 `src/` 是上游 1.7.10 gregapi 的逐字移植（API 层），`mdk/` 是面向现代加载器的实现层，`third-party/modularui/` 是随 jar 分发的 GUI 框架 fork；
- **测试**：提交前请跑通所改版本线的 `./gradlew :mdk:<版本线>:test`；
- 进度、架构决策等工程细节见 [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) 与 [docs/adr/](docs/adr/)。

## 致谢与声明

本仓库是 **非官方移植**，与 GregTech 6 原作者 Gregorius Techneticies 及 GregTech-6 Team 无任何隶属关系，本项目的问题请勿向上游反馈。

感谢：

- Gregorius Techneticies 与 GregTech-6 Team —— GregTech 6 原作；
- brachy84 —— [ModularUI-Modern](https://github.com/brachy84/ModularUI-Modern) 作者（本仓 GUI 框架 fork 的上游）；
- GTCEu Modern 团队 —— 现代 Minecraft 实现的重要参考。

## 许可证

- 代码：[LGPL-3.0-or-later](LICENSE)，与上游一致；
- 资产：CC0-1.0（跟随上游惯例）；
- 上游 GregTech logo 及其衍生资产（CC BY-NC 4.0）未随本仓分发；
- 第三方组件清单见 [NOTICE.md](NOTICE.md)。
