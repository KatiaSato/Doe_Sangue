package br.edu.fatec.doesangue.domain.model

// Representa os dados do perfil do doador no aplicativo.
data class DonorProfile (
    val id: String,
    val displayName: String,        // nome que exibiremos na tela; corresponde a nome_exibicao
)