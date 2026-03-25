package org.pysh.janus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.pysh.janus.ui.MainScreen

class MainActivity : ComponentActivity() {

    fun isModuleActive(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen(isModuleActive = isModuleActive())
        }
    }
}
