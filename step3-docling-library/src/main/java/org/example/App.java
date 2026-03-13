package org.example;

public class App {
    public static void main(String[] args) {
        String path = System.getProperty("graalpy.resources");
        if (path == null || path.isBlank() || path.equals("null")) {
            System.err.println("Please provide 'graalpy.resources' system property.");
            System.err.println("Example: mvn -f step3-docling-library/pom.xml "
                    + "exec:java -Dexec.mainClass=org.example.App "
                    + "-Dgraalpy.resources=./step3-docling-library/python-resources");
            System.exit(1);
        }

        try (var context = GraalPy.createPythonContext(path)) {
            // Step 2: verify a simple Docling method usage.
            // We import DocumentConverter, construct it, and return a short status string.
            String script = """
                    from docling.document_converter import DocumentConverter
                    converter = DocumentConverter()
                    result = f"Docling DocumentConverter created: {converter.__class__.__name__}"
                    """;

            var value = context.eval("python", script + "\nresult");
            System.out.println(value.asString());
        }
    }
}

