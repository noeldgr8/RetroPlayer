package com.retroplayer.app.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** DEBUG ONLY. No-op; exists so the demo app is discoverable as a media player. */
class DemoMediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
