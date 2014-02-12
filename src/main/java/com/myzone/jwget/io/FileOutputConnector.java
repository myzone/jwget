package com.myzone.jwget.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static com.google.common.base.Objects.toStringHelper;

public class FileOutputConnector implements OutputConnector {

    protected final Path path;
    protected final FileChannel fileChannel;

    protected volatile boolean closed;

    public FileOutputConnector(@NotNull Path path, @NotNull FileChannel fileChannel) {
        this.path = path;
        this.fileChannel = fileChannel;

        closed = false;
    }

    @NotNull
    @Override
    public OutputStream getSubstream(long offset, long length) throws IOException {
        if (closed)
            throw new IOException("Connector is closed");

        return new BufferedSubstream(path, fileChannel, offset, length);
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("path", path)
                .add("fileChannel", fileChannel)
                .add("closed", closed)
                .toString();
    }

    protected static class BufferedSubstream extends OutputStream {

        protected static final ConcurrentHashMap<Path, Semaphore> SEMAPHORES_MAP = new ConcurrentHashMap<>();

        protected final Path path;
        protected final FileChannel fileChannel;
        protected final long offset;

        protected final ByteBuffer buffer;

        public BufferedSubstream(@NotNull Path path, @NotNull FileChannel fileChannel, long offset, long length) throws IOException {
            if (length > Integer.MAX_VALUE)
                throw new RuntimeException("length bigger then Integer.MAX_VALUE isn't supported");

            this.path = path;
            this.fileChannel = fileChannel;
            this.offset = offset;

            buffer = ByteBuffer.allocate((int) length);
        }

        @Override
        public void write(int b) {
            buffer.put((byte) b);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) {
            buffer.put(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            buffer.flip();

            // lock on the OS level
            try (FileLock lock = fileChannel.lock(offset, buffer.capacity(), false)) {
                Semaphore semaphore = SEMAPHORES_MAP.computeIfAbsent(path, (path) -> new Semaphore(1, true));

                // lock on JVM level
                semaphore.acquireUninterruptibly();
                try {
                    long initialPosition = fileChannel.position();
                    try {
                        fileChannel.position(offset);
                        fileChannel.write(buffer);
                        fileChannel.force(true);
                    } finally {
                        fileChannel.position(initialPosition);
                    }
                } finally {
                    if (semaphore.availablePermits() == 0) {
                        // this semaphore is unused, so we should get rid of it

                        SEMAPHORES_MAP.remove(path);
                    }

                    semaphore.release();
                }
            }
        }

        @Override
        public void close() throws IOException {
            flush();
        }

    }

}
