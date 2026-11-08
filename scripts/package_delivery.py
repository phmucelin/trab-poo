#!/usr/bin/env python3
"""Empacota o projeto Eclipse, sem Git, classes compiladas ou arquivos temporários."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parent.parent
sources = [".project", ".classpath", ".settings", "src", "test", "imagens", "lib",
           "docs", "scripts", "README.md"]
missing = [name for name in sources if not (root / name).exists()]
if missing:
    raise SystemExit("Arquivos ausentes: " + ", ".join(missing))
output = root / "output" / "entrega" / "HerdeirosDeKhan-iteracao3.zip"
output.parent.mkdir(parents=True, exist_ok=True)
with ZipFile(output, "w", ZIP_DEFLATED) as archive:
    for name in sources:
        path = root / name
        files = sorted(path.rglob("*")) if path.is_dir() else [path]
        for file in files:
            if file.is_file() and file.name != ".DS_Store" and "__pycache__" not in file.parts:
                archive.write(file, Path("HerdeirosDeKhan") / file.relative_to(root))
with ZipFile(output) as archive:
    if archive.testzip() is not None:
        raise SystemExit("Falha na integridade do ZIP.")
print(output)
