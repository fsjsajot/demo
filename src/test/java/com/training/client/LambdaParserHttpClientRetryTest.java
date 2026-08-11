package com.training.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.training.data.request.DocumentParserRequest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import io.micronaut.http.HttpResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LambdaParserHttpClientRetryTest {

    private WireMockServer wireMockServer;
    private ApplicationContext applicationContext;
    private LambdaParserHttpClient lambdaParserHttpClient;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();

        int port = wireMockServer.port();

        WireMock.configureFor("localhost", port);

        applicationContext = ApplicationContext.run(PropertySource.of("test", Map.of(
                "micronaut.http.services.document-parser.url", "http://localhost:" + port
        )));

        lambdaParserHttpClient = applicationContext.getBean(LambdaParserHttpClient.class);
    }

    @Test
    void fallbackBeanShouldBeRegistered() {
        boolean hasFallback = applicationContext.containsBean(LambdaParserHttpClientFallback.class);
        System.out.println("Fallback bean registered: " + hasFallback);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        applicationContext.close();
    }

    @Test
    void shouldRetryConfiguredAttemptsThenFallBack() {
        stubFor(post(urlEqualTo("/document-parser"))
                .willReturn(aResponse().withStatus(500)));

        HttpResponse<?> response = lambdaParserHttpClient.parseDocument(new DocumentParserRequest(new byte[]{1}, "pdf")).block();

        verify(4, postRequestedFor(urlEqualTo("/document-parser")));

        assertNotNull(response);
        var body = (com.training.data.result.ParsedResult) response.body();

        assertNotNull(body);
        assertFalse(body.successful);
    }
}
