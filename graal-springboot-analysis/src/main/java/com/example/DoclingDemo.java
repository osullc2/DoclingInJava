package com.example;

public class DoclingDemo {
    public static void main(String[] args) {
        System.out.println("=== GraalPy + Docling Demo ===");

        try (GraalPyContextManager contextManager = new GraalPyContextManager()) {

            DocumentConverter converter = new DocumentConverter(contextManager);

            String source = "https://arxiv.org/pdf/2408.09869";
            System.out.println("Converting: " + source);

            String markdown = converter.convertToMarkdown(source);

            System.out.println("\nResult (first 500 chars):");
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