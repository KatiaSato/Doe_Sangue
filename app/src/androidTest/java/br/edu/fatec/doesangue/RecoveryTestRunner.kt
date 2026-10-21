package br.edu.fatec.doesangue

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import br.edu.fatec.doesangue.di.AppContainer

/** Activity real com dependências explícitas de teste; nunca inicializa o cliente real. */
class RecoveryTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, RecoveryTestApplication::class.java.name, context)
}

class RecoveryTestApplication : DoeSangueApplication() {
    companion object {
        var container: AppContainer? = null
        var pending = false
    }
    override val appContainer: AppContainer
        get() = checkNotNull(container) { "Configure dependências fictícias antes de abrir MainActivity." }
    override fun isRecoveryPending() = pending
    override fun setRecoveryPending(pending: Boolean): Boolean {
        Companion.pending = pending
        return true
    }
}
