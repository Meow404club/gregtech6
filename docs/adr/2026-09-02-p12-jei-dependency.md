# ADR 2026-09-02-p12-jei-dependency：首个第三方 mod 依赖引入（JEI 接入）

> 状态：accepted（已落地，merge 6be2862，main HEAD）。
> 本文为 state `decisions` 账本同名条目（架构师 p12-arch-ghost-remainder 产出）+
> curator 裁决 + 实现/审查实测的成文整理；只整理结构不改决策内容，全部
> `文件:行号` 锚点原样保留。

## Context

- 本仓此前零第三方 mod 依赖（根/子项目 build.gradle 仅 junit+root project）——
  首例，故立 ADR。
- JEI for 1.20.1 Forge：15.56.0.205 双源一致（blamejared maven-metadata 2026-09-02 +
  Modrinth 2026-08-31 发布）；GTCEu Modern 1.20.1 钉 15.20.0.115
  （gradle/forge.versions.toml）= 降级预案。
- MDG legacyforge 2.0.144 `mod*` 配置自动 SRG→official 重映射、非传递
  （LEGACY.md:68-92，harvest mdg-legacy-doc）；【curator 裁决 2026-09-02 类路径风险
  解除】LEGACY.md L74：mod* 配置=同名标准配置的 child，modRuntimeOnly 依赖重映射后
  **并入对应 run 配置类路径**——runClient 可见 JEI。
- JEI API 面：`@JeiPlugin`+IModPlugin 无参构造（GTCEu GTJEIPlugin.java:43-44 先例）；
  `addIngredientInfo` since 7.6.4；`IRecipeCategory.draw(T,IRecipeSlotsView,GuiGraphics,
  double,double)` since 9.3.0；`getBackground` 自 15.20.0 弃用可空
  （harvest jei-api-irecipecategory-1.20.1 + curator 修正）。
- P8 裁定可复用：结构描述归 lang/datagen 面（tasks.p8-cokeoven-gui-menu）。

## Decision

1. **依赖引入（build 面变更）**：版本钉值 15.56.0.205 入 gradle.properties
   （`jei_version`，注释带降级预案 15.20.0.115）；坐标三件=modCompileOnly
   `mezz.jei:jei-1.20.1-common-api` + modCompileOnly `mezz.jei:jei-1.20.1-forge-api` +
   modRuntimeOnly `mezz.jei:jei-1.20.1-forge`（impl）；maven `https://maven.blamejared.com/`
   加在 **mdk/build.gradle repositories{} 子项目局部**（root 零污染，settings.gradle
   默认不动）。
2. **类路径口径**：LEGACY.md L74 已明（curator 裁决），验收门禁含一次 runClient
   冒烟**确认**（nohup+日志轮询，断言 JEI 初始化日志行+本 plugin 注册行）——非风险
   实证；备选 `obfuscation.createRemappingConfiguration` 挂 additionalRuntimeClasspath
   保留为预案不展开（仅冒烟意外失败时启用+ADR 附录回写）。
3. **插件类**：mdk 新包 `gregtech6.jei/GT6JeiPlugin`——`@JeiPlugin`+无参构造
   （JEI 检测契约），卡内自持不触 GT6Mod/GTModBusListener（冻结红线）；getPluginUid
   稳定常量 `gt6:jei_plugin`。
4. **信息页=内置 `addIngredientInfo` 纯文字路线**挂 coke oven controller 方块物品，
   lang 条目走 GT6EnUs provider datagen（绝不手写 JSON 红线）。
5. **降级预案**：15.56.0.205 解析失败或运行炸 → 钉 15.20.0.115（GTCEu Modern 1.20.1
   实证钉值）+ADR 附录回写。
6. **合并序裁定**：无前置依赖即刻派发，与 pattern-api/渲染卡全并行（build 面 vs
   渲染域零交叠），建议先合（小 diff 先落）——实际执行序因审查排期后于渲染卡，
   rebase 零冲突。
7. **边界写死**：JEI 纯客户端 mod，runServer 仅回归证明 impl 无害；服务端行为面、
   REI/EMI、cheat/give、mods.toml/资源 JSON 变更不做。

## Deviations（否决备选）

- **自定义 IRecipeCategory 起步否决**：GTCEu MultiblockInfoJeiCategory:39-59 走 MUI
  画布，本仓无 MUI，纯 GuiGraphics 从零画=独立整卡量级；文字页已闭环 JEI 联动价值。
  【curator API 面修正】若未来走到：`getBackground` 自 15.20.0 弃用可空，背景=
  `getWidth()`/`getHeight()`+draw 自画，禁覆写 getBackground。
- **起步即钉 15.20.0.115 否决**（保守降级预案非首选）。
- **cokeoven 配方进 JEI recipe category 裁池**（RecipeMap→IGenericRecipe 桥=另一卡）。
- **fg.deobf 类手工重映射否决**（MDG mod* 机制原生覆盖）；modmaven.dev 留兜底源。

## Consequences

- **落地**：merge 6be2862（main HEAD；链位 43c04ed → 248c66b → dbc55ef → 6be2862）。
  分支基线 975aadc，rebase main dbc55ef 零冲突后重签 c69955b/3bdd9bf/a39bb0d
  （GPG 3/3 重验）。
- **门禁**（审查官实测）：根 205/0 + mdk `:test` 737/0（=彼时 main 727+新 10；码农
  自报 715 系旧基线口径）；审查官亲跑 runData 二跑 written:0 树净=en_us.json +1 行
  确系 datagen 产物幂等；runServer 走 gt6server.py 框架（25704/25714）零 ERROR。
- **runClient 冒烟硬证据**（活证 LEGACY L74）："Creating FMLModContainer instance for
  mezz.jei.forge.JustEnoughItems"（JEI 加载）、"[PluginCallerTimerRunnable]: Sending
  ConfigManager: gt6:jei_plugin"（本 plugin 被 JEI 字节码扫描发现并调用）、
  "Generating PackInfo named mod:jei for .../transformed/jei-1.20.1-forge-15.56.0.205.jar"
  （modRuntimeOnly 重映射产物在 run 类路径）、"Created: 256x256x0
  jei:textures/atlas/gui.png-atlas"；无 FATAL 无 crash。
- **承重发现（@JeiPlugin 保留级）**：`@JeiPlugin` 无 @Retention 元注解=默认 CLASS
  保留级——审查官从 gradle cache 实取 15.56.0.205 jar，javap 无
  RuntimeVisibleAnnotations+strings 无 Retention 实证；运行时反射 getAnnotation 恒
  null，检测断言下沉到**字节码层**（描述符串在 GT6JeiPlugin.class 中的唯一来源就是
  注解应用），与 JEI ClassGraph 字节码扫描同层，测试真实非恒绿。
- **GT6EnUs 去 final 裁定成立**：LanguageProvider.add 为 public 非终态，测试子类拦
  add() 走真实全量翻译走查（含重复键即抛），比测常量强，javadoc 已声明。
- **附录勘误（信息页砖数 26→25）**：卡面"26 砖"系 checkStructure2 循环格数口径
  （26 part 格**含控制器自身一格**）；上游 MultiTileEntityCokeOven.java:63 tooltip
  字面 "3x3x3 Hollow of 25 Fire Bricks" + P6 RCON linked_parts=25/25 + pattern 表
  26 格去控制器格=25 砖——信息页文案定为 25。本 ADR 成文时回写，state decisions
  同步勘误条目。
- **遗留**：① runClient 目视两条留用户：JEI 搜 coke oven 出信息页/JEI 面板正常
  无 crash（JEI 全量 registerRecipes 在加入世界后触发，标题屏只走到 ConfigManager
  步）；② mdk 贴图既有 6 条 "Corrupt PNG"（coke oven 方块贴图，P8 借图内容问题，
  missing texture 兜底）——非本卡域，建议池卡。
