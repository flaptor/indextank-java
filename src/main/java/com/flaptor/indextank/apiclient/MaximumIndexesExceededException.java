package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.ClientInterface.HttpCodeException;

public class MaximumIndexesExceededException extends Exception {

    public MaximumIndexesExceededException(
            ClientInterface.HttpCodeException source) {
        super(source.getMessage());
    }
}
