# gt6rcon — GT6 headless 验收的 RCON 正典工具

单一正典客户端 `gt6rcon.py`（蒸馏自 5 份 P4 参考脚本，重复实现已合并）。
测试验收链一律复用它，不要再写任务本地 RCON 脚本。

**三层框架（2026-09-02 起，新卡 RCON 链一律走框架，链条入库 `tools/rcon/chains/`）**：

```
gt6server.py   生命周期层：eula 预检 / ss 端口预检顺延 / nohup 起服 / Done 轮询 / 按 PID 精确停服
gt6world.py    世界层：站点注册表 → bbox 自动清场（漏站点=测试必红，结构性消灭静默污染）+ 摆放 helper
chains/        链层：每卡一条 python 模块（sites + lifecycle 配置 + steps），声明式、可审查复放
gt6rcon.py     客户端层：帧协议/断言判定（judge_output），CLI 与 chains 共用同一套判定语义
```

约定：**新卡的 RCON 验收链不再手写 bash 段、不再落 tmp/**——在 `chains/` 建一条
`Chain` 模块提交入库，`python3 tools/rcon/chains/<卡名>.py` 一条命令完成
起服→两遍跑（第二遍=幂等证明，清场由 gt6world 自动 bbox）→按 PID 停服。
旧 tmp 链是历史工件，不迁移不删除；要迁移见文末「从旧链迁移」。

```
# 单条命令
python3 tools/rcon/gt6rcon.py --password <pw> "gt6oven place 30 64 30"
# 多条链式执行 + 期望断言（--expect N:子串，N 为 1 基命令序号，可重复）
python3 tools/rcon/gt6rcon.py --password <pw> \
    "gt6pipe place 10 64 10 5" "gt6pipe stat 10 64 10" \
    --expect 1:connections\ 16 --expect 2:ioMask\ 0
```

**预期失败的步**：`--allow-failed N`（N 为 1 基命令序号，可重复）把该步标记为可失败——
输出含 `FAILED` 字样或 `--expect` 未命中时记为 **ALLOWED**，不计入退出码，链照常走完
（多盖共存断言场景：「重复装同面必被拒」的探针步夹在链中间，不该让整条验收挂掉）。
默认不传此参数 = 从前行为逐字节不变（任何失败 → 退出码 1）。示例：

```bash
# 第 3 步重复安装同面 cover 必被拒（失败行含 FAILED）——标记后整链退出码 0
python3 tools/rcon/gt6rcon.py --password <pw> \
    "gt6oven place 40 64 40" \
    "gt6cover install 40 64 40 up" \
    "gt6cover install 40 64 40 up" \
    "gt6cover check 40 64 40" \
    --expect 2:ok=true --allow-failed 3 --expect 4:store=alive
```

import 侧同义：`gt6rcon.run_chain(..., allow_failed={3})`（1 基序号集合）。

退出码：0 成功；1 断言失败；2 认证失败（AUTH FAILED）；3 连接失败（CONNECT FAILED）。
慢指令（如 `gt6oven run <大 tick>`）加 `--response-timeout 30`。
import 复用：`sys.path.insert(0, "tools/rcon"); import gt6rcon`，用
`gt6rcon.RconClient(host, port, password)`（上下文管理器自动 connect+auth）或模块级
`connect()/auth()/run_command()/run_chain()`。

**quiet_window 自适应收敛（P18）**：`RconClient` 每 connection 维护自己的收集尾窗——
单帧响应按 `QUIET_DECAY=0.7` 衰减（硬下限 `QUIET_FLOOR=0.05s`；配置已低于下限的
窗口绝不回抬），见到**任何**第二帧立即重置回本 connection 配置的满窗（截断真多帧
body 会伪造 FAIL，宁保守勿激进），零帧（沉默）不动。固定 0.5s 尾巴对占绝对多数的
单帧命令是纯残差，全集 sweep 要付几百次。开关：`GT6_RCON_ADAPTIVE_QUIET=off`
（或构造参数 `adaptive_quiet=False`）恢复固定 0.5s 历史行为；模块级 `run_command`
签名与 `judge_output` 判定语义零变；并发波下每链独立 connection（framework 线程
各持一个 client），per-connection 状态线程自洽。

## ① 帧协议与包类型

```
+------------+----------+----------+-----------------+----------+
| int32 LE   | int32 LE | int32 LE | payload (UTF-8) | NUL  NUL |
| length     | id       | type     |                 |          |
+------------+----------+----------+-----------------+----------+
```

- **length 覆盖自身之后的一切**（id+type+payload+两个 NUL）——漏前缀服务器直接断连。
- 客户端请求：type 3 = AUTH，type 2 = EXECCOMMAND。
- 回包（以 vanilla 1.20.1 反编译为准，`RconClient.java:84/:122/:125-133`；5 份参考脚本
  实测行为一致，且都不检查 type）：
  - AUTH 成功：type 2、id 回显、空 payload；失败：type 2、**id=-1**。
  - EXECCOMMAND 回包：**type 0**、id 逐帧回显；超 4096 字符按 4096 分帧连发（同 id）。
  - 本客户端按 id 匹配、与 type 无关，两者通吃。
- **杂散帧**：auth 回包后实测偶有 0-id 帧（疑似 Forge 侧产物）；客户端 auth 后排空 1s
  窗口，命令阶段不匹配 id 的帧一律丢弃，迟帧/分帧不会错配。
- **尾部 NUL 在 body 内，流上无额外填充**：两 NUL 计入 `length`——vanilla 发送端
  `writeInt(payload.length + 10)` 后补写两 NUL（`RconClient.java:112/:116-117`），读完
  定长 body 后流上没有更多 NUL。剥 NUL 二选一：定长 body 内 `body[8:-2]`
  （`gt6rcon.py:87`）或按 `length - 10` 直读 payload，**不许叠加**。
- **双剥鉴戒**：P4 时代任务本地脚本曾按 `length - 10` 读 payload 后又盲切 `[:-2]`，
  把真实载荷末两字符一起剁掉，断言子串恰在尾部时必挂（脚本已淘汰不入库）。读帧
  统一走 `read_packet` 一条路。
- **失败文本也回传**：成功与失败行都进 RCON 缓冲（`RconConsoleSource.java:36/:45`）。
  GT6 全部验收指令的失败行都含字面量 `FAILED`（GTFluidPipeCommand.java:161/:207、
  GTCoverCommand.java:125/:162、GTOvenCommand.java:149 等实证），故客户端把输出含
  `FAILED` 判为本命令失败。注意 `gt6multiblock check` 的未成型报告走 sendFailure 但
  **不含** FAILED 字样——反向断言用 `--expect N:block_formed=false`。
- **命令长度上限**：服务器单次 `read(buf,0,1460)` 且要求整包一次到达
  （`RconClient.java:48/:55`），单条命令 payload 控制在 ~1400 字节内，超长直接断连。

## ② 服务端开启 RCON

（手工姿势存档——框架用户不用做这步，`gt6server.provision_run_dir` 自动写
eula + ports + online-mode；本节留给手动调试参考。）

`mdk/run/server.properties`（整个 `mdk/run/` 已 gitignore，**不入库、测试后还原**）：

```properties
enable-rcon=true
rcon.port=25575
rcon.password=<测试口令>
online-mode=false   # 仅 headless 无正版账号时；测完还原
```

- `eula.txt` 需 `eula=true`。新 worktree 的 `mdk/run/` 是空的——先从主仓库复制
  `mdk/run/eula.txt` 与 `server.properties` 再改。
- 起服后确认监听：`ss -tlnp | grep 25575`。

## ③ nohup + 短轮询纪律

（手工姿势存档——`gt6server.start_server/wait_done/stop_server` 就是本节纪律的
模块化落地，框架用户不用手敲。）

游戏本体永不自行退出——严禁前台跑、严禁阻塞等待退出。唯一正典姿势（在 worktree 根，
**必须 `./gradlew`，系统 gradle 8.7 过不了 MDG**）。服务端一律带 `--nogui`
（用户裁定 2026-09-04：DedicatedServer 控制台 GUI 不许弹出；两节点的 server run
配置已内置该程序参数，经框架 `gt6server.start_server` 起服即自动生效）：

```bash
nohup ./gradlew :mdk:runServer > /tmp/gt6_rs_<slug>.log 2>&1 & echo $! > /tmp/gt6_rs_<slug>.pid
# 轮询【读日志文件】等 Done（60s 预算）：
for i in $(seq 1 60); do grep -q "Done (" /tmp/gt6_rs_<slug>.log && break; sleep 1; done
grep "Done (" /tmp/gt6_rs_<slug>.log
# 60s 未见 Done：必须先查进程再动——绝不对已死进程长轮询：
kill -0 $(cat /tmp/gt6_rs_<slug>.pid) && echo ALIVE || echo DEAD
tail -50 /tmp/gt6_rs_<slug>.log
# 收尾：
kill $(cat /tmp/gt6_rs_<slug>.pid)
```

GT6 指令需权限 2，RCON 即 console（权限拉满）直接可用。RCON 源位置是 (0,0,0)，
**坐标一律显式给出**，且用出生点附近区块（`BlockPosArgument` 要求区块已加载；保险起见
先 `forceload add <x> <z>`）。

## ④ 五族机器标准验收链目录

每链 = 前置 / 命令序列 / 期望断言。语法均已对 mdk 源码核验（括号内为证据行）。
面序 0..5 = down/up/north/south/west/east；SBIT={1,2,4,8,16,32}
（TileEntityBase09Connector.java:173）；`place <pos> <face>` 实际连接面是
OPOS[face]=face^1（GTFluidPipeBlockEntity.java:325）。

### 管道 /gt6pipe（accept|stat|place|toggle|output|clear|inject|ownable）

前置：起服+RCON 通；木小管容量 1000 L（GTFluidPipeBlockEntity.java:85）。

ownable 面（p24-pipe-owner）：`ownable <pos> <0|1> [ownerUuid]` 是泡沫 applyFoam 的
替身（上游 10ConnectorRendered:159-166 唯一活体写点，泡沫族睡 P10 池）——控制台 OP
强制写点，无 allowInteraction 门；`ownable 0` 双字段复位（removeFoam 形）。stat 增
`ownable/owner` 两字段。锁管语义：默认 ownable=false 零保护（活体等价上游普通管，
既有链零回归）；上锁后 console（null=非 owner）toggle 被拒（06Covers:141 对位 +
09Connector:75 邻居门，同门覆 connect/disconnect 两臂），贴靠锁管 place 得
connections 0（上游 :86 return T 对位）。活链 = `chains/p24_pipe_owner.py`。

```bash
R() { python3 tools/rcon/gt6rcon.py --password <pw> "$@"; }
# 链 A：贴靠桶（桶在管西侧：管 (10,64,10)，桶 (9,64,10)）
setblock 9 64 10 gt6:barrel_wood
R "gt6pipe place 10 64 10 5"  --expect 1:connections\ 16        # 仅贴靠面连接(face5→side4)
R "gt6pipe stat 10 64 10"     --expect 1:connections\ 16        # ioMask 0
R "gt6pipe toggle 10 64 10 4" --expect 1:connections\ 0         # 断开贴靠面
R "gt6pipe inject 10 64 10 4 1000" --expect 1:REJECTED          # 未连接面 filled 0 被拒
R "gt6pipe toggle 10 64 10 4" --expect 1:connections\ 16        # 重连
R "gt6pipe inject 10 64 10 4 1000" --expect 1:"filled 1000 of 1000"
R "gt6pipe output 10 64 10 4" --expect 1:"ioMask 16 (side 4 marked)"  # 外推标记
sleep 2; R "gt6pipe stat 10 64 10" --expect 1:"tank 0: 0/1000"  # 真 ticker 外推进桶
R "gt6pipe clear 10 64 10"    --expect 1:ioMask\ 0               # 清箭头
# 链 B：双管均衡（A=(20,64,20)，B=(21,64,20) 东邻）
R "gt6pipe place 20 64 20 4" "gt6pipe place 21 64 20 5" "gt6pipe accept 20 64 20" \
  --expect 3:"GT6 fluid pipe check OK" --expect 3:"equilibrium stable"   # 50/50+防回流位
```

### 桶 /gt6tank（accept|melt|stat）

```bash
R "setblock 20 64 20 gt6:barrel_wood"
R "gt6tank accept 20 64 20" \
  --expect 1:"GT6 tank check OK" \
  --expect 1:"filled 16000 L from 16 water buckets (17th rejected)" \
  --expect 1:"drained back with 16 empty buckets (17th rejected), barrel empty"
R "setblock 21 64 20 gt6:barrel_wood"
R "gt6tank melt 21 64 20" \
  --expect 1:"GT6 melt check OK" \
  --expect 1:"gt6:iron_molten (1811 K) against the 340 K wood ceiling"  # 熔毁烧桶
R "gt6tank stat 20 64 20" --expect 1:melting\ point\ 340          # stat 熔点
```

### 机器 /gt6oven（place|input|run|check；grid-fed，p8-d3 起 ENERGY_FAKE_SOURCE 缺省 false）

炉子纯电网供电：先摆供能 rig（p8 e2e 正典 idiom——能量邻居**先于**线缆摆放，
GTWireBlock 无 retro-scan），再 poll 冶炼产出。`/gt6energy` 与 `/gt6wire` 命令面见
p8-d3/d4 卡。

```bash
R "gt6oven place 30 64 30"     --expect 1:"GT6 oven placed"
R "gt6oven input 8 30 64 30"   --expect 1:"8 cobblestone"        # 圆石入料
R "gt6oven check 30 64 30"     --expect 1:"energy=0"             # 无假电源体制钉子
R "gt6energy place 32 64 30"   --expect 1:"GT6 energy source placed"
R "gt6energy volt 32 64 30 32" --expect 1:"voltage 32"
R "gt6wire place 2x 31 64 30"  --expect 1:"GT6 wire placed"      # 最后放，双侧已存在
R "gt6wire neighbors 31 64 30" \
  --expect 1:"west=TileEntityOven(connected) east=GTEnergySourceBlockEntity(connected)"
R "gt6energy mode 32 64 30 on" --expect 1:"emitting true"
# 硬断言：连续源自然 tick 完成全部 8 冶炼（32EU/t → ~64 ticks），超时链红
R "gt6oven check 30 64 30"     --expect 1:"output=stonex8" --poll 30
R "gt6oven check 30 64 30"     --expect 1:"GT6 oven at"          # 槽位/能量/朝向报告
```

（`run <ticks>` 只驱动 oven 自身 dispatcher tick——gen/wire 泵送走真实 ticker，
故 run 循环内拿不到连续能量；其 `progress=true done=true idle=true` 三态断言是
fake-source 时代的产物，grid-fed 下结构性不可满足（done=mSuccessful 完成瞬态与
idle=input 耗尽态在循环内不可同框），验收链一律走上面的 poll 产出形态。）

### cover /gt6cover（install|dismantle|check；宿主=oven，side 词 down|up|north|south|west|east）

```bash
R "gt6oven place 40 64 40"
R "gt6cover install 40 64 40 up" \
  --expect 1:"GT6 cover install check OK" --expect 1:"ok=true"   # plate_iron 板+快照
R "gt6cover check 40 64 40"    --expect 1:store=alive
R "gt6cover dismantle 40 64 40 up" \
  --expect 1:"GT6 cover dismantle check OK" \
  --expect 1:"drops=[1x" --expect 1:"store=null"                 # 掉落 plate_iron+全空回 null
```

### 多方块 /gt6multiblock（place|frame|hole|wand|check|tick；卡面简写 gt6multi 的实码正典名）

```bash
R "gt6multiblock place 50 64 50"  --expect 1:"coke oven controller placed"
R "gt6multiblock frame 50 64 50"  --expect 1:"25 bricks placed"   # 25 砖成型
R "gt6multiblock check 50 64 50" \
  --expect 1:"block_formed=true" --expect 1:linked_parts=25/25    # FORMED=true
R "gt6multiblock hole 51 64 51"   --expect 1:"GT6 part broken"    # 拆 1 件（壳砖位）
R "gt6multiblock check 50 64 50"  --expect 1:block_formed=false   # 未成型（无 FAILED 字样）
R "gt6multiblock wand 50 64 50" \
  --expect 1:"GT6 multiblock wand check OK" --expect 1:formed=true  # 25 砖自动放置回填
R "gt6multiblock check 50 64 50"  --expect 1:block_formed=true    # 恢复成型
```

（FACING north → 结构核在南侧一格：controller (50,64,50) 的核为 (50,64,51)，壳=核 ±1
立方 26 格，其中一格是 controller 本体。）

### ACT /gt6act（place|fill|selector|clear|compute|craft|sort|mode|stat；p24-act-machine）

```bash
R "gt6act place 412 64 20" --expect 1:"advanced_crafting_table placed"
R "gt6act fill 24 minecraft:oak_planks 8 412 64 20" --expect 1:"fill slot 24: 8x"   # 格内实料
R "gt6act selector 5 412 64 20" --expect 1:"config 5 into slot 30"                  # Selector Tag 入 30 槽
R "gt6act compute 412 64 20" --expect 1:"grid=[...R..G..]" --expect 1:canDo=true    # 铺料+配方（config 2 竖2=木棍）
R "gt6act craft once 412 64 20" --expect 1:"crafted=true, hold=[4x"                 # 四式点击之左键
R "gt6act mode belt16 on 412 64 20" --expect 1:"mode belt16=true"                   # mBlocked16 开关臂
```

链：`chains/p24_act.py`（slug p24act，端口对 26109/26119；selector payload 双腿
node_cmds 分叉=1.20.1 `{Damage:5}` NBT vs 21.1 `[minecraft:custom_data={Damage:5}]`
组件信封，GT6Circuits 载体裁定）。

### 工具 give 冒烟 /item replace + /data get（p25-tool-hammer-wrench）

```bash
R "item replace block 416 64 124 container.0 with gt6:hammer 1" --expect 1:"Replaced"   # 注册行活体（未注册 id 此处即红）
R "item replace block 416 64 124 container.1 with gt6:wrench 1" --expect 1:"Replaced"   # 同上，扳手行
R "data get block 416 64 124 Items[0]" --expect 1:"gt6:hammer"                          # 槽位实锚（不是裸名单 token）
R "item replace block 416 64 124 container.2 with gt6:hammer{Damage:22} 1" --expect 1:"Replaced"  # 1.20.1 耐久轴 NBT 形
R "data get block 416 64 124 Items[2]" --expect 1:"Damage: 22"
```

链：`chains/p25_tool_hammer_wrench.py`（slug p25toolhammerwrench，端口对
26108/26118；RCON 无玩家——give 臂按 p24_act 先例走 chest `item replace`+`data get`
探针，/recipe give 不可 headless 寻址，配方装载证据=runServer 零 ERROR 行+入库
JSON 形检；耐久 payload 双腿 node_cmds 分叉=1.20.1 `{Damage:22}` NBT vs 21.1
`[minecraft:damage=22]` 组件信封（GT6Circuits 载体裁定同源），data get 渲染漂移
双腿 node_expects 分叉=1.20.1 `Damage: 22`/`Count: 1b` vs 21.1
`"minecraft:damage": 22`/`count: 1`）。

### 食品罐 row0 Canner 冒烟 /gt6machine + /data merge（p25-food-can-row0）

```bash
R "setblock 420 65 125 gt6:canner" --expect 1:"GT6 canner placed"                       # T1 落机（place 臂实锚）
R "data merge block 420 65 125 {inventory:{Size:4,Items:[{Slot:0b,id:\"minecraft:rotten_flesh\",Count:1b},{Slot:1b,id:\"gt6:food_can_empty\",Count:1b}]}}" --expect 1:"Modified block data"  # 双物品输入面（addRecipe2 形）
R "gt6machine canner inject 40 16 420 65 125" --expect 1:"outputs=[1x food_can_rotten_small; ]"  # foodValue 4 → tier 1
```

链：`chains/p25_food_can.py`（slug p25foodcan，端口对 26110/26120；三单元
rotten_flesh/spider_eye/cookie x6 全走 p24_canner_refill 的 inventory data-merge
双物品输入形——1.20.1 `Count:1b/6b` NBT vs 21.1 `count:1/6` 键形分叉；输出 expect
双腿 node_expects 分叉=1.20.1 裸 path `food_can_rotten_small` vs 21.1 前缀
`gt6:food_can_rotten_small`（Step.node_expects，非裸名单 token）；cookie 行 6×
Count 吃满 MultiItemFood.java:600 的注册计数，DEFAULT 分档 12/12=1 → tier 5 超大
罐=Cookie Tin；p24 canner 链回归随跑 [0,0]）。

## ⑤ 三层框架用法（新卡 RCON 链的正典姿势）

### 生命周期层 gt6server（ops 纪律的模块化落地）

```python
import sys; sys.path.insert(0, "tools/rcon")
import gt6server

gt6server.ensure_eula("mdk/run")                     # eula 缺失=静默 24s 退场（ghost-poc 鉴戒），模块兜死
ports = gt6server.pick_ports(25662, 2)               # ss 预检，占用则 +1 顺延；传元组则各起点独立顺延
log, pid = gt6server.artifact_paths("myslug")        # /tmp/gt6_rs_<slug>.{log,pid}，12 阶段既成约定的唯一定义点
gt6server.provision_run_dir(".", game_port, rcon_port, query_port, "gt6")
pid = gt6server.start_server(".", log, pid)          # nohup 语义（start_new_session），绝不前台、绝不阻塞等
gt6server.wait_done(log, timeout=600, pid=pid)       # 轮询日志 Done 标记；进程死了立刻抛错，绝不对死者长轮询
gt6server.stop_server(pid, rcon=("127.0.0.1", rcon_port, "gt6"))
```

固化的行为（用户明令，模块强制）：eula 缺失静默退场不可能发生；**`--stop` 全面禁用**；
**禁泛 pkill/pgrep**——停服只走 RCON `stop` → 记录的 PID → 只属于我们 rcon 端口的
JVM（`ss -ltnp` 按端口精确定位），gradle daemon 一律不碰。

**boot 归属复核门（P17 机制化，任一不满足即 fail-fast `BootOwnershipError`）**：
boot 前 `assert_ports_free` 在起服现场重跑 `ss -ltn`，本 boot 要绑的三个端口任一被占
（含 pick 与 bind 之间的竞态窗口）即拒绝起服并报出占用者 pid——preferred 端口忙不触发
此门（`pick_ports` 早已顺延到空闲值）；boot 后 `assert_pid_file` 读回 start_server 刚写
的 pid 文件，内容必须等于本次 boot 的 wrapper pid——并行 boot 互踩 artifact 路径（P16
session pid 互踩实证）在 boot 时炸，而不是收尾时把句柄指向别人进程。

### 世界层 gt6world（站点注册 + bbox 自动清场）

```python
import gt6world

OVEN, HOPPER = gt6world.Site(0, 64, 0), gt6world.Site(0, 65, 0)
sites = gt6world.declare_sites(OVEN, HOPPER, "10 64 10")   # Site/三元组/"x y z" 都收
region = gt6world.region(sites)                            # 联合 bbox 外扩 MARGIN=2
gt6world.cleanup_commands(region)     # ['fill -2 61 -2 26 67 26 air']（超 32768 自动分片）
gt6world.forceload_commands(region)   # ['forceload add -16 -16 31 31']（超 256 区块自动分片）
```

**漏站点类错误结构性消灭**：站点注册一次，fill 区=全部站点联合 bbox 外扩常数——
站点漏注册 = fill 区不含它 = 下一轮必红（不会再静默污染第二轮）。
2026-09-02 shutter 链的 fill 18..26 漏掉 24 站点教训，即由此封死。

摆放 helper（提炼自真链）：`place_oven(client, pos)`、`place_hopper(client, pos, facing)`、
`feed_container(client, pos, slot, item, count)`、`redstone_block(client, pos, on)`；
声明式命令串版：`hopper_command/feed_container_command/set_block_command`。
teardown 断言：`store_null_command(pos)` + `STORE_NULL_EXPECT="store=null"`。

### 链层 chains/（声明式，入库可复放）

**节点选择（P15 石匠矩阵起）**：链模块本身版本无关；启动节点由
`Chain.node`（模块内钉死）或运行面 `--node <name>`（双节点横扫同一链不碰模块）决定，
缺省 `1.20.1-forge`。**节点会回写进 `chain.node`（P17）**——`Step.node_cmds` 的
键形分叉按 `chain.node` 取值（session/perboot 两路都回写；P16 曾因 `run()` 不回写，
side_io 的 21.1 腿 merge 误用 forge `{FluidName,Amount}` 形）。`Step.node_expects`
（P23）是它在**断言形态 drift** 上的孪生：报文渲染带版本差时（21.1
ItemStack.toString 给物品名带命名空间，1.20.1 plain），单一 spanning expect 无法
双腿逐字节精确——按 `node_key` 各腿钉各自整行（p16pchk s17 正典例）；裸名单 token
expect 在 input 字段同名值前禁止（`stonex8` ⊂ `cobblestonex8`，零秒假绿）。
节点决定两件事：
gradle 任务 `:mdk:<node>:runServer`（裸
`:mdk:runServer` 已随石匠骨架消亡）与节点本地 run 目录 `mdk/versions/<node>/run`
（eula/server.properties 各节点独立，世界存档互不污染）。双节点门禁（ADR-P15-4）
= 同一链集合两节点各跑一遍（passes=2 幂等含内），全绿即 `[0,0]×2` 双节点。
21.1 前置事实（2026-09-04 首验）：gregapi 走 additionalRuntimeClasspath 才进
moddev run（build.neoforge.gradle.kts）；桶族四 BET 的 FLUID_HANDLER 面在
GT6CapabilityWiring 逐 BET 注册——缺一即该方块 capability-blind（1.20.1 侧
getCapability 覆写形态看不到这类缺口，链上才会现形）。

每卡一条模块：sites + lifecycle 配置 + steps。试点样板见
`chains/p11_cover_shutter_filter.py`（p11shutterfilter_atom.sh 的逐步迁移）：

```python
from framework import Chain, Step, main, phase
import gt6world

CHAIN = Chain(
    name="my-card", slug="mycard",
    sites=gt6world.declare_sites(gt6world.Site(0, 64, 0), gt6world.Site(0, 65, 0)),
    preferred_ports=(25662, 25672),            # 本卡锚定 rcon/query 对；game 口自动取 rcon-10，忙则顺延
    passes=2,                                  # 第二遍 = 幂等证明（清场由 gt6world 自动 bbox）
    steps=[
        phase("A: 分节标题（纯打印，不计入断言序号）"),
        Step("gt6oven place 0 64 0", expect="placed"),
        Step("item replace block 0 65 0 container.0 with minecraft:iron_ingot 8", sleep=4),  # tick 驱动断言的真实时序
        Step("gt6cover install 0 64 0 up gt6:cover_shutter", expect="OK"),
        Step("gt6cover install 0 64 0 up", allow_failed=True),  # 预期失败步：记 ALLOWED 不计退出码
        Step(gt6world.store_null_command(gt6world.Site(0, 64, 0)),
             expect=gt6world.STORE_NULL_EXPECT),               # teardown 断言
    ],
)

if __name__ == "__main__":
    main(CHAIN)
```

一条命令跑全程（起服→两遍→停服→退出码）：

```bash
python3 -u tools/rcon/chains/p11_cover_shutter_filter.py
```

判定语义与 CLI 同源：steps 经 `gt6rcon.judge_output`（run_chain 的同一判定器）——
输出含字面量 `FAILED` 或 expect 未命中即 FAIL（allow_failed=True 记 ALLOWED），
打印行与 `gt6rcon.py` CLI 逐字一致。简单链也可直接
`gt6rcon.run_chain(host, port, password, commands, expects, allow_failed={n})`。

### 从旧链迁移（tmp/ bash 段 → chains/ 模块）

1. 段头端口/ss 预检/eula/nohup/Done 轮询/按 PID 杀 → 全部删除，交给框架
   （`preferred_ports` 填本卡锚定对，artifacts 沿用原 slug 名）。
2. 每条 `rc "cmd" --expect 1:子串` → `Step("cmd", expect="子串")`；
   `--allow-failed N` → 该步 `allow_failed=True`。
3. 裸 `sleep N`（时序等待）→ 挂到**前一步**的 `sleep=N`（判定器在命令后等待，
   等待窗口才是断言有效性的来源，别挂到断言步自己身上）。
4. `if [ $? -ne 0 ]; then break; fi` → 删除（框架每步必跑，全图诊断，退出码仍如实）。
5. 手写 `fill` 清场区 → 删除，改把**每个方块位置**（机器+料斗+邻居）登记进
   `sites`——bbox 联合自动覆盖，新摆的件记得注册。
6. `gt6cover check <pos>` store=null 收尾 → `store_null_command(pos)` 步。

## ⑥ 会话执行模型与全集 sweep（p15-rcon-session-perf）

boot 是全集 wall 的第一大头（52-59s/次）。两条正典路径，`GT6_SESSION` 一键切换：

- **session（默认）**：`framework.run_session` 一次 boot 跑 N 链。三层隔离：
  ①链边界跑该链声明 sites 的 bbox cleanup（漏站点必红=结构强制）；
  ②会话级全局态复位基线（time set day / weather clear / vanilla gamerule 集）
  ③`fresh_boot` 链自成单例组、`mutates` 键冲突拆组（`plan_groups`）。
  判定语义零变：同 steps / 同 judge / 同 passes。
- **per-chain boot**：`GT6_SESSION=off python3 chains/<chain>.py` 原样回退
  （`run()` 逐字节保留，门禁永不因优化阻塞）。

时序等待的正典形态是 **poll-to-expect**：`Step(cmd, expect=..., poll=秒)` 重发
只读探针直到 expect 命中或超时（最终响应只判定一次，verdict 与单发一致），
替代最坏情况定长 `sleep`；`sleep` 保留给"相隔 N 秒两次读数"类停稳证明。

全集 runner（单/双节点）：

```bash
python3 tools/rcon/sweep.py --plan                    # 看坐标簇分组
python3 tools/rcon/sweep.py --mode session            # 会话模型全集
python3 tools/rcon/sweep.py --mode perboot            # 基线模型全集
python3 tools/rcon/sweep.py --only p14loop,p13bb      # 收窄到指定链
python3 tools/rcon/sweep.py --group p24_dye           # 收窄到指定簇（整带入选）
python3 tools/rcon/sweep.py --diff old.json new.json  # 逐 step verdict diff
python3 tools/rcon/sweep.py --mode session --dual ../MGT6GA-trees/<另一节点wt> \
    --other-node 1.21.1-neoforge                      # 双 worktree 双节点，wall=max
```

结果（逐 step PASS/FAIL/ALLOWED 账本 + 每链/总 wall）落
`/tmp/gt6_rs_sweep_<mode>[_c<N>]_<节点后缀>_<worktree 哈希>.json`。`--dual` 强制两
worktree 同 commit（gradle runServer 持项目锁，同 worktree 双 boot 会被串行化——
ADR-P15-4 双节点正典形态）。结果 JSON 顶层带 `exit` 聚合键（两执行模型一致，
`_failed` 语义 = main 非 dual 退出码；session 模型沿用 framework 自算值）——
`--dual` 末端 `mine_json["exit"] | other_res.get("exit", 1)` 对 perboot 腿不再
KeyError（P18 修复：run_perboot 形状原无顶层 exit）。

**session artifact 命名（P17）**：session boot 的 log/pid 落
`/tmp/gt6_rs_session_<节点后缀>_<链 slug 名册>-<worktree 哈希>.{log,pid}`——名册
（排序去重的链 slug，超长折叠稳定哈希）标识本 boot 跑了什么，worktree 哈希隔离并行
worktree 的同名 boot。旧裸名 `session_<节点后缀>` 已废（同节点段并行 session 曾互踩
pid，P16 首跑被外部 SIGTERM 实证；无代码读取方，干净改名）。

**sweep 结果 JSON worktree 隔离（P18）**：结果账本名掺 `framework.worktree_tag()`
（md5(worktree 根)[:8]，与 session slug 同源机制）——并行 worktree 跑同一名册的
sweep 不再互踩全局 /tmp 账本。`--dual` 对侧回读按**对侧** worktree 的哈希取文件
（对侧子进程以它自己的 tag 写 /tmp；/tmp 全局共享，目录相同、全靠名字分流，对侧
spawn 日志同样按对侧 tag 命名）。旧裸名 JSON 无活代码读者；`--diff` 走显式路径，
任意两份历史账本（含旧名）仍可比。

**session 端口策略（P17）**：一次 session 只绑一个 (rcon, query, game) 三元组，链经
session 的 rcon 端口连接（链自己的 `preferred_ports` 是 per-boot 语义）。裁决
（`framework.session_ports`）：链中恰有一种非缺省声明 → 以它锚定 boot（单链 session
与其 per-chain boot 编址完全一致；game = rcon-10 或链钉 game_port）；无声明或声明
分歧（常态：每链各钉一对是为了并行 per-chain boot 不打架）→ 回退 SESSION_PORTS
节点段（1.20.1=256xx、1.21.1=25752/25762/25772，--dual 双腿靠它错开）。

**全集簇（`--plan` 可视）**：p11/p12 带 → p13/p12 带 → p14 带 → p14 锅炉 →
**p16 簇（P17 注册：pattern_checker / aqua_fluids / side_io / machine_fluid_gui /
drying_rows / form_scaffold / chisel / distillery 八链，站点两两不相交）** →
**p24/p25 两带（P26 注册，见下节）** →
p15_runtime_smoke（fresh_boot 单例）。注册序 = perboot 顺序 + --plan 文档；
session 跑法把全集摊平成一池（`run_session_recorded`）。（P27 起全集扩至
18 簇 62 链，逐簇清单见下文「名册补录 III」；`--plan` 打印实测 bbox。）

**名册扩容：p24/p25 九链入册（P26 wave1，卡 p26-rcon-sweep-roster）**。名册自
27 链（p11-p16）扩到 **36 链 8 簇**，既有 27 链配置逐字节零改动。九条新链按
**bbox 坐标带准入**（沿用 p16 簇形态）分两簇：

- **z=20 带（x384..414，六链）**：`p24_dye_chemical_fluids` → `p24_pipe_owner` →
  `p25_tag_input_machine_fallback` → `p25_cfoam_spray` → `p24_canner_refill` →
  `p24_act`（带序 = bbox min-x 升序；相邻成员 bbox 交叠，一带之内前链残留面对
  后链的边界清场）。
- **z=124 带（x390..426，三链）**：`p24_grass_block` → `p25_tool_hammer_wrench` →
  `p25_food_can`。grass 与后两者 x 向分离（389..397 vs 414+）；hammer 与
  food_can 仅在 MARGIN 边界 x=418 相触——同簇（清场带局部化），并发波下
  `plan_waves` 的 bbox-disjoint 准入自动不许二者同波。

准入规则（与 p16 簇同构）：链必须已注册 sites（bbox 清场结构性强制）；九链均无
`fresh_boot`/`mutates` 声明，可入共享 session；各链 `preferred_ports`（26106..26110
对）是 per-boot 语义，共享 boot 编址走 `framework.session_ports` 节点段回退；
资源占用形态与既有带一致（单带 ≤6 链、一次 session 一 boot、默认串行，
`--concurrency N` 波交织仍由 plan_waves 结构把关）。

**单簇冒烟/增量复验入口 `--group`（P26）**：`--group <逗号键>` 按「成员 stem /
slug / 链名子串」匹配簇，**命中簇整带入选**（准入单位是坐标带不是单链；
`--only` 再在带内收窄）。缺席 = 全名册，行为逐字节不变。例：
`--group p24_dye`（z=20 带六链）、`--group p25foodcan`（z=124 带三链）、
`--group p24`（两新簇共九链）。冒烟实绩（2026-09-08，本卡）：z=124 带
`--group p24_grass_block` forge 腿（1.20.1-forge）session 一 boot 143.4s 三链
**exit=0 failures none**（grass/hammer/foodcan pass_failures 全 [0,0]，boot 69s）；
runner 日志 `/tmp/gt6_rs_sweep_p26smoke_z124_forge.log`，boot 日志
`/tmp/gt6_rs_session_1201-forge_p24grassblock+p25foodcan-72d4029d-b416fe1f.log`，
账本 `/tmp/gt6_rs_sweep_session_c1_1201-forge_b416fe1f.json`。

**偏离注记（复验时如实记账，不算失败）**：九链双腿复验（2026-09-08，P25 收官
证书）唯一红 = **p25cfoam 21.1 腿 [3,3]**，属**已裁定的显式声明偏离**（P25 卡
p25-c-foam-pipe-spray 审查裁定入档），后续 sweep 复验该腿仍会出现此红并照账本
记录——判定口径以裁定为准，不作为名册/链体缺陷重开。36 链全集 sweep 留阶段
收官门禁由主会话执行（本卡只做单簇冒烟，不跑全量）。
**[2026-09-13 已闭环]** 该偏离由 p28-neo-loot-copy-custom-data 收口：根因 =
GT6DualDirectoryFaces 单数 loot_table 镜像带逐字节搬 1.20.1 正典输出，51 张表
（2 木流体管 + 49 漆机器梯）带 `minecraft:copy_nbt` 在 1.21.1 未注册（已改名
`copy_custom_data`，LootItemFunctions.java:49）→ boot LootDataType 解析死 → 拆管/
拆机零掉落。镜像 loot face 现过 1.21.1 适配器（shape 门禁 + fail-visible），链
E 臂 data get 路径双腿分叉（forge=`Item.tag.BlockEntityTag` /
neo=`Item.components."minecraft:custom_data".BlockEntityTag`）且收尾 kill 探针
双腿转正；复验该腿应为 **[0,0]**，再红按真回归重开。

**名册扩容 II：p26 W1 三链入册（卡 p26-w1-sifter-compressor-wiremill）**。名册
36 链 8 簇 → **39 链 9 簇**。新簇 **z=172 带（x383..411，三链）**：
`p26_w1_sifter`（x384..391，Sifter 梯 T1-T4）→ `p26_w1_compressor`（x394..401）
→ `p26_w1_wiremill`（x404..411）——三族各自带内 x 向分离、teardown fill 各自
带局部化；准入与 p16 簇同构（sites bbox 已注册、无 `fresh_boot`/`mutates`
成员——inject 电网 rig 不碰 fakesource 全局态，一 boot 伺候全簇）；
`preferred_ports` 26150..26152 对为 per-boot 语义，共享 boot 走
`framework.session_ports`。链形：place → input → inject（KU 脉冲
`N size -size` 正负列车 / RU 纯正列车）→ 输出断言 → `check`（`data=-2`=
menu-null GUI 条款活体）→ teardown；`run` 对 menu-less 载体按设计拒绝
（GTMachineCommand「use inject+check instead」）。

**名册补录 III：直跑链全量对账入册（卡 p27-rcon-roster-backfill）**。扩容 II
之后名册又经两笔未及 README 的卡内扩容（cfoam_blocks 入 z=20 带 x452..458、
W1 card B 的 press/extruder 簇），实况 42 链 10 簇；本卡对账 `chains/` 全目录
后一次补齐，现为 **62 链 18 簇**——双向对账（名册→文件在、文件→名册在册）
零差集。补录三块，准入与既有簇同构（sites bbox 已注册、无 fresh_boot 成员，
per-boot 端口钉让位 session_ports）：

- **P19-P23 历史遗漏七链**（名册首次冻结时止步 p16，这七条卡链一直靠
  `--only` 手工点名跑）：**z=20 西段行回填带**（x357..381 三链）`p19_drying` +
  `p26_rm_backfill`（同址 x=360 列，bbox 交叠→带清场互覆）+ `p21_drying_food`
  （桶列远伸 z=59，带内 x 向分离）；**z=124 西段石材/涂料带**（x369..397 五链）
  `p19_chisel` → `p21_stoneblocks_split` → `p21_paintable` → `p23_barrel_paint`
  → `p21_chisel_drops`（min-x 序；drops 与 barrel_paint 带擦 grass 带东缘
  x389..397——跨带交叠由 `plan_waves` 拒同波 + 逐链站点清场兜底，结构性把关）。
- **P26 直跑链十一链**：`p26_cfoam_refill`（x410..416，与 act 带尾交叠）与
  `p26_kitchen_pot`（x418..426）入 z=20 主带带尾（act 之后，min-x 序保持）；
  **坩埚带**（z121..140 三链，坩埚卡 A/B/C）`p26_crucible_multiblock` |
  `p26_mold_faucet`（z132..140 带内 z 向分离）| `p26_crucible_row0`（与
  multiblock 东缘交叠，带清场互覆；multiblock 西缘擦 food_can x426..427）；
  **传感器带** `p26_sensors_core`（x432..436 z125..137）——名册唯一 session
  mutates 成员（fakesource），plan_waves 独占波 + plan_groups 拆组
  （p16_side_io 先例）；**静态仓储带**（z218..232 两链）`p26_hopper_family` |
  `p26_static_storage`（z 向分离）；**MUI 行分派带** `p26_mui_row_dispatch`
  （x477..531 z97..103，与 p16_distillery z 同带 x 离散）。
- **P27 直跑链两链**：`p27_builder_wand_form_fix`（坩埚锚定成型回归，x456..464
  自成带，双腿 GREEN 审查席实证）；`p27_vanilla_tag_dual_tree`（/gt6tags 双树
  活体，slug p27tags——驻 spawn 邻区（原与 EU 出向桥链成簇，该链已随
  EU->FE 出向桥砍除，task p28-cut-eu-fe-bridge）；p11/p12
  带本就驻 spawn 邻区 x-2..60，跨簇交叠同上把关）。

验证引用（创建/末次修正提交）：p26 十链见各自任务卡交卡证书（原十一链，
eu_bridge 37de6542 已随 p28-cut-eu-fe-bridge 砍除）——
cfoam_refill 21523df8（双腿 [0,0] /tmp/p26c-rcon-*.log）、crucible_row0
6d9cb493（双腿幂等两遍 /tmp/p26_rcon_*5.log）、crucible_multiblock b986c975、
mold_faucet cafd4dc5、kitchen_pot d4753ea2（[0,0] 双腿零 ERROR）、
mui_row_dispatch b5b254cf、rm_backfill a9b88479（双腿 [0,0]
/tmp/p26rm-rcon-*.log）、sensors_core 902d04a6、hopper_family 4538efb8、
static_storage 1b8d2022（双腿 [0,0] /tmp/p26stat_rcon-*.log）；历史七链
p19_chisel 9fe6b166 / p19_drying 4beeb6b6 / p21_chisel_drops 14ade19d /
p21_drying_food 74462118 / p21_paintable a4e154c7 / p21_stoneblocks_split
9fe6b166 / p23_barrel_paint 14c34c4c；p27 两链 87716091 与 74b6e262。

**无链清单（诚实对账，不发明）**：worldgen 卡（p26-worldgen-pipeline-skeleton）
的活体验收是 runServer forceload 冒烟（17/17 石种，/tmp/gt6_rs_p26wgen_*.log）
非声明式入库链；「p26staticstoragefix」并非独立链——p26-storage-static-batch
卡的 RCON 活跑修正（9f4f7234..17d92253）全部折进 p26_static_storage.py 主链
本体；p27_tags 无独立文件（tag 骨架的 RCON 面由 p27tags 的 /gt6tags dump
承载）。不入册的 probe 形态两件：`p15_keepfilter_reboot_probe`（PROBE_MODULE，
`--probe` 入口）与 `p19_nbt_rebind_reboot_probe`（自带 main 的两 boot verdict
脚本，非声明式 CHAIN，`load_chain` 不兼容——保持手工点名形态）。

**P27 红链现代化清账（卡 p27-red-chains-modernize）**：P26 收官证书「2 红=链
过时非回归」的两链已重钉转绿——`p15_runtime_smoke` 的 M 臂改 inject 数据面
（run 对 menu-less 载体拒绝=P26 批 A 设计行为，改断言之；fakesource 全局开关
退役，mutates=()）、`p25_tag_input_machine_fallback` 的 D 臂由负臂翻
RECYCLABLE 环行正臂（P26 行回填新行的匹配序演进）；两链双腿 [0,0]
（/tmp/p27_p15_rcon_{forge,neo}.log、/tmp/p27_p25_rcon_{forge,neo}*.log），
簇带归属零改动。

**P27 朝向臂注记**：coke oven facing 回归臂落在既有 `p16_pattern_checker` 链
（7e6c2edb，phase C），不另立链——p16 簇成员与带区间不变。

**框架自检（无服干跑，~1s）**：`python3 tools/rcon/selftest.py`——以假 boot 面
验证九项框架行为：chain.node 回写与 21.1 `{id,amount}` 键形分叉、session artifact
名册化、session 端口策略、p16 簇注册、boot 归属门、sweep 结果 JSON worktree 隔离
（P18）、quiet_window 自适应收敛纯逻辑（P18）、perboot 结果的顶层 exit 聚合键
（P18：`--dual` 读侧 `mine_json["exit"]` 曾对 perboot 形状 KeyError——run_perboot
只有链级 exit，run_and_record 出口以 `setdefault` 补 `_failed` 聚合键，session 模型
framework 自算的顶层 exit 不被踩）、wait_done 单调扫描窗（P19：带病 boot 日志高速
滚动会把 `Done (` 标记或崩溃 ERROR 行推出旧 8KB 尾窗，前者假超时报"never printed
Done"、后者丢归因——改为字节偏移增量扫描（见过的标记永记、跨读边界 carry 拼接），
死亡归因走全日志 error_tail，正常路径总读取量不升）。退出码 0 = 全绿（51 检，
P23 增 node_expects 分叉三检）。

### 并发波执行（用户校准 2026-09-04：并发是主杠杆）

`GT6_CONCURRENCY=N`（sweep `--concurrency N`）把同 boot 的链按**波**交织：
一条链的 must-wait（机器加工/燃烧/生产窗 poll）期间，别的链的 step 在同一
服务器上并发执行。准入是结构性的（`framework.plan_waves`）：

- 站点 bbox **两两不相交**（gt6world.region 含 margin，任一轴分离即异址；
  同带重叠链绝不并发——并发只发生在不同坐标带的链之间）；
- `mutates` 非空的链降级为独占波（全局态只允许波起点的会话级复位控制点改）；
- `fresh_boot` 链永不并入波；
- 波宽 ≤ N；波间串行，波内每链独立 RCON 会话（原版 RCON 天然多客户端，
  服务端命令串行、客户端 quiet_window 重叠=吞吐来源）。
- 并发组的逐 step verdict 必须与 per-chain-boot 基线 diff 一致；不一致的组
  用 `--concurrency 1` 重跑该组降级串行并记录。

### 管道 /gt6pipe 泡沫三子命令（p25-c-foam-pipe-spray，README tail-append）

`spray <pos> <owned> [dye] [ownerUuid]` / `dry <pos>` / `removefoam <pos> [ownerUuid]`
（spec ⑧）走与物品 useOn 相同的受门 BE 面：applyFoam（上游
TileEntityBase10ConnectorRendered:159-166——湿/干拒+allowInteraction 门，喷=同染管）、
dryFoam（:169-174 无门不对称）、removeFoam（:177-183 干+门+四字段复位）。stat 行增
`foam/dried/foamOwned` 三字段（p24 既有 expect 串均为子串，零回归）。注册行
`(accept|stat|place|toggle|output|clear|inject|ownable|spray|dry|removefoam)`。
回流面（spec ⑦）：管 loot 表（GT6PipeBlockLoot）携带函数五键入 BlockEntityTag——
1.20.1 正典带 = `copy_nbt`（item tag 面），1.21.1 单数镜像带 = `copy_custom_data`
（`minecraft:custom_data` 组件面，p28-neo-loot-copy-custom-data 适配器重写，.BlockEntityTag
相对路径原样落组件内）。拆管掉落物断言双腿分叉（Step.node_cmds，p24_act 组件信封
先例）：forge `data get entity … Item.tag.BlockEntityTag` / neo
`data get entity … Item.components."minecraft:custom_data".BlockEntityTag`，断言
gt.foamed/gt.foamdried/gt.ownable 三键在物；收尾 kill 探针双腿转正（无
allow_failed——'No entity was found' 即拆管零掉落回归）；
gt.owner 不入物（再置由 onPlaced 记新放置者，离线腿
GTPipeFoamTest.foamNbtRoundTripsAndOwnerDoesNotRideItems 钉死）。活链 =
`chains/p25_cfoam_spray.py`（place→spray owned→stat→dry→锁 toggle 拒→removefoam
非 owner 拒→owner ok→spray+dry→拆管→掉落物 NBT 断言；双腿 [0,0]；日志路径
/tmp/gt6_rs_p25cfoamspray.*，节点名后缀随 --node）。

### ULV 波收口链 /gt6machine + /gt6oven + /gt6engine + /gt6fe* + /data（p28-c-ulv-chain，README tail-append）

四臂一链 `chains/p28_ulv_chain.py`（slug p28ulv，端口对 26170/26180；新簇
`p28_ulv_chain`，z=40/48/56/64 四站带 x519..545，站带两两 margin 离散可并发）。
零新命令臂——全部读数走既有命令面 + 原版 /data get|merge NBT 探针：

- **A 正链**（z=40）：`gt6:diesel_engine_bronze[facing=east]` 注油 16 RU/t →
  双木轴 → `gt6:electric_dynamo_ulv[facing=east]`（背收 RU 窗 [1..16]，T0 声明
  1:1）→ front **直贴** `gt6:wiremill_ulv`（SBIT_B 能量面，data merge
  `{facing:5}` 旋到 west）。断言：`check` 报 `minIn=4 recIn=8 maxIn=16`
  （ULV 窗钉）、data merge 铜棒后 poll `outputs=[4x wire_fine_copper; ]`、
  引擎 `rate=16 RU/t (DC constant-sign)` + 轴 `break pending=false`。
  **三个 live 校准教训**：①机器 BE 无视 setblock 的 blockstate facing
  （mFacing 缺省 north，只有放置路径/NBT 写它）——`data merge block <pos>
  {facing:5}` 才算数；②机器 doWork 每 tick 先排 mInputMax 再判进度，sub-16
  包永远攒不进缓冲（doInactive 的 CONSTANT_ENERGY :894 再雪上加霜）——inject
  臂包尺寸必须 ≥ 行 mMinEnergy 16；③线缆最小 loss=1、T0 dynamo 整电容包=16EU
  ⇒ **任何穿线 hop 都喂不动 eUt16 行**（16-loss<16 被排空烧掉）——正链的机器
  跳段只能直贴，V[0] 包的穿线载运由 B 臂 volt-8 铜线活证。
- **B 负断言·8EU 白烧**（z=48）：p8 烤箱 rig（oven→copper 线→/gt6energy 源）
  volt 8：`EnergyGate :50` 吞包——5s 窗口两侧双 `energy=0` + `input=cobblestonex8`
  原封；同 rig volt 32 完成 `output=stonex8`（对照腿：红是包尺寸不是 rig）。
- **C 负断言·1375K 熔点门**（z=56）：双 wiremill_ulv。铁棒
  （GT6Fe 1811K > 1375K）`inject 60 8` 后 `progress=0/0`（配方从不绑定，
  TileEntityBasicMachine :655 在任何消耗前拒）+ `data get` 槽 0 铁棒原封
  （零消耗）；铜棒（1357K）`inject 300 16` 完成 `wire_fine_copper`（对照臂）。
- **D 变压器贯通涓流**（z=64）：`gt6fesource`（预算 12288 FE=384 次整包抽取）
  → `gt6feconverter`（auto-pull 1 包/t = 恒 8EU/t 发射，实测速率恰 8EU/t）→
  `gt6:electric_transformer[facing=east]` **直贴烤箱**（无 wire，见下），
  `/data merge {gt.reversed:1b}` 翻 step-up（②卡 W3 驱动面：NBT_REVERSED）。
  断言：翻转前 `reversed: 0b`/后 `reversed: 1b`；翻转后立即
  `input=cobblestonex2`（8EU/t 涓流首 tick 不可能够到 256EU 的第一熔——全速
  旁路早吃掉一熔）；poll `input=airx0`（两熔全耗=贯通可达）；poll
  `gt6fesource stat` → `stored 0 FE`（整包量化抽取零尾差=守恒钉）。
  **本臂刻意无 wire**：实测 wire 会把 step-up 包回送进变压器 all-but-front
  输入面（自反馈环），电容自锁涓流枯死；LV 线载 32EU 包的腿已由 B 臂 volt-32
  对照活证。

链级 [0,0]：负臂预期红全部转成显式断言步（无 allow_failed 逃逸）。

### FE 入向转换四相链 /gt6feconverter + /gt6fesource + /gt6machine + /data（p28_fe_inbound 重写，README tail-append）

四相一链 `chains/p28_fe_inbound.py`（slug p28fein，端口对 26166/26176；与
p28_ulv_chain、p28_builder_wand_oneclick 同簇——P28 能量生态簇，新鲜带
x548..555 z28..56，四站 z=30/38/46/54 两两 margin 离散）。**重写缘由**：
p28-cut-eu-fe-bridge 砍掉 EU→fe_battery 出向桥后，本链 A/B/C 三相的收电
对账终点语义消亡（双腿 RED [6,6]，红步集 {5,6,10,16,17,21} 全为死记账臂，
server ERROR 0=结构性非回归）；ULV 机器梯（p28-c-ulv-machine-ladder）合入
后 8EU 包有了合法消费者——wiremill_ulv 窗 min4/rec8/max16，`EnergyGate
.gateInjection :49`（|8|≥min 4，doInject 真入账；烤箱 min16 的 :50 吞包臂
是 W3 的负对照）。fe_battery 记账臂全删（夹具留世界、不入链）。四相：

- **A push 接收活证 + 裸端负臂**（z=30）：wiremill_ulv `data merge
  {facing:5}`（机器 BE 无视 blockstate facing，W3 驱动面）背面对 converter
  直贴（刻意无 wire：线最小 loss=1 + doWork :465 每 tick 无条件排
  mInputMax=16，任何穿线 hop 都饿死）；铜棒 merge 后双 converter 各 push
  512 FE，共用一个 4s 窗（16 包 ÷ 1 包/t，5× 余量），然后：**被喂
  converter `buffer 0 FE`**（16 包全被吃——只扣实收臂只记网络真用掉的包，
  死 fe_battery 记账臂的收据如今落在 ULV 机端点）；mill `check` 一行钉全
  `progress=0/128 energy=0 minenergy=16 minIn=4 recIn=8 maxIn=16`（forge/neo
  同形零分叉）：铜棒行在第一个包的 tick **绑定并按绑即耗**吃掉棒料（上游
  consume-on-bind，槽位读 air、产物滞留 mOutputItems 永不入槽），卡死在
  0/128 = 8EU/t 永远再够不到已绑定的 16EU/t 启动门（doWork :455 门在
  doInactive 的 CONSTANT_ENERGY 进度复位间振荡）——**1A 流接收≠完成**，
  完成需要 ≥16EU 整包（W3 dynamo 形）或同 tick 双包；平衡裁定的活体注脚。
  **裸 converter（隔 1 空气、零消费者）`buffer 512 FE` 原位不动**
  ——显式负臂：证明上面的排水是消费不是泄漏，链内断言无 allow_failed。
- **B pull + floor 尾差**（z=38）：fe_source 设 130 FE（4 整包 + 2 FE 零
  头），converter 每 tick 拉 1 包（root EnergyBridge.extractFe，p28-a 缝），
  排空后源保 `stored 2 FE`（敌意零头永不离开源）+ converter 自身电容
  `buffer 128 FE`（整包量化收据——本相刻意无消费者，发射臂找不到邻接，
  包滞留电容=死 battery 臂 128 FE 断言的语义镜像）。
- **C 天花板**（z=46）：满源 100k FE 喂机器，**5s 窗**（~100 tick @20tps），
  源保 `stored 96xxx`。带宽算术：有效流逝 [3.1..6.2s] 全落 96xxx（排水
  [2000,4000) FE）；任何 2 包/tick 回归（整型安培 bug，16EU/t）排 6400+
  落 92xxx/93xxx=红。`poll=6.0` 吸收慢侧滞后（滞后窗稍后重读仍 96xxx；
  过排单调，真回归永不可能 poll 绿）。老链单发 6s 窗两侧各只剩 ~0.15s
  有效期——「速率类断言加足窗口」教训的带宽化重推，算术留在链 docstring
  可复查。
- **D 超载爆炸**（z=54，远离 rig）：data merge `{fe:100000}`（双进料面都
  钳不到、只有外来写手能产出的值）→ 2 tick 宽限后 overcharge 爆炸
  （TileEntityBase10EnergyConverter :122-126 → Root :330）→ stat 行
  `STAT FAILED` 判 allow_failed=方块已消失（absence 的诚实证明形，非负臂
  逃逸）。

### 方块随手成型一键链 /gt6multiblock（p28_builder_wand_oneclick，README tail-append）

`chains/p28_builder_wand_oneclick.py`（slug p28wandclick，端口对
26170/26180；站点带 x296..314 z95..105，与全名册零相交）。用户裁定
2026-09-12：builder wand **一键成型整个多方块**（对上游 1.7.10 九击语义的
声明偏离，ADR 2026-09-12-p28-builder-wand-oneclick）——考据
（research.p28-r-builder-wand-second-root）实锤 P28 前「wand 只建半座多方
块」的报告不是移植 bug：玩家进料（useOn→真实点击坐标）被上游 ±1 Chebyshev
点击窗卡住（ITileEntityMultiBlockController.java:145-146 == 上游 :51），
单击只脚手架了锚点邻域（坩埚 24 墙中的 16），而 RCON form 臂喂的是
checker.form aClickedAt=null 一发全成——p27 链的绿灯盖住的是 form 臂，从
未盖住玩家进料。本链的 `wandclick` 臂（GTMultiBlockCommand，本卡新增）就
是玩家进料覆盖：从被点格解析脚手架目标（GT6BuilderWandItem.scaffoldTarget
= useOn 的原样解析）再驱动生产派发：

- **A 一键故事**：只放 controller（零墙）`gt6multiblock wandclick C 24` →
  `formed=true okay=true stock 24 -> 0`；y+2 环格（C+1,y+2,C-1，前语义下
  整环空气=缺失的第八环）与 y+0 环格均为 WALL；`crucible C check` →
  `okay=true linked_parts=24/24`；幂等臂：已成型结构上二次 wandclick
  （stock 1）**零消耗** `stock 1 -> 1`（SET 走查 beat-1 诊断短路）。
- **B form 臂回归锚**（本卡未触碰）：`gt6multiblock form F 24` 照常一发
  全成 + `linked_parts=24/24`。
- **C 拆除**：显式 fill 归还（声明站点清理是结构兜底）。
