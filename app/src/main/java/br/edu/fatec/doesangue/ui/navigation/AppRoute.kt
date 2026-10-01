package br.edu.fatec.doesangue.ui.navigation

import kotlinx.serialization.Serializable
import br.edu.fatec.doesangue.presentation.restrictions.RestrictionCategory

// Define os destinos do aplicativo.
// As ações de avançar e voltar ficam nos grafos de navegação.
sealed interface AppRoute {

    // Abertura e autenticação
    @Serializable
    data object Splash : AppRoute

    @Serializable
    data object Welcome : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Register1 : AppRoute

    @Serializable
    data object Register2 : AppRoute

    @Serializable
    data object Register3 : AppRoute

    @Serializable
    data object Register4 : AppRoute

    @Serializable
    data object EmailVerification : AppRoute

    @Serializable
    data object ForgotPassword : AppRoute

    // Início
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object HomeScheduled : AppRoute

    @Serializable
    data object Restrictions : AppRoute

    @Serializable
    data class RestrictionDetail(val category: RestrictionCategory) : AppRoute

    // Agendamento
    @Serializable
    data object ScheduleCenter : AppRoute

    @Serializable
    data object ScheduleDate : AppRoute

    @Serializable
    data object ScheduleRecurrence : AppRoute

    @Serializable
    data object ScheduleConfirm : AppRoute

    @Serializable
    data object ScheduleSuccess : AppRoute

    // Doações e acompanhamento
    @Serializable
    data object DonationsHistory : AppRoute

    @Serializable
    data object DonationsAppointments : AppRoute

    @Serializable
    data object AppointmentDetail : AppRoute

    @Serializable
    data object CancelAppointment : AppRoute

    @Serializable
    data object EditRoutine : AppRoute

    @Serializable
    data object AppointmentCancelled : AppRoute

    // Campanhas
    @Serializable
    data object Campaigns : AppRoute

    @Serializable
    data object CampaignDetail : AppRoute

    // Unidades
    @Serializable
    data object Centers : AppRoute

    @Serializable
    data object CenterDetail : AppRoute

    // Perfil e configurações
    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object EditProfile : AppRoute

    @Serializable
    data object Security : AppRoute

    @Serializable
    data object Notifications : AppRoute

    @Serializable
    data object Faq : AppRoute

    @Serializable
    data object Contact : AppRoute

    @Serializable
    data object About : AppRoute
}
