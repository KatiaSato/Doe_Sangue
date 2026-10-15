package br.edu.fatec.doesangue.domain.model

class AuthFailure(
    val reason: AuthError,
) : Exception()