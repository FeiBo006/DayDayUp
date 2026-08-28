package com.doapp.ui

/**
 * A stable window class for the app workspace.
 *
 * Width alone is not enough: a 600dp tablet used to be compact in portrait and suddenly become
 * two columns after rotation. The shortest edge identifies the device/window class instead, so a
 * full-screen tablet stays a tablet in both orientations while a phone in landscape stays compact.
 */
internal enum class WorkspaceLayout {
    PHONE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE;

    val isTablet: Boolean get() = this != PHONE
    val usesWideColumns: Boolean get() = this == TABLET_LANDSCAPE
}

internal fun classifyWorkspace(widthDp: Int, heightDp: Int): WorkspaceLayout {
    val width = widthDp.coerceAtLeast(0)
    val height = heightDp.coerceAtLeast(0)
    if (minOf(width, height) < 600) return WorkspaceLayout.PHONE
    return if (width > height) WorkspaceLayout.TABLET_LANDSCAPE
    else WorkspaceLayout.TABLET_PORTRAIT
}
