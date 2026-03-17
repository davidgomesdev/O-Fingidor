package me.davidgomesdev.model

@Suppress("unused")
enum class Persona(val displayName: String, val category: PersonaCategory) {
    O_FINGIDOR("O Fingidor", PersonaCategory.DEV),
    FERNANDO_PESSOA("Fernando Pessoa", PersonaCategory.ORTONIMO),
    ALBERTO_CAEIRO("Alberto Caeiro", PersonaCategory.HETERONIMO),
    ALVARO_DE_CAMPOS("Álvaro de Campos", PersonaCategory.HETERONIMO),
    RICARDO_REIS("Ricardo Reis", PersonaCategory.HETERONIMO),
    BERNARDO_SOARES("Bernardo Soares", PersonaCategory.SEMI_HETERONIMO);

    val codeName = this.name.lowercase()
}

@Suppress("unused")
enum class PersonaCategory(val label: String) {
    DEV("Dev"),
    ORTONIMO("Ortónimo"),
    HETERONIMO("Heterónimos"),
    SEMI_HETERONIMO("Semi-heterónimo")
}
