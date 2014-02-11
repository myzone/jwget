package com.myzone.jwget;

import com.myzone.jwget.io.InputConnector;
import com.myzone.jwget.io.OutputConnector;
import com.myzone.jwget.utils.Chunk;
import com.myzone.jwget.utils.Factory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.io.ByteStreams.copy;

public class Downloader {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    protected final @NotNull ExecutorService executorService;

    public Downloader(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void download(
            final @NotNull Factory<? extends InputConnector> inputConnectorFactory,
            final @NotNull Factory<? extends OutputConnector> outputConnectorFactory,
            final @NotNull Iterator<? extends Chunk> chunkIterator,
            final int streamsCount
    ) throws Exception {
        LOGGER.info("Downloading has been started");

        Semaphore semaphore = new Semaphore(0);
        AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);

        for (int streamsCounter = 0; streamsCounter < streamsCount; streamsCounter++) {
            executorService.submit((Runnable) () -> {
                try {
                    try (InputConnector inputConnector = inputConnectorFactory.create()) {
                        LOGGER.info("{} has been successfully created", inputConnector);

                        try (OutputConnector outputConnector = outputConnectorFactory.create()) {
                            LOGGER.info("{} has been successfully created", outputConnector);

                            downloadInternal(inputConnector, outputConnector, chunkIterator);

                            semaphore.release();
                        } catch (Factory.CreationException e) {
                            LOGGER.error("Creation of outputConnector has been failed", e);

                            throw e.getCause();
                        }
                    } catch (Factory.CreationException e) {
                        LOGGER.error("Creation of inputConnector has been failed", e);

                        throw e.getCause();
                    }
                } catch (Exception e){
                    if (exceptionReference.compareAndSet(null, e)) {
                        semaphore.release(streamsCount);
                    }
                }
            });
        }

        semaphore.acquire(streamsCount);
        Exception exception = exceptionReference.get();
        if (exception == null) {
            LOGGER.info("Downloading has been finished");
        } else {
            LOGGER.error("Downloading has been failed in cause of {}", exception);

            throw exception;
        }
    }

    protected void downloadInternal(InputConnector inputConnector, OutputConnector outputConnector, Iterator<? extends Chunk> chunkIterator) {
        try {
            while (chunkIterator.hasNext()) {
                Chunk chunk = chunkIterator.next();

                try (InputStream inputStream = inputConnector.getSubstream(chunk.getOffset(), chunk.getLength());
                     OutputStream outputStream = outputConnector.getSubstream(chunk.getOffset(), chunk.getLength())) {
                    LOGGER.info("Downloading of {} has been started", chunk);

                    copy(inputStream, outputStream);

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
