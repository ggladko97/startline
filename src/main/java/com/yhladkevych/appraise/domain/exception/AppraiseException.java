package com.yhladkevych.appraise.domain.exception;

public class AppraiseException extends RuntimeException {
    public AppraiseException(String message) {
        super(message);
    }

    public AppraiseException(String message, Throwable cause) {
        super(message, cause);
    }
}


