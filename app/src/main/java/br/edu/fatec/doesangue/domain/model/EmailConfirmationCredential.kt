package br.edu.fatec.doesangue.domain.model

/** Credenciais transitórias de retorno; não persistir nem exibir seus valores. */
sealed interface EmailConfirmationCredential {
    data class Code(val value: String) : EmailConfirmationCredential {
        override fun toString() = "Code([redacted])"
    }
    data class AccessToken(val value: String) : EmailConfirmationCredential {
        override fun toString() = "AccessToken([redacted])"
    }
}
