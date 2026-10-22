package br.edu.fatec.doesangue.presentation.profile

import br.edu.fatec.doesangue.domain.model.ProfileError
import br.edu.fatec.doesangue.domain.model.ProfileFailure

internal fun Throwable.toProfileMessage(fallback: String): String = when ((this as? ProfileFailure)?.reason) {
    ProfileError.SESSION_UNAVAILABLE -> "Sua sessão não está disponível. Entre novamente para continuar."
    ProfileError.MISSING -> "Seu perfil não foi encontrado. Volte ao Perfil e tente novamente."
    ProfileError.ACCESS_DENIED -> "Não foi possível acessar seu perfil. Entre novamente para continuar."
    ProfileError.INVALID_DATA -> "Confira os dados informados antes de salvar."
    ProfileError.SCHEMA_NOT_READY -> "A edição do perfil ainda não está disponível. Tente novamente mais tarde."
    ProfileError.NETWORK -> "Verifique sua conexão e tente novamente."
    ProfileError.UNKNOWN, null -> fallback
}
