package me.davidgomesdev.ofingidor.ui.service

actual fun getHost(): String =
    System.getenv("HOST") ?: DEFAULT_HOST

actual fun isMobileDevice(): Boolean = false

actual fun openUrl(url: String) {
    java.awt.Desktop.getDesktop().browse(java.net.URI(url))
}

