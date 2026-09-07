# ADR-P23 草稿：ModularUI 拆独立 git 仓库 + submodule 按原路径挂回（案A）——推翻 ADR-P20 §2 裁决一

日期：2026-09-07 ｜ 前置：ADR-P20（docs/adr/2026-09-06-p20-modularui-fork-ruling.md） ｜ 评审基线 main=0d57e5c1 ｜ 决策卡 state decisions.2026-09-07-p23-modularui-subrepo ｜ 实现卡 work/p23-modularui-subrepo

- 状态：Accepted（P23 收官主会话正典化 2026-09-07）

## 0. 用户指令与裁决

2026-09-07 用户指令：modularui 应作为**子仓库**（独立一个仓库）。调研（P23 三卡）就此选型定案=**案A：独立仓 + git submodule 按 `third-party/modularui` 原路径挂回**；新仓历史用 `git subtree split -P third-party/modularui` 全量保留。本 ADR 正式推翻 ADR-P20 §2 裁决一（「fork 落点 = 仓内 vendored 模块，不建独立仓」）——该裁决的 LGPL 义务、fork 身份、DIVERGE 台账结论全部继续有效，**仅落点形态**由「仓内 vendored 目录」改为「独立仓 + submodule」。

## 1. 案A 实施（已落地）

- 新仓：`/home/brokestar/workspace/MGT6GA/MGT6GA-repos/modularui.git`（bare，默认分支 main）。历史=subtree split 全量 7 提交（底 `6cb2e813` 上游快照导入，顶 `a078656` test 基建）；**split tip 树哈希 `14e0fc21` 与主仓 `HEAD:third-party/modularui` 树哈希逐位一致、diff 为空**（迁移无损断言）。bare 仓只含 modularui 历史，严禁把主仓历史 push 进去。
- 新仓补丁：`.gitignore`（build/ .gradle/ run/，拆仓前靠主仓根全局规则）+ `tools/gen-forks.py:29`/`DIVERGE.md:17` 重放指令改指子仓基线 `6cb2e813`（原「主仓 `git checkout <基线> -- third-party/modularui/src`」语义随拆仓失效；现=子仓根 `git checkout <子仓基线> -- src`）。
- 主仓：单一 cutover commit（gitlink + .gitmodules，回滚 = revert 即回 vendored）；`.gitmodules` URL=绝对本地路径（主仓暂无 default remote，将来推远程后 set-url + `git submodule sync` 即切相对 `../modularui.git`）。settings.gradle.kts:26-29 注释同步子仓形态。
- 接线零改动实证：三处消费全路径型/git 无关——settings.gradle.kts:30（include）+:48（stonecutter create 第二控制器）、mdk/build.forge.gradle.kts:103-108 与 build.neoforge.gradle.kts:129-135（jarJar(project) 消费）、tools/jar_content_check.py:44,113-115（纯路径断言 REPO/third-party/modularui/versions/<leg>/build/classes）。

## 2. 五维对照（案A vs 案B mavenLocal/file 仓坐标 vs 案C composite includeBuild）

| 维度 | 案A submodule（采纳） | 案B 坐标替换 | 案C composite |
|---|---|---|---|
| gradle 接线 | 零改动（三处消费路径型，gitlink 下目录内容不变） | settings include 删、mdk 两处 jarJar 改坐标、jar_content_check 改 Maven 定位 | includeBuild 替换 include，第二控制器迁移 |
| jarJar/SRG 变体 | 保留 ProjectDependency——MDG 重映射 `instanceof ProjectDependency` 才注入 SRG 属性（tmp/harvest/moddevgradle-src/.../LegacyForgeModDevPlugin.java:214-229），reobfRuntimeElements 变体照选 | **死点**：外部依赖「已在正确命名空间」不重映射，须自建 SRG 发布面（未探明；GTCEu 用 Loom 不可类比） | **死点**：composite 替换「永远指向默认 configuration」，失去变体选择（Gradle composite_builds 文档） |
| 历史保留 | subtree split 全量 7 提交合成完整历史（含 merge），重放 commit id 确定 | 依赖 jar 不带源史 | 同左 |
| worktree 并行 | 各 worktree 独立检出天然隔离；唯一代价=每 worktree 首次 init（见 §3） | mavenLocal 机器级共享态与 worktree 并行**相克**（并行卡互踩发布面） | composite 目录指向与 worktree 路径耦合 |
| 跟随上游/发布 | 重 vendored=子仓内换基线+重放偏离（gen-forks 照用）；将来推远程一条 set-url | 发版摩擦（每次跟随上游需 publish） | 构建入口耦合 |

案B/C 共同死点=MDG jarJar 重映射只认 ProjectDependency；换坐标即失联。

## 3. worktree × submodule 纪律

每个 worktree 首次检出后需 `git -c protocol.file.allow=always submodule update --init`（CVE-2022-39253 后本地路径协议默认拒；失败模式=响亮配置错，不是静默错）。`git worktree add` 不自动检出 submodule。

## 4. 验收留痕（实现卡自测）

- 主仓 worktree `git submodule status` 干净（无 -/+ 前缀），gitlink=新仓 HEAD。
- 双腿各自独立 gradle 调用 clean+assemble --no-build-cache BUILD SUCCESSFUL，jarJar 嵌装完好，tools/jar_content_check.py 双 jar 全 PASS GREEN。
- 全新 worktree（../MGT6GA-trees/p23-modularui-subrepo-verify）init submodule → assemble forge 腿 GREEN（证明新克隆可用）。
- 死引用复扫：`third-party/modularui` 全量命中逐条核对，路径未变 → 构建与工具链引用零死引用（.gitmodules/gitlink/历史 docs 除外）。

## 5. 影响面

ADR-P20 其余裁决（fork 身份钉死 brachy84/ModularUI-Modern、LGPL 三件套、DIVERGE 台账、jarJar 嵌装策略）不受影响；FORK.md/DIVERGE.md/LICENSE 随子仓走。maven 发布形态仍按 docs/TODO.md:235 池项（「maven 发布形态时再议」）——案B 否决不改。
