package com.flaptor.indextank.apiclient;


public class IndexDoesNotExistException extends Exception {
    public IndexDoesNotExistException(ApiClient.HttpCodeException source) {
        super(source.getMessage());
    }
}
