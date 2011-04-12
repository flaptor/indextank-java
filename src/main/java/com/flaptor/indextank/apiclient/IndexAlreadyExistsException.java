package com.flaptor.indextank.apiclient;


public class IndexAlreadyExistsException extends Exception {
    public IndexAlreadyExistsException(ApiClient.HttpCodeException source) {
        super(source.getMessage());
    }
}
