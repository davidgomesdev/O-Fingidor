package me.davidgomesdev.ofingidor.backend.llm.tool

import dev.langchain4j.agent.tool.Tool
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.Month

@Singleton
class DateTool {
    @Tool("Diz a data do dia de hoje")
    fun todayDate(): String =
        LocalDate.now().run {
            "$dayOfMonth de ${month.toPortuguese()} de $year"
        }
}

fun Month.toPortuguese(): String = when (this) {
    Month.JANUARY -> "Janeiro"
    Month.FEBRUARY -> "Fevereiro"
    Month.MARCH -> "Março"
    Month.APRIL -> "Abril"
    Month.MAY -> "Maio"
    Month.JUNE -> "Junho"
    Month.JULY -> "Julho"
    Month.AUGUST -> "Agosto"
    Month.SEPTEMBER -> "Setembro"
    Month.OCTOBER -> "Outubro"
    Month.NOVEMBER -> "Novembro"
    Month.DECEMBER -> "Dezembro"
}
