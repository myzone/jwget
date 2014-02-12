package com.myzone.jwget.io;

import com.google.common.base.Objects;
import com.myzone.jwget.utils.Factory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;

public class NetworkInputConnectorFactory implements Factory<NetworkInputConnector> {

    private final URL url;

    public NetworkInputConnectorFactory(@NotNull URL url) {
        this.url = url;
    }

    @NotNull
    @Override
    public NetworkInputConnector create() throws CreationException {
        try {
            return new NetworkInputConnector(url);
        } catch (IOException e) {
            throw new CreationException(e);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("url", url)
                .toString();
    }

}
