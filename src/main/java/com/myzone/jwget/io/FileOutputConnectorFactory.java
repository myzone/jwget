package com.myzone.jwget.io;

import com.google.common.base.Objects;
import com.myzone.jwget.utils.Factory;
import org.jetbrains.annotations.NotNull;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class FileOutputConnectorFactory implements Factory<FileOutputConnector> {

    protected final Path path;
    protected final FileChannel fileChannel;

    public FileOutputConnectorFactory(@NotNull Path path, @NotNull FileChannel fileChannel) {
        this.path = path;
        this.fileChannel = fileChannel;
    }

    @NotNull
    @Override
    public FileOutputConnector create() {
        return new FileOutputConnector(path, fileChannel);
    }

    @Override public String toString() {
        return Objects
                .toStringHelper(this)
                .add("path", path)
                .add("fileChannel", fileChannel)
                .toString();
    }

}
