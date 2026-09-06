#!/usr/bin/env python3
"""gen-forks.py — P20④ ModularUI vendored fork：从双腿上游快照再生成单树双消化面。

用途（ADR 2026-09-06-p20-modularui-fork-ruling §2 对冲姿势：跟随上游=重 vendored+重放偏离）：
重 vendored 新基线后（src/main = 上游 1.21.1 形原文），用本工具从双腿快照重生成：
  1. hunk 级 `//?` 分叉（小漂移文件）；
  2. leg-split 迁移（大漂移文件整体按腿进 src/forgeMain / src/neoforgeMain，原文零 chisel）。

用法：
    python3 gen-forks.py --a <1.20.1 快照根> --b <1.21.1 快照根> --tree <vendored 根> [--apply]
    快照根 = 上游仓 checkout/harvest 根（含 src/main/java）。
    缺 --apply 时 dry-run（只打印分类统计与清单）。

分类算法（对 src/main/java 中双腿共有的每个文件，a=1.20.1，b=1.21.1）：
- 无差 → 留共享树（零改）。
- 差异全部在注释行 → 留共享树 b 形（compile-neutral，comment-shape 偏离记台账）。
- 漂移率（changed/max(len(a),len(b))）≥ 0.15 或 hunk 数 ≥ 10 → **leg-split**：
  文件移出共享树——b 原文 → src/neoforgeMain/java，a 原文 → src/forgeMain/java。
  理由：大漂移文件按 hunk 分叉会把方法体切成交叉拼接的碎片（可读性/可维护性双输），
  整文件按腿放原文 = 零 chisel 语法、跟随上游时整文件重拷。
- 其余 hunk 级分叉：
    replace:  //? if neoforge {  <b 行>  //?} else {  /*<a 行>*/  //?}
    insert(b 有 a 无):  //? if neoforge {  <b 行>  //?}
    delete(a 有 b 无):  //? if forge {  /*<a 行>*/  //?}
  块注释包裹格式（mdk chisel 先例）：`/*` 紧贴首行行首、`*/` 紧贴收尾 `//?}`。
  forge 腿内容行含 `*/` 的 hunk（会提前终止包裹）保留 b 形不包，列 MANUAL 清单手工 chisel。

纪律：本工具只动 src/main/java 与两个单腿 java 目录；资源树与手写偏离（DIVERGE.md §3）
不归它管。重放顺序：git checkout <vendored 基线> -- third-party/modularui/src → 跑本工具
--apply → 回放 MANUAL 清单与手写偏离。
"""
import argparse
import difflib
import os
import shutil
import sys

COMMENT_PREFIXES = ('//', '/*', '*', '*/')
LEG_SPLIT_RATIO = 0.15
LEG_SPLIT_HUNKS = 10


def is_comment_line(line: str) -> bool:
    s = line.strip()
    return (not s) or s.startswith(COMMENT_PREFIXES)


def load(root: str, sub: str):
    out = {}
    base = os.path.join(root, sub)
    for dp, _, fns in os.walk(base):
        for f in fns:
            if f.endswith('.java'):
                rel = os.path.relpath(os.path.join(dp, f), base)
                with open(os.path.join(dp, f), encoding='utf-8', newline='') as fh:
                    out[rel] = fh.read().splitlines()
    return out


def wrap_forge(lines, indent=''):
    if not lines:
        return []
    wrapped = [indent + '/*' + lines[0]]
    for l in lines[1:]:
        wrapped.append(indent + l if l.strip() else l)
    wrapped.append(indent + '*/' + '//?}')
    return wrapped


def fork_block(tag, a_lines, b_lines, indent=''):
    out = []
    # 注意：wrap_forge 的末行 '*///?}' 已同时是块注释终止符与分叉 scope 关闭符（mdk 粘连格式），
    # replace/delete 分支不得再追加独立 '//?}'，否则 stitcher 解析失败（首跑实证 2026-09-06）。
    if tag == 'replace':
        out.append(indent + '//? if neoforge {')
        out += b_lines
        out.append(indent + '//?} else {')
        out += wrap_forge(a_lines, indent)
    elif tag == 'insert':  # b-only
        out.append(indent + '//? if neoforge {')
        out += b_lines
        out.append(indent + '//?}')
    elif tag == 'delete':  # a-only
        out.append(indent + '//? if forge {')
        out += wrap_forge(a_lines, indent)
    else:
        raise AssertionError(tag)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--a', required=True, help='1.20.1 快照根')
    ap.add_argument('--b', required=True, help='1.21.1 快照根')
    ap.add_argument('--tree', required=True, help='vendored 根（src/main 需为 b 形原文）')
    ap.add_argument('--apply', action='store_true')
    args = ap.parse_args()

    a = load(args.a, 'src/main/java')
    b = load(args.b, 'src/main/java')
    common = sorted(set(a) & set(b))
    tree = lambda *p: os.path.join(args.tree, *p)

    identical, comment_only, legsplit, forked, manual = [], [], [], [], []
    for rel in common:
        if a[rel] == b[rel]:
            identical.append(rel)
            continue
        ops = difflib.SequenceMatcher(None, a[rel], b[rel], autojunk=False).get_opcodes()
        hunks = [op for op in ops if op[0] != 'equal']
        if all(is_comment_line(l) for tag, i1, i2, j1, j2 in hunks
               for l in a[rel][i1:i2] + b[rel][j1:j2]):
            comment_only.append(rel)
            continue
        changed = sum(len(a[rel][i1:i2]) + len(b[rel][j1:j2]) for _, i1, i2, j1, j2 in hunks)
        if changed / max(len(a[rel]), len(b[rel])) >= LEG_SPLIT_RATIO or len(hunks) >= LEG_SPLIT_HUNKS:
            legsplit.append(rel)
            if args.apply:
                shared = tree('src/main/java', rel)
                for dst, lines in ((tree('src/neoforgeMain/java', rel), b[rel]),
                                   (tree('src/forgeMain/java', rel), a[rel])):
                    os.makedirs(os.path.dirname(dst), exist_ok=True)
                    with open(dst, 'w', encoding='utf-8', newline='') as fh:
                        fh.write('\n'.join(lines) + '\n')
                os.remove(shared)
            continue
        forked.append(rel)
        new = []
        for tag, i1, i2, j1, j2 in ops:
            if tag == 'equal':
                new += b[rel][j1:j2]
            elif all(is_comment_line(l) for l in a[rel][i1:i2] + b[rel][j1:j2]):
                # 纯注释 hunk：保持 b 形。要点——javadoc 内部的 hunk 绝不能包 //? 分叉，
                # stitcher 词法器把块注释内的标记当正文，解析直接失败（首跑实证 2026-09-06）。
                new += b[rel][j1:j2]
            elif any('*/' in l for l in a[rel][i1:i2]):
                new += b[rel][j1:j2]
                manual.append((rel, tag, i1, i2, j1, j2, a[rel][i1:i2]))
            else:
                indent = ''
                for l in b[rel][j1:j2] or a[rel][i1:i2]:
                    if l.strip():
                        indent = l[:len(l) - len(l.lstrip())]
                        break
                new += fork_block(tag, a[rel][i1:i2], b[rel][j1:j2], indent)
        if args.apply:
            with open(tree('src/main/java', rel), 'w', encoding='utf-8', newline='') as fh:
                fh.write('\n'.join(new) + '\n')

    print(f'common={len(common)} identical={len(identical)} comment_only={len(comment_only)} '
          f'leg_split={len(legsplit)} hunk_forked={len(forked)} manual_hunks={len(manual)}')
    for name, lst in (('COMMENT_ONLY', comment_only), ('LEG_SPLIT', legsplit)):
        print(f'--- {name} ({len(lst)})')
        for rel in lst:
            print(f'    {rel}')
    print(f'--- MANUAL_HUNKS ({len(manual)})')
    for rel, tag, i1, i2, j1, j2, lines in manual:
        print(f'    {rel} [{tag}] a:{i1 + 1}-{i2} b:{j1 + 1}-{j2}')
        for l in lines:
            print(f'        < {l}')
    if not args.apply:
        print('(dry-run：加 --apply 落盘)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
