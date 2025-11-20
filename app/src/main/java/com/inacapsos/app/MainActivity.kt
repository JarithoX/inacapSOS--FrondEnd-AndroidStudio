package com.inacapsos.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.inacapsos.app.navigation.AppNavHost
import com.inacapsos.app.ui.theme.InacapSOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            InacapSOSTheme {
                // Aplicar el estilo de la barra de estado de forma global
                ApplySystemUiColors()
                AppNavHost()
            }
        }
    }
}

@Composable
private fun ApplySystemUiColors() {
    val view = LocalView.current
    // Usamos el color de la superficie del tema con una ligera transparencia
    val statusBarColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    
    // Decidimos si los iconos de la barra de estado deben ser oscuros o claros.
    // Si el color de fondo de la barra es claro (luminancia > 0.5), usamos iconos oscuros.
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    // Este efecto se aplica solo cuando el composable entra en la composición.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
        }
    }
}
