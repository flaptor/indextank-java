package com.flaptor.indextank.apiclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.json.simple.JSONObject;


public interface ApiClient {

    public class HttpCodeException extends Exception {
        /**
         * 400 = Invalid syntax<br>
         * 401 = Auth failed<br>
         * 404 = Index doesn't exist<br>
         * 204 = Index already exists<br>
         * 409 = Max number of indexes reached<br>
         */
        protected int httpCode;

        public HttpCodeException(int httpCode, String message) {
            super(message);
            this.httpCode = httpCode;
        }

        public int getHttpCode() {
            return httpCode;
        }
    }

    /**
     * Aggregation of the outcome of indexing every document in the batch.
     * 
     * @author flaptor
     * 
     */
    public class BatchResults {
        private boolean hasErrors;
        private List<Boolean> results;
        private List<String> errors;
        private List<ApiClient.Document> documents;

        public BatchResults(List<Boolean> results, List<String> errors,
                List<ApiClient.Document> documents, boolean hasErrors) {
            this.results = results;
            this.errors = errors;
            this.documents = documents;
            this.hasErrors = hasErrors;
        }

        public boolean getResult(int position) {
            if (position >= results.size()) {
                throw new IllegalArgumentException("Position off bounds ("
                        + position + ")");
            }

            return results.get(position);
        }

        /**
         * Get the error message for a specific position. Will be null if
         * getResult(position) is false.
         * 
         * @param position
         * @return the error message
         */
        public String getErrorMessage(int position) {
            if (position >= errors.size()) {
                throw new IllegalArgumentException("Position off bounds ("
                        + position + ")");
            }

            return errors.get(position);
        }

        public ApiClient.Document getDocument(int position) {
            if (position >= documents.size()) {
                throw new IllegalArgumentException("Position off bounds ("
                        + position + ")");
            }

            return documents.get(position);
        }

        /**
         * @return <code>true</code> if at least one of the documents failed to
         *         be indexed
         */
        public boolean hasErrors() {
            return hasErrors;
        }

        /**
         * @return an iterable with all the {@link ApiClient.Document}s
         *         that couldn't be indexed. It can be used to retrofeed the
         *         addDocuments method.
         */
        public Iterable<ApiClient.Document> getFailedDocuments() {
            return new Iterable<ApiClient.Document>() {
                @Override
                public Iterator<ApiClient.Document> iterator() {
                    return new Iterator<ApiClient.Document>() {
                        private ApiClient.Document next = computeNext();
                        private int position = 0;

                        private ApiClient.Document computeNext() {
                            while (position < results.size()
                                    && results.get(position)) {
                                position++;
                            }

                            if (position == results.size()) {
                                return null;
                            }

                            ApiClient.Document next = documents
                                    .get(position);
                            position++;
                            return next;
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public ApiClient.Document next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }

                            ApiClient.Document result = this.next;
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
     * A document to be added to the Index
     * 
     * @author flaptor
     * 
     */
    public class Document {
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

        public Map<String, Object> toDocumentMap() {
            Map<String, Object> documentMap = new HashMap<String, Object>();
            documentMap.put("docid", id);
            documentMap.put("fields", fields);
            if (variables != null) {
                documentMap.put("variables", variables);
            }
            if (categories != null) {
                documentMap.put("categories", categories);
            }
            return documentMap;
        }

        public Document(String id, Map<String, String> fields,
                Map<Integer, Float> variables, Map<String, String> categories) {
            if (id == null)
                throw new IllegalArgumentException("Id cannot be null");
            try {
                if (id.getBytes("UTF-8").length > 1024)
                    throw new IllegalArgumentException(
                            "documentId can not be longer than 1024 bytes when UTF-8 encoded.");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(
                        "Illegal documentId encoding.");
            }

            this.id = id;
            this.fields = fields;
            this.variables = variables;
            this.categories = categories;
        }

    }

    /**
     * A set of paginated sorted search results. The product of performing a
     * 'search' call.
     * 
     * @author flaptor
     */
    public class SearchResults {
        public final long matches;
        public final float searchTime;
        public final List<Map<String, Object>> results;
        public final Map<String, Map<String, Integer>> facets;

        public SearchResults(Map<String, Object> response) {
            matches = (Long) response.get("matches");
            searchTime = Float.valueOf((String) response.get("search_time"));
            results = (List<Map<String, Object>>) response.get("results");
            facets = (Map<String, Map<String, Integer>>) response.get("facets");
        }

        @Override
        public String toString() {
            return "Matches: " + matches + "\nSearch Time: " + searchTime
                    + "\nResults: " + results + "\nFacets: " + facets;
        }
    }

    public class Query {
        public class Range {
            protected int id;
            protected double floor;
            protected double ceil;
    
            public Range(int id, double floor, double ceil) {
                this.id = id;
                this.floor = floor;
                this.ceil = ceil;
            }
            
            public String getFilterDocvar() {
                return "filter_docvar" + id;
            }
            
            public String getFilterFunction() {
                return "filter_function" + id;
            }
    
            public String getValue() {
                return (floor == Double.NEGATIVE_INFINITY ? "*"
                        : String.valueOf(floor))
                        + ":"
                        + (ceil == Double.POSITIVE_INFINITY ? "*"
                                : String.valueOf(ceil));
            }
            
        }
    
        protected Integer start;
        protected Integer length;
        protected Integer scoringFunction;
        protected List<String> snippetFields;
        protected List<String> fetchFields;
        protected Map<String, List<String>> categoryFilters;
        protected List<Range> functionFilters;
        protected List<Range> documentVariableFilters;
        protected Map<Integer, Float> queryVariables;
        protected String queryString;
    
        public static Query forString(String query) {
            return new Query(query);
        }
    
        protected Query(String query) {
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
    
        public Query withSnippetFields(String... snippetFields) {
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
    
        public Query withFetchFields(String... fetchFields) {
            return withFetchFields(Arrays.asList(fetchFields));
        }
    
        public Query withDocumentVariableFilter(int variableIndex, double floor,
                double ceil) {
            if (documentVariableFilters == null) {
                this.documentVariableFilters = new ArrayList<Range>();
            }
    
            documentVariableFilters.add(new Range(variableIndex, floor, ceil));
    
            return this;
        }
    
        public Query withFunctionFilter(int functionIndex, double floor, double ceil) {
            if (functionFilters == null) {
                this.functionFilters = new ArrayList<Range>();
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
                throw new NullPointerException(
                        "Both name and value must be non-null");
            }
    
            if (this.queryVariables == null) {
                this.queryVariables = new HashMap<Integer, Float>();
            }
    
            this.queryVariables.put(name, value);
    
            return this;
        }
    
        Map<String, String> toParameterMap() {
            Map<String, String> params = new HashMap<String, String>();
    
            if (start != null)
                params.put("start", start.toString());
            if (length != null)
                params.put("len", length.toString());
            if (scoringFunction != null)
                params.put("function", scoringFunction.toString());
            if (snippetFields != null)
                params.put("snippet", join(snippetFields, ","));
            if (fetchFields != null)
                params.put("fetch", join(fetchFields, ","));
            if (categoryFilters != null)
                params.put("category_filters",
                        JSONObject.toJSONString(categoryFilters));
    
            if (documentVariableFilters != null) {
                for (Range range : documentVariableFilters) {
                    String key = "filter_docvar" + range.id;
                    String value = (range.floor == Double.NEGATIVE_INFINITY ? "*"
                            : String.valueOf(range.floor))
                            + ":"
                            + (range.ceil == Double.POSITIVE_INFINITY ? "*"
                                    : String.valueOf(range.ceil));
                    String param = params.get(key);
                    if (param == null) {
                        params.put(key, value);
                    } else {
                        params.put(key, param + "," + value);
                    }
                }
            }
    
            if (functionFilters != null) {
                for (Range range : functionFilters) {
                    String key = "filter_function" + range.id;
                    String value = (range.floor == Double.NEGATIVE_INFINITY ? "*"
                            : String.valueOf(range.floor))
                            + ":"
                            + (range.ceil == Double.POSITIVE_INFINITY ? "*"
                                    : String.valueOf(range.ceil));
                    String param = params.get(key);
                    if (param == null) {
                        params.put(key, value);
                    } else {
                        params.put(key, param + "," + value);
                    }
                }
            }
    
            if (queryVariables != null) {
                for (Entry<Integer, Float> entry : queryVariables.entrySet()) {
                    params.put("var" + entry.getKey(),
                            String.valueOf(entry.getValue()));
                }
            }
    
            params.put("q", queryString);
    
            return params;
        }

        public static String join(Iterable<String> s, String delimiter) {
            StringBuilder buffer = new StringBuilder();
            Iterator<String> iter = s.iterator();
            while (iter.hasNext()) {
                buffer.append(iter.next());
                if (iter.hasNext()) {
                    buffer.append(delimiter);
                }
            }
            return buffer.toString();
        }
    
    }

    Index getIndex(String indexName);

    Index createIndex(String indexName) throws IOException,
            IndexAlreadyExistsException, MaximumIndexesExceededException;

    void deleteIndex(String indexName) throws IOException,
            IndexDoesNotExistException;

    List<? extends Index> listIndexes() throws IOException;

}