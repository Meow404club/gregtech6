# POC：Java21 工具链编译 mdk 源（1.20.1 Forge leg 失败面实测）

- 日期：2026-09-03
- 任务卡：p15-poc-java21-toolchain（P15 跨版本 W1 POC 波，裁定 decisions.2026-09-03-p15-crossversion-arch q2）
- 性质：试验数据记录。**本卡 build 行改动不合 main**（worktree 分支 work/p15-poc-java21-toolchain 上 mdk/build.gradle 仍是试验态）；本文件是唯一合入面。
- 先例证据：tmp/harvest/stonecutter-template/build.forge.gradle.kts.txt:83-89 —— Stonecutter 模板对 >=1.20.5 节点用 JavaVersion.VERSION_21、1.18~1.20.x 用 VERSION_17 双工具链并存；即 1.21.1 leg 用 Java21 编译是模板实证过的既有形态。

## Context

P15 拍板双节点矩阵：1.20.1 Forge（现役）+ 1.21.1 NeoForge（目标）。1.20.5+ 的 Minecraft 字节码与工具链要求 Java21，因此 1.21.1 leg 必然要 javac21；而本仓根 gregapi 钉 Java17（红线 ADR-P15-2，零 MC 依赖）。开放问题：**mdk 源在 javac21 下有多少编译失败面？Java21 工具链对 1.20.1 Forge 的 build/test/run 链路有无破坏？** 本 POC 用一行试验改动（mdk/build.gradle:26-30 toolchain 17→21）实测回答，不预设结论。

试验环境：Gradle wrapper 8.14（build.gradle 根 settings 不动）、MDG legacyforge 2.0.144、Forge 1.20.1-47.4.10、基线 main a65576b1（1173 测 = 根 205 + mdk 968）、系统 JDK 17.0.20.1（默认）/ 21.0.12.1（/usr/lib/jvm/java-21-openjdk）。

## 试验内容

worktree 内唯一构建行改动（diff 全量 1 行语义变更）：

```diff
 java {
     toolchain {
-        languageVersion = JavaLanguageVersion.of(17)
+        languageVersion = JavaLanguageVersion.of(21)
     }
 }
```

根项目 build.gradle:11-12 toolchain 17 未动（gregapi 仍 javac17 产物）；mdk 经 `implementation project(':')` 消费根 jar，Java21 编译器消费 Java17 classfile 上游兼容。

## 数据三件套

### ① javac21 编译错误清单：**0 条**

- `./gradlew --no-build-cache :mdk:build`：BUILD SUCCESSFUL in 3m 19s，10 actionable tasks 全 executed。
- mdk main + test 源集在 javac21 下零错误（mdk src 未做任何改动，纯工具链切换）。
- 警告面与 Java17 时代同类，均为上游 API 弃用告警而非语言级错误：`FMLJavaModLoadingContext.get()` 待删除（GT6Mod.java:44）、`ResourceLocation(String,String)` 构造器待删除（GTFluidPipeFlowModel.java:59 / GTOvenOverlayModel.java:212 / GTPipeFlowClientListener.java:43 / GTWrenchGridRenderer.java:211 / GTWireBakedModel.java:226,231,233 等）。javac21 未新增任何语言级报错/新告警类别。
- 硬证据：产物 class 文件 major version = **65**（Java21）——编译确实发生在 javac21，而非 17 回退。

### ② 测试通过数：**968/968 全绿，0 失败 0 跳过**

- 口径：mdk/build/test-results/test/*.xml（JUnit XML）118 个 suite 汇总 tests=968 failed+errors=0 skipped=0。
- 与基线 mdk 968 完全一致（根 gregapi 205 未在本卡跑，其工具链未动）。
- 测试 JVM 亦运行在工具链 21 下（Gradle test JavaExec 默认取 toolchain），材料全量 init（maxHeapSize 2g）路径无回归。

### ③ runServer 冒烟：**通过，GT6 ERROR = 0**

- 启动：`nohup ./gradlew :mdk:runServer`，Done (2.309s)! （对比 Java17 时代历史值 3.5s~16.9s，属正常波动偏快侧，主因 warm daemon+caches，不构成 Java21 加速结论）。
- 日志全量 ERROR 计数 = 1，唯一一条为 vanilla `DedicatedServerProperties: No key layers in MapLike[{}]`（vanilla 数据包层配置解析常态，非 GT6）；GT6 域 ERROR = 0。
- GT6 加载证据：/gt6machine、/gt6oven、/gt6multiblock、/gt6tank、/gt6pipe、/gt6chest、/gt6cover、/gt6energy、/gt6boiler、/gt6burner、/gt6engine、/gt6gui 全部命令注册 INFO 齐全（P13/P14 验收命令全数在位）。
- 收尾：SIGTERM 按 PID 杀 forked 服务端进程，Stopping server → 三维 Saving chunks → exit value 143（=SIGTERM，预期）；Gradle 侧 BUILD FAILED 仅由该 kill 的非零退出引起，非工具链故障。

### 工具链下 run 类路径行为（runServer 实测）

- **runs 的 JVM 也吃 toolchain**：forked 服务端进程 exe = `~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2/bin/java`——Gradle 工具链自动供给（auto-provisioning）下载了 Adoptium 21（未优先采用 /usr/lib/jvm/java-21-openjdk，探测/供给顺序以 Gradle 为准），而 Gradle daemon 本身仍跑默认 JDK 17。即一行 toolchain 改动同时切换了编译与 runs 两处 JVM。
- **Forge 1.20.1 链路在 Java21 下无版本拦截**：BootstrapLauncher 1.1.2 / ModLauncher 10.0.10 / SecureJarHandler 2.1.10 / Mixin 0.8.5 全链在 Java21 启动成功，无 Unknown class file version 或 module 冲突。
- **run 类路径形态与 Java17 时代一致**：`-DlegacyClassPath` exploded 形态 = mdk build/classes/java/main + build/resources/main + forge-1.20.1-47.4.10.jar + client-extra + intermediateToNamed.zip（MDG userdev 工件）+ DevLaunch-1.0.2 + **根 gregtech6-1.0.0-SNAPSHOT.jar**（build.gradle:74 additionalRuntimeClasspath 生效）+ jei-1.20.1-forge-15.56.0.205（modRuntimeOnly 重映射产物可见）+ fml/mixin/asm/nashorn 等运行时依赖。Java17 时代的 run 类路径机制在 toolchain 21 下零行为差异。

## Decision

1. **结论（回答任务卡验收问题）："1.21.1 leg = Java21 编译 mdk 源的失败面 = 零"。** mdk 全部 main+test 源在 javac21 下零编译错误、968 测全绿、1.20.1 runServer 在 Java21 JVM 下冒烟通过。1.20.5+ 用 Java21、1.18~1.20.x 用 Java17 的双工具链并存形态（Stonecutter 模板 :83-89 同款）对本仓成立，无代码级障碍。
2. 本卡 build 行改动不合 main：main 维持 mdk toolchain 17。1.21.1 leg 落地时由骨架卡（W2，构建脚本全域独占）按 Stonecutter 节点参数化引入 per-node 工具链，而非全局翻 21。
3. gregapi 钉 Java17 红线（ADR-P15-2）不受影响且被本次实测进一步支持：Java21 消费 Java17 classfile（根 jar 进 run 类路径并正常加载）向上兼容成立，gregapi 无需跟随升工具链。
4. 骨架卡可复用的两个操作事实：(a) toolchain 21 下 Gradle 会 auto-provision `~/.gradle/jdks/eclipse_adoptium-21-*`，不需要系统预装；(b) runs 与 test 的 JVM 随 toolchain 走，双节点若 per-node 配 toolchain，runServer/runClient/runData 在 1.20.1 leg 仍应钉 17 口径（本 POC 证明 21 也能跑，但等价永绿红线倾向保守）。

## Consequences

- 正面：P15 骨架卡的最大不确定性（mdk 源的 Java21 失败面）清零，1.21.1 迁移波可按"零编译债"起点规划；双工具链并存有本地全链路实证（build+test+run 三面）。
- 风险/边界：
  - 本 POC 只覆盖 1.20.1 leg 在 Java21 下的行为；1.21.1 NeoForge leg 的 javac21 真实失败面（DataComponents/item NBT 载体等 API 变化）是**源码迁移**问题，不在本卡范围（见 arch 卡影响面清单）。
  - javac21 零错误不等于 1.20.1 生产可切 21：`ResourceLocation(String,String)` 等 [removal] 告警在 21 下依旧存在，且 Forge 1.20.1 官方口径仍是 Java17；main 维持 17 是保守正确解。
  - runData 未在本卡跑（不触 DataGen 产物，FILES_SCOPE 之外）；骨架卡落地双节点后须按 M5 门禁补 runData 双幂等验证。
- 采纳/否决记录：否决"POC 顺带把 mdk toolchain 全局升 21"——等价永绿红线（ADR-P15-1）要求 1.20.1 leg 构建行为不变；toolchain 是 per-node 参数化决策，归骨架卡。
