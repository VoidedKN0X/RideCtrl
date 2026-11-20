package com.vknxworks.ridectrl.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import com.vknxworks.ridectrl.data.crypto.DataKeyStore
import com.vknxworks.ridectrl.data.crypto.SealedDataException
import com.vknxworks.ridectrl.data.crypto.SealedFile
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * Photos (vehicles, services, mods, documents) live in the app's private storage as JPEGs. They're picked
 * with Android's photo picker or taken with the camera app, so RideCtrl needs no storage or camera permission.
 *
 * Each file is encrypted at rest with the app's data key ([SealedFile]); photos from before encryption are
 * still read as they are and are sealed one by one by [sealAll]. Without a data key, photos are stored as
 * ordinary JPEGs, as before.
 */
class PhotoStore(private val context: Context, private val keys: DataKeyStore) {
    val dir: File = File(context.filesDir, "photos").apply { mkdirs() }
    private val cameraDir: File = File(context.cacheDir, "camera").apply { mkdirs() }

    /** Held while a photo file is written, replaced or deleted, so sealing an old photo never brings back a deleted one. */
    private val lock = Any()

    /** Decoded thumbnails kept in memory (by file name and size), so scrolling back to a photo doesn't read and decode it again. */
    private val cacheBytes: Int = (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(48L * 1024 * 1024).toInt()
    private val cache = object : LruCache<String, Bitmap>(cacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private fun cacheKey(name: String, maxSide: Int) = "$name@$maxSide"

    private fun evict(name: String) {
        cache.snapshot().keys.forEach { if (it.startsWith("$name@")) cache.remove(it) }
    }

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
        val jpeg = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, jpeg)
        bitmap.recycle()
        write(name, jpeg.toByteArray())
        name
    }.getOrNull()

    /** Stores the JPEG [bytes] as photo [name] (a new photo, or one from a backup being restored). */
    fun write(name: String, bytes: ByteArray) {
        synchronized(lock) {
            val sealed = keys.get(create = true)?.let { SealedFile.sealChecked(it, bytes) }
            if (sealed != null) SealedFile.writeAtomic(file(name), File(dir, "$name.tmp"), sealed)
            else file(name).writeBytes(bytes)
            evict(name)
        }
    }

    /**
     * The photo's JPEG bytes, decrypted when the file is sealed (for backups). Null when there's no such
     * photo; throws [SealedDataException] when it's sealed and can't be opened.
     */
    fun read(name: String): ByteArray? {
        val f = file(name)
        if (!f.exists()) return null
        val bytes = f.readBytes()
        if (!SealedFile.isSealed(bytes)) return bytes
        val key = keys.get(create = false) ?: throw SealedDataException("no data key")
        return SealedFile.open(key, bytes)
    }

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
        synchronized(lock) {
            file(name).delete()
            evict(name)
        }
    }

    /** Deletes every photo ("Forgot PIN?"). */
    fun deleteAll() {
        synchronized(lock) {
            dir.listFiles()?.forEach { it.delete() }
            cache.evictAll()
        }
    }

    /** Deletes every photo file except [keep] (after a restore). */
    fun deleteExcept(keep: Set<String>) {
        synchronized(lock) {
            dir.listFiles()?.forEach { if (it.name !in keep) it.delete() }
            cache.evictAll()
        }
    }

    /** The thumbnail that's already decoded and in memory, if any (no disk access). */
    fun cached(name: String, maxSide: Int = 512): Bitmap? = cache.get(cacheKey(name, maxSide))

    /** A small bitmap for lists and thumbnails. Reads and decodes the file: call it off the main thread. */
    fun thumbnail(name: String, maxSide: Int = 512): Bitmap? = runCatching {
        val k = cacheKey(name, maxSide)
        cache.get(k)?.let { return it }
        // Read in one go and decoded from memory, sealed or not: the file may be sealed at this very moment.
        val jpeg = read(name) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, BitmapFactory.Options().apply { inSampleSize = sample })
        // A full-screen photo is too big to keep next to the thumbnails.
        if (bitmap != null && bitmap.allocationByteCount <= cacheBytes / 4) cache.put(k, bitmap)
        bitmap
    }.getOrNull()

    /** Removes photo files nothing refers to any more (older than an hour, so an open editor keeps its new ones). */
    fun sweep(keep: Set<String>) {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        dir.listFiles()?.forEach { f ->
            if (f.name !in keep && f.lastModified() < cutoff) synchronized(lock) { f.delete() }
        }
    }

    /**
     * Encrypts the photos that are still ordinary JPEGs (from before encryption, or saved while there was no
     * data key), one file at a time. Each one is replaced in a single step by a sealed copy that was first
     * written out, read back and compared ([SealedFile.sealInPlace]); a photo that can't be sealed stays as
     * it is and still shows. Safe to stop at any point: the next start carries on with what's left.
     */
    fun sealAll() {
        val key = keys.get(create = true) ?: return
        dir.listFiles()?.forEach { f ->
            if (!f.name.endsWith(".jpg")) return@forEach
            synchronized(lock) {
                if (f.isFile && !SealedFile.isSealed(f)) SealedFile.sealInPlace(key, f, File(dir, "${f.name}.seal.tmp"))
            }
        }
    }

    companion object {
        const val MAX_SIDE = 2560
    }
}
