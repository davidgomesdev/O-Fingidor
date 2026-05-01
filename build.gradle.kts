plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.allopen") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val webBundlerVersion = "1.9.3"
val langchainMarkdownVersion = "1.8.0-beta15"
val kotlinxSerializationJson = "1.9.0"
val mutinyVersion = "2.0.0"
val otelExtension = "1.59.0"
val qdrantLangchainVersion = "1.12.2-beta22"
val mockito = "5.2.1"
val nimbusJoseJwtVersion = "10.7"
val arrowVersion = "2.2.2.1"

// Fixes Quarkus' BOM imposing a broken Qdrant version
configurations.all {
    resolutionStrategy {
        force("dev.langchain4j:langchain4j-qdrant:$qdrantLangchainVersion")
    }
}

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:quarkus-langchain4j-bom:${quarkusPlatformVersion}"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJson")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-qute")
    implementation("io.quarkiverse.web-bundler:quarkus-web-bundler:$webBundlerVersion")
    implementation("io.quarkiverse.langchain4j:quarkus-langchain4j-core")
    implementation("dev.langchain4j:langchain4j-ollama")
    implementation("dev.langchain4j:langchain4j-anthropic")
    implementation("dev.langchain4j:langchain4j-qdrant:$qdrantLangchainVersion")
    implementation("dev.langchain4j:langchain4j-document-parser-markdown:$langchainMarkdownVersion")
    implementation("io.smallrye.reactive:mutiny-kotlin:$mutinyVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.opentelemetry:opentelemetry-extension-kotlin:$otelExtension")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusJoseJwtVersion")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("com.github.f4b6a3:uuid-creator:6.0.0")
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockito")
    testImplementation("io.quarkus:quarkus-jdbc-h2")
}

group = "me.davidgomesdev.pessoafaladora"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        javaParameters = true
    }
}
