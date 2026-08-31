package br.edu.fatec.doesangue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.edu.fatec.doesangue.ui.navigation.DoeSangueApp
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoeSangueTheme {
                DoeSangueApp()
            }
        }
    }
}
