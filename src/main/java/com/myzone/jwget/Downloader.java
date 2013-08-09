package com.myzone.jwget;

import com.myzone.jwget.io.InputConnector;
import com.myzone.jwget.io.OutputConnector;
import com.myzone.jwget.utils.Chunk;
import com.myzone.jwget.utils.Factory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;

public class Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    public void download(
            final @NotNull Factory<? extends InputConnector> inputConnectorFactory,
            final @NotNull Factory<? extends OutputConnector> outputConnectorFactory,
            final @NotNull Iterator<? extends Chunk> chunkIterator,
            final int workersCount
    ) throws InterruptedException {
        ExecutorService executorService = newFixedThreadPool(workersCount);
        Queue<Closeable> toClose = new ArrayDeque<>();

        for (int launchCounter = 0; launchCounter < workersCount; launchCounter++) {
            try {
                InputConnector inputConnector = inputConnectorFactory.create();
                LOGGER.info("{} has been successfully created", inputConnector);

                try {
                    OutputConnector outputConnector = outputConnectorFactory.create();
                    LOGGER.info("{} has been successfully created", outputConnector);

                    toClose.add(inputConnector);
                    toClose.add(outputConnector);

                    executorService.submit(new ChunkDownloadTask(inputConnector, outputConnector, chunkIterator));
                } catch (Factory.CreationException e) {
                    LOGGER.error("Creation of outputConnector has been failed", e);

                    closeQuietly(inputConnector);
                }
            } catch (Factory.CreationException e) {
                LOGGER.error("Creation of inputConnector has been failed", e);
            }
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } finally {
            while (!toClose.isEmpty()) {
                Closeable closeable = toClose.poll();

                closeQuietly(closeable);

                LOGGER.info("{} has been closed", closeable);
            }
        }
    }

    protected static class ChunkDownloadTask implements Runnable {

        private final InputConnector inputConnector;
        private final OutputConnector outputConnector;
        private final Iterator<? extends Chunk> chunkIterator;

        public ChunkDownloadTask(InputConnector inputConnector, OutputConnector outputConnector, Iterator<? extends Chunk> chunkIterator) {
            this.inputConnector = inputConnector;
            this.outputConnector = outputConnector;
            this.chunkIterator = chunkIterator;
        }

        @Override
        public void run() {
            try {
                while (chunkIterator.hasNext()) {
                    Chunk chunk = chunkIterator.next();

                    try (
                            InputStream inputStream = inputConnector.getSubstream(chunk.getOffset(), chunk.getLength());
                            OutputStream outputStream = outputConnector.getSubstream(chunk.getOffset(), chunk.getLength());
                    ) {
                        LOGGER.info("Downloading of {} has been started", chunk);

                        copyLarge(inputStream, outputStream);

                        LOGGER.info("Downloading of {} has been done", chunk);
                    } catch (IOException exception) {
                        LOGGER.error("Failed to download {} in cause of {}", chunk, exception);
                    }
                }
            } catch (NoSuchElementException ignored) {
                // all is ok, all chunks have been downloaded, so we're shutting down
            }
        }
    }
}
