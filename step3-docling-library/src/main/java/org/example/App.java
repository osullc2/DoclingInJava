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
            // Step 1: only verify that Docling can be imported – no methods used yet.
            var value = context.eval("python", "import docling; 'Docling imported OK'");
            System.out.println(value.asString());
        }
    }
}

