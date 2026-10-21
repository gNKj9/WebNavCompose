package upca.example.mynews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import upca.example.mynews.ui.theme.MyNewsTheme

/**
 * A Activity só monta o tema e a app. O `Scaffold`, o `NavHost` e o ViewModel vivem em
 * MyNewsApp.kt — antes estavam aqui dentro, o que impedia reutilizar a app num preview.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyNewsTheme {
                MyNewsApp()
            }
        }
    }
}
