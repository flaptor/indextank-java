package com.flaptor.indextank.apiclient;


public class InvalidSyntaxException extends Exception {
    public InvalidSyntaxException(ApiClient.HttpCodeException source) {
        super(source.getMessage());
    }
}
