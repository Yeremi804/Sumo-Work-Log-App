package com.resu.sumo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resu.sumo.ui.AnimatedHomeScreen
import com.resu.sumo.ui.ClockInEntryScreen
import com.resu.sumo.ui.EntryFormScreen
import com.resu.sumo.ui.MainDashboard
import com.resu.sumo.ui.PdfPreviewScreen
import com.resu.sumo.ui.SummaryReportScreen
import com.resu.sumo.ui.WorkLogViewModel
import com.resu.sumo.ui.theme.SumoTheme

enum class Screen {
    HOME,
    DASHBOARD,
    CLOCK_IN,
    SUMMARY,
    PREVIEW,
    ENTRY_FORM
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SumoTheme {
                MainAppNav()
            }
        }
    }
}

@Composable
fun MainAppNav() {
    val viewModel: WorkLogViewModel = viewModel()

    // Intercept System Back Button/Gesture to return to HOME screen!
    BackHandler(enabled = viewModel.currentScreen != Screen.HOME) {
        viewModel.currentScreen = Screen.HOME
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (viewModel.currentScreen) {
            Screen.HOME -> {
                AnimatedHomeScreen(
                    viewModel = viewModel,
                    onOpenOptionA = { viewModel.currentScreen = Screen.CLOCK_IN },
                    onOpenOptionB = { viewModel.currentScreen = Screen.SUMMARY },
                    onOpenOptionC = { viewModel.currentScreen = Screen.PREVIEW }
                )
            }
            Screen.DASHBOARD -> {
                MainDashboard(
                    viewModel = viewModel,
                    onOpenOptionA = { viewModel.currentScreen = Screen.CLOCK_IN },
                    onOpenOptionB = { viewModel.currentScreen = Screen.SUMMARY },
                    onOpenOptionC = { viewModel.currentScreen = Screen.PREVIEW },
                    onBack = { viewModel.currentScreen = Screen.HOME }
                )
            }
            Screen.CLOCK_IN -> {
                ClockInEntryScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.currentScreen = Screen.HOME }
                )
            }
            Screen.SUMMARY -> {
                SummaryReportScreen(
                    viewModel = viewModel,
                    onNavigateToOptionC = { viewModel.currentScreen = Screen.PREVIEW },
                    onBack = { viewModel.currentScreen = Screen.HOME }
                )
            }
            Screen.PREVIEW -> {
                PdfPreviewScreen(
                    viewModel = viewModel,
                    onNavigateToOptionB = { viewModel.currentScreen = Screen.SUMMARY },
                    onBack = { viewModel.currentScreen = Screen.HOME }
                )
            }
            Screen.ENTRY_FORM -> {
                EntryFormScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.currentScreen = Screen.SUMMARY }
                )
            }
        }
    }
}
