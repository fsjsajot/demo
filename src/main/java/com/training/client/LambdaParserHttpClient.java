package com.training.client;

import com.training.data.request.DocumentParserRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Mono;

@Client("/document-parser")
public interface LambdaParserHttpClient {

    @Post
    Mono<HttpResponse<ParsedResult>> parseDocument(@Body DocumentParserRequest documentParserRequest);
}
