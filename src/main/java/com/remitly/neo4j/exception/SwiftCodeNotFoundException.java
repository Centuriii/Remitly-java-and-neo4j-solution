package com.remitly.neo4j.exception;

public class SwiftCodeNotFoundException extends RuntimeException {
    public SwiftCodeNotFoundException(String message) {
        super(message);
    }
}