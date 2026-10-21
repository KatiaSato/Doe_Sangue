package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure

internal fun Throwable.toAuthErrorMessage(fallback: String): String =
    when ((this as? AuthFailure)?.reason) {
        AuthError.INVALID_EMAIL ->
            "Este e-mail não é aceito. Confira o endereço ou informe outro e-mail válido."

        AuthError.EMAIL_NOT_CONFIRMED ->
            "Confirme seu e-mail antes de entrar. Verifique sua caixa de entrada e o spam."

        AuthError.INVALID_CREDENTIALS ->
            "E-mail ou senha incorretos."

        AuthError.ACCOUNT_ALREADY_EXISTS ->
            "Já existe uma conta com este e-mail."

        AuthError.WEAK_PASSWORD ->
            "Escolha uma senha mais forte e confira os requisitos."

        AuthError.RATE_LIMITED ->
            "Muitas tentativas. Aguarde um pouco e tente novamente."

        AuthError.EMAIL_NOT_AUTHORIZED ->
            "O envio de confirmação não está permitido para este endereço. Tente outro e-mail."

        AuthError.NETWORK ->
            "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente."

        AuthError.SAME_PASSWORD ->
            "Escolha uma senha diferente da atual."

        AuthError.SESSION_UNAVAILABLE ->
            "Sua sessão não está disponível. Entre novamente para continuar."

        AuthError.UNKNOWN, null -> fallback
    }