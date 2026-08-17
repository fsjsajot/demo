package com.training.services;

import com.training.client.LambdaParserClient;
import com.training.data.document.Document;
import com.training.data.request.AddDocumentRequest;
import com.training.data.request.DocumentParserRequest;
import com.training.data.request.UploadRequest;
import com.training.data.result.ParsedResult;
import com.training.db.DocumentStore;
import com.training.messaging.SocketNotifier;
import io.micronaut.http.HttpResponse;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentProcessingServiceTest {

    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x01, 0x02};
    private static final byte[] XLS_OLE_BYTES = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0x00
    };
    private static final byte[] UNKNOWN_BYTES = {0x00, 0x01, 0x02, 0x03};
    private static final byte[] ZIP_SIGNATURE_BYTES = {'P', 'K', 0x03, 0x04};

    private DocumentStore documentStore;
    private LambdaParserClient lambdaParserClient;
    private SocketNotifier socketNotifier;
    private DocumentProcessingService documentProcessingService;
    private ContentTypeService contentTypeService;

    @BeforeEach
    void setUp() {
        documentStore = mock(DocumentStore.class);
        lambdaParserClient = mock(LambdaParserClient.class);
        socketNotifier = mock(SocketNotifier.class);
        contentTypeService = mock(ContentTypeService.class);

        documentProcessingService = new DocumentProcessingService(documentStore, lambdaParserClient, socketNotifier, contentTypeService);
    }

    @ParameterizedTest
    @MethodSource("contentTypeCases")
    void shouldHandleContentTypes(byte[] payload, String expectedType) {
        UploadRequest uploadRequest = new UploadRequest("doc-1", new ByteArrayInputStream(payload));

        when(contentTypeService.detectType(any(byte[].class))).thenReturn(expectedType);

        if (expectedType != null) {
            ParsedResult parsedResult = new ParsedResult(expectedType);
            when(lambdaParserClient.parse(any(DocumentParserRequest.class))).thenReturn(Mono.just(HttpResponse.ok(parsedResult)));
            when(documentStore.save(any(AddDocumentRequest.class))).thenReturn(Mono.just(mock(Document.class)));
        }

        documentProcessingService.processUpload(uploadRequest).block();

        if (expectedType == null) {
            verify(socketNotifier).notifyFailure(
                    "doc-1",
                    "Unrecognized content type for uploaded document doc-1"
            );

            verifyNoInteractions(lambdaParserClient);
            verifyNoInteractions(documentStore);
        } else {
            verify(lambdaParserClient).parse(any(DocumentParserRequest.class));
            verify(socketNotifier).notifySuccess("doc-1");
            verify(documentStore).save(any(AddDocumentRequest.class));
        }
    }

    @Test
    void shouldReturnEmptyMonoWhenFileStreamThrowsIOException() {
        UploadRequest invalidRequest = new UploadRequest("doc-1", new ThrowingInputStream());

        StepVerifier.create(documentProcessingService.processUpload(invalidRequest))
                .expectNextCount(0)
                .verifyComplete();

        verify(socketNotifier).notifyFailure(
                "doc-1",
                "Failed to read file stream"
        );

        verifyNoInteractions(lambdaParserClient);
        verifyNoInteractions(documentStore);
    }

    @Test
    void shouldReturnEmptyMonoWhenFileIsEmpty() {
        UploadRequest invalidRequest = new UploadRequest("doc-1", new ByteArrayInputStream(new byte[0]));

        StepVerifier.create(documentProcessingService.processUpload(invalidRequest))
                .expectNextCount(0)
                .verifyComplete();


        verify(socketNotifier).notifyFailure(
                "doc-1",
                "Rejected upload for document" + "doc-1: empty file"
        );
        verifyNoInteractions(lambdaParserClient);
        verifyNoInteractions(documentStore);
    }

    @Test
    void shouldNotifyFailureWhenParserThrowsError() {
        UploadRequest uploadRequest = new UploadRequest("doc-1", new ByteArrayInputStream(PDF_BYTES));

        when(contentTypeService.detectType(any(byte[].class))).thenReturn("pdf");

        when(lambdaParserClient.parse(any(DocumentParserRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("simulated parser failure")));

        StepVerifier.create(documentProcessingService.processUpload(uploadRequest))
                .expectError(RuntimeException.class)
                .verify();

        verify(lambdaParserClient).parse(any(DocumentParserRequest.class));
        verify(socketNotifier).notifyFailure(eq("doc-1"), anyString());
        verifyNoInteractions(documentStore);
    }

    @ParameterizedTest
    @MethodSource("invalidRequestCases")
    void shouldReturnEmptyMonoForInvalidRequests(UploadRequest invalidRequest) {
        StepVerifier.create(documentProcessingService.processUpload(invalidRequest)).expectNextCount(0).verifyComplete();

        verifyNoInteractions(lambdaParserClient);
        verifyNoInteractions(socketNotifier);
        verifyNoInteractions(documentStore);
    }

    @Test
    void shouldNotifyFailureWhenParserReturnsUnavailableFallback() {
        ParsedResult unavailableResult = new ParsedResult("Parser unavailable. Please retry later.", false);
        when(lambdaParserHttpClient.parseDocument(any()))
                .thenReturn(Mono.just(HttpResponse.ok(unavailableResult)));

        UploadRequest uploadRequest = new UploadRequest("doc-1", new ByteArrayInputStream(PDF_BYTES));

        StepVerifier.create(documentProcessingService.processUpload(uploadRequest))
                        .expectErrorMatches(ex -> ex instanceof  RuntimeException &&
                                ex.getMessage().contains("Parser unavailable")).verify();

        verify(socketNotifierService).notifyFailure(eq("doc-1"), anyString());
        verify(socketNotifierService, never()).notifySuccess(anyString());
        verify(documentService, never()).createDocument(any());
    }

    @Test
    void shouldNotifySuccessWhenParserReturnsResult() {
        ParsedResult successResult = new ParsedResult("This is a pdf file.", true);
        when(lambdaParserHttpClient.parseDocument(any(DocumentParserRequest.class))).thenReturn(Mono.just(HttpResponse.ok(successResult)));
        when(documentService.createDocument(any(AddDocumentRequest.class))).thenReturn(Mono.just(mock(Document.class)));

        UploadRequest uploadRequest = new UploadRequest("doc-1", new ByteArrayInputStream(PDF_BYTES));

        StepVerifier.create(documentProcessingService.processUpload(uploadRequest)).verifyComplete();

        verify(documentService).createDocument(any());
        verify(socketNotifierService).notifySuccess("doc-1");
        verify(socketNotifierService, never()).notifyFailure(anyString(), anyString());

    }

    @Test
    void shouldNotifyFailureWhenParserReturnsUnavailableFallback() {
        ParsedResult unavailableResult = new ParsedResult("Parser unavailable. Please retry later.", false);
        when(lambdaParserHttpClient.parseDocument(any()))
                .thenReturn(Mono.just(HttpResponse.ok(unavailableResult)));

        UploadRequest uploadRequest = new UploadRequest("doc-1", new ByteArrayInputStream(PDF_BYTES));

        StepVerifier.create(documentProcessingService.processUpload(uploadRequest))
                        .expectErrorMatches(ex -> ex instanceof  RuntimeException &&
                                ex.getMessage().contains("Parser unavailable")).verify();

        verify(socketNotifierService).notifyFailure(eq("doc-1"), anyString());
        verify(socketNotifierService, never()).notifySuccess(anyString());
        verify(documentService, never()).createDocument(any());
    }

    @Test
    void shouldNotifySuccessWhenParserReturnsResult() {
        ParsedResult successResult = new ParsedResult("This is a pdf file.", true);
        when(lambdaParserHttpClient.parseDocument(any(DocumentParserRequest.class))).thenReturn(Mono.just(HttpResponse.ok(successResult)));
        when(documentService.createDocument(any(AddDocumentRequest.class))).thenReturn(Mono.just(mock(Document.class)));

        UploadRequest uploadRequest = new UploadRequest("doc-1", new ByteArrayInputStream(PDF_BYTES));

        StepVerifier.create(documentProcessingService.processUpload(uploadRequest)).verifyComplete();

        verify(documentService).createDocument(any());
        verify(socketNotifierService).notifySuccess("doc-1");
        verify(socketNotifierService, never()).notifyFailure(anyString(), anyString());

    }

    private static Stream<Arguments> invalidRequestCases() {
        return Stream.of(
                Arguments.of((UploadRequest) null),                                       // request == null
                Arguments.of(new UploadRequest("doc-1", null)),                           // null file stream
                Arguments.of(new UploadRequest(null, new ByteArrayInputStream(PDF_BYTES))), // null documentId
                Arguments.of(new UploadRequest("", new ByteArrayInputStream(XLS_OLE_BYTES))) // blank documentId
        );
    }

    private static Stream<Arguments> contentTypeCases() {
        return Stream.of(
                Arguments.of(PDF_BYTES, "pdf"),
                Arguments.of(XLS_OLE_BYTES, "excel"),
                Arguments.of(UNKNOWN_BYTES, null),
                Arguments.of(ZIP_SIGNATURE_BYTES, "excel")
        );
    }

    private static class ThrowingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("simulated read failure");
        }

        @Override
        public byte @NonNull [] readAllBytes() throws IOException {
            throw new IOException("simulated read failure");
        }
    }

}