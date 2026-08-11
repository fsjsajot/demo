package com.training.client;

import com.training.data.request.DocumentParserRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Recoverable;
import io.micronaut.retry.annotation.Retryable;
import reactor.core.publisher.Mono;

@Client(id= "document-parser")
public interface LambdaParserHttpClient {

    @Post("/document-parser")
    @Retryable(attempts = "3", delay = "250ms", multiplier = "2", maxDelay = "1s")
    @Recoverable
    Mono<HttpResponse<ParsedResult>> parseDocument(@Body DocumentParserRequest documentParserRequest);
}
