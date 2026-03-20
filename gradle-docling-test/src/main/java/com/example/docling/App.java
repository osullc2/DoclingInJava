package com.example.docling;

import org.graalvm.polyglot.Context;

/**
 * Gradle standalone smoke tests for Docling via GraalPy.
 *
 * Usage examples:
 * - .\gradlew run
 * - .\gradlew run --args="--input C:\path\to\file.pdf --mode both"
 */
public class App {
    public static void main(String[] args) {
        String venvPath = System.getenv("DOCLING_VENV");
        if (venvPath == null || venvPath.isBlank()) {
            System.err.println("Missing DOCLING_VENV environment variable (path to the GraalPy venv).");
            System.exit(1);
        }

        String inputPath = null;
        String mode = "both";

        for (int i = 0; i < args.length; i++) {
            if ("--input".equals(args[i]) && i + 1 < args.length) {
                inputPath = args[++i];
            } else if ("--mode".equals(args[i]) && i + 1 < args.length) {
                mode = args[++i].toLowerCase();
            }
        }

        boolean wantMarkdown = mode.equals("both") || mode.equals("markdown");
        boolean wantText = mode.equals("both") || mode.equals("text");

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String pythonExecutable = isWindows
                ? venvPath + "\\Scripts\\graalpy.exe"
                : venvPath + "/bin/graalpy";

        String pyInputPathLiteral = inputPath == null ? "None" : toPythonStringLiteral(inputPath);

        // Single Python script that runs multiple smoke checks and optional conversion.
        // We intentionally truncate markdown/text previews to keep console output manageable.
        String script = ""
                + "import json, os, traceback\\n"
                + "out = {}\\n"
                + "def set_ok(key, value):\\n"
                + "  out[key] = value\\n"
                + "def set_err(key, e):\\n"
                + "  out[key] = {\"error\": type(e).__name__, \"message\": str(e)}\\n"
                + "\\n"
                + "try:\\n"
                + "  import docling\\n"
                + "  out['docling_import_ok'] = True\\n"
                + "  out['docling_version'] = getattr(docling, '__version__', None)\\n"
                + "except Exception as e:\\n"
                + "  set_err('docling_import', e)\\n"
                + "\\n"
                + "try:\\n"
                + "  from docling.document_converter import DocumentConverter\\n"
                + "  out['DocumentConverter_import_ok'] = True\\n"
                + "  out['DocumentConverter_class'] = getattr(DocumentConverter, '__name__', str(DocumentConverter))\\n"
                + "  converter = DocumentConverter()\\n"
                + "  out['DocumentConverter_instance_class'] = converter.__class__.__name__\\n"
                + "  out['has_convert'] = hasattr(converter, 'convert')\\n"
                + "except Exception as e:\\n"
                + "  set_err('DocumentConverter_setup', e)\\n"
                + "  converter = None\\n"
                + "\\n"
                + "input_path = " + pyInputPathLiteral + "\\n"
                + "out['input_path'] = input_path\\n"
                + "out['convert_mode'] = {\"wantMarkdown\": " + wantMarkdown + ", \"wantText\": " + wantText + "}\\n"
                + "\\n"
                + "if converter is not None and input_path is not None:\\n"
                + "  try:\\n"
                + "    if not os.path.exists(input_path):\\n"
                + "      out['convert_skipped'] = 'path_does_not_exist'\\n"
                + "    else:\\n"
                + "      result = converter.convert(input_path)\\n"
                + "      out['convert_result_type'] = type(result).__name__\\n"
                + "      doc = getattr(result, 'document', result)\\n"
                + "      out['document_type'] = type(doc).__name__\\n"
                + "      out['has_export_to_markdown'] = hasattr(doc, 'export_to_markdown')\\n"
                + "      out['has_export_to_text'] = hasattr(doc, 'export_to_text')\\n"
                + "      if " + wantMarkdown + " and hasattr(doc, 'export_to_markdown'):\\n"
                + "        md = doc.export_to_markdown()\\n"
                + "        out['markdown_preview'] = md[:500]\\n"
                + "      if " + wantText + " and hasattr(doc, 'export_to_text'):\\n"
                + "        txt = doc.export_to_text()\\n"
                + "        out['text_preview'] = txt[:500]\\n"
                + "  except Exception as e:\\n"
                + "    out['convert_error'] = {\"error\": type(e).__name__, \"message\": str(e)}\\n"
                + "    out['convert_traceback'] = traceback.format_exc()\\n"
                + "else:\\n"
                + "  out['convert_skipped'] = (input_path is None)\\n"
                + "\\n"
                + "json.dumps(out, indent=2)\\n";

        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .option("python.Executable", pythonExecutable)
                .option("python.ForceImportSite", "true")
                .build()) {
            var value = context.eval("python", script);
            System.out.println(value.asString());
        }
    }

    private static String toPythonStringLiteral(String s) {
        // Build a safe Python double-quoted string literal.
        // Note: we escape backslashes because Windows paths contain many.
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}

