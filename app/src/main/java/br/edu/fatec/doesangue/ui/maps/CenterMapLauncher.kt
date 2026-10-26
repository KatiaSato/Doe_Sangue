package br.edu.fatec.doesangue.ui.maps

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import br.edu.fatec.doesangue.domain.model.DonationCenter

fun openCenterMap(context: Context, center: DonationCenter) {
    val intent = createCenterMapIntent(center)

    if (intent == null) {
        Toast.makeText(
            context,
            "Localização não cadastrada.",
            Toast.LENGTH_SHORT,
        ).show()
        return
    }

    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Nenhum aplicativo de mapas disponível.",
            Toast.LENGTH_SHORT,
        ).show()
    } catch (_: SecurityException) {
        Toast.makeText(
            context,
            "O aparelho não permitiu abrir o mapa.",
            Toast.LENGTH_SHORT,
        ).show()
    }
}