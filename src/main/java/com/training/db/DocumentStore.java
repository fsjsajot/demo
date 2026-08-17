package com.training.db;

import com.training.data.document.Document;
import com.training.data.request.AddDocumentRequest;
import reactor.core.publisher.Mono;


public interface DocumentStore {
    Mono<Document> save(AddDocumentRequest request);
}
