package com.flaptor.indextank.apiclient;


public class AuthenticationFailedException extends Exception {
    public AuthenticationFailedException(
            ApiClient.HttpCodeException source) {
        super(source.getMessage());
    }
}
