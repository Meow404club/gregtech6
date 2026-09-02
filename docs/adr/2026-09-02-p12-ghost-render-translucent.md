# ADR 2026-09-02-p12-ghost-render-translucent：RenderHighlightEvent 瞬态例外扩面（整面半透明+绿红分色）

> 状态：accepted（已落地，merge dbc55ef）。
> 本文为 state `decisions` 账本同名条目（架构师 p12-arch-ghost-remainder 产出）+ 实现
> 与审查实测的成文整理；只整理结构不改决策内容，全部 `文件:行号` 锚点原样保留。

## Context

- 先例：P5 扳手九宫格线框（GTWrenchHighlightListener，本仓首例 FORGE+Dist.CLIENT
  三约束实证）+ P10 ghost 线框 POC（c25ed08，27 格线框+FORMED 外壳框）。两者共同
  确立 RenderHighlightEvent 瞬态例外三约束：零静态可变字段 / 零世界写入 /
  除原版选框外零取消。
- P12 ghost 全量四件（pattern API 上收/整面半透明/绿红匹配/JEI 联动）中的渲染件，
  消费前置卡 p12-ghost-pattern-api（merge 248c66b）产出的 `GTMultiBlockPattern`
  （`cells()`/`Cell.matches` 谓词缝/`Cell.isHollow()`/`worldOffset`）。
- 上游 GT6 1.7.10 零客户端结构呈现（p12-ghost-upstream-census q3 负结论：放大镜
  聊天三句+tooltip 是全部）——**色语义无上游锚**，取保守最小集。
- 绿红参照：Litematica Configs（wrongBlock `#4CFF3333` 即 α0.30 / wrongState 橙
  `#4CFF9010` / missing 浅蓝不合用）+ Engineered Schematics（绿=目标位/黄=方向，
  ARR 只作语义参照）+ GTNH MSHP（缺失=红）。
- vanilla 渲染参照：RenderType.java:681-693 `debugQuads`（POSITION_COLOR/TRANSLUCENT/
  NO_CULL/LEQUAL）、:668-680 debugFilledBox、WorldGenAttemptRenderer:31-51 逐格彩填+
  中心对称内缩、StructureBlockRenderer:80-124 分色线框、LevelRenderer:2229-2330
  世界边界（:2259-2260 polygonOffset 可抄、:2265/:2328 Tesselator 直发禁抄）。
- 尺寸选型（census q5）：cokeoven 级 26-80 格每帧直发可行；Fusion 级（600-1000 格）
  缓存 mesh 留后卡。

## Decision

RenderHighlightEvent 瞬态例外扩面至**整格半透明面填充+绿红分色**，三约束保持结构性
成立（审查逐文件 grep 实证：零静态可变=全 static final 原始常量+纯函数；零世界写入=
仅 getBlockState+顶点发射；零取消=listener 无 setCanceled）：

1. **渲染选型钉死**：`RenderType.debugQuads`（原样，零新 RenderType）画整格面 +
   `RenderType.lines()` 同色不透明棱线；translucent 优先（自重叠排序瑕疵 P10 已定性
   可接受），脏则降 cutout；禁 RenderSystem/Tesselator 直发。
2. **绿红语义定稿最小集**：绿=谓词匹配；红=缺失（air/VOID_AIR）或不匹配合并；
   pattern 空气判据格（hollow，声明 AIR 谓词而非硬编码 isAir）世界空气=SKIP 不画、
   世界非空气=红（上游 TileEntityCokeOven:99-104/上游 :52 中心非空气即结构失败）；
   pattern 外格子永不判错不画（GT6 checkStructure 结构外宽容）；未加载 chunk=
   VOID_AIR→RED（ClientChunkCache:75 纯读安全）。
3. **判等口径=谓词级**：cokeoven 首绑 `state.getBlock()` Block 实例判等，对位上游
   checkAndSetTarget 判 partID+mode 不判全量 state；全 BlockState `==` 判等留给黄档
   property 判据时一并裁定。
4. **FORMED=true 维持 12 外沿 shell 框零变化**（与 POC 字面发射逐位等价，钉测
   delta=0.0）；alpha=0.3 静态色不脉冲（脉冲仅留 FORMED shell 现状）。
5. **分类器=纯函数**（新文件 GTMultiBlockGhostMatcher）：(pattern, Level,
   controllerPos, facing) → 与 cells() 按序对齐的 GREEN/RED/SKIP；每帧现算
   27~125 次 getBlockState 免缓存，零静态缓存。
6. **裁池**：进阶黄 wrongState（触发=首个含 property 级判据结构上卡，如 Oven
   18042|18043 双 design/stairs 类，届时参 Litematica 橙 #4CFF9010/Engineered 方向黄
   一并裁定全 BlockState 判等）；持续型 hologram（RenderLevelStageEvent 类别，需独立
   ADR，本 ADR 边界外）；大结构编译缓存 mesh（javadoc 注记）。
7. **验收面裁定**：离线分类器单测（MultiBlockLevel fixture，四朝向矩阵）+ runServer
   零 ERROR + runData 二跑 written:0；**RCON 不适用**（RenderHighlightEvent 仅客户端
   fire，服务端零可观测行为，无断言面不建链）。

## Deviations

- **missing/wrongBlock 双色分档否决**（Litematica 三色）：cokeoven 26 格同谓词无
  missing-vs-wrong 可分辨场景，合并红足够。
- **GTCEu PatternPreviewRenderer 重管线不移植**（后台编译 :156-187+chunk shader
  uniform :221-296+VertexBuffer :479-490），只借数据面/RenderFilter/timeout 三件
  （数据面已由 pattern API 承担）。
- **z-fight 终解=顶点内缩 0.002**（FACE_INSET，WorldGenAttemptRenderer:31-51 中心
  对称收缩同构，棱线同步内缩保共边）：polygonOffset 必须挂 RenderType composite
  state，而 DEBUG_QUADS 是冻结的 vanilla composite（RenderType.java:681-693 实读仅
  shader/transparency/cull 三件）——零新 RenderType 约束下 polygonOffset 结构性
  不可用，内缩是唯一合规手段。ADR 原文"内缩主选/polygonOffset 备选"按实现期实证
  收敛为内缩单选。

## Consequences

- **落地**：merge dbc55ef（链位 248c66b → dbc55ef → 6be2862）；分支提交 08b2754 +
  abbc913（GPG 2/2）。
- **门禁**（审查官 worktree 实测）：mdk `:test` 727/0（717+10）+ 根 `:check` 205/0；
  runData 二跑 written:0；runServer 零 ERROR（25703/25713）。
- **测试**：matcher 矩阵（四朝向全绿 104 GREEN+4 SKIP/缺 1 红/stone 顶替红/中心有块
  红+空气 SKIP/VOID_AIR 对 part=RED·hollow=SKIP/StairBlock FACING 翻转同 block 仍
  GREEN 的谓词级边界/pattern 外永不判错）+ renderer FORMED shell 位等价钉测（期望侧
  测试内独立重算，非自证）。
- **遗留**：① 进阶黄 wrongState 裁池（触发条件见 Decision 6）；② Fusion 级缓存 mesh
  javadoc 注记；③ runClient 目视六条留用户：绿面/红面/混合/成形 shell 零变化/
  alpha 观感/棱线可见性。
