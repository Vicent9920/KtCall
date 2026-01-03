package com.contact.ktcall.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.contact.ktcall.ui.screen.calling.InCallScreen

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InCallScreen(onCallEnded = { finish() })
        }
    }
}