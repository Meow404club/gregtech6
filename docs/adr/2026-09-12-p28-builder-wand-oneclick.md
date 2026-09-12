# ADR-P28：建筑杖一键成型偏离（上游九击语义 → 单击全铺）

日期：2026-09-12 ｜ 前置：ADR 2026-09-08-p24-op2-reform（canEdit/mayBuild 缝）＋ p27-builder-wand-form-fix（patternWalkFacing 缝） ｜ 评审基线 main=035d43de ｜ 决策卡 state research.p28-r-builder-wand-second-root（根因研究＋用户裁定全量） ｜ 实现卡 work/p28-builder-wand-oneclick

- 状态：Accepted（用户裁定 2026-09-12，选研究方向 B「UX 偏离」并要求声明偏离）

## 0. 摘要

建筑杖（Builder's Wand）多方块支架臂从上游 1.7.10 的「九击逐步成型」（±1 点击邻域门）改为「一键成型」：点一下铺完整多方块、材料照常逐块消耗。这是**对上游语义的声明偏离**，由用户在根因研究证实「只造一半长少 1」并非移植 bug 之后裁定（研究给出 A 保真/B 偏离两方向，用户选 B）。±1 门代码本体与多方块校验逻辑零改动，偏离只落在杖的喂参路径上。

## 1. 上游语义（被偏离的原行为）

上游点击喂真实坐标（直点臂 onToolClick2 喂 getCoords()，TileEntityBase10MultiBlockBase.java:143；中继臂喂 aFrom，:132），coords 一路透传到 Util.checkAndSetTarget 的 ±1 点击邻域门（ITileEntityMultiBlockController.java:51：`aClickedAt == null || abs(dx)<2 && abs(dy)<2 && abs(dz)<2` 才自动铺块）。非 null 坐标单击只铺锚点 Chebyshev≤1 邻域——新鲜多方块要点多次（坩埚 24 墙要点 9 次：controller 1 次＋8 面中层墙各 1 次；离线测试 pre-rewrite 钉死该九击故事）。本仓 P24-P27 期间为忠实移植。

根因研究（2026-09-12）证实用户报告「只造一半长少 1」＝该门的忠实行为，非移植 bug：玩家 useOn（GT6BuilderWandItem.java:109/:117）喂真实点击坐标被门截住（单击只铺 16/24 墙，y+2 环永缺）；RCON form 臂（GTMultiBlockCommand form → checker.form beat4 :322）字面喂 aClickedAt=null 门全开一次铺满 24——p27 链 GREEN 验证的是「一键成型臂」而非「玩家点击臂」，构成 rcon_coverage_gap。

## 2. 裁定（用户 2026-09-12）

- 语义改**一键成型**：点一下铺完整多方块，材料照常逐块消耗；
- 失败语义**沿用 checker.form 既有行为**（以现行为为准，如实声明）：已成型再点＝幂等无副作用（beat-1 诊断短路）；材料不足/校验失败＝事务性拒绝（零世界写入、零消耗，RCON 契约 "insufficient stock ⇒ not formed AND not consumed"）；
- ±1 邻域门代码保留不动（其他消费者照旧），仅杖路径绕开；
- 补 RCON 测试盲区：新增 wandclick 臂走真实玩家喂参路径（模拟点击坐标，不喂 null）；
- 偏离以本 ADR 声明。

## 3. 新行为（落地形态）

`GT6BuilderWandItem.builderWandScaffold`（原 :152 处两击臂）改为两机制一语义：

1. **forming-pattern 控制器**（Coke Oven 族、Crucible 族——pattern 带 formingPart 格）：改走 `GTMultiBlockStructureChecker.form(controller, patternWalkFacing(), player, inventory)`（p27 的 patternWalkFacing 缝，与 form 臂同喂法）。SET 行走自带事务语义：beat-1 诊断（已成型/未加载＝零副作用短路）、beat-2/3 计划与库存对账（硬失败/短料＝零写零耗）、beat-4 两趟执行（喂 null＝门全开整结构）；
2. **其余控制器**（LightningRod 无 pattern、LargeBoiler declaration-only pattern——手写 checkStructure2 即校验逻辑，SPEC 明确不动）：喂 `aClickedAt = null` 过自身行走——**同一扇门的自有开臂**（`aClickedAt == null` ＝ 整结构皆目标，正是 SET 行走 beat-4 骑的机制），一键成型结果相同；其逐格尽力铺（非事务）语义保留，如实声明为两臂差异。

耐久扣减维持无条件每击 1 点（上游 :135 无条件 return 10；成型与否不退赔）。**±1 门本体（Util.checkAndSetTarget:145-146）与其余消费者零改动**：600-tick 轮询（三 null 喂参）、RCON form 臂（null 喂参）、手写行走（轮询三 null）全部保持上游行为；门调用方核查（sym_query 实证）：Util.checkAndSetTarget 消费者＝checker.walk/LargeBoiler:299-335/三处离线测试；checkStructure2 消费者＝Base10:147 轮询/杖缝（本次唯一改动点）/GTMultiBlockCommand wand 臂 :369、boilerWand :1094（均不动）/测试三 null 喂参。

## 4. 失败语义选择（声明）

- forming 臂：**checker.form 现行为＝事务性**（短料/硬失败不铺不耗；已成型幂等）。玩家敲在错误材料结构上时单击无效果但仍扣 1 点耐久（每击计费的上游忠实面，接受该 UX 代价）；
- 手写臂（锅炉/避雷针）：保留上游**逐格尽力铺**语义（库存耗尽即半铺，不回滚）——与 form 的事务语义不同，为本偏离的实现细节如实申报；
- 测试边界：离线九击钉重写为单击全铺钉（GTMultiBlockFacingIntegrityTest 单击＋幂等＋短料事务拒三测；CheckerFormTest arm7 同步重写为单击＋relay 幂等）；RCON 走 wandclick 臂（p28_builder_wand_oneclick.py：单击全铺 24、y+2 环判别、幂等 stock 1→1、form 臂回归锚）。

## 5. 验收留痕（tasks.p28-builder-wand-oneclick）

- 双腿 compileJava＋compileTestJava 分调用通过；cleanTest 全量 XML 实数对账（见交卡报告）；
- RCON 双腿：p28 链（wandclick 单击全铺＋幂等＋form 回归）与 p27 链复放（form 臂无回归）GREEN；
- KJS 面：本卡产出＝无 KubeJS 面（游戏交互缝＋测试基建；无注册面/RM 变更）。

## 6. 后果

- 正面：玩家 UX 与 RCON 语义合流（form 臂＝杖臂＝一键成型）；RCON 首次覆盖玩家真实喂参路径（wandclick），喂参不对称类缺陷（p27 型）结构上不可能再漏；
- 义务：未来任何多方块支架语义变更必须同步三处钉（离线单击钉＋wandclick 链＋form 回归锚）；手写臂与 forming 臂的失败语义差异已在测试中各自钉死，不得静默互相靠拢；
- 边界：上游九击语义在上游仓库仍然成立，本偏离为本仓单方面 UX 裁定；若上游后续改收「一键成型」类语义须重开裁定点对齐。
