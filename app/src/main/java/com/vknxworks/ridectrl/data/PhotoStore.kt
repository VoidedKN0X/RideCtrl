package com.vknxworks.ridectrl.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max

/**
 * Photos (vehicles, services, mods, documents) live in the app's private storage as JPEGs. They're picked
 * with Android's photo picker or taken with the camera app, so RideCtrl needs no storage or camera permission.
 */
class PhotoStore(private val context: Context) {
    val dir: File = File(context.filesDir, "photos").apply { mkdirs() }
    private val cameraDir: File = File(context.cacheDir, "camera").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    /** Copies a picked or captured image in, downscaled to at most [MAX_SIDE] px. Returns its file name. */
    fun import(uri: Uri): String? = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val longest = max(w, h)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val name = "${newId()}.jpg"
        file(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        name
    }.getOrNull()

    /** A fresh file for the camera app to write into, shared through RideCtrl's file provider. */
    fun newCaptureUri(): Uri {
        cameraDir.listFiles()?.forEach { it.delete() }
        val f = File(cameraDir, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    }

    /** Removes the camera app's originals from the cache once they're copied in (or the capture was cancelled). */
    fun clearCaptures() {
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    fun delete(name: String) {
        file(name).delete()
    }

    /** A small bitmap for lists and thumbnails. */
    fun thumbnail(name: String, maxSide: Int = 512): Bitmap? = runCatching {
        val f = file(name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    /** Removes photo files nothing refers to any more (older than an hour, so an open editor keeps its new ones). */
    fun sweep(keep: Set<String>) {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        dir.listFiles()?.forEach { f ->
            if (f.name !in keep && f.lastModified() < cutoff) f.delete()
        }
    }

    companion object {
        const val MAX_SIDE = 2560
    }
}
