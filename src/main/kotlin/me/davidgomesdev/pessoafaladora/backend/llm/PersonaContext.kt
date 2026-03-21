package me.davidgomesdev.pessoafaladora.backend.llm

import jakarta.enterprise.context.RequestScoped
import me.davidgomesdev.pessoafaladora.backend.model.Persona

@RequestScoped
class PersonaContext {
    var persona: Persona? = null
}

