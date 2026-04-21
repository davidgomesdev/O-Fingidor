package me.davidgomesdev.pessoafaladora.backend.model

@Suppress("unused")
enum class Persona(
    val displayName: String,
    val category: PersonaCategory,
    val systemPromptFilename: String = "system_prompt.txt"
) {
    /**
     * Skips RAG
     */
    O_FINGIDOR("O Fingidor", PersonaCategory.DEV),
    FERNANDO_PESSOA("Fernando Pessoa", PersonaCategory.ORTONIMO),
    ALBERTO_CAEIRO("Alberto Caeiro", PersonaCategory.HETERONIMO, "system_message_alberto_caeiro.txt"),
    ALVARO_DE_CAMPOS("Álvaro de Campos", PersonaCategory.HETERONIMO, "system_message_alvaro_de_campos.txt"),
    RICARDO_REIS("Ricardo Reis", PersonaCategory.HETERONIMO, "system_message_ricardo_reis.txt"),
    BERNARDO_SOARES("Bernardo Soares", PersonaCategory.SEMI_HETERONIMO, "system_message_bernardo_soares.txt");

    val codeName = this.name.lowercase()
}

@Suppress("unused")
enum class PersonaCategory(val label: String) {
    DEV("Dev"),
    ORTONIMO("Ortónimo"),
    HETERONIMO("Heterónimos"),
    SEMI_HETERONIMO("Semi-heterónimo")
}
