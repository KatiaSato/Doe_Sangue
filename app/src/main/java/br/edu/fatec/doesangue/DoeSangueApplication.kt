package br.edu.fatec.doesangue

import android.app.Application
import br.edu.fatec.doesangue.di.AppContainer

open class DoeSangueApplication : Application() {

    open val appContainer: AppContainer by lazy {
        AppContainer()
    }

    // Só um marcador, sem código, senha ou token. Recuperação interrompida não abre Home.
    open fun isRecoveryPending(): Boolean =
        getSharedPreferences("recovery_flow", MODE_PRIVATE).getBoolean("pending", false)

    open fun setRecoveryPending(pending: Boolean): Boolean =
        getSharedPreferences("recovery_flow", MODE_PRIVATE).edit()
            .putBoolean("pending", pending).commit()
}
