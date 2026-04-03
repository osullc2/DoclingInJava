package com.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;
import java.nio.file.Path;

public class DoclingDemo {
    public static void main(String[] args) {
        System.out.println("=== GraalPy + Docling Demo ===");
        System.out.println("Creating GraalPy context...");

        Path externalDir = Path.of("python-resources");

        try (Context context = GraalPyResources.contextBuilder(externalDir).build()) {
            System.out.println("Context created successfully.");

            // Step 1: Verify docling imports cleanly
            System.out.println("\n[Step 1] Importing docling...");
            context.eval("python", """
                import sys
                print(f"Python version: {sys.version}")

                from docling.document_converter import DocumentConverter
                print("SUCCESS: docling imported successfully!")
            """);

            // Step 2: Convert a document and return the markdown
            System.out.println("\n[Step 2] Converting document...");
            context.eval("python", """
                from docling.document_converter import DocumentConverter

                def convert_to_markdown(source):
                    converter = DocumentConverter()
                    result = converter.convert(source)
                    return result.document.export_to_markdown()
            """);

            // Call the Python function from Java with a sample PDF URL
            Value convertFn = context.getBindings("python").getMember("convert_to_markdown");
            String source = "https://arxiv.org/pdf/2408.09869"; // Docling's own technical report
            System.out.println("Converting: " + source);

            Value result = convertFn.execute(source);
            String markdown = result.asString();

            // Step 3: Print a preview of the output
            System.out.println("\n[Step 3] Conversion result (first 500 chars):");
            System.out.println("---");
            System.out.println(markdown.substring(0, Math.min(500, markdown.length())));
            System.out.println("---");

            System.out.println("\n=== Demo complete ===");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}