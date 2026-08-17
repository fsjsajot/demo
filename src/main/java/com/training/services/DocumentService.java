package com.training.services;

import com.training.data.document.Document;
import com.training.data.request.AddDocumentRequest;
import com.training.db.DocumentStore;
import com.training.repository.DocumentRepository;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class DocumentService implements DocumentStore {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Mono<Document> save(AddDocumentRequest request) {
        Document document = new Document(request.getContent(), request.getDocumentId(), request.getContentType());

        return documentRepository.save(document);
    }
}
