package me.davidgomesdev.ofingidor.ui.service

actual fun isNetworkException(t: Throwable): Boolean = t is Exception
