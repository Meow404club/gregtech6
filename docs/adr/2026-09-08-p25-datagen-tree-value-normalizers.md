# ADR：datagen_tree_check 两层归一——VALUE_NORMALIZERS 值形层（ADR-P17-1 延伸）

日期：2026-09-08 ｜ 前置：ADR-P17-1 datagen 双树裁决（docs/adr/2026-09-05-p17-datagen-tree.md，门禁步 5＝双树 1:1 断言） ｜ 评审基线 main=38365b89 ｜ 决策卡 state decisions.p25-datagen-tree-check-value-normalizer ｜ 实现卡 tasks.p25-datagen-tree-check-unify（merged b17a7a66，2 提交 349589a6＋080cf42f，S1 通过，mdk 零触碰）

- 状态：Accepted（P25 收官主会话正典化 2026-09-08）

## 0. 摘要

ADR-P17-1 的双树断言在 P24 起出现结构性缺口：SEGMENT_MAP 只映射目录段、内容比对逐字节，1.20.1 正典树 vs 1.21 节点输出的「值形」版本斜坡（loot 函数改名、ItemPredicate 形变、codec 重构）无法归一，只能计 227→228 恒红基线——狼来了噪声，真新增差会被淹没。定案＝双层归一：层 1 SEGMENT_MAP 语义不动；层 2 新增 VALUE_NORMALIZERS 按产物带注册 JSON 值形变换（方向恒 node→canonical，正典树永不改写），fail-visible 不放松。恒红基线归零（228 normalized 全收账）。

## 1. Context

- 缺口三证：SEGMENT_MAP 只含目录段映射（tools/datagen_tree_check.py:83-90）；文件名段不参与归一；内容比对逐字节（层 1 时代主判定）。值形斜坡实样：`copy_nbt`↔`copy_custom_data`（1.20.1 vanilla CopyNbtFunction.java:28/:37 `LootItemFunctions.COPY_NBT="minecraft:copy_nbt"` vs 1.21.x 改名）、ItemPredicate `"items"` 单串↔数组、`"items":"#ns/tag"`↔`"tag":"ns/tag"`、enchant predicate `predicates` 映射重构、recipe result `id`↔`item`、tag namespace `data/c`↔`data/forge`。
- 基线演化：P24 prefix 卡 gates 记 227 恒红（tasks.p24-tags-prefix-materials.gates）→P25 census 实测 228（+1＝spray_can_empty shaped 配方 show_notification 形，末波新增）。
- 替代案否决链（decisions 卡 alternatives）：a) 保持基线计数（恒红噪声，否）；b) 全局 JSON 语义比较（无声收窄 byte-exact 纪律、吞未注册形差，违 fail-visible，否）；c) 正典树重生成 1.21 形（翻 ADR-P17-1 形态钉，否）；d) 双层归一＋计数可见（选定）。

## 2. Decision

### 2.1 双层归一结构（tools/datagen_tree_check.py）

- **层 1＝SEGMENT_MAP 路径归一**（:83-90，六对目录段：loot_table/recipe/item/advancement/c/block→复数与 forge），语义不动。
- **层 2＝VALUE_NORMALIZERS 值形归一**（:92 起注册区；变换函数 :108-175 起，三带共 10 条目位＝9 语义变换，`items` 单串→数组 loot/advancement 两带复用）：仅对「byte 不等**且**命中已注册归一器带**且**双侧 JSON 可解析」的文件开放——双侧解析→node 侧施用该带变换→按双腿实测一致的 Gson 格式重序列化→再字节比对。
- **方向铁律**：变换方向恒 node(1.21)→canonical(1.20.1)，正典树（1.20.1 形＝形态钉）永不改写。
- **注册纪律**（沿 SEGMENT_MAP 注释证据纪律，:92-96）：每条变换必须带 ①版本差异出处（vanilla 反编译/census 实测）②census 实测样本文件名；1.21 侧形态以实测钉死，不猜。

### 2.2 fail-visible 三条（不放松）

1. byte 比对仍是快路径与主判定——归一通道不开给 byte 相等文件；
2. 归一成立≠静默放过：NORMALIZED 计数＋逐文件清单（含变换名）必打印（:21-22、:38-41 示形）；
3. 未命中带、解析失败、归一后仍不等→一律原样 FAIL 保留 first-diff offset；`--expect-diff` 白名单仅显式 CLI 注册最后手段，**默认无、本次未用**。

### 2.3 禁令与契约

- 禁止放宽为全局语义等价比较（byte-exact 收窄面最小且有据）；零 mdk 内容文件（本卡 mdk_touch 空）；tools/jar_content_check.py:30 引用面不破。
- 序列化契约：双腿 datagen 同为 Gson `setIndent("  ")`，实测与 `json.dumps(obj, indent=2, ensure_ascii=False)`（无尾换行）字节级一致——canonical 全部 77573 个 JSON 往返零差（:98-100），归一后按此格式重序列化即可逐字节比对。

## 3. Deviations（口径声明）

- census 实测与 brief 偏差如实记账：brief 假设 191 items＋197 copy_nbt＋2 codec 偏高——实测 items 形 183（loot 170＋adv 13）、copy_nbt 26、codec/结构形 19，总 228（tasks.p25-datagen-tree-check-unify.census_actual）。归一器按实测注册，不按 brief。
- P24 记录 227→今日 228 的 +1（show_notification 形）随本卡归一器收编，不再恒红。

## 4. Consequences

- **正面**：227/228 恒红基线归零（正例 exit 0：77345 byte-identical＋228 value-shape normalized＝77573 全对账，分变换 170/26/12/6/6/6/1/1 与 census 全吻合）；负例自证归一不吞差（负例 1＝注册带内未注册形差 rolls 1.0→2.0→FAIL offset 497；负例 2＝未注册带注字节→FAIL offset 114）；Gson 往返抽验 300 只零差；正典树跑后 git status 干净。
- **义务**：未来新产物带出现值形差→必须注册归一器（带版本出处＋样本），不许回退恒红计数；census 对账义务随卡（normalized 计数变动须在卡 gates 留实数）。
- **边界/遗留**：本工具只服务 ADR-P17-1 双树门禁（canonical tracked 树 vs 节点本地 build/datagen-output）；RCON/编译门禁的 21.1 delta 口径（IDENTICAL 族）不在本 ADR 范围。
