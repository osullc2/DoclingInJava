
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

