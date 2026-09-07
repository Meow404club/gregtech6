#!/usr/bin/env python3
"""jarjar_smoke — production-form boot smoke for the P20 ModularUI jarJar packaging.

为什么不用 ./gradlew :mdk:<node>:runServer：gradle run 是 exploded classpath 形态，
jarJar 嵌套内容（META-INF/jarjar/）只有生产启动器（BootstrapLauncher）才处理——
dev run 里 modularui 根本不在类路径，无法成为本卡的验证面。

本脚本组装 50843dff 裁定的分发形态：生产安装的 loader 服务端，mods/ 只放
ONE mod jar（= 玩家 mods/ 单一类加载域），然后：

  1. 起服（nohup 语义：detached 进程 + pid 文件 + 日志轮询，无 pkill），
     等 "Done (" 标记；
  2. 断言探针 marker：第一个 brachy/modularui 类被定义（modularui 从 level-1
     嵌 jar 加载的直接证据），且探针类经【同一加载器】解析成功——
       forge 腿: com.ezylang.evalex.Expression（level-2 嵌套存活）
                 com.llamalad7.mixinextras.platform.forge.MixinExtrasConfigPlugin
                 com.llamalad7.mixinextras.MixinExtrasBootstrap（level-3 存活）
       neo 腿:   com.ezylang.evalex.Expression
  3. SIGTERM 收尾（JVM 优雅停机），核对进程退出。

用法：
  python3 tools/jarjar_smoke.py --leg 1.20.1-forge
  python3 tools/jarjar_smoke.py --leg 1.21.1-neoforge
  [--jar PATH] [--workroot DIR] [--timeout 420] [--keep]

服务端安装缓存在 <workroot>/<leg>/server，重跑只做启动冒烟。
局限（如实声明）：
  - EvalEx 的类加载由探针在 modularui 首类定义时主动触发，非 GUI 真实使用路径；
    "可经同一加载器解析" 是类加载存活的充分证明，不是表达式求值的端到端证明。
  - 服务端安装需网络（maven.minecraftforge.net / maven.neoforged.net +
    Mojang libraries）；首次安装数分钟。
"""

import argparse
import glob
import os
import re
import signal
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

LEGS = {
    "1.20.1-forge": {
        "java": "/usr/lib/jvm/java-17-openjdk/bin/java",
        "installer": ("https://maven.minecraftforge.net/net/minecraftforge/forge/"
                      "1.20.1-47.4.10/forge-1.20.1-47.4.10-installer.jar"),
        "probes": ["com.ezylang.evalex.Expression",
                   "com.llamalad7.mixinextras.platform.forge.MixinExtrasConfigPlugin",
                   "com.llamalad7.mixinextras.MixinExtrasBootstrap"],
        "evidence": [
            # 4 = modularui + EvalEx + mixinextras-forge + MixinExtras 内包：
            # JarInJarDependencyLocator 对嵌套树全量的清点（1.20.1 FML INFO 行）
            "Found 4 dependencies",
            # level-3 MixinExtras 包被 mixin 子系统活体初始化（0.5.0-rc.3 与 marker 对账）
            "Initializing MixinExtras via",
        ],
    },
    "1.21.1-neoforge": {
        "java": "/usr/lib/jvm/java-21-openjdk/bin/java",
        "installer": ("https://maven.neoforged.net/releases/net/neoforged/neoforge/"
                      "21.1.249/neoforge-21.1.249-installer.jar"),
        "probes": ["com.ezylang.evalex.Expression"],
        "evidence": [
            # NeoForge ModDiscoverer 逐 jar 打点：level-1 mod 与 level-2 library
            # 的 parent 链即嵌套树本身（1.21.1 INFO 行）
            "Found mod file \"brachy.modularui.modularui-mc1.21.1",
            "Found library file \"EvalEx-3.6.0.jar\"",
            "ModularUI 3.3.1 (modularui)",
        ],
    },
}

SERVER_PROPERTIES = """\
allow-nether=false
level-type=minecraft\\:flat
generate-structures=false
online-mode=false
spawn-monsters=false
spawn-animals=false
spawn-npcs=false
view-distance=3
simulation-distance=3
max-players=1
motd=jarjar-smoke
level-seed=0
sync-chunk-writes=false
server-port={port}
enable-rcon=false
"""

FAILURES = []


def check(name, condition, detail=""):
    status = "PASS" if condition else "FAIL"
    print(f"[jarjar_smoke] {status} {name}" + (f" — {detail}" if detail else ""))
    if not condition:
        FAILURES.append(name)
    return condition


def default_jar(leg):
    """Canonical distribution jar gt6-<leg>-<mod_version>.jar first (task
    p23-jar-naming, base.archivesName = <mod_id>-<node>); bare *.jar fallback
    only serves pre-rename worktrees (a stale pre-rename jar would sort first
    and shadow the renamed artifact). Same policy as jar_content_check.py."""
    libs = REPO / "mdk" / "versions" / leg / "build" / "libs"
    if not libs.is_dir():
        return None
    canonical = sorted(libs.glob(f"gt6-{leg}-*.jar"))
    if canonical:
        return canonical[0]
    jars = sorted(libs.glob("*.jar"))
    return jars[0] if jars else None


def free_port():
    import socket
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def ensure_server(leg, cfg, server_dir, installer_jar):
    """Production install (cached): run the loader installer once."""
    args_file = find_args_file(server_dir)
    if args_file:
        print(f"[jarjar_smoke] server already installed: {args_file}")
        return args_file
    server_dir.mkdir(parents=True, exist_ok=True)
    if not installer_jar.is_file():
        print(f"[jarjar_smoke] downloading installer {cfg['installer']}")
        # forge maven 拒默认 urllib UA（403 实证），curl 带 UA + 跟随重定向
        subprocess.run(["curl", "-fsSL", "--retry", "3", "-A",
                        "Mozilla/5.0 (X11; Linux x86_64) jarjar-smoke/1.0",
                        "-o", str(installer_jar), cfg["installer"]],
                       check=True)
    print("[jarjar_smoke] running installer --installServer (first time, minutes)…")
    r = subprocess.run([cfg["java"], "-jar", str(installer_jar),
                        "--installServer"],
                       cwd=server_dir, capture_output=True, text=True,
                       timeout=1800)
    if r.returncode != 0:
        print(r.stdout[-2000:])
        print(r.stderr[-2000:])
        raise SystemExit("installer failed")
    args_file = find_args_file(server_dir)
    if not args_file:
        raise SystemExit(f"no launch args file produced under {server_dir}")
    print(f"[jarjar_smoke] installed: {args_file}")
    return args_file


def find_args_file(server_dir):
    """The @-args file the installer generates (unix_args.txt / *-args.txt …)."""
    for pattern in ("libraries/**/unix_args.txt", "libraries/**/*args*.txt"):
        hits = sorted(glob.glob(str(server_dir / pattern), recursive=True))
        if hits:
            return hits[0]
    return None


def ensure_probe(workroot, leg):
    """Compile ProbeAgent fresh every run (<1s; mtime-caching bit us once —
    stale probe jar shipped the pre-timing-fix agent)."""
    src = REPO / "tools" / "jarjar_probe" / "ProbeAgent.java"
    out = workroot / "probe"
    out.mkdir(parents=True, exist_ok=True)
    jar = out / "probe.jar"
    cls = out / "classes"
    subprocess.run(["rm", "-rf", str(cls), str(jar)], check=True)
    cls.mkdir(parents=True)
    jdk = Path("/usr/lib/jvm/java-21-openjdk/bin")
    if not (jdk / "javac").is_file():
        jdk = Path("/usr/lib/jvm/java-17-openjdk/bin")
    subprocess.run([str(jdk / "javac"), "--release", "11", "-d", str(cls),
                    str(src)], check=True, capture_output=True)
    with (out / "manifest.txt").open("w") as f:
        f.write("Premain-Class: ProbeAgent\nCan-Retransform-Classes: false\n")
    subprocess.run([str(jdk / "jar"), "cfm", str(jar),
                    str(out / "manifest.txt"), "-C", str(cls), "."],
                   check=True, capture_output=True)  # 整目录：含匿名内部类 ProbeAgent$1
    return jar


def boot_and_verify(leg, cfg, jar, workroot, timeout):
    server_dir = workroot / "server"
    marker = workroot / "probe_marker.txt"
    log = workroot / "server.log"
    pid_file = workroot / "server.pid"
    for p in (marker, log, pid_file):
        p.unlink(missing_ok=True)

    (server_dir / "eula.txt").write_text("eula=true\n")
    (server_dir / "server.properties").write_text(
        SERVER_PROPERTIES.format(port=free_port()))

    probes = ["brachy.modularui.ModularUI"] + cfg["probes"]
    agent_arg = f"-javaagent:{ensure_probe(workroot, leg)}=" + ",".join(
        [str(marker)] + probes)

    args_file = find_args_file(server_dir)
    cmd = [cfg["java"], "-Xmx2G", "-Xms512M", agent_arg,
           f"@{args_file}", "nogui"]
    print(f"[jarjar_smoke] launching: {Path(cfg['java']).parent.parent.name} "
          f"{' '.join(cmd[1:])}")
    with log.open("wb") as logf:
        proc = subprocess.Popen(cmd, cwd=server_dir, stdout=logf,
                                stderr=subprocess.STDOUT,
                                start_new_session=True)
    pid_file.write_text(str(proc.pid))
    print(f"[jarjar_smoke] pid {proc.pid} -> {pid_file.name}, log -> {log.name}")

    deadline = time.time() + timeout
    done = False
    while time.time() < deadline:
        if proc.poll() is not None:
            break
        try:
            if b"Done (" in log.read_bytes():
                done = True
                break
        except FileNotFoundError:
            pass
        time.sleep(1.0)

    if done:
        print("[jarjar_smoke] server reached Done — holding 8s for post-load "
              "class activity")
        time.sleep(8.0)
    stop_server(proc, log)
    return done, marker, log


def stop_server(proc, log):
    """SIGTERM = JVM graceful shutdown; targeted PID only (never broad pkill)."""
    if proc.poll() is None:
        try:
            os.kill(proc.pid, signal.SIGTERM)
        except ProcessLookupError:
            return
        for _ in range(30):
            if proc.poll() is not None:
                break
            time.sleep(1.0)
        if proc.poll() is None:
            print("[jarjar_smoke] SIGTERM grace exhausted, SIGKILL (same pid)")
            os.kill(proc.pid, signal.SIGKILL)
            proc.wait(timeout=30)
    print(f"[jarjar_smoke] server exited rc={proc.returncode}")


def read_marker(marker):
    data = {}
    if marker.is_file():
        for line in marker.read_text().splitlines():
            if "=" in line:
                k, v = line.split("=", 1)
                data[k] = v
    return data


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--leg", choices=sorted(LEGS), required=True)
    ap.add_argument("--jar", type=Path, default=None)
    ap.add_argument("--workroot", type=Path,
                    default=Path("/tmp/gt6-p20-jarjar-smoke"))
    ap.add_argument("--timeout", type=float, default=420.0)
    ap.add_argument("--keep", action="store_true",
                    help="leave the server running (debug)")
    args = ap.parse_args()
    leg = args.leg
    cfg = LEGS[leg]

    jar = args.jar or default_jar(leg)
    if not check("distribution jar present", jar is not None and Path(jar).is_file(),
                 str(jar) + " — build with ./gradlew :mdk:<node>:assemble"):
        return 1
    print(f"[jarjar_smoke] leg={leg} jar={jar}")

    workroot = args.workroot / leg
    workroot.mkdir(parents=True, exist_ok=True)
    server_dir = workroot / "server"
    mods_dir = server_dir / "mods"
    mods_dir.mkdir(parents=True, exist_ok=True)
    # 分发形态铁律：mods/ 只放本 mod jar 一个类加载域
    for stale in mods_dir.iterdir():
        if stale.name != Path(jar).name:
            stale.unlink()
    target = mods_dir / Path(jar).name
    if not target.is_file():
        target.write_bytes(Path(jar).read_bytes())
    check("mods/ holds exactly the distribution jar",
          [p.name for p in mods_dir.iterdir()] == [Path(jar).name],
          str(mods_dir))

    ensure_server(leg, cfg, server_dir, workroot / "installer.jar")

    done, marker, log = boot_and_verify(leg, cfg, target, workroot, args.timeout)
    check("server boot reached Done", done, str(log))
    log_text = log.read_text(errors="replace") if log.is_file() else ""
    if not done:
        print("--- last 25 log lines (crash window) ---")
        for line in log_text.splitlines()[-25:]:
            print(line[:200])

    m = read_marker(marker)
    check("probe: modularui class defined from level-1 nested jar",
          m.get("modularui_classes") is not None
          and int(m.get("modularui_classes", "0")) > 0,
          f"modularui_classes={m.get('modularui_classes')}")
    for probe in cfg["probes"]:
        check(f"probe: {probe} resolvable via the modularui loader",
              m.get(probe, "").startswith("LOADED"), m.get(probe, "MISSING"))
    log_text = log.read_text(errors="replace") if log.is_file() else ""
    for needle in cfg["evidence"]:
        check(f"log evidence contains '{needle}'", needle in log_text)

    if not args.keep:
        pass  # stop already performed in boot_and_verify

    print("\n--- probe marker ---")
    if marker.is_file():
        print(marker.read_text())
    else:
        print("(missing)")
    print("--- log excerpts ---")
    for line in log_text.splitlines():
        low = line.lower()
        if "modularui" in low or "jarjar" in low or "Done (" in line:
            print(line[:200])

    print()
    if FAILURES:
        print(f"[jarjar_smoke] RED — {len(FAILURES)} failed: {FAILURES}")
        return 1
    print("[jarjar_smoke] GREEN — distribution jarJar nesting loads in "
          "production form")
    return 0


if __name__ == "__main__":
    sys.exit(main())
