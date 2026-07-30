package com.training.data.request;

import java.io.InputStream;

public class UploadRequest {
    public InputStream fileStream;
    public String documentId;

    public InputStream getFileStream() { return fileStream; }
    public String getDocumentId() { return documentId; }
}
