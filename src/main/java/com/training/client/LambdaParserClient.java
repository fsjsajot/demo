package com.training.client;

import com.training.data.request.DocumentParserRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Recoverable;
import io.micronaut.retry.annotation.Retryable;
import reactor.core.publisher.Mono;

@ClientFilter("/**")
@Client(value = "${lambda.parser.url}", id = "document-parser")
public interface LambdaParserClient {

    @Post
    @Retryable(attempts = "3", delay = "250ms", multiplier = "2", maxDelay = "1s")
    @Recoverable
    Mono<HttpResponse<ParsedResult>> parse(@Body DocumentParserRequest documentParserRequest);
}
