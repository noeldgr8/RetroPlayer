package com.retroplayer.app

import android.app.Application
import com.retroplayer.widget.WidgetSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RetroApp : Application() {

    @Inject lateinit var widgetSync: WidgetSync

    override fun onCreate() {
        super.onCreate()
        // Widgets update from media session changes for as long as the process lives
        // (the notification listener keeps it alive while access is granted).
        widgetSync.start()
    }
}
