package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.ClientInterface.HttpCodeException;

public class AuthenticationFailedException extends Exception {
    public AuthenticationFailedException(
            ClientInterface.HttpCodeException source) {
        super(source.getMessage());
    }
}
