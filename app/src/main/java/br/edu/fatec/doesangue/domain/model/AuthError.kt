package br.edu.fatec.doesangue.domain.model

enum class AuthError {
    INVALID_EMAIL,
    EMAIL_NOT_CONFIRMED,
    INVALID_CREDENTIALS,
    ACCOUNT_ALREADY_EXISTS,
    WEAK_PASSWORD,
    RATE_LIMITED,
    EMAIL_NOT_AUTHORIZED,
    NETWORK,
    UNKNOWN,
}