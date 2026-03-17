package me.davidgomesdev.llm

import jakarta.enterprise.context.RequestScoped
import me.davidgomesdev.model.Persona

@RequestScoped
class PersonaContext {
    var persona: Persona? = null
}

