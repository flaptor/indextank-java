package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.IndexTankClient.HttpCodeException;


public class MaximumIndexesExceededException extends Exception {

    public MaximumIndexesExceededException(HttpCodeException source) {
        super(source.getMessage());
    }
}
