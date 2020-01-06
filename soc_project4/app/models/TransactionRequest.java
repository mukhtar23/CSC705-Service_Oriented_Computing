package models;

import java.text.SimpleDateFormat;
import java.util.Date;

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

public class TransactionRequest {

    private String id;
    private boolean isProcessed;
    private boolean isApproved;
    private String senderId;
    private String receiverId;
    private String bankId;
    private String category;
    private int amount;
    private String timestamp;
    private String brokenRule;

    private OntModel ontReasoned;
    final String sourceFile = "../../project3.owl";
    final String sourceUrl = "http://www.semanticweb.org/mukht/ontologies/2019/9/soc_project3";
    final String NS = sourceUrl + "#";
    

    // senderId: String, receiverId: String, bankId: String, category: String, amount: int, transactionRequestId: String
    public TransactionRequest(String senderId, String receiverId, String bankId, String category, int amount, String id, OntModel ontReasoned) {
        this.id = id;
        this.isProcessed = false;
        this.isApproved = true; // Assume approved unless rejected by a rule
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.bankId = bankId;
        this.category = category;
        this.amount = amount;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
        this.ontReasoned = ontReasoned;
    }

    @Override
    public String toString() {
        return "TransactionRequest(" + this.id + ", " + this.isApproved + ", " + this.senderId + ", " +
                this.receiverId + ", " + this.bankId + ", " + this.category + ", " + this.amount + ")";
    }

    public String getId() {
        return this.id;
    }

    public boolean isProcessed() {
        return this.isProcessed;
    }

    public void setProcessed(boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public boolean isApproved() {
        return this.isApproved;
    }

    public void setApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public String getReceiverId() {
        return this.receiverId;
    }

    public String getBankId() {
        return this.bankId;
    }

    public String getCategory() {
        return this.category;
    }

    public int getAmount() {
        return this.amount;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getBrokenRule() {
        return this.brokenRule;
    }

    public void setBrokenRule(String brokenRule) {
        this.brokenRule = brokenRule;
    }

    /**
     *  Checks to see if both the sender and receiver are both trusted
     * 
     */
    public boolean areBothTrusted(){
        boolean trusted = true; // assume both are trusted 

        // Get sender and receiver properties
        Individual sender = ontReasoned.getIndividual(NS + this.senderId);
        Individual receiver = ontReasoned.getIndividual(NS + this.receiverId);

        if(!sender.hasOntClass(NS + "Trusted") || !receiver.hasOntClass(NS + "Trusted"))
            trusted = false; // one of them isn't trusted
        
        return trusted;
    }

    /**
     *  Checks to see if either the sender or receiver is trusted
     */
    public boolean isOneTrusted(){
        boolean trusted = true; // assume one of the is trusted

        // Get sender and receiver properties
        Individual sender = ontReasoned.getIndividual(NS + this.senderId);
        Individual receiver = ontReasoned.getIndividual(NS + this.receiverId);

        if(!sender.hasOntClass(NS + "Trusted") && !receiver.hasOntClass(NS + "Trusted"))
            trusted = false; // both are not trusted
        
        return trusted;
    }
}