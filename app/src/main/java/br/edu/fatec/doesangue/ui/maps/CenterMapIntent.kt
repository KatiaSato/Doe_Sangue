package br.edu.fatec.doesangue.ui.maps

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import br.edu.fatec.doesangue.domain.model.DonationCenter

fun createCenterMapIntent(center: DonationCenter): Intent? {
    val coordinates = center.coordinates ?: return null

    val point = "${coordinates.latitude},${coordinates.longitude}"
    val query = Uri.encode(
        "$point (Demonstração: ${center.name})"
    )

    return Intent(
        Intent.ACTION_VIEW,
        // O formato geo: permite indicar coordenadas e um rótulo para o ponto
        "geo:$point?q=$query".toUri(),
    )
}