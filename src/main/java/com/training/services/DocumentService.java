package com.training.services;

import com.training.data.document.Document;
import com.training.data.request.AddDocumentRequest;
import com.training.repository.DocumentRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class DocumentService {

    private DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Mono<Document> createDocument(AddDocumentRequest request) {
        Document document = new Document(request.getContent(), request.getDocumentId(), request.getContentType());

        return documentRepository.save(document);
    }
}
