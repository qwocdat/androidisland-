#!/bin/sh

# --- AUTO-INTEGRATE LIQUIDISLAND INTO DROIDBUBBLE ---
if [ -f "tools/integrate_liquidisland.py" ] && command -v python3 >/dev/null 2>&1; then
    python3 tools/integrate_liquidisland.py || true
    if [ -n "$GITHUB_ACTIONS" ] && command -v git >/dev/null 2>&1; then
        git add -A || true
        if ! git diff --cached --quiet; then
            git config --global user.name "github-actions[bot]" || true
            git config --global user.email "41898282+github-actions[bot]@users.noreply.github.com" || true
            git commit -m "chore: restore original Androidisland UI and hook LiquidIsland mode [skip ci]" || true
            BRANCH_NAME="${GITHUB_REF_NAME:-main}"
            git pull --rebase origin "$BRANCH_NAME" || true
            git push origin "HEAD:$BRANCH_NAME" || true
        fi
    fi
fi
# ----------------------------------------------------
exec gradle "$@"
