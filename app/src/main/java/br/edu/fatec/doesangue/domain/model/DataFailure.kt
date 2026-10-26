package br.edu.fatec.doesangue.domain.model

/** Causas conhecidas fora do Auth. Não carrega resposta, SQL ou dados pessoais. */
enum class DataError {
    NETWORK, SESSION_UNAVAILABLE, ACCESS_DENIED, SERVICE_UNAVAILABLE, INVALID_DATA, BUSY,
    PROFILE_REQUIRED, SLOT_UNAVAILABLE, SLOT_STARTED, SLOT_FULL, ALREADY_BOOKED,
    REQUEST_CONFLICT, APPOINTMENT_UNAVAILABLE, CANCELLATION_CLOSED, UNKNOWN,
}

class DataFailure(val reason: DataError) : Exception()
