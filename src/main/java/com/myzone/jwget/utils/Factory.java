package com.myzone.jwget.utils;

import org.jetbrains.annotations.NotNull;

public interface Factory<T> {

    @NotNull
    T create() throws CreationException;

    class CreationException extends Exception {

        public CreationException() {
            super();
        }

        public CreationException(String message) {
            super(message);
        }

        public CreationException(Exception cause) {
            super(cause.getMessage(), cause);
        }

        @Override
        public synchronized Exception getCause() {
            return (Exception) super.getCause();
        }

    }

}
