package com.myzone.jwget;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URL;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Files.copy;
import static com.myzone.jwget.Application.download;
import static com.myzone.jwget.io.FileMatchers.contentEqualsToIgnoreEol;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.nio.file.Files.delete;
import static org.junit.Assert.*;

public class ApplicationTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static File origin;
    private static File garbage;

    private URL url;
    private File destination;
    private int workersCount;
    private int chunkSize;

    @BeforeClass
    public static void setUpClass() throws Exception {
        origin = folder.newFile("origin.html");
        garbage = folder.newFile("garbage.txt");

        try (InputStream inputStream = getSystemResourceAsStream("origin.html");
             OutputStream outputStream = new FileOutputStream(origin)) {
            copy(inputStream, outputStream);
        }

        try (InputStream inputStream = getSystemResourceAsStream("garbage.txt");
             OutputStream outputStream = new FileOutputStream(garbage)) {
            copy(inputStream, outputStream);
        }
    }

    @Before
    public void setUp() throws Exception {
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
        assertThat(destination, contentEqualsToIgnoreEol(origin));
    }

    @Test
    public void testFileRewrite() throws Exception {
        assertTrue(destination.exists() || destination.createNewFile());
        copy(garbage, destination);

        download(url, destination, workersCount, chunkSize);

        assertTrue(destination.exists());
        assertThat(destination, contentEqualsToIgnoreEol(origin));
    }

}
