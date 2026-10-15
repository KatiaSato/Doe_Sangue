package br.edu.fatec.doesangue.presentation.common

sealed interface ReadUiState<out T> {
    data object Loading : ReadUiState<Nothing>
    data class Loaded<T>(val data: T) : ReadUiState<T>
    data class Error(val message: String) : ReadUiState<Nothing>
}

internal fun <T> Result<T>.toReadState(message: String): ReadUiState<T> = fold(
    onSuccess = { ReadUiState.Loaded(it) },
    onFailure = { ReadUiState.Error(message) },
)
