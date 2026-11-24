package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.GoogleSignInCredential

sealed interface GoogleSignInResult {
    class Success(val credential: GoogleSignInCredential) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoCredential : GoogleSignInResult
    data object Failed : GoogleSignInResult
}
