package org.example;

import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

public class GraalPy {
    private static VirtualFileSystem vfs;

    /**
     * Create a Python Context that loads packages from an external directory
     * (e.g. the {@code python-resources} folder produced by the GraalPy Maven plugin).
     */
    public static Context createPythonContext(String pythonResourcesDirectory) {
        return GraalPyResources.contextBuilder(Path.of(pythonResourcesDirectory)).build();
    }

    /**
     * Create a Python Context that loads packages embedded as Java resources
     * using a VirtualFileSystem. Not used in this step but mirrors the guide.
     */
    public static Context createPythonContextFromResources() {
        if (vfs == null) {
            vfs = VirtualFileSystem.newBuilder()
                    .allowHostIO(VirtualFileSystem.HostIO.READ)
                    .build();
        }
        return GraalPyResources.contextBuilder(vfs).build();
    }
}

