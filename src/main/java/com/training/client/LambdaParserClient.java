package com.training.client;

import com.training.data.result.ParsedResult;

/**
 * Stub — represents the external "house lambda service" Jhefrey described
 * in the mock interview as owning the actual parsing logic. Not part of
 * the exercise; do not modify.
 */
public interface LambdaParserClient {
    ParsedResult parse(byte[] data, String contentType);
}
