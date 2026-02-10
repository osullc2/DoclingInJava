# GraalVM Python Runner

A simple Maven project that demonstrates running Python code from Java using GraalVM's polyglot capabilities.

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
