package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.IndexTankClient.HttpCodeException;


public class InvalidSyntaxException extends Exception {
    public InvalidSyntaxException(HttpCodeException source) {
        super(source.getMessage());
    }
}
