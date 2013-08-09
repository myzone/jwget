package com.myzone.jwget;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URL;

import static com.myzone.jwget.Application.download;
import static com.myzone.jwget.io.FileMatchers.contentEqualsToIgnoreEOL;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.nio.file.Files.delete;
import static org.apache.commons.io.IOUtils.copy;
import static org.junit.Assert.*;

public class ApplicationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File origin;

    private URL url;
    private File destination;
    private int workersCount;
    private int chunkSize;

    @Before
    public void setUp() throws Exception {
        origin = folder.newFile("origin.html");

        try (
                InputStream inputStream = getSystemResourceAsStream("origin.html");
                OutputStream outputStream = new FileOutputStream(origin)
        ) {
            copy(inputStream, outputStream);
        }

        url = new URL("http://programming-motherfucker.com/");
        destination = folder.newFile("programming-motherfucker.html");
        workersCount = 4;
        chunkSize = 512;
    }

    @After
    public void tearDown() throws Exception {
        System.gc();

        delete(destination.toPath());
    }

    @Test
    public void testFileCreation() throws Exception {
        delete(destination.toPath());
        assertFalse(destination.exists());

        download(url, destination, workersCount, chunkSize);

        assertTrue(destination.exists());
        assertThat(destination, contentEqualsToIgnoreEOL(origin));
    }

    @Test
    public void testFileRewrite() throws Exception {
        destination.createNewFile();
        copy(new StringReader("bla-bla-bla"), new FileOutputStream(destination));

        download(url, destination, workersCount, chunkSize);

        assertTrue(destination.exists());
        assertThat(destination, contentEqualsToIgnoreEOL(origin));
    }

}
