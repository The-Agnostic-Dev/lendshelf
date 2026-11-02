package com.lendshelf.service;

/**
 * A lending rule was broken. The message is shown to the person at the desk as is.
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }
}
