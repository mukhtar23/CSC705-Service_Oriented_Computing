package models;

public class Bank {

    private String id;
    private String nationality;
    private boolean isBlacklisted;
    private int totalRejections;
    private int consecutiveRejections;
    // # of approved transactions
    private int totalApproved;
    // # of transactions that used a trusted participant
    private int totalTrusted;
    // avg amount of money
    private double avgAmount;

    public Bank(String id, String nationality) {
        this.id = id;
        this.nationality = nationality;
        this.isBlacklisted = false;
        this.totalRejections = 0;
        this.consecutiveRejections = 0;
        this.totalApproved = 0;
        this.totalTrusted = 0;
        this.avgAmount = 0.0;
    }

    @Override
    public String toString() {
        return "Bank(" + this.id + ", " +
                this.nationality + ", " +
                this.isBlacklisted + ", " +
                this.totalRejections + ", " +
                this.consecutiveRejections + ", " +
                this.totalApproved + ", " +
                this.totalTrusted + ", " +
                this.avgAmount + ")";
    }

    public String getId() {
        return this.id;
    }

    public String getNationality() {
        return this.nationality;
    }

    public boolean isBlacklisted() {
        return this.isBlacklisted;
    }

    public void setBlacklisted(boolean isBlacklisted) {
        this.isBlacklisted = isBlacklisted;
    }

    public int getTotalRejections() {
        return this.totalRejections;
    }

    public void setTotalRejections(int totalRejections) {
        this.totalRejections = totalRejections;
    }

    public int getConsecutiveRejections() {
        return this.consecutiveRejections;
    }

    public void setConsecutiveRejections(int consecutiveRejections) {
        this.consecutiveRejections = consecutiveRejections;
    }

    public int getTotalApproved() {
        return this.totalApproved;
    }

    public void setTotalApproved(int totalApproved) {
        this.totalApproved = totalApproved;
    }

    public int getTotalTrusted() {
        return this.totalTrusted;
    }

    public void setTotalTrusted(int totalTrusted) {
        this.totalTrusted = totalTrusted;
    }

    public double getAvgAmount() {
        return this.avgAmount;
    }

    public void setAvgAmount(double avgAmount) {
        this.avgAmount = avgAmount;
    }
}