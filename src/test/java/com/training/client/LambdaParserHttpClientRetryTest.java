package com.training.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.training.data.request.DocumentParserRequest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import io.micronaut.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class LambdaParserHttpClientRetryTest {

    private WireMockServer wireMockServer;
    private ApplicationContext applicationContext;
    private LambdaParserClient lambdaParserClient;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();

        int port = wireMockServer.port();

        WireMock.configureFor("localhost", port);

        applicationContext = ApplicationContext.run(PropertySource.of("test", Map.of(
                "lambda.parser.url", "http://localhost:" + port + "/document-parser"
        )));

        lambdaParserClient = applicationContext.getBean(LambdaParserClient.class);
    }

    @Test
    void fallbackBeanShouldBeRegistered() {
        assertTrue(applicationContext.containsBean(LambdaParserHttpClientFallback.class));
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

        HttpResponse<?> response = lambdaParserClient.parse(new DocumentParserRequest(new byte[]{1}, "pdf")).block();

        verify(4, postRequestedFor(urlEqualTo("/document-parser")));

        assertNotNull(response);
        var body = (com.training.data.result.ParsedResult) response.body();

        assertNotNull(body);
        assertFalse(body.isSuccessful());
    }
}
