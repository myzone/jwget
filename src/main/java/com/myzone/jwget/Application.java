package com.myzone.jwget;

import com.myzone.jwget.io.FileOutputConnectorFactory;
import com.myzone.jwget.io.NetworkInputConnectorFactory;
import com.myzone.jwget.utils.Chunk;
import com.myzone.jwget.utils.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Iterator;

import static java.util.concurrent.Executors.newWorkStealingPool;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final String PROPERTY_PREFIX = "jwget";

    public static void main(String[] args) throws Exception {
        URL url = new URL(System.getProperty(PROPERTY_PREFIX + "." + "url"));
        File destination = new File(System.getProperty(PROPERTY_PREFIX + "." + "destination", url.getFile()));
        int workersCount = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "." + "workersCount", "2"));
        int chunkSize = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "." + "chunkSize", "4096"));

        LOGGER.info("Url: {}", url);
        LOGGER.info("Destination: {}", destination);
        LOGGER.info("Workers count: {}", workersCount);
        LOGGER.info("Chunk size: {}", chunkSize);

        download(url, destination, workersCount, chunkSize);
    }

    public static void download(@NotNull URL url, @NotNull File destination, int workersCount, int chunkSize) throws Exception {
        Iterator<Chunk> chunkIterator = new ChunkGenerator(url.openConnection().getContentLength(), chunkSize);
        NetworkInputConnectorFactory inputConnectorFactory = new NetworkInputConnectorFactory(url);
        try (RandomAccessFile file = new RandomAccessFile(destination, "rws")) {
            FileOutputConnectorFactory outputConnectorFactory = new FileOutputConnectorFactory(file);
            Downloader downloader = new Downloader(newWorkStealingPool(workersCount));

            downloader.download(inputConnectorFactory, outputConnectorFactory, chunkIterator, workersCount);
        } catch (Exception e) {
            LOGGER.error("Failed to download {} in cause of {}", url, e);

            throw e;
        }
    }

}
