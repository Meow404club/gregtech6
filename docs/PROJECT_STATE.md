# 项目状态（镜像·维护期版 2026-09-30 · r10）

> 权威数据在 MCP `gt6-brain` 的 state/记忆/KG 里。本文件自 2026-09-27 起改为**维护期精简镜像**：
> P1-P38 逐阶段详账已蒸馏归档至 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)
> （阶段总表+还债期卡终态表+被归档记忆全文+删除清单）。ADR 在 docs/adr/（44+ 篇），已完成卡的过程明细随 git 历史。

## 项目

GT6（1.7.10 GregTech6）→ 现代移植：1.20.1 Forge + 1.21.1 NeoForge 双节点（Stonecutter）。
内容完成度 97-98%+，2026-09-24 发布 v0.1.0（GitHub Release，双 jar），现处**还债/维护阶段**（2026-09-26 起）。

## 当前态（2026-09-30 · r10 还债波收官）

- **r10 还债波七卡全部合入**（main=fb479f3f1，两审查席 5+2 全 approve，用户指令「把还债的能做的先做了」）：
  **数据丢失级两枚**：r4-24d 大机 mTanksInput 字段遮蔽（12 台大机输入罐存档即失+大 Mixer/Bath
  多流体配方静默不可达→删遮蔽+fusion 先例重接基类，红绿法实证）与 r4-19b 六族破坏掉落缺口
  （BatteryBox/ZpmDecharger/GeneratorSolid/ReactorCore2x2/ItemPipe/Crucible 各补公共
  getInventory()，六探针镜像反射探测；Crucible 上游掉 slot-0 喂料熔融物 trash 不掉=零偏差核过）。
  **玩家面**：nojade NCDFE（不装 Jade 开配方页必崩→能量短码表迁 ViewerMeta+字节面守卫）+
  NEI 齿轮装饰位机器物品（JEI renderItem/EMI SlotWidget）。
  **观感面**：材质 tint 返工（陶瓷模具/坩埚/水龙头按材质 mRGBaSolid 着色，stone 行 vanilla 成品图
  不染；ItemColors/BlockColors 同源）+#17 锅炉过半满提示两脸（单罐 .14+多方块 .19 port-authored）。
  **配方面**：宝石姊妹行（wrench/monkey C 变体+hammer G 变体直合成，上游逐字，+1074 JSON 零幽灵）。
- **盘点收获**（research.r10-r4pool-survey）：r4-24c 已被 r8 顺带交付销卡；r4-24b（大机 active 贴图
  三态属性接线）素材 r8 已备齐=次批待做。
- 审查席经验沉淀：rebase 后 tree_check 必报 STALE（neo runData 同轮再生即愈）；XML 时间戳必核
  新鲜度（逮住一次作者旧 XML 假证）；cmd|tail 掩蔽 gradle 退出码。
- 收官在途：docs 镜像（本卡）+push+CI 确认。
- **ops 待用户裁定（持续）**：FML 测试启动面峰值 12-13.5G 三撞 filtered 12G 预算墙（--max-workers=1/
  --task-cap 16 可过闸；疑似 KJS 增重）。

## 前态（2026-09-30 · r9 issue 三修波收官）

- **r9 波（#34/#39/#41）三卡合入+push**（main=1b2e1a4cc，三笔 merge 一审查席零打回）：
  **#41 模具凹凸反转**（36e6eef6d）：上游渲染门 :537「bit=1 不画=挖」位极性被移植读反——
  翻转为阴模+补四壁+选择/碰撞盒上游 verbatim+mold_stone 骑行（顺手清掉 visual-sync 债）；
  **#39 工具头占位符**（3a786a589）：9 族头行+宝石变体 3397 行（含 's'=SAW 小写字母表翻案，
  裁决 decisions.r9-toolhead-s-letter）+材质门（typemin/qualmax/qualmin）；三项判非 bug
  （镶尖镐基底=上游语义/三电头无消费者=上游如此/arrow 头走铸造+loot）；
  **#34 查看器错位**（1b2e1a4cc）：根因=JEI/EMI 配方页**零背景**（r6 batch1 declared deviation
  真身）——按上游 NEI 两层合成复刻（底板 NEI.png 裁 (5,16)+机器带裁 (5,11)）+槽位 sOffset(5,11)
  单点折叠+textBaseY 偏差退休（顺带修 FUSION 溢出）+2 PNG amazawa 版补借。
- **视觉取证方法论首战**（#34 确诊链）：runClient+xdotool+F2 截图+像素统计（裸灰面 61%→30%、
  色彩数翻倍）+4x 放大目验；**视觉模型低分辨率判读两度幻觉**（紫块误报=零洋红像素证伪），
  像素统计是硬证据。EMI dev 投放管线=FART 重映射+refmap 补丁（/tmp/emi-final4.jar，可复用）。
- 副产物真 bug 两枚入账：nojade 下配方页 draw 必崩 NCDFE（known_bugs.r934_*，workaround=带 Jade 跑）；
  worktree 子模组 init 坑（--force remove+protocol.file.allow）。
- may fix 三挂（#34/#39/#41，push 后）；不主动关评（铁律 9）。
- **ops 议题待用户裁定**：neo 腿 FML 测试启动面峰值 12-13.5G 三撞 filtered 12G 预算墙
  （r9-39 审查 13.5G/本夜两次；--max-workers=1 或 --task-cap 16 可过闸）——调预算或查
  FML 启动面增重根因（疑似 KJS？）。
- CI：run 36626354007 在跑（push 后滚动 dev prerelease 自动更新，提出者可下载复验三修）。

## 前态（2026-09-29 · r8 波收官）

- **r8「贴图保真+信息面现代化」波收官**（main=a17a51571，本波 37 笔 merge 九席
  审查零打回全 approve；未闭合债镜像见 [docs/TODO.md](TODO.md)）：
  贴图全量（R2 五批次+itemform AB+占位复核+multiblockmains+桥/动能+传感器+锅炉/储罐/管道）、
  GUI 现代化（amazawa 换皮 73+布局描述符 B 双清 r4-24a 债+部件裁切）、
  Jade J1-J3 重设计（B 案条式/Malfunction/水汽常态）、
  tooltip 体系从零到全量（T1-T5 十三族+GT6MachineBlockItem 载体+ZH_KEY_FLOOR 棘轮）、
  #45 黏土线 C1-C3、issue 四修（#27 canonical 根因=StaticFaceBakery 表被 vanilla 语义消费+
  清扫微卡链翻案 CoverPlate；#42/#42b；#47+stick）、材料树 v2、晶洞回退。
- **门禁 v3→v3.6**（test-gating，tools/gt6testgate.py）：v3 预测准入→v3.1 残留清剿
  （--no-daemon+scope 清扫）→v3.2 信封内准入→v3.4 剔缓存→v3.5 外压护栏退役
  （结构性死锁）→v3.6 信封帽 22G；宪法铁律 8 已同步 v3.x 语义。
- **三次 WSL 崩溃全恢复**（2026-09-29）：分支幸存+续作卡模式成熟（rebase 续作/
  交卡自评基线显式声明）。
- 收官在途：docs 镜像（本卡）+filtered 12G 调优+curator+攒批 push+CI 双腿确认+
  may fix 挂（#27/#42/#45/#47，push 后）+记忆蒸馏（handoff/merge 历史硬删）。

## 前态（2026-09-28 · r5-r7 波收官）

- **r5/r6/r7 三波 26 卡全部合入并推送**（main=d3fb2acd7，CI 全绿 36428109853）：Jade 信息面（锅炉热量+全机器状态行）、CI 手动 sweep 工作流、issue 模板+标签体系（[Bug]/[Feat]/[RFC] triage+p1-3+may fix）、README 重写、轨道空白/EMI 几何/蜂巢染色修复、**主世界 C 融合档落地**（原版团块屏蔽+GT 晶洞+budding+透镜伴生矿+矿石轴 53→122 两波扩轴+GT 铜唯一+深层地质带）、JEI/EMI 图标汉化+能源拆分、工具头组装链归位、模具资产+形状几何、泡沫防水、三 NPE 修复。
- **#17/#31 判决非 bug**（火盒档位饥饿观感死机/负坐标普查四象限绿），判决文案在任务板。
- may fix 已挂 17 张修复 issue（提出者验证后自关）；未处理：#27/#28/#35-38。
- 工作流纪律：流水线审查（恒单会话滚动）/push 攒批点名制/may fix=push 后挂/全量 sweep 退役（宪法③）/不主动关评（铁律 9）。
- 残余池：1% 层内小宝石小代码面、crack 可发现性、mold_stone 视觉同步、机器贴图债、镜像新鲜度门、信息页深带显示。
- 治理待办：KG 蒸馏（3355 节点超阈）、handoff 历史软删（锚点 id1051 已蒸馏）。
- 基础设施注记：gt6-brain MCP 写路径偶发慢响（写超时≠写失败）；datagen treecheck 假红处置=rm -rf build/datagen-output。

## 维护期记忆入口

| 层 | 用法 |
|---|---|
| 语义记忆 | `recall("<查询>")`（按 kind 过滤）；维护期保留 lesson/bug/decision 全量 + 2026-09-26 后 handoff/merge/research |
| state 账本 | `state_read()` 目录页（现仅 ~230 键：known_bugs*/ops.discipline*/decisions.*/还债期活键/*pool*）；语义定位 `state_search(query)` |
| 知识图谱 | `kg_search`（语义）/`kg_query`（带过滤）；已合卡过程边已双时态失效（历史可查，检索不再命中） |
| 归档 | `docs/archive/ARCHIVE-2026-09-27-p1-p37.md`——P1-P38 全史唯一归档（软删记忆全文可按 id 恢复） |
| 检索库 | `search_code`（GT6 1.7.10/原版反编译/NeoForge API/GTCEu Modern/project 本仓） |

## 活跃 state 键速查

- `known_bugs`+`known_bugs.*`：现存 bug 台账（open/pool/field_test 状态在条目内）
- `ops.discipline.*`：工作流纪律（test-gating v1-v3/push 策略/审查流水线/subagent 模型）
- `decisions.*`：全部裁决（不归档，持续追加）
- `ops.reboot-snapshot-2026-09-27`：重启快照（在途/持有/重派/收官批/恢复协议）
- `tasks.debt-*`/`tasks.p37-*`（活跃卡）/`tasks.r3-*`/`tasks.issues-round3*`：还债期流水
- `tasks.p37-pool` 等池键：权威池册与活跃缝
