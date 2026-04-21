package com.se.sebtl.exception;


public class UserNotFoundException extends Exception {
    public UserNotFoundException(int userId) {
        super("User not found in DATACORE: " + userId);
    }
}