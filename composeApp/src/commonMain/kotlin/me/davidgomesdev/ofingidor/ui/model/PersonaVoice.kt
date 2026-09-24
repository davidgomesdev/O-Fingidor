package me.davidgomesdev.ofingidor.ui.model

import me.davidgomesdev.ofingidor.shared.dto.Persona

/** How a persona is presented in the constellation: split name, short name and one real line of theirs. */
data class PersonaVoice(
    val firstName: String,
    val lastName: String,
    val shortName: String,
    val line: String,
)

fun Persona.voice(): PersonaVoice = when (this) {
    Persona.FERNANDO_PESSOA -> PersonaVoice("Fernando", "Pessoa", "Pessoa", "O que em mim sente ’stá pensando.")
    Persona.ALVARO_DE_CAMPOS -> PersonaVoice("Álvaro", "de Campos", "Campos", "Sentir tudo de todas as maneiras.")
    Persona.RICARDO_REIS -> PersonaVoice("Ricardo", "Reis", "Reis", "Para ser grande, sê inteiro.")
    Persona.BERNARDO_SOARES -> PersonaVoice("Bernardo", "Soares", "Soares", "Viajar? Para viajar basta existir.")
    Persona.ALBERTO_CAEIRO -> PersonaVoice("Alberto", "Caeiro", "Caeiro", "Sou do tamanho do que vejo.")
    Persona.O_FINGIDOR -> PersonaVoice("O", "Fingidor", "Fingidor", "O poeta é um fingidor.")
}

/** The five voices placed on the points of the constellation's pentagram, clockwise from the top. */
val constellationPersonas: List<Persona> = listOf(
    Persona.FERNANDO_PESSOA,
    Persona.ALVARO_DE_CAMPOS,
    Persona.RICARDO_REIS,
    Persona.BERNARDO_SOARES,
    Persona.ALBERTO_CAEIRO,
)

data class HeroQuote(val text: String, val citation: String)

val chatHeroQuote = HeroQuote(
    text = "O poeta é um fingidor.\nFinge tão completamente\nQue chega a fingir que é dor\nA dor que deveras sente.",
    citation = "AUTOPSICOGRAFIA · 1931",
)

val debateHeroQuotes: List<HeroQuote> = listOf(
    HeroQuote(
        "Vivem em nós inúmeros;\nSe penso ou sinto, ignoro\nQuem é que pensa ou sente.\nSou somente o lugar\nOnde se sente ou pensa.",
        "RICARDO REIS · ODES",
    ),
    HeroQuote(
        "Cada um de nós é vários, é muitos,\né uma prolixidade de si mesmos.",
        "BERNARDO SOARES · LIVRO DO DESASSOSSEGO",
    ),
    HeroQuote(
        "Não sei quantas almas tenho.\nCada momento mudei.\nContinuamente me estranho.\nNunca me vi nem achei.",
        "FERNANDO PESSOA · NÃO SEI QUANTAS ALMAS TENHO",
    ),
    HeroQuote(
        "Multipliquei-me, para me sentir,\nPara me sentir, precisei sentir tudo,\nTransbordei, não fiz senão extravasar-me…",
        "ÁLVARO DE CAMPOS · PASSAGEM DAS HORAS",
    ),
    HeroQuote("Sê plural como o universo!", "FERNANDO PESSOA"),
)

/** Picks a random quote index that differs from [current], so re-entering debate always shows a new quote. */
fun nextQuoteIndex(current: Int, size: Int, random: (Int) -> Int): Int {
    if (size <= 1) return 0
    return (current + 1 + random(size - 1)) % size
}
