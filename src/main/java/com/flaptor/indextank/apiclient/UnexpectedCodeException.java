package com.flaptor.indextank.apiclient;


public class UnexpectedCodeException extends RuntimeException {
    public final int httpCode;

    public UnexpectedCodeException(ApiClient.HttpCodeException source) {
        super(source.getMessage());
        this.httpCode = source.getHttpCode();
    }
}
