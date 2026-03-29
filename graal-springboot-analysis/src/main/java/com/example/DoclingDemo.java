package com.example;

import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.GraalPyResources;
import java.nio.file.Path;

public class DoclingDemo {
    public static void main(String[] args) {
        System.out.println("=== GraalPy + Docling Demo ===");
        System.out.println("Creating GraalPy context...");

        // Point GraalPyResources at our external python-resources directory
        Path externalDir = Path.of("python-resources");

        try (Context context = GraalPyResources.contextBuilder(externalDir).build()) {
            System.out.println("Context created successfully.");
            System.out.println("Attempting to import docling-core...");

            context.eval("python", """
                import sys
                print(f"Python version: {sys.version}")
                print(f"Python executable: {sys.executable}")

                from docling_core.types.doc import DoclingDocument
                print("SUCCESS: docling_core imported successfully!")
                print(f"DoclingDocument class: {DoclingDocument}")
            """);

            System.out.println("=== Demo complete ===");
        }
    }
}