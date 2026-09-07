# ADR 草稿：P21 ModularUI smoke-gui 范围裁决——离线 headless JUnit（路线 b）

日期：2026-09-07 ｜ 状态：Accepted（P21 收官主会话正典化 2026-09-07）｜ 基线：main 53ba0f31
卡：p21-modularui-smoke-gui-headless ｜ 池项：p20-modularui-smoke-gui（GUI 表达式端到端）

## 裁决

smoke-gui 走 **(b) 离线 headless**：直接构造 ModularScreen/ModularPanel + widget 树，
经 vendored 库共享层 MathUtils→EvalEx（com.ezylang.evalex 3.6.0）做表达式求值断言的
JUnit 冒烟。GUI 逻辑层端到端，不进渲染。

## 四维评估

| 维度 | (a) 真 runClient 自动化 | (b) 离线 headless JUnit |
|---|---|---|
| 无人值守 | runClient 需 display/Xvfb + 截屏/日志断言；本仓 runClient 从未跑通（视觉整备 backlog 在案），先跑通 runClient 本身即独立高风险前置 | gradle test 纯 headless，与现有 JUnit 门同通道 |
| 双腿可重复性 | 分钟级 ×2，环境差异脆 | stonecutter 双节点同形 test 任务，秒级 |
| CI 面 | runner 需帧缓冲支持 | 零新 CI 要求 |
| 维护成本 | 截图断言随渲染/主题改动爆炸 | 断言面窄（构造+求值+树不变量）；测试放第三方项目自持 test sourceSet，vendored 657 java 零触碰 → gen-forks.py 重放（git restore src/main 语义）零负担 |

(c) 缓拆否决：缺口已被交付物自证——tools/jarjar_smoke.py:27-29 明写「探针主动触发类加载
≠表达式求值端到端」，池项悬置即该缺口无限期开放。

## 可行性求证（已实证，非猜测）

- 表达式入口在**共享 main**：brachy/modularui/utils/math/MathUtils.java:13-44（纯
  EvalEx Expression/ExpressionConfiguration，MATH_CFG 挂 SIPrefix/PostfixPercentOperator）。
- ModularScreen 构造纯数据直至单一事件总線 post：forgeMain screen/ModularScreen.java:162
  `MinecraftForge.EVENT_BUS.post(BuildPanelEvent.MainPanel)`——裸 JVM 携 loader 类路径即可
  实例化（neo 腿同构 Neo 事件总线）；ClientGUI.open（:31/:53）才是 MC 客户端单例面，排除。
- 上游自带配方：共享 main test/TestGuis.java（874 行）的 panel/widget 构造形态可直接抄最小面。
- TextFieldWidget 表达式路径：widgets/textfield/TextFieldWidget.java:49-60
  acceptsExpression → MathUtils.parseExpression。

## 边界（如实声明）

(b) 证明 GUI 逻辑层（Menu/panel/widget 树 + 表达式求值），**不**证明渲染/主题与生产嵌套
jar 类加载域——后者由已交付 p20-modularui-jarjar-packaging 的 jarjar_smoke 承担，两冒烟
互补合成端到端；完整视觉端到端仍归 runClient 视觉整备池项，本裁决不取消该池项。

## 卡内遗留 probe（一个小点）

第三方项目 test sourceSet 的 testRuntimeClasspath 是否携带 MC+loader 类（stonecutter 插件
配置形态，forge/neo 依赖常为 compileOnly 口径）——若不下探，卡内补 testImplementation
自同配置接线。评估为接线问题非可行性问题（mdk 测试已有 Bootstrap.bootStrap() 裸 JVM 先例，
GT6LangParityTest:114-125）。
