package com.training.controllers;

import com.training.data.request.DocumentParserRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Controller("/document-parser")
public class DocumentParserController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentParserController.class);

    @Inject
    private ObjectMapper objectMapper;

    @Post
    Mono<HttpResponse<ParsedResult>> parse(@Body DocumentParserRequest documentParserRequest) throws IOException {
        logger.debug("Received documentParserRequest {}", objectMapper.writeValueAsString(documentParserRequest));
        if (documentParserRequest.getData() == null ||
                documentParserRequest.getContentType() == null ||
                documentParserRequest.getContentType().isEmpty()) {
            return Mono.just(HttpResponse.status(HttpStatus.BAD_REQUEST));
        }

        if (documentParserRequest.getContentType().equals("pdf")) {
            logger.info("Parsing PDF");
            return Mono.just(HttpResponse.ok(new ParsedResult("This is a pdf file.", true)));
        }

        if (documentParserRequest.getContentType().equals("excel")) {
            logger.info("Parsing Excel");
            return Mono.just(HttpResponse.ok(new ParsedResult("This is an excel file.", true)));
        }

        logger.info("Unsupported content type");
        return Mono.just(HttpResponse.badRequest(new ParsedResult("Unsupported content type.", false)));
    }
}
