package com.doapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceLayoutTest {

    @Test
    fun tabletStaysTabletAcrossRotation() {
        assertEquals(
            WorkspaceLayout.TABLET_PORTRAIT,
            classifyWorkspace(widthDp = 600, heightDp = 960),
        )
        assertEquals(
            WorkspaceLayout.TABLET_LANDSCAPE,
            classifyWorkspace(widthDp = 960, heightDp = 600),
        )
    }

    @Test
    fun phoneStaysCompactAcrossRotation() {
        assertEquals(
            WorkspaceLayout.PHONE,
            classifyWorkspace(widthDp = 411, heightDp = 914),
        )
        assertEquals(
            WorkspaceLayout.PHONE,
            classifyWorkspace(widthDp = 914, heightDp = 411),
        )
    }

    @Test
    fun narrowMultiWindowUsesCompactLayout() {
        assertEquals(
            WorkspaceLayout.PHONE,
            classifyWorkspace(widthDp = 520, heightDp = 900),
        )
    }

    @Test
    fun onlyLandscapeTabletUsesWideColumns() {
        assertEquals(false, WorkspaceLayout.PHONE.usesWideColumns)
        assertEquals(false, WorkspaceLayout.TABLET_PORTRAIT.usesWideColumns)
        assertEquals(true, WorkspaceLayout.TABLET_LANDSCAPE.usesWideColumns)
    }
}
