mvn compile
mvn exec:java -Dexec.mainClass="com.example.DoclingWrapper"
```

---

## How it works
```
Java (JVM)
└── GraalVM Polyglot Context
    └── GraalPy interpreter (in-process)
        └── Docling (Python, loaded once at startup)
              ↑
        convertFn.execute("file.pdf")  ← Java calls this like any method
              ↓
        returns String back to Java