package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final int code;
    private final HttpStatus status;

    public ApiException(HttpStatus status, int code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
