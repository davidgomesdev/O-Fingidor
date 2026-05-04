package me.davidgomesdev.ofingidor.ui.service

expect fun getHost(): String
expect fun isDevMode(): Boolean
expect fun isMobileDevice(): Boolean
expect fun openUrl(url: String)
