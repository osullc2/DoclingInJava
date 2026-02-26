# Step 2: Simple Python Library in Java (qrcode)

This step demonstrates using the Python [`qrcode`](https://pypi.org/project/qrcode/) library from a Java SE application via GraalPy, following the official GraalPy Java SE guide.

## What this project does

- Uses the GraalPy Maven artifacts and embedding API
- Installs the Python package `qrcode==7.4.2` via the `graalpy-maven-plugin`
- Creates a QR code image from Java by calling into Python
- Displays the generated QR code in a simple Swing window

## Build and run

From the repository root:

```bash
# 1) Compile the project and have the GraalPy Maven plugin install qrcode
mvn -f step2-simple-library/pom.xml compile

# 2) Run the Java app, pointing it at the generated python-resources directory
mvn -f step2-simple-library/pom.xml \
  exec:java \
  -Dexec.mainClass=org.example.App \
  -Dgraalpy.resources=./step2-simple-library/python-resources
```

You should see a desktop window with a QR code that encodes a greeting string containing your JVM version.

## Key files

- `step2-simple-library/pom.xml`
  - Adds:
    - `org.graalvm.polyglot:python` (meta POM)
    - `org.graalvm.python:python-embedding`
  - Configures `org.graalvm.python:graalpy-maven-plugin` to install `qrcode` into `python-resources/`
- `src/main/java/org/example/GraalPy.java`
  - Helper to create a GraalPy `Context` from an external `python-resources` directory
- `src/main/java/org/example/QRCode.java` and `IO.java`
  - Java interfaces that mirror the Python `qrcode` and `io` APIs used
- `src/main/java/org/example/App.java`
  - Main class that:
    - Reads the `graalpy.resources` system property
    - Creates a Python context via `GraalPy`
    - Uses `qrcode` to generate a PNG in memory
    - Shows the QR image in a Swing `JFrame`

