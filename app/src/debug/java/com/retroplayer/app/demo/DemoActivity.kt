package com.retroplayer.app.demo

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.retroplayer.app.MainActivity

/**
 * DEBUG ONLY. "RetroPlayer Demo" launcher entry: starts the demo session, then opens the app.
 * Extras: `--ez stop true` ends the demo session; `--es pin small|medium|large` asks the
 * launcher to pin that widget (for testing without dragging from the widget picker).
 * The app still needs notification access to see the session, like any other source.
 */
class DemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pin = intent.getStringExtra("pin")
        when {
            intent.getBooleanExtra("stop", false) -> DemoSession.stop()
            pin != null -> {
                DemoSession.start(this)
                val receiver = "com.retroplayer.widget.${pin.replaceFirstChar(Char::uppercase)}WidgetReceiver"
                getSystemService(AppWidgetManager::class.java)
                    .requestPinAppWidget(ComponentName(packageName, receiver), null, null)
            }
            else -> {
                DemoSession.start(this)
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
        finish()
    }
}
