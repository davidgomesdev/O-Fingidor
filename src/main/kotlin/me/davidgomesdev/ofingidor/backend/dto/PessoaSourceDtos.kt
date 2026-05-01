package me.davidgomesdev.ofingidor.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class PessoaCategoryDto(
    val id: Int,
    val title: String,
    val subcategories: List<PessoaCategoryDto>,
    val texts: List<PessoaTextDto>?,
)

@Serializable
data class PessoaTextDto(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
)
