# ADR-P20：ModularUI 自维护跨版本 fork —— 落点/LGPL/jarJar 三项裁决

日期：2026-09-06 · 裁决人：architect（用户已定案 fork 总方针，本 ADR 落三项执行层裁决）
状态：正典（P20 开段成文，主会话 promote）

## 0. 背景

用户裁定（todo.current/user_rulings_in_force，2026-09-06）：ModularUI 集成为自维护跨版本 fork，
JarJar 内置 mod jar，后续本仓 GUI 框架逐步用它。跟随上游=跟 GTCEu 走。

## 1. 上游身份与基线（证据）

- GTCEu Modern（本仓 tmp/refs/gtceu-modern，1.20.1 Forge 腿）钉：
  `mui = "3.3.1-SNAPSHOT"` + `brachy.modularui:modularui-mc1.20.1`（tmp/refs/gtceu-modern/gradle/forge.versions.toml:10,62），
  经 `jarJar(modImplementation(forge.mui.get()))` 消费（tmp/refs/gtceu-modern/dependencies.gradle:14）。
- GTCEu Modern 1.21 分支（NeoForge 1.21.1）同钉 3.3.1-SNAPSHOT + `brachy.modularui:modularui-mc1.21.1`
  （raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.21/gradle/forge.versions.toml，访问 2026-09-06）。
- 源仓 = brachy84/ModularUI-Modern（"Port of ModularUI to modern Minecraft versions"，
  https://github.com/brachy84/ModularUI-Modern ，访问 2026-09-06）。
  分支 `1.20.1`@909cda2 与 `1.21.1`@c13e141（均 protected，GitHub API 访问 2026-09-06）。
- maven.gtceu.com 托管 modularui-mc1.21.1：latest 3.3.1-SNAPSHOT（build 2026-07-18.084355-4），
  与 GTCEu 钉版一致（maven-metadata.xml，访问 2026-09-06）。
- License：LGPL-3.0 全文随仓（tmp/harvest/modularui-upstream/LICENSE，165 行）。
- 已收割：tmp/harvest/modularui-upstream/（1.21.1，592 文件/2.7MB）、
  tmp/harvest/modularui-upstream-1201/（1.20.1，596 文件/2.74MB），2026-09-06。

## 2. 裁决一：fork 落点 = 仓内 vendored 模块（方案 a），不建独立仓

裁 a)：`third-party/modularui/` 落源 + 自有构建脚本挂进 settings.gradle.kts；否决 b) 独立 fork 仓库+本地 Maven 发布。

理由：
1. **发版摩擦**：b) 每次改动需双仓同步+publish+版本推进，SNAPSHOT 时序与两端 drift 是持续税；a) 零发布摩擦，GUI 框架迁移期改动频率高，摩擦被放大。
2. **jarJar 合流**：a) 下 vendored 子构建直接产出 per-leg mod jar 由本仓 jar 任务嵌套（工程面复用 main 50843dff 自包含姿势）；b) 多一跳 resolve，无增益。
3. **Stonecutter 接线**：上游跨版本模型=按 MC 版本分分支+互 merge（存在 sc/merge-1.20-into-1.21/up-to-#73 分支为证；两腿同版 3.3.1-SNAPSHOT）→ 代码主体共享、diff 是机械缝。转成我们单树+`//?` 分叉语义等价，且与本仓双腿管线同构（包名 swap 表先例：多方块预览研卡 keybind 双腿消化）。
4. **LGPL 义务等价**：b) 的合规优势不存在——义务（LICENSE 保留+修改声明+对应源可得）在 a) 下由目录内 LICENSE 原文+THIRD_PARTY 声明+偏离台账+分发时 sources 可得满足。
5. **代价与对冲**：a) 跟随上游不能 git merge，需重放补丁。对冲：逐文件偏离台账（卡①交付物）+上游跟随频率低（跟随事件=GTCEu 换钉版，可规划；监视 maven.gtceu.com 两 artifact 的 maven-metadata.xml）。

接线形态留给卡① probe 定形（stonecutter 多项目 create vs mdk 第二 sourceSet 二选一），本 ADR 不拍死实现。

## 3. 裁决二：LGPL-3.0 合规义务清单

- LICENSE 原文（LGPL-3.0，165 行）随 vendored 目录保留，不得删改。
- THIRD_PARTY/README 声明：上游 URL + 基线 commit（1.21.1@c13e141）+ "本仓含修改" +
  逐文件偏离台账（chisel 标注文件逐一登记）。
- 分发义务：mod jar 内嵌的 modularui 子 jar 保留其元数据与来源标识（Specification-Title/Vendor: brachy，
  上游 jars.gradle:20-27 姿势沿用）；sources jar 或公开仓源码满足 §4/§6 对应源义务。
- 本仓 mod（Combined Work）自身许可不受 LGPL 传染（LGPL-3.0 §4），仅需显著声明+许可证副本随附。

## 4. 裁决三：jarJar 内置姿势（双腿）

- 双腿 jar 任务各自 jarJar 嵌入 **本腿自建** 的 modularui mod jar（1.20.1-forge 腿=Forge 元数据 mods.toml；
  1.21.1-neoforge 腿=neoforge.mods.toml；各带 mixin json + pack.mcmeta）。
- EvalEx 3.6.0（modularui 唯一硬 runtime 依赖，其 dependencies.gradle:9 以 jarJar 携带）：
  上游 GTCEu 顶层只 jarJar(mui) 量产在跑（dependencies.gradle:14），嵌套存活机制不猜——
  卡②验收钉死实测：双腿产 jar unzip 验证 META-INF/jarjar/ 内 modularui jar 在列 + EvalEx 类加载断言/runServer 冒烟。
- 打包对账守卫复用 main 50843dff 姿势（jar 内容对账进 selftest），modularui 条目加入守卫清单。

## 5. 体量与移植面（1.21.1 NeoForge 视角）

- 1.21.1 分支已是 NeoForge 原生：注册=DeferredRegister/DeferredHolder+IMenuTypeExtension
  （ModularUIMenuTypes.java:8-22）；网络=CustomPacketPayload record（network/packets/OpenGuiPacket.java:19 等）；
  items=net.neoforged.neoforge.items.*（PlayerSlotType.java:5-7、ModularSlot.java:13-15）。
  → 1.21.1 腿接近零移植；**真正工作量=回向 1.20.1 Forge 腿的 chisel 化**。
- 五缝（chisel 分叉清单，file:line 为 1.21.1 侧锚点）：
  ① registries：neoforged.neoforge.registries→forge.registries；② bus：neoforged.bus.api→forge.eventbus.api；
  ③ distmarker：neoforged.api.distmarker→forge.api.distmarker（TextFieldRenderer.java:13-14）；
  ④ items wrapper：neoforge.items→forge.items（包名 swap）；⑤ hooks：CommonHooks/EventHooks→ForgeHooks 系（ModularCraftingSlot.java:16-17）。
- 预估：chisel 触碰文件两位数（<<50），其余 ~90% 文件（widget/theme/drawable/api 主体）双腿零改共享。
- Mixin 面：core/mixins client 7+common 10+jei/emi（tree 计数），最大 GuiGraphicsMixin 5.4KB；mixin 配置双腿各自成 json。
- 硬依赖仅 EvalEx 3.6.0；JEI/REI/EMI/Curios/Sodium/MouseTweaks 全 compileOnly 可选（dependencies.gradle:12-20）——
  fork 可裁剪不用的 integration 缩小维护面（裁剪决策留卡① probe 后定，默认先全量保真）。

## 6. 首里程碑卡

- p20-modularui-vendor-dual-leg（高险先导）：落仓+双腿编译绿+偏离台账+LGPL 三件套。
- p20-modularui-jarjar-packaging（DEPENDS_ON 前者）：双腿 jarJar+嵌套/守卫验证。
- p20-modularui-smoke-gui（池，缓拆）：最小 Screen/Menu 冒烟，并入 GUI 采纳规划时再启动。

详卡见 state tasks.p20-arch-modularui-fork。
