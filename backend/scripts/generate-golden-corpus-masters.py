#!/usr/bin/env python3
"""Generate minimal OOXML master.docx fixtures for CE-K07 golden corpus."""
from __future__ import annotations

import zipfile
from pathlib import Path

CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>
"""

RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
"""

DOC_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>
"""


def document_xml(body_text: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:t>{body_text}</w:t></w:r></w:p>
    <w:sectPr/>
  </w:body>
</w:document>
"""


def write_docx(path: Path, body_text: str = "{{anchor:BODY}}") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", CONTENT_TYPES)
        zf.writestr("_rels/.rels", RELS)
        zf.writestr("word/document.xml", document_xml(body_text))
        zf.writestr("word/_rels/document.xml.rels", DOC_RELS)
    print(f"wrote {path} ({path.stat().st_size} bytes)")


def main() -> None:
    root = Path(__file__).resolve().parents[1] / "src" / "test" / "resources" / "golden-corpus"
    packages = [
        "01-dual-font-master",
        "02-cross-page-table",
        "03-nested-clauses",
        "04-compute-variables",
        "05-chinese-uppercase-amount",
        "06-specimen-watermark",
        "07-encrypted-pdf",
        "08-long-clause-limits",
    ]
    for name in packages:
        write_docx(root / name / "input" / "master.docx")
    print("done")


if __name__ == "__main__":
    main()
