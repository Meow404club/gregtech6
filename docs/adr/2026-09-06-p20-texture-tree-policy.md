# ADR-P20 草稿：贴图真图/占位双树存放与优先级政策（P20 贴图上游填充 sprint 前置裁决）

- 日期：2026-09-06
- 状态：正典（P20 开段成文，主会话 promote）
- 关联：研卡 tasks.p20-research-texture-census（id333 勘误后口径）、ADR-P17（datagen 共享树）、
  ADR 2026-08-31-p8-prefixblocks（借图 CC0+sha256+小写化先例）、P19 bake_distillery_fronts.py（烘焙先例）
- 基线：main 50843dff

## 0. 问题陈述

P20 sprint 本质 = 占位图换上游真图（id333 勘误：无缺模 bug）。占位 PNG 现居两群：

1. **item material_sets 2785 张**：**恒在静态树** `mdk/src/main/resources/assets/gt6/textures/item/material_sets/`
   （git 跟踪，7c98d2ac 起；gen_textures.py:23 `DIR = mdk/src/generated/resources` 仅是 --scan/--verify
   的模型读面参数非输出树——初稿误读，2026-09-06 W1 审查实证勘误：main 现态=静态 2785/generated 0）。
2. **block/机器占位**：静态树 `mdk/src/main/resources/assets/gt6/textures/block/*.png`（脚本生成已提交，
   assets/README.md 逐条溯源）。

双腿把共享 generated 树**追加**为 main resources srcDir（build.forge.gradle.kts:158、build.neoforge.gradle.kts:194，
sharedDir=parent!!.projectDir :22；generateModMetadata 产物同法 :151）。追加序在默认 `src/main/resources` 之后。

## 1. 裁决

### 1.1 真图落点 = 静态树

上游借入真图（CC0）一律落 `mdk/src/main/resources/assets/gt6/textures/`，CC0+sha256 逐条进
assets/README.md+路径小写化（P8 :52-59、P19 bake_distillery_fronts.py:34,194-195 既有先例，不变）。

### 1.2 同路径跨树共存 = 禁止（核心裁决）

机制勘误（2026-09-06 W1 审查实证，merged 0ddd034c）：Gradle 8.14 无显式 duplicatesStrategy 下，跨树同
相对路径重复条目使 processResources **直接 fail-fast**（"Entry ... is a duplicate but no duplicate
handling strategy has been set"），非静默遮蔽——构建面本身即红线，「同路径跨树共存=禁止」裁决维持并
加固；census 碰撞钉（GT6TextureCensusTest）保留作不依赖构建配置的兜底。因此：

- 借图卡必须**同 commit**：静态树**原地替换**占位 PNG + assets/README.md sha256 行（占位与真图同路径
  同居静态树；`git rm` generated 不需要——generated 无同路径 PNG）。
- **否决**备选方案「调整 srcDir 挂载顺序让静态树赢」：依赖 Gradle 复制顺序语义、双腿两处维护、
  jar 重复条目风险——脆弱，不采纳。

### 1.3 gen_textures.py 让位 = skip-if-real

输出=静态树原地替换。写前 classify() 三态：byte-identical 于脚本占位输出→占位（可覆写）；非
identical→真图（**永不覆写，--force 亦然**，跳过并计数上报）；`--verify` 覆盖面=静态∪generated union。
**不建第二张排除表——静态树即排除表**（单一事实源，防未来回写覆盖真图）。

### 1.4 runData written:0 门禁不破

datagen 只写 JSON，PNG 增删不在 HashCache 面（P17 ADR 机制节 + P2→P20 占位 PNG 与 datagen 产物
共存至今的实证）。约束：census 钉数测试 = **纯 JUnit**，严禁做成 datagen provider 往 generated 树写文件。

## 2. 附带裁决

- **census 对账面数**：JUnit 面两面（generated 模型 layer0 ↔ main∪generated PNG 双向解析 + 跨树碰撞不变量）
  + README sha256 溯源自洽；上游面由 README sha256 表承载（JUnit 不得依赖 tmp/gt6-1.7.10 快照在场，
  coder worktree 与主盘 tmp 状态不保证一致）；jar 面归 p329 打包自包含守卫池，不进本 sprint。
- **overlay_active 16×64 四帧条带泛化**：P19 bake_distillery_fronts.py 的 src-over + FRAME 0 静态切片
  （:22-25）泛化为参数化 bake_machine_fronts.py（--machine group/name，h∈{16,64} 断言）；P19 脚本保留不动，
  distillery 三张产物 byte-identity（同确定性管线必须同 sha256）作新旧管线一致性验收。
- **机器机身键集**：全机器族共享 oven_bottom/top/side（GT6BlockStates.java:303-308 硬编码）；借图只需
  oven 三张 + 每族 front×3 态（base/_active/_running，:283-300）；port side 槽 ← 上游 colored/left.png。

## 3. Consequences

- 任何未来脚本向 generated 树写 assets 文件前必须过 skip-if-real；静态树成为真图唯一正典位。
- census 测试把 1.2 碰撞不变量钉成回归测试，遮蔽事故在 CI 面即红。
- P20 sprint 与 i18n A 波零 Java 文件冲突（P20 不触 GT6DataGenerators/GT6EnUs；唯一共享面
  assets/README.md append-only，合并序 W1→W2→W3 吸收）。

## 4. 勘误记录（2026-09-06，W1 审查 0ddd034c 三纠偏全部实证）

1. §0 第 1 条/§1.3：item material_sets 占位恒在静态树（初稿误读 gen_textures.py docstring 的 DIR
   参数）——W2 借图=静态树原地替换+README sha256 同 commit，无 git rm generated。
2. §1.2 机制句：Gradle 8.14 processResources 跨树重复条目 fail-fast 非静默遮蔽（负向注入实证）；
   政策不变并加固。
3. 集合数：COMBOS=2785 对/**40 集合**（上游 TextureSet.java 41 集合减未用 PLASMA；"37" 为 P3 期
   陈旧口径）。doc-only 遗留（W2 触碰时顺手修）：gen_textures.py docstring 与 GT6TextureCensusTest
   javadoc（:8/:134）残留"37 集合"文本、测试注释 ：67-68 "misc" 实为 PLASMA。
