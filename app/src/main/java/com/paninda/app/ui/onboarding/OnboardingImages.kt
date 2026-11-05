package com.paninda.app.ui.onboarding

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keep the supplied originals in nodpi, and avoid decoding full camera/brand-center images on the UI thread. */
@Composable
internal fun rememberOnboardingImage(@DrawableRes resource: Int, maxEdge: Int): ImageBitmap? {
    val resources = LocalContext.current.resources
    val image by produceState<ImageBitmap?>(null, resources, resource, maxEdge) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, resource, bounds)
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > maxEdge) inSampleSize *= 2
            }
            BitmapFactory.decodeResource(resources, resource, options)?.asImageBitmap()
        }
    }
    return image
}
