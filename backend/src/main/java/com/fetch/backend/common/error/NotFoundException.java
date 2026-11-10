package com.fetch.backend.common.error;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
