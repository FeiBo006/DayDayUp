package com.doapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val DEFAULT_SELF_REMINDER = "想做的事拖着不做，想去的地方不会等你，喜欢的人更不会一直等你。"

data class Appearance(
    val presetId: String = "sierra",
    /** Absolute path to the user's own wallpaper, copied into app storage. Null when unset. */
    val photoPath: String? = null,
    val usePhoto: Boolean = false,
    /** 0f..1f — how much the wallpaper is blurred behind the cards. */
    val blur: Float = 0.25f,
    /** 0f..1f — how far the wallpaper is pushed back so text stays legible. */
    val dim: Float = 0.18f,
    val fontId: String = AppearanceOptions.FONT_SYSTEM,
    val textColorId: String = AppearanceOptions.COLOR_DEFAULT,
    val selfReminder: String = DEFAULT_SELF_REMINDER,
) {
    val photoFile: File? get() = photoPath?.let(::File)?.takeIf { it.exists() }
    val showsPhoto: Boolean get() = usePhoto && photoFile != null
}

class AppearanceStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(
        Appearance(
            presetId = prefs.getString(KEY_PRESET, "sierra") ?: "sierra",
            photoPath = prefs.getString(KEY_PHOTO, null),
            usePhoto = prefs.getBoolean(KEY_USE_PHOTO, false),
            blur = prefs.getFloat(KEY_BLUR, 0.25f),
            dim = prefs.getFloat(KEY_DIM, 0.18f),
            fontId = prefs.getString(KEY_FONT, AppearanceOptions.FONT_SYSTEM)
                ?: AppearanceOptions.FONT_SYSTEM,
            textColorId = prefs.getString(KEY_TEXT_COLOR, AppearanceOptions.COLOR_DEFAULT)
                ?: AppearanceOptions.COLOR_DEFAULT,
            selfReminder = prefs.getString(KEY_SELF_REMINDER, DEFAULT_SELF_REMINDER)
                ?: DEFAULT_SELF_REMINDER,
        )
    )
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    /**
     * Switching to a built-in wallpaper drops the imported photo for good — there is no way back
     * to it in the UI (the photo tile always opens the picker), so keeping the file is dead weight.
     */
    fun selectPreset(id: String) {
        val previous = _appearance.value.photoFile
        write(_appearance.value.copy(presetId = id, usePhoto = false, photoPath = null))
        previous?.delete()
    }

    fun previewBlur(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_appearance.value.blur != clamped) {
            _appearance.value = _appearance.value.copy(blur = clamped)
        }
    }

    fun previewDim(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_appearance.value.dim != clamped) {
            _appearance.value = _appearance.value.copy(dim = clamped)
        }
    }

    /** Sliders preview every frame; persist their final values once when the gesture ends. */
    fun persistWallpaperAdjustments() {
        prefs.edit()
            .putFloat(KEY_BLUR, _appearance.value.blur)
            .putFloat(KEY_DIM, _appearance.value.dim)
            .apply()
    }

    fun selectFont(id: String) = write(_appearance.value.copy(fontId = id))

    fun selectTextColor(id: String) = write(_appearance.value.copy(textColorId = id))

    fun setSelfReminder(text: String) = write(_appearance.value.copy(selfReminder = text.trim()))

    /**
     * Copies the picked image into app storage. The picker grants a one-shot read permission,
     * so keeping the Uri around would break on the next launch — we own the bytes instead.
     */
    suspend fun importPhoto(uri: Uri): Boolean {
        val target = File(context.filesDir, "wallpaper_${UUID.randomUUID()}.jpg")
        var committed = false
        try {
            val copied = withContext(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: return@withContext false
                    input.use { source ->
                        target.outputStream().use { destination ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var total = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = source.read(buffer)
                                if (count < 0) break
                                total += count
                                if (total > MAX_PHOTO_BYTES) return@withContext false
                                destination.write(buffer, 0, count)
                            }
                        }
                    }
                    true
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    false
                }
            }
            if (!copied) return false

            val previous = _appearance.value.photoFile
            write(_appearance.value.copy(photoPath = target.absolutePath, usePhoto = true))
            committed = true
            if (previous?.absolutePath != target.absolutePath) {
                withContext(NonCancellable + Dispatchers.IO) { previous?.delete() }
            }
            return true
        } finally {
            // withContext(IO) promptly rethrows cancellation on the way back to Main. Always
            // remove a copied-but-uncommitted target, even when the sheet that launched us left.
            if (!committed) {
                withContext(NonCancellable + Dispatchers.IO) { target.delete() }
            }
        }
    }

    private fun write(next: Appearance) {
        _appearance.value = next
        prefs.edit()
            .putString(KEY_PRESET, next.presetId)
            .putString(KEY_PHOTO, next.photoPath)
            .putBoolean(KEY_USE_PHOTO, next.usePhoto)
            .putFloat(KEY_BLUR, next.blur)
            .putFloat(KEY_DIM, next.dim)
            .putString(KEY_FONT, next.fontId)
            .putString(KEY_TEXT_COLOR, next.textColorId)
            .putString(KEY_SELF_REMINDER, next.selfReminder)
            .apply()
    }

    private companion object {
        const val MAX_PHOTO_BYTES = 32L * 1024L * 1024L
        const val KEY_PRESET = "preset"
        const val KEY_PHOTO = "photo"
        const val KEY_USE_PHOTO = "use_photo"
        const val KEY_BLUR = "blur"
        const val KEY_DIM = "dim"
        const val KEY_FONT = "font"
        const val KEY_TEXT_COLOR = "text_color"
        const val KEY_SELF_REMINDER = "self_reminder"
    }
}

object AppearanceOptions {
    const val FONT_SYSTEM = "system"
    const val FONT_SERIF = "serif"
    const val FONT_MONO = "mono"

    const val COLOR_DEFAULT = "default"
    const val COLOR_INK = "ink"
    const val COLOR_NAVY = "navy"
    const val COLOR_FOREST = "forest"
    const val COLOR_BURGUNDY = "burgundy"
}
