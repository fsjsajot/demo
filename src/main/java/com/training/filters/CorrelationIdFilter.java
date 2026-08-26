package com.training.filters;

import io.micronaut.context.propagation.slf4j.MdcPropagationContext;
import io.micronaut.core.async.propagation.ReactorPropagation;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Filter("/**")
public class CorrelationIdFilter implements HttpServerFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String correlationId = request.getHeaders()
                .getFirst(CORRELATION_ID_HEADER)
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(MDC_KEY, correlationId);

        PropagatedContext propagatedContext = PropagatedContext.getOrEmpty()
                .plus(new MdcPropagationContext());

        try {
            return propagatedContext.propagateCall(() -> {
                MDC.put(MDC_KEY, correlationId);
                try {
                    return Flux.from(chain.proceed(request))
                            .doOnNext(response -> response.getHeaders().add(CORRELATION_ID_HEADER, correlationId))
                            .contextWrite(ctx -> ReactorPropagation.addPropagatedContext(ctx, PropagatedContext.get()));
                } finally {
                    MDC.remove(MDC_KEY);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.TRACING.order();
    }
}
