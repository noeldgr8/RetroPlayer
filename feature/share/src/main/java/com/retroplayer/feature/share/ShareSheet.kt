package com.retroplayer.feature.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.retroplayer.feature.player.PlayerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * EN-02 share sheet (DESIGN.md 14.2): format toggle, live preview, Save image and Share.
 * The PNG is rendered on the phone and handed to the Android share sheet; the app uploads nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(state: PlayerUiState, onDismiss: () -> Unit) {
    var format by rememberSaveable { mutableStateOf(ShareFormat.Story) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current

    fun export(after: suspend (Bitmap) -> Unit) {
        scope.launch {
            runCatching {
                val image = layer.toImageBitmap().asAndroidBitmap()
                val soft = if (image.config == Bitmap.Config.HARDWARE) image.copy(Bitmap.Config.ARGB_8888, false) else image
                after(soft)
            }.onFailure { Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    // Open fully so Save / Share are visible straight away.
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.share_title),
                fontSize = 22.sp, lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ShareFormat.entries.forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = f == format,
                        onClick = { format = f },
                        shape = SegmentedButtonDefaults.itemShape(i, ShareFormat.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.height(44.dp),
                    ) { Text(stringResource(if (f == ShareFormat.Story) R.string.share_story else R.string.share_post)) }
                }
            }

            // Preview: the real card at export size, recorded into [layer] and drawn scaled down.
            val previewDescription = stringResource(R.string.cd_share_preview, state.title, state.artist)
            BoxWithConstraints(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
                val cardW = (format.exportW / density.density).dp
                val cardH = (format.exportH / density.density).dp
                val scale = minOf(maxHeight / cardH, maxWidth / cardW).coerceAtMost(1f)
                Box(
                    Modifier
                        .size(cardW * scale, cardH * scale)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .semantics { contentDescription = previewDescription },
                ) {
                    Box(
                        Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                    ) { ShareCard(format, state) }
                }
            }

            Text(
                stringResource(R.string.share_caption),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        export { bmp ->
                            withContext(Dispatchers.IO) { saveToPictures(context, bmp) }
                            Toast.makeText(context, R.string.share_saved, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.share_save)) }
                Button(
                    onClick = {
                        export { bmp ->
                            val file = withContext(Dispatchers.IO) { writeCachePng(context, bmp) }
                            shareFile(context, file)
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.share_share)) }
            }
        }
    }
}

private const val FILE_NAME = "now-spinning.png"

private fun writeCachePng(context: Context, bitmap: Bitmap): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, FILE_NAME)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return file
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}

/** Pictures/RetroPlayer via MediaStore (no storage permission needed on Android 10+). */
private fun saveToPictures(context: Context, bitmap: Bitmap) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "RetroPlayer-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RetroPlayer")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("insert failed")
    resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
}
