package com.flaptor.indextank.apiclient;

import java.io.IOException;
import java.util.List;


public interface ApiClient {

    Index getIndex(String indexName);

    Index createIndex(String indexName) throws IOException,
            IndexAlreadyExistsException, MaximumIndexesExceededException;

    void deleteIndex(String indexName) throws IOException,
            IndexDoesNotExistException;

    List<? extends Index> listIndexes() throws IOException;

}