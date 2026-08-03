package com.training.repository;

import com.training.data.document.Document;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import org.bson.types.ObjectId;

@MongoRepository
public interface DocumentRepository extends ReactorCrudRepository<Document, ObjectId> {
}
