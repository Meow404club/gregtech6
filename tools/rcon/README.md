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

### 管道 /gt6pipe（accept|stat|place|toggle|output|clear|inject）

前置：起服+RCON 通；木小管容量 1000 L（GTFluidPipeBlockEntity.java:85）。

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
side_io 的 21.1 腿 merge 误用 forge `{FluidName,Amount}` 形）。节点决定两件事：
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
p15_runtime_smoke（fresh_boot 单例）。注册序 = perboot 顺序 + --plan 文档；
session 跑法把全集摊平成一池（`run_session_recorded`）。

**框架自检（无服干跑，~1s）**：`python3 tools/rcon/selftest.py`——以假 boot 面
验证八项框架行为：chain.node 回写与 21.1 `{id,amount}` 键形分叉、session artifact
名册化、session 端口策略、p16 簇注册、boot 归属门、sweep 结果 JSON worktree 隔离
（P18）、quiet_window 自适应收敛纯逻辑（P18）、perboot 结果的顶层 exit 聚合键
（P18：`--dual` 读侧 `mine_json["exit"]` 曾对 perboot 形状 KeyError——run_perboot
只有链级 exit，run_and_record 出口以 `setdefault` 补 `_failed` 聚合键，session 模型
framework 自算的顶层 exit 不被踩）。退出码 0 = 全绿（39 检）。

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
