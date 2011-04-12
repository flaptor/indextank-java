package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.ClientInterface.HttpCodeException;

public class IndexAlreadyExistsException extends Exception {
    public IndexAlreadyExistsException(ClientInterface.HttpCodeException source) {
        super(source.getMessage());
    }
}
