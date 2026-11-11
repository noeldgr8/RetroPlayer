package com.retroplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.retroplayer.app.ui.HingePx
import com.retroplayer.app.ui.RetroRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val layoutInfo = WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this)
        setContent {
            val info by layoutInfo.collectAsStateWithLifecycle(initialValue = null)
            // Layout D only for a half-opened, horizontal fold (tabletop).
            val hinge = info?.displayFeatures
                ?.filterIsInstance<FoldingFeature>()
                ?.firstOrNull {
                    it.state == FoldingFeature.State.HALF_OPENED &&
                        it.orientation == FoldingFeature.Orientation.HORIZONTAL
                }
                ?.let { HingePx(it.bounds.top, it.bounds.bottom) }
            RetroRoot(viewModel, hinge)
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up access granted (or revoked) in system settings.
        viewModel.refreshAccess()
    }
}
