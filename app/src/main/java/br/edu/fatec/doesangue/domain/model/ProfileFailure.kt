package br.edu.fatec.doesangue.domain.model

enum class ProfileError {
    SESSION_UNAVAILABLE, SESSION_RETRYING, MISSING, ACCESS_DENIED, INVALID_DATA, SCHEMA_NOT_READY, NETWORK, UNKNOWN,
}

class ProfileFailure(val reason: ProfileError) : Exception()
