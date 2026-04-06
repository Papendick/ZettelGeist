package de.zettelgeist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import de.zettelgeist.app.ui.navigation.ZettelGeistNavGraph
import de.zettelgeist.app.ui.theme.ZettelGeistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkModeOverride by remember { mutableStateOf<Boolean?>(null) }
            val isDark = darkModeOverride ?: systemDark

            ZettelGeistTheme(darkTheme = isDark) {
                ZettelGeistNavGraph(
                    isDarkMode = isDark,
                    onToggleDarkMode = {
                        darkModeOverride = !isDark
                    }
                )
            }
        }
    }
}
