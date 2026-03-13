package com.example;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import java.nio.file.Path;

public class DoclingWrapper {

    private final Context context;
    private final Value convertFn;

    public DoclingWrapper(String venvPath) {
        // Point GraalPy at your venv so it finds docling
        context = Context.newBuilder("python")
            .allowAllAccess(true)
            .allowIO(IOAccess.ALL)
            .option("python.Executable", venvPath + "/bin/graalpy")
            .option("python.ForceImportSite", "true")
            .build();

        // Bootstrap: import Docling and grab the converter
        context.eval("python", """
            from docling.document_converter import DocumentConverter
            _converter = DocumentConverter()

            def convert_to_markdown(source):
                result = _converter.convert(source)
                return result.document.export_to_markdown()

            def convert_to_text(source):
                result = _converter.convert(source)
                return result.document.export_to_text()
        """);

        convertFn = context.getBindings("python").getMember("convert_to_markdown");
    }

    public String convertToMarkdown(String filePath) {
        return convertFn.execute(filePath).asString();
    }

    public String convertToText(String filePath) {
        Value fn = context.getBindings("python").getMember("convert_to_text");
        return fn.execute(filePath).asString();
    }

    public void close() {
        context.close();
    }

    // Quick smoke test
    public static void main(String[] args) {
        try (DoclingWrapper wrapper = new DoclingWrapper(
                Path.of(".venv").toAbsolutePath().toString())) {

            String markdown = wrapper.convertToMarkdown("path/to/your.pdf");
            System.out.println(markdown);
        }
    }
}
