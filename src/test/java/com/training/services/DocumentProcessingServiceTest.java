package com.training.services;

import com.training.client.LambdaParserClient;
import com.training.data.request.DocumentParserRequest;
import com.training.data.request.UploadRequest;
import com.training.data.result.ParsedResult;
import com.training.db.DocumentStore;
import com.training.messaging.SocketNotifier;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DocumentProcessingServiceTest {

    @Test
    void shouldIgnoreNullRequest() throws IOException {
        RecordingDocumentStore documentStore = new RecordingDocumentStore();
        RecordingLambdaParserClient parserClient = new RecordingLambdaParserClient();
        RecordingSocketNotifier socketNotifier = new RecordingSocketNotifier();
        DocumentProcessingService service = new DocumentProcessingService(documentStore, parserClient, socketNotifier);

        service.processUpload(null);

        assertFalse(parserClient.wasCalled());
        assertFalse(documentStore.wasCalled());
        assertFalse(socketNotifier.successNotified());
    }

    @Test
    void shouldIgnoreRequestWithoutFileStream() throws IOException {
        RecordingDocumentStore documentStore = new RecordingDocumentStore();
        RecordingLambdaParserClient parserClient = new RecordingLambdaParserClient();
        RecordingSocketNotifier socketNotifier = new RecordingSocketNotifier();
        DocumentProcessingService service = new DocumentProcessingService(documentStore, parserClient, socketNotifier);

        UploadRequest request = buildRequest(null, "doc-1");

        service.processUpload(request);

        assertFalse(parserClient.wasCalled());
        assertFalse(documentStore.wasCalled());
        assertFalse(socketNotifier.successNotified());
    }

    @Test
    void shouldIgnoreRequestWithoutDocumentId() throws IOException {
        RecordingDocumentStore documentStore = new RecordingDocumentStore();
        RecordingLambdaParserClient parserClient = new RecordingLambdaParserClient();
        RecordingSocketNotifier socketNotifier = new RecordingSocketNotifier();
        DocumentProcessingService service = new DocumentProcessingService(documentStore, parserClient, socketNotifier);

        UploadRequest request = buildRequest(new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.ISO_8859_1)), null);

        service.processUpload(request);

        assertFalse(parserClient.wasCalled());
        assertFalse(documentStore.wasCalled());
        assertFalse(socketNotifier.successNotified());
    }

    private UploadRequest buildRequest(InputStream fileStream, String documentId) {
        UploadRequest request = new UploadRequest();
        setField(request, "fileStream", fileStream);
        setField(request, "documentId", documentId);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class RecordingDocumentStore implements DocumentStore {
        private boolean called;

        @Override
        public void save(String documentId, String contentType, ParsedResult result) {
            called = true;
        }

        boolean wasCalled() {
            return called;
        }
    }

    private static class RecordingLambdaParserClient implements LambdaParserClient {
        private boolean called;

        boolean wasCalled() {
            return called;
        }

        @Override
        public Mono<ParsedResult> parse(DocumentParserRequest request) {
            called = true;
            return Mono.just(new ParsedResult("Sample content"));
        }
    }

    private static class RecordingSocketNotifier implements SocketNotifier {
        private boolean successNotified;

        @Override
        public void notifySuccess(String documentId) {
            successNotified = true;
        }

        @Override
        public void notifyFailure(String documentId, String reason) {
            successNotified = false;
        }

        boolean successNotified() {
            return successNotified;
        }
    }
}
