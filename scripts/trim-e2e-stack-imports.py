#!/usr/bin/env python3
"""Trim stack-readiness imports to only used symbols."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "frontend" / "e2e"
IMPORT_RE = re.compile(
    r"import \{ [^}]+\} from '\./helpers/stack-readiness'\n",
)


def symbols_in_use(text: str) -> list[str]:
    names = [
        "requireDockerStack",
        "requireBackendReady",
        "isDockerStackReady",
        "isBackendReady",
        "isFrontendReady",
        "skipUnlessDockerStackReady",
    ]
    return [n for n in names if re.search(rf"\b{n}\b", text)]


def fix_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if "stack-readiness" not in text:
        return False
    used = symbols_in_use(text)
    if not used:
        return False
    new_import = f"import {{ {', '.join(used)} }} from './helpers/stack-readiness'\n"
    new_text, count = IMPORT_RE.subn(new_import, text, count=1)
    if count == 0:
        return False
    if new_text != text:
        path.write_text(new_text, encoding="utf-8")
        return True
    return False


def main() -> None:
    n = 0
    for path in sorted(ROOT.rglob("*.ts")):
        if fix_file(path):
            n += 1
    print(f"fixed imports: {n}")


if __name__ == "__main__":
    main()
