package com.training.data.request;

public class AddDocumentRequest {

    private String content;
    private String contentType;
    private String documentId;

    public AddDocumentRequest(String content, String documentId,  String contentType) {
        this.documentId = documentId;
        this.content = content;
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
