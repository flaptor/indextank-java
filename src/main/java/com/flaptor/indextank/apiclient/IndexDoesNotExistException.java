package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.ClientInterface.HttpCodeException;

public class IndexDoesNotExistException extends Exception {
    public IndexDoesNotExistException(ClientInterface.HttpCodeException source) {
        super(source.getMessage());
    }
}
