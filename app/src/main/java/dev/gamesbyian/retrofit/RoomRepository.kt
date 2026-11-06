package dev.gamesbyian.retrofit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * A deliberately bounded one-room store. The chosen photo is copied into app-private storage
 * immediately, and a separate orientation-normalized JPEG is used for presentation/export.
 * The original raw bytes are never modified. No permission or data leaves this app.
 *
 * The active draft is written atomically. Multi-room + catalog persistence will use a
 * conventional database; this class must not become a second overlapping state authority.
 */
class RoomRepository(private val context: Context) {
    private val roomRoot = File(context.filesDir, "rooms")
    private val draftFile = AtomicFile(File(context.filesDir, "active-room.json"))
    private val writeLock = Mutex()

    suspend fun load(): RoomDraft? = withContext(Dispatchers.IO) {
        if (!draftFile.baseFile.exists()) return@withContext null
        val json = JSONObject(draftFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        require(json.getInt("schemaVersion") == 1) { "Unsupported room draft version" }
        val id = json.getString("id")
        require(safeRoomId(id)) { "Invalid saved room identity" }
        val markers = json.getJSONArray("markers")
        require(markers.length() <= 200) { "Saved marker count exceeds limit" }
        val entries = (0 until markers.length()).map { index ->
            val item = markers.getJSONObject(index)
            RoomMarker(
                id = item.getString("id"),
                label = item.getString("label"),
                x = item.getDouble("x").toFloat(),
                y = item.getDouble("y").toFloat(),
                action = item.getString("action"),
                detail = item.getString("detail")
            )
        }
        require(entries.all { it.x in 0f..1f && it.y in 0f..1f }) {
            "Saved coordinates are invalid"
        }
        RoomDraft(
            id = id,
            width = json.getInt("width"),
            height = json.getInt("height"),
            originalSha256 = json.getString("originalSha256"),
            orientedSha256 = json.getString("orientedSha256"),
            nextLabelIndex = json.getInt("nextLabelIndex"),
            instructions = json.getString("instructions"),
            markers = entries
        )
    }

    suspend fun save(draft: RoomDraft) = withContext(Dispatchers.IO) {
        require(safeRoomId(draft.id))
        writeLock.withLock {
            val markers = JSONArray()
            draft.markers.forEach { marker ->
                markers.put(JSONObject()
                    .put("id", marker.id)
                    .put("label", marker.label)
                    .put("x", marker.x.toDouble())
                    .put("y", marker.y.toDouble())
                    .put("action", marker.action)
                    .put("detail", marker.detail))
            }
            val body = JSONObject()
                .put("schemaVersion", 1)
                .put("id", draft.id)
                .put("width", draft.width)
                .put("height", draft.height)
                .put("originalSha256", draft.originalSha256)
                .put("orientedSha256", draft.orientedSha256)
                .put("nextLabelIndex", draft.nextLabelIndex)
                .put("instructions", draft.instructions)
                .put("markers", markers)
                .toString()
                .toByteArray(Charsets.UTF_8)
            val output = draftFile.startWrite()
            try {
                output.write(body)
                draftFile.finishWrite(output)
            } catch (error: Exception) {
                draftFile.failWrite(output)
                throw error
            }
        }
    }

    suspend fun importPhoto(uri: Uri): RoomDraft = withContext(Dispatchers.IO) {
        val id = "room_" + UUID.randomUUID().toString().replace("-", "")
        val folder = File(roomRoot, id)
        require(folder.mkdirs()) { "Could not create private project directory" }
        try {
            val source = File(folder, "original.bin")
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Photo picker returned an unreadable image")
            input.use { stream ->
                FileOutputStream(source).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var count = 0L
                    while (true) {
                        val amount = stream.read(buffer)
                        if (amount < 0) break
                        count += amount
                        require(count <= MAX_IMAGE_BYTES) {
                            "The selected image exceeds 20 MiB; choose a smaller photo"
                        }
                        output.write(buffer, 0, amount)
                    }
                    output.fd.sync()
                    require(count > 0) { "The selected photo was empty" }
                }
            }
            // Android ImageDecoder accounts for EXIF orientation in the decoded result.
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) {
                    decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                require(w in 1..8192 && h in 1..8192 &&
                    w.toLong() * h <= 24_000_000L) {
                    "The photo is too large to decode safely in this early preview"
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val oriented = File(folder, "oriented.jpg")
            try {
                FileOutputStream(oriented).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)) {
                        "Unable to write oriented photo"
                    }
                    output.fd.sync()
                }
                require(oriented.length() in 1..MAX_IMAGE_BYTES) {
                    "The normalized photo is too large for the current handoff budget"
                }
                RoomDraft(
                    id = id,
                    width = bitmap.width,
                    height = bitmap.height,
                    originalSha256 = sha256(source),
                    orientedSha256 = sha256(oriented)
                )
            } finally {
                bitmap.recycle()
            }
        } catch (error: Exception) {
            // Folder is newly generated and owned by this incomplete import only.
            folder.deleteRecursively()
            throw error
        }
    }

    suspend fun loadPreview(draft: RoomDraft): Bitmap? = withContext(Dispatchers.IO) {
        require(safeRoomId(draft.id))
        val file = File(File(roomRoot, draft.id), "oriented.jpg")
        require(file.isFile) { "The stored photo is missing" }
        val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, probe)
        require(probe.outWidth > 0 && probe.outHeight > 0) {
            "The stored photo cannot be decoded"
        }
        var sample = 1
        while (probe.outWidth / sample > 2048 || probe.outHeight / sample > 2048) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("Cannot decode the saved photo preview")
    }

    private fun safeRoomId(id: String) = id.matches(Regex("^room_[a-f0-9]{32}$"))

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 20L * 1024L * 1024L
    }
}
