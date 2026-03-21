package me.davidgomesdev.pessoafaladora.backend.observability

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.trace.Span

fun attributes(modifier: AttributesBuilder.() -> Unit): Attributes = Attributes.builder().apply(modifier).build()

fun span(): Span = Span.current()
