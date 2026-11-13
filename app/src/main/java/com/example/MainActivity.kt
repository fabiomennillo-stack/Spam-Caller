package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.auth.authStateFlow
import com.example.ui.SpamcallerApp
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.SpamcallerTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialNumber = extractDialNumber(intent)
        setContent {
            SpamcallerTheme {
                AppNavigation(initialDialNumber = initialNumber)
            }
        }
    }

    private fun extractDialNumber(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return if (data.scheme == "tel") data.schemeSpecificPart else null
    }
}

@Composable
private fun AppNavigation(initialDialNumber: String? = null) {
    val context = LocalContext.current
    val hasFirebase = FirebaseApp.getApps(context).isNotEmpty()
    if (!hasFirebase) {
        SpamcallerApp(
            currentUserId = "local_device_user",
            initialDialNumber = initialDialNumber
        )
        return
    }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            SpamcallerApp(
                currentUserId = user.uid,
                initialDialNumber = initialDialNumber
            )
        }
    }
}
