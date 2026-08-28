package com.training.filters;

import io.micronaut.context.propagation.slf4j.MdcPropagationContext;
import io.micronaut.core.async.propagation.ReactorPropagation;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@Filter("/**")
public class CorrelationIdFilter implements HttpServerFilter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String correlationId = request.getHeaders()
                .getFirst(CORRELATION_ID_HEADER)
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(MDC_KEY, correlationId);

        PropagatedContext propagatedContext = PropagatedContext.getOrEmpty()
                .plus(new CorrelationIdContextElement(correlationId))
                .plus(new MdcPropagationContext());

        try {
            return propagatedContext.propagateCall(() ->
                    Flux.from(chain.proceed(request))
                            .doOnNext(response -> response.getHeaders().add(CORRELATION_ID_HEADER, correlationId))
                            .onErrorResume(throwable -> {
                                logger.error("Unhandled error", throwable);
                                MutableHttpResponse<?> errorResponse = HttpResponse.serverError()
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .header(CORRELATION_ID_HEADER, correlationId);
                                return Flux.just(errorResponse);
                            })
                            .doFinally(signal -> MDC.remove(MDC_KEY))
                            .contextWrite(ctx -> ReactorPropagation.addPropagatedContext(ctx, PropagatedContext.get()))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.TRACING.order();
    }
}
