package me.davidgomesdev.ofingidor.backend.llm

import jakarta.enterprise.context.RequestScoped
import me.davidgomesdev.ofingidor.backend.model.Persona

@RequestScoped
class PersonaContext {
    var persona: Persona? = null
}

