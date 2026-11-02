package com.vknxworks.ridectrl.data

import com.vknxworks.ridectrl.data.settings.AppSettings
import com.vknxworks.ridectrl.data.settings.DistanceUnit
import com.vknxworks.ridectrl.data.settings.TempUnit
import com.vknxworks.ridectrl.data.settings.VolumeUnit
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** What a backup file holds besides the photos: every log, plus the unit settings. */
@Serializable
data class BackupManifest(
    val app: String = "RideCtrl",
    val format: Int = FORMAT,
    val created: Long = 0L,
    val data: AppData = AppData(),
    val distance: String? = null,
    val volume: String? = null,
    val temperature: String? = null,
    val currency: String? = null,
    val warnDays: Int? = null,
    val warnKm: Double? = null,
) {
    fun applyTo(s: AppSettings): AppSettings = s.copy(
        distance = DistanceUnit.entries.firstOrNull { it.name == distance } ?: s.distance,
        volume = VolumeUnit.entries.firstOrNull { it.name == volume } ?: s.volume,
        temperature = TempUnit.entries.firstOrNull { it.name == temperature } ?: s.temperature,
        currency = currency?.takeIf { it.length == 3 } ?: s.currency,
        warnDays = warnDays ?: s.warnDays,
        warnKm = warnKm ?: s.warnKm,
    )

    companion object {
        const val FORMAT = 1
    }
}

class BackupException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_A_BACKUP, NEWER, DAMAGED, WRONG_PASSWORD, NEEDS_PASSWORD, TOO_BIG }
}

/** A read backup, checked and ready to restore: the manifest and its photos (name to bytes). */
class ReadBackup(val manifest: BackupManifest, val photos: Map<String, ByteArray>)

/**
 * Backup files: a zip with `ridectrl.json` and `photos/`. An encrypted backup is that zip sealed with
 * AES-256-GCM, its key made from the password with PBKDF2-HMAC-SHA256 (600,000 rounds, random salt):
 *
 *     "RIDECTRL-ENC1" | salt (16) | IV (12) | ciphertext + tag
 */
object BackupFile {
    private val MAGIC = "RIDECTRL-ENC1".toByteArray(Charsets.US_ASCII)
    private const val ITERATIONS = 600_000
    private const val MAX_BYTES = 1_000_000_000L
    /** Unpacked size limits, so a small file that unpacks to gigabytes (a "zip bomb") is refused. */
    private const val MAX_JSON_BYTES = 64L * 1024 * 1024
    private const val MAX_PHOTO_BYTES = 64L * 1024 * 1024

    fun write(out: OutputStream, data: AppData, settings: AppSettings, photosDir: File, password: CharArray?) {
        val manifest = BackupManifest(
            created = System.currentTimeMillis(),
            data = data,
            distance = settings.distance.name,
            volume = settings.volume.name,
            temperature = settings.temperature.name,
            currency = settings.currency,
            warnDays = settings.warnDays,
            warnKm = settings.warnKm,
        )
        if (password == null) {
            writeZip(out, manifest, data.photoNames(), photosDir)
        } else {
            val buf = ByteArrayOutputStream()
            writeZip(buf, manifest, data.photoNames(), photosDir)
            out.write(encrypt(buf.toByteArray(), password))
        }
    }

    private fun writeZip(out: OutputStream, manifest: BackupManifest, photos: Set<String>, photosDir: File) {
        val zip = ZipOutputStream(out)
        zip.putNextEntry(ZipEntry("ridectrl.json"))
        zip.write(dataJson.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
        zip.closeEntry()
        photos.forEach { name ->
            val f = File(photosDir, name)
            if (f.exists() && safeName(name)) {
                zip.putNextEntry(ZipEntry("photos/$name"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        zip.finish()
        zip.flush()
    }

    /** True when the stream starts like an encrypted backup. */
    fun isEncrypted(bytes: ByteArray): Boolean = bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun readAll(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) throw BackupException(BackupException.Reason.TOO_BIG)
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /** Checks and reads a whole backup; nothing on the phone changes here. */
    fun read(bytes: ByteArray, password: CharArray?): ReadBackup {
        val zipBytes = if (isEncrypted(bytes)) {
            if (password == null) throw BackupException(BackupException.Reason.NEEDS_PASSWORD)
            decrypt(bytes, password)
        } else {
            bytes
        }
        var manifest: BackupManifest? = null
        val photos = mutableMapOf<String, ByteArray>()
        var unpacked = 0L
        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == "ridectrl.json" -> {
                            val raw = readEntry(zip, MAX_JSON_BYTES)
                            unpacked += raw.size
                            manifest = dataJson.decodeFromString(BackupManifest.serializer(), raw.toString(Charsets.UTF_8))
                        }
                        e.name.startsWith("photos/") -> {
                            val name = e.name.removePrefix("photos/")
                            if (safeName(name)) {
                                val raw = readEntry(zip, MAX_PHOTO_BYTES)
                                unpacked += raw.size
                                photos[name] = raw
                            }
                        }
                    }
                    if (unpacked > MAX_BYTES) throw BackupException(BackupException.Reason.TOO_BIG)
                }
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(if (manifest == null) BackupException.Reason.NOT_A_BACKUP else BackupException.Reason.DAMAGED)
        }
        val m = manifest ?: throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        if (m.app != "RideCtrl") throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        if (m.format > BackupManifest.FORMAT) throw BackupException(BackupException.Reason.NEWER)
        if (!m.data.isConsistent()) throw BackupException(BackupException.Reason.DAMAGED)
        return ReadBackup(m, photos)
    }

    /** Reads one zip entry, refusing it once it unpacks to more than [limit] bytes. */
    private fun readEntry(zip: ZipInputStream, limit: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = zip.read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw BackupException(BackupException.Reason.TOO_BIG)
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /**
     * A backup RideCtrl itself wrote always passes: every id is unique within its list and every photo is one of
     * our own file names. Anything else is refused before it can replace the logs (duplicate ids would make
     * lists misbehave, and odd photo names could point outside the photo folder).
     */
    private fun AppData.isConsistent(): Boolean {
        fun unique(ids: List<String>) = ids.size == ids.toSet().size
        return unique(vehicles.map { it.id }) && unique(fuel.map { it.id }) && unique(odometer.map { it.id }) &&
            unique(trips.map { it.id }) && unique(serviceItems.map { it.id }) && unique(services.map { it.id }) &&
            unique(parts.map { it.id }) && unique(mods.map { it.id }) && unique(temps.map { it.id }) &&
            unique(docs.map { it.id }) && unique(notes.map { it.id }) &&
            photoNames().all { safeName(it) }
    }

    /** Photo names are our own UUID file names; anything else (paths, "..") is ignored. */
    private fun safeName(name: String): Boolean = name.matches(Regex("[A-Za-z0-9-]{1,64}\\.jpg"))

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(raw, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also(rnd::nextBytes)
        val iv = ByteArray(12).also(rnd::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val sealed = cipher.doFinal(plain)
        return MAGIC + salt + iv + sealed
    }

    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        val headerEnd = MAGIC.size + 16 + 12
        if (bytes.size <= headerEnd + 16) throw BackupException(BackupException.Reason.DAMAGED)
        val salt = bytes.copyOfRange(MAGIC.size, MAGIC.size + 16)
        val iv = bytes.copyOfRange(MAGIC.size + 16, headerEnd)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        return try {
            cipher.doFinal(bytes, headerEnd, bytes.size - headerEnd)
        } catch (e: AEADBadTagException) {
            // A wrong password and a damaged file look the same to GCM.
            throw BackupException(BackupException.Reason.WRONG_PASSWORD)
        }
    }
}

/** CSV export for spreadsheets: one file per log, every vehicle, plain metric units. */
object CsvExport {
    enum class Log { FUEL, SERVICES, PARTS, TEMPERATURES }

    private fun cell(v: Any?): String {
        val s = when (v) {
            null -> ""
            is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
            else -> v.toString()
        }
        return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    private fun row(vararg v: Any?): String = v.joinToString(",") { cell(it) }

    fun build(data: AppData, log: Log, currency: String): String {
        val names = data.vehicles.associate { it.id to it.name }
        val sb = StringBuilder()
        when (log) {
            Log.FUEL -> {
                sb.appendLine(row("vehicle", "date", "odometer_km", "litres", "price_per_litre_$currency", "total_$currency", "full_tank", "premix_ratio", "oil_ml", "oil_cost_$currency", "station", "notes"))
                data.fuel.sortedBy { it.date }.forEach {
                    sb.appendLine(row(names[it.vehicleId], it.date, it.odometer, it.litres, it.pricePerLitre, it.total, it.fullTank, it.premixRatio?.let { r -> "$r:1" }, it.oilMl, it.oilCost, it.station, it.notes))
                }
            }
            Log.SERVICES -> {
                val items = data.serviceItems.associate { it.id to it.name }
                val parts = data.parts.associate { it.id to it.name }
                sb.appendLine(row("vehicle", "date", "odometer_km", "items", "inventory_parts", "inventory_parts_cost_$currency", "other_parts_cost_$currency", "labour_$currency", "done_by", "shop", "notes"))
                data.services.sortedBy { it.date }.forEach { s ->
                    sb.appendLine(
                        row(
                            names[s.vehicleId], s.date, s.odometer,
                            s.itemIds.mapNotNull { items[it] }.joinToString("; "),
                            s.parts.joinToString("; ") { "${parts[it.partId] ?: "?"} x${it.quantity}" },
                            data.partsCost(s.parts), s.otherPartsCost, s.labourCost,
                            s.doneBy.name.lowercase(), s.shop, s.notes,
                        ),
                    )
                }
            }
            Log.PARTS -> {
                sb.appendLine(row("name", "brand", "part_number", "where_bought", "price_$currency", "in_stock", "low_stock_at", "fits", "notes"))
                data.parts.sortedBy { it.name.lowercase() }.forEach { p ->
                    sb.appendLine(row(p.name, p.brand, p.partNumber, p.whereBought, p.price, p.quantity, p.lowStockAt, p.vehicleIds.mapNotNull { names[it] }.joinToString("; "), p.notes))
                }
            }
            Log.TEMPERATURES -> {
                sb.appendLine(row("vehicle", "time", "temp_c", "odometer_km", "ambient_c", "ride_type", "setup", "notes"))
                data.temps.sortedBy { it.time }.forEach { t ->
                    sb.appendLine(row(names[t.vehicleId], java.time.Instant.ofEpochMilli(t.time).toString(), t.tempC, t.odometer, t.ambientC, t.rideType.name.lowercase(), t.setup, t.notes))
                }
            }
        }
        return sb.toString()
    }
}
