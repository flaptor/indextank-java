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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class IndexTankClient {
    
    private static final String GET_METHOD = "GET"; 
    private static final String PUT_METHOD = "PUT"; 
    private static final String DELETE_METHOD = "DELETE";
    
    private static final String SEARCH_URL = "/search";
    private static final String DOCS_URL = "/docs";
    private static final String CATEGORIES_URL = "/docs/categories";
    private static final String VARIABLES_URL = "/docs/variables";
    private static final String PROMOTE_URL = "/promote";
    private static final String FUNCTIONS_URL = "/functions";
    
    private static final DateFormat ISO8601_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"); 
    
    static class HttpCodeException extends Exception {
        /*
         * 400 = Invalid syntax
         * 401 = Auth failed
         * 404 = Index doesn't exist
         * 204 = Index already exists
         * 409 = Max number of indexes reached
         */
        int httpCode;
        
        public HttpCodeException(int httpCode, String message) {
            super(message);
            this.httpCode = httpCode;
        }
    }
    
    private static Object callAPI(String method, String urlString, Map<String, String> params, String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, params, (String)null, privatePass);
    }

    private static Object callAPI(String method, String urlString, String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, null, (String)null, privatePass);
    }
    
    
    private static Object callAPI(String method, String urlString, Map<String, String> params, Map<String, Object> data, String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, params, data == null ? null : JSONObject.toJSONString(data), privatePass);                
    }
    
    private static Object callAPI(String method, String urlString, Map<String, String> params, List<Map<String, Object>> data, String privatePass) throws IOException, HttpCodeException {
        return callAPI(method, urlString, params, data == null ? null : JSONArray.toJSONString(data), privatePass);             
    }

    private static Object callAPI(String method, String urlString, Map<String, String> params, String data, String privatePass) throws IOException, HttpCodeException {
        
        if (params!=null && !params.isEmpty()) {
            urlString += "?" + paramsToQueryString(params);
        }
        URL url = new URL(urlString);
        
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(privatePass.getBytes()));
        urlConnection.setRequestMethod(method);
        
        if (method.equals(PUT_METHOD) && data != null) {
            // write
            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
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
        
        in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        
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
     * A set of paginated sorted search results. The product of performing a 'search' call.
     * 
     * @author flaptor
     */
    public class SearchResults {
        public final long matches;
        public final float searchTime;
        public final List<Map<String, Object>> results;
        public final Map<String, Map<String, Integer>> facets;
        
        private SearchResults(Map<String, Object> response) {
            matches = (Long)response.get("matches"); 
            searchTime = Float.valueOf((String)response.get("search_time"));
            results = (List<Map<String, Object>>) response.get("results");
            facets = (Map<String, Map<String, Integer>>) response.get("facets");
        }
        
        @Override
        public String toString() {
            return "Matches: " + matches + "\nSearch Time: " + searchTime + "\nResults: " + results + "\nFacets: " + facets;
        }
    }
    
    private static class Range {
        private int id;
        private double floor;
        private double ceil;

        public Range(int id, double floor, double ceil) {
            this.id = id;
            this.floor = floor;
            this.ceil = ceil;
        }
    }
    
    public static class Query {
        private Integer start;
        private Integer length;
        private Integer scoringFunction;
        private List<String> snippetFields;
        private List<String> fetchFields;
        private Map<String, List<String>> categoryFilters;
        private List<Range> functionFilters;
        private List<Range> documentVariableFilters;
        
        private Map<Integer, Float> queryVariables;
        
        
        private String queryString;
        
        
        public static Query forString(String query) {
            return new Query(query);
        }
            
        private Query(String query) {
            this.queryString = query;
        }
        
        public Query withStart(Integer start) {
            this.start = start;
            return this;
        }
        
        public Query withLength(Integer length) {
            this.length = length;
            return this;
        }

        public Query withScoringFunction(Integer scoringFunction) {
            this.scoringFunction = scoringFunction;
            return this;
        }

        public Query withSnippetFields(List<String> snippetFields) {
            if (snippetFields == null) {
                throw new NullPointerException("snippetFields must be non-null");
            }
            
            if (this.snippetFields == null) {
                this.snippetFields = new ArrayList<String>();
            }
            
            this.snippetFields.addAll(snippetFields);
            
            return this;
        }
        
        public Query withSnippetFields(String ... snippetFields) {
            return withSnippetFields(Arrays.asList(snippetFields));
        }

        public Query withFetchFields(List<String> fetchFields) {
            if (fetchFields == null) {
                throw new NullPointerException("fetchFields must be non-null");
            }
        
            if (this.fetchFields == null) {
                this.fetchFields = new ArrayList<String>();
            }
            
            this.fetchFields.addAll(fetchFields);
            
            return this;
        }

        public Query withFetchFields(String ... fetchFields) {
            return withFetchFields(Arrays.asList(fetchFields));
        }
        
        public Query withDocumentVariableFilter(int variableIndex, double floor, double ceil) {
            if (documentVariableFilters == null) {
                documentVariableFilters = new ArrayList<Range>();
            }
            
            documentVariableFilters.add(new Range(variableIndex, floor, ceil));
            
            return this;
        }

        public Query withFunctionFilter(int functionIndex, double floor, double ceil) {
            if (functionFilters == null) {
                functionFilters = new ArrayList<Range>();
            }
            
            functionFilters.add(new Range(functionIndex, floor, ceil));
            
            return this;
        }
        
        public Query withCategoryFilters(Map<String, List<String>> categoryFilters) {
            if (categoryFilters == null) {
                throw new NullPointerException("categoryFilters must be non-null");
            }
            
            if (this.categoryFilters == null && !categoryFilters.isEmpty()) {
                this.categoryFilters = new HashMap<String, List<String>>();
            }
            if (!categoryFilters.isEmpty()) {
                this.categoryFilters.putAll(categoryFilters);
            }

            return this;
        }
        
        public Query withQueryVariables(Map<Integer, Float> queryVariables) {
            if (queryVariables == null) {
                throw new NullPointerException("queryVariables must be non-null");
            }

            if (this.queryVariables == null && !queryVariables.isEmpty()) {
                this.queryVariables = new HashMap<Integer, Float>();
            }
            
            if (!queryVariables.isEmpty()) {
                this.queryVariables.putAll(queryVariables);
            }
            
            return this;
        }
        
        public Query withQueryVariable(Integer name, Float value) {
            if (name == null || value == null) {
                throw new NullPointerException("Both name and value must be non-null");
            }
            
            if (this.queryVariables == null) {
                this.queryVariables = new HashMap<Integer, Float>();
            }
            
            this.queryVariables.put(name, value);
            
            return this;
        }
        
    }

    /**
     * A document to be added to the Index
     * 
     * @author flaptor
     *
     */
    public static class Document {
        /**
         * unique identifier
         */
        private String id;
        
        /**
         * fields
         */
        private Map<String, String> fields;
        
        /**
         * scoring variables
         */
        private Map<Integer, Float> variables;
        
        /**
         * faceting categories
         */
        private Map<String, String> categories;

        public Document(String id, Map<String, String> fields, Map<Integer, Float> variables,
                Map<String, String> categories) {
            if (id == null) throw new IllegalArgumentException("Id cannot be null");
            try {
                if (id.getBytes("UTF-8").length > 1024) throw new IllegalArgumentException("documentId can not be longer than 1024 bytes when UTF-8 encoded.");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Illegal documentId encoding.");
            }
            
            this.id = id;
            this.fields = fields;
            this.variables = variables;
            this.categories = categories;
        }
        
    }
    
    public static class BatchResults {
        private boolean hasErrors;
        private List<Boolean> results;
        private List<String> errors;
        private List<Document> documents;

        public BatchResults(List<Boolean> results, List<String> errors,
                List<Document> documents, boolean hasErrors) {
            this.results = results;
            this.errors = errors;
            this.documents = documents;
            this.hasErrors = hasErrors;
        }
        
        public boolean getResult(int position) {
            if (position >= results.size()) {
                throw new IllegalArgumentException("Position off bounds (" + position + ")");
            }
            
            return results.get(position);
        }
        
        public String getErrorMessage(int position) {
            if (position >= errors.size()) {
                throw new IllegalArgumentException("Position off bounds (" + position + ")");
            }
            
            return errors.get(position);
        }
        
        public Document getDocument(int position) {
            if (position >= documents.size()) {
                throw new IllegalArgumentException("Position off bounds (" + position + ")");
            }
            
            return documents.get(position);
        }
        
        /**
         * @return <code>true</code> if at least one of the documents failed to be indexed
         */
        public boolean hasErrors() {
            return hasErrors;
        }
        
        /**
         * @return an iterable with all the {@link Document}s that couldn't be indexed. It can be used to retrofeed the addDocuments method.
         */
        public Iterable<Document> getFailedDocuments() {
            return new Iterable<Document>() {
                @Override
                public Iterator<Document> iterator() {
                    return new Iterator<Document>() {
                        private Document next = computeNext();
                        private int position = 0;
                        
                        private Document computeNext() {
                            while (position < results.size() && results.get(position)) {
                                position++;
                            }
                            
                            if (position == results.size()) {
                                return null;
                            }
                            
                            Document next = documents.get(position);
                            position++;
                            return next;
                        }
                        
                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                        
                        @Override
                        public Document next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            
                            Document result = this.next;
                            this.next = computeNext();
                            return result;
                        }
                        
                        @Override
                        public boolean hasNext() {
                            return next != null;
                        }
                    };
                }
            };
        }
    }

    
    /**
     * Client to control a specific index.
     * 
     * @author flaptor
     *
     */
    public class Index {
        private final String indexUrl;
        private Map<String, Object> metadata;

        private Index(String indexUrl) {
            this.indexUrl = indexUrl;
        }
        
        private Index(String indexUrl, Map<String, Object> metadata) {
            this.indexUrl = indexUrl;
            this.metadata = metadata;
        }
        
        public SearchResults search(String query) throws IOException, InvalidSyntaxException {
            return search(Query.forString(query));
        }
        
        public SearchResults search(Query query) throws IOException, InvalidSyntaxException {
            Map<String, String> params = new HashMap<String, String>(); 
            
            if (query.start != null) params.put("start", query.start.toString());
            if (query.length != null) params.put("len", query.length.toString());
            if (query.scoringFunction != null) params.put("function", query.scoringFunction.toString());
            if (query.snippetFields != null) params.put("snippet", join(query.snippetFields, ","));
            if (query.fetchFields != null) params.put("fetch", join(query.fetchFields, ","));
            if (query.categoryFilters != null) params.put("category_filters", JSONObject.toJSONString(query.categoryFilters));
            
            if (query.documentVariableFilters != null) {
                for (Range range : query.documentVariableFilters) {
                    String key = "filter_docvar" + range.id;
                    String value = (range.floor == Double.NEGATIVE_INFINITY ? "*" : String.valueOf(range.floor)) + ":" + 
                            (range.ceil == Double.POSITIVE_INFINITY ? "*" : String.valueOf(range.ceil));
                    String param = params.get(key);
                    if (param == null) {
                        params.put(key, value);
                    } else {
                        params.put(key, param + "," + value);
                    }
                }
            }
             
            if (query.functionFilters != null) {
                for (Range range : query.functionFilters) {
                    String key = "filter_function" + range.id;
                    String value = (range.floor == Double.NEGATIVE_INFINITY ? "*" : String.valueOf(range.floor)) + ":" + 
                    (range.ceil == Double.POSITIVE_INFINITY ? "*" : String.valueOf(range.ceil));
                    String param = params.get(key);
                    if (param == null) {
                        params.put(key, value);
                    } else {
                        params.put(key, param + "," + value);
                    }
                }
            }

            if (query.queryVariables != null) {
                for (Entry<Integer, Float> entry : query.queryVariables.entrySet()) {
                    params.put("var" + entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            params.put("q", query.queryString);
            
            try {
                return new SearchResults((Map<String, Object>) callAPI(GET_METHOD, indexUrl + SEARCH_URL, params, privatePass));
            } catch (HttpCodeException e) {
                if (e.httpCode == 400) {
                    throw new InvalidSyntaxException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        /*public SearchResults search(String query, Integer start, Integer length, Integer scoringFunctionIndex, String[] snippetFields, String[] fetchFields, Map<String, List<String>> categoryFilters, Map<Integer, Float> queryVariables) throws IOException, InvalidSyntaxException {
            Map<String, String> params = new HashMap<String, String>(); 
            
            if (start != null) params.put("start", start.toString());
            if (length != null) params.put("len", length.toString());
            if (scoringFunctionIndex != null) params.put("function", scoringFunctionIndex.toString());
            if (snippetFields != null) params.put("snippet", join(Arrays.asList(snippetFields), ","));
            if (fetchFields != null) params.put("fetch", join(Arrays.asList(fetchFields), ","));
            if (categoryFilters != null) params.put("category_filters", JSONObject.toJSONString(categoryFilters));
            if (queryVariables != null) {
                for (Entry<Integer, Float> entry : queryVariables.entrySet()) {
                    params.put("var" + entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            params.put("q", query);
            
            try {
                return new SearchResults(callAPI(GET_METHOD, indexUrl + SEARCH_URL, params));
            } catch (HttpCodeException e) {
                if (e.httpCode == 400) {
                    throw new InvalidSyntaxException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }*/
        
        /**
         * Creates this index.
         *  
         * @throws IndexAlreadyExistsException If it already existed
         * @throws MaximumIndexesExceededException If the account has reached the limit 
         */
        public void create() throws IOException, IndexAlreadyExistsException, MaximumIndexesExceededException {
            try {
                callAPI(PUT_METHOD, indexUrl, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 204) {
                    throw new IndexAlreadyExistsException(e);
                } else if (e.httpCode == 409) {
                    throw new MaximumIndexesExceededException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }

        public void delete() throws IOException, IndexDoesNotExistException {
            try {
                callAPI(DELETE_METHOD, indexUrl, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }
        
        /**
         * Indexes a batch of documents
         * 
         * @param documents an iterable of {@link Document}s 
         * @return a {@link BatchResults} with the results information
         * @throws IOException
         * @throws IndexDoesNotExistException if the index name used to build the Index object does not match any index in the account
         * @throws UnexpectedCodeException if an error occurs serverside. This represents a temporary error and it SHOULD BE HANDLED if a retry policy is implemented.
         */
        public BatchResults addDocuments(Iterable<Document> documents)  throws IOException, IndexDoesNotExistException {
            List<Map<String, Object>> data = new ArrayList<Map<String,Object>>();
            
            for (Document document : documents) {
                Map<String, Object> documentMap = new HashMap<String, Object>();
                documentMap.put("docid", document.id);
                documentMap.put("fields", document.fields);
                if (document.variables != null) {
                    documentMap.put("variables", document.variables);
                }
                if (document.categories != null) {
                    documentMap.put("categories", document.categories);
                }
                
                data.add(documentMap);
            }
            
            try {
                List<Map<String, Object>> results = (List<Map<String, Object>>) callAPI(PUT_METHOD, indexUrl + DOCS_URL, null, data, privatePass);
                
                List<Boolean> addeds = new ArrayList<Boolean>();
                List<String> errors = new ArrayList<String>();
                boolean hasErrors = false;
                
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> result = results.get(i);
                    Boolean added = (Boolean) result.get("added");
                    
                    addeds.add(i, added);
                    
                    if (!added) {
                        hasErrors = true;
                        errors.add(i, (String)result.get("error"));
                    }
                }
                
                ArrayList<Document> documentsList = new ArrayList<Document>();
                
                for (Document document : documents) {
                    documentsList.add(document);
                }
                
                return new BatchResults(addeds, errors, documentsList, hasErrors);
                
            } catch (HttpCodeException e) {
                if (e.httpCode == 400) {
                    throw new IllegalArgumentException(e);
                } else if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
            
        }
        
        public void addDocument(String documentId, Map<String, String> fields) throws IOException, IndexDoesNotExistException {
            addDocument(documentId, fields, null);
        }

        /**
         * Indexes a document for the given docid and fields.
         *  
         * @param documentId unique document identifier. Can't be longer than 1024 bytes when UTF-8 encoded. Never {@code null}.
         * @param fields map with the document fields
         * @param variables map integer -&gt; float with values for variables that can later be used in scoring functions during searches.
         * @throws IOException 
         * @throws IndexDoesNotExistException if the index name used to build the Index object does not match any index in the account
         * @throws UnexpectedCodeException if an error occurs serverside. This represents a temporary error and it SHOULD BE HANDLED if a retry policy is implemented.
         */
        public void addDocument(String documentId, Map<String, String> fields, Map<Integer, Float> variables) throws IOException, IndexDoesNotExistException {
            if (null == documentId) throw new IllegalArgumentException("documentId can not be null.");
            if (documentId.getBytes("UTF-8").length > 1024) throw new IllegalArgumentException("documentId can not be longer than 1024 bytes when UTF-8 encoded.");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("fields", fields);
            if (variables != null) {
                data.put("variables", variables);
            }
            
            try {
                callAPI(PUT_METHOD, indexUrl + DOCS_URL, null, data, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 400) {
                    // Should throw InvalidArgument, but it breaks backward compatibility.
                    throw new UnexpectedCodeException(e);
                    //throw new InvalidArgumentException(e);
                } else if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }
        
        /**
         * Deletes the given docid from the index if it existed. Otherwise, does nothing.
         * 
         * @param documentId unique document identifier. Never {@code null}.
         * @throws IOException 
         * @throws IndexDoesNotExistException 
         * @throws UnexpectedCodeException
         */
        public void deleteDocument(String documentId) throws IOException, IndexDoesNotExistException {
            if (null == documentId) throw new IllegalArgumentException("documentId can not be null");
            Map<String, String> params = new HashMap<String, String>();
            params.put("docid", documentId);
            
            try {
                callAPI(DELETE_METHOD, indexUrl + DOCS_URL, params, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }
        
        /**
         * Updates the variables of the document for the given docid.
         * 
         * @param documentId unique document identifier. Never {@code null}.
         * @param variables map integer -&gt; float with values for variables that can later be used in scoring functions during searches.
         * @throws IOException
         * @throws IndexDoesNotExistException
     * @throws UnexpectedCodeException
         */
        public void updateVariables(String documentId, Map<Integer, Float> variables) throws IOException, IndexDoesNotExistException {
            if (null == documentId) throw new IllegalArgumentException("documentId can not be null");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("variables", variables);

            try {
                callAPI(PUT_METHOD, indexUrl + VARIABLES_URL, null, data, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }           
        }

        /**
         * Updates the categories (for faceting purposes) of the document for the given docid.
         * 
         * @param documentId unique document identifier. Never {@code null}.
         * @param categories map string -&gt; string with the values of this document for each category. 
         *                   A blank value equals to removing the category for the document. 
         * @throws IOException
         * @throws IndexDoesNotExistException
     * @throws UnexpectedCodeException
         */
        public void updateCategories(String documentId, Map<String, String> variables) throws IOException, IndexDoesNotExistException {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("categories", variables);

            try {
                callAPI(PUT_METHOD, indexUrl + CATEGORIES_URL, null, data, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }           
        }
        public void promote(String documentId, String query) throws IOException, IndexDoesNotExistException {
            if (null == documentId) throw new IllegalArgumentException("documentId can not be null");
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("docid", documentId);
            data.put("query", query);
            
            try {
                callAPI(PUT_METHOD, indexUrl + PROMOTE_URL, null, data, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }           
        }
        
        public void addFunction(Integer functionIndex, String definition) throws IOException, IndexDoesNotExistException, InvalidSyntaxException {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("definition", definition);
            
            try {
                callAPI(PUT_METHOD, indexUrl + FUNCTIONS_URL + "/" + functionIndex, null, data, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else if (e.httpCode == 400) {
                    throw new InvalidSyntaxException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }           
        }
        
        public void deleteFunction(Integer functionIndex) throws IOException, IndexDoesNotExistException {
            try {
                callAPI(DELETE_METHOD, indexUrl + FUNCTIONS_URL + "/" + functionIndex, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }
        
        public Map<String, String> listFunctions() throws IndexDoesNotExistException, IOException {
            try {
                Map<String, Object> responseMap = (Map<String, Object>) callAPI(GET_METHOD, indexUrl + FUNCTIONS_URL, privatePass);
                Map<String, String> result = new HashMap<String, String>();
                
                for (Entry<String, Object> entry : responseMap.entrySet()) {
                    result.put(entry.getKey(), (String)entry.getValue());
                }
                
                return result;
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }
        
        /**
         * Returns whether an index for the name of this instance
         * exists. If it doesn't, it can be created byAlready calling
         * {@link #create()}.
         * 
         * @return true if the index exists
         */
        public boolean exists() throws IOException {
            try {
                refreshMetadata();
                return true;
            } catch (IndexDoesNotExistException e) {
                return false;
            }
        }
        
        /**
         * Returns whether this index is responsive. Newly created
         * indexes can take a little while to get started.
         * 
         * If this method returns False most methods in this class
         * will raise an HttpException with a status of 503.
         * 
         * @return true if the Index has already started
         * @throws IndexDoesNotExistException 
         * @throws IOException 
         */
        public boolean hasStarted() throws IOException, IndexDoesNotExistException {
            refreshMetadata();
            
            return (Boolean)getMetadata().get("started");
        }
        
        public String getCode() throws IOException, IndexDoesNotExistException {
            return (String) getMetadata().get("code");
        }
        
        public Date getCreationTime() throws IOException, IndexDoesNotExistException {
            try {
                return ISO8601_PARSER.parse((String)getMetadata().get("creation_time"));
            } catch (ParseException e) {
                return null;
            }
        }
        
        public void refreshMetadata() throws IOException, IndexDoesNotExistException {
            try {
                metadata = (Map<String, Object>) callAPI(GET_METHOD, indexUrl, privatePass);
            } catch (HttpCodeException e) {
                if (e.httpCode == 404) {
                    throw new IndexDoesNotExistException(e);
                } else {
                    throw new UnexpectedCodeException(e);
                }
            }
        }
        
        public Map<String, Object> getMetadata() throws IOException, IndexDoesNotExistException {
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
     * @param apiUrl
     * @param privatePass
     */
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
    
    public Index getIndex(String indexName) {
        return new Index(getIndexUrl(indexName)); 
    }
    
    public Index createIndex(String indexName) throws IOException, IndexAlreadyExistsException, MaximumIndexesExceededException {
        Index index = getIndex(indexName);
        index.create();
        return index;
    }
    
    public void deleteIndex(String indexName) throws IOException, IndexDoesNotExistException {
        getIndex(indexName).delete();
    }
    
    public static String join(Collection s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
    
    public List<Index> listIndexes() throws IOException {
        try {
            List<Index> result = new ArrayList<Index>();
            Map<String, Object> responseMap = (Map<String, Object>) callAPI(GET_METHOD, getIndexesUrl(), privatePass);
            
            for (Entry<String, Object> entry : responseMap.entrySet()) {
                result.add(new Index(getIndexUrl(entry.getKey()), (Map<String, Object>) entry.getValue()));
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
