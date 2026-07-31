package com.training.services;

import com.training.client.LambdaParserClient;
import com.training.data.request.DocumentParserRequest;
import com.training.data.request.UploadRequest;
import com.training.data.result.ParsedResult;
import com.training.db.DocumentStore;
import com.training.messaging.SocketNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentProcessingServiceTest {

    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x01, 0x02};
    private static final byte[] XLS_OLE_BYTES = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0x00
    };
    private static final byte[] UNKNOWN_BYTES = {0x00, 0x01, 0x02, 0x03};

    private FakeDocumentStore fakeDocumentStore;
    private FakeLambdaParserClient fakeLambdaParserClient;
    private FakeSocketNotifier fakeSocketNotifier;
    private DocumentProcessingService documentProcessingService;

    @BeforeEach
    void setup() {
        fakeDocumentStore = new FakeDocumentStore();
        fakeLambdaParserClient = new FakeLambdaParserClient();
        fakeSocketNotifier = new FakeSocketNotifier();
        documentProcessingService = new DocumentProcessingService(fakeDocumentStore, fakeLambdaParserClient, fakeSocketNotifier);
    }

    @Test
    void shouldProcessPdfUpload() {
        UploadRequest uploadRequest = new FakeUploadRequest("doc-1", new ByteArrayInputStream(PDF_BYTES));

        documentProcessingService.processUpload(uploadRequest).block();

        assertEquals("pdf", fakeLambdaParserClient.lastContentType);
        assertEquals("doc-1", fakeDocumentStore.savedDocumentId);
        assertEquals("pdf", fakeDocumentStore.savedContentType);
        assertEquals("doc-1", fakeSocketNotifier.documentId);
    }

    @Test
    void shouldProcessExcelUpload() {
        UploadRequest uploadRequest = new FakeUploadRequest("doc-2", new ByteArrayInputStream(XLS_OLE_BYTES));

        documentProcessingService.processUpload(uploadRequest).block();

        assertEquals("excel", fakeLambdaParserClient.lastContentType);
        assertEquals("doc-2", fakeDocumentStore.savedDocumentId);
        assertEquals("excel", fakeDocumentStore.savedContentType);
        assertEquals("doc-2", fakeSocketNotifier.documentId);
    }

    @Test
    void shouldIgnoreNullInputStream() {
        FakeUploadRequest uploadRequest = new FakeUploadRequest("doc-1", null);
        documentProcessingService.processUpload(uploadRequest).block();

        assertNull(fakeLambdaParserClient.lastContentType);
        assertNull(fakeDocumentStore.savedDocumentId);
        assertNull(fakeSocketNotifier.documentId);
    }

    @Test
    void shouldDoNothingUnrecognizedFileType() {
        FakeUploadRequest uploadRequest = new FakeUploadRequest("doc-1", new ByteArrayInputStream(UNKNOWN_BYTES));
        documentProcessingService.processUpload(uploadRequest).block();

        assertNull(fakeLambdaParserClient.lastContentType);
        assertNull(fakeDocumentStore.savedDocumentId);
        assertNull(fakeSocketNotifier.documentId);
    }

    private static class FakeUploadRequest extends UploadRequest {
        private final String documentId;
        private final InputStream inputStream;

        public FakeUploadRequest(String documentId, InputStream inputStream) {
            this.documentId = documentId;
            this.inputStream = inputStream;
        }


        @Override
        public InputStream getFileStream() {
            return inputStream;
        }

        @Override
        public String getDocumentId() {
            return documentId;
        }
    }

    private static class FakeDocumentStore implements DocumentStore {
        String savedDocumentId;
        String savedContentType;
        boolean shouldThrowException;


        @Override
        public void save(String documentId, String contentType, ParsedResult result) {
            if (shouldThrowException) {
                throw new RuntimeException("Simulated save failure.");
            }

            this.savedDocumentId = documentId;
            this.savedContentType = contentType;
        }
    }

    private static class FakeLambdaParserClient implements LambdaParserClient {
        String lastContentType;
        boolean shouldThrowException;

        @Override
        public Mono<ParsedResult> parse(DocumentParserRequest request) {
            if (shouldThrowException) {
                throw new RuntimeException("Simulated parsing exception error.");
            }

            this.lastContentType = request.contentType;
            return Mono.just(new ParsedResult(request.contentType));
        }
    }

    private static class FakeSocketNotifier implements SocketNotifier {
        String documentId;
        String failedDocumentId;
        String failedReason;

        @Override
        public void notifySuccess(String documentId) {
            this.documentId = documentId;
        }

        @Override
        public void notifyFailure(String documentId, String reason) {
            this.failedDocumentId = documentId;
            this.failedReason = reason;
        }
    }
}