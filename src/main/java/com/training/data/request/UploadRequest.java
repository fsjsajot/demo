package com.training.data.request;

import java.io.InputStream;

public class UploadRequest {
    InputStream fileStream;
    String documentId;

    public InputStream getFileStream() { return fileStream; }
    public String getDocumentId() { return documentId; }

    public UploadRequest(String documentId, InputStream fileStream) {
        this.fileStream = fileStream;
        this.documentId = documentId;
    }
}
