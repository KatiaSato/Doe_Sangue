package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvailableDonationDateDto(@SerialName("dia") val date: String)
