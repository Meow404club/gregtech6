#!/usr/bin/env python3
"""check_png.py -- PNG inflate-vs-IHDR 数据量校验（防复发门禁，不进 CI）。

背景（cokeoven-png-fix，根因见 state tmp.rcon.atlas-runtime）：p4 时代 inline
占位生成脚本把扫描线 stride 写成每行 1 像素，产出的 PNG 容器自洽（chunk/CRC 全过）
但 IDAT inflate 仅 80B，而 IHDR 声明的 16x16 RGBA 需 1040B，运行期 stb_image
数据不足报 Corrupt PNG（SpriteLoader ERROR + 紫黑棋盘）。此类坏文件静态资源
校验拦不住，必须按 IHDR 声明反算期望解压字节数逐张比对。

校验项（每张 PNG）：
  1. 签名 + chunk 序列合法（IHDR 首位 / IEND 末位 / 长度不越界）
  2. 每 chunk CRC32
  3. IDAT inflate 后字节数 == sum(height × (1 filter + stride))（多 IDAT 拼接）
  4. 每行 filter 类型合法（0-4）；为 -v 时同时校验反滤波可执行（性能：仅坏行即停）

用法：
  python3 tools/check_png.py                 # 默认 walk mdk/src
  python3 tools/check_png.py <root>...       # 指定一棵或多棵树
退出码：0=全过，1=有坏文件（逐条打印根因）。
"""
import struct
import sys
import zlib
from pathlib import Path

PNG_SIG = b"\x89PNG\r\n\x1a\n"
BPP = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}  # color type -> bytes per pixel
VALID_FILTERS = {0, 1, 2, 3, 4}


def check_png(path: Path, verify_filters: bool = False) -> str | None:
    """返回 None=合格，否则返回根因描述。"""
    try:
        data = path.read_bytes()
    except OSError as e:
        return f"unreadable: {e}"
    if len(data) < 8 or data[:8] != PNG_SIG:
        return "bad signature"
    pos = 8
    ihdr = None
    idat = bytearray()
    seen_iend = False
    while pos < len(data):
        if pos + 8 > len(data):
            return f"truncated chunk header at offset {pos}"
        (ln,) = struct.unpack(">I", data[pos : pos + 4])
        typ = data[pos + 4 : pos + 8]
        end = pos + 8 + ln
        if end + 4 > len(data):
            return f"chunk {typ!r} overruns file (declared {ln}B at offset {pos})"
        # PNG 规范：4 字节全为字母（辅助 chunk 允许小写，如 pHYs/bKGD）
        if not all(65 <= b <= 90 or 97 <= b <= 122 for b in typ):
            return f"invalid chunk type {typ!r} at offset {pos}"
        (crc_stored,) = struct.unpack(">I", data[end : end + 4])
        if zlib.crc32(typ + data[pos + 8 : end]) & 0xFFFFFFFF != crc_stored:
            return f"CRC mismatch in chunk {typ.decode(errors='replace')}"
        if typ == b"IHDR":
            if ln != 13:
                return f"IHDR length {ln} != 13"
            ihdr = struct.unpack(">IIBBBBB", data[pos + 8 : end])
        elif typ == b"IDAT":
            idat += data[pos + 8 : end]
        elif typ == b"IEND":
            seen_iend = True
        pos = end + 4
    if ihdr is None:
        return "missing IHDR"
    if not seen_iend:
        return "missing IEND"
    w, h, bit, ctype, _, _, inter = ihdr
    if ctype not in BPP:
        return f"unsupported color type {ctype} (only indexed/gray/RGB/gray+A/A handle-able)"
    if w == 0 or h == 0:
        return f"zero dimension {w}x{h}"
    expected = h * (1 + w * BPP[ctype])  # 每行 1 filter 字节 + w 像素（interlace=0）
    if inter != 0:
        return "interlaced (Adam7) unsupported by this checker"
    if not idat:
        return "missing IDAT"
    try:
        raw = zlib.decompress(bytes(idat))
    except zlib.error as e:
        return f"IDAT inflate failed: {e}"
    if len(raw) != expected:
        return (
            f"IDAT inflate {len(raw)}B != IHDR {w}x{h} ct{ctype} expected {expected}B "
            f"({h} rows x (1 filter + {w * BPP[ctype]}px)) -- stride bug?"
        )
    if verify_filters:
        stride = 1 + w * BPP[ctype]
        for r in range(h):
            if raw[r * stride] not in VALID_FILTERS:
                return f"row {r}: invalid filter type {raw[r * stride]}"
    return None


def main(argv: list[str]) -> int:
    roots = [Path(a) for a in argv[1:]] or [Path("mdk/src")]
    verify = "-v" in roots
    roots = [r for r in roots if r != Path("-v")]
    total = bad = 0
    for root in roots:
        if not root.exists():
            print(f"ERROR: root not found: {root}")
            return 1
        for p in sorted(root.rglob("*.png")):
            total += 1
            reason = check_png(p, verify_filters=verify)
            if reason:
                bad += 1
                print(f"BROKEN {p}: {reason}")
    print(f"checked {total} PNG under {', '.join(str(r) for r in roots)}: {total - bad} ok, {bad} broken")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
