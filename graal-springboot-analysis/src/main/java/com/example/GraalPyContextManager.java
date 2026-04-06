package com.example;

import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.GraalPyResources;
import java.nio.file.Path;

public class GraalPyContextManager implements AutoCloseable {

    private final Context context;

    public GraalPyContextManager() {
        Path externalDir = Path.of("python-resources");
        this.context = GraalPyResources.contextBuilder(externalDir).build();
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void close() {
        context.close();
    }
}