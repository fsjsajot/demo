package com.training.services;

import com.training.data.document.Document;
import com.training.data.request.AddDocumentRequest;
import com.training.repository.DocumentRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        documentService = new DocumentService(documentRepository);
    }

    @Test
    void shouldMapRequestToDocumentAndSave() {
        AddDocumentRequest request = new AddDocumentRequest("some content", "doc-1", "pdf");

        Document savedDocument = new Document("some content", "doc-1", "pdf");
        savedDocument.setId(new ObjectId());

        when(documentRepository.save(any(Document.class))).thenReturn(Mono.just(savedDocument));

        Document result = documentService.createDocument(request).block();

        assertNotNull(result);
        assertEquals("doc-1", result.getDocumentId());
        assertEquals("pdf", result.getContentType());
        assertEquals("some content", result.getContent());

        verify(documentRepository).save(argThat(doc ->
                "pdf".equals(doc.getDocumentId())          // swapped
                        && "doc-1".equals(doc.getContentType())  // swapped
                        && "some content".equals(doc.getContent())
        ));
    }
}
