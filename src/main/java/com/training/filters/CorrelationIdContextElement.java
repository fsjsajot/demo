package com.training.filters;

import io.micronaut.core.propagation.PropagatedContextElement;

public record CorrelationIdContextElement(String correlationId) implements PropagatedContextElement {}
