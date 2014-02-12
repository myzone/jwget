package com.myzone.jwget.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static com.google.common.base.Objects.toStringHelper;
import static java.lang.Math.min;

public class NetworkInputConnector implements InputConnector {

    protected final InputStream inputStream;
    protected volatile long currentOffset;

    protected final Object lock;

    public NetworkInputConnector(@NotNull URL url) throws IOException {
        inputStream = url.openConnection().getInputStream();
        currentOffset = 0;

        lock = new Object();
    }

    @NotNull
    @Override
    public InputStream getSubstream(long offset, long length) throws IOException {
        if (offset < 0)
            throw new IllegalArgumentException("offset can't be less then 0");

        if (length < 0)
            throw new IllegalArgumentException("length can't be less then 0");

        synchronized (lock) {
            if (offset < currentOffset)
                throw new UnsupportedOperationException("Rollbacks are unsupported");

            try {
                inputStream.skip(offset - currentOffset);

                currentOffset = offset + length;

                return new Substream(inputStream, length);
            } catch (IOException e) {
                throw new IOException("Connector is closed", e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            inputStream.close();
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("inputStream", inputStream)
                .add("currentOffset", currentOffset)
                .toString();
    }

    protected static class Substream extends InputStream {

        protected final InputStream inputStream;
        protected long bytesLeft;

        protected final Object lock;

        public Substream(@NotNull InputStream inputStream, long bytesLeft) {
            this.inputStream = inputStream;
            this.bytesLeft = bytesLeft;

            lock = new Object();
        }

        @Override
        public int read() throws IOException {
            synchronized (lock) {
                if (bytesLeft < 1)
                    return -1;

                int readByte = inputStream.read();

                if (readByte != -1)
                    bytesLeft--;

                return readByte;
            }
        }

        @Override
        public int read(@NotNull byte b[], int off, int len) throws IOException {
            synchronized (lock) {
                return super.read(b, off, len);
            }
        }

        @Override
        public long skip(long n) throws IOException {
            synchronized (lock) {
                long bytesSkipped = inputStream.skip(min(n, bytesLeft));

                bytesLeft -= bytesSkipped;

                return bytesSkipped;
            }
        }

        @Override
        public int available() {
            synchronized (lock) {
                return (int) bytesLeft;
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (lock) {
                skip(bytesLeft);
            }
        }

    }

}
