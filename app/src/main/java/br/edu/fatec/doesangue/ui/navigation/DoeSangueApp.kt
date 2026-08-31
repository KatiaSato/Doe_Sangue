package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import br.edu.fatec.doesangue.ui.screens.auth.EmailVerificationScreen
import br.edu.fatec.doesangue.ui.screens.auth.ForgotPasswordScreen
import br.edu.fatec.doesangue.ui.screens.auth.LoginScreen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep1Screen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep2Screen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep3Screen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep4Screen
import br.edu.fatec.doesangue.ui.screens.campaigns.CampaignDetailScreen
import br.edu.fatec.doesangue.ui.screens.campaigns.CampaignsScreen
import br.edu.fatec.doesangue.ui.screens.centers.CenterDetailScreen
import br.edu.fatec.doesangue.ui.screens.centers.CentersScreen
import br.edu.fatec.doesangue.ui.screens.donations.DonationAppointmentsScreen
import br.edu.fatec.doesangue.ui.screens.donations.DonationHistoryScreen
import br.edu.fatec.doesangue.ui.screens.home.HomeScheduledScreen
import br.edu.fatec.doesangue.ui.screens.home.HomeScreen
import br.edu.fatec.doesangue.ui.screens.onboarding.SplashScreen
import br.edu.fatec.doesangue.ui.screens.onboarding.WelcomeScreen
import br.edu.fatec.doesangue.ui.screens.profile.AboutScreen
import br.edu.fatec.doesangue.ui.screens.profile.ContactScreen
import br.edu.fatec.doesangue.ui.screens.profile.EditProfileScreen
import br.edu.fatec.doesangue.ui.screens.profile.FaqScreen
import br.edu.fatec.doesangue.ui.screens.profile.NotificationsScreen
import br.edu.fatec.doesangue.ui.screens.profile.ProfileScreen
import br.edu.fatec.doesangue.ui.screens.profile.SecurityScreen
import br.edu.fatec.doesangue.ui.screens.profile.SettingsScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.AppointmentCancelledScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.AppointmentDetailScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.CancelAppointmentScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.EditRoutineScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleCenterScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleConfirmScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleDateScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleRecurrenceScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleSuccessScreen

@Composable
fun DoeSangueApp(navigator: AppNavigator = rememberAppNavigator()) {
    fun go(route: AppRoute) {
        if (route in rootRoutes) navigator.replaceRoot(route) else navigator.navigate(route)
    }

    when (navigator.current) {
        AppRoute.Splash -> SplashScreen { navigator.replaceRoot(AppRoute.Welcome) }
        AppRoute.Welcome -> WelcomeScreen(
            onStart = { navigator.navigate(AppRoute.Register1) },
            onLogin = { navigator.navigate(AppRoute.Login) },
        )
        AppRoute.Login -> LoginScreen(
            onEnter = { navigator.replaceRoot(AppRoute.Home) },
            onForgot = { navigator.navigate(AppRoute.ForgotPassword) },
            onRegister = { navigator.navigate(AppRoute.Register1) },
        )
        AppRoute.Register1 -> RegisterStep1Screen(navigator::back, { navigator.navigate(AppRoute.Register2) }, { navigator.navigate(AppRoute.Login) })
        AppRoute.Register2 -> RegisterStep2Screen(navigator::back) { navigator.navigate(AppRoute.Register3) }
        AppRoute.Register3 -> RegisterStep3Screen(navigator::back) { navigator.navigate(AppRoute.Register4) }
        AppRoute.Register4 -> RegisterStep4Screen(navigator::back) { navigator.navigate(AppRoute.EmailVerification) }
        AppRoute.EmailVerification -> EmailVerificationScreen { navigator.replaceRoot(AppRoute.Login) }
        AppRoute.ForgotPassword -> ForgotPasswordScreen(navigator::back)

        AppRoute.Home -> HomeScreen(::go)
        AppRoute.HomeScheduled -> HomeScheduledScreen(::go)

        AppRoute.ScheduleCenter -> ScheduleCenterScreen(navigator::back, { navigator.navigate(AppRoute.ScheduleDate) }, ::go)
        AppRoute.ScheduleDate -> ScheduleDateScreen(navigator::back) { navigator.navigate(AppRoute.ScheduleRecurrence) }
        AppRoute.ScheduleRecurrence -> ScheduleRecurrenceScreen(navigator::back) { navigator.navigate(AppRoute.ScheduleConfirm) }
        AppRoute.ScheduleConfirm -> ScheduleConfirmScreen(navigator::back) { navigator.navigate(AppRoute.ScheduleSuccess) }
        AppRoute.ScheduleSuccess -> ScheduleSuccessScreen { navigator.replaceRoot(AppRoute.HomeScheduled) }

        AppRoute.DonationsHistory -> DonationHistoryScreen(navigator::back, { navigator.replaceRoot(AppRoute.DonationsAppointments) }, ::go)
        AppRoute.DonationsAppointments -> DonationAppointmentsScreen(navigator::back, { navigator.replaceRoot(AppRoute.DonationsHistory) }, { navigator.navigate(AppRoute.AppointmentDetail) }, ::go)
        AppRoute.AppointmentDetail -> AppointmentDetailScreen(
            navigator::back,
            { navigator.navigate(AppRoute.ScheduleDate) },
            { navigator.navigate(AppRoute.EditRoutine) },
            { navigator.navigate(AppRoute.CancelAppointment) },
            ::go,
        )
        AppRoute.CancelAppointment -> CancelAppointmentScreen(navigator::back, { navigator.navigate(AppRoute.AppointmentCancelled) }, ::go)
        AppRoute.EditRoutine -> EditRoutineScreen(navigator::back)
        AppRoute.AppointmentCancelled -> AppointmentCancelledScreen(
            { navigator.replaceRoot(AppRoute.ScheduleCenter) },
            { navigator.replaceRoot(AppRoute.Home) },
        )

        AppRoute.Campaigns -> CampaignsScreen(navigator::back, { navigator.navigate(AppRoute.CampaignDetail) }, { navigator.navigate(AppRoute.ScheduleCenter) }, ::go)
        AppRoute.CampaignDetail -> CampaignDetailScreen(navigator::back, { navigator.navigate(AppRoute.ScheduleCenter) }, ::go)
        AppRoute.Centers -> CentersScreen(navigator::back, { navigator.navigate(AppRoute.CenterDetail) }, ::go)
        AppRoute.CenterDetail -> CenterDetailScreen(navigator::back, { navigator.navigate(AppRoute.ScheduleCenter) }, ::go)

        AppRoute.Profile -> ProfileScreen(
            navigator::back,
            { navigator.replaceRoot(AppRoute.DonationsHistory) },
            { navigator.navigate(AppRoute.AppointmentDetail) },
            { navigator.navigate(AppRoute.Settings) },
            ::go,
        )
        AppRoute.Settings -> SettingsScreen(
            navigator::back,
            { navigator.navigate(AppRoute.EditProfile) },
            { navigator.navigate(AppRoute.Security) },
            { navigator.navigate(AppRoute.Notifications) },
            { navigator.navigate(AppRoute.Faq) },
            { navigator.navigate(AppRoute.Contact) },
            { navigator.navigate(AppRoute.About) },
            ::go,
        )
        AppRoute.EditProfile -> EditProfileScreen(navigator::back)
        AppRoute.Security -> SecurityScreen(navigator::back)
        AppRoute.Notifications -> NotificationsScreen(navigator::back)
        AppRoute.Faq -> FaqScreen(navigator::back)
        AppRoute.Contact -> ContactScreen(navigator::back)
        AppRoute.About -> AboutScreen(navigator::back)
    }
}

private val rootRoutes = setOf(
    AppRoute.Home,
    AppRoute.ScheduleCenter,
    AppRoute.DonationsHistory,
    AppRoute.Centers,
    AppRoute.Profile,
)

