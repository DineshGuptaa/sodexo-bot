package ru.nmedvedev.handler.exception;

public class ErrorResponse {
    public String message;
    public int code;

    public ErrorResponse(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
