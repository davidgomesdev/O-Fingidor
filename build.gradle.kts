plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.allopen") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    kotlin("plugin.jpa") version "2.3.0" apply false
    id("io.quarkus") version "3.32.2" apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
