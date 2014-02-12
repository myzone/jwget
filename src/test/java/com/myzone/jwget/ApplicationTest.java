package com.myzone.jwget;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Files.copy;
import static com.myzone.jwget.Application.download;
import static com.myzone.jwget.io.FileMatchers.contentEqualsToIgnoreEol;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.nio.file.Files.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ApplicationTest {

    @ClassRule
    public static TemporaryFolder classFolder = new TemporaryFolder();

    private static File origin;
    private static File garbage;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private URL url;
    private Path destination;
    private int workersCount;
    private int chunkSize;

    @BeforeClass
    public static void setUpClass() throws Exception {
        origin = classFolder.newFile("origin.html");
        garbage = classFolder.newFile("garbage.txt");

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
        destination = folder.newFile("programming-motherfucker.html").toPath();
        workersCount = 4;
        chunkSize = 512;
    }

    @After
    public void tearDown() throws Exception {
        System.gc();
    }

    @Test
    public void testFileCreation() throws Exception {
        deleteIfExists(destination);

        download(url, destination, workersCount, chunkSize);

        assertTrue(exists(destination));
        assertThat(destination.toFile(), contentEqualsToIgnoreEol(origin));
    }

    @Test
    public void testFileRewrite() throws Exception {
        deleteIfExists(destination);
        createFile(destination);
        copy(garbage, destination.toFile());

        download(url, destination, workersCount, chunkSize);

        assertTrue(exists(destination));
        assertThat(destination.toFile(), contentEqualsToIgnoreEol(origin));
    }

}
