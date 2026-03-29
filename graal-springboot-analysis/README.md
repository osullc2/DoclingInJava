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
