# TODO（镜像·维护期版 2026-09-27）

> 权威数据在 MCP `gt6-brain` state（`tasks.debt-*`/`tasks.p37-pool`/`ops.reboot-snapshot-2026-09-27`）。
> P1-P38 已完成项全史见 [docs/archive/ARCHIVE-2026-09-27-p1-p37.md](archive/ARCHIVE-2026-09-27-p1-p37.md)。

## 在途（还债期 2026-09-26 起）

### 审查持有 10（held_pending_review，开席五审分两席 5+5）
- debt-byproducts-data（34bf4dded）
- p37-bedrock-drill（4 提交）
- debt-flaky-clock-noise（25be631f8）
- debt-slab-gap（3 提交）
- debt-ore-purified-edge（fa5bff145；断言改写 diff 建议 material-tree-a 已合→本卡后合时其审查席应用）
- ops-rundata-leg-canonical（8b14d5c5e）
- p37-fluids-naming（4 提交 e511034d4..118663626）
- debt-emitter-sensor-generators（2 提交 969b244d1/a84fde546）
- debt-barrel-nbt-dialect（44d439c61）
- debt-ore-size-comment（已合 30a4f45f4，占位待复核销行）

### 停摆重派 6（fresh redispatch，考古阶段无提交）
- debt-jei-emi-batch2（批 2 全量开放）
- debt-material-tree-b（材料树展示）
- debt-ore-gen-display（矿物分布页）
- debt-reactor-c-rods（46 棒）
- p37-craftfrom-foil
- debt-jei-emi-batch4-transfer

### 收官批（全部合完后）
- craftfrom wireFine/rockGt+残余族
- material-tree-c
- HeNe
- 微卡群
- sodium curator
- KG 蒸馏（本卡 ops-memory-distill）
- docs 镜像（本卡）

## 权威池册

`state_read("tasks.p37-pool")`——P37-d 终审池册 23 项五族（USB/Hexorium/RM 六图/CraftFrom39/fluids 命名/bedrock-drill/rm-jei-info P3+需研究 r-reactor/r-stone-stairs-walls 等）。

## 用户侧 field_test（3 项）

- embeddium 三态
- kjs 真机三证
- 30G 统一测试门禁体感

## 恢复协议

重启后按崩溃恢复流程：`git worktree list`+各分支 HEAD 盘点 → 开席五审持有 10 → 重派被停 6 → 收官批 → 全部合完一次 push（ops.discipline.push-policy）。
