# Docling in Java — GraalVM / GraalPy Sandbox

This repository collects **Maven** and **Gradle** experiments for calling **Python from Java** with **GraalVM / GraalPy**, oriented toward eventually running **IBM Docling** for document conversion. The [Project catalog](#project-catalog) below describes **each subproject**, what it proves, difficulties encountered, and next steps where work is incomplete.

## Table of contents

- [Repository map](#repository-map)
- [Project catalog](#project-catalog)
  - [1. Root Maven module (Step 1)](#1-root-maven-module-step-1)
  - [2. `step2-simple-library`](#2-step2-simple-library)
  - [3. `step3-docling-library`](#3-step3-docling-library)
  - [4. `gradle-docling-test`](#4-gradle-docling-test)
  - [5. `graal-springboot-analysis`](#5-graal-springboot-analysis)
- [Appendix: Step 1 setup tutorial](#appendix-step-1-setup-tutorial-hands-on)

## Repository map

| Location | Build tool | Role |
|----------|------------|------|
| Repository root (`pom.xml`, `src/`) | Maven | Minimal polyglot “Hello World” (Step 1). |
| `step2-simple-library/` | Maven + `graalpy-maven-plugin` | GraalPy + PyPI (`qrcode`) embedding example. |
| `step3-docling-library/` | Maven + `graalpy-maven-plugin` | Docling install + import smoke test (GraalPy 25.x style). |
| `gradle-docling-test/` | Gradle | External GraalPy venv; Docling smoke / optional conversion harness. |
| `graal-springboot-analysis/` | Gradle + `org.graalvm.python` | Research notes + `DoclingDemo` using `python-resources`. |
| `WrapperTest/` | Maven + GraalPy plugin | `DoclingWrapper` pointed at a host venv path. |
| `target/` | Maven output | Build directory for the **root** module only (gitignored). |

## Project catalog

### 1. Root Maven module (Step 1)

**Purpose.** Show that a standard Maven Java app can open a GraalVM polyglot `Context`, enable Python, and run `print('Hello World')` without any extra Python packages.

**Layout.** Root `pom.xml` (artifact `graalvm-python`); sources under `src/main/java/com/example/` (`PythonRunner`).

**How to run.**

```bash
mvn clean compile
mvn dependency:copy-dependencies
# Windows PowerShell:
$env:CLASSPATH="target\classes;target\dependency\*"
java com.example.PythonRunner
```

**Dependencies / versions.** The root POM pins **GraalVM 23.1.0** artifacts (`graal-sdk`, `polyglot`, `python-language`, `python-resources`). Sibling folders use **24.x / 25.x** coordinates; a short version matrix appears under **Final notes** near the end of this README.

**Difficulties and blockers.**

- **`mvn exec:java`:** Documented below as **unreliable** in this setup; prefer `dependency:copy-dependencies` plus explicit `java` with `CLASSPATH`.
- **Wrong Maven coordinates:** `org.graalvm.polyglot:python` is not the same as pulling full GraalPy support; this module needs `org.graalvm.python:python-language` and `python-resources`.
- **“No language implementation found”:** Appears if Python language JARs are missing or versions disagree with the runtime JDK.
- **Windows paths with spaces (e.g. OneDrive):** `InvalidPathException` or engine init failures when paths are rewritten oddly; explicit classpath and stable working directories help.
- **POM version vs installed GraalVM:** Mismatch causes confusing resolution or runtime errors.

**Status.** **Complete** for hello-world polyglot. Does **not** exercise PyPI, Docling, or the GraalPy Maven/Gradle plugins.

### 2. `step2-simple-library`

**Purpose.** Demonstrate the **official GraalPy-on-Java** pattern with a real PyPI package: **`qrcode==7.4.2`**, installed by **`graalpy-maven-plugin`** into `python-resources/`, then called from Java (with a small Swing UI). This is the first subproject that uses **managed Python resources** instead of only the bare polyglot engine.

**Key files.** `step2-simple-library/pom.xml` (GraalPy 25.0.2 meta POM + `python-embedding` + plugin); Java under `step2-simple-library/src/main/java/org/example/` (`App`, `GraalPy`, `QRCode`, etc.—see that folder’s README).

**How to run (summary).**

```bash
mvn -f step2-simple-library/pom.xml compile
mvn -f step2-simple-library/pom.xml exec:java -Dexec.mainClass=org.example.App -Dgraalpy.resources=./step2-simple-library/python-resources
```

**Difficulties and blockers.**

- **Generated `python-resources/` is gitignored** (see root `.gitignore`): a fresh clone must run a Maven phase that triggers `process-graalpy-resources` before the app can start.
- **`-Dgraalpy.resources` path:** Must point at the directory the plugin populated; wrong or relative-path mistakes show up as import errors in Python.
- **First-time pip downloads:** Needs network access to PyPI during the GraalPy plugin run.
- **Swing / desktop:** Headless CI cannot display the window without an X server or equivalent (local dev expectation).

**Status.** **Working** as a teaching step for “PyPI + GraalPy Maven plugin + external resources directory.” Not related to Docling.

### 3. `step3-docling-library`

**Purpose.** Use the same **GraalPy Maven plugin + external `python-resources/`** pattern as Step 2, but install the **`docling`** PyPI package (full install with dependencies, not `--no-deps`). Java code verifies that **`import docling`** succeeds via `GraalPyResources.contextBuilder(Path)`—the intended “Step 3” on the path to document conversion.

**Key files.** `step3-docling-library/pom.xml` (GraalPy **25.0.2**, `graalpy-maven-plugin` with `<package>docling</package>`); `org.example.GraalPy` and `org.example.App` under `src/main/java/`.

**How to run (summary).**

```bash
mvn -f step3-docling-library/pom.xml compile
mvn -f step3-docling-library/pom.xml exec:java -Dexec.mainClass=org.example.App -Dgraalpy.resources=./step3-docling-library/python-resources
```

**Difficulties and blockers.**

- **Heavy dependency graph:** Full `docling` pulls native-heavy stacks (e.g. **`docling-parse`**) and large transitive deps. Builds can be **slow**, require **strong network** access to PyPI (and sometimes source/patch URLs), and may try to **compile** wheels where no GraalPy-compatible prebuilt wheel exists.
- **Windows + GraalPy wheels:** Unlike CPython, GraalPy needs **GraalPy-built** wheels for many extensions; missing wheels trigger local compiles or failures. The LAFO mirror used in other demos may be required for packages like **NumPy** on some platforms—this POM does not yet add that mirror by default.
- **`python-resources/` is gitignored:** Fresh clones must run a build that executes `process-graalpy-resources` before runtime.
- **README drift:** The folder `README.md` still reads like a placeholder in places; the code and POM have moved ahead to a real import smoke test—documentation there should be aligned.

**Status.** **Partially complete.** Import-only smoke test is implemented; **end-to-end conversion** (e.g. `DocumentConverter` on a sample PDF), **tutorial polish**, and **reliable reproducible installs** (mirrors, pinned versions, documented disk/time) are still open.

**Next steps.** Add **`--extra-index-url`** / LAFO-style configuration if Windows builds keep compiling NumPy from source; add a **minimal conversion example** and expected output; update `step3-docling-library/README.md` to match current behavior; consider pinning versions to match a known-good GraalPy wheel set.

### 4. `gradle-docling-test`

**Purpose.** A **Gradle** counterpoint to the Maven plugin flow: create a **real GraalPy venv** under `python-resources/venv` using `graalpy -m venv`, run **`pip install --no-deps docling`**, then run Java with **`python-embedding` 25.0.2**. The `run` task sets **`DOCLING_VENV`** so `com.example.docling.App` can point polyglot **`python.Executable`** at `venv\Scripts\graalpy.exe` (Windows) or `venv/bin/graalpy`. `App.java` runs a JSON-style smoke script: import `docling`, construct **`DocumentConverter`**, and optionally **`--input`** a file for markdown/text previews.

**How to run.**

```powershell
cd gradle-docling-test
.\gradlew run
# Optional: .\gradlew run --args="--input C:\path\to\file.pdf --mode both"
```

**Requirements.** `JAVA_HOME` must be a **GraalVM JDK** that ships **`graalpy`** next to `java` (the build checks this).

**Difficulties and blockers.**

- **`--no-deps` tradeoff:** Avoids pulling NumPy/toolchains during install but yields an **incomplete** environment; **imports may succeed** while **`convert()` fails** at runtime depending on what Docling actually needs loaded.
- **Platform variance:** Windows vs Unix paths for the venv executable must stay consistent (`Scripts` vs `bin`).
- **Full Docling closure:** Moving from “loadable” to “convert real documents” likely requires **either** removing `--no-deps` and solving native wheels **or** curating explicit extra packages—same GraalPy wheel story as Step 3.

**Status.** **Partially complete** as a **Gradle + external venv + polyglot options** harness; **reliable conversion** on Windows without a curated wheel/index strategy is still open.

**Next steps.** Decide on **full install vs minimal curated deps**; add LAFO / GraalPy wheel index if needed; document expected JSON output fields and a **known-good sample PDF**; consider a small CI recipe (or document why CI is skipped).

### 5. `graal-springboot-analysis`

**Purpose.** Two things live here: (1) **`README.md`** — long-form **research notes** comparing Fabio Niephaus’s **Spring Boot** and **Quarkus** GraalPy demos, how **`org.graalvm.python` Gradle** bundles a venv into a VFS, why **Docling** is harder than **MarkItDown** (notably **`docling-parse`** and GraalPy wheel availability), and a **staged plan** (e.g. `docling-core` smoke vs full `docling`). (2) A **Gradle `application`** — `DoclingDemo` loads **`GraalPyContextManager`** (`GraalPyResources.contextBuilder` over **`python-resources`**), uses **`DocumentConverter`** Java glue to call Python **`DocumentConverter.convert`** and print the first ~500 characters of Markdown from a sample URL (e.g. an arXiv PDF).

**Build.** `build.gradle` uses **`org.graalvm.python` 25.0.1**, **`graalPy { externalDirectory ... packages = [...] }`**, JVM args for native access / stack, and may reference a **LAFO extra index** for GraalPy wheels (see the folder README and `build.gradle` for the exact package list).

**Difficulties and blockers.**

- **Name vs contents:** The directory name suggests “Spring Boot,” but the demo is **not** a Spring application—it is a small **Java `main`** plus research notes.
- **Package list vs Python imports:** The Gradle file has targeted **`docling-core`** for staged installs, while **`DocumentConverter.java`** imports **`from docling.document_converter import DocumentConverter`** (the **full Docling** API). Those need to stay **aligned**: either install the **`docling`** meta-package and dependencies, or change Python to use **`docling_core`**-only APIs for a true “core-only” stage.
- **Wheels and network:** Folder notes describe **`graalPyInstallPackages`** failing when **NumPy** tried to **compile from source** and could not reach **PyPI / raw GitHub** (proxy/firewall). Adding the **LAFO mirror** (as in the upstream Spring demo) is the documented mitigation when standard indexes lack a Windows GraalPy wheel.
- **`docling-parse`:** No GraalPy prebuilt wheel on the usual indexes; **compatibility with GraalPy** remains an open research question.

**Status.** **Research + prototype:** documentation is rich; the runnable demo depends on a **successful venv populate** and consistent **`docling` vs `docling-core`** wiring.

**Next steps.** Reconcile **Gradle package pins** with **Python import paths**; ensure **LAFO / extra index** is set whenever NumPy-style deps would otherwise compile; re-run the **staged plan** (core import → full Docling) and record **Windows vs Linux** outcomes; optionally rename the folder or add a one-line clarification in its README to avoid “Spring Boot” confusion.

---

## Appendix: Step 1 setup tutorial (hands-on)

Step-by-command walkthrough for the root Maven module (requirements, `pom.xml`, `PythonRunner`, classpath run, and troubleshooting).

## Requirements

- Java 21 (GraalVM JDK recommended)
- Maven 3.x

## Step-by-Step Setup from Empty Repository

### Step 1: Verify Prerequisites

First, check if Maven and GraalVM are installed:

```bash
# Check Maven version
mvn --version

# Check Java version (should show GraalVM)
java -version
```

**Expected output for Maven:**
```
Apache Maven 3.x.x
Java version: 21.x.x, vendor: Oracle Corporation, runtime: ...graalvm-jdk...
```

**Expected output for Java:**
```
java version "21.x.x" ... Oracle GraalVM ...
```

If GraalVM is not installed, download it from [GraalVM Downloads](https://graalvm.org/downloads/).

### Step 2: Create Maven Project Structure

Create the directory structure manually:

```bash
# Windows PowerShell
New-Item -ItemType Directory -Force -Path "src\main\java\com\example", "src\test\java\com\example"

# Linux/Mac
mkdir -p src/main/java/com/example src/test/java/com/example
```

### Step 3: Create pom.xml

Create a `pom.xml` file in the root directory with the following content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>graalvm-python</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>GraalVM Python Runner</name>
    <description>A simple Maven project that runs Python code using GraalVM polyglot</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- GraalVM Polyglot API - included with GraalVM -->
        <dependency>
            <groupId>org.graalvm.sdk</groupId>
            <artifactId>graal-sdk</artifactId>
            <version>23.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.graalvm.polyglot</groupId>
            <artifactId>polyglot</artifactId>
            <version>23.1.0</version>
        </dependency>
        <!-- GraalVM Python language support -->
        <dependency>
            <groupId>org.graalvm.python</groupId>
            <artifactId>python-language</artifactId>
            <version>23.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.graalvm.python</groupId>
            <artifactId>python-resources</artifactId>
            <version>23.1.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.example.PythonRunner</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Important Notes:**
- The version numbers (23.1.0) should match your GraalVM version. Check with `java -version` to see your GraalVM version.
- The Python dependencies use `org.graalvm.python` groupId, NOT `org.graalvm.polyglot`.

### Step 4: Create Java Source File

Create `src/main/java/com/example/PythonRunner.java`:

```java
package com.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class PythonRunner {
    public static void main(String[] args) {
        // Create a polyglot context that allows Python
        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build()) {
            
            // Execute Python code
            context.eval("python", "print('Hello World')");
        }
    }
}
```

### Step 5: Build the Project

```bash
mvn clean compile
```

This will download all dependencies (which may take a few minutes on first run, especially the Python language JARs which are ~200MB total).

### Step 6: Run the Program

**Option 1: Using Maven exec plugin**
```bash
mvn exec:java
```
**THIS DID NOT WORK FOR ME!!**


**Option 2: Using Java directly**
```bash
# First, copy all dependencies
mvn dependency:copy-dependencies

# Then run with proper classpath
# Windows PowerShell:
$env:CLASSPATH="target\classes;target\dependency\*"
java com.example.PythonRunner

# Windows CMD:
set CLASSPATH=target\classes;target\dependency\*
java com.example.PythonRunner

# Linux/Mac:
java -cp "target/classes:target/dependency/*" com.example.PythonRunner
```

**Expected Output:**
```
[To redirect Truffle log output to a file use one of the following options:...]
[engine] WARNING: The polyglot engine uses a fallback runtime...
Hello World
```

The warning about the runtime is informational and doesn't affect functionality - it just means the code runs in interpreter mode rather than with JIT compilation.

## Issues Encountered and Solutions

### Issue 1: PowerShell Command Syntax
**Problem:** Using `&&` to chain commands in PowerShell doesn't work (that's bash syntax).

**Error:**
```
The token '&&' is not a valid statement separator in this version.
```

**Solution:** Use semicolons (`;`) instead of `&&` in PowerShell:
```powershell
cd "path"; mvn command
```

### Issue 2: Missing Python Language Dependency
**Problem:** Initially tried using `org.graalvm.polyglot:python` which doesn't exist in Maven Central.

**Error:**
```
Could not find artifact org.graalvm.polyglot:python:jar:23.1.0
```

**Solution:** Use the correct groupId and artifactIds:
- `org.graalvm.python:python-language` (not `org.graalvm.polyglot:python`)
- `org.graalvm.python:python-resources` (also required)

### Issue 3: No Language Implementation Found
**Problem:** After adding polyglot dependencies but before adding Python language support, the program couldn't find any language implementation.

**Error:**
```
No language and polyglot implementation was found on the class-path.
```

**Solution:** Ensure both `python-language` and `python-resources` dependencies are included in `pom.xml`.

### Issue 4: Path Issues with Spaces (Windows)
**Problem:** When using Maven exec plugin with paths containing spaces (like OneDrive paths), GraalVM polyglot engine initialization can fail.

**Error:**
```
java.nio.file.InvalidPathException: Illegal char <:> at index 2: /C:/Users/...
```

**Solution:** 
- Use the direct Java approach with proper classpath instead of Maven exec plugin
- Or ensure paths don't have spaces (not always possible with OneDrive)
- The direct Java method with `dependency:copy-dependencies` works reliably

### Issue 5: ClassNotFoundException
**Problem:** Running Java directly without proper classpath setup.

**Error:**
```
java.lang.NoClassDefFoundError: org/graalvm/polyglot/Context
```

**Solution:** 
1. Run `mvn dependency:copy-dependencies` first to copy all JARs to `target/dependency/`
2. Include both `target/classes` and `target/dependency/*` in the classpath
3. Use proper path separators: `;` on Windows, `:` on Linux/Mac

### Issue 6: Runtime Compilation Warning
**Problem:** Warning about fallback runtime and no optimizing Truffle runtime.

**Warning:**
```
[engine] WARNING: The polyglot engine uses a fallback runtime that does not support runtime compilation to native code.
```

**Solution:** This is informational only. The code works correctly but runs in interpreter mode. To suppress:
- Add `-Dpolyglot.engine.WarnInterpreterOnly=false` as a JVM argument
- Or use `--engine.WarnInterpreterOnly=false` if using a guest language launcher

## Troubleshooting

### Check GraalVM Version
If you get dependency resolution errors, ensure the version in `pom.xml` matches your GraalVM version:

```bash
java -version
# Look for the version number, e.g., "21.0.10+8.1"
# Use the major.minor version (e.g., 23.1.0) in pom.xml
```

### Verify Dependencies Downloaded
Check that dependencies were downloaded:
```bash
ls ~/.m2/repository/org/graalvm/python/  # Linux/Mac
dir %USERPROFILE%\.m2\repository\org\graalvm\python\  # Windows
```

### Clean and Rebuild
If you encounter strange errors, try a clean rebuild:
```bash
mvn clean
mvn dependency:purge-local-repository
mvn compile
```

### Check Classpath
When running directly with Java, verify the classpath includes all dependencies:
```bash
# Windows PowerShell - check if files exist
Test-Path target\classes\com\example\PythonRunner.class
Test-Path target\dependency\polyglot-*.jar
Test-Path target\dependency\python-language-*.jar
```

## What it does

The `PythonRunner` class uses GraalVM's polyglot API to create a context that supports Python, then executes the Python code `print('Hello World')`.

## Project Structure

```
.
├── pom.xml                          # Maven configuration
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── PythonRunner.java
└── README.md
```

## Dependencies

- `org.graalvm.sdk:graal-sdk` - GraalVM SDK
- `org.graalvm.polyglot:polyglot` - Polyglot API
- `org.graalvm.python:python-language` - Python language support
- `org.graalvm.python:python-resources` - Python resources

## Additional Resources

- [GraalVM Documentation](https://www.graalvm.org/latest/docs/)
- [GraalVM Python (GraalPy) Guide](https://www.graalvm.org/jdk22/reference-manual/python/)
- [Embedding Languages](https://www.graalvm.org/latest/reference-manual/embed-languages/)
