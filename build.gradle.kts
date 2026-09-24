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
    alias(libs.plugins.ktlint)
}

allprojects {
    apply(
        plugin =
            rootProject.libs.plugins.ktlint
                .get()
                .pluginId,
    )

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(
            rootProject.libs.versions.ktlint
                .get(),
        )
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }
}
