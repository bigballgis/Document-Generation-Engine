#!/usr/bin/env python3
"""CQ-08: migrate all E2E specs to stack-readiness helper."""
from __future__ import annotations

import re
from pathlib import Path

E2E_DIR = Path(__file__).resolve().parents[1] / "frontend" / "e2e"
IMPORT = "import { isDockerStackReady, requireDockerStack } from './helpers/stack-readiness'"

# Full healthcheck + test.skip block (beforeAll or standalone)
SKIP_BLOCK = re.compile(
    r"let backendReady = false\s*"
    r"let frontendReady = false\s*"
    r"try \{\s*"
    r"(?:const backend = await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\s*"
    r"backendReady = backend\.ok\(\)\s*"
    r"|backendReady = \(await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\)\.ok\(\)\s*)"
    r"\} catch \{\s*backendReady = false\s*\}\s*"
    r"try \{\s*"
    r"(?:const frontend = await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\s*"
    r"frontendReady = frontend\.ok\(\)\s*"
    r"|frontendReady = \(await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\)\.ok\(\)\s*)"
    r"\} catch \{\s*frontendReady = false\s*\}\s*"
    r"test\.skip\(\s*!\(backendReady && frontendReady\),\s*`([^`]+)`,\s*\)",
    re.MULTILINE,
)

ASSERT_STACK_FN = re.compile(
    r"async function assertStackReady\(request: APIRequestContext\) \{\s*"
    r"let backendReady = false\s*let frontendReady = false\s*"
    r"try \{\s*backendReady = \(await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\)\.ok\(\)\s*"
    r"\} catch \{\s*backendReady = false\s*\}\s*"
    r"try \{\s*frontendReady = \(await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\)\.ok\(\)\s*"
    r"\} catch \{\s*frontendReady = false\s*\}\s*"
    r"test\.skip\(\s*!\(backendReady && frontendReady\),\s*`([^`]+)`,\s*\)\s*\}",
    re.MULTILINE,
)

STACK_READY_BOOL = re.compile(
    r"async function stackReady\(request: APIRequestContext\): Promise<boolean> \{\s*"
    r"try \{\s*"
    r"const backend = await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\s*"
    r"const frontend = await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\s*"
    r"return backend\.ok\(\) && frontend\.ok\(\)\s*"
    r"\} catch \{\s*return false\s*\}\s*\}",
    re.MULTILINE,
)

ASSERT_DOCKER_BOOL = re.compile(
    r"async function assertDockerStackReady\(request: APIRequestContext\): Promise<boolean> \{\s*"
    r"let backendReady = false\s*let frontendReady = false\s*"
    r"try \{\s*const backend = await request\.get\('http://127\.0\.0\.1:8080/healthz', \{ timeout: 5_000 \}\)\s*"
    r"backendReady = backend\.ok\(\)\s*\} catch \{\s*backendReady = false\s*\}\s*"
    r"try \{\s*const frontend = await request\.get\(([^,\n]+), \{ timeout: 5_000 \}\)\s*"
    r"frontendReady = frontend\.ok\(\)\s*\} catch \{\s*frontendReady = false\s*\}\s*"
    r"return backendReady && frontendReady\s*\}",
    re.MULTILINE,
)


def ensure_import(text: str) -> str:
    if "stack-readiness" in text:
        return text
    return text.replace(
        "from '@playwright/test'\n",
        f"from '@playwright/test'\n\n{IMPORT}\n",
        1,
    )


def migrate(text: str) -> str:
    if "127.0.0.1:8080/healthz" not in text:
        return text
    text = ensure_import(text)

    text = ASSERT_STACK_FN.sub(
        lambda m: (
            "async function assertStackReady(request: APIRequestContext) {\n"
            + f"  await requireDockerStack(request, {{ frontendBaseUrl: {m.group(1)}, "
            + f"skipMessage: `{m.group(2)}` }})\n"
            + "}"
        ),
        text,
    )

    text = STACK_READY_BOOL.sub(
        lambda m: (
            "async function stackReady(request: APIRequestContext): Promise<boolean> {\n"
            + f"  return isDockerStackReady(request, {{ frontendBaseUrl: {m.group(1)} }})\n"
            + "}"
        ),
        text,
    )

    text = ASSERT_DOCKER_BOOL.sub(
        lambda m: (
            "async function assertDockerStackReady(request: APIRequestContext): Promise<boolean> {\n"
            + f"  return isDockerStackReady(request, {{ frontendBaseUrl: {m.group(1)} }})\n"
            + "}"
        ),
        text,
    )

    def skip_repl(m: re.Match[str]) -> str:
        frontend = m.group(1) or m.group(2)
        msg = m.group(3)
        return (
            f"await requireDockerStack(request, {{ frontendBaseUrl: {frontend}, "
            f"skipMessage: `{msg}` }})"
        )

    text = SKIP_BLOCK.sub(skip_repl, text)
    return text


def main() -> None:
    n = 0
    for path in sorted(E2E_DIR.rglob("*.spec.ts")):
        original = path.read_text(encoding="utf-8")
        updated = migrate(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8", newline="\n")
            n += 1
            print(path.relative_to(E2E_DIR))
    # helpers
    helper = E2E_DIR / "helpers" / "core-fortress-f7.ts"
    if helper.exists():
        original = helper.read_text(encoding="utf-8")
        updated = migrate(original)
        if updated != original:
            helper.write_text(updated, encoding="utf-8", newline="\n")
            n += 1
            print("helpers/core-fortress-f7.ts")
    print(f"migrated: {n}")


if __name__ == "__main__":
    main()
