package com.flaptor.indextank.apiclient;


public class MaximumIndexesExceededException extends Exception {

    public MaximumIndexesExceededException(
            ApiClient.HttpCodeException source) {
        super(source.getMessage());
    }
}
