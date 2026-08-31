package br.edu.fatec.doesangue.domain.model

data class DonationOverview(
    val centers: List<DonationCenter>,
    val needs: List<BloodNeed>,
)

