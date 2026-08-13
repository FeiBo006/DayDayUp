package com.doapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

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
    val styleId: String = AppearanceOptions.STYLE_SOFT,
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
            styleId = prefs.getString(KEY_STYLE, AppearanceOptions.STYLE_SOFT)
                ?: AppearanceOptions.STYLE_SOFT,
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

    fun setBlur(value: Float) = write(_appearance.value.copy(blur = value))

    fun setDim(value: Float) = write(_appearance.value.copy(dim = value))

    fun selectStyle(id: String) = write(_appearance.value.copy(styleId = id))

    fun selectFont(id: String) = write(_appearance.value.copy(fontId = id))

    fun selectTextColor(id: String) = write(_appearance.value.copy(textColorId = id))

    fun setSelfReminder(text: String) = write(_appearance.value.copy(selfReminder = text.trim()))

    /**
     * Copies the picked image into app storage. The picker grants a one-shot read permission,
     * so keeping the Uri around would break on the next launch — we own the bytes instead.
     */
    fun importPhoto(uri: Uri): Boolean {
        val target = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
        }.isSuccess
        if (!ok) return false

        val previous = _appearance.value.photoFile
        write(_appearance.value.copy(photoPath = target.absolutePath, usePhoto = true))
        if (previous?.absolutePath != target.absolutePath) previous?.delete()
        return true
    }

    private fun write(next: Appearance) {
        _appearance.value = next
        prefs.edit()
            .putString(KEY_PRESET, next.presetId)
            .putString(KEY_PHOTO, next.photoPath)
            .putBoolean(KEY_USE_PHOTO, next.usePhoto)
            .putFloat(KEY_BLUR, next.blur)
            .putFloat(KEY_DIM, next.dim)
            .putString(KEY_STYLE, next.styleId)
            .putString(KEY_FONT, next.fontId)
            .putString(KEY_TEXT_COLOR, next.textColorId)
            .putString(KEY_SELF_REMINDER, next.selfReminder)
            .apply()
    }

    private companion object {
        const val KEY_PRESET = "preset"
        const val KEY_PHOTO = "photo"
        const val KEY_USE_PHOTO = "use_photo"
        const val KEY_BLUR = "blur"
        const val KEY_DIM = "dim"
        const val KEY_STYLE = "style"
        const val KEY_FONT = "font"
        const val KEY_TEXT_COLOR = "text_color"
        const val KEY_SELF_REMINDER = "self_reminder"
    }
}

object AppearanceOptions {
    const val STYLE_SOFT = "soft"
    const val STYLE_NEO_BRUTALIST = "neo-brutalist"
    const val STYLE_DOODLE = "doodle"

    const val FONT_SYSTEM = "system"
    const val FONT_SERIF = "serif"
    const val FONT_MONO = "mono"

    const val COLOR_DEFAULT = "default"
    const val COLOR_INK = "ink"
    const val COLOR_NAVY = "navy"
    const val COLOR_FOREST = "forest"
    const val COLOR_BURGUNDY = "burgundy"
}
