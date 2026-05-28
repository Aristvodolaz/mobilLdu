package com.app.mobilldu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.mobilldu.navigation.AppNavigation
import com.app.mobilldu.ui.theme.MobilLduTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilLduTheme {
                AppNavigation()
            }
        }
    }
}