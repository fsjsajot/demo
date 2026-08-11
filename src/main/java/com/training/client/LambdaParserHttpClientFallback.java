package com.training.client;

import com.training.data.request.DocumentParserRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.retry.annotation.Fallback;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
@Fallback
public class LambdaParserHttpClientFallback implements LambdaParserHttpClient {

    @Override
    public Mono<HttpResponse<ParsedResult>> parseDocument(DocumentParserRequest documentParserRequest) {
        return Mono.just(
                HttpResponse.ok(
                        new ParsedResult(
                                "Parser unavailable. Please retry later.",
                                false
                        )
                )
        );
    }
}
