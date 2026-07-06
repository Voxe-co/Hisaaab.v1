package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.navigation.HisaabNavigation
import com.example.ui.theme.HisaabTheme

class MainActivity : ComponentActivity() {
    private val borrowerIdToOpenState = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable true full-screen Edge-to-Edge immersion
        enableEdgeToEdge()

        val initialId = intent?.getLongExtra("borrower_id", -1L)?.takeIf { it != -1L }
        borrowerIdToOpenState.value = initialId

        setContent {
            // Manage dark theme state locally at the root level to react to setting toggles instantly
            val systemIsDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(true) } // Default to premium dark mode as specified
            val borrowerIdToOpen by borrowerIdToOpenState

            HisaabTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HisaabNavigation(
                        onToggleTheme = {
                            isDarkTheme = !isDarkTheme
                        },
                        borrowerIdToOpen = borrowerIdToOpen,
                        onClearOpenRequest = {
                            borrowerIdToOpenState.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent.getLongExtra("borrower_id", -1L).takeIf { it != -1L }
        borrowerIdToOpenState.value = id
    }
}
