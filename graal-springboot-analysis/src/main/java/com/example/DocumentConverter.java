package com.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class DocumentConverter {

    private final Context context;

    public DocumentConverter(GraalPyContextManager contextManager) {
        this.context = contextManager.getContext();
        initialize();
    }

    private void initialize() {
        context.eval("python", """
            from docling.document_converter import DocumentConverter as _DoclingConverter

            _converter = _DoclingConverter()

            def convert_to_markdown(source):
                result = _converter.convert(source)
                return result.document.export_to_markdown()
        """);
    }

    public String convertToMarkdown(String source) {
        Value convertFn = context.getBindings("python").getMember("convert_to_markdown");
        return convertFn.execute(source).asString();
    }
}