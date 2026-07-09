#!/usr/bin/env python3
"""Repair partial CQ-08 E2E migrations."""
from __future__ import annotations

import re
from pathlib import Path

E2E_DIR = Path(__file__).resolve().parents[1] / "frontend" / "e2e"

BROKEN_TAIL = re.compile(
    r"\s*await requireDockerStack\(request, \{ frontendBaseUrl: None, skipMessage: `None` \}\)\} catch \{[^}]*\}\s*"
    r"(?:try \{[^}]*\} catch \{[^}]*\}\s*)*"
    r"test\.skip\(\s*!\(backendReady && frontendReady\),\s*`([^`]+)`,\s*\)",
    re.MULTILINE | re.DOTALL,
)

OLD_BLOCK = re.compile(
    r"\s*let backendReady = false\s*"
    r"let frontendReady = false\s*"
    r"try \{\s*"
    r"(?:const backend = await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\s*"
    r"backendReady = backend\.ok\(\)\s*"
    r"|backendReady = \(await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\)\.ok\(\)\s*)"
    r"\} catch \{\s*"
    r"backendReady = false\s*"
    r"\}\s*"
    r"try \{\s*"
    r"(?:const frontend = await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\s*"
    r"frontendReady = frontend\.ok\(\)\s*"
    r"|frontendReady = \(await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\)\.ok\(\)\s*)"
    r"\} catch \{\s*"
    r"frontendReady = false\s*"
    r"\}\s*"
    r"test\.skip\(\s*"
    r"!\(backendReady && frontendReady\),\s*"
    r"`([^`]+)`,\s*"
    r"\)",
    re.MULTILINE,
)

IMPORT = "import { isDockerStackReady, requireDockerStack } from './helpers/stack-readiness'"


def ensure_import(text: str) -> str:
    if "stack-readiness" in text:
        return text
    return text.replace(
        "from '@playwright/test'\n",
        f"from '@playwright/test'\n\n{IMPORT}\n",
        1,
    )


def repair(text: str) -> str:
    text = ensure_import(text)

    def broken_repl(m: re.Match[str]) -> str:
        msg = m.group(1)
        return f"\n    await requireDockerStack(request, {{ frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `{msg}` }})"

    text = BROKEN_TAIL.sub(broken_repl, text)

    def old_repl(m: re.Match[str]) -> str:
        frontend = m.group(1) or m.group(2)
        msg = m.group(3)
        return (
            f"\n    await requireDockerStack(request, {{ "
            f"frontendBaseUrl: {frontend}, skipMessage: `{msg}` }})"
        )

    text = OLD_BLOCK.sub(old_repl, text)
    return text


def main() -> None:
    n = 0
    for path in sorted(E2E_DIR.rglob("*.spec.ts")):
        original = path.read_text(encoding="utf-8")
        fixed = repair(original)
        if fixed != original:
            path.write_text(fixed, encoding="utf-8", newline="\n")
            n += 1
            print(path.name)
    print(f"repaired: {n}")


if __name__ == "__main__":
    main()
