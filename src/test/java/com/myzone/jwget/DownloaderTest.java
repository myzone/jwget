package com.myzone.jwget;

import com.myzone.jwget.io.ByteArrayInputConnector;
import com.myzone.jwget.io.ByteArrayOutputConnector;
import com.myzone.jwget.utils.ChunkGenerator;
import org.junit.Before;
import org.junit.Test;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.*;

public class DownloaderTest {

    private Downloader downloader;

    @Before
    public void setUp() throws Exception {
        downloader = new Downloader(newCachedThreadPool());
    }

    @Test
    public void testDownloadInOneThread() throws Exception {
        final byte[] expected = "oololoololoololololoololoololoololoololoololoololoololooololoololoololololo".getBytes();
        final byte[] actual = new byte[expected.length];

        downloader.download(
                () -> new ByteArrayInputConnector(expected),
                () -> new ByteArrayOutputConnector(actual),
                new ChunkGenerator(expected.length, 32),
                1
        );

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testDownloadManyThreads() throws Exception {
        final byte[] expected = "oololoololoololololoololoololoololoololoololoololoololooololoololoololololo".getBytes();
        final byte[] actual = new byte[expected.length];

        downloader.download(
                () -> new ByteArrayInputConnector(expected),
                () -> new ByteArrayOutputConnector(actual),
                new ChunkGenerator(expected.length, 32),
                10
        );

        assertArrayEquals(expected, actual);
    }


    @Test(expected = Exception.class)
    public void testDownloadFailedData() throws Exception {
        final byte[] expected = "oololoololoololololoololoololoololoololoololoololoololooololoololoololololo".getBytes();
        final byte[] actual = new byte[expected.length];

        downloader.download(
                () -> new ByteArrayInputConnector(expected),
                () -> new ByteArrayOutputConnector(actual),
                new ChunkGenerator(expected.length + 5, 32),
                10
        );

        assertArrayEquals(expected, actual);
    }

}
