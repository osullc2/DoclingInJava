package org.example;

import org.graalvm.polyglot.io.ByteSequence;

/**
 * Java interface that mirrors the bits of the Python io module we need.
 */
interface IO {
    BytesIO BytesIO();

    interface BytesIO {
        ByteSequence getvalue();
    }
}

