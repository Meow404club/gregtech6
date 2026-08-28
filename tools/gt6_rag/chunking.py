"""Chunking helpers: split source files into retrieval-friendly chunks.

Returns (line, header, text) triples. `header` is used by the indexer to build
Anthropic-style contextual chunk prefixes before embedding (contextual retrieval).

Java: split top-level declarations (class/interface/enum) by brace depth.
Markdown: split by headings, keeping heading lineage as the header.
"""
from __future__ import annotations

import re

MAX_CHARS = 1600
HARD_MAX = 2200


def _split_by_top_level(text: str) -> list[tuple[int, str, str]]:
    """Return list of (line_no, header, body) for top-level Java declarations."""
    lines = text.splitlines(keepends=True)
    decls: list[tuple[int, str, str]] = []
    depth = 0
    start = None
    buf: list[str] = []
    header = ""
    for i, line in enumerate(lines):
        stripped = line.strip()
        if depth == 0 and start is None and (
            re.match(r"(public|private|protected|final|abstract|static|class|interface|enum|@)", stripped)
            and not stripped.startswith(("import ", "package ", "*", "/*", "//"))
        ):
            start = i
            header = ""
            buf = []
        if start is not None:
            buf.append(line)
            m = re.match(r"\s*((?:public|private|protected|final|abstract|sealed|static)\s+)*"
                         r"(class|interface|enum|record)\s+(\w+)", line)
            if m and not header:
                header = f"{m.group(2)} {m.group(3)}"
        depth += line.count("{") - line.count("}")
        if start is not None and depth <= 0 and "{" in "".join(buf):
            decls.append((start + 1, header or "(top-level)", "".join(buf)))
            start = None
            depth = 0
        elif start is not None and depth < 0:
            decls.append((start + 1, header or "(top-level)", "".join(buf)))
            start = None
            depth = 0
    if start is not None and buf:
        decls.append((start + 1, header or "(top-level)", "".join(buf)))
    if not decls:
        return [(1, "(file)", text)]
    first_ln = decls[0][0]
    prelude = "".join(lines[: first_ln - 1])
    if prelude.strip():
        decls.insert(0, (1, "(imports)", prelude))
    return decls


def _split_long(body: str, base_line: int) -> list[tuple[int, str]]:
    """Further split an over-long declaration into brace-balanced member chunks."""
    if len(body) <= HARD_MAX:
        return [(base_line, body)]
    lines = body.splitlines(keepends=True)
    out: list[tuple[int, str]] = []
    depth = 0
    start = 0
    for i, line in enumerate(lines):
        depth += line.count("{") - line.count("}")
        cur = "".join(lines[start : i + 1])
        if depth <= 0 and (len(cur) >= MAX_CHARS or i == len(lines) - 1):
            out.append((base_line + start, cur))
            start = i + 1
    if start < len(lines):
        out.append((base_line + start, "".join(lines[start:])))
    merged: list[tuple[int, str]] = []
    for ln, chunk in out:
        if merged and len(merged[-1][1]) + len(chunk) <= MAX_CHARS:
            merged[-1] = (merged[-1][0], merged[-1][1] + chunk)
        else:
            merged.append((ln, chunk))
    return merged or [(base_line, body)]


def chunk_java(text: str) -> list[tuple[int, str, str]]:
    """Yield (start_line, header, chunk_text) triples."""
    chunks: list[tuple[int, str, str]] = []
    for line_no, header, body in _split_by_top_level(text):
        if len(body) <= MAX_CHARS:
            chunks.append((line_no, header, body))
        else:
            for ln, part in _split_long(body, line_no):
                chunks.append((ln, header, part))
    return chunks


_MD_H = re.compile(r"^(#{1,6})\s+(.*)$")


def chunk_markdown(text: str) -> list[tuple[int, str, str]]:
    """Split markdown by headings; header is the heading lineage path."""
    lines = text.splitlines(keepends=True)
    sections: list[tuple[int, str, str]] = []
    stack: list[tuple[int, str]] = []
    cur: list[str] = []
    cur_line = 1
    for i, line in enumerate(lines):
        m = _MD_H.match(line)
        if m:
            if cur:
                sections.append((cur_line, " > ".join(t for _, t in stack) or "(top)", "".join(cur)))
                cur = []
            level = len(m.group(1))
            title = m.group(2).strip()
            while stack and stack[-1][0] >= level:
                stack.pop()
            stack.append((level, title))
            cur_line = i + 1
            cur.append(line)
        else:
            if cur or stack:
                cur.append(line)
    if cur:
        sections.append((cur_line, " > ".join(t for _, t in stack) or "(top)", "".join(cur)))
    chunks: list[tuple[int, str, str]] = []
    for line_no, path, body in sections:
        body = body.strip()
        if not body:
            continue
        if len(body) <= HARD_MAX:
            chunks.append((line_no, path, body))
        else:
            part: list[str] = []
            size = 0
            for para in body.split("\n\n"):
                if size + len(para) > MAX_CHARS and part:
                    chunks.append((line_no, path, "\n\n".join(part)))
                    part, size = [], 0
                part.append(para)
                size += len(para) + 2
            if part:
                chunks.append((line_no, path, "\n\n".join(part)))
    return chunks or [(1, "(file)", text)]


def chunk_file(lang: str, text: str) -> list[tuple[int, str, str]]:
    try:
        if lang == "java":
            return chunk_java(text)
        if lang == "md":
            return chunk_markdown(text)
    except Exception:
        pass
    out = []
    lines = text.splitlines(keepends=True)
    step, size = 60, 80
    for s in range(0, len(lines), step):
        out.append((s + 1, "(file)", "".join(lines[s : s + size])))
    return out
