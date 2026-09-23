#!/usr/bin/env python3
"""Generate the GT6 written-book text data class (task p35-books-written).

Reads the GregTech 1.7.10 book loader (tmp/gt6-1.7.10/src/main/java/gregtech/loaders/b/
Loader_Books.java — NOT in the repo, the ADR §1.1 CI principle like gen_zhcn_ref.py's dump)
and emits mdk/src/main/java/gregtech6/registry/GT6BookText.java: the 15 static books of the
upstream "gt.books" obtainability face (Loader_Loot.java:340-356) with their pages VERBATIM
(no hand-copying — this script is the only writer, the committed file is the reviewable
distillate).

Census basis (task card 申报, coordinator-approved 15-book scope): Loader_Books.java carries
20 createWrittenBook calls, not the 3 the p35 census-refresh ledger claimed (:45/:61/:72 are
just the first three — the same read-truncation class as the RM "67 字段" errata). Five are
CUT here, each with its own evidence:
  - Manual_Punch_Cards  (:45)  — no obtainability face upstream at all (punch cards never
                                 implemented; the book is a placeholder).
  - Manual_Microwave    (:61)  — obtainable only via the Microwave recipe-map easter egg
                                 (RecipeMapMicrowave.java:56); the Microwave RM is not ported.
  - Manual_Portal_TF    (:111) — Twilight Forest domain (loot from the TF portal room chest +
                                 TwilightTreasureReplacer.java:164/:176); TF is not ported.
  - Manual_Alloys       (:582) — pages generated at runtime from OreDictMaterial.ALLOYS
                                 (:574-580), not static text; cannot be mechanically pinned.
  - Manual_Elements     (:596) — pages generated at runtime from MATERIAL_ARRAY elements
                                 (:588-594), same class.
The generator enforces this set: the emitted BOOKS list is exactly the 15, and the page
literals are extracted by a string-aware Java expression walker (adjacent "..."+"..."
concatenations joined, the tAlexGryllsIntro variable inlined, the tBook.add(...) statement
sequences for Manual_Tools/Manual_Smeltery replayed from their last tBook.clear()).

Upstream semantics preserved verbatim here: the '¶' page-marker characters are kept as-is in
the emitted literals; the runtime conversion ('¶' -> '\\n', pages >= 256 chars dropped) lives
in GT6Books (the single-source converter, gregapi/util/UT.java:622-628 semantics). No static
page hits the 256 bound (measured max 253), the rule only ever fired for the dynamic books.

Idempotent: same upstream file -> byte-identical output. NOT run by the build (the committed
file is the source of truth; rerun is a review tool).

Usage:
  python3 extract_books.py --upstream /path/to/Loader_Books.java \
      [--out ../mdk/src/main/java/gregtech6/registry/GT6BookText.java]
"""

import argparse
import re
import sys
from pathlib import Path

# The CUT set (see docstring). The generator refuses to emit these and lists them in the
# file header comment so the declared deviation travels with the data class.
CUT_BOOKS = [
    "Manual_Punch_Cards",
    "Manual_Microwave",
    "Manual_Portal_TF",
    "Manual_Alloys",
    "Manual_Elements",
]


def strip_comments(text: str) -> str:
    """Removes // line comments (string-aware) and /* */ block comments, preserving newlines."""
    out = []
    i, n = 0, len(text)
    in_str = False
    while i < n:
        c = text[i]
        if in_str:
            out.append(c)
            if c == "\\" and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if c == '"':
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def java_string_literal(expr: str) -> str:
    """Decodes a single Java string literal (the expr must be exactly one literal)."""
    expr = expr.strip()
    if not (expr.startswith('"') and expr.endswith('"')):
        raise ValueError(f"not a plain literal: {expr[:60]!r}")
    body = expr[1:-1]
    out = []
    i, n = 0, len(body)
    while i < n:
        c = body[i]
        if c == "\\" and i + 1 < n:
            nxt = body[i + 1]
            escapes = {"n": "\n", "t": "\t", "r": "\r", '"': '"', "\\": "\\", "'": "'",
                       "b": "\b", "f": "\f", "0": "\0"}
            if nxt in escapes:
                out.append(escapes[nxt])
                i += 2
                continue
            if nxt == "u":
                out.append(chr(int(body[i + 2:i + 6], 16)))
                i += 6
                continue
            raise ValueError(f"unknown escape \\{nxt}")
        out.append(c)
        i += 1
    return "".join(out)


def join_string_expr(expr: str) -> str:
    """Concatenates the string literals of an expression (adjacent "..."+"..." form).

    A bare identifier (tAlexGryllsIntro) is NOT a literal — returned as None marker via
    ValueError by the caller's resolution pass.
    """
    tokens = re.findall(r'"(?:[^"\\]|\\.)*"', expr)
    if not tokens:
        raise ValueError(f"no literals in expression: {expr.strip()[:60]!r}")
    return "".join(java_string_literal(t) for t in tokens)


def split_top_level(body: str, sep: str = ",") -> list:
    """Splits body on sep at zero bracket depth, string-aware."""
    parts, depth, cur, in_str = [], 0, [], False
    i, n = 0, len(body)
    while i < n:
        c = body[i]
        if in_str:
            cur.append(c)
            if c == "\\":
                cur.append(body[i + 1])
                i += 2
                continue
            if c == '"':
                in_str = False
        elif c == '"':
            in_str = True
            cur.append(c)
        elif c in "([{":
            depth += 1
            cur.append(c)
        elif c in ")]}":
            depth -= 1
            cur.append(c)
        elif c == sep and depth == 0:
            parts.append("".join(cur))
            cur = []
        else:
            cur.append(c)
        i += 1
    if "".join(cur).strip():
        parts.append("".join(cur))
    return parts


def matching_paren(text: str, open_idx: int) -> int:
    """Index of the ')' matching the '(' at open_idx, string-aware."""
    depth, in_str, i, n = 0, False, open_idx, len(text)
    while i < n:
        c = text[i]
        if in_str:
            if c == "\\":
                i += 2
                continue
            if c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced parens")


def snake(mapping: str) -> str:
    """Manual_Printer -> manual_printer (the registry path form; no double underscore)."""
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", mapping).lower()


def extract(upstream: Path):
    raw = upstream.read_text(encoding="utf-8")
    # real-file line numbers for the emitted provenance comments: measured on the RAW
    # text (the stripped text shifts lines by the stripped comment blocks)
    raw_lines = {m.group(1): raw.count("\n", 0, m.start()) + 1
                 for m in re.finditer(r'createWrittenBook\("([A-Za-z_]+)"', raw)}
    text = strip_comments(raw)
    lines = text.split("\n")

    # resolve the shared page variables (String tAlexGryllsIntro = "...";)
    variables = {}
    for var_m in re.finditer(r'String\s+(t[A-Za-z]+)\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', text):
        variables[var_m.group(1)] = java_string_literal('"' + var_m.group(2) + '"')

    books = []  # (mapping, title, author, pages, first_line)
    for m in re.finditer(r"createWrittenBook\(", text):
        open_idx = m.end() - 1
        close_idx = matching_paren(text, open_idx)
        call = text[open_idx + 1:close_idx]
        mapping = java_string_literal(split_top_level(call)[0])
        first_line = raw_lines[mapping]
        args = split_top_level(call)
        title = join_string_expr(args[1])
        author = join_string_expr(args[2])
        pages = []
        if "new String[]" in args[4]:
            brace = args[4][args[4].index("{") + 1:]
            elements = split_top_level(brace)
            for el in elements:
                el = el.strip()
                if not el:
                    continue
                ident = re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", el)
                if ident:
                    pages.append(variables[el])  # the shared-variable first page
                else:
                    pages.append(join_string_expr(el))
        elif "tBook.toArray" in args[4]:
            # the tBook-built form: replay the tBook.add(...) statements from the last
            # tBook.clear() before this call (Manual_Tools/Manual_Smeltery shape). An
            # add() over a bare variable (tPage — the Alloy/Elements generator loops)
            # marks the book DYNAMIC: page text is runtime-computed, unextractable.
            dynamic = False
            call_start = text.rfind("tBook.clear()", 0, m.start())
            segment = text[call_start:m.start()]
            for add_m in re.finditer(r"tBook\.add\(", segment):
                a_open = add_m.end() - 1
                a_close = matching_paren(segment, a_open)
                try:
                    pages.append(join_string_expr(segment[a_open + 1:a_close]))
                except ValueError:
                    dynamic = True
                    break
            if dynamic:
                # CUT candidates only (Alloys/Elements); validated against CUT_BOOKS below
                print(f"  dynamic (runtime-generated pages, not extractable): {mapping}")
                continue
        else:
            raise ValueError(f"{mapping}: unrecognized page form: {args[4][:40]!r}")
        books.append((mapping, title, author, pages, first_line))
    return books


def emit_java(books, out_path: Path) -> None:
    # the extractor does not see the dynamic books (Alloys/Elements skip with a print);
    # the static CUT books must all be present and the kept set exactly the approved 15
    kept = [b for b in books if b[0] not in CUT_BOOKS]
    cut_seen = [b[0] for b in books if b[0] in CUT_BOOKS]
    missing = [c for c in ["Manual_Punch_Cards", "Manual_Microwave", "Manual_Portal_TF"] if c not in cut_seen]
    if missing:
        sys.exit(f"FATAL: static CUT books absent from upstream file: {missing}")
    if len(kept) != 15:
        sys.exit(f"FATAL: expected the approved 15-book set, got {len(kept)}: {[b[0] for b in kept]}")

    out = []
    out.append("package gregtech6.registry;")
    out.append("")
    out.append("import java.util.List;")
    out.append("")
    out.append("/**")
    out.append(" * GENERATED by mdk/tools/extract_books.py from the GregTech 1.7.10 book loader")
    out.append(" * (loaders/b/Loader_Books.java) — DO NOT HAND-EDIT; rerun the script to regenerate.")
    out.append(" *")
    out.append(" * <p>The " + str(len(kept)) + " static books of the upstream \"gt.books\" obtainability face")
    out.append(" * (Loader_Loot.java:340-356), pages VERBATIM ('¶' page markers included — the")
    out.append(" * runtime conversion lives in GT6Books, the single-source converter, UT.java:622-628")
    out.append(" * semantics). Task p35-books-written, coordinator-approved 15-book scope; the five CUT")
    out.append(" * books carry their own evidence in the generator docstring (Punch_Cards/Microwave/")
    out.append(" * Portal_TF/Alloys/Elements).")
    out.append(" */")
    out.append("public final class GT6BookText {")
    out.append("")
    out.append("\t/** One book: the registry path, the title/author columns and the raw pages (upstream order). */")
    out.append("\tpublic record BookText(String path, String title, String author, List<String> pages) {")
    out.append("\t}")
    out.append("")
    out.append("\tpublic static final List<BookText> BOOKS = List.of(")

    def esc(s: str) -> str:
        return s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\t", "\\t")

    for idx, (mapping, title, author, pages, line_no) in enumerate(kept):
        if not pages:
            sys.exit(f"FATAL: {mapping} has no pages")
        out.append(f"\t\t\t// upstream Loader_Books.java:{line_no} — {len(pages)} pages")
        out.append("\t\t\tnew BookText(\"" + snake(mapping) + "\", \"" + esc(title) + "\", \"" + esc(author) + "\", List.of(")
        for p_idx, page in enumerate(pages):
            comma = "," if p_idx < len(pages) - 1 else ""
            out.append("\t\t\t\t\"" + esc(page) + "\"" + comma)
        semicolon = "," if idx < len(kept) - 1 else ");"
        out.append("\t\t\t))" + semicolon)
    out.append("")
    out.append("\tprivate GT6BookText() {")
    out.append("\t}")
    out.append("}")
    out.append("")
    out_path.write_text("\n".join(out), encoding="utf-8")
    print(f"emitted {len(kept)} books -> {out_path}")
    for mapping, title, _author, pages, line_no in kept:
        print(f"  {snake(mapping):28s} {len(pages):3d} pages  (upstream :{line_no})  max page len {max(len(p) for p in pages)}")


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--upstream", type=Path, required=True, help="path to Loader_Books.java")
    ap.add_argument("--out", type=Path, default=Path(__file__).resolve().parent.parent
                    / "src/main/java/gregtech6/registry/GT6BookText.java")
    args = ap.parse_args(argv)
    books = extract(args.upstream)
    emit_java(books, args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
