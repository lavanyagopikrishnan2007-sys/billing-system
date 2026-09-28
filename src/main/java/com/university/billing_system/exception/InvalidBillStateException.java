package com.university.billing_system.exception;

public class InvalidBillStateException extends RuntimeException {
    public InvalidBillStateException(String message) {
        super(message);
    }
}