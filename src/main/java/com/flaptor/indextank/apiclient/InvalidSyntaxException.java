package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.ClientInterface.HttpCodeException;

public class InvalidSyntaxException extends Exception {
    public InvalidSyntaxException(ClientInterface.HttpCodeException source) {
        super(source.getMessage());
    }
}
