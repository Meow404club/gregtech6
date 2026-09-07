# ADR 草稿：P22 stonecutter `.color(` swap 正则劈分裁决

- 日期：2026-09-07
- 状态：Accepted（P22 收官主会话正典化 2026-09-07）
- 实现卡：state key=`tasks.p22-stonecutter-color-narrow`（merged `f58fb0a5`，commits 692f7e1b + a2463716；spec 全文见 `tasks.p22-arch-smallbatch`）
- 证据路径根：port=`mdk/src/`，swap 表=`mdk/stonecutter.gradle.kts`

## 背景与证据

1. **裸条目假阳性债务**：`mdk/stonecutter.gradle.kts:229` 裸正则 `\.color\(` 把 1.20.1→1.21.1 的 VertexConsumer `.color(...)` swap 变成全仓无差别替换。P21 Jade 卡被这个词被迫用空格锚规避：`GT6MachineProvider.java:127` 写成 `.color (tColor)`（Jade `ProgressStyle.color(int)` 是合法 21.1 API，不可换 `setColor`）——假阳性类实锤。
2. **真阳性两类**：4 参逗号形 8 处（`GTMultiBlockPreviewRenderer.java:237-240/273/277` + `GTWrenchGridRenderer.java:155/159`，VertexConsumer RGBA 四参）；单参形唯一真阳性 `GTWrenchGridRenderer.java:212-219`（`.color(tColor)`，VertexConsumer packed 单参→21.1 需 `setColor`）。
3. **regex 结构不可分辨**：VertexConsumer 单参 packed 色与 Jade `ProgressStyle.color(int)` 的调用点——接收者同为 `)` 结尾的链式结果、变量同 t 前缀、arg 同为标识符——任何接收者/边界启发式同时命中或同时漏，无正则可分。

## 裁定

### D1：按实参形态劈两路（4 参留表 lookahead + 单参删表行内分叉）

- **4 参逗号形留表**：`\.color\(` → `\.color\((?=[^)]*,)`，仅命中括号内含逗号的调用 → 换 `.setColor(`（8 处真阳性全命中；注释行 `.color(int)`/`.color(` 因 lookahead 无 ASCII 逗号不命中，census 复核）。
- **单参形从表删除**，唯一真阳性 `GTWrenchGridRenderer.java:212-219` 改 `//? if forge {` / `//? } else {` 行内分叉（forge 腿 `.color(tColor)` / neoforge 腿 `.setColor(tColor)`），正典形态=`GT6CircuitsTest.java:57`、`GT6MachineProvider.java:153`。

### D2：双向编译红 fail-visible 口径

- 漏改的 `.color(` 在 21.1 腿编译红（VertexConsumer 无该方法）；
- census 残差显式声明：`ARGB32.color(` 等 4 参静态工具形仍会被 4 参条目命中 → 21.1 编译红=**fail-visible 非静默**，swap 表允许这种"误命中必炸"的粗粒度，禁止静默语义错。

### D3：维护启发入表注释

swap 表维护规则固化为注释：**regex 不可分辨接收者时，按调用形态（实参个数/逗号）劈条目；形态也不可分辨时，降级 `//?` 行内分叉**（本卡两类各用其一）。P21 空格锚规避点随卡归一：`GT6MachineProvider.java:127` `.color (tColor)` → `.color(tColor)`——收窄后单参不进表，空格锚即收窄生效的活体证明。

### D4：验收（merged `f58fb0a5`）

双腿 compileJava 绿+全量套件绿（forge 147 类/1189 测、neo 147 类/1191 测）；21.1 生成树运行时证据=既有 RCON 链一条双遍 [0,0]；census：4 参 8 处全命中、单参唯一真阳性行内分叉、VertexConsumer 单参 `.color(` 残留=0；diff 红线=swap 表其它条目（vertex/normal/uv/uv2/endVertex）零变更。

## 后果与池项移交

- Jade `ProgressStyle.color(int)` 类第三方 API 调用点从 swap 表射程永久豁免（单参出表），后续 Jade/GUI 卡不再需要空格锚类规避技巧。
- 表注释即后续 swap 条目扩面时的操作手册（劈形态优先、`//?` 兜底、编译红为可接受失败模式）。
- 留痕核实（debt_handover）：p15_runtime_smoke 21.1 dryer 竞态红 + p16_pattern_checker 21.1 namespace drift 均已移交 `p22-oven-dual-verify` 判读，不属本裁定面。
