package com.myzone.jwget.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Objects.toStringHelper;
import static java.lang.Math.min;

public class ChunkGenerator implements Iterator<Chunk> {

    private final long totalSize;
    private final long chunkSize;

    private final AtomicLong offset;

    public ChunkGenerator(long totalSize, long chunkSize) {
        this.totalSize = totalSize;
        this.chunkSize = chunkSize;

        this.offset = new AtomicLong(0);
    }

    @Override
    public boolean hasNext() {
        return offset.get() < totalSize;
    }

    @Override
    public Chunk next() {
        while (true) {
            long currentOffset = offset.get();
            long currentChunkSize = min(totalSize - currentOffset, chunkSize);

            if (currentChunkSize <= 0)
                throw new NoSuchElementException();

            if (offset.compareAndSet(currentOffset, currentOffset + currentChunkSize))
                return new ImmutableChunk(currentOffset, currentChunkSize);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected static final class ImmutableChunk implements Chunk {

        private final long offset;
        private final long length;

        public ImmutableChunk(long offset, long length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImmutableChunk)) return false;

            ImmutableChunk that = (ImmutableChunk) o;

            if (length != that.length) return false;
            if (offset != that.offset) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (offset ^ (offset >>> 32));
            result = 31 * result + (int) (length ^ (length >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("offset", offset)
                    .add("length", length)
                    .toString();
        }

    }

}
