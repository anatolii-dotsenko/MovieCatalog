package com.example.moviecatalog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import com.example.moviecatalog.core.navigation.MovieCatalogNavHost
import com.example.moviecatalog.data.local.AppThemeMode
import com.example.moviecatalog.data.local.ProvideAppLocalization
import com.example.moviecatalog.ui.theme.MovieCatalogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        val app = application as MovieCatalogApp
        setContent {
            ProvideAppLocalization(app.localizationService) { themeMode ->
                val darkTheme = when (themeMode) {
                    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                }
                MovieCatalogTheme(darkTheme = darkTheme) {
                    MovieCatalogNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val app = application as MovieCatalogApp
        lifecycleScope.launch {
            app.authService.handleOAuthRedirect(uri)
        }
    }
}
