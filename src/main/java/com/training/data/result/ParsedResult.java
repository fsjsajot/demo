package com.training.data.result;


public class ParsedResult {
    public String summary;
    public boolean successful;

    public ParsedResult(String summary) {
        this.summary = summary;
    }

    public ParsedResult(String summary, boolean successful) {
        this.summary = summary;
        this.successful = successful;
    }

    public String getSummary() { return summary; }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
