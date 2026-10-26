package br.edu.fatec.doesangue.presentation.common

import br.edu.fatec.doesangue.domain.model.DataError
import br.edu.fatec.doesangue.domain.model.DataFailure
import br.edu.fatec.doesangue.domain.model.ProfileFailure
import br.edu.fatec.doesangue.presentation.profile.toProfileMessage

internal fun Throwable.toDataMessage(fallback: String): String {
    if (this is ProfileFailure) return toProfileMessage(fallback)
    return when ((this as? DataFailure)?.reason) {
        DataError.NETWORK -> "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente."
        DataError.SESSION_UNAVAILABLE -> "Sua sessão não está disponível. Entre novamente para continuar."
        DataError.SESSION_RETRYING -> "Estamos tentando renovar sua sessão. Aguarde e tente novamente."
        DataError.ACCESS_DENIED -> "Você não tem permissão para realizar esta operação."
        DataError.SERVICE_UNAVAILABLE -> "Este serviço está indisponível no momento. Tente novamente mais tarde."
        DataError.INVALID_DATA -> "Os dados da solicitação são inválidos. Volte e confira sua escolha."
        DataError.BUSY -> "A operação encontrou outra atualização em andamento. Tente novamente."
        DataError.PROFILE_REQUIRED -> "Seu perfil precisa estar cadastrado. Acesse Perfil antes de continuar."
        DataError.SLOT_UNAVAILABLE -> "Este horário não está mais disponível. Volte e escolha outro horário."
        DataError.SLOT_STARTED -> "O horário escolhido já começou. Volte e escolha outro horário."
        DataError.SLOT_FULL -> "Este horário ficou sem vagas. Volte e escolha outro horário."
        DataError.ALREADY_BOOKED -> "Você já possui um agendamento ativo neste horário. Consulte suas doações."
        DataError.REQUEST_CONFLICT -> "Este pedido está vinculado a outro horário. Volte e selecione o horário novamente."
        DataError.APPOINTMENT_UNAVAILABLE -> "Este agendamento não está disponível para sua conta. Volte à lista e atualize."
        DataError.CANCELLATION_CLOSED -> "O prazo para cancelar terminou porque o horário já começou."
        DataError.UNKNOWN, null -> fallback
    }
}
