package com.smhabibjr.betterwififtp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.smhabibjr.betterwififtp.ui.WifiFtpApp
import com.smhabibjr.betterwififtp.ui.theme.BetterWiFiFTPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterWiFiFTPTheme {
                WifiFtpApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
