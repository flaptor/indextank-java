package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.IndexTankClient.HttpCodeException;

public class AuthenticationFailedException extends Exception {
	public AuthenticationFailedException(HttpCodeException source) {
		super(source.getMessage());
	}
}
