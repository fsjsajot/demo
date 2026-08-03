package com.training.services;

import com.training.client.LambdaParserHttpClient;
import com.training.data.request.AddDocumentRequest;
import com.training.data.request.DocumentParserRequest;
import com.training.data.request.UploadRequest;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.training.services.ContentTypeService.detectType;

/**
 * Handles an incoming loan document upload: detects the content type, parses
 * it via the Lambda parsing service, stores the result, and notifies the
 * front end over the socket connection.
 */
@Singleton
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private LambdaParserHttpClient lambdaParseHttpClient;
    private DocumentService documentService;
    private SocketNotifierService socketNotifierService;


    public DocumentProcessingService(DocumentService documentService,
                                     LambdaParserHttpClient lambdaParseHttpClient,
                                     SocketNotifierService socketNotifierService
                                     ) {
        this.documentService = documentService;
        this.lambdaParseHttpClient = lambdaParseHttpClient;
        this.socketNotifierService = socketNotifierService;
    }

    public Mono<Void> processUpload(UploadRequest request) {
        if (request == null || request.getFileStream() == null || request.getDocumentId() == null || request.getDocumentId().isBlank()) {
            return Mono.empty();
        }

        String documentId = request.getDocumentId();
        byte[] data;

        try (InputStream inputStream = request.getFileStream()) {
            data = inputStream.readAllBytes();
        } catch (IOException ex) {
            logger.error("Failed to read file stream from document {}", documentId, ex);
            return Mono.empty();
        }

        if (data.length == 0) {
            logger.warn("Rejected upload for document {}: empty file", documentId);
            return Mono.empty();
        }

        String contentType = detectType(data);
        if (contentType == null) {
            logger.warn("Unrecognized content type for uploaded document {}", documentId);
            return Mono.empty();
        }

        return lambdaParseHttpClient.parseDocument(new DocumentParserRequest(data, contentType))
                .flatMap(parsedResult ->
                        documentService.createDocument(new AddDocumentRequest(Objects.requireNonNull(parsedResult.body()).summary, contentType, documentId))
                                .doOnNext(savedDocument -> socketNotifierService.notifySuccess(documentId))
                )
                .doOnError(ex -> {
                    logger.error("Failed to process document {}", documentId, ex);
                    socketNotifierService.notifyFailure(documentId, "Failed to process document " + documentId);
                })
                .then();
    }
}
