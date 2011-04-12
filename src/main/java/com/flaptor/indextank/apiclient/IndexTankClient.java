package com.flaptor.indextank.apiclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.flaptor.indextank.apiclient.spec.ClientInterface;
import com.flaptor.indextank.apiclient.spec.IndexInterface;

public class IndexTankClient implements ClientInterface {

    private static final String GET_METHOD = "GET";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";

    private static final String SEARCH_URL = "/search";
    private static final String DOCS_URL = "/docs";
    private static final String CATEGORIES_URL = "/docs/categories";
    private static final String VARIABLES_URL = "/docs/variables";
    private static final String PROMOTE_URL = "/promote";
    private static final String FUNCTIONS_URL = "/functions";

    private static final DateFormat ISO8601_PARSER = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssz");

    private static Object callAPI(String method, String urlString,
            Map<String, String> params, String privatePass) throws IOException,
            HttpCodeException {
        return callAPI(method, urlString, params, (String) null, privatePass);
    }

    private static Object callAPI(String method, String urlString,
            String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, null, (String) null, privatePass);
    }

    private static Object callAPI(String method, String urlString,
            Map<String, String> params, Map<String, Object> data,
            String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, params, data == null ? null
                : JSONObject.toJSONString(data), privatePass);
    }

    private static Object callAPI(String method, String urlString,
            Map<String, String> params, List<Map<String, Object>> data,
            String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, params, data == null ? null
                : JSONArray.toJSONString(data), privatePass);
    }

    private static Object callAPI(String method, String urlString,
            Map<String, String> params, String data, String privatePass)
            throws IOException, HttpCodeException {

        if (params != null && !params.isEmpty()) {
            urlString += "?" + paramsToQueryString(params);
        }
        URL url = new URL(urlString);

        HttpURLConnection urlConnection = (HttpURLConnection) url
                .openConnection();

        // GAE fix:
        // http://code.google.com/p/googleappengine/issues/detail?id=1454
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Authorization",
                "Basic " + Base64.encodeBytes(privatePass.getBytes()));
        urlConnection.setRequestMethod(method);

        if (method.equals(PUT_METHOD) && data != null) {
            // write
            OutputStreamWriter out = new OutputStreamWriter(
                    urlConnection.getOutputStream(), "UTF-8");
            out.write(data);
            out.close();
        }

        BufferedReader in;
        int responseCode = urlConnection.getResponseCode();
        StringBuffer response = new StringBuffer();

        if (responseCode >= 400) {
            InputStream errorStream = urlConnection.getErrorStream();

            if (errorStream != null) {
                in = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }

            throw new HttpCodeException(responseCode, response.toString());
        }

        in = new BufferedReader(new InputStreamReader(
                urlConnection.getInputStream()));

        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        if (responseCode != 200 && responseCode != 201) {
            throw new HttpCodeException(responseCode, response.toString());
        }

        in.close();

        String jsonResponse = response.toString();
        if (!jsonResponse.isEmpty()) {
            JSONParser parser = new JSONParser();
            try {
                return parser.parse(jsonResponse);
            } catch (org.json.simple.parser.ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    private static String paramsToQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                sb.append("&");
            } catch (UnsupportedEncodingException e) {
            }
        }

        return sb.toString();
    }

    /**
     * Client to control a specific index.
     * 
     * @author flaptor
     * 
     */
    public class Index implements IndexInterface {
        private final String indexUrl;
        private Map<String, Object> metadata;

        private Index(String indexUrl) {
            this.indexUrl = indexUrl;
        }

        private Index(String indexUrl, Map<String, Object> metadata) {
            this.indexUrl = indexUrl;
            this.metadata = metadata;
        }

        @Override
        public SearchResults search(String query) throws IOException,
                InvalidSyntaxException {
            return search(ClientInterface.Query.forString(query));
        }

        @Override
        public SearchResults search(ClientInterface.Query query) throws IOException,
                InvalidSyntaxException {
            Map<String, String> params = query.toParameterMap();

            try {
                return new SearchResults((Map<String, Object>) callAPI(
                        GET_METHOD, indexUrl + SEARCH_URL, params, privatePass));
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 400) {
                    throw new InvalidSyntaxException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void create() throws IOException, IndexAlreadyExistsException,
                MaximumIndexesExceededException {
            try {
                callAPI(PUT_METHOD, indexUrl, privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 204) {
                    throw new IndexAlreadyExistsException(e);
                } else if (e.getHttpCode() == 409) {
                    throw new MaximumIndexesExceededException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void delete() throws IOException, IndexDoesNotExistException {
            try {
                callAPI(DELETE_METHOD, indexUrl, privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public BatchResults addDocuments(Iterable<Document> documents)
                throws IOException, IndexDoesNotExistException {
            List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

            for (Document document : documents) {
                Map<String, Object> documentMap = document.toDocumentMap();
                data.add(documentMap);
            }

            try {
                List<Map<String, Object>> results = (List<Map<String, Object>>) callAPI(
                        PUT_METHOD, indexUrl + DOCS_URL, null, data,
                        privatePass);

                List<Boolean> addeds = new ArrayList<Boolean>();
                List<String> errors = new ArrayList<String>();
                boolean hasErrors = false;

                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> result = results.get(i);
                    Boolean added = (Boolean) result.get("added");

                    addeds.add(i, added);

                    if (!added) {
                        hasErrors = true;
                        errors.add(i, (String) result.get("error"));
                    }
                }

                ArrayList<Document> documentsList = new ArrayList<Document>();

                for (Document document : documents) {
                    documentsList.add(document);
                }

                return new BatchResults(addeds, errors, documentsList,
                        hasErrors);

            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 400) {
                    throw new IllegalArgumentException(e);
                } else if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }

        }

        @Override
        public void addDocument(String documentId, Map<String, String> fields)
                throws IOException, IndexDoesNotExistException {
            addDocument(documentId, fields, null);
        }

        @Override
        public void addDocument(String documentId, Map<String, String> fields,
                Map<Integer, Float> variables) throws IOException,
                IndexDoesNotExistException {
            addDocument(documentId, fields, variables, null);
        }

        @Override
        public void addDocument(String documentId, Map<String, String> fields,
                Map<Integer, Float> variables, Map<String, String> categories)
                throws IOException, IndexDoesNotExistException {
            if (null == documentId)
                throw new IllegalArgumentException(
                        "documentId can not be null.");
            if (documentId.getBytes("UTF-8").length > 1024)
                throw new IllegalArgumentException(
                        "documentId can not be longer than 1024 bytes when UTF-8 encoded.");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("fields", fields);
            if (variables != null) {
                data.put("variables", variables);
            }

            if (categories != null) {
                data.put("categories", categories);
            }

            try {
                callAPI(PUT_METHOD, indexUrl + DOCS_URL, null, data,
                        privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 400) {
                    // Should throw InvalidArgument, but it breaks backward
                    // compatibility.
                    throw new UnexpectedCodeException(e);
                    // throw new InvalidArgumentException(e);
                } else if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void deleteDocument(String documentId) throws IOException,
                IndexDoesNotExistException {
            if (null == documentId)
                throw new IllegalArgumentException("documentId can not be null");
            Map<String, String> params = new HashMap<String, String>();
            params.put("docid", documentId);

            try {
                callAPI(DELETE_METHOD, indexUrl + DOCS_URL, params, privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void updateVariables(String documentId,
                Map<Integer, Float> variables) throws IOException,
                IndexDoesNotExistException {
            if (null == documentId)
                throw new IllegalArgumentException("documentId can not be null");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("variables", variables);

            try {
                callAPI(PUT_METHOD, indexUrl + VARIABLES_URL, null, data,
                        privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void updateCategories(String documentId,
                Map<String, String> variables) throws IOException,
                IndexDoesNotExistException {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("categories", variables);

            try {
                callAPI(PUT_METHOD, indexUrl + CATEGORIES_URL, null, data,
                        privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void promote(String documentId, String query)
                throws IOException, IndexDoesNotExistException {
            if (null == documentId)
                throw new IllegalArgumentException("documentId can not be null");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("query", query);

            try {
                callAPI(PUT_METHOD, indexUrl + PROMOTE_URL, null, data,
                        privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void addFunction(Integer functionIndex, String definition)
                throws IOException, IndexDoesNotExistException,
                InvalidSyntaxException {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("definition", definition);

            try {
                callAPI(PUT_METHOD, indexUrl + FUNCTIONS_URL + "/"
                        + functionIndex, null, data, privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else if (e.getHttpCode() == 400) {
                    throw new InvalidSyntaxException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public void deleteFunction(Integer functionIndex) throws IOException,
                IndexDoesNotExistException {
            try {
                callAPI(DELETE_METHOD, indexUrl + FUNCTIONS_URL + "/"
                        + functionIndex, privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public Map<String, String> listFunctions()
                throws IndexDoesNotExistException, IOException {
            try {
                Map<String, Object> responseMap = (Map<String, Object>) callAPI(
                        GET_METHOD, indexUrl + FUNCTIONS_URL, privatePass);
                Map<String, String> result = new HashMap<String, String>();

                for (Entry<String, Object> entry : responseMap.entrySet()) {
                    result.put(entry.getKey(), (String) entry.getValue());
                }

                return result;
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public boolean exists() throws IOException {
            try {
                refreshMetadata();
                return true;
            } catch (IndexDoesNotExistException e) {
                return false;
            }
        }

        @Override
        public boolean hasStarted() throws IOException,
                IndexDoesNotExistException {
            refreshMetadata();

            return (Boolean) getMetadata().get("started");
        }

        @Override
        public String getCode() throws IOException, IndexDoesNotExistException {
            return (String) getMetadata().get("code");
        }

        @Override
        public Date getCreationTime() throws IOException,
                IndexDoesNotExistException {
            try {
                return ISO8601_PARSER.parse((String) getMetadata().get(
                        "creation_time"));
            } catch (ParseException e) {
                return null;
            }
        }

        @Override
        public void refreshMetadata() throws IOException,
                IndexDoesNotExistException {
            try {
                metadata = (Map<String, Object>) callAPI(GET_METHOD, indexUrl,
                        privatePass);
            } catch (HttpCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        @Override
        public Map<String, Object> getMetadata() throws IOException,
                IndexDoesNotExistException {
            if (metadata == null) {
                this.refreshMetadata();
            }

            return this.metadata;
        }
    }

    private final String apiUrl;
    private final String privatePass;

    public IndexTankClient(String apiUrl) {
        this.apiUrl = appendTrailingSlash(apiUrl);
        try {
            this.privatePass = new URL(apiUrl).getUserInfo();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Used for gae compat.
     * 
     * @param apiUrl
     * @param privatePass
     * @deprecated use {@link IndexTankClient#IndexTankClient(String)} instead
     */
    @Deprecated
    public IndexTankClient(String apiUrl, String privatePass) {
        this.apiUrl = appendTrailingSlash(apiUrl);
        this.privatePass = privatePass;
    }

    private static String appendTrailingSlash(String apiUrl) {
        if (!apiUrl.endsWith("/")) {
            apiUrl += "/";
        }
        return apiUrl;
    }

    @Override
    public IndexInterface getIndex(String indexName) {
        return new Index(getIndexUrl(indexName));
    }

    @Override
    public IndexInterface createIndex(String indexName) throws IOException,
            IndexAlreadyExistsException, MaximumIndexesExceededException {
        IndexInterface index = getIndex(indexName);
        index.create();
        return index;
    }

    @Override
    public void deleteIndex(String indexName) throws IOException,
            IndexDoesNotExistException {
        getIndex(indexName).delete();
    }

    @Override
    public List<IndexInterface> listIndexes() throws IOException {
        try {
            List<IndexInterface> result = new ArrayList<IndexInterface>();
            Map<String, Object> responseMap = (Map<String, Object>) callAPI(
                    GET_METHOD, getIndexesUrl(), privatePass);

            for (Entry<String, Object> entry : responseMap.entrySet()) {
                result.add(new Index(getIndexUrl(entry.getKey()),
                        (Map<String, Object>) entry.getValue()));
            }

            return result;
        } catch (HttpCodeException e) {
            throw new UnexpectedCodeException(e);
        }
    }

    private String getIndexUrl(String indexName) {
        return getIndexesUrl() + encodeIndexName(indexName);
    }

    private static String encodeIndexName(String indexName) {
        java.net.URI url;
        try {
            url = new java.net.URI("http", "none.com", "/" + indexName, null);
        } catch (URISyntaxException e) {
            return indexName;
        }
        return url.getRawPath().substring(1);
    }

    private String getIndexesUrl() {
        String indexesUrl = apiUrl + "v1/indexes/";
        return indexesUrl;
    }
}
