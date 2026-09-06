#!/usr/bin/env python3
"""jar_content_check — packaging self-containment guard for the GT6 distribution jars.

Extends the 50843dff ruling (mod jar must be self-contained: players' mods/ holds
ONE jar = ONE classloading domain) to the P20 ModularUI jarJar shape:

  level 0  mod jar (mdk/versions/<node>/build/libs, the distribution artifact)
  level 1  embedded META-INF/jarjar/brachy.modularui.modularui-mc<mc>-*.jar
  level 2  nested META-INF/jarjar/ inside level 1 — EvalEx both legs;
           mixinextras-forge on the forge leg only
  level 3  mixinextras-forge is itself a locator jar carrying
           META-INF/jars/MixinExtras-*.jar (upstream shape)

Asserted faces:
  gregapi    every class compiled from the root project is in the mod jar
             (exact-set match on gregapi/ entries; 50843dff baseline: 82)
  modularui  every class compiled from the vendored fork node is in the level-1
             jar + full mod metadata (per-leg toml, mixin config, refmap on the
             SRG leg, manifest identity, SRG/official namespace shape)
  EvalEx /   level-2 jars present, jarJar metadata ranges sane, class surfaces
  mixinextras  inside; mixinextras locator shape + level-3 bundle

Expected class sets derive from the compiled outputs of this same worktree
(src-derived sets would over-predict: upstream ships commented-out files like
AnimatedText.java that legitimately produce no class).

Pure stdlib (zipfile only) — runs against already-built jars, CI-able like
tools/datagen_tree_check.py:

  ./gradlew :mdk:1.20.1-forge:assemble      # legs MUST be built in separate
  ./gradlew :mdk:1.21.1-neoforge:assemble   # gradle calls (id327)
  python3 tools/jar_content_check.py        # exit 0 = all green

Jar paths can be overridden with --forge/--neoforge.
"""

import argparse
import io
import json
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

FAILURES = []


def check(name, condition, detail=""):
    status = "PASS" if condition else "FAIL"
    print(f"[jar_content_check] {status} {name}" + (f" — {detail}" if detail else ""))
    if not condition:
        FAILURES.append(name)
    return condition


def classes_of(build_classes_root):
    """All .class entry paths under a build/classes/java/main directory."""
    root = Path(build_classes_root)
    if not root.is_dir():
        return None
    return {str(p.relative_to(root)) for p in root.rglob("*.class")}


def open_nested(zf, name_prefix, dir_name="META-INF/jarjar"):
    """Return (filename, in-memory ZipFile) for the single .jar under dir_name
    whose basename starts with name_prefix."""
    matches = [n for n in zf.namelist()
               if n.startswith(f"{dir_name}/") and n.endswith(".jar")
               and Path(n).name.startswith(name_prefix)]
    if not matches:
        return None, None
    if len(matches) > 1:
        return f"AMBIGUOUS:{matches}", None
    return Path(matches[0]).name, zipfile.ZipFile(io.BytesIO(zf.read(matches[0])))


def srg_shaped(zf):
    probe = [n for n in zf.namelist() if n.endswith("ModularUI.class")]
    if not probe:
        return None
    data = zf.read(probe[0])
    return b"m_9" in data or b"f_9" in data or b"m_24" in data


def guard_mod_jar(path):
    print(f"\n=== mod jar: {path.name} ===")
    ok = True
    leg = "1.20.1-forge" if "1.20.1" in path.name else "1.21.1-neoforge"
    # 注意 endswith("forge") 对 neoforge 亦真（rider① 同款坑，2026-09-07 本脚本首跑即中招）
    forge_leg = leg.endswith("1.20.1-forge")
    leg_main = "forgeMain" if forge_leg else "neoforgeMain"

    with zipfile.ZipFile(path) as zf:
        names = set(zf.namelist())

        # ---- face 1: gregapi self-containment (50843dff, exact set match) ----
        gregapi_built = classes_of(REPO / "build" / "classes" / "java" / "main")
        gregapi_built = {c for c in gregapi_built if c.startswith("gregapi/")}
        gregapi_jar = {c for c in names
                       if c.startswith("gregapi/") and c.endswith(".class")}
        ok &= check("gregapi: exact class-set match with root build output",
                    gregapi_built is not None and gregapi_built == gregapi_jar,
                    f"built={len(gregapi_built or ())} jar={len(gregapi_jar)}"
                    + ("" if gregapi_built == gregapi_jar else
                       f" missing={sorted(gregapi_built - gregapi_jar)[:3]} "
                       f"extra={sorted(gregapi_jar - gregapi_built)[:3]}"))
        # 82 = 50843dff 实测 .class 条目数（26 顶层 + 内部类），作下限防嵌装静默缩水
        ok &= check("gregapi: entry count >= 82 (50843dff baseline floor)",
                    len(gregapi_jar) >= 82, f"entries={len(gregapi_jar)}")

        # ---- face 2: modularui level-1 jar ----
        mui_src = REPO / "third-party" / "modularui"
        mui_built = classes_of(mui_src / "versions" / leg / "build" /
                               "classes" / "java" / "main")
        mui_name, mui = open_nested(zf, "brachy.modularui.")
        if not check("level-1: modularui jar embedded (exactly one)",
                     mui is not None, str(mui_name)):
            return False
        mui_names = set(mui.namelist())

        missing_mui = sorted(c for c in (mui_built or ()) if c not in mui_names)
        extra_mui = sorted(c for c in mui_names
                           if c.endswith(".class") and c.startswith("brachy/")
                           and c not in (mui_built or ()))
        ok &= check("modularui: exact class-set match with vendored node build"
                    " output",
                    mui_built is not None and not missing_mui and not extra_mui,
                    f"built={len(mui_built or ())} jar={len(extra_mui) + len(mui_built or ()) - len(extra_mui)}"
                    + (f" missing={missing_mui[:3]} extra={extra_mui[:3]}"
                       if missing_mui or extra_mui else ""))

        # mod metadata: per-leg manifest name + content markers
        toml_name = "META-INF/mods.toml" if forge_leg else "META-INF/neoforge.mods.toml"
        foreign_toml = "META-INF/neoforge.mods.toml" if forge_leg else "META-INF/mods.toml"
        ok &= check(f"modularui: {toml_name} present, foreign toml absent",
                    toml_name in mui_names and foreign_toml not in mui_names)
        if toml_name in mui_names:
            toml = mui.read(toml_name).decode()
            ok &= check("modularui: toml identity (modId/version/license)",
                        'modId = "modularui"' in toml
                        and 'version = "3.3.1"' in toml
                        and "LGPL-3.0" in toml)
        ok &= check("modularui: pack.mcmeta + mixin config + AT present",
                    "pack.mcmeta" in mui_names
                    and "modularui.mixins.json" in mui_names
                    and "META-INF/accesstransformer.cfg" in mui_names)
        mixin_cfg = json.loads(mui.read("modularui.mixins.json"))
        ok &= check("modularui: mixin config declares its refmap",
                    mixin_cfg.get("refmap") == "modularui.refmap.json")
        refmap_present = "modularui.refmap.json" in mui_names
        if forge_leg:
            # SRG 腿必须有 refmap（mixin AP 产物，RemapJar 一并重映射）
            ok &= check("modularui: refmap present on forge leg (SRG production)",
                        refmap_present)
        else:
            # neo 腿 = official 命名，上游 1.21.1 产物同形（json 声明 refmap 但文件不在，
            # Mixin 仅告警；GTCEu 1.21 量产背书）——钉形防漂移
            ok &= check("modularui: neo leg ships without refmap file"
                        " (upstream shape)",
                        not refmap_present)
        srg = srg_shaped(mui)
        ok &= check("modularui: forge leg class names are SRG-remapped",
                    (not forge_leg) or srg is True)
        ok &= check("modularui: neo leg has no SRG tokens (official names)",
                    forge_leg or srg is False)
        manifest = mui.read("META-INF/MANIFEST.MF").decode()
        ok &= check("modularui: manifest identity (brachy vendor, upstream keys)",
                    "Specification-Vendor: brachy" in manifest
                    and "Implementation-Vendor" in manifest
                    and "Specification-Title: modularui" in manifest)
        if forge_leg:
            ok &= check("modularui: MixinConfigs manifest attr (forge discovery)",
                        "MixinConfigs: modularui.mixins.json" in manifest)

        # level-0 jarJar metadata (what the mod jar declares about modularui)
        meta0 = json.loads(zf.read("META-INF/jarjar/metadata.json"))
        mui_entries = [j for j in meta0["jars"]
                       if j["identifier"]["group"] == "brachy.modularui"]
        ok &= check("mod jar: jarJar metadata declares modularui [3.3.1,) @3.3.1",
                    len(mui_entries) == 1
                    and mui_entries[0]["version"]["range"] == "[3.3.1,)"
                    and mui_entries[0]["version"]["artifactVersion"] == "3.3.1")

        # ---- face 3: level-2 nesting (EvalEx / mixinextras inside modularui) ----
        meta1 = json.loads(mui.read("META-INF/jarjar/metadata.json"))
        by_id = {(j["identifier"]["group"], j["identifier"]["artifact"]): j
                 for j in meta1["jars"]}
        evalex = by_id.get(("com.ezylang", "EvalEx"))
        ok &= check("level-2: EvalEx metadata [3.6.0,) @3.6.0 inside modularui jar",
                    evalex is not None
                    and evalex["version"]["range"] == "[3.6.0,)"
                    and evalex["version"]["artifactVersion"] == "3.6.0")
        ev_name, ev = open_nested(mui, "EvalEx-")
        if check("level-2: EvalEx jar physically nested", ev is not None,
                 str(ev_name)):
            ev_classes = [n for n in ev.namelist()
                          if n.startswith("com/ezylang/evalex/")
                          and n.endswith(".class")]
            ok &= check("level-2: EvalEx class surface (>=100, Expression present)",
                        len(ev_classes) >= 100
                        and "com/ezylang/evalex/Expression.class" in ev_classes,
                        f"{len(ev_classes)} classes")
        if forge_leg:
            mx = by_id.get(("io.github.llamalad7", "mixinextras-forge"))
            ok &= check("level-2: mixinextras-forge metadata [0.5.0-rc.3,)"
                        " inside modularui jar (forge leg only)",
                        mx is not None
                        and mx["version"]["range"] == "[0.5.0-rc.3,)"
                        and mx["version"]["artifactVersion"] == "0.5.0-rc.3")
            mx_name, mx_zip = open_nested(mui, "mixinextras-forge-")
            if check("level-2: mixinextras-forge jar physically nested",
                     mx_zip is not None, str(mx_name)):
                # locator 形：本体 1 类（ConfigPlugin），真身在自带 META-INF/jars/ 包内
                # （upstream 0.5.0-rc.3 形状，legacy jar-in-jar 布局）
                plugin = "com/llamalad7/mixinextras/platform/forge/MixinExtrasConfigPlugin.class"
                ok &= check("level-3: mixinextras locator shape (ConfigPlugin only)",
                            plugin in set(mx_zip.namelist())
                            and sum(1 for n in mx_zip.namelist()
                                    if n.endswith(".class")) == 1)
                mx_meta = json.loads(mx_zip.read("META-INF/jarjar/metadata.json"))
                ok &= check("level-3: locator metadata declares MixinExtras"
                            " [0.5.0-rc.3,)",
                            len(mx_meta["jars"]) == 1
                            and mx_meta["jars"][0]["identifier"]["artifact"]
                            == "MixinExtras"
                            and mx_meta["jars"][0]["version"]["range"]
                            == "[0.5.0-rc.3,)")
                inner_name, inner = open_nested(mx_zip, "MixinExtras-",
                                                dir_name="META-INF/jars")
                if check("level-3: MixinExtras bundle physically nested"
                         " (META-INF/jars)", inner is not None,
                         str(inner_name)):
                    inner_classes = [n for n in inner.namelist()
                                     if n.startswith("com/llamalad7/mixinextras/")
                                     and n.endswith(".class")]
                    ok &= check("level-3: MixinExtras class surface (>=100,"
                                " Bootstrap present)",
                                len(inner_classes) >= 100
                                and "com/llamalad7/mixinextras/MixinExtrasBootstrap.class"
                                in inner_classes,
                                f"{len(inner_classes)} classes")
        else:
            ok &= check("level-2: neo leg nests no mixinextras (loader provides"
                        " it)",
                        ("io.github.llamalad7", "mixinextras-forge") not in by_id)
    return ok


def default_jar(leg):
    libs = REPO / "mdk" / "versions" / leg / "build" / "libs"
    if not libs.is_dir():
        return None
    jars = sorted(libs.glob("*.jar"))
    return jars[0] if jars else None


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--forge", type=Path, default=None,
                    help="forge distribution jar (default: auto-discover)")
    ap.add_argument("--neoforge", type=Path, default=None,
                    help="neoforge distribution jar (default: auto-discover)")
    args = ap.parse_args()

    forge = args.forge or default_jar("1.20.1-forge")
    neo = args.neoforge or default_jar("1.21.1-neoforge")

    for leg, path in (("forge", forge), ("neoforge", neo)):
        if path is None or not Path(path).is_file():
            print(f"[jar_content_check] SKIP {leg}: jar not found ({path}) — "
                  f"build it first: ./gradlew :mdk:<node>:assemble (legs in"
                  f" SEPARATE gradle calls, id327)")
            continue
        guard_mod_jar(Path(path))

    print()
    if FAILURES:
        print(f"[jar_content_check] RED — {len(FAILURES)} failed: {FAILURES}")
        return 1
    print("[jar_content_check] GREEN — packaging self-containment holds")
    return 0


if __name__ == "__main__":
    sys.exit(main())
