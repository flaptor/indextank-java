package com.flaptor.indextank.apiclient;

import java.io.IOException;
import java.util.List;

import com.flaptor.indextank.apiclient.IndexTankClient.Index;
import com.flaptor.indextank.apiclient.IndexTankClient.IndexConfiguration;


public interface ApiClient {

    Index getIndex(String indexName);

    Index createIndex(String indexName) throws IOException, IndexAlreadyExistsException,
            MaximumIndexesExceededException;

    Index createIndex(String indexName, IndexConfiguration conf) throws IOException,
            IndexAlreadyExistsException, MaximumIndexesExceededException;

    void deleteIndex(String indexName) throws IOException,
            IndexDoesNotExistException;

    List<? extends Index> listIndexes() throws IOException;

}