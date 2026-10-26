package br.edu.fatec.doesangue.presentation.profile

import br.edu.fatec.doesangue.domain.model.ProfileError
import br.edu.fatec.doesangue.domain.model.ProfileFailure

internal fun Throwable.toProfileMessage(fallback: String): String = when ((this as? ProfileFailure)?.reason) {
    ProfileError.SESSION_UNAVAILABLE -> "Sua sessão não está disponível. Entre novamente para continuar."
    ProfileError.SESSION_RETRYING -> "Estamos tentando renovar sua sessão. Aguarde e tente novamente."
    ProfileError.MISSING -> "Seu perfil não foi encontrado. Volte ao Perfil e tente novamente."
    ProfileError.ACCESS_DENIED -> "Você não tem permissão para realizar esta operação no perfil."
    ProfileError.INVALID_DATA -> "Confira os dados informados antes de salvar."
    ProfileError.SCHEMA_NOT_READY -> "O serviço de perfil está indisponível no momento. Tente novamente mais tarde."
    ProfileError.NETWORK -> "Verifique sua conexão e tente novamente."
    ProfileError.UNKNOWN, null -> fallback
}
