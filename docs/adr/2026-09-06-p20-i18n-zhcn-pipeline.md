# ADR-P20 草稿：zh_cn lang 键映射管线与键集守卫（P20③ i18n 拆卡前置裁决）

- 日期：2026-09-06
- 状态：正典（P20 开段成文，主会话 promote）
- 关联：研卡 tasks.p20-research-i18n-zh（记忆 id332）、ADR-P17（datagen 共享树门禁）、
  ADR-P20 贴图树政策 docs/adr/2026-09-06-p20-texture-tree-policy.md §3（i18n 隔离）、
  ADR-P2-4（lang 模板两表先例）、用户三裁定 id330（标准 lang JSON + 不预装全串靠拼接 + zh 参考 tmp/gregtech.lang）
- 基线：main 50843dff

## 0. 问题陈述

en_us 唯一生成链 = GT6EnUs（534 行，GT6DataGenerators:36-37 注册），en_us.json 3578 键。
zh_cn 若直放静态 JSON：破坏「零手写 JSON」红线、无键漂移校验、在 P17 治理面外（研卡 wiring_verdict 已否）。
既定 A 案 = GT6ZhCn datagen provider，与 en_us 同源注册表走查。本 ADR 只裁三个遗留决策点：
键映射产物管线、tagprefix 值源、键集守卫测试形态。

## 1. 裁决

### 1.1 键映射管线 = tools 脚本预处理 + TSV 数据源 + datagen 期 classpath 读入

- 生成器 `mdk/tools/gen_zhcn_ref.py`（与 gen_textures.py 同族落点）：读 `tmp/gregtech.lang`
  → 剥 `    S:` 前缀 → 只收 `gt.material.*` / `itemGroup.*` / `gt.multitileentity.*` 三族，
  丢 `ktfru.*`（addon）/`written.book.*`/`enchantment.*`（非 gt6 键面）→ 产 TSV。
- TSV 落 `mdk/src/main/resources/gregtech6/lang/zh_cn_ref.tsv`，git 跟踪，列
  `kind<TAB>source<TAB>value<TAB>status`：kind∈{material,itemgroup,mte,direct}（direct 行
  source 列即最终 gt6 键，供 tagprefix 手译/零星键用）；status∈{auto,hand,review}；
  review = 值纯 ASCII（疑似未翻译，实证样本 `gt.material.Magnite=Magnite`，tmp/gregtech.lang:5003），
  provider 跳过该行 → 运行时逐键回退英文。
- **target 键不在 TSV 预推导**：provider 端以 `MaterialPrefixItem.snakeCase` 单实现从
  mNameInternal/mNameCategory 派生（与 GT6EnUs 同一函数），Python 侧零键名逻辑——
  消灭双实现漂移面。
- **红线边界论证**：红线管的是「DataGen 能生成的 JSON 不手写」——zh_cn.json 本体由
  GT6ZhCn 经 runData 生成，绝不手写；TSV 是翻译**数据源**（与 en 侧 `mNameLocal` 同位），
  属数据注入非 datagen 推导。datagen 不从 tmp/gregtech.lang 现场直读：coder worktree
  不保证 tmp 快照在场（ADR-P20 §2 census 同款 CI 原则），且 117k 行 dump 的过滤裁决
  需一次性显式化+可 review，脚本产物（TSV）即过滤规则的可 diff 固化。
- TSV 随 jar 打包（~150KB，processResources 不排除）：接受，注释声明；不入 assets/
  （不进贴图 README 对账面）。

### 1.2 tagprefix 值源 = 手译在用 105 条；词缀推导脚本只作 review 辅助、不作值源

1.7.10 dump 无模板键（1.7.10 是 oredict 预组合名），**不存在权威 zh 模板源**；从 oredict
组合名做词缀剥离多数投票=统计泛化，模板错一条即乘以 1273 材质（系统性错误），且实质
是无复核的组合猜测，与「禁机翻」红线精神冲突。105 条手译一次性有界（dump 的 oredict
组合名可作人工对照证据），进 TSV kind=direct。推导脚本可作为建议生成器（status=review
落表），永不直进 provider 输出。

### 1.3 键集守卫 = GT6LangParityTest（离线 recording subclass，双腿 parity）

- 形态复用 GT6EnUsJeiInfoTest:52-64 的 recording subclass（add 非 final + 同包覆写，
  裸 JVM 收集，无需 datagen run）：同一测试内收集 en（GT6EnUs）与 zh（GT6ZhCn）两键集。
- 断言四条：① `zh.keySet() ⊆ en.keySet()`（结构性防漂移：TSV 派生键或走查错位即红）；
  ② A 波负断言——zh 不含组合域键族（`block.gt6.wire_*`、石头/rows 族前缀清单），
  B 波每改造一域收缩一条负断言，改造完清空；③ `ZH_KEY_FLOOR` 钉数（初值 1800，
  只升不降，升即 PR 显式改常量）；④ 全值非空。
- **A 波零改动 GT6EnUs 走查体**：GT6ZhCn 镜像四段小单元走查（tab/blocktab/tagprefix/
  material，各 ~6 行，同注册表源），防漂移由 parity 测试承担而非共享基类——避免扩大
  B 波必触文件（GT6EnUs）的冲突面。
- 双腿成本为零：stonecutter.gradle.kts:62/:92 的 import 平移映射已覆盖
  `LanguageProvider` 与 `data.event.*`，GT6ZhCn 纯共享树源码，无节点本地变体。

### 1.4 组合域拼接姿势（B 波，DEPENDS_ON A 波）

- Block 侧 `getName()` 覆写组 `Component.translatable(模板键, args...)`（en 侧
  MaterialPrefixItem.java:64-65 运行时填模板先例的 Block 版）；模板键用**位置参**
  （`"%sx %s%s"` / zh 免空格 `"%sx%s%s"`），词序自由由模板承载；
- 材质参数复用 A 波小单元键 `gt6.material.<snake>`（zh 已覆盖）→ B 波组合域**零新增
  翻译债**即全中文；
- GT6EnUs 对应 add* 方法收缩为模板键（en_us.json 净减 ≈1120 键），parity 测试与锚字面
  测试（GTWireSpecs.displayName/StoneVariant.compose/row displayName 钉串处）同步更新；
- 贴图 census 测试零涉：其对账面=模型 layer0↔PNG，无 lang 维度（研卡 census 口径），
  en_us 键数变化不触发任何贴图守卫。

## 2. Consequences

- 翻译贡献者界面 = TSV PR（改一行翻一条），zh_cn.json 永不入 PR（datagen 产物）。
- P17 五步门禁零新增：zh_cn.json 入正典 tracked 树（1.20.1 runData 产出；21.1 腿
  本地 build/datagen-output 仅验证），datagen_tree_check 1:1 自动纳入。
- zh 缺键运行时回退 = vanilla 内建（LanguageManager.java:50-52 双语按序 +
  ClientLanguage.java:29-46），零移植代码；组合域 B 波改造前 zh 显示英文是**声明工作状态**。
- 触点属主：GT6DataGenerators 归 A 波；GT6EnUs 归 B1/B2；GT6JeiPlugin 本 sprint 零触
  （INFO_KEY_COKE_OVEN 的 zh 值走 TSV kind=direct，插件无代码变化）；assets/README.md
  i18n 三卡全不触（贴图波 append-only 面，ADR-P20 §3 复核成立）。
