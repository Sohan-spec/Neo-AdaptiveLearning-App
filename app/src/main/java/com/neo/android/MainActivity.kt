package com.neo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.neo.android.model.ModelManager
import com.neo.android.ui.chat.ChatScreen
import com.neo.android.ui.loading.LoadingScreen
import com.neo.android.ui.theme.NeoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            NeoTheme {
                var modelReady by remember { mutableStateOf(ModelManager.isModelReady(this)) }
                if (modelReady) {
                    ChatScreen()
                } else {
                    LoadingScreen(onReady = { modelReady = true })
                }
            }
        }
    }
}