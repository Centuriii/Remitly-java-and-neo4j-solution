package com.remitly.neo4j.exception;

public class SwiftCodeAlreadyExistsException extends RuntimeException {
    public SwiftCodeAlreadyExistsException(String message) {
        super(message);
    }
}