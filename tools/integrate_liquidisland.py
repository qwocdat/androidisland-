#!/usr/bin/env python3
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def run_cmd(cmd: list[str]) -> str:
    res = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
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
    orig_commit = find_original_droidbubble_commit()
    manifest_p = ROOT / "app/src/main/AndroidManifest.xml"
    needs_restore = (
        not manifest_p.exists()
        or "com.xiao.androidisland" in manifest_p.read_text(encoding="utf-8", errors="ignore")
        or not (ROOT / "app/src/main/java/com/droidbubble").exists()
    )
    if orig_commit and needs_restore:
        print(f"===> Restoring original com.droidbubble.app files from commit {orig_commit}...")
        for rel in [
            "app/src/main/AndroidManifest.xml",
            "app/build.gradle.kts",
            "build.gradle.kts",
            "settings.gradle.kts",
            "gradle.properties",
            "gradle/libs.versions.toml",
            "app/src/main/res",
            "app/src/main/java/com/droidbubble",
        ]:
            subprocess.run(["git", "checkout", orig_commit, "--", rel], cwd=ROOT)
    elif orig_commit:
        # Always restore SettingsRepository.kt cleanly from orig_commit
        subprocess.run(
            ["git", "checkout", orig_commit, "--", "app/src/main/java/com/droidbubble/app/data/SettingsRepository.kt"],
            cwd=ROOT,
        )

    for rel in [
        "app/src/main/java/com/xiao/androidisland/MainActivity.kt",
        "app/src/main/java/com/xiao/androidisland/LiquidControlCenterActivity.kt",
        "app/src/main/java/com/xiao/androidisland/AndroidIslandApp.kt",
        "app/src/main/java/com/xiao/androidisland/service",
        "app/src/main/java/com/xiao/androidisland/droidbubble",
        "app/src/main/res/xml/android_island_accessibility_config.xml",
        "app/src/main/res/drawable/ic_island_tile.xml",
        "app/src/main/res/values/colors.xml",
    ]:
        p = ROOT / rel
        if rel == "app/src/main/res/values/colors.xml" and orig_commit:
            chk = subprocess.run(["git", "cat-file", "-e", f"{orig_commit}:{rel}"], cwd=ROOT)
            if chk.returncode == 0:
                subprocess.run(["git", "checkout", orig_commit, "--", rel], cwd=ROOT)
                continue
        if p.is_dir():
            shutil.rmtree(p, ignore_errors=True)
        elif p.is_file():
            p.unlink(missing_ok=True)


def patch_build_gradle():
    gradle_p = ROOT / "app/build.gradle.kts"
    if not gradle_p.exists():
        return
    txt = gradle_p.read_text(encoding="utf-8")

    if (ROOT / "app/src/main/cpp/CMakeLists.txt").exists() and "src/main/cpp/CMakeLists.txt" not in txt:
        cmake_block = (
            '\n    externalNativeBuild {\n'
            '        cmake {\n'
            '            path = file("src/main/cpp/CMakeLists.txt")\n'
            '            version = "3.22.1"\n'
            '        }\n'
            '    }\n'
        )
        m = re.search(r"compileSdk\s*=\s*\d+", txt)
        if m:
            idx = m.end()
            txt = txt[:idx] + "\n" + cmake_block + txt[idx:]

    if "dev.rikka.shizuku" not in txt and "shizuku" not in txt:
        dep_match = re.search(r"dependencies\s*\{", txt)
        if dep_match:
            idx = dep_match.end()
            txt = (
                txt[:idx]
                + '\n    implementation("dev.rikka.shizuku:api:13.1.5")\n    implementation("dev.rikka.shizuku:provider:13.1.5")\n'
                + txt[idx:]
            )

    if "ExperimentalFoundationApi" not in txt:
        txt += (
            '\n\ntasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {\n'
            '    compilerOptions {\n'
            '        freeCompilerArgs.addAll(\n'
            '            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",\n'
            '            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",\n'
            '            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",\n'
            '        )\n'
            '    }\n'
            '}\n'
        )
    gradle_p.write_text(txt, encoding="utf-8")


def parse_kotlin_function_params(txt: str, fun_pos: int):
    open_paren = txt.find("(", fun_pos)
    if open_paren == -1:
        return [], "", -1, False

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
        return [], "", -1, False

    params_raw = txt[open_paren + 1 : close_paren]
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
        colon_idx = p.find(":")
        if colon_idx == -1:
            continue
        left = p[:colon_idx].strip()
        tokens = re.findall(r"[A-Za-z_][A-Za-z0-9_]*", left)
        if tokens:
            param_names.append(tokens[-1])

    body_brace = txt.find("{", close_paren)
    if body_brace == -1:
        return param_names, params_raw, -1, False
    between = txt[close_paren + 1 : body_brace].strip()
    has_non_unit_return = bool(between.startswith(":") and "Unit" not in between)
    return param_names, params_raw, body_brace, has_non_unit_return


def patch_island_view():
    iv_p = ROOT / "app/src/main/java/com/droidbubble/app/island/IslandView.kt"
    if not iv_p.exists():
        return
    txt = iv_p.read_text(encoding="utf-8")
    if "LiquidIslandDroidBubbleBridge" in txt:
        return

    target = None
    for m in re.finditer(r"@Composable\s+(?:public\s+|internal\s+)?fun\s+([A-Za-z0-9_]+)\s*\(", txt):
        fn_name = m.group(1)
        params, params_raw, brace_idx, has_return = parse_kotlin_function_params(txt, m.start())
        if brace_idx == -1 or has_return:
            continue
        if fn_name in ("IslandView", "IslandOverlay", "DynamicIslandView") or "DesignLanguage" in params_raw:
            target = (fn_name, params, brace_idx)
            break

    if target is None:
        print("===> No matching Island composable found in IslandView.kt (skipping)")
        return

    fn_name, params, brace_idx = target
    args_str = ", ".join(params)
    cond_args = f"androidx.compose.ui.platform.LocalContext.current, {args_str}" if args_str else "androidx.compose.ui.platform.LocalContext.current"
    injection = (
        f"\n    if (com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})) {{\n"
        f"        com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.RenderLiquidIslandOverlay({args_str})\n"
        f"        return\n"
        f"    }}\n"
    )
    txt = txt[: brace_idx + 1] + injection + txt[brace_idx + 1 :]
    iv_p.write_text(txt, encoding="utf-8")
    print(f"===> Hooked LiquidIslandDroidBubbleBridge into {fn_name} in IslandView.kt")


def patch_liquid_home_tab():
    lht_p = ROOT / "app/src/main/java/com/droidbubble/app/ui/liquid/LiquidHomeTab.kt"
    if not lht_p.exists():
        return
    txt = lht_p.read_text(encoding="utf-8")
    if "LiquidIslandDroidBubbleBridge" in txt:
        return

    composables = list(re.finditer(r"\bfun\s+([A-Za-z0-9_]+)\s*\(", txt))
    hooked_preview_fn = False
    for idx, cm in enumerate(composables):
        fn_name = cm.group(1)
        if fn_name == "LiquidHomeTab":
            continue
        params, _, brace_idx, has_return = parse_kotlin_function_params(txt, cm.start())
        if brace_idx == -1 or has_return:
            continue
        next_start = composables[idx + 1].start() if idx + 1 < len(composables) else len(txt)
        fn_body = txt[brace_idx:next_start]
        if "FastRewind" in fn_body and "FastForward" in fn_body:
            args_str = ", ".join(params)
            cond_args = f"androidx.compose.ui.platform.LocalContext.current, {args_str}" if args_str else "androidx.compose.ui.platform.LocalContext.current"
            inj = (
                f"\n    if (com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})) {{\n"
                f"        com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.RenderLiquidIslandHomePreview({args_str})\n"
                f"        return\n"
                f"    }}\n"
            )
            txt = txt[: brace_idx + 1] + inj + txt[brace_idx + 1 :]
            hooked_preview_fn = True
            print(f"===> Hooked LiquidIslandHomePreview into {fn_name} in LiquidHomeTab.kt")
            break

    m_home = re.search(r"\bfun\s+LiquidHomeTab\s*\(", txt)
    if m_home:
        params, _, brace_idx, has_return = parse_kotlin_function_params(txt, m_home.start())
        if brace_idx != -1 and not has_return and hooked_preview_fn:
            args_str = ", ".join(params)
            cond_args = f"androidx.compose.ui.platform.LocalContext.current, {args_str}" if args_str else "androidx.compose.ui.platform.LocalContext.current"
            inj = f"\n    com.xiao.androidisland.liquidisland.LiquidIslandDroidBubbleBridge.shouldRenderLiquidIsland({cond_args})\n"
            txt = txt[: brace_idx + 1] + inj + txt[brace_idx + 1 :]

    lht_p.write_text(txt, encoding="utf-8")
    print("===> Hooked LiquidIslandDroidBubbleBridge into LiquidHomeTab.kt")


def main():
    restore_original_project_if_needed()
    patch_build_gradle()
    patch_island_view()
    patch_liquid_home_tab()


if __name__ == "__main__":
    main()
