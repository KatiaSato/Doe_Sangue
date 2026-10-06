package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonationCenter

// Define a operação de consulta às unidades,
// sem depender da tecnologia usada para buscá-las.
interface DonationCenterRepository {
    suspend fun getCenters(): Result<List<DonationCenter>>
}
