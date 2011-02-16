package com.flaptor.indextank.apiclient;

import com.flaptor.indextank.apiclient.IndexTankClient.HttpCodeException;

public class IndexDoesNotExistException extends Exception {
	public IndexDoesNotExistException(HttpCodeException source) {
		super(source.getMessage());
	}
}
