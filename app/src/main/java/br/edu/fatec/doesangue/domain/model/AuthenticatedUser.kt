package br.edu.fatec.doesangue.domain.model

data class AuthenticatedUser (
    val id: String,
    val requiresProfileCheck: Boolean = false,
)
