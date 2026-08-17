package com.training.config;

import com.training.data.request.AddDocumentRequest;
import com.training.data.result.ParsedResult;
import io.micronaut.serde.annotation.SerdeImport;

@SerdeImport(ParsedResult.class)
@SerdeImport(AddDocumentRequest.class)
public class SerdeImports {
}
