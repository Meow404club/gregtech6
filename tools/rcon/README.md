# gt6rcon — GT6 headless 验收的 RCON 正典工具

单一正典客户端 `gt6rcon.py`（蒸馏自 5 份 P4 参考脚本，重复实现已合并）。
测试验收链一律复用它，不要再写任务本地 RCON 脚本。

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

游戏本体永不自行退出——严禁前台跑、严禁阻塞等待退出。唯一正典姿势（在 worktree 根，
**必须 `./gradlew`，系统 gradle 8.7 过不了 MDG**）：

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

### 机器 /gt6oven（place|input|run|check）

```bash
R "gt6oven place 30 64 30"     --expect 1:"GT6 oven placed"
R "gt6oven input 8 30 64 30"   --expect 1:"8 cobblestone"        # 圆石入料
R "gt6oven run 200 30 64 30" \
  --expect 1:"GT6 oven run check OK" \
  --expect 1:"progress=true done=true idle=true"                 # ContainerData 三态
R "gt6oven check 30 64 30"     --expect 1:"GT6 oven at"          # 槽位/能量/朝向报告
```

（假电源 mInputMax=64、配方 mDuration=16 → 全程数十 tick，200 充裕；不足则 run 自报
FAILED，调大重跑。产出核对走 run/check 报告的 output 段。）

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
