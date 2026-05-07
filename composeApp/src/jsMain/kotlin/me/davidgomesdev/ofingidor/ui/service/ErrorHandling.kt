package me.davidgomesdev.ofingidor.ui.service

@OptIn(ExperimentalWasmJsInterop::class)
actual fun isNetworkException(t: Throwable): Boolean =
    t is Exception || t is JsException
