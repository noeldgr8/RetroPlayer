package com.retroplayer.core.media

import android.service.notification.NotificationListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Exists so the system grants this app access to other apps' media sessions.
 * It never reads notification content.
 */
@AndroidEntryPoint
class MediaListenerService : NotificationListenerService() {

    @Inject lateinit var repository: MediaRepository

    override fun onListenerConnected() {
        repository.refreshAccess()
    }

    override fun onListenerDisconnected() {
        repository.refreshAccess()
    }
}
