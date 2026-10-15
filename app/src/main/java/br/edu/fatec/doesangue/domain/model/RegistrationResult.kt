package br.edu.fatec.doesangue.domain.model

// Sem sessão, a pessoa deve confirmar o endereço e depois entrar.
data class RegistrationResult(val authenticatedUser: AuthenticatedUser?)
