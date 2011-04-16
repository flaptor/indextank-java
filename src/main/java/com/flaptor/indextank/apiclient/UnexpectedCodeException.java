package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.IndexTankClient.HttpCodeException;


public class UnexpectedCodeException extends RuntimeException {
    public final int httpCode;

    public UnexpectedCodeException(HttpCodeException source) {
        super(source.getMessage());
        this.httpCode = source.getHttpCode();
    }
}
