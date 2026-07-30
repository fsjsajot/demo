package com.training.services;

import com.training.client.LambdaParserClient;
import com.training.data.request.DocumentParserRequest;
import com.training.data.request.UploadRequest;
import com.training.data.result.ParsedResult;
import com.training.db.DocumentStore;
import com.training.messaging.SocketNotifier;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles an incoming loan document upload: detects the content type, parses
 * it via the Lambda parsing service, stores the result, and notifies the
 * front end over the socket connection.
 */
@Singleton
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    DocumentStore documentStore;
    LambdaParserClient lambdaParserClient;
    SocketNotifier socketNotifier;

    public DocumentProcessingService(DocumentStore documentStore,
                                      LambdaParserClient lambdaParserClient,
                                      SocketNotifier socketNotifier) {
        this.documentStore = documentStore;
        this.lambdaParserClient = lambdaParserClient;
        this.socketNotifier = socketNotifier;
    }

    public void processUpload(UploadRequest request) {
        if (request == null || request.getFileStream() == null || request.getDocumentId() == null || request.getDocumentId().isBlank()) {
            return;
        }

        String documentId = request.getDocumentId();
        byte[] data;

        try (InputStream inputStream = request.getFileStream()) {
            data = inputStream.readAllBytes();
        } catch (IOException ex) {
            logger.error("Failed to read file stream from document {}", documentId, ex);
            return;
        }

        if (data.length == 0) {
            logger.warn("Rejected upload for document {}: empty file", documentId);
            return;
        }

        String contentType = detectType(data);
        if (contentType == null) {
            logger.warn("Unrecognized content type for uploaded document {}", documentId);
            return;
        }

        try {
            lambdaParserClient.parse(new DocumentParserRequest(data, contentType))
                    .map(parsedResult -> {
                        documentStore.save(documentId, contentType, parsedResult);
                        socketNotifier.notifySuccess(documentId);

                        return parsedResult;
                    });
        } catch (Exception ex) {
            logger.error("Failed to process document {}", documentId, ex);
            socketNotifier.notifyFailure(documentId, "Failed to process document " + documentId);
        }
    }

    private String detectType(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        if (data.length >= 4
                && data[0] == (byte) 0x25
                && data[1] == (byte) 0x50
                && data[2] == (byte) 0x44
                && data[3] == (byte) 0x46) {
            return "pdf";
        }

        if (data.length >= 8
                && data[0] == (byte) 0xD0
                && data[1] == (byte) 0xCF
                && data[2] == (byte) 0x11
                && data[3] == (byte) 0xE0
                && data[4] == (byte) 0xA1
                && data[5] == (byte) 0xB1
                && data[6] == (byte) 0x1A
                && data[7] == (byte) 0xE1) {
            return "excel";
        }

        if (data.length >= 4
                && data[0] == 'P'
                && data[1] == 'K'
                && data[2] == 0x03
                && data[3] == 0x04) {
            return "excel";
        }

        return null;
    }
}
