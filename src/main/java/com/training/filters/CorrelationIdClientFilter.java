package com.training.filters;


import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import jakarta.inject.Singleton;

@ClientFilter("/**")
@Singleton
public class CorrelationIdClientFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @RequestFilter
    void filterRequest(MutableHttpRequest<?> request) {
        PropagatedContext.getOrEmpty()
                .find(CorrelationIdContextElement.class)
                .ifPresent(el -> request.header(CORRELATION_ID_HEADER, el.correlationId()));
    }
}
