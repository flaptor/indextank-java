package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.ClientInterface.HttpCodeException;

public class UnexpectedCodeException extends RuntimeException {
    public final int httpCode;

    public UnexpectedCodeException(ClientInterface.HttpCodeException source) {
        super(source.getMessage());
        this.httpCode = source.getHttpCode();
    }
}
