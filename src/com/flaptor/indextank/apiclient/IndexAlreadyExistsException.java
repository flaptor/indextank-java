package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.IndexTankClient.HttpCodeException;

public class IndexAlreadyExistsException extends Exception {
	public IndexAlreadyExistsException(HttpCodeException source) {
		super(source.getMessage());
	}
}
