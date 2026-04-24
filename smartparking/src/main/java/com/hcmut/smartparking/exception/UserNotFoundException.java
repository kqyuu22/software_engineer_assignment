package com.hcmut.smartparking.exception;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(int userId) {
        super("User not found in DATACORE: " + userId);
    }
}