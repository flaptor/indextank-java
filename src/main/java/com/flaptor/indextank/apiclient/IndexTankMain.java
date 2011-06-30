package com.flaptor.indextank.apiclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.flaptor.indextank.apiclient.IndexTankClient.Index;
import com.flaptor.indextank.apiclient.IndexTankClient.Query;
import com.flaptor.indextank.apiclient.IndexTankClient.SearchResults;

public class IndexTankMain {

    private static final String INDEX_NAME = "index";
    private static final String APIURL = "apiurl";
    private static final String INDEXTANK_CONFIG_FILENAME = "indextank.config";

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            if ("config".equals(args[0])) {
                if (args.length != 3){
                    printUsage();
                    System.exit(1);
                }
                config(args[1], args[2]);
            } else if ("search".equals(args[0])) {
                if (args.length < 2 || args.length > 4){
                    printUsage();
                    System.exit(1);
                }
                String query =  args[1];
                int start = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                int length = args.length > 3 ? Integer.parseInt(args[3]) : 10;
                
                search(query, start, length);
                
            } else if ("index".equals(args[0])) {
                try {
                    if (args.length != 3) {
                        printUsage();
                        System.exit(1);
                    }
                    JSONParser parser = new JSONParser();
                    Map<String, String> fields = (Map<String, String>) parser.parse(args[2]);
                    index(args[1], fields);
                } catch (ParseException e) {
                    System.err.println("Invalid json map.");
                    e.printStackTrace();
                    System.exit(1);
                }
                
            } else {
                printUsage();
                System.exit(1);
            }
        } else {
            printUsage();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage options:\n");
        System.out.println("java -jar indextank.jar [config|index|search] options");
        System.out.println("config options: <private_apiurl> <index_name>");
        System.out.println("index options: <doc_id> <fields>");
        System.out.println("search options: <query> [start] [length]");
        System.out.println();
        System.out.println("fields: a json map with fields for this document");
        System.out.println("start,length: optional parameters");
    }

    private static void config(String apiUrl, String indexName) {
        IndexTankClient client = new IndexTankClient(apiUrl);
        Index index = client.getIndex(indexName);
        
        try {
            System.out.println("Creating index " + indexName);
            if (index.exists()) {
                System.out.println("You already have an index called " + indexName + "." +
                		"\nThis process will delete this index and all your documents will be lost." +
                		"\nDo you want to continue (Y/n)?");
                char response = (char)System.in.read();
                if (response != 'y' && response != 'Y' && response != '\n') {
                    System.out.println("Aborting");
                    System.exit(1);
                }
                
                index.delete();
            }

            index.create();
            System.out.println("Index created.");
            System.out.println("Saving configuration to indextank.config file");
            
            File configFile = new File(INDEXTANK_CONFIG_FILENAME);
            if (configFile.exists())
                configFile.delete();
            
            boolean created = configFile.createNewFile();
            if (!created) {
                System.err.println("Couldn't create config file");
                System.exit(1);
            }
            
            Properties prop = new Properties();
            prop.setProperty(APIURL, apiUrl);
            prop.setProperty(INDEX_NAME, indexName);
            Writer writer = new FileWriter(configFile);
            prop.store(writer, "IndexTank configuration. Do not remove this file if you want to try search and index options");
            writer.close();
            
            System.out.println("Configuration successfully finished.");
            
        } catch (IOException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (IndexDoesNotExistException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (IndexAlreadyExistsException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (MaximumIndexesExceededException e) {
            System.err.println("Max quantity of index reached. Please delete any index in your account to continue with this configuration.");
        }
    }

    private static void search(String query, int start, int length) {

        System.out.println("Reading configuration file");
        File configFile = new File(INDEXTANK_CONFIG_FILENAME);
        if (!configFile.exists() || !configFile.canRead()) {
            System.err.println("File " + INDEXTANK_CONFIG_FILENAME + "doesn't exist or can't be read.");
            System.err.println("Aborting");
            System.exit(1);
        }
        
        try {
            
            Properties prop = new Properties();
            Reader reader = new FileReader(configFile);
            prop.load(reader);
            reader.close();
            
            IndexTankClient client = new IndexTankClient(prop.getProperty(APIURL));
            Index index = client.getIndex(prop.getProperty(INDEX_NAME));
            
            if (!index.exists()) {
                System.err.println("Index " + prop.getProperty(INDEX_NAME) + " doesn't exist. Please run 'config' option first");
                System.exit(1);
            }
            
            System.out.println("Searching for:\n  query: " + query + "\n  start:" + start + "\n  length:" + length);
            SearchResults results = index.search(Query.forString(query).withStart(start).withLength(length));
            System.out.println(results);

        } catch (FileNotFoundException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (InvalidSyntaxException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        }
    }

    private static void index(String docid, Map<String, String> fields) {
        System.out.println("Reading configuration file");
        File configFile = new File(INDEXTANK_CONFIG_FILENAME);
        if (!configFile.exists() || !configFile.canRead()) {
            System.err.println("File " + INDEXTANK_CONFIG_FILENAME + "doesn't exist or can't be read.");
            System.err.println("Aborting");
            System.exit(1);
        }

        try {
            
            Properties prop = new Properties();
            Reader reader = new FileReader(configFile);
            prop.load(reader);
            reader.close();
            
            IndexTankClient client = new IndexTankClient(prop.getProperty(APIURL));
            Index index = client.getIndex(prop.getProperty(INDEX_NAME));
            
            if (!index.exists()) {
                System.err.println("Index " + prop.getProperty(INDEX_NAME) + " doesn't exist. Please run 'config' option first");
                System.exit(1);
            }
            
            System.out.println("Indexing document:\n  id: " + docid + "\n  fields:" + fields);
            index.addDocument(docid, fields);
            System.out.println("Document indexed successfully.");

        } catch (FileNotFoundException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        } catch (IndexDoesNotExistException e) {
            System.err.println("Unexpected exception");
            e.printStackTrace();
        }
    }
}
