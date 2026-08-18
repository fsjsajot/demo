package com.training.data.request;

import java.io.InputStream;

public class UploadRequest {
    InputStream fileStream;
    String documentId;
    String requestId;

    public InputStream getFileStream() { return fileStream; }
    public String getDocumentId() { return documentId; }
    public String getRequestId() { return requestId; }

    public UploadRequest(String documentId, InputStream fileStream) {
        this(documentId, fileStream, null);
    }

    public UploadRequest(String documentId, InputStream fileStream, String requestId) {
        this.fileStream = fileStream;
        this.documentId = documentId;
        this.requestId = requestId;
    }
}
