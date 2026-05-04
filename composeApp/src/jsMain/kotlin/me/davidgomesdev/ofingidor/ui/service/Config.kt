package me.davidgomesdev.ofingidor.ui.service

actual fun getHost(): String =
    js("window.HOST") as? String ?: (js("window.location.hostname") as String)

actual fun isDevMode(): Boolean =
    (js("window.IS_DEV_MODE") as? String) != "false"

actual fun isMobileDevice(): Boolean {
    val userAgent = js("navigator.userAgent") as String
    return userAgent.contains(Regex("Mobile|Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini"))
}

@Suppress("unused")
actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}

