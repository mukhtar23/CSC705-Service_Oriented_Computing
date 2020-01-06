package controllers;

// Drool import
import plugins.Drools;

import models.Bank;
import models.TransactionRequest;

import play.mvc.*;
import java.util.*;
import java.io.*;
import com.google.inject.Inject;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.kie.api.runtime.rule.FactHandle;

// Openllet import
import openllet.jena.PelletReasonerFactory;

// Jena imports
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.reasoner.*;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;


// File reading and validation printing imports
import java.io.InputStream;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    // Project 3 variables
    private final String SUCCESS = "success";
    private final String FAILURE = "failure";

    final String sourceFile = "project3.owl";
    final String sourceUrl = "http://www.semanticweb.org/mukht/ontologies/2019/9/soc_project3";
    final String NS = sourceUrl + "#";
    
    private OntModel baseOntology;
    private OntModel ontReasoned;

    // Project 4 variables
    @Inject
    Drools drools;

    private HashMap<String, Bank> banks;
    private HashMap<String, TransactionRequest> transactionRequests;

    public HomeController(){
        loadOntology();
        banks = new HashMap<>();
        transactionRequests = new HashMap<>();
        try{
            createLogs();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

    /////////////////////////
    // Project 3 Methods
    ////////////////////////

    /**
     * Loads the ontology with a reasoner
     */
    public void loadOntology(){
        // Read the ontology. No reasoner yet.
        baseOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        try
            {
            InputStream in = FileManager.get().open(sourceFile);
            try
                {
                baseOntology.read(in, null);
                }
            catch (Exception e)
                {
                e.printStackTrace();
                System.exit(0);
                }
            }
        catch (JenaException je)
            {
            System.err.println("ERROR" + je.getMessage());
            je.printStackTrace();
            System.exit(0);
            }

        baseOntology.setNsPrefix( "csc750", NS ); // Just for compact printing; doesn't really matter


        // This will create an ontology that has a reasoner attached.
        // This means that it will automatically infer classes an individual belongs to, according to restrictions, etc.
        ontReasoned = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, baseOntology);

        // print validation report 
        final ValidityReport report = ontReasoned.validate();
        printIterator(report.getReports(), "Validation Results");

        printOntology();
    }

   /**
    * This helper method is used to print the validation results
    * It is from the Openllet example here: https://github.com/Galigator/openllet/blob/integration/examples/src/main/java/openllet/examples/JenaReasoner.java
    */
    public static void printIterator(final Iterator<?> i, final String header)
    {
        System.out.println(header);
        for (int c = 0; c < header.length(); c++)
        System.out.print("=");
        System.out.println();

        if (i.hasNext())
        while (i.hasNext())
            System.out.println(i.next());
        else
        System.out.println("<EMPTY>");

        System.out.println();
    }

    /**
     * Prints all the classes and individuals in the ontology
     */
    public void printOntology() {
        ExtendedIterator classes = ontReasoned.listClasses();

        System.out.println("Printing all classes and individuals");
        System.out.println("-------------------------------------");
        while (classes.hasNext())
        {
            OntClass thisClass = (OntClass) classes.next();
            if (thisClass.toString().contains("http")) {
                System.out.println("Found class: " + thisClass.toString());
                ExtendedIterator instances = thisClass.listInstances();
                while (instances.hasNext()) {
                    Individual thisInstance = (Individual) instances.next();
                    System.out.println("  Found instance: " + thisInstance.toString());
                }
            }
        }
        System.out.println("-------------------------------------");
        System.out.println();
    }

    /**
     * Add an individual to Merchant class
     */
    public Result addMerchant(String id){
        // First, get the Merchant class
        OntClass merchantClass = ontReasoned.getOntClass(NS + "Merchant");
        OntClass transaction = ontReasoned.getOntClass(NS + "Transaction");
        OntProperty participates = ontReasoned.getObjectProperty(NS + "participates");


        // Add Merchant to model
        Individual merchant = ontReasoned.createIndividual(NS + id, merchantClass);
    
        printOntology();

        ObjectNode response = Json.newObject();

        response.put("status", SUCCESS);

        return ok(response);
    }

    /**
     * Add an individual to Consumer class
     */
    public Result addConsumer(String id){
        // First, get the Consumer class
        OntClass consumerClass = ontReasoned.getOntClass(NS + "Consumer");
        OntClass transaction = ontReasoned.getOntClass(NS + "Transaction");
        OntProperty participates = ontReasoned.getObjectProperty(NS + "participates");

        // Add Consumer to model
        Individual consumer = ontReasoned.createIndividual(NS + id, consumerClass);
    
        printOntology();
        
        ObjectNode response = Json.newObject();

        response.put("status", SUCCESS);

        return ok(response);
    }

    /**
     * Add an individual to Transaction class
     */
    public Result addTransaction(String senderId, String receiverId, String id){
        // First, get the Transaction class
        OntClass transactionClass = ontReasoned.getOntClass(NS + "Transaction");

        // Get properties
        OntProperty hasSender = ontReasoned.getObjectProperty(NS + "has_sender");
        OntProperty hasReceiver = ontReasoned.getObjectProperty(NS + "has_receiver");

        // Get sender and receiver properties
        Individual sender = ontReasoned.getIndividual(NS + senderId);
        Individual receiver = ontReasoned.getIndividual(NS + receiverId);
        
        // Add Transaction to model
        Individual transaction = ontReasoned.createIndividual(NS + id, transactionClass);
        
        ObjectNode response = Json.newObject();

        if(sender != null && receiver != null){
            // Now add the properties
            transaction.addProperty(hasSender, sender);
            transaction.addProperty(hasReceiver, receiver);

            response.put("status", SUCCESS);

        }else if(sender != null && receiver == null){
            response.put("status", FAILURE);
            response.put("reason", "Receiver is not a Merchant/Consumer");

        }else if(sender == null && receiver != null){
            response.put("status", FAILURE);
            response.put("reason", "Sender is not a Merchant/Consumer");

        }else if(sender == null && receiver == null){
            response.put("status", FAILURE);
            response.put("reason", "Both Sender and Receiver are not a Merchant/Consumer");

        }

        printOntology();

        return ok(response);
    }

    /**
     * Checks if the given transaction is a commerical transaction
     */
    public Result isCommercial(String id){
        
        Boolean isCommercial = null;
       
        // First, get the Commercial class
        OntClass commercialClass = ontReasoned.getOntClass(NS + "Commercial_Transaction");

        // Get Transaction by id
        Individual transaction = ontReasoned.getIndividual(NS + id);

        if(transaction != null){
            isCommercial = transaction.hasOntClass(commercialClass);
        }

        printOntology();
        
        ObjectNode response = Json.newObject();

        if(isCommercial != null){
            response.put("status", SUCCESS);
            response.put("result", isCommercial);
        }else{
            response.put("status", FAILURE);
            response.put("reason", "not a transaction");
        }

        return ok(response);
    }

    /**
     * Checks if the given transaction is a peronsal transaction
     */
    public Result isPersonal(String id){
        
        Boolean isPersonal = null;
       
        // First, get the Personal transaction class
        OntClass personalClass = ontReasoned.getOntClass(NS + "Personal_Transaction");

        // Get Transaction by id
        Individual transaction = ontReasoned.getIndividual(NS + id);

        if(transaction != null){
            isPersonal = transaction.hasOntClass(personalClass);
        }

        printOntology();
        
        ObjectNode response = Json.newObject();

        if(isPersonal != null){
            response.put("status", SUCCESS);
            response.put("result", isPersonal);
        }else{
            response.put("status", FAILURE);
            response.put("reason", "not a transaction");
        }

        return ok(response);
    }

    /**
     * Checks if the given transaction is a purchase transaction
     */
    public Result isPurchase(String id){
        
        Boolean isPurchase = null;
       
        // First, get the purchase transaction class
        OntClass purchaseClass = ontReasoned.getOntClass(NS + "Purchase_Transaction");

        // Get Transaction by id
        Individual transaction = ontReasoned.getIndividual(NS + id);

        if(transaction != null){
            isPurchase = transaction.hasOntClass(purchaseClass);
        }

        printOntology();
        
        ObjectNode response = Json.newObject();

        if(isPurchase != null){
            response.put("status", SUCCESS);
            response.put("result", isPurchase);
        }else{
            response.put("status", FAILURE);
            response.put("reason", "not a transaction");
        }

        return ok(response);
    }

    /**
     * Checks if the given transaction is a refund transaction
     */
    public Result isRefund(String id){
        
        Boolean isRefund = null;
       
        // First, get the refund transaction class
        OntClass refundClass = ontReasoned.getOntClass(NS + "Refund_Transaction");

        // Get Transaction by id
        Individual transaction = ontReasoned.getIndividual(NS + id);

        if(transaction != null){
            isRefund = transaction.hasOntClass(refundClass);
        }

        printOntology();
        
        ObjectNode response = Json.newObject();

        if(isRefund != null){
            response.put("status", SUCCESS);
            response.put("result", isRefund);
        }else{
            response.put("status", FAILURE);
            response.put("reason", "not a transaction");
        }

        return ok(response);
    }

    /**
     * Checks if the given merchant is Trusted
     */
    public Result isTrusted(String id){
        
        Boolean isTrusted = null;
       
        // First, get the Trusted class
        OntClass trustedClass = ontReasoned.getOntClass(NS + "Trusted");

        // Get Merchant by id
        Individual merchant = ontReasoned.getIndividual(NS + id);

        if(merchant != null){
            isTrusted = merchant.hasOntClass(trustedClass);
        }

        printOntology();
        
        ObjectNode response = Json.newObject();

        if(isTrusted != null){
            response.put("status", SUCCESS);
            response.put("result", isTrusted);
        }else{
            response.put("status", FAILURE);
            response.put("reason", "not a merchant");
        }

        return ok(response);
    }

    /**
     * Resets the ontology or deletes all the individuals in the ontology 
     * so we can start over
     */
    public Result reset(){
        // resets ontology
        System.out.println();
        System.out.println("RESETTING ONTOLOGY");
        System.out.println();
        loadOntology();

        ////////////////////////
        // Project 4 additions
        ///////////////////////

        // clear hashmaps
        banks.clear();
        transactionRequests.clear();

        // clear droolsSession
        for(FactHandle h: drools.kieSession.getFactHandles()){
            drools.kieSession.retract(h);
        }

        // clear logs
        try{
            createLogs();
        }catch(Exception e){
            e.printStackTrace();
        }
        //////////////////////////////

        ObjectNode response = Json.newObject();

        response.put("result", SUCCESS);

        return ok(response);
    }

    /////////////////////////////
    //  Project 4 Methods
    /////////////////////////////

    /**
     * Adds a Bank with nationality and ID
     */
    public Result addBank(String nationality, String id){
        Bank b = new Bank(id, nationality);
        banks.put(id, b);

        drools.kieSession.insert(b);

        System.out.println("Banks: " + banks);
        System.out.println();
        
        ObjectNode response = Json.newObject();
        response.put("status", SUCCESS);
        return ok(response);
    }

    /**
     * Adds a transactionrequest 
     */
    public Result addTransactionRequest(String senderId, String receiverId, String bankId, String category, int amount, String transactionRequestId){
        TransactionRequest t = new TransactionRequest(senderId, receiverId, bankId, category, amount, transactionRequestId, ontReasoned);
        transactionRequests.put(transactionRequestId, t);

        FactHandle tFactHandle = drools.kieSession.insert(t);
        drools.kieSession.fireAllRules();

        System.out.println("Transaction Requests: " + transactionRequests);
        System.out.println();

        Bank b = banks.get(t.getBankId());

        ObjectNode response = Json.newObject();

        if (t.isApproved()) {
            // Update so rules don't get run repeatedly
            t.setProcessed(true);
            drools.kieSession.update(tFactHandle, t);

            //  First, get the classes we need
            OntClass transactionClass = ontReasoned.getOntClass(NS + "Transaction");

            // Get the properties we need
            OntProperty hasSender = ontReasoned.getObjectProperty(NS + "has_sender");
            OntProperty hasReceiver = ontReasoned.getObjectProperty(NS + "has_receiver");

            // Get existing individuals
            Individual sender = ontReasoned.getIndividual( NS + senderId);
            Individual receiver = ontReasoned.getIndividual(NS + receiverId);

            // Update bank
            b.setTotalApproved(b.getTotalApproved() + 1);
            b.setAvgAmount((b.getAvgAmount() + t.getAmount()) / b.getTotalApproved());
            if (sender.hasOntClass(NS + "Trusted") || receiver.hasOntClass(NS + "Trusted")) {
                b.setTotalTrusted(b.getTotalTrusted() + 1);
            }
            b.setConsecutiveRejections(0);
            System.out.println(b);

            // Create the individuals we need
            Individual transaction = ontReasoned.createIndividual(NS + transactionRequestId, transactionClass);

            // Add the properties
            transaction.addProperty(hasSender, sender);
            transaction.addProperty(hasReceiver, receiver);

            // Print ontology
            printOntology();

            // Log approval to file
            String logEntry = "Transaction Id - " + t.getId() +
                    ", Bank Id - " + t.getBankId() +
                    ", Sender Id - " + t.getSenderId() +
                    ", Receiver Id - " + t.getReceiverId() +
                    ", Amount - " + t.getAmount() +
                    ", Category - " + t.getCategory() +
                    ", Timestamp - " + t.getTimestamp();
            try {
                appendToLog("acceptance", logEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }

            response.put("status", SUCCESS);
        
        } else {
            // Log rejection to file
            String logEntry = "Transaction Id - " + t.getId() +
                    ", Bank Id - " + t.getBankId() +
                    ", Sender Id - " + t.getSenderId() +
                    ", Receiver Id - " + t.getReceiverId() +
                    ", Amount - " + t.getAmount() +
                    ", Category - " + t.getCategory() +
                    ", Timestamp - " + t.getTimestamp() +
                    ", Rule Broken - " + t.getBrokenRule();
            try {
                appendToLog("rejection", logEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }

            response.put("status", FAILURE);
            response.put("reason", String.valueOf(t.getBrokenRule()));
        }

        // Update the bank instance in drools
        FactHandle bFactHandle = drools.kieSession.getFactHandle(b);
        drools.kieSession.update(bFactHandle, b);

        return ok(response);
        
        // ObjectNode response = Json.newObject();
        // response.put("status", SUCCESS);
        // return ok(response);
    }

    /**
     * Checks whether a bank is blacklisted
     */
    public Result isBlacklisted(String id){

        // Check if id belongs to a bank
        boolean isBank = banks.containsKey(id);

        if(isBank){

            // Checks if the bank is blacklisted
            boolean isBlacklisted = banks.get(id).isBlacklisted();

            ObjectNode response = Json.newObject();
            response.put("status", SUCCESS);
            response.put("result", String.valueOf(isBlacklisted));
            return ok(response);
        }else{
            // id doesn't belong to a bank;
            ObjectNode response = Json.newObject();
            response.put("status", FAILURE);
            response.put("reason", "Not a bank id");
            return ok(response);
        }
    }

    /**
     * Gets the number of rejections suffered from a bank
     */
    public Result getBankRejections(String id){
        
        // Checks if the id belongs to a bank
        boolean isBank = banks.containsKey(id);

        if(isBank){

            // Get number of rejections
            int numOfRejections = banks.get(id).getTotalRejections();

            ObjectNode response = Json.newObject();
            response.put("status", SUCCESS);
            response.put("rejections", String.valueOf(numOfRejections));
            return ok(response);
        }else{
            // ID did not belong to a bank
            ObjectNode response = Json.newObject();
            response.put("status", FAILURE);
            response.put("reason", "Not a bank");
            return ok(response);
        }
        

    }

    /**
     * Print rejection log
     */
    public Result rejectionLog(){
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("./logs/rejectionLog.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuffer logContents = new StringBuffer();
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {

                logContents.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ok(logContents.toString());


        // ObjectNode response = Json.newObject();
        // response.put("status", SUCCESS);
        // return ok(response);
    }

    /**
     * Print acceptance log
     */
    public Result acceptanceLog(){
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("./logs/acceptanceLog.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuffer logContents = new StringBuffer();
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {

                logContents.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ok(logContents.toString());

        // ObjectNode response = Json.newObject();
        // response.put("status", SUCCESS);
        // return ok(response);
    }

    /**
     * Create acceptance and rejection logs
     */
    public void createLogs() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./logs/acceptanceLog.txt"));
        writer.write("BEGIN ACCEPTANCE LOG:");
        writer.newLine();
        writer.close();
        BufferedWriter writer2 = new BufferedWriter(new FileWriter("./logs/rejectionLog.txt"));
        writer2.write("BEGIN REJECTION LOG:");
        writer2.newLine();
        writer2.close();
    }

    /**
     * Add text to a log
     */
    public void appendToLog(String logType, String logEntry) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./logs/" + logType + "Log.txt", true));
        writer.append(logEntry);
        writer.newLine();
        writer.close();
    }
    
}
