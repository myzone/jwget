package com.myzone.jwget;

import com.myzone.jwget.io.FileOutputConnectorFactory;
import com.myzone.jwget.io.NetworkInputConnectorFactory;
import com.myzone.jwget.utils.Chunk;
import com.myzone.jwget.utils.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static java.nio.file.StandardOpenOption.*;
import static java.util.concurrent.Executors.newWorkStealingPool;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final String PROPERTY_PREFIX = "jwget";

    public static void main(String[] args) throws Exception {
        URL url = new URL(System.getProperty(PROPERTY_PREFIX + "." + "url"));
        Path destination = Paths.get(System.getProperty(PROPERTY_PREFIX + "." + "destination", url.getFile()));
        int workersCount = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "." + "workersCount", "2"));
        int chunkSize = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "." + "chunkSize", "4096"));

        LOGGER.info("Url: {}", url);
        LOGGER.info("Destination: {}", destination);
        LOGGER.info("Workers count: {}", workersCount);
        LOGGER.info("Chunk size: {}", chunkSize);

        download(url, destination, workersCount, chunkSize);
    }

    public static void download(@NotNull URL url, @NotNull Path destination, int workersCount, int chunkSize) throws Exception {
        Iterator<Chunk> chunkIterator = new ChunkGenerator(url.openConnection().getContentLength(), chunkSize);
        NetworkInputConnectorFactory inputConnectorFactory = new NetworkInputConnectorFactory(url);
        FileOutputConnectorFactory outputConnectorFactory = new FileOutputConnectorFactory(destination, FileChannel.open(destination, CREATE, WRITE, TRUNCATE_EXISTING));
        Downloader downloader = new Downloader(newWorkStealingPool(workersCount));

        try {
            downloader.download(inputConnectorFactory, outputConnectorFactory, chunkIterator, workersCount);
        } catch (Exception e) {
            LOGGER.error("Failed to download {} in cause of {}", url, e);

            throw e;
        }
    }

}
