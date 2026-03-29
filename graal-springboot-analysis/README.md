
GRAALPY + DOCLING RESEARCH NOTES
=================================
Last updated: March 2026


OVERVIEW
--------
This document summarizes research into embedding Python libraries inside JVM
applications using GraalPy, with the end goal of embedding Docling -- an IBM
Research document understanding library -- into a Java application. We examined
two existing reference implementations, analyzed why Docling presents unique
challenges compared to what those demos use, and documented the strategy we
decided to pursue.


PART 1: REFERENCE IMPLEMENTATIONS EXAMINED
-------------------------------------------

We examined two repos by Fabio Niephaus (GraalVM team, Oracle Labs):

  1. graalpy-spring-boot-summarize
     https://github.com/fniephaus/graalpy-spring-boot-summarize

  2. graalpy-quarkus-summarize
     https://github.com/fniephaus/graalpy-quarkus-summarize

Both demos embed two Python libraries into a Java web application via GraalPy:

  - MarkItDown (Microsoft) -- converts files like PDFs, Word docs, spreadsheets
    to plain text/Markdown
  - Hugging Face Transformers -- runs an NLP model to summarize that text

They expose three HTTP endpoints: /hello (sanity check), /convert (file to
text), and /summarize (file to summary). Both use the org.graalvm.python Gradle
plugin (version 25.0.0/25.0.1) and the same graalPy { packages.set(...) } block
to declare Python dependencies.

KEY DIFFERENCES BETWEEN THE TWO REPOS:

  - Spring Boot repo: written in Java, targets Java 25, uses Spring WebMVC,
    has a separate native image plugin (org.graalvm.buildtools.native), and
    includes a custom --extra-index-url pointing to a LAFO mirror for prebuilt
    GraalPy wheels (more on this below).

  - Quarkus repo: written in Kotlin, targets Java 21, uses JAX-RS/RESTEasy,
    has native image support built into the io.quarkus plugin, and includes a
    graalpy.lock file for reproducible Python dependency resolution. The LAFO
    mirror is absent because those wheels were still pending at time of commit.

  - The Python/GraalPy side is IDENTICAL between the two -- same plugin, same
    version, same 40+ pinned packages. The only thing that changes is the Java
    web framework wrapping it, which is exactly the point these demos make.

HOW GRAALPY IS WIRED UP (applies to both repos):

  At build time, the org.graalvm.python Gradle plugin:
    - Injects GraalPy JAR dependencies onto the classpath automatically
    - Runs pip install for each listed package into an embedded Python venv
    - Bundles that venv into the JAR as a Virtual Filesystem (VFS)

  At runtime, the app uses GraalVM's Polyglot API:

    try (Context context = GraalPyResources.contextBuilder().build()) {
        context.eval("python", "from markitdown import MarkItDown");
    }

  GraalPyResources.contextBuilder() wires the context to the embedded VFS so
  Python's import system can find all bundled packages. The Value API is used
  to pass data between Java and Python -- calling Python functions, getting
  return values, etc.


PART 2: WHY DOCLING IS DIFFERENT
---------------------------------

At first glance MarkItDown and Docling seem similar -- both are document
processing libraries. But the demos work not because MarkItDown is pure Python
(it isn't -- its [all] dependencies include pillow, lxml, onnxruntime, torch,
etc.), but because every native dependency those demos need already has a
PREBUILT GraalPy-compatible binary wheel available.

The key difference comes down to one specific package: docling-parse.

MarkItDown's native dependencies (torch, onnxruntime, numpy, pillow, etc.) are
all high-profile packages with large userbases. The GraalPy team and the LAFO
research group have already done the work of building and publishing GraalPy-
compatible wheels for them.

docling-parse is a C++ PDF parsing library created by IBM Research. It is
relatively new and niche. Nobody has built a GraalPy-compatible wheel for it.


PART 3: THE WHEELS SITUATION
-----------------------------

What is a wheel?
  A Python wheel (.whl) is a prebuilt binary package. When you pip install a
  package that has C/C++/Rust extensions, pip either downloads a prebuilt wheel
  for your platform or compiles the extension from source. Prebuilt wheels are
  faster and more reliable; source builds require the right compiler toolchain
  and can fail.

The problem with GraalPy and native extensions:
  GraalPy is not CPython. Its internal ABI is different. A wheel built for
  CPython (the standard Python) is not binary compatible with GraalPy. Packages
  with native extensions must be built specifically against GraalPy's headers,
  or GraalPy must apply compatibility patches and compile them itself.

Where GraalPy wheels come from:

  1. Official GraalPy wheels index:
     https://www.graalvm.org/python/wheels/
     A small curated list of packages with confirmed GraalPy-compatible wheels:
     numpy, onnxruntime, psutil, pydantic-core, torch (via pytorch), ujson,
     xxhash, and a handful of others.

  2. LAFO mirror (used in the Spring Boot demo):
     https://lafo.ssw.uni-linz.ac.at/pub/demowheels/simple/
     A custom PyPI mirror run by Fabio Niephaus's research group at JKU Linz.
     This is what makes the Spring Boot demo work on all platforms -- it hosts
     prebuilt GraalPy wheels for the exact package versions pinned in the demo
     (torch 2.7.0, onnxruntime 1.17.1, numpy 2.2.6, etc.). Without this mirror,
     those packages would need to compile from source.

  3. GraalPy auto-patching:
     For some packages, GraalPy's pip will automatically download compatibility
     patches from github.com/oracle/graalpython and apply them before compiling.
     This is what the "auto-patching C API usages" lines in build output refer
     to. It works for some packages but not all.

Where docling-parse stands:
  docling-parse ships prebuilt wheels for CPython on PyPI for common platforms
  (Linux x86_64, macOS arm64/x86_64, Windows x86_64). These are useless to
  GraalPy because of the ABI incompatibility. It does NOT appear in the official
  GraalPy wheels index, and it is NOT on the LAFO mirror. This means GraalPy
  would need to compile it from source -- and whether that succeeds depends on
  whether GraalPy's C extension compatibility layer can handle its particular
  C++ code.

  This is the core open question: "docling-parse doesn't have a prebuilt GraalPy
  wheel yet, and may or may not compile from source."


PART 4: OUR APPROACH
---------------------

Given the above, we decided on a staged approach designed to isolate exactly
where things succeed or fail.

DECISION: External directory deployment instead of embedded VFS
  Rather than bundling the Python venv inside the JAR (the default VFS mode),
  we use an external directory on disk:

    graalPy {
        externalDirectory = file("${rootDir}/python-resources")
        packages = ['docling-core']
    }

  This is more reliable on Windows for packages with native components, because
  it means pip installs into a real folder that the OS and compiler tools can
  access normally, rather than into an abstracted virtual filesystem.

STAGE 1: Pure Python smoke test with docling-core
  docling-core is the pure-Python schema and data model layer of the Docling
  project. It has no C/C++ extensions itself. Installing it first verifies that:
    - The GraalPy Gradle plugin resolves and runs correctly
    - The external directory venv is created and populated
    - The GraalPy Context can be created from Java
    - Python import machinery works end to end
  If this fails, the problem is infrastructure, not Docling-specific.

  Target import for smoke test:
    from docling_core.types.doc import DoclingDocument

STAGE 2: Full docling install (pending Stage 1 success)
  Once Stage 1 passes, swap the package to "docling" and update the import to:
    from docling.document_converter import DocumentConverter

  This is where docling-parse enters the picture. The build will attempt to
  either find a compatible wheel or compile docling-parse from source using
  MSVC on Windows. The outcome of this step is the answer to our open question.

CURRENT STATUS
--------------
Stage 1 (docling-core smoke test) is in progress. We hit a build error during
the graalPyInstallPackages task where GraalPy's pip attempted to compile numpy
from source (a transitive dependency of docling-core) and failed to reach
raw.githubusercontent.com and pypi.org to download build dependencies. This
appears to be a network connectivity issue (DNS resolution failures suggesting
a proxy or firewall on the network) rather than a fundamental GraalPy or
Docling incompatibility.

The numpy compilation was triggered because docling-core pulls in numpy as a
dependency, numpy has no prebuilt GraalPy wheel for Windows x86_64 on the
standard index, and the LAFO mirror (which does have a numpy wheel) was not
configured in our build file. Adding the LAFO mirror as an extra index URL
-- matching what the Spring Boot demo does -- is the next step once the network
issue is resolved.

OPEN QUESTIONS
--------------
1. Can docling-parse compile from source under GraalPy on Windows x86_64?
2. If not, what is the path to producing a GraalPy-compatible wheel for it?
3. Is the build failure network-related (proxy/firewall) or a deeper issue
   with GraalPy's pip on Windows?





