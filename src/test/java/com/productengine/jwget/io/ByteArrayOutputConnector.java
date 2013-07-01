package com.productengine.jwget.io;

import com.productengine.jwget.io.OutputConnector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayOutputConnector implements OutputConnector {

    private final byte[] bytes;

    public ByteArrayOutputConnector(byte[] bytes) {
        this.bytes = bytes;
    }

    @NotNull
    @Override
    public OutputStream getSubstream(final long offset, final long length) {
        if (offset > Integer.MAX_VALUE)
            throw new RuntimeException("offset bigger then Integer.MAX_VALUE isn't supported");

        return new OutputStream() {

            private int currentPosition = (int) offset;
            private int endPosition = (int) offset + (int) length;

            @Override
            public void write(int b) throws IOException {
                if (currentPosition >= endPosition)
                    throw new IOException();

                bytes[currentPosition++] = (byte) b;
            }
        };
    }

}