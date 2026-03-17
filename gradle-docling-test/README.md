# Gradle Docling Test (GraalVM)

This is a minimal Gradle-based Java project that embeds GraalPy and runs a tiny Docling smoke test.

## What it tests

- Creates a local GraalPy venv under `python-resources/venv`
- Installs `docling` **without dependencies** (`pip install --no-deps docling`) to avoid native builds
- Runs Java and evaluates Python: `import docling; docling.__version__`

## Requirements

- GraalVM JDK (Java 21) with GraalPy available as `graalpy` / `graalpy.exe`
- `JAVA_HOME` set to that GraalVM installation

## Run

From the repo root (PowerShell):

```powershell
cd "gradle-docling-test"
.\gradlew run
```

Expected output:

```text
docling version: <some version>
```

## Notes

- This project intentionally uses `--no-deps` for `docling` so we can validate “Docling is loadable” without pulling in heavy native dependencies (e.g. NumPy toolchains on Windows).
- Next step (once this is stable) is to selectively add the minimal dependencies needed for a real conversion call.

