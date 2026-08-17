package com.training.db;

import com.training.data.result.ParsedResult;

/**
 * Stub — enough context to compile against. Not part of the exercise;
 * do not modify. Named to match the mock interview's "document store"
 * language, not a generic repository.
 */
public interface DocumentStore {
    void save(String documentId, String contentType, ParsedResult result);
}
