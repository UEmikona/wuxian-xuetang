package com.example.buildlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.buildlearn.data.prefs.ThemeMode
import com.example.buildlearn.ui.navigation.BuildLearnNavHost
import com.example.buildlearn.ui.theme.BuildLearnTheme
import dagger.hilt.android.AndroidEntryPoint
import com.example.buildlearn.ui.AppViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val themeMode by appViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            BuildLearnTheme(darkTheme = darkTheme) {
                BuildLearnNavHost(appViewModel = appViewModel)
            }
        }
    }
}
