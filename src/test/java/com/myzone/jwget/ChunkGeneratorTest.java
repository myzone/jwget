package com.myzone.jwget;

import com.myzone.jwget.utils.Chunk;
import com.myzone.jwget.utils.ChunkGenerator;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ChunkGeneratorTest {

    @Test
    public void testLastChunk() {
        Chunk lastChunk = null;

        for (Iterator<Chunk> chunkGenerator = new ChunkGenerator(17 * 1024 + 763, 1024); chunkGenerator.hasNext(); ) {
            lastChunk = chunkGenerator.next();
        }

        assertNotNull(lastChunk);
        assertEquals(763, lastChunk.getLength());
        assertEquals(17 * 1024, lastChunk.getOffset());
    }

    @Test
    public void testOneChunk() {
        int chunksCount = 0;
        Chunk lastChunk = null;

        for (Iterator<Chunk> chunkGenerator = new ChunkGenerator(1024 + 763, 2048); chunkGenerator.hasNext(); ) {
            chunksCount++;
            lastChunk = chunkGenerator.next();
        }

        assertEquals(1, chunksCount);

        assertNotNull(lastChunk);
        assertEquals(1024 + 763, lastChunk.getLength());
        assertEquals(0, lastChunk.getOffset());
    }

    @Test(expected = NoSuchElementException.class)
    public void testWrongAccess() throws Throwable {
        for (final Iterator<Chunk> chunkGenerator = new ChunkGenerator(17 * 1024, 1024); chunkGenerator.hasNext(); ) {
            chunkGenerator.next();
            chunkGenerator.next();
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testConcurrentAccess() throws Throwable {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        for (final Iterator<Chunk> chunkGenerator = new ChunkGenerator(17 * 1024, 1024); chunkGenerator.hasNext(); ) {
            try {
                executor.submit(() -> {
                    chunkGenerator.next();
                }).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }

            chunkGenerator.next();
        }
    }

}
