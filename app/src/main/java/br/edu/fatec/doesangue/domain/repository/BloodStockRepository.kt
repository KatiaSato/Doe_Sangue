package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.BloodStock

interface BloodStockRepository {
    suspend fun getBloodStocks(): Result<List<BloodStock>>
}
