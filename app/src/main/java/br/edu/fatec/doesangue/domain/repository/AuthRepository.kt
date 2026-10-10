package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser

// Define a operação de login sem depender do Supabase.
/*
  Esse contrato diz: “recebo e-mail e senha e devolvo o usuário autenticado ou uma falha”.
- suspend permite aguardar a operação de autenticação dentro de uma corrotina.
- Result<AuthenticatedUser> representa sucesso com o usuário ou falha.
- interface define a operação; a implementação na camada data fará a comunicação com o Supabase.
*/
interface AuthRepository {

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthenticatedUser>
}