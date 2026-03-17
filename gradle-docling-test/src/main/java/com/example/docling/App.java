package com.example.docling;

import org.graalvm.polyglot.Context;

public class App {
    public static void main(String[] args) {
        String venvPath = System.getenv("DOCLING_VENV");
        if (venvPath == null || venvPath.isBlank()) {
            System.err.println("Missing DOCLING_VENV environment variable (path to the GraalPy venv).");
            System.exit(1);
        }

        String isWindows = System.getProperty("os.name").toLowerCase().contains("win") ? "true" : "false";

        String pythonExecutable = isWindows.equals("true")
                ? venvPath + "\\Scripts\\graalpy.exe"
                : venvPath + "/bin/graalpy";

        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .option("python.Executable", pythonExecutable)
                .option("python.ForceImportSite", "true")
                .build()) {

            // Keep it intentionally simple: prove docling is importable and return a version string.
            String script = """
                    import docling
                    result = getattr(docling, "__version__", "unknown")
                    """;
            var value = context.eval("python", script + "\nresult");
            System.out.println("docling version: " + value.asString());
        }
    }
}

