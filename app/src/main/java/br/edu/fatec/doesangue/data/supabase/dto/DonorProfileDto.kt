package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Representa os dados do perfil recebidos do Supabase
/*
  O DTO representa os dados que chegam pela API:
- @Serializable permite transformar o JSON recebido em um objeto Kotlin.
- @SerialName("nome_exibicao") conecta o nome usado no banco à propriedade displayName.
*/
@Serializable
data class DonorProfileDto (
    val id: String,

    @SerialName("nome_exibicao")
    val displayName: String,
)