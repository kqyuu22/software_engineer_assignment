package com.hcmut.smartparking.exception;

public class DbUnreachableException extends Exception {
    public DbUnreachableException(String message) {
        super(message);
    }
}