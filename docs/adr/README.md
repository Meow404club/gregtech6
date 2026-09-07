# ADR 索引与约定（Architecture Decision Records）

本目录收纳「GT6 现代复兴计划」的正式架构决策记录。此前 ADR 以字符串形状散落在
state `decisions` 账本（列表条目）与 `docs/PROJECT_STATE.md`、`docs/TODO.md` 的
内联提及中；自 P12 起（任务卡 p12-hygiene-style-adr）新 ADR 逐篇落盘本目录。
**历史 ADR 不回填**（留池）：账本原文仍是权威来源，下表仅列条目占位。

## 约定

- **命名**：`YYYY-MM-DD-<slug>.md`，日期取决策落账日（state `decisions` 条目的
  日期），slug 用卡名/主题短横线小写（如 `2026-09-01-p10-cover-item-intercept.md`）。
- **四节结构**（每篇必须有，节名固定英文，正文语言随源）：
  1. **Context** —— 决策面对的事实与约束（上游/本仓/1.20.1 证据，`文件:行号` 锚点）；
  2. **Decision** —— 裁定本身（范围写死，含"恰量"边界）；
  3. **Deviations** —— 声明偏离（与上游语义的显式差异，每条带证据锚点）；
  4. **Consequences** —— 落地后果（提交/门禁/测试计数/遗留与后续卡指针）。
- **锚点纪律**：`文件:行号`、类名、常量名、上游 MTE id 等技术锚点一律原样保留，
  整理（转正成文）不得改写决策内容——只允许重排结构与措辞衔接。
- **冻结面扩面**：凡解除既有冻结面（如 ICover/ICoverableTE）的决策必须在 Decision
  节写明 sanctioned 范围与理由，并在 Consequences 节声明"唯一一次"或后续复用规则。
- **权威顺序**：本目录成文 ADR > state `decisions` 账本条目 > docs 内联提及；
  三者冲突时以本目录为准并回写勘误。

## 已成文

| 日期 | 文件 | 主题 |
| --- | --- | --- |
| 2026-09-01 | [2026-09-01-p10-cover-item-intercept.md](2026-09-01-p10-cover-item-intercept.md) | ICover item 族冻结面扩面：恰八钩解冻 + 三宿主门 + TileEntityOven 侧感知 wrapper |
| 2026-09-02 | [2026-09-02-p12-ghost-render-translucent.md](2026-09-02-p12-ghost-render-translucent.md) | RenderHighlightEvent 瞬态例外扩面：整面半透明 + 绿红分色（三约束保持） |
| 2026-09-02 | [2026-09-02-p12-jei-dependency.md](2026-09-02-p12-jei-dependency.md) | 首个第三方 mod 依赖：JEI 接入（版本/坐标/类路径/信息页，含砖数 26→25 勘误附录） |
| 2026-09-02 | [2026-09-02-p12-rotation-carrier.md](2026-09-02-p12-rotation-carrier.md) | 旋转载体：零新 EnergyType，复用 RU/KU+push 握手，Axle 邻接递归 |
| 2026-09-02 | [2026-09-02-p12-fluid-engine-chain.md](2026-09-02-p12-fluid-engine-chain.md) | 流体罐引擎链总 ADR：范围/三波九卡/蒸汽产源/声明偏离（含桶 GUI 终裁） |
| 2026-09-02 | [2026-09-02-p12-steam-proof-deviation.md](2026-09-02-p12-steam-proof-deviation.md) | 蒸汽四防销毁链：声明偏离 + P13 强制还账四件（POWER_CONDUCTING 实证；2026-09-03 O1 补账转正） |
| 2026-09-03 | [2026-09-03-p13-barrel-base-unfreeze.md](2026-09-03-p13-barrel-base-unfreeze.md) | TileEntityBase08Barrel P5 冻结面恰量解冻：tick 两查+gasProof 覆写点+allowFluid 最小面（还账四件随卡 ADR） |
| 2026-09-03 | [2026-09-03-p13-hu-energy-face.md](2026-09-03-p13-hu-energy-face.md) | HU 能量面零新 EnergyType：复用 push 握手，发射面仅顶面（原 state 条目因账本覆盖事故按 architect 卡重建成文） |
| 2026-09-03 | [2026-09-03-p13-boiler-family-split.md](2026-09-03-p13-boiler-family-split.md) | 锅炉族总拆定裁定：四波串行/端口/注册家/常量作用域纪律/爆炸纪律（同上重建成文） |
| 2026-09-03 | [2026-09-03-p15-3-chisel-fork-discipline-r1.md](2026-09-03-p15-3-chisel-fork-discipline-r1.md) | ADR-P15-3 r1 成文：chisel 分叉三级优先序（下沉 gregapi > swap 表 > //?）+ javadoc 禁独占 //? 分支 |
| 2026-09-03 | [2026-09-03-p15-10-monotone-shrink-gate-r1.md](2026-09-03-p15-10-monotone-shrink-gate-r1.md) | ADR-P15-10 r1 成文：1.21.1 单调收缩门禁口径钉 compileJava 红文件数；v1=W2 收官重测为真基线 |
| 2026-09-05 | [2026-09-05-p16-blockstate-prop-identity.md](2026-09-05-p16-blockstate-prop-identity.md) | ADR-P16-2：BlockState Property 单一 owner（1.21.1 StateHolder Reference map 按 == 查 key；GTBlockProperties holder+类内别名） |
| 2026-09-05 | [2026-09-05-p16-closeout-gate-ledger.md](2026-09-05-p16-closeout-gate-ledger.md) | P16 收口数据文档：四判据门禁总账 + 11 卡表 + 21.1 运行时 delta 三案 + P17 池移交 |
| 2026-09-05 | [2026-09-05-p17-datagen-tree.md](2026-09-05-p17-datagen-tree.md) | ADR-P17-1：datagen 共享树跨节点裁决 = 输出目录按节点参数化（正典节点共享 tracked 树不变；21.1 输出节点本地 build/；五步门禁取代六步） |
| 2026-09-05 | [2026-09-05-p17-machine-io-adjudication.md](2026-09-05-p17-machine-io-adjudication.md) | P17①：21.1 机器 IO 两 delta 单根因（wiring 漏注册 DRYER/DISTILLERY_BE）+ IDENTICAL-22 逐条裁决（19 fix/1 permanent/2 defer，残集 IDENTICAL-3） |
| 2026-09-06 | [2026-09-06-p18-keepfilter-ruling.md](2026-09-06-p18-keepfilter-ruling.md) | ADR-P18 keepFilter 读侧重建：写侧双腿同上游合同键不丢，21.1 腿 parseOptional MAP_CODEC POSITIVE_INT 拒 0 量 orElse(EMPTY) 静默吞——裁读侧 FluidName 反查重建（permanent-delta 撤销，IDENTICAL-3→2） |
| 2026-09-06 | [2026-09-06-p18-staticinit-poison-fix.md](2026-09-06-p18-staticinit-poison-fix.md) | ADR-P18 staticinit 毒态：junit-fml boot 真跑 load() × reset() 不清旗标——裁世代化重置钩子注册制（7 loader 全集+守卫测试先红后绿；否 load() 自愈重灌）（IDENTICAL-2→0） |
| 2026-09-06 | [2026-09-06-p18-beload-provider.md](2026-09-06-p18-beload-provider.md) | ADR-P18 多方块机 21.1 BE load NPE（P1 数据丢失）：裁 A 先落（:808/:830 换 NBT_ACCESS，等价性读码证实）+ B 根治登池（W4 provider 沿 Root:140 穿透） |
| 2026-09-06 | [2026-09-06-p19-w4-nbt-provider-ruling.md](2026-09-06-p19-w4-nbt-provider-ruling.md) | ADR-P19 W4 NBT provider 穿透池项裁决：本体缓做（条件触发池化，P18 后全树 21.1 NBT 面 level 依赖=0+saveToItem 0 调用）；注册表覆盖缺口（1.21 一切附魔数据驱动，frozen 视图 parse 静默剥附魔）由 B' 研究卡钉证可行→1 文件小卡（AtomicReference 委托+AboutToStart 复合重绑，b2e16830 落地） |
| 2026-09-06 | [2026-09-06-p19-porting-split.md](2026-09-06-p19-porting-split.md) | ADR-P19 移植批拆卡：5 卡 2 波（stoneblocks registry/render∥drying∥front∥chisel）+三研究卡复核裁决（否决 GTMaterialBlocks 扩展/盐流体走 SIMPLE_LIQUID_SPECS/circuit 行整面裁池随工具卡）+共享 append-only 面合并序 |
| 2026-09-06 | [2026-09-06-p20-texture-tree-policy.md](2026-09-06-p20-texture-tree-policy.md) | ADR-P20 贴图真图/占位双树政策：真图正典位=静态树+同路径跨树共存禁止（generated srcDir 后拷遮蔽静态树实证）+gen_textures.py skip-if-real 让位（静态树即排除表）+census 纯 JUnit 禁做 datagen provider+overlay_active 烘焙泛化+机器机身键集钉定 |
| 2026-09-06 | [2026-09-06-p20-i18n-zhcn-pipeline.md](2026-09-06-p20-i18n-zhcn-pipeline.md) | ADR-P20 i18n zh_cn 管线：GT6ZhCn datagen provider+TSV 翻译数据源注入（非 datagen 推导，zh_cn.json 不手写）+GT6LangParityTest zh⊆en 守卫+ZH_KEY_FLOOR 棘轮+tagprefix 手译在用 105 条（否决词缀推导值源）+B 波拼接改造组合域（改造前 zh 回退英文=声明工作状态） |
| 2026-09-06 | [2026-09-06-p20-modularui-fork-ruling.md](2026-09-06-p20-modularui-fork-ruling.md) | ADR-P20 ModularUI fork 三裁决：上游=brachy84/ModularUI-Modern per-MC 分支（1.20.1@909cda2/1.21.1@c13e141 双腿收割）+落点=仓内 vendored third-party/modularui（否独立仓：fork 期 publish 税）+LGPL-3.0 义务三件套+jarJar 双腿各嵌本腿 jar（EvalEx 嵌套存活卡②实测） |
| 2026-09-07 | [2026-09-07-p21-smoke-gui-headless-ruling.md](2026-09-07-p21-smoke-gui-headless-ruling.md) | P21 smoke-gui 范围裁决：离线 headless JUnit（四维否 runClient 自动化；unit.testing=true 上游钩子+Bootstrap 双闸实证；与 jarjar_smoke 互补合成端到端；runClient 视觉池项保留） |
| 2026-09-07 | [2026-09-07-p21-stoneblocks-16item-registry-split.md](2026-09-07-p21-stoneblocks-16item-registry-split.md) | P21 stoneblocks 16-item registry 拆分：17×1 property 形→272 per-pair Block+BlockItem（P8 先例）+272 loot 独立表（variant0=dropOther 同石 COBBL）+id 方案 variant0 保裸 snake；旧世界 property 迁移损失声明 |
| 2026-09-07 | [2026-09-07-p21-paintable-rulings.md](2026-09-07-p21-paintable-rulings.md) | P21 机器喷漆两卡裁定：存储直存免反码（DYES_INT_INVERTED 复合恒等）+已漆通道平均混色+NBT gt.color/gt.painted 键逐字+IPaintableTE 挂 03 基类全族+写入入口=离线测+RCON 臂（喷漆罐物品留池） |
| 2026-09-07 | [2026-09-07-p21-jade-compat-card.md](2026-09-07-p21-jade-compat-card.md) | P21 Jade 兼容单卡：@WailaPlugin 零静态引用隔离（NCDFE 防线）+Modrinth maven compileOnly 双腿钉值+versionRange 硬编码声明偏离+tooltip v1 四段（流体/喷漆色/lang 键/runClient 目视入池） |
| 2026-09-07 | [2026-09-07-p21-chisel-drop-conversion-ruling.md](2026-09-07-p21-chisel-drop-conversion-ruling.md) | P21 凿石掉落转换落点裁决：loot 逐表 alternatives[match_tool(gt6:chisel)→CHISEL_MAPPINGS item]（否 getDrops override 与 GLM）+170/102 两形态+21.1 items 序列化 delta=p17 单数化口径 |
| 2026-09-07 | [2026-09-07-p22-front-overlay-split-ruling.md](2026-09-07-p22-front-overlay-split-ruling.md) | P22 front overlay 拆层裁决：P21 灰轨结论翻案（colored/ 即灰度待染轨且 P20 已 byte-identical 借入，机身显色零差）+C1 偏差声明（front overlay 连带染，known_bugs 在案）+C2 双 element 正典（body tintindex0+decal 无 tintindex 键+epsilon 外浮 cullface north，96f9d355）+C3 已漆切灰轨否决（前提不成立） |
| 2026-09-07 | [2026-09-07-p22-jade-fluid-seam-ruling.md](2026-09-07-p22-jade-fluid-seam-ruling.md) | P22 Jade 流体缝裁决：罐同步触发面不含流体→必须服务端推 appendServerData+C-1 多块类零罐字段单分支+D-1 amount() 上游 :330 同形零触碰+形态=v1 内嵌（b9b0b23f）非 universal registerFluidStorage（入池升级无沉没成本）+载体 of(fluid,1000)+真 long 避 readDefault INT_MAX |
| 2026-09-07 | [2026-09-07-p22-spraycan-toolitem-rulings.md](2026-09-07-p22-spraycan-toolitem-rulings.md) | P22 喷漆罐工具物品域裁定（并篇 painted-item-domain）：单物品/色+gt.remaining ×10 单位制+paintPaintableTE 路由逐字（04:227-235）+GT6 通道平均混色否覆盖式+三偏离（隐形罐裁/无目标臂池/占位音效）；21 表 copy_nbt 携漆+ItemColor 显式注册（ItemColors 无 BlockItem 委托铁证）+13 块 loot 缺口修复（7ba12b14/cc21dd96） |
| 2026-09-07 | [2026-09-07-p22-stonecutter-regex-split.md](2026-09-07-p22-stonecutter-regex-split.md) | P22 stonecutter `.color(` swap 表劈分：4 参逗号形 lookahead 留表+单参删表改 //? 行内分叉（f58fb0a5）+双向编译红 fail-visible 口径+P21 空格锚归一+regex 不可分辨接收者时按实参形态劈分的维护启发 |

## 占位（历史 ADR，未回填，权威原文在 state `decisions` 账本与 docs/PROJECT_STATE.md）

- ADR-P2-*（第 2 阶段：注册桥幂等、材料条件系统、datagen 管线等五卡）
- ADR-P3-*（第 3 阶段：BE/Menu 框架四卡，含 2026-08-30-p3-fullprefix-registration-universe）
- 2026-08-30-p4-*（第 4 阶段拆卡九条：recipe-core / fluid-pipes / machine-oven /
  render-foundation / multiblock-framework / cover-core / fluid-barrel 等）
- 2026-08-31-p6-cokeoven-processing（CokeOven 加工八裁定）
- 2026-08-31-p8-prefixblocks（storage 前缀方块 A/B 拆卡）
- 2026-08-31-p8-machine-closeout（机器族收尾 M1/M2）
- 2026-09-01-p9-redstone-hooks（cover 红石钩框架卡）
- 2026-09-01-p9-tool-crowbar（正式 crowbar + 工具 id 入口）
- 2026-09-01-p9-wire-family（线缆全谱系 W1/W2）
- 2026-09-01-p9-render-c-oven-overlay（C 档 oven 动态渲染）
- 2026-09-01-p9-render-d-formed-look（D 档成型态渲染）及其 erratum 勘误
- 2026-09-01-p9-redstone-cover-emitter（Redstone Emitter 盖）
- 2026-09-01-p10-tools-covers-split（P10 工具族 + cover 残余拆卡）
- 渲染路线 ADR（BakedModel 快照制 / BEWLR-TESR 全仓禁令，散见于 P4/P5 卡面与
  javadoc 引用）

后续卡若需引用某条历史 ADR 的完整原文，以 `state_read("decisions")` 与
`docs/PROJECT_STATE.md` 对应阶段段为源，按本 README 四节约定转正成文。
