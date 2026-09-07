# ADR 草稿：P22 front overlay 拆层裁决（colored/ 灰轨翻案 + 双 element 正典）

- 日期：2026-09-07
- 状态：Accepted（P22 收官主会话正典化 2026-09-07）
- 研究依据：state key=`tasks.p22-research-paint-grayscale`（翻案全证）
- 实现卡：state key=`tasks.p22-paint-front-overlay-split`（merged `96f9d355`，commits ddd61634 + aafef320）
- 偏差落账：state `known_bugs[0]` `painted-machine-front-overlay-tint`
- 证据路径根：上游=`tmp/gt6-1.7.10/src/main/java/`，port=`mdk/src/`（下同）

## 背景与证据

**翻案**：P21 审查结论『colored/ 灰轨 repo 无、机器贴图为彩色内容、乘法机制有偏』被本阶段研究推翻。事实链：

1. **colored/ 即灰度待染轨且就在 repo 内**：上游 `MultiTileEntityBasicMachine.java:176-182` mTexturesMaterial 直指 `machines/basicmachines/<name>/colored/{bottom,top,left,front,right,back}.png`；『colored』命名=接受 mRGBa 染色的轨，非彩色内容。目验 oven/shredder/boiler_steam 四张 colored/*.png 全灰度。
2. **本仓已 byte-identical 借入**：P20 烘焙卡自述（`mdk/src/main/resources/assets/README.md:1374-1388`）oven_bottom/top/side ← 上游 `basicmachines/oven/colored/{bottom,top,left}.png`，sha256 `db9560d386...` 三键同图。**机身显色与上游零差**。
3. **上游两层制=第二层恒不染**：`MultiTileEntityBasicMachine.java:1014` getTexture2 = `BlockTextureMulti(BlockTextureDefault(colored×mRGBa), BlockTextureDefault(状态 overlay))`；第二层构造走 `BlockTextureDefault.java:179-180` `(IIconContainer, boolean aGlow)` → fRGBa=UNCOLOURED 白=恒不染色。染色机制=`ITexture.java:159-198` Tessellator.setColorRGBA 顶点色调制，与 1.20.1 tintindex 乘法同数学；未漆白乘恒等 ≡ UNCOLOURED ≡ 本仓 -1 哨兵。
4. **真实且唯一的显色偏差**：本仓 front=烘焙合图（灰底+状态 overlay 压平，README.md:1351-1372），port 模型单 element 全脸 `tintindex(0)`（`mdk/src/main/java/gregtech6/datagen/GT6BlockStates.java:324-338`）→ overlay 层被 tintindex0 连带染。已漆机器 active 态的彩色火光件（目验 `oven/overlay_active/front.png` 橙色）被漆色二次染色；机身五面与未漆机器零影响。P20 借图与 P21 乘法机制均无偏——偏差仅在 front 合图单层。

## 裁定

### C1：P21 结论翻案 + 偏差声明落账（零代码，已闭环）

`known_bugs` `painted-machine-front-overlay-tint` 即本裁定的偏差声明形态：机身显色=上游同构（灰轨×漆），front 合图 overlay 随脸染色为已知偏差。原池项『colored/ 灰轨借入』销项（伪需求——已借入）。

### C2：双 element 拆层正典（已合 `96f9d355`）

`GT6BlockStates.machineModel` 单 element 立方改双 element：

- **body 六面灰轨 `tintindex 0` 现役不动**（colored/ 轨语义保持）；
- **front 薄 overlay element 无 tintindex 键**（=恒不染，映射上游第二层 UNCOLOURED），贴 `overlay[/_active/_running]/front.png` 三态；
- **epsilon 0.01 外浮 + cullface north 同步消隐**防 z-fight（active 态 overlay 消隐与面剔除联动）；
- 借入 6 族 colored/front + 三态 overlay/front 计 24 PNG（动画条 FRAME-0 裁剪沿 P20 声明偏离），README 台账记账；三态烘焙合图 18 张退役；
- census 测试 elements 1→2 断言（63 模型），runData 二跑 written:0 门禁。

上游忠实性核对通过：`MultiTileEntityBasicMachine.java:1014` + `BlockTextureDefault.java:179-180` 原文复核，双 element=两层制直译。合并态门禁：forge 147 类/1189 测、neo 147 类/1191 测（2 skip 既有）全绿；24/24 sha256 台账复验一致。

### C3：『已漆切灰轨底图』题面原案否决（前提不成立）

C3 依赖『机身底图是彩图、需按 painted 切灰轨』——被翻案证伪：底图本就是灰轨，无彩图可切；front 无法切回纯灰（overlay 已压平进合图）。若按 PAINT 动态换轨需 blockstate painted bool 或 BakedModel ModelData 选模型=模型翻倍/烘焙复杂化（GTCEu IS_PAINTED variant 路线，P21 已否决）。白付模型翻倍成本、收益为零，存档否决。

## 后果与池项移交

- 63 模型 JSON 全量重生成（element 2/body tintindex 0 保持/overlay 脸无 tintindex+cullface north），runData 幂等二跑 written:0 入门禁。
- runClient 目验（已漆+active 火光不被漆色二次染，外部客户端截图）列收官清单，不入门禁（runClient 首启双腿未发生）。
- 不做：动画忠实化、其它面 overlay、tint 消费链改动（`GTMachinePaintTint` 零修改）。
- 已漆机器视觉与上游全同构，known_bugs 该条可随本卡合入转 fixed（残项仅目验截图）。
