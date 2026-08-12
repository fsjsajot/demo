package com.training.client;

import com.training.data.request.DocumentParserRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Mono;

@Client("${lambda.parser.url}")
public interface LambdaParserClient {

    @Post
    Mono<ParsedResult> parse(@Body DocumentParserRequest request);
}
