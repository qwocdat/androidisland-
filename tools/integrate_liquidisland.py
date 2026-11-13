#!/usr/bin/env python3
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def run_cmd(cmd: list[str], check: bool = False) -> str:
    res = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
    if check and res.returncode != 0:
        print(f"Command failed: {' '.join(cmd)}\n{res.stderr}", file=sys.stderr)
    return res.stdout.strip()


def find_original_droidbubble_commit() -> str:
    commits = run_cmd(["git", "rev-list", "HEAD"]).splitlines()
    for c in commits:
        manifest = run_cmd(["git", "show", f"{c}:app/src/main/AndroidManifest.xml"])
        if manifest and "com.xiao.androidisland" not in manifest and "droidbubble" in manifest.lower():
            return c
    for c in commits:
        manifest = run_cmd(["git", "show", f"{c}:app/src/main/AndroidManifest.xml"])
        if manifest and "com.xiao.androidisland" not in manifest:
            return c
    return ""


def restore_original_project_if_needed():
    manifest_p = ROOT / "app/src/main/AndroidManifest.xml"
    needs_restore = (
        not manifest_p.exists()
        or "com.xiao.androidisland" in manifest_p.read_text(encoding="utf-8", errors="ignore")
        or not (ROOT / "app/src/main/java/com/droidbubble").exists()
    )
    orig_commit = find_original_droidbubble_commit()
    if orig_commit and needs_restore:
        print(f"===> Restoring original com.droidbubble.app files from commit {orig_commit}...")
        paths_to_restore = [
            "app/src/main/AndroidManifest.xml",
            "app/build.gradle.kts",
            "build.gradle.kts",
            "settings.gradle.kts",
            "gradle.properties",
            "gradle/libs.versions.toml",
            "app/src/main/res",
            "app/src/main/java/com/droidbubble",
        ]
        for rel in paths_to_restore:
            subprocess.run(["git", "checkout", orig_commit, "--", rel], cwd=ROOT)

    # Remove standalone replacement files from com/xiao/androidisland so only com.droidbubble.app is the app
    obsolete = [
        "app/src/main/java/com/xiao/androidisland/MainActivity.kt",
        "app/src/main/java/com/xiao/androidisland/LiquidControlCenterActivity.kt",
        "app/src/main/java/com/xiao/androidisland/AndroidIslandApp.kt",
        "app/src/main/java/com/xiao/androidisland/service",
        "app/src/main/java/com/xiao/androidisland/droidbubble",
        "app/src/main/res/xml/android_island_accessibility_config.xml",
        "app/src/main/res/drawable/ic_island_tile.xml",
        "app/src/main/res/values/colors.xml",
    ]
    for rel in obsolete:
        p = ROOT / rel
        if rel == "app/src/main/res/values/colors.xml" and orig_commit:
            # Check if colors.xml existed in orig_commit; if so restore it, else remove
            chk = subprocess.run(
                ["git", "cat-file", "-e", f"{orig_commit}:{rel}"], cwd=ROOT
            )
            if chk.returncode == 0:
                subprocess.run(["git", "checkout", orig_commit, "--", rel], cwd=ROOT)
                continue
        if p.is_dir():
            import shutil
            shutil.rmtree(p, ignore_errors=True)
        elif p.is_file():
            p.unlink(missing_ok=True)


def patch_build_gradle():
    gradle_p = ROOT / "app/build.gradle.kts"
    if not gradle_p.exists():
        return
    txt = gradle_p.read_text(encoding="utf-8")

    # 1. Ensure CMake externalNativeBuild is configured if CMakeLists.txt is present
    if (ROOT / "app/src/main/cpp/CMakeLists.txt").exists() and "src/main/cpp/CMakeLists.txt" not in txt:
        cmake_block = """
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
"""
        # Insert inside android { ... } right after defaultConfig or compileSdk
        m = re.search(r"compileSdk\s*=\s*\d+", txt)
        if m:
            idx = m.end()
            txt = txt[:idx] + "\n" + cmake_block + txt[idx:]

    # 2. Ensure Shizuku dependencies are present for ShizukuBridge.kt
    if "dev.rikka.shizuku" not in txt and "shizuku" not in txt:
        dep_match = re.search(r"dependencies\s*\{", txt)
        if dep_match:
            idx = dep_match.end()
            txt = (
                txt[:idx]
                + '\n    implementation("dev.rikka.shizuku:api:13.1.5")\n    implementation("dev.rikka.shizuku:provider:13.1.5")\n'
                + txt[idx:]
            )

    # 3. Ensure ExperimentalFoundationApi & Material3 opt-ins are globally active
    if "ExperimentalFoundationApi" not in txt:
        txt += """

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
"""
    gradle_p.write_text(txt, encoding="utf-8")


def parse_kotlin_function_params(txt: str, fun_pos: int):
    """
    Given position of 'fun FunctionName(', returns (param_names, body_open_brace_index, has_return_type).
    """
    open_paren = txt.find("(", fun_pos)
    if open_paren == -1:
        return [], -1, False

    depth = 0
    close_paren = -1
    for i in range(open_paren, len(txt)):
        ch = txt[i]
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                close_paren = i
                break
    if close_paren == -1:
        return [], -1, False

    params_raw = txt[open_paren + 1 : close_paren]
    # Split top-level commas
    parts = []
    cur = []
    d_paren = d_angle = d_brack = 0
    for ch in params_raw:
        if ch == "(":
            d_paren += 1
        elif ch == ")":
            d_paren -= 1
        elif ch == "<":
            d_angle += 1
        elif ch == ">":
            d_angle -= 1
        elif ch == "[":
            d_brack += 1
        elif ch == "]":
            d_brack -= 1
        if ch == "," and d_paren == 0 and d_angle == 0 and d_brack == 0:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        parts.append("".join(cur).strip())

    param_names = []
    for p in parts:
        if not p:
            continue
        # Remove default value '= ...' at top level
        colon_idx = p.find(":")
        if colon_idx == -1:
            continue
        left = p[:colon_idx].strip()
        # Take last identifier token (handles 'val x', 'var x', '@Composable x')
        tokens = re.findall(r"[A-Za-z_][A-Za-z0-9_]*", left)
        if tokens:
            param_names.append(tokens[-1])

    # Find opening '{' after close_paren
    body_brace = txt.find("{", close_paren)
    if body_brace == -1:
        return param_names, -1, False
    between = txt[close_paren + 1 : body_brace].strip()
    has_non_unit_return = bool(between.startswith(":") and "Unit" not in between)
    return param_names, body_brace, has_non_unit_return


def patch_island_view():
    iv_p = ROOT / "app/src/main/java/com/droidbubble/app/island/IslandView.kt"
    if not iv_p.exists():
        return
    txt = iv_p.read_text(encoding="utf-8")
    if "LiquidIslandDroidBubbleBridge" in txt:
        return

    m = re.search(r"\bfun\s+IslandView\s*\(", txt)
    if not m:
        return
    params, brace_idx, has_return = parse_kotlin_function_params(txt, m.start())
    if brace_idx == -1 or has_return:
        return

    args_str = ", ".join(params)
    cond_args = f"androidx.compose.ui.platform.LocalContext.current, {args_str}" if args_str else "androidx.compose.ui.platform.LocalContext.current"
    injection = f"""
    if (com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})) {{
        com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.RenderLiquidIslandOverlay({args_str})
        return
    }}
"""
    txt = txt[: brace_idx + 1] + injection + txt[brace_idx + 1 :]
    iv_p.write_text(txt, encoding="utf-8")
    print("===> Hooked LiquidIslandDroidBubbleBridge into IslandView.kt")


def patch_liquid_home_tab():
    lht_p = ROOT / "app/src/main/java/com/droidbubble/app/ui/liquid/LiquidHomeTab.kt"
    if not lht_p.exists():
        return
    txt = lht_p.read_text(encoding="utf-8")
    if "LiquidIslandDroidBubbleBridge" in txt:
        return

    # 1. Hook any dedicated preview Composable function in LiquidHomeTab.kt that contains FastRewind & FastForward
    composables = list(re.finditer(r"\bfun\s+([A-Za-z0-9_]+)\s*\(", txt))
    hooked_preview_fn = False
    for idx, cm in enumerate(composables):
        fn_name = cm.group(1)
        if fn_name == "LiquidHomeTab":
            continue
        params, brace_idx, has_return = parse_kotlin_function_params(txt, cm.start())
        if brace_idx == -1 or has_return:
            continue
        next_start = composables[idx + 1].start() if idx + 1 < len(composables) else len(txt)
        fn_body = txt[brace_idx:next_start]
        if "FastRewind" in fn_body and "FastForward" in fn_body:
            args_str = ", ".join(params)
            cond_args = f"androidx.compose.ui.platform.LocalContext.current, {args_str}" if args_str else "androidx.compose.ui.platform.LocalContext.current"
            inj = f"""
    if (com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})) {{
        com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.RenderLiquidIslandHomePreview({args_str})
        return
    }}
"""
            txt = txt[: brace_idx + 1] + inj + txt[brace_idx + 1 :]
            hooked_preview_fn = True
            print(f"===> Hooked LiquidIslandHomePreview into preview composable {fn_name} in LiquidHomeTab.kt")
            break

    # 2. Also hook top of LiquidHomeTab(...) to keep DesignLanguage state synced (and render preview if inline)
    m_home = re.search(r"\bfun\s+LiquidHomeTab\s*\(", txt)
    if m_home:
        params, brace_idx, has_return = parse_kotlin_function_params(txt, m_home.start())
        if brace_idx != -1 and not has_return:
            args_str = ", ".join(params)
            cond_args = f"androidx.compose.ui.platform.LocalContext.current, {args_str}" if args_str else "androidx.compose.ui.platform.LocalContext.current"
            if hooked_preview_fn:
                inj = f"\n    com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})\n"
                txt = txt[: brace_idx + 1] + inj + txt[brace_idx + 1 :]
            else:
                # Find the main Column inside LiquidHomeTab so the preview card sits at the top of the scroll column
                col_m = re.search(r"\bColumn\s*\(", txt[brace_idx:])
                if col_m:
                    _, col_brace, _ = parse_kotlin_function_params(txt, brace_idx + col_m.start())
                    if col_brace != -1:
                        inj = f"""
        if (com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})) {{
            com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.RenderLiquidIslandHomePreview({args_str})
        }}
"""
                        txt = txt[: col_brace + 1] + inj + txt[col_brace + 1 :]

    lht_p.write_text(txt, encoding="utf-8")
    print("===> Hooked LiquidIslandDroidBubbleBridge into LiquidHomeTab.kt")


def patch_settings_repository():
    sr_p = ROOT / "app/src/main/java/com/droidbubble/app/data/SettingsRepository.kt"
    if not sr_p.exists():
        return
    txt = sr_p.read_text(encoding="utf-8")
    if "LiquidIslandDroidBubbleBridge" in txt:
        return
    # Whenever designLanguage is resolved in SettingsRepository, notify LiquidIslandDroidBubbleBridge
    txt_new = re.sub(
        r"(designLanguage\s*=\s*)([^\n,]+)",
        r"\1(\2).also { com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.updateDesignLanguage(it) }",
        txt,
        count=1,
    )
    if txt_new != txt:
        sr_p.write_text(txt_new, encoding="utf-8")
        print("===> Hooked designLanguage observer in SettingsRepository.kt")


def main():
    restore_original_project_if_needed()
    patch_build_gradle()
    patch_settings_repository()
    patch_island_view()
    patch_liquid_home_tab()


if __name__ == "__main__":
    main()
