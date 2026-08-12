package com.training.controllers;

import com.training.data.request.UploadRequest;
import com.training.services.DocumentProcessingService;
import com.training.services.SocketMessageCreatorService;
import com.training.websocket.WebSocketNotifier;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.websocket.WebSocketBroadcaster;
import jakarta.inject.Inject;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Controller("/document-process")
public class DocumentProcessController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessController.class);


    private final DocumentProcessingService documentProcessingService;

    public DocumentProcessController(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @Post(value = "/", consumes = MediaType.MULTIPART_FORM_DATA)
    public Mono<HttpResponse<String>> processDocument(CompletedFileUpload file, String documentId) {
        if (file == null || documentId == null || documentId.isBlank()) {
            return Mono.just(HttpResponse.badRequest("Invalid request."));
        }

        logger.debug("Processing file upload");
        logger.debug("Document ID: {}", documentId);
        logger.debug("File name: {}", file.getFilename());

        UploadRequest uploadRequest;

        try {
            uploadRequest = new UploadRequest(documentId, file.getInputStream());
        } catch (IOException e) {
            logger.error("Error processing file upload", e);
            return Mono.just(HttpResponse.serverError());
        }


        return documentProcessingService.processUpload(uploadRequest)
                .thenReturn(HttpResponse.ok("uploaded and processed"));
    }
}
