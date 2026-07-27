package com.training.data.request;

import java.io.InputStream;

/**
 * Stub — enough context to compile against. Not part of the exercise;
 * do not modify. Note there is deliberately no validation on this object
 * (no @NotNull / @NotBlank) — that is one of the day's defects.
 */
public class UploadRequest {
    private InputStream fileStream;
    private String documentId;

    public InputStream getFileStream() { return fileStream; }
    public String getDocumentId() { return documentId; }
}
